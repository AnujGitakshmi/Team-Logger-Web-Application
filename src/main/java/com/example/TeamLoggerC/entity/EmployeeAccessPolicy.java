package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_access_policies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_access_policy_employee",
                        columnNames = {"employee_id"}
                )
        }
)
public class EmployeeAccessPolicy {

    // =====================================================
    // ID
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =====================================================
    // EMPLOYEE
    // =====================================================

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;


    // =====================================================
    // ACCESS POLICY
    // =====================================================

    @Column(name = "policy_id", nullable = false)
    private Long policyId;


    // =====================================================
    // ASSIGNED BY
    // =====================================================

    @Column(name = "assigned_by", nullable = false)
    private Integer assignedBy;


    // =====================================================
    // ASSIGNED AT
    // =====================================================

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;


    // =====================================================
    // STATUS
    // =====================================================

    @Column(name = "status", nullable = false, length = 20)
    private String status = "ACTIVE";


    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public EmployeeAccessPolicy() {
    }


    public EmployeeAccessPolicy(
            Integer employeeId,
            Long policyId,
            Integer assignedBy
    ) {

        this.employeeId = employeeId;
        this.policyId = policyId;
        this.assignedBy = assignedBy;
        this.assignedAt = LocalDateTime.now();
        this.status = "ACTIVE";
    }


    // =====================================================
    // PRE PERSIST
    // =====================================================

    @PrePersist
    protected void onCreate() {

        if (assignedAt == null) {
            assignedAt = LocalDateTime.now();
        }

        if (status == null || status.isBlank()) {
            status = "ACTIVE";
        }
    }


    // =====================================================
    // GETTERS
    // =====================================================

    public Long getId() {
        return id;
    }


    public Integer getEmployeeId() {
        return employeeId;
    }


    public Long getPolicyId() {
        return policyId;
    }


    public Integer getAssignedBy() {
        return assignedBy;
    }


    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }


    public String getStatus() {
        return status;
    }


    // =====================================================
    // SETTERS
    // =====================================================

    public void setId(Long id) {
        this.id = id;
    }


    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }


    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }


    public void setAssignedBy(Integer assignedBy) {
        this.assignedBy = assignedBy;
    }


    public void setAssignedAt(LocalDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }


    public void setStatus(String status) {
        this.status = status;
    }
}