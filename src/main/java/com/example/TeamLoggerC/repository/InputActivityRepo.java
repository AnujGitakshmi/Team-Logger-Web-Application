package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.InputActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface InputActivityRepo
        extends JpaRepository<InputActivity, Integer> {

    // =====================================================
    // FIND INPUT ACTIVITY FOR A SESSION
    // =====================================================

    List<InputActivity> findBySessionId(
            Integer sessionId
    );


    // =====================================================
    // FIND INPUT ACTIVITY WITHIN A TIME RANGE
    // =====================================================

    List<InputActivity> findBySessionIdAndEventTimeBetween(
            Integer sessionId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND SPECIFIC INPUT TYPE
    // =====================================================

    List<InputActivity> findBySessionIdAndActivityType(
            Integer sessionId,
            String activityType
    );


    // =====================================================
    // FIND LATEST INPUT ACTIVITY
    // =====================================================

    InputActivity findFirstBySessionIdOrderByEventTimeDesc(
            Integer sessionId
    );


    // =====================================================
    // COUNT INPUT EVENTS IN A TIME RANGE
    // =====================================================

    long countBySessionIdAndActivityTypeAndEventTimeBetween(
            Integer sessionId,
            String activityType,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // COUNT KEYBOARD EVENTS
    // =====================================================

    default long countKeyboardEvents(
            Integer sessionId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return countBySessionIdAndActivityTypeAndEventTimeBetween(
                sessionId,
                "KEYBOARD",
                start,
                end
        );
    }


    // =====================================================
    // COUNT MOUSE CLICKS
    // =====================================================

    default long countMouseClicks(
            Integer sessionId,
            LocalDateTime start,
            LocalDateTime end
    ) {
        return countBySessionIdAndActivityTypeAndEventTimeBetween(
                sessionId,
                "MOUSE_CLICK",
                start,
                end
        );
    }
}

