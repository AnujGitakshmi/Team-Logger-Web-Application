
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.OrganizationAdminSignupRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationAdminSignupRequestRepo
        extends JpaRepository<OrganizationAdminSignupRequest, Integer> {

    boolean existsByEmailAndStatus(String email, String status);

    boolean existsByEmailAndOrganizationIdAndStatus(
            String email,
            Integer organizationId,
            String status
    );

    List<OrganizationAdminSignupRequest>
    findByOrganizationIdAndStatusOrderByRequestedAtDesc(
            Integer organizationId,
            String status
    );

    Optional<OrganizationAdminSignupRequest>
    findByIdAndOrganizationId(
            Integer id,
            Integer organizationId
    );
}
