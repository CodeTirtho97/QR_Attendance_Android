package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Session {
    private String sessionId;
    private String courseId;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private String instructorId;
    private List<String> attendanceRecordIds;
    private String qrCodeId;
    private SessionStatus status;
    private int lateThresholdMinutes; // Minutes after startTime to mark attendance as "late"

    // Session status enum
    public enum SessionStatus {
        SCHEDULED("SCHEDULED"),
        IN_PROGRESS("IN_PROGRESS"),
        COMPLETED("COMPLETED"),
        CANCELLED("CANCELLED");

        private final String value;

        SessionStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Default constructor for Firestore
    public Session() {
        this.attendanceRecordIds = new ArrayList<>();
        this.status = SessionStatus.SCHEDULED;
        this.lateThresholdMinutes = 10; // Default: 10 minutes late threshold
    }

    // Constructor with required fields
    public Session(String courseId, String title, Date startTime, Date endTime, String location, String instructorId) {
        this.courseId = courseId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.instructorId = instructorId;
        this.attendanceRecordIds = new ArrayList<>();
        this.status = SessionStatus.SCHEDULED;
        this.lateThresholdMinutes = 10; // Default: 10 minutes late threshold
    }

    // Getters and setters
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public List<String> getAttendanceRecordIds() {
        return attendanceRecordIds;
    }

    public void setAttendanceRecordIds(List<String> attendanceRecordIds) {
        this.attendanceRecordIds = attendanceRecordIds;
    }

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    public int getLateThresholdMinutes() {
        return lateThresholdMinutes;
    }

    public void setLateThresholdMinutes(int lateThresholdMinutes) {
        this.lateThresholdMinutes = lateThresholdMinutes;
    }

    // Utility methods
    public void addAttendanceRecord(String recordId) {
        if (this.attendanceRecordIds == null) {
            this.attendanceRecordIds = new ArrayList<>();
        }

        if (!this.attendanceRecordIds.contains(recordId)) {
            this.attendanceRecordIds.add(recordId);
        }
    }

    public boolean removeAttendanceRecord(String recordId) {
        if (this.attendanceRecordIds != null) {
            return this.attendanceRecordIds.remove(recordId);
        }
        return false;
    }

    // Check if session is active (can mark attendance)
    public boolean isActive() {
        if (this.status == SessionStatus.CANCELLED) {
            return false;
        }

        Date now = new Date();

        // Session is active if current time is between startTime and endTime
        return now.after(startTime) && now.before(endTime);
    }

    // Update status based on current time
    public void updateStatus() {
        Date now = new Date();

        if (this.status == SessionStatus.CANCELLED) {
            return;
        }

        if (now.before(startTime)) {
            this.status = SessionStatus.SCHEDULED;
        } else if (now.after(endTime)) {
            this.status = SessionStatus.COMPLETED;
        } else {
            this.status = SessionStatus.IN_PROGRESS;
        }
    }
}