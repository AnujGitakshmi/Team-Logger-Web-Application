
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.EmployeeIpAccess;
import com.example.TeamLoggerC.entity.IpApplicationRule;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.EmployeeIpAccessRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.IpApplicationRuleService;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/ip-application-rules")
public class ApplicationPermissionController {

    private final IpApplicationRuleService ruleService;
    private final UserRepo userRepo;
    private final EmployeeIpAccessRepo employeeIpAccessRepo;

    public ApplicationPermissionController(
            IpApplicationRuleService ruleService,
            UserRepo userRepo,
            EmployeeIpAccessRepo employeeIpAccessRepo
    ) {
        this.ruleService = ruleService;
        this.userRepo = userRepo;
        this.employeeIpAccessRepo = employeeIpAccessRepo;
    }

    // =====================================================
    // UNIFIED APPLICATION + WEBSITE RULES PAGE
    // =====================================================

    @GetMapping
    public String rulesPage(
            Authentication authentication,
            Model model
    ) {
        User admin = getAuthenticatedAdmin(authentication);
        loadPageModel(admin, model);

        if (!model.containsAttribute("editRule")) {
            IpApplicationRule newRule = new IpApplicationRule();
            newRule.setTargetType("APPLICATION");
            newRule.setPermission("ALLOW");
            newRule.setStatus("ACTIVE");
            model.addAttribute("editRule", newRule);
        }

        return "application-permissions";
    }

    // =====================================================
    // OPEN EXISTING RULE FOR EDITING
    // =====================================================

