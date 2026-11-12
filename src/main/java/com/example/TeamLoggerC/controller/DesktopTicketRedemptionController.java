
package com.example.TeamLoggerC.controller;

import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.UserRepo;
import com.example.TeamLoggerC.service.DesktopLaunchTicketService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/desktop")
public class DesktopTicketRedemptionController {

    private final DesktopLaunchTicketService ticketService;
    private final UserRepo userRepo;
    private final UserDetailsService userDetailsService;
    private final SecurityContextRepository securityContextRepository;

    public DesktopTicketRedemptionController(
            DesktopLaunchTicketService ticketService,
            UserRepo userRepo,
            UserDetailsService userDetailsService,
            SecurityContextRepository securityContextRepository) {

        this.ticketService = ticketService;
        this.userRepo = userRepo;
        this.userDetailsService = userDetailsService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/redeem")
    public ResponseEntity<Map<String, Object>> redeemTicket(
            @RequestBody(required = false) Map<String, String> requestBody,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (requestBody == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "INVALID_REQUEST"));
        }

        String ticket = requestBody.get("ticket");

        if (ticket == null || ticket.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("status", "INVALID_TICKET"));
        }

        // Redeem the ticket only once.
        Integer employeeId = ticketService.redeemTicket(ticket);

        if (employeeId == null) {
            return ResponseEntity.status(401)
                    .body(Map.of("status", "TICKET_EXPIRED_OR_INVALID"));
        }

        Optional<User> optionalUser = userRepo.findById(employeeId);

        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401)
                    .body(Map.of("status", "EMPLOYEE_NOT_FOUND"));
        }

        User employee = optionalUser.get();

        // Verify employee role.
        if (!"EMPLOYEE".equalsIgnoreCase(employee.getRole())) {
            return ResponseEntity.status(403)
                    .body(Map.of("status", "INVALID_ROLE"));
        }

        // Verify active account.
        if (employee.getStatus() == null
                || !"ACTIVE".equalsIgnoreCase(employee.getStatus().trim())) {

            return ResponseEntity.status(403)
                    .body(Map.of("status", "ACCOUNT_NOT_ACTIVE"));
        }

        // Verify workstation activation.
        if (!employee.isWorkstationActivated()) {
            return ResponseEntity.status(403)
                    .body(Map.of("status", "WORKSTATION_NOT_ACTIVATED"));
        }

        if (employee.getEmail() == null
                || employee.getEmail().isBlank()) {

            return ResponseEntity.status(401)
                    .body(Map.of("status", "EMPLOYEE_EMAIL_MISSING"));
        }

        try {

            // Load the employee's actual Spring Security identity.
            UserDetails userDetails =
                    userDetailsService.loadUserByUsername(employee.getEmail());

            if (!userDetails.isEnabled()) {
                return ResponseEntity.status(403)
                        .body(Map.of("status", "ACCOUNT_NOT_ENABLED"));
            }

            boolean employeeAuthority = userDetails.getAuthorities()
                    .stream()
                    .anyMatch(authority ->
                            "ROLE_EMPLOYEE".equals(authority.getAuthority()));

            if (!employeeAuthority) {
                return ResponseEntity.status(403)
                        .body(Map.of("status", "EMPLOYEE_AUTHORITY_MISSING"));
            }

            // Create a separate HTTP session for the desktop client.
            request.getSession(true);

            // Build a new authenticated Spring Security context.
            Authentication authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContext securityContext =
                    SecurityContextHolder.createEmptyContext();

            securityContext.setAuthentication(authentication);

            // Persist authentication in the new desktop HTTP session.
            securityContextRepository.saveContext(
                    securityContext,
                    request,
                    response
            );

            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .body(Map.of(
                            "status", "DESKTOP_SESSION_CREATED"
                    ));

        } catch (Exception e) {

            System.err.println(
                    "Desktop session creation failed: "
                            + e.getMessage()
            );

            return ResponseEntity.status(500)
                    .body(Map.of("status", "SESSION_CREATION_FAILED"));
        }
    }
}
