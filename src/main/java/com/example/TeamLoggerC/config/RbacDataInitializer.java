 package com.example.TeamLoggerC.config;

import com.example.TeamLoggerC.entity.Permission;
import com.example.TeamLoggerC.repository.PermissionRepo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RbacDataInitializer {

    @Bean
    CommandLineRunner initializeRbacPermissions(
            PermissionRepo permissionRepo
    ) {

        return args -> {

            List<Permission> permissions = List.of(

                    // =====================================================
                    // DASHBOARD
                    // =====================================================

                    new Permission(
                            "DASHBOARD_VIEW",
                            "View employee dashboard"
                    ),

                    // =====================================================
                    // TASKS
                    // =====================================================

                    new Permission(
                            "TASK_VIEW",
                            "View daily tasks"
                    ),

                    new Permission(
                            "TASK_CREATE",
                            "Create daily tasks"
                    ),

                    new Permission(
                            "TASK_UPDATE",
                            "Update daily tasks"
                    ),

                    new Permission(
                            "TASK_DELETE",
                            "Delete daily tasks"
                    ),

                    // =====================================================
                    // PROJECTS
                    // =====================================================

                    new Permission(
                            "PROJECT_VIEW",
                            "View assigned projects"
                    ),

                    new Permission(
                            "PROJECT_CREATE",
                            "Create projects"
                    ),

                    new Permission(
                            "PROJECT_UPDATE",
                            "Update projects"
                    ),

                    new Permission(
                            "PROJECT_DELETE",
                            "Delete projects"
                    ),

                    new Permission(
                            "PROJECT_COMPLETE_REQUEST",
                            "Request project completion"
                    ),

                    new Permission(
                            "PROJECT_COMPLETE_APPROVE",
                            "Approve project completion"
                    ),

                    // =====================================================
                    // TEAM
                    // =====================================================

                    new Permission(
                            "TEAM_VIEW",
                            "View team members"
                    ),

                    new Permission(
                            "TEAM_MANAGE",
                            "Manage team members"
                    ),

                    // =====================================================
                    // ACTIVITY
                    // =====================================================

                    new Permission(
                            "ACTIVITY_VIEW",
                            "View activity information"
                    ),

                    new Permission(
                            "ACTIVITY_VIEW_TEAM",
                            "View activity of team members"
                    ),

                    // =====================================================
                    // PROFILE
                    // =====================================================

                    new Permission(
                            "PROFILE_VIEW",
                            "View own profile"
                    ),

                    new Permission(
                            "PROFILE_UPDATE",
                            "Update own profile"
                    ),

                    new Permission(
                            "PASSWORD_CHANGE",
                            "Change own password"
                    ),

                    // =====================================================
                    // CERTIFICATES
                    // =====================================================

                    new Permission(
                            "CERTIFICATE_VIEW",
                            "View own certificates"
                    ),

                    new Permission(
                            "CERTIFICATE_GENERATE",
                            "Generate own certificate"
                    ),

                    // =====================================================
                    // REPORTS
                    // =====================================================

                    new Permission(
                            "REPORT_VIEW",
                            "View reports"
                    ),

                    new Permission(
                            "REPORT_EXPORT",
                            "Export reports"
                    )

            );

            for (Permission permission : permissions) {

                if (!permissionRepo.existsByName(permission.getName())) {
                    permissionRepo.save(permission);
                }
            }
        };
    }
}

