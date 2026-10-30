 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.LoginApprovalRequest;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LoginApprovalRequestRepo
        extends JpaRepository<LoginApprovalRequest, Integer> {


    // =====================================================
    // ADMIN - GET PENDING REQUESTS
    // =====================================================

    List<LoginApprovalRequest>
    findByAdminIdAndStatusOrderByLoginTimeDesc(
            Integer adminId,
            String status
    );


    // =====================================================
    // FIND EMPLOYEE REQUESTS BY STATUS AND DATE
    // =====================================================

    List<LoginApprovalRequest>
    findByEmployeeIdAndStatusAndLoginTimeBetween(
            Integer employeeId,
            String status,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // GET LATEST REQUEST FOR EMPLOYEE
    // =====================================================

    Optional<LoginApprovalRequest>
    findTopByEmployeeIdOrderByLoginTimeDesc(
            Integer employeeId
    );


    // =====================================================
    // GET LATEST REQUEST FOR EMPLOYEE TODAY
    // =====================================================

    Optional<LoginApprovalRequest>
    findTopByEmployeeIdAndLoginTimeBetweenOrderByLoginTimeDesc(
            Integer employeeId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND REQUEST USING APPROVAL TOKEN
    // =====================================================

    /*
     * Existing token-based login approval flow.
     *
     * LOGIN_APPROVAL_TOKEN
     *          ↓
     * approvalToken
     *          ↓
     * database
     *          ↓
     * LoginApprovalRequest
     */
    Optional<LoginApprovalRequest>
    findByApprovalToken(
            String approvalToken
    );


    // =====================================================
    // FIND PENDING REQUEST FOR EMPLOYEE TODAY
    // =====================================================

    /**
     * Used when an employee attempts login.
     *
     * Prevents TeamLogger from creating multiple pending
     * approval requests for the same employee on the same day.
     */
    Optional<LoginApprovalRequest>
    findTopByEmployeeIdAndStatusAndLoginTimeBetweenOrderByLoginTimeDesc(
            Integer employeeId,
            String status,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND REQUEST FOR EMPLOYEE + IP TODAY
    // =====================================================

    /**
     * Used for unknown-IP login attempts.
     *
     * Example:
     *
     * Employee:
     *     10
     *
     * IP:
     *     117.25.60.80
     *
     * TeamLogger checks whether an approval request for
     * this exact IP already exists today.
     */
    Optional<LoginApprovalRequest>
    findTopByEmployeeIdAndAttemptedIpAddressAndLoginTimeBetweenOrderByLoginTimeDesc(
            Integer employeeId,
            String attemptedIpAddress,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND REJECTED REQUESTS FOR EMPLOYEE TODAY
    // =====================================================

    /**
     * Used to enforce:
     *
     * ADMIN REJECTS
     *      ↓
     * Employee cannot login again today
     */
    List<LoginApprovalRequest>
    findByEmployeeIdAndStatusAndRejectedForDayTrueAndLoginTimeBetween(
            Integer employeeId,
            String status,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // FIND REJECTED REQUEST FOR EMPLOYEE + IP TODAY
    // =====================================================

    /**
     * Used when an employee attempts login from an unknown
     * IP that was already rejected today.
     */
    Optional<LoginApprovalRequest>
    findTopByEmployeeIdAndAttemptedIpAddressAndStatusAndRejectedForDayTrueAndLoginTimeBetweenOrderByLoginTimeDesc(
            Integer employeeId,
            String attemptedIpAddress,
            String status,
            LocalDateTime start,
            LocalDateTime end
    );


    // =====================================================
    // ADMIN - GET ALL REQUESTS
    // =====================================================

    /**
     * Useful later for the complete Login Approval History
     * page.
     */
    List<LoginApprovalRequest>
    findByAdminIdOrderByLoginTimeDesc(
            Integer adminId
    );


    // =====================================================
    // ADMIN - GET REQUESTS BY DATE
    // =====================================================

    /**
     * Useful later for filtering the admin approval panel
     * by a particular date.
     */
    List<LoginApprovalRequest>
    findByAdminIdAndLoginTimeBetweenOrderByLoginTimeDesc(
            Integer adminId,
            LocalDateTime start,
            LocalDateTime end
    );
}

