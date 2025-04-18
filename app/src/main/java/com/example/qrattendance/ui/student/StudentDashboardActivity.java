package com.example.qrattendance.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.auth.LoginActivity;
import com.example.qrattendance.util.SessionManager;

public class StudentDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
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
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeStudent);

        // Find all clickable elements
        findViewById(R.id.cardScanQr).setOnClickListener(v -> openQRScanner());
        findViewById(R.id.cardMyAttendance).setOnClickListener(v -> viewMyAttendance());
        findViewById(R.id.cardMyCourses).setOnClickListener(v -> viewMyCourses());
        findViewById(R.id.cardProfile).setOnClickListener(v -> viewProfile());

        // FAB click listener
        findViewById(R.id.fabScanQr).setOnClickListener(v -> openQRScanner());

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Student Dashboard");
        }
    }

    // Add these handler methods
    private void openQRScanner() {
        Intent intent = new Intent(this, QRScannerActivity.class);
        startActivity(intent);
    }

    private void viewMyAttendance() {
        // TODO: Open Attendance view activity
        Toast.makeText(this, "View Attendance feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void viewMyCourses() {
        // TODO: Open My Courses activity
        Toast.makeText(this, "My Courses feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void viewProfile() {
        // TODO: Open Profile activity
        Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void setupUserInfo() {
        Student student = (Student) sessionManager.getUserData();
        if (student != null) {
            String welcomeMessage = "Welcome, " + student.getName() + "!";
            tvWelcome.setText(welcomeMessage);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_dashboard, menu);
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

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}