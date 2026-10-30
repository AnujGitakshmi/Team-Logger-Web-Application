
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.WebsiteActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WebsiteActivityRepository
        extends JpaRepository<WebsiteActivity, Long> {

    List<WebsiteActivity> findByAdminIdOrderByVisitedAtDesc(
            Long adminId
    );

    List<WebsiteActivity> findByEmployeeIdOrderByVisitedAtDesc(
            Long employeeId
    );

    List<WebsiteActivity> findTop100ByAdminIdOrderByVisitedAtDesc(
            Long adminId
    );

    List<WebsiteActivity> findTop100ByEmployeeIdOrderByVisitedAtDesc(
            Long employeeId
    );
}
