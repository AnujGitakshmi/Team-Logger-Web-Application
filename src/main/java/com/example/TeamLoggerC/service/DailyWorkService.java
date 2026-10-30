package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.DailyWork;
import com.example.TeamLoggerC.entity.Project;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.DailyWorkRepo;
import com.example.TeamLoggerC.repository.ProjectRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional
public class DailyWorkService {

    private final DailyWorkRepo dailyWorkRepo;
    private final ProjectRepo projectRepo;
    private final UserRepo userRepo;

    public DailyWorkService(
            DailyWorkRepo dailyWorkRepo,
            ProjectRepo projectRepo,
            UserRepo userRepo
    ) {
        this.dailyWorkRepo = dailyWorkRepo;
        this.projectRepo = projectRepo;
        this.userRepo = userRepo;
    }


    // ============================================================
    // ADD DAILY WORK
    // ONLY TODAY IS ALLOWED
    // ============================================================

    public DailyWork addDailyWork(
            Integer employeeId,
            Integer projectId,
            LocalDate workDate,
            String description
    ) {

        User employee = getUser(employeeId);
        Project project = getProject(projectId);

        validateEmployee(employee);
        validateProjectAccess(employee, project);

        // --------------------------------------------------------
        // COMPLETED PROJECTS ARE READ-ONLY
        // --------------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new IllegalStateException(
                    "Daily work cannot be added to a completed project."
            );
        }

        // --------------------------------------------------------
        // DATE VALIDATION
        // --------------------------------------------------------

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "Work date is required."
            );
        }

        LocalDate today = LocalDate.now();

        // Only today's work can be added.
        if (!workDate.equals(today)) {
            throw new IllegalArgumentException(
                    "Daily work can only be added for today."
            );
        }

        // --------------------------------------------------------
        // DESCRIPTION VALIDATION
        // --------------------------------------------------------

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Work description cannot be empty."
            );
        }

        String cleanedDescription = description.trim();

        if (cleanedDescription.length() > 5000) {
            throw new IllegalArgumentException(
                    "Work description cannot exceed 5000 characters."
            );
        }

        // --------------------------------------------------------
        // CREATE DAILY WORK
        // --------------------------------------------------------

        DailyWork dailyWork = new DailyWork();

        dailyWork.setEmployeeId(employeeId);
        dailyWork.setProjectId(projectId);
        dailyWork.setWorkDate(today);
        dailyWork.setDescription(cleanedDescription);
        dailyWork.setCreatedAt(LocalDateTime.now());
        dailyWork.setUpdatedAt(null);

        return dailyWorkRepo.save(dailyWork);
    }


    // ============================================================
    // UPDATE DAILY WORK
    // ONLY TODAY'S ENTRY CAN BE EDITED
    // ============================================================

    public DailyWork updateDailyWork(
            Integer employeeId,
            Integer workId,
            LocalDate workDate,
            String description
    ) {

        User employee = getUser(employeeId);
        validateEmployee(employee);

        DailyWork dailyWork = getDailyWork(workId);

        // --------------------------------------------------------
        // ONLY THE OWNER CAN EDIT
        // --------------------------------------------------------

        if (!employeeId.equals(dailyWork.getEmployeeId())) {
            throw new IllegalStateException(
                    "You are not allowed to edit this daily work."
            );
        }

        Project project = getProject(dailyWork.getProjectId());

        // --------------------------------------------------------
        // COMPLETED PROJECTS ARE READ-ONLY
        // --------------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new IllegalStateException(
                    "Daily work of a completed project cannot be edited."
            );
        }

        // --------------------------------------------------------
        // ONLY TODAY'S EXISTING ENTRY CAN BE EDITED
        // --------------------------------------------------------

        LocalDate today = LocalDate.now();

        if (!today.equals(dailyWork.getWorkDate())) {
            throw new IllegalStateException(
                    "Only today's daily work can be edited."
            );
        }

        // --------------------------------------------------------
        // UPDATED DATE MUST ALSO BE TODAY
        // --------------------------------------------------------

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "Work date is required."
            );
        }

        if (!workDate.equals(today)) {
            throw new IllegalArgumentException(
                    "Daily work date must be today."
            );
        }

        // --------------------------------------------------------
        // DESCRIPTION VALIDATION
        // --------------------------------------------------------

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Work description cannot be empty."
            );
        }

        String cleanedDescription = description.trim();

        if (cleanedDescription.length() > 5000) {
            throw new IllegalArgumentException(
                    "Work description cannot exceed 5000 characters."
            );
        }

        // --------------------------------------------------------
        // UPDATE
        // --------------------------------------------------------

        // Keep the date as today.
        dailyWork.setWorkDate(today);

        dailyWork.setDescription(cleanedDescription);

        dailyWork.setUpdatedAt(LocalDateTime.now());

        return dailyWorkRepo.save(dailyWork);
    }


    // ============================================================
    // DELETE DAILY WORK
    // ONLY TODAY'S ENTRY CAN BE DELETED
    // ============================================================

    public void deleteDailyWork(
            Integer employeeId,
            Integer workId
    ) {

        User employee = getUser(employeeId);
        validateEmployee(employee);

        DailyWork dailyWork = getDailyWork(workId);

        // --------------------------------------------------------
        // ONLY THE OWNER CAN DELETE
        // --------------------------------------------------------

        if (!employeeId.equals(dailyWork.getEmployeeId())) {
            throw new IllegalStateException(
                    "You are not allowed to delete this daily work."
            );
        }

        Project project = getProject(dailyWork.getProjectId());

        // --------------------------------------------------------
        // COMPLETED PROJECTS ARE READ-ONLY
        // --------------------------------------------------------

        if ("COMPLETED".equalsIgnoreCase(project.getStatus())) {
            throw new IllegalStateException(
                    "Daily work of a completed project cannot be deleted."
            );
        }

        // --------------------------------------------------------
        // ONLY TODAY'S ENTRY CAN BE DELETED
        // --------------------------------------------------------

        if (!LocalDate.now().equals(dailyWork.getWorkDate())) {
            throw new IllegalStateException(
                    "Only today's daily work can be deleted."
            );
        }

        // --------------------------------------------------------
        // DELETE
        // --------------------------------------------------------

        dailyWorkRepo.delete(dailyWork);
    }


    // ============================================================
    // GET EMPLOYEE'S OWN WORK
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getOwnWork(Integer employeeId) {

        getUser(employeeId);

        return dailyWorkRepo
                .findByEmployeeIdOrderByWorkDateDescCreatedAtDesc(
                        employeeId
                );
    }


    // ============================================================
    // GET EMPLOYEE WORK FOR A PROJECT
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getOwnProjectWork(
            Integer employeeId,
            Integer projectId
    ) {

        User employee = getUser(employeeId);
        Project project = getProject(projectId);

        validateEmployee(employee);
        validateProjectAccess(employee, project);

        return dailyWorkRepo
                .findByEmployeeIdAndProjectIdOrderByWorkDateDescCreatedAtDesc(
                        employeeId,
                        projectId
                );
    }


    // ============================================================
    // GET TODAY'S OWN WORK
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getOwnTodayWork(
            Integer employeeId
    ) {

        User employee = getUser(employeeId);

        validateEmployee(employee);

        return dailyWorkRepo
                .findByEmployeeIdAndWorkDateOrderByCreatedAtDesc(
                        employeeId,
                        LocalDate.now()
                );
    }


    // ============================================================
    // GET WORK OF AN EMPLOYEE
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getEmployeeWork(
            Integer viewerId,
            Integer employeeId
    ) {

        User viewer = getUser(viewerId);
        User employee = getUser(employeeId);

        validateEmployee(viewer);
        validateEmployee(employee);

        if (!canViewEmployeeWork(viewer, employee)) {
            throw new IllegalStateException(
                    "You are not allowed to view this employee's work."
            );
        }

        return dailyWorkRepo
                .findByEmployeeIdOrderByWorkDateDescCreatedAtDesc(
                        employeeId
                );
    }


    // ============================================================
    // GET WORK FOR A PROJECT
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getProjectWork(
            Integer viewerId,
            Integer projectId
    ) {

        User viewer = getUser(viewerId);
        Project project = getProject(projectId);

        validateEmployee(viewer);
        validateProjectAccess(viewer, project);

        List<DailyWork> allWork =
                dailyWorkRepo
                        .findByProjectIdOrderByWorkDateDescCreatedAtDesc(
                                projectId
                        );

        List<DailyWork> visibleWork =
                new ArrayList<>();

        for (DailyWork work : allWork) {

            User workOwner =
                    userRepo.findById(work.getEmployeeId())
                            .orElse(null);

            if (workOwner == null) {
                continue;
            }

            if (canViewEmployeeWork(viewer, workOwner)) {
                visibleWork.add(work);
            }
        }

        return visibleWork;
    }


    // ============================================================
    // GET TEAM WORK FOR A PROJECT
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getTeamWork(
            Integer viewerId,
            Integer projectId
    ) {

        User viewer = getUser(viewerId);
        Project project = getProject(projectId);

        validateEmployee(viewer);
        validateProjectAccess(viewer, project);

        List<User> visibleEmployees =
                getVisibleEmployeesForProject(
                        viewer,
                        project
                );

        Set<Integer> visibleEmployeeIds =
                new HashSet<>();

        for (User user : visibleEmployees) {
            visibleEmployeeIds.add(user.getId());
        }

        List<DailyWork> allWork =
                dailyWorkRepo
                        .findByProjectIdOrderByWorkDateDescCreatedAtDesc(
                                projectId
                        );

        List<DailyWork> result =
                new ArrayList<>();

        for (DailyWork work : allWork) {

            if (visibleEmployeeIds.contains(
                    work.getEmployeeId()
            )) {
                result.add(work);
            }
        }

        return result;
    }


    // ============================================================
    // GET WORK OF A PARTICULAR DATE
    // ============================================================

    @Transactional(readOnly = true)
    public List<DailyWork> getProjectWorkForDate(
            Integer viewerId,
            Integer projectId,
            LocalDate workDate
    ) {

        User viewer = getUser(viewerId);
        Project project = getProject(projectId);

        validateEmployee(viewer);
        validateProjectAccess(viewer, project);

        if (workDate == null) {
            throw new IllegalArgumentException(
                    "Work date is required."
            );
        }

        List<DailyWork> allWork =
                dailyWorkRepo
                        .findByProjectIdAndWorkDateOrderByCreatedAtDesc(
                                projectId,
                                workDate
                        );

        List<DailyWork> result =
                new ArrayList<>();

        for (DailyWork work : allWork) {

            User workOwner =
                    userRepo.findById(work.getEmployeeId())
                            .orElse(null);

            if (workOwner != null
                    && canViewEmployeeWork(viewer, workOwner)) {

                result.add(work);
            }
        }

        return result;
    }


    // ============================================================
    // GET VISIBLE EMPLOYEES FOR A PROJECT
    // ============================================================

    @Transactional(readOnly = true)
    public List<User> getVisibleEmployeesForProject(
            User viewer,
            Project project
    ) {

        List<User> result = new ArrayList<>();

        // --------------------------------------------------------
        // PROJECT LEADER
        // --------------------------------------------------------

        if (isProjectLeader(viewer, project)) {

            result.add(viewer);

            List<User> allEmployees =
                    userRepo.findByAdminIdAndRole(
                            viewer.getAdminId(),
                            "EMPLOYEE"
                    );

            for (User employee : allEmployees) {

                if (employee.getId()
                        .equals(viewer.getId())) {
                    continue;
                }

                if (isUnderLeader(
                        employee,
                        viewer.getId()
                )) {

                    result.add(employee);
                }
            }

            return result;
        }


        // --------------------------------------------------------
        // SENIOR
        // --------------------------------------------------------

        if (isSenior(viewer)) {

            result.add(viewer);

            List<User> juniors =
                    userRepo.findBySeniorId(
                            viewer.getId()
                    );

            for (User junior : juniors) {

                if ("EMPLOYEE".equalsIgnoreCase(
                        junior.getRole()
                )) {
                    result.add(junior);
                }
            }

            return result;
        }


        // --------------------------------------------------------
        // JUNIOR / INTERN / OTHER EMPLOYEE
        // --------------------------------------------------------

        result.add(viewer);

        return result;
    }


    // ============================================================
    // CHECK WHETHER VIEWER CAN SEE EMPLOYEE WORK
    // ============================================================

    @Transactional(readOnly = true)
    public boolean canViewEmployeeWork(
            User viewer,
            User employee
    ) {

        // --------------------------------------------------------
        // OWN WORK
        // --------------------------------------------------------

        if (viewer.getId().equals(employee.getId())) {
            return true;
        }

        // --------------------------------------------------------
        // PROJECT LEADER
        // --------------------------------------------------------

        if ("Project Leader".equalsIgnoreCase(
                safe(viewer.getDesignation())
        )) {

            return isUnderLeader(
                    employee,
                    viewer.getId()
            );
        }

        // --------------------------------------------------------
        // SENIOR
        // --------------------------------------------------------

        if (isSenior(viewer)) {

            return employee.getSeniorId() != null
                    && employee.getSeniorId()
                    .equals(viewer.getId());
        }

        // --------------------------------------------------------
        // JUNIOR / INTERN
        // --------------------------------------------------------

        return false;
    }


    // ============================================================
    // CHECK PROJECT ACCESS
    // ============================================================

    @Transactional(readOnly = true)
    public boolean hasProjectAccess(
            Integer employeeId,
            Integer projectId
    ) {

        User employee = getUser(employeeId);
        Project project = getProject(projectId);

        try {

            validateProjectAccess(
                    employee,
                    project
            );

            return true;

        } catch (RuntimeException e) {

            return false;
        }
    }


    // ============================================================
    // PROJECT ACCESS VALIDATION
    // ============================================================

    private void validateProjectAccess(
            User employee,
            Project project
    ) {

        if (!"EMPLOYEE".equalsIgnoreCase(
                safe(employee.getRole())
        )) {

            throw new IllegalStateException(
                    "Only employees can manage daily work."
            );
        }

        // --------------------------------------------------------
        // SAME ADMIN
        // --------------------------------------------------------

        if (employee.getAdminId() == null
                || project.getAdminId() == null
                || !employee.getAdminId()
                .equals(project.getAdminId())) {

            throw new IllegalStateException(
                    "You do not have access to this project."
            );
        }

        // --------------------------------------------------------
        // PROJECT LEADER
        // --------------------------------------------------------

        if (employee.getId()
                .equals(project.getProjectLeaderId())) {

            return;
        }

        // --------------------------------------------------------
        // EMPLOYEE MUST BE UNDER PROJECT LEADER
        // --------------------------------------------------------

        if (!isUnderLeader(
                employee,
                project.getProjectLeaderId()
        )) {

            throw new IllegalStateException(
                    "You are not a member of this project."
            );
        }
    }


    // ============================================================
    // CHECK IF EMPLOYEE IS UNDER PROJECT LEADER
    // ============================================================

    private boolean isUnderLeader(
            User employee,
            Integer projectLeaderId
    ) {

        if (employee == null
                || projectLeaderId == null) {

            return false;
        }

        Set<Integer> visited =
                new HashSet<>();

        User current = employee;

        while (current != null) {

            if (current.getId() == null) {
                return false;
            }

            // ----------------------------------------------------
            // PROTECT AGAINST BAD/CYCLIC HIERARCHY DATA
            // ----------------------------------------------------

            if (!visited.add(current.getId())) {
                return false;
            }

            Integer parentId =
                    current.getSeniorId();

            if (parentId == null) {
                return false;
            }

            if (parentId.equals(projectLeaderId)) {
                return true;
            }

            current =
                    userRepo.findById(parentId)
                            .orElse(null);
        }

        return false;
    }


    // ============================================================
    // CHECK PROJECT LEADER
    // ============================================================

    private boolean isProjectLeader(
            User employee,
            Project project
    ) {

        return employee.getId() != null
                && project.getProjectLeaderId() != null
                && employee.getId()
                .equals(project.getProjectLeaderId());
    }


    // ============================================================
    // CHECK SENIOR
    // ============================================================

    private boolean isSenior(User employee) {

        String designation =
                safe(employee.getDesignation());

        return designation.equalsIgnoreCase("Senior")
                || designation.equalsIgnoreCase("Senior Developer")
                || designation.equalsIgnoreCase("Senior Employee");
    }


    // ============================================================
    // VALIDATE EMPLOYEE
    // ============================================================

    private void validateEmployee(User employee) {

        if (employee == null) {
            throw new IllegalStateException(
                    "Employee not found."
            );
        }

        if (!"EMPLOYEE".equalsIgnoreCase(
                safe(employee.getRole())
        )) {

            throw new IllegalStateException(
                    "User is not an employee."
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(
                safe(employee.getStatus())
        )) {

            throw new IllegalStateException(
                    "Inactive employees cannot manage daily work."
            );
        }
    }


    // ============================================================
    // GET USER
    // ============================================================

    private User getUser(Integer employeeId) {

        if (employeeId == null) {
            throw new IllegalArgumentException(
                    "Employee ID is required."
            );
        }

        return userRepo.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Employee not found."
                        )
                );
    }


    // ============================================================
    // GET PROJECT
    // ============================================================

    private Project getProject(Integer projectId) {

        if (projectId == null) {
            throw new IllegalArgumentException(
                    "Project ID is required."
            );
        }

        return projectRepo.findById(projectId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Project not found."
                        )
                );
    }


    // ============================================================
    // GET DAILY WORK
    // ============================================================

    private DailyWork getDailyWork(Integer workId) {

        if (workId == null) {
            throw new IllegalArgumentException(
                    "Daily work ID is required."
            );
        }

        return dailyWorkRepo.findById(workId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Daily work entry not found."
                        )
                );
    }


    // ============================================================
    // SAFE STRING
    // ============================================================

    private String safe(String value) {

        return value == null
                ? ""
                : value.trim();
    }


    // ============================================================
    // GET DAILY WORK BY ID FOR OWNER
    // ============================================================

    @Transactional(readOnly = true)
    public DailyWork getWorkByIdForEmployee(
            Integer employeeId,
            Integer workId
    ) {

        User employee = getUser(employeeId);

        validateEmployee(employee);

        DailyWork work = getDailyWork(workId);

        // --------------------------------------------------------
        // ONLY THE OWNER CAN ACCESS THIS ENTRY
        // --------------------------------------------------------

        if (!employeeId.equals(work.getEmployeeId())) {
            throw new IllegalStateException(
                    "You are not allowed to access this daily work."
            );
        }

        return work;
    }
}

