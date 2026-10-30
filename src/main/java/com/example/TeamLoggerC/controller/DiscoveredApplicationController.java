package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.DiscoveredApplication;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.DiscoveredApplicationService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/discovered-applications")
public class DiscoveredApplicationController {

    private final DiscoveredApplicationService
            discoveredApplicationService;

    private final UserRepo userRepo;


    public DiscoveredApplicationController(
            DiscoveredApplicationService discoveredApplicationService,
            UserRepo userRepo
    ) {
        this.discoveredApplicationService =
                discoveredApplicationService;

        this.userRepo =
                userRepo;
    }


    // =========================================================
    // APPLICATION INVENTORY
    // =========================================================

    @GetMapping
    public String applications(
            Authentication authentication,
            Model model
    ) {

        User admin =
                getAuthenticatedAdmin(authentication);

        List<DiscoveredApplication> applications =
                discoveredApplicationService
                        .getApplicationsForAdmin(
                                admin.getId()
                        );

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "applications",
                applications
        );

        model.addAttribute(
                "activeApplicationCount",
                discoveredApplicationService
                        .countActiveApplications(
                                admin.getId()
                        )
        );

        model.addAttribute(
                "totalApplicationCount",
                discoveredApplicationService
                        .countApplications(
                                admin.getId()
                        )
        );

        return "discovered-applications";
    }


    // =========================================================
    // SEARCH APPLICATIONS
    // =========================================================

    @GetMapping("/search")
    public String searchApplications(
            @RequestParam(required = false)
            String query,

            Authentication authentication,

            Model model
    ) {

        User admin =
                getAuthenticatedAdmin(authentication);

        List<DiscoveredApplication> applications =
                discoveredApplicationService
                        .searchApplications(
                                admin.getId(),
                                query
                        );

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "applications",
                applications
        );

        model.addAttribute(
                "searchQuery",
                query
        );

        model.addAttribute(
                "activeApplicationCount",
                discoveredApplicationService
                        .countActiveApplications(
                                admin.getId()
                        )
        );

        model.addAttribute(
                "totalApplicationCount",
                discoveredApplicationService
                        .countApplications(
                                admin.getId()
                        )
        );

        return "discovered-applications";
    }


    // =========================================================
    // APPLICATION DETAILS
    // =========================================================

    @GetMapping("/{id}")
    public String applicationDetails(
            @PathVariable Long id,

            Authentication authentication,

            Model model
    ) {

        User admin =
                getAuthenticatedAdmin(authentication);

        DiscoveredApplication application =
                discoveredApplicationService
                        .getApplicationForAdmin(
                                admin.getId(),
                                id
                        );

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "application",
                application
        );

        return "discovered-application-details";
    }


    // =========================================================
    // ACTIVATE APPLICATION
    // =========================================================

    @PostMapping("/activate/{id}")
    public String activateApplication(
            @PathVariable Long id,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );

            discoveredApplicationService
                    .activateApplication(
                            admin.getId(),
                            id
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Application activated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/discovered-applications";
    }


    // =========================================================
    // DEACTIVATE APPLICATION
    // =========================================================

    @PostMapping("/deactivate/{id}")
    public String deactivateApplication(
            @PathVariable Long id,

            Authentication authentication,

            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );

            discoveredApplicationService
                    .deactivateApplication(
                            admin.getId(),
                            id
                    );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Application deactivated successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/discovered-applications";
    }


    // =========================================================
    // ADMIN AUTHENTICATION
    // =========================================================

    private User getAuthenticatedAdmin(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalArgumentException(
                    "Admin authentication is required."
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof CustomUserDetails userDetails)) {

            throw new IllegalArgumentException(
                    "Invalid authenticated user."
            );
        }


        Integer userId =
                userDetails
                        .getUser()
                        .getId();


        User admin =
                userRepo
                        .findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Admin account not found."
                                )
                        );


        if (!"ADMIN".equalsIgnoreCase(
                admin.getRole()
        )) {

            throw new IllegalArgumentException(
                    "Only administrators can access discovered applications."
            );
        }


        return admin;
    }
}