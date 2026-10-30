 package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepo
        extends JpaRepository<AuditLog, Long> {

    // =====================================================
    // USER HISTORY
    // =====================================================

    List<AuditLog> findByUserIdOrderByCreatedAtDesc(
            Integer userId
    );


    // =====================================================
    // ACTION HISTORY
    // =====================================================

    List<AuditLog> findByActionOrderByCreatedAtDesc(
            String action
    );


    // =====================================================
    // ENTITY HISTORY
    // =====================================================

    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType,
            String entityId
    );


    // =====================================================
    // RECENT AUDIT LOGS
    // =====================================================

    List<AuditLog> findTop100ByOrderByCreatedAtDesc();
}

