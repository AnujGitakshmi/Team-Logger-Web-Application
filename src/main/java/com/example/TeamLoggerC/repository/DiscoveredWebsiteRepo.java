 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.DiscoveredWebsite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscoveredWebsiteRepo
        extends JpaRepository<DiscoveredWebsite, Long> {


    // =========================================================
    // ALL WEBSITES FOR ADMIN
    // =========================================================

    List<DiscoveredWebsite>
    findByAdminIdOrderByDomainAsc(
            Integer adminId
    );


    // =========================================================
    // ACTIVE WEBSITES FOR ADMIN
    // =========================================================

    List<DiscoveredWebsite>
    findByAdminIdAndStatusOrderByDomainAsc(
            Integer adminId,
            String status
    );


    // =========================================================
    // FIND WEBSITE WITH ADMIN OWNERSHIP
    // =========================================================

    Optional<DiscoveredWebsite>
    findByIdAndAdminId(
            Long id,
            Integer adminId
    );


    // =========================================================
    // FIND BY DOMAIN
    // =========================================================

    Optional<DiscoveredWebsite>
    findByAdminIdAndDomain(
            Integer adminId,
            String domain
    );


    // =========================================================
    // CHECK EXISTENCE
    // =========================================================

    boolean existsByAdminIdAndDomain(
            Integer adminId,
            String domain
    );


    // =========================================================
    // SEARCH
    // =========================================================

    List<DiscoveredWebsite>
    findByAdminIdAndDomainContainingIgnoreCaseOrderByDomainAsc(
            Integer adminId,
            String domain
    );


    // =========================================================
    // COUNTS
    // =========================================================

    long countByAdminId(
            Integer adminId
    );


    long countByAdminIdAndStatus(
            Integer adminId,
            String status
    );


    // =========================================================
    // DELETE ALL FOR ADMIN
    // =========================================================

    void deleteByAdminId(
            Integer adminId
    );
}
