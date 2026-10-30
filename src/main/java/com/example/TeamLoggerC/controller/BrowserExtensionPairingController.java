
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.BrowserExtensionTokenService;
import com.example.TeamLoggerC.service.ClientIpAddressService;
import com.example.TeamLoggerC.service.EmployeeIpAccessService;
import com.example.TeamLoggerC.service.EmployeeIpApprovalService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/employee/browser-extension")
public class BrowserExtensionPairingController {

    private static final String LOGIN_RECORDED = "LOGIN_RECORDED";

    private final BrowserExtensionTokenService tokenService;
    private final AdminSettingsService adminSettingsService;
    private final ClientIpAddressService clientIpAddressService;
    private final EmployeeIpAccessService employeeIpAccessService;
    private final EmployeeIpApprovalService employeeIpApprovalService;

    public BrowserExtensionPairingController(
            BrowserExtensionTokenService tokenService,
            AdminSettingsService adminSettingsService,
            ClientIpAddressService clientIpAddressService,
            EmployeeIpAccessService employeeIpAccessService,
            EmployeeIpApprovalService employeeIpApprovalService
    ) {
        this.tokenService = tokenService;
        this.adminSettingsService = adminSettingsService;
        this.clientIpAddressService = clientIpAddressService;
        this.employeeIpAccessService = employeeIpAccessService;
        this.employeeIpApprovalService = employeeIpApprovalService;
    }

    @PostMapping("/pair")
    public ResponseEntity<Map<String, Object>> pairExtension(
            Authentication authentication,
            HttpServletRequest request,
            HttpSession session,
            @RequestParam(defaultValue = "Google Chrome") String deviceName
    ) {
        // Require a real authenticated employee.
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {
            return error(HttpStatus.UNAUTHORIZED,
                    "Please log in to TeamLoggerC.");
        }

        User employee = userDetails.getUser();

        if (employee == null
                || employee.getId() == null
                || !"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return error(HttpStatus.FORBIDDEN,
                    "Only employees can pair the browser extension.");
        }

        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(employee.getStatus().trim())) {
            return error(HttpStatus.FORBIDDEN,
                    "Your employee account is not active.");
        }

        Integer employeeId = employee.getId();
        Integer adminId = employee.getAdminId();

        if (adminId == null) {
            return error(HttpStatus.FORBIDDEN,
                    "No administrator is assigned to your account.");
        }

        // This flag is set by the existing successful-login flow,
        // after the required IP and late-login checks have passed.
        if (!Boolean.TRUE.equals(
                session.getAttribute(LOGIN_RECORDED))) {
            return error(HttpStatus.FORBIDDEN,
                    "Complete your existing login and approval process "
                            + "before pairing the extension.");
        }

        // Recheck the current IP before issuing a long-lived token.
        String clientIp = clientIpAddressService.getClientIp(request);

        if (clientIp == null || clientIp.isBlank()) {
            return error(HttpStatus.FORBIDDEN,
                    "Your IP address could not be verified.");
        }

        clientIp = clientIp.trim();

        AdminSettings settings = adminSettingsService.getSettings(adminId);

        if (settings == null) {
            return error(HttpStatus.FORBIDDEN,
                    "Administrator IP settings could not be loaded.");
        }

        if (settings.isIpRestrictionEnabled()) {
            String companyIp = settings.getCompanyIpAddress();

            boolean isCompanyIp = companyIp != null
                    && !companyIp.isBlank()
                    && companyIp.trim().equalsIgnoreCase(clientIp);

            boolean isApprovedEmployeeIp =
                    employeeIpAccessService.isApprovedLoginIp(
                            employeeId,
                            adminId,
                            clientIp
                    );

            if (!isCompanyIp && !isApprovedEmployeeIp) {

                if (settings.isUnknownIpRequiresApproval()) {
                    try {
                        employeeIpApprovalService.createOrGetRequest(
                                employeeId,
                                adminId,
                                clientIp
                        );
                    } catch (IllegalStateException ex) {
                        // The IP may have been approved concurrently.
                        isApprovedEmployeeIp =
                                employeeIpAccessService.isApprovedLoginIp(
                                        employeeId,
                                        adminId,
                                        clientIp
                                );
                    } catch (IllegalArgumentException ex) {
                        return error(HttpStatus.FORBIDDEN,
                                "An IP approval request could not be created.");
                    }
                }

                if (!isApprovedEmployeeIp && !isCompanyIp) {
                    return error(HttpStatus.FORBIDDEN,
                            "This IP address is not approved. "
                                    + "Complete IP approval before pairing.");
                }
            }
        }

        String safeDeviceName = deviceName == null
                ? "Google Chrome"
                : deviceName.trim();

        if (safeDeviceName.isBlank()) {
            safeDeviceName = "Google Chrome";
        }

        if (safeDeviceName.length() > 100) {
            safeDeviceName = safeDeviceName.substring(0, 100);
        }

        // The service returns the raw token once and stores its hash.
        String token = tokenService.createToken(
                employeeId,
                adminId,
                safeDeviceName
        );

        return ResponseEntity.ok(Map.of(
                "message", "Browser extension paired successfully.",
                "token", token,
                "deviceName", safeDeviceName
        ));
    }

    private ResponseEntity<Map<String, Object>> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status).body(Map.of(
                "error", message
        ));
    }
}
