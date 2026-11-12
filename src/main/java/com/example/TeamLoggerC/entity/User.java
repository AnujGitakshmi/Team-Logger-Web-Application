 package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "workstation_activated", nullable = false)
    private boolean workstationActivated = false;

    /**
     * IMPORTANT:
     * This field must always contain a HASHED password.
     * Never store the user's plain-text password here.
     */
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Timestamp of the most recent password change.
     */
    @Column(name = "password_changed_at")
    private LocalDateTime passwordChangedAt;

    /**
     * Incremented whenever the password is changed.
     *
     * Can later be used to invalidate existing sessions/tokens.
     */
    @Column(name = "password_version")
    private Integer passwordVersion = 1;

    /**
     * If true, the user must change their password
     * before continuing to use the application.
     */
    @Column(name = "password_must_change", nullable = false)
    private boolean passwordMustChange = false;

    /**
     * Number of consecutive failed login attempts.
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    /**
     * Indicates whether the account is currently locked.
     */
    @Column(name = "account_locked", nullable = false)
    private boolean accountLocked = false;

    /**
     * Timestamp when the account was locked.
     */
    @Column(name = "locked_at")
    private LocalDateTime lockedAt;

    @Column(name = "role", nullable = false)
    private String role;

    @Column(name = "team_id")
    private Integer teamId;

    @Column(name = "admin_id")
    private Integer adminId;

    @Column(name = "application_name")
    private String applicationName;

    @Column(name = "employee_login_id", unique = true)
    private String employeeLoginId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "disabled_at")
    private LocalDateTime disabledAt;

    @Column(name = "designation")
    private String designation;

    @Column(name = "senior_id")
    private Integer seniorId;

    @Column(name = "scheduled_deletion_at")
    private LocalDateTime scheduledDeletionAt;

    /*
     * Example:
     * Development
     * QA
     * HR
     * Finance
     * UI/UX
     */
    @Column(name = "domain")
    private String domain;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "organization_id")
    private Integer organizationId;

    @Column(name = "branch_id")
    private Integer branchId;

    @Column(name = "organization_role", length = 30)
    private String organizationRole;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public User() {
    }


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public boolean isWorkstationActivated() {
        return workstationActivated;
    }

    public void setWorkstationActivated(boolean workstationActivated) {
        this.workstationActivated = workstationActivated;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    // =====================================================
    // PASSWORD SECURITY
    // =====================================================

    public LocalDateTime getPasswordChangedAt() {
        return passwordChangedAt;
    }

    public void setPasswordChangedAt(LocalDateTime passwordChangedAt) {
        this.passwordChangedAt = passwordChangedAt;
    }


    public Integer getPasswordVersion() {
        return passwordVersion;
    }

    public void setPasswordVersion(Integer passwordVersion) {
        this.passwordVersion = passwordVersion;
    }


    public boolean isPasswordMustChange() {
        return passwordMustChange;
    }

    public void setPasswordMustChange(boolean passwordMustChange) {
        this.passwordMustChange = passwordMustChange;
    }


    // =====================================================
    // ACCOUNT LOCKOUT
    // =====================================================

    public Integer getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public void setFailedLoginAttempts(Integer failedLoginAttempts) {
        this.failedLoginAttempts = failedLoginAttempts;
    }


    public boolean isAccountLocked() {
        return accountLocked;
    }

    public void setAccountLocked(boolean accountLocked) {
        this.accountLocked = accountLocked;
    }


    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }


    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }


    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }


    public String getEmployeeLoginId() {
        return employeeLoginId;
    }

    public void setEmployeeLoginId(String employeeLoginId) {
        this.employeeLoginId = employeeLoginId;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public LocalDateTime getDisabledAt() {
        return disabledAt;
    }

    public void setDisabledAt(LocalDateTime disabledAt) {
        this.disabledAt = disabledAt;
    }


    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }


    public Integer getSeniorId() {
        return seniorId;
    }

    public void setSeniorId(Integer seniorId) {
        this.seniorId = seniorId;
    }


    public LocalDateTime getScheduledDeletionAt() {
        return scheduledDeletionAt;
    }

    public void setScheduledDeletionAt(
            LocalDateTime scheduledDeletionAt) {

        this.scheduledDeletionAt = scheduledDeletionAt;
    }


    // =====================================================
    // DOMAIN
    // =====================================================

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }
    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public Integer getBranchId() {
        return branchId;
    }

    public void setBranchId(Integer branchId) {
        this.branchId = branchId;
    }

    public String getOrganizationRole() {
        return organizationRole;
    }

    public void setOrganizationRole(String organizationRole) {
        this.organizationRole = organizationRole;
    }
}

