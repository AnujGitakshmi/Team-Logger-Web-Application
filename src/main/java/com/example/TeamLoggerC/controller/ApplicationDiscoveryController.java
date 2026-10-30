package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.DiscoveredApplication;
import com.example.TeamLoggerC.service.DiscoveredApplicationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/agent/applications")
public class ApplicationDiscoveryController {

    private final DiscoveredApplicationService
            discoveredApplicationService;

    public ApplicationDiscoveryController(
            DiscoveredApplicationService discoveredApplicationService
    ) {
        this.discoveredApplicationService =
                discoveredApplicationService;
    }

    /**
     * Desktop Agent reports a discovered application.
     *
     * POST /api/agent/applications/discover
     */
    @PostMapping("/discover")
    public ResponseEntity<?> discoverApplication(
            @RequestBody ApplicationDiscoveryRequest request
    ) {

        try {

            System.out.println("========================================");
            System.out.println("APPLICATION DISCOVERY API CALLED");
            System.out.println("Admin ID          : " + request.getAdminId());
            System.out.println("Application Name  : " + request.getApplicationName());
            System.out.println("Executable Name   : " + request.getExecutableName());
            System.out.println("Publisher         : " + request.getPublisher());
            System.out.println("Version           : " + request.getVersion());
            System.out.println("========================================");

            DiscoveredApplication application =
                    discoveredApplicationService.discoverApplication(
                            request.getAdminId(),
                            request.getApplicationName(),
                            request.getExecutableName(),
                            request.getPublisher(),
                            request.getVersion()
                    );

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    true
            );

            response.put(
                    "message",
                    "Application discovered successfully."
            );

            response.put(
                    "applicationId",
                    application.getId()
            );

            response.put(
                    "applicationName",
                    application.getApplicationName()
            );

            response.put(
                    "executableName",
                    application.getExecutableName()
            );

            response.put(
                    "status",
                    application.getStatus()
            );

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {

            Map<String, Object> response =
                    new HashMap<>();

            response.put(
                    "success",
                    false
            );

            response.put(
                    "message",
                    e.getMessage()
            );

            return ResponseEntity
                    .badRequest()
                    .body(response);
        }
    }


    /**
     * Request received from TeamLogger Desktop Agent.
     */
    public static class ApplicationDiscoveryRequest {

        private Integer adminId;

        private String applicationName;

        private String executableName;

        private String publisher;

        private String version;


        public ApplicationDiscoveryRequest() {
        }


        public Integer getAdminId() {
            return adminId;
        }

        public void setAdminId(Integer adminId) {
            this.adminId = adminId;
        }


        public String getApplicationName() {
            return applicationName;
        }

        public void setApplicationName(String applicationName) {
            this.applicationName = applicationName;
        }


        public String getExecutableName() {
            return executableName;
        }

        public void setExecutableName(String executableName) {
            this.executableName = executableName;
        }


        public String getPublisher() {
            return publisher;
        }

        public void setPublisher(String publisher) {
            this.publisher = publisher;
        }


        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }
    }
}