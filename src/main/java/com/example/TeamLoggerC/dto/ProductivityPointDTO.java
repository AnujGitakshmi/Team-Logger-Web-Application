package com.example.TeamLoggerC.dto;

public class ProductivityPointDTO {

    private String label;
    private long activeSeconds;
    private long idleSeconds;
    private double productivity;

    public ProductivityPointDTO(
            String label,
            long activeSeconds,
            long idleSeconds,
            double productivity) {

        this.label = label;
        this.activeSeconds = activeSeconds;
        this.idleSeconds = idleSeconds;
        this.productivity = productivity;
    }

    public String getLabel() {
        return label;
    }

    public long getActiveSeconds() {
        return activeSeconds;
    }

    public long getIdleSeconds() {
        return idleSeconds;
    }

    public double getProductivity() {
        return productivity;
    }
}