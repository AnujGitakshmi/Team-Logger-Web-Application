package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.EmployeeDailyReportDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.SessionRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminReportPdfService;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportsController {


    private static final int REGULAR_WORK_MINUTES = 8 * 60;

    private final UserRepo userRepo;
    private final SessionRepo sessionRepo;
    private final AdminReportPdfService adminReportPdfService;


    public AdminReportsController(
            UserRepo userRepo,
            SessionRepo sessionRepo,
            AdminReportPdfService adminReportPdfService) {

        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.adminReportPdfService =
                adminReportPdfService;
    }


// =====================================================
// REPORT PAGE
// =====================================================

    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_REPORT_VIEW')")
    @GetMapping
    public String reports(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        List<User> employees =
                userRepo.findByAdminIdAndRole(
                        adminId,
                        "EMPLOYEE"
                );

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "employees",
                employees
        );

        return "adminReports";
    }


// =====================================================
// GENERATE REPORT
// =====================================================

    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_REPORT_VIEW')")
    @GetMapping("/generate")
    public String generateReport(
            @RequestParam Integer employeeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // -------------------------------------------------
        // GET ADMIN'S EMPLOYEES
        // -------------------------------------------------

        List<User> employees =
                userRepo.findByAdminIdAndRole(
                        adminId,
                        "EMPLOYEE"
                );


        // -------------------------------------------------
        // VERIFY EMPLOYEE
        // -------------------------------------------------

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );


        // -------------------------------------------------
        // SECURITY CHECK
        // -------------------------------------------------

        validateEmployeeAccess(
                employee,
                adminId
        );


        // -------------------------------------------------
        // VALIDATE DATES
        // -------------------------------------------------

        validateSevenDayPeriod(
                startDate,
                endDate
        );


        // =================================================
        // BUILD DAILY REPORT
        // =================================================

        List<EmployeeDailyReportDTO> dailyReports =
                buildDailyReports(
                        employeeId,
                        startDate,
                        endDate
                );


        // =================================================
        // CALCULATE TOTALS
        // =================================================

        ReportTotals totals =
                calculateTotals(
                        dailyReports
                );


        // =================================================
        // SEND DATA TO THYMELEAF
        // =================================================

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "employees",
                employees
        );

        model.addAttribute(
                "selectedEmployee",
                employee
        );

        model.addAttribute(
                "employeeId",
                employeeId
        );

        model.addAttribute(
                "startDate",
                startDate
        );

        model.addAttribute(
                "endDate",
                endDate
        );

        model.addAttribute(
                "dailyReports",
                dailyReports
        );

        model.addAttribute(
                "totalActiveMinutes",
                totals.totalActiveMinutes
        );

        model.addAttribute(
                "totalIdleMinutes",
                totals.totalIdleMinutes
        );

        model.addAttribute(
                "totalOvertimeMinutes",
                totals.totalOvertimeMinutes
        );

        model.addAttribute(
                "daysWorked",
                totals.daysWorked
        );

        model.addAttribute(
                "averageActiveMinutes",
                totals.averageActiveMinutes
        );

        model.addAttribute(
                "reportGenerated",
                true
        );

        return "adminReports";
    }


