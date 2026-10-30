package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "input_activity")
public class InputActivity {

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
    // INPUT ACTIVITY
    // =====================================================

    @Column(name = "activity_type", nullable = false)
    private String activityType;


    // =====================================================
    // APPLICATION INFORMATION
    // =====================================================

    @Column(name = "application_name")
    private String applicationName;

    @Column(name = "window_title")
    private String windowTitle;


    // =====================================================
    // EVENT TIME
    // =====================================================

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public InputActivity() {
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

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
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

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }
}