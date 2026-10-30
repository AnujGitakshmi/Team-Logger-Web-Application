package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.ProjectCertificateService;
import com.example.TeamLoggerC.service.ProjectService;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;

@Controller
@RequestMapping("/employee/projects")
public class EmployeeProjectController {

    private final ProjectService projectService;
    private final UserRepo userRepo;
    private final ProjectCertificateService projectCertificateService;

    public EmployeeProjectController(
            ProjectService projectService,
            UserRepo userRepo,
            ProjectCertificateService projectCertificateService
    ) {
        this.projectService = projectService;
        this.userRepo = userRepo;
        this.projectCertificateService = projectCertificateService;
    }


    // ============================================================
    // EMPLOYEE PROJECTS PAGE
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_VIEW')")
    @GetMapping
    public String projects(
            Authentication authentication,
            Model model
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();

        User employee = userRepo.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        )
                );

        List<Project> projects =
                projectService.getProjectsForEmployee(employeeId);

        model.addAttribute("employee", employee);
        model.addAttribute("projects", projects);

        return "myprojectsEmployee";
    }


    // ============================================================
    // PROJECT DETAILS
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_VIEW')")
    @GetMapping("/{projectId}")
    public String projectDetails(
            @PathVariable Integer projectId,
            Authentication authentication,
            Model model
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();

        User employee = userRepo.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        )
                );


        // --------------------------------------------------------
        // FIND PROJECT
        // --------------------------------------------------------

        Project project =
                projectService.getProject(projectId);


        // --------------------------------------------------------
        // VERIFY EMPLOYEE CAN VIEW PROJECT
        // --------------------------------------------------------

        List<Project> visibleProjects =
                projectService.getProjectsForEmployee(
                        employeeId
                );

        boolean canView =
                visibleProjects.stream()
                        .anyMatch(p ->
                                p.getId().equals(projectId)
                        );

        if (!canView) {

            throw new IllegalArgumentException(
                    "You are not authorized to view this project."
            );
        }


        // --------------------------------------------------------
        // FIND PROJECT LEADER
        // --------------------------------------------------------

        if (project.getProjectLeaderId() == null) {

            throw new IllegalArgumentException(
                    "Project does not have a Project Leader."
            );
        }

        User projectLeader =
                userRepo.findById(
                        project.getProjectLeaderId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Project Leader not found."
                        )
                );


        // --------------------------------------------------------
        // CHECK WHETHER CURRENT EMPLOYEE IS PROJECT LEADER
        // --------------------------------------------------------

        boolean isProjectLeader =
                project.getProjectLeaderId()
                        .equals(employeeId);


        // --------------------------------------------------------
        // SEND DATA TO THYMELEAF
        // --------------------------------------------------------

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "project",
                project
        );

        model.addAttribute(
                "projectLeader",
                projectLeader
        );

        model.addAttribute(
                "isProjectLeader",
                isProjectLeader
        );


        return "employeeProjectDetails";
    }


    // ============================================================
    // REQUEST PROJECT COMPLETION
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_COMPLETE_REQUEST')")
    @PostMapping("/{projectId}/request-completion")
    public String requestCompletion(
            @PathVariable Integer projectId,
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();


        /*
         * ProjectService performs the important authorization:
         *
         * Only the Project Leader can request completion.
         *
         * ASSIGNED -> PENDING
         *
         * It does NOT become COMPLETED here.
         */

        projectService.requestCompletion(
                projectId,
                employeeId
        );


        return "redirect:/employee/projects/"
                + projectId;
    }

    @PreAuthorize("hasAuthority('PERMISSION_CERTIFICATE_VIEW')")
    @GetMapping("/{projectId}/certificate")
    public ResponseEntity<byte[]> downloadCertificate(
            @PathVariable Integer projectId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();

        byte[] pdf =
                projectCertificateService.generateCertificate(
                        projectId,
                        employeeId
                );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"project-certificate-"
                                + projectId
                                + ".pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }


    @PreAuthorize("hasAuthority('PERMISSION_CERTIFICATE_VIEW')")
    @GetMapping("/{projectId}/linkedin-certificate")
    public ResponseEntity<byte[]> downloadLinkedInCertificate(
            @PathVariable Integer projectId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();

        byte[] pdf =
                projectCertificateService
                        .generateLinkedInCertificate(
                                projectId,
                                employeeId
                        );

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"linkedin-project-certificate-"
                                + projectId
                                + ".pdf\""
                )
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}