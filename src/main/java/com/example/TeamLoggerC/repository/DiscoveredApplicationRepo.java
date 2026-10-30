package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.DiscoveredApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DiscoveredApplicationRepo
        extends JpaRepository<DiscoveredApplication, Long> {


    // =========================================================
    // GET ALL APPLICATIONS FOR ADMIN
    // =========================================================

    List<DiscoveredApplication>
    findByAdminIdOrderByApplicationNameAsc(
            Integer adminId
    );


    // =========================================================
    // GET ACTIVE APPLICATIONS FOR ADMIN
    // =========================================================

    List<DiscoveredApplication>
    findByAdminIdAndStatusOrderByApplicationNameAsc(
            Integer adminId,
            String status
    );


    // =========================================================
    // FIND APPLICATION
    // =========================================================

    Optional<DiscoveredApplication>
    findByIdAndAdminId(
            Long id,
            Integer adminId
    );


    // =========================================================
    // FIND BY APPLICATION + EXECUTABLE
    // =========================================================

    Optional<DiscoveredApplication>
    findByAdminIdAndApplicationNameAndExecutableName(
            Integer adminId,
            String applicationName,
            String executableName
    );


    // =========================================================
    // CHECK DUPLICATE
    // =========================================================

    boolean existsByAdminIdAndApplicationNameAndExecutableName(
            Integer adminId,
            String applicationName,
            String executableName
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
    // SEARCH
    // =========================================================

    List<DiscoveredApplication>
    findByAdminIdAndApplicationNameContainingIgnoreCaseOrderByApplicationNameAsc(
            Integer adminId,
            String applicationName
    );


    // =========================================================
    // DELETE ALL ADMIN APPLICATIONS
    // =========================================================

    void deleteByAdminId(
            Integer adminId
    );
}
