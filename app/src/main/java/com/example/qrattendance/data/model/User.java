package com.example.qrattendance.data.model;

import java.util.Date;

public abstract class User {
    private String userId;
    private String email;
    private String name;
    private String phoneNumber;
    private String profileImageUrl;
    private Date createdAt;
    private Date lastLoginAt;
    private UserRole role;

    public enum UserRole {
        STUDENT("STUDENT"),
        INSTRUCTOR("INSTRUCTOR"),
        ADMIN("ADMIN");

        private final String value;

        UserRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    // Default constructor
    public User() {
        this.createdAt = new Date();
    }

    // Constructor with parameters
    public User(String userId, String email, String name, String phoneNumber, UserRole role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.role = role;
        this.createdAt = new Date();
    }

    // Getters and setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(Date lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    // Abstract method to ensure each user type has specific dashboard behavior
    public abstract void accessDashboard();
}