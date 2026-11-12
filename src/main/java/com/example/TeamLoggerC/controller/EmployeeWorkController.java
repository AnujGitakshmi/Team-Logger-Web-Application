package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.SessionService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee/work")
public class EmployeeWorkController {

    private final SessionService sessionService;

    public EmployeeWorkController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/start")
    public String startWork(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        if (user == null ||
                !"ACTIVE".equalsIgnoreCase(String.valueOf(user.getStatus()))) {
            return "redirect:/login";
        }

        if (!"EMPLOYEE".equalsIgnoreCase(String.valueOf(user.getRole()))) {
            return "redirect:/workstation";
        }

        // Prevent starting work before workstation activation
        if (!user.isWorkstationActivated()) {
            return "redirect:/employee/dashboard?error=workstation-not-activated";
        }

        sessionService.startWork(user.getId());

        return "redirect:/workstation";
    }

    @PostMapping("/stop")
    public String stopWork(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        if (user == null ||
                !"ACTIVE".equalsIgnoreCase(String.valueOf(user.getStatus()))) {
            return "redirect:/login";
        }

        if (!"EMPLOYEE".equalsIgnoreCase(String.valueOf(user.getRole()))) {
            return "redirect:/workstation";
        }

        sessionService.stopWork(user.getId());

        return "redirect:/workstation";
    }
}