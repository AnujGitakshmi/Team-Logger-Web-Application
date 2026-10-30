package com.example.TeamLoggerC.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AWTException;
import java.awt.Dimension;
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
import java.util.UUID;

@Service
public class ScreenshotStorageService {

// =========================================================
// SCREENSHOT DIRECTORY
// =========================================================

    private final String screenshotDirectory;


// =========================================================
// FILE NAME FORMAT
// =========================================================

    private static final DateTimeFormatter FILE_DATE_FORMAT =
            DateTimeFormatter.ofPattern(
                    "yyyyMMdd_HHmmss_SSS"
            );


// =========================================================
// CONSTRUCTOR
// =========================================================

    public ScreenshotStorageService(
            @Value("${teamlogger.screenshot.directory:./screenshots}")
            String screenshotDirectory) {

        this.screenshotDirectory =
                screenshotDirectory;
    }


// =========================================================
// CAPTURE SCREEN
// =========================================================

    public String captureScreen(Integer userId) {

        try {

            System.out.println(
                    "Java headless mode: "
                            + java.awt.GraphicsEnvironment
                            .isHeadless()
            );


            // -------------------------------------------------
            // CREATE ROBOT
            // -------------------------------------------------

            Robot robot =
                    new Robot();


            // -------------------------------------------------
            // GET SCREEN SIZE
            // -------------------------------------------------

            Dimension screenSize =
                    Toolkit.getDefaultToolkit()
                            .getScreenSize();


            // -------------------------------------------------
            // DEFINE COMPLETE SCREEN AREA
            // -------------------------------------------------

            Rectangle screenRectangle =
                    new Rectangle(
                            screenSize
                    );


            // -------------------------------------------------
            // CAPTURE SCREEN
            // -------------------------------------------------

            BufferedImage screenshot =
                    robot.createScreenCapture(
                            screenRectangle
                    );


            // -------------------------------------------------
            // CREATE EMPLOYEE DIRECTORY
            // -------------------------------------------------

            Path employeeDirectory =
                    Paths.get(
                            screenshotDirectory,
                            "employee_" + userId
                    );


            Files.createDirectories(
                    employeeDirectory
            );


            // -------------------------------------------------
            // CREATE TIMESTAMP
            // -------------------------------------------------

            String timestamp =
                    LocalDateTime.now()
                            .format(
                                    FILE_DATE_FORMAT
                            );


            // -------------------------------------------------
            // CREATE UNIQUE IDENTIFIER
            // -------------------------------------------------

            String uniqueId =
                    UUID.randomUUID()
                            .toString()
                            .replace(
                                    "-",
                                    ""
                            );


            // -------------------------------------------------
            // CREATE UNIQUE FILE NAME
            // -------------------------------------------------

            String fileName =
                    "screenshot_"
                            + timestamp
                            + "_"
                            + uniqueId
                            + ".png";


            // Example:
            //
            // screenshot_20260928_110530_247_
            // 7f4e8b1c2a6d4f8a9c3b7e1d5a6f2c10.png
            //


            // -------------------------------------------------
            // COMPLETE FILE PATH
            // -------------------------------------------------

            Path filePath =
                    employeeDirectory.resolve(
                            fileName
                    );


            // -------------------------------------------------
            // SAVE SCREENSHOT AS PNG
            // -------------------------------------------------

            boolean saved =
                    ImageIO.write(
                            screenshot,
                            "png",
                            filePath.toFile()
                    );


            if (!saved) {

                throw new IOException(
                        "PNG image writer is not available."
                );
            }


            // -------------------------------------------------
            // LOG RESULT
            // -------------------------------------------------

            System.out.println(
                    "Screenshot captured successfully."
                            + " User: "
                            + userId
                            + " | File: "
                            + fileName
                            + " | Path: "
                            + filePath
                            .toAbsolutePath()
            );


            // -------------------------------------------------
            // RETURN ABSOLUTE PATH
            // -------------------------------------------------

            return filePath
                    .toAbsolutePath()
                    .toString();


        } catch (AWTException e) {

            throw new RuntimeException(
                    "Unable to capture screen for User: "
                            + userId,
                    e
            );


        } catch (IOException e) {

            throw new RuntimeException(
                    "Unable to save screenshot for User: "
                            + userId,
                    e
            );
        }
    }


// =========================================================
// DELETE SCREENSHOT FILE
// =========================================================

    public void deleteScreenshotFile(
            String filePath) {

        if (filePath == null
                || filePath.isBlank()) {

            return;
        }


        try {

            Path path =
                    Paths.get(
                            filePath
                    );


            boolean deleted =
                    Files.deleteIfExists(
                            path
                    );


            if (deleted) {

                System.out.println(
                        "Screenshot file deleted: "
                                + filePath
                );

            } else {

                System.out.println(
                        "Screenshot file already "
                                + "does not exist: "
                                + filePath
                );
            }


        } catch (IOException e) {

            System.err.println(
                    "Unable to delete screenshot: "
                            + filePath
            );

            e.printStackTrace();
        }
    }


}
