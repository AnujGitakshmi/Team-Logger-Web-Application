package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Activity;
import com.example.TeamLoggerC.tracker.ActivityState;
import org.springframework.data.jpa.repository.JpaRepository;

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
}