package com.example.qrattendance.data.model;

import java.util.Date;

public class QRCode {
    private String qrCodeId;
    private String sessionId;
    private String content; // Encoded data content of the QR code
    private String imageUrl; // URL or path to the QR code image
    private Date generatedAt;
    private Date expiresAt;
    private boolean isActive;
    private int scanCount; // Number of times this QR code has been scanned
    private QRCodeType type;

    private String courseId;
    private String instructorId;


    public enum QRCodeType {
        SESSION_ATTENDANCE("SESSION_ATTENDANCE"),
        COURSE_ENROLLMENT("COURSE_ENROLLMENT"),
        USER_VERIFICATION("USER_VERIFICATION");

        private final String value;

        QRCodeType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Default constructor
    public QRCode() {
        this.generatedAt = new Date();
        this.isActive = true;
        this.scanCount = 0;
        this.type = QRCodeType.SESSION_ATTENDANCE;
    }

    // Constructor with parameters
    public QRCode(String qrCodeId, String sessionId, String content, Date expiresAt, QRCodeType type) {
        this.qrCodeId = qrCodeId;
        this.sessionId = sessionId;
        this.content = content;
        this.generatedAt = new Date();
        this.expiresAt = expiresAt;
        this.isActive = true;
        this.scanCount = 0;
        this.type = type;
    }

    // Getters and Setters
    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Date getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(Date generatedAt) {
        this.generatedAt = generatedAt;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public int getScanCount() {
        return scanCount;
    }

    public void setScanCount(int scanCount) {
        this.scanCount = scanCount;
    }

    public QRCodeType getType() {
        return type;
    }

    public void setType(QRCodeType type) {
        this.type = type;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    // Increment scan count
    public void incrementScanCount() {
        this.scanCount++;
    }

    // Check if QR code is valid (not expired and active)
    public boolean isValid() {
        if (!isActive) {
            return false;
        }

        Date now = new Date();
        return now.before(expiresAt);
    }

    // Deactivate the QR code
    public void deactivate() {
        this.isActive = false;
    }

    // Extend expiration time
    public void extendExpiration(int additionalMinutes) {
        if (expiresAt != null) {
            long additionalMillis = additionalMinutes * 60 * 1000L;
            expiresAt = new Date(expiresAt.getTime() + additionalMillis);
        }
    }
}