package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AccountLockoutService;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.ClientIpAddressService;
import com.example.TeamLoggerC.service.EmployeeIpAccessService;
import com.example.TeamLoggerC.service.LoginApprovalService;
import com.example.TeamLoggerC.service.LoginHistoryService;
import com.example.TeamLoggerC.service.PermissionService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/employee")
public class EmployeeLoginApprovalController {

    // =====================================================
    // SESSION KEYS
    // =====================================================

    private static final String APPROVAL_TOKEN_SESSION_KEY =
            "LOGIN_APPROVAL_TOKEN";

    private static final String APPROVAL_COMPLETED_SESSION_KEY =
            "LOGIN_APPROVAL_COMPLETED";

    private static final String LOGIN_RECORDED_SESSION_KEY =
            "LOGIN_RECORDED";


    // =====================================================
    // DEPENDENCIES
    // =====================================================

    private final LoginApprovalService loginApprovalService;

    private final UserRepo userRepo;

    private final SecurityContextRepository securityContextRepository;

    private final PermissionService permissionService;

    private final ClientIpAddressService clientIpAddressService;

    private final EmployeeIpAccessService employeeIpAccessService;

    private final AdminSettingsService adminSettingsService;

    private final AccountLockoutService accountLockoutService;

    private final LoginHistoryService loginHistoryService;


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public EmployeeLoginApprovalController(
            LoginApprovalService loginApprovalService,
            UserRepo userRepo,
            SecurityContextRepository securityContextRepository,
            PermissionService permissionService,
            ClientIpAddressService clientIpAddressService,
            EmployeeIpAccessService employeeIpAccessService,
            AdminSettingsService adminSettingsService,
            AccountLockoutService accountLockoutService,
            LoginHistoryService loginHistoryService
    ) {

        this.loginApprovalService =
                loginApprovalService;

        this.userRepo =
                userRepo;

        this.securityContextRepository =
                securityContextRepository;

        this.permissionService =
                permissionService;

        this.clientIpAddressService =
                clientIpAddressService;

        this.employeeIpAccessService =
                employeeIpAccessService;

        this.adminSettingsService =
                adminSettingsService;

        this.accountLockoutService =
                accountLockoutService;

        this.loginHistoryService =
                loginHistoryService;
    }


    // =====================================================
    // LOGIN PENDING PAGE
    // =====================================================

    @GetMapping("/login-pending")
    public String loginPending(
            Authentication authentication,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            Model model
    ) {

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "EMPLOYEE LOGIN PENDING PAGE"
        );

        System.out.println(
                "SESSION ID = " + session.getId()
        );

        System.out.println(
                "=========================================="
        );


        User employee = null;


