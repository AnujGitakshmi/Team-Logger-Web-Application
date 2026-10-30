 package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // ==========================================
    // USER WHO RECEIVES THE NOTIFICATION
    // ==========================================

    @Column(name = "recipient_id", nullable = false)
    private Integer recipientId;


    // ==========================================
    // USER WHO CREATED/SENT THE NOTIFICATION
    // ==========================================

    @Column(name = "sender_id")
    private Integer senderId;


    // ==========================================
    // NOTIFICATION TYPE
    //
    // Examples:
    // LOGIN_REQUEST
    // LOGIN_APPROVED
    // LOGIN_REJECTED
    // PROJECT_ASSIGNED
    // GENERAL
    // ==========================================

    @Column(name = "type", nullable = false)
    private String type;


    // ==========================================
    // NOTIFICATION TITLE
    // ==========================================

    @Column(name = "title", nullable = false)
    private String title;


    // ==========================================
    // NOTIFICATION MESSAGE
    // ==========================================

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;


    // ==========================================
    // OPTIONAL RELATED RECORD
    //
    // Example:
    // Project ID
    // Login Request ID
    // ==========================================

    @Column(name = "reference_id")
    private Integer referenceId;


    // ==========================================
    // READ STATUS
    // ==========================================

    @Column(name = "is_read", nullable = false)
    private boolean read = false;


    // ==========================================
    // CREATED TIME
    // ==========================================

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Notification() {
    }


    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public Integer getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(Integer recipientId) {
        this.recipientId = recipientId;
    }


    public Integer getSenderId() {
        return senderId;
    }

    public void setSenderId(Integer senderId) {
        this.senderId = senderId;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }


    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }


    public Integer getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(Integer referenceId) {
        this.referenceId = referenceId;
    }


    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }


    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

