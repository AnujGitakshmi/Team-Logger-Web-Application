
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Branch;
import com.example.TeamLoggerC.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BranchRepo extends JpaRepository<Branch, Integer> {

    List<Branch> findByOrganization(Organization organization);

    Optional<Branch> findByOrganizationAndNormalizedName(
            Organization organization,
            String normalizedName
    );

    boolean existsByOrganizationAndNormalizedName(
            Organization organization,
            String normalizedName
    );

    List<Branch> findByOrganizationId(Integer organizationId);
}
