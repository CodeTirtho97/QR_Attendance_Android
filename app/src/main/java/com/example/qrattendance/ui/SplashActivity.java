package com.example.qrattendance.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.qrattendance.MainActivity;
import com.example.qrattendance.R;

/**
 * SplashActivity shows a splash screen with the app logo
 * for a brief period before directing to the main activity.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DISPLAY_LENGTH = 1500; // 1.5 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The SplashTheme with splash_background is applied in the manifest,
        // so no setContentView() is needed.

        // Use a handler to delay loading the MainActivity
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Create an Intent that will start the MainActivity
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(mainIntent);
            finish();
        }, SPLASH_DISPLAY_LENGTH);
    }
}