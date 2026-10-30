package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionRepo extends JpaRepository<Session, Integer> {

    List<Session> findByUserId(Integer userId);

    List<Session> findByUserIdAndLogoutTimeIsNull(Integer userId);

    List<Session> findByUserIdAndLoginTimeBetween(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Session> findByLogoutTimeIsNull();

    List<Session> findByLoginTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    List<Session> findByUserIdAndLoginTimeBetweenAndLogoutTimeIsNull(
            Integer userId,
            LocalDateTime start,
            LocalDateTime end
    );

    List<Session> findByUserIdAndOfficeStartAndLogoutTimeIsNull(
            Integer userId,
            LocalDateTime officeStart
    );

    List<Session> findByOfficeEndAndLogoutTimeIsNull(
            LocalDateTime officeEnd
    );

    List<Session> findByOfficeStartAndOfficeEndAndLogoutTimeIsNull(
            LocalDateTime officeStart,
            LocalDateTime officeEnd
    );

    // Admin dashboard
    List<Session> findByUserIdInAndLogoutTimeIsNull(
            List<Integer> userIds
    );

    List<Session> findByUserIdInAndLoginTimeBetween(
            List<Integer> userIds,
            LocalDateTime start,
            LocalDateTime end
    );
}