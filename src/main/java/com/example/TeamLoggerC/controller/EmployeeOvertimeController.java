package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.SessionService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/employee")
public class EmployeeOvertimeController {

    private final SessionService sessionService;

    public EmployeeOvertimeController(
            SessionService sessionService) {

        this.sessionService = sessionService;
    }


    // ==========================================
    // EMPLOYEE OVERTIME PAGE
    // ==========================================

    @GetMapping("/overtime")
    public String overtime(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user =
                userDetails.getUser();

        Integer userId =
                user.getId();


        // ======================================
        // OVERTIME DATA
        // ======================================

        int todayOvertime =
                sessionService.getTodayOvertimeMinutes(userId);

        int weekOvertime =
                sessionService.getThisWeekOvertimeMinutes(userId);

        int monthOvertime =
                sessionService.getThisMonthOvertimeMinutes(userId);


        model.addAttribute(
                "user",
                user
        );

        model.addAttribute(
                "todayOvertime",
                todayOvertime
        );

        model.addAttribute(
                "weekOvertime",
                weekOvertime
        );

        model.addAttribute(
                "monthOvertime",
                monthOvertime
        );

        model.addAttribute(
                "overtimeHistory",
                sessionService.getOvertimeHistory(userId)
        );


        return "employee-overtime";
    }
}