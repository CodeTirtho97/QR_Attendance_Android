package com.example.qrattendance;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.model.User;
import com.example.qrattendance.data.repository.AuthRepository;
import com.example.qrattendance.ui.admin.AdminDashboardActivity;
import com.example.qrattendance.ui.auth.LoginActivity;
import com.example.qrattendance.ui.instructor.InstructorDashboardActivity;
import com.example.qrattendance.ui.student.StudentDashboardActivity;
import com.example.qrattendance.util.SessionManager;
import com.google.firebase.FirebaseApp;

/**
 * MainActivity serves as the entry point of the application.
 * It handles Firebase initialization and redirects users to the appropriate screen
 * based on their authentication status and role.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private AuthRepository authRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize authentication repository
        authRepository = AuthRepository.getInstance();

        // Check if user is logged in
        if (SessionManager.getInstance(this).isLoggedIn()) {
            // User is logged in, redirect to appropriate dashboard
            redirectBasedOnUserRole();
        } else {
            // User is not logged in, redirect to login screen
            redirectToLogin();
        }
    }

    /**
     * Redirect user to the appropriate dashboard based on their role
     */
    private void redirectBasedOnUserRole() {
        User user = SessionManager.getInstance(this).getUserData();
        if (user == null) {
            // No user data in session, redirect to login
            Log.e(TAG, "User is logged in but no user data found in session");
            redirectToLogin();
            return;
        }

        Intent intent;

        // Check user type and redirect accordingly
        if (user instanceof Student) {
            intent = new Intent(this, StudentDashboardActivity.class);
        } else if (user instanceof Instructor) {
            intent = new Intent(this, InstructorDashboardActivity.class);
        } else if (user instanceof Admin) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else {
            // Unknown user type, redirect to login
            Log.e(TAG, "Unknown user type: " + user.getClass().getSimpleName());
            redirectToLogin();
            return;
        }

        startActivity(intent);
        finish();
    }

    /**
     * Redirect user to the login screen
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}