package com.example.TeamLoggerC.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "attendance",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {
                        "employee_id",
                        "attendance_date"
                }
        )
)
public class Attendance {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Employee who owns this attendance
    @Column(name = "employee_id", nullable = false)
    private Integer employeeId;

    // One attendance record per day
    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    // Time when Start Work was clicked
    @Column(name = "start_time")
    private LocalTime startTime;

    // Time when Stop Work was clicked
    @Column(name = "end_time")
    private LocalTime endTime;

    // PRESENT or ABSENT
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // When the attendance record was created
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    /* ===================== GETTERS & SETTERS ===================== */

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Integer employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }


}
