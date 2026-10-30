 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepo
        extends JpaRepository<Notification, Integer> {

    // ==========================================
    // ALL NOTIFICATIONS FOR A USER
    // ==========================================

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(
            Integer recipientId
    );


    // ==========================================
    // UNREAD NOTIFICATIONS
    // ==========================================

    List<Notification> findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
            Integer recipientId
    );


    // ==========================================
    // UNREAD COUNT
    // ==========================================

    long countByRecipientIdAndReadFalse(
            Integer recipientId
    );


    // ==========================================
    // MARK NOTIFICATION AS READ
    // ==========================================

    void deleteByRecipientId(Integer recipientId);
}

