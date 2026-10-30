 package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.WebsiteActivityRequest;
import com.example.TeamLoggerC.entity.BrowserExtensionToken;
import com.example.TeamLoggerC.service.BrowserExtensionTokenService;
import com.example.TeamLoggerC.service.WebsiteActivityService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/extension/website-activity")
public class ExtensionWebsiteActivityController {

    private final BrowserExtensionTokenService tokenService;
    private final WebsiteActivityService websiteActivityService;

    public ExtensionWebsiteActivityController(
            BrowserExtensionTokenService tokenService,
            WebsiteActivityService websiteActivityService
    ) {
        this.tokenService = tokenService;
        this.websiteActivityService = websiteActivityService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> recordWebsiteActivity(
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String authorizationHeader,
            @RequestBody(required = false)
            WebsiteActivityRequest request
    ) {

        // 1. Require a Bearer token.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "A valid Bearer token is required."
            );
        }

        String rawToken = authorizationHeader
                .substring(7)
                .trim();

        if (rawToken.isBlank()) {
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "A valid Bearer token is required."
            );
        }

        // 2. Validate the extension token.
        Optional<BrowserExtensionToken> tokenOptional =
                tokenService.validateToken(rawToken);

        if (tokenOptional.isEmpty()) {
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "The extension token is invalid, expired, or revoked."
            );
        }

        BrowserExtensionToken token = tokenOptional.get();

        // 3. Validate the request payload.
        if (request == null
                || request.getWebsiteName() == null
                || request.getWebsiteName().isBlank()) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "Website name is required."
            );
        }

        // 4. Obtain employee and administrator IDs from the token.
        Integer employeeId = token.getEmployeeId();
        Integer adminId = token.getAdminId();

        if (employeeId == null || adminId == null) {
            return error(
                    HttpStatus.UNAUTHORIZED,
                    "The extension token has invalid account information."
            );
        }

        // 5. Save activity under the token's employee and administrator.
        websiteActivityService.saveActivity(
                employeeId.longValue(),
                adminId.longValue(),
                request.getBrowser(),
                request.getWebsiteName().trim(),
                request.getUrl(),
                request.getPageTitle()
        );

        return ResponseEntity.ok(Map.of(
                "message",
                "Website activity recorded successfully."
        ));
    }

    private ResponseEntity<Map<String, String>> error(
            HttpStatus status,
            String message
    ) {
        return ResponseEntity.status(status).body(
                Map.of("error", message)
        );
    }
}
