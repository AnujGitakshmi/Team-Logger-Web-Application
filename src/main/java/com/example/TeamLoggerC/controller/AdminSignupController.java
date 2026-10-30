
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.BranchAdminSignupRequest;
import com.example.TeamLoggerC.entity.Organization;
import com.example.TeamLoggerC.repository.BranchAdminSignupRequestRepo;
import com.example.TeamLoggerC.repository.OrganizationRepo;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.OrganizationRegistrationService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.TeamLoggerC.entity.OrganizationAdminSignupRequest;
import com.example.TeamLoggerC.repository.OrganizationAdminSignupRequestRepo;

import java.util.Locale;

@Controller
@RequestMapping("/signup")
public class AdminSignupController {

    private final UserRepo userRepo;
    private final OrganizationRepo organizationRepo;
    private final BranchAdminSignupRequestRepo signupRequestRepo;
    private final OrganizationRegistrationService organizationRegistrationService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationAdminSignupRequestRepo organizationAdminRequestRepo;

    public AdminSignupController(
            UserRepo userRepo,
            OrganizationRepo organizationRepo,
            BranchAdminSignupRequestRepo signupRequestRepo,
            OrganizationAdminSignupRequestRepo organizationAdminRequestRepo,
            OrganizationRegistrationService organizationRegistrationService,
            PasswordEncoder passwordEncoder) {

        this.userRepo = userRepo;
        this.organizationRepo = organizationRepo;
        this.signupRequestRepo = signupRequestRepo;
        this.organizationAdminRequestRepo = organizationAdminRequestRepo;
        this.organizationRegistrationService = organizationRegistrationService;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------------------------------------------------------
    // SHOW SIGNUP PAGE
    // ---------------------------------------------------------

    @GetMapping
    public String showSignupPage(Model model) {

        model.addAttribute(
                "organizations",
                organizationRepo.findAllByStatusIgnoreCaseOrderByNameAsc("ACTIVE")
        );

        return "signup";
    }

    // ---------------------------------------------------------
    // HANDLE ALL ADMIN REGISTRATIONS
    // ---------------------------------------------------------

    @PostMapping
    public String createAdminAccount(

            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String password,

            @RequestParam(required = false) String organizationRole,
            @RequestParam(required = false) String organizationName,
            @RequestParam(required = false) Integer organizationId,

            @RequestParam(required = false) String branchName,

            @RequestParam(required = false) String country,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String postalCode,
            @RequestParam(required = false) String address,

            RedirectAttributes redirectAttributes) {

        // 1. Validate common fields.

        if (name == null || name.isBlank()) {
            return signupError(redirectAttributes, "Name is required.");
        }

        if (email == null || email.isBlank()) {
            return signupError(redirectAttributes, "Email is required.");
        }

        if (password == null || password.isBlank()) {
            return signupError(redirectAttributes, "Password is required.");
        }

        if (organizationRole == null || organizationRole.isBlank()) {
            return signupError(
                    redirectAttributes,
                    "Please select an administrator type."
            );
        }

        String cleanName = name.trim();
        String cleanEmail = email.trim().toLowerCase(Locale.ROOT);
        String cleanRole = organizationRole.trim().toUpperCase(Locale.ROOT);

        // 2. Validate common field lengths.

        if (cleanName.length() > 100 || cleanEmail.length() > 150) {
            return signupError(
                    redirectAttributes,
                    "Name or email exceeds the permitted length."
            );
        }

        if (password.length() < 8) {
            return signupError(
                    redirectAttributes,
                    "Password must contain at least 8 characters."
            );
        }

        // 3. Validate administrator type.

        if (!cleanRole.equals("SUPER_ADMIN")
                && !cleanRole.equals("ORG_ADMIN")
                && !cleanRole.equals("BRANCH_ADMIN")) {

            return signupError(
                    redirectAttributes,
                    "Invalid administrator type."
            );
        }

        // 4. Prevent duplicate user accounts.

        if (userRepo.findByEmail(cleanEmail).isPresent()) {
            return signupError(
                    redirectAttributes,
                    "An account with this email already exists."
            );
        }

        // -----------------------------------------------------
        // SUPER ADMIN REGISTRATION
        // -----------------------------------------------------

        if ("SUPER_ADMIN".equals(cleanRole)) {

            if (organizationName == null || organizationName.isBlank()) {
                return signupError(
                        redirectAttributes,
                        "Organization name is required."
                );
            }

            String cleanOrganizationName = organizationName.trim();

            if (cleanOrganizationName.length() > 150) {
                return signupError(
                        redirectAttributes,
                        "Organization name cannot exceed 150 characters."
                );
            }

            String cleanBranchName = branchName == null
                    ? ""
                    : branchName.trim();

            if (cleanBranchName.isBlank()) {
                return signupError(
                        redirectAttributes,
                        "Initial branch name is required."
                );
            }

            if (cleanBranchName.length() > 150) {
                return signupError(
                        redirectAttributes,
                        "Branch name cannot exceed 150 characters."
                );
            }

            // Validate initial branch location.
            String cleanCountry = country == null ? "" : country.trim();
            String cleanState = state == null ? "" : state.trim();
            String cleanCity = city == null ? "" : city.trim();
            String cleanPostalCode = postalCode == null ? "" : postalCode.trim();
            String cleanAddress = address == null ? "" : address.trim();

            String locationError = validateBranchLocation(
                    cleanCountry,
                    cleanState,
                    cleanCity,
                    cleanPostalCode,
                    cleanAddress
            );

            if (locationError != null) {
                return signupError(redirectAttributes, locationError);
            }

            String normalizedOrganizationName =
                    cleanOrganizationName.toLowerCase(Locale.ROOT);

            if (organizationRepo.existsByNormalizedName(
                    normalizedOrganizationName)) {

                return signupError(
                        redirectAttributes,
                        "An organization with this name already exists. "
                                + "Super Admin is already registered for this organization."
                );
            }

            try {

                organizationRegistrationService.registerOrganization(
                        cleanOrganizationName,
                        cleanBranchName,
                        cleanCountry,
                        cleanState,
                        cleanCity,
                        cleanPostalCode,
                        cleanAddress,
                        cleanName,
                        cleanEmail,
                        password
                );

            } catch (IllegalArgumentException | IllegalStateException ex) {

                return signupError(
                        redirectAttributes,
                        ex.getMessage()
                );
            }

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Organization, initial branch, and Super Admin account "
                            + "created successfully. You can now log in."
            );

            return "redirect:/login";
        }

        // -----------------------------------------------------
        // ORGANIZATION ADMIN / BRANCH ADMIN
        // -----------------------------------------------------

        if (organizationId == null) {
            return signupError(
                    redirectAttributes,
                    "Please select an organization."
            );
        }

        Organization organization = organizationRepo
                .findById(organizationId)
                .orElse(null);

        if (organization == null) {
            return signupError(
                    redirectAttributes,
                    "Selected organization was not found."
            );
        }

        if (!"ACTIVE".equalsIgnoreCase(organization.getStatus())) {
            return signupError(
                    redirectAttributes,
                    "This organization is not currently accepting registrations."
            );
        }

        // -----------------------------------------------------
        // ORGANIZATION ADMIN
        // -----------------------------------------------------

        if ("ORG_ADMIN".equals(cleanRole)) {

            // Prevent duplicate pending Organization Admin requests.
            if (organizationAdminRequestRepo.existsByEmailAndStatus(
                    cleanEmail, "PENDING")) {

                return signupError(
                        redirectAttributes,
                        "An Organization Admin signup request for this email is already pending."
                );
            }

            // Create a separate Organization Admin approval request.
            OrganizationAdminSignupRequest request =
                    new OrganizationAdminSignupRequest();

            request.setAdminName(cleanName);
            request.setEmail(cleanEmail);

            // Store only the BCrypt-hashed password.
            request.setPassword(passwordEncoder.encode(password));

            // Associate the request with the exact selected organization.
            request.setOrganizationId(organization.getId());

            request.setStatus("PENDING");

            organizationAdminRequestRepo.save(request);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Your Organization Admin signup request has been submitted. "
                            + "The organization's Super Admin must approve it before you can log in."
            );

            return "redirect:/login?signup=pending";
        }

