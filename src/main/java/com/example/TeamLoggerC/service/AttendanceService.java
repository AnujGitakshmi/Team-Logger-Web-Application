
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Attendance;
import com.example.TeamLoggerC.entity.User;
import com.example.TeamLoggerC.repository.AttendanceRepo;
import com.example.TeamLoggerC.repository.UserRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AttendanceService {

    private final AttendanceRepo attendanceRepo;
    private final UserRepo userRepo;

    public AttendanceService(
            AttendanceRepo attendanceRepo,
            UserRepo userRepo
    ) {
        this.attendanceRepo = attendanceRepo;
        this.userRepo = userRepo;
    }

    // ============================================================
    // MARK PRESENT WHEN EMPLOYEE CLICKS START WORK
    // ============================================================

    @Transactional
    public Attendance markPresent(Integer employeeId) {

        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        Optional<Attendance> existing =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        today
                );

        if (existing.isPresent()) {

            Attendance attendance = existing.get();

            if ("PRESENT".equalsIgnoreCase(attendance.getStatus())) {
                return attendance;
            }

            // Convert an existing ABSENT record to PRESENT.
            attendance.setStatus("PRESENT");
            attendance.setStartTime(now);
            attendance.setEndTime(null);

            return attendanceRepo.save(attendance);
        }

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setStartTime(now);
        attendance.setStatus("PRESENT");
        attendance.setCreatedAt(LocalDateTime.now());

        return attendanceRepo.save(attendance);
    }

    // ============================================================
    // MARK ABSENT IF EMPLOYEE HAS NOT STARTED WORK
    // ============================================================

    @Transactional
    public void markAbsentIfNotStarted(Integer employeeId) {

        LocalDate today = LocalDate.now();

        Optional<Attendance> existing =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        today
                );

        if (existing.isPresent()) {

            Attendance attendance = existing.get();

            // Never change PRESENT to ABSENT.
            if ("PRESENT".equalsIgnoreCase(attendance.getStatus())) {
                return;
            }

            attendance.setStatus("ABSENT");
            attendanceRepo.save(attendance);

            return;
        }

        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setStatus("ABSENT");
        attendance.setCreatedAt(LocalDateTime.now());

        attendanceRepo.save(attendance);
    }

    // ============================================================
    // MARK ALL ACTIVE EMPLOYEES ABSENT IF NOT STARTED
    // ============================================================

    @Transactional
    public void markAllEmployeesAbsentIfNotStarted() {

        List<User> employees =
                userRepo.findByRoleIgnoreCaseAndStatusIgnoreCase(
                        "EMPLOYEE",
                        "ACTIVE"
                );

        for (User employee : employees) {

            if (employee.getId() == null) {
                continue;
            }

            markAbsentIfNotStarted(employee.getId());
        }
    }

    // ============================================================
    // GET TODAY'S ATTENDANCE
    // ============================================================

    public Optional<Attendance> getTodayAttendance(
            Integer employeeId
    ) {

        return attendanceRepo.findByEmployeeIdAndAttendanceDate(
                employeeId,
                LocalDate.now()
        );
    }

    // ============================================================
    // GET ATTENDANCE FOR A YEAR
    // ============================================================

    public List<Attendance> getYearAttendance(
            Integer employeeId,
            int year
    ) {

        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);

        return attendanceRepo
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        employeeId,
                        startDate,
                        endDate
                );
    }

    // ============================================================
    // GET ALL ATTENDANCE
    // ============================================================

    public List<Attendance> getAllAttendance(
            Integer employeeId
    ) {

        return attendanceRepo
                .findByEmployeeIdOrderByAttendanceDateDesc(
                        employeeId
                );
    }

    // ============================================================
    // COUNT PRESENT DAYS
    // ============================================================

    public long countPresentDays(Integer employeeId) {

        return attendanceRepo.countByEmployeeIdAndStatus(
                employeeId,
                "PRESENT"
        );
    }

    // ============================================================
    // COUNT ABSENT DAYS
    // ============================================================

    public long countAbsentDays(Integer employeeId) {

        return attendanceRepo.countByEmployeeIdAndStatus(
                employeeId,
                "ABSENT"
        );
    }

    // ============================================================
    // COUNT PRESENT DAYS FOR SELECTED YEAR
    // ============================================================

    public long countPresentDays(
            Integer employeeId,
            int year
    ) {

        return getYearAttendance(employeeId, year)
                .stream()
                .filter(attendance ->
                        "PRESENT".equalsIgnoreCase(
                                attendance.getStatus()
                        )
                )
                .count();
    }

    // ============================================================
    // COUNT ABSENT DAYS FOR SELECTED YEAR
    // ============================================================

    public long countAbsentDays(
            Integer employeeId,
            int year
    ) {

        return getYearAttendance(employeeId, year)
                .stream()
                .filter(attendance ->
                        "ABSENT".equalsIgnoreCase(
                                attendance.getStatus()
                        )
                )
                .count();
    }

    // ============================================================
    // DETERMINE DISPLAY STATUS FOR CALENDAR
    // ============================================================

    public String getDisplayStatus(
            Integer employeeId,
            LocalDate date
    ) {

        Optional<Attendance> attendance =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        date
                );

        if (attendance.isPresent()) {

            String status = attendance.get().getStatus();

            if (status != null) {
                return status.toUpperCase();
            }

            return "EMPTY";
        }

        LocalDate today = LocalDate.now();

        if (date.isAfter(today)) {
            return "FUTURE";
        }

        return "EMPTY";
    }

    // ============================================================
    // MARK END TIME FOR A SPECIFIC ATTENDANCE DATE
    // ============================================================

    @Transactional
    public Attendance markEndTimeAt(
            Integer employeeId,
            LocalDate attendanceDate,
            LocalTime endTime
    ) {

        Optional<Attendance> existing =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        attendanceDate
                );

        if (existing.isEmpty()) {
            return null;
        }

        Attendance attendance = existing.get();

        if (!"PRESENT".equalsIgnoreCase(attendance.getStatus())) {
            return null;
        }

        attendance.setEndTime(endTime);

        return attendanceRepo.save(attendance);
    }

    // ============================================================
    // BACKWARD-COMPATIBLE METHOD
    // Uses today's attendance date.
    // ============================================================

    @Transactional
    public Attendance markEndTimeAt(
            Integer employeeId,
            LocalTime endTime
    ) {

        return markEndTimeAt(
                employeeId,
                LocalDate.now(),
                endTime
        );
    }

    // ============================================================
    // MARK END TIME USING CURRENT TIME
    // ============================================================

    @Transactional
    public Attendance markEndTime(Integer employeeId) {

        return markEndTimeAt(
                employeeId,
                LocalDate.now(),
                LocalTime.now()
        );
    }
}
