package com.example.qrattendance.data.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SettingsRepository {
    private static final String TAG = "SettingsRepository";
    private static final String SETTINGS_COLLECTION = "settings";
    private static final String SYSTEM_SETTINGS_DOCUMENT = "system_settings";

    private static SettingsRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<Map<String, Object>> settingsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Private constructor for singleton pattern
    private SettingsRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized SettingsRepository getInstance() {
        if (instance == null) {
            instance = new SettingsRepository();
        }
        return instance;
    }

    // Get settings
    public LiveData<Map<String, Object>> getSettings() {
        return settingsLiveData;
    }

    // Get error message
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Get loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Fetch system settings
    public void fetchSystemSettings() {
        isLoading.setValue(true);

        firestore.collection(SETTINGS_COLLECTION).document(SYSTEM_SETTINGS_DOCUMENT)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        Map<String, Object> settings = documentSnapshot.getData();
                        settingsLiveData.setValue(settings);
                    } else {
                        // If settings don't exist, create default settings
                        createDefaultSettings();
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load settings: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Create default settings
    private void createDefaultSettings() {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("instituteName", "Indian Institute of Information Technology, Allahabad");
        defaultSettings.put("offlineMode", true);
        defaultSettings.put("enableNotifications", true);
        defaultSettings.put("autoLockAttendance", false);
        defaultSettings.put("qrCodeExpiryMinutes", 15L);
        defaultSettings.put("minAttendancePercentage", 75);
        defaultSettings.put("createdAt", System.currentTimeMillis());
        defaultSettings.put("updatedAt", System.currentTimeMillis());

        firestore.collection(SETTINGS_COLLECTION).document(SYSTEM_SETTINGS_DOCUMENT)
                .set(defaultSettings)
                .addOnSuccessListener(aVoid -> {
                    settingsLiveData.setValue(defaultSettings);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to create default settings: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Update system settings
    public void updateSystemSettings(Map<String, Object> updatedSettings, OnCompleteListener listener) {
        isLoading.setValue(true);

        // Add timestamp for the update
        updatedSettings.put("updatedAt", System.currentTimeMillis());

        firestore.collection(SETTINGS_COLLECTION).document(SYSTEM_SETTINGS_DOCUMENT)
                .update(updatedSettings)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);

                    // Update the LiveData value with the new settings
                    Map<String, Object> currentSettings = settingsLiveData.getValue();
                    if (currentSettings != null) {
                        currentSettings.putAll(updatedSettings);
                        settingsLiveData.setValue(currentSettings);
                    }

                    listener.onSuccess();
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to update settings: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Get system settings directly (non-LiveData version)
    public void getSystemSettings(OnSettingsLoadedListener listener) {
        isLoading.setValue(true);

        firestore.collection(SETTINGS_COLLECTION).document(SYSTEM_SETTINGS_DOCUMENT)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        Map<String, Object> settings = documentSnapshot.getData();
                        listener.onSettingsLoaded(settings != null ? settings : new HashMap<>());
                    } else {
                        // If settings don't exist, create default settings and return them
                        createDefaultSettingsAndReturn(listener);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load settings: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onError(e.getMessage());
                });
    }

    // Create default settings and return them
    private void createDefaultSettingsAndReturn(OnSettingsLoadedListener listener) {
        Map<String, Object> defaultSettings = new HashMap<>();
        defaultSettings.put("instituteName", "Indian Institute of Information Technology, Allahabad");
        defaultSettings.put("offlineMode", true);
        defaultSettings.put("enableNotifications", true);
        defaultSettings.put("autoLockAttendance", false);
        defaultSettings.put("qrCodeExpiryMinutes", 15L);
        defaultSettings.put("minAttendancePercentage", 75);
        defaultSettings.put("createdAt", System.currentTimeMillis());
        defaultSettings.put("updatedAt", System.currentTimeMillis());

        firestore.collection(SETTINGS_COLLECTION).document(SYSTEM_SETTINGS_DOCUMENT)
                .set(defaultSettings)
                .addOnSuccessListener(aVoid -> {
                    settingsLiveData.setValue(defaultSettings);
                    isLoading.setValue(false);
                    listener.onSettingsLoaded(defaultSettings);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to create default settings: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onError(e.getMessage());
                });
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(String errorMessage);
    }

    public interface OnSettingsLoadedListener {
        void onSettingsLoaded(Map<String, Object> settings);
        void onError(String errorMessage);
    }
}