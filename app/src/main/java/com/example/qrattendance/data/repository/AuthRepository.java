package com.example.qrattendance.data.repository;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.Admin;
import com.example.qrattendance.data.model.Instructor;
import com.example.qrattendance.data.model.ModelUtils;
import com.example.qrattendance.data.model.Student;
import com.example.qrattendance.data.model.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.Map;

/**
 * Repository class that handles all authentication related operations.
 * This class serves as a single source of truth for authentication data and operations.
 */
public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private static final String USERS_COLLECTION = "users";

    private static AuthRepository instance;
    private final FirebaseAuth firebaseAuth;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<String> authError = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);

    // Private constructor for singleton pattern
    private AuthRepository() {
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Check if user is already signed in
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null) {
            fetchUserDetails(user.getUid());
        } else {
            currentUser.setValue(null);
        }
    }

    /**
     * Get the singleton instance of AuthRepository
     * @return The AuthRepository instance
     */
    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            instance = new AuthRepository();
        }
        return instance;
    }

    /**
     * Get the current authenticated user
     * @return LiveData containing the current user or null if not authenticated
     */
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    /**
     * Get authentication errors
     * @return LiveData containing the latest authentication error message
     */
    public LiveData<String> getAuthError() {
        return authError;
    }

    /**
     * Get loading state
     * @return LiveData containing the current loading state
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Register a new user with email and password
     * @param email The user's email address
     * @param password The user's password
     * @param userRole The role of the user (STUDENT, INSTRUCTOR, ADMIN)
     * @param userData Additional user data to store in Firestore
     */
    public void registerUser(String email, String password, User.UserRole userRole, Map<String, Object> userData) {
        isLoading.setValue(true);

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        String userId = task.getResult().getUser().getUid();

                        // Create user object based on role
                        User newUser;
                        if (userRole == User.UserRole.STUDENT) {
                            newUser = new Student();
                        } else if (userRole == User.UserRole.INSTRUCTOR) {
                            newUser = new Instructor();
                        } else {
                            newUser = new Admin();
                        }

                        // Set common user properties
                        newUser.setUserId(userId);
                        newUser.setEmail(email);
                        newUser.setRole(userRole);
                        newUser.setCreatedAt(new Date());
                        newUser.setLastLoginAt(new Date());

                        // Set additional data from userData map
                        if (userData != null) {
                            if (userData.containsKey("name")) {
                                newUser.setName((String) userData.get("name"));
                            }
                            if (userData.containsKey("phoneNumber")) {
                                newUser.setPhoneNumber((String) userData.get("phoneNumber"));
                            }
                            if (userData.containsKey("profileImageUrl")) {
                                newUser.setProfileImageUrl((String) userData.get("profileImageUrl"));
                            }

                            // Set role-specific data
                            if (newUser instanceof Student) {
                                Student student = (Student) newUser;
                                if (userData.containsKey("rollNumber")) {
                                    student.setRollNumber((String) userData.get("rollNumber"));
                                }
                                if (userData.containsKey("department")) {
                                    student.setDepartment((String) userData.get("department"));
                                }
                                if (userData.containsKey("semester")) {
                                    student.setSemester((String) userData.get("semester"));
                                }
                                if (userData.containsKey("batch")) {
                                    student.setBatch((String) userData.get("batch"));
                                }
                            } else if (newUser instanceof Instructor) {
                                Instructor instructor = (Instructor) newUser;
                                if (userData.containsKey("employeeId")) {
                                    instructor.setEmployeeId((String) userData.get("employeeId"));
                                }
                                if (userData.containsKey("department")) {
                                    instructor.setDepartment((String) userData.get("department"));
                                }
                                if (userData.containsKey("designation")) {
                                    instructor.setDesignation((String) userData.get("designation"));
                                }
                            } else if (newUser instanceof Admin) {
                                Admin admin = (Admin) newUser;
                                if (userData.containsKey("adminId")) {
                                    admin.setAdminId((String) userData.get("adminId"));
                                }
                                if (userData.containsKey("position")) {
                                    admin.setPosition((String) userData.get("position"));
                                }
                                if (userData.containsKey("privilegeLevel")) {
                                    String level = (String) userData.get("privilegeLevel");
                                    if (level != null) {
                                        admin.setPrivilegeLevel(Admin.AdminPrivilegeLevel.valueOf(level));
                                    }
                                }
                            }
                        }

                        // Save user data to Firestore
                        saveUserToFirestore(newUser);
                    } else {
                        isLoading.setValue(false);
                        if (task.getException() != null) {
                            authError.setValue(task.getException().getMessage());
                            Log.e(TAG, "Registration failed", task.getException());
                        } else {
                            authError.setValue("Registration failed. Please try again.");
                            Log.e(TAG, "Registration failed with no exception");
                        }
                    }
                });
    }

    /**
     * Save user data to Firestore
     * @param user The user object to save
     */
    private void saveUserToFirestore(User user) {
        Map<String, Object> userMap = ModelUtils.userToMap(user);

        firestore.collection(USERS_COLLECTION).document(user.getUserId())
                .set(userMap)
                .addOnSuccessListener(aVoid -> {
                    isLoading.setValue(false);
                    currentUser.setValue(user);
                    Log.d(TAG, "User data saved to Firestore");
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    authError.setValue("Failed to save user data: " + e.getMessage());
                    Log.e(TAG, "Error saving user data", e);

                    // Delete the Firebase Auth user if Firestore save fails
                    FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        firebaseUser.delete();
                    }
                });
    }

    /**
     * Login with email and password
     * @param email The user's email address
     * @param password The user's password
     */
    public void login(String email, String password) {
        isLoading.setValue(true);

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                        // Update last login time
                        String userId = task.getResult().getUser().getUid();
                        DocumentReference userRef = firestore.collection(USERS_COLLECTION).document(userId);
                        userRef.update("lastLoginAt", new Date());

                        // Fetch user details
                        fetchUserDetails(userId);
                    } else {
                        isLoading.setValue(false);
                        if (task.getException() != null) {
                            authError.setValue(task.getException().getMessage());
                            Log.e(TAG, "Login failed", task.getException());
                        } else {
                            authError.setValue("Login failed. Please check your credentials.");
                            Log.e(TAG, "Login failed with no exception");
                        }
                    }
                });
    }

    /**
     * Fetch user details from Firestore
     * @param userId The user ID to fetch details for
     */
    private void fetchUserDetails(String userId) {
        firestore.collection(USERS_COLLECTION).document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    isLoading.setValue(false);
                    if (documentSnapshot.exists()) {
                        User user = ModelUtils.documentToUser(documentSnapshot);
                        currentUser.setValue(user);
                        Log.d(TAG, "User details fetched successfully");
                    } else {
                        authError.setValue("User data not found. Please contact support.");
                        Log.e(TAG, "User document does not exist for ID: " + userId);
                        logout(); // Logout if user data doesn't exist
                    }
                })
                .addOnFailureListener(e -> {
                    isLoading.setValue(false);
                    authError.setValue("Failed to fetch user data: " + e.getMessage());
                    Log.e(TAG, "Error fetching user data", e);
                });
    }

    /**
     * Send password reset email
     * @param email The email address to send the reset link to
     * @return Task that will complete when the email is sent
     */
    public Task<Void> sendPasswordResetEmail(String email) {
        isLoading.setValue(true);
        return firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> isLoading.setValue(false));
    }

    /**
     * Logout the current user
     */
    public void logout() {
        firebaseAuth.signOut();
        currentUser.setValue(null);
    }

    /**
     * Check if a user exists with the given email
     * @param email The email to check
     * @param onSuccess Callback for success
     * @param onFailure Callback for failure
     */
    public void checkIfUserExists(String email, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        firebaseAuth.fetchSignInMethodsForEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        boolean isNewUser = task.getResult().getSignInMethods() == null ||
                                task.getResult().getSignInMethods().isEmpty();
                        onSuccess.onSuccess(!isNewUser);
                    } else {
                        if (onFailure != null && task.getException() != null) {
                            onFailure.onFailure(task.getException());
                        }
                    }
                });
    }
}