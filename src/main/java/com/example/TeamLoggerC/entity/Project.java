package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "projects")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "project_name", nullable = false, length = 150)
    private String projectName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "expected_completion_date")
    private LocalDate expectedCompletionDate;

    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "project_leader_id")
    private Integer projectLeaderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "admin_id")
    private Integer adminId;

    @Column(name = "completion_requested_at")
    private LocalDateTime completionRequestedAt;

    @Column(name = "completion_requested_by")
    private Integer completionRequestedBy;

    public Project() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getExpectedCompletionDate() {
        return expectedCompletionDate;
    }

    public void setExpectedCompletionDate(LocalDate expectedCompletionDate) {
        this.expectedCompletionDate = expectedCompletionDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProjectLeaderId() {
        return projectLeaderId;
    }

    public void setProjectLeaderId(Integer projectLeaderId) {
        this.projectLeaderId = projectLeaderId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }
    public LocalDateTime getCompletionRequestedAt() {
        return completionRequestedAt;
    }

    public void setCompletionRequestedAt(LocalDateTime completionRequestedAt) {
        this.completionRequestedAt = completionRequestedAt;
    }

    public Integer getCompletionRequestedBy() {
        return completionRequestedBy;
    }

    public void setCompletionRequestedBy(Integer completionRequestedBy) {
        this.completionRequestedBy = completionRequestedBy;
    }
}