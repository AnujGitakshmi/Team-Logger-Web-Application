package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Screenshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ScreenshotRepo
        extends JpaRepository<Screenshot, Integer> {

    List<Screenshot> findByUserIdOrderByCapturedAtDesc(
            Integer userId
    );

    List<Screenshot> findByUserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Screenshot> findBySessionIdOrderByCapturedAtDesc(
            Integer sessionId
    );

    Screenshot findFirstByUserIdOrderByCapturedAtDesc(
            Integer userId
    );


    // =========================================================
    // RETENTION
    // =========================================================

    /**
     * Finds screenshots of a particular storage type
     * that are older than the specified date and
     * have not already been archived.
     *
     * Example:
     * LOCAL + older than 7 days
     * DATABASE + older than 12 months
     */
    List<Screenshot> findByStorageTypeAndCapturedAtBeforeAndArchivedFalse(
            String storageType,
            LocalDateTime capturedAt
    );


    /**
     * Existing generic retention query.
     */
    List<Screenshot> findByStorageTypeAndCapturedAtBefore(
            String storageType,
            LocalDateTime capturedAt
    );


    /**
     * Finds screenshots whose expiration date has passed.
     */
    List<Screenshot> findByExpiresAtBefore(
            LocalDateTime expiresAt
    );


    long countByFilePathAndStorageType(
            String filePath,
            String storageType
    );

}