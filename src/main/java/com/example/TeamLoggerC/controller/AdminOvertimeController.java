package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.OvertimeRecordDTO;
import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminSettingsService;
import com.example.TeamLoggerC.service.SessionService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/overtime")
public class AdminOvertimeController {

    private final UserRepo userRepo;
    private final SessionService sessionService;
    private final AdminSettingsService adminSettingsService;

    public AdminOvertimeController(
            UserRepo userRepo,
            SessionService sessionService,
            AdminSettingsService adminSettingsService) {

        this.userRepo = userRepo;
        this.sessionService = sessionService;
        this.adminSettingsService = adminSettingsService;
    }


    // =====================================================
    // ADMIN OVERTIME PAGE
    // =====================================================

    @GetMapping
    public String overtime(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        // =================================================
        // COMPANY / ORGANIZATION SETTINGS
        // =================================================

        AdminSettings adminSettings =
                adminSettingsService.getSettings(adminId);

        List<User> employees =
                userRepo.findByAdminIdAndRole(
                        adminId,
                        "EMPLOYEE"
                );

        Map<Integer, Integer> todayOvertime =
                new LinkedHashMap<>();

        Map<Integer, Integer> weekOvertime =
                new LinkedHashMap<>();

        Map<Integer, Integer> monthOvertime =
                new LinkedHashMap<>();

        for (User employee : employees) {

            Integer employeeId =
                    employee.getId();

            todayOvertime.put(
                    employeeId,
                    sessionService.getTodayOvertimeMinutes(employeeId)
            );

            weekOvertime.put(
                    employeeId,
                    sessionService.getThisWeekOvertimeMinutes(employeeId)
            );

            monthOvertime.put(
                    employeeId,
                    sessionService.getThisMonthOvertimeMinutes(employeeId)
            );
        }

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "adminSettings",
                adminSettings
        );

        model.addAttribute(
                "employees",
                employees
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

        return "adminOvertime";
    }


    // =====================================================
    // EMPLOYEE OVERTIME DETAILS
    // =====================================================

    @GetMapping("/employee/{employeeId}")
    public String employeeOvertime(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        // =================================================
        // COMPANY / ORGANIZATION SETTINGS
        // =================================================

        AdminSettings adminSettings =
                adminSettingsService.getSettings(adminId);


        // -------------------------------------------------
        // FIND EMPLOYEE
        // -------------------------------------------------

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );


        // -------------------------------------------------
        // ROLE CHECK
        // -------------------------------------------------

        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole())) {

            throw new RuntimeException(
                    "Invalid employee"
            );
        }


        // -------------------------------------------------
        // ADMIN OWNERSHIP CHECK
        // -------------------------------------------------

        if (employee.getAdminId() == null ||
                !employee.getAdminId().equals(adminId)) {

            throw new RuntimeException(
                    "You are not authorized to view this employee"
            );
        }


        // -------------------------------------------------
        // OVERTIME HISTORY
        // -------------------------------------------------

        List<OvertimeRecordDTO> overtimeHistory =
                sessionService.getOvertimeHistory(
                        employeeId
                );


        // -------------------------------------------------
        // SUMMARY
        // -------------------------------------------------

        int todayOvertime =
                sessionService.getTodayOvertimeMinutes(
                        employeeId
                );

        int weekOvertime =
                sessionService.getThisWeekOvertimeMinutes(
                        employeeId
                );

        int monthOvertime =
                sessionService.getThisMonthOvertimeMinutes(
                        employeeId
                );


        // -------------------------------------------------
        // MODEL
        // -------------------------------------------------

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "adminSettings",
                adminSettings
        );

        model.addAttribute(
                "employee",
                employee
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
                overtimeHistory
        );


        // templates/admin/overtime-details.html
        return "overtime-details";
    }
}