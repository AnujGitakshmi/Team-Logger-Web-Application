package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.Attendance;
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


    /*
     * ============================================================
     * MARK PRESENT WHEN EMPLOYEE CLICKS START WORK
     * ============================================================
     */
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

            /*
             * Already PRESENT.
             * Do not create another record.
             */
            if ("PRESENT".equalsIgnoreCase(
                    attendance.getStatus()
            )) {
                return attendance;
            }

            /*
             * If an ABSENT record already exists,
             * Start Work changes it to PRESENT.
             */
            attendance.setStatus("PRESENT");
            attendance.setStartTime(now);

            return attendanceRepo.save(attendance);
        }

        /*
         * No attendance record exists.
         * Create a new PRESENT record.
         */
        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setStartTime(now);
        attendance.setStatus("PRESENT");
        attendance.setCreatedAt(LocalDateTime.now());

        return attendanceRepo.save(attendance);
    }


    /*
     * ============================================================
     * MARK ABSENT AFTER 12 PM
     * ============================================================
     *
     * If employee has not started work by 12 PM,
     * create/update today's attendance as ABSENT.
     */
    @Transactional
    public void markAbsentIfNotStarted(Integer employeeId) {

        LocalDate today = LocalDate.now();

        Optional<Attendance> existing =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        today
                );

        /*
         * Attendance record already exists.
         */
        if (existing.isPresent()) {

            Attendance attendance = existing.get();

            /*
             * Never change PRESENT to ABSENT.
             */
            if ("PRESENT".equalsIgnoreCase(
                    attendance.getStatus()
            )) {
                return;
            }

            attendance.setStatus("ABSENT");

            attendanceRepo.save(attendance);

            return;
        }

        /*
         * No record exists.
         * Create ABSENT record.
         */
        Attendance attendance = new Attendance();

        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(today);
        attendance.setStatus("ABSENT");
        attendance.setCreatedAt(LocalDateTime.now());

        attendanceRepo.save(attendance);
    }


    /*
     * ============================================================
     * MARK ALL EMPLOYEES ABSENT
     * ============================================================
     *
     * NOTE:
     * This method currently searches with adminId = null.
     *
     * If employees belong to an admin through adminId,
     * this query will probably NOT find them.
     *
     * We will fix this after checking the scheduler/user repository.
     */
    @Transactional
    public void markAllEmployeesAbsentIfNotStarted() {

        List<com.example.TeamLoggerC.entity.User> employees =
                userRepo.findByAdminIdAndRole(
                        null,
                        "EMPLOYEE"
                );

        for (com.example.TeamLoggerC.entity.User employee : employees) {

            if (employee.getId() == null) {
                continue;
            }

            markAbsentIfNotStarted(employee.getId());
        }
    }


    /*
     * ============================================================
     * GET TODAY'S ATTENDANCE
     * ============================================================
     */
    public Optional<Attendance> getTodayAttendance(
            Integer employeeId
    ) {

        return attendanceRepo
                .findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        LocalDate.now()
                );
    }


    /*
     * ============================================================
     * GET ATTENDANCE FOR A YEAR
     * ============================================================
     */
    public List<Attendance> getYearAttendance(
            Integer employeeId,
            int year
    ) {

        LocalDate startDate =
                LocalDate.of(year, 1, 1);

        LocalDate endDate =
                LocalDate.of(year, 12, 31);

        return attendanceRepo
                .findByEmployeeIdAndAttendanceDateBetweenOrderByAttendanceDateAsc(
                        employeeId,
                        startDate,
                        endDate
                );
    }


    /*
     * ============================================================
     * GET ALL ATTENDANCE
     * ============================================================
     */
    public List<Attendance> getAllAttendance(
            Integer employeeId
    ) {

        return attendanceRepo
                .findByEmployeeIdOrderByAttendanceDateDesc(
                        employeeId
                );
    }


    /*
     * ============================================================
     * COUNT PRESENT DAYS
     * ============================================================
     *
     * Existing method retained for compatibility.
     */
    public long countPresentDays(
            Integer employeeId
    ) {

        return attendanceRepo.countByEmployeeIdAndStatus(
                employeeId,
                "PRESENT"
        );
    }


    /*
     * ============================================================
     * COUNT ABSENT DAYS
     * ============================================================
     *
     * Existing method retained for compatibility.
     */
    public long countAbsentDays(
            Integer employeeId
    ) {

        return attendanceRepo.countByEmployeeIdAndStatus(
                employeeId,
                "ABSENT"
        );
    }


    /*
     * ============================================================
     * COUNT PRESENT DAYS FOR SELECTED YEAR
     * ============================================================
     */
    public long countPresentDays(
            Integer employeeId,
            int year
    ) {

        return getYearAttendance(
                employeeId,
                year
        )
                .stream()
                .filter(attendance ->
                        "PRESENT".equalsIgnoreCase(
                                attendance.getStatus()
                        )
                )
                .count();
    }


    /*
     * ============================================================
     * COUNT ABSENT DAYS FOR SELECTED YEAR
     * ============================================================
     */
    public long countAbsentDays(
            Integer employeeId,
            int year
    ) {

        return getYearAttendance(
                employeeId,
                year
        )
                .stream()
                .filter(attendance ->
                        "ABSENT".equalsIgnoreCase(
                                attendance.getStatus()
                        )
                )
                .count();
    }


    /*
     * ============================================================
     * DETERMINE DISPLAY STATUS FOR CALENDAR
     * ============================================================
     *
     * Rules:
     *
     * PRESENT -> green
     * ABSENT  -> red
     * No record -> EMPTY / white
     * Future   -> FUTURE / white
     *
     * IMPORTANT:
     * A missing attendance record is NOT automatically ABSENT.
     */
    public String getDisplayStatus(
            Integer employeeId,
            LocalDate date
    ) {

        Optional<Attendance> attendance =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        date
                );

        /*
         * Database record exists.
         */
        if (attendance.isPresent()) {

            String status =
                    attendance.get()
                            .getStatus();

            if (status != null) {

                return status.toUpperCase();
            }

            return "EMPTY";
        }

        /*
         * Future date.
         */
        LocalDate today = LocalDate.now();

        if (date.isAfter(today)) {
            return "FUTURE";
        }

        /*
         * No database record.
         *
         * This is intentionally NOT ABSENT.
         *
         * The calendar will display this day as white.
         */
        return "EMPTY";
    }


    /*
     * ============================================================
     * MARK END TIME
     * ============================================================
     */
    @Transactional
    public Attendance markEndTimeAt(
            Integer employeeId,
            LocalTime endTime
    ) {

        LocalDate today = LocalDate.now();

        Optional<Attendance> existing =
                attendanceRepo.findByEmployeeIdAndAttendanceDate(
                        employeeId,
                        today
                );

        if (existing.isEmpty()) {
            return null;
        }

        Attendance attendance = existing.get();

        attendance.setEndTime(endTime);

        return attendanceRepo.save(attendance);
    }


    /*
     * ============================================================
     * MARK END TIME USING CURRENT TIME
     * ============================================================
     */
    @Transactional
    public Attendance markEndTime(
            Integer employeeId
    ) {

        return markEndTimeAt(
                employeeId,
                LocalTime.now()
        );
    }
}