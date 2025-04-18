package com.example.qrattendance.ui.instructor;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.util.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class GenerateQRActivity extends AppCompatActivity {

    private AutoCompleteTextView courseDropdown, sessionDropdown;
    private TextView tvCourseInfo, tvQRValidity, tvExpiryTime;
    private ImageView ivQRCode;
    private Button btnGenerateQR, btnShareQR, btnDisplayQR;
    private CardView cardQRResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // Initialize UI components
        initViews();
        setupDropdowns();
        setupListeners();
    }

    private void initViews() {
        // Dropdowns
        courseDropdown = findViewById(R.id.courseDropdown);
        sessionDropdown = findViewById(R.id.sessionDropdown);

        // Buttons
        btnGenerateQR = findViewById(R.id.btnGenerateQR);
        btnShareQR = findViewById(R.id.btnShareQR);
        btnDisplayQR = findViewById(R.id.btnDisplayQR);

        // QR code result views
        cardQRResult = findViewById(R.id.cardQRResult);
        ivQRCode = findViewById(R.id.ivQRCode);
        tvCourseInfo = findViewById(R.id.tvCourseInfo);
        tvQRValidity = findViewById(R.id.tvQRValidity);
        tvExpiryTime = findViewById(R.id.tvExpiryTime);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Generate QR Code");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupDropdowns() {
        // Sample courses - in a real app, these would come from the database
        String[] courses = {"Software Engineering", "Mobile Application Development", "Data Structures", "Database Systems"};
        ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, courses);
        courseDropdown.setAdapter(courseAdapter);

        // Sample sessions - would change based on selected course
        String[] sessions = {"Lecture 1", "Lecture 2", "Tutorial 1", "Lab Session 1"};
        ArrayAdapter<String> sessionAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, sessions);
        sessionDropdown.setAdapter(sessionAdapter);
    }

    private void setupListeners() {
        btnGenerateQR.setOnClickListener(v -> generateQRCode());

        btnShareQR.setOnClickListener(v -> {
            // Implement share functionality here
            Toast.makeText(this, "Share functionality coming soon!", Toast.LENGTH_SHORT).show();
        });

        btnDisplayQR.setOnClickListener(v -> {
            // Implement full screen display here
            Toast.makeText(this, "Full screen display coming soon!", Toast.LENGTH_SHORT).show();
        });
    }

    private void generateQRCode() {
        String course = courseDropdown.getText().toString();
        String session = sessionDropdown.getText().toString();

        if (course.isEmpty() || session.isEmpty()) {
            Toast.makeText(this, "Please select both course and session", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Create QR code content (would be more complex in a real app)
            String qrCodeId = UUID.randomUUID().toString();
            String sessionId = UUID.randomUUID().toString();

            // Create QR code content
            String content = "{\"qrCodeId\":\"" + qrCodeId + "\",\"sessionId\":\"" + sessionId +
                    "\",\"course\":\"" + course + "\",\"session\":\"" + session +
                    "\",\"timestamp\":\"" + System.currentTimeMillis() + "\"}";

            // Generate QR code bitmap
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            // Set QR code image
            ivQRCode.setImageBitmap(bitmap);

            // Set other UI elements
            tvCourseInfo.setText(course + " - " + session);

            // Calculate expiry time (15 minutes from now)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 15);
            Date expiryTime = calendar.getTime();

            // Format and set expiry time text
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            tvQRValidity.setText("Valid for 15 minutes");
            tvExpiryTime.setText("Expires at " + sdf.format(expiryTime));

            // Show QR code result card
            cardQRResult.setVisibility(View.VISIBLE);

            // In a real app, we would save the QR code to the database here
            saveQRCodeToDatabase(qrCodeId, sessionId, content, expiryTime);
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void saveQRCodeToDatabase(String qrCodeId, String sessionId, String content, Date expiryTime) {
        // This is a placeholder for saving to Firebase
        // In a real app implementation, we would:
        // 1. Create a QRCode object
        // 2. Save it to Firestore
        // 3. Update the instructor's generated QR codes list

        // For now, just get the current user (instructor)
        Instructor instructor = (Instructor) SessionManager.getInstance(this).getUserData();
        if (instructor != null) {
            instructor.addGeneratedQRCode(qrCodeId);
            // In a real app, we would update this in Firestore
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}