package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AccessPolicy;
import com.example.TeamLoggerC.entity.EmployeeAccessPolicy;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.AccessPolicyRepo;
import com.example.TeamLoggerC.repository.EmployeeAccessPolicyRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class EmployeeAccessPolicyService {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private final EmployeeAccessPolicyRepo employeeAccessPolicyRepo;
    private final AccessPolicyRepo accessPolicyRepo;
    private final UserRepo userRepo;

    public EmployeeAccessPolicyService(
            EmployeeAccessPolicyRepo employeeAccessPolicyRepo,
            AccessPolicyRepo accessPolicyRepo,
            UserRepo userRepo
    ) {
        this.employeeAccessPolicyRepo = employeeAccessPolicyRepo;
        this.accessPolicyRepo = accessPolicyRepo;
        this.userRepo = userRepo;
    }

    // =====================================================
    // GET EMPLOYEE POLICY
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<EmployeeAccessPolicy> getEmployeePolicy(
            Integer adminId,
            Integer employeeId
    ) {

        if (adminId == null || employeeId == null) {
            return Optional.empty();
        }

        User employee = validateEmployee(adminId, employeeId);

        return employeeAccessPolicyRepo
                .findByEmployeeId(employee.getId());
    }

    // =====================================================
    // GET ACTIVE EMPLOYEE POLICY
    // =====================================================

    @Transactional(readOnly = true)
    public Optional<EmployeeAccessPolicy> getActiveEmployeePolicy(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return Optional.empty();
        }

        return employeeAccessPolicyRepo
                .findByEmployeeIdAndStatus(
                        employeeId,
                        STATUS_ACTIVE
                );
    }

    // =====================================================
    // GET ACTIVE POLICY FOR EMPLOYEE
    // =====================================================

    @Transactional(readOnly = true)
    public EmployeeAccessPolicy getActivePolicyForEmployee(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return null;
        }

        return employeeAccessPolicyRepo
                .findByEmployeeIdAndStatus(
                        employeeId,
                        STATUS_ACTIVE
                )
                .orElse(null);
    }

    // =====================================================
    // ASSIGN POLICY
    // =====================================================

    public EmployeeAccessPolicy assignPolicy(
            Integer adminId,
            Integer employeeId,
            Long policyId
    ) {

        if (adminId == null) {
            throw new IllegalArgumentException(
                    "Admin ID is required."
            );
        }

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        if (policyId == null) {
            throw new IllegalArgumentException(
                    "Policy ID is required."
            );
        }

        User employee =
                validateEmployee(
                        adminId,
                        employeeId
                );

        AccessPolicy policy =
                accessPolicyRepo
                        .findByIdAndAdminId(
                                policyId,
                                adminId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Access policy not found."
                                )
                        );

        if (!STATUS_ACTIVE.equalsIgnoreCase(
                policy.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Cannot assign an inactive access policy."
            );
        }

        EmployeeAccessPolicy assignment =
                employeeAccessPolicyRepo
                        .findByEmployeeId(employee.getId())
                        .orElse(null);

        if (assignment == null) {

            assignment =
                    new EmployeeAccessPolicy();

            assignment.setEmployeeId(
                    employee.getId()
            );
        }

        assignment.setPolicyId(policyId);
        assignment.setAssignedBy(adminId);
        assignment.setAssignedAt(
                java.time.LocalDateTime.now()
        );
        assignment.setStatus(
                STATUS_ACTIVE
        );

        return employeeAccessPolicyRepo.save(
                assignment
        );
    }

    // =====================================================
    // CHANGE POLICY
    // =====================================================

    public EmployeeAccessPolicy changePolicy(
            Integer adminId,
            Integer employeeId,
            Long policyId
    ) {

        return assignPolicy(
                adminId,
                employeeId,
                policyId
        );
    }

    // =====================================================
    // REMOVE POLICY
    // =====================================================

    public void removePolicy(
            Integer adminId,
            Integer employeeId
    ) {

        validateEmployee(
                adminId,
                employeeId
        );

        EmployeeAccessPolicy assignment =
                employeeAccessPolicyRepo
                        .findByEmployeeId(employeeId)
                        .orElse(null);

        if (assignment == null) {
            return;
        }

        assignment.setStatus(
                STATUS_INACTIVE
        );

        employeeAccessPolicyRepo.save(
                assignment
        );
    }

    // =====================================================
    // ACTIVATE POLICY
    // =====================================================

    public EmployeeAccessPolicy activatePolicy(
            Integer adminId,
            Integer employeeId
    ) {

        validateEmployee(
                adminId,
                employeeId
        );

        EmployeeAccessPolicy assignment =
                employeeAccessPolicyRepo
                        .findByEmployeeId(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No access policy is assigned to this employee."
                                )
                        );

        AccessPolicy policy =
                accessPolicyRepo
                        .findByIdAndAdminId(
                                assignment.getPolicyId(),
                                adminId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Access policy not found."
                                )
                        );

        if (!STATUS_ACTIVE.equalsIgnoreCase(
                policy.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Cannot activate assignment because the access policy is inactive."
            );
        }

        assignment.setStatus(
                STATUS_ACTIVE
        );

        return employeeAccessPolicyRepo.save(
                assignment
        );
    }

    // =====================================================
    // DEACTIVATE POLICY
    // =====================================================

    public EmployeeAccessPolicy deactivatePolicy(
            Integer adminId,
            Integer employeeId
    ) {

        validateEmployee(
                adminId,
                employeeId
        );

        EmployeeAccessPolicy assignment =
                employeeAccessPolicyRepo
                        .findByEmployeeId(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "No access policy is assigned to this employee."
                                )
                        );

        assignment.setStatus(
                STATUS_INACTIVE
        );

        return employeeAccessPolicyRepo.save(
                assignment
        );
    }

    // =====================================================
    // HAS ACTIVE POLICY
    // =====================================================

    @Transactional(readOnly = true)
    public boolean hasActivePolicy(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return false;
        }

        return employeeAccessPolicyRepo
                .existsByEmployeeIdAndStatus(
                        employeeId,
                        STATUS_ACTIVE
                );
    }

    // =====================================================
    // GET EMPLOYEES USING POLICY
    // =====================================================

    @Transactional(readOnly = true)
    public List<EmployeeAccessPolicy> getEmployeesUsingPolicy(
            Long policyId
    ) {

        if (policyId == null) {
            return List.of();
        }

        return employeeAccessPolicyRepo
                .findByPolicyIdAndStatus(
                        policyId,
                        STATUS_ACTIVE
                );
    }

    // =====================================================
    // COUNT EMPLOYEES USING POLICY
    // =====================================================

    @Transactional(readOnly = true)
    public long countEmployeesUsingPolicy(
            Long policyId
    ) {

        if (policyId == null) {
            return 0;
        }

        return employeeAccessPolicyRepo
                .countByPolicyIdAndStatus(
                        policyId,
                        STATUS_ACTIVE
                );
    }

    // =====================================================
    // VALIDATE EMPLOYEE
    // =====================================================

    @Transactional(readOnly = true)
    private User validateEmployee(
            Integer adminId,
            Integer employeeId
    ) {

        User employee =
                userRepo
                        .findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        if (!ROLE_EMPLOYEE.equalsIgnoreCase(
                employee.getRole()
        )) {

            throw new IllegalArgumentException(
                    "Selected user is not an employee."
            );
        }

        if (
                employee.getAdminId() == null
                        ||
                        !employee.getAdminId().equals(adminId)
        ) {

            throw new IllegalArgumentException(
                    "Employee does not belong to this admin."
            );
        }

        return employee;
    }
}