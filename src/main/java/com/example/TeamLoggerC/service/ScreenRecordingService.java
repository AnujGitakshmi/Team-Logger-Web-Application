package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.RecordingSession;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.RecordingSessionRepo;
import com.example.TeamLoggerC.repository.SessionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScreenRecordingService {

    private static final String STATUS_RECORDING = "RECORDING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";

    private final SessionRepo sessionRepo;
    private final RecordingSessionRepo recordingSessionRepo;
    private final ScreenRecordingStorageService storageService;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ScreenRecordingService(
            SessionRepo sessionRepo,
            RecordingSessionRepo recordingSessionRepo,
            ScreenRecordingStorageService storageService) {

        this.sessionRepo = sessionRepo;
        this.recordingSessionRepo = recordingSessionRepo;
        this.storageService = storageService;
    }


    // =========================================================
    // START RECORDING
    // =========================================================

    @Transactional
    public synchronized String startRecording(Integer userId) {

        if (userId == null) {
            return "FAILED: Employee ID is required.";
        }


        // -----------------------------------------------------
        // Get current employee work session
        // -----------------------------------------------------

        Session session = getCurrentSession(userId);

        if (session == null) {

            return "FAILED: Employee does not have an active session.";
        }


        // -----------------------------------------------------
        // Recording is allowed only after Start Work
        // -----------------------------------------------------

        if (session.getEmployeeStartedAt() == null) {
            return "FAILED: Employee has not clicked Start Work.";
        }

        if (session.isWorkPaused()) {
            return "FAILED: Employee work session is paused.";
        }


        Integer sessionId = session.getId();


        // -----------------------------------------------------
        // Check physical recording first
        // -----------------------------------------------------

        if (storageService.isRecording(userId)) {

            return "ALREADY_RECORDING: Screen recording is already running.";
        }


        // -----------------------------------------------------
        // Check database for an existing RECORDING row
        //
        // This protects us from creating multiple database
        // recording rows for the same work session.
        // -----------------------------------------------------

        RecordingSession existingRecording =
                recordingSessionRepo
                        .findFirstBySessionIdAndStatusOrderByStartedAtDesc(
                                sessionId,
                                STATUS_RECORDING
                        )
                        .orElse(null);


        if (existingRecording != null) {

            /*
             * There is already a RECORDING database row.
             *
             * If the physical recorder is not running, the old
             * database row is stale. Mark it FAILED before
             * starting a new recording.
             */

            existingRecording.setStatus(STATUS_FAILED);

            if (existingRecording.getEndedAt() == null) {

                existingRecording.setEndedAt(
                        LocalDateTime.now()
                );
            }

            recordingSessionRepo.save(existingRecording);
        }


        // -----------------------------------------------------
        // Start physical screen recording
        // -----------------------------------------------------

        String filePath;

        try {

            filePath =
                    storageService.startRecording(
                            userId,
                            sessionId
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return "FAILED: Unable to start screen recording. "
                    + e.getMessage();
        }


        // -----------------------------------------------------
        // Create database record
        // -----------------------------------------------------

        try {

            RecordingSession recording =
                    new RecordingSession();

            recording.setUserId(userId);

            recording.setSessionId(sessionId);

            recording.setStartedAt(
                    LocalDateTime.now()
            );

            recording.setFilePath(filePath);

            Path path =
                    Paths.get(filePath);

            recording.setFileName(
                    path.getFileName().toString()
            );

            recording.setStatus(
                    STATUS_RECORDING
            );

            recording.setFileSize(0L);

            recording.setDurationSeconds(0L);

            recordingSessionRepo.save(recording);


            System.out.println(
                    "RecordingSession created."
                            + " ID: " + recording.getId()
                            + " | User: " + userId
                            + " | Session: " + sessionId
                            + " | File: " + filePath
            );


            return "SUCCESS: Recording started. File: "
                    + filePath;

        } catch (Exception e) {

            /*
             * Database creation failed after the physical
             * recorder started.
             *
             * Stop the physical recorder so we don't leave
             * an orphan MP4 running.
             */

            e.printStackTrace();

            try {
                storageService.stopRecording(userId);
            } catch (Exception stopException) {
                stopException.printStackTrace();
            }

            return "FAILED: Unable to create recording database record. "
                    + e.getMessage();
        }
    }


    // =========================================================
    // STOP RECORDING
    // =========================================================

    @Transactional
    public synchronized String stopRecording(Integer userId) {

        if (userId == null) {
            return "FAILED: Employee ID is required.";
        }


        // -----------------------------------------------------
        // Check physical recording
        // -----------------------------------------------------

        if (!storageService.isRecording(userId)) {

            /*
             * Physical recorder is already stopped.
             *
             * However, there may still be a stale RECORDING
             * database row. Try to finalize it below.
             */

            Session currentSession =
                    getCurrentSession(userId);

            if (currentSession != null) {

                RecordingSession staleRecording =
                        recordingSessionRepo
                                .findFirstBySessionIdAndStatusOrderByStartedAtDesc(
                                        currentSession.getId(),
                                        STATUS_RECORDING
                                )
                                .orElse(null);

                if (staleRecording != null) {

                    finalizeRecording(
                            staleRecording,
                            staleRecording.getFilePath()
                    );

                    return "SUCCESS: Recording database record finalized.";
                }
            }

            return "NOT_RECORDING: Employee is not currently being recorded.";
        }


        // -----------------------------------------------------
        // Find current work session
        // -----------------------------------------------------

        Session currentSession =
                getCurrentSession(userId);


        RecordingSession recording = null;


        if (currentSession != null) {

            recording =
                    recordingSessionRepo
                            .findFirstBySessionIdAndStatusOrderByStartedAtDesc(
                                    currentSession.getId(),
                                    STATUS_RECORDING
                            )
                            .orElse(null);
        }


        // -----------------------------------------------------
        // Stop physical recorder
        // -----------------------------------------------------

        String filePath;

        try {

            filePath =
                    storageService.stopRecording(userId);

        } catch (Exception e) {

            e.printStackTrace();

            return "FAILED: Unable to stop screen recording. "
                    + e.getMessage();
        }


        // -----------------------------------------------------
        // Update database
        // -----------------------------------------------------

        if (recording != null) {

            finalizeRecording(
                    recording,
                    filePath
            );

        } else {

            /*
             * Physical recording existed but the database row
             * could not be found.
             *
             * Do not create a fake completed record here.
             */

            System.err.println(
                    "WARNING: Physical recording stopped but "
                            + "no RECORDING database row was found."
                            + " User: " + userId
            );
        }


        return "SUCCESS: Recording stopped. File: "
                + filePath;
    }


    // =========================================================
    // FINALIZE RECORDING
    // =========================================================

    private void finalizeRecording(
            RecordingSession recording,
            String filePath) {

        LocalDateTime endedAt =
                LocalDateTime.now();


        // -----------------------------------------------------
        // End time
        // -----------------------------------------------------

        recording.setEndedAt(
                endedAt
        );


        // -----------------------------------------------------
        // Duration
        // -----------------------------------------------------

        if (recording.getStartedAt() != null) {

            long duration =
                    Duration.between(
                            recording.getStartedAt(),
                            endedAt
                    ).getSeconds();

            if (duration < 0) {
                duration = 0;
            }

            recording.setDurationSeconds(
                    duration
            );

        } else {

            recording.setDurationSeconds(0L);
        }


        // -----------------------------------------------------
        // File information
        // -----------------------------------------------------

        if (filePath != null
                && !filePath.isBlank()) {

            recording.setFilePath(
                    filePath
            );


            try {

                Path path =
                        Paths.get(filePath);


                recording.setFileName(
                        path.getFileName().toString()
                );


                if (Files.exists(path)) {

                    long fileSize =
                            Files.size(path);

                    recording.setFileSize(
                            fileSize
                    );

                } else {

                    recording.setFileSize(0L);

                    System.err.println(
                            "Recording file does not exist: "
                                    + filePath
                    );
                }

            } catch (Exception e) {

                recording.setFileSize(0L);

                System.err.println(
                        "Unable to determine recording file size: "
                                + filePath
                );

                e.printStackTrace();
            }
        }


        // -----------------------------------------------------
        // Mark completed
        // -----------------------------------------------------

        recording.setStatus(
                STATUS_COMPLETED
        );


        recordingSessionRepo.save(
                recording
        );


        System.out.println(
                "RecordingSession completed."
                        + " ID: " + recording.getId()
                        + " | User: " + recording.getUserId()
                        + " | Session: " + recording.getSessionId()
                        + " | Duration: "
                        + recording.getDurationSeconds()
                        + " sec"
                        + " | File size: "
                        + recording.getFileSize()
                        + " bytes"
        );
    }


    // =========================================================
    // CHECK RECORDING STATUS
    // =========================================================

    public boolean isRecording(Integer userId) {

        if (userId == null) {
            return false;
        }

        return storageService.isRecording(
                userId
        );
    }


    // =========================================================
    // GET RECORDING HISTORY
    // =========================================================

    public List<RecordingSession> getRecordings(
            Integer userId) {

        return recordingSessionRepo
                .findByUserIdOrderByStartedAtDesc(
                        userId
                );
    }


    // =========================================================
    // GET SINGLE RECORDING
    // =========================================================

    public RecordingSession getRecording(
            Integer recordingId) {

        if (recordingId == null) {
            return null;
        }

        return recordingSessionRepo
                .findById(recordingId)
                .orElse(null);
    }


    // =========================================================
    // GET CURRENT SESSION
    // =========================================================

    private Session getCurrentSession(Integer userId) {

        List<Session> openSessions =
                sessionRepo.findByUserIdAndLogoutTimeIsNull(userId);

        if (openSessions.isEmpty()) {
            return null;
        }

        return openSessions.stream()
                .max((s1, s2) ->
                        s1.getLoginTime().compareTo(s2.getLoginTime())
                )
                .orElse(null);
    }
}