package com.example.qrattendance.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.model.User;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.admin.AdminDashboardActivity;
import com.example.qrattendance.ui.instructor.InstructorDashboardActivity;
import com.example.qrattendance.ui.student.StudentDashboardActivity;
import com.example.qrattendance.util.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    // UI components
    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvRegister;
    private ProgressBar progressBar;

    // Repository for authentication
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize authentication repository
        authRepository = AuthRepository.getInstance();

        // Check if user is already logged in
        if (SessionManager.getInstance(this).isLoggedIn()) {
            redirectBasedOnUserRole(authRepository.getCurrentUser().getValue());
            return;
        }

        // Initialize UI components
        initViews();
        setupListeners();
        observeAuthState();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvRegister = findViewById(R.id.tvRegisterPrompt);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupListeners() {
        // Login button click
        btnLogin.setOnClickListener(v -> {
            if (validateInputs()) {
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                authRepository.login(email, password);
            }
        });

        // Forgot password click
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });

        // Register click
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void observeAuthState() {
        // Observe current user
        authRepository.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // Save user session
                SessionManager.getInstance(this).saveUserSession(user);

                // Redirect to appropriate dashboard based on user role
                redirectBasedOnUserRole(user);
            }
        });

        // Observe authentication errors - FIX HERE
        authRepository.getAuthError().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                showErrorDialog(errorMsg);
                // Clear the error after showing it
                authRepository.clearError();  // You'll need to add this method
            }
        });

        // Observe loading state
        authRepository.getIsLoading().observe(this, isLoading -> {
            if (progressBar != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
            if (btnLogin != null) {
                btnLogin.setEnabled(!isLoading);
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate email
        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.field_required));
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        // Validate password
        String password = etPassword.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.field_required));
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.invalid_password));
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        return isValid;
    }

    private void showErrorDialog(String message) {
        if (isFinishing()) return;  // Don't show dialog if activity is finishing

        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .setCancelable(true)
                .show();
    }

    private void redirectBasedOnUserRole(User user) {
        if (user == null) {
            Log.e(TAG, "Cannot redirect: User is null");
            return;
        }

        // Refresh student enrolled courses if it's a student
        if (user instanceof Student) {
            FirebaseFirestore.getInstance().collection("users").document(user.getUserId())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> enrolledCourseIds = (List<String>) documentSnapshot.get("enrolledCourseIds");
                            if (enrolledCourseIds != null) {
                                ((Student) user).setEnrolledCourseIds(enrolledCourseIds);
                                // Update the user in session manager
                                SessionManager.getInstance(this).saveUserSession(user);
                            }

                            // Continue with redirection
                            continueRedirection(user);
                        } else {
                            // Just continue with redirection
                            continueRedirection(user);
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Just continue with redirection
                        continueRedirection(user);
                    });
        } else {
            // For non-student users, just redirect
            continueRedirection(user);
        }
    }

    private void continueRedirection(User user) {
        Intent intent;

        // Check user role and redirect accordingly
        if (user instanceof Student) {
            intent = new Intent(LoginActivity.this, StudentDashboardActivity.class);
        } else if (user instanceof Instructor) {
            intent = new Intent(LoginActivity.this, InstructorDashboardActivity.class);
        } else if (user instanceof Admin) {
            intent = new Intent(LoginActivity.this, AdminDashboardActivity.class);
        } else {
            Log.e(TAG, "Unknown user role: " + user.getRole());
            Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show();
            return;
        }

        // Start the dashboard activity
        startActivity(intent);
        finish(); // Close the login activity
    }
}