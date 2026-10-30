package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.ApplicationPermission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationPermissionRepo
        extends JpaRepository<ApplicationPermission, Long> {

    /*
     * Get all application permissions
     * belonging to a specific access policy.
     */
    List<ApplicationPermission>
    findByPolicyIdOrderByApplicationNameAsc(
            Long policyId
    );


    /*
     * Get only ACTIVE application permissions
     * for a specific access policy.
     */
    List<ApplicationPermission>
    findByPolicyIdAndStatusOrderByApplicationNameAsc(
            Long policyId,
            String status
    );


    /*
     * Find a specific application permission
     * inside a specific policy.
     */
    Optional<ApplicationPermission>
    findByIdAndPolicyId(
            Long id,
            Long policyId
    );


    /*
     * Find an application by name inside a policy.
     *
     * This helps prevent duplicate applications
     * inside the same access policy.
     */
    Optional<ApplicationPermission>
    findByPolicyIdAndApplicationName(
            Long policyId,
            String applicationName
    );


    /*
     * Check whether an application already exists
     * inside a policy.
     */
    boolean existsByPolicyIdAndApplicationName(
            Long policyId,
            String applicationName
    );


    /*
     * Check duplicate application name while editing.
     *
     * The current permission ID is excluded.
     */
    boolean existsByPolicyIdAndApplicationNameAndIdNot(
            Long policyId,
            String applicationName,
            Long id
    );


    /*
     * Count all applications assigned to a policy.
     */
    long countByPolicyId(
            Long policyId
    );


    /*
     * Count ACTIVE applications assigned to a policy.
     */
    long countByPolicyIdAndStatus(
            Long policyId,
            String status
    );


    /*
     * Delete all application permissions
     * belonging to a policy.
     *
     * Useful when deleting an AccessPolicy.
     */
    void deleteByPolicyId(
            Long policyId
    );
}