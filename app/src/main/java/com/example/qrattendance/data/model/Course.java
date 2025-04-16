package com.example.qrattendance.data.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Course class representing a course in the system.
 * A course has an instructor, enrolled students, and scheduled sessions.
 */
public class Course {
    private String courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private String department;
    private String semester;
    private int credits;
    private Date startDate;
    private Date endDate;
    private String instructorId;
    private List<String> enrolledStudentIds;
    private List<String> sessionIds;
    private int attendanceThreshold; // Minimum attendance percentage required (e.g., 75)
    private boolean isActive;

    // Default constructor
    public Course() {
        this.enrolledStudentIds = new ArrayList<>();
        this.sessionIds = new ArrayList<>();
        this.attendanceThreshold = 75; // Default attendance threshold
        this.isActive = true;
    }

    // Parameterized constructor
    public Course(String courseId, String courseCode, String courseName, String description,
                  String department, String semester, int credits, Date startDate, Date endDate,
                  String instructorId) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.department = department;
        this.semester = semester;
        this.credits = credits;
        this.startDate = startDate;
        this.endDate = endDate;
        this.instructorId = instructorId;
        this.enrolledStudentIds = new ArrayList<>();
        this.sessionIds = new ArrayList<>();
        this.attendanceThreshold = 75; // Default attendance threshold
        this.isActive = true;
    }

    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public int getCredits() {
        return credits;
    }

    public void setCredits(int credits) {
        this.credits = credits;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getInstructorId() {
        return instructorId;
    }

    public void setInstructorId(String instructorId) {
        this.instructorId = instructorId;
    }

    public List<String> getEnrolledStudentIds() {
        return enrolledStudentIds;
    }

    public void setEnrolledStudentIds(List<String> enrolledStudentIds) {
        this.enrolledStudentIds = enrolledStudentIds;
    }

    public List<String> getSessionIds() {
        return sessionIds;
    }

    public void setSessionIds(List<String> sessionIds) {
        this.sessionIds = sessionIds;
    }

    public int getAttendanceThreshold() {
        return attendanceThreshold;
    }

    public void setAttendanceThreshold(int attendanceThreshold) {
        this.attendanceThreshold = attendanceThreshold;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    /**
     * Adds a student ID to the course's enrolled students list
     * @param studentId The ID of the student to enroll
     * @return true if the student was successfully enrolled, false if already enrolled
     */
    public boolean enrollStudent(String studentId) {
        if (!this.enrolledStudentIds.contains(studentId)) {
            this.enrolledStudentIds.add(studentId);
            return true;
        }
        return false;
    }

    /**
     * Removes a student ID from the course's enrolled students list
     * @param studentId The ID of the student to un-enroll
     * @return true if the student was successfully un-enrolled, false otherwise
     */
    public boolean unenrollStudent(String studentId) {
        return this.enrolledStudentIds.remove(studentId);
    }

    /**
     * Adds a session ID to the course's sessions list
     * @param sessionId The ID of the session to add
     */
    public void addSession(String sessionId) {
        if (!this.sessionIds.contains(sessionId)) {
            this.sessionIds.add(sessionId);
        }
    }

    /**
     * Removes a session ID from the course's sessions list
     * @param sessionId The ID of the session to remove
     * @return true if the session was successfully removed, false otherwise
     */
    public boolean removeSession(String sessionId) {
        return this.sessionIds.remove(sessionId);
    }
}