// =====================================================
// DOWNLOAD 7-DAY PDF
// =====================================================


    @PreAuthorize("hasAuthority('ROLE_ADMIN') and hasAuthority('PERMISSION_REPORT_VIEW')")
    @GetMapping("/download")
    public void downloadSevenDayReport(
            @RequestParam Integer employeeId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Authentication authentication,
            HttpServletResponse response)
            throws IOException {

        // -------------------------------------------------
        // GET LOGGED-IN ADMIN
        // -------------------------------------------------

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // -------------------------------------------------
        // VERIFY EMPLOYEE
        // -------------------------------------------------

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(
                                () -> new RuntimeException(
                                        "Employee not found"
                                )
                        );


        // -------------------------------------------------
        // ORGANIZATION SECURITY CHECK
        // -------------------------------------------------

        validateEmployeeAccess(
                employee,
                adminId
        );


        // -------------------------------------------------
        // VALIDATE 7-DAY PERIOD
        // -------------------------------------------------

        validateSevenDayPeriod(
                startDate,
                endDate
        );


        // =================================================
        // BUILD DAILY REPORT
        // =================================================

        List<EmployeeDailyReportDTO> dailyReports =
                buildDailyReports(
                        employeeId,
                        startDate,
                        endDate
                );


        // =================================================
        // CALCULATE TOTALS
        // =================================================

        ReportTotals totals =
                calculateTotals(
                        dailyReports
                );


        // =================================================
        // GENERATE PDF
        // =================================================

        byte[] pdf =
                adminReportPdfService.generateSevenDayReport(
                        employee,
                        startDate,
                        endDate,
                        dailyReports,
                        totals.totalActiveMinutes,
                        totals.totalIdleMinutes,
                        totals.totalOvertimeMinutes,
                        totals.daysWorked,
                        totals.averageActiveMinutes
                );


        // =================================================
        // DOWNLOAD RESPONSE
        // =================================================

        String employeeName =
                employee.getName() == null
                        ? "employee"
                        : employee.getName()
                        .trim()
                        .replaceAll(
                                "[^a-zA-Z0-9_-]",
                                "_"
                        );


        String fileName =
                "TeamLogger_7-Day_Report_"
                        + employeeName
                        + "_"
                        + startDate
                        + "_to_"
                        + endDate
                        + ".pdf";


        response.setContentType(
                "application/pdf"
        );

        response.setHeader(
                "Content-Disposition",
                "attachment; filename=\"" +
                        fileName +
                        "\""
        );

        response.setContentLength(
                pdf.length
        );


        response.getOutputStream()
                .write(pdf);

        response.getOutputStream()
                .flush();
    }


// =====================================================
// VERIFY EMPLOYEE ACCESS
// =====================================================

    private void validateEmployeeAccess(
            User employee,
            Integer adminId) {

        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole())) {

            throw new RuntimeException(
                    "Selected user is not an employee"
            );
        }


        if (employee.getAdminId() == null ||
                !employee.getAdminId().equals(adminId)) {

            throw new RuntimeException(
                    "You are not authorized to view this employee"
            );
        }
    }


// =====================================================
// VALIDATE 7-DAY PERIOD
// =====================================================

    private void validateSevenDayPeriod(
            LocalDate startDate,
            LocalDate endDate) {

        if (startDate == null ||
                endDate == null) {

            throw new RuntimeException(
                    "Start date and end date are required"
            );
        }


        if (endDate.isBefore(startDate)) {

            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }


        long numberOfDays =
                ChronoUnit.DAYS.between(
                        startDate,
                        endDate
                ) + 1;


        if (numberOfDays != 7) {

            throw new RuntimeException(
                    "Report period must contain exactly 7 days"
            );
        }
    }


