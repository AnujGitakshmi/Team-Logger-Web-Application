 package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.HierarchyPermission;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.HierarchyPermissionRepo;
import com.example.TeamLoggerC.security.rbac.EmployeeLevel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class PermissionService {

    private final HierarchyPermissionRepo hierarchyPermissionRepo;

    public PermissionService(
            HierarchyPermissionRepo hierarchyPermissionRepo
    ) {
        this.hierarchyPermissionRepo = hierarchyPermissionRepo;
    }

    /**
     * Converts the employee's existing domain-specific designation
     * into the canonical RBAC hierarchy level.
     *
     * Examples:
     *
     * Project Leader        -> PROJECT_LEADER
     * QA Lead               -> PROJECT_LEADER
     * Senior Developer      -> SENIOR
     * Senior QA Engineer    -> SENIOR
     * Junior Developer      -> JUNIOR
     * Developer Intern      -> INTERN
     */
    public EmployeeLevel getEmployeeLevel(User user) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null"
            );
        }

        String designation = user.getDesignation();

        if (designation == null || designation.isBlank()) {
            throw new IllegalStateException(
                    "User designation is missing for user ID: "
                            + user.getId()
            );
        }

        String normalizedDesignation =
                designation.trim()
                        .toLowerCase(Locale.ROOT);

        return resolveEmployeeLevel(normalizedDesignation);
    }

    /**
     * Returns all permission names assigned to the employee's
     * hierarchy level.
     */
    public Set<String> getPermissions(User user) {

        EmployeeLevel level = getEmployeeLevel(user);

        List<HierarchyPermission> mappings =
                hierarchyPermissionRepo.findByHierarchyLevel(level);

        if (mappings == null || mappings.isEmpty()) {
            return Collections.emptySet();
        }

        return mappings.stream()
                .filter(mapping -> mapping.getPermission() != null)
                .map(mapping ->
                        mapping.getPermission()
                                .getName()
                                .trim()
                                .toUpperCase(Locale.ROOT)
                )
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Checks whether an employee has a particular permission.
     */
    public boolean hasPermission(
            User user,
            String permissionName
    ) {

        if (permissionName == null || permissionName.isBlank()) {
            return false;
        }

        String normalizedPermission =
                permissionName.trim()
                        .toUpperCase(Locale.ROOT);

        return getPermissions(user)
                .contains(normalizedPermission);
    }

    /**
     * Converts domain-specific designations into the canonical
     * RBAC hierarchy.
     */
    private EmployeeLevel resolveEmployeeLevel(
            String designation
    ) {

        // =========================================================
        // PROJECT LEADER LEVEL
        // =========================================================

        if (designation.equals("project leader")
                || designation.equals("qa lead")
                || designation.equals("hr head")
                || designation.equals("finance lead")
                || designation.equals("ui/ux lead")) {

            return EmployeeLevel.PROJECT_LEADER;
        }

        // =========================================================
        // SENIOR LEVEL
        // =========================================================

        if (designation.equals("senior developer")
                || designation.equals("senior qa engineer")
                || designation.equals("senior hr")
                || designation.equals("senior finance executive")
                || designation.equals("senior ui/ux designer")) {

            return EmployeeLevel.SENIOR;
        }

        // =========================================================
        // JUNIOR LEVEL
        // =========================================================

        if (designation.equals("junior developer")
                || designation.equals("junior qa engineer")
                || designation.equals("junior hr")
                || designation.equals("junior finance executive")
                || designation.equals("junior ui/ux designer")) {

            return EmployeeLevel.JUNIOR;
        }

        // =========================================================
        // INTERN LEVEL
        // =========================================================

        if (designation.equals("developer intern")
                || designation.equals("qa intern")
                || designation.equals("hr intern")
                || designation.equals("finance intern")
                || designation.equals("ui/ux intern")) {

            return EmployeeLevel.INTERN;
        }

        throw new IllegalStateException(
                "Unknown employee designation: "
                        + designation
        );
    }
}

