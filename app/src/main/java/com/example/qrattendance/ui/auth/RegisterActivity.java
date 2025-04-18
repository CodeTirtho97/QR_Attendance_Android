package com.example.qrattendance.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.User;
import com.example.qrattendance.data.repository.AuthRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    // UI Components
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private LinearLayout layoutStudentFields, layoutInstructorFields, layoutAdminFields;
    private Button btnRegister;
    private ProgressBar progressBar;
    private TextView tvLogin;

    // Role-specific fields
    private TextInputLayout tilRollNumber, tilDepartment, tilSemester, tilBatch;
    private TextInputEditText etRollNumber, etDepartment, etSemester, etBatch;
    private TextInputLayout tilEmployeeId, tilInstructorDepartment, tilDesignation;
    private TextInputEditText etEmployeeId, etInstructorDepartment, etDesignation;
    private TextInputLayout tilAdminId, tilPosition;
    private TextInputEditText etAdminId, etPosition;
    private Spinner spinnerPrivilegeLevel;

    // Repository
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize repository
        authRepository = AuthRepository.getInstance();

        // Initialize views
        initializeViews();

        // Setup event listeners
        setupEventListeners();
    }

    private void initializeViews() {
        // Common fields
        tilName = findViewById(R.id.tilName);
        tilEmail = findViewById(R.id.tilEmail);
        tilPhone = findViewById(R.id.tilPhone);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        // Role selection
        rgRole = findViewById(R.id.rgRole);

        // Role-specific layouts
        layoutStudentFields = findViewById(R.id.layoutStudentFields);
        layoutInstructorFields = findViewById(R.id.layoutInstructorFields);
        layoutAdminFields = findViewById(R.id.layoutAdminFields);

        // Student fields
        tilRollNumber = findViewById(R.id.tilRollNumber);
        tilDepartment = findViewById(R.id.tilDepartment);
        tilSemester = findViewById(R.id.tilSemester);
        tilBatch = findViewById(R.id.tilBatch);
        etRollNumber = findViewById(R.id.etRollNumber);
        etDepartment = findViewById(R.id.etDepartment);
        etSemester = findViewById(R.id.etSemester);
        etBatch = findViewById(R.id.etBatch);

        // Instructor fields
        tilEmployeeId = findViewById(R.id.tilEmployeeId);
        tilInstructorDepartment = findViewById(R.id.tilInstructorDepartment);
        tilDesignation = findViewById(R.id.tilDesignation);
        etEmployeeId = findViewById(R.id.etEmployeeId);
        etInstructorDepartment = findViewById(R.id.etInstructorDepartment);
        etDesignation = findViewById(R.id.etDesignation);

        // Admin fields
        tilAdminId = findViewById(R.id.tilAdminId);
        tilPosition = findViewById(R.id.tilPosition);
        etAdminId = findViewById(R.id.etAdminId);
        etPosition = findViewById(R.id.etPosition);
        spinnerPrivilegeLevel = findViewById(R.id.spinnerPrivilegeLevel);

        // Button and others
        btnRegister = findViewById(R.id.btnRegister);
        progressBar = findViewById(R.id.progressBar);
        tvLogin = findViewById(R.id.tvLogin);

        // Setup the admin spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Course Admin", "Department Admin", "Super Admin"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrivilegeLevel.setAdapter(adapter);
    }

    private void setupEventListeners() {
        // Role selection listener
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbStudent) {
                layoutStudentFields.setVisibility(View.VISIBLE);
                layoutInstructorFields.setVisibility(View.GONE);
                layoutAdminFields.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbInstructor) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutInstructorFields.setVisibility(View.VISIBLE);
                layoutAdminFields.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbAdmin) {
                layoutStudentFields.setVisibility(View.GONE);
                layoutInstructorFields.setVisibility(View.GONE);
                layoutAdminFields.setVisibility(View.VISIBLE);
            }
        });

        // Register button listener
        btnRegister.setOnClickListener(v -> {
            if (validateInputs()) {
                registerUser();
            }
        });

        // Login text listener
        tvLogin.setOnClickListener(v -> finish());
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate basic fields
        if (TextUtils.isEmpty(etName.getText())) {
            tilName.setError(getString(R.string.field_required));
            isValid = false;
        } else {
            tilName.setError(null);
        }

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

        if (TextUtils.isEmpty(etPhone.getText())) {
            tilPhone.setError(getString(R.string.field_required));
            isValid = false;
        } else {
            tilPhone.setError(null);
        }

        String password = etPassword.getText().toString();
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.field_required));
            isValid = false;
        } else if (password.length() < 6) {
            tilPassword.setError(getString(R.string.invalid_password));
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        String confirmPassword = etConfirmPassword.getText().toString();
        if (TextUtils.isEmpty(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.field_required));
            isValid = false;
        } else if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.error_passwords_dont_match));
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        // Validate role-specific fields
        int checkedRadioButtonId = rgRole.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbStudent) {
            // Validate student fields
            if (TextUtils.isEmpty(etRollNumber.getText())) {
                tilRollNumber.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilRollNumber.setError(null);
            }

            if (TextUtils.isEmpty(etDepartment.getText())) {
                tilDepartment.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilDepartment.setError(null);
            }

            if (TextUtils.isEmpty(etSemester.getText())) {
                tilSemester.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilSemester.setError(null);
            }

            if (TextUtils.isEmpty(etBatch.getText())) {
                tilBatch.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilBatch.setError(null);
            }
        } else if (checkedRadioButtonId == R.id.rbInstructor) {
            // Validate instructor fields
            if (TextUtils.isEmpty(etEmployeeId.getText())) {
                tilEmployeeId.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilEmployeeId.setError(null);
            }

            if (TextUtils.isEmpty(etInstructorDepartment.getText())) {
                tilInstructorDepartment.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilInstructorDepartment.setError(null);
            }

            if (TextUtils.isEmpty(etDesignation.getText())) {
                tilDesignation.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilDesignation.setError(null);
            }
        } else if (checkedRadioButtonId == R.id.rbAdmin) {
            // Validate admin fields
            if (TextUtils.isEmpty(etAdminId.getText())) {
                tilAdminId.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilAdminId.setError(null);
            }

            if (TextUtils.isEmpty(etPosition.getText())) {
                tilPosition.setError(getString(R.string.field_required));
                isValid = false;
            } else {
                tilPosition.setError(null);
            }
        }

        return isValid;
    }

    private void registerUser() {
        // Show loading
        progressBar.setVisibility(View.VISIBLE);
        btnRegister.setEnabled(false);

        // Get user data
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Check selected role
        User.UserRole role;
        int checkedRadioButtonId = rgRole.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbInstructor) {
            role = User.UserRole.INSTRUCTOR;
        } else if (checkedRadioButtonId == R.id.rbAdmin) {
            role = User.UserRole.ADMIN;
        } else {
            role = User.UserRole.STUDENT;
        }

        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", etName.getText().toString().trim());
        userData.put("phoneNumber", etPhone.getText().toString().trim());

        // Add role-specific data
        if (role == User.UserRole.STUDENT) {
            userData.put("rollNumber", etRollNumber.getText().toString().trim());
            userData.put("department", etDepartment.getText().toString().trim());
            userData.put("semester", etSemester.getText().toString().trim());
            userData.put("batch", etBatch.getText().toString().trim());
        } else if (role == User.UserRole.INSTRUCTOR) {
            userData.put("employeeId", etEmployeeId.getText().toString().trim());
            userData.put("department", etInstructorDepartment.getText().toString().trim());
            userData.put("designation", etDesignation.getText().toString().trim());
        } else if (role == User.UserRole.ADMIN) {
            userData.put("adminId", etAdminId.getText().toString().trim());
            userData.put("position", etPosition.getText().toString().trim());

            // Get privilege level
            String privLevel;
            switch (spinnerPrivilegeLevel.getSelectedItemPosition()) {
                case 1:
                    privLevel = Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN.name();
                    break;
                case 2:
                    privLevel = Admin.AdminPrivilegeLevel.SUPER_ADMIN.name();
                    break;
                default:
                    privLevel = Admin.AdminPrivilegeLevel.COURSE_ADMIN.name();
                    break;
            }
            userData.put("privilegeLevel", privLevel);
        }

        // Check if user with this email already exists
        authRepository.checkIfUserExists(email, userExists -> {
            if (userExists) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnRegister.setEnabled(true);
                    tilEmail.setError("Email already in use");
                });
            } else {
                // Register user
                authRepository.registerUser(email, password, role, userData);

                // Observe results
                authRepository.getCurrentUser().observe(this, user -> {
                    if (user != null) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Registration successful", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });

                authRepository.getAuthError().observe(this, error -> {
                    if (error != null && !error.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        btnRegister.setEnabled(true);

                        new AlertDialog.Builder(RegisterActivity.this)
                                .setTitle(R.string.error)
                                .setMessage(error)
                                .setPositiveButton(R.string.ok, null)
                                .setCancelable(true)
                                .show();

                        // Clear the error after showing
                        authRepository.clearError();
                    }
                });
            }
        }, e -> {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                btnRegister.setEnabled(true);
                new AlertDialog.Builder(RegisterActivity.this)
                        .setTitle(R.string.error)
                        .setMessage(e.getMessage())
                        .setPositiveButton(R.string.ok, null)
                        .show();
            });
        });
    }
}