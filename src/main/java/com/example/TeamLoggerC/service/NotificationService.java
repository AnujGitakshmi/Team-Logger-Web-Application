package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Notification;
import com.example.TeamLoggerC.repository.NotificationRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepo notificationRepo;

    public NotificationService(
            NotificationRepo notificationRepo) {

        this.notificationRepo = notificationRepo;
    }


    // ==========================================
    // CREATE NOTIFICATION
    // ==========================================

    public Notification createNotification(
            Integer recipientId,
            Integer senderId,
            String type,
            String title,
            String message,
            Integer referenceId) {

        Notification notification =
                new Notification();

        notification.setRecipientId(recipientId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setReferenceId(referenceId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        return notificationRepo.save(notification);
    }


    // ==========================================
    // GET ALL NOTIFICATIONS
    // ==========================================

    public List<Notification> getNotifications(
            Integer recipientId) {

        System.out.println(
                "GET NOTIFICATIONS FOR USER: "
                        + recipientId
        );

        return notificationRepo
                .findByRecipientIdOrderByCreatedAtDesc(
                        recipientId
                );
    }


    // ==========================================
    // GET UNREAD NOTIFICATIONS
    // ==========================================

    public List<Notification> getUnreadNotifications(
            Integer recipientId) {

        return notificationRepo
                .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
                        recipientId
                );
    }


    // ==========================================
    // UNREAD COUNT
    // ==========================================

    public long getUnreadCount(
            Integer recipientId) {

        long count =
                notificationRepo
                        .countByRecipientIdAndReadFalse(
                                recipientId
                        );

        System.out.println(
                "UNREAD COUNT FOR USER "
                        + recipientId
                        + ": "
                        + count
        );

        return count;
    }


    // ==========================================
    // MARK ONE AS READ
    // ==========================================

    public void markAsRead(
            Integer notificationId,
            Integer userId) {

        System.out.println(
                "MARK AS READ"
        );

        System.out.println(
                "Notification ID: "
                        + notificationId
        );

        System.out.println(
                "User ID: "
                        + userId
        );

        Notification notification =
                notificationRepo
                        .findById(notificationId)
                        .orElse(null);

        if (notification == null) {

            System.out.println(
                    "NOTIFICATION NOT FOUND"
            );

            return;
        }

        System.out.println(
                "Notification recipient ID: "
                        + notification.getRecipientId()
        );

        System.out.println(
                "Current read status: "
                        + notification.isRead()
        );


        // Security check
        if (!notification
                .getRecipientId()
                .equals(userId)) {

            System.out.println(
                    "USER DOES NOT OWN THIS NOTIFICATION"
            );

            return;
        }


        notification.setRead(true);

        notificationRepo.save(notification);

        System.out.println(
                "NOTIFICATION MARKED AS READ"
        );
    }


    // ==========================================
    // MARK ALL AS READ
    // ==========================================

    public void markAllAsRead(
            Integer userId) {

        List<Notification> notifications =
                notificationRepo
                        .findByRecipientIdAndReadFalseOrderByCreatedAtDesc(
                                userId
                        );

        System.out.println(
                "MARK ALL AS READ"
        );

        System.out.println(
                "USER ID: " + userId
        );

        System.out.println(
                "UNREAD NOTIFICATIONS: "
                        + notifications.size()
        );

        for (Notification notification :
                notifications) {

            notification.setRead(true);
        }

        notificationRepo.saveAll(notifications);

        System.out.println(
                "ALL NOTIFICATIONS MARKED AS READ"
        );
    }
}