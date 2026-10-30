package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.TodayActivityDTO;
import com.example.TeamLoggerC.entity.ApplicationActivity;
import com.example.TeamLoggerC.entity.InputActivity;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.ApplicationActivityRepo;
import com.example.TeamLoggerC.repository.InputActivityRepo;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.tracker.ActivityState;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AdminTrackingService {

    private final UserRepo userRepo;
    private final SessionRepo sessionRepo;
    private final ApplicationActivityRepo applicationActivityRepo;
    private final InputActivityRepo inputActivityRepo;
    private final ActivityTrackerService trackerService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public AdminTrackingService(
            UserRepo userRepo,
            SessionRepo sessionRepo,
            ApplicationActivityRepo applicationActivityRepo,
            InputActivityRepo inputActivityRepo,
            ActivityTrackerService trackerService) {

        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.applicationActivityRepo = applicationActivityRepo;
        this.inputActivityRepo = inputActivityRepo;
        this.trackerService = trackerService;
    }


    // =====================================================
    // GET TODAY'S ACTIVITY
    // =====================================================

    public List<TodayActivityDTO> getTodayActivity(
            Integer adminId) {

        List<TodayActivityDTO> activityList =
                new ArrayList<>();


        // -------------------------------------------------
        // Get employees belonging to this admin
        // -------------------------------------------------

        List<User> employees =
                userRepo.findByAdminIdAndRole(
                        adminId,
                        "EMPLOYEE"
                );

        if (employees.isEmpty()) {
            return activityList;
        }


        // -------------------------------------------------
        // Employee IDs
        // -------------------------------------------------

        List<Integer> employeeIds =
                employees.stream()
                        .map(User::getId)
                        .toList();


        // -------------------------------------------------
        // Today's date/time
        // -------------------------------------------------

        LocalDate today =
                LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.atTime(LocalTime.MAX);


        // -------------------------------------------------
        // Get today's sessions
        // -------------------------------------------------

        List<Session> sessions =
                sessionRepo.findByUserIdInAndLoginTimeBetween(
                        employeeIds,
                        startOfDay,
                        endOfDay
                );


        // -------------------------------------------------
        // Create activity row for each session
        // -------------------------------------------------

        for (Session session : sessions) {

            User employee =
                    employees.stream()
                            .filter(user ->
                                    user.getId()
                                            .equals(
                                                    session.getUserId()
                                            )
                            )
                            .findFirst()
                            .orElse(null);

            if (employee == null) {
                continue;
            }


            TodayActivityDTO dto =
                    new TodayActivityDTO();


            // =================================================
            // EMPLOYEE INFORMATION
            // =================================================

            dto.setEmployeeId(
                    employee.getId()
            );

            dto.setEmployeeName(
                    employee.getName()
            );

            dto.setEmployeeLoginId(
                    employee.getEmployeeLoginId()
            );

            dto.setDesignation(
                    employee.getDesignation()
            );


            // =================================================
            // SESSION INFORMATION
            // =================================================

            dto.setSessionId(
                    session.getId()
            );

            dto.setLoginTime(
                    formatTime(
                            session.getLoginTime()
                    )
            );

            dto.setLogoutTime(
                    formatTime(
                            session.getLogoutTime()
                    )
            );


            // =================================================
            // ACTIVE / IDLE TIME
            // =================================================

            long activeSeconds =
                    session.getActiveSeconds() != null
                            ? session.getActiveSeconds()
                            : 0;

            long idleSeconds =
                    session.getIdleSeconds() != null
                            ? session.getIdleSeconds()
                            : 0;


            /*
             * If this session is currently being tracked,
             * use the live tracker values.
             *
             * This makes the admin page show current values
             * instead of waiting for another database update.
             */
            if (trackerService.isTracking(
                    employee.getId())) {

                activeSeconds =
                        trackerService.getActiveSeconds(
                                employee.getId()
                        );

                idleSeconds =
                        trackerService.getIdleSeconds(
                                employee.getId()
                        );
            }


            dto.setActiveSeconds(
                    activeSeconds
            );

            dto.setIdleSeconds(
                    idleSeconds
            );

            dto.setActiveTime(
                    formatDuration(
                            activeSeconds
                    )
            );

            dto.setIdleTime(
                    formatDuration(
                            idleSeconds
                    )
            );


            // =================================================
            // CURRENT APPLICATION
            // =================================================

            ApplicationActivity latestApplication =
                    applicationActivityRepo
                            .findFirstBySessionIdOrderByStartTimeDesc(
                                    session.getId()
                            );

            if (latestApplication != null) {

                dto.setCurrentApplication(
                        latestApplication
                                .getApplicationName()
                );

                dto.setCurrentWindow(
                        latestApplication
                                .getWindowTitle()
                );

            } else {

                dto.setCurrentApplication(
                        "-"
                );

                dto.setCurrentWindow(
                        "-"
                );
            }


            // =================================================
            // WORK STATUS
            // =================================================

            String status =
                    determineStatus(
                            session,
                            employee.getId()
                    );

            dto.setStatus(status);


            // =================================================
            // INPUT ACTIVITY
            // =================================================

            LocalDateTime inputStart =
                    session.getLoginTime();

            LocalDateTime inputEnd =
                    session.getLogoutTime() != null
                            ? session.getLogoutTime()
                            : LocalDateTime.now();


            List<InputActivity> inputActivities =
                    inputActivityRepo
                            .findBySessionIdAndEventTimeBetween(
                                    session.getId(),
                                    inputStart,
                                    inputEnd
                            );


            long mouseClicks = 0;
            long keyboardActivity = 0;
            long mouseMovement = 0;


            for (InputActivity input :
                    inputActivities) {

                if ("MOUSE_CLICK".equalsIgnoreCase(
                        input.getActivityType()
                )) {

                    mouseClicks++;

                } else if (
                        "KEYBOARD_ACTIVITY"
                                .equalsIgnoreCase(
                                        input.getActivityType()
                                )
                ) {

                    keyboardActivity++;

                } else if (
                        "MOUSE_MOVE"
                                .equalsIgnoreCase(
                                        input.getActivityType()
                                )
                ) {

                    mouseMovement++;
                }
            }


            dto.setMouseClicks(
                    mouseClicks
            );

            dto.setKeyboardActivity(
                    keyboardActivity
            );

            dto.setMouseMovement(
                    mouseMovement
            );


            // =================================================
            // ADD DTO
            // =================================================

            activityList.add(dto);
        }


        return activityList;
    }


    // =====================================================
    // DETERMINE EMPLOYEE STATUS
    // =====================================================

    private String determineStatus(
            Session session,
            Integer userId) {


        // -------------------------------------------------
        // 1. Session is closed
        // -------------------------------------------------

        if (session.getLogoutTime() != null) {

            return "OFFLINE";
        }


        // -------------------------------------------------
        // 2. Employee has not clicked Start Work
        // -------------------------------------------------
        //
        // This is important for the automatic 10 AM
        // office session.
        //
        // The session can exist while the employee is
        // still OFFLINE from the work-tracking perspective.
        // -------------------------------------------------

        if (session.getEmployeeStartedAt() == null) {

            return "OFFLINE";
        }


        // -------------------------------------------------
        // 3. Tracker is not running
        // -------------------------------------------------

        if (!trackerService.isTracking(userId)) {

            return "OFFLINE";
        }


        // -------------------------------------------------
        // 4. Get live tracker state
        // -------------------------------------------------

        ActivityState state =
                trackerService.getCurrentState(userId);


        // -------------------------------------------------
        // 5. Employee is idle
        // -------------------------------------------------

        if (state == ActivityState.IDLE) {

            return "IDLE";
        }


        // -------------------------------------------------
        // 6. Employee is actively working
        // -------------------------------------------------

        return "WORKING";
    }


    // =====================================================
    // FORMAT TIME
    // =====================================================

    private String formatTime(
            LocalDateTime time) {

        if (time == null) {
            return "-";
        }

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(
                        "hh:mm a"
                );

        return time.format(formatter);
    }


    // =====================================================
    // FORMAT DURATION
    // =====================================================

    private String formatDuration(
            long seconds) {

        if (seconds <= 0) {
            return "0m";
        }

        long hours =
                seconds / 3600;

        long minutes =
                (seconds % 3600) / 60;


        if (hours > 0) {

            return hours
                    + "h "
                    + minutes
                    + "m";
        }


        return minutes + "m";
    }
}