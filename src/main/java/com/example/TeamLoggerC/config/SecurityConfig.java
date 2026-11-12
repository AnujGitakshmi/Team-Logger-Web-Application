
package com.example.TeamLoggerC.config;

import com.example.TeamLoggerC.security.LoginFailureHandler;
import com.example.TeamLoggerC.security.LoginHistoryLogoutSuccessHandler;
import com.example.TeamLoggerC.security.LoginSuccessHandler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final LoginSuccessHandler loginSuccessHandler;
    private final LoginFailureHandler loginFailureHandler;
    private final LoginHistoryLogoutSuccessHandler logoutSuccessHandler;

    public SecurityConfig(
            LoginSuccessHandler loginSuccessHandler,
            LoginFailureHandler loginFailureHandler,
            LoginHistoryLogoutSuccessHandler logoutSuccessHandler) {

        this.loginSuccessHandler = loginSuccessHandler;
        this.loginFailureHandler = loginFailureHandler;
        this.logoutSuccessHandler = logoutSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            SecurityContextRepository securityContextRepository)
            throws Exception {

        http
                // Keep CSRF enabled everywhere except the exact
                // one-time desktop ticket redemption POST endpoint.
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(request ->
                                HttpMethod.POST.matches(request.getMethod())
                                        && "/api/desktop/redeem".equals(
                                        request.getServletPath()
                                )
                        )
                        .ignoringRequestMatchers(
                                "/api/extension/website-activity"
                        )
                )

                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )

                .authorizeHttpRequests(auth -> auth

                        // Public authentication pages.
                        .requestMatchers(
                                "/login",
                                "/signup",
                                "/organization/register"
                        ).permitAll()

                        // Existing login approval pages.
                        .requestMatchers(
                                "/employee/login-pending",
                                "/employee/login-approval-status"
                        ).permitAll()

                        // Desktop bootstrap page.
                        .requestMatchers(
                                HttpMethod.GET,
                                "/desktop/bootstrap"
                        ).permitAll()

                        // Only the exact ticket redemption endpoint
                        // is publicly accessible.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/desktop/redeem"
                        ).permitAll()

                        // Existing public endpoints.
                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers("/test/recording/**").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/extension/website-activity"
                        ).permitAll()

                        // Static resources.
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Super Admin routes.
                        .requestMatchers("/super-admin/**")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        // Admin routes.
                        .requestMatchers("/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        // Employee routes.
                        .requestMatchers("/employee/**")
                        .hasAuthority("ROLE_EMPLOYEE")

                        // Everything else requires authentication.
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("email")
                        .passwordParameter("password")
                        .successHandler(loginSuccessHandler)
                        .failureHandler(loginFailureHandler)
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessHandler(logoutSuccessHandler)
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }
}
