 package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_history")
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * User ID associated with this login event.
     *
     * Nullable because a login attempt can be made
     * with an email that does not exist.
     */
    @Column(name = "user_id")
    private Integer userId;

    /*
     * Email submitted during the login attempt.
     *
     * Useful for failed attempts where the user
     * does not exist in the database.
     */
    @Column(name = "email")
    private String email;

    /*
     * LOGIN or LOGOUT
     */
    @Column(name = "event_type", nullable = false)
    private String eventType;

    /*
     * SUCCESS / FAILED / LOCKED
     */
    @Column(name = "status", nullable = false)
    private String status;

    /*
     * Example:
     *
     * BAD_CREDENTIALS
     * ACCOUNT_LOCKED
     * ACCOUNT_DISABLED
     * USER_NOT_FOUND
     * MAX_FAILED_ATTEMPTS
     */
    @Column(name = "failure_reason")
    private String failureReason;

    /*
     * Login attempt timestamp.
     */
    @Column(name = "login_time")
    private LocalDateTime loginTime;

    /*
     * Logout timestamp.
     *
     * Only applicable to logout events.
     */
    @Column(name = "logout_time")
    private LocalDateTime logoutTime;

    /*
     * Client IP address.
     */
    @Column(name = "ip_address")
    private String ipAddress;

    /*
     * Browser / client user-agent string.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /*
     * Basic device information.
     */
    @Column(name = "device_info")
    private String deviceInfo;

    public LoginHistory() {
    }

    public Long getId() {
        return id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
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

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(String deviceInfo) {
        this.deviceInfo = deviceInfo;
    }
}
