package com.example.qrattendance.ui.instructor;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
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
import com.example.qrattendance.data.model.Session;
import com.example.qrattendance.data.repository.AttendanceRepository;
import com.example.qrattendance.data.repository.CourseRepository;
import com.example.qrattendance.util.SessionManager;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GenerateQRActivity extends AppCompatActivity {

    private AutoCompleteTextView courseDropdown, sessionDropdown;
    private TextView tvCourseInfo, tvQRValidity, tvExpiryTime;
    private ImageView ivQRCode;
    private Button btnGenerateQR, btnShareQR, btnDisplayQR;
    private CardView cardQRResult;

    private CourseRepository courseRepository;
    private AttendanceRepository attendanceRepository;
    private Instructor currentInstructor;

    private List<Course> instructorCourses = new ArrayList<>();
    private Session currentSession; // Current session for QR generation

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_generate_qr);

        // Initialize repositories
        courseRepository = CourseRepository.getInstance();
        attendanceRepository = AttendanceRepository.getInstance();

        // Get current instructor from session
        currentInstructor = (Instructor) SessionManager.getInstance(this).getUserData();
        if (currentInstructor == null) {
            Toast.makeText(this, "Session error. Please log in again.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize UI components
        initViews();
        loadCourses();
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
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Generate QR Code");
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        Button btnDebugTest = findViewById(R.id.btnDebugTest);
        btnDebugTest.setOnClickListener(v -> debugGenerateQR());
    }

    private void loadCourses() {
        // Observe courses
        courseRepository.getCourses().observe(this, courses -> {
            instructorCourses = courses;

            // Create course dropdown items
            String[] courseItems = new String[courses.size()];
            for (int i = 0; i < courses.size(); i++) {
                Course course = courses.get(i);
                courseItems[i] = course.getCourseCode() + " - " + course.getCourseName();
            }

            // Set up dropdown adapter
            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, courseItems);
            courseDropdown.setAdapter(courseAdapter);
        });

        // Observe errors
        courseRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Load the instructor's courses
        courseRepository.fetchCoursesByInstructor(currentInstructor.getUserId());
    }

    private void setupListeners() {
        // Course selection listener
        courseDropdown.setOnItemClickListener((parent, view, position, id) -> {
            // When course is selected, update session dropdown
            updateSessionDropdown(position);
        });

        // Generate QR button listener
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

    private void updateSessionDropdown(int coursePosition) {
        if (coursePosition >= 0 && coursePosition < instructorCourses.size()) {
            Course selectedCourse = instructorCourses.get(coursePosition);

            // For simplicity, create session types as dropdown items
            // In a real app, you would fetch actual sessions from Firebase
            String[] sessionTypes = {
                    "Lecture", "Tutorial", "Lab", "Exam", "Quiz"
            };

            ArrayAdapter<String> sessionAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_dropdown_item_1line, sessionTypes);
            sessionDropdown.setAdapter(sessionAdapter);
            sessionDropdown.setText("", false);
        }
    }

    private void generateQRCode() {
        String courseText = courseDropdown.getText().toString();
        String sessionType = sessionDropdown.getText().toString();
        String validityStr = ((TextView) findViewById(R.id.etValidityPeriod)).getText().toString();
        String location = ((TextView) findViewById(R.id.etLocation)).getText().toString();

        // Validate inputs
        if (courseText.isEmpty() || sessionType.isEmpty() || validityStr.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find selected course
        Course selectedCourse = null;
        for (int i = 0; i < instructorCourses.size(); i++) {
            Course course = instructorCourses.get(i);
            String courseItem = course.getCourseCode() + " - " + course.getCourseName();
            if (courseItem.equals(courseText)) {
                selectedCourse = course;
                break;
            }
        }

        if (selectedCourse == null) {
            Toast.makeText(this, "Please select a valid course", Toast.LENGTH_SHORT).show();
            return;
        }

        // Parse validity period
        int validityMinutes;
        try {
            validityMinutes = Integer.parseInt(validityStr);
            if (validityMinutes <= 0) {
                Toast.makeText(this, "Validity period must be positive", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number for validity period", Toast.LENGTH_SHORT).show();
            return;
        }

        // If location is empty, set a default
        if (location.isEmpty()) {
            location = "Default Location";
        }

        // Create a new session
        Calendar now = Calendar.getInstance();
        Date startTime = now.getTime();

        // Set end time to validity minutes later
        now.add(Calendar.MINUTE, validityMinutes);
        Date endTime = now.getTime();

        // Create session title based on session type and current time
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String sessionTitle = sessionType + " - " + sdf.format(startTime);

        // Create new session
        Session newSession = new Session(
                selectedCourse.getCourseId(),
                sessionTitle,
                startTime,
                endTime,
                location,
                currentInstructor.getUserId()
        );

        // Create session in Firebase
        attendanceRepository.createSession(newSession, new AttendanceRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String sessionId) {
                // Session created successfully, generate QR code
                newSession.setSessionId(sessionId);
                currentSession = newSession;

                // Calculate expiry time for QR code (same as session end time)
                Date expiryTime = endTime;

                // Generate QR code for the session
                attendanceRepository.generateQRCodeForSession(newSession, expiryTime,
                        new AttendanceRepository.OnQRCodeListener() {
                            @Override
                            public void onSuccess(String qrCodeId, String qrCodeContent) {
                                // QR code generated successfully, display it
                                generateQRBitmap(qrCodeContent, validityMinutes);
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Toast.makeText(GenerateQRActivity.this,
                                        "Failed to generate QR code: " + errorMessage,
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
            }

            @Override
            public void onFailure(String errorMessage) {
                Toast.makeText(GenerateQRActivity.this,
                        "Failed to create session: " + errorMessage,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void generateQRBitmap(String content, int validityMinutes) {
        try {
            // Generate QR code bitmap
            MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
            BitMatrix bitMatrix = multiFormatWriter.encode(content, BarcodeFormat.QR_CODE, 500, 500);
            BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
            Bitmap bitmap = barcodeEncoder.createBitmap(bitMatrix);

            // Display the QR code
            runOnUiThread(() -> {
                // Set QR code image
                ivQRCode.setImageBitmap(bitmap);

                // Set course and session info
                if (currentSession != null) {
                    tvCourseInfo.setText(currentSession.getTitle() + "\nLocation: " + currentSession.getLocation());
                }

                // Set validity info
                tvQRValidity.setText("Valid for " + validityMinutes + " minutes");

                // Calculate and display expiry time
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MINUTE, validityMinutes);
                Date expiryTime = calendar.getTime();

                SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
                tvExpiryTime.setText("Expires at " + sdf.format(expiryTime));

                // Show QR result card
                cardQRResult.setVisibility(View.VISIBLE);
            });
        } catch (Exception e) {
            Toast.makeText(this, "Error generating QR code: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void debugGenerateQR() {
        // Get the selected course from dropdown
        String courseText = courseDropdown.getText().toString();
        if (courseText.isEmpty()) {
            Toast.makeText(this, "Please select a course first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Find the selected course
        Course selectedCourse = null;
        for (Course course : instructorCourses) {
            String courseItem = course.getCourseCode() + " - " + course.getCourseName();
            if (courseItem.equals(courseText)) {
                selectedCourse = course;
                break;
            }
        }

        if (selectedCourse == null) {
            Toast.makeText(this, "Please select a valid course", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use the actual course ID from the selected course
        String courseId = selectedCourse.getCourseId();
        String sessionTitle = "Debug Session";
        String location = "Debug Location";

        // Create test Session
        Session testSession = new Session(
                courseId,  // Use the actual course ID
                sessionTitle,
                new Date(),  // Start time now
                new Date(System.currentTimeMillis() + 30 * 60 * 1000),  // End time 30 mins later
                location,
                currentInstructor.getUserId()
        );

        // Log the process
        Log.d("DEBUG_QR", "Creating test session for course: " + courseId);

        // Create session in Firestore
        attendanceRepository.createSession(testSession, new AttendanceRepository.OnCompleteListener() {
            @Override
            public void onSuccess(String sessionId) {
                Log.d("DEBUG_QR", "Session created successfully with ID: " + sessionId);
                testSession.setSessionId(sessionId);

                // Generate QR code for the session
                Date expiryTime = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
                attendanceRepository.generateQRCodeForSession(testSession, expiryTime,
                        new AttendanceRepository.OnQRCodeListener() {
                            @Override
                            public void onSuccess(String qrCodeId, String qrCodeContent) {
                                Log.d("DEBUG_QR", "QR Code generated with ID: " + qrCodeId);
                                Log.d("DEBUG_QR", "QR Content: " + qrCodeContent);

                                // Save QR content for testing
                                // You can add a TextView on your screen to display this value
                                runOnUiThread(() -> {
                                    Toast.makeText(GenerateQRActivity.this,
                                            "QR Code generated for testing: " + qrCodeId,
                                            Toast.LENGTH_LONG).show();

                                    // Store the QR content in shared preferences for scanning tests
                                    getSharedPreferences("QRAttendance", MODE_PRIVATE)
                                            .edit()
                                            .putString("lastTestQRContent", qrCodeContent)
                                            .apply();
                                });
                            }

                            @Override
                            public void onFailure(String errorMessage) {
                                Log.e("DEBUG_QR", "QR Code generation failed: " + errorMessage);
                                runOnUiThread(() -> {
                                    Toast.makeText(GenerateQRActivity.this,
                                            "QR Code generation failed: " + errorMessage,
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        }
                );
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e("DEBUG_QR", "Session creation failed: " + errorMessage);
                runOnUiThread(() -> {
                    Toast.makeText(GenerateQRActivity.this,
                            "Session creation failed: " + errorMessage,
                            Toast.LENGTH_LONG).show();
                });
            }
        });
    }
}