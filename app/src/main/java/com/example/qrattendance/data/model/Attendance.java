package com.example.qrattendance.data.model;

import java.util.Date;

/**
 * Attendance class representing an attendance record for a student in a session.
 * Each record tracks when a student marked their attendance for a specific session.
 */
public class Attendance {
    private String attendanceId;
    private String studentId;
    private String sessionId;
    private String courseId;
    private Date markedAt;
    private AttendanceStatus status;
    private String qrCodeId; // ID of the QR code that was scanned
    private String deviceInfo; // Information about the device used to mark attendance
    private String locationInfo; // Geographic location where attendance was marked
    private boolean isOfflineMarked; // Whether attendance was marked in offline mode
    private Date syncedAt; // When the offline attendance was synced with the server

    /**
     * Enum representing the possible statuses of an attendance record
     */
    public enum AttendanceStatus {
        PRESENT,
        ABSENT,
        LATE,
        EXCUSED
    }

    // Default constructor
    public Attendance() {
        this.markedAt = new Date();
        this.status = AttendanceStatus.PRESENT;
        this.isOfflineMarked = false;
    }

    // Parameterized constructor
    public Attendance(String attendanceId, String studentId, String sessionId, String courseId,
                      AttendanceStatus status, String qrCodeId) {
        this.attendanceId = attendanceId;
        this.studentId = studentId;
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.markedAt = new Date();
        this.status = status;
        this.qrCodeId = qrCodeId;
        this.isOfflineMarked = false;
    }

    // Getters and Setters
    public String getAttendanceId() {
        return attendanceId;
    }

    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public Date getMarkedAt() {
        return markedAt;
    }

    public void setMarkedAt(Date markedAt) {
        this.markedAt = markedAt;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public String getLocationInfo() {
        return locationInfo;
    }

    public void setLocationInfo(String locationInfo) {
        this.locationInfo = locationInfo;
    }

    public boolean isOfflineMarked() {
        return isOfflineMarked;
    }

    public void setOfflineMarked(boolean offlineMarked) {
        isOfflineMarked = offlineMarked;
    }

    public Date getSyncedAt() {
        return syncedAt;
    }

    public void setSyncedAt(Date syncedAt) {
        this.syncedAt = syncedAt;
    }

    /**
     * Marks this attendance as synced with the server
     */
    public void markAsSynced() {
        this.syncedAt = new Date();
        this.isOfflineMarked = false;
    }

    /**
     * Checks if the attendance was marked late (after the session started)
     * @param sessionStartTime The start time of the session
     * @return true if the attendance was marked late, false otherwise
     */
    public boolean isLate(Date sessionStartTime) {
        return this.markedAt.after(sessionStartTime);
    }

    /**
     * Updates the attendance status based on when it was marked relative to the session start time
     * @param sessionStartTime The start time of the session
     * @param lateThresholdMinutes Number of minutes after which an attendance is considered late
     */
    public void updateStatus(Date sessionStartTime, int lateThresholdMinutes) {
        if (status == AttendanceStatus.EXCUSED) {
            return; // Keep excused status unchanged
        }

        if (markedAt == null || sessionStartTime == null) {
            status = AttendanceStatus.ABSENT;
            return;
        }

        // Calculate time difference in minutes
        long diffMillis = markedAt.getTime() - sessionStartTime.getTime();
        long diffMinutes = diffMillis / (60 * 1000);

        if (diffMinutes <= 0) {
            // Marked before or at session start time
            status = AttendanceStatus.PRESENT;
        } else if (diffMinutes <= lateThresholdMinutes) {
            // Marked within the late threshold
            status = AttendanceStatus.LATE;
        } else {
            // Marked too late, consider absent
            status = AttendanceStatus.ABSENT;
        }
    }
}