
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.BranchAdminSignupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BranchAdminSignupRequestRepo
        extends JpaRepository<BranchAdminSignupRequest, Integer> {

    List<BranchAdminSignupRequest>
    findByStatusOrderByRequestedAtDesc(String status);

    boolean existsByEmailAndStatus(String email, String status);

    boolean existsByEmailAndStatusIn(
            String email,
            List<String> statuses
    );
}
