package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.EmployeeIpApprovalRequest;
import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AccountLockoutService;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.ClientIpAddressService;
import com.example.TeamLoggerC.service.EmployeeIpAccessService;
import com.example.TeamLoggerC.service.EmployeeIpApprovalService;
import com.example.TeamLoggerC.service.LoginApprovalService;
import com.example.TeamLoggerC.service.LoginHistoryService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/employee")
public class EmployeeIpApprovalController {

    private static final LocalTime LATE_LOGIN_TIME =
            LocalTime.of(10, 5);

    private static final String LOGIN_APPROVAL_TOKEN =
            "LOGIN_APPROVAL_TOKEN";

    private static final String LOGIN_APPROVAL_COMPLETED =
            "LOGIN_APPROVAL_COMPLETED";

    private static final String LOGIN_RECORDED =
            "LOGIN_RECORDED";

    private final EmployeeIpApprovalService employeeIpApprovalService;
    private final EmployeeIpAccessService employeeIpAccessService;
    private final LoginApprovalService loginApprovalService;
    private final ClientIpAddressService clientIpAddressService;
    private final AdminSettingsService adminSettingsService;
    private final AccountLockoutService accountLockoutService;
    private final LoginHistoryService loginHistoryService;

    public EmployeeIpApprovalController(
            EmployeeIpApprovalService employeeIpApprovalService,
            EmployeeIpAccessService employeeIpAccessService,
            LoginApprovalService loginApprovalService,
            ClientIpAddressService clientIpAddressService,
            AdminSettingsService adminSettingsService,
            AccountLockoutService accountLockoutService,
            LoginHistoryService loginHistoryService
    ) {
        this.employeeIpApprovalService = employeeIpApprovalService;
        this.employeeIpAccessService = employeeIpAccessService;
        this.loginApprovalService = loginApprovalService;
        this.clientIpAddressService = clientIpAddressService;
        this.adminSettingsService = adminSettingsService;
        this.accountLockoutService = accountLockoutService;
        this.loginHistoryService = loginHistoryService;
    }

    // =========================================================
    // IP APPROVAL PENDING PAGE
    // =========================================================

    @GetMapping("/ip-approval-pending")
    public String ipApprovalPending(
            Authentication authentication,
            HttpServletRequest request,
            Model model
    ) {
        User employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        String ipAddress =
                clientIpAddressService.getClientIp(request);

        if (ipAddress == null || ipAddress.isBlank()) {
            return "redirect:/login?error=ip-detection";
        }

        ipAddress = ipAddress.trim();

        // Check whether this IP already has an approval request.
        String status = employeeIpApprovalService.getRequestStatus(
                employee.getId(),
                employee.getAdminId(),
                ipAddress
        );

        // If approved, continue through the normal login checks.
        if ("APPROVED".equalsIgnoreCase(status)
                && employeeIpAccessService.isIpApproved(
                employee.getId(),
                employee.getAdminId(),
                ipAddress
        )) {
            return "redirect:/employee/continue-login";
        }

        // Do not allow this page to create an approval implicitly
        // when the administrator has disabled unknown-IP approval.
        AdminSettings settings =
                adminSettingsService.getSettings(employee.getAdminId());

        if (settings == null) {
            return "redirect:/login?error=ip-configuration";
        }

        if (!settings.isIpRestrictionEnabled()) {
            return "redirect:/employee/continue-login";
        }

        String companyIp = settings.getCompanyIpAddress();

        if (companyIp != null
                && companyIp.trim().equalsIgnoreCase(ipAddress)) {
            return "redirect:/employee/continue-login";
        }

        if (!settings.isUnknownIpRequiresApproval()) {
            return "redirect:/login?error=ip-approval-disabled";
        }

        // Make sure a pending request exists for this unknown IP.
        // This does NOT approve the IP.
        try {
            employeeIpApprovalService.createOrGetRequest(
                    employee.getId(),
                    employee.getAdminId(),
                    ipAddress
            );
        } catch (IllegalStateException ex) {
            // The IP may have been approved between the earlier
            // status check and this request creation attempt.
            if (employeeIpAccessService.isIpApproved(
                    employee.getId(),
                    employee.getAdminId(),
                    ipAddress
            )) {
                return "redirect:/employee/continue-login";
            }

            return "redirect:/login?error=ip-approval";
        } catch (IllegalArgumentException ex) {
            return "redirect:/login?error=ip-approval";
        }

        status = employeeIpApprovalService.getRequestStatus(
                employee.getId(),
                employee.getAdminId(),
                ipAddress
        );

        model.addAttribute("employee", employee);
        model.addAttribute("ipAddress", ipAddress);
        model.addAttribute(
                "approvalStatus",
                status != null ? status : "PENDING"
        );

        return "ip-approval-pending";
    }

