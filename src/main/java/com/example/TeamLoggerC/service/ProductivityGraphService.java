package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.dto.ProductivityPointDTO;
import com.example.TeamLoggerC.entity.Session;
import com.example.TeamLoggerC.repository.SessionRepo;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductivityGraphService {

    private final SessionRepo sessionRepo;

    public ProductivityGraphService(SessionRepo sessionRepo) {
        this.sessionRepo = sessionRepo;
    }

    public List<ProductivityPointDTO> getProductivityData(
            Integer userId,
            String period) {

        LocalDate today = LocalDate.now();

        if ("daily".equalsIgnoreCase(period)) {
            return getDailyData(userId, today);
        }

        if ("monthly".equalsIgnoreCase(period)) {
            return getMonthlyData(userId, today);
        }

        if ("yearly".equalsIgnoreCase(period)) {
            return getYearlyData(userId, today);
        }

        throw new IllegalArgumentException("Invalid productivity period");
    }

    private List<ProductivityPointDTO> getDailyData(
            Integer userId,
            LocalDate today) {

        LocalDate firstDay = today.withDayOfMonth(1);

        List<Session> sessions = getSessions(
                userId,
                firstDay.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        Map<Integer, Totals> totals = new HashMap<>();

        for (Session session : sessions) {
            if (session.getEmployeeStartedAt() == null) {
                continue;
            }

            int day = session.getEmployeeStartedAt().getDayOfMonth();

            totals.computeIfAbsent(day, key -> new Totals())
                    .add(session);
        }

        List<ProductivityPointDTO> result = new ArrayList<>();

        for (int day = 1; day <= today.getDayOfMonth(); day++) {
            Totals value = totals.getOrDefault(day, new Totals());

            result.add(createPoint(
                    String.valueOf(day),
                    value
            ));
        }

        return result;
    }

    private List<ProductivityPointDTO> getMonthlyData(
            Integer userId,
            LocalDate today) {

        LocalDate start = today.withDayOfYear(1);

        List<Session> sessions = getSessions(
                userId,
                start.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        Map<Integer, Totals> totals = new HashMap<>();

        for (Session session : sessions) {
            if (session.getEmployeeStartedAt() == null) {
                continue;
            }

            int month = session.getEmployeeStartedAt().getMonthValue();

            totals.computeIfAbsent(month, key -> new Totals())
                    .add(session);
        }

        List<ProductivityPointDTO> result = new ArrayList<>();

        for (int month = 1; month <= 12; month++) {
            Totals value = totals.getOrDefault(month, new Totals());

            result.add(createPoint(
                    Month.of(month).name().substring(0, 3),
                    value
            ));
        }

        return result;
    }

    private List<ProductivityPointDTO> getYearlyData(
            Integer userId,
            LocalDate today) {

        int currentYear = today.getYear();
        int firstYear = currentYear - 4;

        LocalDate start = LocalDate.of(firstYear, 1, 1);

        List<Session> sessions = getSessions(
                userId,
                start.atStartOfDay(),
                today.plusDays(1).atStartOfDay()
        );

        Map<Integer, Totals> totals = new HashMap<>();

        for (Session session : sessions) {
            if (session.getEmployeeStartedAt() == null) {
                continue;
            }

            int year = session.getEmployeeStartedAt().getYear();

            totals.computeIfAbsent(year, key -> new Totals())
                    .add(session);
        }

        List<ProductivityPointDTO> result = new ArrayList<>();

        for (int year = firstYear; year <= currentYear; year++) {
            Totals value = totals.getOrDefault(year, new Totals());

            result.add(createPoint(
                    String.valueOf(year),
                    value
            ));
        }

        return result;
    }

    private List<Session> getSessions(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end) {

        return sessionRepo.findByUserIdAndEmployeeStartedAtBetween(
                userId,
                start,
                end
        );
    }

    private ProductivityPointDTO createPoint(
            String label,
            Totals totals) {

        long totalSeconds =
                totals.activeSeconds + totals.idleSeconds;

        double productivity = totalSeconds > 0
                ? (totals.activeSeconds * 100.0) / totalSeconds
                : 0.0;

        return new ProductivityPointDTO(
                label,
                totals.activeSeconds,
                totals.idleSeconds,
                Math.round(productivity * 100.0) / 100.0
        );
    }

    private static class Totals {

        private long activeSeconds;
        private long idleSeconds;

        private void add(Session session) {

            activeSeconds += session.getActiveSeconds() != null
                    ? Math.max(0, session.getActiveSeconds())
                    : 0;

            idleSeconds += session.getIdleSeconds() != null
                    ? Math.max(0, session.getIdleSeconds())
                    : 0;
        }
    }
}