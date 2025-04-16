package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Student class representing a student user in the system.
 * Students can enroll in courses and mark their attendance by scanning QR codes.
 */
public class Student extends User {
    private String rollNumber;
    private String department;
    private String semester;
    private String batch;
    private List<String> enrolledCourseIds;
    private List<String> attendanceRecordIds;

    // Default constructor
    public Student() {
        super();
        setRole(UserRole.STUDENT);
        this.enrolledCourseIds = new ArrayList<>();
        this.attendanceRecordIds = new ArrayList<>();
    }

    // Parameterized constructor
    public Student(String userId, String email, String name, String phoneNumber,
                   String rollNumber, String department, String semester, String batch) {
        super(userId, email, name, phoneNumber, UserRole.STUDENT);
        this.rollNumber = rollNumber;
        this.department = department;
        this.semester = semester;
        this.batch = batch;
        this.enrolledCourseIds = new ArrayList<>();
        this.attendanceRecordIds = new ArrayList<>();
    }

    // Getters and Setters
    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getBatch() {
        return batch;
    }

    public void setBatch(String batch) {
        this.batch = batch;
    }

    public List<String> getEnrolledCourseIds() {
        return enrolledCourseIds;
    }

    public void setEnrolledCourseIds(List<String> enrolledCourseIds) {
        this.enrolledCourseIds = enrolledCourseIds;
    }

    public List<String> getAttendanceRecordIds() {
        return attendanceRecordIds;
    }

    public void setAttendanceRecordIds(List<String> attendanceRecordIds) {
        this.attendanceRecordIds = attendanceRecordIds;
    }

    /**
     * Adds a course ID to the student's enrolled courses list
     * @param courseId The ID of the course to enroll in
     */
    public void enrollInCourse(String courseId) {
        if (!this.enrolledCourseIds.contains(courseId)) {
            this.enrolledCourseIds.add(courseId);
        }
    }

    /**
     * Removes a course ID from the student's enrolled courses list
     * @param courseId The ID of the course to un-enroll from
     * @return true if the course was successfully un-enrolled, false otherwise
     */
    public boolean unenrollFromCourse(String courseId) {
        return this.enrolledCourseIds.remove(courseId);
    }

    /**
     * Adds an attendance record ID to the student's attendance history
     * @param attendanceId The ID of the attendance record to add
     */
    public void addAttendanceRecord(String attendanceId) {
        if (!this.attendanceRecordIds.contains(attendanceId)) {
            this.attendanceRecordIds.add(attendanceId);
        }
    }

    @Override
    public void accessDashboard() {
        // Student-specific dashboard logic would be implemented here
        // For now, this is a placeholder
        System.out.println("Accessing Student Dashboard");
    }
}