        // -----------------------------------------------------
        // BRANCH ADMIN REQUEST
        // -----------------------------------------------------

        if ("BRANCH_ADMIN".equals(cleanRole)) {

            String cleanBranchName = branchName == null
                    ? ""
                    : branchName.trim();

            if (cleanBranchName.isBlank()) {
                return signupError(
                        redirectAttributes,
                        "Branch name is required."
                );
            }

            if (cleanBranchName.length() > 150) {
                return signupError(
                        redirectAttributes,
                        "Branch name cannot exceed 150 characters."
                );
            }

            // Validate requested branch location.
            String cleanCountry = country == null ? "" : country.trim();
            String cleanState = state == null ? "" : state.trim();
            String cleanCity = city == null ? "" : city.trim();
            String cleanPostalCode = postalCode == null ? "" : postalCode.trim();
            String cleanAddress = address == null ? "" : address.trim();

            String locationError = validateBranchLocation(
                    cleanCountry,
                    cleanState,
                    cleanCity,
                    cleanPostalCode,
                    cleanAddress
            );

            if (locationError != null) {
                return signupError(redirectAttributes, locationError);
            }

            // Prevent duplicate pending requests.
            if (signupRequestRepo.existsByEmailAndStatus(
                    cleanEmail, "PENDING")) {

                return signupError(
                        redirectAttributes,
                        "A signup request for this email is already pending."
                );
            }

            // Save only a BCrypt-hashed password.
            BranchAdminSignupRequest request =
                    new BranchAdminSignupRequest();

            request.setAdminName(cleanName);
            request.setEmail(cleanEmail);
            request.setPassword(passwordEncoder.encode(password));

            request.setOrganizationName(organization.getName());
            request.setBranchName(cleanBranchName);

            // Save branch location in the pending request.
            request.setCountry(cleanCountry);
            request.setState(cleanState);
            request.setCity(cleanCity);
            request.setPostalCode(
                    cleanPostalCode.isBlank() ? null : cleanPostalCode
            );
            request.setAddress(cleanAddress);

            request.setStatus("PENDING");

            signupRequestRepo.save(request);

            redirectAttributes.addFlashAttribute(
                    "success",
                    "Your Branch Admin signup request has been submitted. "
                            + "The Super Admin must approve it before you can log in."
            );

            return "redirect:/login?signup=pending";
        }

        return signupError(
                redirectAttributes,
                "Unable to process registration."
        );
    }

    // ---------------------------------------------------------
    // BRANCH LOCATION VALIDATION
    // ---------------------------------------------------------

    private String validateBranchLocation(
            String country,
            String state,
            String city,
            String postalCode,
            String address) {

        if (country == null || country.isBlank()) {
            return "Country is required.";
        }

        if (state == null || state.isBlank()) {
            return "State is required.";
        }

        if (city == null || city.isBlank()) {
            return "City is required.";
        }

        if (address == null || address.isBlank()) {
            return "Branch address is required.";
        }

        if (country.length() > 100
                || state.length() > 100
                || city.length() > 100
                || (postalCode != null && postalCode.length() > 20)
                || address.length() > 500) {

            return "One or more branch location fields exceed the permitted length.";
        }

        return null;
    }

    // ---------------------------------------------------------
    // COMMON ERROR HANDLER
    // ---------------------------------------------------------

    private String signupError(
            RedirectAttributes redirectAttributes,
            String message) {

        redirectAttributes.addFlashAttribute("error", message);

        return "redirect:/signup";
    }
}
