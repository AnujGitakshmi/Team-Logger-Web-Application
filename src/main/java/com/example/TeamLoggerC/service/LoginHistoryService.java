package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.LoginHistory;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.LoginHistoryRepository;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoginHistoryService {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryService(
            LoginHistoryRepository loginHistoryRepository
    ) {
        this.loginHistoryRepository =
                loginHistoryRepository;
    }

    /**
     * Records a successful login.
     */
    @Transactional
    public void recordSuccessfulLogin(
            User user,
            HttpServletRequest request
    ) {

        if (user == null) {
            return;
        }

        LoginHistory history =
                new LoginHistory();

        history.setUserId(user.getId());

        history.setEmail(
                user.getEmail()
        );

        history.setEventType(
                "LOGIN"
        );

        history.setStatus(
                "SUCCESS"
        );

        history.setLoginTime(
                LocalDateTime.now()
        );

        history.setIpAddress(
                getClientIpAddress(request)
        );

        history.setUserAgent(
                request.getHeader("User-Agent")
        );

        history.setDeviceInfo(
                extractDeviceInfo(
                        request.getHeader("User-Agent")
                )
        );

        loginHistoryRepository.save(history);
    }

    /**
     * Records a failed login attempt.
     */
    @Transactional
    public void recordFailedLogin(
            User user,
            String email,
            String failureReason,
            HttpServletRequest request
    ) {

        LoginHistory history =
                new LoginHistory();

        /*
         * User can be null when the submitted
         * email does not exist.
         */
        if (user != null) {

            history.setUserId(
                    user.getId()
            );
        }

        history.setEmail(
                email
        );

        history.setEventType(
                "LOGIN"
        );

        history.setStatus(
                "FAILED"
        );

        history.setFailureReason(
                failureReason
        );

        history.setLoginTime(
                LocalDateTime.now()
        );

        history.setIpAddress(
                getClientIpAddress(request)
        );

        history.setUserAgent(
                request.getHeader("User-Agent")
        );

        history.setDeviceInfo(
                extractDeviceInfo(
                        request.getHeader("User-Agent")
                )
        );

        loginHistoryRepository.save(history);
    }

    /**
     * Records an account-lock event.
     */
    @Transactional
    public void recordAccountLocked(
            User user,
            HttpServletRequest request
    ) {

        if (user == null) {
            return;
        }

        LoginHistory history =
                new LoginHistory();

        history.setUserId(
                user.getId()
        );

        history.setEmail(
                user.getEmail()
        );

        history.setEventType(
                "LOGIN"
        );

        history.setStatus(
                "LOCKED"
        );

        history.setFailureReason(
                "MAX_FAILED_ATTEMPTS"
        );

        history.setLoginTime(
                LocalDateTime.now()
        );

        history.setIpAddress(
                getClientIpAddress(request)
        );

        history.setUserAgent(
                request.getHeader("User-Agent")
        );

        history.setDeviceInfo(
                extractDeviceInfo(
                        request.getHeader("User-Agent")
                )
        );

        loginHistoryRepository.save(history);
    }

    /**
     * Records a logout event.
     */
    @Transactional
    public void recordLogout(
            User user,
            HttpServletRequest request
    ) {

        if (user == null) {
            return;
        }

        LoginHistory history =
                new LoginHistory();

        history.setUserId(
                user.getId()
        );

        history.setEmail(
                user.getEmail()
        );

        history.setEventType(
                "LOGOUT"
        );

        history.setStatus(
                "SUCCESS"
        );

        history.setLogoutTime(
                LocalDateTime.now()
        );

        history.setIpAddress(
                getClientIpAddress(request)
        );

        history.setUserAgent(
                request.getHeader("User-Agent")
        );

        history.setDeviceInfo(
                extractDeviceInfo(
                        request.getHeader("User-Agent")
                )
        );

        loginHistoryRepository.save(history);
    }

    /**
     * Get history for a specific user.
     */
    public List<LoginHistory> getUserHistory(
            Integer userId
    ) {

        return loginHistoryRepository
                .findByUserIdOrderByLoginTimeDesc(
                        userId
                );
    }

    /**
     * Get history by email.
     */
    public List<LoginHistory> getEmailHistory(
            String email
    ) {

        return loginHistoryRepository
                .findByEmailOrderByLoginTimeDesc(
                        email
                );
    }

    /**
     * Get the latest 50 login events.
     */
    public List<LoginHistory> getRecentHistory() {

        return loginHistoryRepository
                .findTop50ByOrderByLoginTimeDesc();
    }

    /**
     * Get the real client IP address.
     *
     * X-Forwarded-For is checked first because
     * the application may later run behind a proxy.
     */
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

    /**
     * Stores basic browser/device information.
     *
     * This is intentionally kept simple for now.
     * We can make device detection more detailed later.
     */
    private String extractDeviceInfo(
            String userAgent
    ) {

        if (userAgent == null
                || userAgent.isBlank()) {

            return "UNKNOWN";
        }

        String ua =
                userAgent.toLowerCase();

        if (ua.contains("mobile")
                || ua.contains("android")
                || ua.contains("iphone")) {

            return "MOBILE";
        }

        if (ua.contains("tablet")
                || ua.contains("ipad")) {

            return "TABLET";
        }

        return "DESKTOP";
    }

    /**
     * Returns login activity for normal Admin accounts
     * belonging to the specified organization.
     */
    @Transactional(readOnly = true)
    public List<LoginHistory> getAdminActivityByOrganization(
            Integer organizationId) {

        if (organizationId == null) {
            return List.of();
        }

        return loginHistoryRepository
                .findAdminActivityByOrganization(organizationId)
                .stream()
                .limit(50)
                .toList();
    }

}

