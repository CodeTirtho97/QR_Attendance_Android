package com.example.qrattendance.ui.instructor;

import android.content.Intent;
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

/**
 * InstructorDashboardActivity serves as the main screen for instructor users.
 * It will display courses, generate QR codes, and manage attendance records.
 */
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

    /**
     * Initialize UI components
     */
    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeInstructor);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Instructor Dashboard");
        }
    }

    /**
     * Set up user info in the dashboard
     */
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

    /**
     * Logout the current user and redirect to login screen
     */
    private void logout() {
        authRepository.logout();
        sessionManager.clearSession();

        // Redirect to login screen
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
    }
}