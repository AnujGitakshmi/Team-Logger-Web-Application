package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.service.CurrentNetworkIpService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CurrentNetworkIpTestController {

    private final CurrentNetworkIpService currentNetworkIpService;

    public CurrentNetworkIpTestController(
            CurrentNetworkIpService currentNetworkIpService) {

        this.currentNetworkIpService = currentNetworkIpService;
    }

    @GetMapping("/api/test/current-ip")
    public Map<String, Object> getCurrentIp() {

        String currentIp =
                currentNetworkIpService.getCurrentIpv4Address();

        return Map.of(
                "success", currentIp != null,
                "currentIp", currentIp != null ? currentIp : "NOT_FOUND"
        );
    }
}