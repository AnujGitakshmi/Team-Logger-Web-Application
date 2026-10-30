package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.Screenshot;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminTrackingService;
import com.example.TeamLoggerC.service.ScreenshotService;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.List;

@Controller
@RequestMapping("/admin/tracking")
public class AdminTrackingController {

    private final AdminTrackingService trackingService;
    private final ScreenshotService screenshotService;
    private final UserRepo userRepo;

    public AdminTrackingController(
            AdminTrackingService trackingService,
            ScreenshotService screenshotService,
            UserRepo userRepo) {

        this.trackingService = trackingService;
        this.screenshotService = screenshotService;
        this.userRepo = userRepo;
    }


    // =========================================================
    // TODAY'S ACTIVITY
    // URL:
    // /admin/tracking
    // =========================================================

    @GetMapping
    public String todayActivity(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        model.addAttribute(
                "activities",
                trackingService.getTodayActivity(adminId)
        );

        model.addAttribute(
                "admin",
                admin
        );

        return "adminTodayActivity";
    }


    // =========================================================
    // EMPLOYEE SCREENSHOT TRACKING
    // URL:
    // /admin/tracking/{employeeId}
    // =========================================================

    @GetMapping("/{employeeId}")
    public String employeeTracking(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model) {

        // -----------------------------------------------------
        // Get logged-in admin
        // -----------------------------------------------------

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // -----------------------------------------------------
        // Find employee
        // -----------------------------------------------------

        User employee =
                userRepo.findById(employeeId)
                        .orElse(null);


        // -----------------------------------------------------
        // Employee does not exist
        // -----------------------------------------------------

        if (employee == null) {
            return "redirect:/admin/dashboard";
        }


        // -----------------------------------------------------
        // Security check
        //
        // Make sure this employee belongs to the logged-in admin.
        // -----------------------------------------------------

        if (employee.getAdminId() == null
                || !employee.getAdminId().equals(adminId)) {

            return "redirect:/admin/dashboard";
        }


        // -----------------------------------------------------
        // Get all screenshots of this employee
        // -----------------------------------------------------

        List<Screenshot> screenshots =
                screenshotService.getEmployeeScreenshots(employeeId);


        // -----------------------------------------------------
        // Send data to HTML
        // -----------------------------------------------------

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "screenshots",
                screenshots
        );


        // -----------------------------------------------------
        // Return screenshot tracking page
        // -----------------------------------------------------

        return "adminEmployeeTracking";
    }
    // =========================================================
// SERVE SCREENSHOT IMAGE
// URL:
// /admin/tracking/image/{screenshotId}
// =========================================================

    @GetMapping("/image/{screenshotId}")
    @ResponseBody
    public ResponseEntity<Resource> getScreenshotImage(
            @PathVariable Integer screenshotId,
            Authentication authentication) {

        try {

            // Logged-in admin
            CustomUserDetails userDetails =
                    (CustomUserDetails) authentication.getPrincipal();

            User admin = userDetails.getUser();

            // Screenshot
            Screenshot screenshot =
                    screenshotService.getScreenshotById(screenshotId);

            if (screenshot == null) {
                return ResponseEntity.notFound().build();
            }

            // Employee
            User employee =
                    userRepo.findById(screenshot.getUserId())
                            .orElse(null);

            if (employee == null
                    || !admin.getId().equals(employee.getAdminId())) {

                return ResponseEntity.status(403).build();
            }

            Path path = Paths.get(screenshot.getFilePath());

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache")
                    .body(resource);

        } catch (Exception e) {

            return ResponseEntity.internalServerError().build();
        }
    }
}