package com.example.qrattendance.data.repository;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.qrattendance.data.model.AttendanceRecord;
import com.example.qrattendance.data.model.QRCode;
import com.example.qrattendance.data.model.Session;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;

import org.json.JSONObject;

import java.util.ArrayList;
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

        firestore.collection(ATTENDANCE_COLLECTION)
                .whereEqualTo("studentId", studentId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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