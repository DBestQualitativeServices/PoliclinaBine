package com.example.policlicabine.entity.enums;

/**
 * Enumeration of security audit event types.
 * Covers both backend Spring Security events and frontend-originated events.
 */
public enum AuditEventType {
    // Authentication Events
    LOGIN_SUCCESS,
    LOGIN_FAILURE,
    LOGOUT,
    TOKEN_EXPIRED,
    TOKEN_REFRESHED,
    REFRESH_FAILED,

    // Authorization Events
    UNAUTHORIZED_ACCESS,
    FORBIDDEN_ACCESS,
    ROLE_MISMATCH,
    UNDEFINED_ROUTE_ACCESS,

    // Password Events
    PASSWORD_CHANGED,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_RESET_COMPLETED,
    PASSWORD_RESET_FAILED,

    // User Management Events
    USER_REGISTERED,
    USER_CREATED,
    USER_UPDATED,
    USER_DELETED,
    USER_ENABLED,
    USER_DISABLED,
    USER_LOCKED,
    USER_UNLOCKED,

    // Session Events
    SESSION_CREATED,
    SESSION_EXPIRED,
    SESSION_TERMINATED,
    CONCURRENT_SESSION_DETECTED,

    // Security Violations
    BRUTE_FORCE_DETECTED,
    SUSPICIOUS_ACTIVITY,
    INVALID_TOKEN,
    CSRF_VIOLATION,
    SQL_INJECTION_ATTEMPT,
    XSS_ATTEMPT,

    // Data Access Events
    SENSITIVE_DATA_ACCESS,
    MEDICAL_FILE_ACCESS,
    UNAUTHORIZED_DATA_EXPORT,

    // System Events
    SECURITY_CONFIG_CHANGED,
    AUDIT_LOG_ACCESSED,
    AUDIT_LOG_EXPORTED,
    ACTUATOR_ACCESS,

    // Generic
    CUSTOM_EVENT
}