        // =================================================
        // CHECK EXISTING AUTHENTICATION
        // =================================================

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal()
                instanceof CustomUserDetails userDetails) {

            employee =
                    userDetails.getUser();
        }


        // =================================================
        // CHECK APPROVAL COMPLETED
        // =================================================

        Boolean approvalCompleted =
                (Boolean) session.getAttribute(
                        APPROVAL_COMPLETED_SESSION_KEY
                );


        if (Boolean.TRUE.equals(
                approvalCompleted
        )) {

            return "redirect:/employee/dashboard";
        }


        // =================================================
        // GET APPROVAL TOKEN
        // =================================================

        String approvalToken =
                (String) session.getAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );


        if (approvalToken == null
                || approvalToken.isBlank()) {

            return "redirect:/login";
        }


        // =================================================
        // FIND APPROVAL REQUEST
        // =================================================

        LoginApprovalRequest approvalRequest =
                loginApprovalService.getRequestByToken(
                        approvalToken
                );


        if (approvalRequest == null) {

            clearApprovalSession(session);

            return "redirect:/login?error=approval";
        }


        // =================================================
        // LOAD EMPLOYEE
        // =================================================

        if (employee == null) {

            Integer employeeId =
                    approvalRequest.getEmployeeId();


            if (employeeId == null) {

                clearApprovalSession(session);

                return "redirect:/login?error=approval";
            }


            employee =
                    userRepo.findById(employeeId)
                            .orElse(null);


            if (employee == null) {

                clearApprovalSession(session);

                return "redirect:/login?error=approval";
            }
        }


        // =================================================
        // VERIFY TOKEN BELONGS TO EMPLOYEE
        // =================================================

        if (approvalRequest.getEmployeeId() == null
                || employee.getId() == null
                || !approvalRequest
                .getEmployeeId()
                .equals(employee.getId())) {

            clearApprovalSession(session);

            return "redirect:/login?error=unauthorized";
        }


        // =================================================
        // VERIFY ROLE
        // =================================================

        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole()
        )) {

            clearApprovalSession(session);

            return "redirect:/login?error=unauthorized";
        }


        // =================================================
        // VERIFY ACTIVE
        // =================================================

        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(
                employee.getStatus().trim()
        )) {

            clearApprovalSession(session);

            return "redirect:/login?error=employee-disabled";
        }


        // =================================================
        // GET STATUS
        // =================================================

        String status =
                approvalRequest.getStatus();


        if (status == null
                || status.isBlank()) {

            clearApprovalSession(session);

            return "redirect:/login?error=approval";
        }


        status =
                status.trim().toUpperCase();


        // =================================================
        // APPROVED
        // =================================================

        /*
         * IMPORTANT:
         *
         * APPROVED is checked before expiration.
         */

        if ("APPROVED".equals(status)) {

            String restoreResult =
                    restoreEmployeeAuthentication(
                            approvalRequest,
                            session,
                            request,
                            response
                    );


            if ("APPROVED".equals(
                    restoreResult
            )) {

                return "redirect:/employee/dashboard";
            }


            if ("IP_NOT_APPROVED".equals(
                    restoreResult
            )) {

                return "redirect:/employee/ip-approval-pending";
            }


            return "redirect:/login?error=approval";
        }


        // =================================================
        // REJECTED
        // =================================================

        if ("REJECTED".equals(status)) {

            clearApprovalSession(session);

            return "redirect:/login?error=login-rejected";
        }


        // =================================================
        // PENDING
        // =================================================

        if ("PENDING".equals(status)) {

            LocalDateTime expiresAt =
                    approvalRequest.getTokenExpiresAt();


            if (expiresAt != null
                    && !expiresAt.isAfter(
                    LocalDateTime.now()
            )) {

                clearApprovalSession(session);

                return "redirect:/login?error=approval-expired";
            }


            // =============================================
            // VERIFY CURRENT IP
            // =============================================

            IpCheckResult ipCheckResult =
                    verifyCurrentEmployeeIp(
                            employee,
                            request
                    );


            if (!ipCheckResult.allowed()) {

                /*
                 * Do NOT clear the late-login token here.
                 *
                 * The employee may need to complete the
                 * IP approval process first.
                 */

                return ipCheckResult.redirectUrl();
            }


            // =============================================
            // MODEL
            // =============================================

            model.addAttribute(
                    "employee",
                    employee
            );

            model.addAttribute(
                    "request",
                    approvalRequest
            );

            model.addAttribute(
                    "ipAddress",
                    ipCheckResult.ipAddress()
            );


            return "login-pending";
        }


        // =================================================
        // UNKNOWN STATUS
        // =================================================

        clearApprovalSession(session);

        return "redirect:/login?error=approval";
    }


    // =====================================================
    // AJAX APPROVAL STATUS
    // =====================================================

    @GetMapping("/login-approval-status")
    @ResponseBody
    public String loginApprovalStatus(
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        Boolean approvalCompleted =
                (Boolean) session.getAttribute(
                        APPROVAL_COMPLETED_SESSION_KEY
                );


        if (Boolean.TRUE.equals(
                approvalCompleted
        )) {

            return "APPROVED";
        }


        // =================================================
        // GET TOKEN
        // =================================================

        String approvalToken =
                (String) session.getAttribute(
                        APPROVAL_TOKEN_SESSION_KEY
                );


        if (approvalToken == null
                || approvalToken.isBlank()) {

            return "NONE";
        }


        // =================================================
        // FIND REQUEST
        // =================================================

        LoginApprovalRequest approvalRequest =
                loginApprovalService.getRequestByToken(
                        approvalToken
                );


        if (approvalRequest == null) {

            clearApprovalSession(session);

            return "NONE";
        }


        // =================================================
        // STATUS
        // =================================================

        String status =
                approvalRequest.getStatus();


        if (status == null
                || status.isBlank()) {

            clearApprovalSession(session);

            return "NONE";
        }


        status =
                status.trim().toUpperCase();


        // =================================================
        // APPROVED
        // =================================================

        if ("APPROVED".equals(status)) {

            return restoreEmployeeAuthentication(
                    approvalRequest,
                    session,
                    request,
                    response
            );
        }


        // =================================================
        // REJECTED
        // =================================================

        if ("REJECTED".equals(status)) {

            clearApprovalSession(session);

            return "REJECTED";
        }


        // =================================================
        // PENDING
        // =================================================

        if ("PENDING".equals(status)) {

            LocalDateTime expiresAt =
                    approvalRequest.getTokenExpiresAt();


            if (expiresAt != null
                    && !expiresAt.isAfter(
                    LocalDateTime.now()
            )) {

                clearApprovalSession(session);

                return "EXPIRED";
            }


            Integer employeeId =
                    approvalRequest.getEmployeeId();


            if (employeeId == null) {

                clearApprovalSession(session);

                return "NONE";
            }


            User employee =
                    userRepo.findById(employeeId)
                            .orElse(null);


            if (employee == null) {

                clearApprovalSession(session);

                return "NONE";
            }


            // =============================================
            // ROLE
            // =============================================

            if (!"EMPLOYEE".equalsIgnoreCase(
                    employee.getRole()
            )) {

                clearApprovalSession(session);

                return "NONE";
            }


            // =============================================
            // ACTIVE
            // =============================================

            if (employee.getStatus() == null
                    || !"ACTIVE".equalsIgnoreCase(
                    employee.getStatus().trim()
            )) {

                clearApprovalSession(session);

                return "NONE";
            }


            // =============================================
            // IP
            // =============================================

            IpCheckResult ipCheckResult =
                    verifyCurrentEmployeeIp(
                            employee,
                            request
                    );


            if (!ipCheckResult.allowed()) {

                /*
                 * The late-login request remains intact.
                 *
                 * The frontend will move the employee to
                 * the IP approval page.
                 */

                return "IP_NOT_APPROVED";
            }


            return "PENDING";
        }


        // =================================================
        // UNKNOWN STATUS
        // =================================================

        clearApprovalSession(session);

        return "NONE";
    }


    // =====================================================
    // RESTORE EMPLOYEE AUTHENTICATION
    // =====================================================

    private String restoreEmployeeAuthentication(
            LoginApprovalRequest approvalRequest,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response
    ) {

        Integer employeeId =
                approvalRequest.getEmployeeId();


        if (employeeId == null) {

            clearApprovalSession(session);

            return "NONE";
        }


        // =================================================
        // LOAD EMPLOYEE
        // =================================================

        User employee =
                userRepo.findById(employeeId)
                        .orElse(null);


        if (employee == null) {

            clearApprovalSession(session);

            return "NONE";
        }


        // =================================================
        // ROLE
        // =================================================

        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole()
        )) {

            clearApprovalSession(session);

            return "NONE";
        }


        // =================================================
        // ACTIVE
        // =================================================

        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(
                employee.getStatus().trim()
        )) {

            clearApprovalSession(session);

            return "NONE";
        }


        // =================================================
        // VERIFY CURRENT IP
        // =================================================

        IpCheckResult ipCheckResult =
                verifyCurrentEmployeeIp(
                        employee,
                        request
                );


        if (!ipCheckResult.allowed()) {

            return "IP_NOT_APPROVED";
        }


        // =================================================
        // CREATE USER DETAILS
        // =================================================

        CustomUserDetails employeeDetails =
                new CustomUserDetails(
                        employee,
                        permissionService
                );


        // =================================================
        // CREATE AUTHENTICATION
        // =================================================

        UsernamePasswordAuthenticationToken
                newAuthentication =
                new UsernamePasswordAuthenticationToken(
                        employeeDetails,
                        employeeDetails.getPassword(),
                        employeeDetails.getAuthorities()
                );


        // =================================================
        // CREATE SECURITY CONTEXT
        // =================================================

        SecurityContext securityContext =
                SecurityContextHolder
                        .createEmptyContext();


        securityContext.setAuthentication(
                newAuthentication
        );


        SecurityContextHolder.setContext(
                securityContext
        );


        // =================================================
        // SAVE SECURITY CONTEXT
        // =================================================

        HttpSession httpSession =
                request.getSession(true);


        httpSession.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                securityContext
        );


        securityContextRepository.saveContext(
                securityContext,
                request,
                response
        );


        // =================================================
        // MARK TOKEN USED
        // =================================================

        if (approvalRequest.getTokenUsedAt() == null) {

            approvalRequest.setTokenUsedAt(
                    LocalDateTime.now()
            );

            loginApprovalService.markTokenUsed(
                    approvalRequest
            );
        }


        // =================================================
        // REMOVE APPROVAL TOKEN
        // =================================================

        httpSession.removeAttribute(
                APPROVAL_TOKEN_SESSION_KEY
        );


        // =================================================
        // MARK APPROVAL COMPLETED
        // =================================================

        httpSession.setAttribute(
                APPROVAL_COMPLETED_SESSION_KEY,
                Boolean.TRUE
        );


        // =================================================
        // RECORD SUCCESSFUL LOGIN
        // =================================================

        recordSuccessfulLoginIfNeeded(
                employee,
                request,
                httpSession
        );


        // =================================================
        // SUCCESS LOG
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "LATE LOGIN APPROVAL COMPLETED"
        );

        System.out.println(
                "EMPLOYEE ID = "
                        + employee.getId()
        );

        System.out.println(
                "CURRENT IP = "
                        + ipCheckResult.ipAddress()
        );

        System.out.println(
                "EMPLOYEE AUTHENTICATION RESTORED"
        );

        System.out.println(
                "SUCCESSFUL LOGIN RECORDED"
        );

        System.out.println(
                "=========================================="
        );


        return "APPROVED";
    }


    // =====================================================
    // RECORD SUCCESSFUL LOGIN
    // =====================================================

    private void recordSuccessfulLoginIfNeeded(
            User employee,
            HttpServletRequest request,
            HttpSession session
    ) {

        Boolean alreadyRecorded =
                (Boolean) session.getAttribute(
                        LOGIN_RECORDED_SESSION_KEY
                );


        if (Boolean.TRUE.equals(
                alreadyRecorded
        )) {

            System.out.println(
                    "LOGIN ALREADY RECORDED"
            );

            return;
        }


        try {

            accountLockoutService.recordSuccessfulLogin(
                    employee
            );


            loginHistoryService.recordSuccessfulLogin(
                    employee,
                    request
            );


            session.setAttribute(
                    LOGIN_RECORDED_SESSION_KEY,
                    Boolean.TRUE
            );


            System.out.println(
                    "SUCCESSFUL EMPLOYEE LOGIN RECORDED"
            );

        }
        catch (Exception exception) {

            /*
             * Do not destroy the authenticated session
             * because of a logging/history failure.
             */

            System.out.println(
                    "LOGIN HISTORY RECORDING ERROR = "
                            + exception.getMessage()
            );
        }
    }


    // =====================================================
    // VERIFY CURRENT EMPLOYEE IP
    // =====================================================

    private IpCheckResult verifyCurrentEmployeeIp(
            User employee,
            HttpServletRequest request
    ) {

        // =================================================
        // ADMIN ID
        // =================================================

        Integer adminId =
                employee.getAdminId();


        if (adminId == null) {

            return new IpCheckResult(
                    false,
                    null,
                    "redirect:/login?error=ip-configuration"
            );
        }


        // =================================================
        // CURRENT IP
        // =================================================

        String clientIp =
                clientIpAddressService.getClientIp(
                        request
                );


        if (clientIp == null
                || clientIp.isBlank()) {

            return new IpCheckResult(
                    false,
                    null,
                    "redirect:/login?error=ip-detection"
            );
        }


        clientIp =
                clientIp.trim();


        // =================================================
        // ADMIN SETTINGS
        // =================================================

        AdminSettings settings =
                adminSettingsService.getSettings(
                        adminId
                );


        if (settings == null) {

            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-configuration"
            );
        }


        // =================================================
        // IP RESTRICTION DISABLED
        // =================================================

        if (!settings.isIpRestrictionEnabled()) {

            System.out.println(
                    "LATE LOGIN IP CHECK = "
                            + "RESTRICTION DISABLED"
            );

            return new IpCheckResult(
                    true,
                    clientIp,
                    null
            );
        }


        // =================================================
        // COMPANY IP
        // =================================================

        String companyIp =
                settings.getCompanyIpAddress();


        if (companyIp != null
                && !companyIp.isBlank()) {

            companyIp =
                    companyIp.trim();
        }


        if (companyIp != null
                && companyIp.equalsIgnoreCase(
                clientIp
        )) {

            System.out.println(
                    "LATE LOGIN IP CHECK = "
                            + "COMPANY IP"
            );

            return new IpCheckResult(
                    true,
                    clientIp,
                    null
            );
        }


        // =================================================
        // EMPLOYEE APPROVED IP
        // =================================================

        boolean employeeIpApproved =
                employeeIpAccessService.isIpApproved(
                        employee.getId(),
                        adminId,
                        clientIp
                );


        if (employeeIpApproved) {

            System.out.println(
                    "LATE LOGIN IP CHECK = "
                            + "EMPLOYEE IP APPROVED"
            );

            return new IpCheckResult(
                    true,
                    clientIp,
                    null
            );
        }


        // =================================================
        // NOT APPROVED
        // =================================================

        System.out.println(
                "LATE LOGIN IP CHECK = "
                        + "NOT APPROVED"
        );


        return new IpCheckResult(
                false,
                clientIp,
                "redirect:/employee/ip-approval-pending"
        );
    }


    // =====================================================
    // CLEAR APPROVAL SESSION
    // =====================================================

    private void clearApprovalSession(
            HttpSession session
    ) {

        session.removeAttribute(
                APPROVAL_TOKEN_SESSION_KEY
        );

        session.removeAttribute(
                APPROVAL_COMPLETED_SESSION_KEY
        );
    }


// =====================================================
// IP CHECK RESULT
// =====================================================

    private record IpCheckResult(
            boolean allowed,
            String ipAddress,
            String redirectUrl
    ) {
    }

}