package com.example.qrattendance.data.repository;

import android.util.Log;
import androidx.annotation.NonNull;

import com.example.qrattendance.data.model.Course;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enhanced course repository with complete data deletion functionality.
 * This extends the core functionality of CourseRepository with methods
 * to completely delete a course and all its related data.
 */
public class EnhancedCourseRepository {
    private static final String TAG = "EnhancedCourseRepo";
    private static EnhancedCourseRepository instance;

    // Collections in Firestore
    private static final String COURSES_COLLECTION = "courses";
    private static final String SESSIONS_COLLECTION = "sessions";
    private static final String ATTENDANCE_RECORDS_COLLECTION = "attendance_records";
    private static final String QR_CODES_COLLECTION = "qr_codes";
    private static final String USERS_COLLECTION = "users";

    private final FirebaseFirestore firestore;

    // Private constructor for singleton pattern
    private EnhancedCourseRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized EnhancedCourseRepository getInstance() {
        if (instance == null) {
            instance = new EnhancedCourseRepository();
        }
        return instance;
    }

    /**
     * Completely deletes a course and all related data including:
     * - All sessions associated with the course
     * - All QR codes used for those sessions
     * - All attendance records for those sessions
     * - References to the course in student and instructor records
     *
     * @param courseId ID of the course to delete
     * @param listener Callback to notify of completion or failure
     */
    public void deleteCourseWithAllData(String courseId, OnCompleteListener listener) {
        Log.d(TAG, "Starting deep deletion of course: " + courseId);

        // First, fetch the course to get information about related entities
        firestore.collection(COURSES_COLLECTION).document(courseId)
                .get()
                .addOnSuccessListener(courseDoc -> {
                    if (!courseDoc.exists()) {
                        Log.e(TAG, "Course not found: " + courseId);
                        listener.onFailure("Course not found");
                        return;
                    }

                    // Get the course data
                    Course course = courseDoc.toObject(Course.class);
                    if (course == null) {
                        Log.e(TAG, "Failed to parse course data: " + courseId);
                        listener.onFailure("Failed to parse course data");
                        return;
                    }

                    // Keep track of the instructor to update later
                    String instructorId = course.getInstructorId();

                    // Get list of enrolled students to update later
                    List<String> enrolledStudentIds = course.getEnrolledStudentIds();

                    // Get list of sessions to delete
                    List<String> sessionIds = course.getSessionIds();

                    // Begin the deletion process with sessions and QR codes
                    deleteAllSessions(courseId, sessionIds, () -> {
                        // After sessions are deleted, update instructor and students
                        updateInstructor(instructorId, courseId, () -> {
                            updateStudents(enrolledStudentIds, courseId, () -> {
                                // Finally, delete the course itself
                                deleteCourse(courseId, listener);
                            });
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error fetching course: " + e.getMessage());
                    listener.onFailure("Error fetching course: " + e.getMessage());
                });
    }

    /**
     * Delete all sessions associated with a course
     */
    private void deleteAllSessions(String courseId, List<String> sessionIds, Runnable onComplete) {
        Log.d(TAG, "Deleting " + (sessionIds != null ? sessionIds.size() : 0) + " sessions for course: " + courseId);

        if (sessionIds == null || sessionIds.isEmpty()) {
            // No sessions to delete
            onComplete.run();
            return;
        }

        // For each session, delete associated QR codes and attendance records
        AtomicInteger remainingSessionsToProcess = new AtomicInteger(sessionIds.size());

        for (String sessionId : sessionIds) {
            deleteSessionData(sessionId, () -> {
                // Check if this is the last session to be processed
                if (remainingSessionsToProcess.decrementAndGet() == 0) {
                    onComplete.run();
                }
            });
        }
    }

    /**
     * Delete a session and its QR codes and attendance records
     */
    private void deleteSessionData(String sessionId, Runnable onComplete) {
        Log.d(TAG, "Deleting session data for session: " + sessionId);

        // 1. Find and delete QR codes for this session
        firestore.collection(QR_CODES_COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(qrDocs -> {
                    // Delete all QR codes found
                    List<String> qrCodeIds = new ArrayList<>();

                    for (QueryDocumentSnapshot qrDoc : qrDocs) {
                        qrCodeIds.add(qrDoc.getId());
                    }

                    deleteQRCodes(qrCodeIds, () -> {
                        // 2. Delete attendance records for this session
                        deleteAttendanceRecordsForSession(sessionId, () -> {
                            // 3. Finally delete the session itself
                            firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                                    .delete()
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Session deleted: " + sessionId);
                                        onComplete.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        // Continue with deletion process even if session deletion fails
                                        Log.e(TAG, "Error deleting session: " + e.getMessage());
                                        onComplete.run();
                                    });
                        });
                    });
                })
                .addOnFailureListener(e -> {
                    // Continue with deletion process even if QR code retrieval fails
                    Log.e(TAG, "Error finding QR codes: " + e.getMessage());

                    // Try to delete attendance records anyway
                    deleteAttendanceRecordsForSession(sessionId, () -> {
                        // Then try to delete the session itself
                        firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                                .delete()
                                .addOnSuccessListener(aVoid -> onComplete.run())
                                .addOnFailureListener(e2 -> onComplete.run());
                    });
                });
    }

    /**
     * Delete a list of QR codes
     */
    private void deleteQRCodes(List<String> qrCodeIds, Runnable onComplete) {
        Log.d(TAG, "Deleting " + qrCodeIds.size() + " QR codes");

        if (qrCodeIds.isEmpty()) {
            onComplete.run();
            return;
        }

        AtomicInteger remainingQRCodes = new AtomicInteger(qrCodeIds.size());

        for (String qrCodeId : qrCodeIds) {
            firestore.collection(QR_CODES_COLLECTION).document(qrCodeId)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "QR code deleted: " + qrCodeId);
                        if (remainingQRCodes.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error deleting QR code: " + e.getMessage());
                        if (remainingQRCodes.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    });
        }
    }

    /**
     * Delete all attendance records for a session
     */
    private void deleteAttendanceRecordsForSession(String sessionId, Runnable onComplete) {
        Log.d(TAG, "Deleting attendance records for session: " + sessionId);

        firestore.collection(ATTENDANCE_RECORDS_COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .get()
                .addOnSuccessListener(recordDocs -> {
                    if (recordDocs.isEmpty()) {
                        onComplete.run();
                        return;
                    }

                    // Delete each attendance record
                    AtomicInteger remainingRecords = new AtomicInteger(recordDocs.size());

                    for (QueryDocumentSnapshot recordDoc : recordDocs) {
                        String recordId = recordDoc.getId();
                        String studentId = recordDoc.getString("studentId");

                        // Remove record from student if studentId is available
                        if (studentId != null) {
                            removeAttendanceRecordFromStudent(studentId, recordId, () -> {
                                // Delete the attendance record
                                deleteAttendanceRecord(recordId, () -> {
                                    if (remainingRecords.decrementAndGet() == 0) {
                                        onComplete.run();
                                    }
                                });
                            });
                        } else {
                            // Just delete the attendance record
                            deleteAttendanceRecord(recordId, () -> {
                                if (remainingRecords.decrementAndGet() == 0) {
                                    onComplete.run();
                                }
                            });
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding attendance records: " + e.getMessage());
                    onComplete.run();
                });
    }

    /**
     * Delete a specific attendance record
     */
    private void deleteAttendanceRecord(String recordId, Runnable onComplete) {
        firestore.collection(ATTENDANCE_RECORDS_COLLECTION).document(recordId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Attendance record deleted: " + recordId);
                    onComplete.run();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting attendance record: " + e.getMessage());
                    onComplete.run();
                });
    }

    /**
     * Remove the attendance record reference from a student
     */
    private void removeAttendanceRecordFromStudent(String studentId, String recordId, Runnable onComplete) {
        firestore.collection(USERS_COLLECTION).document(studentId)
                .get()
                .addOnSuccessListener(studentDoc -> {
                    if (studentDoc.exists()) {
                        List<String> attendanceRecordIds = (List<String>) studentDoc.get("attendanceRecordIds");

                        if (attendanceRecordIds != null && attendanceRecordIds.contains(recordId)) {
                            attendanceRecordIds.remove(recordId);

                            // Update the student document
                            firestore.collection(USERS_COLLECTION).document(studentId)
                                    .update("attendanceRecordIds", attendanceRecordIds)
                                    .addOnSuccessListener(aVoid -> onComplete.run())
                                    .addOnFailureListener(e -> onComplete.run());
                        } else {
                            onComplete.run();
                        }
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> onComplete.run());
    }

    /**
     * Update instructor to remove reference to the course
     */
    private void updateInstructor(String instructorId, String courseId, Runnable onComplete) {
        Log.d(TAG, "Updating instructor: " + instructorId);

        if (instructorId == null || instructorId.isEmpty()) {
            onComplete.run();
            return;
        }

        firestore.collection(USERS_COLLECTION).document(instructorId)
                .get()
                .addOnSuccessListener(instructorDoc -> {
                    if (instructorDoc.exists()) {
                        List<String> coursesIds = (List<String>) instructorDoc.get("coursesIds");

                        if (coursesIds != null && coursesIds.contains(courseId)) {
                            coursesIds.remove(courseId);

                            // Update the instructor document
                            firestore.collection(USERS_COLLECTION).document(instructorId)
                                    .update("coursesIds", coursesIds)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Instructor updated: " + instructorId);
                                        onComplete.run();
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error updating instructor: " + e.getMessage());
                                        onComplete.run();
                                    });
                        } else {
                            onComplete.run();
                        }
                    } else {
                        onComplete.run();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error finding instructor: " + e.getMessage());
                    onComplete.run();
                });
    }

    /**
     * Update all students to remove references to the course
     */
    private void updateStudents(List<String> studentIds, String courseId, Runnable onComplete) {
        Log.d(TAG, "Updating " + (studentIds != null ? studentIds.size() : 0) + " students");

        if (studentIds == null || studentIds.isEmpty()) {
            onComplete.run();
            return;
        }

        AtomicInteger remainingStudents = new AtomicInteger(studentIds.size());

        for (String studentId : studentIds) {
            firestore.collection(USERS_COLLECTION).document(studentId)
                    .get()
                    .addOnSuccessListener(studentDoc -> {
                        if (studentDoc.exists()) {
                            List<String> enrolledCourseIds = (List<String>) studentDoc.get("enrolledCourseIds");

                            if (enrolledCourseIds != null && enrolledCourseIds.contains(courseId)) {
                                enrolledCourseIds.remove(courseId);

                                // Update the student document
                                firestore.collection(USERS_COLLECTION).document(studentId)
                                        .update("enrolledCourseIds", enrolledCourseIds)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d(TAG, "Student updated: " + studentId);
                                            if (remainingStudents.decrementAndGet() == 0) {
                                                onComplete.run();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Error updating student: " + e.getMessage());
                                            if (remainingStudents.decrementAndGet() == 0) {
                                                onComplete.run();
                                            }
                                        });
                            } else {
                                if (remainingStudents.decrementAndGet() == 0) {
                                    onComplete.run();
                                }
                            }
                        } else {
                            if (remainingStudents.decrementAndGet() == 0) {
                                onComplete.run();
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error finding student: " + e.getMessage());
                        if (remainingStudents.decrementAndGet() == 0) {
                            onComplete.run();
                        }
                    });
        }
    }

    /**
     * Delete the course document itself
     */
    private void deleteCourse(String courseId, OnCompleteListener listener) {
        Log.d(TAG, "Deleting course document: " + courseId);

        firestore.collection(COURSES_COLLECTION).document(courseId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Course deleted successfully: " + courseId);
                    listener.onSuccess(courseId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error deleting course: " + e.getMessage());
                    listener.onFailure("Error deleting course: " + e.getMessage());
                });
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(String errorMessage);
    }
}