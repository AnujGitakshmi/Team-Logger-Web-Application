
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Branch;
import com.example.TeamLoggerC.entity.Organization;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.BranchRepo;
import com.example.TeamLoggerC.repository.OrganizationRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class OrganizationRegistrationService {

    private final OrganizationRepo organizationRepo;
    private final BranchRepo branchRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public OrganizationRegistrationService(
            OrganizationRepo organizationRepo,
            BranchRepo branchRepo,
            UserRepo userRepo,
            PasswordEncoder passwordEncoder) {

        this.organizationRepo = organizationRepo;
        this.branchRepo = branchRepo;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerOrganization(
            String organizationName,
            String branchName,
            String country,
            String state,
            String city,
            String postalCode,
            String address,
            String adminName,
            String email,
            String rawPassword) {

        // Validate required fields.
        if (organizationName == null
                || branchName == null
                || country == null
                || state == null
                || city == null
                || address == null
                || adminName == null
                || email == null
                || rawPassword == null) {

            throw new IllegalArgumentException(
                    "All required registration fields must be provided."
            );
        }

        organizationName = organizationName.trim();
        branchName = branchName.trim();
        country = country.trim();
        state = state.trim();
        city = city.trim();
        address = address.trim();
        postalCode = postalCode == null ? "" : postalCode.trim();
        adminName = adminName.trim();
        email = email.trim().toLowerCase(Locale.ROOT);

        if (organizationName.isBlank()
                || branchName.isBlank()
                || country.isBlank()
                || state.isBlank()
                || city.isBlank()
                || address.isBlank()
                || adminName.isBlank()
                || email.isBlank()
                || rawPassword.isBlank()) {

            throw new IllegalArgumentException(
                    "All required registration fields must be filled."
            );
        }

        // Validate field lengths.
        if (organizationName.length() > 150
                || branchName.length() > 150
                || country.length() > 100
                || state.length() > 100
                || city.length() > 100
                || postalCode.length() > 20
                || address.length() > 500
                || adminName.length() > 100
                || email.length() > 150) {

            throw new IllegalArgumentException(
                    "One or more fields exceed the permitted length."
            );
        }

        if (rawPassword.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        String normalizedOrgName =
                organizationName.toLowerCase(Locale.ROOT);

        // Prevent duplicate organization registration.
        if (organizationRepo.existsByNormalizedName(normalizedOrgName)) {
            throw new IllegalArgumentException(
                    "An organization with this name already exists."
            );
        }

        // Prevent duplicate user accounts.
        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        // Create organization.
        Organization organization = new Organization();
        organization.setName(organizationName);
        organization.setStatus("ACTIVE");

        organization = organizationRepo.saveAndFlush(organization);

        // Create initial branch with location details.
        Branch branch = new Branch();

        branch.setOrganization(organization);
        branch.setName(branchName);
        branch.setCountry(country);
        branch.setState(state);
        branch.setCity(city);
        branch.setPostalCode(postalCode.isBlank() ? null : postalCode);
        branch.setAddress(address);
        branch.setStatus("ACTIVE");

        branch = branchRepo.saveAndFlush(branch);

        // Create Super Admin.
        User admin = new User();

        admin.setName(adminName);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(rawPassword));

        admin.setRole("SUPER_ADMIN");
        admin.setOrganizationRole("SUPER_ADMIN");
        admin.setDesignation("SUPER_ADMIN");

        admin.setOrganizationId(organization.getId());
        admin.setBranchId(branch.getId());

        admin.setStatus("ACTIVE");

        admin.setPasswordVersion(1);
        admin.setPasswordMustChange(false);
        admin.setFailedLoginAttempts(0);
        admin.setAccountLocked(false);

        return userRepo.save(admin);
    }
}
