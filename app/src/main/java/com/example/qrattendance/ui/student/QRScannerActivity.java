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
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

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

        // Set up click listeners
        btnClose.setOnClickListener(v -> finish());
        btnTorch.setOnClickListener(v -> toggleTorch());
        btnDone.setOnClickListener(v -> finish());

        // Check and request camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startScanning();
        }

        // For testing on emulator
        if (TEST_MODE) {
            // Use test data instead of camera scanning
            processScanResult(TEST_QR_CONTENT);
        } else {
            // Regular camera scanning code
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
            } else {
                startScanning();
            }
        }

        Button btnDebugScan = findViewById(R.id.btnDebugScan);
        btnDebugScan.setOnClickListener(v -> {
            // Get the last generated QR content from shared preferences
            String savedQrContent = getSharedPreferences("QRAttendance", MODE_PRIVATE)
                    .getString("lastTestQRContent", null);

            if (savedQrContent != null) {
                debugScanQR(savedQrContent);
            } else {
                Toast.makeText(this, "No test QR content available. Generate a QR code first.",
                        Toast.LENGTH_LONG).show();
            }
        });
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

        // Mark attendance in Firebase
        attendanceRepository.markAttendance(scanContent, currentStudent.getUserId(), location,
                new AttendanceRepository.OnAttendanceListener() {
                    @Override
                    public void onSuccess(String message) {
                        // Show success overlay
                        runOnUiThread(() -> {
                            // Set timestamp to now
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
                            tvTimestamp.setText(sdf.format(new Date()));

                            // Update course details - in a real app, you'd get this from Firebase
                            // For demo, just show a message
                            tvCourseDetails.setText("Attendance marked successfully!");

                            // Show success overlay
                            successOverlay.setVisibility(View.VISIBLE);

                            // Add the attendance record ID to student's records
                            // Note: In a real app with proper transaction handling, this would be done in the repository
                            currentStudent.addAttendanceRecord("att_" + System.currentTimeMillis());
                        });
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
        Log.d("DEBUG_SCAN", "Processing test QR content: " + qrContent);

        // Create a location for testing
        AttendanceRecord.LocationData location = new AttendanceRecord.LocationData(
                0.0, 0.0, "Test Location");

        // Mark attendance with the test QR code
        attendanceRepository.markAttendance(qrContent, currentStudent.getUserId(), location,
                new AttendanceRepository.OnAttendanceListener() {
                    @Override
                    public void onSuccess(String message) {
                        Log.d("DEBUG_SCAN", "Attendance marked successfully: " + message);
                        runOnUiThread(() -> {
                            Toast.makeText(QRScannerActivity.this,
                                    "Attendance marked successfully!",
                                    Toast.LENGTH_LONG).show();

                            // Show success overlay
                            tvCourseDetails.setText("Attendance marked for test session");
                            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
                            tvTimestamp.setText(sdf.format(new Date()));
                            successOverlay.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onFailure(String errorMessage) {
                        Log.e("DEBUG_SCAN", "Attendance marking failed: " + errorMessage);
                        runOnUiThread(() -> {
                            Toast.makeText(QRScannerActivity.this,
                                    "Error: " + errorMessage,
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                });
    }
}