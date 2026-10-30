 package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.AuditLog;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.AuditLogRepo;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepo auditLogRepo;

    public AuditLogService(AuditLogRepo auditLogRepo) {
        this.auditLogRepo = auditLogRepo;
    }


    // =====================================================
    // RECORD AUDIT LOG
    // =====================================================

    @Transactional
    public void record(
            User user,
            String action,
            String entityType,
            String entityId,
            String description,
            String status
    ) {

        AuditLog auditLog = new AuditLog();


        // -------------------------------------------------
        // USER INFORMATION
        // -------------------------------------------------

        if (user != null) {

            auditLog.setUserId(
                    user.getId()
            );

            auditLog.setUserEmail(
                    user.getEmail()
            );

            auditLog.setUserRole(
                    user.getRole()
            );
        }


        // -------------------------------------------------
        // ACTION INFORMATION
        // -------------------------------------------------

        auditLog.setAction(action);

        auditLog.setEntityType(entityType);

        auditLog.setEntityId(entityId);

        auditLog.setDescription(description);

        auditLog.setStatus(status);


        // -------------------------------------------------
        // CURRENT HTTP REQUEST
        // -------------------------------------------------

        HttpServletRequest request =
                getCurrentRequest();


        if (request != null) {

            auditLog.setIpAddress(
                    getClientIpAddress(request)
            );

            String userAgent =
                    request.getHeader("User-Agent");

            auditLog.setUserAgent(
                    userAgent
            );

            auditLog.setDeviceInfo(
                    extractDeviceInfo(userAgent)
            );
        }


        // -------------------------------------------------
        // TIMESTAMP
        // -------------------------------------------------

        auditLog.setCreatedAt(
                LocalDateTime.now()
        );


        // -------------------------------------------------
        // SAVE
        // -------------------------------------------------

        auditLogRepo.save(
                auditLog
        );
    }


    // =====================================================
    // USER HISTORY
    // =====================================================

    public List<AuditLog> getUserHistory(
            Integer userId
    ) {

        return auditLogRepo
                .findByUserIdOrderByCreatedAtDesc(
                        userId
                );
    }


    // =====================================================
    // ACTION HISTORY
    // =====================================================

    public List<AuditLog> getActionHistory(
            String action
    ) {

        return auditLogRepo
                .findByActionOrderByCreatedAtDesc(
                        action
                );
    }


    // =====================================================
    // ENTITY HISTORY
    // =====================================================

    public List<AuditLog> getEntityHistory(
            String entityType,
            String entityId
    ) {

        return auditLogRepo
                .findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
                        entityType,
                        entityId
                );
    }


    // =====================================================
    // RECENT AUDIT LOGS
    // =====================================================

    public List<AuditLog> getRecentLogs() {

        return auditLogRepo
                .findTop100ByOrderByCreatedAtDesc();
    }


    // =====================================================
    // CURRENT REQUEST
    // =====================================================

    private HttpServletRequest getCurrentRequest() {

        RequestAttributes attributes =
                RequestContextHolder.getRequestAttributes();


        if (!(attributes
                instanceof ServletRequestAttributes servletAttributes)) {

            return null;
        }


        return servletAttributes.getRequest();
    }


    // =====================================================
    // CLIENT IP
    // =====================================================

    private String getClientIpAddress(
            HttpServletRequest request
    ) {

        if (request == null) {
            return null;
        }


        String forwardedFor =
                request.getHeader("X-Forwarded-For");


        if (forwardedFor != null
                && !forwardedFor.isBlank()) {

            return forwardedFor
                    .split(",")[0]
                    .trim();
        }


        String realIp =
                request.getHeader("X-Real-IP");


        if (realIp != null
                && !realIp.isBlank()) {

            return realIp.trim();
        }


        return request.getRemoteAddr();
    }


    // =====================================================
    // DEVICE INFORMATION
    // =====================================================

    private String extractDeviceInfo(
            String userAgent
    ) {

        if (userAgent == null
                || userAgent.isBlank()) {

            return "UNKNOWN";
        }


        String ua =
                userAgent.toLowerCase();


        if (ua.contains("tablet")
                || ua.contains("ipad")) {

            return "TABLET";
        }


        if (ua.contains("mobile")
                || ua.contains("android")
                || ua.contains("iphone")) {

            return "MOBILE";
        }


        return "DESKTOP";
    }
}

