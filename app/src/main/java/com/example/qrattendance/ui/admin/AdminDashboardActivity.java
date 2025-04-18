package com.example.qrattendance.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.auth.LoginActivity;
import com.example.qrattendance.util.SessionManager;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize session manager and auth repository
        sessionManager = SessionManager.getInstance(this);
        authRepository = AuthRepository.getInstance();

        // Initialize UI components
        initViews();
        setupUserInfo();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeAdmin);

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
    }

    private void setupUserInfo() {
        Admin admin = (Admin) sessionManager.getUserData();
        if (admin != null) {
            String welcomeMessage = "Welcome, " + admin.getName() + "!";
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