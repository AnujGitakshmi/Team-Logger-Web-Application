package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.Attendance;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AttendanceService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {
        this.attendanceService = attendanceService;
    }


    /*
     * ============================================================
     * EMPLOYEE ATTENDANCE PAGE
     * ============================================================
     *
     * URL:
     * /employee/attendance
     *
     * Optional:
     * /employee/attendance?year=2026
     */
    @GetMapping
    public String attendance(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) Integer year
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        int currentYear =
                LocalDate.now().getYear();

        int selectedYear =
                year != null
                        ? year
                        : currentYear;

        /*
         * Prevent invalid year selection.
         */
        if (selectedYear < 2000
                || selectedYear > currentYear) {

            selectedYear = currentYear;
        }


        /*
         * Get attendance records for selected year.
         */
        List<Attendance> attendanceList =
                attendanceService.getYearAttendance(
                        employee.getId(),
                        selectedYear
                );


        /*
         * Today's attendance.
         */
        Attendance todayAttendance =
                attendanceService
                        .getTodayAttendance(
                                employee.getId()
                        )
                        .orElse(null);


        /*
         * Count only records from selected year.
         */
        long presentDays =
                attendanceService.countPresentDays(
                        employee.getId(),
                        selectedYear
                );

        long absentDays =
                attendanceService.countAbsentDays(
                        employee.getId(),
                        selectedYear
                );


        /*
         * Total days in selected year.
         */
        int totalDays =
                LocalDate.of(
                        selectedYear,
                        1,
                        1
                ).lengthOfYear();


        /*
         * Calculate attendance percentage.
         *
         * Only PRESENT and ABSENT records
         * are included.
         */
        long recordedDays =
                presentDays + absentDays;

        double attendancePercentage = 0;

        if (recordedDays > 0) {

            attendancePercentage =
                    (presentDays * 100.0)
                            / recordedDays;
        }


        /*
         * Send employee to Thymeleaf.
         */
        model.addAttribute(
                "user",
                employee
        );

        model.addAttribute(
                "attendanceList",
                attendanceList
        );

        model.addAttribute(
                "todayAttendance",
                todayAttendance
        );

        model.addAttribute(
                "presentDays",
                presentDays
        );

        model.addAttribute(
                "absentDays",
                absentDays
        );

        model.addAttribute(
                "totalDays",
                totalDays
        );

        model.addAttribute(
                "attendancePercentage",
                String.format(
                        "%.1f",
                        attendancePercentage
                )
        );

        model.addAttribute(
                "selectedYear",
                selectedYear
        );

        model.addAttribute(
                "currentYear",
                currentYear
        );

        model.addAttribute(
                "today",
                LocalDate.now()
        );

        return "attendance";
    }


    /*
     * ============================================================
     * EMPLOYEE ATTENDANCE HISTORY
     * ============================================================
     *
     * URL:
     * /employee/attendance/history
     *
     * Example:
     * /employee/attendance/history?year=2026
     */
    @GetMapping("/history")
    public String attendanceHistory(
            Authentication authentication,
            Model model,
            @RequestParam(required = false) Integer year
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        int currentYear =
                LocalDate.now().getYear();

        int selectedYear =
                year != null
                        ? year
                        : currentYear;

        if (selectedYear < 2000
                || selectedYear > currentYear) {

            selectedYear = currentYear;
        }

        /*
         * ============================================================
         * DEBUG
         * ============================================================
         */

        System.out.println("==========================================");
        System.out.println("ATTENDANCE HISTORY DEBUG");
        System.out.println("Employee ID      : " + employee.getId());
        System.out.println("Employee Name    : " + employee.getName());
        System.out.println("Selected Year    : " + selectedYear);

        List<Attendance> attendanceHistory =
                attendanceService.getYearAttendance(
                        employee.getId(),
                        selectedYear
                );

        System.out.println(
                "Attendance Count: "
                        + attendanceHistory.size()
        );

        for (Attendance attendance : attendanceHistory) {

            System.out.println(
                    "Record -> ID="
                            + attendance.getId()
                            + ", employeeId="
                            + attendance.getEmployeeId()
                            + ", date="
                            + attendance.getAttendanceDate()
                            + ", start="
                            + attendance.getStartTime()
                            + ", end="
                            + attendance.getEndTime()
                            + ", status="
                            + attendance.getStatus()
            );
        }

        System.out.println("==========================================");


        model.addAttribute(
                "user",
                employee
        );

        model.addAttribute(
                "attendanceHistory",
                attendanceHistory
        );

        model.addAttribute(
                "selectedYear",
                selectedYear
        );

        model.addAttribute(
                "currentYear",
                currentYear
        );

        model.addAttribute(
                "today",
                LocalDate.now()
        );

        return "attendance-history";


    }



    /*
     * ============================================================
     * GET AUTHENTICATED EMPLOYEE
     * ============================================================
     */
    private User getAuthenticatedEmployee(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()) {

            throw new IllegalStateException(
                    "User is not authenticated."
            );
        }


        Object principal =
                authentication.getPrincipal();


        if (!(principal instanceof CustomUserDetails)) {

            throw new IllegalStateException(
                    "Invalid authenticated user."
            );
        }


        CustomUserDetails userDetails =
                (CustomUserDetails) principal;


        return userDetails.getUser();
    }
}