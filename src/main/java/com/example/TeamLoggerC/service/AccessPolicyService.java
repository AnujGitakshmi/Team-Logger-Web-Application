package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AccessPolicy;
import com.example.TeamLoggerC.repository.AccessPolicyRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AccessPolicyService {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final AccessPolicyRepo accessPolicyRepo;

    public AccessPolicyService(AccessPolicyRepo accessPolicyRepo) {
        this.accessPolicyRepo = accessPolicyRepo;
    }


    // =========================================================
    // CREATE POLICY
    // =========================================================

    public AccessPolicy createPolicy(
            Integer adminId,
            String name,
            String description
    ) {

        validateAdminId(adminId);

        String cleanName = normalizeName(name);
        String cleanDescription = normalizeDescription(description);

        validatePolicyName(cleanName);

        if (accessPolicyRepo.existsByAdminIdAndName(
                adminId,
                cleanName
        )) {

            throw new IllegalArgumentException(
                    "A policy with this name already exists."
            );
        }

        AccessPolicy policy = new AccessPolicy();

        policy.setAdminId(adminId);
        policy.setName(cleanName);
        policy.setDescription(cleanDescription);
        policy.setStatus(STATUS_ACTIVE);

        return accessPolicyRepo.save(policy);
    }


    // =========================================================
    // GET ALL POLICIES FOR ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public List<AccessPolicy> getPoliciesForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return accessPolicyRepo
                .findByAdminIdOrderByCreatedAtDesc(adminId);
    }


    // =========================================================
    // GET ACTIVE POLICIES
    // =========================================================

    @Transactional(readOnly = true)
    public List<AccessPolicy> getActivePoliciesForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return accessPolicyRepo
                .findByAdminIdAndStatusOrderByCreatedAtDesc(
                        adminId,
                        STATUS_ACTIVE
                );
    }


    // =========================================================
    // GET ONE POLICY
    // =========================================================

    @Transactional(readOnly = true)
    public AccessPolicy getPolicyForAdmin(
            Integer adminId,
            Long policyId
    ) {

        validateAdminId(adminId);
        validatePolicyId(policyId);

        return accessPolicyRepo
                .findByIdAndAdminId(policyId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Access policy not found."
                        )
                );
    }


    // =========================================================
    // UPDATE POLICY
    // =========================================================

    public AccessPolicy updatePolicy(
            Integer adminId,
            Long policyId,
            String name,
            String description
    ) {

        validateAdminId(adminId);
        validatePolicyId(policyId);

        String cleanName = normalizeName(name);
        String cleanDescription = normalizeDescription(description);

        validatePolicyName(cleanName);

        AccessPolicy policy = accessPolicyRepo
                .findByIdAndAdminId(policyId, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Access policy not found."
                        )
                );

        boolean nameChanged =
                !policy.getName().equalsIgnoreCase(cleanName);

        if (nameChanged &&
                accessPolicyRepo.existsByAdminIdAndNameAndIdNot(
                        adminId,
                        cleanName,
                        policyId
                )) {

            throw new IllegalArgumentException(
                    "A policy with this name already exists."
            );
        }

        policy.setName(cleanName);
        policy.setDescription(cleanDescription);

        return accessPolicyRepo.save(policy);
    }


    // =========================================================
    // ACTIVATE POLICY
    // =========================================================

    public AccessPolicy activatePolicy(
            Integer adminId,
            Long policyId
    ) {

        AccessPolicy policy =
                getPolicyForAdmin(adminId, policyId);

        policy.setStatus(STATUS_ACTIVE);

        return accessPolicyRepo.save(policy);
    }


    // =========================================================
    // DEACTIVATE POLICY
    // =========================================================

    public AccessPolicy deactivatePolicy(
            Integer adminId,
            Long policyId
    ) {

        AccessPolicy policy =
                getPolicyForAdmin(adminId, policyId);

        policy.setStatus(STATUS_INACTIVE);

        return accessPolicyRepo.save(policy);
    }


    // =========================================================
    // DELETE POLICY
    // =========================================================

    public void deletePolicy(
            Integer adminId,
            Long policyId
    ) {

        AccessPolicy policy =
                getPolicyForAdmin(adminId, policyId);

        accessPolicyRepo.delete(policy);
    }


    // =========================================================
    // COUNT POLICIES
    // =========================================================

    @Transactional(readOnly = true)
    public long countPolicies(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return accessPolicyRepo.countByAdminId(adminId);
    }


    // =========================================================
    // COUNT ACTIVE POLICIES
    // =========================================================

    @Transactional(readOnly = true)
    public long countActivePolicies(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return accessPolicyRepo.countByAdminIdAndStatus(
                adminId,
                STATUS_ACTIVE
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateAdminId(Integer adminId) {

        if (adminId == null || adminId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid admin ID."
            );
        }
    }


    private void validatePolicyId(Long policyId) {

        if (policyId == null || policyId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid policy ID."
            );
        }
    }


    private void validatePolicyName(String name) {

        if (name == null || name.isBlank()) {

            throw new IllegalArgumentException(
                    "Policy name is required."
            );
        }

        if (name.length() > 100) {

            throw new IllegalArgumentException(
                    "Policy name cannot exceed 100 characters."
            );
        }
    }


    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeName(String name) {

        if (name == null) {
            return null;
        }

        return name.trim()
                .replaceAll("\\s+", " ");
    }


    private String normalizeDescription(
            String description
    ) {

        if (description == null) {
            return null;
        }

        String cleaned = description.trim();

        if (cleaned.isBlank()) {
            return null;
        }

        if (cleaned.length() > 500) {

            throw new IllegalArgumentException(
                    "Policy description cannot exceed 500 characters."
            );
        }

        return cleaned;
    }
}