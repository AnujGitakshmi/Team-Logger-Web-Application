
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.entity.WebsiteActivity;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.WebsiteActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/admin/website-activity")
public class WebsiteActivityController {

    private final WebsiteActivityService websiteActivityService;

    public WebsiteActivityController(
            WebsiteActivityService websiteActivityService) {
        this.websiteActivityService = websiteActivityService;
    }

    @GetMapping
    public List<WebsiteActivity> getWebsiteActivity(
            Authentication authentication) {

        if (authentication == null ||
                !authentication.isAuthenticated() ||
                !(authentication.getPrincipal()
                        instanceof CustomUserDetails userDetails)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in first.");
        }

        User user = userDetails.getUser();

        if (user == null ||
                user.getRole() == null ||
                !user.getRole().equalsIgnoreCase("ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only administrators can view website activity.");
        }

        Long adminId = user.getId().longValue();

        return websiteActivityService
                .getRecentActivityForAdmin(adminId);
    }
}
