package com.example.qrattendance.ui.student;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.util.SessionManager;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

    private DecoratedBarcodeView barcodeView;
    private ImageButton btnClose, btnTorch;
    private ConstraintLayout successOverlay;
    private TextView tvCourseDetails, tvTimestamp;
    private Button btnDone;

    private boolean torchOn = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

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
    }

    private void startScanning() {
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (result != null && result.getText() != null) {
                    // Stop scanning when a QR code is detected
                    barcodeView.pause();

                    // Process the scan result
                    processScanResult(result.getText());
                }
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {
                // Implement this required method (can be empty)
            }
        });
    }

    private void processScanResult(String scanContent) {
        try {
            // Parse the QR code content
            JsonObject jsonObject = JsonParser.parseString(scanContent).getAsJsonObject();

            String qrCodeId = jsonObject.get("qrCodeId").getAsString();
            String sessionId = jsonObject.get("sessionId").getAsString();
            String course = jsonObject.get("course").getAsString();
            String session = jsonObject.get("session").getAsString();

            // Mark attendance in database (would be implemented fully in a real app)
            markAttendance(qrCodeId, sessionId);

            // Show success overlay
            tvCourseDetails.setText(course + " - " + session);
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault());
            tvTimestamp.setText(sdf.format(new Date()));
            successOverlay.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            // Show error message
            Toast.makeText(this, "Invalid QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            // Resume scanning after a delay
            barcodeView.postDelayed(() -> barcodeView.resume(), 2000);
        }
    }

    private void markAttendance(String qrCodeId, String sessionId) {
        // In a real app, this would communicate with Firebase to:
        // 1. Verify the QR code is valid (not expired)
        // 2. Mark the student's attendance for this session
        // 3. Update the student's attendance records

        // For now, just get the current user (student)
        Student student = (Student) SessionManager.getInstance(this).getUserData();
        if (student != null) {
            // In a real app, we would update this in Firestore
            // For now, just add a placeholder attendanceId
            student.addAttendanceRecord("att_" + System.currentTimeMillis());
        }
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
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}