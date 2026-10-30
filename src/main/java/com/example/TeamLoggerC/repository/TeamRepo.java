package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepo extends JpaRepository<Team, Integer> {


    // ==========================================
    // FIND TEAM BY NAME
    // ==========================================

    /*
     * Organization-specific team lookup.
     *
     * Two different organizations can have
     * the same team name.
     */
    Optional<Team> findByAdminIdAndTeamName(
            Integer adminId,
            String teamName
    );


    // ==========================================
    // FIND TEAM BY ID + ADMIN
    // ==========================================

    /*
     * Used to verify that a team belongs
     * to the currently logged-in ADMIN.
     */
    Optional<Team> findByIdAndAdminId(
            Integer id,
            Integer adminId
    );


    // ==========================================
    // FIND ALL TEAMS OF ADMIN
    // ==========================================

    /*
     * Returns ONLY teams belonging to the
     * specified organization/admin.
     */
    List<Team> findAllByAdminIdOrderByTeamNameAsc(
            Integer adminId
    );
}

