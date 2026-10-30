package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(
        name = "admin_settings",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_admin_settings_admin_id",
                        columnNames = "admin_id"
                )
        }
)
public class AdminSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;


    // =========================================================
    // COMPANY / ORGANIZATION
    // =========================================================

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;


    // =========================================================
    // WORKING HOURS
    // =========================================================

    @Column(name = "working_start_time")
    private LocalTime workingStartTime;

    @Column(name = "working_end_time")
    private LocalTime workingEndTime;


    // =========================================================
    // ACTIVITY TRACKING
    // =========================================================

    @Column(name = "idle_threshold_seconds")
    private Integer idleThresholdSeconds;


    // =========================================================
    // IP ACCESS CONTROL
    // =========================================================

    /**
     * Enables or disables IP restriction for this admin's
     * organization.
     *
     * false = existing login behavior
     * true  = employee IP access must be checked
     */
    @Column(
            name = "ip_restriction_enabled",
            nullable = false
    )
    private boolean ipRestrictionEnabled = false;


    /**
     * Determines whether an employee attempting to log in
     * from an unregistered IP should require admin approval.
     *
     * true = unknown IP goes through Login Approval
     * false = unknown IP is rejected
     */
    @Column(
            name = "unknown_ip_requires_approval",
            nullable = false
    )
    private boolean unknownIpRequiresApproval = true;


    /**
     * Primary company / office IP address.
     *
     * Employees connecting from this IP are trusted
     * automatically when IP restriction is enabled.
     */
    @Column(
            name = "company_ip_address",
            length = 45
    )
    private String companyIpAddress;

    public String getCompanyIpAddress() {
        return companyIpAddress;
    }

    public void setCompanyIpAddress(String companyIpAddress) {
        this.companyIpAddress = companyIpAddress;
    }
// =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AdminSettings() {
    }


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    // =========================================================
    // COMPANY NAME
    // =========================================================

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }


    // =========================================================
    // WORKING HOURS
    // =========================================================

    public LocalTime getWorkingStartTime() {
        return workingStartTime;
    }

    public void setWorkingStartTime(LocalTime workingStartTime) {
        this.workingStartTime = workingStartTime;
    }


    public LocalTime getWorkingEndTime() {
        return workingEndTime;
    }

    public void setWorkingEndTime(LocalTime workingEndTime) {
        this.workingEndTime = workingEndTime;
    }


    // =========================================================
    // ACTIVITY TRACKING
    // =========================================================

    public Integer getIdleThresholdSeconds() {
        return idleThresholdSeconds;
    }

    public void setIdleThresholdSeconds(
            Integer idleThresholdSeconds) {

        this.idleThresholdSeconds =
                idleThresholdSeconds;
    }


    // =========================================================
    // IP ACCESS CONTROL
    // =========================================================

    public boolean isIpRestrictionEnabled() {
        return ipRestrictionEnabled;
    }

    public void setIpRestrictionEnabled(
            boolean ipRestrictionEnabled) {

        this.ipRestrictionEnabled =
                ipRestrictionEnabled;
    }


    public boolean isUnknownIpRequiresApproval() {
        return unknownIpRequiresApproval;
    }

    public void setUnknownIpRequiresApproval(
            boolean unknownIpRequiresApproval) {

        this.unknownIpRequiresApproval =
                unknownIpRequiresApproval;
    }
}

