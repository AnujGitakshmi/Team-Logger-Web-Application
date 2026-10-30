
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.service.OrganizationRegistrationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/organization")
public class OrganizationRegistrationController {

    private final OrganizationRegistrationService registrationService;

    public OrganizationRegistrationController(
            OrganizationRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/register")
    public String showRegistrationPage() {
        return "organization-register";
    }

    @PostMapping("/register")
    public String registerOrganization(
            @RequestParam String organizationName,
            @RequestParam String branchName,
            @RequestParam String country,
            @RequestParam String state,
            @RequestParam String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam String address,
            @RequestParam String adminName,
            @RequestParam String email,
            @RequestParam String password,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            registrationService.registerOrganization(
                    organizationName,
                    branchName,
                    country,
                    state,
                    city,
                    postalCode,
                    address,
                    adminName,
                    email,
                    password
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Organization registered successfully. You can now log in."
            );

            return "redirect:/login";

        } catch (IllegalArgumentException ex) {

            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("organizationName", organizationName);
            model.addAttribute("branchName", branchName);
            model.addAttribute("country", country);
            model.addAttribute("state", state);
            model.addAttribute("city", city);
            model.addAttribute("postalCode", postalCode);
            model.addAttribute("address", address);
            model.addAttribute("adminName", adminName);
            model.addAttribute("email", email);

            return "organization-register";
        }
    }
}
