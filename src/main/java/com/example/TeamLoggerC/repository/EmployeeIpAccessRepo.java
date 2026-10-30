package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.EmployeeIpAccess;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeIpAccessRepo
        extends JpaRepository<EmployeeIpAccess, Long> {


    // =====================================================
    // GET ALL IP ACCESS RECORDS OF AN EMPLOYEE
    // =====================================================

    List<EmployeeIpAccess>
    findByEmployeeIdOrderByCreatedAtAsc(
            Integer employeeId
    );


    // =====================================================
    // GET ALL ACTIVE / APPROVED IPs OF AN EMPLOYEE
    // =====================================================

    List<EmployeeIpAccess>
    findByEmployeeIdAndStatusOrderByCreatedAtAsc(
            Integer employeeId,
            String status
    );


    // =====================================================
    // FIND OFFICE IP
    // =====================================================

    Optional<EmployeeIpAccess>
    findByEmployeeIdAndAccessType(
            Integer employeeId,
            String accessType
    );


    // =====================================================
    // FIND OFFICE/SECONDARY IP BY STATUS
    // =====================================================

    Optional<EmployeeIpAccess>
    findByEmployeeIdAndAccessTypeAndStatus(
            Integer employeeId,
            String accessType,
            String status
    );


    // =====================================================
    // FIND A SPECIFIC IP FOR AN EMPLOYEE
    // =====================================================

    Optional<EmployeeIpAccess>
    findByEmployeeIdAndIpAddress(
            Integer employeeId,
            String ipAddress
    );


    // =====================================================
    // CHECK WHETHER AN IP IS ALREADY REGISTERED
    // =====================================================

    boolean existsByEmployeeIdAndIpAddress(
            Integer employeeId,
            String ipAddress
    );


    // =====================================================
    // CHECK WHETHER AN APPROVED IP EXISTS
    // =====================================================

    boolean existsByEmployeeIdAndIpAddressAndStatus(
            Integer employeeId,
            String ipAddress,
            String status
    );


    // =====================================================
    // COUNT IP ACCESS RECORDS
    // =====================================================

    long countByEmployeeId(
            Integer employeeId
    );


    // =====================================================
    // COUNT APPROVED IPs
    // =====================================================

    long countByEmployeeIdAndStatus(
            Integer employeeId,
            String status
    );

    // COUNT APPROVED IPs FOR AN EMPLOYEE + ADMIN
    long countByEmployeeIdAndAdminIdAndStatus(
            Integer employeeId,
            Integer adminId,
            String status
    );


    // =====================================================
    // GET PENDING REQUESTS FOR AN ADMIN
    // =====================================================

    List<EmployeeIpAccess>
    findByAdminIdAndStatusOrderByCreatedAtDesc(
            Integer adminId,
            String status
    );


    // =====================================================
    // GET ALL ACCESS RECORDS OF AN ADMIN
    // =====================================================

    List<EmployeeIpAccess>
    findByAdminIdOrderByCreatedAtDesc(
            Integer adminId
    );


    // =====================================================
    // GET ALL ACCESS RECORDS FOR AN EMPLOYEE + ADMIN
    // =====================================================

    List<EmployeeIpAccess>
    findByEmployeeIdAndAdminIdOrderByCreatedAtAsc(
            Integer employeeId,
            Integer adminId
    );


    // =====================================================
    // FIND IP BY EMPLOYEE + ADMIN
    // =====================================================

    Optional<EmployeeIpAccess>
    findByEmployeeIdAndAdminIdAndIpAddress(
            Integer employeeId,
            Integer adminId,
            String ipAddress
    );


    // =====================================================
    // CHECK APPROVED IP FOR EMPLOYEE + ADMIN
    // =====================================================

    boolean existsByEmployeeIdAndAdminIdAndIpAddressAndStatus(
            Integer employeeId,
            Integer adminId,
            String ipAddress,
            String status
    );

      // =====================================================
      // FIND THE CURRENT APPROVED SECONDARY IP
     // =====================================================
    Optional<EmployeeIpAccess>
    findByEmployeeIdAndAdminIdAndAccessTypeAndStatus(
            Integer employeeId,
            Integer adminId,
            String accessType,
            String status
    );

}

