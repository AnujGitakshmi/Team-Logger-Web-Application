package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.ApplicationPermission;
import com.example.TeamLoggerC.repository.ApplicationPermissionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ApplicationPermissionService {

    private static final String PERMISSION_ALLOW = "ALLOW";
    private static final String PERMISSION_BLOCK = "BLOCK";
    private static final String PERMISSION_RESTRICT = "RESTRICT";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final ApplicationPermissionRepo applicationPermissionRepo;

    public ApplicationPermissionService(
            ApplicationPermissionRepo applicationPermissionRepo
    ) {
        this.applicationPermissionRepo =
                applicationPermissionRepo;
    }


    // =========================================================
    // CREATE
    // =========================================================

    public ApplicationPermission createPermission(
            Long policyId,
            String applicationName,
            String executableName,
            String permission
    ) {

        validatePolicyId(policyId);

        String cleanApplicationName =
                normalizeApplicationName(applicationName);

        String cleanExecutableName =
                normalizeExecutableName(executableName);

        String cleanPermission =
                normalizePermission(permission);

        validateApplicationName(cleanApplicationName);

        validatePermission(cleanPermission);

        if (applicationPermissionRepo
                .existsByPolicyIdAndApplicationName(
                        policyId,
                        cleanApplicationName
                )) {

            throw new IllegalArgumentException(
                    "This application already exists in this policy."
            );
        }

        ApplicationPermission applicationPermission =
                new ApplicationPermission();

        applicationPermission.setPolicyId(policyId);
        applicationPermission.setApplicationName(
                cleanApplicationName
        );
        applicationPermission.setExecutableName(
                cleanExecutableName
        );
        applicationPermission.setPermission(
                cleanPermission
        );
        applicationPermission.setStatus(
                STATUS_ACTIVE
        );

        return applicationPermissionRepo.save(
                applicationPermission
        );
    }


    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<ApplicationPermission>
    getPermissionsForPolicy(Long policyId) {

        validatePolicyId(policyId);

        return applicationPermissionRepo
                .findByPolicyIdOrderByApplicationNameAsc(
                        policyId
                );
    }


    // =========================================================
    // GET ACTIVE
    // =========================================================

    @Transactional(readOnly = true)
    public List<ApplicationPermission>
    getActivePermissionsForPolicy(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return applicationPermissionRepo
                .findByPolicyIdAndStatusOrderByApplicationNameAsc(
                        policyId,
                        STATUS_ACTIVE
                );
    }


    // =========================================================
    // GET SINGLE
    // =========================================================

    @Transactional(readOnly = true)
    public ApplicationPermission getPermissionForPolicy(
            Long policyId,
            Long permissionId
    ) {

        validatePolicyId(policyId);
        validatePermissionId(permissionId);

        return applicationPermissionRepo
                .findByIdAndPolicyId(
                        permissionId,
                        policyId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Application permission not found."
                        )
                );
    }


    // =========================================================
    // UPDATE
    // =========================================================

    public ApplicationPermission updatePermission(
            Long policyId,
            Long permissionId,
            String applicationName,
            String executableName,
            String permission
    ) {

        validatePolicyId(policyId);
        validatePermissionId(permissionId);

        String cleanApplicationName =
                normalizeApplicationName(applicationName);

        String cleanExecutableName =
                normalizeExecutableName(executableName);

        String cleanPermission =
                normalizePermission(permission);

        validateApplicationName(cleanApplicationName);

        validatePermission(cleanPermission);

        ApplicationPermission applicationPermission =
                applicationPermissionRepo
                        .findByIdAndPolicyId(
                                permissionId,
                                policyId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Application permission not found."
                                )
                        );

        boolean applicationNameChanged =
                !applicationPermission
                        .getApplicationName()
                        .equalsIgnoreCase(
                                cleanApplicationName
                        );

        if (applicationNameChanged &&
                applicationPermissionRepo
                        .existsByPolicyIdAndApplicationNameAndIdNot(
                                policyId,
                                cleanApplicationName,
                                permissionId
                        )) {

            throw new IllegalArgumentException(
                    "This application already exists in this policy."
            );
        }

        applicationPermission.setApplicationName(
                cleanApplicationName
        );

        applicationPermission.setExecutableName(
                cleanExecutableName
        );

        applicationPermission.setPermission(
                cleanPermission
        );

        return applicationPermissionRepo.save(
                applicationPermission
        );
    }


    // =========================================================
    // CHANGE PERMISSION
    // =========================================================

    public ApplicationPermission changePermission(
            Long policyId,
            Long permissionId,
            String permission
    ) {

        validatePolicyId(policyId);
        validatePermissionId(permissionId);

        String cleanPermission =
                normalizePermission(permission);

        validatePermission(cleanPermission);

        ApplicationPermission applicationPermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );

        applicationPermission.setPermission(
                cleanPermission
        );

        return applicationPermissionRepo.save(
                applicationPermission
        );
    }


    // =========================================================
    // ACTIVATE
    // =========================================================

    public ApplicationPermission activatePermission(
            Long policyId,
            Long permissionId
    ) {

        ApplicationPermission applicationPermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );

        applicationPermission.setStatus(
                STATUS_ACTIVE
        );

        return applicationPermissionRepo.save(
                applicationPermission
        );
    }


    // =========================================================
    // DEACTIVATE
    // =========================================================

    public ApplicationPermission deactivatePermission(
            Long policyId,
            Long permissionId
    ) {

        ApplicationPermission applicationPermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );

        applicationPermission.setStatus(
                STATUS_INACTIVE
        );

        return applicationPermissionRepo.save(
                applicationPermission
        );
    }


    // =========================================================
    // DELETE
    // =========================================================

    public void deletePermission(
            Long policyId,
            Long permissionId
    ) {

        ApplicationPermission applicationPermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );

        applicationPermissionRepo.delete(
                applicationPermission
        );
    }


    // =========================================================
    // DELETE ALL FOR POLICY
    // =========================================================

    public void deletePermissionsForPolicy(
            Long policyId
    ) {

        validatePolicyId(policyId);

        applicationPermissionRepo.deleteByPolicyId(
                policyId
        );
    }


    // =========================================================
    // COUNTS
    // =========================================================

    @Transactional(readOnly = true)
    public long countPermissions(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return applicationPermissionRepo
                .countByPolicyId(policyId);
    }


    @Transactional(readOnly = true)
    public long countActivePermissions(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return applicationPermissionRepo
                .countByPolicyIdAndStatus(
                        policyId,
                        STATUS_ACTIVE
                );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validatePolicyId(
            Long policyId
    ) {

        if (policyId == null || policyId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid access policy ID."
            );
        }
    }


    private void validatePermissionId(
            Long permissionId
    ) {

        if (permissionId == null || permissionId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid application permission ID."
            );
        }
    }


    private void validateApplicationName(
            String applicationName
    ) {

        if (applicationName == null ||
                applicationName.isBlank()) {

            throw new IllegalArgumentException(
                    "Application name is required."
            );
        }

        if (applicationName.length() > 150) {

            throw new IllegalArgumentException(
                    "Application name cannot exceed 150 characters."
            );
        }
    }


    private void validatePermission(
            String permission
    ) {

        if (permission == null ||
                permission.isBlank()) {

            throw new IllegalArgumentException(
                    "Permission type is required."
            );
        }

        if (!PERMISSION_ALLOW.equals(permission) &&
                !PERMISSION_BLOCK.equals(permission) &&
                !PERMISSION_RESTRICT.equals(permission)) {

            throw new IllegalArgumentException(
                    "Invalid permission type. "
                            + "Allowed values are ALLOW, BLOCK, or RESTRICT."
            );
        }
    }


    // =========================================================
    // NORMALIZATION
    // =========================================================

    private String normalizeApplicationName(
            String applicationName
    ) {

        if (applicationName == null) {
            return null;
        }

        return applicationName
                .trim()
                .replaceAll("\\s+", " ");
    }


    private String normalizeExecutableName(
            String executableName
    ) {

        if (executableName == null) {
            return null;
        }

        String cleaned =
                executableName
                        .trim()
                        .replaceAll("\\s+", " ");

        return cleaned.isBlank()
                ? null
                : cleaned;
    }


    private String normalizePermission(
            String permission
    ) {

        if (permission == null) {
            return null;
        }

        return permission
                .trim()
                .toUpperCase();
    }


    public static final String CATEGORY_GAMING = "GAMING";
    public static final String CATEGORY_ENTERTAINMENT = "ENTERTAINMENT";
    public static final String CATEGORY_SOCIAL_MEDIA = "SOCIAL_MEDIA";
    public static final String CATEGORY_PRODUCTIVITY = "PRODUCTIVITY";
    public static final String CATEGORY_OTHER = "OTHER";

    /*
     * Creates a permission with an optional category.
     * Existing createPermission(...) callers remain compatible.
     */
    public ApplicationPermission createPermission(
            Long policyId,
            String applicationName,
            String executableName,
            String category,
            String permission
    ) {
        validatePolicyId(policyId);

        String cleanApplicationName =
                normalizeApplicationName(applicationName);

        String cleanExecutableName =
                normalizeExecutableName(executableName);

        String cleanCategory = normalizeCategory(category);
        String cleanPermission = normalizePermission(permission);

        validateApplicationName(cleanApplicationName);
        validatePermission(cleanPermission);

        if (applicationPermissionRepo
                .existsByPolicyIdAndApplicationName(
                        policyId,
                        cleanApplicationName
                )) {
            throw new IllegalArgumentException(
                    "This application already exists in this policy."
            );
        }

        ApplicationPermission rule = new ApplicationPermission();

        rule.setPolicyId(policyId);
        rule.setApplicationName(cleanApplicationName);
        rule.setExecutableName(cleanExecutableName);
        rule.setCategory(cleanCategory);
        rule.setPermission(cleanPermission);
        rule.setStatus(STATUS_ACTIVE);

        return applicationPermissionRepo.save(rule);
    }

    /*
     * Updates an existing permission and its category.
     */
    public ApplicationPermission updatePermission(
            Long policyId,
            Long permissionId,
            String applicationName,
            String executableName,
            String category,
            String permission
    ) {
        ApplicationPermission rule =
                getPermissionForPolicy(policyId, permissionId);

        String cleanApplicationName =
                normalizeApplicationName(applicationName);

        String cleanExecutableName =
                normalizeExecutableName(executableName);

        String cleanCategory = normalizeCategory(category);
        String cleanPermission = normalizePermission(permission);

        validateApplicationName(cleanApplicationName);
        validatePermission(cleanPermission);

        boolean nameChanged =
                !rule.getApplicationName()
                        .equalsIgnoreCase(cleanApplicationName);

        if (nameChanged
                && applicationPermissionRepo
                .existsByPolicyIdAndApplicationNameAndIdNot(
                        policyId,
                        cleanApplicationName,
                        permissionId
                )) {
            throw new IllegalArgumentException(
                    "This application already exists in this policy."
            );
        }

        rule.setApplicationName(cleanApplicationName);
        rule.setExecutableName(cleanExecutableName);
        rule.setCategory(cleanCategory);
        rule.setPermission(cleanPermission);

        return applicationPermissionRepo.save(rule);
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }

        String normalized = category.trim()
                .toUpperCase(java.util.Locale.ROOT)
                .replace(' ', '_')
                .replace('-', '_');

        if (!java.util.Set.of(
                CATEGORY_GAMING,
                CATEGORY_ENTERTAINMENT,
                CATEGORY_SOCIAL_MEDIA,
                CATEGORY_PRODUCTIVITY,
                CATEGORY_OTHER
        ).contains(normalized)) {
            throw new IllegalArgumentException(
                    "Invalid category. Allowed values: GAMING, "
                            + "ENTERTAINMENT, SOCIAL_MEDIA, "
                            + "PRODUCTIVITY, OTHER."
            );
        }

        return normalized;
    }

}