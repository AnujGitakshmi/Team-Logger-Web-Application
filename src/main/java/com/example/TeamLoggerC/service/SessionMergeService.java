
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SessionMergeService {

    private final SessionRepo sessionRepo;
    private final EntityManager entityManager;
    private final WorkScheduleService workScheduleService;
    private final UserRepo userRepo;

    public SessionMergeService(
            SessionRepo sessionRepo,
            EntityManager entityManager,
            WorkScheduleService workScheduleService,
            UserRepo userRepo) {

        this.sessionRepo = sessionRepo;
        this.entityManager = entityManager;
        this.workScheduleService = workScheduleService;
        this.userRepo = userRepo;
    }

    // =========================================================
    // GET CURRENT SESSION AND MERGE DUPLICATES
    // =========================================================

    @Transactional
    public Session getOrMergeCurrentSession(Integer userId) {

        List<Session> openSessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        // No open session
        if (openSessions.isEmpty()) {
            return null;
        }

        // Only one open session
        if (openSessions.size() == 1) {

            Session session = openSessions.get(0);

            ensureOfficeTimes(session);

            return sessionRepo.save(session);
        }

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "OPEN SESSIONS FOUND: " + openSessions.size()
        );

        System.out.println(
                "USER ID: " + userId
        );

        System.out.println(
                "=========================================="
        );

        // =====================================================
        // GROUP OPEN SESSIONS BY LOGIN DATE
        // =====================================================

        Map<LocalDate, List<Session>> sessionsByDate =
                openSessions.stream()
                        .filter(session -> session.getLoginTime() != null)
                        .collect(Collectors.groupingBy(
                                session ->
                                        session.getLoginTime().toLocalDate()
                        ));

        Session currentSession = null;

        // =====================================================
        // PROCESS EACH DATE SEPARATELY
        // =====================================================

        for (Map.Entry<LocalDate, List<Session>> entry
                : sessionsByDate.entrySet()) {

            LocalDate date = entry.getKey();

            List<Session> sessionsForDate =
                    new ArrayList<>(entry.getValue());

            System.out.println(
                    "Date: " + date
                            + " | Open sessions: "
                            + sessionsForDate.size()
            );

            // -------------------------------------------------
            // Only one session on this date
            // -------------------------------------------------

            if (sessionsForDate.size() == 1) {

                Session session = sessionsForDate.get(0);

                ensureOfficeTimes(session);

                sessionRepo.save(session);

                currentSession = getLatestSession(
                        currentSession,
                        session
                );

                continue;
            }

            // =================================================
            // DUPLICATE SESSIONS ON SAME DATE
            // =================================================

            sessionsForDate.sort(
                    Comparator.comparing(Session::getLoginTime)
            );

            // Keep the oldest session as the main session
            Session mainSession = sessionsForDate.get(0);

            ensureOfficeTimes(mainSession);

            // -------------------------------------------------
            // Merge duplicate sessions into the main session
            // -------------------------------------------------

            for (int i = 1; i < sessionsForDate.size(); i++) {

                Session duplicate = sessionsForDate.get(i);

                System.out.println(
                        "Merging session "
                                + duplicate.getId()
                                + " into session "
                                + mainSession.getId()
                );

                // Move tracking records
                mergeTrackingData(
                        duplicate.getId(),
                        mainSession.getId()
                );

                // Merge active and idle seconds
                mergeSessionTime(
                        mainSession,
                        duplicate
                );

                // Merge work start timestamps
                mergeWorkStartedAt(
                        mainSession,
                        duplicate
                );

                // Merge employee Start Work timestamp
                mergeEmployeeStartedAt(
                        mainSession,
                        duplicate
                );

                // Delete duplicate only after moving references
                sessionRepo.delete(duplicate);
            }

            // -------------------------------------------------
            // Recalculate active and idle minutes
            // -------------------------------------------------

            mainSession.setActiveMinutes(
                    safe(mainSession.getActiveSeconds()) / 60
            );

            mainSession.setIdleMinutes(
                    safe(mainSession.getIdleSeconds()) / 60
            );

            sessionRepo.save(mainSession);

            sessionRepo.flush();

            System.out.println(
                    "Merged sessions for date: " + date
            );

            currentSession = getLatestSession(
                    currentSession,
                    mainSession
            );
        }

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "SESSION CHECK COMPLETED"
        );

        System.out.println(
                "=========================================="
        );

        return currentSession;
    }

    // =========================================================
    // GET LATEST OPEN SESSION
    // =========================================================

    private Session getLatestSession(
            Session currentSession,
            Session candidate) {

        if (candidate == null) {
            return currentSession;
        }

        if (currentSession == null) {
            return candidate;
        }

        LocalDateTime currentLoginTime =
                currentSession.getLoginTime();

        LocalDateTime candidateLoginTime =
                candidate.getLoginTime();

        if (currentLoginTime == null) {
            return candidate;
        }

        if (candidateLoginTime != null
                && candidateLoginTime.isAfter(currentLoginTime)) {

            return candidate;
        }

        return currentSession;
    }

    // =========================================================
    // ENSURE OFFICE START / END
    // =========================================================

    private void ensureOfficeTimes(Session session) {

        if (session == null || session.getLoginTime() == null) {
            return;
        }

        /*
         * Manual sessions intentionally have null officeStart
         * and officeEnd.
         *
         * Do not assign office timings to manually started
         * sessions.
         */
        if (session.getEmployeeStartedAt() != null) {
            return;
        }

        User employee = userRepo.findById(session.getUserId())
                .orElse(null);

        if (employee == null || employee.getAdminId() == null) {
            return;
        }

        Integer adminId = employee.getAdminId();

        LocalDate date =
                session.getLoginTime().toLocalDate();

        LocalDateTime expectedOfficeStart =
                workScheduleService.getOfficeStart(adminId, date);

        LocalDateTime expectedOfficeEnd =
                workScheduleService.getOfficeEnd(adminId, date);

        /*
         * Only identify this as a legacy automatic office
         * session if loginTime matches the admin's office start.
         */
        if (!session.getLoginTime().equals(expectedOfficeStart)) {
            return;
        }

        if (session.getOfficeStart() == null) {
            session.setOfficeStart(expectedOfficeStart);
        }

        if (session.getOfficeEnd() == null) {
            session.setOfficeEnd(expectedOfficeEnd);
        }
    }

    // =========================================================
    // MOVE ALL TRACKING DATA
    // =========================================================

    private void mergeTrackingData(
            Integer duplicateSessionId,
            Integer mainSessionId) {

        // 1. Application activity
        updateSessionId(
                "application_activity",
                duplicateSessionId,
                mainSessionId
        );

        // 2. Employee daily work
        updateSessionId(
                "employee_daily_work",
                duplicateSessionId,
                mainSessionId
        );

        // 3. Employee screenshots
        updateSessionId(
                "employee_screenshots",
                duplicateSessionId,
                mainSessionId
        );

        // 4. Overtime activity
        updateSessionId(
                "overtime_activity",
                duplicateSessionId,
                mainSessionId
        );

        // 5. Work activity
        updateSessionId(
                "work_activity",
                duplicateSessionId,
                mainSessionId
        );
    }

    // =========================================================
    // UPDATE CHILD TABLE SESSION ID
    // =========================================================

    private void updateSessionId(
            String tableName,
            Integer oldSessionId,
            Integer newSessionId) {

        String sql =
                "UPDATE " + tableName
                        + " SET session_id = :newId"
                        + " WHERE session_id = :oldId";

        Query query =
                entityManager.createNativeQuery(sql);

        query.setParameter("newId", newSessionId);
        query.setParameter("oldId", oldSessionId);

        int updatedRows =
                query.executeUpdate();

        if (updatedRows > 0) {

            System.out.println(
                    tableName
                            + " -> moved "
                            + updatedRows
                            + " record(s)"
            );
        }
    }

    // =========================================================
    // MERGE ACTIVE / IDLE SECONDS
    // =========================================================

    private void mergeSessionTime(
            Session mainSession,
            Session duplicate) {

        int mainActive =
                safe(mainSession.getActiveSeconds());

        int duplicateActive =
                safe(duplicate.getActiveSeconds());

        int mainIdle =
                safe(mainSession.getIdleSeconds());

        int duplicateIdle =
                safe(duplicate.getIdleSeconds());

        mainSession.setActiveSeconds(
                mainActive + duplicateActive
        );

        mainSession.setIdleSeconds(
                mainIdle + duplicateIdle
        );
    }

    // =========================================================
    // MERGE WORK START TIME
    // =========================================================

    private void mergeWorkStartedAt(
            Session mainSession,
            Session duplicate) {

        LocalDateTime mainStart =
                mainSession.getWorkStartedAt();

        LocalDateTime duplicateStart =
                duplicate.getWorkStartedAt();

        if (mainStart == null) {

            mainSession.setWorkStartedAt(
                    duplicateStart
            );

            return;
        }

        if (duplicateStart != null
                && duplicateStart.isBefore(mainStart)) {

            mainSession.setWorkStartedAt(
                    duplicateStart
            );
        }
    }

    // =========================================================
    // MERGE EMPLOYEE STARTED AT
    // =========================================================

    private void mergeEmployeeStartedAt(
            Session mainSession,
            Session duplicate) {

        LocalDateTime mainStart =
                mainSession.getEmployeeStartedAt();

        LocalDateTime duplicateStart =
                duplicate.getEmployeeStartedAt();

        /*
         * Preserve the earliest actual employee Start Work
         * click. Null means the employee had not started work.
         */

        if (mainStart == null) {

            mainSession.setEmployeeStartedAt(
                    duplicateStart
            );

            return;
        }

        if (duplicateStart != null
                && duplicateStart.isBefore(mainStart)) {

            mainSession.setEmployeeStartedAt(
                    duplicateStart
            );
        }
    }

    // =========================================================
    // NULL-SAFE INTEGER
    // =========================================================

    private int safe(Integer value) {

        return value == null ? 0 : value;
    }
}
