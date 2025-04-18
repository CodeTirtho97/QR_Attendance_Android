package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.List;

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

    // Constructor with parameters
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

    // Getters and setters
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

    // Enroll in a course
    public void enrollInCourse(String courseId) {
        if (!this.enrolledCourseIds.contains(courseId)) {
            this.enrolledCourseIds.add(courseId);
        }
    }

    // Unenroll from a course
    public boolean unenrollFromCourse(String courseId) {
        return this.enrolledCourseIds.remove(courseId);
    }

    // Add an attendance record
    public void addAttendanceRecord(String attendanceId) {
        if (!this.attendanceRecordIds.contains(attendanceId)) {
            this.attendanceRecordIds.add(attendanceId);
        }
    }

    @Override
    public void accessDashboard() {
        // Student-specific dashboard logic
    }
}