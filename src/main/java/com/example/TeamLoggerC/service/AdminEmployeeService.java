package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.EmployeeFormDTO;
import com.example.TeamLoggerC.dto.ReportingManagerDTO;
import com.example.TeamLoggerC.entity.Team;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.TeamRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class AdminEmployeeService {

    private final UserRepo userRepo;

    private final PasswordEncoder passwordEncoder;

    private final TeamRepo teamRepo;

    private final AuditLogService auditLogService;


    public AdminEmployeeService(
            UserRepo userRepo,
            TeamRepo teamRepo,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService) {

        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.teamRepo = teamRepo;
        this.auditLogService = auditLogService;
    }


    // =====================================================
    // GET EMPLOYEES OF ADMIN
    // =====================================================

    public List<User> getEmployees(Integer adminId) {

        return userRepo.findByAdminIdAndRole(
                adminId,
                "EMPLOYEE"
        );
    }


    // =====================================================
    // GET ALL TEAMS OF ADMIN
    // =====================================================

    public List<Team> getAllTeams(Integer adminId) {

        return teamRepo.findAllByAdminIdOrderByTeamNameAsc(
                adminId
        );
    }


    // =====================================================
    // GET REPORTING MANAGERS
    // =====================================================

    public List<ReportingManagerDTO> getReportingManagers(
            Integer adminId,
            Integer teamId,
            String domain,
            String designation) {

        if (domain == null ||
                domain.trim().isEmpty()) {

            return List.of();
        }

        if (designation == null ||
                designation.trim().isEmpty()) {

            return List.of();
        }

        if (teamId == null) {

            return List.of();
        }


        // -------------------------------------------------
        // VERIFY TEAM BELONGS TO CURRENT ADMIN
        // -------------------------------------------------

        if (!teamRepo.findByIdAndAdminId(
                teamId,
                adminId
        ).isPresent()) {

            return List.of();
        }


        String parentDesignation =
                getParentDesignation(
                        domain,
                        designation
                );


        if (parentDesignation == null) {

            return List.of();
        }


        List<User> managers =
                userRepo
                        .findByAdminIdAndRoleAndDomainIgnoreCaseAndDesignationIgnoreCaseAndTeamId(
                                adminId,
                                "EMPLOYEE",
                                domain.trim(),
                                parentDesignation,
                                teamId
                        );


        return managers.stream()
                .filter(user ->
                        "ACTIVE".equalsIgnoreCase(
                                user.getStatus()
                        )
                )
                .map(user ->
                        new ReportingManagerDTO(
                                user.getId(),
                                user.getName(),
                                user.getEmployeeLoginId(),
                                user.getDesignation()
                        )
                )
                .toList();
    }


    // =====================================================
    // ADD EMPLOYEE
    // =====================================================

    @Transactional
    public User addEmployee(
            Integer adminId,
            EmployeeFormDTO dto) {

        validateBasicFields(dto);


        String email =
                dto.getEmail().trim();

        String domain =
                dto.getDomain().trim();

        String hierarchyLevel =
                dto.getHierarchyLevel() == null
                        ? ""
                        : dto.getHierarchyLevel().trim();


        // -------------------------------------------------
        // EMAIL DUPLICATE CHECK
        // -------------------------------------------------

        if (userRepo.findByEmail(email).isPresent()) {

            throw new IllegalArgumentException(
                    "Email already exists."
            );
        }


        // -------------------------------------------------
        // DETERMINE DESIGNATION
        // -------------------------------------------------

        String designation =
                resolveDesignation(
                        domain,
                        hierarchyLevel
                );


        if (designation == null) {

            throw new IllegalArgumentException(
                    "Invalid hierarchy level for selected domain."
            );
        }


        // -------------------------------------------------
        // PROJECT LEADER
        // -------------------------------------------------

        boolean projectLeader =
                isTopLevelDesignation(
                        domain,
                        designation
                );


        Team team;


        if (projectLeader) {

            // ---------------------------------------------
            // PROJECT LEADER MUST CREATE NEW TEAM
            // ---------------------------------------------

            if (dto.getNewTeamName() == null ||
                    dto.getNewTeamName().trim().isEmpty()) {

                throw new IllegalArgumentException(
                        "New team name is required for a Project Leader."
                );
            }


            String newTeamName =
                    dto.getNewTeamName().trim();


            // ---------------------------------------------
            // TEAM NAME DUPLICATE
            // ---------------------------------------------

            if (teamRepo.findByAdminIdAndTeamName(
                    adminId,
                    newTeamName
            ).isPresent()) {

                throw new IllegalArgumentException(
                        "A team with the name '" +
                                newTeamName +
                                "' already exists in your organization."
                );
            }


            // ---------------------------------------------
            // CREATE TEAM
            // ---------------------------------------------

            team = new Team();

            team.setTeamName(
                    newTeamName
            );

            team.setAdminId(
                    adminId
            );

            team =
                    teamRepo.save(team);

        } else {

            // ---------------------------------------------
            // CHILD EMPLOYEE MUST SELECT TEAM
            // ---------------------------------------------

            if (dto.getTeamId() == null) {

                throw new IllegalArgumentException(
                        "Please select a team."
                );
            }


            // ---------------------------------------------
            // GET TEAM + VERIFY ADMIN OWNERSHIP
            // ---------------------------------------------

            team =
                    teamRepo.findByIdAndAdminId(
                            dto.getTeamId(),
                            adminId
                    ).orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected team does not belong to your organization."
                            )
                    );
        }


        // -------------------------------------------------
        // VALIDATE REPORTING MANAGER
        // -------------------------------------------------

        User senior = null;


        if (!projectLeader) {

            if (dto.getSeniorId() == null) {

                throw new IllegalArgumentException(
                        designation +
                                " must have a reporting manager."
                );
            }


            senior =
                    userRepo.findById(
                            dto.getSeniorId()
                    ).orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected reporting manager was not found."
                            )
                    );


            // SAME ADMIN
            if (!adminId.equals(
                    senior.getAdminId())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager does not belong to your administration."
                );
            }


            // MUST BE EMPLOYEE
            if (!"EMPLOYEE".equalsIgnoreCase(
                    senior.getRole())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager is not an employee."
                );
            }


            // MUST BE ACTIVE
            if (!"ACTIVE".equalsIgnoreCase(
                    senior.getStatus())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager is not active."
                );
            }


            // SAME DOMAIN
            if (senior.getDomain() == null ||
                    !domain.equalsIgnoreCase(
                            senior.getDomain()
                    )) {

                throw new IllegalArgumentException(
                        "Employee and reporting manager must belong to the same domain."
                );
            }


            // SAME TEAM
            if (senior.getTeamId() == null ||
                    !team.getId().equals(
                            senior.getTeamId()
                    )) {

                throw new IllegalArgumentException(
                        "Employee and reporting manager must belong to the same team."
                );
            }


            // CORRECT PARENT
            String requiredParent =
                    getParentDesignation(
                            domain,
                            designation
                    );


            if (requiredParent == null ||
                    senior.getDesignation() == null ||
                    !requiredParent.equalsIgnoreCase(
                            senior.getDesignation()
                    )) {

                throw new IllegalArgumentException(
                        designation +
                                " must report to " +
                                requiredParent +
                                "."
                );
            }
        }


        // -------------------------------------------------
        // GENERATE LOGIN ID
        // -------------------------------------------------

        String loginId =
                generateEmployeeLoginId(
                        domain,
                        designation,
                        senior
                );


        // -------------------------------------------------
        // CREATE USER
        // -------------------------------------------------

        User employee =
                new User();


        employee.setName(
                dto.getName().trim()
        );


        employee.setEmail(
                email
        );


        employee.setPassword(
                passwordEncoder.encode(
                        dto.getPassword()
                )
        );


        employee.setPasswordChangedAt(
                LocalDateTime.now()
        );


        employee.setPasswordVersion(1);


        employee.setPasswordMustChange(false);


        employee.setRole(
                "EMPLOYEE"
        );


        // ORGANIZATION OWNER
        employee.setAdminId(
                adminId
        );


        employee.setDomain(
                domain
        );


        employee.setEmployeeLoginId(
                loginId
        );


        employee.setDesignation(
                designation
        );


        // ALWAYS ASSIGN TEAM
        employee.setTeamId(
                team.getId()
        );


        // PROJECT LEADER = NO SENIOR
        if (projectLeader) {

            employee.setSeniorId(
                    null
            );

        } else {

            employee.setSeniorId(
                    senior.getId()
            );
        }


        employee.setStatus(
                "ACTIVE"
        );


        employee.setDisabledAt(
                null
        );


        employee.setScheduledDeletionAt(
                null
        );


        employee.setApplicationName(
                null
        );


        // -------------------------------------------------
        // SAVE EMPLOYEE
        // -------------------------------------------------

        User savedEmployee =
                userRepo.save(employee);


        // -------------------------------------------------
        // AUDIT LOG
        // -------------------------------------------------

        User admin =
                userRepo.findById(adminId)
                        .orElse(null);


        auditLogService.record(
                admin,
                "EMPLOYEE_CREATED",
                "USER",
                savedEmployee.getId().toString(),
                "Employee '" +
                        savedEmployee.getName() +
                        "' was created.",
                "SUCCESS"
        );


        return savedEmployee;
    }


    // =====================================================
    // BASIC VALIDATION
    // =====================================================

    private void validateBasicFields(
            EmployeeFormDTO dto) {

        if (dto.getName() == null ||
                dto.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Employee name is required."
            );
        }


        if (dto.getEmail() == null ||
                dto.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Email is required."
            );
        }


        if (dto.getPassword() == null ||
                dto.getPassword().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Password is required."
            );
        }


        if (dto.getDomain() == null ||
                dto.getDomain().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Domain is required."
            );
        }


        if (dto.getHierarchyLevel() == null ||
                dto.getHierarchyLevel().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Hierarchy level is required."
            );
        }
    }


    // =====================================================
    // RESOLVE DESIGNATION
    // =====================================================

    private String resolveDesignation(
            String domain,
            String hierarchyLevel) {

        String d =
                domain.toLowerCase(
                        Locale.ROOT
                ).trim();

        String level =
                hierarchyLevel.toLowerCase(
                        Locale.ROOT
                ).trim();


        return switch (d) {

            case "development" -> switch (level) {

                case "project_leader" ->
                        "Project Leader";

                case "senior" ->
                        "Senior Developer";

                case "junior" ->
                        "Junior Developer";

                case "intern" ->
                        "Developer Intern";

                default ->
                        null;
            };


            case "qa" -> switch (level) {

                case "project_leader" ->
                        "QA Lead";

                case "senior" ->
                        "Senior QA Engineer";

                case "junior" ->
                        "Junior QA Engineer";

                case "intern" ->
                        "QA Intern";

                default ->
                        null;
            };


            case "hr" -> switch (level) {

                case "project_leader" ->
                        "HR Head";

                case "senior" ->
                        "Senior HR";

                case "junior" ->
                        "Junior HR";

                case "intern" ->
                        "HR Intern";

                default ->
                        null;
            };


            case "finance" -> switch (level) {

                case "project_leader" ->
                        "Finance Lead";

                case "senior" ->
                        "Senior Finance Executive";

                case "junior" ->
                        "Junior Finance Executive";

                case "intern" ->
                        "Finance Intern";

                default ->
                        null;
            };


            case "ui/ux" -> switch (level) {

                case "project_leader" ->
                        "UI/UX Lead";

                case "senior" ->
                        "Senior UI/UX Designer";

                case "junior" ->
                        "Junior UI/UX Designer";

                case "intern" ->
                        "UI/UX Intern";

                default ->
                        null;
            };


            default ->
                    null;
        };
    }


    // =====================================================
    // DESIGNATION VALIDATION
    // =====================================================

    private boolean isValidDesignation(
            String domain,
            String designation) {

        String d =
                domain.toLowerCase(
                        Locale.ROOT
                ).trim();

        String role =
                designation.toLowerCase(
                        Locale.ROOT
                ).trim();


        return switch (d) {

            case "development" ->
                    role.equals("project leader") ||
                            role.equals("senior developer") ||
                            role.equals("junior developer") ||
                            role.equals("developer intern");

            case "qa" ->
                    role.equals("qa lead") ||
                            role.equals("senior qa engineer") ||
                            role.equals("junior qa engineer") ||
                            role.equals("qa intern");

            case "hr" ->
                    role.equals("hr head") ||
                            role.equals("senior hr") ||
                            role.equals("junior hr") ||
                            role.equals("hr intern");

            case "finance" ->
                    role.equals("finance lead") ||
                            role.equals("senior finance executive") ||
                            role.equals("junior finance executive") ||
                            role.equals("finance intern");

            case "ui/ux" ->
                    role.equals("ui/ux lead") ||
                            role.equals("senior ui/ux designer") ||
                            role.equals("junior ui/ux designer") ||
                            role.equals("ui/ux intern");

            default ->
                    false;
        };
    }


    // =====================================================
    // GET PARENT DESIGNATION
    // =====================================================

    private String getParentDesignation(
            String domain,
            String designation) {

        String d =
                domain.toLowerCase(
                        Locale.ROOT
                ).trim();

        String role =
                designation.toLowerCase(
                        Locale.ROOT
                ).trim();


        return switch (d) {

            case "development" -> {

                if (role.equals(
                        "senior developer")) {

                    yield "Project Leader";
                }

                if (role.equals(
                        "junior developer")) {

                    yield "Senior Developer";
                }

                if (role.equals(
                        "developer intern")) {

                    yield "Junior Developer";
                }

                yield null;
            }


            case "qa" -> {

                if (role.equals(
                        "senior qa engineer")) {

                    yield "QA Lead";
                }

                if (role.equals(
                        "junior qa engineer")) {

                    yield "Senior QA Engineer";
                }

                if (role.equals(
                        "qa intern")) {

                    yield "Junior QA Engineer";
                }

                yield null;
            }


            case "hr" -> {

                if (role.equals(
                        "senior hr")) {

                    yield "HR Head";
                }

                if (role.equals(
                        "junior hr")) {

                    yield "Senior HR";
                }

                if (role.equals(
                        "hr intern")) {

                    yield "Junior HR";
                }

                yield null;
            }


            case "finance" -> {

                if (role.equals(
                        "senior finance executive")) {

                    yield "Finance Lead";
                }


                if (role.equals(
                        "junior finance executive")) {

                    yield "Senior Finance Executive";
                }


                if (role.equals(
                        "finance intern")) {

                    yield "Junior Finance Executive";
                }


                yield null;
            }


            case "ui/ux" -> {

                if (role.equals(
                        "senior ui/ux designer")) {

                    yield "UI/UX Lead";
                }

                if (role.equals(
                        "junior ui/ux designer")) {

                    yield "Senior UI/UX Designer";
                }

                if (role.equals(
                        "ui/ux intern")) {

                    yield "Junior UI/UX Designer";
                }

                yield null;
            }


            default ->
                    null;
        };
    }


    // =====================================================
    // TOP LEVEL
    // =====================================================

    private boolean isTopLevelDesignation(
            String domain,
            String designation) {

        return getParentDesignation(
                domain,
                designation
        ) == null;
    }


    // =====================================================
    // LOGIN ID
    // =====================================================

    private String generateEmployeeLoginId(
            String domain,
            String designation,
            User senior) {

        String domainCode =
                getDomainCode(domain);

        String designationCode =
                getDesignationCode(
                        designation
                );


        if (senior == null) {

            int sequence = 1;

            String loginId;

            do {

                loginId =
                        domainCode +
                                "-" +
                                designationCode +
                                "-" +
                                String.format(
                                        "%03d",
                                        sequence
                                );

                sequence++;

            } while (
                    userRepo.findByEmployeeLoginId(
                            loginId
                    ).isPresent()
            );

            return loginId;
        }


        String parentId =
                senior.getEmployeeLoginId();


        int sequence = 1;

        String loginId;


        do {

            loginId =
                    domainCode +
                            "-" +
                            designationCode +
                            "-" +
                            extractParentReference(
                                    parentId
                            ) +
                            "-" +
                            String.format(
                                    "%02d",
                                    sequence
                            );

            sequence++;

        } while (
                userRepo.findByEmployeeLoginId(
                        loginId
                ).isPresent()
        );


        return loginId;
    }


    // =====================================================
    // DOMAIN CODE
    // =====================================================

    private String getDomainCode(
            String domain) {

        return switch (
                domain.toLowerCase(
                        Locale.ROOT
                ).trim()) {

            case "development" ->
                    "DEV";

            case "qa" ->
                    "QA";

            case "hr" ->
                    "HR";

            case "finance" ->
                    "FIN";

            case "ui/ux" ->
                    "UIUX";

            default ->
                    domain
                            .replaceAll(
                                    "[^A-Za-z]",
                                    ""
                            )
                            .toUpperCase(
                                    Locale.ROOT
                            );
        };
    }


    // =====================================================
    // DESIGNATION CODE
    // =====================================================

    private String getDesignationCode(
            String designation) {

        String role =
                designation.toLowerCase(
                        Locale.ROOT
                ).trim();


        return switch (role) {

            case "project leader" ->
                    "PL";

            case "senior developer" ->
                    "SD";

            case "junior developer" ->
                    "JD";

            case "developer intern" ->
                    "IN";


            case "qa lead" ->
                    "QL";

            case "senior qa engineer" ->
                    "SQ";

            case "junior qa engineer" ->
                    "JQ";

            case "qa intern" ->
                    "IN";


            case "hr head" ->
                    "HH";

            case "senior hr" ->
                    "SH";

            case "junior hr" ->
                    "JH";

            case "hr intern" ->
                    "IN";


            case "finance lead" ->
                    "FL";

            case "senior finance executive" ->
                    "SF";

            case "junior finance executive" ->
                    "JF";

            case "finance intern" ->
                    "IN";


            case "ui/ux lead" ->
                    "UL";

            case "senior ui/ux designer" ->
                    "SU";

            case "junior ui/ux designer" ->
                    "JU";

            case "ui/ux intern" ->
                    "IN";


            default ->
                    "EMP";
        };
    }


    // =====================================================
    // PARENT REFERENCE
    // =====================================================

    private String extractParentReference(
            String parentId) {

        if (parentId == null ||
                parentId.isBlank()) {

            return "ROOT";
        }


        String[] parts =
                parentId.split("-");


        if (parts.length >= 3) {

            return parts[1] +
                    parts[2];
        }


        return parentId.replace(
                "-",
                ""
        );
    }


    // =====================================================
    // GET SINGLE EMPLOYEE
    // =====================================================

    public User getEmployee(
            Integer adminId,
            Integer employeeId) {

        User employee =
                userRepo.findById(
                        employeeId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Employee not found."
                        )
                );


        if (!adminId.equals(
                employee.getAdminId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to access this employee."
            );
        }


        if (!"EMPLOYEE".equalsIgnoreCase(
                employee.getRole())) {

            throw new IllegalArgumentException(
                    "User is not an employee."
            );
        }


        return employee;
    }


    // =====================================================
    // DISABLE
    // =====================================================

    public void disableEmployee(
            Integer adminId,
            Integer employeeId) {

        User employee =
                getEmployee(
                        adminId,
                        employeeId
                );


        employee.setStatus(
                "INACTIVE"
        );


        employee.setDisabledAt(
                LocalDateTime.now()
        );


        userRepo.save(employee);


        User admin =
                userRepo.findById(adminId)
                        .orElse(null);


        auditLogService.record(
                admin,
                "EMPLOYEE_DISABLED",
                "USER",
                employee.getId().toString(),
                "Employee '" +
                        employee.getName() +
                        "' was disabled.",
                "SUCCESS"
        );
    }


    // =====================================================
    // ENABLE
    // =====================================================

    public void enableEmployee(
            Integer adminId,
            Integer employeeId) {

        User employee =
                getEmployee(
                        adminId,
                        employeeId
                );


        employee.setStatus(
                "ACTIVE"
        );


        employee.setDisabledAt(
                null
        );


        employee.setScheduledDeletionAt(
                null
        );


        userRepo.save(employee);


        User admin =
                userRepo.findById(adminId)
                        .orElse(null);


        auditLogService.record(
                admin,
                "EMPLOYEE_ENABLED",
                "USER",
                employee.getId().toString(),
                "Employee '" +
                        employee.getName() +
                        "' was enabled.",
                "SUCCESS"
        );
    }


    // =====================================================
    // TEAM NAME
    // =====================================================

    public String getTeamName(
            Integer adminId,
            Integer teamId) {

        if (teamId == null) {
            return "-";
        }


        return teamRepo.findByIdAndAdminId(
                        teamId,
                        adminId
                )
                .map(Team::getTeamName)
                .orElse("-");
    }


    // =====================================================
    // SENIOR NAME
    // =====================================================

    public String getSeniorName(
            Integer seniorId) {

        if (seniorId == null) {
            return "-";
        }


        return userRepo.findById(
                        seniorId
                )
                .map(User::getName)
                .orElse("-");
    }


    // =====================================================
    // OLD METHOD - OPTIONAL COMPATIBILITY
    // =====================================================

    public List<User> getReportingManagers(
            Integer adminId) {

        return userRepo.findByAdminIdAndRole(
                adminId,
                "EMPLOYEE"
        );
    }


    // =====================================================
    // UPDATE EMPLOYEE
    // =====================================================

    @Transactional
    public User updateEmployee(
            Integer adminId,
            Integer employeeId,
            EmployeeFormDTO dto) {

        // -------------------------------------------------
        // GET EMPLOYEE + VERIFY ADMIN OWNERSHIP
        // -------------------------------------------------

        User employee =
                getEmployee(
                        adminId,
                        employeeId
                );


        // -------------------------------------------------
        // BASIC VALIDATION
        // -------------------------------------------------

        if (dto.getName() == null ||
                dto.getName().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Employee name is required."
            );
        }


        if (dto.getEmail() == null ||
                dto.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Email is required."
            );
        }


        if (dto.getDomain() == null ||
                dto.getDomain().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Domain is required."
            );
        }


        if (dto.getHierarchyLevel() == null ||
                dto.getHierarchyLevel().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Hierarchy level is required."
            );
        }


        String email =
                dto.getEmail().trim();


        String domain =
                dto.getDomain().trim();


        String hierarchyLevel =
                dto.getHierarchyLevel().trim();


        // -------------------------------------------------
        // EMAIL DUPLICATE CHECK
        // -------------------------------------------------

        userRepo.findByEmail(email)
                .ifPresent(existingUser -> {

                    if (!existingUser.getId()
                            .equals(employee.getId())) {

                        throw new IllegalArgumentException(
                                "Email already exists."
                        );
                    }
                });


        // -------------------------------------------------
        // DETERMINE DESIGNATION
        // -------------------------------------------------

        String designation =
                resolveDesignation(
                        domain,
                        hierarchyLevel
                );


        if (designation == null) {

            throw new IllegalArgumentException(
                    "Invalid hierarchy level for selected domain."
            );
        }


        // -------------------------------------------------
        // VALIDATE DESIGNATION
        // -------------------------------------------------

        if (!isValidDesignation(
                domain,
                designation
        )) {

            throw new IllegalArgumentException(
                    "Invalid designation for selected domain."
            );
        }


        // -------------------------------------------------
        // DETERMINE TOP LEVEL
        // -------------------------------------------------

        boolean projectLeader =
                isTopLevelDesignation(
                        domain,
                        designation
                );


        // -------------------------------------------------
        // TEAM
        // -------------------------------------------------

        if (dto.getTeamId() == null) {

            throw new IllegalArgumentException(
                    "Please select a team."
            );
        }


        Team team =
                teamRepo.findByIdAndAdminId(
                        dto.getTeamId(),
                        adminId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Selected team does not belong to your organization."
                        )
                );


        // -------------------------------------------------
        // REPORTING MANAGER
        // -------------------------------------------------

        User senior = null;


        if (projectLeader) {

            senior = null;

        } else {

            if (dto.getSeniorId() == null) {

                throw new IllegalArgumentException(
                        designation +
                                " must have a reporting manager."
                );
            }


            if (employee.getId()
                    .equals(dto.getSeniorId())) {

                throw new IllegalArgumentException(
                        "An employee cannot be their own reporting manager."
                );
            }


            senior =
                    userRepo.findById(
                            dto.getSeniorId()
                    ).orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Selected reporting manager was not found."
                            )
                    );


            // SAME ADMIN
            if (!adminId.equals(
                    senior.getAdminId())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager does not belong to your administration."
                );
            }


            // MUST BE EMPLOYEE
            if (!"EMPLOYEE".equalsIgnoreCase(
                    senior.getRole())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager is not an employee."
                );
            }


            // MUST BE ACTIVE
            if (!"ACTIVE".equalsIgnoreCase(
                    senior.getStatus())) {

                throw new IllegalArgumentException(
                        "Selected reporting manager is not active."
                );
            }


            // SAME DOMAIN
            if (senior.getDomain() == null ||
                    !domain.equalsIgnoreCase(
                            senior.getDomain()
                    )) {

                throw new IllegalArgumentException(
                        "Employee and reporting manager must belong to the same domain."
                );
            }


            // SAME TEAM
            if (senior.getTeamId() == null ||
                    !team.getId().equals(
                            senior.getTeamId()
                    )) {

                throw new IllegalArgumentException(
                        "Employee and reporting manager must belong to the same team."
                );
            }


            // CORRECT PARENT
            String requiredParent =
                    getParentDesignation(
                            domain,
                            designation
                    );


            if (requiredParent == null ||
                    senior.getDesignation() == null ||
                    !requiredParent.equalsIgnoreCase(
                            senior.getDesignation()
                    )) {

                throw new IllegalArgumentException(
                        designation +
                                " must report to " +
                                requiredParent +
                                "."
                );
            }
        }


        // -------------------------------------------------
        // TRACK CHANGES FOR AUDIT
        // -------------------------------------------------

        String oldName =
                employee.getName();

        String oldEmail =
                employee.getEmail();

        String oldDomain =
                employee.getDomain();

        String oldDesignation =
                employee.getDesignation();

        Integer oldTeamId =
                employee.getTeamId();

        Integer oldSeniorId =
                employee.getSeniorId();


        // -------------------------------------------------
        // DETERMINE PASSWORD CHANGE
        // -------------------------------------------------

        /*
         * Password is optional during employee editing.
         *
         * Blank/null password:
         *     Keep existing password.
         *
         * Non-blank password:
         *     Encode and replace old password.
         */

        boolean passwordChanged =
                dto.getPassword() != null &&
                        !dto.getPassword().trim().isEmpty();


        // -------------------------------------------------
        // UPDATE BASIC INFORMATION
        // -------------------------------------------------

        employee.setName(
                dto.getName().trim()
        );


        employee.setEmail(
                email
        );


        employee.setDomain(
                domain
        );


        employee.setDesignation(
                designation
        );


        employee.setTeamId(
                team.getId()
        );


        employee.setSeniorId(
                senior == null
                        ? null
                        : senior.getId()
        );


        // -------------------------------------------------
        // PASSWORD CHANGE
        // -------------------------------------------------

        if (passwordChanged) {

            employee.setPassword(
                    passwordEncoder.encode(
                            dto.getPassword().trim()
                    )
            );


            employee.setPasswordChangedAt(
                    LocalDateTime.now()
            );


            Integer currentVersion =
                    employee.getPasswordVersion();


            if (currentVersion == null) {

                currentVersion = 0;
            }


            employee.setPasswordVersion(
                    currentVersion + 1
            );


            employee.setPasswordMustChange(
                    false
            );
        }


        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        User updatedEmployee =
                userRepo.save(employee);


        // -------------------------------------------------
        // BUILD AUDIT DESCRIPTION
        // -------------------------------------------------

        StringBuilder changes =
                new StringBuilder();


        if (!java.util.Objects.equals(
                oldName,
                updatedEmployee.getName())) {

            changes.append(
                    "name changed; "
            );
        }


        if (!java.util.Objects.equals(
                oldEmail,
                updatedEmployee.getEmail())) {

            changes.append(
                    "email changed; "
            );
        }


        if (!java.util.Objects.equals(
                oldDomain,
                updatedEmployee.getDomain())) {

            changes.append(
                    "domain changed; "
            );
        }


        if (!java.util.Objects.equals(
                oldDesignation,
                updatedEmployee.getDesignation())) {

            changes.append(
                    "designation changed; "
            );
        }


        if (!java.util.Objects.equals(
                oldTeamId,
                updatedEmployee.getTeamId())) {

            changes.append(
                    "team changed; "
            );
        }


        if (!java.util.Objects.equals(
                oldSeniorId,
                updatedEmployee.getSeniorId())) {

            changes.append(
                    "reporting manager changed; "
            );
        }


        if (passwordChanged) {

            changes.append(
                    "password changed; "
            );
        }


        if (changes.length() == 0) {

            changes.append(
                    "employee information updated."
            );
        }


        // -------------------------------------------------
        // ADMIN
        // -------------------------------------------------

        User admin =
                userRepo.findById(adminId)
                        .orElse(null);


        // -------------------------------------------------
        // EMPLOYEE UPDATED AUDIT
        // -------------------------------------------------

        auditLogService.record(
                admin,
                "EMPLOYEE_UPDATED",
                "USER",
                updatedEmployee.getId().toString(),
                "Employee '" +
                        updatedEmployee.getName() +
                        "' was updated. Changes: " +
                        changes,
                "SUCCESS"
        );


        // -------------------------------------------------
        // PASSWORD CHANGED AUDIT
        // -------------------------------------------------

        /*
         * IMPORTANT:
         *
         * Never store the actual password in audit logs.
         *
         * This audit only records that the password
         * was changed.
         */

        if (passwordChanged) {

            auditLogService.record(
                    admin,
                    "PASSWORD_CHANGED",
                    "USER",
                    updatedEmployee.getId().toString(),
                    "Password was changed for employee '" +
                            updatedEmployee.getName() +
                            "'.",
                    "SUCCESS"
            );
        }


        return updatedEmployee;
    }
}

