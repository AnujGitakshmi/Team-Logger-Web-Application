
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.LoginHistoryService;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/super-admin")
public class SuperAdminDashboardController {

    private final UserRepo userRepo;
    private final LoginHistoryService loginHistoryService;

    public SuperAdminDashboardController(
            UserRepo userRepo,
            LoginHistoryService loginHistoryService) {

        this.userRepo = userRepo;
        this.loginHistoryService = loginHistoryService;
    }

    @GetMapping("/dashboard")
    public String showDashboard(Model model) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            return "redirect:/login";
        }

        User admin = userRepo.findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Logged-in user not found"));

        // Only active Super Admin accounts can access this dashboard.
        if (!"SUPER_ADMIN".equalsIgnoreCase(admin.getRole())
                || !"SUPER_ADMIN".equalsIgnoreCase(admin.getOrganizationRole())
                || admin.getOrganizationId() == null
                || !"ACTIVE".equalsIgnoreCase(admin.getStatus())) {

            return "redirect:/login?error=access-denied";
        }

        Integer organizationId = admin.getOrganizationId();

        // Retrieve organization-level Admin accounts only.
        List<User> adminAccounts = new ArrayList<>();

        adminAccounts.addAll(
                userRepo
                        .findByOrganizationIdAndOrganizationRoleIgnoreCaseOrderByNameAsc(
                                organizationId, "ORG_ADMIN")
        );

        adminAccounts.addAll(
                userRepo
                        .findByOrganizationIdAndOrganizationRoleIgnoreCaseOrderByNameAsc(
                                organizationId, "BRANCH_ADMIN")
        );

        // Exclude the current Super Admin and unexpected account types.
        adminAccounts.removeIf(account ->
                Objects.equals(account.getId(), admin.getId())
                        || !"ADMIN".equalsIgnoreCase(account.getRole())
                        || !("ORG_ADMIN".equalsIgnoreCase(
                        account.getOrganizationRole())
                        || "BRANCH_ADMIN".equalsIgnoreCase(
                        account.getOrganizationRole()))
        );

        // Sort account names safely, including null names.
        adminAccounts.sort(
                Comparator.comparing(
                        User::getName,
                        Comparator.nullsLast(
                                String.CASE_INSENSITIVE_ORDER)
                )
        );

        long activeAdminCount = adminAccounts.stream()
                .filter(account ->
                        "ACTIVE".equalsIgnoreCase(account.getStatus()))
                .count();

        long branchAdminCount = adminAccounts.stream()
                .filter(account ->
                        "BRANCH_ADMIN".equalsIgnoreCase(
                                account.getOrganizationRole()))
                .count();

        long organizationAdminCount = adminAccounts.stream()
                .filter(account ->
                        "ORG_ADMIN".equalsIgnoreCase(
                                account.getOrganizationRole()))
                .count();

        // Dashboard identity and organization.
        model.addAttribute("admin", admin);
        model.addAttribute("organizationId", organizationId);

        // Admin-only metrics; do not include employee metrics.
        model.addAttribute("totalAdminCount", adminAccounts.size());
        model.addAttribute("activeAdminCount", activeAdminCount);
        model.addAttribute("branchAdminCount", branchAdminCount);
        model.addAttribute(
                "organizationAdminCount",
                organizationAdminCount
        );

        model.addAttribute("adminAccounts", adminAccounts);

        // Recent login/logout activity for Admin accounts
        // belonging to this organization only.
        model.addAttribute(
                "recentAdminActivity",
                loginHistoryService.getAdminActivityByOrganization(
                        organizationId)
        );

        return "super-admin-dashboard";
    }
}
