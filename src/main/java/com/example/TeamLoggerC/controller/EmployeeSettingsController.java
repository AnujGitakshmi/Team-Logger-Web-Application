package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Controller
@RequestMapping("/employee/settings")
public class EmployeeSettingsController {

    private final UserRepo userRepo;

    /*
     * Profile photos will be stored here:
     *

     *
     * Using the project working directory makes this
     * portable between development environments.
     */
    private final Path uploadDirectory =
            Paths.get("uploads", "profile")
                    .toAbsolutePath()
                    .normalize();

    public EmployeeSettingsController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }


    // =====================================================
    // EMPLOYEE SETTINGS PAGE
    // =====================================================

    @GetMapping
    public String settings(
            Authentication authentication,
            Model model) {

        User employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", employee);

        return "settings";
    }


    // =====================================================
    // SAVE EMPLOYEE SETTINGS
    // =====================================================

    @PostMapping("/save")
    public String saveSettings(
            Authentication authentication,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) MultipartFile profilePhoto,
            RedirectAttributes redirectAttributes) {

        User employee = getLoggedInEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        // =================================================
        // SAVE DESCRIPTION
        // =================================================

        if (description != null) {

            employee.setDescription(
                    description.trim()
            );
        }


        // =================================================
        // SAVE PROFILE PHOTO
        // =================================================

        if (profilePhoto != null
                && !profilePhoto.isEmpty()) {

            try {

                String originalFilename =
                        profilePhoto.getOriginalFilename();

                if (originalFilename == null
                        || originalFilename.isBlank()) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Invalid profile photo."
                    );

                    return "redirect:/employee/settings";
                }


                // -----------------------------------------
                // VALIDATE FILE TYPE
                // -----------------------------------------

                String contentType =
                        profilePhoto.getContentType();

                if (contentType == null
                        || !(contentType.equalsIgnoreCase("image/jpeg")
                        || contentType.equalsIgnoreCase("image/png")
                        || contentType.equalsIgnoreCase("image/webp"))) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Only JPG, PNG and WEBP images are allowed."
                    );

                    return "redirect:/employee/settings";
                }


                // -----------------------------------------
                // CREATE UPLOAD DIRECTORY
                // -----------------------------------------

                Files.createDirectories(uploadDirectory);


                // -----------------------------------------
                // CREATE UNIQUE FILE NAME
                // -----------------------------------------

                String extension =
                        getFileExtension(originalFilename);

                String fileName =
                        "employee_"
                                + employee.getId()
                                + "_"
                                + UUID.randomUUID()
                                + extension;


                // -----------------------------------------
                // SAVE FILE
                // -----------------------------------------

                Path targetPath =
                        uploadDirectory.resolve(fileName)
                                .toAbsolutePath()
                                .normalize();

                if (!targetPath.startsWith(uploadDirectory)) {

                    redirectAttributes.addFlashAttribute(
                            "error",
                            "Invalid file path."
                    );

                    return "redirect:/employee/settings";
                }


                Files.copy(
                        profilePhoto.getInputStream(),
                        targetPath,
                        StandardCopyOption.REPLACE_EXISTING
                );


                // -----------------------------------------
                // DELETE OLD PHOTO
                // -----------------------------------------

                String oldPhoto =
                        employee.getProfilePhoto();

                if (oldPhoto != null
                        && !oldPhoto.isBlank()
                        && !oldPhoto.equals(fileName)) {

                    try {

                        Path oldPath =
                                uploadDirectory
                                        .resolve(oldPhoto)
                                        .toAbsolutePath()
                                        .normalize();

                        if (oldPath.startsWith(uploadDirectory)) {

                            Files.deleteIfExists(oldPath);
                        }

                    } catch (IOException ignored) {

                        /*
                         * If deleting the old image fails,
                         * do not fail the entire settings update.
                         */
                    }
                }


                // -----------------------------------------
                // STORE ONLY FILE NAME IN DATABASE
                // -----------------------------------------

                employee.setProfilePhoto(fileName);

            } catch (IOException ex) {

                redirectAttributes.addFlashAttribute(
                        "error",
                        "Unable to upload profile photo."
                );

                return "redirect:/employee/settings";
            }
        }


        // =================================================
        // SAVE USER
        // =================================================

        userRepo.save(employee);


        redirectAttributes.addFlashAttribute(
                "success",
                "Profile settings updated successfully."
        );

        return "redirect:/employee/settings";
    }


    // =====================================================
    // GET LOGGED-IN EMPLOYEE
    // =====================================================

    private User getLoggedInEmployee(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return null;
        }

        if (!(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return null;
        }

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        return userDetails.getUser();
    }


    // =====================================================
    // FILE EXTENSION
    // =====================================================

    private String getFileExtension(
            String fileName) {

        int lastDot =
                fileName.lastIndexOf('.');

        if (lastDot == -1) {
            return "";
        }

        return fileName
                .substring(lastDot)
                .toLowerCase();
    }
}
