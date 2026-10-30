package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.OvertimeRecordDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.tracker.ActivityTrackerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

@Service
public class SessionService {

    private final SessionRepo sessionRepo;
    private final WorkScheduleService workScheduleService;
    private final ActivityTrackerService trackerService;
    private final ScreenshotCaptureService screenshotCaptureService;
    private final ScreenRecordingService screenRecordingService;
    private final AttendanceService attendanceService;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public SessionService(
            SessionRepo sessionRepo,
            WorkScheduleService workScheduleService,
            ActivityTrackerService trackerService,
            ScreenshotCaptureService screenshotCaptureService,
            ScreenRecordingService screenRecordingService,
            AttendanceService attendanceService) {

        this.sessionRepo = sessionRepo;
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

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.plusDays(1).atStartOfDay();

        return sessionRepo.findByUserIdAndLoginTimeBetween(
                userId,
                startOfDay,
                endOfDay
        );
    }


    // ==========================================
    // GET CURRENT OPEN SESSION
    // ==========================================

    public Session getCurrentSession(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.plusDays(1).atStartOfDay();

        List<Session> openSessions =
                sessionRepo
                        .findByUserIdAndLoginTimeBetweenAndLogoutTimeIsNull(
                                userId,
                                startOfDay,
                                endOfDay
                        );

        if (openSessions.isEmpty()) {
            return null;
        }

        return openSessions.stream()
                .max((s1, s2) ->
                        s1.getLoginTime()
                                .compareTo(s2.getLoginTime()))
                .orElse(null);
    }


    // ==========================================
    // OFFICE HOURS STATUS
    // ==========================================

    public boolean isOfficeHours() {

        return workScheduleService.isOfficeHours();
    }


    // ==========================================
    // MANUAL START / STOP ALLOWED?
    // ==========================================

    public boolean canManuallyStartOrStop() {

        return !workScheduleService.isOfficeHours();
    }


    // ==========================================
    // START WORK
    // ==========================================

    @Transactional
    public Session startWork(Integer userId) {

        LocalDateTime now = LocalDateTime.now();

        // ======================================
        // OFFICE HOURS: 10 AM - 7 PM
        // ======================================

        if (workScheduleService.isOfficeHours(now)) {

            Session officeSession =
                    getTodayOfficeSession(userId);

            // ==================================
            // OFFICE SESSION ALREADY EXISTS
            // ==================================

            if (officeSession != null) {

                /*
                 * Record employee's Start Work click.
                 */
                if (officeSession.getEmployeeStartedAt() == null) {

                    officeSession.setEmployeeStartedAt(now);

                    officeSession =
                            sessionRepo.save(officeSession);
                }

                /*
                 * Mark attendance PRESENT.
                 *
                 * Attendance is marked only when
                 * the employee actually clicks
                 * Start Work.
                 */
                attendanceService.markPresent(userId);

                /*
                 * Start keyboard/mouse activity tracking.
                 */
                trackerService.startTracking(userId);

                /*
                 * Start screenshot capture.
                 *
                 * The first screenshot is captured immediately.
                 * Further screenshots use the Admin's configured
                 * interval.
                 */
                screenshotCaptureService.startCapture(userId);

                return officeSession;
            }

            // ==================================
            // OFFICE SESSION DOES NOT EXIST
            // ==================================

            Session newOfficeSession =
                    startOfficeWork(userId);

            if (newOfficeSession != null) {

                /*
                 * Record actual employee click time.
                 */
                newOfficeSession.setEmployeeStartedAt(now);

                newOfficeSession =
                        sessionRepo.save(newOfficeSession);

                /*
                 * Mark attendance PRESENT.
                 */
                attendanceService.markPresent(userId);

                /*
                 * Start activity tracking.
                 */
                trackerService.startTracking(userId);

                /*
                 * Start screenshot capture.
                 */
                screenshotCaptureService.startCapture(userId);
            }

            return newOfficeSession;
        }


        // ======================================
        // BEFORE 10 AM / AFTER 7 PM
        // MANUAL MODE
        // ======================================

        Session existingSession =
                getCurrentSession(userId);

        if (existingSession != null) {

            /*
             * If an open session already exists,
             * make sure attendance is also marked.
             *
             * markPresent() is safe to call repeatedly
             * because it returns the existing PRESENT record.
             */
            attendanceService.markPresent(userId);

            /*
             * If an open session already exists,
             * make sure tracking is running.
             */
            trackerService.startTracking(userId);

            /*
             * Make sure screenshot capture is running.
             */
            screenshotCaptureService.startCapture(userId);

            return existingSession;
        }


        // ======================================
        // CREATE MANUAL SESSION
        // ======================================

        Session session = new Session();

        session.setUserId(userId);

        /*
         * Manual session starts when employee
         * clicks Start Work.
         */
        session.setLoginTime(now);

        session.setWorkStartedAt(now);

        session.setOfficeStart(null);
        session.setOfficeEnd(null);

        /*
         * Employee has clicked Start Work.
         */
        session.setEmployeeStartedAt(now);

        session.setTimezone(
                ZoneId.systemDefault().toString()
        );

        session.setActiveMinutes(0);
        session.setIdleMinutes(0);

        session.setActiveSeconds(0);
        session.setIdleSeconds(0);

        session.setProductivity(0.0);

        session.setLogoutTime(null);

        session =
                sessionRepo.save(session);

        /*
         * Mark attendance PRESENT after the
         * session has successfully been saved.
         */
        attendanceService.markPresent(userId);

        /*
         * Start activity tracking AFTER the
         * session has successfully been saved.
         */
        trackerService.startTracking(userId);

        /*
         * Start screenshot capture AFTER the
         * session has successfully been saved.
         */
        screenshotCaptureService.startCapture(userId);

        return session;
    }


