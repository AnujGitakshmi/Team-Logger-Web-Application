
package com.example.TeamLoggerC.tracker;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.ApplicationActivityRepo;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.ActivityService;
import com.example.TeamLoggerC.service.ApplicationEnforcementService;
import com.example.TeamLoggerC.service.CurrentNetworkIpService;
import com.example.TeamLoggerC.service.DiscoveredApplicationService;
import com.example.TeamLoggerC.service.IpApplicationRuleService;

import jakarta.annotation.PreDestroy;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ActivityTrackerService {

    // =========================================================
    // SERVICES / REPOSITORIES
    // =========================================================

    private final ActivityService activityService;
    private final SessionRepo sessionRepo;
    private final ApplicationActivityRepo applicationActivityRepo;
    private final IpApplicationRuleService ipApplicationRuleService;
    private final ApplicationEnforcementService applicationEnforcementService;
    private final UserRepo userRepo;
    private final DiscoveredApplicationService discoveredApplicationService;
    private final CurrentNetworkIpService currentNetworkIpService;
    private final ActiveWindowService activeWindowService;


    // =========================================================
    // ACTIVE / IDLE TRACKERS
    // =========================================================

    private final Map<Integer, ActivityTracker> trackers =
            new ConcurrentHashMap<>();


    // =========================================================
    // APPLICATION TRACKERS
    // =========================================================

    private final Map<Integer, ApplicationActivityTracker>
            applicationTrackers = new ConcurrentHashMap<>();


    // =========================================================
    // LAST ACTIVITY STATES
    // =========================================================

    private final Map<Integer, ActivityState> lastStates =
            new ConcurrentHashMap<>();


    // =========================================================
    // PER-EMPLOYEE START / STOP LOCKS
    // =========================================================

    private final Map<Integer, Object> trackerLocks =
            new ConcurrentHashMap<>();


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ActivityTrackerService(
            ActivityService activityService,
            ActiveWindowService activeWindowService,
            ApplicationActivityRepo applicationActivityRepo,
            SessionRepo sessionRepo,
            UserRepo userRepo,
            DiscoveredApplicationService discoveredApplicationService,
            IpApplicationRuleService ipApplicationRuleService,
            CurrentNetworkIpService currentNetworkIpService,
            ApplicationEnforcementService applicationEnforcementService
    ) {

        this.activityService = activityService;
        this.activeWindowService = activeWindowService;
        this.applicationActivityRepo = applicationActivityRepo;
        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.discoveredApplicationService = discoveredApplicationService;
        this.ipApplicationRuleService = ipApplicationRuleService;
        this.currentNetworkIpService = currentNetworkIpService;
        this.applicationEnforcementService = applicationEnforcementService;
    }


    // =========================================================
    // START TRACKING
    // =========================================================

    public void startTracking(Integer userId) {

        Object lock = trackerLocks.computeIfAbsent(
                userId,
                id -> new Object()
        );

        synchronized (lock) {

            // Already tracking.
            if (trackers.containsKey(userId)) {
                return;
            }

            // Find current open session.
            Session session = getCurrentOpenSession(userId);

            if (session == null) {

                System.out.println(
                        "Cannot start activity tracking. "
                                + "No open session for User: "
                                + userId
                );

                return;
            }

            // Employee must explicitly click Start Work.
            if (session.getEmployeeStartedAt() == null
                    || session.isWorkPaused()) {

                System.out.println(
                        "Cannot start activity tracking. "
                                + "Employee has not started or session is paused. "
                                + "User: " + userId
                );

                return;
            }

            ActivityTracker tracker = new ActivityTracker();

            ApplicationActivityTracker applicationTracker = null;

            try {

                System.out.println(
                        "========== STARTING ACTIVITY TRACKER =========="
                );

                System.out.println("USER ID = " + userId);
                System.out.println("SESSION ID = " + session.getId());
                System.out.println(
                        "EMPLOYEE STARTED AT = "
                                + session.getEmployeeStartedAt()
                );

                // 1. Initialize active / idle counters.
                tracker.initializeCounters(
                        session.getActiveSeconds() != null
                                ? session.getActiveSeconds()
                                : 0,

                        session.getIdleSeconds() != null
                                ? session.getIdleSeconds()
                                : 0
                );

                // 2. Start active / idle tracking.
                tracker.start();

                // 3. Start database activity tracking.
                activityService.startTracking(userId);

                // 4. Create application tracker using
                // the injected ActiveWindowService.
                applicationTracker =
                        new ApplicationActivityTracker(
                                userId,
                                activeWindowService,
                                applicationActivityRepo,
                                sessionRepo,
                                userRepo,
                                discoveredApplicationService,
                                ipApplicationRuleService,
                                currentNetworkIpService,
                                applicationEnforcementService
                        );

                // 5. Start application tracking.
                applicationTracker.start();

                // 6. Register trackers only after successful startup.
                trackers.put(userId, tracker);

                applicationTrackers.put(
                        userId,
                        applicationTracker
                );

                // 7. Store initial state.
                lastStates.put(
                        userId,
                        tracker.getCurrentState()
                );

                System.out.println(
                        "Activity tracking started for User: " + userId
                );

                System.out.println(
                        "Application tracking started for User: " + userId
                );

                System.out.println(
                        "TRACKER CREATED = "
                                + trackers.containsKey(userId)
                );

                System.out.println(
                        "==============================================="
                );

            } catch (Exception e) {

                System.err.println(
                        "Error starting tracking for User "
                                + userId + ": " + e.getMessage()
                );

                e.printStackTrace();

                // Clean up partially started services.
                if (applicationTracker != null) {
                    try {
                        applicationTracker.stop();
                    } catch (Exception stopError) {
                        stopError.printStackTrace();
                    }
                }

                try {
                    tracker.stop();
                } catch (Exception stopError) {
                    stopError.printStackTrace();
                }

                applicationTrackers.remove(userId);
                trackers.remove(userId);
                lastStates.remove(userId);

                activityService.stopTracking(userId);
            }
        }
    }


    // =========================================================
    // MONITOR ACTIVITY STATES
    // =========================================================

    @Scheduled(fixedDelay = 5000)
    public void monitorActivityStates() {

        for (Map.Entry<Integer, ActivityTracker> entry
                : trackers.entrySet()) {

            Integer userId = entry.getKey();

            ActivityTracker tracker = entry.getValue();

            synchronized (tracker) {

                // Ensure tracker is still active.
                if (trackers.get(userId) != tracker) {
                    continue;
                }

                updateActivityState(userId);

                syncSessionActivity(userId);
            }
        }
    }


    // =========================================================
    // CHECK STATE CHANGE
    // =========================================================

    public void updateActivityState(Integer userId) {

        ActivityTracker tracker = trackers.get(userId);

        if (tracker == null) {
            return;
        }

        ActivityState currentState =
                tracker.getCurrentState();

        ActivityState previousState =
                lastStates.get(userId);

        // First state.
        if (previousState == null) {

            lastStates.put(
                    userId,
                    currentState
            );

            activityService.startTracking(userId);

            return;
        }

        // State changed.
        if (currentState != previousState) {

            activityService.updateState(
                    userId,
                    currentState
            );

            lastStates.put(
                    userId,
                    currentState
            );

            System.out.println(
                    "User "
                            + userId
                            + " state changed: "
                            + previousState
                            + " -> "
                            + currentState
            );
        }
    }


    // =========================================================
    // SYNC TRACKER TIME -> SESSION
    // =========================================================

    private void syncSessionActivity(Integer userId) {

        ActivityTracker tracker = trackers.get(userId);

        if (tracker == null) {
            return;
        }

        Session session = getCurrentOpenSession(userId);

        if (session == null) {
            return;
        }

        // Employee must have clicked Start Work.
        if (session.getEmployeeStartedAt() == null
                || session.isWorkPaused()) {
            return;
        }

        // Get counters.
        long activeSeconds = tracker.getActiveSeconds();

        long idleSeconds = tracker.getIdleSeconds();

        // Save seconds.
        session.setActiveSeconds((int) activeSeconds);

        session.setIdleSeconds((int) idleSeconds);

        // Save minutes.
        session.setActiveMinutes((int) (activeSeconds / 60));

        session.setIdleMinutes((int) (idleSeconds / 60));

        // Calculate productivity.
        long totalSeconds = activeSeconds + idleSeconds;

        if (totalSeconds > 0) {

            double productivity =
                    ((double) activeSeconds / totalSeconds) * 100.0;

            session.setProductivity(productivity);
        }

        // Save session.
        sessionRepo.save(session);

        System.out.println(
                "User "
                        + userId
                        + " | ACTIVE: "
                        + activeSeconds
                        + " sec | IDLE: "
                        + idleSeconds
                        + " sec"
        );
    }


    // =========================================================
    // GET CURRENT OPEN SESSION
    // =========================================================

    private Session getCurrentOpenSession(Integer userId) {

        List<Session> sessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        if (sessions.isEmpty()) {
            return null;
        }

        return sessions.stream()
                .max(
                        (s1, s2) ->
                                s1.getLoginTime().compareTo(
                                        s2.getLoginTime()
                                )
                )
                .orElse(null);
    }


    // =========================================================
    // STOP TRACKING
    // =========================================================

    public void stopTracking(Integer userId) {

        Object lock = trackerLocks.computeIfAbsent(
                userId,
                id -> new Object()
        );

        synchronized (lock) {

            ActivityTracker tracker = trackers.get(userId);

            if (tracker == null) {
                return;
            }

            synchronized (tracker) {

                // Confirm tracker is still active.
                if (trackers.get(userId) != tracker) {
                    return;
                }

                // 1. Stop application tracking.
                ApplicationActivityTracker applicationTracker =
                        applicationTrackers.remove(userId);

                if (applicationTracker != null) {

                    applicationTracker.stop();

                    System.out.println(
                            "Application tracking stopped for User: "
                                    + userId
                    );
                }

                // 2. Stop active / idle tracker.
                tracker.stop();

                // 3. Save final counters.
                syncSessionActivity(userId);

                // 4. Remove tracker.
                trackers.remove(userId, tracker);

                lastStates.remove(userId);

                // 5. Close current database activity.
                activityService.stopTracking(userId);

                System.out.println(
                        "Activity tracking stopped for User: " + userId
                );
            }
        }
    }


    // =========================================================
    // GET ACTIVE SECONDS
    // =========================================================

    public long getActiveSeconds(Integer userId) {

        ActivityTracker tracker = trackers.get(userId);

        if (tracker != null) {
            return tracker.getActiveSeconds();
        }

        Session session = getCurrentOpenSession(userId);

        if (session == null || session.getActiveSeconds() == null) {
            return 0L;
        }

        return session.getActiveSeconds();
    }


    // =========================================================
    // GET IDLE SECONDS
    // =========================================================

    public long getIdleSeconds(Integer userId) {

        ActivityTracker tracker = trackers.get(userId);

        if (tracker != null) {
            return tracker.getIdleSeconds();
        }

        Session session = getCurrentOpenSession(userId);

        if (session == null || session.getIdleSeconds() == null) {
            return 0L;
        }

        return session.getIdleSeconds();
    }


    // =========================================================
    // GET CURRENT STATE
    // =========================================================

    public ActivityState getCurrentState(Integer userId) {

        ActivityTracker tracker = trackers.get(userId);

        return tracker != null
                ? tracker.getCurrentState()
                : ActivityState.IDLE;
    }


    // =========================================================
    // CHECK TRACKING
    // =========================================================

    public boolean isTracking(Integer userId) {

        return trackers.containsKey(userId);
    }


    // =========================================================
    // APPLICATION SHUTDOWN
    // =========================================================

    @PreDestroy
    public void shutdownTracking() {

        System.out.println(
                "Shutting down all activity trackers..."
        );

        // Stop each employee's tracking cleanly.
        for (Integer userId : trackers.keySet()) {

            try {
                stopTracking(userId);

            } catch (Exception e) {

                System.err.println(
                        "Error stopping trackers for User "
                                + userId + ": " + e.getMessage()
                );

                e.printStackTrace();
            }
        }

        // Stop any remaining application trackers.
        for (ApplicationActivityTracker tracker :
                applicationTrackers.values()) {

            try {
                tracker.stop();

            } catch (Exception e) {

                System.err.println(
                        "Error stopping application tracker: "
                                + e.getMessage()
                );
            }
        }

        applicationTrackers.clear();
        trackers.clear();
        lastStates.clear();

        // Do not unregister the process-wide native hook here.
        // Spring context shutdown does not always mean JVM shutdown.

        System.out.println(
                "All activity trackers shut down."
        );
    }
}
