package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_ip_approval_requests"
)
public class EmployeeIpApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =========================================================
    // EMPLOYEE
    // =========================================================

    @Column(
            name = "employee_id",
            nullable = false
    )
    private Integer employeeId;


    // =========================================================
    // ADMIN / ORGANIZATION
    // =========================================================

    @Column(
            name = "admin_id",
            nullable = false
    )
    private Integer adminId;


    // =========================================================
    // REQUESTED IP
    // =========================================================

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
     * Possible values:
     *
     * PENDING
     * APPROVED
     * REJECTED
     */
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "PENDING";


    // =========================================================
    // REQUEST TIME
    // =========================================================

    @Column(
            name = "requested_at",
            nullable = false
    )
    private LocalDateTime requestedAt;


    // =========================================================
    // PROCESSING INFORMATION
    // =========================================================

    @Column(
            name = "processed_at"
    )
    private LocalDateTime processedAt;


    /**
     * Admin who approved/rejected the request.
     */
    @Column(
            name = "processed_by_admin_id"
    )
    private Integer processedByAdminId;


    // =========================================================
    // OPTIONAL REJECTION REASON
    // =========================================================

    @Column(
            name = "decision_note",
            length = 500
    )
    private String decisionNote;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public EmployeeIpApprovalRequest() {
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

    public void setProcessedByAdminId(
            Integer processedByAdminId) {

        this.processedByAdminId =
                processedByAdminId;
    }


    public String getDecisionNote() {
        return decisionNote;
    }

    public void setDecisionNote(
            String decisionNote) {

        this.decisionNote =
                decisionNote;
    }


    // =========================================================
    // JPA LIFECYCLE
    // =========================================================

    @PrePersist
    protected void onCreate() {

        if (requestedAt == null) {
            requestedAt =
                    LocalDateTime.now();
        }

        if (status == null
                || status.isBlank()) {

            status = "PENDING";
        }
    }
}

