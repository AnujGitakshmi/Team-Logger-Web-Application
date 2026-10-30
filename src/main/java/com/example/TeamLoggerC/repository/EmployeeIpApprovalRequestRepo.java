 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.EmployeeIpApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EmployeeIpApprovalRequestRepo
        extends JpaRepository<EmployeeIpApprovalRequest, Integer> {


    // =========================================================
    // FIND REQUEST BY EMPLOYEE + ADMIN + IP
    // =========================================================

    Optional<EmployeeIpApprovalRequest>
    findByEmployeeIdAndAdminIdAndIpAddress(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    );


    // =========================================================
    // FIND PENDING REQUEST
    // =========================================================

    Optional<EmployeeIpApprovalRequest>
    findByEmployeeIdAndAdminIdAndIpAddressAndStatus(
            Integer employeeId,
            Integer adminId,
            String ipAddress,
            String status
    );


    // =========================================================
    // GET ALL REQUESTS OF AN EMPLOYEE
    // =========================================================

    List<EmployeeIpApprovalRequest>
    findByEmployeeIdAndAdminIdOrderByRequestedAtDesc(
            Integer employeeId,
            Integer adminId
    );


    // =========================================================
    // GET ADMIN REQUESTS
    // =========================================================

    List<EmployeeIpApprovalRequest>
    findByAdminIdOrderByRequestedAtDesc(
            Integer adminId
    );


    // =========================================================
    // GET ADMIN PENDING REQUESTS
    // =========================================================

    List<EmployeeIpApprovalRequest>
    findByAdminIdAndStatusOrderByRequestedAtDesc(
            Integer adminId,
            String status
    );


    // =========================================================
    // FIND REQUEST BY ID + ADMIN
    // =========================================================

    Optional<EmployeeIpApprovalRequest>
    findByIdAndAdminId(
            Integer id,
            Integer adminId
    );


    // =========================================================
    // FIND TODAY'S REQUEST
    // =========================================================

    List<EmployeeIpApprovalRequest>
    findByEmployeeIdAndAdminIdAndRequestedAtBetweenOrderByRequestedAtDesc(
            Integer employeeId,
            Integer adminId,
            LocalDateTime start,
            LocalDateTime end
    );


    // =========================================================
    // FIND TODAY'S REQUEST FOR SPECIFIC IP
    // =========================================================

    List<EmployeeIpApprovalRequest>
    findByEmployeeIdAndAdminIdAndIpAddressAndRequestedAtBetweenOrderByRequestedAtDesc(
            Integer employeeId,
            Integer adminId,
            String ipAddress,
            LocalDateTime start,
            LocalDateTime end
    );
}

