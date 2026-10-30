 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.DailyWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyWorkRepo
        extends JpaRepository<DailyWork, Integer> {


    // =====================================================
    // GET ALL WORK OF AN EMPLOYEE
    // =====================================================

    List<DailyWork> findByEmployeeIdOrderByWorkDateDescCreatedAtDesc(
            Integer employeeId
    );


    // =====================================================
    // GET WORK OF AN EMPLOYEE FOR A PROJECT
    // =====================================================

    List<DailyWork> findByEmployeeIdAndProjectIdOrderByWorkDateDescCreatedAtDesc(
            Integer employeeId,
            Integer projectId
    );


    // =====================================================
    // GET WORK OF AN EMPLOYEE ON A PARTICULAR DATE
    // =====================================================

    List<DailyWork> findByEmployeeIdAndWorkDateOrderByCreatedAtDesc(
            Integer employeeId,
            LocalDate workDate
    );


    // =====================================================
    // GET WORK FOR A PROJECT
    // =====================================================

    List<DailyWork> findByProjectIdOrderByWorkDateDescCreatedAtDesc(
            Integer projectId
    );


    // =====================================================
    // GET WORK FOR A PROJECT ON A PARTICULAR DATE
    // =====================================================

    List<DailyWork> findByProjectIdAndWorkDateOrderByCreatedAtDesc(
            Integer projectId,
            LocalDate workDate
    );


    // =====================================================
    // GET EMPLOYEE WORK FOR A PROJECT ON A DATE
    // =====================================================

    List<DailyWork>
    findByEmployeeIdAndProjectIdAndWorkDateOrderByCreatedAtDesc(
            Integer employeeId,
            Integer projectId,
            LocalDate workDate
    );


    // =====================================================
    // COUNT WORK ENTRIES OF AN EMPLOYEE
    // =====================================================

    long countByEmployeeId(Integer employeeId);


    // =====================================================
    // COUNT WORK ENTRIES FOR A PROJECT
    // =====================================================

    long countByProjectId(Integer projectId);


    // =====================================================
    // COUNT TODAY'S WORK OF AN EMPLOYEE
    // =====================================================

    long countByEmployeeIdAndWorkDate(
            Integer employeeId,
            LocalDate workDate
    );
}

