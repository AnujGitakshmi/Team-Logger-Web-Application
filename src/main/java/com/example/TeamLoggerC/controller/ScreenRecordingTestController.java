package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.service.ScreenRecordingStorageService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test/recording")
public class ScreenRecordingTestController {

    private final ScreenRecordingStorageService recordingStorageService;

    public ScreenRecordingTestController(
            ScreenRecordingStorageService recordingStorageService) {
        this.recordingStorageService = recordingStorageService;
    }

    @PostMapping("/start/{userId}/{sessionId}")
    public String startRecording(
            @PathVariable Integer userId,
            @PathVariable Integer sessionId) {

        return recordingStorageService.startRecording(
                userId,
                sessionId
        );
    }

    @PostMapping("/stop/{userId}")
    public String stopRecording(
            @PathVariable Integer userId) {

        String filePath =
                recordingStorageService.stopRecording(userId);

        if (filePath == null) {
            return "No active recording found.";
        }

        return "Recording stopped: " + filePath;
    }

    @GetMapping("/status/{userId}")
    public String status(
            @PathVariable Integer userId) {

        return recordingStorageService.isRecording(userId)
                ? "Recording is running."
                : "Recording is not running.";
    }
}