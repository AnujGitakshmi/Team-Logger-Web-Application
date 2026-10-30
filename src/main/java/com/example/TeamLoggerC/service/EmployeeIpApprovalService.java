package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.EmployeeIpAccess;
import com.example.TeamLoggerC.entity.EmployeeIpApprovalRequest;
import com.example.TeamLoggerC.repository.EmployeeIpApprovalRequestRepo;
import com.example.TeamLoggerC.repository.EmployeeIpAccessRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeIpApprovalService {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";

    private static final String ACCESS_TYPE_SECONDARY = "SECONDARY";

    // Maximum approved secondary IPs per employee.
    // The office IP is managed separately.
    private static final int MAX_SECONDARY_IP_ADDRESSES = 5;

    private final EmployeeIpApprovalRequestRepo approvalRequestRepo;
    private final EmployeeIpAccessRepo ipAccessRepo;
    private final EmployeeIpAccessService employeeIpAccessService;

    public EmployeeIpApprovalService(
            EmployeeIpApprovalRequestRepo approvalRequestRepo,
            EmployeeIpAccessRepo ipAccessRepo,
            EmployeeIpAccessService employeeIpAccessService
    ) {
        this.approvalRequestRepo = approvalRequestRepo;
        this.ipAccessRepo = ipAccessRepo;
        this.employeeIpAccessService = employeeIpAccessService;
    }

    // =========================================================
    // GET EXISTING REQUEST
    // =========================================================

    public Optional<EmployeeIpApprovalRequest> getRequest(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    ) {
        String normalizedIp = normalizeIp(ipAddress);

        return approvalRequestRepo
                .findByEmployeeIdAndAdminIdAndIpAddress(
                        employeeId,
                        adminId,
                        normalizedIp
                );
    }

    // =========================================================
    // GET PENDING REQUEST
    // =========================================================

    public Optional<EmployeeIpApprovalRequest> getPendingRequest(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    ) {
        String normalizedIp = normalizeIp(ipAddress);

        return approvalRequestRepo
                .findByEmployeeIdAndAdminIdAndIpAddressAndStatus(
                        employeeId,
                        adminId,
                        normalizedIp,
                        STATUS_PENDING
                );
    }

    // =========================================================
    // CREATE OR REUSE EMPLOYEE IP REQUEST
    // =========================================================

    @Transactional
    public EmployeeIpApprovalRequest createOrGetRequest(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    ) {
        String normalizedIp = normalizeIp(ipAddress);

        // Already approved IPs do not need another request.
        if (employeeIpAccessService.isIpApproved(
                employeeId, adminId, normalizedIp
        )) {
            throw new IllegalStateException(
                    "This IP address is already approved."
            );
        }

        Optional<EmployeeIpApprovalRequest> existing =
                approvalRequestRepo
                        .findByEmployeeIdAndAdminIdAndIpAddress(
                                employeeId,
                                adminId,
                                normalizedIp
                        );

        LocalDateTime now = LocalDateTime.now();

        if (existing.isPresent()) {
            EmployeeIpApprovalRequest request = existing.get();

            // Keep an existing pending request.
            if (STATUS_PENDING.equalsIgnoreCase(
                    request.getStatus()
            )) {
                return request;
            }

            // Reopen rejected or historical requests when the IP
            // is still not approved.
            request.setStatus(STATUS_PENDING);
            request.setRequestedAt(now);
            request.setProcessedAt(null);
            request.setProcessedByAdminId(null);
            request.setDecisionNote(null);

            return approvalRequestRepo.save(request);
        }

        EmployeeIpApprovalRequest request =
                new EmployeeIpApprovalRequest();

        request.setEmployeeId(employeeId);
        request.setAdminId(adminId);
        request.setIpAddress(normalizedIp);
        request.setStatus(STATUS_PENDING);
        request.setRequestedAt(now);

        return approvalRequestRepo.save(request);
    }

    // =========================================================
    // CHECK REQUEST STATUS
    // =========================================================

    public String getRequestStatus(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    ) {
        String normalizedIp = normalizeIp(ipAddress);

        Optional<EmployeeIpApprovalRequest> request =
                approvalRequestRepo
                        .findByEmployeeIdAndAdminIdAndIpAddress(
                                employeeId,
                                adminId,
                                normalizedIp
                        );

        return request.map(
                EmployeeIpApprovalRequest::getStatus
        ).orElse(null);
    }

    // =========================================================
    // APPROVE REQUEST
    // =========================================================


    @Transactional
    public EmployeeIpApprovalRequest approveRequest(
            Integer requestId,
            Integer adminId,
            Integer processedByAdminId,
            String decisionNote
    ) {
        EmployeeIpApprovalRequest request =
                approvalRequestRepo
                        .findByIdAndAdminId(requestId, adminId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "IP approval request not found."
                                )
                        );

        if (!STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException(
                    "Only pending IP requests can be approved."
            );
        }

        Integer employeeId = request.getEmployeeId();
        String requestedIp = normalizeIp(request.getIpAddress());
        LocalDateTime now = LocalDateTime.now();

        // Find the existing record for this employee, admin and IP.
        EmployeeIpAccess requestedIpRecord =
                ipAccessRepo
                        .findByEmployeeIdAndAdminIdAndIpAddress(
                                employeeId,
                                adminId,
                                requestedIp
                        )
                        .orElse(null);

        // If not found, check whether the IP record already exists
        // for this employee under another organization.
        if (requestedIpRecord == null) {
            EmployeeIpAccess sameEmployeeIp =
                    ipAccessRepo
                            .findByEmployeeIdAndIpAddress(
                                    employeeId,
                                    requestedIp
                            )
                            .orElse(null);

            if (sameEmployeeIp != null) {
                if (!adminId.equals(sameEmployeeIp.getAdminId())) {
                    throw new IllegalStateException(
                            "This IP record belongs to another organization."
                    );
                }

                requestedIpRecord = sameEmployeeIp;
            }
        }

        // Never convert the office IP into a secondary IP.
        if (requestedIpRecord != null
                && EmployeeIpAccessService.ACCESS_TYPE_OFFICE
                .equalsIgnoreCase(
                        requestedIpRecord.getAccessType()
                )) {
            throw new IllegalStateException(
                    "The office IP cannot be registered as a secondary IP."
            );
        }

        // Load this employee's IP records.
        List<EmployeeIpAccess> employeeIpRecords =
                ipAccessRepo
                        .findByEmployeeIdAndAdminIdOrderByCreatedAtAsc(
                                employeeId,
                                adminId
                        );

        // Revoke every OTHER approved secondary IP.
        // Do not delete records: preserve them for history.
        for (EmployeeIpAccess existingIp : employeeIpRecords) {
            boolean isSecondary =
                    ACCESS_TYPE_SECONDARY.equalsIgnoreCase(
                            existingIp.getAccessType()
                    );

            boolean isApproved =
                    EmployeeIpAccessService.STATUS_APPROVED
                            .equalsIgnoreCase(existingIp.getStatus());

            String existingAddress = existingIp.getIpAddress() == null
                    ? ""
                    : existingIp.getIpAddress().trim();

            boolean isRequestedIp =
                    requestedIp.equalsIgnoreCase(existingAddress);

            if (isSecondary && isApproved && !isRequestedIp) {
                existingIp.setStatus(
                        EmployeeIpAccessService.STATUS_REVOKED
                );
                existingIp.setUpdatedAt(now);

                ipAccessRepo.save(existingIp);
            }
        }

        // Reuse the requested IP's historical record if it exists.
        // Otherwise, create a new record.
        EmployeeIpAccess newSecondary = requestedIpRecord;

        if (newSecondary == null) {
            newSecondary = new EmployeeIpAccess();

            newSecondary.setEmployeeId(employeeId);
            newSecondary.setAdminId(adminId);
            newSecondary.setIpAddress(requestedIp);
            newSecondary.setCreatedAt(now);
        }

        // Activate the newly approved IP.
        newSecondary.setAccessType(ACCESS_TYPE_SECONDARY);
        newSecondary.setStatus(
                EmployeeIpAccessService.STATUS_APPROVED
        );
        newSecondary.setApprovedBy(processedByAdminId);
        newSecondary.setApprovedAt(now);
        newSecondary.setUpdatedAt(now);

        ipAccessRepo.save(newSecondary);

        // Complete the approval request.
        request.setStatus(STATUS_APPROVED);
        request.setProcessedAt(now);
        request.setProcessedByAdminId(processedByAdminId);
        request.setDecisionNote(decisionNote);

        return approvalRequestRepo.save(request);
    }


    // =========================================================
    // REJECT REQUEST
    // =========================================================

    @Transactional
    public EmployeeIpApprovalRequest rejectRequest(
            Integer requestId,
            Integer adminId,
            Integer processedByAdminId,
            String decisionNote
    ) {
        EmployeeIpApprovalRequest request =
                approvalRequestRepo
                        .findByIdAndAdminId(
                                requestId,
                                adminId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "IP approval request not found."
                                )
                        );

        if (!STATUS_PENDING.equalsIgnoreCase(
                request.getStatus()
        )) {
            throw new IllegalStateException(
                    "Only pending IP requests can be rejected."
            );
        }

        request.setStatus(STATUS_REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedByAdminId(processedByAdminId);
        request.setDecisionNote(decisionNote);

        return approvalRequestRepo.save(request);
    }

    // =========================================================
    // ADMIN PENDING REQUESTS
    // =========================================================

    public List<EmployeeIpApprovalRequest> getPendingRequestsForAdmin(
            Integer adminId
    ) {
        return approvalRequestRepo
                .findByAdminIdAndStatusOrderByRequestedAtDesc(
                        adminId,
                        STATUS_PENDING
                );
    }

    // =========================================================
    // ALL ADMIN REQUESTS
    // =========================================================

    public List<EmployeeIpApprovalRequest> getRequestsForAdmin(
            Integer adminId
    ) {
        return approvalRequestRepo
                .findByAdminIdOrderByRequestedAtDesc(adminId);
    }

    // =========================================================
    // EMPLOYEE REQUEST HISTORY
    // =========================================================

    public List<EmployeeIpApprovalRequest> getEmployeeRequests(
            Integer employeeId,
            Integer adminId
    ) {
        return approvalRequestRepo
                .findByEmployeeIdAndAdminIdOrderByRequestedAtDesc(
                        employeeId,
                        adminId
                );
    }

    // =========================================================
    // NORMALIZE IP
    // =========================================================

    private String normalizeIp(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new IllegalArgumentException(
                    "IP address cannot be empty."
            );
        }

        return ipAddress.trim();
    }
}