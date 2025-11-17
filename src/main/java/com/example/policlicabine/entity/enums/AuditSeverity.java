package com.example.policlicabine.entity.enums;

/**
 * Severity level for security audit events.
 * Used for filtering, alerting, and prioritization.
 */
public enum AuditSeverity {
    /**
     * Informational events - normal system operations.
     * Examples: successful login, password change, user registration.
     */
    INFO,

    /**
     * Warning events - potentially suspicious but not critical.
     * Examples: single failed login, token expiration, minor violations.
     */
    WARNING,

    /**
     * Critical events - security threats or violations.
     * Examples: brute force attacks, unauthorized access attempts, injection attempts.
     */
    CRITICAL
}
