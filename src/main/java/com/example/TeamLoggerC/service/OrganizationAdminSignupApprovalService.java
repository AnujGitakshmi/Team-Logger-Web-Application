
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Organization;
import com.example.TeamLoggerC.entity.OrganizationAdminSignupRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.OrganizationAdminSignupRequestRepo;
import com.example.TeamLoggerC.repository.OrganizationRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class OrganizationAdminSignupApprovalService {

    private final OrganizationAdminSignupRequestRepo requestRepo;
    private final OrganizationRepo organizationRepo;
    private final UserRepo userRepo;

    public OrganizationAdminSignupApprovalService(
            OrganizationAdminSignupRequestRepo requestRepo,
            OrganizationRepo organizationRepo,
            UserRepo userRepo) {

        this.requestRepo = requestRepo;
        this.organizationRepo = organizationRepo;
        this.userRepo = userRepo;
    }

    // Get pending requests belonging only to this Super Admin's organization.
    @Transactional(readOnly = true)
    public List<OrganizationAdminSignupRequest> getPendingRequests(
            Integer actingAdminId) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        return requestRepo.findByOrganizationIdAndStatusOrderByRequestedAtDesc(
                superAdmin.getOrganizationId(),
                "PENDING"
        );
    }

    // Approve a pending Organization Admin request.
    @Transactional
    public User approveRequest(
            Integer requestId,
            Integer actingAdminId,
            String decisionNote) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        OrganizationAdminSignupRequest request =
                requestRepo.findByIdAndOrganizationId(
                        requestId,
                        superAdmin.getOrganizationId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Signup request was not found for your organization."
                        ));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending signup requests can be approved."
            );
        }

        Organization organization = organizationRepo
                .findById(superAdmin.getOrganizationId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Your organization no longer exists."
                        ));

        if (organization.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(organization.getStatus())) {
            throw new IllegalStateException(
                    "Your organization is not active."
            );
        }

        if (!request.getOrganizationId().equals(organization.getId())) {
            throw new SecurityException(
                    "This request does not belong to your organization."
            );
        }

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException(
                    "An account with this email already exists."
            );
        }

        User organizationAdmin = new User();

        organizationAdmin.setName(request.getAdminName().trim());
        organizationAdmin.setEmail(email);

        // Password is already BCrypt-encoded during signup.
        organizationAdmin.setPassword(request.getPassword());

        organizationAdmin.setPasswordChangedAt(LocalDateTime.now());
        organizationAdmin.setPasswordVersion(1);
        organizationAdmin.setPasswordMustChange(false);

        organizationAdmin.setRole("ADMIN");
        organizationAdmin.setOrganizationRole("ORG_ADMIN");
        organizationAdmin.setDesignation("ORG_ADMIN");

        organizationAdmin.setOrganizationId(organization.getId());
        organizationAdmin.setBranchId(null);

        organizationAdmin.setAdminId(null);
        organizationAdmin.setTeamId(null);
        organizationAdmin.setApplicationName(null);
        organizationAdmin.setEmployeeLoginId(null);

        organizationAdmin.setSeniorId(null);
        organizationAdmin.setDomain(null);
        organizationAdmin.setDescription(null);
        organizationAdmin.setProfilePhoto(null);

        organizationAdmin.setStatus("ACTIVE");
        organizationAdmin.setDisabledAt(null);
        organizationAdmin.setScheduledDeletionAt(null);

        organizationAdmin = userRepo.saveAndFlush(organizationAdmin);

        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(superAdmin.getId());
        request.setDecisionNote(normalizeNote(decisionNote));

        requestRepo.save(request);

        return organizationAdmin;
    }

    // Reject a pending Organization Admin request.
    @Transactional
    public void rejectRequest(
            Integer requestId,
            Integer actingAdminId,
            String decisionNote) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        OrganizationAdminSignupRequest request =
                requestRepo.findByIdAndOrganizationId(
                        requestId,
                        superAdmin.getOrganizationId()
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Signup request was not found for your organization."
                        ));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending signup requests can be rejected."
            );
        }

        request.setStatus("REJECTED");
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(superAdmin.getId());
        request.setDecisionNote(normalizeNote(decisionNote));

        requestRepo.save(request);
    }

    // Verify that the acting account is an organization Super Admin.
    private User requireSuperAdmin(Integer adminId) {

        if (adminId == null) {
            throw new SecurityException(
                    "Super Admin authentication is required."
            );
        }

        User user = userRepo.findById(adminId)
                .orElseThrow(() ->
                        new SecurityException(
                                "The authenticated administrator was not found."
                        ));

        if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole())
                || !"SUPER_ADMIN".equalsIgnoreCase(
                user.getOrganizationRole())
                || user.getOrganizationId() == null) {

            throw new SecurityException(
                    "Only the organization Super Admin can process signup requests."
            );
        }

        return user;
    }

    private String normalizeNote(String note) {

        if (note == null || note.isBlank()) {
            return null;
        }

        String cleanNote = note.trim();

        if (cleanNote.length() > 500) {
            throw new IllegalArgumentException(
                    "Decision note cannot exceed 500 characters."
            );
        }

        return cleanNote;
    }
}
