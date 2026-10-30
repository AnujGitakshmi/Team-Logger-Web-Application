package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.ProjectFormDTO;
import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.ProjectService;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/projects")
public class AdminProjectController {

    private final ProjectService projectService;
    private final UserRepo userRepo;

    public AdminProjectController(
            ProjectService projectService,
            UserRepo userRepo
    ) {
        this.projectService = projectService;
        this.userRepo = userRepo;
    }


    // ============================================================
    // PROJECT LIST
    // ============================================================

    @GetMapping
    public String projects(
            Authentication authentication,
            Model model
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer adminId = userDetails.getUser().getId();

        List<Project> projects =
                projectService.getAdminProjects(adminId);

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin not found."
                ));

        model.addAttribute("projects", projects);
        model.addAttribute("admin", admin);

        return "adminProjects";
    }


    // ============================================================
    // ADD PROJECT PAGE
    // ============================================================

    @GetMapping("/add")
    public String addProject(
            Authentication authentication,
            Model model
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer adminId = userDetails.getUser().getId();

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin not found."
                ));

        /*
         * Only Project Leaders belonging to this admin
         * should be available for project assignment.
         */
        List<User> projectLeaders =
                userRepo.findByAdminIdAndRole(
                                adminId,
                                "EMPLOYEE"
                        )
                        .stream()
                        .filter(user ->
                                user.getDesignation() != null
                                        && user.getDesignation()
                                        .equalsIgnoreCase("Project Leader")
                        )
                        .toList();

        model.addAttribute("admin", admin);
        model.addAttribute("projectLeaders", projectLeaders);
        model.addAttribute("projectForm", new ProjectFormDTO());

        return "adminProjectAdd";
    }


    // ============================================================
    // SAVE PROJECT
    // ============================================================

    @PostMapping("/save")
    public String saveProject(
            @ModelAttribute("projectForm") ProjectFormDTO form,
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer adminId = userDetails.getUser().getId();

        Project project = new Project();

        project.setProjectName(form.getProjectName());
        project.setDescription(form.getDescription());
        project.setStartDate(form.getStartDate());
        project.setExpectedCompletionDate(
                form.getExpectedCompletionDate()
        );
        project.setProjectLeaderId(
                form.getProjectLeaderId()
        );

        projectService.createProject(
                project,
                adminId
        );

        return "redirect:/admin/projects";
    }


    // ============================================================
    // PROJECT DETAILS
    // ============================================================

    @GetMapping("/{projectId}")
    public String projectDetails(
            @PathVariable Integer projectId,
            Authentication authentication,
            Model model
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer adminId = userDetails.getUser().getId();

        Project project =
                projectService.getProject(projectId);

        /*
         * Security check:
         *
         * An admin should only view projects created
         * under his/her own admin account.
         */
        if (project.getAdminId() == null
                || !project.getAdminId().equals(adminId)) {

            throw new IllegalArgumentException(
                    "You are not authorized to view this project."
            );
        }

        User admin = userRepo.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Admin not found."
                ));

        User projectLeader =
                userRepo.findById(project.getProjectLeaderId())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Project Leader not found."
                        ));

        /*
         * Load all employees belonging to this Project Leader's
         * hierarchy.
         */
        List<User> teamMembers =
                getTeamMembersUnderLeader(
                        projectLeader.getId()
                );

        model.addAttribute("admin", admin);
        model.addAttribute("project", project);
        model.addAttribute("projectLeader", projectLeader);
        model.addAttribute("teamMembers", teamMembers);

        return "adminProjectDetails";
    }


    // ============================================================
// ADMIN CONFIRM PROJECT COMPLETION
// ============================================================

    @PostMapping("/{projectId}/confirm-completion")
    public String confirmCompletion(
            @PathVariable Integer projectId,
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer adminId = userDetails.getUser().getId();

        projectService.confirmCompletion(
                projectId,
                adminId
        );

        return "redirect:/admin/projects/" + projectId;
    }


    // ============================================================
    // GET ALL EMPLOYEES UNDER PROJECT LEADER
    // ============================================================

    private List<User> getTeamMembersUnderLeader(
            Integer projectLeaderId
    ) {

        List<User> allEmployees =
                userRepo.findAll();

        List<User> teamMembers = new java.util.ArrayList<>();

        for (User employee : allEmployees) {

            if (employee.getId() == null) {
                continue;
            }

            if (employee.getId().equals(projectLeaderId)) {
                continue;
            }

            if (isUnderLeader(
                    employee,
                    projectLeaderId
            )) {

                teamMembers.add(employee);
            }
        }

        return teamMembers;
    }


    // ============================================================
    // CHECK EMPLOYEE HIERARCHY
    // ============================================================

    private boolean isUnderLeader(
            User employee,
            Integer projectLeaderId
    ) {

        User current = employee;

        while (current != null) {

            Integer parentId = current.getSeniorId();

            if (parentId == null) {
                return false;
            }

            if (parentId.equals(projectLeaderId)) {
                return true;
            }

            current = userRepo.findById(parentId)
                    .orElse(null);
        }

        return false;
    }

    @PostMapping("/projects/{projectId}/request-completion")
    public String requestCompletion(
            @PathVariable Integer projectId,
            Authentication authentication
    ) {

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Integer employeeId =
                userDetails.getUser().getId();

        projectService.requestCompletion(
                projectId,
                employeeId
        );

        return "redirect:/employee/projects/" + projectId;
    }
}