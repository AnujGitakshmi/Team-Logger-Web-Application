package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee/workstation")
public class WorkstationActivationController {

    private final UserRepo userRepo;

    public WorkstationActivationController(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/activate")
    public String activateWorkstation(Authentication authentication) {

        if (authentication == null ||
                !(authentication.getPrincipal() instanceof CustomUserDetails)) {
            return "redirect:/login";
        }

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        if (user == null ||
                !"EMPLOYEE".equalsIgnoreCase(user.getRole()) ||
                !"ACTIVE".equalsIgnoreCase(String.valueOf(user.getStatus()))) {
            return "redirect:/login";
        }


        if (!user.isWorkstationActivated()) {
            user.setWorkstationActivated(true);
            userRepo.save(user);
        }

        return "redirect:/workstation";
    }
}