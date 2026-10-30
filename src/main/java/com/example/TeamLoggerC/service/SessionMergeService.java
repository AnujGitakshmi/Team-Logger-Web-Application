package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.SessionRepo;
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

    public SessionMergeService(
            SessionRepo sessionRepo,
            EntityManager entityManager,
            WorkScheduleService workScheduleService) {

        this.sessionRepo = sessionRepo;
        this.entityManager = entityManager;
        this.workScheduleService = workScheduleService;
    }

    // =========================================================
    // GET CURRENT SESSION
    // =========================================================

    @Transactional
    public Session getOrMergeCurrentSession(Integer userId) {

        List<Session> openSessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        // -----------------------------------------------------
        // No open session
        // -----------------------------------------------------

        if (openSessions.isEmpty()) {
            return null;
        }

        // -----------------------------------------------------
        // Only one open session
        // -----------------------------------------------------

        if (openSessions.size() == 1) {

            Session session = openSessions.get(0);

            ensureOfficeTimes(session);

            sessionRepo.save(session);

            return session;
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
        // GROUP OPEN SESSIONS BY DATE
        // =====================================================

        Map<LocalDate, List<Session>> sessionsByDate =
                openSessions.stream()
                        .collect(Collectors.groupingBy(
                                session ->
                                        session.getLoginTime()
                                                .toLocalDate()
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

                // If this is today's session, remember it
                if (date.equals(LocalDate.now())) {
                    currentSession = session;
                }

                continue;
            }

            // =================================================
            // DUPLICATE SESSIONS ON SAME DATE
            // =================================================

            sessionsForDate.sort(
                    Comparator.comparing(
                            Session::getLoginTime
                    )
            );

            Session mainSession =
                    sessionsForDate.get(0);

            ensureOfficeTimes(mainSession);

            // -------------------------------------------------
            // Merge duplicates into oldest session
            // -------------------------------------------------

            for (int i = 1; i < sessionsForDate.size(); i++) {

                Session duplicate =
                        sessionsForDate.get(i);

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

                // Merge time/activity information
                mergeSessionTime(
                        mainSession,
                        duplicate
                );

                // Merge work start time
                mergeWorkStartedAt(
                        mainSession,
                        duplicate
                );

                // ------------------------------------------------
                // Delete duplicate session AFTER references moved
                // ------------------------------------------------

                sessionRepo.delete(duplicate);
            }

            // -------------------------------------------------
            // Recalculate minutes
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

            // Today's merged session becomes current
            if (date.equals(LocalDate.now())) {
                currentSession = mainSession;
            }
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
    // ENSURE OFFICE START / END
    // =========================================================

    private void ensureOfficeTimes(Session session) {

        if (session.getLoginTime() == null) {
            return;
        }

        LocalDate date =
                session.getLoginTime().toLocalDate();

        if (session.getOfficeStart() == null) {

            session.setOfficeStart(
                    workScheduleService.getOfficeStart(date)
            );
        }

        if (session.getOfficeEnd() == null) {

            session.setOfficeEnd(
                    workScheduleService.getOfficeEnd(date)
            );
        }
    }

    // =========================================================
    // MOVE ALL TRACKING DATA
    // =========================================================

    private void mergeTrackingData(
            Integer duplicateSessionId,
            Integer mainSessionId) {

        // -----------------------------------------------
        // 1. APPLICATION ACTIVITY
        // -----------------------------------------------

        updateSessionId(
                "application_activity",
                duplicateSessionId,
                mainSessionId
        );

        // -----------------------------------------------
        // 2. EMPLOYEE DAILY WORK
        // -----------------------------------------------

        updateSessionId(
                "employee_daily_work",
                duplicateSessionId,
                mainSessionId
        );

        // -----------------------------------------------
        // 3. EMPLOYEE SCREENSHOTS
        // -----------------------------------------------

        updateSessionId(
                "employee_screenshots",
                duplicateSessionId,
                mainSessionId
        );

        // -----------------------------------------------
        // 4. OVERTIME ACTIVITY
        // -----------------------------------------------

        updateSessionId(
                "overtime_activity",
                duplicateSessionId,
                mainSessionId
        );

        // -----------------------------------------------
        // 5. WORK ACTIVITY
        // -----------------------------------------------

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
    // NULL-SAFE INTEGER
    // =========================================================

    private int safe(Integer value) {

        return value == null ? 0 : value;
    }
}