package com.example.qrattendance.data.model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for model conversions between app models and Firebase.
 * Provides methods to convert between model objects and Firestore maps.
 */
public class ModelUtils {

    /**
     * Convert a User object to a Firestore map
     * @param user The user to convert
     * @return A map representing the user for Firestore storage
     */
    public static Map<String, Object> userToMap(User user) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", user.getUserId());
        map.put("email", user.getEmail());
        map.put("name", user.getName());
        map.put("phoneNumber", user.getPhoneNumber());
        map.put("profileImageUrl", user.getProfileImageUrl());
        map.put("createdAt", user.getCreatedAt());
        map.put("lastLoginAt", user.getLastLoginAt());
        map.put("role", user.getRole().name());

        // Add specific fields based on user type
        if (user instanceof Student) {
            Student student = (Student) user;
            map.put("userType", "STUDENT");
            map.put("rollNumber", student.getRollNumber());
            map.put("department", student.getDepartment());
            map.put("semester", student.getSemester());
            map.put("batch", student.getBatch());
            map.put("enrolledCourseIds", student.getEnrolledCourseIds());
            map.put("attendanceRecordIds", student.getAttendanceRecordIds());
        } else if (user instanceof Instructor) {
            Instructor instructor = (Instructor) user;
            map.put("userType", "INSTRUCTOR");
            map.put("employeeId", instructor.getEmployeeId());
            map.put("department", instructor.getDepartment());
            map.put("designation", instructor.getDesignation());
            map.put("coursesIds", instructor.getCoursesIds());
            map.put("generatedQRCodeIds", instructor.getGeneratedQRCodeIds());
        } else if (user instanceof Admin) {
            Admin admin = (Admin) user;
            map.put("userType", "ADMIN");
            map.put("adminId", admin.getAdminId());
            map.put("position", admin.getPosition());
            map.put("privilegeLevel", admin.getPrivilegeLevel().name());
        }

