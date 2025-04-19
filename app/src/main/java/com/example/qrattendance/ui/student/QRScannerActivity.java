package com.example.qrattendance.ui.student;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.AttendanceRecord;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AttendanceRepository;
import com.example.qrattendance.util.SessionManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QRScannerActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;

    private static final boolean TEST_MODE = true; // Set to false for production
    private static final String TEST_QR_CONTENT = "{\"qrCodeId\":\"test_qr_123\",\"sessionId\":\"test_session_123\",\"timestamp\":\"1619123456789\"}";

    private DecoratedBarcodeView barcodeView;
    private ImageButton btnClose, btnTorch;
    private ConstraintLayout successOverlay;
    private TextView tvCourseDetails, tvTimestamp;
    private Button btnDone;

    private boolean torchOn = false;
    private boolean scanComplete = false;

    private AttendanceRepository attendanceRepository;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Initialize repository
        attendanceRepository = AttendanceRepository.getInstance();

        // Get current student from session
        currentStudent = (Student) SessionManager.getInstance(this).getUserData();
        if (currentStudent == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        barcodeView = findViewById(R.id.barcodeView);
        btnClose = findViewById(R.id.btnClose);
        btnTorch = findViewById(R.id.btnTorch);
        successOverlay = findViewById(R.id.successOverlay);
        tvCourseDetails = findViewById(R.id.tvCourseDetails);
        tvTimestamp = findViewById(R.id.tvTimestamp);
        btnDone = findViewById(R.id.btnDone);

        // Set up click listeners - make sure each of these buttons exists in your layout
        btnClose.setOnClickListener(v -> finish());
        btnTorch.setOnClickListener(v -> toggleTorch());
        btnDone.setOnClickListener(v -> finish());

        // Debug button - ONLY set click listener if the button exists in layout
        Button btnDebugScan = findViewById(R.id.btnDebugScan);
        if (btnDebugScan != null) {
            btnDebugScan.setOnClickListener(v -> {
                // Get the last generated QR content from shared preferences
                String savedQrContent = getSharedPreferences("QRAttendance", MODE_PRIVATE)
                        .getString("lastTestQRContent", null);

                if (savedQrContent != null) {
                    Log.d("QR_SCAN_DEBUG", "Scanned content: " + savedQrContent);
                    processScanResult(savedQrContent);
                } else {
                    Toast.makeText(this, "No test QR content available. Generate a QR code first.",
                            Toast.LENGTH_LONG).show();
                }
            });
        }

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null && !scanComplete) {
                    // Set scanComplete to prevent multiple scans
                    scanComplete = true;

                    // Pause the scanner
                    barcodeView.pause();

                    // Process the scan result
                    processScanResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Not needed
            }
        });
    }

    private void processScanResult(String scanContent) {
        // Create a simple location data (in a real app, you would use device location)
        AttendanceRecord.LocationData location = new AttendanceRecord.LocationData(
                0.0, 0.0, "Unknown Location");

        Log.d("QR_SCAN_DEBUG", "Scanned content: " + scanContent);

        // Mark attendance in Firebase
        attendanceRepository.markAttendance(scanContent, currentStudent.getUserId(), location,
                new AttendanceRepository.OnAttendanceListener() {
                    @Override
                    public void onSuccess(String message) {
                        // Parse the QR content to get sessionId
                        try {
                            JSONObject jsonObject = new JSONObject(scanContent);
                            String sessionId = jsonObject.getString("sessionId");

                            // Fetch session details to display course info
                            fetchSessionDetails(sessionId);
                        } catch (Exception e) {
                            // If parsing fails, just show generic success
                            showSuccessOverlay("Attendance marked successfully!", "Unknown Course", new Date());
                        }
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        // Show error message and resume scanning after delay
                        runOnUiThread(() -> {
                            Toast.makeText(QRScannerActivity.this,
                                    "Error: " + errorMessage, Toast.LENGTH_LONG).show();

                            // Resume scanning after a delay
                            scanComplete = false;
                            barcodeView.postDelayed(() -> barcodeView.resume(), 2000);
                        });
                    }
                });
    }


    private void fetchSessionDetails(String sessionId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("sessions").document(sessionId)
                .get()
                .addOnSuccessListener(sessionDoc -> {
                    if (sessionDoc.exists()) {
                        String courseId = sessionDoc.getString("courseId");
                        String sessionTitle = sessionDoc.getString("title");
                        Date timestamp = new Date();

                        // Fetch course details if courseId exists
                        if (courseId != null && !courseId.isEmpty()) {
                            db.collection("courses").document(courseId)
                                    .get()
                                    .addOnSuccessListener(courseDoc -> {
                                        if (courseDoc.exists()) {
                                            String courseName = courseDoc.getString("courseName");
                                            String courseCode = courseDoc.getString("courseCode");
                                            String courseInfo = courseCode + " - " + courseName;

                                            if (sessionTitle != null && !sessionTitle.isEmpty()) {
                                                courseInfo += "\n" + sessionTitle;
                                            }

                                            showSuccessOverlay("Attendance marked successfully!", courseInfo, timestamp);
                                        } else {
                                            showSuccessOverlay("Attendance marked successfully!", "Unknown Course", timestamp);
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        showSuccessOverlay("Attendance marked successfully!", "Unknown Course", timestamp);
                                    });
                        } else {
                            showSuccessOverlay("Attendance marked successfully!", "Unknown Course", timestamp);
                        }
                    } else {
                        showSuccessOverlay("Attendance marked successfully!", "Unknown Course", new Date());
                    }
                })
                .addOnFailureListener(e -> {
                    showSuccessOverlay("Attendance marked successfully!", "Unknown Course", new Date());
                });
    }

    // Helper method to display success overlay
    private void showSuccessOverlay(String message, String courseInfo, Date timestamp) {
        runOnUiThread(() -> {
            // Format timestamp
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            String timestampStr = sdf.format(timestamp);

            // Update UI elements
            tvCourseDetails.setText(courseInfo);
            tvTimestamp.setText(timestampStr);

            // Show success overlay
            successOverlay.setVisibility(View.VISIBLE);
        });
    }


    private void toggleTorch() {
        if (barcodeView != null) {
            if (torchOn) {
                barcodeView.setTorchOff();
            } else {
                barcodeView.setTorchOn();
            }
            torchOn = !torchOn;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!scanComplete) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    private void debugScanQR(String qrContent) {
        Log.d("QR_SCAN_DEBUG", "Scanning QR content: " + qrContent);

        try {
            // Parse the JSON to see what we're getting
            JSONObject jsonObject = new JSONObject(qrContent);
            String qrCodeId = jsonObject.optString("qrCodeId", "missing");
            String sessionId = jsonObject.optString("sessionId", "missing");

            Log.d("QR_SCAN_DEBUG", "Parsed QR code ID: " + qrCodeId);
            Log.d("QR_SCAN_DEBUG", "Parsed session ID: " + sessionId);

            // Verify this QR code exists in Firestore
            FirebaseFirestore.getInstance().collection("qr_codes")
                    .document(qrCodeId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            Log.d("QR_SCAN_DEBUG", "QR code found in database!");
                            // Continue with attendance marking
                            AttendanceRecord.LocationData location = new AttendanceRecord.LocationData(0.0, 0.0, "Debug Location");
                            attendanceRepository.markAttendance(qrContent, currentStudent.getUserId(), location,
                                    new AttendanceRepository.OnAttendanceListener() {
                                        @Override
                                        public void onSuccess(String message) {
                                            Log.d("QR_SCAN_DEBUG", "Attendance marked successfully: " + message);
                                            showSuccessOverlay("Success!", "Debug Attendance", new Date());
                                        }

                                        @Override
                                        public void onFailure(String errorMessage) {
                                            Log.e("QR_SCAN_DEBUG", "Attendance marking failed: " + errorMessage);
                                            Toast.makeText(QRScannerActivity.this,
                                                    "Error: " + errorMessage, Toast.LENGTH_LONG).show();
                                        }
                                    });
                        } else {
                            Log.e("QR_SCAN_DEBUG", "QR code NOT found in database!");
                            Toast.makeText(QRScannerActivity.this,
                                    "QR code not found in database. ID: " + qrCodeId, Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("QR_SCAN_DEBUG", "Error checking QR code: " + e.getMessage());
                        Toast.makeText(QRScannerActivity.this,
                                "Error checking QR code: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });

        } catch (Exception e) {
            Log.e("QR_SCAN_DEBUG", "Error parsing QR content: " + e.getMessage());
            Toast.makeText(this, "Invalid QR code format: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}