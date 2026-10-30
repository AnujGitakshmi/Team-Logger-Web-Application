package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User who performed the action
    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "user_role")
    private String userRole;

    // Example: EMPLOYEE_CREATED, PROJECT_UPDATED
    @Column(name = "action", nullable = false)
    private String action;

    // Example: USER, PROJECT, SETTINGS
    @Column(name = "entity_type")
    private String entityType;

    // ID of the affected record
    @Column(name = "entity_id")
    private String entityId;

    // Human-readable explanation of the action
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // SUCCESS / FAILED
    @Column(name = "status", nullable = false)
    private String status;

    // Client IP address
    @Column(name = "ip_address")
    private String ipAddress;

    // Browser / application information
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // DESKTOP / MOBILE / TABLET / UNKNOWN
    @Column(name = "device_info")
    private String deviceInfo;

    // Time when the action occurred
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public Long getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

