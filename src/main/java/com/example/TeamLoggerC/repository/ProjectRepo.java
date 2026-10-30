package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepo extends JpaRepository<Project, Integer> {

    // Projects created/owned by a particular admin
    List<Project> findByAdminId(Integer adminId);

    // Projects led by a particular employee
    List<Project> findByProjectLeaderId(Integer projectLeaderId);

    // Projects having a particular status
    List<Project> findByStatus(String status);

    // Projects led by an employee with a particular status
    List<Project> findByProjectLeaderIdAndStatus(
            Integer projectLeaderId,
            String status
    );

    // Projects created by an admin with a particular status
    List<Project> findByAdminIdAndStatus(
            Integer adminId,
            String status
    );


}