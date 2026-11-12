
package com.example.TeamLoggerC.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.FFmpegLogCallback;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class ScreenRecordingStorageService {

    // =========================================================
    // RECORDING DIRECTORY
    // =========================================================

    private final String recordingDirectory;


    // =========================================================
    // RECORDING SETTINGS
    // =========================================================

    private static final int DEFAULT_FPS = 5;


    // =========================================================
    // FILE NAME FORMAT
    // =========================================================

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");


    // =========================================================
    // ACTIVE RECORDINGS
    //
    // userId -> recording worker
    // =========================================================

    private final Map<Integer, RecordingWorker> activeRecordings =
            new ConcurrentHashMap<>();


    // =========================================================
    // PER-EMPLOYEE LOCKS
    // =========================================================

    private final Map<Integer, Object> recordingLocks =
            new ConcurrentHashMap<>();


    private Object getRecordingLock(Integer userId) {

        return recordingLocks.computeIfAbsent(
                userId,
                id -> new Object()
        );
    }


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public ScreenRecordingStorageService(
            @Value("${teamlogger.recording.directory:./recordings}")
            String recordingDirectory) {

        this.recordingDirectory = recordingDirectory;
    }


    // =========================================================
    // START RECORDING
    // =========================================================

    public String startRecording(
            Integer userId,
            Integer sessionId) {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID cannot be null."
            );
        }

        if (sessionId == null) {
            throw new IllegalArgumentException(
                    "Session ID cannot be null."
            );
        }

        synchronized (getRecordingLock(userId)) {

            FFmpegLogCallback.set();

            if (GraphicsEnvironment.isHeadless()) {
                throw new IllegalStateException(
                        "Screen recording cannot start because "
                                + "the application is running in headless mode."
                );
            }

            // -------------------------------------------------
            // Check existing worker
            // -------------------------------------------------

            RecordingWorker existing =
                    activeRecordings.get(userId);

            if (existing != null) {

                if (existing.isRunning()) {

                    System.out.println(
                            "Screen recording is already running."
                                    + " User: " + userId
                    );

                    return existing.getFilePath();
                }

                // Wait for complete cleanup of previous worker.
                existing.waitForTermination();

                activeRecordings.remove(
                        userId,
                        existing
                );
            }


            // -------------------------------------------------
            // Create employee/session directory
            // -------------------------------------------------

            Path sessionDirectory =
                    Paths.get(
                            recordingDirectory,
                            "employee_" + userId,
                            "session_" + sessionId
                    );

            try {

                Files.createDirectories(
                        sessionDirectory
                );

            } catch (IOException e) {

                throw new RuntimeException(
                        "Unable to create recording directory: "
                                + sessionDirectory,
                        e
                );
            }


            // -------------------------------------------------
            // Create unique filename
            // -------------------------------------------------

            String timestamp =
                    LocalDateTime.now()
                            .format(FILE_DATE_FORMAT);

            String uniqueId =
                    UUID.randomUUID()
                            .toString()
                            .replace("-", "")
                            .substring(0, 12);

            String fileName =
                    "recording_"
                            + timestamp
                            + "_"
                            + uniqueId
                            + ".mp4";

            Path filePath =
                    sessionDirectory.resolve(fileName);


            // -------------------------------------------------
            // Create worker
            // -------------------------------------------------

            RecordingWorker worker =
                    new RecordingWorker(
                            userId,
                            sessionId,
                            filePath
                    );


            // Register worker before starting its thread.
            activeRecordings.put(
                    userId,
                    worker
            );


            // -------------------------------------------------
            // Start worker
            // -------------------------------------------------

            try {

                worker.start();

            } catch (RuntimeException e) {

                worker.stop();

                activeRecordings.remove(
                        userId,
                        worker
                );

                throw e;
            }


            System.out.println(
                    "Screen recording started."
                            + " User: " + userId
                            + " | Session: " + sessionId
                            + " | File: "
                            + filePath.toAbsolutePath()
            );

            return filePath
                    .toAbsolutePath()
                    .toString();
        }
    }


    // =========================================================
    // STOP RECORDING
    // =========================================================

    public String stopRecording(Integer userId) {

        if (userId == null) {
            return null;
        }

        synchronized (getRecordingLock(userId)) {

            RecordingWorker worker =
                    activeRecordings.get(userId);

            if (worker == null) {

                System.out.println(
                        "No active screen recording found."
                                + " User: " + userId
                );

                return null;
            }


            /*
             * Wait until the worker has completely terminated
             * and FFmpeg cleanup has finished.
             */

            worker.stop();


            // Remove only after complete termination.
            activeRecordings.remove(
                    userId,
                    worker
            );


            System.out.println(
                    "Screen recording stopped."
                            + " User: " + userId
                            + " | File: "
                            + worker.getFilePath()
            );

            return worker.getFilePath();
        }
    }


    // =========================================================
    // CHECK RECORDING STATUS
    // =========================================================

    public boolean isRecording(Integer userId) {

        if (userId == null) {
            return false;
        }

        RecordingWorker worker =
                activeRecordings.get(userId);

        return worker != null
                && worker.isRunning();
    }


    // =========================================================
    // GET ACTIVE RECORDING FILE
    // =========================================================

    public String getActiveRecordingPath(
            Integer userId) {

        if (userId == null) {
            return null;
        }

        RecordingWorker worker =
                activeRecordings.get(userId);

        if (worker == null) {
            return null;
        }

        return worker.getFilePath();
    }


    // =========================================================
    // STOP ALL RECORDINGS
    // =========================================================

    public void stopAllRecordings() {

        for (Integer userId : activeRecordings.keySet()) {

            stopRecording(userId);
        }

        System.out.println(
                "All screen recordings stopped."
        );
    }


    // =========================================================
    // DELETE RECORDING FILE
    // =========================================================

    public void deleteRecordingFile(
            String filePath) {

        if (filePath == null
                || filePath.isBlank()) {

            return;
        }

        try {

            Files.deleteIfExists(
                    Paths.get(filePath)
            );

            System.out.println(
                    "Recording file deleted: "
                            + filePath
            );

        } catch (IOException e) {

            System.err.println(
                    "Unable to delete recording file: "
                            + filePath
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // GET FILE SIZE
    // =========================================================

    public long getFileSize(
            String filePath) {

        if (filePath == null
                || filePath.isBlank()) {

            return 0L;
        }

        try {

            return Files.size(
                    Paths.get(filePath)
            );

        } catch (IOException e) {

            System.err.println(
                    "Unable to determine recording file size: "
                            + filePath
            );

            return 0L;
        }
    }


    // =========================================================
    // RECORDING WORKER
    // =========================================================

    private static class RecordingWorker
            implements Runnable {

        private final Integer userId;

        private final Integer sessionId;

        private final Path filePath;


        private final AtomicBoolean running =
                new AtomicBoolean(false);


        private volatile Thread workerThread;


        private FFmpegFrameRecorder recorder;

        private Java2DFrameConverter converter;


        // =====================================================
        // CONSTRUCTOR
        // =====================================================

        RecordingWorker(
                Integer userId,
                Integer sessionId,
                Path filePath) {

            this.userId = userId;
            this.sessionId = sessionId;
            this.filePath = filePath;
        }


        // =====================================================
        // START
        // =====================================================

        void start() {

            if (!running.compareAndSet(false, true)) {
                return;
            }

            workerThread =
                    new Thread(
                            this,
                            "ScreenRecorder-User-" + userId
                    );

            workerThread.setDaemon(true);

            try {

                workerThread.start();

            } catch (RuntimeException e) {

                running.set(false);

                throw e;
            }
        }


        // =====================================================
        // RUN RECORDING LOOP
        // =====================================================

        @Override
        public void run() {

            Robot robot = null;

            try {

                // ---------------------------------------------
                // Create Robot
                // ---------------------------------------------

                robot = new Robot();


                // ---------------------------------------------
                // Get screen size
                // ---------------------------------------------

                Dimension screenSize =
                        Toolkit.getDefaultToolkit()
                                .getScreenSize();

                int width =
                        screenSize.width;

                int height =
                        screenSize.height;


                // ---------------------------------------------
                // Ensure even dimensions for YUV420P
                // ---------------------------------------------

                width =
                        width % 2 == 0
                                ? width
                                : width - 1;

                height =
                        height % 2 == 0
                                ? height
                                : height - 1;


                Rectangle screenRectangle =
                        new Rectangle(
                                0,
                                0,
                                width,
                                height
                        );


                // ---------------------------------------------
                // Create FFmpeg recorder
                // ---------------------------------------------

                recorder =
                        new FFmpegFrameRecorder(
                                filePath.toFile(),
                                width,
                                height
                        );


                // ---------------------------------------------
                // MP4 settings
                // ---------------------------------------------

                recorder.setFormat("mp4");

                recorder.setFrameRate(DEFAULT_FPS);

                recorder.setVideoCodec(
                        avcodec.AV_CODEC_ID_H264
                );

                recorder.setPixelFormat(
                        avutil.AV_PIX_FMT_YUV420P
                );


                // ---------------------------------------------
                // Start FFmpeg
                // ---------------------------------------------

                recorder.start();


                // ---------------------------------------------
                // Create converter
                // ---------------------------------------------

                converter =
                        new Java2DFrameConverter();


                // ---------------------------------------------
                // Frame timing
                // ---------------------------------------------

                final long frameIntervalNanos =
                        1_000_000_000L / DEFAULT_FPS;


                System.out.println(
                        "Recording worker initialized."
                                + " User: " + userId
                                + " | Session: " + sessionId
                                + " | Resolution: "
                                + width + "x" + height
                                + " | FPS: " + DEFAULT_FPS
                );


                // ---------------------------------------------
                // Recording loop
                // ---------------------------------------------

                long nextFrameTime =
                        System.nanoTime();

                while (running.get()) {

                    // Capture screen.
                    BufferedImage image =
                            robot.createScreenCapture(
                                    screenRectangle
                            );


                    // Convert image.
                    Frame frame =
                            converter.convert(image);


                    // Record frame.
                    if (frame != null
                            && running.get()) {

                        recorder.record(frame);
                    }


                    // Schedule next frame.
                    nextFrameTime +=
                            frameIntervalNanos;

                    long remainingNanos =
                            nextFrameTime
                                    - System.nanoTime();

                    if (remainingNanos > 0) {

                        try {

                            long millis =
                                    remainingNanos / 1_000_000L;

                            int nanos =
                                    (int) (
                                            remainingNanos
                                                    % 1_000_000L
                                    );

                            Thread.sleep(
                                    millis,
                                    nanos
                            );

                        } catch (InterruptedException e) {

                            // Normal stop operation.
                            break;
                        }

                    } else {

                        // Reset timing if recording falls behind.
                        nextFrameTime =
                                System.nanoTime();
                    }
                }

            } catch (AWTException e) {

                System.err.println(
                        "Unable to access screen for User: "
                                + userId
                );

                e.printStackTrace();

            } catch (Exception e) {

                System.err.println(
                        "Screen recording failed."
                                + " User: " + userId
                                + " | Session: " + sessionId
                );

                e.printStackTrace();

            } finally {

                // Only the recording thread closes FFmpeg.
                closeRecorder();

                running.set(false);

                System.out.println(
                        "Recording worker finished."
                                + " User: " + userId
                                + " | Session: " + sessionId
                );
            }
        }


        // =====================================================
        // STOP WORKER
        // =====================================================

        void stop() {

            running.set(false);

            Thread thread =
                    workerThread;

            if (thread == null
                    || thread == Thread.currentThread()) {

                return;
            }

            thread.interrupt();

            waitForTermination();
        }


        // =====================================================
        // WAIT FOR COMPLETE TERMINATION
        // =====================================================

        void waitForTermination() {

            Thread thread =
                    workerThread;

            if (thread == null
                    || thread == Thread.currentThread()) {

                return;
            }

            boolean interrupted = false;

            /*
             * Do not use a fixed timeout.
             * Wait until the recording thread finishes
             * its finally block and closes FFmpeg.
             */

            while (thread.isAlive()) {

                try {

                    thread.join();

                } catch (InterruptedException e) {

                    /*
                     * Remember interruption, but continue
                     * waiting for safe FFmpeg cleanup.
                     */

                    interrupted = true;
                }
            }

            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }


        // =====================================================
        // CLOSE FFMPEG RECORDER
        // =====================================================

        private void closeRecorder() {

            if (recorder != null) {

                try {

                    recorder.stop();

                } catch (Exception e) {

                    System.err.println(
                            "Unable to stop FFmpeg recorder."
                                    + " User: " + userId
                    );

                    e.printStackTrace();

                } finally {

                    try {

                        recorder.release();

                    } catch (Exception e) {

                        System.err.println(
                                "Unable to release FFmpeg recorder."
                                        + " User: " + userId
                        );

                        e.printStackTrace();
                    }

                    recorder = null;
                }
            }


            if (converter != null) {

                try {

                    converter.close();

                } catch (Exception e) {

                    System.err.println(
                            "Unable to close frame converter."
                    );

                    e.printStackTrace();
                }

                converter = null;
            }
        }


        // =====================================================
        // IS WORKER RUNNING?
        // =====================================================

        boolean isRunning() {

            return running.get();
        }


        // =====================================================
        // GET FILE PATH
        // =====================================================

        String getFilePath() {

            return filePath
                    .toAbsolutePath()
                    .toString();
        }
    }
}
