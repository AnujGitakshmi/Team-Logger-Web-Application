
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
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/extension/website-activity"
                        )
                )

                .securityContext(securityContext -> securityContext
                        .securityContextRepository(securityContextRepository)
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/login",
                                "/signup",
                                "/organization/register"
                        ).permitAll()

                        .requestMatchers(
                                "/employee/login-pending",
                                "/employee/login-approval-status"
                        ).permitAll()

                        .requestMatchers("/api/test").permitAll()
                        .requestMatchers("/test/recording/**").permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/extension/website-activity"
                        ).permitAll()

                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // Only accounts granted ROLE_SUPER_ADMIN can enter.
                        .requestMatchers("/super-admin/**")
                        .hasAuthority("ROLE_SUPER_ADMIN")

                        // Normal Admin and Branch Admin routes.
                        .requestMatchers("/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        // Employee routes.
                        .requestMatchers("/employee/**")
                        .hasAuthority("ROLE_EMPLOYEE")

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
