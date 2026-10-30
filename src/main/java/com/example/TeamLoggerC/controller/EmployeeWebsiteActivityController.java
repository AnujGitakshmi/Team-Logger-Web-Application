
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.WebsiteActivityRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.WebsiteActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/employee/website-activity")
public class EmployeeWebsiteActivityController {

    private final WebsiteActivityService websiteActivityService;

    public EmployeeWebsiteActivityController(
            WebsiteActivityService websiteActivityService) {
        this.websiteActivityService = websiteActivityService;
    }

    @PostMapping
    public ResponseEntity<?> recordWebsiteActivity(
            @RequestBody WebsiteActivityRequest request,
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Please log in to TeamLoggerC."));
        }

        User employee = userDetails.getUser();

        if (employee == null
                || employee.getRole() == null
                || !employee.getRole().equalsIgnoreCase("EMPLOYEE")) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Only authenticated employees can submit activity."));
        }

        if (employee.getAdminId() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "No administrator is assigned to this employee."));
        }

        if (request == null
                || request.getWebsiteName() == null
                || request.getWebsiteName().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Website name is required."));
        }

        websiteActivityService.saveActivity(
                employee.getId().longValue(),
                employee.getAdminId().longValue(),
                request.getBrowser(),
                request.getWebsiteName(),
                request.getUrl(),
                request.getPageTitle()
        );

        return ResponseEntity.ok(
                Map.of("message", "Website activity recorded."));
    }
}
