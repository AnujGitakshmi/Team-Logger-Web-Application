
package com.example.TeamLoggerC.security;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.EmployeeIpAccess;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.service.AccountLockoutService;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.ClientIpAddressService;
import com.example.TeamLoggerC.service.EmployeeIpAccessService;
import com.example.TeamLoggerC.service.EmployeeIpApprovalService;
import com.example.TeamLoggerC.service.LoginHistoryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final AccountLockoutService accountLockoutService;
    private final LoginHistoryService loginHistoryService;
    private final AdminSettingsService adminSettingsService;
    private final ClientIpAddressService clientIpAddressService;
    private final EmployeeIpAccessService employeeIpAccessService;
    private final EmployeeIpApprovalService employeeIpApprovalService;

    public LoginSuccessHandler(
            AccountLockoutService accountLockoutService,
            LoginHistoryService loginHistoryService,
            AdminSettingsService adminSettingsService,
            ClientIpAddressService clientIpAddressService,
            EmployeeIpAccessService employeeIpAccessService,
            EmployeeIpApprovalService employeeIpApprovalService) {

        this.accountLockoutService = accountLockoutService;
        this.loginHistoryService = loginHistoryService;
        this.adminSettingsService = adminSettingsService;
        this.clientIpAddressService = clientIpAddressService;
        this.employeeIpAccessService = employeeIpAccessService;
        this.employeeIpApprovalService = employeeIpApprovalService;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication)
            throws IOException, ServletException {

        if (authentication == null
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {

            response.sendRedirect(
                    request.getContextPath() + "/login?error=user");
            return;
        }

        User user = userDetails.getUser();

        if (user == null) {
            response.sendRedirect(
                    request.getContextPath() + "/login?error=user");
            return;
        }

        String role = user.getRole();
        String organizationRole = user.getOrganizationRole();

        HttpSession session = request.getSession();

        System.out.println("========== LOGIN SUCCESS HANDLER ==========");
        System.out.println("USER ID = " + user.getId());
        System.out.println("USER EMAIL = " + user.getEmail());
        System.out.println("USER ROLE = " + role);
        System.out.println("ORGANIZATION ROLE = " + organizationRole);
        System.out.println("SESSION ID = " + session.getId());

        // Defense in depth: do not route inactive accounts.
        if (user.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(user.getStatus().trim())) {

            response.sendRedirect(
                    request.getContextPath() + "/login?error=inactive");
            return;
        }

        // =====================================================
        // SUPER ADMIN LOGIN
        // Both stored role fields must identify a Super Admin.
        // =====================================================
        if ("SUPER_ADMIN".equalsIgnoreCase(role)) {

            if (!"SUPER_ADMIN".equalsIgnoreCase(organizationRole)
                    || user.getOrganizationId() == null) {

                System.err.println(
                        "SUPER ADMIN LOGIN REJECTED: Invalid role or organization. "
                                + "User ID = " + user.getId());

                response.sendRedirect(
                        request.getContextPath() + "/login?error=role");
                return;
            }

            // Record successful login only after validating the role.
            accountLockoutService.recordSuccessfulLogin(user);
            loginHistoryService.recordSuccessfulLogin(user, request);

            response.sendRedirect(
                    request.getContextPath() + "/super-admin/dashboard");
            return;
        }

        // =====================================================
        // NORMAL ADMIN / BRANCH ADMIN LOGIN
        // =====================================================
        if ("ADMIN".equalsIgnoreCase(role)) {

            // A normal ADMIN must not use the Super Admin organization role.
            if ("SUPER_ADMIN".equalsIgnoreCase(organizationRole)) {

                System.err.println(
                        "ADMIN LOGIN REJECTED: ADMIN role cannot use "
                                + "SUPER_ADMIN organization role. User ID = "
                                + user.getId());

                response.sendRedirect(
                        request.getContextPath() + "/login?error=role");
                return;
            }

            if (user.getOrganizationId() == null
                    || user.getBranchId() == null) {

                response.sendRedirect(
                        request.getContextPath() + "/login?error=organization");
                return;
            }

            boolean isOrganizationAdmin =
                    "ORG_ADMIN".equalsIgnoreCase(organizationRole);

            boolean isBranchAdmin =
                    "BRANCH_ADMIN".equalsIgnoreCase(organizationRole);

            if (!isOrganizationAdmin && !isBranchAdmin) {

                System.err.println(
                        "ADMIN LOGIN REJECTED: Invalid organization role. "
                                + "User ID = " + user.getId());

                response.sendRedirect(
                        request.getContextPath() + "/login?error=role");
                return;
            }

            accountLockoutService.recordSuccessfulLogin(user);
            loginHistoryService.recordSuccessfulLogin(user, request);

            response.sendRedirect(
                    request.getContextPath() + "/admin/dashboard");
            return;
        }

        // =====================================================
        // EMPLOYEE LOGIN
        // Preserve IP validation and late-login approval.
        // =====================================================
        if ("EMPLOYEE".equalsIgnoreCase(role)) {

            String clientIp = clientIpAddressService.getClientIp(request);

            System.out.println("EMPLOYEE CLIENT IP = " + clientIp);

            boolean ipAllowed = checkEmployeeIpAccess(
                    user,
                    clientIp,
                    response
            );

            if (!ipAllowed) {
                return;
            }

            System.out.println("EMPLOYEE IP ACCESS = APPROVED");

            // Preserve the existing late-login approval flow.
            response.sendRedirect(
                    request.getContextPath() + "/employee/continue-login");
            return;
        }

        // Reject any unsupported or unexpected role.
        response.sendRedirect(
                request.getContextPath() + "/login?error=role");
    }

    private boolean checkEmployeeIpAccess(
            User employee,
            String clientIp,
            HttpServletResponse response) throws IOException {

        Integer employeeId = employee.getId();
        Integer adminId = employee.getAdminId();

        System.out.println("========== EMPLOYEE IP ACCESS CHECK ==========");
        System.out.println("EMPLOYEE ID = " + employeeId);
        System.out.println("ADMIN ID = " + adminId);
        System.out.println("CLIENT IP = " + clientIp);

        if (adminId == null) {
            System.err.println(
                    "IP CHECK FAILED = EMPLOYEE HAS NO ADMIN ID");

            response.sendRedirect(
                    "/login?error=ip-configuration");
            return false;
        }

        if (clientIp == null
                || clientIp.isBlank()
                || "UNKNOWN".equalsIgnoreCase(clientIp.trim())) {

            System.err.println(
                    "IP CHECK FAILED = CLIENT IP NOT DETECTED");

            response.sendRedirect(
                    "/login?error=ip-detection");
            return false;
        }

        clientIp = clientIp.trim();

        AdminSettings settings = adminSettingsService.getSettings(adminId);

        if (settings == null) {
            System.err.println(
                    "IP CHECK FAILED = ADMIN SETTINGS NOT FOUND");

            response.sendRedirect(
                    "/login?error=ip-configuration");
            return false;
        }

        System.out.println("========== IP APPROVAL DIAGNOSTICS ==========");
        System.out.println("EMPLOYEE ID = " + employeeId);
        System.out.println("ADMIN ID = " + adminId);
        System.out.println("CLIENT IP = " + clientIp);
        System.out.println(
                "COMPANY IP = " + settings.getCompanyIpAddress());
        System.out.println(
                "IP RESTRICTION ENABLED = "
                        + settings.isIpRestrictionEnabled());
        System.out.println(
                "UNKNOWN IP REQUIRES APPROVAL = "
                        + settings.isUnknownIpRequiresApproval());

        // STEP 1: Check whether IP restriction is enabled.
        if (!settings.isIpRestrictionEnabled()) {
            System.out.println("IP RESTRICTION = DISABLED");
            return true;
        }

        // STEP 2: Check the configured company IP.
        String companyIp = settings.getCompanyIpAddress();

        if (companyIp != null
                && !companyIp.isBlank()
                && companyIp.trim().equalsIgnoreCase(clientIp)) {

            System.out.println("IP MATCH = CONFIGURED COMPANY IP");
            return true;
        }

        // STEP 3: Check the approved OFFICE IP.
        EmployeeIpAccess officeIp =
                employeeIpAccessService.getOfficeIp(adminId, employeeId);

        if (matches(officeIp, clientIp)) {

            System.out.println("IP MATCH = EMPLOYEE OFFICE/PRIMARY IP");

            employeeIpAccessService.recordIpUsage(
                    employeeId, adminId, clientIp);

            return true;
        }

        // STEP 4: Check the approved SECONDARY IP.
        EmployeeIpAccess secondaryIp =
                employeeIpAccessService.getSecondaryIp(adminId, employeeId);

        if (matches(secondaryIp, clientIp)) {

            System.out.println("IP MATCH = APPROVED SECONDARY IP");

            employeeIpAccessService.recordIpUsage(
                    employeeId, adminId, clientIp);

            return true;
        }

        // STEP 5: Neither approved IP matches.
        System.out.println("UNKNOWN EMPLOYEE IP DETECTED");
        System.out.println("UNKNOWN IP = " + clientIp);

        if (!settings.isUnknownIpRequiresApproval()) {

            System.out.println("UNKNOWN IP APPROVAL IS DISABLED");

            response.sendRedirect(
                    "/login?error=ip-approval-disabled");
            return false;
        }

        // STEP 6: Create or reuse a pending IP approval request.
        try {
            System.out.println(
                    "ACTION = CREATING/REUSING IP APPROVAL REQUEST");

            employeeIpApprovalService.createOrGetRequest(
                    employeeId,
                    adminId,
                    clientIp
            );

            System.out.println(
                    "IP APPROVAL REQUEST CREATED OR REUSED");

            response.sendRedirect(
                    "/employee/ip-approval-pending");
            return false;

        } catch (IllegalStateException ex) {

            // Recheck in case the IP was approved concurrently.
            EmployeeIpAccess latestOffice =
                    employeeIpAccessService.getOfficeIp(adminId, employeeId);

            EmployeeIpAccess latestSecondary =
                    employeeIpAccessService.getSecondaryIp(adminId, employeeId);

            boolean companyIpMatches =
                    companyIp != null
                            && !companyIp.isBlank()
                            && companyIp.trim().equalsIgnoreCase(clientIp);

            boolean nowApproved =
                    matches(latestOffice, clientIp)
                            || matches(latestSecondary, clientIp)
                            || companyIpMatches;

            if (nowApproved) {

                employeeIpAccessService.recordIpUsage(
                        employeeId, adminId, clientIp);

                System.out.println("IP APPROVED AFTER RECHECK");
                return true;
            }

            System.err.println(
                    "IP APPROVAL REQUEST NOT CREATED: " + ex.getMessage());

            response.sendRedirect(
                    "/login?error=ip-approval");
            return false;

        } catch (IllegalArgumentException ex) {

            System.err.println(
                    "INVALID IP APPROVAL REQUEST: " + ex.getMessage());

            response.sendRedirect(
                    "/login?error=ip-approval");
            return false;
        }
    }

    private boolean matches(
            EmployeeIpAccess ipAccess,
            String clientIp) {

        return ipAccess != null
                && ipAccess.getIpAddress() != null
                && clientIp != null
                && ipAccess.getIpAddress().trim()
                .equalsIgnoreCase(clientIp.trim());
    }
}
