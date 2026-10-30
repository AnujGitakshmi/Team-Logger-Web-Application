package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.OfficeIpAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OfficeIpAddressRepo
        extends JpaRepository<OfficeIpAddress, Integer> {

    Optional<OfficeIpAddress>
    findByAdminId(Integer adminId);

    Optional<OfficeIpAddress>
    findByAdminIdAndEnabledTrue(Integer adminId);

    boolean existsByAdminId(Integer adminId);
}

