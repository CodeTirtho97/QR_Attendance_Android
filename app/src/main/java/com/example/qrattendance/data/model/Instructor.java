package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Instructor class representing an instructor user in the system.
 * Instructors can create courses, manage class sessions, and generate QR codes for attendance.
 */
public class Instructor extends User {
    private String employeeId;
    private String department;
    private String designation;
    private List<String> coursesIds; // Courses taught by this instructor
    private List<String> generatedQRCodeIds;

    // Default constructor
    public Instructor() {
        super();
        setRole(UserRole.INSTRUCTOR);
        this.coursesIds = new ArrayList<>();
        this.generatedQRCodeIds = new ArrayList<>();
    }

    // Parameterized constructor
    public Instructor(String userId, String email, String name, String phoneNumber,
                      String employeeId, String department, String designation) {
        super(userId, email, name, phoneNumber, UserRole.INSTRUCTOR);
        this.employeeId = employeeId;
        this.department = department;
        this.designation = designation;
        this.coursesIds = new ArrayList<>();
        this.generatedQRCodeIds = new ArrayList<>();
    }

    // Getters and Setters
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

    /**
     * Adds a course ID to the instructor's courses list
     * @param courseId The ID of the course to add
     */
    public void addCourse(String courseId) {
        if (!this.coursesIds.contains(courseId)) {
            this.coursesIds.add(courseId);
        }
    }

    /**
     * Removes a course ID from the instructor's courses list
     * @param courseId The ID of the course to remove
     * @return true if the course was successfully removed, false otherwise
     */
    public boolean removeCourse(String courseId) {
        return this.coursesIds.remove(courseId);
    }

    /**
     * Adds a QR code ID to the instructor's generated QR codes list
     * @param qrCodeId The ID of the QR code to add
     */
    public void addGeneratedQRCode(String qrCodeId) {
        if (!this.generatedQRCodeIds.contains(qrCodeId)) {
            this.generatedQRCodeIds.add(qrCodeId);
        }
    }

    @Override
    public void accessDashboard() {
        // Instructor-specific dashboard logic would be implemented here
        // For now, this is a placeholder
        System.out.println("Accessing Instructor Dashboard");
    }
}