package com.example.TeamLoggerC.dto;

public class LoginResponse {

    private boolean success;
    private String message;
    private Integer userId;
    private String name;
    private String email;
    private String role;
    private String status;

    public LoginResponse() {
    }

    public LoginResponse(
            boolean success,
            String message,
            Integer userId,
            String name,
            String email,
            String role,
            String status
    ) {
        this.success = success;
        this.message = message;
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.role = role;
        this.status = status;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public String getStatus() {
        return status;
    }
}