package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.List;

public class Instructor extends User {
    private String employeeId;
    private String department;
    private String designation;
    private List<String> coursesIds;
    private List<String> generatedQRCodeIds;

    // Default constructor
    public Instructor() {
        super();
        setRole(UserRole.INSTRUCTOR);
        this.coursesIds = new ArrayList<>();
        this.generatedQRCodeIds = new ArrayList<>();
    }

    // Constructor with parameters
    public Instructor(String userId, String email, String name, String phoneNumber,
                      String employeeId, String department, String designation) {
        super(userId, email, name, phoneNumber, UserRole.INSTRUCTOR);
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.coursesIds = new ArrayList<>();
        this.generatedQRCodeIds = new ArrayList<>();
    }

    // Getters and setters
    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public List<String> getCoursesIds() {
        return coursesIds;
    }

    public void setCoursesIds(List<String> coursesIds) {
        this.coursesIds = coursesIds;
    }

    public List<String> getGeneratedQRCodeIds() {
        return generatedQRCodeIds;
    }

    public void setGeneratedQRCodeIds(List<String> generatedQRCodeIds) {
        this.generatedQRCodeIds = generatedQRCodeIds;
    }

    // Add a course
    public void addCourse(String courseId) {
        if (!this.coursesIds.contains(courseId)) {
            this.coursesIds.add(courseId);
        }
    }

    // Remove a course
    public boolean removeCourse(String courseId) {
        return this.coursesIds.remove(courseId);
    }

    // Add a generated QR code
    public void addGeneratedQRCode(String qrCodeId) {
        if (!this.generatedQRCodeIds.contains(qrCodeId)) {
            this.generatedQRCodeIds.add(qrCodeId);
        }
    }

    @Override
    public void accessDashboard() {
        // Instructor-specific dashboard logic
    }
}