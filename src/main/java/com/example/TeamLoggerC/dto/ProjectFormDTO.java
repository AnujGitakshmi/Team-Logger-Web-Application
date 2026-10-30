package com.example.TeamLoggerC.dto;

import java.time.LocalDate;

public class ProjectFormDTO {

    // ============================================================
    // PROJECT INFORMATION
    // ============================================================

    private String projectName;

    private String description;


    // ============================================================
    // PROJECT DURATION
    // ============================================================

    private LocalDate startDate;

    private LocalDate expectedCompletionDate;


    // ============================================================
    // PROJECT LEADER
    // ============================================================

    private Integer projectLeaderId;


    // ============================================================
    // CONSTRUCTORS
    // ============================================================

    public ProjectFormDTO() {
    }


    // ============================================================
    // GETTERS AND SETTERS
    // ============================================================

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


    public Integer getProjectLeaderId() {
        return projectLeaderId;
    }

    public void setProjectLeaderId(Integer projectLeaderId) {
        this.projectLeaderId = projectLeaderId;
    }
}