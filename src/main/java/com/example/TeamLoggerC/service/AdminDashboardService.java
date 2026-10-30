package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.AdminEmployeeActivityDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.tracker.ActivityState;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.example.TeamLoggerC.dto.AdminScreenRecordingDTO;

@Service
public class AdminDashboardService {

    private final UserRepo userRepo;
    private final SessionRepo sessionRepo;
    private final ActivityTrackerService trackerService;
    private final ScreenRecordingService screenRecordingService;

    public AdminDashboardService(
            UserRepo userRepo,
            SessionRepo sessionRepo,
            ActivityTrackerService trackerService,
            ScreenRecordingService screenRecordingService) {

        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.trackerService = trackerService;
        this.screenRecordingService = screenRecordingService;
    }

    /**
     * Get all employees belonging to the logged-in admin.
     */
    public List<User> getEmployees(Integer adminId) {

        return userRepo.findByAdminIdAndRole(
                adminId,
                "EMPLOYEE"
        );
    }

    /**
     * Total number of employees under this admin.
     */
    public long getTotalEmployees(Integer adminId) {

        return userRepo.countByAdminIdAndRole(
                adminId,
                "EMPLOYEE"
        );
    }

    /**
     * Number of employees currently having an open session.
     */
    public long getWorkingNow(Integer adminId) {

        List<User> employees = getEmployees(adminId);

        if (employees.isEmpty()) {
            return 0;
        }

        List<Integer> employeeIds =
                employees.stream()
                        .map(User::getId)
                        .toList();

        List<Session> openSessions =
                sessionRepo.findByUserIdInAndLogoutTimeIsNull(
                        employeeIds
                );

        return openSessions.stream()
                .filter(session ->
                        trackerService.isTracking(
                                session.getUserId()
                        ))
                .map(Session::getUserId)
                .distinct()
                .count();
    }

    /**
     * Total active time of all employees today.
     */
    public long getTodayActiveSeconds(Integer adminId) {

        List<User> employees = getEmployees(adminId);

        if (employees.isEmpty()) {
            return 0;
        }

        List<Integer> employeeIds =
                employees.stream()
                        .map(User::getId)
                        .toList();

        LocalDate today = LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdInAndLoginTimeBetween(
                        employeeIds,
                        start,
                        end
                );

        return sessions.stream()
                .mapToLong(session ->
                        session.getActiveSeconds() != null
                                ? session.getActiveSeconds()
                                : 0
                )
                .sum();
    }

    /**
     * Total idle time of all employees today.
     */
    public long getTodayIdleSeconds(Integer adminId) {

        List<User> employees = getEmployees(adminId);

        if (employees.isEmpty()) {
            return 0;
        }

        List<Integer> employeeIds =
                employees.stream()
                        .map(User::getId)
                        .toList();

        LocalDate today = LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdInAndLoginTimeBetween(
                        employeeIds,
                        start,
                        end
                );

        return sessions.stream()
                .mapToLong(session ->
                        session.getIdleSeconds() != null
                                ? session.getIdleSeconds()
                                : 0
                )
                .sum();
    }

