package com.example.TeamLoggerC.dto;

import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.User;

public class LoginApprovalViewDTO {

    private LoginApprovalRequest request;
    private User employee;
    private long delayMinutes;

    public LoginApprovalViewDTO() {
    }

    public LoginApprovalViewDTO(
            LoginApprovalRequest request,
            User employee,
            long delayMinutes
    ) {
        this.request = request;
        this.employee = employee;
        this.delayMinutes = delayMinutes;
    }

    public LoginApprovalRequest getRequest() {
        return request;
    }

    public void setRequest(LoginApprovalRequest request) {
        this.request = request;
    }

    public User getEmployee() {
        return employee;
    }

    public void setEmployee(User employee) {
        this.employee = employee;
    }

    public long getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(long delayMinutes) {
        this.delayMinutes = delayMinutes;
    }
}