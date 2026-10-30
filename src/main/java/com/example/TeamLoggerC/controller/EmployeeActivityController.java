package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.tracker.ActivityState;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;
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

    public EmployeeActivityController(
            ActivityTrackerService trackerService) {

        this.trackerService = trackerService;
    }


    @GetMapping("/current")
    public Map<String, Object> getCurrentActivity(
            Authentication authentication) {

        // ==========================================
        // GET LOGGED-IN USER
        // ==========================================

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer userId =
                userDetails.getUser().getId();


        // ==========================================
        // GET TRACKER DATA
        // ==========================================

        long activeSeconds =
                trackerService.getActiveSeconds(userId);

        long idleSeconds =
                trackerService.getIdleSeconds(userId);

        ActivityState state =
                trackerService.getCurrentState(userId);


        // ==========================================
        // RESPONSE
        // ==========================================

        Map<String, Object> response =
                new HashMap<>();

        response.put(
                "activeSeconds",
                activeSeconds
        );

        response.put(
                "idleSeconds",
                idleSeconds
        );

        response.put(
                "activeHours",
                activeSeconds / 3600
        );

        response.put(
                "activeRemainingMinutes",
                (activeSeconds % 3600) / 60
        );

        response.put(
                "idleHours",
                idleSeconds / 3600
        );

        response.put(
                "idleRemainingMinutes",
                (idleSeconds % 3600) / 60
        );

        response.put(
                "state",
                state.name()
        );

        response.put(
                "tracking",
                trackerService.isTracking(userId)
        );


        return response;
    }
}