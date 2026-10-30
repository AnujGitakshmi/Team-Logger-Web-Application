 package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "office_ip_addresses",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_office_ip_admin",
                        columnNames = "admin_id"
                )
        }
)
public class OfficeIpAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =========================================================
    // ADMIN
    // =========================================================

    /**
     * Admin who owns this office IP configuration.
     *
     * This keeps IP configuration isolated between organizations.
     */
    @Column(
            name = "admin_id",
            nullable = false
    )
    private Integer adminId;


    // =========================================================
    // PRIMARY IP
    // =========================================================

    /**
     * Primary office/public IP manually configured by the admin.
     */
    @Column(
            name = "primary_ip_address",
            nullable = false,
            length = 45
    )
    private String primaryIpAddress;


    // =========================================================
    // STATUS
    // =========================================================

    /**
     * Determines whether IP restriction should use
     * this primary IP configuration.
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
            name = "created_at",
            nullable = false
    )
    private LocalDateTime createdAt;


    @Column(
            name = "updated_at"
    )
    private LocalDateTime updatedAt;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public OfficeIpAddress() {
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


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getPrimaryIpAddress() {
        return primaryIpAddress;
    }

    public void setPrimaryIpAddress(String primaryIpAddress) {
        this.primaryIpAddress =
                primaryIpAddress;
    }


    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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


    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }


    @PreUpdate
    protected void onUpdate() {

        updatedAt =
                LocalDateTime.now();
    }
}