    // ==========================================
    // STOP WORK
    // ==========================================

    @Transactional
    public Session stopWork(Integer userId) {

        LocalDateTime now = LocalDateTime.now();

        // ======================================
        // OFFICE HOURS
        // ======================================

        if (workScheduleService.isOfficeHours(now)) {

            throw new IllegalStateException(
                    "Work cannot be stopped during office hours."
            );
        }


        // ======================================
        // FIND TODAY'S OPEN SESSION
        // ======================================

        Session session =
                getCurrentSession(userId);

        if (session == null) {

            return null;
        }


        // ======================================
        // STOP SCREENSHOT CAPTURE FIRST
        // ======================================

        /*
         * Cancel the scheduled screenshot task.
         *
         * No more screenshots will be captured
         * after the employee stops work.
         */
        screenshotCaptureService.stopCapture(userId);

        /*
         * Stop activity tracking.
         */
        trackerService.stopTracking(userId);

        /*
         * Stop screen recording.
         */
        screenRecordingService.stopRecording(userId);


        // ======================================
        // CLOSE SESSION
        // ======================================

        session.setLogoutTime(now);

        session =
                sessionRepo.save(session);


        // ======================================
        // CLOSE ATTENDANCE
        // ======================================

        /*
         * Record the actual Stop Work time
         * in today's attendance record.
         */
        attendanceService.markEndTime(userId);

        return session;
    }


    // ==========================================
    // GET TODAY'S AUTOMATIC OFFICE SESSION
    // ==========================================

    private Session getTodayOfficeSession(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(today);

        List<Session> officeSessions =
                sessionRepo
                        .findByUserIdAndOfficeStartAndLogoutTimeIsNull(
                                userId,
                                officeStart
                        );

        if (officeSessions.isEmpty()) {

            return null;
        }

        return officeSessions.stream()
                .max((s1, s2) ->
                        s1.getLoginTime()
                                .compareTo(s2.getLoginTime()))
                .orElse(null);
    }


    // ==========================================
    // CHECK WHETHER EMPLOYEE CLICKED
    // START WORK TODAY
    // ==========================================

    public boolean hasEmployeeStartedWork(Integer userId) {

        Session officeSession =
                getTodayOfficeSession(userId);

        return officeSession != null
                && officeSession.getEmployeeStartedAt() != null;
    }


    // ==========================================
    // AUTOMATIC OFFICE SESSION
    // ==========================================

