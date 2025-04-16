package com.example.qrattendance.data.model;

/**
 * Admin class representing an administrator user in the system.
 * Admins can manage users, courses, and system settings.
 */
public class Admin extends User {
    private String adminId;
    private String position;
    private AdminPrivilegeLevel privilegeLevel;

    /**
     * Enum representing the different privilege levels an admin can have
     */
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

    // Parameterized constructor
    public Admin(String userId, String email, String name, String phoneNumber,
                 String adminId, String position, AdminPrivilegeLevel privilegeLevel) {
        super(userId, email, name, phoneNumber, UserRole.ADMIN);
        this.adminId = adminId;
        this.position = position;
        this.privilegeLevel = privilegeLevel;
    }

    // Getters and Setters
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

    /**
     * Checks if the admin has sufficient privileges to perform a certain action
     * @param requiredLevel The privilege level required for the action
     * @return true if the admin has sufficient privileges, false otherwise
     */
    public boolean hasPrivilege(AdminPrivilegeLevel requiredLevel) {
        // Super admin has all privileges
        if (this.privilegeLevel == AdminPrivilegeLevel.SUPER_ADMIN) {
            return true;
        }

        // Department admin has department and course level privileges
        if (this.privilegeLevel == AdminPrivilegeLevel.DEPARTMENT_ADMIN &&
                requiredLevel == AdminPrivilegeLevel.COURSE_ADMIN) {
            return true;
        }

        // Otherwise, check if the admin has exactly the required privilege level
        return this.privilegeLevel == requiredLevel;
    }

    @Override
    public void accessDashboard() {
        // Admin-specific dashboard logic would be implemented here
        // For now, this is a placeholder
        System.out.println("Accessing Admin Dashboard");
    }
}