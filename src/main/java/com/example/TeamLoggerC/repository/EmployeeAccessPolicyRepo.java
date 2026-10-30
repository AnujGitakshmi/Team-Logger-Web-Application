package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.EmployeeAccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeAccessPolicyRepo
        extends JpaRepository<EmployeeAccessPolicy, Long> {

    Optional<EmployeeAccessPolicy>
    findByEmployeeId(Integer employeeId);


    Optional<EmployeeAccessPolicy>
    findByEmployeeIdAndStatus(
            Integer employeeId,
            String status
    );


    Optional<EmployeeAccessPolicy>
    findByEmployeeIdAndPolicyId(
            Integer employeeId,
            Long policyId
    );


    Optional<EmployeeAccessPolicy>
    findByEmployeeIdAndAssignedBy(
            Integer employeeId,
            Integer assignedBy
    );


    Optional<EmployeeAccessPolicy>
    findByEmployeeIdAndPolicyIdAndStatus(
            Integer employeeId,
            Long policyId,
            String status
    );


    boolean existsByEmployeeId(
            Integer employeeId
    );


    boolean existsByEmployeeIdAndStatus(
            Integer employeeId,
            String status
    );


    List<EmployeeAccessPolicy>
    findByPolicyId(
            Long policyId
    );


    List<EmployeeAccessPolicy>
    findByPolicyIdAndStatus(
            Long policyId,
            String status
    );


    long countByPolicyId(
            Long policyId
    );


    long countByPolicyIdAndStatus(
            Long policyId,
            String status
    );


    void deleteByEmployeeId(
            Integer employeeId
    );


    void deleteByPolicyId(
            Long policyId
    );
}