        return map;
    }

    /**
     * Convert a Firestore document to a User object based on userType
     * @param document The Firestore document to convert
     * @return The appropriate User subclass (Student, Instructor, or Admin)
     */
    public static User documentToUser(DocumentSnapshot document) {
        String userType = document.getString("userType");
        User user;

        if ("STUDENT".equals(userType)) {
            Student student = new Student();
            student.setRollNumber(document.getString("rollNumber"));
            student.setDepartment(document.getString("department"));
            student.setSemester(document.getString("semester"));
            student.setBatch(document.getString("batch"));
            student.setEnrolledCourseIds((List<String>) document.get("enrolledCourseIds"));
            student.setAttendanceRecordIds((List<String>) document.get("attendanceRecordIds"));
            user = student;
        } else if ("INSTRUCTOR".equals(userType)) {
            Instructor instructor = new Instructor();
            instructor.setEmployeeId(document.getString("employeeId"));
            instructor.setDepartment(document.getString("department"));
            instructor.setDesignation(document.getString("designation"));
            instructor.setCoursesIds((List<String>) document.get("coursesIds"));
            instructor.setGeneratedQRCodeIds((List<String>) document.get("generatedQRCodeIds"));
            user = instructor;
        } else if ("ADMIN".equals(userType)) {
            Admin admin = new Admin();
            admin.setAdminId(document.getString("adminId"));
            admin.setPosition(document.getString("position"));
            if (document.getString("privilegeLevel") != null) {
                admin.setPrivilegeLevel(Admin.AdminPrivilegeLevel.valueOf(document.getString("privilegeLevel")));
            }
            user = admin;
        } else {
            // Default to a basic user if type is not recognized
            return null;
        }

        // Set common User properties
        user.setUserId(document.getString("userId"));
        user.setEmail(document.getString("email"));
        user.setName(document.getString("name"));
        user.setPhoneNumber(document.getString("phoneNumber"));
        user.setProfileImageUrl(document.getString("profileImageUrl"));
        user.setCreatedAt(document.getDate("createdAt"));
        user.setLastLoginAt(document.getDate("lastLoginAt"));
        if (document.getString("role") != null) {
            user.setRole(User.UserRole.valueOf(document.getString("role")));
        }

        return user;
    }

    /**
     * Convert a Course object to a Firestore map
     * @param course The course to convert
     * @return A map representing the course for Firestore storage
     */
    public static Map<String, Object> courseToMap(Course course) {
        Map<String, Object> map = new HashMap<>();
        map.put("courseId", course.getCourseId());
        map.put("courseCode", course.getCourseCode());
        map.put("courseName", course.getCourseName());
        map.put("description", course.getDescription());
        map.put("department", course.getDepartment());
        map.put("semester", course.getSemester());
        map.put("credits", course.getCredits());
        map.put("startDate", course.getStartDate());
        map.put("endDate", course.getEndDate());
        map.put("instructorId", course.getInstructorId());
        map.put("enrolledStudentIds", course.getEnrolledStudentIds());
        map.put("sessionIds", course.getSessionIds());
        map.put("attendanceThreshold", course.getAttendanceThreshold());
        map.put("isActive", course.isActive());
        return map;
    }

    /**
     * Convert a Firestore document to a Course object
     * @param document The Firestore document to convert
     * @return The Course object
     */
    public static Course documentToCourse(DocumentSnapshot document) {
        Course course = new Course();
        course.setCourseId(document.getString("courseId"));
        course.setCourseCode(document.getString("courseCode"));
        course.setCourseName(document.getString("courseName"));
        course.setDescription(document.getString("description"));
        course.setDepartment(document.getString("department"));
        course.setSemester(document.getString("semester"));
        if (document.getLong("credits") != null) {
            course.setCredits(document.getLong("credits").intValue());
        }
        course.setStartDate(document.getDate("startDate"));
        course.setEndDate(document.getDate("endDate"));
        course.setInstructorId(document.getString("instructorId"));
        course.setEnrolledStudentIds((List<String>) document.get("enrolledStudentIds"));
        course.setSessionIds((List<String>) document.get("sessionIds"));
        if (document.getLong("attendanceThreshold") != null) {
            course.setAttendanceThreshold(document.getLong("attendanceThreshold").intValue());
        }
        if (document.getBoolean("isActive") != null) {
            course.setActive(document.getBoolean("isActive"));
        }
        return course;
    }

    /**
     * Convert a Session object to a Firestore map
     * @param session The session to convert
     * @return A map representing the session for Firestore storage
     */
    public static Map<String, Object> sessionToMap(Session session) {
        Map<String, Object> map = new HashMap<>();
        map.put("sessionId", session.getSessionId());
        map.put("courseId", session.getCourseId());
        map.put("title", session.getTitle());
        map.put("description", session.getDescription());
        map.put("startTime", session.getStartTime());
        map.put("endTime", session.getEndTime());
        map.put("location", session.getLocation());
        map.put("qrCodeId", session.getQrCodeId());
        map.put("qrCodeGeneratedAt", session.getQrCodeGeneratedAt());
        map.put("qrCodeExpiresAt", session.getQrCodeExpiresAt());
        map.put("attendingStudentIds", session.getAttendingStudentIds());
        map.put("status", session.getStatus().name());
        return map;
    }

    /**
     * Convert a Firestore document to a Session object
     * @param document The Firestore document to convert
     * @return The Session object
     */
    public static Session documentToSession(DocumentSnapshot document) {
        Session session = new Session();
        session.setSessionId(document.getString("sessionId"));
        session.setCourseId(document.getString("courseId"));
        session.setTitle(document.getString("title"));
        session.setDescription(document.getString("description"));
        session.setStartTime(document.getDate("startTime"));
        session.setEndTime(document.getDate("endTime"));
        session.setLocation(document.getString("location"));
        session.setQrCodeId(document.getString("qrCodeId"));
        session.setQrCodeGeneratedAt(document.getDate("qrCodeGeneratedAt"));
        session.setQrCodeExpiresAt(document.getDate("qrCodeExpiresAt"));
        session.setAttendingStudentIds((List<String>) document.get("attendingStudentIds"));
        if (document.getString("status") != null) {
            session.setStatus(Session.SessionStatus.valueOf(document.getString("status")));
        }
        return session;
    }

    /**
     * Convert a QRCode object to a Firestore map
     * @param qrCode The QR code to convert
     * @return A map representing the QR code for Firestore storage
     */
    public static Map<String, Object> qrCodeToMap(QRCode qrCode) {
        Map<String, Object> map = new HashMap<>();
        map.put("qrCodeId", qrCode.getQrCodeId());
        map.put("sessionId", qrCode.getSessionId());
        map.put("content", qrCode.getContent());
        map.put("imageUrl", qrCode.getImageUrl());
        map.put("generatedAt", qrCode.getGeneratedAt());
        map.put("expiresAt", qrCode.getExpiresAt());
        map.put("isActive", qrCode.isActive());
        map.put("scanCount", qrCode.getScanCount());
        map.put("type", qrCode.getType().name());
        return map;
    }

    /**
     * Convert a Firestore document to a QRCode object
     * @param document The Firestore document to convert
     * @return The QRCode object
     */
    public static QRCode documentToQRCode(DocumentSnapshot document) {
        QRCode qrCode = new QRCode();
        qrCode.setQrCodeId(document.getString("qrCodeId"));
        qrCode.setSessionId(document.getString("sessionId"));
        qrCode.setContent(document.getString("content"));
        qrCode.setImageUrl(document.getString("imageUrl"));
        qrCode.setGeneratedAt(document.getDate("generatedAt"));
        qrCode.setExpiresAt(document.getDate("expiresAt"));
        if (document.getBoolean("isActive") != null) {
            qrCode.setActive(document.getBoolean("isActive"));
        }
        if (document.getLong("scanCount") != null) {
            qrCode.setScanCount(document.getLong("scanCount").intValue());
        }
        if (document.getString("type") != null) {
            qrCode.setType(QRCode.QRCodeType.valueOf(document.getString("type")));
        }
        return qrCode;
    }

    /**
     * Convert an Attendance object to a Firestore map
     * @param attendance The attendance record to convert
     * @return A map representing the attendance record for Firestore storage
     */
    public static Map<String, Object> attendanceToMap(Attendance attendance) {
        Map<String, Object> map = new HashMap<>();
        map.put("attendanceId", attendance.getAttendanceId());
        map.put("studentId", attendance.getStudentId());
        map.put("sessionId", attendance.getSessionId());
        map.put("courseId", attendance.getCourseId());
        map.put("markedAt", attendance.getMarkedAt());
        map.put("status", attendance.getStatus().name());
        map.put("qrCodeId", attendance.getQrCodeId());
        map.put("deviceInfo", attendance.getDeviceInfo());
        map.put("locationInfo", attendance.getLocationInfo());
        map.put("isOfflineMarked", attendance.isOfflineMarked());
        map.put("syncedAt", attendance.getSyncedAt());
        return map;
    }

    /**
     * Convert a Firestore document to an Attendance object
     * @param document The Firestore document to convert
     * @return The Attendance object
     */
    public static Attendance documentToAttendance(DocumentSnapshot document) {
        Attendance attendance = new Attendance();
        attendance.setAttendanceId(document.getString("attendanceId"));
        attendance.setStudentId(document.getString("studentId"));
        attendance.setSessionId(document.getString("sessionId"));
        attendance.setCourseId(document.getString("courseId"));
        attendance.setMarkedAt(document.getDate("markedAt"));
        if (document.getString("status") != null) {
            attendance.setStatus(Attendance.AttendanceStatus.valueOf(document.getString("status")));
        }
        attendance.setQrCodeId(document.getString("qrCodeId"));
        attendance.setDeviceInfo(document.getString("deviceInfo"));
        attendance.setLocationInfo(document.getString("locationInfo"));
        if (document.getBoolean("isOfflineMarked") != null) {
            attendance.setOfflineMarked(document.getBoolean("isOfflineMarked"));
        }
        attendance.setSyncedAt(document.getDate("syncedAt"));
        return attendance;
    }
}