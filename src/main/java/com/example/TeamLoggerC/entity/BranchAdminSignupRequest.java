
package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "branch_admin_signup_requests")
public class BranchAdminSignupRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_name", nullable = false, length = 100)
    private String adminName;

    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "organization_name", nullable = false, length = 150)
    private String organizationName;

    @Column(name = "branch_name", nullable = false, length = 150)
    private String branchName;

    // Branch location details
    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "state", length = 100)
    private String state;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "address", length = 500)
    private String address;

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

    public BranchAdminSignupRequest() {
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

        if (organizationName != null) {
            organizationName = organizationName.trim();
        }

        if (branchName != null) {
            branchName = branchName.trim();
        }

        if (country != null) country = country.trim();
        if (state != null) state = state.trim();
        if (city != null) city = city.trim();
        if (postalCode != null) postalCode = postalCode.trim();
        if (address != null) address = address.trim();
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

    public String getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(String organizationName) {
        this.organizationName = organizationName;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
