package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.dto.EmployeeFormDTO;
import com.example.TeamLoggerC.dto.ReportingManagerDTO;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.AdminEmployeeService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/employees")
public class AdminEmployeeController {

    private final AdminEmployeeService employeeService;


    public AdminEmployeeController(
            AdminEmployeeService employeeService) {

        this.employeeService =
                employeeService;
    }


    // =====================================================
    // EMPLOYEE LIST
    // =====================================================

    @GetMapping
    public String employees(
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        Integer adminId =
                admin.getId();


        List<User> employees =
                employeeService.getEmployees(
                        adminId
                );


        model.addAttribute(
                "admin",
                admin
        );


        model.addAttribute(
                "employees",
                employees
        );


        model.addAttribute(
                "employeeService",
                employeeService
        );


        return "adminEmployee";
    }


    // =====================================================
    // ADD EMPLOYEE PAGE
    // =====================================================

    @GetMapping("/add")
    public String addEmployeePage(
            Authentication authentication,
            Model model,
            @RequestParam(
                    required = false
            )
            Integer teamId,
            @RequestParam(
                    required = false
            )
            String created) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        Integer adminId =
                admin.getId();


        EmployeeFormDTO form =
                new EmployeeFormDTO();


        if (teamId != null) {

            form.setTeamId(
                    teamId
            );
        }


        model.addAttribute(
                "admin",
                admin
        );


        // ---------------------------------------------
        // ONLY LOAD TEAMS OF THIS ADMIN
        // ---------------------------------------------

        model.addAttribute(
                "teams",
                employeeService.getAllTeams(
                        adminId
                )
        );


        model.addAttribute(
                "employeeForm",
                form
        );


        // ---------------------------------------------
        // CREATED TEAM MESSAGE
        // ---------------------------------------------

        if ("1".equals(created) &&
                teamId != null) {

            String teamName =
                    employeeService.getTeamName(
                            adminId,
                            teamId
                    );

            model.addAttribute(
                    "createdTeamName",
                    teamName
            );
        }


