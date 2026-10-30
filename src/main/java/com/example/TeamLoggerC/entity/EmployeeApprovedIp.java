package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_approved_ips",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_approved_ip",
                        columnNames = {
                                "employee_id",
                                "ip_address"
                        }
                )
        }
)
public class EmployeeApprovedIp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    /**
     * Employee who owns this approved IP.
     *
     * We store the employee ID instead of a direct User
     * relationship so the IP security layer remains simple
     * and isolated from the User entity.
     */
    @Column(
            name = "employee_id",
            nullable = false
    )
    private Integer employeeId;


    // =========================================================
    // ADMIN / ORGANIZATION
    // =========================================================

    /**
     * Admin / organization that approved this IP.
     *
     * This prevents an IP record from being accidentally
     * used across different organizations.
     */
    @Column(
            name = "admin_id",
            nullable = false
    )
    private Integer adminId;


    // =========================================================
    // IP ADDRESS
    // =========================================================

    /**
     * Approved public IP address.
     *
     * Maximum 45 characters supports IPv4 and IPv6.
     */
    @Column(
            name = "ip_address",
            nullable = false,
            length = 45
    )
    private String ipAddress;


    // =========================================================
    // STATUS
    // =========================================================

    /**
     * true  = employee can use this IP
     * false = IP has been revoked/disabled
     */
    @Column(
            name = "enabled",
            nullable = false
    )
    private boolean enabled = true;


    // =========================================================
    // AUDIT
    // =========================================================

    @Column(
            name = "approved_at",
            nullable = false
    )
    private LocalDateTime approvedAt;


    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EmployeeApprovedIp() {
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }


    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (approvedAt == null) {
            approvedAt = now;
        }

        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}

