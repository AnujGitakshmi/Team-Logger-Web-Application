package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "teams",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_team_admin_name",
                        columnNames = {"admin_id", "team_name"}
                )
        }
)
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // ==========================================
    // TEAM NAME
    // ==========================================

    @Column(name = "team_name", nullable = false)
    private String teamName;


    // ==========================================
    // ORGANIZATION / ADMIN OWNER
    // ==========================================

    /*
     * Every team belongs to one ADMIN.
     *
     * The ADMIN represents the organization.
     *
     * Example:
     *
     * Admin 1 -> Team A
     * Admin 1 -> Team B
     *
     * Admin 2 -> Team C
     *
     * Admin 1 must never be able to access
     * Admin 2's teams.
     */
    @Column(name = "admin_id", nullable = false)
    private Integer adminId;


    // ==========================================
    // CONSTRUCTOR
    // ==========================================

    public Team() {
    }


    // ==========================================
    // GETTERS AND SETTERS
    // ==========================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }


    public Integer getAdminId() {
        return adminId;
    }

    public void setAdminId(Integer adminId) {
        this.adminId = adminId;
    }
}

