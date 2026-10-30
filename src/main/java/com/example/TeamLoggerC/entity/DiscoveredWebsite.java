package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "discovered_websites",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_discovered_website_admin_domain",
                        columnNames = {
                                "admin_id",
                                "domain"
                        }
                )
        }
)
public class DiscoveredWebsite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // =========================================================
    // ADMIN
    // =========================================================

    @Column(
            name = "admin_id",
            nullable = false
    )
    private Integer adminId;


    // =========================================================
    // WEBSITE DOMAIN
    // =========================================================

    @Column(
            name = "domain",
            nullable = false,
            length = 255
    )
    private String domain;


    // =========================================================
    // DISPLAY NAME
    // =========================================================

    @Column(
            name = "website_name",
            length = 255
    )
    private String websiteName;


    // =========================================================
    // BROWSER
    // =========================================================

    @Column(
            name = "browser",
            length = 100
    )
    private String browser;


    // =========================================================
    // DISCOVERY COUNT
    // =========================================================

    @Column(
            name = "discovery_count",
            nullable = false
    )
    private Integer discoveryCount = 0;


    // =========================================================
    // FIRST / LAST SEEN
    // =========================================================

    @Column(
            name = "first_seen_at",
            nullable = false
    )
    private LocalDateTime firstSeenAt;

    @Column(
            name = "last_seen_at",
            nullable = false
    )
    private LocalDateTime lastSeenAt;


    // =========================================================
    // STATUS
    // =========================================================

    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "ACTIVE";


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DiscoveredWebsite() {
    }


    public DiscoveredWebsite(
            Integer adminId,
            String domain,
            String websiteName,
            String browser
    ) {

        this.adminId = adminId;
        this.domain = domain;
        this.websiteName = websiteName;
        this.browser = browser;

        this.discoveryCount = 0;

        this.status = "ACTIVE";
    }


    // =========================================================
    // PRE PERSIST
    // =========================================================

    @PrePersist
    protected void onCreate() {

        LocalDateTime now =
                LocalDateTime.now();

        if (this.firstSeenAt == null) {
            this.firstSeenAt = now;
        }

        if (this.lastSeenAt == null) {
            this.lastSeenAt = now;
        }

        if (this.discoveryCount == null) {
            this.discoveryCount = 0;
        }

        if (this.status == null ||
                this.status.isBlank()) {

            this.status = "ACTIVE";
        }
    }


    // =========================================================
    // PRE UPDATE
    // =========================================================

    @PreUpdate
    protected void onUpdate() {

        this.lastSeenAt =
                LocalDateTime.now();
    }


    // =========================================================
    // GETTERS / SETTERS
    // =========================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }


    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }


    public String getWebsiteName() {
        return websiteName;
    }

    public void setWebsiteName(String websiteName) {
        this.websiteName = websiteName;
    }


    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }


    public Integer getDiscoveryCount() {
        return discoveryCount;
    }

    public void setDiscoveryCount(Integer discoveryCount) {
        this.discoveryCount = discoveryCount;
    }


    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public void setFirstSeenAt(
            LocalDateTime firstSeenAt
    ) {
        this.firstSeenAt = firstSeenAt;
    }


    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(
            LocalDateTime lastSeenAt
    ) {
        this.lastSeenAt = lastSeenAt;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
