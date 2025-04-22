package com.example.qrattendance.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.AttendanceRecord;
import com.example.qrattendance.data.model.Course;
import com.example.qrattendance.data.model.QRCode;
import com.example.qrattendance.data.model.Session;
import com.example.qrattendance.data.model.Student;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class AttendanceRepository {
    private static final String TAG = "AttendanceRepository";
    private static final String ATTENDANCE_COLLECTION = "attendance_records";
    private static final String SESSIONS_COLLECTION = "sessions";
    private static final String USERS_COLLECTION = "users";
    private static final String QRCODES_COLLECTION = "qr_codes";
    private static final String COURSES_COLLECTION = "courses";

    private static AttendanceRepository instance;
    private final FirebaseFirestore firestore;
    private final MutableLiveData<List<AttendanceRecord>> attendanceRecordsLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<Session>> sessionsLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Map<String, Object>> courseDetailsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Object>> sessionDetailsLiveData = new MutableLiveData<>();

    // Private constructor for singleton pattern
    private AttendanceRepository() {
        firestore = FirebaseFirestore.getInstance();
    }

    // Get singleton instance
    public static synchronized AttendanceRepository getInstance() {
        if (instance == null) {
            instance = new AttendanceRepository();
        }
        return instance;
    }

    // Get attendance records
    public LiveData<List<AttendanceRecord>> getAttendanceRecords() {
        return attendanceRecordsLiveData;
    }

    // Get sessions
    public LiveData<List<Session>> getSessions() {
        return sessionsLiveData;
    }

    // Get error message
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Get loading state
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    // Get course details with related data
    public LiveData<Map<String, Object>> getCourseDetails() {
        return courseDetailsLiveData;
    }

    // Get session details with related data
    public LiveData<Map<String, Object>> getSessionDetails() {
        return sessionDetailsLiveData;
    }

    // Fetch sessions by course ID
    public void fetchSessionsByCourse(String courseId) {
        isLoading.setValue(true);

        firestore.collection(SESSIONS_COLLECTION)
                .whereEqualTo("courseId", courseId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        errorMessage.setValue("Failed to load sessions: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }

                    List<Session> sessions = new ArrayList<>();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            Session session = document.toObject(Session.class);
                            session.setSessionId(document.getId());
                            // Update the status based on current time
                            session.updateStatus();
                            sessions.add(session);
                        }
                    }

                    sessionsLiveData.setValue(sessions);
                    isLoading.setValue(false);
                });
    }

    // Fetch attendance records by session ID
    public void fetchAttendanceBySession(String sessionId) {
        isLoading.setValue(true);

        firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("sessionId", sessionId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        errorMessage.setValue("Failed to load attendance records: " + e.getMessage());
                        isLoading.setValue(false);
                        return;
                    }

                    List<AttendanceRecord> records = new ArrayList<>();

                    if (snapshots != null) {
                        for (QueryDocumentSnapshot document : snapshots) {
                            AttendanceRecord record = document.toObject(AttendanceRecord.class);
                            record.setRecordId(document.getId());
                            records.add(record);
                        }
                    }

                    attendanceRecordsLiveData.setValue(records);
                    isLoading.setValue(false);
                });
    }

    // Fetch attendance records by student ID
    public void fetchAttendanceByStudent(String studentId) {
        isLoading.setValue(true);

        // Remove the orderBy to avoid composite index requirement
        firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<AttendanceRecord> records = new ArrayList<>();

                    for (DocumentSnapshot document : queryDocumentSnapshots.getDocuments()) {
                        AttendanceRecord record = document.toObject(AttendanceRecord.class);
                        if (record != null) {
                            record.setRecordId(document.getId());
                            records.add(record);
                        }
                    }

                    // Sort locally instead of in the query
                    Collections.sort(records, (r1, r2) -> {
                        if (r1.getTimestamp() == null && r2.getTimestamp() == null) return 0;
                        if (r1.getTimestamp() == null) return 1;
                        if (r2.getTimestamp() == null) return -1;
                        // Descending order (newest first)
                        return r2.getTimestamp().compareTo(r1.getTimestamp());
                    });

                    attendanceRecordsLiveData.setValue(records);
                    isLoading.setValue(false);
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to load attendance records: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Fetch course information by ID
    public void fetchCourseInfo(String courseId, OnCourseInfoListener listener) {
        if (courseId == null) {
            listener.onResult("Unknown Course", false);
            return;
        }

        firestore.collection("courses").document(courseId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String courseCode = documentSnapshot.getString("courseCode");
                        String courseName = documentSnapshot.getString("courseName");

                        String result;
                        if (courseCode != null && courseName != null) {
                            result = courseCode + " - " + courseName;
                        } else if (courseName != null) {
                            result = courseName;
                        } else if (courseCode != null) {
                            result = courseCode;
                        } else {
                            result = "Unnamed Course";
                        }
                        listener.onResult(result, true);
                    } else {
                        listener.onResult("Unknown Course", false);
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onResult("Unknown Course", false);
                });
    }

    // Fetch session information by ID
    public void fetchSessionInfo(String sessionId, OnSessionInfoListener listener) {
        if (sessionId == null) {
            listener.onResult("Unknown Session", false);
            return;
        }

        firestore.collection("sessions").document(sessionId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String title = documentSnapshot.getString("title");
                        if (title != null) {
                            listener.onResult(title, true);
                        } else {
                            listener.onResult("Unnamed Session", false);
                        }
                    } else {
                        listener.onResult("Unknown Session", false);
                    }
                })
                .addOnFailureListener(e -> {
                    listener.onResult("Unknown Session", false);
                });
    }

    // Interface for course info callback
    public interface OnCourseInfoListener {
        void onResult(String courseInfo, boolean success);
    }

    // Interface for session info callback
    public interface OnSessionInfoListener {
        void onResult(String sessionInfo, boolean success);
    }

    // Mark attendance using QR code
    public void markAttendance(String qrCodeContent, String studentId, AttendanceRecord.LocationData location, OnAttendanceListener listener) {
        isLoading.setValue(true);

        try {
            // Parse QR code content
            QRCodeData qrData = parseQRCodeContent(qrCodeContent);
            if (qrData == null) {
                isLoading.setValue(false);
                listener.onFailure("Invalid QR code format");
                return;
            }

            String qrCodeId = qrData.qrCodeId;
            String sessionId = qrData.sessionId;
            String courseId = qrData.courseId;

            Log.d(TAG, "Looking for QR code with ID: " + qrCodeId);
            Log.d(TAG, "Session ID: " + sessionId);
            Log.d(TAG, "Course ID: " + (courseId != null ? courseId : "Not provided in QR"));

            // 1. First check if attendance has already been marked
            firestore.collection(ATTENDANCE_COLLECTION)
                    .whereEqualTo("sessionId", sessionId)
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // Attendance already marked
                                isLoading.setValue(false);
                                listener.onFailure("Attendance already marked for this session");
                                return;
                            }

                            // 2. Verify QR code exists and is valid
                            firestore.collection(QRCODES_COLLECTION).document(qrCodeId)
                                    .get()
                                    .addOnSuccessListener(qrCodeDoc -> {
                                        if (!qrCodeDoc.exists()) {
                                            isLoading.setValue(false);
                                            listener.onFailure("QR code not found");
                                            return;
                                        }

                                        QRCode qrCode = qrCodeDoc.toObject(QRCode.class);
                                        if (qrCode == null || !qrCode.isValid()) {
                                            isLoading.setValue(false);
                                            listener.onFailure("QR code has expired or is inactive");
                                            return;
                                        }

                                        // 3. Verify session exists and is active
                                        firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                                                .get()
                                                .addOnSuccessListener(sessionDoc -> {
                                                    if (!sessionDoc.exists()) {
                                                        isLoading.setValue(false);
                                                        listener.onFailure("Session not found");
                                                        return;
                                                    }

                                                    Session session = sessionDoc.toObject(Session.class);
                                                    if (session == null) {
                                                        isLoading.setValue(false);
                                                        listener.onFailure("Invalid session data");
                                                        return;
                                                    }

                                                    session.setSessionId(sessionId);
                                                    session.updateStatus();

                                                    if (!session.isActive()) {
                                                        isLoading.setValue(false);
                                                        listener.onFailure("Session is not active, can't mark attendance");
                                                        return;
                                                    }

                                                    // If courseId is not in the QR code, get it from the session
                                                    String finalCourseId = (courseId != null) ? courseId : session.getCourseId();

                                                    // 4. Create new attendance record with a specific ID
                                                    String attendanceId = firestore.collection(ATTENDANCE_COLLECTION).document().getId();
                                                    AttendanceRecord record = new AttendanceRecord(sessionId, finalCourseId, studentId, qrCodeId);
                                                    record.setRecordId(attendanceId);
                                                    record.setLocation(location);
                                                    record.setTimestamp(new Date());
                                                    record.checkLateStatus(session.getStartTime(), session.getLateThresholdMinutes());

                                                    Map<String, Object> recordMap = attendanceRecordToMap(record);

                                                    // 5. Add attendance record to Firestore
                                                    firestore.collection(ATTENDANCE_COLLECTION)
                                                            .document(attendanceId)
                                                            .set(recordMap)
                                                            .addOnSuccessListener(aVoid -> {
                                                                // 6. Update attendance count in QR code
                                                                Log.d(TAG, "Attendance record added with ID: " + attendanceId);
                                                                firestore.collection(QRCODES_COLLECTION).document(qrCodeId)
                                                                        .update("scanCount", qrCode.getScanCount() + 1);

                                                                // 7. Update session with new attendance record
                                                                List<String> attendanceRecords = session.getAttendanceRecordIds();
                                                                if (attendanceRecords == null) {
                                                                    attendanceRecords = new ArrayList<>();
                                                                }
                                                                attendanceRecords.add(attendanceId);
                                                                firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                                                                        .update("attendanceRecordIds", attendanceRecords);

                                                                // 8. Update student with attendance record
                                                                firestore.collection(USERS_COLLECTION).document(studentId)
                                                                        .get()
                                                                        .addOnSuccessListener(studentDoc -> {
                                                                            if (studentDoc.exists()) {
                                                                                List<String> studentAttendanceRecords = (List<String>) studentDoc.get("attendanceRecordIds");
                                                                                if (studentAttendanceRecords == null) {
                                                                                    studentAttendanceRecords = new ArrayList<>();
                                                                                }
                                                                                studentAttendanceRecords.add(attendanceId);
                                                                                firestore.collection(USERS_COLLECTION).document(studentId)
                                                                                        .update("attendanceRecordIds", studentAttendanceRecords);
                                                                            }

                                                                            isLoading.setValue(false);
                                                                            listener.onSuccess("Attendance marked successfully");
                                                                        })
                                                                        .addOnFailureListener(e -> {
                                                                            // If student update fails, still consider attendance successful
                                                                            isLoading.setValue(false);
                                                                            listener.onSuccess("Attendance marked, but student record update failed");
                                                                        });
                                                            })
                                                            .addOnFailureListener(e -> {
                                                                isLoading.setValue(false);
                                                                listener.onFailure("Failed to create attendance record: " + e.getMessage());
                                                            });
                                                })
                                                .addOnFailureListener(e -> {
                                                    isLoading.setValue(false);
                                                    listener.onFailure("Failed to verify session: " + e.getMessage());
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        isLoading.setValue(false);
                                        listener.onFailure("Failed to verify QR code: " + e.getMessage());
                                    });
                        } else {
                            isLoading.setValue(false);
                            listener.onFailure("Failed to check existing attendance: " + task.getException().getMessage());
                        }
                    });
        } catch (Exception e) {
            isLoading.setValue(false);
            listener.onFailure("Error processing request: " + e.getMessage());
        }

    }

    // Create new session
    public void createSession(Session session, OnCompleteListener listener) {
        isLoading.setValue(true);

        Map<String, Object> sessionMap = sessionToMap(session);

        firestore.collection(SESSIONS_COLLECTION)
                .add(sessionMap)
                .addOnSuccessListener(documentReference -> {
                    String sessionId = documentReference.getId();
                    session.setSessionId(sessionId);

                    // Update course with new session ID
                    DocumentReference courseRef = firestore.collection(COURSES_COLLECTION).document(session.getCourseId());
                    courseRef.get().addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            List<String> sessionIds = (List<String>) documentSnapshot.get("sessionIds");
                            if (sessionIds == null) {
                                sessionIds = new ArrayList<>();
                            }
                            sessionIds.add(sessionId);

                            courseRef.update("sessionIds", sessionIds)
                                    .addOnSuccessListener(aVoid -> {
                                        isLoading.setValue(false);
                                        listener.onSuccess(sessionId);
                                    })
                                    .addOnFailureListener(e -> {
                                        errorMessage.setValue("Failed to update course: " + e.getMessage());
                                        isLoading.setValue(false);
                                        listener.onFailure(e.getMessage());
                                    });
                        } else {
                            errorMessage.setValue("Course not found");
                            isLoading.setValue(false);
                            listener.onFailure("Course not found");
                        }
                    }).addOnFailureListener(e -> {
                        errorMessage.setValue("Failed to get course: " + e.getMessage());
                        isLoading.setValue(false);
                        listener.onFailure(e.getMessage());
                    });
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to create session: " + e.getMessage());
                    isLoading.setValue(false);
                    listener.onFailure(e.getMessage());
                });
    }

    // Generate QR code for session
    public void generateQRCodeForSession(Session session, Date expiryDate, OnQRCodeListener listener) {
        isLoading.setValue(true);

        try {
            // 1. Create QR code object with complete data
            QRCode qrCode = new QRCode();
            qrCode.setSessionId(session.getSessionId());
            qrCode.setCourseId(session.getCourseId());  // Set course ID
            qrCode.setExpiresAt(expiryDate);
            qrCode.setInstructorId(session.getInstructorId());  // Set instructor ID
            qrCode.setType(QRCode.QRCodeType.SESSION_ATTENDANCE);
            qrCode.setActive(true);
            qrCode.setGeneratedAt(new Date());

            // Generate a unique ID for the QR code
            String qrCodeId = firestore.collection(QRCODES_COLLECTION).document().getId();
            qrCode.setQrCodeId(qrCodeId);

            Log.d(TAG, "Generated QR code with ID: " + qrCodeId);

            // Create content with the QR code ID included
            String content = createQRCodeContent(qrCode);
            qrCode.setContent(content);

            Map<String, Object> qrCodeMap = qrCodeToMap(qrCode);

            // 2. Save QR code to Firestore with the pre-generated ID
            firestore.collection(QRCODES_COLLECTION)
                    .document(qrCodeId)
                    .set(qrCodeMap)
                    .addOnSuccessListener(aVoid -> {
                        // 3. Update session with QR code ID
                        firestore.collection(SESSIONS_COLLECTION)
                                .document(session.getSessionId())
                                .update("qrCodeId", qrCodeId)
                                .addOnSuccessListener(aVoid2 -> {
                                    // 4. Add QR code ID to instructor's list of generated QR codes
                                    firestore.collection(USERS_COLLECTION)
                                            .document(session.getInstructorId())
                                            .get()
                                            .addOnSuccessListener(instructorDoc -> {
                                                if (instructorDoc.exists()) {
                                                    List<String> generatedQRs = (List<String>) instructorDoc.get("generatedQRCodeIds");
                                                    if (generatedQRs == null) {
                                                        generatedQRs = new ArrayList<>();
                                                    }
                                                    if (!generatedQRs.contains(qrCodeId)) {
                                                        generatedQRs.add(qrCodeId);
                                                        firestore.collection(USERS_COLLECTION)
                                                                .document(session.getInstructorId())
                                                                .update("generatedQRCodeIds", generatedQRs);
                                                    }
                                                }

                                                isLoading.setValue(false);
                                                listener.onSuccess(qrCodeId, content);
                                            })
                                            .addOnFailureListener(e -> {
                                                // Still consider successful even if instructor update fails
                                                isLoading.setValue(false);
                                                listener.onSuccess(qrCodeId, content);
                                            });
                                })
                                .addOnFailureListener(e -> {
                                    errorMessage.setValue("Failed to update session: " + e.getMessage());
                                    isLoading.setValue(false);
                                    listener.onFailure(e.getMessage());
                                });
                    })
                    .addOnFailureListener(e -> {
                        errorMessage.setValue("Failed to create QR code: " + e.getMessage());
                        isLoading.setValue(false);
                        listener.onFailure(e.getMessage());
                    });
        } catch (Exception e) {
            errorMessage.setValue("Failed to generate QR code: " + e.getMessage());
            isLoading.setValue(false);
            listener.onFailure(e.getMessage());
        }
    }

    // Helper method to create QR code content
    private String createQRCodeContent(QRCode qrCode) {
        Map<String, Object> contentMap = new HashMap<>();
        contentMap.put("qrCodeId", qrCode.getQrCodeId());
        contentMap.put("sessionId", qrCode.getSessionId());
        contentMap.put("courseId", qrCode.getCourseId());  // Add courseId
        contentMap.put("generatedAt", System.currentTimeMillis());
        contentMap.put("expiresAt", qrCode.getExpiresAt().getTime());
        contentMap.put("instructorId", qrCode.getInstructorId());  // Add instructor ID for verification

        // Convert to JSON string - in a real app, use Gson or similar library
        try {
            return new JSONObject(contentMap).toString();
        } catch (Exception e) {
            Log.e(TAG, "Error creating QR content", e);
            return "{}";  // Return empty JSON as fallback
        }
    }

    // Helper method to parse QR code content
    private QRCodeData parseQRCodeContent(String content) {
        try {
            JSONObject jsonObject = new JSONObject(content);
            QRCodeData data = new QRCodeData();

            // Extract all needed data
            if (jsonObject.has("qrCodeId") && jsonObject.has("sessionId")) {
                data.qrCodeId = jsonObject.getString("qrCodeId");
                data.sessionId = jsonObject.getString("sessionId");

                // Get optional data if available
                if (jsonObject.has("courseId")) {
                    data.courseId = jsonObject.getString("courseId");
                }

                if (jsonObject.has("expiresAt")) {
                    data.expiresAt = jsonObject.getLong("expiresAt");
                }

                return data;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parsing QR code content", e);
        }
        return null;
    }

    // Fetch session and its attendance details
    public void fetchSessionWithAttendanceDetails(String sessionId) {
        isLoading.setValue(true);

        Map<String, Object> sessionDetails = new HashMap<>();

        // First, get the session data
        firestore.collection(SESSIONS_COLLECTION).document(sessionId)
                .get()
                .addOnSuccessListener(sessionSnapshot -> {
                    if (sessionSnapshot.exists()) {
                        Session session = sessionSnapshot.toObject(Session.class);
                        if (session != null) {
                            session.setSessionId(sessionSnapshot.getId());
                            sessionDetails.put("session", session);

                            // Get the course data
                            firestore.collection(COURSES_COLLECTION).document(session.getCourseId())
                                    .get()
                                    .addOnSuccessListener(courseSnapshot -> {
                                        if (courseSnapshot.exists()) {
                                            Course course = courseSnapshot.toObject(Course.class);
                                            if (course != null) {
                                                course.setCourseId(courseSnapshot.getId());
                                                sessionDetails.put("course", course);
                                            }
                                        }

                                        // Get all attendance records for this session
                                        firestore.collection(ATTENDANCE_COLLECTION)
                                                .whereEqualTo("sessionId", sessionId)
                                                .get()
                                                .addOnSuccessListener(recordSnapshots -> {
                                                    List<AttendanceRecord> records = new ArrayList<>();
                                                    List<String> studentIds = new ArrayList<>();

                                                    for (DocumentSnapshot doc : recordSnapshots.getDocuments()) {
                                                        AttendanceRecord record = doc.toObject(AttendanceRecord.class);
                                                        if (record != null) {
                                                            record.setRecordId(doc.getId());
                                                            records.add(record);
                                                            studentIds.add(record.getStudentId());
                                                        }
                                                    }
                                                    sessionDetails.put("attendanceRecords", records);

                                                    // Calculate attendance statistics
                                                    Map<String, Object> stats = new HashMap<>();
                                                    stats.put("totalPresent", records.size());

                                                    Course course = (Course) sessionDetails.get("course");
                                                    if (course != null && course.getEnrolledStudentIds() != null) {
                                                        stats.put("totalStudents", course.getEnrolledStudentIds().size());
                                                        stats.put("absentCount", course.getEnrolledStudentIds().size() - records.size());

                                                        if (course.getEnrolledStudentIds().size() > 0) {
                                                            double percentage = (double) records.size() * 100 / course.getEnrolledStudentIds().size();
                                                            stats.put("attendancePercentage", Math.round(percentage * 10) / 10.0); // Round to 1 decimal place
                                                        } else {
                                                            stats.put("attendancePercentage", 0.0);
                                                        }
                                                    } else {
                                                        stats.put("totalStudents", 0);
                                                        stats.put("absentCount", 0);
                                                        stats.put("attendancePercentage", 0.0);
                                                    }

                                                    sessionDetails.put("stats", stats);

                                                    // Get all student details for those who are present
                                                    fetchStudentDetails(studentIds, sessionDetails);
                                                })
                                                .addOnFailureListener(e -> {
                                                    errorMessage.setValue("Failed to fetch attendance records: " + e.getMessage());
                                                    isLoading.setValue(false);
                                                });
                                    })
                                    .addOnFailureListener(e -> {
                                        errorMessage.setValue("Failed to fetch course: " + e.getMessage());
                                        isLoading.setValue(false);
                                    });
                        } else {
                            errorMessage.setValue("Failed to parse session data");
                            isLoading.setValue(false);
                        }
                    } else {
                        errorMessage.setValue("Session not found");
                        isLoading.setValue(false);
                    }
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to fetch session: " + e.getMessage());
                    isLoading.setValue(false);
                });
    }

    // Fetch student details for attendance records
    private void fetchStudentDetails(List<String> studentIds, Map<String, Object> sessionDetails) {
        if (studentIds.isEmpty()) {
            sessionDetailsLiveData.setValue(sessionDetails);
            isLoading.setValue(false);
            return;
        }

        Map<String, Student> studentMap = new HashMap<>();
        final int[] fetchCount = {0};
        final int totalToFetch = studentIds.size();

        for (String studentId : studentIds) {
            firestore.collection(USERS_COLLECTION).document(studentId)
                    .get()
                    .addOnSuccessListener(studentDoc -> {
                        if (studentDoc.exists()) {
                            Student student = studentDoc.toObject(Student.class);
                            if (student != null) {
                                student.setUserId(studentDoc.getId());
                                studentMap.put(studentId, student);
                            }
                        }

                        fetchCount[0]++;
                        if (fetchCount[0] >= totalToFetch) {
                            sessionDetails.put("students", studentMap);
                            sessionDetailsLiveData.setValue(sessionDetails);
                            isLoading.setValue(false);
                        }
                    })
                    .addOnFailureListener(e -> {
                        fetchCount[0]++;
                        if (fetchCount[0] >= totalToFetch) {
                            sessionDetails.put("students", studentMap);
                            sessionDetailsLiveData.setValue(sessionDetails);
                            isLoading.setValue(false);
                        }
                    });
        }
    }


    /**
     * Clean up expired QR codes
     */
    public void cleanupExpiredQRCodes() {
        Date now = new Date();

        firestore.collection(QRCODES_COLLECTION)
                .whereLessThan("expiresAt", now)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        return;
                    }

                    int count = 0;
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        // Deactivate the QR code
                        doc.getReference().update("isActive", false)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "QR code deactivated: " + doc.getId()))
                                .addOnFailureListener(e -> Log.e(TAG, "Error deactivating QR code: " + e.getMessage()));
                        count++;
                    }

                    Log.d(TAG, "Deactivated " + count + " expired QR codes");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error cleaning up expired QR codes: " + e.getMessage());
                });
    }


    // Static class to hold QR code parsed data
    private static class QRCodeData {
        String qrCodeId;
        String sessionId;
        String courseId;
        long expiresAt;
    }

    // Helper method to convert AttendanceRecord to Map for Firestore
    private Map<String, Object> attendanceRecordToMap(AttendanceRecord record) {
        Map<String, Object> recordMap = new HashMap<>();
        recordMap.put("recordId", record.getRecordId());  // Make sure this field is included
        recordMap.put("sessionId", record.getSessionId());
        recordMap.put("courseId", record.getCourseId());
        recordMap.put("studentId", record.getStudentId());
        recordMap.put("qrCodeId", record.getQrCodeId());
        recordMap.put("timestamp", record.getTimestamp());
        recordMap.put("location", record.getLocation());
        recordMap.put("verified", record.isVerified());
        recordMap.put("verifiedBy", record.getVerifiedBy());
        recordMap.put("verifiedAt", record.getVerifiedAt());
        recordMap.put("status", record.getStatus() != null ? record.getStatus().getValue() : null);
        return recordMap;
    }

    // Helper method to convert Session to Map for Firestore
    private Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> sessionMap = new HashMap<>();
        sessionMap.put("courseId", session.getCourseId());
        sessionMap.put("title", session.getTitle());
        sessionMap.put("description", session.getDescription());
        sessionMap.put("startTime", session.getStartTime());
        sessionMap.put("endTime", session.getEndTime());
        sessionMap.put("location", session.getLocation());
        sessionMap.put("instructorId", session.getInstructorId());
        sessionMap.put("attendanceRecordIds", session.getAttendanceRecordIds());
        sessionMap.put("qrCodeId", session.getQrCodeId());
        sessionMap.put("status", session.getStatus() != null ? session.getStatus().getValue() : null);
        sessionMap.put("lateThresholdMinutes", session.getLateThresholdMinutes());
        return sessionMap;
    }

    // Helper method to convert QRCode to Map for Firestore
    private Map<String, Object> qrCodeToMap(QRCode qrCode) {
        Map<String, Object> qrCodeMap = new HashMap<>();
        qrCodeMap.put("qrCodeId", qrCode.getQrCodeId());
        qrCodeMap.put("sessionId", qrCode.getSessionId());
        qrCodeMap.put("courseId", qrCode.getCourseId());
        qrCodeMap.put("instructorId", qrCode.getInstructorId());
        qrCodeMap.put("content", qrCode.getContent());
        qrCodeMap.put("imageUrl", qrCode.getImageUrl());
        qrCodeMap.put("generatedAt", qrCode.getGeneratedAt());
        qrCodeMap.put("expiresAt", qrCode.getExpiresAt());
        qrCodeMap.put("isActive", qrCode.isActive());
        qrCodeMap.put("scanCount", qrCode.getScanCount());
        qrCodeMap.put("type", qrCode.getType() != null ? qrCode.getType().getValue() : null);
        return qrCodeMap;
    }

    // Callback interfaces
    public interface OnCompleteListener {
        void onSuccess(String id);
        void onFailure(String errorMessage);
    }

    public interface OnAttendanceListener {
        void onSuccess(String message);
        void onFailure(String errorMessage);
    }

    public interface OnQRCodeListener {
        void onSuccess(String qrCodeId, String qrCodeContent);
        void onFailure(String errorMessage);
    }
}