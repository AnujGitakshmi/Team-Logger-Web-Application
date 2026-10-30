
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ip_application_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_ip_app_rule",
                        columnNames = {
                                "admin_id",
                                "ip_address",
                                "target_type",
                                "application_name"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_ip_app_rule_lookup",
                        columnList = "admin_id, ip_address, status"
                )
        }
)
public class IpApplicationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    // APPLICATION or WEBSITE
    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType = "APPLICATION";

    // Application name or website domain
    @Column(name = "application_name", nullable = false, length = 255)
    private String applicationName;

    // Used for desktop application rules; can be null for websites
    @Column(name = "executable_name", length = 255)
    private String executableName;

    // BLOCK or ALLOW
    @Column(name = "permission", nullable = false, length = 20)
    private String permission;

    // ACTIVE or INACTIVE
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getExecutableName() {
        return executableName;
    }

    public void setExecutableName(String executableName) {
        this.executableName = executableName;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
