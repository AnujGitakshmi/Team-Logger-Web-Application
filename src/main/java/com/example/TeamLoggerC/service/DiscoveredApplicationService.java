package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.DiscoveredApplication;
import com.example.TeamLoggerC.repository.DiscoveredApplicationRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class DiscoveredApplicationService {

    private static final String ACTIVE = "ACTIVE";
    private static final String INACTIVE = "INACTIVE";

    private final DiscoveredApplicationRepo
            discoveredApplicationRepo;


    public DiscoveredApplicationService(
            DiscoveredApplicationRepo discoveredApplicationRepo
    ) {
        this.discoveredApplicationRepo =
                discoveredApplicationRepo;
    }


    // =========================================================
    // DISCOVER / REGISTER APPLICATION
    // =========================================================

    public DiscoveredApplication discoverApplication(
            Integer adminId,
            String applicationName,
            String executableName,
            String publisher,
            String version
    ) {

        validateAdminId(adminId);

        String normalizedApplicationName =
                normalizeRequired(
                        applicationName,
                        "Application name"
                );

        String normalizedExecutableName =
                normalizeRequired(
                        executableName,
                        "Executable name"
                );

        String normalizedPublisher =
                normalizeOptional(publisher);

        String normalizedVersion =
                normalizeOptional(version);


        /*
         * Check whether this application has already
         * been discovered for this admin.
         */
        DiscoveredApplication application =
                discoveredApplicationRepo
                        .findByAdminIdAndApplicationNameAndExecutableName(
                                adminId,
                                normalizedApplicationName,
                                normalizedExecutableName
                        )
                        .orElse(null);


        LocalDateTime now =
                LocalDateTime.now();


        /*
         * Existing application
         */
        if (application != null) {

            application.setPublisher(
                    normalizedPublisher
            );

            application.setVersion(
                    normalizedVersion
            );

            Integer currentCount =
                    application.getDiscoveryCount();

            if (currentCount == null) {
                currentCount = 0;
            }

            application.setDiscoveryCount(
                    currentCount + 1
            );

            application.setLastSeenAt(now);

            /*
             * If an application becomes visible again,
             * make sure it is active.
             */
            application.setStatus(ACTIVE);

            return discoveredApplicationRepo.save(
                    application
            );
        }


        /*
         * New application
         */
        application =
                new DiscoveredApplication(
                        adminId,
                        normalizedApplicationName,
                        normalizedExecutableName
                );

        application.setPublisher(
                normalizedPublisher
        );

        application.setVersion(
                normalizedVersion
        );

        application.setDiscoveryCount(1);

        application.setFirstSeenAt(now);

        application.setLastSeenAt(now);

        application.setStatus(ACTIVE);


        return discoveredApplicationRepo.save(
                application
        );
    }


    // =========================================================
    // GET ALL APPLICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredApplication>
    getApplicationsForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredApplicationRepo
                .findByAdminIdOrderByApplicationNameAsc(
                        adminId
                );
    }


    // =========================================================
    // GET ACTIVE APPLICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredApplication>
    getActiveApplicationsForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredApplicationRepo
                .findByAdminIdAndStatusOrderByApplicationNameAsc(
                        adminId,
                        ACTIVE
                );
    }


    // =========================================================
    // GET APPLICATION BY ID
    // =========================================================

    @Transactional(readOnly = true)
    public DiscoveredApplication
    getApplicationForAdmin(
            Integer adminId,
            Long applicationId
    ) {

        validateAdminId(adminId);

        if (applicationId == null) {
            throw new IllegalArgumentException(
                    "Application ID is required."
            );
        }

        return discoveredApplicationRepo
                .findByIdAndAdminId(
                        applicationId,
                        adminId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Application not found."
                        )
                );
    }


    // =========================================================
    // SEARCH APPLICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<DiscoveredApplication>
    searchApplications(
            Integer adminId,
            String query
    ) {

        validateAdminId(adminId);

        String normalizedQuery =
                normalizeOptional(query);


        /*
         * Empty search should return all applications.
         */
        if (normalizedQuery == null) {

            return getApplicationsForAdmin(
                    adminId
            );
        }


        return discoveredApplicationRepo
                .findByAdminIdAndApplicationNameContainingIgnoreCaseOrderByApplicationNameAsc(
                        adminId,
                        normalizedQuery
                );
    }


    // =========================================================
    // ACTIVATE APPLICATION
    // =========================================================

    public DiscoveredApplication
    activateApplication(
            Integer adminId,
            Long applicationId
    ) {

        DiscoveredApplication application =
                getApplicationForAdmin(
                        adminId,
                        applicationId
                );

        application.setStatus(ACTIVE);

        return discoveredApplicationRepo.save(
                application
        );
    }


    // =========================================================
    // DEACTIVATE APPLICATION
    // =========================================================

    public DiscoveredApplication
    deactivateApplication(
            Integer adminId,
            Long applicationId
    ) {

        DiscoveredApplication application =
                getApplicationForAdmin(
                        adminId,
                        applicationId
                );

        application.setStatus(INACTIVE);

        return discoveredApplicationRepo.save(
                application
        );
    }


    // =========================================================
    // COUNT ALL APPLICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public long countApplications(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredApplicationRepo
                .countByAdminId(adminId);
    }


    // =========================================================
    // COUNT ACTIVE APPLICATIONS
    // =========================================================

    @Transactional(readOnly = true)
    public long countActiveApplications(
            Integer adminId
    ) {

        validateAdminId(adminId);

        return discoveredApplicationRepo
                .countByAdminIdAndStatus(
                        adminId,
                        ACTIVE
                );
    }


    // =========================================================
    // DELETE ALL APPLICATIONS FOR ADMIN
    // =========================================================

    public void deleteApplicationsForAdmin(
            Integer adminId
    ) {

        validateAdminId(adminId);

        discoveredApplicationRepo.deleteByAdminId(
                adminId
        );
    }


    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateAdminId(
            Integer adminId
    ) {

        if (adminId == null) {
            throw new IllegalArgumentException(
                    "Admin ID is required."
            );
        }

        if (adminId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid admin ID."
            );
        }
    }


    // =========================================================
    // REQUIRED STRING NORMALIZATION
    // =========================================================

    private String normalizeRequired(
            String value,
            String fieldName
    ) {

        if (value == null ||
                value.trim().isEmpty()) {

            throw new IllegalArgumentException(
                    fieldName + " is required."
            );
        }

        return value.trim();
    }


    // =========================================================
    // OPTIONAL STRING NORMALIZATION
    // =========================================================

    private String normalizeOptional(
            String value
    ) {

        if (value == null) {
            return null;
        }

        String normalized =
                value.trim();

        return normalized.isEmpty()
                ? null
                : normalized;
    }
}