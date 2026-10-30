
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.IpApplicationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IpApplicationRuleRepo
        extends JpaRepository<IpApplicationRule, Long> {

    // Find active rules for a particular admin and IP.
    List<IpApplicationRule> findByAdminIdAndIpAddressAndStatus(
            Integer adminId,
            String ipAddress,
            String status
    );

    // Find a rule by admin, IP, and application name.
    Optional<IpApplicationRule>
    findByAdminIdAndIpAddressAndApplicationNameIgnoreCase(
            Integer adminId,
            String ipAddress,
            String applicationName
    );

    // Find a rule by admin, IP, executable, and status.
    Optional<IpApplicationRule>
    findByAdminIdAndIpAddressAndExecutableNameIgnoreCaseAndStatus(
            Integer adminId,
            String ipAddress,
            String executableName,
            String status
    );

    // NEW: List all rules belonging to the logged-in admin.
    List<IpApplicationRule> findByAdminIdOrderByUpdatedAtDesc(
            Integer adminId
    );

    // NEW: Retrieve a rule only if it belongs to the specified admin.
    Optional<IpApplicationRule> findByIdAndAdminId(
            Long id,
            Integer adminId
    );

    List<IpApplicationRule>
    findByAdminIdAndIpAddressAndTargetTypeAndStatus(
            Integer adminId,
            String ipAddress,
            String targetType,
            String status
    );
    List<IpApplicationRule> findByAdminIdAndTargetTypeOrderByUpdatedAtDesc(
            Integer adminId,
            String targetType
    );

    java.util.Optional<IpApplicationRule>
    findByAdminIdAndIpAddressAndTargetTypeAndApplicationNameIgnoreCase(
            Integer adminId,
            String ipAddress,
            String targetType,
            String applicationName
    );
}
