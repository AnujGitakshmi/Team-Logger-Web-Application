package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.WebsitePermission;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WebsitePermissionRepo
        extends JpaRepository<WebsitePermission, Long> {


    // =========================================================
    // GET ALL PERMISSIONS FOR POLICY
    // =========================================================

    List<WebsitePermission>
    findByPolicyIdOrderByDomainAsc(
            Long policyId
    );


    // =========================================================
    // GET ACTIVE PERMISSIONS FOR POLICY
    // =========================================================

    List<WebsitePermission>
    findByPolicyIdAndStatusOrderByDomainAsc(
            Long policyId,
            String status
    );


    // =========================================================
    // GET PERMISSION BY ID + POLICY
    // =========================================================

    Optional<WebsitePermission>
    findByIdAndPolicyId(
            Long id,
            Long policyId
    );


    // =========================================================
    // GET PERMISSION BY DOMAIN + POLICY
    // =========================================================

    Optional<WebsitePermission>
    findByPolicyIdAndDomain(
            Long policyId,
            String domain
    );


    // =========================================================
    // CHECK EXISTING DOMAIN
    // =========================================================

    boolean existsByPolicyIdAndDomain(
            Long policyId,
            String domain
    );


    // =========================================================
    // CHECK DUPLICATE DOMAIN DURING UPDATE
    // =========================================================

    boolean existsByPolicyIdAndDomainAndIdNot(
            Long policyId,
            String domain,
            Long id
    );


    // =========================================================
    // COUNT ALL PERMISSIONS
    // =========================================================

    long countByPolicyId(
            Long policyId
    );


    // =========================================================
    // COUNT ACTIVE PERMISSIONS
    // =========================================================

    long countByPolicyIdAndStatus(
            Long policyId,
            String status
    );


    // =========================================================
    // DELETE ALL PERMISSIONS FOR POLICY
    // =========================================================

    void deleteByPolicyId(
            Long policyId
    );
}