// =====================================================
// BUILD DAILY REPORTS
// =====================================================

    private List<EmployeeDailyReportDTO> buildDailyReports(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate) {

        LocalDateTime startDateTime =
                startDate.atStartOfDay();

        LocalDateTime endDateTime =
                endDate.plusDays(1)
                        .atStartOfDay();


        List<Session> sessions =
                sessionRepo.findByUserIdAndLoginTimeBetween(
                        employeeId,
                        startDateTime,
                        endDateTime
                );


        // =================================================
        // GROUP SESSIONS BY DATE
        // =================================================

        Map<LocalDate, List<Session>> sessionsByDate =
                new HashMap<>();


        for (Session session : sessions) {

            if (session.getLoginTime() == null) {
                continue;
            }

            LocalDate date =
                    session.getLoginTime()
                            .toLocalDate();

            sessionsByDate
                    .computeIfAbsent(
                            date,
                            key -> new ArrayList<>()
                    )
                    .add(session);
        }


        // =================================================
        // BUILD 7 DAYS
        // =================================================

        List<EmployeeDailyReportDTO> dailyReports =
                new ArrayList<>();


        LocalDate currentDate =
                startDate;


        while (!currentDate.isAfter(endDate)) {

            List<Session> dailySessions =
                    sessionsByDate.getOrDefault(
                            currentDate,
                            new ArrayList<>()
                    );


            // -------------------------------------------------
            // NO ACTIVITY
            // -------------------------------------------------

            if (dailySessions.isEmpty()) {

                dailyReports.add(
                        new EmployeeDailyReportDTO(
                                currentDate,
                                null,
                                null,
                                0,
                                0,
                                null,
                                0,
                                "NO ACTIVITY"
                        )
                );

                currentDate =
                        currentDate.plusDays(1);

                continue;
            }


            // -------------------------------------------------
            // SORT
            // -------------------------------------------------

            dailySessions.sort(
                    Comparator.comparing(
                            Session::getLoginTime
                    )
            );


            // -------------------------------------------------
            // LOGIN
            // -------------------------------------------------

            LocalDateTime firstLogin =
                    dailySessions
                            .get(0)
                            .getLoginTime();


            // -------------------------------------------------
            // LOGOUT
            // -------------------------------------------------

            LocalDateTime latestLogout =
                    null;

            boolean currentlyWorking =
                    false;


            for (Session session :
                    dailySessions) {

                if (session.getLogoutTime() == null) {

                    currentlyWorking = true;

                } else if (latestLogout == null ||
                        session.getLogoutTime()
                                .isAfter(latestLogout)) {

                    latestLogout =
                            session.getLogoutTime();
                }
            }


            // -------------------------------------------------
            // ACTIVE / IDLE
            // -------------------------------------------------

            int activeMinutes = 0;

            int idleMinutes = 0;


            // -------------------------------------------------
            // PRODUCTIVITY
            // -------------------------------------------------

            double productivityTotal = 0;

            int productivityCount = 0;


            for (Session session :
                    dailySessions) {

                if (session.getActiveMinutes() != null) {

                    activeMinutes +=
                            session.getActiveMinutes();

                } else if (session.getActiveSeconds() != null) {

                    activeMinutes +=
                            session.getActiveSeconds() / 60;
                }


                if (session.getIdleMinutes() != null) {

                    idleMinutes +=
                            session.getIdleMinutes();

                } else if (session.getIdleSeconds() != null) {

                    idleMinutes +=
                            session.getIdleSeconds() / 60;
                }


                if (session.getProductivity() != null) {

                    productivityTotal +=
                            session.getProductivity();

                    productivityCount++;
                }
            }


            Double productivity =
                    productivityCount > 0
                            ? productivityTotal /
                            productivityCount
                            : null;


            // -------------------------------------------------
            // OVERTIME
            // -------------------------------------------------

            int overtimeMinutes =
                    Math.max(
                            activeMinutes -
                                    REGULAR_WORK_MINUTES,
                            0
                    );


            // -------------------------------------------------
            // STATUS
            // -------------------------------------------------

            String status =
                    currentlyWorking
                            ? "WORKING"
                            : "WORKED";


            dailyReports.add(
                    new EmployeeDailyReportDTO(
                            currentDate,
                            firstLogin,
                            latestLogout,
                            activeMinutes,
                            idleMinutes,
                            productivity,
                            overtimeMinutes,
                            status
                    )
            );


            currentDate =
                    currentDate.plusDays(1);
        }


        return dailyReports;
    }


// =====================================================
// CALCULATE TOTALS
// =====================================================

    private ReportTotals calculateTotals(
            List<EmployeeDailyReportDTO> dailyReports) {

        int totalActiveMinutes = 0;

        int totalIdleMinutes = 0;

        int totalOvertimeMinutes = 0;

        int daysWorked = 0;


        for (EmployeeDailyReportDTO report :
                dailyReports) {

            totalActiveMinutes +=
                    report.getActiveMinutes();

            totalIdleMinutes +=
                    report.getIdleMinutes();

            totalOvertimeMinutes +=
                    report.getOvertimeMinutes();


            if (!"NO ACTIVITY".equals(
                    report.getStatus())) {

                daysWorked++;
            }
        }


        double averageActiveMinutes =
                daysWorked > 0
                        ? (double) totalActiveMinutes /
                        daysWorked
                        : 0;


        return new ReportTotals(
                totalActiveMinutes,
                totalIdleMinutes,
                totalOvertimeMinutes,
                daysWorked,
                averageActiveMinutes
        );
    }


// =====================================================
// INTERNAL TOTALS CLASS
// =====================================================

    private static class ReportTotals {

        private final int totalActiveMinutes;
        private final int totalIdleMinutes;
        private final int totalOvertimeMinutes;
        private final int daysWorked;
        private final double averageActiveMinutes;


        private ReportTotals(
                int totalActiveMinutes,
                int totalIdleMinutes,
                int totalOvertimeMinutes,
                int daysWorked,
                double averageActiveMinutes) {

            this.totalActiveMinutes =
                    totalActiveMinutes;

            this.totalIdleMinutes =
                    totalIdleMinutes;

            this.totalOvertimeMinutes =
                    totalOvertimeMinutes;

            this.daysWorked =
                    daysWorked;

            this.averageActiveMinutes =
                    averageActiveMinutes;
        }
    }


}
