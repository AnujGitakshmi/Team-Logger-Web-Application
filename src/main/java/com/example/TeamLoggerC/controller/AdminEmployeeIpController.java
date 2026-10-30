 package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AccessPolicy;
import com.example.TeamLoggerC.entity.EmployeeIpAccess;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AccessPolicyService;
import com.example.TeamLoggerC.service.EmployeeIpAccessService;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/employees")
public class AdminEmployeeIpController {

    private final EmployeeIpAccessService employeeIpAccessService;
    private final AccessPolicyService accessPolicyService;

    public AdminEmployeeIpController(
            EmployeeIpAccessService employeeIpAccessService,
            AccessPolicyService accessPolicyService) {

        this.employeeIpAccessService =
                employeeIpAccessService;

        this.accessPolicyService =
                accessPolicyService;
    }


    // =====================================================
    // GET EMPLOYEE IP ACCESS
    // =====================================================

    /**
     * GET
     * /admin/employees/{employeeId}/ip-access
     *
     * Returns all IP access records belonging to
     * the selected employee.
     */
    @GetMapping("/{employeeId}/ip-access")
    public List<EmployeeIpAccess> getEmployeeIpAccess(
            @PathVariable Integer employeeId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        return employeeIpAccessService
                .getEmployeeIpAccess(
                        adminId,
                        employeeId
                );
    }


    // =====================================================
    // SET / CHANGE OFFICE IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/office
     *
     * Office IP is immediately APPROVED.
     *
     * Request:
     *
     * {
     *     "ipAddress": "192.168.1.25"
     * }
     */
    @PostMapping("/{employeeId}/ip-access/office")
    public EmployeeIpAccess setOfficeIp(
            @PathVariable Integer employeeId,
            @RequestBody IpAddressRequest request,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        validateIpAddressRequest(request);

        return employeeIpAccessService
                .setOfficeIp(
                        adminId,
                        employeeId,
                        request.ipAddress()
                );
    }


    // =====================================================
    // REQUEST SECONDARY IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/secondary
     *
     * Creates a secondary IP request with PENDING status.
     *
     * Request:
     *
     * {
     *     "ipAddress": "192.168.1.50"
     * }
     */
    @PostMapping("/{employeeId}/ip-access/secondary")
    public EmployeeIpAccess requestSecondaryIp(
            @PathVariable Integer employeeId,
            @RequestBody IpAddressRequest request,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        validateIpAddressRequest(request);

        return employeeIpAccessService
                .requestSecondaryIp(
                        adminId,
                        employeeId,
                        request.ipAddress()
                );
    }


