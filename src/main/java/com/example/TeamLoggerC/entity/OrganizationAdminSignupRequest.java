
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(
        name = "organization_admin_signup_requests",
        indexes = {
                @Index(
                        name = "idx_org_admin_request_org_status",
                        columnList = "organization_id, status"
                ),
                @Index(
                        name = "idx_org_admin_request_email",
                        columnList = "email"
                )
        }
)
public class OrganizationAdminSignupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_name", nullable = false, length = 100)
    private String adminName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    // Store only the BCrypt-hashed password.
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    // The exact organization this applicant selected.
    @Column(name = "organization_id", nullable = false)
    private Integer organizationId;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "PENDING";

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by_admin_id")
    private Integer processedByAdminId;

    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    public OrganizationAdminSignupRequest() {
    }

    @PrePersist
    protected void onCreate() {

        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        if (email != null) {
            email = email.trim().toLowerCase(Locale.ROOT);
        }

        if (adminName != null) {
            adminName = adminName.trim();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getAdminName() {
        return adminName;
    }

    public void setAdminName(String adminName) {
        this.adminName = adminName;
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

    public Integer getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(Integer organizationId) {
        this.organizationId = organizationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public Integer getProcessedByAdminId() {
        return processedByAdminId;
    }

    public void setProcessedByAdminId(Integer processedByAdminId) {
        this.processedByAdminId = processedByAdminId;
    }

    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(String decisionNote) {
        this.decisionNote = decisionNote;
    }
}
