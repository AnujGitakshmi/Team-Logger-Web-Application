
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.security.CustomUserDetails;
import com.example.TeamLoggerC.service.DesktopLaunchTicketService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/employee/desktop")
public class DesktopLaunchController {

    private static final String LOGIN_RECORDED = "LOGIN_RECORDED";

    private final DesktopLaunchTicketService ticketService;

    public DesktopLaunchController(
            DesktopLaunchTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping("/launch-ticket")
    public ResponseEntity<Map<String, Object>> createLaunchTicket(
            Authentication authentication,
            HttpSession session) {

        // Require an authenticated employee.
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails userDetails)) {

            return ResponseEntity.status(401)
                    .body(Map.of("status", "UNAUTHENTICATED"));
        }

        User employee = userDetails.getUser();

        if (employee == null
                || !"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {

            return ResponseEntity.status(403)
                    .body(Map.of("status", "EMPLOYEE_ONLY"));
        }

        // Require an active employee account.
        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(employee.getStatus().trim())) {

            return ResponseEntity.status(403)
                    .body(Map.of("status", "ACCOUNT_NOT_ACTIVE"));
        }

        // Require successful login recording.
        Boolean loginRecorded =
                (Boolean) session.getAttribute(LOGIN_RECORDED);

        if (!Boolean.TRUE.equals(loginRecorded)) {

            return ResponseEntity.status(403)
                    .body(Map.of("status", "LOGIN_NOT_COMPLETED"));
        }

        // Do not launch a workstation that has not been activated.
        if (!employee.isWorkstationActivated()) {

            return ResponseEntity.status(403)
                    .body(Map.of("status", "WORKSTATION_NOT_ACTIVATED"));
        }

        // Generate a short-lived, single-use ticket.
        String ticket = ticketService.createTicket(employee.getId());

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Map.of(
                        "status", "READY",
                        "ticket", ticket
                ));
    }
}
