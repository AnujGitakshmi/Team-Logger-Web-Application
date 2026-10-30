package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.AdminSettings;
import com.example.TeamLoggerC.entity.RecordingSession;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminDashboardService;
import com.example.TeamLoggerC.service.ScreenRecordingService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.example.TeamLoggerC.service.LoginApprovalService;
import com.example.TeamLoggerC.service.AdminSettingsService;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;
    private final ScreenRecordingService screenRecordingService;
    private final LoginApprovalService loginApprovalService;
    private final AdminSettingsService adminSettingsService;

    public AdminDashboardController(
            AdminDashboardService adminDashboardService,
            ScreenRecordingService screenRecordingService,
            LoginApprovalService loginApprovalService,
            AdminSettingsService adminSettingsService) {

        this.adminDashboardService =
                adminDashboardService;

        this.screenRecordingService =
                screenRecordingService;

        this.loginApprovalService =
                loginApprovalService;
        this.adminSettingsService=adminSettingsService;
    }

    @GetMapping("/dashboard")
    public String dashboard(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        AdminSettings adminSettings =
                adminSettingsService.getSettings(adminId);

        model.addAttribute(
                "adminSettings",
                adminSettings
        );

        int pendingLoginApprovals =
                loginApprovalService
                        .getPendingRequests(adminId)
                        .size();

        model.addAttribute(
                "pendingApprovalCount",
                pendingLoginApprovals
        );

        model.addAttribute(
                "totalEmployees",
                adminDashboardService.getTotalEmployees(adminId)
        );

        model.addAttribute(
                "workingNow",
                adminDashboardService.getWorkingNow(adminId)
        );

        model.addAttribute(
                "activeTime",
                adminDashboardService.getTodayActiveTime(adminId)
        );

        model.addAttribute(
                "idleTime",
                adminDashboardService.getTodayIdleTime(adminId)
        );

        model.addAttribute(
                "overtime",
                adminDashboardService.getMonthlyOvertime(adminId)
        );

        model.addAttribute(
                "employees",
                adminDashboardService.getTeamActivity(adminId)
        );

        model.addAttribute(
                "admin",
                admin
        );

        return "dashboardAdmin";
    }


    @GetMapping("/screen-recording")
    public String screenRecording(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();

        model.addAttribute(
                "employees",
                adminDashboardService.getScreenRecordingEmployees(adminId)
        );

        model.addAttribute(
                "admin",
                admin
        );

        return "screen-recording";
    }


    @PostMapping("/screen-recording/start/{employeeId}")
    public String startRecording(
            @PathVariable Integer employeeId,
            RedirectAttributes redirectAttributes) {

        String result =
                screenRecordingService.startRecording(employeeId);

        redirectAttributes.addFlashAttribute(
                "recordingMessage",
                result
        );

        return "redirect:/admin/screen-recording";
    }


    @PostMapping("/screen-recording/stop/{employeeId}")
    public String stopRecording(
            @PathVariable Integer employeeId,
            RedirectAttributes redirectAttributes) {

        String result =
                screenRecordingService.stopRecording(employeeId);

        redirectAttributes.addFlashAttribute(
                "recordingMessage",
                result
        );

        return "redirect:/admin/screen-recording";
    }

    @GetMapping("/screen-recording/{employeeId}")
    public String viewRecordings(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        // ---------------------------------------------------------
        // Verify that this employee belongs to this admin
        // ---------------------------------------------------------

        User employee =
                adminDashboardService
                        .getEmployees(adminId)
                        .stream()
                        .filter(user ->
                                user.getId()
                                        .equals(employeeId))
                        .findFirst()
                        .orElse(null);


        if (employee == null) {

            return "redirect:/admin/screen-recording";
        }


        // ---------------------------------------------------------
        // Get recordings
        // ---------------------------------------------------------

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "recordings",
                screenRecordingService
                        .getRecordings(employeeId)
        );

        model.addAttribute(
                "admin",
                admin
        );


        return "screen-recording-history";
    }

    @GetMapping("/screen-recording/view/{recordingId}")
    public ResponseEntity<Resource> viewRecording(
            @PathVariable Integer recordingId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        RecordingSession recording =
                screenRecordingService
                        .getRecording(recordingId);


        if (recording == null) {
            return ResponseEntity.notFound().build();
        }


        // ---------------------------------------------------------
        // Security check
        // ---------------------------------------------------------

        boolean employeeBelongsToAdmin =
                adminDashboardService
                        .getEmployees(adminId)
                        .stream()
                        .anyMatch(employee ->
                                employee.getId()
                                        .equals(recording.getUserId())
                        );


        if (!employeeBelongsToAdmin) {
            return ResponseEntity.status(403).build();
        }


        // ---------------------------------------------------------
        // Check file
        // ---------------------------------------------------------

        if (recording.getFilePath() == null
                || recording.getFilePath().isBlank()) {

            return ResponseEntity.notFound().build();
        }


        FileSystemResource resource =
                new FileSystemResource(
                        recording.getFilePath()
                );


        if (!resource.exists()
                || !resource.isReadable()) {

            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "video/mp4"
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\""
                                + recording.getFileName()
                                + "\""
                )
                .body(resource);
    }
    @GetMapping("/screen-recording/download/{recordingId}")
    public ResponseEntity<Resource> downloadRecording(
            @PathVariable Integer recordingId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User admin =
                userDetails.getUser();

        Integer adminId =
                admin.getId();


        RecordingSession recording =
                screenRecordingService
                        .getRecording(recordingId);


        if (recording == null) {
            return ResponseEntity.notFound().build();
        }


        // ---------------------------------------------------------
        // Verify employee belongs to admin
        // ---------------------------------------------------------

        boolean employeeBelongsToAdmin =
                adminDashboardService
                        .getEmployees(adminId)
                        .stream()
                        .anyMatch(employee ->
                                employee.getId()
                                        .equals(recording.getUserId())
                        );


        if (!employeeBelongsToAdmin) {
            return ResponseEntity.status(403).build();
        }


        if (recording.getFilePath() == null
                || recording.getFilePath().isBlank()) {

            return ResponseEntity.notFound().build();
        }


        FileSystemResource resource =
                new FileSystemResource(
                        recording.getFilePath()
                );


        if (!resource.exists()
                || !resource.isReadable()) {

            return ResponseEntity.notFound().build();
        }


        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_OCTET_STREAM
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + recording.getFileName()
                                + "\""
                )
                .body(resource);
    }
}