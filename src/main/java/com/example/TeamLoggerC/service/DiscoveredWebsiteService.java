package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.DiscoveredWebsite;
import com.example.TeamLoggerC.repository.DiscoveredWebsiteRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DiscoveredWebsiteService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";


    // =========================================================
    // REPOSITORY
    // =========================================================

    private final DiscoveredWebsiteRepo discoveredWebsiteRepo;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public DiscoveredWebsiteService(
            DiscoveredWebsiteRepo discoveredWebsiteRepo
    ) {
        this.discoveredWebsiteRepo = discoveredWebsiteRepo;
    }


    // =========================================================
    // DISCOVER WEBSITE
    // =========================================================

    public DiscoveredWebsite discoverWebsite(
            Integer adminId,
            String domain,
            String websiteName,
            String browser
    ) {

        validateAdminId(adminId);

        String normalizedDomain =
                normalizeDomain(domain);

        String normalizedWebsiteName =
                normalizeOptionalValue(websiteName);

        String normalizedBrowser =
                normalizeOptionalValue(browser);


        // =====================================================
        // CHECK EXISTING WEBSITE
        // =====================================================

        DiscoveredWebsite website =
                discoveredWebsiteRepo
                        .findByAdminIdAndDomain(
                                adminId,
                                normalizedDomain
                        )
                        .orElse(null);


        // =====================================================
        // EXISTING WEBSITE
        // =====================================================

        if (website != null) {

            Integer count =
                    website.getDiscoveryCount();

            if (count == null) {
                count = 0;
            }

            website.setDiscoveryCount(
                    count + 1
            );


            // Update website name when available
            if (normalizedWebsiteName != null) {

                website.setWebsiteName(
                        normalizedWebsiteName
                );
            }


            // Update browser when available
            if (normalizedBrowser != null) {

                website.setBrowser(
                        normalizedBrowser
                );
            }


            // Discovered again = active
            website.setStatus(ACTIVE);


            // Update last seen time
            website.setLastSeenAt(
                    LocalDateTime.now()
            );


            return discoveredWebsiteRepo.save(
                    website
            );
        }


        // =====================================================
        // NEW WEBSITE
        // =====================================================

        DiscoveredWebsite newWebsite =
                new DiscoveredWebsite(
                        adminId,
                        normalizedDomain,
                        normalizedWebsiteName,
                        normalizedBrowser
                );


        newWebsite.setDiscoveryCount(1);

        newWebsite.setStatus(ACTIVE);


        LocalDateTime now =
                LocalDateTime.now();

        newWebsite.setFirstSeenAt(now);
        newWebsite.setLastSeenAt(now);


        return discoveredWebsiteRepo.save(
                newWebsite
        );
    }


    // =========================================================
    // GET ALL WEBSITES FOR ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredWebsite>
    getWebsitesForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredWebsiteRepo
                .findByAdminIdOrderByDomainAsc(
                        adminId
                );
    }


    // =========================================================
    // GET ACTIVE WEBSITES FOR ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredWebsite>
    getActiveWebsitesForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredWebsiteRepo
                .findByAdminIdAndStatusOrderByDomainAsc(
                        adminId,
                        ACTIVE
                );
    }


    // =========================================================
    // GET WEBSITE BY ID + ADMIN
    // =========================================================

    @Transactional(readOnly = true)
    public DiscoveredWebsite getWebsiteForAdmin(
            Integer adminId,
            Long websiteId
    ) {

        validateAdminId(adminId);

        if (websiteId == null) {

            throw new IllegalArgumentException(
                    "Website ID is required."
            );
        }


        return discoveredWebsiteRepo
                .findByIdAndAdminId(
                        websiteId,
                        adminId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Website not found."
                        )
                );
    }


    // =========================================================
    // GET WEBSITE BY DOMAIN
    // =========================================================

    @Transactional(readOnly = true)
    public DiscoveredWebsite getWebsiteByDomain(
            Integer adminId,
            String domain
    ) {

        validateAdminId(adminId);

        String normalizedDomain =
                normalizeDomain(domain);


        return discoveredWebsiteRepo
                .findByAdminIdAndDomain(
                        adminId,
                        normalizedDomain
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Website not found."
                        )
                );
    }


    // =========================================================
    // SEARCH WEBSITES
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredWebsite> searchWebsites(
            Integer adminId,
            String domain
    ) {

        validateAdminId(adminId);

        String searchText =
                normalizeOptionalValue(domain);


        if (searchText == null) {

            return getWebsitesForAdmin(
                    adminId
            );
        }


        return discoveredWebsiteRepo
                .findByAdminIdAndDomainContainingIgnoreCaseOrderByDomainAsc(
                        adminId,
                        searchText
                );
    }


    // =========================================================
    // ACTIVATE WEBSITE
    // =========================================================

    public DiscoveredWebsite activateWebsite(
            Integer adminId,
            Long websiteId
    ) {

        DiscoveredWebsite website =
                getWebsiteForAdmin(
                        adminId,
                        websiteId
                );


        website.setStatus(ACTIVE);


        return discoveredWebsiteRepo.save(
                website
        );
    }


    // =========================================================
    // DEACTIVATE WEBSITE
    // =========================================================

    public DiscoveredWebsite deactivateWebsite(
            Integer adminId,
            Long websiteId
    ) {

        DiscoveredWebsite website =
                getWebsiteForAdmin(
                        adminId,
                        websiteId
                );


        website.setStatus(INACTIVE);


        return discoveredWebsiteRepo.save(
                website
        );
    }


    // =========================================================
    // COUNT ALL WEBSITES
    // =========================================================

    @Transactional(readOnly = true)
    public long countWebsites(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredWebsiteRepo
                .countByAdminId(
                        adminId
                );
    }


    // =========================================================
    // COUNT ACTIVE WEBSITES
    // =========================================================

    @Transactional(readOnly = true)
    public long countActiveWebsites(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredWebsiteRepo
                .countByAdminIdAndStatus(
                        adminId,
                        ACTIVE
                );
    }


    // =========================================================
    // DELETE WEBSITE
    // =========================================================

    public void deleteWebsite(
            Integer adminId,
            Long websiteId
    ) {

        DiscoveredWebsite website =
                getWebsiteForAdmin(
                        adminId,
                        websiteId
                );


        discoveredWebsiteRepo.delete(
                website
        );
    }


    // =========================================================
    // DELETE ALL WEBSITES FOR ADMIN
    // =========================================================

    public void deleteWebsitesForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        discoveredWebsiteRepo
                .deleteByAdminId(
                        adminId
                );
    }


    // =========================================================
    // VALIDATE ADMIN ID
    // =========================================================

    private void validateAdminId(
            Integer adminId
    ) {

        if (adminId == null ||
                adminId <= 0) {

            throw new IllegalArgumentException(
                    "Valid admin ID is required."
            );
        }
    }


    // =========================================================
    // NORMALIZE DOMAIN
    // =========================================================

    private String normalizeDomain(
            String domain
    ) {

        if (domain == null ||
                domain.isBlank()) {

            throw new IllegalArgumentException(
                    "Website domain is required."
            );
        }


        String normalized =
                domain.trim()
                        .toLowerCase();


        // Remove HTTPS
        if (normalized.startsWith(
                "https://"
        )) {

            normalized =
                    normalized.substring(8);
        }


        // Remove HTTP
        if (normalized.startsWith(
                "http://"
        )) {

            normalized =
                    normalized.substring(7);
        }


        // Remove www.
        if (normalized.startsWith(
                "www."
        )) {

            normalized =
                    normalized.substring(4);
        }


        // Remove path
        int slashIndex =
                normalized.indexOf('/');

        if (slashIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            slashIndex
                    );
        }


        // Remove query
        int questionIndex =
                normalized.indexOf('?');

        if (questionIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            questionIndex
                    );
        }


        // Remove fragment
        int hashIndex =
                normalized.indexOf('#');

        if (hashIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            hashIndex
                    );
        }


        normalized =
                normalized.trim();


        if (normalized.isBlank()) {

            throw new IllegalArgumentException(
                    "Invalid website domain."
            );
        }


        return normalized;
    }


    // =========================================================
    // NORMALIZE OPTIONAL VALUE
    // =========================================================

    private String normalizeOptionalValue(
            String value
    ) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }


        return value.trim();
    }
}
