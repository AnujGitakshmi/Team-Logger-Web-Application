package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "access_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_access_policy_admin_name",
                        columnNames = {"admin_id", "name"}
                )
        }
)
public class AccessPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Admin who owns this policy
    @Column(name = "admin_id", nullable = false)
    private Integer adminId;

    // Policy name
    @Column(
            name = "name",
            nullable = false,
            length = 100
    )
    private String name;

    // Optional description
    @Column(
            name = "description",
            length = 500
    )
    private String description;

    // ACTIVE / INACTIVE
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "ACTIVE";

    // Created timestamp
    @Column(
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;

    // Updated timestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "team_id")
    private Integer teamId;


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public AccessPolicy() {
    }

    public AccessPolicy(
            Integer adminId,
            String name,
            String description
    ) {
        this.adminId = adminId;
        this.name = name;
        this.description = description;
        this.status = "ACTIVE";
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }


    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        this.createdAt = now;
        this.updatedAt = now;

        if (this.status == null || this.status.isBlank()) {
            this.status = "ACTIVE";
        }
    }

    @PreUpdate
    protected void onUpdate() {

        this.updatedAt = LocalDateTime.now();
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

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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