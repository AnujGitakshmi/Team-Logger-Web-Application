package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;


@Entity
@Table(
        name = "employee_ip_access",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_employee_ip",
                        columnNames = {"employee_id", "ip_address"}
                )
        }
)
public class EmployeeIpAccess {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;


    // =====================================================
    // EMPLOYEE / ORGANIZATION
    // =====================================================

    /**
     * Employee whose IP access this record belongs to.
     *
     * We intentionally store the ID instead of using
     * @ManyToOne User so the access record remains simple
     * and avoids unnecessary entity coupling.
     */
    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    /**
     * Admin/organization owner of this employee.
     *
     * This helps maintain organization-level data isolation.
     */
    @Column(name = "admin_id", nullable = false)
    private Integer adminId;


    // =====================================================
    // IP ACCESS
    // =====================================================

    /**
     * IPv4 or IPv6 address.
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    /**
     * OFFICE or SECONDARY
     */
    @Column(name = "access_type", nullable = false, length = 20)
    private String accessType;

    /**
     * PENDING / APPROVED / REJECTED / REVOKED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;


    // =====================================================
    // IP LOCATION
    // =====================================================

    /**
     * Country returned by IP geolocation service.
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * State/region returned by IP geolocation service.
     */
    @Column(name = "state", length = 150)
    private String state;

    /**
     * Approximate city returned by IP geolocation service.
     */
    @Column(name = "city", length = 150)
    private String city;

    /**
     * Internet Service Provider / network organization.
     */
    @Column(name = "isp", length = 255)
    private String isp;

    /**
     * Approximate latitude.
     *
     * This is IP-based approximate information,
     * NOT exact GPS location.
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Approximate longitude.
     *
     * This is IP-based approximate information,
     * NOT exact GPS location.
     */
    @Column(name = "longitude")
    private Double longitude;


    // =====================================================
    // APPROVAL INFORMATION
    // =====================================================

    /**
     * Admin who approved/rejected/revoked this access.
     */
    @Column(name = "approved_by")
    private Integer approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;


    // =====================================================
    // TIMESTAMPS
    // =====================================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Last time this IP was successfully used
     * for employee authentication.
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    // =====================================================
// USAGE SECURITY POLICY
// =====================================================

    /**
     * Access policy applied when this employee is using
     * this approved IP address.
     *
     * This is intentionally nullable because an IP can be
     * approved for login without having a usage policy assigned.
     */
    @Column(name = "policy_id")
    private Long policyId;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeIpAccess() {
    }


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }


    public String getAccessType() {
        return accessType;
    }

    public void setAccessType(String accessType) {
        this.accessType = accessType;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }


    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }


    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }


    public String getIsp() {
        return isp;
    }

    public void setIsp(String isp) {
        this.isp = isp;
    }


    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }


    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }


    public Integer getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(Integer approvedBy) {
        this.approvedBy = approvedBy;
    }


    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}

