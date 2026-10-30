package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminSettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class AdminGlobalModelAdvice {

    private final AdminSettingsService adminSettingsService;

    public AdminGlobalModelAdvice(
            AdminSettingsService adminSettingsService) {

        this.adminSettingsService = adminSettingsService;
    }

    @ModelAttribute("adminSettings")
    public AdminSettings adminSettings(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails)) {
            return null;
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) principal;

        User user =
                userDetails.getUser();

        if (user == null
                || user.getId() == null) {
            return null;
        }

        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            return null;
        }

        return adminSettingsService.getSettings(
                user.getId()
        );
    }
}