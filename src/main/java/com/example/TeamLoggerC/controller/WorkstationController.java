
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.SessionService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/workstation")
public class WorkstationController {

    private final SessionService sessionService;

    public WorkstationController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping
    public String showWorkstation(
            Authentication authentication,
            Model model) {

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

        model.addAttribute("user", user);

        boolean isEmployee =
                "EMPLOYEE".equalsIgnoreCase(String.valueOf(user.getRole()));

        model.addAttribute("isEmployee", isEmployee);

        if (isEmployee) {

            Session currentSession =
                    sessionService.getCurrentSession(user.getId());

            boolean workStarted =
                    currentSession != null &&
                            currentSession.getEmployeeStartedAt() != null;

            model.addAttribute("currentSession", currentSession);
            model.addAttribute("workStarted", workStarted);
        } else {
            model.addAttribute("currentSession", null);
            model.addAttribute("workStarted", false);
        }

        return "workstation";
    }
}
