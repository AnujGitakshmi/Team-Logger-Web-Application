package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.ProjectRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectService {

    private final ProjectRepo projectRepo;
    private final UserRepo userRepo;
    private final NotificationService notificationService;
    private final PermissionService permissionService;

    public ProjectService(
            ProjectRepo projectRepo,
            UserRepo userRepo,
            NotificationService notificationService,
            PermissionService permissionService
    ) {
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
        this.notificationService = notificationService;
        this.permissionService = permissionService;
    }


    // ============================================================
    // CREATE PROJECT
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Project createProject(
            Project project,
            Integer adminId
    ) {

        if (project.getProjectLeaderId() == null) {

            throw new IllegalArgumentException(
                    "Project Leader must be selected."
            );
        }

        if (project.getStartDate() == null) {

            throw new IllegalArgumentException(
                    "Project start date is required."
            );
        }

        if (project.getExpectedCompletionDate() == null) {

            throw new IllegalArgumentException(
                    "Expected completion date is required."
            );
        }

        if (project.getExpectedCompletionDate()
                .isBefore(project.getStartDate())) {

            throw new IllegalArgumentException(
                    "Expected completion date cannot be before start date."
            );
        }


        // --------------------------------------------------------
        // VERIFY PROJECT LEADER
        // --------------------------------------------------------

        User leader = userRepo.findById(
                project.getProjectLeaderId()
        ).orElseThrow(() ->
                new IllegalArgumentException(
                        "Selected Project Leader does not exist."
                )
        );


        // --------------------------------------------------------
        // PROJECT LEADER MUST BELONG TO CURRENT ADMIN
        // --------------------------------------------------------

        if (leader.getAdminId() == null
                || !leader.getAdminId().equals(adminId)) {

            throw new IllegalArgumentException(
                    "Selected Project Leader does not belong to your organization."
            );
        }


        // --------------------------------------------------------
        // PROJECT LEADER MUST BE ACTIVE
        // --------------------------------------------------------

        if (leader.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(
                leader.getStatus().trim()
        )) {

            throw new IllegalArgumentException(
                    "Selected Project Leader is not active."
            );
        }


        // --------------------------------------------------------
        // VERIFY PROJECT LEADER LEVEL
        // --------------------------------------------------------

        if (!isProjectLeader(leader)) {

            throw new IllegalArgumentException(
                    "Selected employee is not a Project Leader."
            );
        }


        // --------------------------------------------------------
        // ASSIGN ADMIN
        // --------------------------------------------------------

        project.setAdminId(adminId);


        // --------------------------------------------------------
        // NEW PROJECT ALWAYS STARTS AS ASSIGNED
        // --------------------------------------------------------

        project.setStatus("ASSIGNED");


        // --------------------------------------------------------
        // CREATED TIME
        // --------------------------------------------------------

        project.setCreatedAt(
                LocalDateTime.now()
        );


        // --------------------------------------------------------
        // NO COMPLETION WHEN CREATED
        // --------------------------------------------------------

        project.setCompletedAt(null);
        project.setCompletionRequestedAt(null);
        project.setCompletionRequestedBy(null);


        // --------------------------------------------------------
        // SAVE PROJECT FIRST
        // --------------------------------------------------------

        Project savedProject =
                projectRepo.save(project);


        // --------------------------------------------------------
        // SEND PROJECT NOTIFICATIONS
        // --------------------------------------------------------

        notifyProjectAssignment(
                savedProject,
                adminId
        );


        return savedProject;
    }


    // ============================================================
    // NOTIFY PROJECT ASSIGNMENT
    // ============================================================

    private void notifyProjectAssignment(
            Project project,
            Integer adminId
    ) {

        Integer leaderId =
                project.getProjectLeaderId();

        if (leaderId == null) {
            return;
        }

        Set<Integer> notifiedUserIds =
                new HashSet<>();

        sendProjectNotification(
                project,
                leaderId,
                adminId
        );

        notifiedUserIds.add(leaderId);

        List<User> teamMembers =
                getAllEmployeesUnderLeader(
                        leaderId
                );

        for (User employee : teamMembers) {

            if (employee.getId() == null) {
                continue;
            }

            if (notifiedUserIds.contains(
                    employee.getId()
            )) {
                continue;
            }

            if (employee.getStatus() != null
                    && !"ACTIVE".equalsIgnoreCase(
                    employee.getStatus()
            )) {
                continue;
            }

            sendProjectNotification(
                    project,
                    employee.getId(),
                    adminId
            );

            notifiedUserIds.add(
                    employee.getId()
            );
        }
    }


    // ============================================================
    // SEND PROJECT NOTIFICATION
    // ============================================================

    private void sendProjectNotification(
            Project project,
            Integer recipientId,
            Integer adminId
    ) {

        String projectName =
                project.getProjectName();

        notificationService.createNotification(
                recipientId,
                adminId,
                "PROJECT",
                "New Project Assigned",
                "You have been assigned to project \""
                        + projectName
                        + "\".",
                project.getId()
        );
    }


    // ============================================================
    // FIND ALL EMPLOYEES UNDER PROJECT LEADER
    // ============================================================

    private List<User> getAllEmployeesUnderLeader(
            Integer projectLeaderId
    ) {

        List<User> allEmployees =
                userRepo.findAll();

        List<User> teamMembers =
                new ArrayList<>();

        for (User employee : allEmployees) {

            if (employee.getId() == null) {
                continue;
            }

            if (employee.getId().equals(
                    projectLeaderId
            )) {
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
    // CHECK WHETHER EMPLOYEE IS UNDER LEADER
    // ============================================================

    private boolean isUnderLeader(
            User employee,
            Integer projectLeaderId
    ) {

        User current = employee;

        Set<Integer> visited =
                new HashSet<>();

        while (current != null) {

            if (current.getId() != null) {

                if (!visited.add(
                        current.getId()
                )) {

                    return false;
                }
            }

            Integer parentId =
                    current.getSeniorId();

            if (parentId == null) {
                return false;
            }

            if (parentId.equals(
                    projectLeaderId
            )) {

                return true;
            }

            current =
                    userRepo.findById(
                            parentId
                    ).orElse(null);
        }

        return false;
    }


    // ============================================================
    // GET ALL PROJECTS FOR ADMIN
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public List<Project> getAdminProjects(
            Integer adminId
    ) {

        updateExpiredProjects(adminId);

        return projectRepo.findByAdminId(adminId);
    }


    // ============================================================
    // GET SINGLE PROJECT
    // ============================================================

    @Transactional
    public Project getProject(
            Integer projectId
    ) {

        Project project =
                projectRepo.findById(projectId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project not found."
                                )
                        );

        updateProjectStatus(project);

        return project;
    }


    // ============================================================
    // GET PROJECTS LED BY PROJECT LEADER
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_VIEW')")
    public List<Project> getLeaderProjects(
            Integer leaderId
    ) {

        List<Project> projects =
                projectRepo.findByProjectLeaderId(
                        leaderId
                );

        for (Project project : projects) {

            updateProjectStatus(project);
        }

        return projects;
    }


    // ============================================================
    // GET PROJECTS VISIBLE TO EMPLOYEE
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_VIEW')")
    public List<Project> getProjectsForEmployee(
            Integer employeeId
    ) {

        List<Project> visibleProjects =
                new ArrayList<>();

        User employee =
                userRepo.findById(employeeId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Employee not found."
                                )
                        );

        List<Integer> leaderIds =
                findProjectLeaderIds(employee);

        for (Integer leaderId : leaderIds) {

            List<Project> projects =
                    projectRepo.findByProjectLeaderId(
                            leaderId
                    );

            for (Project project : projects) {

                updateProjectStatus(project);

                if (!containsProject(
                        visibleProjects,
                        project.getId()
                )) {

                    visibleProjects.add(project);
                }
            }
        }

        return visibleProjects;
    }


    // ============================================================
    // REQUEST PROJECT COMPLETION
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('PERMISSION_PROJECT_COMPLETE_REQUEST')")
    public Project requestCompletion(
            Integer projectId,
            Integer employeeId
    ) {

        Project project =
                projectRepo.findById(projectId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project not found."
                                )
                        );

        if (project.getProjectLeaderId() == null
                || !project.getProjectLeaderId()
                .equals(employeeId)) {

            throw new IllegalArgumentException(
                    "Only the Project Leader can request project completion."
            );
        }

        if ("COMPLETED".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Project is already completed."
            );
        }

        if ("PENDING".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Project completion is already pending admin confirmation."
            );
        }

        if (!"ASSIGNED".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Project cannot be submitted for completion."
            );
        }

        project.setStatus("PENDING");

        project.setCompletionRequestedBy(
                employeeId
        );

        project.setCompletionRequestedAt(
                LocalDateTime.now()
        );

        project.setCompletedAt(null);

        return projectRepo.save(project);
    }


    // ============================================================
    // ADMIN CONFIRMS PROJECT COMPLETION
    // ============================================================

    @Transactional
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public Project confirmCompletion(
            Integer projectId,
            Integer adminId
    ) {

        Project project =
                projectRepo.findById(projectId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Project not found."
                                )
                        );

        if (project.getAdminId() == null
                || !project.getAdminId()
                .equals(adminId)) {

            throw new IllegalArgumentException(
                    "You are not authorized to complete this project."
            );
        }

        if (!"PENDING".equalsIgnoreCase(
                project.getStatus()
        )) {

            throw new IllegalArgumentException(
                    "Project completion has not been requested."
            );
        }

        project.setStatus("COMPLETED");

        project.setCompletedAt(
                LocalDateTime.now()
        );

        return projectRepo.save(project);
    }


    // ============================================================
    // UPDATE EXPIRED PROJECTS FOR ADMIN
    // ============================================================

    @Transactional
    public void updateExpiredProjects(
            Integer adminId
    ) {

        List<Project> projects =
                projectRepo.findByAdminId(adminId);

        for (Project project : projects) {

            updateProjectStatus(project);
        }
    }


    // ============================================================
    // UPDATE PROJECT STATUS
    // ============================================================

    private void updateProjectStatus(
            Project project
    ) {

        if ("COMPLETED".equalsIgnoreCase(
                project.getStatus()
        )) {

            return;
        }
    }


    // ============================================================
    // CHECK PROJECT LEADER
    // ============================================================

    private boolean isProjectLeader(
            User user
    ) {

        if (user == null
                || user.getDesignation() == null) {

            return false;
        }

        try {

            return permissionService
                    .getEmployeeLevel(user)
                    .name()
                    .equals("PROJECT_LEADER");

        } catch (IllegalStateException ex) {

            return false;
        }
    }


    // ============================================================
    // FIND PROJECT LEADER(S) ABOVE EMPLOYEE
    // ============================================================

    private List<Integer> findProjectLeaderIds(
            User employee
    ) {

        List<Integer> leaderIds =
                new ArrayList<>();

        User current = employee;

        Set<Integer> visited =
                new HashSet<>();

        while (current != null) {

            if (current.getId() != null
                    && !visited.add(
                    current.getId()
            )) {

                break;
            }

            if (isProjectLeader(current)) {

                if (current.getId() != null) {

                    leaderIds.add(
                            current.getId()
                    );
                }

                break;
            }

            Integer parentId =
                    getParentId(current);

            if (parentId == null) {
                break;
            }

            current =
                    userRepo.findById(
                            parentId
                    ).orElse(null);
        }

        return leaderIds;
    }


    // ============================================================
    // GET EMPLOYEE'S PARENT
    // ============================================================

    private Integer getParentId(
            User user
    ) {

        return user.getSeniorId();
    }


    // ============================================================
    // CHECK DUPLICATE PROJECT
    // ============================================================

    private boolean containsProject(
            List<Project> projects,
            Integer projectId
    ) {

        for (Project project : projects) {

            if (project.getId() != null
                    && project.getId().equals(projectId)) {

                return true;
            }
        }

        return false;
    }
}