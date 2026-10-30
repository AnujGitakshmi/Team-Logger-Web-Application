package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.ApplicationActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ApplicationActivityRepo
        extends JpaRepository<ApplicationActivity, Integer> {

    // =====================================================
    // FIND APPLICATION ACTIVITY FOR A SESSION
    // =====================================================

    List<ApplicationActivity> findBySessionId(
            Integer sessionId
    );


    // =====================================================
    // FIND ACTIVITY WITHIN A TIME RANGE
    // =====================================================

    List<ApplicationActivity> findBySessionIdAndStartTimeBetween(
            Integer sessionId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND LATEST APPLICATION ACTIVITY
    // =====================================================

    ApplicationActivity findFirstBySessionIdOrderByStartTimeDesc(
            Integer sessionId
    );
}