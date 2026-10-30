package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "website_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_website_permission_policy_domain",
                        columnNames = {
                                "policy_id",
                                "domain"
                        }
                )
        }
)
public class WebsitePermission {

    // =========================================================
    // ID
    // =========================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // ACCESS POLICY
    // =========================================================

    @Column(
            name = "policy_id",
            nullable = false
    )
    private Long policyId;


    // =========================================================
    // WEBSITE DOMAIN
    // =========================================================

    @Column(
            name = "domain",
            nullable = false,
            length = 255
    )
    private String domain;


    // =========================================================
    // PERMISSION
    // =========================================================
    // ALLOW
    // BLOCK
    // RESTRICT

    @Column(
            name = "permission",
            nullable = false,
            length = 20
    )
    private String permission = "ALLOW";


    // =========================================================
    // STATUS
    // =========================================================
    // ACTIVE
    // INACTIVE

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "ACTIVE";


    // =========================================================
    // CREATED / UPDATED
    // =========================================================

    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;


    // =========================================================
    // DEFAULT CONSTRUCTOR
    // =========================================================

    public WebsitePermission() {
    }


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public WebsitePermission(
            Long policyId,
            String domain,
            String permission
    ) {

        this.policyId = policyId;
        this.domain = domain;
        this.permission = permission;
        this.status = "ACTIVE";
    }


    // =========================================================
    // PRE PERSIST
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;


        if (this.permission == null ||
                this.permission.isBlank()) {

            this.permission = "ALLOW";
        }


        if (this.status == null ||
                this.status.isBlank()) {

            this.status = "ACTIVE";
        }
    }


    // =========================================================
    // PRE UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt =
                LocalDateTime.now();
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

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


    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    public void setCreatedAt(
            LocalDateTime createdAt
    ) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
            LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}