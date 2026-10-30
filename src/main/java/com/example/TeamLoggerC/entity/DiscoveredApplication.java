 package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "discovered_applications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_discovered_app_admin_app_exe",
                        columnNames = {
                                "admin_id",
                                "application_name",
                                "executable_name"
                        }
                )
        }
)
public class DiscoveredApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Administrator / organization that owns
     * this discovered application record.
     */
    @Column(
            name = "admin_id",
            nullable = false
    )
    private Integer adminId;

    /*
     * Friendly application name.
     *
     * Example:
     * Google Chrome
     * Microsoft Word
     * IntelliJ IDEA
     * Visual Studio Code
     */
    @Column(
            name = "application_name",
            nullable = false,
            length = 150
    )
    private String applicationName;

    /*
     * Actual Windows executable.
     *
     * Example:
     * chrome.exe
     * WINWORD.EXE
     * idea64.exe
     * Code.exe
     */
    @Column(
            name = "executable_name",
            nullable = false,
            length = 150
    )
    private String executableName;

    /*
     * Application publisher/vendor.
     *
     * Example:
     * Google LLC
     * Microsoft Corporation
     * JetBrains
     */
    @Column(
            name = "publisher",
            length = 150
    )
    private String publisher;

    /*
     * Detected application version.
     */
    @Column(
            name = "version",
            length = 100
    )
    private String version;

    /*
     * Number of employees/devices on which
     * this application has been discovered.
     */
    @Column(
            name = "discovery_count",
            nullable = false
    )
    private Integer discoveryCount = 0;

    /*
     * First time TeamLogger discovered
     * this application.
     */
    @Column(
            name = "first_seen_at",
            nullable = false
    )
    private LocalDateTime firstSeenAt;

    /*
     * Most recent time TeamLogger received
     * information about this application.
     */
    @Column(
            name = "last_seen_at",
            nullable = false
    )
    private LocalDateTime lastSeenAt;

    /*
     * Whether this discovered application
     * should currently be shown to admins.
     */
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private String status = "ACTIVE";


    // =========================================================
    // CONSTRUCTORS
    // =========================================================

    public DiscoveredApplication() {
    }


    public DiscoveredApplication(
            Integer adminId,
            String applicationName,
            String executableName
    ) {
        this.adminId = adminId;
        this.applicationName = applicationName;
        this.executableName = executableName;
        this.discoveryCount = 0;
        this.status = "ACTIVE";
    }


    // =========================================================
    // JPA LIFECYCLE
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


    public String getApplicationName() {
        return applicationName;
    }


    public void setApplicationName(
            String applicationName
    ) {
        this.applicationName =
                applicationName;
    }


    public String getExecutableName() {
        return executableName;
    }


    public void setExecutableName(
            String executableName
    ) {
        this.executableName =
                executableName;
    }


    public String getPublisher() {
        return publisher;
    }


    public void setPublisher(
            String publisher
    ) {
        this.publisher =
                publisher;
    }


    public String getVersion() {
        return version;
    }


    public void setVersion(
            String version
    ) {
        this.version =
                version;
    }


    public Integer getDiscoveryCount() {
        return discoveryCount;
    }


    public void setDiscoveryCount(
            Integer discoveryCount
    ) {
        this.discoveryCount =
                discoveryCount;
    }


    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }


    public void setFirstSeenAt(
            LocalDateTime firstSeenAt
    ) {
        this.firstSeenAt =
                firstSeenAt;
    }


    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }


    public void setLastSeenAt(
            LocalDateTime lastSeenAt
    ) {
        this.lastSeenAt =
                lastSeenAt;
    }


    public String getStatus() {
        return status;
    }


    public void setStatus(
            String status
    ) {
        this.status =
                status;
    }
}
