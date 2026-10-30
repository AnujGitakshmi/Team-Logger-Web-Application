package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.DailyWork;
import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.DailyWorkService;
import com.example.TeamLoggerC.service.LoginApprovalService;
import com.example.TeamLoggerC.service.ProjectService;
import com.example.TeamLoggerC.service.SessionService;

import jakarta.servlet.http.HttpSession;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/employee")
public class EmployeeDashboardController {

    /*
     * =====================================================
     * APPROVAL TOKEN SESSION KEY
     * =====================================================
     */

    private static final String APPROVAL_TOKEN_SESSION_KEY =
            "LOGIN_APPROVAL_TOKEN";

    private final SessionService sessionService;
    private final LoginApprovalService loginApprovalService;
    private final ProjectService projectService;
    private final DailyWorkService dailyWorkService;
    private final UserRepo userRepo;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeDashboardController(
            SessionService sessionService,
            LoginApprovalService loginApprovalService,
            ProjectService projectService,
            DailyWorkService dailyWorkService,
            UserRepo userRepo
    ) {
        this.sessionService = sessionService;
        this.loginApprovalService = loginApprovalService;
        this.projectService = projectService;
        this.dailyWorkService = dailyWorkService;
        this.userRepo = userRepo;
    }

    // =====================================================
    // EMPLOYEE DASHBOARD
    // =====================================================

    @PreAuthorize("hasAuthority('PERMISSION_DASHBOARD_VIEW')")
    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            HttpSession httpSession,
            Model model
    ) {

        System.out.println("========================================");
        System.out.println("EMPLOYEE DASHBOARD REQUEST");
        System.out.println("Authentication : " + authentication);
        System.out.println("Authenticated  : " +
                (authentication != null && authentication.isAuthenticated()));
        System.out.println("Authorities    : " +
                (authentication != null
                        ? authentication.getAuthorities()
                        : "NULL"));
        System.out.println("Session ID     : " +
                (httpSession != null
                        ? httpSession.getId()
                        : "NO SESSION"));
        System.out.println("========================================");

        // =================================================
        // AUTHENTICATION
        // =================================================

        if (authentication == null
                || !authentication.isAuthenticated()) {

            return "redirect:/login";
        }

        if (!(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user =
                userDetails.getUser();

        Integer userId =
                user.getId();

        // =================================================
        // COMPANY / ADMIN
        // =================================================

        User admin = null;

        if (user.getAdminId() != null) {

            admin = userRepo.findById(
                    user.getAdminId()
            ).orElse(null);
        }

        // =================================================
        // PROJECTS
        // =================================================

        List<Project> projects =
                projectService.getProjectsForEmployee(userId);

        Project currentProject =
                projects.stream()
                        .filter(p -> "ASSIGNED".equalsIgnoreCase(p.getStatus()))
                        .findFirst()
                        .orElse(
                                projects.isEmpty()
                                        ? null
                                        : projects.get(0)
                        );

        // =================================================
        // TODAY'S WORK
        // =================================================

        List<DailyWork> todayWork =
                dailyWorkService.getOwnTodayWork(userId);

        // =================================================
        // CHECK CURRENT APPROVAL TOKEN
        // =================================================

        String approvalToken =
                (String) httpSession.getAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );

        if (approvalToken != null
                && !approvalToken.isBlank()) {

            LoginApprovalRequest request =
                    loginApprovalService.getRequestByToken(
                            approvalToken
                    );

            if (request == null) {

                httpSession.removeAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );
            }

            else if (!userId.equals(
                    request.getEmployeeId()
            )) {

                httpSession.removeAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );

                return "redirect:/login?error=unauthorized";
            }

            else if ("PENDING".equalsIgnoreCase(
                    request.getStatus()
            )) {

                return "redirect:/employee/login-pending";
            }

            else if ("REJECTED".equalsIgnoreCase(
                    request.getStatus()
            )) {

                httpSession.removeAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );

                return "redirect:/login?error=login-rejected";
            }

            else if ("APPROVED".equalsIgnoreCase(
                    request.getStatus()
            )) {

                httpSession.removeAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );
            }
        }

        // =================================================
        // CURRENT SESSION
        // =================================================

        Session currentSession =
                sessionService.getCurrentSession(userId);

        // =================================================
        // OFFICE HOURS
        // =================================================

        boolean officeHours =
                sessionService.isOfficeHours();

        // =================================================
        // ACTIVE / IDLE
        // =================================================

        int activeMinutes =
                sessionService.getTodayActiveMinutes(userId);

        int idleMinutes =
                sessionService.getTodayIdleMinutes(userId);

        int activeHours =
                sessionService.getActiveHours(userId);

        int activeRemainingMinutes =
                sessionService.getActiveRemainingMinutes(userId);

        int idleHours =
                sessionService.getIdleHours(userId);

        int idleRemainingMinutes =
                sessionService.getIdleRemainingMinutes(userId);

        // =================================================
        // ATTENDANCE
        // =================================================

        var loginTime =
                sessionService.getTodayLoginTime(userId);

        var logoutTime =
                sessionService.getTodayLogoutTime(userId);

        // =================================================
        // MODEL
        // =================================================

        model.addAttribute("user", user);

        // Company/Admin information
        model.addAttribute("admin", admin);

        model.addAttribute("currentSession", currentSession);
        model.addAttribute("officeHours", officeHours);

        model.addAttribute("activeMinutes", activeMinutes);
        model.addAttribute("idleMinutes", idleMinutes);

        model.addAttribute("activeHours", activeHours);
        model.addAttribute("activeRemainingMinutes", activeRemainingMinutes);

        model.addAttribute("idleHours", idleHours);
        model.addAttribute("idleRemainingMinutes", idleRemainingMinutes);

        model.addAttribute("loginTime", loginTime);
        model.addAttribute("logoutTime", logoutTime);

        // =================================================
        // DASHBOARD PROJECT DATA
        // =================================================

        model.addAttribute("projects", projects);
        model.addAttribute("currentProject", currentProject);
        model.addAttribute("todayWork", todayWork);

        return "dashboardEmployee";
    }
}