 package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.DiscoveredWebsite;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AccessPolicyService;
import com.example.TeamLoggerC.service.DiscoveredWebsiteService;
import com.example.TeamLoggerC.service.WebsitePermissionService;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping(
        "/admin/access-policies/{policyId}/websites"
)
public class WebsitePermissionController {


    // =========================================================
    // SERVICES / REPOSITORY
    // =========================================================

    private final WebsitePermissionService websitePermissionService;

    private final AccessPolicyService accessPolicyService;

    private final UserRepo userRepo;

    private final DiscoveredWebsiteService discoveredWebsiteService;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public WebsitePermissionController(
            WebsitePermissionService websitePermissionService,
            AccessPolicyService accessPolicyService,
            UserRepo userRepo,
            DiscoveredWebsiteService discoveredWebsiteService
    ) {

        this.websitePermissionService =
                websitePermissionService;

        this.accessPolicyService =
                accessPolicyService;

        this.userRepo =
                userRepo;

        this.discoveredWebsiteService =
                discoveredWebsiteService;
    }


    // =========================================================
    // WEBSITE PERMISSION PAGE
    //
    // The main permission page is now the unified
    // Application & Website Permissions page.
    // =========================================================

    @GetMapping
    public String showWebsitePermissions(
            @PathVariable Long policyId,
            Authentication authentication
    ) {

        User admin =
                getAuthenticatedAdmin(
                        authentication
                );


        // =====================================================
        // VERIFY POLICY OWNERSHIP
        // =====================================================

        accessPolicyService
                .getPolicyForAdmin(
                        admin.getId(),
                        policyId
                );


        // =====================================================
        // REDIRECT TO UNIFIED PAGE
        // =====================================================

        return "redirect:/admin/access-policies/"
                + policyId
                + "/applications";
    }


    // =========================================================
    // CREATE PAGE
    //
    // This page remains separate because it is a dedicated
    // website permission creation form.
    // =========================================================

    @GetMapping("/create")
    public String showCreateForm(
            @PathVariable Long policyId,
            Authentication authentication,
            Model model
    ) {

        User admin =
                getAuthenticatedAdmin(
                        authentication
                );


        // =====================================================
        // VERIFY POLICY OWNERSHIP
        // =====================================================

        var policy =
                accessPolicyService
                        .getPolicyForAdmin(
                                admin.getId(),
                                policyId
                        );


        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "policy",
                policy
        );


