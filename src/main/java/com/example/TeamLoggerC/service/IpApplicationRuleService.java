
package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.IpApplicationRule;
import com.example.TeamLoggerC.repository.IpApplicationRuleRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class IpApplicationRuleService {

    public static final String TARGET_APPLICATION = "APPLICATION";
    public static final String TARGET_WEBSITE = "WEBSITE";

    public static final String PERMISSION_ALLOW = "ALLOW";
    public static final String PERMISSION_BLOCK = "BLOCK";

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_INACTIVE = "INACTIVE";

    private final IpApplicationRuleRepo repository;

    public IpApplicationRuleService(IpApplicationRuleRepo repository) {
        this.repository = repository;
    }

    // =====================================================
    // GET RULES
    // =====================================================

    @Transactional(readOnly = true)
    public List<IpApplicationRule> getRules(Integer adminId) {
        validateAdminId(adminId);
        return repository.findByAdminIdOrderByUpdatedAtDesc(adminId);
    }

    @Transactional(readOnly = true)
    public List<IpApplicationRule> getRules(
            Integer adminId,
            String targetType
    ) {
        validateAdminId(adminId);

        String type = normalizeTargetType(targetType);

        return repository.findByAdminIdAndTargetTypeOrderByUpdatedAtDesc(
                adminId,
                type
        );
    }

    // =====================================================
    // CREATE APPLICATION RULE (BACKWARD COMPATIBLE)
    // =====================================================

    public void createRule(
            Integer adminId,
            String ipAddress,
            String applicationName,
            String executableName,
            String permission
    ) {
        createRule(
                adminId,
                ipAddress,
                TARGET_APPLICATION,
                applicationName,
                executableName,
                permission
        );
    }

    // =====================================================
    // CREATE RULE
    // Supports APPLICATION and WEBSITE
    // =====================================================

    public void createRule(
            Integer adminId,
            String ipAddress,
            String targetType,
            String applicationName,
            String executableName,
            String permission
    ) {
        validateAdminId(adminId);

        String ip = normalizeIp(ipAddress);
        String type = normalizeTargetType(targetType);
        String name = requiredText(
                applicationName,
                type.equals(TARGET_WEBSITE) ? "Website domain" : "Application name",
                255
        );

        String executable = type.equals(TARGET_WEBSITE)
                ? null
                : optionalText(executableName, 255);

        String action = normalizePermission(permission);

        var existing =
                repository.findByAdminIdAndIpAddressAndTargetTypeAndApplicationNameIgnoreCase(
                        adminId,
                        ip,
                        type,
                        name
                );

        IpApplicationRule rule;

        if (existing.isPresent()) {
            rule = existing.get();
        } else {
            rule = new IpApplicationRule();
            rule.setAdminId(adminId);
            rule.setIpAddress(ip);
            rule.setTargetType(type);
            rule.setApplicationName(name);
        }

        rule.setExecutableName(executable);
        rule.setPermission(action);
        rule.setStatus(STATUS_ACTIVE);

        repository.save(rule);
    }

    // =====================================================
    // UPDATE APPLICATION RULE (BACKWARD COMPATIBLE)
    // =====================================================

    public void updateRule(
            Integer adminId,
            Long id,
            String ipAddress,
            String applicationName,
            String executableName,
            String permission
    ) {
        updateRule(
                adminId,
                id,
                ipAddress,
                TARGET_APPLICATION,
                applicationName,
                executableName,
                permission
        );
    }

    // =====================================================
    // UPDATE RULE
    // =====================================================

    public void updateRule(
            Integer adminId,
            Long id,
            String ipAddress,
            String targetType,
            String applicationName,
            String executableName,
            String permission
    ) {
        validateAdminId(adminId);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid rule ID.");
        }

        IpApplicationRule rule = repository
                .findByIdAndAdminId(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Rule not found.")
                );

        String ip = normalizeIp(ipAddress);
        String type = normalizeTargetType(targetType);

        String name = requiredText(
                applicationName,
                type.equals(TARGET_WEBSITE) ? "Website domain" : "Application name",
                255
        );

        String executable = type.equals(TARGET_WEBSITE)
                ? null
                : optionalText(executableName, 255);

        String action = normalizePermission(permission);

        var duplicate =
                repository.findByAdminIdAndIpAddressAndTargetTypeAndApplicationNameIgnoreCase(
                        adminId,
                        ip,
                        type,
                        name
                );

        if (duplicate.isPresent()
                && !duplicate.get().getId().equals(id)) {
            throw new IllegalArgumentException(
                    "A rule already exists for this IP, target type, and name."
            );
        }

        rule.setIpAddress(ip);
        rule.setTargetType(type);
        rule.setApplicationName(name);
        rule.setExecutableName(executable);
        rule.setPermission(action);

        repository.save(rule);
    }

    // =====================================================
    // TOGGLE STATUS
    // =====================================================

    public void toggleStatus(Integer adminId, Long id) {
        validateAdminId(adminId);

        IpApplicationRule rule = repository
                .findByIdAndAdminId(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Rule not found.")
                );

        rule.setStatus(
                STATUS_ACTIVE.equalsIgnoreCase(rule.getStatus())
                        ? STATUS_INACTIVE
                        : STATUS_ACTIVE
        );

        repository.save(rule);
    }

    // =====================================================
    // APPLICATION PERMISSION LOOKUP
    // =====================================================

    @Transactional(readOnly = true)
    public String getApplicationPermission(
            Integer adminId,
            String currentIp,
            String applicationName,
            String executableName
    ) {
        /*
         * Identity and IP validation are handled by the tracker.
         * If this method is called directly with missing information,
         * it does not attempt to query IP-specific rules.
         */
        if (adminId == null
                || currentIp == null
                || currentIp.isBlank()) {
            return PERMISSION_ALLOW;
        }

        String ip;

        try {
            ip = normalizeIp(currentIp);
        } catch (IllegalArgumentException exception) {
            return PERMISSION_ALLOW;
        }

        if ((applicationName == null || applicationName.isBlank())
                && (executableName == null || executableName.isBlank())) {
            return PERMISSION_ALLOW;
        }

        List<IpApplicationRule> rules =
                repository.findByAdminIdAndIpAddressAndTargetTypeAndStatus(
                        adminId,
                        ip,
                        TARGET_APPLICATION,
                        STATUS_ACTIVE
                );

        if (rules == null || rules.isEmpty()) {
            return PERMISSION_ALLOW;
        }

        String app = applicationName == null
                ? ""
                : applicationName.trim();

        String exe = executableName == null
                ? ""
                : executableName.trim();

        // 1. Prefer an exact application + executable match.
        if (!app.isBlank() && !exe.isBlank()) {
            for (IpApplicationRule rule : rules) {
                if (rule == null
                        || rule.getApplicationName() == null
                        || rule.getExecutableName() == null) {
                    continue;
                }

                if (rule.getApplicationName().trim().equalsIgnoreCase(app)
                        && rule.getExecutableName().trim().equalsIgnoreCase(exe)) {
                    return normalizeRulePermission(rule.getPermission());
                }
            }
        }

        // 2. Match executable name.
        if (!exe.isBlank()) {
            for (IpApplicationRule rule : rules) {
                if (rule != null
                        && rule.getExecutableName() != null
                        && rule.getExecutableName().trim().equalsIgnoreCase(exe)) {
                    return normalizeRulePermission(rule.getPermission());
                }
            }
        }

        // 3. Match application name with a compatible executable.
        if (!app.isBlank()) {
            for (IpApplicationRule rule : rules) {
                if (rule == null || rule.getApplicationName() == null
                        || !rule.getApplicationName().trim().equalsIgnoreCase(app)) {
                    continue;
                }

                String ruleExecutable = rule.getExecutableName() == null
                        ? ""
                        : rule.getExecutableName().trim();

                if (exe.isBlank()
                        || ruleExecutable.isBlank()
                        || ruleExecutable.equalsIgnoreCase(exe)) {
                    return normalizeRulePermission(rule.getPermission());
                }
            }
        }

        // No matching rule means the default is ALLOW.
        return PERMISSION_ALLOW;
    }

    private String normalizeRulePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            return PERMISSION_ALLOW;
        }

        String normalized = permission.trim().toUpperCase(Locale.ROOT);

        if (PERMISSION_BLOCK.equals(normalized)) {
            return PERMISSION_BLOCK;
        }

        if (PERMISSION_ALLOW.equals(normalized)) {
            return PERMISSION_ALLOW;
        }

        return PERMISSION_ALLOW;
    }

    // =====================================================
    // VALIDATION AND NORMALIZATION
    // =====================================================

    private void validateAdminId(Integer adminId) {
        if (adminId == null || adminId <= 0) {
            throw new IllegalArgumentException("Invalid admin ID.");
        }
    }

    private String normalizeTargetType(String targetType) {
        if (targetType == null || targetType.isBlank()) {
            throw new IllegalArgumentException(
                    "Target type must be APPLICATION or WEBSITE."
            );
        }

        String type = targetType.trim().toUpperCase(Locale.ROOT);

        if (!TARGET_APPLICATION.equals(type)
                && !TARGET_WEBSITE.equals(type)) {
            throw new IllegalArgumentException(
                    "Target type must be APPLICATION or WEBSITE."
            );
        }

        return type;
    }

    private String normalizeIp(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("IP address is required.");
        }

        String ip = value.trim();

        if (ip.length() > 45) {
            throw new IllegalArgumentException(
                    "Enter a valid IPv4 or IPv6 address."
            );
        }

        boolean looksLikeIp =
                ip.matches("[0-9.]+")
                        || ip.matches("[0-9a-fA-F:]+");

        if (!looksLikeIp) {
            throw new IllegalArgumentException(
                    "Enter a valid IPv4 or IPv6 address."
            );
        }

        try {
            InetAddress parsed = InetAddress.getByName(ip);

            if (ip.contains(":") && !(parsed instanceof Inet6Address)) {
                throw new IllegalArgumentException("Invalid IPv6 address.");
            }

            if (!ip.contains(":") && parsed.getAddress().length != 4) {
                throw new IllegalArgumentException("Invalid IPv4 address.");
            }

            return parsed.getHostAddress();

        } catch (Exception exception) {
            throw new IllegalArgumentException(
                    "Enter a valid IPv4 or IPv6 address."
            );
        }
    }

    private String requiredText(
            String value,
            String fieldName,
            int maxLength
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw new IllegalArgumentException(
                    fieldName + " must not exceed "
                            + maxLength + " characters."
            );
        }

        return result;
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String result = value.trim();

        if (result.length() > maxLength) {
            throw new IllegalArgumentException(
                    "Executable name must not exceed "
                            + maxLength + " characters."
            );
        }

        return result;
    }

    private String normalizePermission(String permission) {
        if (permission == null || permission.isBlank()) {
            throw new IllegalArgumentException("Choose BLOCK or ALLOW.");
        }

        String value = permission.trim().toUpperCase(Locale.ROOT);

        if (!PERMISSION_BLOCK.equals(value)
                && !PERMISSION_ALLOW.equals(value)) {
            throw new IllegalArgumentException("Choose BLOCK or ALLOW.");
        }

        return value;
    }

    // =====================================================
// DELETE RULE — ADMIN-OWNED RULES ONLY
// =====================================================

    public void deleteRule(Integer adminId, Long id) {
        validateAdminId(adminId);

        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid rule ID.");
        }

        IpApplicationRule rule = repository
                .findByIdAndAdminId(id, adminId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Rule not found.")
                );

        repository.delete(rule);
    }
}
