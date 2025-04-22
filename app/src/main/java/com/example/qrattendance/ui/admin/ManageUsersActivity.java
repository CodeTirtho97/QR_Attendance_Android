package com.example.qrattendance.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.User;
import com.example.qrattendance.data.repository.UserRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity implements UserAdapter.UserActionListener {

    private Toolbar toolbar;
    private Spinner spinnerUserType;
    private RecyclerView recyclerViewUsers;
    private TextView tvNoUsers;
    private ProgressBar progressBar;
    private FloatingActionButton fabAddUser;

    private UserRepository userRepository;
    private Admin currentAdmin;
    private UserAdapter userAdapter;
    private String currentUserRole = "ALL"; // Default filter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_users);

        // Initialize repository
        userRepository = UserRepository.getInstance();

        // Get current admin from session
        currentAdmin = (Admin) SessionManager.getInstance(this).getUserData();
        if (currentAdmin == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Initialize views
        initViews();
        setupRecyclerView();
        setupUserTypeSpinner();
        loadUsers();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        spinnerUserType = findViewById(R.id.spinnerUserType);
        recyclerViewUsers = findViewById(R.id.recyclerViewUsers);
        tvNoUsers = findViewById(R.id.tvNoUsers);
        progressBar = findViewById(R.id.progressBar);
        fabAddUser = findViewById(R.id.fabAddUser);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Users");
        }

        // Set FAB click listener
        fabAddUser.setOnClickListener(v -> {
            // Check admin privileges
            if (currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
                navigateToAddUserActivity();
            } else {
                UIHelper.showErrorDialog(this, "Permission Denied",
                        "You need Department Admin or higher privileges to add users.");
            }
        });
    }

    private void setupRecyclerView() {
        userAdapter = new UserAdapter(new ArrayList<>(), this);
        recyclerViewUsers.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewUsers.setAdapter(userAdapter);
    }

    private void setupUserTypeSpinner() {
        // Create user type options
        String[] userTypes = {"All Users", "Students", "Instructors", "Admins"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, userTypes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerUserType.setAdapter(adapter);

        // Set selection listener
        spinnerUserType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Set the filter based on selection
                switch (position) {
                    case 0:
                        currentUserRole = "ALL";
                        break;
                    case 1:
                        currentUserRole = "STUDENT";
                        break;
                    case 2:
                        currentUserRole = "INSTRUCTOR";
                        break;
                    case 3:
                        currentUserRole = "ADMIN";
                        break;
                }
                // Reload users with the new filter
                loadUsers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Use default
            }
        });
    }

    private void loadUsers() {
        progressBar.setVisibility(View.VISIBLE);
        tvNoUsers.setVisibility(View.GONE);
        recyclerViewUsers.setVisibility(View.GONE);

        // Fetch users based on the selected filter
        userRepository.fetchUsers(currentUserRole, new UserRepository.OnUsersLoadedListener() {
            @Override
            public void onUsersLoaded(List<Map<String, Object>> users) {
                progressBar.setVisibility(View.GONE);

                if (users.isEmpty()) {
                    tvNoUsers.setVisibility(View.VISIBLE);
                    recyclerViewUsers.setVisibility(View.GONE);
                } else {
                    tvNoUsers.setVisibility(View.GONE);
                    recyclerViewUsers.setVisibility(View.VISIBLE);
                    userAdapter.updateUsers(users);
                }
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                tvNoUsers.setText("Error: " + errorMessage);
                tvNoUsers.setVisibility(View.VISIBLE);
                recyclerViewUsers.setVisibility(View.GONE);

                UIHelper.showErrorDialog(ManageUsersActivity.this,
                        "Error Loading Users", errorMessage);
            }
        });
    }

    private void navigateToAddUserActivity() {
        Intent intent = new Intent(this, AddEditUserActivity.class);
        startActivity(intent);
    }

    @Override
    public void onEditUser(Map<String, Object> user) {
        if (!currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
            UIHelper.showErrorDialog(this, "Permission Denied",
                    "You need Department Admin or higher privileges to edit users.");
            return;
        }

        // Check if trying to edit a higher privilege admin
        String userRole = (String) user.get("role");
        if ("ADMIN".equals(userRole)) {
            String privilegeLevel = (String) user.get("privilegeLevel");
            if (privilegeLevel != null) {
                Admin.AdminPrivilegeLevel userLevel = Admin.AdminPrivilegeLevel.valueOf(privilegeLevel);
                if (currentAdmin.getPrivilegeLevel().ordinal() > userLevel.ordinal()) {
                    UIHelper.showErrorDialog(this, "Permission Denied",
                            "You cannot edit an admin with higher privileges than yours.");
                    return;
                }
            }
        }

        Intent intent = new Intent(this, AddEditUserActivity.class);
        intent.putExtra("userId", (String) user.get("userId"));
        startActivity(intent);
    }

    @Override
    public void onToggleUserActiveStatus(Map<String, Object> user) {
        if (!currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
            UIHelper.showErrorDialog(this, "Permission Denied",
                    "You need Department Admin or higher privileges to change user status.");
            return;
        }

        // Check if trying to modify a higher privilege admin
        String userRole = (String) user.get("role");
        if ("ADMIN".equals(userRole)) {
            String privilegeLevel = (String) user.get("privilegeLevel");
            if (privilegeLevel != null) {
                Admin.AdminPrivilegeLevel userLevel = Admin.AdminPrivilegeLevel.valueOf(privilegeLevel);
                if (currentAdmin.getPrivilegeLevel().ordinal() > userLevel.ordinal()) {
                    UIHelper.showErrorDialog(this, "Permission Denied",
                            "You cannot modify an admin with higher privileges than yours.");
                    return;
                }
            }
        }

        // Get current status and user ID
        String userId = (String) user.get("userId");
        boolean isActive = user.containsKey("isActive") ? (boolean) user.get("isActive") : true;

        // Create confirmation dialog
        String title = isActive ? "Deactivate User" : "Activate User";
        String message = isActive ?
                "Are you sure you want to deactivate this user? They will no longer be able to access the system." :
                "Are you sure you want to reactivate this user? They will regain access to the system.";

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Yes", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("isActive", !isActive);

                    userRepository.updateUser(userId, updateData, new UserRepository.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(ManageUsersActivity.this,
                                    "User status updated successfully", Toast.LENGTH_SHORT).show();

                            // Refresh the list
                            loadUsers();
                        }

                        @Override
                        public void onError(String errorMessage) {
                            progressBar.setVisibility(View.GONE);
                            UIHelper.showErrorDialog(ManageUsersActivity.this,
                                    "Error", "Failed to update user status: " + errorMessage);
                        }
                    });
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onViewUserDetails(Map<String, Object> user) {
        // Create and show user details dialog
        String userId = (String) user.get("userId");
        String name = (String) user.get("name");
        String email = (String) user.get("email");
        String role = (String) user.get("role");

        StringBuilder details = new StringBuilder();
        details.append("Name: ").append(name).append("\n");
        details.append("Email: ").append(email).append("\n");
        details.append("Role: ").append(role).append("\n");

        // Add role-specific details
        if ("STUDENT".equals(role)) {
            details.append("Roll Number: ").append(user.get("rollNumber")).append("\n");
            details.append("Department: ").append(user.get("department")).append("\n");
            details.append("Semester: ").append(user.get("semester")).append("\n");
            details.append("Batch: ").append(user.get("batch")).append("\n");
        } else if ("INSTRUCTOR".equals(role)) {
            details.append("Employee ID: ").append(user.get("employeeId")).append("\n");
            details.append("Department: ").append(user.get("department")).append("\n");
            details.append("Designation: ").append(user.get("designation")).append("\n");
        } else if ("ADMIN".equals(role)) {
            details.append("Admin ID: ").append(user.get("adminId")).append("\n");
            details.append("Position: ").append(user.get("position")).append("\n");
            details.append("Privilege Level: ").append(user.get("privilegeLevel")).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("User Details")
                .setMessage(details.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning to this activity
        loadUsers();
    }
}