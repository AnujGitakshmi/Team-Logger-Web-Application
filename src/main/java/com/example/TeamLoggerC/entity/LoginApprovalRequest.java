package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_approval_requests")
public class LoginApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =====================================================
    // EMPLOYEE / ADMIN
    // =====================================================

    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    @Column(name = "admin_id", nullable = false)
    private Integer adminId;


    // =====================================================
    // LOGIN INFORMATION
    // =====================================================

    @Column(name = "login_time", nullable = false)
    private LocalDateTime loginTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;


    // =====================================================
    // APPROVAL REASON
    // =====================================================

    /**
     * Reason why administrator approval is required.
     *
     * Possible values:
     *
     * LATE_LOGIN
     * UNKNOWN_IP
     * LATE_LOGIN,UNKNOWN_IP
     */
    @Column(
            name = "approval_reason",
            nullable = false,
            length = 100
    )
    private String approvalReason;


    // =====================================================
    // ATTEMPTED IP ADDRESS
    // =====================================================

    /**
     * IP address from which the employee attempted
     * to log in.
     *
     * Maximum IPv6 length is 45 characters.
     */
    @Column(
            name = "attempted_ip_address",
            length = 45
    )
    private String attemptedIpAddress;


    // =====================================================
    // APPROXIMATE IP LOCATION
    // =====================================================

    /**
     * These fields represent approximate IP-based
     * geolocation.
     *
     * They are NOT GPS coordinates.
     */

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "state", length = 150)
    private String state;

    @Column(name = "city", length = 150)
    private String city;

    @Column(name = "isp", length = 255)
    private String isp;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;


    // =====================================================
    // PROCESSING INFORMATION
    // =====================================================

    @Column(name = "processed_at")
    private LocalDateTime processedAt;


    // =====================================================
    // REJECTED FOR TODAY
    // =====================================================

    /**
     * When an administrator rejects the login request,
     * the employee should not be allowed to create another
     * successful login from the same rejected condition
     * for the rest of that day.
     *
     * This flag represents that the rejection has been
     * applied to today's login attempt.
     */
    @Column(
            name = "rejected_for_day",
            nullable = false
    )
    private boolean rejectedForDay = false;


    // =====================================================
    // APPROVAL TOKEN
    // =====================================================

    /*
     * Random token generated when the login approval
     * request is created.
     *
     * The same token remains attached to the request while
     * the request changes:
     *
     * PENDING -> APPROVED
     *
     * This token is NOT the request ID.
     */
    @Column(
            name = "approval_token",
            unique = true,
            length = 128
    )
    private String approvalToken;


    // =====================================================
    // TOKEN EXPIRATION
    // =====================================================

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;


    // =====================================================
    // TOKEN USED TIME
    // =====================================================

    @Column(name = "token_used_at")
    private LocalDateTime tokenUsedAt;


    // =====================================================
    // REASON CONSTANTS
    // =====================================================

    public static final String REASON_LATE_LOGIN =
            "LATE_LOGIN";

    public static final String REASON_UNKNOWN_IP =
            "UNKNOWN_IP";


    // =====================================================
    // STATUS CONSTANTS
    // =====================================================

    public static final String STATUS_PENDING =
            "PENDING";

    public static final String STATUS_APPROVED =
            "APPROVED";

    public static final String STATUS_REJECTED =
            "REJECTED";


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LoginApprovalRequest() {
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
    // EMPLOYEE ID
    // =====================================================

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }


    // =====================================================
    // ADMIN ID
    // =====================================================

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    // =====================================================
    // LOGIN TIME
    // =====================================================

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(LocalDateTime loginTime) {
        this.loginTime = loginTime;
    }


    // =====================================================
    // STATUS
    // =====================================================

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    // =====================================================
    // APPROVAL REASON
    // =====================================================

    public String getApprovalReason() {
        return approvalReason;
    }

    public void setApprovalReason(String approvalReason) {
        this.approvalReason = approvalReason;
    }


    // =====================================================
    // ATTEMPTED IP ADDRESS
    // =====================================================

    public String getAttemptedIpAddress() {
        return attemptedIpAddress;
    }

    public void setAttemptedIpAddress(String attemptedIpAddress) {
        this.attemptedIpAddress = attemptedIpAddress;
    }


    // =====================================================
    // COUNTRY
    // =====================================================

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }


    // =====================================================
    // STATE
    // =====================================================

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    // =====================================================
    // CITY
    // =====================================================

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    // =====================================================
    // ISP
    // =====================================================

    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }


    // =====================================================
    // LATITUDE
    // =====================================================

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }


    // =====================================================
    // LONGITUDE
    // =====================================================

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }


    // =====================================================
    // PROCESSED AT
    // =====================================================

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }


    // =====================================================
    // REJECTED FOR DAY
    // =====================================================

    public boolean isRejectedForDay() {
        return rejectedForDay;
    }

    public void setRejectedForDay(boolean rejectedForDay) {
        this.rejectedForDay = rejectedForDay;
    }


    // =====================================================
    // APPROVAL TOKEN
    // =====================================================

    public String getApprovalToken() {
        return approvalToken;
    }

    public void setApprovalToken(String approvalToken) {
        this.approvalToken = approvalToken;
    }


    // =====================================================
    // TOKEN EXPIRES AT
    // =====================================================

    public LocalDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(LocalDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }


    // =====================================================
    // TOKEN USED AT
    // =====================================================

    public LocalDateTime getTokenUsedAt() {
        return tokenUsedAt;
    }

    public void setTokenUsedAt(LocalDateTime tokenUsedAt) {
        this.tokenUsedAt = tokenUsedAt;
    }
}

