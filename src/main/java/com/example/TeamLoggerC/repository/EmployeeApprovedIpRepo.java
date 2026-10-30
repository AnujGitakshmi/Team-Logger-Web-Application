package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.EmployeeApprovedIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeApprovedIpRepo
        extends JpaRepository<EmployeeApprovedIp, Integer> {


    // =========================================================
    // FIND EMPLOYEE IP
    // =========================================================

    Optional<EmployeeApprovedIp>
    findByEmployeeIdAndIpAddress(
            Integer employeeId,
            String ipAddress
    );


    // =========================================================
    // FIND ENABLED EMPLOYEE IP
    // =========================================================

    Optional<EmployeeApprovedIp>
    findByEmployeeIdAndIpAddressAndEnabledTrue(
            Integer employeeId,
            String ipAddress
    );


    // =========================================================
    // GET ALL IPs OF EMPLOYEE
    // =========================================================

    List<EmployeeApprovedIp>
    findByEmployeeId(
            Integer employeeId
    );


    // =========================================================
    // GET ENABLED IPs OF EMPLOYEE
    // =========================================================

    List<EmployeeApprovedIp>
    findByEmployeeIdAndEnabledTrue(
            Integer employeeId
    );


    // =========================================================
    // GET IPs OF ADMIN / ORGANIZATION
    // =========================================================

    List<EmployeeApprovedIp>
    findByAdminId(
            Integer adminId
    );


    // =========================================================
    // CHECK EXISTING IP
    // =========================================================

    boolean existsByEmployeeIdAndIpAddress(
            Integer employeeId,
            String ipAddress
    );


    // =========================================================
    // CHECK ACTIVE IP
    // =========================================================

    boolean existsByEmployeeIdAndIpAddressAndEnabledTrue(
            Integer employeeId,
            String ipAddress
    );
}

