
package com.example.TeamLoggerC.tracker;

import com.example.TeamLoggerC.entity.ApplicationActivity;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.ApplicationActivityRepo;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.ApplicationEnforcementService;
import com.example.TeamLoggerC.service.CurrentNetworkIpService;
import com.example.TeamLoggerC.service.DiscoveredApplicationService;
import com.example.TeamLoggerC.service.IpApplicationRuleService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ApplicationActivityTracker {

    private static final Logger LOGGER =
            Logger.getLogger(ApplicationActivityTracker.class.getName());

    private final Integer userId;

    private final ActiveWindowService activeWindowService;
    private final ApplicationActivityRepo applicationActivityRepo;
    private final SessionRepo sessionRepo;
    private final UserRepo userRepo;
    private final DiscoveredApplicationService discoveredApplicationService;
    private final IpApplicationRuleService ipApplicationRuleService;
    private final CurrentNetworkIpService currentNetworkIpService;
    private final ApplicationEnforcementService applicationEnforcementService;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private ApplicationActivity currentActivity;
    private volatile boolean running = false;
    private Integer sessionId;

    public ApplicationActivityTracker(
            Integer userId,
            ActiveWindowService activeWindowService,
            ApplicationActivityRepo applicationActivityRepo,
            SessionRepo sessionRepo,
            UserRepo userRepo,
            DiscoveredApplicationService discoveredApplicationService,
            IpApplicationRuleService ipApplicationRuleService,
            CurrentNetworkIpService currentNetworkIpService,
            ApplicationEnforcementService applicationEnforcementService
    ) {
        this.userId = userId;
        this.activeWindowService = activeWindowService;
        this.applicationActivityRepo = applicationActivityRepo;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.discoveredApplicationService = discoveredApplicationService;
        this.ipApplicationRuleService = ipApplicationRuleService;
        this.currentNetworkIpService = currentNetworkIpService;
        this.applicationEnforcementService = applicationEnforcementService;
    }

    // =====================================================
    // START TRACKING
    // =====================================================

    public synchronized void start() {

        if (running) {
            return;
        }

        if (scheduler.isShutdown()) {
            LOGGER.severe(
                    "Cannot restart ApplicationActivityTracker because its "
                            + "scheduler has already been shut down. userId="
                            + userId
            );
            return;
        }

        Session session = getCurrentOpenSession();

        if (session == null) {
            LOGGER.warning(
                    "Cannot start application tracking: no open session. "
                            + "userId=" + userId
            );
            return;
        }

        if (session.getEmployeeStartedAt() == null) {
            LOGGER.warning(
                    "Cannot start application tracking: employee has not "
                            + "clicked Start Work. userId=" + userId
            );
            return;
        }

        sessionId = session.getId();
        running = true;

        try {
            scheduler.scheduleAtFixedRate(
                    this::checkActiveWindow,
                    0,
                    2,
                    TimeUnit.SECONDS
            );

            LOGGER.info(
                    "Application activity tracking started. userId="
                            + userId + ", sessionId=" + sessionId
            );

        } catch (RuntimeException exception) {
            running = false;

            LOGGER.log(
                    Level.SEVERE,
                    "Could not schedule application tracking. userId="
                            + userId,
                    exception
            );
        }
    }

    // =====================================================
    // CHECK ACTIVE WINDOW AND APPLY IP APPLICATION RULES
    // =====================================================

    private void checkActiveWindow() {

        if (!running) {
            return;
        }

        try {
            ActiveWindowService.ActiveWindowInfo info =
                    activeWindowService.getActiveWindowInfo();

            if (info == null) {
                return;
            }

            String applicationName =
                    normalize(info.getApplicationName());

            String executableName =
                    normalize(info.getExecutableName());

            String windowTitle =
                    normalize(info.getWindowTitle());

            int processId = info.getProcessId();

            if (applicationName.isBlank()) {
                return;
            }

            // Obtain the employee's current network IP.
            String currentIp = null;

            try {
                currentIp = normalize(
                        currentNetworkIpService.getCurrentIpv4Address()
                );

                if (currentIp.isBlank()) {
                    currentIp = null;
                }

            } catch (Exception ipException) {
                LOGGER.log(
                        Level.SEVERE,
                        "Could not determine the current network IP. "
                                + "userId=" + userId,
                        ipException
                );
            }

            LOGGER.info(
                    "APPLICATION_CHECK"
                            + " userId=" + userId
                            + ", ip=" + (currentIp == null ? "UNKNOWN" : currentIp)
                            + ", application=" + applicationName
                            + ", executable=" + executableName
                            + ", pid=" + processId
            );

            // Discover the application independently of its rule result.
            discoverApplication(applicationName, executableName);

            // Resolve the employee and their administrator.
            User user = userRepo.findById(userId).orElse(null);

            if (user == null || user.getAdminId() == null) {
                LOGGER.warning(
                        "Cannot resolve application rules: employee or "
                                + "adminId not found. userId=" + userId
                );

                closeCurrentActivity();
                return;
            }

            // Do not evaluate IP-specific rules with an unknown IP.
            if (currentIp == null || currentIp.isBlank()) {
                LOGGER.warning(
                        "Cannot evaluate IP-specific application rules "
                                + "because the current IP is unavailable. "
                                + "userId=" + userId
                );

                closeCurrentActivity();
                return;
            }

            Integer adminId = user.getAdminId();

            String permission =
                    ipApplicationRuleService.getApplicationPermission(
                            adminId,
                            currentIp,
                            applicationName,
                            executableName
                    );

            LOGGER.info(
                    "IP_APPLICATION_RULE_RESULT"
                            + " userId=" + userId
                            + ", adminId=" + adminId
                            + ", ip=" + currentIp
                            + ", application=" + applicationName
                            + ", executable=" + executableName
                            + ", pid=" + processId
                            + ", permission=" + permission
            );

            // BLOCK: close the current activity and enforce the process block.
            if (IpApplicationRuleService.PERMISSION_BLOCK
                    .equalsIgnoreCase(permission)) {

                handleBlockedApplication(
                        applicationName,
                        executableName,
                        processId
                );

                return;
            }

            // The unified rule system supports only ALLOW and BLOCK.
            if (!IpApplicationRuleService.PERMISSION_ALLOW
                    .equalsIgnoreCase(permission)) {

                LOGGER.warning(
                        "Unexpected application rule result. "
                                + "Skipping activity tracking. "
                                + "userId=" + userId
                                + ", permission=" + permission
                );

                closeCurrentActivity();
                return;
            }

            // ALLOW: track the active application normally.
            trackAllowedApplication(applicationName, windowTitle);

        } catch (Exception exception) {
            /*
             * A failed check should not terminate future scheduled checks.
             */
            LOGGER.log(
                    Level.SEVERE,
                    "Error checking active application. userId=" + userId,
                    exception
            );
        }
    }

    // =====================================================
    // HANDLE BLOCKED APPLICATION
    // =====================================================

    private void handleBlockedApplication(
            String applicationName,
            String executableName,
            int processId
    ) {
        LOGGER.warning(
                "APPLICATION_BLOCK_REQUESTED"
                        + " userId=" + userId
                        + ", application=" + applicationName
                        + ", executable=" + executableName
                        + ", pid=" + processId
        );

        // Do not leave the previously tracked activity open.
        closeCurrentActivity();

        if (processId <= 0) {
            LOGGER.severe(
                    "Cannot enforce application BLOCK: invalid PID. "
                            + "application=" + applicationName
                            + ", pid=" + processId
            );
            return;
        }

        try {
            boolean terminated =
                    applicationEnforcementService.blockProcess(processId);

            if (terminated) {
                LOGGER.info(
                        "APPLICATION_BLOCK_ENFORCED"
                                + " userId=" + userId
                                + ", application=" + applicationName
                                + ", executable=" + executableName
                                + ", pid=" + processId
                );

            } else {
                LOGGER.severe(
                        "APPLICATION_BLOCK_FAILED"
                                + " userId=" + userId
                                + ", application=" + applicationName
                                + ", executable=" + executableName
                                + ", pid=" + processId
                                + ". Check Windows permissions and "
                                + "ApplicationEnforcementService logs."
                );
            }

        } catch (Exception exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Exception while enforcing application BLOCK. "
                            + "userId=" + userId
                            + ", application=" + applicationName
                            + ", pid=" + processId,
                    exception
            );
        }
    }

    // =====================================================
    // TRACK ALLOWED APPLICATION
    // =====================================================

    private void trackAllowedApplication(
            String applicationName,
            String windowTitle
    ) {
        if (currentActivity == null) {
            startNewActivity(applicationName, windowTitle);
            return;
        }

        boolean applicationChanged =
                !safeEquals(
                        currentActivity.getApplicationName(),
                        applicationName
                );

        boolean windowChanged =
                !safeEquals(
                        currentActivity.getWindowTitle(),
                        windowTitle
                );

        if (applicationChanged || windowChanged) {
            closeCurrentActivity();
            startNewActivity(applicationName, windowTitle);
        }
    }

    // =====================================================
    // DISCOVER APPLICATION
    // =====================================================

    private void discoverApplication(
            String applicationName,
            String executableName
    ) {
        try {
            User user = userRepo.findById(userId).orElse(null);

            if (user == null) {
                LOGGER.warning(
                        "Cannot discover application: user not found. "
                                + "userId=" + userId
                );
                return;
            }

            Integer adminId = user.getAdminId();

            if (adminId == null) {
                LOGGER.warning(
                        "Cannot discover application: adminId is missing. "
                                + "userId=" + userId
                );
                return;
            }

            discoveredApplicationService.discoverApplication(
                    adminId,
                    applicationName,
                    executableName,
                    null,
                    null
            );

        } catch (Exception exception) {
            // Discovery failure must not stop application rule evaluation.
            LOGGER.log(
                    Level.WARNING,
                    "Application discovery failed. userId=" + userId
                            + ", application=" + applicationName,
                    exception
            );
        }
    }

    // =====================================================
    // START NEW ACTIVITY
    // =====================================================

    private void startNewActivity(
            String applicationName,
            String windowTitle
    ) {
        if (sessionId == null) {
            LOGGER.warning(
                    "Cannot save application activity without a session. "
                            + "userId=" + userId
            );
            return;
        }

        try {
            ApplicationActivity activity = new ApplicationActivity();

            activity.setSessionId(sessionId);
            activity.setApplicationName(applicationName);
            activity.setWindowTitle(windowTitle);
            activity.setStartTime(LocalDateTime.now());
            activity.setEndTime(null);
            activity.setDurationMinutes(0L);

            currentActivity = applicationActivityRepo.save(activity);

            LOGGER.info(
                    "Application activity started."
                            + " userId=" + userId
                            + ", application=" + applicationName
                            + ", window=" + windowTitle
            );

        } catch (Exception exception) {
            currentActivity = null;

            LOGGER.log(
                    Level.SEVERE,
                    "Could not save application activity. userId=" + userId
                            + ", application=" + applicationName,
                    exception
            );
        }
    }

    // =====================================================
    // CLOSE CURRENT ACTIVITY
    // =====================================================

    private void closeCurrentActivity() {

        if (currentActivity == null) {
            return;
        }

        ApplicationActivity activityToClose = currentActivity;

        try {
            LocalDateTime endTime = LocalDateTime.now();

            activityToClose.setEndTime(endTime);

            long durationSeconds =
                    Duration.between(
                            activityToClose.getStartTime(),
                            endTime
                    ).getSeconds();

            activityToClose.setDurationMinutes(
                    Math.max(0L, durationSeconds / 60)
            );

            applicationActivityRepo.save(activityToClose);

        } catch (Exception exception) {
            LOGGER.log(
                    Level.SEVERE,
                    "Could not close application activity. userId=" + userId,
                    exception
            );

        } finally {
            currentActivity = null;
        }
    }

    // =====================================================
    // STOP TRACKING
    // =====================================================

    public synchronized void stop() {

        if (!running && scheduler.isShutdown()) {
            return;
        }

        running = false;

        // Stop scheduled checks before closing the last activity.
        scheduler.shutdownNow();

        closeCurrentActivity();

        LOGGER.info(
                "Application activity tracking stopped. userId=" + userId
                        + ", sessionId=" + sessionId
        );
    }

    // =====================================================
    // CURRENT OPEN SESSION
    // =====================================================

    private Session getCurrentOpenSession() {

        List<Session> sessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        if (sessions == null || sessions.isEmpty()) {
            return null;
        }

        return sessions.stream()
                .filter(session ->
                        session != null && session.getLoginTime() != null
                )
                .max((first, second) ->
                        first.getLoginTime().compareTo(second.getLoginTime())
                )
                .orElse(null);
    }

    // =====================================================
    // NORMALIZATION HELPERS
    // =====================================================

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean safeEquals(String first, String second) {
        if (first == null && second == null) {
            return true;
        }

        if (first == null || second == null) {
            return false;
        }

        return first.equals(second);
    }

    // =====================================================
    // STATUS
    // =====================================================

    public boolean isRunning() {
        return running;
    }

    public Integer getSessionId() {
        return sessionId;
    }
}
