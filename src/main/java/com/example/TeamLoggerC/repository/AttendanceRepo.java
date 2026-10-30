package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRepo extends JpaRepository<Attendance, Integer> {

    // Find attendance for one employee on one specific date
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(
            Integer employeeId,
            LocalDate attendanceDate
    );

    // Get attendance for an employee between two dates
    List<Attendance> findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
            Integer employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    // Get all attendance records of an employee
    List<Attendance> findByEmployeeIdOrderByAttendanceDateDesc(
            Integer employeeId
    );

    // Count PRESENT days
    long countByEmployeeIdAndStatus(
            Integer employeeId,
            String status
    );
}
