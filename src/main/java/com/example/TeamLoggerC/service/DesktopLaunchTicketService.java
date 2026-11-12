
package com.example.TeamLoggerC.service;

import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DesktopLaunchTicketService {

    private static final long TICKET_VALIDITY_SECONDS = 60;

    private final SecureRandom secureRandom = new SecureRandom();

    private final ConcurrentHashMap<String, TicketData> tickets =
            new ConcurrentHashMap<>();

    public String createTicket(Integer employeeId) {

        if (employeeId == null) {
            throw new IllegalArgumentException("Employee ID is required.");
        }

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String ticket = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        tickets.put(
                ticket,
                new TicketData(
                        employeeId,
                        Instant.now().plusSeconds(TICKET_VALIDITY_SECONDS)
                )
        );

        removeExpiredTickets();

        return ticket;
    }

    public Integer redeemTicket(String ticket) {

        if (ticket == null || ticket.isBlank()) {
            return null;
        }

        // remove() makes redemption single-use.
        TicketData ticketData = tickets.remove(ticket);

        if (ticketData == null) {
            return null;
        }

        if (!Instant.now().isBefore(ticketData.expiresAt())) {
            return null;
        }

        return ticketData.employeeId();
    }

    private void removeExpiredTickets() {

        Instant now = Instant.now();

        tickets.entrySet().removeIf(
                entry -> !now.isBefore(entry.getValue().expiresAt())
        );
    }

    private record TicketData(
            Integer employeeId,
            Instant expiresAt
    ) {
    }
}
