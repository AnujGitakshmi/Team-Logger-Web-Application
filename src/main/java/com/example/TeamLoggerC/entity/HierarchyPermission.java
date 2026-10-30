package com.example.TeamLoggerC.entity;

import com.example.TeamLoggerC.security.rbac.EmployeeLevel;
import jakarta.persistence.*;

@Entity
@Table(
        name = "hierarchy_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_hierarchy_permission",
                        columnNames = {"hierarchy_level", "permission_id"}
                )
        }
)
public class HierarchyPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "hierarchy_level",
            nullable = false,
            length = 50
    )
    private EmployeeLevel hierarchyLevel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "permission_id",
            nullable = false,
            foreignKey = @ForeignKey(
                    name = "fk_hierarchy_permission_permission"
            )
    )
    private Permission permission;

    public HierarchyPermission() {
    }

    public HierarchyPermission(
            EmployeeLevel hierarchyLevel,
            Permission permission
    ) {
        this.hierarchyLevel = hierarchyLevel;
        this.permission = permission;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public EmployeeLevel getHierarchyLevel() {
        return hierarchyLevel;
    }

    public void setHierarchyLevel(EmployeeLevel hierarchyLevel) {
        this.hierarchyLevel = hierarchyLevel;
    }

    public Permission getPermission() {
        return permission;
    }

    public void setPermission(Permission permission) {
        this.permission = permission;
    }
}

