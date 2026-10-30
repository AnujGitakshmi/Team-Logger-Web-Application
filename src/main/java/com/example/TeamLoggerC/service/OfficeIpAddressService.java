package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.OfficeIpAddress;
import com.example.TeamLoggerC.repository.OfficeIpAddressRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Transactional
public class OfficeIpAddressService {

    private final OfficeIpAddressRepo officeIpAddressRepo;


    // =========================================================
    // IPv4 / IPv6 VALIDATION
    // =========================================================

    /*
     * Basic validation is performed here first.
     *
     * IPv4:
     * 192.168.1.1
     * 203.0.113.10
     *
     * IPv6:
     * 2001:db8::1
     */
    private static final Pattern IPV4_PATTERN =
            Pattern.compile(
                    "^((25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})\\.){3}"
                            + "(25[0-5]|2[0-4][0-9]|1?[0-9]{1,2})$"
            );

    private static final Pattern IPV6_PATTERN =
            Pattern.compile(
                    "^[0-9a-fA-F:]+$"
            );


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public OfficeIpAddressService(
            OfficeIpAddressRepo officeIpAddressRepo) {

        this.officeIpAddressRepo =
                officeIpAddressRepo;
    }


    // =========================================================
    // GET PRIMARY IP
    // =========================================================

    /**
     * Returns the primary IP configuration for an admin.
     *
     * @param adminId logged-in admin ID
     * @return primary IP configuration or null
     */
    @Transactional(readOnly = true)
    public OfficeIpAddress getPrimaryIp(
            Integer adminId) {

        validateAdminId(adminId);

        return officeIpAddressRepo
                .findByAdminId(adminId)
                .orElse(null);
    }


    // =========================================================
    // GET ENABLED PRIMARY IP
    // =========================================================

    /**
     * Returns the enabled primary IP configuration.
     *
     * This method will later be used by the IP validation
     * process during employee access/login.
     */
    @Transactional(readOnly = true)
    public Optional<OfficeIpAddress> getEnabledPrimaryIp(
            Integer adminId) {

        validateAdminId(adminId);

        return officeIpAddressRepo
                .findByAdminIdAndEnabledTrue(adminId);
    }


    // =========================================================
    // SAVE PRIMARY IP
    // =========================================================

    /**
     * Creates or updates the primary office IP.
     *
     * The admin can manually enter the IP address.
     *
     * There can only be ONE primary IP configuration
     * for each admin.
     */
    public OfficeIpAddress savePrimaryIp(
            Integer adminId,
            String ipAddress) {

        validateAdminId(adminId);

        String normalizedIp =
                normalizeIp(ipAddress);

        validateIpAddress(normalizedIp);


        OfficeIpAddress officeIpAddress =
                officeIpAddressRepo
                        .findByAdminId(adminId)
                        .orElseGet(() -> {

                            OfficeIpAddress newConfig =
                                    new OfficeIpAddress();

                            newConfig.setAdminId(adminId);

                            return newConfig;
                        });


        officeIpAddress.setPrimaryIpAddress(
                normalizedIp
        );

        officeIpAddress.setEnabled(true);


        return officeIpAddressRepo.save(
                officeIpAddress
        );
    }


    // =========================================================
    // ENABLE PRIMARY IP
    // =========================================================

    /**
     * Enables the primary IP restriction configuration.
     */
    public OfficeIpAddress enablePrimaryIp(
            Integer adminId) {

        validateAdminId(adminId);

        OfficeIpAddress officeIpAddress =
                officeIpAddressRepo
                        .findByAdminId(adminId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Primary IP is not configured."
                                )
                        );

        officeIpAddress.setEnabled(true);

        return officeIpAddressRepo.save(
                officeIpAddress
        );
    }


    // =========================================================
    // DISABLE PRIMARY IP
    // =========================================================

    /**
     * Disables the primary IP configuration.
     *
     * The configuration remains stored so the admin
     * can enable it again later.
     */
    public OfficeIpAddress disablePrimaryIp(
            Integer adminId) {

        validateAdminId(adminId);

        OfficeIpAddress officeIpAddress =
                officeIpAddressRepo
                        .findByAdminId(adminId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Primary IP is not configured."
                                )
                        );

        officeIpAddress.setEnabled(false);

        return officeIpAddressRepo.save(
                officeIpAddress
        );
    }


    // =========================================================
    // DELETE PRIMARY IP
    // =========================================================

    /**
     * Completely removes the primary IP configuration.
     *
     * This does NOT delete employee approval records.
     * Those will be handled separately later.
     */
    public void deletePrimaryIp(
            Integer adminId) {

        validateAdminId(adminId);

        OfficeIpAddress officeIpAddress =
                officeIpAddressRepo
                        .findByAdminId(adminId)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Primary IP is not configured."
                                )
                        );

        officeIpAddressRepo.delete(
                officeIpAddress
        );
    }


    // =========================================================
    // CHECK PRIMARY IP
    // =========================================================

    /**
     * Checks whether the supplied IP matches the
     * configured primary IP of the admin.
     *
     * This method does NOT check employee-approved IPs yet.
     */
    @Transactional(readOnly = true)
    public boolean isPrimaryIp(
            Integer adminId,
            String ipAddress) {

        validateAdminId(adminId);

        String normalizedIp =
                normalizeIp(ipAddress);

        if (!isValidIp(normalizedIp)) {
            return false;
        }

        Optional<OfficeIpAddress> config =
                officeIpAddressRepo
                        .findByAdminIdAndEnabledTrue(adminId);

        if (config.isEmpty()) {
            return false;
        }

        return normalizedIp.equals(
                config.get().getPrimaryIpAddress()
        );
    }


    // =========================================================
    // NORMALIZE IP
    // =========================================================

    private String normalizeIp(
            String ipAddress) {

        if (ipAddress == null) {
            return "";
        }

        return ipAddress.trim();
    }


    // =========================================================
    // VALIDATE IP
    // =========================================================

    private void validateIpAddress(
            String ipAddress) {

        if (!isValidIp(ipAddress)) {

            throw new IllegalArgumentException(
                    "Invalid IP address: "
                            + ipAddress
            );
        }
    }


    // =========================================================
    // CHECK IP FORMAT
    // =========================================================

    private boolean isValidIp(
            String ipAddress) {

        if (ipAddress == null
                || ipAddress.isBlank()) {

            return false;
        }

        return IPV4_PATTERN.matcher(ipAddress).matches()
                || IPV6_PATTERN.matcher(ipAddress).matches();
    }


    // =========================================================
    // VALIDATE ADMIN ID
    // =========================================================

    private void validateAdminId(
            Integer adminId) {

        if (adminId == null
                || adminId <= 0) {

            throw new IllegalArgumentException(
                    "Invalid admin ID."
            );
        }
    }
}

