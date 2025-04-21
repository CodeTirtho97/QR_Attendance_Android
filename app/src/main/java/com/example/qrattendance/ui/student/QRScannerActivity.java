package com.example.qrattendance.ui.student;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.AttendanceRecord;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AttendanceRepository;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QRScannerActivity extends AppCompatActivity {

    private static final String TAG = "QRScannerActivity";
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
    private CourseRepository courseRepository;
    private Student currentStudent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        // Initialize repositories
        attendanceRepository = AttendanceRepository.getInstance();
        courseRepository = CourseRepository.getInstance();

        // Get current student from session
        currentStudent = (Student) SessionManager.getInstance(this).getUserData();
        if (currentStudent == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
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

        // Debug button - ONLY set click listener if the button exists in layout
        Button btnDebugScan = findViewById(R.id.btnDebugScan);
        if (btnDebugScan != null) {
            btnDebugScan.setOnClickListener(v -> {
                // Show dialog with list of active QR codes to test
                showTestQRSelection();
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
        try {
            // Parse QR code content
            JSONObject jsonObject = new JSONObject(scanContent);

            // Extract course ID if available
            String courseId = null;
            if (jsonObject.has("courseId")) {
                courseId = jsonObject.getString("courseId");
            }

            // If courseId is not in the QR content, extract it from the session
            if (courseId == null && jsonObject.has("sessionId")) {
                String sessionId = jsonObject.getString("sessionId");
                courseId = getSessionCourseId(sessionId);
            }

            // If we have a courseId, check enrollment before marking attendance
            if (courseId != null) {
                checkEnrollmentAndMarkAttendance(scanContent, courseId);
            } else {
                // If we still couldn't get courseId, proceed with attendance marking
                // This is a fallback for compatibility with older QR codes
                markAttendance(scanContent);
            }

        } catch (Exception e) {
            scanComplete = false;
            barcodeView.resume();
            UIHelper.showErrorDialog(this, "Error", "Invalid QR code format: " + e.getMessage());
        }
    }

    private String getSessionCourseId(String sessionId) {
        // This is a synchronous method to get courseId from sessionId
        // In a real app, you might want to make this asynchronous
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String[] courseId = {null};

        try {
            DocumentSnapshot sessionDoc = db.collection("sessions").document(sessionId)
                    .get()
                    .getResult();

            if (sessionDoc.exists()) {
                courseId[0] = sessionDoc.getString("courseId");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting session data: " + e.getMessage());
        }

        return courseId[0];
    }

    private void checkEnrollmentAndMarkAttendance(String scanContent, String courseId) {
        // Check if student is enrolled in the course
        courseRepository.isStudentEnrolled(courseId, currentStudent.getUserId(),
                isEnrolled -> {
                    if (isEnrolled) {
                        // Student is enrolled, proceed with marking attendance
                        markAttendance(scanContent);
                    } else {
                        // Student is not enrolled
                        runOnUiThread(() -> {
                            UIHelper.showErrorDialog(this, "Not Enrolled",
                                    "You are not enrolled in this course. Please enroll before marking attendance.");

                            // Ask if they want to go to enrollment screen
                            new AlertDialog.Builder(this)
                                    .setTitle("Enroll Now?")
                                    .setMessage("Would you like to go to the course enrollment screen?")
                                    .setPositiveButton("Yes", (dialog, which) -> {
                                        // Open CourseEnrollmentActivity
                                        // navigateToCourseEnrollment();
                                        finish();
                                    })
                                    .setNegativeButton("No", (dialog, which) -> {
                                        // Resume scanning
                                        scanComplete = false;
                                        barcodeView.resume();
                                    })
                                    .show();
                        });
                    }
                });
    }

    private void markAttendance(String scanContent) {
        // Create a simple location data (in a real app, you would use device location)
        AttendanceRecord.LocationData location = new AttendanceRecord.LocationData(
                0.0, 0.0, "Unknown Location");

        Log.d(TAG, "Scanned content: " + scanContent);

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
                            UIHelper.showErrorDialog(QRScannerActivity.this, "Error", errorMessage);

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
                UIHelper.showErrorDialog(this, "Permission Required",
                        "Camera permission is required to scan QR codes");
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

    private void showTestQRSelection() {
        // Create AlertDialog with a list of active QR codes
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select a QR code to test");

        // Show loading indicator
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading QR codes...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Fetch active QR codes from Firestore
        FirebaseFirestore.getInstance().collection("qr_codes")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    progressDialog.dismiss();

                    if (querySnapshot.isEmpty()) {
                        UIHelper.showErrorToast(this, "No QR codes found");
                        return;
                    }

                    // Create list of QR codes with session/course info
                    List<Map<String, String>> qrInfoList = new ArrayList<>();

                    // Counter for tracking async operations
                    AtomicInteger counter = new AtomicInteger(0);
                    int totalQrCodes = querySnapshot.size();

                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String qrCodeId = doc.getId();
                        String sessionId = doc.getString("sessionId");
                        String courseId = doc.getString("courseId");
                        String qrContent = doc.getString("content");
                        boolean isActive = Boolean.TRUE.equals(doc.getBoolean("isActive"));

                        Map<String, String> qrInfo = new HashMap<>();
                        qrInfo.put("qrCodeId", qrCodeId);
                        qrInfo.put("content", qrContent);
                        qrInfo.put("isActive", isActive ? "Active" : "Inactive");

                        // Fetch session details
                        if (sessionId != null) {
                            FirebaseFirestore.getInstance().collection("sessions").document(sessionId)
                                    .get()
                                    .addOnSuccessListener(sessionDoc -> {
                                        String sessionTitle = sessionDoc.getString("title");
                                        qrInfo.put("sessionTitle", sessionTitle != null ? sessionTitle : "Unknown Session");

                                        // Fetch course details
                                        if (courseId != null) {
                                            FirebaseFirestore.getInstance().collection("courses").document(courseId)
                                                    .get()
                                                    .addOnSuccessListener(courseDoc -> {
                                                        String courseName = courseDoc.getString("courseName");
                                                        String courseCode = courseDoc.getString("courseCode");

                                                        qrInfo.put("courseInfo", (courseCode != null ? courseCode : "Unknown") +
                                                                " - " + (courseName != null ? courseName : "Course"));

                                                        // Check if this is the last item to process
                                                        if (counter.incrementAndGet() == totalQrCodes) {
                                                            showQRSelectionDialog(builder, qrInfoList);
                                                        }
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        qrInfo.put("courseInfo", "Error loading course");
                                                        if (counter.incrementAndGet() == totalQrCodes) {
                                                            showQRSelectionDialog(builder, qrInfoList);
                                                        }
                                                    });
                                        } else {
                                            qrInfo.put("courseInfo", "No Course ID");
                                            if (counter.incrementAndGet() == totalQrCodes) {
                                                showQRSelectionDialog(builder, qrInfoList);
                                            }
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        qrInfo.put("sessionTitle", "Error loading session");
                                        qrInfo.put("courseInfo", "Unknown Course");

                                        if (counter.incrementAndGet() == totalQrCodes) {
                                            showQRSelectionDialog(builder, qrInfoList);
                                        }
                                    });
                        } else {
                            qrInfo.put("sessionTitle", "No Session ID");
                            qrInfo.put("courseInfo", "Unknown Course");

                            if (counter.incrementAndGet() == totalQrCodes) {
                                showQRSelectionDialog(builder, qrInfoList);
                            }
                        }

                        qrInfoList.add(qrInfo);
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    UIHelper.showErrorToast(this, "Error fetching QR codes: " + e.getMessage());
                });
    }

    private void showQRSelectionDialog(AlertDialog.Builder builder, List<Map<String, String>> qrInfoList) {
        // Sort the list - active QR codes first, then by course code
        Collections.sort(qrInfoList, (a, b) -> {
            // Active status comparison
            int statusCompare = b.get("isActive").compareTo(a.get("isActive"));
            if (statusCompare != 0) return statusCompare;

            // Then by course code
            return a.get("courseInfo").compareTo(b.get("courseInfo"));
        });

        // Create description strings
        String[] qrDescriptions = new String[qrInfoList.size()];
        for (int i = 0; i < qrInfoList.size(); i++) {
            Map<String, String> info = qrInfoList.get(i);
            qrDescriptions[i] = info.get("courseInfo") + "\n" +
                    info.get("sessionTitle") + " (" + info.get("isActive") + ")";
        }

        builder.setItems(qrDescriptions, (dialog, which) -> {
            // Process the selected QR code
            String selectedQRContent = qrInfoList.get(which).get("content");
            if (selectedQRContent != null) {
                processScanResult(selectedQRContent);
            } else {
                UIHelper.showErrorToast(QRScannerActivity.this, "Error: QR content is null");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}