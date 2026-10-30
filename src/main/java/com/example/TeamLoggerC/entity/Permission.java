package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_name",
                        columnNames = "name"
                )
        }
)
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    // =====================================================
    // PERMISSION NAME
    // =====================================================

    /**
     * Unique permission identifier.
     *
     * Examples:
     *
     * EMPLOYEE_VIEW
     * EMPLOYEE_CREATE
     * PROJECT_VIEW
     * PROJECT_CREATE
     * TASK_CREATE
     */
    @Column(
            name = "name",
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;


    // =====================================================
    // DESCRIPTION
    // =====================================================

    /**
     * Human-readable explanation of the permission.
     */
    @Column(
            name = "description",
            length = 255
    )
    private String description;


    // =====================================================
    // CONSTRUCTORS
    // =====================================================

    public Permission() {
    }


    public Permission(
            String name,
            String description) {

        this.name = name;
        this.description = description;
    }


    // =====================================================
    // GETTERS / SETTERS
    // =====================================================

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

