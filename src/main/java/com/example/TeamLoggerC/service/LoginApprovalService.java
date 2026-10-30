package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.LoginApprovalRequest;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.LoginApprovalRequestRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

@Service
public class LoginApprovalService {

    private final LoginApprovalRequestRepo approvalRepo;
    private final NotificationService notificationService;


    // =====================================================
    // TOKEN CONFIGURATION
    // =====================================================

    /*
     * 32 random bytes = 256 bits of randomness.
     */
    private static final int TOKEN_BYTE_LENGTH = 32;

    /*
     * Approval token remains valid for 15 minutes.
     */
    private static final long TOKEN_EXPIRY_MINUTES = 15;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();


    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public LoginApprovalService(
            LoginApprovalRequestRepo approvalRepo,
            NotificationService notificationService
    ) {
        this.approvalRepo = approvalRepo;
        this.notificationService = notificationService;
    }


    // =====================================================
    // GENERATE SECURE APPROVAL TOKEN
    // =====================================================

    private String generateApprovalToken() {

        byte[] randomBytes =
                new byte[TOKEN_BYTE_LENGTH];

        SECURE_RANDOM.nextBytes(randomBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);
    }


    // =====================================================
    // CREATE LATE LOGIN REQUEST
    // =====================================================

    /**
     * Existing method.
     *
     * This is intentionally preserved so your existing
     * LoginSuccessHandler can continue calling:
     *
     *     createRequest(employee)
     *
     * The request is treated as a LATE_LOGIN request.
     */
    @Transactional
    public LoginApprovalRequest createRequest(
            User employee
    ) {

        return createRequest(
                employee,
                LoginApprovalRequest.REASON_LATE_LOGIN,
                null
        );
    }


    // =====================================================
    // CREATE LOGIN APPROVAL REQUEST
    // =====================================================

    /**
     * Creates or reuses today's login approval request.
     *
     * Supported reasons:
     *
     * LATE_LOGIN
     * UNKNOWN_IP
     * LATE_LOGIN,UNKNOWN_IP
     *
     * For an unknown IP:
     *
     * The IP is NOT automatically added to
     * employee_ip_access.
     *
     * The administrator is only approving the login.
     */
    @Transactional
    public LoginApprovalRequest createRequest(
            User employee,
            String approvalReason,
            String attemptedIpAddress
    ) {

        validateEmployee(employee);

        String normalizedReason =
                normalizeApprovalReason(approvalReason);

        LocalDateTime now =
                LocalDateTime.now();

        LocalDateTime startOfDay =
                LocalDate.now().atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();


        // =================================================
        // CHECK WHETHER EMPLOYEE WAS REJECTED TODAY
        // =================================================

        if (hasRejectedToday(employee.getId())) {

            throw new LoginRejectedForTodayException(
                    "Login approval was rejected for this employee today."
            );
        }


        // =================================================
        // FIND TODAY'S PENDING REQUEST
        // =================================================

        LoginApprovalRequest existingPending =
                approvalRepo
                        .findTopByEmployeeIdAndStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
                                employee.getId(),
                                LoginApprovalRequest.STATUS_PENDING,
                                startOfDay,
                                startOfTomorrow
                        )
                        .orElse(null);


        if (existingPending != null) {

            /*
             * If an existing request is pending, we reuse
             * the same request and token.
             *
             * IMPORTANT:
             * We do not silently replace the attempted IP.
             */
            existingPending =
                    refreshTokenIfNecessary(
                            existingPending
                    );

            System.out.println(
                    "LOGIN APPROVAL - EXISTING PENDING REQUEST"
            );

            System.out.println(
                    "REQUEST ID = "
                            + existingPending.getId()
            );

            System.out.println(
                    "EMPLOYEE ID = "
                            + existingPending.getEmployeeId()
            );

            System.out.println(
                    "APPROVAL REASON = "
                            + existingPending.getApprovalReason()
            );

            System.out.println(
                    "IP EXISTS = "
                            + (
                            existingPending.getAttemptedIpAddress()
                                    != null
                    )
            );

            System.out.println(
                    "TOKEN EXISTS = "
                            + (
                            existingPending.getApprovalToken()
                                    != null
                    )
            );

            return existingPending;
        }


