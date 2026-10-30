package com.example.TeamLoggerC.config;

import com.example.TeamLoggerC.entity.HierarchyPermission;
import com.example.TeamLoggerC.entity.Permission;
import com.example.TeamLoggerC.repository.HierarchyPermissionRepo;
import com.example.TeamLoggerC.repository.PermissionRepo;
import com.example.TeamLoggerC.security.rbac.EmployeeLevel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RbacHierarchyInitializer {

    @Bean
    CommandLineRunner initializeHierarchyPermissions(
            PermissionRepo permissionRepo,
            HierarchyPermissionRepo hierarchyPermissionRepo
    ) {

        return args -> {

            // =========================================================
            // PROJECT LEADER
            // =========================================================

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "DASHBOARD_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TASK_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TASK_CREATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TASK_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TASK_DELETE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_CREATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_DELETE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_COMPLETE_REQUEST",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROJECT_COMPLETE_APPROVE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TEAM_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "TEAM_MANAGE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "ACTIVITY_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "ACTIVITY_VIEW_TEAM",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROFILE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PROFILE_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "PASSWORD_CHANGE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "CERTIFICATE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "CERTIFICATE_GENERATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "REPORT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.PROJECT_LEADER,
                    "REPORT_EXPORT",
                    permissionRepo,
                    hierarchyPermissionRepo
            );


            // =========================================================
            // SENIOR
            // =========================================================

            addPermission(
                    EmployeeLevel.SENIOR,
                    "DASHBOARD_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "TASK_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "TASK_CREATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "TASK_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "TASK_DELETE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "PROJECT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "TEAM_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "ACTIVITY_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "ACTIVITY_VIEW_TEAM",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "PROFILE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "PROFILE_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "PASSWORD_CHANGE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "CERTIFICATE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "CERTIFICATE_GENERATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "REPORT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.SENIOR,
                    "REPORT_EXPORT",
                    permissionRepo,
                    hierarchyPermissionRepo
            );


            // =========================================================
            // JUNIOR
            // =========================================================

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "DASHBOARD_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "TASK_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "TASK_CREATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "TASK_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "PROJECT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "TEAM_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "ACTIVITY_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "PROFILE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "PROFILE_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "PASSWORD_CHANGE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "CERTIFICATE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.JUNIOR,
                    "CERTIFICATE_GENERATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );


            // =========================================================
            // INTERN
            // =========================================================

            addPermission(
                    EmployeeLevel.INTERN,
                    "DASHBOARD_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "TASK_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "TASK_CREATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "PROJECT_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "TEAM_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "ACTIVITY_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "PROFILE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "PROFILE_UPDATE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "PASSWORD_CHANGE",
                    permissionRepo,
                    hierarchyPermissionRepo
            );

            addPermission(
                    EmployeeLevel.INTERN,
                    "CERTIFICATE_VIEW",
                    permissionRepo,
                    hierarchyPermissionRepo
            );
        };
    }

    private void addPermission(
            EmployeeLevel level,
            String permissionName,
            PermissionRepo permissionRepo,
            HierarchyPermissionRepo hierarchyPermissionRepo
    ) {

        if (
                hierarchyPermissionRepo
                        .existsByHierarchyLevelAndPermission_Name(
                                level,
                                permissionName
                        )
        ) {
            return;
        }

        Permission permission = permissionRepo
                .findByName(permissionName)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "RBAC permission not found: "
                                        + permissionName
                        )
                );

        HierarchyPermission hierarchyPermission =
                new HierarchyPermission(
                        level,
                        permission
                );

        hierarchyPermissionRepo.save(hierarchyPermission);
    }
}

