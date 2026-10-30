package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "overtime")
public class Overtime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "overtime_date", nullable = false)
    private LocalDate overtimeDate;

    @Column(name = "office_end")
    private LocalDateTime officeEnd;

    @Column(name = "overtime_start")
    private LocalDateTime overtimeStart;

    @Column(name = "overtime_end")
    private LocalDateTime overtimeEnd;

    @Column(name = "overtime_seconds")
    private Integer overtimeSeconds = 0;

    public Overtime() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDate getOvertimeDate() {
        return overtimeDate;
    }

    public void setOvertimeDate(LocalDate overtimeDate) {
        this.overtimeDate = overtimeDate;
    }

    public LocalDateTime getOfficeEnd() {
        return officeEnd;
    }

    public void setOfficeEnd(LocalDateTime officeEnd) {
        this.officeEnd = officeEnd;
    }

    public LocalDateTime getOvertimeStart() {
        return overtimeStart;
    }

    public void setOvertimeStart(LocalDateTime overtimeStart) {
        this.overtimeStart = overtimeStart;
    }

    public LocalDateTime getOvertimeEnd() {
        return overtimeEnd;
    }

    public void setOvertimeEnd(LocalDateTime overtimeEnd) {
        this.overtimeEnd = overtimeEnd;
    }

    public Integer getOvertimeSeconds() {
        return overtimeSeconds;
    }

    public void setOvertimeSeconds(Integer overtimeSeconds) {
        this.overtimeSeconds = overtimeSeconds;
    }
}

