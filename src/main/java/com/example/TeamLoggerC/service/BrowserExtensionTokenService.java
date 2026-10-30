
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.BrowserExtensionToken;
import com.example.TeamLoggerC.repository.BrowserExtensionTokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class BrowserExtensionTokenService {

    private static final int TOKEN_BYTES = 32;

    private static final int TOKEN_VALIDITY_DAYS = 30;

    private final BrowserExtensionTokenRepository tokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public BrowserExtensionTokenService(
            BrowserExtensionTokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String createToken(
            Integer employeeId,
            Integer adminId,
            String deviceName) {

        if (employeeId == null || adminId == null) {
            throw new IllegalArgumentException(
                    "Employee ID and Admin ID are required.");
        }

        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        LocalDateTime now = LocalDateTime.now();

        BrowserExtensionToken token = new BrowserExtensionToken();
        token.setEmployeeId(employeeId);
        token.setAdminId(adminId);
        token.setTokenHash(hashToken(rawToken));
        token.setDeviceName(deviceName);
        token.setCreatedAt(now);
        token.setExpiresAt(now.plusDays(TOKEN_VALIDITY_DAYS));
        token.setRevoked(false);

        tokenRepository.save(token);

        // Return the raw token only once to the pairing caller.
        return rawToken;
    }

    @Transactional(readOnly = true)
    public Optional<BrowserExtensionToken> validateToken(
            String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }

        String tokenHash = hashToken(rawToken);

        return tokenRepository.findByTokenHash(tokenHash)
                .filter(token -> !token.isRevoked())
                .filter(token ->
                        token.getExpiresAt() != null
                                && token.getExpiresAt().isAfter(
                                LocalDateTime.now()));
    }

    @Transactional
    public boolean revokeToken(
            Integer employeeId,
            Long tokenId) {

        if (employeeId == null || tokenId == null) {
            return false;
        }

        Optional<BrowserExtensionToken> result =
                tokenRepository.findById(tokenId);

        if (result.isEmpty()) {
            return false;
        }

        BrowserExtensionToken token = result.get();

        // An employee may revoke only their own token.
        if (!employeeId.equals(token.getEmployeeId())) {
            return false;
        }

        if (token.isRevoked()) {
            return true;
        }

        token.setRevoked(true);
        tokenRepository.save(token);

        return true;
    }

    @Transactional(readOnly = true)
    public List<BrowserExtensionToken> getActiveTokens(
            Integer employeeId,
            Integer adminId) {

        if (employeeId == null || adminId == null) {
            return List.of();
        }

        return tokenRepository
                .findByEmployeeIdAndAdminIdAndRevokedFalse(
                        employeeId, adminId)
                .stream()
                .filter(token ->
                        token.getExpiresAt() != null
                                && token.getExpiresAt().isAfter(
                                LocalDateTime.now()))
                .toList();
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8));

            return java.util.HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable.", exception);
        }
    }
}
