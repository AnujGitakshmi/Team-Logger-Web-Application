package com.example.TeamLoggerC.security;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.AccountLockoutService;
import com.example.TeamLoggerC.service.LoginHistoryService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class LoginFailureHandler
        implements AuthenticationFailureHandler {

    private final UserRepo userRepo;
    private final AccountLockoutService accountLockoutService;
    private final LoginHistoryService loginHistoryService;

    public LoginFailureHandler(
            UserRepo userRepo,
            AccountLockoutService accountLockoutService,
            LoginHistoryService loginHistoryService
    ) {
        this.userRepo = userRepo;
        this.accountLockoutService = accountLockoutService;
        this.loginHistoryService = loginHistoryService;
    }


    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {


        // =====================================================
        // GET LOGIN EMAIL
        // =====================================================

        String email = request.getParameter("email");


        if (email == null || email.trim().isEmpty()) {

            response.sendRedirect("/login?error");

            return;
        }


        email = email.trim().toLowerCase(java.util.Locale.ROOT);


        // =====================================================
        // FIND USER
        // =====================================================

        User user = userRepo.findByEmail(email)
                .orElse(null);


        /*
         * If no account exists with this email,
         * do not reveal that information.
         *
         * Simply record a generic failed login event.
         */

        if (user == null) {

            loginHistoryService.recordFailedLogin(
                    null,
                    email,
                    "USER_NOT_FOUND",
                    request
            );

            response.sendRedirect("/login?error");

            return;
        }


        // =====================================================
        // ALREADY LOCKED
        // =====================================================

        if (accountLockoutService.isLocked(user)) {

            loginHistoryService.recordFailedLogin(
                    user,
                    email,
                    "ACCOUNT_LOCKED",
                    request
            );

            response.sendRedirect(
                    "/login?error=locked"
            );

            return;
        }


        // =====================================================
        // COUNT ONLY BAD CREDENTIALS
        // =====================================================

        /*
         * We only count an authentication failure as a
         * failed password attempt when Spring Security
         * reports BadCredentialsException.
         *
         * Other authentication errors should not increase
         * the password failure counter.
         */

        if (exception instanceof BadCredentialsException) {


            // =================================================
            // RECORD FAILED LOGIN
            // =================================================

            loginHistoryService.recordFailedLogin(
                    user,
                    email,
                    "BAD_CREDENTIALS",
                    request
            );


            // =================================================
            // INCREMENT FAILED ATTEMPT COUNTER
            // =================================================

            accountLockoutService.recordFailedLogin(user);


            // =================================================
            // CHECK WHETHER THIS ATTEMPT CAUSED LOCKOUT
            // =================================================

            if (accountLockoutService.isLocked(user)) {


                // =============================================
                // RECORD LOCK EVENT
                // =============================================

                loginHistoryService.recordAccountLocked(
                        user,
                        request
                );


                response.sendRedirect(
                        "/login?error=locked"
                );

                return;
            }
        }


        // =====================================================
        // OTHER AUTHENTICATION FAILURE
        // =====================================================

        /*
         * Authentication failed for a reason other than
         * BadCredentialsException.
         *
         * We do not increase the failed password counter.
         */

        if (!(exception instanceof BadCredentialsException)) {

            loginHistoryService.recordFailedLogin(
                    user,
                    email,
                    exception.getClass().getSimpleName(),
                    request
            );
        }


        // =====================================================
        // NORMAL LOGIN FAILURE
        // =====================================================

        response.sendRedirect(
                "/login?error"
        );
    }
}