        return "website-permission-form";
    }


    // =========================================================
    // CREATE WEBSITE PERMISSION
    // =========================================================

    @PostMapping("/create")
    public String createPermission(
            @PathVariable Long policyId,
            @RequestParam String domain,
            @RequestParam(defaultValue = "ALLOW")
            String permission,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // CREATE PERMISSION
            // =================================================

            websitePermissionService
                    .createPermission(
                            policyId,
                            domain,
                            permission
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission created successfully."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        // =====================================================
        // RETURN TO UNIFIED PAGE
        // =====================================================

        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // EDIT PAGE
    // =========================================================

    @GetMapping("/edit/{permissionId}")
    public String showEditForm(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            Authentication authentication,
            Model model
    ) {

        User admin =
                getAuthenticatedAdmin(
                        authentication
                );


        // =====================================================
        // VERIFY POLICY OWNERSHIP
        // =====================================================

        var policy =
                accessPolicyService
                        .getPolicyForAdmin(
                                admin.getId(),
                                policyId
                        );


        // =====================================================
        // LOAD WEBSITE PERMISSION
        // =====================================================

        var permission =
                websitePermissionService
                        .getPermissionForPolicy(
                                policyId,
                                permissionId
                        );


        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "policy",
                policy
        );

        model.addAttribute(
                "websitePermission",
                permission
        );


        return "website-permission-form";
    }


    // =========================================================
    // UPDATE WEBSITE PERMISSION
    // =========================================================

    @PostMapping("/edit/{permissionId}")
    public String updatePermission(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            @RequestParam String domain,
            @RequestParam String permission,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // UPDATE
            // =================================================

            websitePermissionService
                    .updatePermission(
                            policyId,
                            permissionId,
                            domain,
                            permission
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission updated successfully."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // CHANGE PERMISSION
    // =========================================================

    @PostMapping("/permission/{permissionId}")
    public String changePermission(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            @RequestParam String permission,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // CHANGE PERMISSION
            // =================================================

            websitePermissionService
                    .changePermission(
                            policyId,
                            permissionId,
                            permission
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission changed to "
                                    + permission.toUpperCase()
                                    + "."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // ACTIVATE
    // =========================================================

    @PostMapping("/activate/{permissionId}")
    public String activatePermission(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // ACTIVATE
            // =================================================

            websitePermissionService
                    .activatePermission(
                            policyId,
                            permissionId
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission activated."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    @PostMapping("/deactivate/{permissionId}")
    public String deactivatePermission(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // DEACTIVATE
            // =================================================

            websitePermissionService
                    .deactivatePermission(
                            policyId,
                            permissionId
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission deactivated."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    @PostMapping("/delete/{permissionId}")
    public String deletePermission(
            @PathVariable Long policyId,
            @PathVariable Long permissionId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // DELETE
            // =================================================

            websitePermissionService
                    .deletePermission(
                            policyId,
                            permissionId
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            "Website permission deleted."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // ADD DISCOVERED WEBSITE AS PERMISSION
    // =========================================================

    @PostMapping(
            "/discovered/{discoveredId}/permission"
    )
    public String addDiscoveredWebsitePermission(
            @PathVariable Long policyId,
            @PathVariable Long discoveredId,
            Authentication authentication,
            @RequestParam(defaultValue = "ALLOW")
            String permission,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin =
                    getAuthenticatedAdmin(
                            authentication
                    );


            // =================================================
            // VERIFY POLICY OWNERSHIP
            // =================================================

            accessPolicyService
                    .getPolicyForAdmin(
                            admin.getId(),
                            policyId
                    );


            // =================================================
            // VERIFY DISCOVERED WEBSITE OWNERSHIP
            // =================================================

            DiscoveredWebsite discoveredWebsite =
                    discoveredWebsiteService
                            .getWebsiteForAdmin(
                                    admin.getId(),
                                    discoveredId
                            );


            // =================================================
            // CREATE WEBSITE PERMISSION
            // =================================================

            websitePermissionService
                    .createPermission(
                            policyId,
                            discoveredWebsite.getDomain(),
                            permission
                    );


            redirectAttributes
                    .addFlashAttribute(
                            "successMessage",
                            discoveredWebsite.getDomain()
                                    + " permission added as "
                                    + permission.toUpperCase()
                                    + "."
                    );

        } catch (IllegalArgumentException e) {

            redirectAttributes
                    .addFlashAttribute(
                            "errorMessage",
                            e.getMessage()
                    );
        }


        // =====================================================
        // RETURN TO UNIFIED PAGE
        // =====================================================

        return redirectToUnifiedPermissions(
                policyId
        );
    }


    // =========================================================
    // UNIFIED PERMISSION PAGE REDIRECT
    // =========================================================

    private String redirectToUnifiedPermissions(
            Long policyId
    ) {

        return "redirect:/admin/access-policies/"
                + policyId
                + "/applications";
    }


    // =========================================================
    // AUTHENTICATED ADMIN
    // =========================================================

    private User getAuthenticatedAdmin(
            Authentication authentication
    ) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalArgumentException(
                    "Authentication is required."
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof CustomUserDetails)) {

            throw new IllegalArgumentException(
                    "Invalid authentication."
            );
        }


        CustomUserDetails userDetails =
                (CustomUserDetails) principal;


        User admin =
                userRepo
                        .findById(
                                userDetails
                                        .getUser()
                                        .getId()
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Admin account not found."
                                )
                        );


        if (!"ADMIN".equalsIgnoreCase(
                admin.getRole()
        )) {

            throw new IllegalArgumentException(
                    "Admin access required."
            );
        }


        return admin;
    }
}
