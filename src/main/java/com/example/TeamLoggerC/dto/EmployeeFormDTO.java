package com.example.TeamLoggerC.dto;

public class EmployeeFormDTO {

    private Integer id;

    private String name;

    private String email;

    private String password;

    private String domain;

    private String designation;

    private Integer teamId;

    private Integer seniorId;

    private String hierarchyLevel;

    private String newTeamName;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeFormDTO() {
    }


    // =====================================================
    // ID
    // =====================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    // =====================================================
    // NAME
    // =====================================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    // =====================================================
    // EMAIL
    // =====================================================

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    // =====================================================
    // PASSWORD
    // =====================================================

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    // =====================================================
    // DOMAIN
    // =====================================================

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }


    // =====================================================
    // DESIGNATION
    // =====================================================

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }


    // =====================================================
    // TEAM
    // =====================================================

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }


    // =====================================================
    // REPORTING SENIOR
    // =====================================================

    public Integer getSeniorId() {
        return seniorId;
    }

    public void setSeniorId(Integer seniorId) {
        this.seniorId = seniorId;
    }


    // =====================================================
    // HIERARCHY LEVEL
    // =====================================================

    public String getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(String hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }


    // =====================================================
    // NEW TEAM NAME
    // =====================================================

    public String getNewTeamName() {
        return newTeamName;
    }

    public void setNewTeamName(String newTeamName) {
        this.newTeamName = newTeamName;
    }
}