package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Activity;
import com.example.TeamLoggerC.tracker.ActivityState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityRepo extends JpaRepository<Activity, Long> {

    List<Activity> findByUserId(Integer userId);

    List<Activity> findByUserIdAndActivityState(
            Integer userId,
            ActivityState activityState
    );

    List<Activity> findByUserIdAndStartTimeBetween(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Query("""
    SELECT a
    FROM Activity a
    WHERE a.userId = :userId
      AND a.startTime < :end
      AND (a.endTime IS NULL OR a.endTime > :start)
    ORDER BY a.startTime ASC
    """)
    List<Activity> findActivitiesOverlappingPeriod(
            @Param("userId") Integer userId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}