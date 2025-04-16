package com.example.qrattendance.util;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.qrattendance.data.model.User;
import com.google.gson.Gson;

/**
 * SessionManager handles user session management using SharedPreferences.
 * This includes saving, retrieving, and clearing user session data.
 */
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

    // SharedPreferences and Editor
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;
    private final Context context;

    // Gson for serializing/deserializing user data
    private final Gson gson;

    // Private constructor
    private SessionManager(Context context) {
        this.context = context;
        sharedPreferences = context.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = sharedPreferences.edit();
        gson = new Gson();
    }

    /**
     * Get singleton instance of SessionManager
     * @param context Application context
     * @return SessionManager instance
     */
    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Save user session data
     * @param user The user to save
     */
    public void saveUserSession(User user) {
        if (user == null) return;

        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_USER_DATA, gson.toJson(user));
        editor.putString(KEY_USER_ROLE, user.getRole().name());
        editor.putString(KEY_USER_ID, user.getUserId());
        editor.apply();
    }

    /**
     * Check if user is logged in
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Get user ID from session
     * @return User ID or null if not logged in
     */
    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, null);
    }

    /**
     * Get user role from session
     * @return User role as string or null if not logged in
     */
    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, null);
    }

    /**
     * Get user data from session
     * @return User object or null if not logged in or deserialization fails
     */
    public User getUserData() {
        String userData = sharedPreferences.getString(KEY_USER_DATA, null);
        if (userData == null) return null;

        try {
            return gson.fromJson(userData, User.class);
        } catch (Exception e) {
            clearSession();
            return null;
        }
    }

    /**
     * Clear user session data (logout)
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /**
     * Update specific user data in session
     * @param user Updated user data
     */
    public void updateUserData(User user) {
        if (user == null || !isLoggedIn()) return;

        editor.putString(KEY_USER_DATA, gson.toJson(user));
        editor.apply();
    }
}