package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.ScreenshotSettings;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.ScreenshotSettingsService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;

@Controller
@RequestMapping("/admin/settings")
public class AdminSettingsController {

    private final AdminSettingsService adminSettingsService;
    private final ScreenshotSettingsService screenshotSettingsService;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public AdminSettingsController(
            AdminSettingsService adminSettingsService,
            ScreenshotSettingsService screenshotSettingsService) {

        this.adminSettingsService =
                adminSettingsService;

        this.screenshotSettingsService =
                screenshotSettingsService;
    }


    // =========================================================
    // SHOW SETTINGS PAGE
    // =========================================================

    @GetMapping
    public String settings(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // -----------------------------------------------------
        // General admin settings
        // -----------------------------------------------------

        AdminSettings adminSettings =
                adminSettingsService.getSettings(
                        adminId
                );


        // -----------------------------------------------------
        // Screenshot settings
        // -----------------------------------------------------

        ScreenshotSettings screenshotSettings =
                screenshotSettingsService.getSettings(
                        adminId
                );


        // -----------------------------------------------------
        // Add data to model
        // -----------------------------------------------------

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "user",
                admin
        );

        model.addAttribute(
                "adminSettings",
                adminSettings
        );

        model.addAttribute(
                "screenshotSettings",
                screenshotSettings
        );


        return "adminSettings";
    }


    // =========================================================
    // SAVE ALL SETTINGS
    // =========================================================

    @PostMapping("/save")
    public String saveSettings(

            // -------------------------------------------------
            // Company
            // -------------------------------------------------

            @RequestParam("companyName")
            String companyName,


            // -------------------------------------------------
            // Working hours
            // -------------------------------------------------

            @RequestParam(
                    "workingStartTime"
            )
            String workingStartTime,

            @RequestParam(
                    "workingEndTime"
            )
            String workingEndTime,


            // -------------------------------------------------
            // Activity tracking
            // -------------------------------------------------

            @RequestParam(
                    "idleThresholdSeconds"
            )
            Integer idleThresholdSeconds,


            // -------------------------------------------------
            // IP access control
            // -------------------------------------------------

            @RequestParam(
                    value = "ipRestrictionEnabled",
                    required = false
            )
            Boolean ipRestrictionEnabled,

            @RequestParam(
                    value = "companyIpAddress",
                    required = false
            )
            String companyIpAddress,

            @RequestParam(
                    value = "unknownIpRequiresApproval",
                    required = false
            )
            Boolean unknownIpRequiresApproval,


            // -------------------------------------------------
            // Screenshot settings
            // -------------------------------------------------

            @RequestParam(
                    value = "screenshotEnabled",
                    required = false
            )
            Boolean screenshotEnabled,

            @RequestParam(
                    value = "screenshotIntervalMinutes",
                    required = false
            )
            Integer screenshotIntervalMinutes,


            // -------------------------------------------------
            // Authentication
            // -------------------------------------------------

            Authentication authentication) {


        // =====================================================
        // GET LOGGED-IN ADMIN
        // =====================================================

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // =====================================================
        // CONVERT TIME
        // =====================================================

        LocalTime startTime =
                LocalTime.parse(
                        workingStartTime
                );

        LocalTime endTime =
                LocalTime.parse(
                        workingEndTime
                );


        // =====================================================
        // CHECK IP SETTINGS
        // =====================================================

        /*
         * HTML checkboxes do not submit a value when unchecked.
         *
         * Therefore:
         *
         * null = unchecked
         * true = checked
         */

        if (ipRestrictionEnabled == null) {
            ipRestrictionEnabled = false;
        }

        if (unknownIpRequiresApproval == null) {
            unknownIpRequiresApproval = false;
        }


        // =====================================================
        // SAVE GENERAL + IP SETTINGS
        // =====================================================

        adminSettingsService.saveSettings(
                adminId,
                companyName,
                startTime,
                endTime,
                idleThresholdSeconds,
                ipRestrictionEnabled,
                companyIpAddress,
                unknownIpRequiresApproval
        );


        // =====================================================
        // SAVE SCREENSHOT SETTINGS
        // =====================================================

        if (screenshotEnabled == null) {
            screenshotEnabled = false;
        }

        if (screenshotIntervalMinutes == null
                || screenshotIntervalMinutes <= 0) {

            screenshotIntervalMinutes = 5;
        }


        screenshotSettingsService.saveSettings(
                adminId,
                screenshotEnabled,
                screenshotIntervalMinutes
        );


        // =====================================================
        // SUCCESS
        // =====================================================

        return "redirect:/admin/settings?success";
    }
}