    // =========================================================
    // CONTINUE LOGIN AFTER IP APPROVAL
    // =========================================================

    @GetMapping("/continue-login")
    public String continueLogin(
            Authentication authentication,
            HttpServletRequest request,
            HttpSession session
    ) {
        System.out.println("========================================");
        System.out.println("EMPLOYEE CONTINUE LOGIN");
        System.out.println("========================================");

        User employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            return "redirect:/login";
        }

        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(
                employee.getStatus().trim()
        )) {
            return "redirect:/login?error=employee-disabled";
        }

        if (employee.getAdminId() == null) {
            return "redirect:/login?error=admin-not-found";
        }

        String ipAddress =
                clientIpAddressService.getClientIp(request);

        if (ipAddress == null || ipAddress.isBlank()) {
            return "redirect:/login?error=ip-detection";
        }

        ipAddress = ipAddress.trim();

        System.out.println("EMPLOYEE ID = " + employee.getId());
        System.out.println("CURRENT IP = " + ipAddress);

        // Always recheck IP authorization before proceeding.
        IpCheckResult ipCheckResult =
                verifyCurrentEmployeeIp(employee, ipAddress);

        if (!ipCheckResult.allowed()) {
            System.out.println("CONTINUE LOGIN - IP NOT APPROVED");
            return ipCheckResult.redirectUrl();
        }

        System.out.println("CONTINUE LOGIN - IP APPROVED");

        // =====================================================
        // TIME CHECK
        // =====================================================

        LocalTime now = LocalTime.now();

        System.out.println("CURRENT TIME = " + now);
        System.out.println("LATE LOGIN TIME = " + LATE_LOGIN_TIME);

        // =====================================================
        // ON-TIME LOGIN
        // =====================================================

        if (now.isBefore(LATE_LOGIN_TIME)) {
            System.out.println("EMPLOYEE LOGIN = ON TIME");

            // No late-login approval is required.
            session.removeAttribute(LOGIN_APPROVAL_TOKEN);
            session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

            recordSuccessfulLoginIfNeeded(
                    employee,
                    request,
                    session
            );

            return "redirect:/employee/dashboard";
        }

        // =====================================================
        // LATE LOGIN: SEPARATE APPROVAL
        // =====================================================

        System.out.println("EMPLOYEE LOGIN = LATE");

        try {
            LoginApprovalRequest approvalRequest =
                    loginApprovalService.createRequest(employee);

            if (approvalRequest == null) {
                session.removeAttribute(LOGIN_APPROVAL_TOKEN);
                session.removeAttribute(LOGIN_APPROVAL_COMPLETED);
                return "redirect:/login?error=approval";
            }

            String approvalStatus = approvalRequest.getStatus();
            String approvalToken = approvalRequest.getApprovalToken();

            System.out.println(
                    "LOGIN APPROVAL REQUEST ID = "
                            + approvalRequest.getId()
            );
            System.out.println(
                    "LOGIN APPROVAL STATUS = " + approvalStatus
            );

            // A previously approved late-login request is valid
            // only after the IP check above has passed.
            if ("APPROVED".equalsIgnoreCase(approvalStatus)) {
                session.removeAttribute(LOGIN_APPROVAL_TOKEN);
                session.setAttribute(
                        LOGIN_APPROVAL_COMPLETED,
                        Boolean.TRUE
                );

                recordSuccessfulLoginIfNeeded(
                        employee,
                        request,
                        session
                );

                return "redirect:/employee/dashboard";
            }

            if ("REJECTED".equalsIgnoreCase(approvalStatus)) {
                session.removeAttribute(LOGIN_APPROVAL_TOKEN);
                session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

                return "redirect:/login?error=login-rejected";
            }

            if ("PENDING".equalsIgnoreCase(approvalStatus)) {
                if (approvalToken == null || approvalToken.isBlank()) {
                    session.removeAttribute(LOGIN_APPROVAL_TOKEN);
                    session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

                    return "redirect:/login?error=approval";
                }

                // Store the approval token, not the database request ID.
                session.setAttribute(
                        LOGIN_APPROVAL_TOKEN,
                        approvalToken
                );

                session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

                return "redirect:/employee/login-pending";
            }

            session.removeAttribute(LOGIN_APPROVAL_TOKEN);
            session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

            return "redirect:/login?error=approval";

        } catch (
                LoginApprovalService.LoginRejectedForTodayException ex
        ) {
            session.removeAttribute(LOGIN_APPROVAL_TOKEN);
            session.removeAttribute(LOGIN_APPROVAL_COMPLETED);

            return "redirect:/login?error=login-rejected";
        }
    }

    // =========================================================
    // IP APPROVAL STATUS
    // =========================================================

    @GetMapping("/ip-approval-status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> ipApprovalStatus(
            Authentication authentication,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        User employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            response.put("status", "UNAUTHENTICATED");

            return ResponseEntity.status(401).body(response);
        }

        String ipAddress =
                clientIpAddressService.getClientIp(request);

        if (ipAddress == null || ipAddress.isBlank()) {
            response.put("status", "IP_DETECTION_FAILED");

            return ResponseEntity.badRequest().body(response);
        }

        ipAddress = ipAddress.trim();

        String status = employeeIpApprovalService.getRequestStatus(
                employee.getId(),
                employee.getAdminId(),
                ipAddress
        );

        // Report approval only if the IP is actually authorized.

        if ("APPROVED".equalsIgnoreCase(status)
                && !employeeIpAccessService.isApprovedLoginIp(
                employee.getId(),
                employee.getAdminId(),
                ipAddress
        )) {
            status = "PENDING";
        }

        response.put("status", status != null ? status : "NONE");
        response.put("ipAddress", ipAddress);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // CURRENT IP APPROVAL REQUEST
    // =========================================================

    @GetMapping("/ip-approval-request")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getCurrentRequest(
            Authentication authentication,
            HttpServletRequest request
    ) {
        Map<String, Object> response = new HashMap<>();

        User employee = getAuthenticatedEmployee(authentication);

        if (employee == null) {
            response.put("status", "UNAUTHENTICATED");

            return ResponseEntity.status(401).body(response);
        }

        String ipAddress =
                clientIpAddressService.getClientIp(request);

        if (ipAddress == null || ipAddress.isBlank()) {
            response.put("status", "IP_DETECTION_FAILED");

            return ResponseEntity.badRequest().body(response);
        }

        ipAddress = ipAddress.trim();

        Optional<EmployeeIpApprovalRequest> requestOptional =
                employeeIpApprovalService.getRequest(
                        employee.getId(),
                        employee.getAdminId(),
                        ipAddress
                );

        if (requestOptional.isEmpty()) {
            response.put("status", "NONE");

            return ResponseEntity.ok(response);
        }

        EmployeeIpApprovalRequest approvalRequest =
                requestOptional.get();

        response.put("status", approvalRequest.getStatus());
        response.put("requestedAt", approvalRequest.getRequestedAt());
        response.put("processedAt", approvalRequest.getProcessedAt());
        response.put("decisionNote", approvalRequest.getDecisionNote());
        response.put("ipAddress", approvalRequest.getIpAddress());

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // VERIFY CURRENT EMPLOYEE IP
    // =========================================================

    private IpCheckResult verifyCurrentEmployeeIp(
            User employee,
            String clientIp
    ) {
        Integer adminId = employee.getAdminId();

        if (adminId == null) {
            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-configuration"
            );
        }

        AdminSettings adminSettings =
                adminSettingsService.getSettings(adminId);

        if (adminSettings == null) {
            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-configuration"
            );
        }

        // If IP restriction is disabled, do not require IP approval.
        if (!adminSettings.isIpRestrictionEnabled()) {
            System.out.println("CURRENT IP CHECK = RESTRICTION DISABLED");

            return new IpCheckResult(true, clientIp, null);
        }

        // Company IP is allowed without employee secondary-IP approval.
        String companyIp = adminSettings.getCompanyIpAddress();

        if (companyIp != null && !companyIp.isBlank()) {
            companyIp = companyIp.trim();

            if (companyIp.equalsIgnoreCase(clientIp)) {
                System.out.println("CURRENT IP CHECK = COMPANY IP APPROVED");

                return new IpCheckResult(true, clientIp, null);
            }
        }

        // Existing approved employee IP.

        if (employeeIpAccessService.isApprovedLoginIp(
                employee.getId(),
                adminId,
                clientIp
        )) {
            System.out.println(
                    "CURRENT IP CHECK = APPROVED OFFICE/SECONDARY IP"
            );

            employeeIpAccessService.recordIpUsage(
                    employee.getId(),
                    adminId,
                    clientIp
            );

            return new IpCheckResult(true, clientIp, null);
        }

        // Unknown IP: never automatically grant access.
        System.out.println("CURRENT IP CHECK = IP NOT APPROVED");

        if (!adminSettings.isUnknownIpRequiresApproval()) {
            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-approval-disabled"
            );
        }

        try {
            // Create or reuse a PENDING request.
            // Approval must happen through the admin approval flow.
            employeeIpApprovalService.createOrGetRequest(
                    employee.getId(),
                    adminId,
                    clientIp
            );

            System.out.println(
                    "IP APPROVAL REQUEST CREATED OR REUSED"
            );

            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/employee/ip-approval-pending"
            );

        } catch (IllegalStateException ex) {
            // An administrator may have approved the IP between
            // the authorization check and request creation.
            if (employeeIpAccessService.isIpApproved(
                    employee.getId(),
                    adminId,
                    clientIp
            )) {
                return new IpCheckResult(true, clientIp, null);
            }

            System.out.println(
                    "IP APPROVAL ERROR: " + ex.getMessage()
            );

            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-approval"
            );

        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "INVALID IP APPROVAL REQUEST: "
                            + ex.getMessage()
            );

            return new IpCheckResult(
                    false,
                    clientIp,
                    "redirect:/login?error=ip-approval"
            );
        }
    }

    // =========================================================
    // RECORD SUCCESSFUL LOGIN ONCE PER SESSION
    // =========================================================

    private void recordSuccessfulLoginIfNeeded(
            User employee,
            HttpServletRequest request,
            HttpSession session
    ) {
        Boolean loginRecorded =
                (Boolean) session.getAttribute(LOGIN_RECORDED);

        if (Boolean.TRUE.equals(loginRecorded)) {
            System.out.println("SUCCESSFUL LOGIN ALREADY RECORDED");
            return;
        }

        accountLockoutService.recordSuccessfulLogin(employee);

        loginHistoryService.recordSuccessfulLogin(
                employee,
                request
        );

        session.setAttribute(LOGIN_RECORDED, Boolean.TRUE);

        System.out.println("SUCCESSFUL LOGIN RECORDED");
    }

    // =========================================================
    // GET AUTHENTICATED EMPLOYEE
    // =========================================================

    private User getAuthenticatedEmployee(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        if (!(authentication.getPrincipal()
                instanceof CustomUserDetails customUserDetails)) {
            return null;
        }

        User employee = customUserDetails.getUser();

        if (employee == null) {
            return null;
        }

        if (!"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return null;
        }

        return employee;
    }

    // =========================================================
    // IP CHECK RESULT
    // =========================================================

    private record IpCheckResult(
            boolean allowed,
            String ipAddress,
            String redirectUrl
    ) {
    }
}
