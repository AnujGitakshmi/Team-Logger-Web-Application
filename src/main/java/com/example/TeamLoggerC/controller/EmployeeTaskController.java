 package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.DailyWork;
import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.ProjectRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.DailyWorkService;
import com.example.TeamLoggerC.service.ProjectService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/employee/tasks")
public class EmployeeTaskController {

    private final DailyWorkService dailyWorkService;
    private final ProjectService projectService;
    private final ProjectRepo projectRepo;
    private final UserRepo userRepo;

    public EmployeeTaskController(
            DailyWorkService dailyWorkService,
            ProjectService projectService,
            ProjectRepo projectRepo,
            UserRepo userRepo
    ) {
        this.dailyWorkService = dailyWorkService;
        this.projectService = projectService;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
    }

    @PreAuthorize("hasAuthority('PERMISSION_TASK_VIEW')")
    @GetMapping
    public String myTasks(
            Authentication authentication,
            Model model
    ) {

        User employee = getAuthenticatedEmployee(authentication);

        Integer employeeId = employee.getId();

        List<Project> projects =
                projectService.getProjectsForEmployee(employeeId);

        List<Project> currentProjects =
                projects.stream()
                        .filter(project ->
                                !"COMPLETED".equalsIgnoreCase(
                                        project.getStatus()
                                )
                        )
                        .toList();

        List<Project> completedProjects =
                projects.stream()
                        .filter(project ->
                                "COMPLETED".equalsIgnoreCase(
                                        project.getStatus()
                                )
                        )
                        .toList();

        List<DailyWork> todayWork =
                dailyWorkService.getOwnTodayWork(employeeId);

        model.addAttribute("employee", employee);
        model.addAttribute("projects", projects);
        model.addAttribute("currentProjects", currentProjects);
        model.addAttribute("completedProjects", completedProjects);
        model.addAttribute("todayWork", todayWork);
        model.addAttribute("today", LocalDate.now());

        return "tasks";
    }

    // ============================================================
    // ADD DAILY WORK PAGE
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_CREATE')")
    @GetMapping("/project/{projectId}/add")
    public String addDailyWorkPage(
            @PathVariable Integer projectId,
            Authentication authentication,
            Model model
    ) {

        User employee = getAuthenticatedEmployee(authentication);

        Project project = projectService.getProjectsForEmployee(employee.getId())
                .stream()
                .filter(p -> p.getId().equals(projectId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("You do not have access to this project.")
                );

        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new IllegalStateException(
                    "Daily work cannot be added to a completed project."
            );
        }

        model.addAttribute("employee", employee);
        model.addAttribute("project", project);
        model.addAttribute("today", LocalDate.now());

        return "addDailyWork";
    }

    // ============================================================
    // VIEW PROJECT DAILY WORK HISTORY
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_VIEW')")
    @GetMapping("/project/{projectId}")
    public String projectWorkHistory(
            @PathVariable Integer projectId,
            Authentication authentication,
            Model model
    ) {

        User employee = getAuthenticatedEmployee(authentication);

        Project project = projectService.getProjectsForEmployee(employee.getId())
                .stream()
                .filter(p -> p.getId().equals(projectId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("You do not have access to this project.")
                );

        List<DailyWork> ownWork =
                dailyWorkService.getOwnProjectWork(
                        employee.getId(),
                        projectId
                );

        List<DailyWork> teamWork =
                dailyWorkService.getTeamWork(
                        employee.getId(),
                        projectId
                );

        List<User> visibleEmployees =
                dailyWorkService.getVisibleEmployeesForProject(
                        employee,
                        project
                );

        model.addAttribute("employee", employee);
        model.addAttribute("project", project);
        model.addAttribute("ownWork", ownWork);
        model.addAttribute("teamWork", teamWork);
        model.addAttribute("visibleEmployees", visibleEmployees);
        model.addAttribute("today", LocalDate.now());

        return "dailyWorkHistory";
    }

    // ============================================================
    // ADD DAILY WORK
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_CREATE')")
    @PostMapping("/project/{projectId}/add")
    public String addDailyWork(
            @PathVariable Integer projectId,
            @RequestParam String description,
            @RequestParam(required = false) String workDate,
            Authentication authentication
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        LocalDate date;

        if (workDate == null
                || workDate.trim().isEmpty()) {

            date = LocalDate.now();

        } else {

            date = LocalDate.parse(workDate);
        }

        dailyWorkService.addDailyWork(
                employee.getId(),
                projectId,
                date,
                description
        );

        return "redirect:/employee/tasks/project/"
                + projectId;
    }

// ============================================================
// EDIT DAILY WORK PAGE
// ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_UPDATE')")
    @GetMapping("/work/{workId}/edit")
    public String editDailyWorkPage(
            @PathVariable Integer workId,
            Authentication authentication,
            Model model
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        DailyWork work =
                dailyWorkService.getWorkByIdForEmployee(
                        employee.getId(),
                        workId
                );

        // --------------------------------------------------------
        // ONLY TODAY'S WORK CAN BE EDITED
        // --------------------------------------------------------

        if (!LocalDate.now().equals(work.getWorkDate())) {

            throw new IllegalStateException(
                    "Only today's work can be edited."
            );
        }

        Project project =
                projectRepo.findById(work.getProjectId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Project not found."
                                )
                        );

        // --------------------------------------------------------
        // COMPLETED PROJECT CANNOT BE EDITED
        // --------------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalStateException(
                    "Daily work of a completed project cannot be edited."
            );
        }

        model.addAttribute("employee", employee);
        model.addAttribute("work", work);
        model.addAttribute("project", project);

        return "editDailyWork";
    }


