package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    @Column(name = "timezone", nullable = false)
    private String timezone;

    @Column(name = "active_minutes")
    private Integer activeMinutes;

    @Column(name = "idle_minutes")
    private Integer idleMinutes;

    @Column(name = "productivity")
    private Double productivity;

    @Column(name = "office_start")
    private LocalDateTime officeStart;

    @Column(name = "office_end")
    private LocalDateTime officeEnd;

    @Column(name = "work_started_at")
    private LocalDateTime workStartedAt;

    @Column(name = "active_seconds")
    private Integer activeSeconds;

    @Column(name = "idle_seconds")
    private Integer idleSeconds;

    @Column(name = "employee_started_at")
    private LocalDateTime employeeStartedAt;

    @Column(name = "work_paused", nullable = false)
    private boolean workPaused = false;

    public Session() {
    }

    public boolean isWorkPaused() {
        return workPaused;
    }

    public void setWorkPaused(boolean workPaused) {
        this.workPaused = workPaused;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }

    public LocalDateTime getLogoutTime() {
        return logoutTime;
    }

    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Integer getActiveMinutes() {
        return activeMinutes;
    }

    public void setActiveMinutes(Integer activeMinutes) {
        this.activeMinutes = activeMinutes;
    }

    public Integer getIdleMinutes() {
        return idleMinutes;
    }

    public void setIdleMinutes(Integer idleMinutes) {
        this.idleMinutes = idleMinutes;
    }

    public Double getProductivity() {
        return productivity;
    }

    public void setProductivity(Double productivity) {
        this.productivity = productivity;
    }

    public LocalDateTime getOfficeStart() {
        return officeStart;
    }

    public void setOfficeStart(LocalDateTime officeStart) {
        this.officeStart = officeStart;
    }

    public LocalDateTime getOfficeEnd() {
        return officeEnd;
    }

    public void setOfficeEnd(LocalDateTime officeEnd) {
        this.officeEnd = officeEnd;
    }

    public LocalDateTime getWorkStartedAt() {
        return workStartedAt;
    }

    public void setWorkStartedAt(LocalDateTime workStartedAt) {
        this.workStartedAt = workStartedAt;
    }

    public Integer getActiveSeconds() {
        return activeSeconds;
    }

    public void setActiveSeconds(Integer activeSeconds) {
        this.activeSeconds = activeSeconds;
    }

    public Integer getIdleSeconds() {
        return idleSeconds;
    }

    public void setIdleSeconds(Integer idleSeconds) {
        this.idleSeconds = idleSeconds;
    }

    public LocalDateTime getEmployeeStartedAt() {
        return employeeStartedAt;
    }

    public void setEmployeeStartedAt(LocalDateTime employeeStartedAt) {
        this.employeeStartedAt = employeeStartedAt;
    }
}