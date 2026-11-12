package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.OvertimeRecordDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SessionService {

    private final SessionRepo sessionRepo;
    private final UserRepo userRepo;
    private final WorkScheduleService workScheduleService;
    private final ActivityTrackerService trackerService;
    private final ScreenshotCaptureService screenshotCaptureService;
    private final ScreenRecordingService screenRecordingService;
    private final AttendanceService attendanceService;

    private static final int REGULAR_WORK_MINUTES = 8 * 60;

    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public SessionService(
            SessionRepo sessionRepo,
            UserRepo userRepo,
            WorkScheduleService workScheduleService,
            ActivityTrackerService trackerService,
            ScreenshotCaptureService screenshotCaptureService,
            ScreenRecordingService screenRecordingService,
            AttendanceService attendanceService) {

        this.sessionRepo = sessionRepo;
        this.userRepo = userRepo;
        this.workScheduleService = workScheduleService;
        this.trackerService = trackerService;
        this.screenshotCaptureService = screenshotCaptureService;
        this.screenRecordingService = screenRecordingService;
        this.attendanceService = attendanceService;
    }

    // ==========================================
    // GET TODAY'S SESSIONS
    // ==========================================

    public List<Session> getTodaySessions(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        return sessionRepo.findByUserIdAndLoginTimeBetween(
                userId,
                startOfDay,
                endOfDay
        );
    }

    // ==========================================
    // GET CURRENT OPEN SESSION
    // ==========================================

    /*
     * Finds the latest open session regardless of login date.
     *
     * Paused sessions remain open because Stop Work does
     * not set logoutTime.
     */

    public Session getCurrentSession(Integer userId) {

        return sessionRepo
                .findByUserIdAndLogoutTimeIsNull(userId)
                .stream()
                .max(Comparator.comparing(Session::getLoginTime))
                .orElse(null);
    }

    // ==========================================
    // GET EMPLOYEE
    // ==========================================

    private User getEmployee(Integer userId) {

        return userRepo.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        ));
    }

    // ==========================================
    // GET EMPLOYEE ADMIN ID
    // ==========================================

    private Integer getAdminId(User employee) {

        Integer adminId = employee.getAdminId();

        if (adminId == null) {

            throw new IllegalStateException(
                    "Employee has no assigned admin. " +
                            "Office schedule cannot be determined."
            );
        }

        return adminId;
    }

    // ==========================================
    // OFFICE HOURS STATUS
    // ==========================================

    public boolean isOfficeHours(Integer userId) {

        User employee = getEmployee(userId);

        Integer adminId = getAdminId(employee);

        return workScheduleService.isOfficeHours(adminId);
    }

    // ==========================================
    // MANUAL START / STOP ALLOWED?
    // ==========================================

    public boolean canManuallyStartOrStop() {

        return true;
    }

    // ==========================================
    // START WORK
    // ==========================================

    @Transactional
    public Session startWork(Integer userId) {

        User employee = userRepo.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        ));

        /*
         * If the application was stopped or unavailable
         * around midnight, close any previous-day session
         * before looking for the current session.
         */
        closeExpiredSessionsForUser(userId, LocalDate.now());

        Integer adminId = getAdminId(employee);
        LocalDateTime now = LocalDateTime.now();

        boolean officeHours =
                workScheduleService.isOfficeHours(adminId, now);

        Session session = getCurrentSession(userId);

        boolean attendanceNeedsStarting = false;

        // ======================================
        // EXISTING OPEN SESSION
        // ======================================

        if (session != null) {

            /*
             * An automatic office session may exist before
             * the employee clicks Start Work.
             */
            if (session.getEmployeeStartedAt() == null) {

                session.setEmployeeStartedAt(now);

                if (session.getOfficeStart() == null) {
                    session.setWorkStartedAt(now);
                }

                attendanceNeedsStarting = true;
            }

            /*
             * If this session was paused, this resumes
             * the same session instead of creating another.
             */
            session.setWorkPaused(false);

            session = sessionRepo.save(session);

            if (attendanceNeedsStarting) {
                attendanceService.markPresent(userId);
            }

            startTrackingServices(userId);

            return session;
        }

        // ======================================
        // OFFICE HOURS
        // ======================================

        if (officeHours) {

            session = createOfficeSession(userId, adminId, now);

            if (session == null) {

                throw new IllegalStateException(
                        "Unable to create or retrieve the office session."
                );
            }

            if (session.getEmployeeStartedAt() == null) {

                session.setEmployeeStartedAt(now);
                session.setWorkPaused(false);

                session = sessionRepo.save(session);

                attendanceNeedsStarting = true;
            }

            if (attendanceNeedsStarting) {
                attendanceService.markPresent(userId);
            }

            startTrackingServices(userId);

            return session;
        }

        // ======================================
        // MANUAL SESSION
        // ======================================

        session = createManualSession(userId, now);

        attendanceService.markPresent(userId);

        startTrackingServices(userId);

        return session;
    }

    // ==========================================
    // CREATE MANUAL SESSION
    // ==========================================

    private Session createManualSession(
            Integer userId,
            LocalDateTime now) {

        Session session = new Session();

        session.setUserId(userId);

        session.setLoginTime(now);
        session.setWorkStartedAt(now);
        session.setEmployeeStartedAt(now);

        session.setOfficeStart(null);
        session.setOfficeEnd(null);

        session.setTimezone(
                ZoneId.systemDefault().toString()
        );

        session.setActiveMinutes(0);
        session.setIdleMinutes(0);

        session.setActiveSeconds(0);
        session.setIdleSeconds(0);

        session.setProductivity(0.0);

        session.setLogoutTime(null);
        session.setWorkPaused(false);

        return sessionRepo.save(session);
    }

    // ==========================================
    // START TRACKING SERVICES
    // ==========================================

    private void startTrackingServices(Integer userId) {

        trackerService.startTracking(userId);

        screenshotCaptureService.startCapture(userId);

        screenRecordingService.startRecording(userId);
    }

    // ==========================================
    // STOP WORK / PAUSE SESSION
    // ==========================================

    @Transactional
    public Session stopWork(Integer userId) {

        userRepo.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        ));

        Session session = getCurrentSession(userId);

        if (session == null) {
            return null;
        }

        /*
         * An automatic office session that the employee
         * has not started cannot be paused.
         */
        if (session.getEmployeeStartedAt() == null) {
            return null;
        }

        // Avoid repeating Stop Work operations.
        if (session.isWorkPaused()) {
            return session;
        }

        // ======================================
        // STOP SCREENSHOT CAPTURE
        // ======================================

        screenshotCaptureService.stopCapture(userId);

        // ======================================
        // STOP ACTIVITY TRACKING
        // ======================================

        /*
         * Stop tracking before saving the paused state.
         * ActivityTrackerService should synchronize the
         * final active and idle counters during stop.
         */
        trackerService.stopTracking(userId);

        // ======================================
        // STOP SCREEN RECORDING
        // ======================================

        screenRecordingService.stopRecording(userId);

        // ======================================
        // PAUSE SESSION
        // ======================================

        session.setWorkPaused(true);

        /*
         * IMPORTANT:
         * Do not set logoutTime.
         * Do not end attendance.
         * Do not create a new session on resume.
         */

        return sessionRepo.save(session);
    }

    // ==========================================
    // GET TODAY'S AUTOMATIC OFFICE SESSION
    // ==========================================

    private Session getTodayOfficeSession(
            Integer userId,
            Integer adminId) {

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(
                        adminId,
                        today
                );

        return sessionRepo
                .findByUserIdAndOfficeStartAndLogoutTimeIsNull(
                        userId,
                        officeStart
                )
                .stream()
                .max(Comparator.comparing(Session::getLoginTime))
                .orElse(null);
    }

    // ==========================================
    // CHECK WHETHER EMPLOYEE STARTED WORK TODAY
    // ==========================================

    public boolean hasEmployeeStartedWork(Integer userId) {

        User employee = getEmployee(userId);

        Integer adminId = getAdminId(employee);

        Session officeSession =
                getTodayOfficeSession(userId, adminId);

        return officeSession != null
                && officeSession.getEmployeeStartedAt() != null;
    }

    // ==========================================
    // AUTOMATIC OFFICE SESSION
    // ==========================================

    /*
     * Creates or retrieves the automatic office session.
     * It does not start tracking or attendance by itself.
     */

    @Transactional
    public Session startOfficeWork(Integer userId) {

        User employee = userRepo.findByIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        ));

        /*
         * Prevent an old open session from being reused
         * when the application was unavailable overnight.
         */
        closeExpiredSessionsForUser(userId, LocalDate.now());

        Integer adminId = getAdminId(employee);

        return createOfficeSession(
                userId,
                adminId,
                LocalDateTime.now()
        );
    }

    // ==========================================
    // CREATE / RETRIEVE AUTOMATIC OFFICE SESSION
    // ==========================================

    private Session createOfficeSession(
            Integer userId,
            Integer adminId,
            LocalDateTime now) {

        LocalDate today = now.toLocalDate();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(
                        adminId,
                        today
                );

        LocalDateTime officeEnd =
                workScheduleService.getOfficeEnd(
                        adminId,
                        today
                );

        // ======================================
        // CHECK OFFICE HOURS
        // ======================================

        if (!workScheduleService.isOfficeHours(adminId, now)) {
            return null;
        }

        // ======================================
        // TRANSITION INTO OFFICE HOURS
        // ======================================

        transitionToOfficeHours(
                userId,
                adminId,
                officeStart,
                officeEnd
        );

        // ======================================
        // CHECK FOR EXISTING OPEN SESSION
        // ======================================

        Session currentSession = getCurrentSession(userId);

        if (currentSession != null) {
            return currentSession;
        }

        // ======================================
        // CHECK EXISTING OFFICE SESSION
        // ======================================

        Session existingOfficeSession =
                sessionRepo
                        .findByUserIdAndOfficeStartAndLogoutTimeIsNull(
                                userId,
                                officeStart
                        )
                        .stream()
                        .max(Comparator.comparing(Session::getLoginTime))
                        .orElse(null);

        if (existingOfficeSession != null) {
            return existingOfficeSession;
        }

        // ======================================
        // CREATE AUTOMATIC OFFICE SESSION
        // ======================================

        Session session = new Session();

        session.setUserId(userId);

        session.setLoginTime(officeStart);

        session.setOfficeStart(officeStart);
        session.setOfficeEnd(officeEnd);

        session.setWorkStartedAt(officeStart);

        /*
         * The employee has not clicked Start Work yet.
         */
        session.setEmployeeStartedAt(null);

        session.setTimezone(
                ZoneId.systemDefault().toString()
        );

        session.setActiveMinutes(0);
        session.setIdleMinutes(0);

        session.setActiveSeconds(0);
        session.setIdleSeconds(0);

        session.setProductivity(0.0);

        session.setLogoutTime(null);
        session.setWorkPaused(false);

        return sessionRepo.save(session);
    }

    // ==========================================
    // AUTOMATIC STOP OF OFFICE SESSIONS
    // ==========================================

    /*
     * Closes automatic office sessions at their own
     * configured officeEnd time.
     *
     * Manual sessions have null officeStart and officeEnd,
     * so this method does not close manual sessions.
     */

    @Transactional
    public void stopOfficeWork() {

        LocalDateTime now = LocalDateTime.now();

        List<Session> openSessions =
                sessionRepo.findByLogoutTimeIsNull();

        for (Session candidate : openSessions) {

            if (candidate.getOfficeStart() == null
                    || candidate.getOfficeEnd() == null) {
                continue;
            }

            if (now.isBefore(candidate.getOfficeEnd())) {
                continue;
            }

            Integer userId = candidate.getUserId();

            if (userRepo.findByIdForUpdate(userId).isEmpty()) {
                continue;
            }

            Session session = sessionRepo.findById(candidate.getId())
                    .orElse(null);

            if (session == null || session.getLogoutTime() != null) {
                continue;
            }

            if (session.getOfficeStart() == null
                    || session.getOfficeEnd() == null) {
                continue;
            }

            LocalDateTime officialEndTime = session.getOfficeEnd();

            if (now.isBefore(officialEndTime)) {
                continue;
            }

            // ==================================
            // STOP EMPLOYEE SERVICES IF STARTED
            // ==================================

            if (session.getEmployeeStartedAt() != null) {

                screenshotCaptureService.stopCapture(userId);

                trackerService.stopTracking(userId);

                screenRecordingService.stopRecording(userId);
            }

            // ==================================
            // PERMANENTLY CLOSE SESSION
            // ==================================

            session.setWorkPaused(false);
            session.setLogoutTime(officialEndTime);

            sessionRepo.save(session);

            // ==================================
            // CLOSE ATTENDANCE ON CORRECT DATE
            // ==================================

            if (session.getEmployeeStartedAt() != null) {

                attendanceService.markEndTimeAt(
                        userId,
                        officialEndTime.toLocalDate(),
                        officialEndTime.toLocalTime()
                );
            }
        }
    }

    // ==========================================
    // CLOSE SESSIONS AT MIDNIGHT
    // ==========================================

    /*
     * Permanently closes sessions that belong to a
     * previous calendar date and are still open.
     *
     * A paused session is also closed at midnight.
     * Paused time is not added to activity counters.
     */

    @Transactional
    public void closeSessionsAtMidnight() {

        LocalDate today = LocalDate.now();

        List<Session> openSessions =
                sessionRepo.findByLogoutTimeIsNull();

        Set<Integer> processedUsers = new HashSet<>();

        for (Session candidate : openSessions) {

            if (candidate.getLoginTime() == null) {
                continue;
            }

            LocalDate sessionDate =
                    candidate.getLoginTime().toLocalDate();

            if (!sessionDate.isBefore(today)) {
                continue;
            }

            Integer userId = candidate.getUserId();

            if (userId == null || !processedUsers.add(userId)) {
                continue;
            }

            closeExpiredSessionsForUser(userId, today);
        }
    }

    // ==========================================
    // CLOSE PREVIOUS-DAY SESSIONS FOR ONE EMPLOYEE
    // ==========================================

    /*
     * Shared helper used by:
     * 1. The midnight scheduler.
     * 2. Start Work, in case the application was offline
     *    when midnight occurred.
     */

    private void closeExpiredSessionsForUser(
            Integer userId,
            LocalDate today) {

        if (userRepo.findByIdForUpdate(userId).isEmpty()) {
            return;
        }

        List<Session> openSessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        for (Session candidate : openSessions) {

            if (candidate.getLoginTime() == null) {
                continue;
            }

            LocalDate sessionDate =
                    candidate.getLoginTime().toLocalDate();

            /*
             * Only sessions from dates before today
             * are eligible for midnight closure.
             */
            if (!sessionDate.isBefore(today)) {
                continue;
            }

            Session session = sessionRepo.findById(candidate.getId())
                    .orElse(null);

            if (session == null || session.getLogoutTime() != null) {
                continue;
            }

            if (session.getLoginTime() == null) {
                continue;
            }

            sessionDate = session.getLoginTime().toLocalDate();

            if (!sessionDate.isBefore(today)) {
                continue;
            }

            LocalDateTime midnight =
                    sessionDate.plusDays(1).atStartOfDay();

            // ==================================
            // STOP EMPLOYEE SERVICES
            // ==================================

            if (session.getEmployeeStartedAt() != null) {

                screenshotCaptureService.stopCapture(userId);

                /*
                 * Stop tracking before closing the session.
                 * This allows the final activity counters
                 * to be synchronized while the session is open.
                 */
                trackerService.stopTracking(userId);

                screenRecordingService.stopRecording(userId);
            }

            // ==================================
            // PERMANENTLY CLOSE SESSION
            // ==================================

            session.setWorkPaused(false);
            session.setLogoutTime(midnight);

            sessionRepo.save(session);

            // ==================================
            // CLOSE ATTENDANCE ON SESSION DATE
            // ==================================

            if (session.getEmployeeStartedAt() != null) {

                attendanceService.markEndTimeAt(
                        userId,
                        sessionDate,
                        LocalTime.MIDNIGHT
                );
            }

            System.out.println(
                    "Previous-day session closed at midnight. " +
                            "User ID: " + userId +
                            ", Session ID: " + session.getId() +
                            ", Session date: " + sessionDate
            );
        }
    }

    // ==========================================
    // TODAY ACTIVE MINUTES
    // ==========================================

    public int getTodayActiveMinutes(Integer userId) {

        return getTodaySessions(userId)
                .stream()
                .mapToInt(session ->
                        session.getActiveMinutes() != null
                                ? session.getActiveMinutes()
                                : 0
                )
                .sum();
    }

    // ==========================================
    // TODAY IDLE MINUTES
    // ==========================================

    public int getTodayIdleMinutes(Integer userId) {

        return getTodaySessions(userId)
                .stream()
                .mapToInt(session ->
                        session.getIdleMinutes() != null
                                ? session.getIdleMinutes()
                                : 0
                )
                .sum();
    }

    // ==========================================
    // ACTIVE HOURS
    // ==========================================

    public int getActiveHours(Integer userId) {

        return getTodayActiveMinutes(userId) / 60;
    }

    // ==========================================
    // ACTIVE REMAINING MINUTES
    // ==========================================

    public int getActiveRemainingMinutes(Integer userId) {

        return getTodayActiveMinutes(userId) % 60;
    }

    // ==========================================
    // IDLE HOURS
    // ==========================================

    public int getIdleHours(Integer userId) {

        return getTodayIdleMinutes(userId) / 60;
    }

    // ==========================================
    // IDLE REMAINING MINUTES
    // ==========================================

    public int getIdleRemainingMinutes(Integer userId) {

        return getTodayIdleMinutes(userId) % 60;
    }

    // ==========================================
    // TODAY LOGIN TIME
    // ==========================================

    public LocalDateTime getTodayLoginTime(Integer userId) {

        return getTodaySessions(userId)
                .stream()
                .map(Session::getLoginTime)
                .min(LocalDateTime::compareTo)
                .orElse(null);
    }

    // ==========================================
    // TODAY LOGOUT TIME
    // ==========================================

    public LocalDateTime getTodayLogoutTime(Integer userId) {

        if (getCurrentSession(userId) != null) {
            return null;
        }

        return getTodaySessions(userId)
                .stream()
                .filter(session ->
                        session.getLogoutTime() != null
                )
                .map(Session::getLogoutTime)
                .max(LocalDateTime::compareTo)
                .orElse(null);
    }

    // ==========================================
    // TRANSITION INTO OFFICE HOURS
    // ==========================================

    /*
     * Closes pre-office manual sessions when the employee
     * enters the office-hours period.
     */

    @Transactional
    public void transitionToOfficeHours(Integer userId) {

        User employee = getEmployee(userId);

        Integer adminId = getAdminId(employee);

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(
                        adminId,
                        today
                );

        LocalDateTime officeEnd =
                workScheduleService.getOfficeEnd(
                        adminId,
                        today
                );

        transitionToOfficeHours(
                userId,
                adminId,
                officeStart,
                officeEnd
        );
    }

    // ==========================================
    // INTERNAL OFFICE-HOURS TRANSITION
    // ==========================================

    private void transitionToOfficeHours(
            Integer userId,
            Integer adminId,
            LocalDateTime officeStart,
            LocalDateTime officeEnd) {

        LocalDate today = officeStart.toLocalDate();

        List<Session> openSessions =
                sessionRepo
                        .findByUserIdAndLoginTimeBetweenAndLogoutTimeIsNull(
                                userId,
                                today.atStartOfDay(),
                                officeStart.plusSeconds(1)
                        );

        for (Session session : openSessions) {

            boolean isOfficeSession =
                    officeStart.equals(session.getOfficeStart())
                            && officeEnd.equals(session.getOfficeEnd());

            if (isOfficeSession) {
                continue;
            }

            if (session.getLoginTime().isAfter(officeStart)) {
                continue;
            }

            // ==================================
            // STOP SERVICES AND CLOSE ATTENDANCE
            // ==================================

            if (session.getEmployeeStartedAt() != null) {

                screenshotCaptureService.stopCapture(userId);

                trackerService.stopTracking(userId);

                screenRecordingService.stopRecording(userId);

                attendanceService.markEndTimeAt(
                        userId,
                        officeStart.toLocalDate(),
                        officeStart.toLocalTime()
                );
            }

            // ==================================
            // PERMANENTLY CLOSE OLD SESSION
            // ==================================

            session.setWorkPaused(false);
            session.setLogoutTime(officeStart);

            sessionRepo.save(session);
        }
    }

    // ==========================================
    // TODAY OVERTIME
    // ==========================================

    public int getTodayOvertimeMinutes(Integer userId) {

        int activeMinutes = getTodayActiveMinutes(userId);

        return Math.max(
                activeMinutes - REGULAR_WORK_MINUTES,
                0
        );
    }

    // ==========================================
    // THIS WEEK OVERTIME
    // ==========================================

    public int getThisWeekOvertimeMinutes(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDate startOfWeek =
                today.minusDays(
                        today.getDayOfWeek().getValue() - 1
                );

        LocalDateTime start = startOfWeek.atStartOfDay();

        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdAndLoginTimeBetween(
                        userId,
                        start,
                        end
                );

        return calculateOvertimeFromSessions(sessions);
    }

    // ==========================================
    // THIS MONTH OVERTIME
    // ==========================================

    public int getThisMonthOvertimeMinutes(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDate firstDay = today.withDayOfMonth(1);

        LocalDateTime start = firstDay.atStartOfDay();

        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdAndLoginTimeBetween(
                        userId,
                        start,
                        end
                );

        return calculateOvertimeFromSessions(sessions);
    }

    // ==========================================
    // CALCULATE OVERTIME FROM SESSIONS
    // ==========================================

    private int calculateOvertimeFromSessions(
            List<Session> sessions) {

        if (sessions == null || sessions.isEmpty()) {
            return 0;
        }

        Map<LocalDate, Integer> dailyActive = new HashMap<>();

        for (Session session : sessions) {

            if (session.getLoginTime() == null) {
                continue;
            }

            LocalDate date =
                    session.getLoginTime().toLocalDate();

            int activeMinutes =
                    session.getActiveMinutes() != null
                            ? session.getActiveMinutes()
                            : 0;

            dailyActive.merge(
                    date,
                    activeMinutes,
                    Integer::sum
            );
        }

        int totalOvertime = 0;

        for (Integer activeMinutes : dailyActive.values()) {

            totalOvertime += Math.max(
                    activeMinutes - REGULAR_WORK_MINUTES,
                    0
            );
        }

        return totalOvertime;
    }

    // ==========================================
    // DAILY OVERTIME HISTORY
    // ==========================================

    public List<OvertimeRecordDTO> getOvertimeHistory(
            Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDate startDate = today.withDayOfMonth(1);

        LocalDateTime start = startDate.atStartOfDay();

        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdAndLoginTimeBetween(
                        userId,
                        start,
                        end
                );

        Map<LocalDate, Integer> dailyActive = new HashMap<>();

        for (Session session : sessions) {

            if (session.getLoginTime() == null) {
                continue;
            }

            LocalDate date =
                    session.getLoginTime().toLocalDate();

            int activeMinutes =
                    session.getActiveMinutes() != null
                            ? session.getActiveMinutes()
                            : 0;

            dailyActive.merge(
                    date,
                    activeMinutes,
                    Integer::sum
            );
        }

        List<OvertimeRecordDTO> records = new ArrayList<>();

        for (Map.Entry<LocalDate, Integer> entry
                : dailyActive.entrySet()) {

            int activeMinutes = entry.getValue();

            int overtimeMinutes =
                    Math.max(
                            activeMinutes - REGULAR_WORK_MINUTES,
                            0
                    );

            if (overtimeMinutes > 0) {

                records.add(
                        new OvertimeRecordDTO(
                                entry.getKey(),
                                activeMinutes,
                                overtimeMinutes
                        )
                );
            }
        }

        records.sort(
                Comparator.comparing(
                        OvertimeRecordDTO::getDate
                ).reversed()
        );

        return records;
    }

    // ==========================================
    // TODAY ACTIVE SECONDS
    // ==========================================

    public long getTodayActiveSeconds(Integer userId) {

        return getTodaySessions(userId)
                .stream()
                .mapToLong(session ->
                        session.getActiveSeconds() != null
                                ? session.getActiveSeconds()
                                : 0L
                )
                .sum();
    }

    // ==========================================
    // TODAY IDLE SECONDS
    // ==========================================

    public long getTodayIdleSeconds(Integer userId) {

        return getTodaySessions(userId)
                .stream()
                .mapToLong(session ->
                        session.getIdleSeconds() != null
                                ? session.getIdleSeconds()
                                : 0L
                )
                .sum();
    }
}