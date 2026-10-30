package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Screenshot;
import com.example.TeamLoggerC.repository.ScreenshotRepo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ScreenshotService {

    private final ScreenshotRepo screenshotRepo;

    public ScreenshotService(ScreenshotRepo screenshotRepo) {
        this.screenshotRepo = screenshotRepo;
    }


    // =========================================================
    // GET ALL SCREENSHOTS OF AN EMPLOYEE
    // =========================================================

    public List<Screenshot> getEmployeeScreenshots(Integer employeeId) {

        return screenshotRepo.findByUserIdOrderByCapturedAtDesc(employeeId);
    }


    // =========================================================
    // GET TODAY'S SCREENSHOTS
    // =========================================================

    public List<Screenshot> getTodayScreenshots(Integer employeeId) {

        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime endOfDay =
                today.plusDays(1).atStartOfDay();

        return screenshotRepo
                .findByUserIdAndCapturedAtBetweenOrderByCapturedAtDesc(
                        employeeId,
                        startOfDay,
                        endOfDay
                );
    }


    // =========================================================
    // GET SCREENSHOTS OF A SESSION
    // =========================================================

    public List<Screenshot> getSessionScreenshots(Integer sessionId) {

        return screenshotRepo
                .findBySessionIdOrderByCapturedAtDesc(sessionId);
    }


    // =========================================================
    // GET LATEST SCREENSHOT
    // =========================================================

    public Screenshot getLatestScreenshot(Integer employeeId) {

        return screenshotRepo
                .findFirstByUserIdOrderByCapturedAtDesc(employeeId);
    }


    // =========================================================
    // SAVE SCREENSHOT
    // =========================================================

    public Screenshot saveScreenshot(Screenshot screenshot) {

        return screenshotRepo.save(screenshot);
    }


    // =========================================================
    // FIND SCREENSHOT BY ID
    // =========================================================

    public Screenshot getScreenshotById(Integer screenshotId) {

        return screenshotRepo
                .findById(screenshotId)
                .orElse(null);
    }


    // =========================================================
    // DELETE SCREENSHOT
    // =========================================================

    public void deleteScreenshot(Integer screenshotId) {

        screenshotRepo.deleteById(screenshotId);
    }


    // =========================================================
    // FIND LOCAL SCREENSHOTS READY FOR ARCHIVING
    //
    // Screenshots older than 7 days are moved from
    // local storage into the database.
    // =========================================================

    public List<Screenshot> getScreenshotsReadyForArchive(
            LocalDateTime cutoff) {

        return screenshotRepo
                .findByStorageTypeAndCapturedAtBefore(
                        "LOCAL",
                        cutoff
                );
    }


    // =========================================================
    // FIND EXPIRED SCREENSHOTS
    //
    // These screenshots have completed their 2-month
    // database retention period.
    // =========================================================

    public List<Screenshot> getExpiredScreenshots(
            LocalDateTime now) {

        return screenshotRepo
                .findByExpiresAtBefore(now);
    }
}