package com.example.qrattendance.ui.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;

import com.example.qrattendance.R;
import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.repository.SettingsRepository;
import com.example.qrattendance.util.SessionManager;
import com.example.qrattendance.util.UIHelper;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.HashMap;
import java.util.Map;

public class SystemSettingsActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private Button btnSaveSettings;

    // Settings UI components
    private SwitchCompat switchOfflineMode;
    private SwitchCompat switchNotifications;
    private SwitchCompat switchAutoLockAttendance;
    private Slider sliderQRCodeExpiry;
    private TextView tvQRCodeExpiryValue;
    private TextInputLayout tilInstituteName;
    private TextInputEditText etInstituteName;
    private TextInputLayout tilMinAttendancePercentage;
    private TextInputEditText etMinAttendancePercentage;

    private SettingsRepository settingsRepository;
    private Admin currentAdmin;
    private Map<String, Object> currentSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_system_settings);

        // Initialize repository
        settingsRepository = SettingsRepository.getInstance();

        // Get current admin from session
        currentAdmin = (Admin) SessionManager.getInstance(this).getUserData();
        if (currentAdmin == null) {
            UIHelper.showErrorDialog(this, "Session Error", "Please log in again.");
            finish();
            return;
        }

        // Check admin privileges
        if (!currentAdmin.hasPrivilege(Admin.AdminPrivilegeLevel.DEPARTMENT_ADMIN)) {
            UIHelper.showErrorDialog(this, "Permission Denied",
                    "You need Department Admin or higher privileges to modify system settings.");
            finish();
            return;
        }

        // Initialize views
        initViews();
        setupEventListeners();
        observeSettingsData();
        loadSettings();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        progressBar = findViewById(R.id.progressBar);
        btnSaveSettings = findViewById(R.id.btnSaveSettings);

        // Settings components
        switchOfflineMode = findViewById(R.id.switchOfflineMode);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchAutoLockAttendance = findViewById(R.id.switchAutoLockAttendance);
        sliderQRCodeExpiry = findViewById(R.id.sliderQRCodeExpiry);
        tvQRCodeExpiryValue = findViewById(R.id.tvQRCodeExpiryValue);
        tilInstituteName = findViewById(R.id.tilInstituteName);
        etInstituteName = findViewById(R.id.etInstituteName);
        tilMinAttendancePercentage = findViewById(R.id.tilMinAttendancePercentage);
        etMinAttendancePercentage = findViewById(R.id.etMinAttendancePercentage);

        // Set up toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("System Settings");
        }

        // Disable fields for Course Admin
        if (currentAdmin.getPrivilegeLevel() == Admin.AdminPrivilegeLevel.COURSE_ADMIN) {
            disableSettingFields();
            btnSaveSettings.setEnabled(false);
            Toast.makeText(this, "Course Admins can only view settings", Toast.LENGTH_LONG).show();
        }
    }

    private void setupEventListeners() {
        // QR code slider listener
        sliderQRCodeExpiry.addOnChangeListener((slider, value, fromUser) -> {
            int minutes = (int) value;
            tvQRCodeExpiryValue.setText(minutes + " minutes");
        });

        // Setup save button
        btnSaveSettings.setOnClickListener(v -> {
            if (validateInputs()) {
                saveSettings();
            }
        });
    }

    private void observeSettingsData() {
        // Observe loading state
        settingsRepository.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });

        // Observe error messages
        settingsRepository.getErrorMessage().observe(this, errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                UIHelper.showErrorDialog(this, "Error", errorMsg);
            }
        });

        // Observe settings data
        settingsRepository.getSettings().observe(this, settings -> {
            if (settings != null) {
                currentSettings = settings;
                populateSettings(settings);
            }
        });
    }

    private void loadSettings() {
        settingsRepository.fetchSystemSettings();
    }

    private void populateSettings(Map<String, Object> settings) {
        // Populate UI with settings
        if (settings.containsKey("offlineMode")) {
            switchOfflineMode.setChecked((Boolean) settings.get("offlineMode"));
        }

        if (settings.containsKey("enableNotifications")) {
            switchNotifications.setChecked((Boolean) settings.get("enableNotifications"));
        }

        if (settings.containsKey("autoLockAttendance")) {
            switchAutoLockAttendance.setChecked((Boolean) settings.get("autoLockAttendance"));
        }

        if (settings.containsKey("qrCodeExpiryMinutes")) {
            Long expiryMinutes = (Long) settings.get("qrCodeExpiryMinutes");
            sliderQRCodeExpiry.setValue(expiryMinutes != null ? expiryMinutes.floatValue() : 15f);
            tvQRCodeExpiryValue.setText(expiryMinutes != null ? expiryMinutes.intValue() + " minutes" : "15 minutes");
        }

        if (settings.containsKey("instituteName")) {
            etInstituteName.setText((String) settings.get("instituteName"));
        }

        if (settings.containsKey("minAttendancePercentage")) {
            Object percentage = settings.get("minAttendancePercentage");
            if (percentage instanceof Long) {
                etMinAttendancePercentage.setText(String.valueOf(((Long) percentage).intValue()));
            } else if (percentage instanceof Integer) {
                etMinAttendancePercentage.setText(String.valueOf(percentage));
            } else if (percentage instanceof Double) {
                etMinAttendancePercentage.setText(String.valueOf(((Double) percentage).intValue()));
            }
        }
    }

    private boolean validateInputs() {
        boolean isValid = true;

        // Validate institute name
        if (etInstituteName.getText().toString().trim().isEmpty()) {
            tilInstituteName.setError("Institute name is required");
            isValid = false;
        } else {
            tilInstituteName.setError(null);
        }

        // Validate minimum attendance percentage
        String minAttendanceStr = etMinAttendancePercentage.getText().toString().trim();
        if (minAttendanceStr.isEmpty()) {
            tilMinAttendancePercentage.setError("Minimum attendance percentage is required");
            isValid = false;
        } else {
            try {
                int percentage = Integer.parseInt(minAttendanceStr);
                if (percentage < 0 || percentage > 100) {
                    tilMinAttendancePercentage.setError("Value must be between 0 and 100");
                    isValid = false;
                } else {
                    tilMinAttendancePercentage.setError(null);
                }
            } catch (NumberFormatException e) {
                tilMinAttendancePercentage.setError("Please enter a valid number");
                isValid = false;
            }
        }

        return isValid;
    }

    private void saveSettings() {
        Map<String, Object> updatedSettings = new HashMap<>();
        updatedSettings.put("offlineMode", switchOfflineMode.isChecked());
        updatedSettings.put("enableNotifications", switchNotifications.isChecked());
        updatedSettings.put("autoLockAttendance", switchAutoLockAttendance.isChecked());
        updatedSettings.put("qrCodeExpiryMinutes", (long) sliderQRCodeExpiry.getValue());
        updatedSettings.put("instituteName", etInstituteName.getText().toString().trim());
        updatedSettings.put("minAttendancePercentage",
                Integer.parseInt(etMinAttendancePercentage.getText().toString().trim()));
        updatedSettings.put("lastUpdatedBy", currentAdmin.getUserId());
        updatedSettings.put("lastUpdateTimestamp", System.currentTimeMillis());

        settingsRepository.updateSystemSettings(updatedSettings, new SettingsRepository.OnCompleteListener() {
            @Override
            public void onSuccess() {
                Toast.makeText(SystemSettingsActivity.this,
                        "Settings updated successfully", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(String errorMessage) {
                UIHelper.showErrorDialog(SystemSettingsActivity.this,
                        "Error", "Failed to update settings: " + errorMessage);
            }
        });
    }

    private void disableSettingFields() {
        // Disable all interactive components for view-only mode
        switchOfflineMode.setEnabled(false);
        switchNotifications.setEnabled(false);
        switchAutoLockAttendance.setEnabled(false);
        sliderQRCodeExpiry.setEnabled(false);
        etInstituteName.setEnabled(false);
        etMinAttendancePercentage.setEnabled(false);
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