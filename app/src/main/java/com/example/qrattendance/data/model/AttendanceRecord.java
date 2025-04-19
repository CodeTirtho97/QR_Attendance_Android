package com.example.qrattendance.data.model;

import java.util.Date;

public class AttendanceRecord {
    private String recordId;
    private String sessionId;
    private String courseId;
    private String studentId;
    private String qrCodeId;
    private Date timestamp;
    private LocationData location;
    private boolean verified;
    private String verifiedBy; // instructor ID who verified the record, if applicable
    private Date verifiedAt;
    private AttendanceStatus status;

    // Nested class for location data
    public static class LocationData {
        private double latitude;
        private double longitude;
        private String locationName;

        public LocationData() {
            // Required empty constructor for Firestore
        }

        public LocationData(double latitude, double longitude, String locationName) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.locationName = locationName;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public String getLocationName() {
            return locationName;
        }

        public void setLocationName(String locationName) {
            this.locationName = locationName;
        }
    }

    // Attendance status enum
    public enum AttendanceStatus {
        PRESENT("PRESENT"),
        ABSENT("ABSENT"),
        LATE("LATE"),
        EXCUSED("EXCUSED");

        private final String value;

        AttendanceStatus(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Default constructor for Firestore
    public AttendanceRecord() {
        this.timestamp = new Date();
        this.verified = false;
        this.status = AttendanceStatus.PRESENT;
    }

    // Constructor with required fields
    public AttendanceRecord(String sessionId, String courseId, String studentId, String qrCodeId) {
        this.sessionId = sessionId;
        this.courseId = courseId;
        this.studentId = studentId;
        this.qrCodeId = qrCodeId;
        this.timestamp = new Date();
        this.verified = false;
        this.status = AttendanceStatus.PRESENT;
    }

    // Getters and setters
    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
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

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(String qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public LocationData getLocation() {
        return location;
    }

    public void setLocation(LocationData location) {
        this.location = location;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public Date getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Date verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }

    // Utility method to verify attendance
    public void verify(String instructorId) {
        this.verified = true;
        this.verifiedBy = instructorId;
        this.verifiedAt = new Date();
    }

    // Utility method to mark as late if timestamp is after expected time
    public void checkLateStatus(Date sessionStartTime, int lateThresholdMinutes) {
        if (timestamp != null && sessionStartTime != null) {
            // Calculate the late threshold timestamp
            long lateThresholdMillis = sessionStartTime.getTime() + (lateThresholdMinutes * 60 * 1000L);

            // Check if the attendance timestamp is after the threshold
            if (timestamp.getTime() > lateThresholdMillis) {
                this.status = AttendanceStatus.LATE;
            }
        }
    }
}