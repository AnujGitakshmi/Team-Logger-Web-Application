
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Branch;
import com.example.TeamLoggerC.entity.BranchAdminSignupRequest;
import com.example.TeamLoggerC.entity.Organization;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.BranchAdminSignupRequestRepo;
import com.example.TeamLoggerC.repository.BranchRepo;
import com.example.TeamLoggerC.repository.OrganizationRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class BranchAdminSignupApprovalService {

    private final BranchAdminSignupRequestRepo requestRepo;
    private final OrganizationRepo organizationRepo;
    private final BranchRepo branchRepo;
    private final UserRepo userRepo;

    public BranchAdminSignupApprovalService(
            BranchAdminSignupRequestRepo requestRepo,
            OrganizationRepo organizationRepo,
            BranchRepo branchRepo,
            UserRepo userRepo) {

        this.requestRepo = requestRepo;
        this.organizationRepo = organizationRepo;
        this.branchRepo = branchRepo;
        this.userRepo = userRepo;
    }

    /**
     * Returns pending branch-admin signup requests.
     */
    @Transactional(readOnly = true)
    public List<BranchAdminSignupRequest> getPendingRequests(
            Integer actingAdminId) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        return requestRepo.findByStatusOrderByRequestedAtDesc("PENDING")
                .stream()
                .filter(request -> belongsToOrganization(
                        request, superAdmin.getOrganizationId()))
                .toList();
    }

    /**
     * Approves a pending request, creates its branch with location,
     * and activates the branch-admin account atomically.
     */
    @Transactional
    public User approveRequest(
            Integer requestId,
            Integer actingAdminId,
            String decisionNote) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        BranchAdminSignupRequest request = requestRepo.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Signup request was not found."));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending signup requests can be approved.");
        }

        Organization organization = getRequestOrganization(
                request, superAdmin);

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepo.findByEmail(email).isPresent()) {
            throw new IllegalStateException(
                    "An account with this email already exists.");
        }

        String branchName = request.getBranchName().trim();
        String normalizedBranchName =
                branchName.toLowerCase(Locale.ROOT);

        if (branchRepo.existsByOrganizationAndNormalizedName(
                organization, normalizedBranchName)) {

            throw new IllegalStateException(
                    "A branch with this name already exists in this organization.");
        }

        // Create branch with submitted location details.
        Branch branch = new Branch();

        branch.setOrganization(organization);
        branch.setName(branchName);
        branch.setNormalizedName(normalizedBranchName);

        branch.setCountry(request.getCountry());
        branch.setState(request.getState());
        branch.setCity(request.getCity());
        branch.setPostalCode(request.getPostalCode());
        branch.setAddress(request.getAddress());

        branch.setStatus("ACTIVE");

        branch = branchRepo.saveAndFlush(branch);

        // Create branch-admin account using the already-encoded password.
        User branchAdmin = new User();

        branchAdmin.setName(request.getAdminName().trim());
        branchAdmin.setEmail(email);
        branchAdmin.setPassword(request.getPassword());

        branchAdmin.setPasswordChangedAt(LocalDateTime.now());
        branchAdmin.setPasswordVersion(1);
        branchAdmin.setPasswordMustChange(false);

        branchAdmin.setRole("ADMIN");
        branchAdmin.setOrganizationId(organization.getId());
        branchAdmin.setBranchId(branch.getId());
        branchAdmin.setOrganizationRole("BRANCH_ADMIN");

        // The branch admin manages their own employees.
        branchAdmin.setAdminId(null);
        branchAdmin.setTeamId(null);
        branchAdmin.setApplicationName(null);
        branchAdmin.setEmployeeLoginId(null);

        branchAdmin.setDesignation("BRANCH_ADMIN");
        branchAdmin.setSeniorId(null);
        branchAdmin.setDomain(null);
        branchAdmin.setDescription(null);
        branchAdmin.setProfilePhoto(null);

        branchAdmin.setStatus("ACTIVE");
        branchAdmin.setDisabledAt(null);
        branchAdmin.setScheduledDeletionAt(null);

        branchAdmin = userRepo.saveAndFlush(branchAdmin);

        // Record approval after both records have been saved.
        request.setStatus("APPROVED");
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(superAdmin.getId());
        request.setDecisionNote(normalizeNote(decisionNote));

        requestRepo.save(request);

        return branchAdmin;
    }

    /**
     * Rejects a pending request without creating an account or branch.
     */
    @Transactional
    public void rejectRequest(
            Integer requestId,
            Integer actingAdminId,
            String decisionNote) {

        User superAdmin = requireSuperAdmin(actingAdminId);

        BranchAdminSignupRequest request = requestRepo.findById(requestId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Signup request was not found."));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending signup requests can be rejected.");
        }

        getRequestOrganization(request, superAdmin);

        request.setStatus("REJECTED");
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(superAdmin.getId());
        request.setDecisionNote(normalizeNote(decisionNote));

        requestRepo.save(request);
    }

    /**
     * Finds the request organization and ensures it belongs to
     * the currently authenticated Super Admin.
     */
    private Organization getRequestOrganization(
            BranchAdminSignupRequest request,
            User superAdmin) {

        String normalizedOrganizationName =
                request.getOrganizationName()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        Organization organization = organizationRepo
                .findByNormalizedName(normalizedOrganizationName)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "The requested organization no longer exists."));

        if (organization.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(organization.getStatus())) {

            throw new IllegalStateException(
                    "The requested organization is not active.");
        }

        if (!organization.getId().equals(
                superAdmin.getOrganizationId())) {

            throw new IllegalStateException(
                    "You cannot process requests for another organization.");
        }

        return organization;
    }

    /**
     * Checks whether a request belongs to the specified organization.
     */
    private boolean belongsToOrganization(
            BranchAdminSignupRequest request,
            Integer organizationId) {

        if (request.getOrganizationName() == null) {
            return false;
        }

        String normalizedOrganizationName =
                request.getOrganizationName()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        return organizationRepo
                .findByNormalizedName(normalizedOrganizationName)
                .map(organization ->
                        organizationId.equals(organization.getId())
                                && "ACTIVE".equalsIgnoreCase(
                                organization.getStatus()))
                .orElse(false);
    }

    /**
     * Enforces organization-level Super Admin authorization.
     */
    private User requireSuperAdmin(Integer adminId) {

        if (adminId == null) {
            throw new SecurityException(
                    "Super Admin authentication is required.");
        }

        User user = userRepo.findById(adminId)
                .orElseThrow(() ->
                        new SecurityException(
                                "The authenticated administrator was not found."));

        if (!"SUPER_ADMIN".equalsIgnoreCase(user.getRole())
                || !"SUPER_ADMIN".equalsIgnoreCase(
                user.getOrganizationRole())
                || user.getOrganizationId() == null) {

            throw new SecurityException(
                    "Only the organization Super Admin can process signup requests.");
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
                    "Decision note cannot exceed 500 characters.");
        }

        return cleanNote;
    }
}
