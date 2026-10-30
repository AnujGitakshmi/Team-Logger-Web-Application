package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "screenshot_settings")
public class ScreenshotSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "admin_id", nullable = false, unique = true)
    private Integer adminId;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes = 5;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ScreenshotSettings() {
    }


    // =========================================================
    // GETTERS AND SETTERS
    // =========================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getIntervalMinutes() {
        return intervalMinutes;
    }

    public void setIntervalMinutes(Integer intervalMinutes) {
        this.intervalMinutes = intervalMinutes;
    }
}