package com.example.qrattendance.ui.instructor;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.auth.LoginActivity;
import com.example.qrattendance.util.SessionManager;

public class InstructorDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instructor_dashboard);

        // Initialize session manager and auth repository
        sessionManager = SessionManager.getInstance(this);
        authRepository = AuthRepository.getInstance();

        // Initialize UI components
        initViews();
        setupUserInfo();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeInstructor);

        // Find all clickable elements
        findViewById(R.id.cardGenerateQr).setOnClickListener(v -> generateQRCode());
        findViewById(R.id.cardViewAttendance).setOnClickListener(v -> viewAttendance());
        findViewById(R.id.cardManageCourses).setOnClickListener(v -> manageCourses());
        findViewById(R.id.cardReports).setOnClickListener(v -> viewReports());

        // FAB click listener
        findViewById(R.id.fabGenerateQr).setOnClickListener(v -> generateQRCode());

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Instructor Dashboard");
        }
    }

    // Add these handler methods
    private void generateQRCode() {
        Intent intent = new Intent(this, GenerateQRActivity.class);
        startActivity(intent);
    }

    private void viewAttendance() {
        // TODO: Open View Attendance activity
        Toast.makeText(this, "View Attendance feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void manageCourses() {
        Intent intent = new Intent(this, ManageCoursesActivity.class);
        startActivity(intent);
    }

    private void viewReports() {
        // TODO: Open Reports activity
        Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void setupUserInfo() {
        Instructor instructor = (Instructor) sessionManager.getUserData();
        if (instructor != null) {
            String welcomeMessage = "Welcome, " + instructor.getName() + "!";
            tvWelcome.setText(welcomeMessage);
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

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}