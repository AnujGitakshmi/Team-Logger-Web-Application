package com.example.TeamLoggerC.dto;

public class ReportingManagerDTO {

    private Integer id;

    private String name;

    private String employeeLoginId;

    private String designation;


    public ReportingManagerDTO() {
    }


    public ReportingManagerDTO(
            Integer id,
            String name,
            String employeeLoginId,
            String designation) {

        this.id = id;
        this.name = name;
        this.employeeLoginId = employeeLoginId;
        this.designation = designation;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getEmployeeLoginId() {
        return employeeLoginId;
    }

    public void setEmployeeLoginId(String employeeLoginId) {
        this.employeeLoginId = employeeLoginId;
    }


    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }
}