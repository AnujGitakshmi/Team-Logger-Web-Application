
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepo extends JpaRepository<User, Integer> {

    // =====================================================
    // FIND BY EMAIL
    // =====================================================
    Optional<User> findByEmail(String email);

    // Count users by role
    long countByRoleIgnoreCase(String role);

    // =====================================================
    // FIND BY EMPLOYEE LOGIN ID
    // =====================================================
    Optional<User> findByEmployeeLoginId(String employeeLoginId);

    // =====================================================
    // GET LATEST USER
    // =====================================================
    Optional<User> findTopByOrderByIdDesc();

    // =====================================================
    // GET EMPLOYEES OF ADMIN
    // =====================================================
    List<User> findByAdminIdAndRole(Integer adminId, String role);

    // =====================================================
    // GET ALL USERS OF ADMIN
    // =====================================================
    List<User> findByAdminId(Integer adminId);

    // =====================================================
    // COUNT EMPLOYEES
    // =====================================================
    long countByAdminIdAndRole(Integer adminId, String role);

    // =====================================================
    // COUNT EMPLOYEES BY STATUS
    // =====================================================
    long countByAdminIdAndRoleAndStatus(
            Integer adminId,
            String role,
            String status
    );

    // =====================================================
    // FIND EMPLOYEES BY DESIGNATION
    // =====================================================
    List<User> findByAdminIdAndRoleAndDesignationContainingIgnoreCase(
            Integer adminId,
            String role,
            String designation
    );

    // =====================================================
    // FIND EMPLOYEES BY STATUS
    // =====================================================
    List<User> findByAdminIdAndRoleAndStatus(
            Integer adminId,
            String role,
            String status
    );

    // =====================================================
    // FIND EMPLOYEES BY TEAM
    // =====================================================
    List<User> findByAdminIdAndRoleAndTeamId(
            Integer adminId,
            String role,
            Integer teamId
    );

    // =====================================================
    // SEARCH EMPLOYEES
    // =====================================================
    List<User> findByAdminIdAndRoleAndNameContainingIgnoreCase(
            Integer adminId,
            String role,
            String name
    );

    // =====================================================
    // FIND EMPLOYEES BY DOMAIN
    // =====================================================
    List<User> findByAdminIdAndRoleAndDomainIgnoreCase(
            Integer adminId,
            String role,
            String domain
    );

    // =====================================================
    // FIND EMPLOYEES BY DOMAIN + DESIGNATION
    // =====================================================
    List<User> findByAdminIdAndRoleAndDomainIgnoreCaseAndDesignationIgnoreCase(
            Integer adminId,
            String role,
            String domain,
            String designation
    );

    // =====================================================
    // FIND DIRECT REPORTS
    // =====================================================
    List<User> findBySeniorId(Integer seniorId);

    long countBySeniorId(Integer seniorId);

    List<User>
    findByAdminIdAndRoleAndDomainIgnoreCaseAndDesignationIgnoreCaseAndTeamId(
            Integer adminId,
            String role,
            String domain,
            String designation,
            Integer teamId
    );

    Optional<User>
    findFirstByAdminIdAndRoleAndDomainIgnoreCaseAndDesignationIgnoreCaseAndTeamId(
            Integer adminId,
            String role,
            String domain,
            String designation,
            Integer teamId
    );

    // =====================================================
    // SUPER ADMIN: ORGANIZATION-WIDE METRICS
    // =====================================================

    // Count all users belonging to an organization
    long countByOrganizationId(Integer organizationId);

    // Count users by role within an organization
    long countByOrganizationIdAndRoleIgnoreCase(
            Integer organizationId,
            String role
    );

    // Count users by status within an organization
    long countByOrganizationIdAndStatusIgnoreCase(
            Integer organizationId,
            String status
    );

    // Count users by organization role
    long countByOrganizationIdAndOrganizationRoleIgnoreCase(
            Integer organizationId,
            String organizationRole
    );

    // Get branch administrators in an organization
    List<User>
    findByOrganizationIdAndOrganizationRoleIgnoreCaseOrderByNameAsc(
            Integer organizationId,
            String organizationRole
    );
}
