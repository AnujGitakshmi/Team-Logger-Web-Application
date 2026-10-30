package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.security.rbac.EmployeeLevel;
import com.example.TeamLoggerC.service.PermissionService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RbacTestController {

    private final PermissionService permissionService;

    public RbacTestController(
            PermissionService permissionService
    ) {
        this.permissionService = permissionService;
    }

    @GetMapping("/api/rbac/test")
    public String testRbac(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {

        if (userDetails == null) {
            return "Not authenticated";
        }

        User user = userDetails.getUser();

        EmployeeLevel level =
                permissionService.getEmployeeLevel(user);

        return "Employee: " + user.getName()
                + " | Designation: " + user.getDesignation()
                + " | Level: " + level
                + " | Permissions: "
                + permissionService.getPermissions(user);
    }
}

