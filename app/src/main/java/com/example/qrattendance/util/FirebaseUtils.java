package com.example.qrattendance.util;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

public class FirebaseUtils {
    private static final String TAG = "FirebaseUtils";

    public static void testConnection() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("test").document("test")
                .set(new TestData("Hello Firebase!"))
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Firebase connection successful"))
                .addOnFailureListener(e -> Log.e(TAG, "Firebase connection failed", e));
    }

    static class TestData {
        String message;

        TestData(String message) {
            this.message = message;
        }
    }
}