
package com.example.TeamLoggerC.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkScheduleScheduler {

    private final SessionService sessionService;

    public WorkScheduleScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    // ==========================================
    // AUTOMATIC SESSION MANAGEMENT
    //
    // Runs every minute.
    //
    // 1. Closes previous-day sessions at midnight.
    // 2. Closes today's office sessions at their
    //    configured office end time.
    // ==========================================

    @Scheduled(fixedRate = 60000)
    public void stopOfficeWorkAutomatically() {

        // First: permanently close sessions from previous dates.
        sessionService.closeSessionsAtMidnight();

        // Second: close sessions whose office hours have ended.
        sessionService.stopOfficeWork();
    }
}
