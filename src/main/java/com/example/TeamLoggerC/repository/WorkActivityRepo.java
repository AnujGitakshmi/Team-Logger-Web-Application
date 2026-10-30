package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.WorkActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WorkActivityRepo
        extends JpaRepository<WorkActivity, Integer> {

    // =====================================================
    // FIND WORK ACTIVITY FOR A SESSION
    // =====================================================

    List<WorkActivity> findBySessionId(
            Integer sessionId
    );


    // =====================================================
    // FIND WORK ACTIVITY WITHIN A TIME RANGE
    // =====================================================

    List<WorkActivity> findBySessionIdAndStartTimeBetween(
            Integer sessionId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND SPECIFIC ACTIVITY TYPE
    // =====================================================

    List<WorkActivity> findBySessionIdAndActivityType(
            Integer sessionId,
            String activityType
    );


    // =====================================================
    // FIND LATEST WORK ACTIVITY
    // =====================================================

    WorkActivity findFirstBySessionIdOrderByStartTimeDesc(
            Integer sessionId
    );
}