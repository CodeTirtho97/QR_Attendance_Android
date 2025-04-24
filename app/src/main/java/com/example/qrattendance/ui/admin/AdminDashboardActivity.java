package com.example.qrattendance.ui.admin;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.common.ProfileActivity;
import com.example.qrattendance.util.SessionManager;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvWelcome;
    private TextView tvAdminId;
    private TextView tvAdminType;
    private SessionManager sessionManager;
    private AuthRepository authRepository;

    // Dashboard cards
    private CardView cardManageUsers;
    private CardView cardManageCourses;
    private CardView cardSystemSettings;
    private CardView cardReports;
    private CardView cardLogs;
    private CardView cardBackup;

    private Admin currentAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Initialize session manager and auth repository
        sessionManager = SessionManager.getInstance(this);
        authRepository = AuthRepository.getInstance();

        // Get current admin
        currentAdmin = (Admin) sessionManager.getUserData();
        if (currentAdmin == null) {
            Toast.makeText(this, "Session error. Please login again.", Toast.LENGTH_SHORT).show();
            logout();
            return;
        }

        // Initialize UI components
        initViews();
        setupUserInfo();
        setupDashboardAccess();
    }

    private void initViews() {
        tvWelcome = findViewById(R.id.tvWelcomeAdmin);
        tvAdminId = findViewById(R.id.tvAdminId);
        tvAdminType = findViewById(R.id.tvAdminType);

        // Find all dashboard cards
        cardManageUsers = findViewById(R.id.cardManageUsers);
        cardManageCourses = findViewById(R.id.cardManageCourses);
        cardSystemSettings = findViewById(R.id.cardSystemSettings);
        cardReports = findViewById(R.id.cardReports);
        cardLogs = findViewById(R.id.cardLogs);
        cardBackup = findViewById(R.id.cardBackup);

        // Set click listeners
        cardManageUsers.setOnClickListener(v -> manageUsers());
        cardManageCourses.setOnClickListener(v -> manageCourses());
        cardSystemSettings.setOnClickListener(v -> systemSettings());
        cardReports.setOnClickListener(v -> viewReports());
        cardLogs.setOnClickListener(v -> viewLogs());
        cardBackup.setOnClickListener(v -> backupRestore());

        // Set up toolbar
        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Admin Dashboard");
        }
    }

    private void setupUserInfo() {
        if (currentAdmin != null) {
            String welcomeMessage = "Welcome, " + currentAdmin.getName() + "!";
            tvWelcome.setText(welcomeMessage);

            String adminIdText = "ID: " + (currentAdmin.getAdminId() != null ?
                    currentAdmin.getAdminId() : "Unknown");
            tvAdminId.setText(adminIdText);

            // Display admin privilege level
            Admin.AdminPrivilegeLevel privilegeLevel = currentAdmin.getPrivilegeLevel();
            String adminTypeText = "";

            switch (privilegeLevel) {
                case SUPER_ADMIN:
                    adminTypeText = "Super Admin";
                    break;
                case DEPARTMENT_ADMIN:
                    adminTypeText = "Department Admin";
                    break;
                case COURSE_ADMIN:
                    adminTypeText = "Course Admin";
                    break;
            }

            tvAdminType.setText(adminTypeText);
        }
    }

    private void setupDashboardAccess() {
        if (currentAdmin == null) return;

        Admin.AdminPrivilegeLevel privilegeLevel = currentAdmin.getPrivilegeLevel();

        // All admin types can access Manage Users
        cardManageUsers.setVisibility(View.VISIBLE);

        // All admin types can access Manage Courses
        cardManageCourses.setVisibility(View.VISIBLE);

        // System Settings - Only Department Admin and Super Admin
        if (privilegeLevel == Admin.AdminPrivilegeLevel.SUPER_ADMIN ||
                privilegeLevel == Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN) {
            cardSystemSettings.setVisibility(View.VISIBLE);
        } else {
            cardSystemSettings.setVisibility(View.GONE);
        }

        // Reports - All admin types
        cardReports.setVisibility(View.VISIBLE);

        // Logs - Only Super Admin
        if (privilegeLevel == Admin.AdminPrivilegeLevel.SUPER_ADMIN) {
            cardLogs.setVisibility(View.VISIBLE);
        } else {
            cardLogs.setVisibility(View.GONE);
        }

        // Backup & Restore - Only Super Admin
        if (privilegeLevel == Admin.AdminPrivilegeLevel.SUPER_ADMIN) {
            cardBackup.setVisibility(View.VISIBLE);
        } else {
            cardBackup.setVisibility(View.GONE);
        }
    }

    private void manageUsers() {
        // Navigate to ManageUsersActivity
        Intent intent = new Intent(this, ManageUsersActivity.class);
        startActivity(intent);
    }

    private void manageCourses() {
        // Navigate to CourseManagementActivity
        Intent intent = new Intent(this, CourseManagementActivity.class);
        startActivity(intent);
    }

    private void systemSettings() {
        if (currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
            // Navigate to SystemSettingsActivity
            Intent intent = new Intent(this, SystemSettingsActivity.class);
            startActivity(intent);
        } else {
            Toast.makeText(this, "You need Department Admin or higher privileges to access system settings",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void viewReports() {
        // TODO: Open Reports activity
        Toast.makeText(this, "Reports feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void viewLogs() {
        if (currentAdmin.getPrivilegeLevel() == Admin.AdminPrivilegeLevel.SUPER_ADMIN) {
            // TODO: Open Logs activity
            Toast.makeText(this, "Logs feature coming soon", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "You need Super Admin privileges to access system logs",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void backupRestore() {
        if (currentAdmin.getPrivilegeLevel() == Admin.AdminPrivilegeLevel.SUPER_ADMIN) {
            // TODO: Open Backup & Restore activity
            Toast.makeText(this, "Backup & Restore feature coming soon", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "You need Super Admin privileges to access backup and restore",
                    Toast.LENGTH_SHORT).show();
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
        if (item.getItemId() == R.id.action_profile) {
            // Open ProfileActivity
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_logout) {
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