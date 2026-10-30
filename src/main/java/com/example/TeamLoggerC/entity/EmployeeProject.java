package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "employee_projects")
public class EmployeeProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "project_id", nullable = false)
    private Integer projectId;

    @Column(name = "assigned_date", nullable = false)
    private LocalDate assignedDate;

    @Column(name = "unassigned_date")
    private LocalDate unassignedDate;

    public EmployeeProject() {
    }

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

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public LocalDate getAssignedDate() {
        return assignedDate;
    }

    public void setAssignedDate(LocalDate assignedDate) {
        this.assignedDate = assignedDate;
    }

    public LocalDate getUnassignedDate() {
        return unassignedDate;
    }

    public void setUnassignedDate(LocalDate unassignedDate) {
        this.unassignedDate = unassignedDate;
    }
}