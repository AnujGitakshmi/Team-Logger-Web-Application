
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.LoginHistory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoginHistoryRepository
        extends JpaRepository<LoginHistory, Long> {

    List<LoginHistory> findByUserIdOrderByLoginTimeDesc(Integer userId);

    List<LoginHistory> findByEmailOrderByLoginTimeDesc(String email);

    List<LoginHistory> findTop50ByOrderByLoginTimeDesc();

    @Query("""
        SELECT h
        FROM LoginHistory h, User u
        WHERE h.userId = u.id
          AND u.organizationId = :organizationId
          AND UPPER(u.role) = 'ADMIN'
          AND UPPER(u.organizationRole) IN ('ORG_ADMIN', 'BRANCH_ADMIN')
        ORDER BY COALESCE(h.logoutTime, h.loginTime) DESC, h.id DESC
        """)
    List<LoginHistory> findAdminActivityByOrganization(
            @Param("organizationId") Integer organizationId
    );
}
