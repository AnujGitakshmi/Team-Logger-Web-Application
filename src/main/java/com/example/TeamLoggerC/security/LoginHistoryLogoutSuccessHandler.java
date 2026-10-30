package com.example.TeamLoggerC.security;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.service.LoginHistoryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginHistoryLogoutSuccessHandler
        implements LogoutSuccessHandler {

    private final LoginHistoryService loginHistoryService;

    public LoginHistoryLogoutSuccessHandler(
            LoginHistoryService loginHistoryService
    ) {
        this.loginHistoryService =
                loginHistoryService;
    }

    @Override
    public void onLogoutSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        // =====================================================
        // RECORD LOGOUT
        // =====================================================

        if (authentication != null
                && authentication.getPrincipal()
                instanceof CustomUserDetails userDetails) {

            User user =
                    userDetails.getUser();

            loginHistoryService.recordLogout(
                    user,
                    request
            );
        }

        // =====================================================
        // REDIRECT TO LOGIN
        // =====================================================

        response.sendRedirect(
                "/login?logout"
        );
    }
}

