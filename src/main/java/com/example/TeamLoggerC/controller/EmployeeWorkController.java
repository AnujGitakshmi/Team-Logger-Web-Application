
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

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        sessionService.startWork(user.getId());

        return "redirect:/employee/dashboard";
    }

    @PostMapping("/stop")
    public String stopWork(Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        sessionService.stopWork(user.getId());

        return "redirect:/employee/dashboard";
    }
}
