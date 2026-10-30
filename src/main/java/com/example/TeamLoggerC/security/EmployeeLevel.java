package com.example.TeamLoggerC.security.rbac;


/**
 * Canonical RBAC hierarchy levels used by TeamLogger.
 *
 * These levels are independent from:
 *
 * 1. Spring Security role
 *    ADMIN / EMPLOYEE
 *
 * 2. Domain-specific designation
 *    Senior Developer
 *    Senior QA Engineer
 *    Senior HR
 *    etc.
 *
 * The designation determines the EmployeeLevel.
 */
public enum EmployeeLevel {

    /**
     * Highest employee hierarchy level.
     *
     * Examples:
     * Project Leader
     * QA Lead
     * HR Head
     * Finance Lead
     * UI/UX Lead
     */
    PROJECT_LEADER,


    /**
     * Senior-level employee.
     */
    SENIOR,


    /**
     * Junior-level employee.
     */
    JUNIOR,


    /**
     * Intern-level employee.
     */
    INTERN
}

