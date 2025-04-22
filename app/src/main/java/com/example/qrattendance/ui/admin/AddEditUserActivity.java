package com.example.qrattendance.ui.admin;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.data.repository.UserRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddEditUserActivity extends AppCompatActivity {

    private static final String TAG = "AddEditUserActivity";

    // UI Components
    private Toolbar toolbar;
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private RadioGroup rgRole;
    private RadioButton rbStudent, rbInstructor, rbAdmin;
    private LinearLayout layoutStudentFields, layoutInstructorFields, layoutAdminFields;
    private Button btnSave;
    private ProgressBar progressBar;

    // Role-specific fields
    private TextInputLayout tilRollNumber, tilDepartment, tilSemester, tilBatch;
    private TextInputEditText etRollNumber, etDepartment, etSemester, etBatch;
    private TextInputLayout tilEmployeeId, tilInstructorDepartment, tilDesignation;
    private TextInputEditText etEmployeeId, etInstructorDepartment, etDesignation;
    private TextInputLayout tilAdminId, tilPosition;
    private TextInputEditText etAdminId, etPosition;
    private Spinner spinnerPrivilegeLevel;

    // Data
    private UserRepository userRepository;
    private AuthRepository authRepository;
    private Admin currentAdmin;
    private String userId; // Will be null for new users, populated for edits
    private boolean isEditMode;
    private Map<String, Object> userData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_user);

        // Initialize repositories
        userRepository = UserRepository.getInstance();
        authRepository = AuthRepository.getInstance();

        // Get current admin from session
        currentAdmin = (Admin) SessionManager.getInstance(this).getUserData();
        if (currentAdmin == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Check if we're in edit mode
        userId = getIntent().getStringExtra("userId");
        isEditMode = userId != null;

        // Initialize views
        initViews();
        setupEventListeners();

        // Setup based on mode (add vs edit)
        if (isEditMode) {
            loadUserData();
            // Password fields not needed for edit
            tilPassword.setVisibility(View.GONE);
            tilConfirmPassword.setVisibility(View.GONE);

            // Disable email field in edit mode
            etEmail.setEnabled(false);

            setTitle("Edit User");
        } else {
            setTitle("Add New User");
        }
    }

    private void initViews() {
        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

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
        rbStudent = findViewById(R.id.rbStudent);
        rbInstructor = findViewById(R.id.rbInstructor);
        rbAdmin = findViewById(R.id.rbAdmin);

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

        // Button and progress
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);

        // Set up admin privileges spinner
        ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Course Admin", "Department Admin", "Super Admin"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrivilegeLevel.setAdapter(adapter);

        // Limit admin's ability to create higher privilege levels
        if (currentAdmin.getPrivilegeLevel() == Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN) {
            // Department admins can only create Course Admins
            String[] limitedPrivileges = {"Course Admin"};
            spinnerPrivilegeLevel.setAdapter(new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, limitedPrivileges));
        } else if (currentAdmin.getPrivilegeLevel() == Admin.AdminPrivilegeLevel.COURSE_ADMIN) {
            // Course admins cannot create other admins
            rbAdmin.setEnabled(false);
        }
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

        // Save button listener
        btnSave.setOnClickListener(v -> {
            if (validateInputs()) {
                if (isEditMode) {
                    updateUser();
                } else {
                    createUser();
                }
            }
        });
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate basic fields
        if (TextUtils.isEmpty(etName.getText())) {
            tilName.setError("Name is required");
            isValid = false;
        } else {
            tilName.setError(null);
        }

        String email = etEmail.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            isValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email address");
            isValid = false;
        } else {
            tilEmail.setError(null);
        }

        if (TextUtils.isEmpty(etPhone.getText())) {
            tilPhone.setError("Phone number is required");
            isValid = false;
        } else {
            tilPhone.setError(null);
        }

        // Only validate password for new users
        if (!isEditMode) {
            String password = etPassword.getText().toString();
            if (TextUtils.isEmpty(password)) {
                tilPassword.setError("Password is required");
                isValid = false;
            } else if (password.length() < 6) {
                tilPassword.setError("Password must be at least 6 characters");
                isValid = false;
            } else {
                tilPassword.setError(null);
            }

            String confirmPassword = etConfirmPassword.getText().toString();
            if (TextUtils.isEmpty(confirmPassword)) {
                tilConfirmPassword.setError("Confirm password is required");
                isValid = false;
            } else if (!password.equals(confirmPassword)) {
                tilConfirmPassword.setError("Passwords don't match");
                isValid = false;
            } else {
                tilConfirmPassword.setError(null);
            }
        }

        // Validate role-specific fields
        int checkedRadioButtonId = rgRole.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbStudent) {
            // Validate student fields
            if (TextUtils.isEmpty(etRollNumber.getText())) {
                tilRollNumber.setError("Roll number is required");
                isValid = false;
            } else {
                tilRollNumber.setError(null);
            }

            if (TextUtils.isEmpty(etDepartment.getText())) {
                tilDepartment.setError("Department is required");
                isValid = false;
            } else {
                tilDepartment.setError(null);
            }

            if (TextUtils.isEmpty(etSemester.getText())) {
                tilSemester.setError("Semester is required");
                isValid = false;
            } else {
                tilSemester.setError(null);
            }

            if (TextUtils.isEmpty(etBatch.getText())) {
                tilBatch.setError("Batch is required");
                isValid = false;
            } else {
                tilBatch.setError(null);
            }
        } else if (checkedRadioButtonId == R.id.rbInstructor) {
            // Validate instructor fields
            if (TextUtils.isEmpty(etEmployeeId.getText())) {
                tilEmployeeId.setError("Employee ID is required");
                isValid = false;
            } else {
                tilEmployeeId.setError(null);
            }

            if (TextUtils.isEmpty(etInstructorDepartment.getText())) {
                tilInstructorDepartment.setError("Department is required");
                isValid = false;
            } else {
                tilInstructorDepartment.setError(null);
            }

            if (TextUtils.isEmpty(etDesignation.getText())) {
                tilDesignation.setError("Designation is required");
                isValid = false;
            } else {
                tilDesignation.setError(null);
            }
        } else if (checkedRadioButtonId == R.id.rbAdmin) {
            // Validate admin fields
            if (TextUtils.isEmpty(etAdminId.getText())) {
                tilAdminId.setError("Admin ID is required");
                isValid = false;
            } else {
                tilAdminId.setError(null);
            }

            if (TextUtils.isEmpty(etPosition.getText())) {
                tilPosition.setError("Position is required");
                isValid = false;
            } else {
                tilPosition.setError(null);
            }
        }

        return isValid;
    }

    private void loadUserData() {
        progressBar.setVisibility(View.VISIBLE);

        userRepository.getUserById(userId, new UserRepository.OnUserLoadedListener() {
            @Override
            public void onUserLoaded(Map<String, Object> userData) {
                progressBar.setVisibility(View.GONE);
                AddEditUserActivity.this.userData = userData;

                // Fill in the form with user data
                String name = (String) userData.get("name");
                String email = (String) userData.get("email");
                String phone = (String) userData.get("phoneNumber");
                String role = (String) userData.get("role");

                etName.setText(name);
                etEmail.setText(email);
                etPhone.setText(phone);

                // Set the appropriate role
                if ("STUDENT".equals(role)) {
                    rbStudent.setChecked(true);
                    // Set student fields
                    String rollNumber = (String) userData.get("rollNumber");
                    String department = (String) userData.get("department");
                    String semester = (String) userData.get("semester");
                    String batch = (String) userData.get("batch");

                    etRollNumber.setText(rollNumber);
                    etDepartment.setText(department);
                    etSemester.setText(semester);
                    etBatch.setText(batch);
                } else if ("INSTRUCTOR".equals(role)) {
                    rbInstructor.setChecked(true);
                    // Set instructor fields
                    String employeeId = (String) userData.get("employeeId");
                    String department = (String) userData.get("department");
                    String designation = (String) userData.get("designation");

                    etEmployeeId.setText(employeeId);
                    etInstructorDepartment.setText(department);
                    etDesignation.setText(designation);
                } else if ("ADMIN".equals(role)) {
                    rbAdmin.setChecked(true);
                    // Set admin fields
                    String adminId = (String) userData.get("adminId");
                    String position = (String) userData.get("position");
                    String privilegeLevel = (String) userData.get("privilegeLevel");

                    etAdminId.setText(adminId);
                    etPosition.setText(position);

                    // Set the privilege level in spinner
                    if (privilegeLevel != null) {
                        int position2 = 0; // Default to Course Admin
                        if (privilegeLevel.equals("DEPARTMENT_ADMIN")) {
                            position2 = 1;
                        } else if (privilegeLevel.equals("SUPER_ADMIN")) {
                            position2 = 2;
                        }

                        if (spinnerPrivilegeLevel.getAdapter().getCount() > position2) {
                            spinnerPrivilegeLevel.setSelection(position2);
                        }
                    }
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditUserActivity.this, "Error", errorMessage);
                finish();
            }
        });
    }

    private void createUser() {
        progressBar.setVisibility(View.VISIBLE);

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Determine user role
        String userRole;
        int checkedRadioButtonId = rgRole.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbStudent) {
            userRole = "STUDENT";
        } else if (checkedRadioButtonId == R.id.rbInstructor) {
            userRole = "INSTRUCTOR";
        } else {
            userRole = "ADMIN";
        }

        // Create user data map
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", etName.getText().toString().trim());
        userData.put("email", email);
        userData.put("phoneNumber", etPhone.getText().toString().trim());
        userData.put("role", userRole);
        userData.put("createdAt", new Date());
        userData.put("isActive", true);

        // Add role-specific data
        if (userRole.equals("STUDENT")) {
            userData.put("rollNumber", etRollNumber.getText().toString().trim());
            userData.put("department", etDepartment.getText().toString().trim());
            userData.put("semester", etSemester.getText().toString().trim());
            userData.put("batch", etBatch.getText().toString().trim());
            userData.put("enrolledCourseIds", new ArrayList<String>());
            userData.put("attendanceRecordIds", new ArrayList<String>());
        } else if (userRole.equals("INSTRUCTOR")) {
            userData.put("employeeId", etEmployeeId.getText().toString().trim());
            userData.put("department", etInstructorDepartment.getText().toString().trim());
            userData.put("designation", etDesignation.getText().toString().trim());
            userData.put("coursesIds", new ArrayList<String>());
            userData.put("generatedQRCodeIds", new ArrayList<String>());
        } else if (userRole.equals("ADMIN")) {
            userData.put("adminId", etAdminId.getText().toString().trim());
            userData.put("position", etPosition.getText().toString().trim());

            // Get selected privilege level
            String privilegeLevel;
            switch (spinnerPrivilegeLevel.getSelectedItemPosition()) {
                case 1:
                    privilegeLevel = "DEPARTMENT_ADMIN";
                    break;
                case 2:
                    privilegeLevel = "SUPER_ADMIN";
                    break;
                default:
                    privilegeLevel = "COURSE_ADMIN";
                    break;
            }
            userData.put("privilegeLevel", privilegeLevel);
        }

        // First create the Firebase Auth account
        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    // Get the new user ID
                    String newUserId = authResult.getUser().getUid();
                    userData.put("userId", newUserId);

                    // Now add to Firestore
                    FirebaseFirestore.getInstance().collection("users").document(newUserId)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(AddEditUserActivity.this,
                                        "User created successfully", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // If Firestore fails, delete the Auth account
                                authResult.getUser().delete();

                                progressBar.setVisibility(View.GONE);
                                UIHelper.showErrorDialog(AddEditUserActivity.this,
                                        "Error", "Failed to create user: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    UIHelper.showErrorDialog(AddEditUserActivity.this,
                            "Error", "Failed to create authentication: " + e.getMessage());
                });
    }

    private void updateUser() {
        progressBar.setVisibility(View.VISIBLE);

        // Create update data map
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("name", etName.getText().toString().trim());
        updateData.put("phoneNumber", etPhone.getText().toString().trim());

        // Determine user role
        String userRole;
        int checkedRadioButtonId = rgRole.getCheckedRadioButtonId();
        if (checkedRadioButtonId == R.id.rbStudent) {
            userRole = "STUDENT";
        } else if (checkedRadioButtonId == R.id.rbInstructor) {
            userRole = "INSTRUCTOR";
        } else {
            userRole = "ADMIN";
        }
        updateData.put("role", userRole);

        // Update role-specific data
        if (userRole.equals("STUDENT")) {
            updateData.put("rollNumber", etRollNumber.getText().toString().trim());
            updateData.put("department", etDepartment.getText().toString().trim());
            updateData.put("semester", etSemester.getText().toString().trim());
            updateData.put("batch", etBatch.getText().toString().trim());
            // Preserve existing lists if they exist
            if (userData != null) {
                if (userData.containsKey("enrolledCourseIds")) {
                    updateData.put("enrolledCourseIds", userData.get("enrolledCourseIds"));
                }
                if (userData.containsKey("attendanceRecordIds")) {
                    updateData.put("attendanceRecordIds", userData.get("attendanceRecordIds"));
                }
            }
        } else if (userRole.equals("INSTRUCTOR")) {
            updateData.put("employeeId", etEmployeeId.getText().toString().trim());
            updateData.put("department", etInstructorDepartment.getText().toString().trim());
            updateData.put("designation", etDesignation.getText().toString().trim());
            // Preserve existing lists if they exist
            if (userData != null) {
                if (userData.containsKey("coursesIds")) {
                    updateData.put("coursesIds", userData.get("coursesIds"));
                }
                if (userData.containsKey("generatedQRCodeIds")) {
                    updateData.put("generatedQRCodeIds", userData.get("generatedQRCodeIds"));
                }
            }
        } else if (userRole.equals("ADMIN")) {
            updateData.put("adminId", etAdminId.getText().toString().trim());
            updateData.put("position", etPosition.getText().toString().trim());

            // Get selected privilege level
            String privilegeLevel;
            switch (spinnerPrivilegeLevel.getSelectedItemPosition()) {
                case 1:
                    privilegeLevel = "DEPARTMENT_ADMIN";
                    break;
                case 2:
                    privilegeLevel = "SUPER_ADMIN";
                    break;
                default:
                    privilegeLevel = "COURSE_ADMIN";
                    break;
            }
            updateData.put("privilegeLevel", privilegeLevel);
        }

        // Update the user in Firestore
        userRepository.updateUser(userId, updateData, new UserRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AddEditUserActivity.this,
                        "User updated successfully", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                UIHelper.showErrorDialog(AddEditUserActivity.this,
                        "Error", "Failed to update user: " + errorMessage);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}