    public Session startOfficeWork(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(today);

        LocalDateTime officeEnd =
                workScheduleService.getOfficeEnd(today);


        // ======================================
        // OUTSIDE OFFICE HOURS
        // ======================================

        if (!workScheduleService.isOfficeHours()) {

            return null;
        }


        // ======================================
        // CLOSE PRE-10 AM MANUAL SESSION
        // ======================================

        transitionToOfficeHours(userId);


        // ======================================
        // CHECK EXISTING OFFICE SESSION
        // ======================================

        List<Session> existingSessions =
                sessionRepo
                        .findByUserIdAndOfficeStartAndLogoutTimeIsNull(
                                userId,
                                officeStart
                        );

        if (!existingSessions.isEmpty()) {

            /*
             * The office session already exists.
             *
             * IMPORTANT:
             * Do NOT change employeeStartedAt here.
             *
             * The scheduler must NOT count as the
             * employee clicking Start Work.
             */
            return existingSessions.stream()
                    .max((s1, s2) ->
                            s1.getLoginTime()
                                    .compareTo(s2.getLoginTime()))
                    .orElse(null);
        }


        // ======================================
        // CREATE AUTOMATIC OFFICE SESSION
        // ======================================

        Session session = new Session();

        session.setUserId(userId);

        /*
         * Official session starts exactly at 10 AM.
         */
        session.setLoginTime(officeStart);

        session.setOfficeStart(officeStart);

        session.setOfficeEnd(officeEnd);

        /*
         * Official work period starts at 10 AM.
         */
        session.setWorkStartedAt(officeStart);

        /*
         * Employee has NOT clicked Start Work yet.
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

        return sessionRepo.save(session);
    }


    // ==========================================
    // AUTOMATIC STOP AT 7 PM
    // ==========================================

    public void stopOfficeWork() {

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(today);

        LocalDateTime officeEnd =
                workScheduleService.getOfficeEnd(today);

        List<Session> openOfficeSessions =
                sessionRepo.findByOfficeStartAndOfficeEndAndLogoutTimeIsNull(
                        officeStart,
                        officeEnd
                );

        for (Session session : openOfficeSessions) {

            Integer userId = session.getUserId();

            // Stop screenshot capture
            screenshotCaptureService.stopCapture(userId);

            // Stop activity tracking
            trackerService.stopTracking(userId);

            // Stop screen recording
            screenRecordingService.stopRecording(userId);

            // Close office session
            session.setLogoutTime(officeEnd);

            sessionRepo.save(session);

            /*
             * If the employee actually clicked Start Work,
             * close today's attendance at the official office
             * end time.
             *
             * If the employee never clicked Start Work,
             * there should be no PRESENT attendance record.
             */
            if (session.getEmployeeStartedAt() != null) {

                attendanceService.markEndTimeAt(
                        userId,
                        officeEnd.toLocalTime()
                );
            }
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

        /*
         * If any session is currently open,
         * employee has not completely stopped working.
         */
        if (getCurrentSession(userId) != null) {

            return null;
        }


        /*
         * Once all sessions are closed,
         * return the latest logout time.
         */
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

    public void transitionToOfficeHours(Integer userId) {

        LocalDate today = LocalDate.now();

        LocalDateTime officeStart =
                workScheduleService.getOfficeStart(today);

        LocalDateTime officeEnd =
                workScheduleService.getOfficeEnd(today);


        // ======================================
        // FIND OPEN SESSIONS BEFORE 10 AM
        // ======================================

        List<Session> openSessions =
                sessionRepo
                        .findByUserIdAndLoginTimeBetweenAndLogoutTimeIsNull(
                                userId,
                                today.atStartOfDay(),
                                officeStart.plusSeconds(1)
                        );


        // ======================================
        // CLOSE MANUAL SESSIONS
        // ======================================

        for (Session session : openSessions) {

            boolean isOfficeSession =
                    officeStart.equals(session.getOfficeStart())
                            &&
                            officeEnd.equals(session.getOfficeEnd());


            if (!isOfficeSession
                    && !session.getLoginTime().isAfter(officeStart)) {

                /*
                 * If a pre-10 AM manual session had
                 * screenshot capture running, stop it
                 * before closing the session.
                 */
                screenshotCaptureService.stopCapture(
                        session.getUserId()
                );

                /*
                 * Also stop activity tracking for
                 * the pre-10 AM manual session.
                 */
                trackerService.stopTracking(
                        session.getUserId()
                );

                session.setLogoutTime(officeStart);

                screenRecordingService.stopRecording(
                        session.getUserId()
                );

                sessionRepo.save(session);
            }
        }
    }


    // ==========================================
    // OVERTIME CONSTANT
    // ==========================================

    private static final int REGULAR_WORK_MINUTES = 8 * 60;


    // ==========================================
    // TODAY OVERTIME
    // ==========================================

    public int getTodayOvertimeMinutes(Integer userId) {

        int activeMinutes =
                getTodayActiveMinutes(userId);

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

        LocalDateTime start =
                startOfWeek.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

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

        LocalDate firstDay =
                today.withDayOfMonth(1);

        LocalDateTime start =
                firstDay.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

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

        /*
         * Group active minutes by date.
         *
         * Each working day gets its own
         * 8-hour regular-work allowance.
         */

        java.util.Map<LocalDate, Integer> dailyActive =
                new java.util.HashMap<>();

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

        LocalDate startDate =
                today.withDayOfMonth(1);

        LocalDateTime start =
                startDate.atStartOfDay();

        LocalDateTime end =
                today.plusDays(1).atStartOfDay();

        List<Session> sessions =
                sessionRepo.findByUserIdAndLoginTimeBetween(
                        userId,
                        start,
                        end
                );

        java.util.Map<LocalDate, Integer> dailyActive =
                new java.util.HashMap<>();

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

        List<OvertimeRecordDTO> records =
                new ArrayList<>();

        for (java.util.Map.Entry<LocalDate, Integer> entry
                : dailyActive.entrySet()) {

            int activeMinutes =
                    entry.getValue();

            int overtimeMinutes =
                    Math.max(
                            activeMinutes - REGULAR_WORK_MINUTES,
                            0
                    );

            // Only show days where actual overtime exists
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
}