    /**
     * Team activity for admin dashboard.
     */
    public List<AdminEmployeeActivityDTO> getTeamActivity(
            Integer adminId) {

        List<User> employees =
                getEmployees(adminId);

        List<AdminEmployeeActivityDTO> result =
                new ArrayList<>();

        LocalDate today = LocalDate.now();

        LocalDateTime start =
                today.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        for (User employee : employees) {

            Integer employeeId =
                    employee.getId();

            List<Session> sessions =
                    sessionRepo.findByUserIdAndLoginTimeBetween(
                            employeeId,
                            start,
                            end
                    );

            long activeSeconds =
                    sessions.stream()
                            .mapToLong(session ->
                                    session.getActiveSeconds() != null
                                            ? session.getActiveSeconds()
                                            : 0
                            )
                            .sum();

            long idleSeconds =
                    sessions.stream()
                            .mapToLong(session ->
                                    session.getIdleSeconds() != null
                                            ? session.getIdleSeconds()
                                            : 0
                            )
                            .sum();

            boolean workingNow =
                    trackerService.isTracking(employeeId);

            ActivityState state =
                    trackerService.getCurrentState(employeeId);

            AdminEmployeeActivityDTO dto =
                    new AdminEmployeeActivityDTO();

            dto.setId(employeeId);
            dto.setName(employee.getName());
            dto.setDesignation(employee.getDesignation());
            dto.setStatus(employee.getStatus());

            dto.setWorkingNow(workingNow);

            dto.setActiveSeconds(activeSeconds);
            dto.setIdleSeconds(idleSeconds);

            dto.setActiveTime(
                    formatTime(activeSeconds)
            );

            dto.setIdleTime(
                    formatTime(idleSeconds)
            );

            dto.setCurrentState(
                    state.name()
            );

            result.add(dto);
        }

        return result;
    }

    /**
     * Format seconds as:
     * 2h 35m
     */
    private String formatTime(long seconds) {

        long hours =
                seconds / 3600;

        long minutes =
                (seconds % 3600) / 60;

        if (hours > 0) {
            return hours + "h " +
                    minutes + "m";
        }

        return minutes + "m";
    }

    /**
     * Dashboard active time formatted.
     */
    public String getTodayActiveTime(Integer adminId) {

        return formatTime(
                getTodayActiveSeconds(adminId)
        );
    }

    /**
     * Dashboard idle time formatted.
     */
    public String getTodayIdleTime(Integer adminId) {

        return formatTime(
                getTodayIdleSeconds(adminId)
        );
    }

    /**
     * Temporary overtime value.
     *
     * Overtime calculation should be implemented
     * separately once the overtime rules are finalized.
     */
    public String getMonthlyOvertime(Integer adminId) {

        return "0h";
    }

    /**
     * Get employees with screen recording information.
     */
    public List<AdminScreenRecordingDTO> getScreenRecordingEmployees(
            Integer adminId) {

        List<User> employees = getEmployees(adminId);

        List<AdminScreenRecordingDTO> result =
                new ArrayList<>();

        for (User employee : employees) {

            Integer employeeId = employee.getId();

            AdminScreenRecordingDTO dto =
                    new AdminScreenRecordingDTO();

            dto.setId(employeeId);
            dto.setName(employee.getName());
            dto.setDesignation(employee.getDesignation());
            dto.setStatus(employee.getStatus());

            boolean recording =
                    screenRecordingService.isRecording(employeeId);

            dto.setRecording(recording);

            boolean workingNow =
                    trackerService.isTracking(employeeId);

            dto.setWorkingNow(workingNow);

            /*
             * Find the employee's currently open session.
             */
            LocalDate today = LocalDate.now();

            LocalDateTime start =
                    today.atStartOfDay();

            LocalDateTime end =
                    today.plusDays(1).atStartOfDay();

            List<Session> openSessions =
                    sessionRepo
                            .findByUserIdAndLoginTimeBetweenAndLogoutTimeIsNull(
                                    employeeId,
                                    start,
                                    end
                            );

            if (!openSessions.isEmpty()) {

                Session currentSession =
                        openSessions.stream()
                                .max((s1, s2) ->
                                        s1.getLoginTime()
                                                .compareTo(s2.getLoginTime()))
                                .orElse(null);

                if (currentSession != null) {

                    dto.setSessionId(
                            currentSession.getId()
                    );

                    dto.setEmployeeStartedAt(
                            currentSession.getEmployeeStartedAt()
                    );

                    dto.setEmployeeStarted(
                            currentSession.getEmployeeStartedAt() != null
                    );
                }

            } else {

                dto.setSessionId(null);
                dto.setEmployeeStartedAt(null);
                dto.setEmployeeStarted(false);
            }

            result.add(dto);
        }

        return result;
    }
}