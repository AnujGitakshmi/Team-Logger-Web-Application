package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.LoginApprovalViewDTO;
import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.AdminSettingsRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.LoginApprovalService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.ResponseEntity;

import java.util.Map;

@Controller
@RequestMapping("/admin/login-approvals")
public class AdminLoginApprovalController {

    private final LoginApprovalService loginApprovalService;
    private final AdminSettingsRepo adminSettingsRepo;
    private final UserRepo userRepo;

    /*
     * Normal office login time.
     */
    private static final LocalTime SCHEDULED_LOGIN_TIME =
            LocalTime.of(10, 0);

    public AdminLoginApprovalController(
            LoginApprovalService loginApprovalService,
            AdminSettingsRepo adminSettingsRepo,
            UserRepo userRepo
    ) {
        this.loginApprovalService = loginApprovalService;
        this.adminSettingsRepo = adminSettingsRepo;
        this.userRepo = userRepo;
    }


    // =========================================================
    // LOGIN APPROVAL PAGE
    // =========================================================

    @GetMapping
    public String loginApprovals(
            Authentication authentication,
            Model model
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // =====================================================
        // GET PENDING REQUESTS
        // =====================================================

        List<LoginApprovalRequest> requests =
                loginApprovalService
                        .getPendingRequests(adminId);


        // =====================================================
        // BUILD VIEW DATA
        // =====================================================

        List<LoginApprovalViewDTO> approvalRows =
                new ArrayList<>();

        for (LoginApprovalRequest request : requests) {

            User employee =
                    userRepo.findById(
                            request.getEmployeeId()
                    ).orElse(null);

            /*
             * If employee was deleted after creating
             * the request, don't break the page.
             */
            if (employee == null) {
                continue;
            }

            long delayMinutes =
                    calculateDelayMinutes(
                            request.getLoginTime()
                    );

            approvalRows.add(
                    new LoginApprovalViewDTO(
                            request,
                            employee,
                            delayMinutes
                    )
            );
        }


        // =====================================================
        // ADMIN SETTINGS
        // =====================================================

        AdminSettings adminSettings =
                adminSettingsRepo
                        .findByAdminId(adminId)
                        .orElseGet(() -> {

                            AdminSettings settings =
                                    new AdminSettings();

                            settings.setAdminId(adminId);
                            settings.setCompanyName(
                                    "My Company"
                            );

                            return settings;
                        });


        // =====================================================
        // SEND DATA TO THYMELEAF
        // =====================================================

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "adminSettings",
                adminSettings
        );

        model.addAttribute(
                "requests",
                approvalRows
        );

        model.addAttribute(
                "pendingApprovalCount",
                approvalRows.size()
        );

