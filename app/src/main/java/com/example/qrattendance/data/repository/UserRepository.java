package com.example.qrattendance.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static final String USERS_COLLECTION = "users";

    private static UserRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Private constructor for singleton pattern
    private UserRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    // Get loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Get error message
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Fetch all users or filter by role
     *
     * @param userRole "ALL", "STUDENT", "INSTRUCTOR", or "ADMIN"
     * @param listener callback for results
     */
    public void fetchUsers(String userRole, OnUsersLoadedListener listener) {
        isLoading.setValue(true);

        Query query = firestore.collection(USERS_COLLECTION);

        // Apply role filter if specified
        if (userRole != null && !userRole.equals("ALL")) {
            query = query.whereEqualTo("role", userRole);
        }

        query.get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Map<String, Object>> users = new ArrayList<>();

                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        Map<String, Object> userData = document.getData();
                        userData.put("userId", document.getId());
                        users.add(userData);
                    }

                    isLoading.setValue(false);
                    if (listener != null) {
                        listener.onUsersLoaded(users);
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Failed to load users: " + e.getMessage();
                    Log.e(TAG, errorMsg);
                    errorMessage.setValue(errorMsg);
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
    }

    /**
     * Get a user by ID
     *
     * @param userId   User ID
     * @param listener callback for result
     */
    public void getUserById(String userId, OnUserLoadedListener listener) {
        isLoading.setValue(true);

        firestore.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);

                    if (documentSnapshot.exists()) {
                        Map<String, Object> userData = documentSnapshot.getData();
                        if (userData != null) {
                            userData.put("userId", documentSnapshot.getId());

                            if (listener != null) {
                                listener.onUserLoaded(userData);
                            }
                        } else {
                            String errorMsg = "User data is null";
                            errorMessage.setValue(errorMsg);

                            if (listener != null) {
                                listener.onError(errorMsg);
                            }
                        }
                    } else {
                        String errorMsg = "User not found";
                        errorMessage.setValue(errorMsg);

                        if (listener != null) {
                            listener.onError(errorMsg);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Failed to get user: " + e.getMessage();
                    Log.e(TAG, errorMsg);
                    errorMessage.setValue(errorMsg);
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
    }

    /**
     * Update user data
     *
     * @param userId     User ID
     * @param updateData Map of fields to update
     * @param listener   callback for result
     */
    public void updateUser(String userId, Map<String, Object> updateData, OnCompleteListener listener) {
        isLoading.setValue(true);

        firestore.collection(USERS_COLLECTION).document(userId)
                .update(updateData)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Failed to update user: " + e.getMessage();
                    Log.e(TAG, errorMsg);
                    errorMessage.setValue(errorMsg);
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
    }

    /**
     * Add a new user document directly (for admin creation without authentication)
     *
     * @param userData Map of user data
     * @param listener callback for result
     */
    public void addUser(Map<String, Object> userData, OnAddUserListener listener) {
        isLoading.setValue(true);

        firestore.collection(USERS_COLLECTION)
                .add(userData)
                .addOnSuccessListener(documentReference -> {
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onSuccess(documentReference.getId());
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Failed to add user: " + e.getMessage();
                    Log.e(TAG, errorMsg);
                    errorMessage.setValue(errorMsg);
                    isLoading.setValue(false);

                    if (listener != null) {
                        listener.onError(errorMsg);
                    }
                });
    }

    // Listener interfaces
    public interface OnUsersLoadedListener {
        void onUsersLoaded(List<Map<String, Object>> users);

        void onError(String errorMessage);
    }

    public interface OnUserLoadedListener {
        void onUserLoaded(Map<String, Object> userData);

        void onError(String errorMessage);
    }

    public interface OnCompleteListener {
        void onSuccess();

        void onError(String errorMessage);
    }

    public interface OnAddUserListener {
        void onSuccess(String userId);

        void onError(String errorMessage);
    }
}