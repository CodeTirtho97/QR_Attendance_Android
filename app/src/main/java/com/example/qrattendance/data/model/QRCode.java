package com.example.qrattendance.data.model;

import java.util.Date;

/**
 * QRCode class representing a QR code generated for attendance tracking.
 * Each QR code is linked to a specific session and contains encoded attendance data.
 */
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

    /**
     * Enum representing the different types of QR codes
     */
    public enum QRCodeType {
        SESSION_ATTENDANCE,
        COURSE_ENROLLMENT,
        USER_VERIFICATION
    }

    // Default constructor
    public QRCode() {
        this.generatedAt = new Date();
        this.isActive = true;
        this.scanCount = 0;
        this.type = QRCodeType.SESSION_ATTENDANCE;
    }

    // Parameterized constructor
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

    /**
     * Increments the scan count for this QR code
     */
    public void incrementScanCount() {
        this.scanCount++;
    }

    /**
     * Checks if the QR code is still valid (not expired and active)
     *
     * @return true if the QR code is valid, false otherwise
     */
    public boolean isValid() {
        if (!isActive) {
            return false;
        }

        Date now = new Date();
        return now.before(expiresAt);
    }

    /**
     * Deactivates this QR code (e.g., after the session has ended)
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Extends the expiration time of this QR code
     *
     * @param additionalMinutes Number of minutes to extend the expiration time by
     */
    public void extendExpiration(int additionalMinutes) {
        if (expiresAt != null) {
            // Convert minutes to milliseconds and add to current expiration time
            long additionalMillis = additionalMinutes * 60 * 1000L;
            expiresAt = new Date(expiresAt.getTime() + additionalMillis);
        }
    }
}