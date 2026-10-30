
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "application_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_application_permission_policy_app",
                        columnNames = {"policy_id", "application_name"}
                )
        }
)
public class ApplicationPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private Long policyId;

    @Column(name = "application_name", nullable = false, length = 150)
    private String applicationName;

    @Column(name = "executable_name", length = 150)
    private String executableName;

    /*
     * Supported examples:
     * GAMING, ENTERTAINMENT, SOCIAL_MEDIA, PRODUCTIVITY, OTHER
     */
    @Column(name = "category", length = 50)
    private String category;

    /*
     * ALLOW / BLOCK / RESTRICT
     */
    @Column(name = "permission", nullable = false, length = 20)
    private String permission = "ALLOW";

    /*
     * ACTIVE / INACTIVE
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public ApplicationPermission() {
    }

    /*
     * Backward-compatible constructor.
     */
    public ApplicationPermission(
            Long policyId,
            String applicationName,
            String executableName,
            String permission
    ) {
        this.policyId = policyId;
        this.applicationName = applicationName;
        this.executableName = executableName;
        this.permission = permission;
        this.status = "ACTIVE";
    }

    /*
     * Constructor including category.
     */
    public ApplicationPermission(
            Long policyId,
            String applicationName,
            String executableName,
            String category,
            String permission
    ) {
        this(policyId, applicationName, executableName, permission);
        this.category = category;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.permission == null || this.permission.isBlank()) {
            this.permission = "ALLOW";
        } else {
            this.permission = this.permission.trim().toUpperCase();
        }

        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        } else {
            this.status = this.status.trim().toUpperCase();
        }

        if (this.category != null && this.category.isBlank()) {
            this.category = null;
        } else if (this.category != null) {
            this.category = this.category.trim().toUpperCase();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.category != null) {
            this.category = this.category.isBlank()
                    ? null
                    : this.category.trim().toUpperCase();
        }

        if (this.permission != null) {
            this.permission = this.permission.trim().toUpperCase();
        }

        if (this.status != null) {
            this.status = this.status.trim().toUpperCase();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
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

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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