        return "loginApprovals";
    }


    // =========================================================
    // CALCULATE LOGIN DELAY
    // =========================================================

    private long calculateDelayMinutes(
            LocalDateTime loginTime
    ) {

        if (loginTime == null) {
            return 0;
        }

        LocalDateTime scheduledTime =
                LocalDateTime.of(
                        loginTime.toLocalDate(),
                        SCHEDULED_LOGIN_TIME
                );

        if (!loginTime.isAfter(scheduledTime)) {
            return 0;
        }

        return Duration
                .between(
                        scheduledTime,
                        loginTime
                )
                .toMinutes();
    }


    // =========================================================
    // APPROVE LOGIN
    // =========================================================

    @PostMapping("/approve/{id}")
    public String approveLogin(
            @PathVariable Integer id,
            Authentication authentication
    ) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("ADMIN LOGIN APPROVAL - APPROVE CLICK");
        System.out.println("==========================================");
        System.out.println(
                "APPROVAL REQUEST ID = " + id
        );


        // =====================================================
        // VALIDATE ADMIN AUTHENTICATION
        // =====================================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            System.out.println(
                    "APPROVAL FAILED - INVALID ADMIN AUTHENTICATION"
            );

            return "redirect:/login";
        }


        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        System.out.println(
                "PROCESSING ADMIN ID = " + adminId
        );


        // =====================================================
        // APPROVE REQUEST
        // =====================================================

        LoginApprovalRequest approvedRequest =
                loginApprovalService.approve(
                        id,
                        adminId
                );


        // =====================================================
        // VERIFY RESULT
        // =====================================================

        if (approvedRequest != null) {

            System.out.println(
                    "APPROVAL RESULT REQUEST ID = "
                            + approvedRequest.getId()
            );

            System.out.println(
                    "APPROVAL RESULT EMPLOYEE ID = "
                            + approvedRequest.getEmployeeId()
            );

            System.out.println(
                    "APPROVAL RESULT STATUS = "
                            + approvedRequest.getStatus()
            );

            System.out.println(
                    "APPROVAL RESULT PROCESSED AT = "
                            + approvedRequest.getProcessedAt()
            );

        } else {

            System.out.println(
                    "WARNING: APPROVAL SERVICE RETURNED NULL"
            );
        }

        System.out.println(
                "=========================================="
        );
        System.out.println();

        return "redirect:/admin/login-approvals";
    }


    // =========================================================
    // REJECT LOGIN
    // =========================================================

    @PostMapping("/reject/{id}")
    public String rejectLogin(
            @PathVariable Integer id,
            Authentication authentication
    ) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("ADMIN LOGIN APPROVAL - REJECT CLICK");
        System.out.println("==========================================");
        System.out.println(
                "REJECTION REQUEST ID = " + id
        );


        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            System.out.println(
                    "REJECTION FAILED - INVALID ADMIN AUTHENTICATION"
            );

            return "redirect:/login";
        }


        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        System.out.println(
                "PROCESSING ADMIN ID = " + adminId
        );


        LoginApprovalRequest rejectedRequest =
                loginApprovalService.reject(
                        id,
                        adminId
                );


        if (rejectedRequest != null) {

            System.out.println(
                    "REJECTION RESULT REQUEST ID = "
                            + rejectedRequest.getId()
            );

            System.out.println(
                    "REJECTION RESULT EMPLOYEE ID = "
                            + rejectedRequest.getEmployeeId()
            );

            System.out.println(
                    "REJECTION RESULT STATUS = "
                            + rejectedRequest.getStatus()
            );

            System.out.println(
                    "REJECTION RESULT PROCESSED AT = "
                            + rejectedRequest.getProcessedAt()
            );

        } else {

            System.out.println(
                    "WARNING: REJECTION SERVICE RETURNED NULL"
            );
        }

        System.out.println(
                "=========================================="
        );
        System.out.println();

        return "redirect:/admin/login-approvals";
    }
    // =========================================================
// REFRESH LOGIN APPROVAL DATA
// =========================================================

    @GetMapping("/data")
    @ResponseBody
    public ResponseEntity<?> getLoginApprovalData(
            Authentication authentication
    ) {

        System.out.println();
        System.out.println("==========================================");
        System.out.println("ADMIN LOGIN APPROVAL - REFRESH DATA");
        System.out.println("==========================================");

        // =====================================================
        // VALIDATE ADMIN AUTHENTICATION
        // =====================================================

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails)) {

            System.out.println(
                    "REFRESH FAILED - INVALID ADMIN AUTHENTICATION"
            );

            return ResponseEntity.status(401).build();
        }

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        System.out.println(
                "REFRESH ADMIN ID = " + adminId
        );


        // =====================================================
        // GET PENDING REQUESTS
        // =====================================================

        List<LoginApprovalRequest> requests =
                loginApprovalService
                        .getPendingRequests(adminId);


        // =====================================================
        // BUILD RESPONSE
        // =====================================================

        List<Map<String, Object>> rows =
                new ArrayList<>();

        for (LoginApprovalRequest request : requests) {

            User employee =
                    userRepo.findById(
                            request.getEmployeeId()
                    ).orElse(null);

            if (employee == null) {
                continue;
            }

            long delayMinutes =
                    calculateDelayMinutes(
                            request.getLoginTime()
                    );

            Map<String, Object> row =
                    new java.util.HashMap<>();

            row.put("id", request.getId());
            row.put("employeeId", employee.getId());
            row.put("employeeName", employee.getName());
            row.put("email", employee.getEmail());
            row.put("loginTime", request.getLoginTime());
            row.put("status", request.getStatus());
            row.put("delayMinutes", delayMinutes);

            rows.add(row);
        }


        // =====================================================
        // RESPONSE
        // =====================================================

        Map<String, Object> response =
                new java.util.HashMap<>();

        response.put(
                "requests",
                rows
        );

        response.put(
                "pendingApprovalCount",
                rows.size()
        );

        System.out.println(
                "PENDING REQUEST COUNT = "
                        + rows.size()
        );

        System.out.println(
                "=========================================="
        );
        System.out.println();

        return ResponseEntity.ok(response);
    }


}