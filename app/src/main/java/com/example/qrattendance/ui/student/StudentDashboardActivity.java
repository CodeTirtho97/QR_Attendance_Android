package com.example.qrattendance.ui.student;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvStudentId;
    private CardView cardScanQr, cardMyAttendance, cardMyCourses, cardProfile, cardEnrollment;
    private FloatingActionButton fabScanQr;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Initialize session manager and auth repository
        sessionManager = SessionManager.getInstance(this);
        authRepository = AuthRepository.getInstance();

        // Initialize UI components
        initViews();
        setupUserInfo();
        setupClickListeners();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeStudent);
        tvStudentId = findViewById(R.id.tvStudentId);

        // Find all card views
        cardScanQr = findViewById(R.id.cardScanQr);
        cardMyAttendance = findViewById(R.id.cardMyAttendance);
        cardMyCourses = findViewById(R.id.cardMyCourses);
        cardProfile = findViewById(R.id.cardProfile);
        cardEnrollment = findViewById(R.id.cardEnrollment); // New enrollment card

        // FAB for scanning
        fabScanQr = findViewById(R.id.fabScanQr);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Student Dashboard");
        }
    }

    private void setupClickListeners() {
        // Setup click listeners for all cards and the FAB
        cardScanQr.setOnClickListener(v -> openQRScanner());
        cardMyAttendance.setOnClickListener(v -> viewMyAttendance());
        cardMyCourses.setOnClickListener(v -> viewMyCourses());
        cardProfile.setOnClickListener(v -> viewProfile());
        cardEnrollment.setOnClickListener(v -> openCourseEnrollment());
        fabScanQr.setOnClickListener(v -> openQRScanner());
    }

    private void openQRScanner() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivity(intent);
    }

    private void viewMyAttendance() {
        // Open MyAttendanceActivity to view attendance records
        Intent intent = new Intent(this, MyAttendanceActivity.class);
        startActivity(intent);
    }

    private void viewMyCourses() {
        // For now, this just shows a toast message
        UIHelper.showErrorToast(this, "My Courses feature coming soon");
    }

    private void viewProfile() {
        // For now, this just shows a toast message
        UIHelper.showErrorToast(this, "Profile feature coming soon");
    }

    private void openCourseEnrollment() {
        // Open CourseEnrollmentActivity
        Intent intent = new Intent(this, CourseEnrollmentActivity.class);
        startActivity(intent);
    }

    private void setupUserInfo() {
        Student student = (Student) sessionManager.getUserData();
        if (student != null) {
            String welcomeMessage = "Welcome, " + student.getName() + "!";
            tvWelcome.setText(welcomeMessage);

            String studentIdText = "ID: " + (student.getRollNumber() != null ?
                    student.getRollNumber() : "Unknown");
            tvStudentId.setText(studentIdText);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);

        // Force white tint for all menu items
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            Drawable icon = item.getIcon();
            if (icon != null) {
                icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        authRepository.logout();
        SessionManager.getInstance(this).logout(this);

        UIHelper.showSuccessToast(this, "Logged out successfully");
    }
}