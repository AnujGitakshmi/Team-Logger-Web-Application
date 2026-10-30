package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepo extends JpaRepository<Organization, Integer> {

    Optional<Organization> findByNormalizedName(String normalizedName);

    boolean existsByNormalizedName(String normalizedName);

    List<Organization> findAllByStatusIgnoreCaseOrderByNameAsc(String status);
}