        return "employeesAdd";
    }


    // =====================================================
    // GET REPORTING MANAGERS
    // =====================================================

    @GetMapping("/reporting-managers")
    @ResponseBody
    public List<ReportingManagerDTO> getReportingManagers(
            Authentication authentication,
            @RequestParam String domain,
            @RequestParam String designation,
            @RequestParam Integer teamId) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        return employeeService.getReportingManagers(
                admin.getId(),
                teamId,
                domain,
                designation
        );
    }


    // =====================================================
    // SAVE EMPLOYEE
    // =====================================================

    @PostMapping("/save")
    public String saveEmployee(
            Authentication authentication,
            @ModelAttribute EmployeeFormDTO dto,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        Integer adminId =
                admin.getId();


        try {

            User employee =
                    employeeService.addEmployee(
                            adminId,
                            dto
                    );


            // ---------------------------------------------
            // PROJECT LEADER CREATED
            // ---------------------------------------------

            if (employee.getSeniorId() == null &&
                    employee.getTeamId() != null) {

                return "redirect:/admin/employees/add?teamId=" +
                        employee.getTeamId() +
                        "&created=1";
            }


            return "redirect:/admin/employees";


        } catch (IllegalArgumentException ex) {

            model.addAttribute(
                    "errorMessage",
                    ex.getMessage()
            );


            model.addAttribute(
                    "admin",
                    admin
            );


            // ---------------------------------------------
            // ONLY LOAD TEAMS OF THIS ADMIN
            // ---------------------------------------------

            model.addAttribute(
                    "teams",
                    employeeService.getAllTeams(
                            adminId
                    )
            );


            model.addAttribute(
                    "employeeForm",
                    dto
            );


            return "employeesAdd";
        }
    }


    // =====================================================
    // EDIT EMPLOYEE PAGE
    // =====================================================

    @GetMapping("/edit/{employeeId}")
    public String editEmployeePage(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        Integer adminId =
                admin.getId();


        // ---------------------------------------------
        // GET EMPLOYEE
        // ---------------------------------------------

        User employee =
                employeeService.getEmployee(
                        adminId,
                        employeeId
                );


        // ---------------------------------------------
        // CONVERT USER → FORM DTO
        // ---------------------------------------------

        EmployeeFormDTO form =
                new EmployeeFormDTO();

        form.setId(
                employee.getId()
        );

        form.setName(
                employee.getName()
        );

        form.setEmail(
                employee.getEmail()
        );

        form.setDomain(
                employee.getDomain()
        );

        form.setDesignation(
                employee.getDesignation()
        );

        form.setTeamId(
                employee.getTeamId()
        );

        form.setSeniorId(
                employee.getSeniorId()
        );


        // ---------------------------------------------
        // DETERMINE HIERARCHY LEVEL
        // ---------------------------------------------

        form.setHierarchyLevel(
                getHierarchyLevel(
                        employee.getDomain(),
                        employee.getDesignation()
                )
        );


        // ---------------------------------------------
        // LOAD PAGE DATA
        // ---------------------------------------------

        model.addAttribute(
                "admin",
                admin
        );

        model.addAttribute(
                "employee",
                employee
        );

        model.addAttribute(
                "employeeForm",
                form
        );

        model.addAttribute(
                "teams",
                employeeService.getAllTeams(
                        adminId
                )
        );


        // ---------------------------------------------
        // LOAD REPORTING MANAGERS
        // ---------------------------------------------

        if (employee.getTeamId() != null &&
                employee.getDesignation() != null &&
                !isProjectLeader(
                        employee.getDomain(),
                        employee.getDesignation()
                )) {

            List<ReportingManagerDTO> managers =
                    employeeService.getReportingManagers(
                            adminId,
                            employee.getTeamId(),
                            employee.getDomain(),
                            employee.getDesignation()
                    );

            model.addAttribute(
                    "reportingManagers",
                    managers
            );

        } else {

            model.addAttribute(
                    "reportingManagers",
                    List.of()
            );
        }


        return "employeeEdit";
    }


    // =====================================================
    // UPDATE EMPLOYEE
    // =====================================================

    @PostMapping("/update")
    public String updateEmployee(
            Authentication authentication,
            @ModelAttribute EmployeeFormDTO dto,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        Integer adminId =
                admin.getId();


        if (dto.getId() == null) {

            model.addAttribute(
                    "errorMessage",
                    "Employee ID is required."
            );

            model.addAttribute(
                    "admin",
                    admin
            );

            model.addAttribute(
                    "employeeForm",
                    dto
            );

            model.addAttribute(
                    "teams",
                    employeeService.getAllTeams(
                            adminId
                    )
            );

            model.addAttribute(
                    "reportingManagers",
                    List.of()
            );

            return "employeeEdit";
        }


        try {

            User updatedEmployee =
                    employeeService.updateEmployee(
                            adminId,
                            dto.getId(),
                            dto
                    );


            return "redirect:/admin/employees/" +
                    updatedEmployee.getId();


        } catch (IllegalArgumentException ex) {

            // ---------------------------------------------
            // KEEP FORM DATA AFTER VALIDATION ERROR
            // ---------------------------------------------

            model.addAttribute(
                    "errorMessage",
                    ex.getMessage()
            );


            model.addAttribute(
                    "admin",
                    admin
            );


            model.addAttribute(
                    "employeeForm",
                    dto
            );


            // ---------------------------------------------
            // RELOAD TEAMS
            // ---------------------------------------------

            model.addAttribute(
                    "teams",
                    employeeService.getAllTeams(
                            adminId
                    )
            );


            // ---------------------------------------------
            // RELOAD REPORTING MANAGERS
            // ---------------------------------------------

            if (dto.getTeamId() != null &&
                    dto.getDomain() != null &&
                    dto.getDesignation() != null &&
                    !isProjectLeader(
                            dto.getDomain(),
                            dto.getDesignation()
                    )) {

                List<ReportingManagerDTO> managers =
                        employeeService.getReportingManagers(
                                adminId,
                                dto.getTeamId(),
                                dto.getDomain(),
                                dto.getDesignation()
                        );

                model.addAttribute(
                        "reportingManagers",
                        managers
                );

            } else {

                model.addAttribute(
                        "reportingManagers",
                        List.of()
                );
            }


            return "employeeEdit";
        }
    }


    // =====================================================
    // EMPLOYEE DETAILS
    // =====================================================

    @GetMapping("/{employeeId}")
    public String employeeDetails(
            @PathVariable Integer employeeId,
            Authentication authentication,
            Model model) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        User employee =
                employeeService.getEmployee(
                        admin.getId(),
                        employeeId
                );


        model.addAttribute(
                "admin",
                admin
        );


        model.addAttribute(
                "employee",
                employee
        );


        model.addAttribute(
                "employeeService",
                employeeService
        );


        return "employeeDetails";
    }


    // =====================================================
    // DISABLE
    // =====================================================

    @PostMapping("/{employeeId}/disable")
    public String disableEmployee(
            @PathVariable Integer employeeId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        employeeService.disableEmployee(
                admin.getId(),
                employeeId
        );


        return "redirect:/admin/employees";
    }


    // =====================================================
    // ENABLE
    // =====================================================

    @PostMapping("/{employeeId}/enable")
    public String enableEmployee(
            @PathVariable Integer employeeId,
            Authentication authentication) {

        CustomUserDetails userDetails =
                (CustomUserDetails)
                        authentication.getPrincipal();


        User admin =
                userDetails.getUser();


        employeeService.enableEmployee(
                admin.getId(),
                employeeId
        );


        return "redirect:/admin/employees";
    }


    // =====================================================
    // HIERARCHY LEVEL HELPER
    // =====================================================

    private String getHierarchyLevel(
            String domain,
            String designation) {

        if (domain == null ||
                designation == null) {

            return "";
        }


        String d =
                domain.trim()
                        .toUpperCase();

        String role =
                designation.trim()
                        .toLowerCase();


        return switch (d) {

            case "DEVELOPMENT" -> {

                if (role.equals("project leader")) {
                    yield "PROJECT_LEADER";
                }

                if (role.equals("senior developer")) {
                    yield "SENIOR";
                }

                if (role.equals("junior developer")) {
                    yield "JUNIOR";
                }

                if (role.equals("developer intern")) {
                    yield "INTERN";
                }

                yield "";
            }


            case "QA" -> {

                if (role.equals("qa lead")) {
                    yield "PROJECT_LEADER";
                }

                if (role.equals("senior qa engineer")) {
                    yield "SENIOR";
                }

                if (role.equals("junior qa engineer")) {
                    yield "JUNIOR";
                }

                if (role.equals("qa intern")) {
                    yield "INTERN";
                }

                yield "";
            }


            case "HR" -> {

                if (role.equals("hr head")) {
                    yield "PROJECT_LEADER";
                }

                if (role.equals("senior hr")) {
                    yield "SENIOR";
                }

                if (role.equals("junior hr")) {
                    yield "JUNIOR";
                }

                if (role.equals("hr intern")) {
                    yield "INTERN";
                }

                yield "";
            }


            case "FINANCE" -> {

                if (role.equals("finance lead")) {
                    yield "PROJECT_LEADER";
                }

                if (role.equals("senior finance executive")) {
                    yield "SENIOR";
                }

                if (role.equals("junior finance executive")) {
                    yield "JUNIOR";
                }

                if (role.equals("finance intern")) {
                    yield "INTERN";
                }

                yield "";
            }


            case "UI/UX" -> {

                if (role.equals("ui/ux lead")) {
                    yield "PROJECT_LEADER";
                }

                if (role.equals("senior ui/ux designer")) {
                    yield "SENIOR";
                }

                if (role.equals("junior ui/ux designer")) {
                    yield "JUNIOR";
                }

                if (role.equals("ui/ux intern")) {
                    yield "INTERN";
                }

                yield "";
            }


            default -> "";
        };
    }


    // =====================================================
    // PROJECT LEADER CHECK
    // =====================================================

    private boolean isProjectLeader(
            String domain,
            String designation) {

        if (domain == null ||
                designation == null) {

            return false;
        }


        String d =
                domain.trim()
                        .toLowerCase();

        String role =
                designation.trim()
                        .toLowerCase();


        return switch (d) {

            case "development" ->
                    role.equals("project leader");

            case "qa" ->
                    role.equals("qa lead");

            case "hr" ->
                    role.equals("hr head");

            case "finance" ->
                    role.equals("finance lead");

            case "ui/ux" ->
                    role.equals("ui/ux lead");

            default ->
                    false;
        };
    }

}
