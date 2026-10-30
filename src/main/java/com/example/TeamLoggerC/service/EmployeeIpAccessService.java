package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AccessPolicy;
import com.example.TeamLoggerC.entity.EmployeeIpAccess;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.EmployeeIpAccessRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeIpAccessService {

    public static final String ACCESS_TYPE_OFFICE = "OFFICE";
    public static final String ACCESS_TYPE_SECONDARY = "SECONDARY";

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_REVOKED = "REVOKED";

    private static final int MAX_IP_ADDRESSES = 2;

    private static final String POLICY_STATUS_ACTIVE = "ACTIVE";

    private final EmployeeIpAccessRepo employeeIpAccessRepo;
    private final UserRepo userRepo;
    private final AccessPolicyService accessPolicyService;

    public EmployeeIpAccessService(
            EmployeeIpAccessRepo employeeIpAccessRepo,
            UserRepo userRepo,
            AccessPolicyService accessPolicyService) {

        this.employeeIpAccessRepo = employeeIpAccessRepo;
        this.userRepo = userRepo;
        this.accessPolicyService = accessPolicyService;
    }


    // =====================================================
    // GET EMPLOYEE IP ACCESS
    // =====================================================

    @Transactional(readOnly = true)
    public List<EmployeeIpAccess> getEmployeeIpAccess(
            Integer adminId,
            Integer employeeId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        return employeeIpAccessRepo
                .findByEmployeeIdAndAdminIdOrderByCreatedAtAsc(
                        employeeId,
                        adminId
                );
    }



// =====================================================
// GET ACTIVE APPROVED OFFICE IP
// =====================================================

    @Transactional(readOnly = true)
    public EmployeeIpAccess getOfficeIp(
            Integer adminId,
            Integer employeeId) {

        validateEmployeeBelongsToAdmin(adminId, employeeId);

        return employeeIpAccessRepo
                .findByEmployeeIdAndAdminIdAndAccessTypeAndStatus(
                        employeeId,
                        adminId,
                        ACCESS_TYPE_OFFICE,
                        STATUS_APPROVED
                )
                .orElse(null);
    }


// =====================================================
// GET ACTIVE APPROVED SECONDARY IP
// =====================================================

    @Transactional(readOnly = true)
    public EmployeeIpAccess getSecondaryIp(
            Integer adminId,
            Integer employeeId) {

        validateEmployeeBelongsToAdmin(adminId, employeeId);

        return employeeIpAccessRepo
                .findByEmployeeIdAndAdminIdAndAccessTypeAndStatus(
                        employeeId,
                        adminId,
                        ACCESS_TYPE_SECONDARY,
                        STATUS_APPROVED
                )
                .orElse(null);
    }

    // =====================================================
    // SET / REPLACE OFFICE IP
    // =====================================================

    /**
     * Office IP is completely controlled by the administrator.
     *
     * The office IP is immediately APPROVED.
     *
     * If an office IP already exists, it is replaced.
     *
     * If the IP address changes, the old IP-specific usage
     * policy is removed.
     */
    @Transactional
    public EmployeeIpAccess setOfficeIp(
            Integer adminId,
            Integer employeeId,
            String ipAddress) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        String normalizedIp = normalizeIp(ipAddress);

        validateIpFormat(normalizedIp);

        /*
         * Prevent the same IP from being used as both
         * OFFICE and SECONDARY.
         */
        EmployeeIpAccess existingIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndIpAddress(
                                employeeId,
                                normalizedIp
                        )
                        .orElse(null);

        if (existingIp != null &&
                !ACCESS_TYPE_OFFICE.equalsIgnoreCase(
                        existingIp.getAccessType())) {

            throw new IllegalArgumentException(
                    "This IP address is already registered as the employee's secondary IP."
            );
        }

        EmployeeIpAccess officeIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAccessType(
                                employeeId,
                                ACCESS_TYPE_OFFICE
                        )
                        .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (officeIp == null) {

            officeIp = new EmployeeIpAccess();

            officeIp.setEmployeeId(employeeId);
            officeIp.setAdminId(adminId);
            officeIp.setAccessType(ACCESS_TYPE_OFFICE);
            officeIp.setCreatedAt(now);

        } else {

            if (!adminId.equals(
                    officeIp.getAdminId())) {

                throw new IllegalArgumentException(
                        "Office IP does not belong to your organization."
                );
            }
        }

        /*
         * If the IP changes, remove the old IP-specific policy.
         *
         * The administrator must explicitly assign a policy
         * to the new office IP.
         */
        if (officeIp.getIpAddress() == null ||
                !officeIp.getIpAddress().equalsIgnoreCase(
                        normalizedIp)) {

            officeIp.setPolicyId(null);
        }

        officeIp.setIpAddress(normalizedIp);
        officeIp.setStatus(STATUS_APPROVED);

        officeIp.setApprovedBy(adminId);
        officeIp.setApprovedAt(now);

        officeIp.setUpdatedAt(now);

        return employeeIpAccessRepo.save(officeIp);
    }


    // =====================================================
    // ADD / SET SECONDARY IP
    // =====================================================

    /**
     * Adds or changes the employee's secondary IP.
     *
     * IMPORTANT:
     *
     * This method is called by the ADMIN controller.
     *
     * Therefore the administrator is explicitly configuring
     * the employee's trusted secondary IP.
     *
     * The secondary IP is immediately APPROVED.
     *
     * There is no employee approval request here.
     *
     * POLICY BEHAVIOR:
     *
     * - New secondary IP record:
     *       policyId remains null.
     *
     * - Existing secondary IP record:
     *       existing policyId is preserved when the IP changes.
     *
     * Example:
     *
     * Old:
     *     ::1
     *     Policy #5
     *
     * New:
     *     192.168.56.1
     *     Policy #5
     */
    @Transactional
    public EmployeeIpAccess requestSecondaryIp(
            Integer adminId,
            Integer employeeId,
            String ipAddress) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        String normalizedIp = normalizeIp(ipAddress);

        validateIpFormat(normalizedIp);

        /*
         * Check whether this IP already exists for this employee.
         */
        EmployeeIpAccess existingIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndIpAddress(
                                employeeId,
                                normalizedIp
                        )
                        .orElse(null);

        if (existingIp != null) {

            /*
             * The IP cannot be both OFFICE and SECONDARY.
             */
            if (ACCESS_TYPE_OFFICE.equalsIgnoreCase(
                    existingIp.getAccessType())) {

                throw new IllegalArgumentException(
                        "This IP address is already registered as the office IP."
                );
            }

            /*
             * If it is already the employee's secondary IP,
             * simply return it instead of creating a duplicate.
             */
            if (ACCESS_TYPE_SECONDARY.equalsIgnoreCase(
                    existingIp.getAccessType())) {

                if (!adminId.equals(
                        existingIp.getAdminId())) {

                    throw new IllegalArgumentException(
                            "Secondary IP does not belong to your organization."
                    );
                }

                return existingIp;
            }
        }

        /*
         * Find the employee's existing secondary slot.
         */
        EmployeeIpAccess secondaryIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAccessType(
                                employeeId,
                                ACCESS_TYPE_SECONDARY
                        )
                        .orElse(null);

        LocalDateTime now = LocalDateTime.now();

        if (secondaryIp == null) {

            /*
             * Maximum allowed:
             *
             * 1 OFFICE
             * 1 SECONDARY
             */
            long totalRecords =
                    employeeIpAccessRepo.countByEmployeeId(
                            employeeId
                    );

            if (totalRecords >= MAX_IP_ADDRESSES) {

                throw new IllegalArgumentException(
                        "Maximum of two IP addresses is already configured for this employee."
                );
            }

            secondaryIp = new EmployeeIpAccess();

            secondaryIp.setEmployeeId(employeeId);
            secondaryIp.setAdminId(adminId);
            secondaryIp.setAccessType(
                    ACCESS_TYPE_SECONDARY
            );
            secondaryIp.setCreatedAt(now);

        } else {

            /*
             * Existing secondary record must belong to
             * the same organization.
             */
            if (!adminId.equals(
                    secondaryIp.getAdminId())) {

                throw new IllegalArgumentException(
                        "Secondary IP does not belong to your organization."
                );
            }
        }

        /*
         * ADMIN is explicitly setting this IP.
         *
         * Therefore it becomes APPROVED immediately.
         */
        secondaryIp.setIpAddress(normalizedIp);
        secondaryIp.setStatus(STATUS_APPROVED);

        /*
         * IMPORTANT:
         *
         * DO NOT clear policyId here.
         *
         * If this is an existing secondary-IP slot,
         * its existing usage policy remains attached
         * when the administrator changes the IP.
         *
         * Example:
         *
         *     ::1 -> Policy 5
         *
         * becomes:
         *
         *     192.168.56.1 -> Policy 5
         *
         * If this is a newly created secondary record,
         * policyId is naturally null because the new
         * EmployeeIpAccess object has no policy assigned yet.
         *
         * The administrator can assign a policy explicitly
         * afterward through assignPolicyToIp().
         */

        /*
         * Record who configured/approved the IP.
         */
        secondaryIp.setApprovedBy(adminId);
        secondaryIp.setApprovedAt(now);

        secondaryIp.setUpdatedAt(now);

        return employeeIpAccessRepo.save(secondaryIp);
    }


    // =====================================================
    // APPROVE SECONDARY IP
    // =====================================================

    /**
     * Kept for compatibility with existing records/UI.
     *
     * Normally this should no longer be required when an
     * administrator creates a secondary IP because the IP
     * is already APPROVED.
     */
    @Transactional
    public EmployeeIpAccess approveSecondaryIp(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (!ACCESS_TYPE_SECONDARY.equalsIgnoreCase(
                ipAccess.getAccessType())) {

            throw new IllegalArgumentException(
                    "Only a secondary IP can be approved through this operation."
            );
        }

        if (STATUS_APPROVED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            return ipAccess;
        }

        if (!STATUS_PENDING.equalsIgnoreCase(
                ipAccess.getStatus())) {

            throw new IllegalArgumentException(
                    "Only a pending secondary IP request can be approved."
            );
        }

        EmployeeIpAccess officeIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAccessType(
                                employeeId,
                                ACCESS_TYPE_OFFICE
                        )
                        .orElse(null);

        if (officeIp != null &&
                officeIp.getIpAddress() != null &&
                officeIp.getIpAddress().equalsIgnoreCase(
                        ipAccess.getIpAddress())) {

            throw new IllegalArgumentException(
                    "The secondary IP cannot be the same as the office IP."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        ipAccess.setStatus(STATUS_APPROVED);
        ipAccess.setApprovedBy(adminId);
        ipAccess.setApprovedAt(now);
        ipAccess.setUpdatedAt(now);

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // REJECT SECONDARY IP
    // =====================================================

    /**
     * Kept for compatibility with older pending requests.
     *
     * Newly configured admin secondary IPs will normally
     * never enter PENDING state.
     */
    @Transactional
    public EmployeeIpAccess rejectSecondaryIp(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (!ACCESS_TYPE_SECONDARY.equalsIgnoreCase(
                ipAccess.getAccessType())) {

            throw new IllegalArgumentException(
                    "Only a secondary IP request can be rejected."
            );
        }

        if (!STATUS_PENDING.equalsIgnoreCase(
                ipAccess.getStatus())) {

            throw new IllegalArgumentException(
                    "Only a pending secondary IP request can be rejected."
            );
        }

        LocalDateTime now = LocalDateTime.now();

        ipAccess.setStatus(STATUS_REJECTED);
        ipAccess.setApprovedBy(adminId);
        ipAccess.setApprovedAt(now);
        ipAccess.setUpdatedAt(now);

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // REVOKE IP
    // =====================================================

    /**
     * Revokes an existing IP.
     *
     * The record is retained for security history.
     *
     * The existing policyId is preserved historically.
     *
     * Runtime policy lookup ignores revoked IPs.
     */
    @Transactional
    public EmployeeIpAccess revokeIp(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (STATUS_REVOKED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            return ipAccess;
        }

        LocalDateTime now = LocalDateTime.now();

        ipAccess.setStatus(STATUS_REVOKED);
        ipAccess.setApprovedBy(adminId);
        ipAccess.setApprovedAt(now);
        ipAccess.setUpdatedAt(now);

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // CHECK WHETHER IP IS APPROVED
    // =====================================================

    @Transactional(readOnly = true)
    public boolean isIpApproved(
            Integer employeeId,
            Integer adminId,
            String ipAddress) {

        if (employeeId == null ||
                adminId == null ||
                ipAddress == null ||
                ipAddress.isBlank()) {

            return false;
        }

        String normalizedIp = normalizeIp(ipAddress);

        return employeeIpAccessRepo
                .existsByEmployeeIdAndAdminIdAndIpAddressAndStatus(
                        employeeId,
                        adminId,
                        normalizedIp,
                        STATUS_APPROVED
                );
    }

// =====================================================
// CHECK WHETHER IP IS AN APPROVED LOGIN IP
// =====================================================

    @Transactional(readOnly = true)
    public boolean isApprovedLoginIp(
            Integer employeeId,
            Integer adminId,
            String ipAddress) {

        if (employeeId == null
                || adminId == null
                || ipAddress == null
                || ipAddress.isBlank()) {
            return false;
        }

        String normalizedIp = normalizeIp(ipAddress);

        // 1. Check the approved OFFICE IP.
        EmployeeIpAccess officeIp =
                getOfficeIp(adminId, employeeId);

        if (officeIp != null
                && STATUS_APPROVED.equalsIgnoreCase(
                officeIp.getStatus())
                && officeIp.getIpAddress() != null
                && normalizedIp.equalsIgnoreCase(
                normalizeIp(officeIp.getIpAddress()))) {
            return true;
        }

        // 2. Check the approved SECONDARY IP.
        EmployeeIpAccess secondaryIp =
                getSecondaryIp(adminId, employeeId);

        return secondaryIp != null
                && STATUS_APPROVED.equalsIgnoreCase(
                secondaryIp.getStatus())
                && secondaryIp.getIpAddress() != null
                && normalizedIp.equalsIgnoreCase(
                normalizeIp(secondaryIp.getIpAddress()));
    }



    // =====================================================
    // RECORD IP USAGE
    // =====================================================

    @Transactional
    public void recordIpUsage(
            Integer employeeId,
            Integer adminId,
            String ipAddress) {

        if (employeeId == null ||
                adminId == null ||
                ipAddress == null ||
                ipAddress.isBlank()) {

            return;
        }

        String normalizedIp = normalizeIp(ipAddress);

        EmployeeIpAccess ipAccess =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAdminIdAndIpAddress(
                                employeeId,
                                adminId,
                                normalizedIp
                        )
                        .orElse(null);

        if (ipAccess == null) {
            return;
        }

        if (!STATUS_APPROVED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            return;
        }

        LocalDateTime now = LocalDateTime.now();

        ipAccess.setLastUsedAt(now);
        ipAccess.setUpdatedAt(now);

        employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // ASSIGN USAGE POLICY TO IP
    // =====================================================

    /**
     * Assigns an active access policy to an approved IP.
     *
     * Login security and usage security remain separate.
     */
    @Transactional
    public EmployeeIpAccess assignPolicyToIp(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId,
            Long policyId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        if (!STATUS_APPROVED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            throw new IllegalArgumentException(
                    "A usage policy can only be assigned to an approved IP."
            );
        }

        AccessPolicy policy =
                accessPolicyService.getPolicyForAdmin(
                        adminId,
                        policyId
                );

        if (!POLICY_STATUS_ACTIVE.equalsIgnoreCase(
                policy.getStatus())) {

            throw new IllegalArgumentException(
                    "Only an active access policy can be assigned to an IP."
            );
        }

        ipAccess.setPolicyId(policyId);
        ipAccess.setUpdatedAt(LocalDateTime.now());

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // REMOVE USAGE POLICY FROM IP
    // =====================================================

    @Transactional
    public EmployeeIpAccess removePolicyFromIp(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        ipAccess.setPolicyId(null);
        ipAccess.setUpdatedAt(LocalDateTime.now());

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // GET POLICY ID FOR IP - ADMIN SCOPED
    // =====================================================

    @Transactional(readOnly = true)
    public Long getPolicyIdForIp(
            Integer employeeId,
            Integer adminId,
            String ipAddress) {

        if (employeeId == null ||
                adminId == null ||
                ipAddress == null ||
                ipAddress.isBlank()) {

            return null;
        }

        String normalizedIp = normalizeIp(ipAddress);

        EmployeeIpAccess ipAccess =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAdminIdAndIpAddress(
                                employeeId,
                                adminId,
                                normalizedIp
                        )
                        .orElse(null);

        if (ipAccess == null) {
            return null;
        }

        if (!STATUS_APPROVED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            return null;
        }

        return ipAccess.getPolicyId();
    }


    // =====================================================
    // GET POLICY ID FOR EMPLOYEE CURRENT IP
    // =====================================================

    /**
     * Runtime lookup.
     *
     * Example:
     *
     * Employee 6
     * Current IP = 192.168.56.1
     *
     * If:
     *
     * 192.168.56.1 -> APPROVED -> Policy 5
     *
     * then this method returns:
     *
     * 5
     */
    @Transactional(readOnly = true)
    public Long getPolicyIdForIp(
            Integer employeeId,
            String ipAddress) {

        if (employeeId == null ||
                ipAddress == null ||
                ipAddress.isBlank()) {

            return null;
        }

        String normalizedIp =
                normalizeIp(ipAddress);

        EmployeeIpAccess ipAccess =
                employeeIpAccessRepo
                        .findByEmployeeIdAndIpAddress(
                                employeeId,
                                normalizedIp
                        )
                        .orElse(null);

        if (ipAccess == null) {
            return null;
        }

        if (!STATUS_APPROVED.equalsIgnoreCase(
                ipAccess.getStatus())) {

            return null;
        }

        return ipAccess.getPolicyId();
    }


    // =====================================================
    // UPDATE IP LOCATION INFORMATION
    // =====================================================

    @Transactional
    public EmployeeIpAccess updateLocation(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId,
            String country,
            String state,
            String city,
            String isp,
            Double latitude,
            Double longitude) {

        validateEmployeeBelongsToAdmin(
                adminId,
                employeeId
        );

        EmployeeIpAccess ipAccess =
                getIpAccessForAdmin(
                        adminId,
                        employeeId,
                        ipAccessId
                );

        ipAccess.setCountry(country);
        ipAccess.setState(state);
        ipAccess.setCity(city);
        ipAccess.setIsp(isp);
        ipAccess.setLatitude(latitude);
        ipAccess.setLongitude(longitude);
        ipAccess.setUpdatedAt(LocalDateTime.now());

        return employeeIpAccessRepo.save(ipAccess);
    }


    // =====================================================
    // INTERNAL VALIDATION
    // =====================================================

    private User validateEmployeeBelongsToAdmin(
            Integer adminId,
            Integer employeeId) {

        if (adminId == null) {

            throw new IllegalArgumentException(
                    "Administrator ID is required."
            );
        }

        if (employeeId == null) {

            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee was not found."
                                )
                        );

        if (!adminId.equals(
                employee.getAdminId())) {

            throw new IllegalArgumentException(
                    "Employee does not belong to your organization."
            );
        }

        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole())) {

            throw new IllegalArgumentException(
                    "Selected user is not an employee."
            );
        }

        return employee;
    }


    // =====================================================
    // GET IP ACCESS WITH ORGANIZATION VALIDATION
    // =====================================================

    private EmployeeIpAccess getIpAccessForAdmin(
            Integer adminId,
            Integer employeeId,
            Long ipAccessId) {

        EmployeeIpAccess ipAccess =
                employeeIpAccessRepo.findById(ipAccessId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "IP access record was not found."
                                )
                        );

        if (!employeeId.equals(
                ipAccess.getEmployeeId())) {

            throw new IllegalArgumentException(
                    "IP access does not belong to this employee."
            );
        }

        if (!adminId.equals(
                ipAccess.getAdminId())) {

            throw new IllegalArgumentException(
                    "IP access does not belong to your organization."
            );
        }

        return ipAccess;
    }


    // =====================================================
    // NORMALIZE IP
    // =====================================================

    private String normalizeIp(String ipAddress) {

        if (ipAddress == null) {
            return null;
        }

        return ipAddress.trim();
    }


    // =====================================================
    // BASIC IP VALIDATION
    // =====================================================

    private void validateIpFormat(String ipAddress) {

        if (ipAddress == null ||
                ipAddress.isBlank()) {

            throw new IllegalArgumentException(
                    "IP address is required."
            );
        }

        if (ipAddress.length() > 45) {

            throw new IllegalArgumentException(
                    "IP address is too long."
            );
        }

        if (ipAddress.contains(" ")) {

            throw new IllegalArgumentException(
                    "IP address cannot contain spaces."
            );
        }
    }


    // =====================================================
    // AUTOMATICALLY REGISTER UNKNOWN IP AS SECONDARY
    // =====================================================

    /**
     * Registers an employee's first eligible unknown IP
     * as their secondary IP.
     *
     * Call this only after the login flow has confirmed:
     * 1. The employee belongs to the organization.
     * 2. The IP is not the organization's company IP.
     * 3. The IP is not already approved.
     *
     * The employee can never replace an existing secondary IP
     * automatically.
     */
    @Transactional
    public EmployeeIpAccess registerUnknownIpAsSecondary(
            Integer adminId,
            Integer employeeId,
            String ipAddress) {

        validateEmployeeBelongsToAdmin(adminId, employeeId);

        String normalizedIp = normalizeIp(ipAddress);
        validateIpFormat(normalizedIp);

        // Never register the office IP as a secondary IP.
        EmployeeIpAccess officeIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAccessType(
                                employeeId,
                                ACCESS_TYPE_OFFICE
                        )
                        .orElse(null);

        if (officeIp != null) {
            if (!adminId.equals(officeIp.getAdminId())) {
                throw new IllegalArgumentException(
                        "Office IP does not belong to your organization."
                );
            }

            if (officeIp.getIpAddress() != null
                    && officeIp.getIpAddress().equalsIgnoreCase(normalizedIp)) {

                if (STATUS_APPROVED.equalsIgnoreCase(officeIp.getStatus())) {
                    return officeIp;
                }

                throw new IllegalArgumentException(
                        "The office IP is not approved."
                );
            }
        }

        // Check whether this IP already has a record.
        EmployeeIpAccess existingIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndIpAddress(
                                employeeId,
                                normalizedIp
                        )
                        .orElse(null);

        if (existingIp != null) {

            if (!adminId.equals(existingIp.getAdminId())) {
                throw new IllegalArgumentException(
                        "This IP does not belong to your organization."
                );
            }

            if (ACCESS_TYPE_OFFICE.equalsIgnoreCase(
                    existingIp.getAccessType())) {

                throw new IllegalArgumentException(
                        "This IP is already registered as the office IP."
                );
            }

            // An already approved secondary IP can be reused.
            if (ACCESS_TYPE_SECONDARY.equalsIgnoreCase(
                    existingIp.getAccessType())
                    && STATUS_APPROVED.equalsIgnoreCase(
                    existingIp.getStatus())) {

                return existingIp;
            }

            // Do not silently reactivate revoked or rejected IPs.
            throw new IllegalArgumentException(
                    "This IP already has a non-approved record. "
                            + "An administrator must review it."
            );
        }

        // There is only one secondary-IP slot per employee.
        EmployeeIpAccess secondaryIp =
                employeeIpAccessRepo
                        .findByEmployeeIdAndAccessType(
                                employeeId,
                                ACCESS_TYPE_SECONDARY
                        )
                        .orElse(null);

        if (secondaryIp != null) {

            if (!adminId.equals(secondaryIp.getAdminId())) {
                throw new IllegalArgumentException(
                        "Secondary IP does not belong to your organization."
                );
            }

            // Never replace an existing secondary IP automatically.
            if (secondaryIp.getIpAddress() != null
                    && !secondaryIp.getIpAddress().isBlank()) {

                throw new IllegalArgumentException(
                        "The secondary IP slot is already occupied. "
                                + "Contact your administrator to change it."
                );
            }

            // Do not reuse revoked or rejected records automatically.
            if (STATUS_REVOKED.equalsIgnoreCase(secondaryIp.getStatus())
                    || STATUS_REJECTED.equalsIgnoreCase(secondaryIp.getStatus())) {

                throw new IllegalArgumentException(
                        "The secondary IP slot requires administrator review."
                );
            }

        } else {
            long totalRecords =
                    employeeIpAccessRepo.countByEmployeeId(employeeId);

            if (totalRecords >= MAX_IP_ADDRESSES) {
                throw new IllegalArgumentException(
                        "The maximum number of IP records has been reached."
                );
            }

            secondaryIp = new EmployeeIpAccess();
            secondaryIp.setEmployeeId(employeeId);
            secondaryIp.setAdminId(adminId);
            secondaryIp.setAccessType(ACCESS_TYPE_SECONDARY);
            secondaryIp.setCreatedAt(LocalDateTime.now());
        }

        LocalDateTime now = LocalDateTime.now();

        secondaryIp.setIpAddress(normalizedIp);
        secondaryIp.setStatus(STATUS_APPROVED);
        secondaryIp.setApprovedBy(adminId);
        secondaryIp.setApprovedAt(now);
        secondaryIp.setLastUsedAt(now);
        secondaryIp.setUpdatedAt(now);

        // Preserve any policy already attached to a reusable slot.
        return employeeIpAccessRepo.save(secondaryIp);
    }



    // =====================================================
    // CONFIGURATION
    // =====================================================

    public int getMaxIpAddresses() {
        return MAX_IP_ADDRESSES;
    }
}