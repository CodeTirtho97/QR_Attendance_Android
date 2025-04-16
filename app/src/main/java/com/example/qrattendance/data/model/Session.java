package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Session class representing a single class session for a course.
 * Each session has a QR code for attendance and tracks attending students.
 */
public class Session {
    private String sessionId;
    private String courseId;
    private String title;
    private String description;
    private Date startTime;
    private Date endTime;
    private String location;
    private String qrCodeId;
    private Date qrCodeGeneratedAt;
    private Date qrCodeExpiresAt;
    private List<String> attendingStudentIds;
    private SessionStatus status;

    /**
     * Enum representing the possible statuses of a session
     */
    public enum SessionStatus {
        SCHEDULED,
        ONGOING,
        COMPLETED,
        CANCELLED
    }

    // Default constructor
    public Session() {
        this.attendingStudentIds = new ArrayList<>();
        this.status = SessionStatus.SCHEDULED;
    }

    // Parameterized constructor
    public Session(String sessionId, String courseId, String title, String description,
                   Date startTime, Date endTime, String location) {
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.location = location;
        this.attendingStudentIds = new ArrayList<>();
        this.status = SessionStatus.SCHEDULED;
    }

    // Getters and Setters
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

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public Date getQrCodeGeneratedAt() {
        return qrCodeGeneratedAt;
    }

    public void setQrCodeGeneratedAt(Date qrCodeGeneratedAt) {
        this.qrCodeGeneratedAt = qrCodeGeneratedAt;
    }

    public Date getQrCodeExpiresAt() {
        return qrCodeExpiresAt;
    }

    public void setQrCodeExpiresAt(Date qrCodeExpiresAt) {
        this.qrCodeExpiresAt = qrCodeExpiresAt;
    }

    public List<String> getAttendingStudentIds() {
        return attendingStudentIds;
    }

    public void setAttendingStudentIds(List<String> attendingStudentIds) {
        this.attendingStudentIds = attendingStudentIds;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public void setStatus(SessionStatus status) {
        this.status = status;
    }

    /**
     * Adds a student ID to the session's attending students list
     * @param studentId The ID of the student to mark as attending
     * @return true if the student was successfully marked as attending, false if already marked
     */
    public boolean markStudentAttendance(String studentId) {
        if (!this.attendingStudentIds.contains(studentId)) {
            this.attendingStudentIds.add(studentId);
            return true;
        }
        return false;
    }

    /**
     * Checks if the QR code for this session is still valid
     * @return true if the QR code is valid (not expired), false otherwise
     */
    public boolean isQRCodeValid() {
        if (qrCodeId == null || qrCodeExpiresAt == null) {
            return false;
        }

        Date now = new Date();
        return now.before(qrCodeExpiresAt);
    }

    /**
     * Updates the session status based on current time
     */
    public void updateSessionStatus() {
        Date now = new Date();

        if (status == SessionStatus.CANCELLED) {
            return; // Keep canceled sessions as canceled
        }

        if (now.before(startTime)) {
            status = SessionStatus.SCHEDULED;
        } else if (now.after(startTime) && now.before(endTime)) {
            status = SessionStatus.ONGOING;
        } else if (now.after(endTime)) {
            status = SessionStatus.COMPLETED;
        }
    }
}