// ============================================================
// UPDATE DAILY WORK
// ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_UPDATE')")
    @PostMapping("/work/{workId}/edit")
    public String updateDailyWork(
            @PathVariable Integer workId,
            @RequestParam String workDate,
            @RequestParam String description,
            Authentication authentication
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        DailyWork existingWork =
                dailyWorkService.getWorkByIdForEmployee(
                        employee.getId(),
                        workId
                );

        // --------------------------------------------------------
        // ONLY TODAY'S EXISTING WORK CAN BE UPDATED
        // --------------------------------------------------------

        if (!LocalDate.now().equals(existingWork.getWorkDate())) {

            throw new IllegalStateException(
                    "Only today's work can be updated."
            );
        }

        // --------------------------------------------------------
        // SUBMITTED DATE MUST ALSO BE TODAY
        // --------------------------------------------------------

        LocalDate date =
                LocalDate.parse(workDate);

        if (!LocalDate.now().equals(date)) {

            throw new IllegalStateException(
                    "Daily work can only be updated for today."
            );
        }

        // --------------------------------------------------------
        // PROJECT MUST NOT BE COMPLETED
        // --------------------------------------------------------

        Project project =
                projectRepo.findById(existingWork.getProjectId())
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Project not found."
                                )
                        );

        if ("COMPLETED".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalStateException(
                    "Daily work of a completed project cannot be edited."
            );
        }

        // --------------------------------------------------------
        // UPDATE
        // --------------------------------------------------------

        dailyWorkService.updateDailyWork(
                employee.getId(),
                workId,
                date,
                description
        );

        return "redirect:/employee/tasks/project/"
                + existingWork.getProjectId();
    }


// ============================================================
// DELETE DAILY WORK
// ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_TASK_DELETE')")
    @PostMapping("/work/{workId}/delete")
    public String deleteDailyWork(
            @PathVariable Integer workId,
            Authentication authentication
    ) {

        User employee =
                getAuthenticatedEmployee(authentication);

        DailyWork existingWork =
                dailyWorkService.getWorkByIdForEmployee(
                        employee.getId(),
                        workId
                );

        // --------------------------------------------------------
        // ONLY TODAY'S WORK CAN BE DELETED
        // --------------------------------------------------------

        if (!LocalDate.now().equals(existingWork.getWorkDate())) {

            throw new IllegalStateException(
                    "Only today's work can be deleted."
            );
        }

        Integer projectId =
                existingWork.getProjectId();

        // --------------------------------------------------------
        // DELETE
        // --------------------------------------------------------

        dailyWorkService.deleteDailyWork(
                employee.getId(),
                workId
        );

        return "redirect:/employee/tasks/project/"
                + projectId;
    }



    // ============================================================
    // VIEW EMPLOYEE WORK
    // ============================================================

    @PreAuthorize("hasAuthority('PERMISSION_ACTIVITY_VIEW_TEAM')")
    @GetMapping("/employee/{employeeId}")
    public String employeeWork(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model
    ) {

        User viewer =
                getAuthenticatedEmployee(authentication);

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Employee not found."
                                )
                        );

        List<DailyWork> work =
                dailyWorkService.getEmployeeWork(
                        viewer.getId(),
                        employeeId
                );

        model.addAttribute("employee", viewer);
        model.addAttribute("workEmployee", employee);
        model.addAttribute("work", work);

        return "employeeWorkHistory";
    }

    // ============================================================
    // GET AUTHENTICATED EMPLOYEE
    // ============================================================

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

