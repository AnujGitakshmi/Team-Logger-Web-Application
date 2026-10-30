package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.RecordingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecordingSessionRepo
        extends JpaRepository<RecordingSession, Integer> {

    // ---------------------------------------------------------
    // Employee recording history
    // ---------------------------------------------------------

    List<RecordingSession> findByUserIdOrderByStartedAtDesc(
            Integer userId
    );


    // ---------------------------------------------------------
    // Recordings belonging to one work session
    // ---------------------------------------------------------

    List<RecordingSession> findBySessionIdOrderByStartedAtAsc(
            Integer sessionId
    );


    // ---------------------------------------------------------
    // Find the latest recording with a particular status
    //
    // IMPORTANT:
    // We deliberately use "findFirst" because old database
    // records may contain duplicates.
    // ---------------------------------------------------------

    Optional<RecordingSession>
    findFirstBySessionIdAndStatusOrderByStartedAtDesc(
            Integer sessionId,
            String status
    );


    // ---------------------------------------------------------
    // Find latest recording for employee/session
    // ---------------------------------------------------------

    Optional<RecordingSession>
    findFirstByUserIdAndSessionIdOrderByStartedAtDesc(
            Integer userId,
            Integer sessionId
    );
}