        // =================================================
        // CHECK TODAY'S APPROVED REQUEST
        // =================================================

        List<LoginApprovalRequest> approvedRequests =
                approvalRepo
                        .findByEmployeeIdAndStatusAndLoginTimeBetween(
                                employee.getId(),
                                LoginApprovalRequest.STATUS_APPROVED,
                                startOfDay,
                                startOfTomorrow
                        );

        if (!approvedRequests.isEmpty()) {

            LoginApprovalRequest existing =
                    approvedRequests.stream()
                            .max(
                                    Comparator.comparing(
                                            LoginApprovalRequest::getLoginTime
                                    )
                            )
                            .orElse(
                                    approvedRequests.get(0)
                            );

            System.out.println(
                    "LOGIN APPROVAL - ALREADY APPROVED TODAY"
            );

            System.out.println(
                    "REQUEST ID = "
                            + existing.getId()
            );

            System.out.println(
                    "EMPLOYEE ID = "
                            + existing.getEmployeeId()
            );

            System.out.println(
                    "STATUS = "
                            + existing.getStatus()
            );

            return existing;
        }


        // =================================================
        // CREATE NEW REQUEST
        // =================================================

        LoginApprovalRequest request =
                new LoginApprovalRequest();

        request.setEmployeeId(
                employee.getId()
        );

        request.setAdminId(
                employee.getAdminId()
        );

        request.setLoginTime(now);

        request.setStatus(
                LoginApprovalRequest.STATUS_PENDING
        );

        request.setApprovalReason(
                normalizedReason
        );


        // =================================================
        // STORE ATTEMPTED IP
        // =================================================

        if (attemptedIpAddress != null
                && !attemptedIpAddress.isBlank()) {

            request.setAttemptedIpAddress(
                    attemptedIpAddress.trim()
            );
        }


        // =================================================
        // REJECTED-FOR-DAY FLAG
        // =================================================

        request.setRejectedForDay(false);


        // =================================================
        // GENERATE APPROVAL TOKEN
        // =================================================

        request.setApprovalToken(
                generateApprovalToken()
        );


        // =================================================
        // SET TOKEN EXPIRATION
        // =================================================

        request.setTokenExpiresAt(
                now.plusMinutes(
                        TOKEN_EXPIRY_MINUTES
                )
        );


        // =================================================
        // TOKEN HAS NOT BEEN USED
        // =================================================

        request.setTokenUsedAt(null);


        // =================================================
        // SAVE
        // =================================================

        LoginApprovalRequest saved =
                approvalRepo.save(request);


        // =================================================
        // LOG REQUEST INFORMATION
        // =================================================

        System.out.println(
                "=========================================="
        );

        System.out.println(
                "NEW LOGIN APPROVAL REQUEST CREATED"
        );

        System.out.println(
                "REQUEST ID = "
                        + saved.getId()
        );

        System.out.println(
                "EMPLOYEE ID = "
                        + saved.getEmployeeId()
        );

        System.out.println(
                "ADMIN ID = "
                        + saved.getAdminId()
        );

        System.out.println(
                "LOGIN TIME = "
                        + saved.getLoginTime()
        );

        System.out.println(
                "STATUS = "
                        + saved.getStatus()
        );

        System.out.println(
                "APPROVAL REASON = "
                        + saved.getApprovalReason()
        );

        /*
         * Never print the actual IP unnecessarily in logs.
         * We only indicate whether one was captured.
         */
        System.out.println(
                "ATTEMPTED IP CAPTURED = "
                        + (
                        saved.getAttemptedIpAddress()
                                != null
                )
        );

        /*
         * NEVER print the actual approval token.
         */
        System.out.println(
                "APPROVAL TOKEN CREATED = YES"
        );

        System.out.println(
                "TOKEN EXPIRES AT = "
                        + saved.getTokenExpiresAt()
        );

        System.out.println(
                "=========================================="
        );


