package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.NotificationService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/employee")
public class EmployeeNotificationController {

    private final NotificationService notificationService;

    public EmployeeNotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    // ==========================================
    // NOTIFICATION PAGE
    // ==========================================

    @GetMapping("/notifications")
    public String notifications(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user =
                userDetails.getUser();

        Integer userId =
                user.getId();

        System.out.println(
                "NOTIFICATION PAGE - USER ID: " + userId
        );

        model.addAttribute("user", user);

        model.addAttribute(
                "notifications",
                notificationService.getNotifications(userId)
        );

        model.addAttribute(
                "unreadNotificationCount",
                notificationService.getUnreadCount(userId)
        );

        return "notifications";
    }


    // ==========================================
    // MARK ONE NOTIFICATION AS READ
    // ==========================================

    @PostMapping("/notifications/read/{id}")
    public String markAsRead(
            @PathVariable Integer id,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer userId =
                userDetails.getUser().getId();

        System.out.println(
                "MARK READ REQUEST RECEIVED"
        );

        System.out.println(
                "NOTIFICATION ID: " + id
        );

        System.out.println(
                "USER ID: " + userId
        );

        notificationService.markAsRead(
                id,
                userId
        );

        System.out.println(
                "MARK READ REQUEST COMPLETED"
        );

        return "redirect:/employee/notifications";
    }


    // ==========================================
    // MARK ALL AS READ
    // ==========================================

    @PostMapping("/notifications/read-all")
    public String markAllAsRead(
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer userId =
                userDetails.getUser().getId();

        System.out.println(
                "MARK ALL READ REQUEST RECEIVED"
        );

        System.out.println(
                "USER ID: " + userId
        );

        notificationService.markAllAsRead(userId);

        System.out.println(
                "MARK ALL READ REQUEST COMPLETED"
        );

        return "redirect:/employee/notifications";
    }
}