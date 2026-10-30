package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "application_activity")
public class ApplicationActivity {

    // =====================================================
    // PRIMARY KEY
    // =====================================================

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =====================================================
    // SESSION
    // =====================================================

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;


    // =====================================================
    // APPLICATION INFORMATION
    // =====================================================

    @Column(name = "application_name")
    private String applicationName;

    @Column(name = "window_title")
    private String windowTitle;


    // =====================================================
    // TIME INFORMATION
    // =====================================================

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "duration_minutes")
    private Long durationMinutes;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ApplicationActivity() {
    }


    // =====================================================
    // GETTERS AND SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public String getWindowTitle() {
        return windowTitle;
    }

    public void setWindowTitle(String windowTitle) {
        this.windowTitle = windowTitle;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}