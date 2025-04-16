package com.example.qrattendance.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.repository.AuthRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * ForgotPasswordActivity handles password reset functionality
 * by sending reset emails to users.
 */
public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputLayout tilEmail;
    private TextInputEditText etEmail;
    private Button btnSendResetLink;
    private TextView tvBackToLogin;
    private ProgressBar progressBar;

    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize authentication repository
        authRepository = AuthRepository.getInstance();

        // Initialize UI components
        initViews();
        setupListeners();
    }

    /**
     * Initialize all UI components
     */
    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        etEmail = findViewById(R.id.etEmail);
        btnSendResetLink = findViewById(R.id.btnSendResetLink);
        tvBackToLogin = findViewById(R.id.tvBackToLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    /**
     * Setup click listeners for buttons and text views
     */
    private void setupListeners() {
        // Send reset link button click
        btnSendResetLink.setOnClickListener(v -> {
            if (validateEmail()) {
                String email = etEmail.getText().toString().trim();
                sendPasswordResetEmail(email);
            }
        });

        // Back to login click
        tvBackToLogin.setOnClickListener(v -> finish());
    }

    /**
     * Validate email input
     * @return true if email is valid, false otherwise
     */
    private boolean validateEmail() {
        String email = etEmail.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.field_required));
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return false;
        } else {
            tilEmail.setError(null);
            return true;
        }
    }

    /**
     * Send password reset email
     * @param email The email address to send the reset link to
     */
    private void sendPasswordResetEmail(String email) {
        progressBar.setVisibility(View.VISIBLE);
        btnSendResetLink.setEnabled(false);

        // First, check if the user exists with this email
        authRepository.checkIfUserExists(email,
                userExists -> {
                    if (userExists) {
                        // User exists, send reset email
                        authRepository.sendPasswordResetEmail(email)
                                .addOnSuccessListener(aVoid -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnSendResetLink.setEnabled(true);
                                    showSuccessDialog();
                                })
                                .addOnFailureListener(e -> {
                                    progressBar.setVisibility(View.GONE);
                                    btnSendResetLink.setEnabled(true);
                                    showErrorDialog(e.getMessage());
                                });
                    } else {
                        // No user with this email
                        progressBar.setVisibility(View.GONE);
                        btnSendResetLink.setEnabled(true);
                        showErrorDialog("No account found with this email");
                    }
                },
                e -> {
                    progressBar.setVisibility(View.GONE);
                    btnSendResetLink.setEnabled(true);
                    showErrorDialog(e.getMessage());
                }
        );
    }

    /**
     * Show success dialog after reset link is sent
     */
    private void showSuccessDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.success)
                .setMessage(R.string.send_reset_link)
                .setPositiveButton(R.string.ok, (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * Show error dialog with the given message
     * @param message Error message to display
     */
    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.error)
                .setMessage(message)
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}