package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.EmployeeProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeProjectRepo
        extends JpaRepository<EmployeeProject, Integer> {

    // All project assignments of an employee
    List<EmployeeProject> findByEmployeeId(Integer employeeId);

    // Only currently assigned projects
    List<EmployeeProject> findByEmployeeIdAndUnassignedDateIsNull(
            Integer employeeId
    );

    // All employees assigned to a project
    List<EmployeeProject> findByProjectId(Integer projectId);

    // Check whether an employee is assigned to a project
    boolean existsByEmployeeIdAndProjectIdAndUnassignedDateIsNull(
            Integer employeeId,
            Integer projectId
    );
}