    @GetMapping("/edit/{id}")
    public String editRule(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User admin = getAuthenticatedAdmin(authentication);

            IpApplicationRule rule = ruleService.getRules(admin.getId())
                    .stream()
                    .filter(item -> item.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() ->
                            new IllegalArgumentException("Rule not found.")
                    );

            redirectAttributes.addFlashAttribute("editRule", rule);

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );
        }

        return "redirect:/admin/ip-application-rules";
    }

    // =====================================================
    // CREATE OR UPDATE RULE
    // =====================================================

    @PostMapping("/save")
    public String saveRule(
            Authentication authentication,

            @RequestParam(required = false) Long id,
            @RequestParam String ipSource,
            @RequestParam(required = false) String ipAddress,
            @RequestParam(required = false) Integer employeeId,
            @RequestParam(required = false) String selectedIpAddress,

            @RequestParam String targetType,
            @RequestParam String applicationName,
            @RequestParam(required = false) String executableName,
            @RequestParam(defaultValue = "ALLOW") String permission,

            RedirectAttributes redirectAttributes
    ) {
        try {
            User admin = getAuthenticatedAdmin(authentication);

            String resolvedIp;

            if ("EMPLOYEE".equalsIgnoreCase(ipSource)) {
                if (employeeId == null ||
                        selectedIpAddress == null ||
                        selectedIpAddress.isBlank()) {
                    throw new IllegalArgumentException(
                            "Select an employee and an approved IP address."
                    );
                }

                validateApprovedEmployeeIp(
                        admin.getId(),
                        employeeId,
                        selectedIpAddress
                );

                resolvedIp = selectedIpAddress;

            } else if ("MANUAL".equalsIgnoreCase(ipSource)) {
                if (ipAddress == null || ipAddress.isBlank()) {
                    throw new IllegalArgumentException(
                            "Enter an IP address."
                    );
                }

                resolvedIp = ipAddress;

            } else {
                throw new IllegalArgumentException(
                        "Choose manual IP entry or employee selection."
                );
            }

            validateTargetType(targetType);

            if ("WEBSITE".equalsIgnoreCase(targetType)) {
                // Website rules use a domain; executable is not applicable.
                executableName = null;
            }

            if (id == null) {
                ruleService.createRule(
                        admin.getId(),
                        resolvedIp,
                        targetType,
                        applicationName,
                        executableName,
                        permission
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "IP-based rule created successfully."
                );

            } else {
                ruleService.updateRule(
                        admin.getId(),
                        id,
                        resolvedIp,
                        targetType,
                        applicationName,
                        executableName,
                        permission
                );

                redirectAttributes.addFlashAttribute(
                        "successMessage",
                        "IP-based rule updated successfully."
                );
            }

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    "Unable to save the rule. Check the entered values and try again."
            );
        }

        return "redirect:/admin/ip-application-rules";
    }

    // =====================================================
    // ACTIVATE / DEACTIVATE RULE
    // =====================================================

    @PostMapping("/toggle/{id}")
    public String toggleRule(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User admin = getAuthenticatedAdmin(authentication);

            ruleService.toggleStatus(admin.getId(), id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Rule status updated successfully."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );
        }

        return "redirect:/admin/ip-application-rules";
    }

    // =====================================================
    // DELETE RULE
    // =====================================================

    @PostMapping("/delete/{id}")
    public String deleteRule(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) {
        try {
            User admin = getAuthenticatedAdmin(authentication);

            ruleService.deleteRule(admin.getId(), id);

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Rule deleted successfully."
            );

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage()
            );
        }

        return "redirect:/admin/ip-application-rules";
    }

    // =====================================================
    // LOAD PAGE DATA
    // =====================================================

    private void loadPageModel(User admin, Model model) {

        List<IpApplicationRule> rules =
                ruleService.getRules(admin.getId());

        List<User> employees = userRepo.findByAdminId(admin.getId())
                .stream()
                .filter(user -> !"ADMIN".equalsIgnoreCase(user.getRole()))
                .sorted(Comparator.comparing(
                        User::getName,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
                ))
                .toList();

        Map<Integer, User> employeesById = employees.stream()
                .collect(Collectors.toMap(
                        User::getId,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<ApprovedIpOption> approvedIpOptions = new ArrayList<>();

        List<EmployeeIpAccess> approvedAccesses =
                employeeIpAccessRepo
                        .findByAdminIdAndStatusOrderByCreatedAtDesc(
                                admin.getId(),
                                "APPROVED"
                        );

        for (EmployeeIpAccess access : approvedAccesses) {
            User employee = employeesById.get(access.getEmployeeId());

            if (employee == null ||
                    access.getIpAddress() == null ||
                    access.getIpAddress().isBlank()) {
                continue;
            }

            approvedIpOptions.add(new ApprovedIpOption(
                    employee.getId(),
                    employee.getName(),
                    access.getIpAddress(),
                    access.getAccessType()
            ));
        }

        model.addAttribute("admin", admin);
        model.addAttribute("rules", rules);
        model.addAttribute("employees", employees);
        model.addAttribute("approvedIpOptions", approvedIpOptions);

        model.addAttribute("totalRuleCount", rules.size());

        model.addAttribute(
                "activeRuleCount",
                rules.stream()
                        .filter(rule ->
                                "ACTIVE".equalsIgnoreCase(rule.getStatus()))
                        .count()
        );

        model.addAttribute(
                "applicationRuleCount",
                rules.stream()
                        .filter(rule ->
                                "APPLICATION".equalsIgnoreCase(
                                        rule.getTargetType()))
                        .count()
        );

        model.addAttribute(
                "websiteRuleCount",
                rules.stream()
                        .filter(rule ->
                                "WEBSITE".equalsIgnoreCase(
                                        rule.getTargetType()))
                        .count()
        );
    }

    // =====================================================
    // VERIFY EMPLOYEE IP APPROVAL AND OWNERSHIP
    // =====================================================

    private void validateApprovedEmployeeIp(
            Integer adminId,
            Integer employeeId,
            String ipAddress
    ) {
        User employee = userRepo.findById(employeeId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Employee not found.")
                );

        if (!adminId.equals(employee.getAdminId()) ||
                "ADMIN".equalsIgnoreCase(employee.getRole())) {
            throw new IllegalArgumentException(
                    "That employee does not belong to your organization."
            );
        }

        EmployeeIpAccess access = employeeIpAccessRepo
                .findByEmployeeIdAndAdminIdAndIpAddress(
                        employeeId,
                        adminId,
                        ipAddress
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "This IP is not registered to the selected employee."
                        )
                );

        if (!"APPROVED".equalsIgnoreCase(access.getStatus())) {
            throw new IllegalArgumentException(
                    "Only approved employee IP addresses can be selected."
            );
        }
    }

    private void validateTargetType(String targetType) {
        if (targetType == null ||
                (!"APPLICATION".equalsIgnoreCase(targetType) &&
                        !"WEBSITE".equalsIgnoreCase(targetType))) {
            throw new IllegalArgumentException(
                    "Target type must be APPLICATION or WEBSITE."
            );
        }
    }

    // =====================================================
    // AUTHENTICATED ADMIN
    // =====================================================

    private User getAuthenticatedAdmin(
            Authentication authentication
    ) {
        if (authentication == null ||
                !authentication.isAuthenticated() ||
                authentication.getPrincipal() instanceof String &&
                        "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalArgumentException(
                    "Admin authentication is required."
            );
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof CustomUserDetails userDetails)) {
            throw new IllegalArgumentException(
                    "Invalid authenticated user."
            );
        }

        Integer userId = userDetails.getUser().getId();

        User admin = userRepo.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Admin account not found.")
                );

        if (!"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new IllegalArgumentException(
                    "Only administrators can manage IP-based rules."
            );
        }

        return admin;
    }

    // =====================================================
    // DROPDOWN OPTION FOR APPROVED EMPLOYEE IP ADDRESSES
    // =====================================================

    public record ApprovedIpOption(
            Integer employeeId,
            String employeeName,
            String ipAddress,
            String accessType
    ) {
    }
}
