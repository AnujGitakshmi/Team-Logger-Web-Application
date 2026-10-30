package com.example.TeamLoggerC.dto;

public class TodayActivityDTO {

    // =====================================================
    // EMPLOYEE INFORMATION
    // =====================================================

    private Integer employeeId;

    private String employeeName;

    private String employeeLoginId;

    private String designation;


    // =====================================================
    // SESSION INFORMATION
    // =====================================================

    private Integer sessionId;

    private String loginTime;

    private String logoutTime;


    // =====================================================
    // WORK ACTIVITY
    // =====================================================

    private long activeSeconds;

    private long idleSeconds;

    private String activeTime;

    private String idleTime;


    // =====================================================
    // CURRENT APPLICATION
    // =====================================================

    private String currentApplication;

    private String currentWindow;


    // =====================================================
    // CURRENT STATUS
    // =====================================================

    private String status;


    // =====================================================
    // INPUT ACTIVITY
    // =====================================================

    private long mouseClicks;

    private long keyboardActivity;

    private long mouseMovement;


    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public TodayActivityDTO() {
    }


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }


    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
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


    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }


    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }


    public String getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(String logoutTime) {
        this.logoutTime = logoutTime;
    }


    public long getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(long activeSeconds) {
        this.activeSeconds = activeSeconds;
    }


    public long getIdleSeconds() {
        return idleSeconds;
    }

    public void setIdleSeconds(long idleSeconds) {
        this.idleSeconds = idleSeconds;
    }


    public String getActiveTime() {
        return activeTime;
    }

    public void setActiveTime(String activeTime) {
        this.activeTime = activeTime;
    }


    public String getIdleTime() {
        return idleTime;
    }

    public void setIdleTime(String idleTime) {
        this.idleTime = idleTime;
    }


    public String getCurrentApplication() {
        return currentApplication;
    }

    public void setCurrentApplication(String currentApplication) {
        this.currentApplication = currentApplication;
    }


    public String getCurrentWindow() {
        return currentWindow;
    }

    public void setCurrentWindow(String currentWindow) {
        this.currentWindow = currentWindow;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public long getMouseClicks() {
        return mouseClicks;
    }

    public void setMouseClicks(long mouseClicks) {
        this.mouseClicks = mouseClicks;
    }


    public long getKeyboardActivity() {
        return keyboardActivity;
    }

    public void setKeyboardActivity(long keyboardActivity) {
        this.keyboardActivity = keyboardActivity;
    }


    public long getMouseMovement() {
        return mouseMovement;
    }

    public void setMouseMovement(long mouseMovement) {
        this.mouseMovement = mouseMovement;
    }
}