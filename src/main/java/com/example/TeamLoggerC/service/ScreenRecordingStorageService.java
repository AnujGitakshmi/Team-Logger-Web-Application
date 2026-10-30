package com.example.TeamLoggerC.service;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

//import javax.imageio.ImageIO;
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
import org.bytedeco.javacv.FFmpegLogCallback;

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

    //private static final int VIDEO_BITRATE = 2_500_000;


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

        FFmpegLogCallback.set();

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


        // -----------------------------------------------------
        // Check Java headless mode
        // -----------------------------------------------------

        if (GraphicsEnvironment.isHeadless()) {

            throw new IllegalStateException(
                    "Screen recording cannot start because "
                            + "the application is running in headless mode."
            );
        }


        // -----------------------------------------------------
        // Prevent duplicate recording
        // -----------------------------------------------------

        RecordingWorker existing =
                activeRecordings.get(userId);

        if (existing != null && existing.isRunning()) {

            System.out.println(
                    "Screen recording is already running."
                            + " User: " + userId
            );

            return existing.getFilePath();
        }


        // -----------------------------------------------------
        // Create employee/session directory
        // -----------------------------------------------------

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


        // -----------------------------------------------------
        // Create unique timestamp
        // -----------------------------------------------------

        String timestamp =
                LocalDateTime.now()
                        .format(FILE_DATE_FORMAT);


        // -----------------------------------------------------
        // Create unique file name
        //
        // Example:
        //
        // recording_20260928_160044_237_a8f31c.mp4
        // -----------------------------------------------------

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
                sessionDirectory.resolve(
                        fileName
                );


        // -----------------------------------------------------
        // Create recording worker
        // -----------------------------------------------------

        RecordingWorker worker =
                new RecordingWorker(
                        userId,
                        sessionId,
                        filePath
                );


        // -----------------------------------------------------
        // Store worker
        // -----------------------------------------------------

        RecordingWorker previous =
                activeRecordings.putIfAbsent(
                        userId,
                        worker
                );

        if (previous != null) {

            if (previous.isRunning()) {

                return previous.getFilePath();
            }

            activeRecordings.remove(
                    userId,
                    previous
            );

            activeRecordings.put(
                    userId,
                    worker
            );
        }


        // -----------------------------------------------------
        // Start worker
        // -----------------------------------------------------

        try {

            worker.start();

        } catch (RuntimeException e) {

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


    // =========================================================
    // STOP RECORDING
    // =========================================================

    public String stopRecording(Integer userId) {

        if (userId == null) {
            return null;
        }


        RecordingWorker worker =
                activeRecordings.remove(userId);


        if (worker == null) {

            System.out.println(
                    "No active screen recording found."
                            + " User: " + userId
            );

            return null;
        }


        /*
         * IMPORTANT:
         *
         * stop() does NOT directly call recorder.stop()
         * or recorder.release().
         *
         * The recording thread owns the FFmpeg recorder
         * and performs cleanup inside run() -> finally.
         *
         * This prevents concurrent FFmpeg access.
         */

        worker.stop();


        System.out.println(
                "Screen recording stopped."
                        + " User: " + userId
                        + " | File: "
                        + worker.getFilePath()
        );


        return worker.getFilePath();
    }


    // =========================================================
    // CHECK RECORDING STATUS
    // =========================================================

    public boolean isRecording(Integer userId) {

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

        for (Integer userId :
                activeRecordings.keySet()) {

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


            // Allow application shutdown.
            workerThread.setDaemon(true);


            workerThread.start();
        }


        // =====================================================
        // RUN RECORDING LOOP
        // =====================================================

        @Override
        public void run() {

            Robot robot = null;


            try {

                // -------------------------------------------------
                // Create Robot
                // -------------------------------------------------

                robot = new Robot();


                // -------------------------------------------------
                // Get complete screen size
                // -------------------------------------------------

                Dimension screenSize =
                        Toolkit.getDefaultToolkit()
                                .getScreenSize();


                Rectangle screenRectangle =
                        new Rectangle(
                                screenSize
                        );


                int width =
                        screenRectangle.width;


                int height =
                        screenRectangle.height;


                // -------------------------------------------------
                // Ensure dimensions are compatible with YUV420P
                //
                // H.264/YUV420P generally requires even dimensions.
                // -------------------------------------------------

                width =
                        width % 2 == 0
                                ? width
                                : width - 1;


                height =
                        height % 2 == 0
                                ? height
                                : height - 1;


                screenRectangle =
                        new Rectangle(
                                0,
                                0,
                                width,
                                height
                        );


                // -------------------------------------------------
                // Create FFmpeg recorder
                // -------------------------------------------------

                recorder =
                        new FFmpegFrameRecorder(
                                filePath.toFile(),
                                width,
                                height
                        );


                // -------------------------------------------------
                // Basic MP4 settings
                // -------------------------------------------------

//                recorder.setFormat("mp4");
//
//
//                recorder.setFrameRate(
//                        DEFAULT_FPS
//                );
//
//
//                recorder.setVideoBitrate(
//                        VIDEO_BITRATE
//                );
//
//
//                recorder.setVideoCodec(
//                        avcodec.AV_CODEC_ID_H264
//                );
//
//
//                recorder.setPixelFormat(
//                        avutil.AV_PIX_FMT_YUV420P
//                );
//
//
//                // -------------------------------------------------
//                // H.264 encoder settings
//                //
//                // The previous OpenH264 warning about bitrate
//                // control can be reduced by explicitly using
//                // constant-quality mode rather than asking
//                // OpenH264 to perform incompatible bitrate control.
//                // -------------------------------------------------
//
//                recorder.setVideoOption(
//                        "profile",
//                        "baseline"
//                );

                recorder.setFormat("mp4");

                recorder.setFrameRate(DEFAULT_FPS);

                recorder.setVideoCodec(
                        avcodec.AV_CODEC_ID_H264
                );

                recorder.setPixelFormat(
                        avutil.AV_PIX_FMT_YUV420P
                );


                // -------------------------------------------------
                // Start FFmpeg
                // -------------------------------------------------

                recorder.start();


                // -------------------------------------------------
                // Create converter
                // -------------------------------------------------

                converter =
                        new Java2DFrameConverter();


                // -------------------------------------------------
                // Frame timing
                // -------------------------------------------------

                final long frameIntervalNanos =
                        1_000_000_000L / DEFAULT_FPS;


                System.out.println(
                        "Recording worker initialized."
                                + " User: " + userId
                                + " | Session: " + sessionId
                                + " | Resolution: "
                                + width + "x" + height
                                + " | FPS: "
                                + DEFAULT_FPS
                );


                // -------------------------------------------------
                // Recording loop
                // -------------------------------------------------

                long nextFrameTime =
                        System.nanoTime();


                while (running.get()) {

                    // ---------------------------------------------
                    // Capture screen
                    // ---------------------------------------------

                    BufferedImage image =
                            robot.createScreenCapture(
                                    screenRectangle
                            );


                    // ---------------------------------------------
                    // Convert image
                    // ---------------------------------------------

                    Frame frame =
                            converter.convert(
                                    image
                            );


                    // ---------------------------------------------
                    // Record frame
                    // ---------------------------------------------

                    if (frame != null
                            && running.get()) {

                        recorder.record(
                                frame
                        );
                    }


                    // ---------------------------------------------
                    // Schedule next frame
                    // ---------------------------------------------

                    nextFrameTime +=
                            frameIntervalNanos;


                    long remainingNanos =
                            nextFrameTime
                                    - System.nanoTime();


                    if (remainingNanos > 0) {

                        try {

                            long millis =
                                    remainingNanos
                                            / 1_000_000L;


                            int nanos =
                                    (int)
                                            (remainingNanos
                                                    % 1_000_000L);


                            Thread.sleep(
                                    millis,
                                    nanos
                            );


                        } catch (InterruptedException e) {

                            /*
                             * Normal stop operation.
                             *
                             * Do not print this as an error.
                             */

                            Thread.currentThread()
                                    .interrupt();

                            break;
                        }
                    } else {

                        /*
                         * Recording took longer than the frame
                         * interval. Reset timing rather than
                         * continuously falling behind.
                         */

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

                // -------------------------------------------------
                // ONLY THE RECORDING THREAD CLOSES FFmpeg
                // -------------------------------------------------

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

            /*
             * Only signal the worker to stop.
             *
             * Do NOT call closeRecorder() here.
             */

            running.set(false);


            Thread thread =
                    workerThread;


            if (thread != null
                    && thread != Thread.currentThread()) {

                thread.interrupt();


                try {

                    /*
                     * Wait for the recording thread to execute
                     * its finally block and close FFmpeg.
                     */

                    thread.join(5000);


                } catch (InterruptedException e) {

                    Thread.currentThread()
                            .interrupt();
                }
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

                } finally {

                    try {

                        recorder.release();

                    } catch (Exception e) {

                        System.err.println(
                                "Unable to release FFmpeg recorder."
                                        + " User: " + userId
                        );
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