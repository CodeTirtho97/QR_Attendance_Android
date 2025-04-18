package com.example.qrattendance.data.model;

public class Admin extends User {
    private String adminId;
    private String position;
    private AdminPrivilegeLevel privilegeLevel;

    public enum AdminPrivilegeLevel {
        SUPER_ADMIN,
        DEPARTMENT_ADMIN,
        COURSE_ADMIN
    }

    // Default constructor
    public Admin() {
        super();
        setRole(UserRole.ADMIN);
        this.privilegeLevel = AdminPrivilegeLevel.COURSE_ADMIN; // Default privilege level
    }

    // Constructor with parameters
    public Admin(String userId, String email, String name, String phoneNumber,
                 String adminId, String position, AdminPrivilegeLevel privilegeLevel) {
        super(userId, email, name, phoneNumber, UserRole.ADMIN);
        this.adminId = adminId;
        this.position = position;
        this.privilegeLevel = privilegeLevel;
    }

    // Getters and setters
    public String getAdminId() {
        return adminId;
    }

    public void setAdminId(String adminId) {
        this.adminId = adminId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public AdminPrivilegeLevel getPrivilegeLevel() {
        return privilegeLevel;
    }

    public void setPrivilegeLevel(AdminPrivilegeLevel privilegeLevel) {
        this.privilegeLevel = privilegeLevel;
    }

    // Check if admin has sufficient privileges for an action
    public boolean hasPrivilege(AdminPrivilegeLevel requiredLevel) {
        if (this.privilegeLevel == AdminPrivilegeLevel.SUPER_ADMIN) {
            return true;
        }

        if (this.privilegeLevel == AdminPrivilegeLevel.DEPARTMENT_ADMIN &&
                requiredLevel == AdminPrivilegeLevel.COURSE_ADMIN) {
            return true;
        }

        return this.privilegeLevel == requiredLevel;
    }

    @Override
    public void accessDashboard() {
        // Admin-specific dashboard logic
    }
}