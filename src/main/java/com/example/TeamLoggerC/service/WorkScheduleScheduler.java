
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
    // AUTOMATIC WORK STOP
    //
    // Runs every minute.
    //
    // After office hours:
    // Close today's office sessions at the
    // configured office end time.
    // ==========================================

    @Scheduled(fixedRate = 60000)
    public void stopOfficeWorkAutomatically() {

        if (sessionService.isOfficeHours()) {
            return;
        }

        sessionService.stopOfficeWork();
    }
}
