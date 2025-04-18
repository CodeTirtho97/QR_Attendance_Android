package com.example.qrattendance.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.model.User;
import com.google.gson.Gson;

public class SessionManager {
    // Constants
    private static final String PREF_NAME = "QRAttendanceSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_DATA = "userData";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_USER_ID = "userId";
    private static final int PRIVATE_MODE = 0;

    // Singleton instance
    private static SessionManager instance;

    // SharedPreferences
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context context;
    private final Gson gson;

    // Private constructor
    private SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    // Get singleton instance
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    // Save user session
    public void saveUserSession(User user) {
        if (user == null) return;

        String userJson = gson.toJson(user);

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_DATA, userJson);
        editor.putString(KEY_USER_ROLE, user.getRole().name());
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.apply();
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Get user ID
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    // Get user role
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    // Get user data
    public User getUserData() {
        String userJson = sharedPreferences.getString(KEY_USER_DATA, null);
        if (userJson == null) return null;

        String role = getUserRole();
        if (role == null) return null;

        try {
            if (role.equals(User.UserRole.STUDENT.name())) {
                return gson.fromJson(userJson, Student.class);
            } else if (role.equals(User.UserRole.INSTRUCTOR.name())) {
                return gson.fromJson(userJson, Instructor.class);
            } else if (role.equals(User.UserRole.ADMIN.name())) {
                return gson.fromJson(userJson, Admin.class);
            }
        } catch (Exception e) {
            clearSession();
        }

        return null;
    }

    // Clear user session (logout)
    public void clearSession() {
        editor.clear();
        editor.apply();
    }
}