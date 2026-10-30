package com.example.TeamLoggerC.service;

import com.example.TeamLoggerC.entity.WebsitePermission;
import com.example.TeamLoggerC.repository.WebsitePermissionRepo;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class WebsitePermissionService {

    // =========================================================
    // CONSTANTS
    // =========================================================

    public static final String ALLOW = "ALLOW";
    public static final String BLOCK = "BLOCK";
    public static final String RESTRICT = "RESTRICT";

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";


    // =========================================================
    // REPOSITORY
    // =========================================================

    private final WebsitePermissionRepo websitePermissionRepo;


    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public WebsitePermissionService(
            WebsitePermissionRepo websitePermissionRepo
    ) {
        this.websitePermissionRepo =
                websitePermissionRepo;
    }


    // =========================================================
    // CREATE WEBSITE PERMISSION
    // =========================================================

    public WebsitePermission createPermission(
            Long policyId,
            String domain,
            String permission
    ) {

        validatePolicyId(policyId);

        String normalizedDomain =
                normalizeDomain(domain);

        String normalizedPermission =
                normalizePermission(permission);


        // =====================================================
        // CHECK DUPLICATE
        // =====================================================

        if (websitePermissionRepo
                .existsByPolicyIdAndDomain(
                        policyId,
                        normalizedDomain
                )) {

            throw new IllegalArgumentException(
                    "Permission for this website already exists."
            );
        }


        // =====================================================
        // CREATE
        // =====================================================

        WebsitePermission websitePermission =
                new WebsitePermission(
                        policyId,
                        normalizedDomain,
                        normalizedPermission
                );


        websitePermission.setStatus(ACTIVE);


        return websitePermissionRepo.save(
                websitePermission
        );
    }


    // =========================================================
    // GET ALL PERMISSIONS FOR POLICY
    // =========================================================

    @Transactional(readOnly = true)
    public List<WebsitePermission>
    getPermissionsForPolicy(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return websitePermissionRepo
                .findByPolicyIdOrderByDomainAsc(
                        policyId
                );
    }


    // =========================================================
    // GET ACTIVE PERMISSIONS FOR POLICY
    // =========================================================

    @Transactional(readOnly = true)
    public List<WebsitePermission>
    getActivePermissionsForPolicy(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return websitePermissionRepo
                .findByPolicyIdAndStatusOrderByDomainAsc(
                        policyId,
                        ACTIVE
                );
    }


    // =========================================================
    // GET PERMISSION BY ID + POLICY
    // =========================================================

    @Transactional(readOnly = true)
    public WebsitePermission getPermissionForPolicy(
            Long policyId,
            Long permissionId
    ) {

        validatePolicyId(policyId);

        if (permissionId == null) {

            throw new IllegalArgumentException(
                    "Permission ID is required."
            );
        }


        return websitePermissionRepo
                .findByIdAndPolicyId(
                        permissionId,
                        policyId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Website permission not found."
                        )
                );
    }


    // =========================================================
    // GET PERMISSION BY DOMAIN
    // =========================================================

    @Transactional(readOnly = true)
    public WebsitePermission getPermissionByDomain(
            Long policyId,
            String domain
    ) {

        validatePolicyId(policyId);

        String normalizedDomain =
                normalizeDomain(domain);


        return websitePermissionRepo
                .findByPolicyIdAndDomain(
                        policyId,
                        normalizedDomain
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Website permission not found."
                        )
                );
    }


    // =========================================================
    // UPDATE PERMISSION
    // =========================================================

    public WebsitePermission updatePermission(
            Long policyId,
            Long permissionId,
            String domain,
            String permission
    ) {

        validatePolicyId(policyId);

        WebsitePermission websitePermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );


        String normalizedDomain =
                normalizeDomain(domain);

        String normalizedPermission =
                normalizePermission(permission);


        // =====================================================
        // CHECK DOMAIN CHANGE
        // =====================================================

        if (!normalizedDomain.equals(
                websitePermission.getDomain()
        )) {

            if (websitePermissionRepo
                    .existsByPolicyIdAndDomainAndIdNot(
                            policyId,
                            normalizedDomain,
                            permissionId
                    )) {

                throw new IllegalArgumentException(
                        "Permission for this website already exists."
                );
            }
        }


        // =====================================================
        // UPDATE
        // =====================================================

        websitePermission.setDomain(
                normalizedDomain
        );

        websitePermission.setPermission(
                normalizedPermission
        );


        return websitePermissionRepo.save(
                websitePermission
        );
    }


    // =========================================================
    // CHANGE PERMISSION
    // =========================================================

    public WebsitePermission changePermission(
            Long policyId,
            Long permissionId,
            String permission
    ) {

        validatePolicyId(policyId);

        WebsitePermission websitePermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );


        String normalizedPermission =
                normalizePermission(permission);


        websitePermission.setPermission(
                normalizedPermission
        );

        websitePermission.setStatus(
                ACTIVE
        );


        return websitePermissionRepo.save(
                websitePermission
        );
    }


    // =========================================================
    // ACTIVATE PERMISSION
    // =========================================================

    public WebsitePermission activatePermission(
            Long policyId,
            Long permissionId
    ) {

        validatePolicyId(policyId);

        WebsitePermission websitePermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );


        websitePermission.setStatus(
                ACTIVE
        );


        return websitePermissionRepo.save(
                websitePermission
        );
    }


    // =========================================================
    // DEACTIVATE PERMISSION
    // =========================================================

    public WebsitePermission deactivatePermission(
            Long policyId,
            Long permissionId
    ) {

        validatePolicyId(policyId);

        WebsitePermission websitePermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );


        websitePermission.setStatus(
                INACTIVE
        );


        return websitePermissionRepo.save(
                websitePermission
        );
    }


    // =========================================================
    // DELETE PERMISSION
    // =========================================================

    public void deletePermission(
            Long policyId,
            Long permissionId
    ) {

        validatePolicyId(policyId);

        WebsitePermission websitePermission =
                getPermissionForPolicy(
                        policyId,
                        permissionId
                );


        websitePermissionRepo.delete(
                websitePermission
        );
    }


    // =========================================================
    // DELETE ALL PERMISSIONS FOR POLICY
    // =========================================================

    public void deletePermissionsForPolicy(
            Long policyId
    ) {

        validatePolicyId(policyId);

        websitePermissionRepo
                .deleteByPolicyId(
                        policyId
                );
    }


    // =========================================================
    // COUNT ALL PERMISSIONS
    // =========================================================

    @Transactional(readOnly = true)
    public long countPermissions(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return websitePermissionRepo
                .countByPolicyId(
                        policyId
                );
    }


    // =========================================================
    // COUNT ACTIVE PERMISSIONS
    // =========================================================

    @Transactional(readOnly = true)
    public long countActivePermissions(
            Long policyId
    ) {

        validatePolicyId(policyId);

        return websitePermissionRepo
                .countByPolicyIdAndStatus(
                        policyId,
                        ACTIVE
                );
    }


    // =========================================================
    // VALIDATE POLICY ID
    // =========================================================

    private void validatePolicyId(
            Long policyId
    ) {

        if (policyId == null ||
                policyId <= 0) {

            throw new IllegalArgumentException(
                    "Valid policy ID is required."
            );
        }
    }


    // =========================================================
    // NORMALIZE PERMISSION
    // =========================================================

    private String normalizePermission(
            String permission
    ) {

        if (permission == null ||
                permission.isBlank()) {

            return ALLOW;
        }


        String normalized =
                permission.trim()
                        .toUpperCase();


        if (!ALLOW.equals(normalized) &&
                !BLOCK.equals(normalized) &&
                !RESTRICT.equals(normalized)) {

            throw new IllegalArgumentException(
                    "Invalid website permission. " +
                            "Allowed values are ALLOW, BLOCK, or RESTRICT."
            );
        }


        return normalized;
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


        // =====================================================
        // REMOVE HTTPS
        // =====================================================

        if (normalized.startsWith(
                "https://"
        )) {

            normalized =
                    normalized.substring(8);
        }


        // =====================================================
        // REMOVE HTTP
        // =====================================================

        if (normalized.startsWith(
                "http://"
        )) {

            normalized =
                    normalized.substring(7);
        }


        // =====================================================
        // REMOVE WWW
        // =====================================================

        if (normalized.startsWith(
                "www."
        )) {

            normalized =
                    normalized.substring(4);
        }


        // =====================================================
        // REMOVE PATH
        // =====================================================

        int slashIndex =
                normalized.indexOf('/');

        if (slashIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            slashIndex
                    );
        }


        // =====================================================
        // REMOVE QUERY
        // =====================================================

        int questionIndex =
                normalized.indexOf('?');

        if (questionIndex >= 0) {

            normalized =
                    normalized.substring(
                            0,
                            questionIndex
                    );
        }


        // =====================================================
        // REMOVE FRAGMENT
        // =====================================================

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
}