    // =====================================================
    // APPROVE SECONDARY IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/approve
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/approve"
    )
    public EmployeeIpAccess approveSecondaryIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        return employeeIpAccessService
                .approveSecondaryIp(
                        adminId,
                        employeeId,
                        ipAccessId
                );
    }


    // =====================================================
    // REJECT SECONDARY IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/reject
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/reject"
    )
    public EmployeeIpAccess rejectSecondaryIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        return employeeIpAccessService
                .rejectSecondaryIp(
                        adminId,
                        employeeId,
                        ipAccessId
                );
    }


    // =====================================================
    // REVOKE IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/revoke
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/revoke"
    )
    public EmployeeIpAccess revokeIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        return employeeIpAccessService
                .revokeIp(
                        adminId,
                        employeeId,
                        ipAccessId
                );
    }


    // =====================================================
    // UPDATE APPROXIMATE LOCATION
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/location
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/location"
    )
    public EmployeeIpAccess updateLocation(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            @RequestBody IpLocationRequest request,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        if (request == null) {
            throw new IllegalArgumentException(
                    "Location information is required."
            );
        }

        return employeeIpAccessService
                .updateLocation(
                        adminId,
                        employeeId,
                        ipAccessId,
                        request.country(),
                        request.state(),
                        request.city(),
                        request.isp(),
                        request.latitude(),
                        request.longitude()
                );
    }


    // =====================================================
    // GET ACTIVE POLICIES FOR IP
    // =====================================================

    /**
     * GET
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/policies
     *
     * Returns active access policies belonging to the
     * authenticated administrator.
     *
     * The frontend can use this endpoint to populate
     * the policy dropdown.
     */
    @GetMapping(
            "/{employeeId}/ip-access/{ipAccessId}/policies"
    )
    public List<AccessPolicy> getPoliciesForIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (!EmployeeIpAccessService.STATUS_APPROVED
                .equalsIgnoreCase(ipAccess.getStatus())) {

            throw new IllegalArgumentException(
                    "Only an approved IP can have a usage policy."
            );
        }

        return accessPolicyService
                .getActivePoliciesForAdmin(adminId);
    }


    // =====================================================
    // ASSIGN POLICY TO IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/policy
     *
     * Assigns an access policy to an approved IP.
     *
     * Request:
     *
     * {
     *     "policyId": 3
     * }
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/policy"
    )
    public EmployeeIpAccess assignPolicyToIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            @RequestBody PolicyAssignmentRequest request,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        if (request == null ||
                request.policyId() == null) {

            throw new IllegalArgumentException(
                    "Policy ID is required."
            );
        }

        return employeeIpAccessService
                .assignPolicyToIp(
                        adminId,
                        employeeId,
                        ipAccessId,
                        request.policyId()
                );
    }


    // =====================================================
    // REMOVE POLICY FROM IP
    // =====================================================

    /**
     * POST
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/policy/remove
     *
     * Removes the usage policy from an IP.
     *
     * The IP remains approved for login.
     */
    @PostMapping(
            "/{employeeId}/ip-access/{ipAccessId}/policy/remove"
    )
    public EmployeeIpAccess removePolicyFromIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        return employeeIpAccessService
                .removePolicyFromIp(
                        adminId,
                        employeeId,
                        ipAccessId
                );
    }


    // =====================================================
    // GET CURRENT POLICY ASSIGNED TO IP
    // =====================================================

    /**
     * GET
     * /admin/employees/{employeeId}/ip-access/{ipAccessId}/policy
     *
     * Returns the policy currently assigned to the IP.
     */
    @GetMapping(
            "/{employeeId}/ip-access/{ipAccessId}/policy"
    )
    public AccessPolicy getPolicyForIp(
            @PathVariable Integer employeeId,
            @PathVariable Long ipAccessId,
            Authentication authentication) {

        Integer adminId =
                getAuthenticatedAdminId(authentication);

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (!EmployeeIpAccessService.STATUS_APPROVED
                .equalsIgnoreCase(ipAccess.getStatus())) {

            throw new IllegalArgumentException(
                    "Only an approved IP can have a usage policy."
            );
        }

        if (ipAccess.getPolicyId() == null) {
            throw new IllegalArgumentException(
                    "No policy is assigned to this IP."
            );
        }

        return accessPolicyService
                .getPolicyForAdmin(
                        adminId,
                        ipAccess.getPolicyId()
                );
    }


    // =====================================================
    // AUTHENTICATED ADMIN
    // =====================================================

    private Integer getAuthenticatedAdminId(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "Administrator authentication is required."
            );
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {

            throw new IllegalStateException(
                    "Authenticated administrator information was not found."
            );
        }

        if (userDetails.getUser() == null) {

            throw new IllegalStateException(
                    "Authenticated user information was not found."
            );
        }

        if (!"ADMIN".equalsIgnoreCase(
                userDetails.getUser().getRole())) {

            throw new IllegalStateException(
                    "Only administrators can manage employee IP access."
            );
        }

        return userDetails.getUser().getId();
    }


    // =====================================================
    // IP ACCESS VALIDATION
    // =====================================================

    private EmployeeIpAccess getIpAccessForAdmin(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        return employeeIpAccessService
                .getEmployeeIpAccess(
                        adminId,
                        employeeId
                )
                .stream()
                .filter(ip ->
                        ip.getId() != null &&
                                ip.getId().equals(ipAccessId)
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "IP access record was not found."
                        )
                );
    }


    // =====================================================
    // REQUEST VALIDATION
    // =====================================================

    private void validateIpAddressRequest(
            IpAddressRequest request) {

        if (request == null ||
                request.ipAddress() == null ||
                request.ipAddress().isBlank()) {

            throw new IllegalArgumentException(
                    "IP address is required."
            );
        }
    }


    // =====================================================
    // REQUEST DTOs
    // =====================================================

    public record IpAddressRequest(
            String ipAddress
    ) {
    }


    public record IpLocationRequest(
            String country,
            String state,
            String city,
            String isp,
            Double latitude,
            Double longitude
    ) {
    }


    public record PolicyAssignmentRequest(
            Long policyId
    ) {
    }
}
