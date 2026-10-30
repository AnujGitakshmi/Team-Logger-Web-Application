
package com.example.TeamLoggerC.repository;

import com.example.TeamLoggerC.entity.BrowserExtensionToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrowserExtensionTokenRepository
        extends JpaRepository<BrowserExtensionToken, Long> {

    Optional<BrowserExtensionToken> findByTokenHash(String tokenHash);

    List<BrowserExtensionToken> findByEmployeeIdAndRevokedFalse(
            Integer employeeId);

    List<BrowserExtensionToken> findByEmployeeIdAndAdminIdAndRevokedFalse(
            Integer employeeId, Integer adminId);
}
