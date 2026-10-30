package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.AccessPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccessPolicyRepo
        extends JpaRepository<AccessPolicy, Long> {

    // =========================================================
    // FIND POLICIES BY ADMIN
    // =========================================================

    List<AccessPolicy>
    findByAdminIdOrderByCreatedAtDesc(Integer adminId);


    // =========================================================
    // FIND ACTIVE POLICIES BY ADMIN
    // =========================================================

    List<AccessPolicy>
    findByAdminIdAndStatusOrderByCreatedAtDesc(
            Integer adminId,
            String status
    );


    // =========================================================
    // FIND ONE POLICY
    // =========================================================

    Optional<AccessPolicy>
    findByIdAndAdminId(
            Long id,
            Integer adminId
    );


    // =========================================================
    // FIND POLICY BY NAME
    // =========================================================

    Optional<AccessPolicy>
    findByAdminIdAndName(
            Integer adminId,
            String name
    );


    // =========================================================
    // CHECK DUPLICATE POLICY NAME
    // =========================================================

    boolean existsByAdminIdAndName(
            Integer adminId,
            String name
    );


    // =========================================================
    // CHECK DUPLICATE POLICY NAME DURING UPDATE
    // =========================================================

    boolean existsByAdminIdAndNameAndIdNot(
            Integer adminId,
            String name,
            Long id
    );


    // =========================================================
    // COUNT POLICIES
    // =========================================================

    long countByAdminId(Integer adminId);


    // =========================================================
    // COUNT ACTIVE POLICIES
    // =========================================================

    long countByAdminIdAndStatus(
            Integer adminId,
            String status
    );

    List<AccessPolicy>
    findByAdminIdAndTeamIdOrderByCreatedAtDesc(
            Integer adminId,
            Integer teamId
    );

    Optional<AccessPolicy>
    findByIdAndAdminIdAndTeamId(
            Long id,
            Integer adminId,
            Integer teamId
    );

    boolean existsByAdminIdAndTeamIdAndName(
            Integer adminId,
            Integer teamId,
            String name
    );
}