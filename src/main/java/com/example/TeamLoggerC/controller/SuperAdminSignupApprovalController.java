
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.BranchAdminSignupApprovalService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/super-admin/signup-requests")
public class SuperAdminSignupApprovalController {

    private final BranchAdminSignupApprovalService approvalService;
    private final UserRepo userRepo;

    public SuperAdminSignupApprovalController(
            BranchAdminSignupApprovalService approvalService,
            UserRepo userRepo) {

        this.approvalService = approvalService;
        this.userRepo = userRepo;
    }

    @GetMapping
    public String showPendingRequests(Model model) {

        User superAdmin = getAuthenticatedSuperAdmin();

        model.addAttribute(
                "requests",
                approvalService.getPendingRequests(superAdmin.getId())
        );

        return "signup-requests";
    }

    @PostMapping("/{requestId}/approve")
    public String approveRequest(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String decisionNote,
            RedirectAttributes redirectAttributes) {

        try {
            User superAdmin = getAuthenticatedSuperAdmin();

            approvalService.approveRequest(
                    requestId,
                    superAdmin.getId(),
                    decisionNote
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Signup request approved. The branch and branch-admin account were created."
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(exception)
            );
        }

        return "redirect:/super-admin/signup-requests";
    }

    @PostMapping("/{requestId}/reject")
    public String rejectRequest(
            @PathVariable Integer requestId,
            @RequestParam(required = false) String decisionNote,
            RedirectAttributes redirectAttributes) {

        try {
            User superAdmin = getAuthenticatedSuperAdmin();

            approvalService.rejectRequest(
                    requestId,
                    superAdmin.getId(),
                    decisionNote
            );

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Signup request rejected."
            );

        } catch (RuntimeException exception) {

            redirectAttributes.addFlashAttribute(
                    "error",
                    safeMessage(exception)
            );
        }

        return "redirect:/super-admin/signup-requests";
    }

    private User getAuthenticatedSuperAdmin() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new SecurityException(
                    "Please log in as Super Admin."
            );
        }

        String email = authentication.getName();

        User user = userRepo.findByEmail(email)
                .orElseThrow(() ->
                        new SecurityException(
                                "Authenticated user account was not found."
                        ));

        // Validate the actual Super Admin account structure.
        if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole())
                || !"SUPER_ADMIN".equalsIgnoreCase(
                user.getOrganizationRole())
                || user.getOrganizationId() == null
                || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {

            throw new SecurityException(
                    "Only an active Super Admin can access signup approvals."
            );
        }

        // Verify the authenticated user also has the required authority.
        boolean hasSuperAdminAuthority =
                authentication.getAuthorities().stream()
                        .anyMatch(authority ->
                                "ROLE_SUPER_ADMIN".equals(
                                        authority.getAuthority()
                                ));

        if (!hasSuperAdminAuthority) {
            throw new SecurityException(
                    "Super Admin authority is missing."
            );
        }

        return user;
    }

    private String safeMessage(RuntimeException exception) {

        if (exception instanceof SecurityException) {
            return "You are not authorized to process this request.";
        }

        if (exception instanceof IllegalArgumentException
                || exception instanceof IllegalStateException) {
            return exception.getMessage();
        }

        return "The request could not be processed. Please try again.";
    }
}
