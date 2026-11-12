
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.SessionService;
import com.example.TeamLoggerC.tracker.ActivityState;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;
import com.example.TeamLoggerC.service.ActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/employee/activity")
public class EmployeeActivityController {

    private final ActivityTrackerService trackerService;
    private final SessionService sessionService;
    private final ActivityService activityService;

    public EmployeeActivityController(
            ActivityTrackerService trackerService,
            SessionService sessionService,
            ActivityService activityService) {

        this.trackerService = trackerService;
        this.sessionService = sessionService;
        this.activityService = activityService;
    }

    // =====================================================
    // CURRENT EMPLOYEE ACTIVITY
    // =====================================================

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentActivity(
            Authentication authentication) {

        // ======================================
        // AUTHENTICATION CHECK
        // ======================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        var user = userDetails.getUser();

        // ======================================
        // USER VALIDATION
        // ======================================

        if (user == null
                || !"ACTIVE".equalsIgnoreCase(
                String.valueOf(user.getStatus()))
                || !"EMPLOYEE".equalsIgnoreCase(
                String.valueOf(user.getRole()))
                || !user.isWorkstationActivated()) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }

        Integer userId = user.getId();

        // ======================================
        // CURRENT SESSION
        // ======================================

        Session session =
                sessionService.getCurrentSession(userId);

        // ======================================
        // SESSION STATE
        // ======================================

        boolean workStarted =
                session != null
                        && session.getEmployeeStartedAt() != null;

        boolean workPaused =
                workStarted && session.isWorkPaused();

        boolean workRunning =
                workStarted && !workPaused;

        /*
         * The session state is authoritative.
         *
         * A paused session must never be reported as
         * actively tracking, even if a tracker reference
         * unexpectedly remains in memory.
         */
        boolean tracking =
                workRunning && trackerService.isTracking(userId);

        String workStatus;

        if (!workStarted) {

            workStatus = "NOT_STARTED";

        } else if (workPaused) {

            workStatus = "PAUSED";

        } else {

            workStatus = "WORKING";
        }

        // ======================================
        // ACTIVITY COUNTERS
        // ======================================

        long activeSeconds = 0L;
        long idleSeconds = 0L;

        String state = "STOPPED";

        if (session != null && workStarted) {

            if (workPaused) {

                /*
                 * A paused session uses the counters
                 * saved in the database.
                 */
                activeSeconds =
                        session.getActiveSeconds() != null
                                ? session.getActiveSeconds()
                                : 0L;

                idleSeconds =
                        session.getIdleSeconds() != null
                                ? session.getIdleSeconds()
                                : 0L;

                state = "PAUSED";

            } else if (tracking) {

                /*
                 * A running session uses live tracker
                 * counters.
                 */
                activeSeconds =
                        trackerService.getActiveSeconds(userId);

                idleSeconds =
                        trackerService.getIdleSeconds(userId);

                ActivityState currentState =
                        trackerService.getCurrentState(userId);

                state = currentState != null
                        ? currentState.name()
                        : "ACTIVE";

            } else {

                /*
                 * Session has started but tracker is not
                 * currently available.
                 *
                 * Return the last saved counters rather
                 * than resetting them to zero.
                 */
                activeSeconds =
                        session.getActiveSeconds() != null
                                ? session.getActiveSeconds()
                                : 0L;

                idleSeconds =
                        session.getIdleSeconds() != null
                                ? session.getIdleSeconds()
                                : 0L;

                state = "STOPPED";
            }
        }

        // ======================================
        // RESPONSE
        // ======================================

        Map<String, Object> response = new HashMap<>();

        // Session information
        response.put(
                "sessionId",
                session != null ? session.getId() : null
        );

        response.put("workStarted", workStarted);
        response.put("workPaused", workPaused);
        response.put("workRunning", workRunning);
        response.put("workStatus", workStatus);

        // Activity information
        response.put("activeSeconds", activeSeconds);
        response.put("idleSeconds", idleSeconds);

        response.put("activeHours", activeSeconds / 3600);

        response.put(
                "activeRemainingMinutes",
                (activeSeconds % 3600) / 60
        );

        response.put("idleHours", idleSeconds / 3600);

        response.put(
                "idleRemainingMinutes",
                (idleSeconds % 3600) / 60
        );

        // Tracker information
        response.put("state", state);
        response.put("tracking", tracking);

        return ResponseEntity.ok(response);
    }

    // =====================================================
    // WEEKLY PRODUCTIVITY
    // =====================================================

    @GetMapping("/productivity/weekly")
    public ResponseEntity<?> getWeeklyProductivity(
            Authentication authentication) {

        // ======================================
        // AUTHENTICATION CHECK
        // ======================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .build();
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        var user = userDetails.getUser();

        // ======================================
        // USER VALIDATION
        // ======================================

        if (user == null
                || !"ACTIVE".equalsIgnoreCase(
                String.valueOf(user.getStatus()))
                || !"EMPLOYEE".equalsIgnoreCase(
                String.valueOf(user.getRole()))
                || !user.isWorkstationActivated()) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .build();
        }

        // ======================================
        // WEEKLY PRODUCTIVITY DATA
        // ======================================

        return ResponseEntity.ok(
                activityService.getWeeklyProductivity(user.getId())
        );
    }
}
