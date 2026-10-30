 package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Screenshot;
import com.example.TeamLoggerC.repository.ScreenshotRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ScreenshotRetentionService {

    private final ScreenshotRepo screenshotRepo;
    private final ScreenshotStorageService storageService;

    private static final DateTimeFormatter ZIP_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy_MM");

    /*
     * ZIP files will be stored here:
     *
     * screenshot_archives/
     *     employee_10/
     *         screenshots_2026_08.zip
     *         screenshots_2026_09.zip
     */
    private static final String ARCHIVE_DIRECTORY =
            "./screenshot_archives";


    public ScreenshotRetentionService(
            ScreenshotRepo screenshotRepo,
            ScreenshotStorageService storageService) {

        this.screenshotRepo = screenshotRepo;
        this.storageService = storageService;
    }


    // =========================================================
    // RETENTION PROCESS
    //
    // 0 - 7 DAYS
    //      LOCAL PNG
    //
    // 7 DAYS - 1 MONTH
    //      DATABASE LONGBLOB
    //
    // AFTER 1 MONTH
    //      ZIP ARCHIVE
    // =========================================================

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void runRetentionTask() {

        System.out.println(
                "================================================="
        );

        System.out.println(
                "Screenshot retention task started: "
                        + LocalDateTime.now()
        );

        System.out.println(
                "================================================="
        );


        // -----------------------------------------------------
        // STEP 1
        // LOCAL → DATABASE
        // -----------------------------------------------------

        moveOldScreenshotsToDatabase();


        // -----------------------------------------------------
        // STEP 2
        // DATABASE → ZIP
        // -----------------------------------------------------

        moveOldDatabaseScreenshotsToZip();


        System.out.println(
                "================================================="
        );

        System.out.println(
                "Screenshot retention task completed: "
                        + LocalDateTime.now()
        );

        System.out.println(
                "================================================="
        );
    }


    // =========================================================
    // STEP 1
    // LOCAL → DATABASE
    //
    // Screenshots older than 7 days are moved into
    // image_data (LONGBLOB).
    // =========================================================

    @Transactional
    public void moveOldScreenshotsToDatabase() {

        LocalDateTime sevenDaysAgo =
                LocalDateTime.now().minusDays(7);


        List<Screenshot> screenshots =
                screenshotRepo
                        .findByStorageTypeAndCapturedAtBeforeAndArchivedFalse(
                                "LOCAL",
                                sevenDaysAgo
                        );


        if (screenshots.isEmpty()) {

            System.out.println(
                    "No LOCAL screenshots found "
                            + "for 7-day migration."
            );

            return;
        }


        System.out.println(
                "Found "
                        + screenshots.size()
                        + " LOCAL screenshot(s) "
                        + "for database migration."
        );


        for (Screenshot screenshot : screenshots) {

            moveScreenshotToDatabase(
                    screenshot
            );
        }
    }


    // =========================================================
    // MOVE ONE LOCAL SCREENSHOT → DATABASE
    // =========================================================

    private void moveScreenshotToDatabase(
            Screenshot screenshot) {

        String filePath =
                screenshot.getFilePath();


        if (filePath == null
                || filePath.isBlank()) {

            System.err.println(
                    "Cannot migrate screenshot "
                            + screenshot.getId()
                            + ". File path is empty."
            );

            return;
        }


        Path path =
                Paths.get(filePath);


        // -----------------------------------------------------
        // CHECK FILE
        // -----------------------------------------------------

        if (!Files.exists(path)) {

            System.err.println(
                    "Cannot migrate screenshot "
                            + screenshot.getId()
                            + ". Local file not found: "
                            + filePath
            );

            /*
             * IMPORTANT:
             *
             * Do NOT mark this DATABASE.
             *
             * There is no image_data.
             *
             * Do NOT mark archived=true either.
             *
             * The record remains LOCAL because migration
             * was not successful.
             */

            return;
        }


        try {

            // -------------------------------------------------
            // READ PNG
            // -------------------------------------------------

            byte[] imageBytes =
                    Files.readAllBytes(path);


            if (imageBytes.length == 0) {

                System.err.println(
                        "Cannot migrate screenshot "
                                + screenshot.getId()
                                + ". Image file is empty."
                );

                return;
            }


            // -------------------------------------------------
            // SAVE IMAGE INTO DATABASE
            // -------------------------------------------------

            screenshot.setImageData(
                    imageBytes
            );

            screenshot.setStorageType(
                    "DATABASE"
            );

            screenshot.setArchived(
                    false
            );

            /*
             * archivedAt means:
             *
             * "When was this screenshot archived into ZIP?"
             *
             * Therefore it must remain NULL here.
             */

            screenshot.setArchivedAt(
                    null
            );

            screenshot.setArchivePath(
                    null
            );


            // -------------------------------------------------
            // SAVE DATABASE RECORD
            // -------------------------------------------------

            screenshotRepo.save(
                    screenshot
            );


            // -------------------------------------------------
            // CHECK DUPLICATE LOCAL REFERENCES
            // -----------------------------------------------------

            long remainingLocalReferences =
                    screenshotRepo.countByFilePathAndStorageType(
                            filePath,
                            "LOCAL"
                    );


            // -------------------------------------------------
            // DELETE PHYSICAL FILE ONLY IF SAFE
            // -----------------------------------------------------

            if (remainingLocalReferences == 0) {

                storageService.deleteScreenshotFile(
                        filePath
                );

                System.out.println(
                        "Local PNG deleted after successful "
                                + "database migration: "
                                + filePath
                );

            } else {

                System.out.println(
                        "Local PNG retained because "
                                + remainingLocalReferences
                                + " other LOCAL record(s) "
                                + "reference the same file."
                );
            }


            System.out.println(
                    "Screenshot migrated to DATABASE."
                            + " ID="
                            + screenshot.getId()
                            + " | File="
                            + screenshot.getFileName()
                            + " | Size="
                            + imageBytes.length
                            + " bytes"
            );


        } catch (IOException e) {

            System.err.println(
                    "Failed to migrate screenshot "
                            + screenshot.getId()
                            + " to DATABASE."
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // STEP 2
    // DATABASE → ZIP
    //
    // Screenshots older than 1 calendar month are archived.
    //
    // ZIP grouping:
    //
    // employee_10/
    //     screenshots_2026_08.zip
    //
    // employee_11/
    //     screenshots_2026_08.zip
    // =========================================================

    @Transactional
    public void moveOldDatabaseScreenshotsToZip() {

        LocalDateTime oneMonthAgo =
                LocalDateTime.now().minusMonths(1);


        List<Screenshot> screenshots =
                screenshotRepo
                        .findByStorageTypeAndCapturedAtBeforeAndArchivedFalse(
                                "DATABASE",
                                oneMonthAgo
                        );


        if (screenshots.isEmpty()) {

            System.out.println(
                    "No DATABASE screenshots found "
                            + "for 1-month ZIP migration."
            );

            return;
        }


        System.out.println(
                "Found "
                        + screenshots.size()
                        + " DATABASE screenshot(s) "
                        + "for ZIP migration."
        );


        // -----------------------------------------------------
        // GROUP BY EMPLOYEE + MONTH
        // -----------------------------------------------------

        Map<String, List<Screenshot>> groups =
                new LinkedHashMap<>();


        for (Screenshot screenshot : screenshots) {

            if (screenshot.getCapturedAt() == null) {

                System.err.println(
                        "Skipping screenshot "
                                + screenshot.getId()
                                + ". capturedAt is null."
                );

                continue;
            }


            YearMonth yearMonth =
                    YearMonth.from(
                            screenshot.getCapturedAt()
                    );


            String groupKey =
                    screenshot.getUserId()
                            + "_"
                            + yearMonth;


            groups.computeIfAbsent(
                    groupKey,
                    key -> new ArrayList<>()
            ).add(screenshot);
        }


        // -----------------------------------------------------
        // CREATE ONE ZIP PER EMPLOYEE / MONTH
        // -----------------------------------------------------

        for (List<Screenshot> group :
                groups.values()) {

            archiveScreenshotGroup(
                    group
            );
        }
    }


    // =========================================================
    // ARCHIVE ONE EMPLOYEE/MONTH GROUP
    // =========================================================

    private void archiveScreenshotGroup(
            List<Screenshot> screenshots) {

        if (screenshots == null
                || screenshots.isEmpty()) {

            return;
        }


        Screenshot first =
                screenshots.get(0);


        YearMonth yearMonth =
                YearMonth.from(
                        first.getCapturedAt()
                );


        // -----------------------------------------------------
        // CREATE DIRECTORY
        // -----------------------------------------------------

        Path employeeArchiveDirectory =
                Paths.get(
                                ARCHIVE_DIRECTORY,
                                "employee_" + first.getUserId()
                        )
                        .toAbsolutePath()
                        .normalize();


        try {

            Files.createDirectories(
                    employeeArchiveDirectory
            );


            // -------------------------------------------------
            // FINAL ZIP PATH
            // -------------------------------------------------

            Path zipPath =
                    employeeArchiveDirectory.resolve(
                            "screenshots_"
                                    + yearMonth.format(
                                    ZIP_DATE_FORMAT
                            )
                                    + ".zip"
                    );


            /*
             * We first create a temporary ZIP.
             *
             * This prevents a partially-created ZIP from
             * being treated as the final archive.
             */

            Path tempZipPath =
                    employeeArchiveDirectory.resolve(
                            "screenshots_"
                                    + yearMonth.format(
                                    ZIP_DATE_FORMAT
                            )
                                    + ".zip.tmp"
                    );


            Files.deleteIfExists(
                    tempZipPath
            );


            int archivedCount = 0;


            // -------------------------------------------------
            // CREATE ZIP
            // -------------------------------------------------

            try (
                    ZipOutputStream zipOutputStream =
                            new ZipOutputStream(
                                    Files.newOutputStream(
                                            tempZipPath
                                    )
                            )
            ) {

                for (Screenshot screenshot :
                        screenshots) {

                    byte[] imageData =
                            screenshot.getImageData();


                    if (imageData == null
                            || imageData.length == 0) {

                        System.err.println(
                                "Skipping screenshot "
                                        + screenshot.getId()
                                        + ". image_data is empty."
                        );

                        continue;
                    }


                    String entryName =
                            createUniqueZipEntryName(
                                    screenshot,
                                    screenshots
                            );


                    ZipEntry zipEntry =
                            new ZipEntry(
                                    entryName
                            );


                    zipOutputStream.putNextEntry(
                            zipEntry
                    );


                    zipOutputStream.write(
                            imageData
                    );


                    zipOutputStream.closeEntry();


                    archivedCount++;
                }
            }


            // -------------------------------------------------
            // NOTHING TO ARCHIVE
            // -------------------------------------------------

            if (archivedCount == 0) {

                Files.deleteIfExists(
                        tempZipPath
                );

                System.out.println(
                        "No valid image_data found for "
                                + "employee "
                                + first.getUserId()
                                + " / "
                                + yearMonth
                );

                return;
            }


            // -------------------------------------------------
            // REPLACE FINAL ZIP
            // -------------------------------------------------

            moveTempZipToFinal(
                    tempZipPath,
                    zipPath
            );


            // -------------------------------------------------
            // UPDATE DATABASE
            // -------------------------------------------------

            LocalDateTime archivedAt =
                    LocalDateTime.now();


            for (Screenshot screenshot :
                    screenshots) {

                if (screenshot.getImageData() == null
                        || screenshot.getImageData().length == 0) {

                    continue;
                }


                screenshot.setImageData(
                        null
                );


                screenshot.setStorageType(
                        "ZIP"
                );


                screenshot.setArchived(
                        true
                );


                screenshot.setArchivedAt(
                        archivedAt
                );


                screenshot.setArchivePath(
                        zipPath.toString()
                );


                screenshotRepo.save(
                        screenshot
                );
            }


            System.out.println(
                    "================================================="
            );

            System.out.println(
                    "ZIP archive created successfully."
            );

            System.out.println(
                    "Employee: "
                            + first.getUserId()
            );

            System.out.println(
                    "Month: "
                            + yearMonth
            );

            System.out.println(
                    "Screenshots archived: "
                            + archivedCount
            );

            System.out.println(
                    "ZIP: "
                            + zipPath
            );

            System.out.println(
                    "================================================="
            );


        } catch (IOException e) {

            System.err.println(
                    "Failed to create ZIP archive for "
                            + "employee "
                            + first.getUserId()
                            + " / "
                            + yearMonth
            );

            e.printStackTrace();
        }
    }


    // =========================================================
    // CREATE UNIQUE ZIP ENTRY NAME
    // =========================================================

    private String createUniqueZipEntryName(
            Screenshot screenshot,
            List<Screenshot> screenshots) {

        String originalName =
                screenshot.getFileName();


        if (originalName == null
                || originalName.isBlank()) {

            originalName =
                    "screenshot.png";
        }


        /*
         * New screenshots will have unique filenames.
         *
         * Old records may contain duplicate filenames.
         *
         * To avoid duplicate ZIP entries, use the database
         * ID when a duplicate filename exists.
         */

        int duplicateCount = 0;


        for (Screenshot other :
                screenshots) {

            if (other.getFileName() != null
                    && other.getFileName()
                    .equals(originalName)) {

                duplicateCount++;
            }
        }


        if (duplicateCount <= 1) {

            return originalName;
        }


        int dotIndex =
                originalName.lastIndexOf('.');


        if (dotIndex > 0) {

            return originalName.substring(
                    0,
                    dotIndex
            )
                    + "_id"
                    + screenshot.getId()
                    + originalName.substring(
                    dotIndex
            );
        }


        return originalName
                + "_id"
                + screenshot.getId();
    }


    // =========================================================
    // MOVE TEMP ZIP → FINAL ZIP
    // =========================================================

    private void moveTempZipToFinal(
            Path tempZipPath,
            Path zipPath)
            throws IOException {

        try {

            Files.move(
                    tempZipPath,
                    zipPath,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } catch (java.nio.file.AtomicMoveNotSupportedException e) {

            Files.move(
                    tempZipPath,
                    zipPath,
                    StandardCopyOption.REPLACE_EXISTING
            );
        }
    }
}

