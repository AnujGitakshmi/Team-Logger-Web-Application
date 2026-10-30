package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AccessPolicy;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AccessPolicyService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/access-policies")
public class AccessPolicyController {

    private final AccessPolicyService accessPolicyService;
    private final UserRepo userRepo;

    public AccessPolicyController(
            AccessPolicyService accessPolicyService,
            UserRepo userRepo
    ) {
        this.accessPolicyService = accessPolicyService;
        this.userRepo = userRepo;
    }


    // =========================================================
    // POLICY LIST
    // =========================================================

    @GetMapping
    public String policies(
            Authentication authentication,
            Model model
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        List<AccessPolicy> policies =
                accessPolicyService.getPoliciesForAdmin(admin.getId());

        model.addAttribute("admin", admin);
        model.addAttribute("policies", policies);
        model.addAttribute(
                "activePolicyCount",
                accessPolicyService.countActivePolicies(admin.getId())
        );
        model.addAttribute(
                "totalPolicyCount",
                accessPolicyService.countPolicies(admin.getId())
        );

        return "access-policies";
    }


    // =========================================================
    // CREATE POLICY PAGE
    // =========================================================

    @GetMapping("/create")
    public String createPolicyPage(
            Authentication authentication,
            Model model
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        model.addAttribute("admin", admin);
        model.addAttribute(
                "accessPolicy",
                new AccessPolicy()
        );

        return "access-policy-form";
    }


    // =========================================================
    // CREATE POLICY
    // =========================================================

    @PostMapping("/create")
    public String createPolicy(
            Authentication authentication,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin = getAuthenticatedAdmin(authentication);

            accessPolicyService.createPolicy(
                    admin.getId(),
                    name,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access policy created successfully."
            );

            return "redirect:/admin/access-policies";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/access-policies/create";
        }
    }


    // =========================================================
    // EDIT POLICY PAGE
    // =========================================================

    @GetMapping("/edit/{id}")
    public String editPolicyPage(
            @PathVariable("id") Long policyId,
            Authentication authentication,
            Model model
    ) {

        User admin = getAuthenticatedAdmin(authentication);

        AccessPolicy policy =
                accessPolicyService.getPolicyForAdmin(
                        admin.getId(),
                        policyId
                );

        model.addAttribute("admin", admin);
        model.addAttribute(
                "accessPolicy",
                policy
        );

        return "admin/access-policy-form";
    }


    // =========================================================
    // UPDATE POLICY
    // =========================================================

    @PostMapping("/edit/{id}")
    public String updatePolicy(
            @PathVariable("id") Long policyId,
            Authentication authentication,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin = getAuthenticatedAdmin(authentication);

            accessPolicyService.updatePolicy(
                    admin.getId(),
                    policyId,
                    name,
                    description
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access policy updated successfully."
            );

            return "redirect:/admin/access-policies";

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );

            return "redirect:/admin/access-policies/edit/"
                    + policyId;
        }
    }


    // =========================================================
    // ACTIVATE POLICY
    // =========================================================

    @PostMapping("/activate/{id}")
    public String activatePolicy(
            @PathVariable("id") Long policyId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin = getAuthenticatedAdmin(authentication);

            accessPolicyService.activatePolicy(
                    admin.getId(),
                    policyId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access policy activated."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/access-policies";
    }


    // =========================================================
    // DEACTIVATE POLICY
    // =========================================================

    @PostMapping("/deactivate/{id}")
    public String deactivatePolicy(
            @PathVariable("id") Long policyId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin = getAuthenticatedAdmin(authentication);

            accessPolicyService.deactivatePolicy(
                    admin.getId(),
                    policyId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access policy deactivated."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/access-policies";
    }


    // =========================================================
    // DELETE POLICY
    // =========================================================

    @PostMapping("/delete/{id}")
    public String deletePolicy(
            @PathVariable("id") Long policyId,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {

        try {

            User admin = getAuthenticatedAdmin(authentication);

            accessPolicyService.deletePolicy(
                    admin.getId(),
                    policyId
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Access policy deleted successfully."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin/access-policies";
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
                userDetails.getUser().getId();

        User admin = userRepo.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Admin account not found."
                        )
                );

        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {

            throw new IllegalArgumentException(
                    "Only administrators can manage access policies."
            );
        }

        return admin;
    }
}