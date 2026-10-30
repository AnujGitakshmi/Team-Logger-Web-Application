package com.example.TeamLoggerC.tracker;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.ApplicationActivityRepo;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.ActivityService;
import com.example.TeamLoggerC.service.ApplicationEnforcementService;
import com.example.TeamLoggerC.service.CurrentNetworkIpService;
import com.example.TeamLoggerC.service.DiscoveredApplicationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.TeamLoggerC.service.IpApplicationRuleService;

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

    /*
     * Used by ApplicationActivityTracker to resolve:
     *
     * userId -> User -> adminId
     */
    private final UserRepo userRepo;

    /*
     * Used to automatically add detected applications
     * into discovered_applications.
     */
    private final DiscoveredApplicationService discoveredApplicationService;

    /*
     * Used by ApplicationActivityTracker to detect
     * the employee machine's current IPv4 address.
     *
     * Current IP
     *     ↓
     * IP-specific policy
     *     ↓
     * Employee policy
     *     ↓
     * Application permission
     */
    private final CurrentNetworkIpService currentNetworkIpService;


    // =========================================================
    // ACTIVE / IDLE TRACKERS
    // =========================================================

    private final Map<Integer, ActivityTracker> trackers =
            new ConcurrentHashMap<>();


    // =========================================================
    // APPLICATION TRACKERS
    // =========================================================

    private final Map<Integer, ApplicationActivityTracker>
            applicationTrackers =
            new ConcurrentHashMap<>();


    // =========================================================
    // LAST ACTIVITY STATES
    // =========================================================

    private final Map<Integer, ActivityState> lastStates =
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

        this.applicationActivityRepo =
                applicationActivityRepo;

        this.sessionRepo = sessionRepo;

        this.userRepo = userRepo;

        this.discoveredApplicationService =
                discoveredApplicationService;

        this.ipApplicationRuleService = ipApplicationRuleService;

        this.currentNetworkIpService =
                currentNetworkIpService;

        this.applicationEnforcementService =
                applicationEnforcementService;
    }


    // =========================================================
    // START TRACKING
    // =========================================================

    public void startTracking(Integer userId) {

        // -----------------------------------------------------
        // Already tracking
        // -----------------------------------------------------

        if (trackers.containsKey(userId)) {
            return;
        }


        // -----------------------------------------------------
        // Find current session
        // -----------------------------------------------------

        Session session =
                getCurrentOpenSession(userId);

        if (session == null) {

            System.out.println(
                    "Cannot start activity tracking. "
                            + "No open session for User: "
                            + userId
            );

            return;
        }


        // -----------------------------------------------------
        // Employee must click Start Work
        // -----------------------------------------------------

        if (session.getEmployeeStartedAt() == null) {

            System.out.println(
                    "Cannot start activity tracking. "
                            + "Employee has not clicked Start Work. "
                            + "User: "
                            + userId
            );

            return;
        }


        // =====================================================
        // 1. CREATE ACTIVE / IDLE TRACKER
        // =====================================================

        ActivityTracker tracker =
                new ActivityTracker();

        tracker.start();

        trackers.put(
                userId,
                tracker
        );


        // =====================================================
        // 2. START ACTIVITY SERVICE
        // =====================================================

        activityService.startTracking(userId);


        // =====================================================
        // 3. CREATE APPLICATION TRACKER
        // =====================================================

        ActiveWindowService activeWindowService =
                new ActiveWindowService();


        /*
         * ApplicationActivityTracker receives:
         *
         * 1. userId
         * 2. ActiveWindowService
         * 3. ApplicationActivityRepo
         * 4. SessionRepo
         * 5. UserRepo
         * 6. DiscoveredApplicationService
         * 7. ApplicationPolicyService
         * 8. CurrentNetworkIpService
         * 9. ApplicationEnforcementService
         *
         * Application policy resolution:
         *
         * Employee
         *      ↓
         * Current machine IP
         *      ↓
         * IP-specific AccessPolicy
         *      ↓
         * Employee AccessPolicy
         *      ↓
         * ApplicationPermission
         *      ↓
         * ALLOW / BLOCK / RESTRICT
         *      ↓
         * Windows process enforcement
         */

        ApplicationActivityTracker
                applicationActivityTracker =
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


        applicationActivityTracker.start();


        applicationTrackers.put(
                userId,
                applicationActivityTracker
        );


        // =====================================================
        // 4. STORE INITIAL STATE
        // =====================================================

        lastStates.put(
                userId,
                tracker.getCurrentState()
        );


        System.out.println(
                "Activity tracking started for User: "
                        + userId
        );

        System.out.println(
                "Application tracking started for User: "
                        + userId
        );
    }


    // =========================================================
    // MONITOR ACTIVITY STATES
    // =========================================================

    @Scheduled(fixedDelay = 5000)
    public void monitorActivityStates() {

        for (Integer userId : trackers.keySet()) {

            updateActivityState(userId);

            syncSessionActivity(userId);
        }
    }


    // =========================================================
    // CHECK STATE CHANGE
    // =========================================================

    public void updateActivityState(Integer userId) {

        ActivityTracker tracker =
                trackers.get(userId);

        if (tracker == null) {
            return;
        }


        ActivityState currentState =
                tracker.getCurrentState();


        ActivityState previousState =
                lastStates.get(userId);


        // -----------------------------------------------------
        // First state
        // -----------------------------------------------------

        if (previousState == null) {

            lastStates.put(
                    userId,
                    currentState
            );

            activityService.startTracking(userId);

            return;
        }


        // -----------------------------------------------------
        // STATE CHANGED
        // -----------------------------------------------------

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

        ActivityTracker tracker =
                trackers.get(userId);

        if (tracker == null) {
            return;
        }


        Session session =
                getCurrentOpenSession(userId);

        if (session == null) {
            return;
        }


        // -----------------------------------------------------
        // Employee must have clicked Start Work
        // -----------------------------------------------------

        if (session.getEmployeeStartedAt() == null) {
            return;
        }


        // -----------------------------------------------------
        // Get counters
        // -----------------------------------------------------

        long activeSeconds =
                tracker.getActiveSeconds();

        long idleSeconds =
                tracker.getIdleSeconds();


        // -----------------------------------------------------
        // Save seconds
        // -----------------------------------------------------

        session.setActiveSeconds(
                (int) activeSeconds
        );

        session.setIdleSeconds(
                (int) idleSeconds
        );


        // -----------------------------------------------------
        // Save minutes
        // -----------------------------------------------------

        session.setActiveMinutes(
                (int) (activeSeconds / 60)
        );

        session.setIdleMinutes(
                (int) (idleSeconds / 60)
        );


        // -----------------------------------------------------
        // Productivity
        // -----------------------------------------------------

        long totalSeconds =
                activeSeconds + idleSeconds;


        if (totalSeconds > 0) {

            double productivity =
                    ((double) activeSeconds
                            / totalSeconds)
                            * 100.0;

            session.setProductivity(
                    productivity
            );
        }


        // -----------------------------------------------------
        // Save session
        // -----------------------------------------------------

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

    private Session getCurrentOpenSession(
            Integer userId
    ) {

        List<Session> sessions =
                sessionRepo
                        .findByUserIdAndLogoutTimeIsNull(
                                userId
                        );


        if (sessions.isEmpty()) {
            return null;
        }


        return sessions.stream()
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
    // STOP TRACKING
    // =========================================================

    public void stopTracking(Integer userId) {

        ActivityTracker tracker =
                trackers.get(userId);

        if (tracker == null) {
            return;
        }


        // -----------------------------------------------------
        // Final synchronization
        // -----------------------------------------------------

        syncSessionActivity(userId);


        // =====================================================
        // STOP APPLICATION TRACKING FIRST
        // =====================================================

        ApplicationActivityTracker
                applicationActivityTracker =
                applicationTrackers.remove(userId);


        if (applicationActivityTracker != null) {

            applicationActivityTracker.stop();


            System.out.println(
                    "Application tracking stopped for User: "
                            + userId
            );
        }


        // =====================================================
        // REMOVE ACTIVE / IDLE TRACKER
        // =====================================================

        trackers.remove(userId);

        lastStates.remove(userId);


        // =====================================================
        // STOP NATIVE INPUT TRACKING
        // =====================================================

        tracker.stop();


        // =====================================================
        // CLOSE ACTIVE / IDLE DATABASE ACTIVITY
        // =====================================================

        activityService.stopTracking(userId);


        System.out.println(
                "Activity tracking stopped for User: "
                        + userId
        );
    }


    // =========================================================
    // GET ACTIVE SECONDS
    // =========================================================

    public long getActiveSeconds(Integer userId) {

        ActivityTracker tracker =
                trackers.get(userId);


        return tracker != null
                ? tracker.getActiveSeconds()
                : 0;
    }


    // =========================================================
    // GET IDLE SECONDS
    // =========================================================

    public long getIdleSeconds(Integer userId) {

        ActivityTracker tracker =
                trackers.get(userId);


        return tracker != null
                ? tracker.getIdleSeconds()
                : 0;
    }


    // =========================================================
    // GET CURRENT STATE
    // =========================================================

    public ActivityState getCurrentState(
            Integer userId
    ) {

        ActivityTracker tracker =
                trackers.get(userId);


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
}
