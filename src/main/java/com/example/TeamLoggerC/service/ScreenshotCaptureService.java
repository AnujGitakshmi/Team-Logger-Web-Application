package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Screenshot;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ScreenshotCaptureService {

    private final ScreenshotService screenshotService;
    private final ScreenshotStorageService storageService;
    private final ScreenshotSettingsService settingsService;
    private final SessionRepo sessionRepo;
    private final UserRepo userRepo;


    // =========================================================
    // SCREENSHOT SCHEDULER
    // =========================================================

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(4);


    // =========================================================
    // ACTIVE SCREENSHOT TASKS
    //
    // userId -> scheduled screenshot task
    // =========================================================

    private final Map<Integer, ScheduledFuture<?>> screenshotTasks =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ScreenshotCaptureService(
            ScreenshotService screenshotService,
            ScreenshotStorageService storageService,
            ScreenshotSettingsService settingsService,
            SessionRepo sessionRepo,
            UserRepo userRepo) {

        this.screenshotService = screenshotService;
        this.storageService = storageService;
        this.settingsService = settingsService;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
    }


    // =========================================================
    // START SCREENSHOT CAPTURE
    // =========================================================

    public void startCapture(Integer userId) {

        // -----------------------------------------------------
        // Prevent duplicate scheduler
        // -----------------------------------------------------

        if (isCapturing(userId)) {

            System.out.println(
                    "Screenshot capture is already running "
                            + "for User: " + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Get current session
        // -----------------------------------------------------

        Session session =
                getCurrentOpenSession(userId);

        if (session == null) {

            System.out.println(
                    "Cannot start screenshot capture. "
                            + "No open session for User: "
                            + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Employee must have clicked Start Work
        // -----------------------------------------------------

        if (session.getEmployeeStartedAt() == null) {

            System.out.println(
                    "Cannot start screenshot capture. "
                            + "Employee has not started work. "
                            + "User: " + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Get employee
        // -----------------------------------------------------

        User employee =
                userRepo.findById(userId)
                        .orElse(null);

        if (employee == null) {

            System.out.println(
                    "Cannot start screenshot capture. "
                            + "Employee not found. "
                            + "User: " + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Get admin ID
        // -----------------------------------------------------

        Integer adminId =
                employee.getAdminId();

        if (adminId == null) {

            System.out.println(
                    "Cannot start screenshot capture. "
                            + "Employee has no admin assigned. "
                            + "User: " + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Check screenshot setting
        // -----------------------------------------------------

        if (!settingsService.isEnabled(adminId)) {

            System.out.println(
                    "Screenshot capture is disabled "
                            + "by Admin: " + adminId
                            + " for User: " + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Get screenshot interval
        // -----------------------------------------------------

        int intervalMinutes =
                settingsService.getIntervalMinutes(adminId);


        // -----------------------------------------------------
        // Validate interval
        // -----------------------------------------------------

        if (intervalMinutes <= 0) {
            intervalMinutes = 5;
        }


        final int finalIntervalMinutes =
                intervalMinutes;


        // -----------------------------------------------------
        // Capture first screenshot immediately
        // -----------------------------------------------------

        captureScreenshot(userId);


        // -----------------------------------------------------
        // Schedule future screenshots
        // -----------------------------------------------------

        ScheduledFuture<?> task =
                scheduler.scheduleAtFixedRate(
                        () -> captureScreenshot(userId),
                        finalIntervalMinutes,
                        finalIntervalMinutes,
                        TimeUnit.MINUTES
                );


        // -----------------------------------------------------
        // Store task
        // -----------------------------------------------------

        screenshotTasks.put(
                userId,
                task
        );


        System.out.println(
                "Screenshot capture started."
                        + " User: " + userId
                        + " | Interval: "
                        + finalIntervalMinutes
                        + " minutes"
        );
    }


    // =========================================================
    // CAPTURE ONE SCREENSHOT
    // =========================================================

    public void captureScreenshot(Integer userId) {

        try {

            // -------------------------------------------------
            // Check current session
            // -------------------------------------------------

            Session session =
                    getCurrentOpenSession(userId);

            if (session == null) {

                System.out.println(
                        "No open session. "
                                + "Screenshot skipped for User: "
                                + userId
                );

                stopCapture(userId);

                return;
            }


            // -------------------------------------------------
            // Check Start Work
            // -------------------------------------------------

            if (session.getEmployeeStartedAt() == null) {

                System.out.println(
                        "Employee has not started work. "
                                + "Screenshot skipped for User: "
                                + userId
                );

                return;
            }


            // -------------------------------------------------
            // Get employee
            // -------------------------------------------------

            User employee =
                    userRepo.findById(userId)
                            .orElse(null);

            if (employee == null) {

                System.out.println(
                        "Employee not found. "
                                + "Screenshot skipped for User: "
                                + userId
                );

                stopCapture(userId);

                return;
            }


            // -------------------------------------------------
            // Get admin
            // -------------------------------------------------

            Integer adminId =
                    employee.getAdminId();

            if (adminId == null) {

                System.out.println(
                        "Employee has no admin. "
                                + "Screenshot skipped for User: "
                                + userId
                );

                stopCapture(userId);

                return;
            }


            // -------------------------------------------------
            // Check if screenshot feature is enabled
            // -------------------------------------------------

            if (!settingsService.isEnabled(adminId)) {

                System.out.println(
                        "Screenshot capture disabled "
                                + "by Admin: " + adminId
                                + ". Skipping User: "
                                + userId
                );

                stopCapture(userId);

                return;
            }


            // -------------------------------------------------
            // Capture screen
            // -------------------------------------------------

            String filePath =
                    storageService.captureScreen(userId);


            // -------------------------------------------------
            // Extract file name
            // -------------------------------------------------

            String fileName =
                    Paths.get(filePath)
                            .getFileName()
                            .toString();


            // -------------------------------------------------
            // Create Screenshot entity
            // -----------------------------------------------------

            Screenshot screenshot =
                    new Screenshot();

            screenshot.setUserId(
                    userId
            );

            screenshot.setSessionId(
                    session.getId()
            );

            screenshot.setFileName(
                    fileName
            );

            screenshot.setFilePath(
                    filePath
            );

            LocalDateTime capturedAt = LocalDateTime.now();

            screenshot.setCapturedAt(capturedAt);

            // =========================================================
            // INITIAL STORAGE
           // =========================================================

            // Screenshot is stored locally for the first 7 days.
            screenshot.setStorageType("LOCAL");

          // Screenshot has not been archived yet.
            screenshot.setArchived(false);

         // No ZIP archive exists yet.
            screenshot.setArchivePath(null);

         // expiresAt is not used for permanent deletion.
         // The screenshot will be retained for 12 months
         // before being moved to ZIP archive.
            screenshot.setExpiresAt(null);

            // -------------------------------------------------
            // Save DB record
            // -------------------------------------------------

            screenshotService.saveScreenshot(
                    screenshot
            );


            System.out.println(
                    "Screenshot saved successfully."
                            + " User: " + userId
                            + " | Session: "
                            + session.getId()
                            + " | File: "
                            + fileName
            );

        } catch (Exception e) {

            System.err.println(
                    "Screenshot capture failed "
                            + "for User: " + userId
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // STOP SCREENSHOT CAPTURE
    // =========================================================

    public void stopCapture(Integer userId) {

        ScheduledFuture<?> task =
                screenshotTasks.remove(userId);

        if (task != null) {

            task.cancel(false);

            System.out.println(
                    "Screenshot capture stopped "
                            + "for User: " + userId
            );
        }
    }


    // =========================================================
    // CHECK WHETHER SCREENSHOT CAPTURE IS RUNNING
    // =========================================================

    public boolean isCapturing(Integer userId) {

        ScheduledFuture<?> task =
                screenshotTasks.get(userId);

        return task != null
                && !task.isCancelled()
                && !task.isDone();
    }


    // =========================================================
    // GET CURRENT OPEN SESSION
    // =========================================================

    private Session getCurrentOpenSession(
            Integer userId) {

        return sessionRepo
                .findByUserIdAndLogoutTimeIsNull(userId)
                .stream()
                .max(
                        (s1, s2) ->
                                s1.getLoginTime()
                                        .compareTo(
                                                s2.getLoginTime()
                                        )
                )
                .orElse(null);
    }


    // =========================================================
    // STOP ALL SCREENSHOT CAPTURE
    // =========================================================

    public void stopAllCaptures() {

        for (Integer userId :
                screenshotTasks.keySet()) {

            stopCapture(userId);
        }

        System.out.println(
                "All screenshot capture tasks stopped."
        );
    }


    // =========================================================
    // SHUTDOWN SCHEDULER
    // =========================================================

    public void shutdown() {

        stopAllCaptures();

        scheduler.shutdownNow();

        System.out.println(
                "Screenshot scheduler shut down."
        );
    }
}