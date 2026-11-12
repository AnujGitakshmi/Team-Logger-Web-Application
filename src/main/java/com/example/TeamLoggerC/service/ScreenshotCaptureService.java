
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
import java.util.Set;
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
    // =========================================================

    private final Map<Integer, ScheduledFuture<?>> screenshotTasks =
            new ConcurrentHashMap<>();

    // Employees currently allowed to capture screenshots.
    private final Set<Integer> activeCaptureUsers =
            ConcurrentHashMap.newKeySet();

    // Synchronization lock for each employee.
    private final Map<Integer, Object> captureLocks =
            new ConcurrentHashMap<>();

    private Object getCaptureLock(Integer userId) {
        return captureLocks.computeIfAbsent(
                userId,
                id -> new Object()
        );
    }

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

        synchronized (getCaptureLock(userId)) {

            if (isCapturing(userId)) {
                System.out.println(
                        "Screenshot capture is already running "
                                + "for User: " + userId
                );
                return;
            }

            Session session = getCurrentOpenSession(userId);

            if (session == null) {
                System.out.println(
                        "Cannot start screenshot capture. "
                                + "No open session for User: "
                                + userId
                );
                return;
            }

            if (session.getEmployeeStartedAt() == null
                    || session.isWorkPaused()) {

                System.out.println(
                        "Cannot start screenshot capture. "
                                + "Employee has not started work "
                                + "or session is paused. User: "
                                + userId
                );
                return;
            }

            User employee = userRepo.findById(userId)
                    .orElse(null);

            if (employee == null) {
                System.out.println(
                        "Cannot start screenshot capture. "
                                + "Employee not found. User: "
                                + userId
                );
                return;
            }

            Integer adminId = employee.getAdminId();

            if (adminId == null) {
                System.out.println(
                        "Cannot start screenshot capture. "
                                + "Employee has no admin assigned. "
                                + "User: " + userId
                );
                return;
            }

            if (!settingsService.isEnabled(adminId)) {
                System.out.println(
                        "Screenshot capture is disabled "
                                + "by Admin: " + adminId
                                + " for User: " + userId
                );
                return;
            }

            int intervalMinutes =
                    settingsService.getIntervalMinutes(adminId);

            if (intervalMinutes <= 0) {
                intervalMinutes = 5;
            }

            final int finalIntervalMinutes = intervalMinutes;

            // Allow capture before scheduling the first screenshot.
            activeCaptureUsers.add(userId);

            try {

                ScheduledFuture<?> task =
                        scheduler.scheduleAtFixedRate(
                                () -> captureScreenshot(userId),
                                finalIntervalMinutes,
                                finalIntervalMinutes,
                                TimeUnit.MINUTES
                        );

                screenshotTasks.put(userId, task);

                // Capture the first screenshot immediately.
                captureScreenshot(userId);

                System.out.println(
                        "Screenshot capture started. "
                                + "User: " + userId
                                + " | Interval: "
                                + finalIntervalMinutes
                                + " minutes"
                );

            } catch (Exception e) {

                activeCaptureUsers.remove(userId);

                ScheduledFuture<?> task =
                        screenshotTasks.remove(userId);

                if (task != null) {
                    task.cancel(false);
                }

                System.err.println(
                        "Unable to start screenshot capture "
                                + "for User: " + userId
                );

                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // CAPTURE ONE SCREENSHOT
    // =========================================================

    public void captureScreenshot(Integer userId) {

        synchronized (getCaptureLock(userId)) {

            // Do not capture after Stop Work.
            if (!activeCaptureUsers.contains(userId)) {
                return;
            }

            try {

                // Get current open session.
                Session session = getCurrentOpenSession(userId);

                if (session == null) {

                    System.out.println(
                            "No open session. "
                                    + "Screenshot skipped for User: "
                                    + userId
                    );

                    stopCapture(userId);
                    return;
                }

                // Check Start Work and pause status.
                if (session.getEmployeeStartedAt() == null
                        || session.isWorkPaused()) {

                    System.out.println(
                            "Employee has not started work "
                                    + "or session is paused. "
                                    + "Screenshot skipped for User: "
                                    + userId
                    );

                    stopCapture(userId);
                    return;
                }

                // Get employee.
                User employee = userRepo.findById(userId)
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

                // Get admin.
                Integer adminId = employee.getAdminId();

                if (adminId == null) {

                    System.out.println(
                            "Employee has no admin. "
                                    + "Screenshot skipped for User: "
                                    + userId
                    );

                    stopCapture(userId);
                    return;
                }

                // Check screenshot settings.
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

                // Capture screen.
                String filePath =
                        storageService.captureScreen(userId);

                // Check active state again before saving.
                if (!activeCaptureUsers.contains(userId)) {
                    return;
                }

                // Extract file name.
                String fileName = Paths.get(filePath)
                        .getFileName()
                        .toString();

                // Create Screenshot entity.
                Screenshot screenshot = new Screenshot();

                screenshot.setUserId(userId);
                screenshot.setSessionId(session.getId());
                screenshot.setFileName(fileName);
                screenshot.setFilePath(filePath);

                LocalDateTime capturedAt = LocalDateTime.now();

                screenshot.setCapturedAt(capturedAt);

                // Initial storage.
                screenshot.setStorageType("LOCAL");
                screenshot.setArchived(false);
                screenshot.setArchivePath(null);
                screenshot.setExpiresAt(null);

                // Save database record.
                screenshotService.saveScreenshot(screenshot);

                System.out.println(
                        "Screenshot saved successfully. "
                                + "User: " + userId
                                + " | Session: " + session.getId()
                                + " | File: " + fileName
                );

            } catch (Exception e) {

                System.err.println(
                        "Screenshot capture failed "
                                + "for User: " + userId
                );

                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // STOP SCREENSHOT CAPTURE
    // =========================================================

    public void stopCapture(Integer userId) {

        // Disable new captures immediately.
        activeCaptureUsers.remove(userId);

        synchronized (getCaptureLock(userId)) {

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
    }

    // =========================================================
    // CHECK WHETHER SCREENSHOT CAPTURE IS RUNNING
    // =========================================================

    public boolean isCapturing(Integer userId) {

        ScheduledFuture<?> task =
                screenshotTasks.get(userId);

        return activeCaptureUsers.contains(userId)
                && task != null
                && !task.isCancelled()
                && !task.isDone();
    }

    // =========================================================
    // GET CURRENT OPEN SESSION
    // =========================================================

    private Session getCurrentOpenSession(Integer userId) {

        return sessionRepo
                .findByUserIdAndLogoutTimeIsNull(userId)
                .stream()
                .max(
                        (s1, s2) ->
                                s1.getLoginTime().compareTo(
                                        s2.getLoginTime()
                                )
                )
                .orElse(null);
    }

    // =========================================================
    // STOP ALL SCREENSHOT CAPTURE
    // =========================================================

    public void stopAllCaptures() {

        for (Integer userId : screenshotTasks.keySet()) {
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
