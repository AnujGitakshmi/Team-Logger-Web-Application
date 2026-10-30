package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.HierarchyPermission;
import com.example.TeamLoggerC.security.rbac.EmployeeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HierarchyPermissionRepo
        extends JpaRepository<HierarchyPermission, Integer> {

    List<HierarchyPermission> findByHierarchyLevel(
            EmployeeLevel hierarchyLevel
    );

    boolean existsByHierarchyLevelAndPermission_Name(
            EmployeeLevel hierarchyLevel,
            String permissionName
    );
}