        // =================================================
        // NOTIFY ADMIN
        // =================================================

        String notificationTitle =
                buildNotificationTitle(
                        normalizedReason
                );

        String notificationMessage =
                buildNotificationMessage(
                        employee,
                        normalizedReason,
                        saved.getAttemptedIpAddress()
                );

        notificationService.createNotification(
                employee.getAdminId(),
                employee.getId(),
                "LOGIN_APPROVAL",
                notificationTitle,
                notificationMessage,
                saved.getId()
        );


        return saved;
    }


    // =====================================================
    // ADD LOCATION INFORMATION
    // =====================================================

    /**
     * Stores approximate IP geolocation information on
     * the login approval request.
     *
     * This will be called later by the IP geolocation
     * service.
     */
    @Transactional
    public LoginApprovalRequest updateRequestLocation(
            Integer requestId,
            String country,
            String state,
            String city,
            String isp,
            Double latitude,
            Double longitude
    ) {

        LoginApprovalRequest request =
                getRequest(requestId);

        request.setCountry(country);
        request.setState(state);
        request.setCity(city);
        request.setIsp(isp);
        request.setLatitude(latitude);
        request.setLongitude(longitude);

        return approvalRepo.save(request);
    }


    // =====================================================
    // CHECK WHETHER EMPLOYEE IS APPROVED TODAY
    // =====================================================

    public boolean isApprovedToday(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return false;
        }

        LocalDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        List<LoginApprovalRequest> approvedRequests =
                approvalRepo
                        .findByEmployeeIdAndStatusAndLoginTimeBetween(
                                employeeId,
                                LoginApprovalRequest.STATUS_APPROVED,
                                startOfDay,
                                startOfTomorrow
                        );

        return !approvedRequests.isEmpty();
    }


    // =====================================================
    // CHECK WHETHER EMPLOYEE WAS REJECTED TODAY
    // =====================================================

    /**
     * This is the important method for:
     *
     * ADMIN REJECTS
     *       ↓
     * EMPLOYEE BLOCKED FOR TODAY
     */
    public boolean hasRejectedToday(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return false;
        }

        LocalDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        List<LoginApprovalRequest> rejectedRequests =
                approvalRepo
                        .findByEmployeeIdAndStatusAndRejectedForDayTrueAndLoginTimeBetween(
                                employeeId,
                                LoginApprovalRequest.STATUS_REJECTED,
                                startOfDay,
                                startOfTomorrow
                        );

        return !rejectedRequests.isEmpty();
    }


    // =====================================================
    // GET TODAY'S PENDING REQUEST
    // =====================================================

    public LoginApprovalRequest getPendingRequestToday(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return null;
        }

        LocalDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        return approvalRepo
                .findTopByEmployeeIdAndStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
                        employeeId,
                        LoginApprovalRequest.STATUS_PENDING,
                        startOfDay,
                        startOfTomorrow
                )
                .orElse(null);
    }


    // =====================================================
    // GET PENDING REQUESTS FOR ADMIN
    // =====================================================

    public List<LoginApprovalRequest> getPendingRequests(
            Integer adminId
    ) {

        return approvalRepo
                .findByAdminIdAndStatusOrderByLoginTimeDesc(
                        adminId,
                        LoginApprovalRequest.STATUS_PENDING
                );
    }


    // =====================================================
    // GET REQUEST BY ID
    // =====================================================

    public LoginApprovalRequest getRequest(
            Integer requestId
    ) {

        return approvalRepo
                .findById(requestId)
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Login approval request not found: "
                                        + requestId
                        )
                );
    }


    // =====================================================
    // GET REQUEST BY TOKEN
    // =====================================================

    /**
     * Used by EmployeeLoginApprovalController.
     *
     * LOGIN_APPROVAL_TOKEN
     *          ↓
     * approvalToken
     *          ↓
     * database
     *          ↓
     * LoginApprovalRequest
     */
    public LoginApprovalRequest getRequestByToken(
            String approvalToken
    ) {

        if (approvalToken == null
                || approvalToken.isBlank()) {

            return null;
        }

        return approvalRepo
                .findByApprovalToken(
                        approvalToken
                )
                .orElse(null);
    }


    // =====================================================
    // ADMIN APPROVE
    // =====================================================

    @Transactional
    public LoginApprovalRequest approve(
            Integer requestId,
            Integer adminId
    ) {

        LoginApprovalRequest request =
                getRequest(requestId);


        // =================================================
        // VERIFY ADMIN
        // =================================================

        verifyAdmin(
                request,
                adminId
        );


        // =================================================
        // PREVENT DOUBLE APPROVAL
        // =================================================

        if (!LoginApprovalRequest.STATUS_PENDING
                .equalsIgnoreCase(
                        request.getStatus()
                )) {

            return request;
        }


        // =================================================
        // APPROVE
        // =================================================

        request.setStatus(
                LoginApprovalRequest.STATUS_APPROVED
        );

        request.setProcessedAt(
                LocalDateTime.now()
        );

        /*
         * IMPORTANT:
         *
         * We DO NOT change:
         *
         * request.getApprovalToken()
         *
         * The same token remains attached to the request.
         */


        LoginApprovalRequest saved =
                approvalRepo.save(request);


        // =================================================
        // NOTIFY EMPLOYEE
        // =================================================

        String title =
                isUnknownIpRequest(request)
                        ? "Login Approved"
                        : "Late Login Approved";

        String message =
                buildApprovalMessage(request);


        notificationService.createNotification(
                saved.getEmployeeId(),
                adminId,
                "LOGIN_APPROVED",
                title,
                message,
                saved.getId()
        );


        return saved;
    }


    // =====================================================
    // ADMIN REJECT
    // =====================================================

    @Transactional
    public LoginApprovalRequest reject(
            Integer requestId,
            Integer adminId
    ) {

        LoginApprovalRequest request =
                getRequest(requestId);


        // =================================================
        // VERIFY ADMIN
        // =================================================

        verifyAdmin(
                request,
                adminId
        );


        // =================================================
        // PREVENT DOUBLE REJECTION
        // =================================================

        if (!LoginApprovalRequest.STATUS_PENDING
                .equalsIgnoreCase(
                        request.getStatus()
                )) {

            return request;
        }


        // =================================================
        // REJECT
        // =================================================

        request.setStatus(
                LoginApprovalRequest.STATUS_REJECTED
        );

        request.setProcessedAt(
                LocalDateTime.now()
        );


        /*
         * IMPORTANT:
         *
         * This rejection applies to the employee for
         * today's date.
         */
        request.setRejectedForDay(true);


        /*
         * The token remains in the database.
         *
         * It cannot be used because the request status
         * is now REJECTED.
         */
        LoginApprovalRequest saved =
                approvalRepo.save(request);


        // =================================================
        // NOTIFY EMPLOYEE
        // =================================================

        String title =
                isUnknownIpRequest(request)
                        ? "Login Rejected"
                        : "Late Login Rejected";

        String message =
                "Your login approval request has been rejected. "
                        + "You cannot log in again today.";


        notificationService.createNotification(
                saved.getEmployeeId(),
                adminId,
                "LOGIN_REJECTED",
                title,
                message,
                saved.getId()
        );


        return saved;
    }


    // =====================================================
    // CHECK UNKNOWN IP REJECTION TODAY
    // =====================================================

    /**
     * Checks whether a particular IP was rejected today.
     *
     * This is useful later if we want to display a more
     * specific message to the employee.
     */
    public boolean wasIpRejectedToday(
            Integer employeeId,
            String ipAddress
    ) {

        if (employeeId == null
                || ipAddress == null
                || ipAddress.isBlank()) {

            return false;
        }

        LocalDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        return approvalRepo
                .findTopByEmployeeIdAndAttemptedIpAddressAndStatusAndRejectedForDayTrueAndLoginTimeBetweenOrderByLoginTimeDesc(
                        employeeId,
                        ipAddress.trim(),
                        LoginApprovalRequest.STATUS_REJECTED,
                        startOfDay,
                        startOfTomorrow
                )
                .isPresent();
    }


    // =====================================================
    // VERIFY ADMIN
    // =====================================================

    private void verifyAdmin(
            LoginApprovalRequest request,
            Integer adminId
    ) {

        if (request.getAdminId() == null) {

            throw new IllegalArgumentException(
                    "Login approval request has no assigned admin."
            );
        }

        if (adminId == null
                || !request.getAdminId().equals(adminId)) {

            throw new IllegalArgumentException(
                    "You are not authorized to process this request."
            );
        }
    }


    // =====================================================
    // GET LATEST REQUEST FOR EMPLOYEE
    // =====================================================

    public LoginApprovalRequest getRequestForEmployee(
            Integer employeeId
    ) {

        return approvalRepo
                .findTopByEmployeeIdOrderByLoginTimeDesc(
                        employeeId
                )
                .orElse(null);
    }


    // =====================================================
    // MARK APPROVAL TOKEN AS USED
    // =====================================================

    @Transactional
    public LoginApprovalRequest markTokenUsed(
            LoginApprovalRequest request
    ) {

        if (request == null) {
            return null;
        }

        /*
         * Only set the timestamp if it hasn't already been
         * recorded.
         */
        if (request.getTokenUsedAt() == null) {

            request.setTokenUsedAt(
                    LocalDateTime.now()
            );
        }

        return approvalRepo.save(request);
    }


    // =====================================================
    // GET REQUEST BY ID
    // =====================================================

    public LoginApprovalRequest getRequestById(
            Integer requestId
    ) {

        return approvalRepo
                .findById(requestId)
                .orElse(null);
    }


    // =====================================================
    // GET LATEST REQUEST FOR EMPLOYEE TODAY
    // =====================================================

    public LoginApprovalRequest getLatestRequestToday(
            Integer employeeId
    ) {

        if (employeeId == null) {
            return null;
        }

        LocalDateTime startOfDay =
                LocalDate.now()
                        .atStartOfDay();

        LocalDateTime startOfTomorrow =
                LocalDate.now()
                        .plusDays(1)
                        .atStartOfDay();

        return approvalRepo
                .findTopByEmployeeIdAndLoginTimeBetweenOrderByLoginTimeDesc(
                        employeeId,
                        startOfDay,
                        startOfTomorrow
                )
                .orElse(null);
    }


    // =====================================================
    // REFRESH TOKEN IF NECESSARY
    // =====================================================

    private LoginApprovalRequest refreshTokenIfNecessary(
            LoginApprovalRequest request
    ) {

        LocalDateTime now =
                LocalDateTime.now();

        boolean tokenMissing =
                request.getApprovalToken() == null
                        || request.getApprovalToken().isBlank();

        boolean tokenExpired =
                request.getTokenExpiresAt() != null
                        && request.getTokenExpiresAt()
                        .isBefore(now);

        if (tokenMissing || tokenExpired) {

            request.setApprovalToken(
                    generateApprovalToken()
            );

            request.setTokenExpiresAt(
                    now.plusMinutes(
                            TOKEN_EXPIRY_MINUTES
                    )
            );

            request.setTokenUsedAt(null);

            return approvalRepo.save(request);
        }

        return request;
    }


    // =====================================================
    // VALIDATE EMPLOYEE
    // =====================================================

    private void validateEmployee(
            User employee
    ) {

        if (employee == null) {

            throw new IllegalArgumentException(
                    "Employee cannot be null."
            );
        }

        if (employee.getId() == null) {

            throw new IllegalArgumentException(
                    "Employee ID cannot be null."
            );
        }

        if (employee.getAdminId() == null) {

            throw new IllegalArgumentException(
                    "Employee does not have an assigned admin."
            );
        }
    }


    // =====================================================
    // NORMALIZE APPROVAL REASON
    // =====================================================

    private String normalizeApprovalReason(
            String approvalReason
    ) {

        if (approvalReason == null
                || approvalReason.isBlank()) {

            return LoginApprovalRequest.REASON_LATE_LOGIN;
        }

        String reason =
                approvalReason
                        .trim()
                        .toUpperCase();

        /*
         * Single reason.
         */
        if (LoginApprovalRequest.REASON_LATE_LOGIN
                .equals(reason)) {

            return LoginApprovalRequest.REASON_LATE_LOGIN;
        }

        if (LoginApprovalRequest.REASON_UNKNOWN_IP
                .equals(reason)) {

            return LoginApprovalRequest.REASON_UNKNOWN_IP;
        }

        /*
         * Combined reason.
         */
        if ("LATE_LOGIN,UNKNOWN_IP".equals(reason)
                || "UNKNOWN_IP,LATE_LOGIN".equals(reason)) {

            return "LATE_LOGIN,UNKNOWN_IP";
        }

        throw new IllegalArgumentException(
                "Unsupported login approval reason: "
                        + approvalReason
        );
    }


    // =====================================================
    // CHECK UNKNOWN IP
    // =====================================================

    private boolean isUnknownIpRequest(
            LoginApprovalRequest request
    ) {

        if (request == null
                || request.getApprovalReason() == null) {

            return false;
        }

        return request.getApprovalReason()
                .contains(
                        LoginApprovalRequest.REASON_UNKNOWN_IP
                );
    }


    // =====================================================
    // BUILD ADMIN NOTIFICATION TITLE
    // =====================================================

    private String buildNotificationTitle(
            String approvalReason
    ) {

        if (approvalReason.contains(
                LoginApprovalRequest.REASON_LATE_LOGIN)
                && approvalReason.contains(
                LoginApprovalRequest.REASON_UNKNOWN_IP)) {

            return "Login Approval Required";
        }

        if (approvalReason.contains(
                LoginApprovalRequest.REASON_UNKNOWN_IP)) {

            return "Unknown IP Login Approval Required";
        }

        return "Late Login Approval Required";
    }


    // =====================================================
    // BUILD ADMIN NOTIFICATION MESSAGE
    // =====================================================

    private String buildNotificationMessage(
            User employee,
            String approvalReason,
            String attemptedIpAddress
    ) {

        StringBuilder message =
                new StringBuilder();

        message.append(
                employee.getName()
        );

        if (approvalReason.contains(
                LoginApprovalRequest.REASON_LATE_LOGIN)) {

            message.append(
                    " is requesting approval for a late login."
            );
        }

        if (approvalReason.contains(
                LoginApprovalRequest.REASON_UNKNOWN_IP)) {

            if (approvalReason.contains(
                    LoginApprovalRequest.REASON_LATE_LOGIN)) {

                message.append(
                        " The login was also attempted from an unknown IP."
                );

            } else {

                message.append(
                        " is requesting login approval from an unknown IP."
                );
            }
        }

        if (attemptedIpAddress != null
                && !attemptedIpAddress.isBlank()) {

            message.append(
                    " IP: "
            );

            message.append(
                    attemptedIpAddress
            );
        }

        return message.toString();
    }


    // =====================================================
    // BUILD EMPLOYEE APPROVAL MESSAGE
    // =====================================================

    private String buildApprovalMessage(
            LoginApprovalRequest request
    ) {

        String reason =
                request.getApprovalReason();

        if (reason != null
                && reason.contains(
                LoginApprovalRequest.REASON_UNKNOWN_IP)
                && reason.contains(
                LoginApprovalRequest.REASON_LATE_LOGIN)) {

            return "Your late login from an unrecognized IP "
                    + "has been approved.";
        }

        if (reason != null
                && reason.contains(
                LoginApprovalRequest.REASON_UNKNOWN_IP)) {

            return "Your login from the unrecognized IP "
                    + "has been approved.";
        }

        return "Your late login request has been approved.";
    }


    // =====================================================
    // EXCEPTION
    // =====================================================

    /**
     * Used internally when the employee has already been
     * rejected for today's login.
     *
     * We will handle this in the login layer later.
     */
    public static class LoginRejectedForTodayException
            extends RuntimeException {

        public LoginRejectedForTodayException(
                String message
        ) {
            super(message);
        }
    }
}

