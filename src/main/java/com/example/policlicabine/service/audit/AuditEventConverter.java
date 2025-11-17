package com.example.policlicabine.service.audit;

import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

/**
 * Bidirectional converter between Spring Boot's AuditEvent and our SecurityAuditLog entity.
 * Enables integration with Spring Boot Actuator's audit events system.
 */
@Component
public class AuditEventConverter {

    /**
     * Convert Spring Boot AuditEvent to SecurityAuditLog entity.
     */
    public SecurityAuditLog toEntity(AuditEvent auditEvent) {
        SecurityAuditLog entity = new SecurityAuditLog();

        // Basic fields
        entity.setPrincipal(auditEvent.getPrincipal());
        entity.setTimestamp(OffsetDateTime.ofInstant(auditEvent.getTimestamp(), ZoneOffset.UTC));

        // Parse event type
        entity.setEventType(parseEventType(auditEvent.getType()));

        // Determine severity based on event type
        entity.setSeverity(determineSeverity(entity.getEventType()));

        // Extract data from audit event
        Map<String, Object> data = auditEvent.getData();
        if (data != null && !data.isEmpty()) {
            extractDataFields(entity, data);
        }

        return entity;
    }

    /**
     * Convert SecurityAuditLog entity to Spring Boot AuditEvent.
     */
    public AuditEvent toAuditEvent(SecurityAuditLog entity) {
        String principal = entity.getPrincipal() != null ? entity.getPrincipal() : "anonymous";
        String type = entity.getEventType() != null ? entity.getEventType().name() : "CUSTOM_EVENT";
        Instant timestamp = entity.getTimestamp().toInstant();

        Map<String, Object> data = new HashMap<>();

        // Add all relevant fields to data map
        addIfNotNull(data, "severity", entity.getSeverity());
        addIfNotNull(data, "userAgent", entity.getUserAgent());
        addIfNotNull(data, "url", entity.getUrl());
        addIfNotNull(data, "pathname", entity.getPathname());
        addIfNotNull(data, "userRole", entity.getUserRole());
        addIfNotNull(data, "userId", entity.getUserId());
        addIfNotNull(data, "allowedRoles", entity.getAllowedRoles());
        addIfNotNull(data, "reason", entity.getReason());
        addIfNotNull(data, "error", entity.getError());
        addIfNotNull(data, "ipAddress", entity.getIpAddress());
        addIfNotNull(data, "sessionId", entity.getSessionId());

        // Add additional data if present
        if (entity.getAdditionalData() != null && !entity.getAdditionalData().isEmpty()) {
            data.putAll(entity.getAdditionalData());
        }

        return new AuditEvent(timestamp, principal, type, data);
    }

    /**
     * Parse event type string to enum.
     */
    private AuditEventType parseEventType(String type) {
        if (type == null || type.isEmpty()) {
            return AuditEventType.CUSTOM_EVENT;
        }

        try {
            return AuditEventType.valueOf(type);
        } catch (IllegalArgumentException e) {
            // Map common Spring Security event types
            return switch (type) {
                case "AUTHENTICATION_SUCCESS" -> AuditEventType.LOGIN_SUCCESS;
                case "AUTHENTICATION_FAILURE" -> AuditEventType.LOGIN_FAILURE;
                case "AUTHORIZATION_FAILURE" -> AuditEventType.UNAUTHORIZED_ACCESS;
                case "LOGOUT_SUCCESS" -> AuditEventType.LOGOUT;
                default -> AuditEventType.CUSTOM_EVENT;
            };
        }
    }

    /**
     * Determine severity based on event type.
     */
    private AuditSeverity determineSeverity(AuditEventType eventType) {
        return switch (eventType) {
            case BRUTE_FORCE_DETECTED, SUSPICIOUS_ACTIVITY, SQL_INJECTION_ATTEMPT,
                 XSS_ATTEMPT, UNAUTHORIZED_DATA_EXPORT -> AuditSeverity.CRITICAL;
            case LOGIN_FAILURE, UNAUTHORIZED_ACCESS, FORBIDDEN_ACCESS, TOKEN_EXPIRED,
                 REFRESH_FAILED, PASSWORD_RESET_FAILED, USER_LOCKED -> AuditSeverity.WARNING;
            default -> AuditSeverity.INFO;
        };
    }

    /**
     * Extract data fields from AuditEvent data map into entity.
     */
    private void extractDataFields(SecurityAuditLog entity, Map<String, Object> data) {
        // Extract known fields
        entity.setIpAddress(getStringValue(data, "ipAddress"));
        entity.setUserAgent(getStringValue(data, "userAgent"));
        entity.setUrl(getStringValue(data, "url"));
        entity.setPathname(getStringValue(data, "pathname"));
        entity.setUserRole(getStringValue(data, "userRole"));
        entity.setUserId(getStringValue(data, "userId"));
        entity.setReason(getStringValue(data, "reason"));
        entity.setError(getStringValue(data, "error"));
        entity.setSessionId(getStringValue(data, "sessionId"));
        entity.setAllowedRoles(getStringValue(data, "allowedRoles"));

        // Parse severity if provided
        String severityStr = getStringValue(data, "severity");
        if (severityStr != null) {
            try {
                entity.setSeverity(AuditSeverity.valueOf(severityStr));
            } catch (IllegalArgumentException ignored) {
                // Keep default severity
            }
        }

        // Store remaining data in additionalData
        Map<String, Object> additionalData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            if (!isKnownField(key)) {
                additionalData.put(key, entry.getValue());
            }
        }
        if (!additionalData.isEmpty()) {
            entity.setAdditionalData(additionalData);
        }
    }

    /**
     * Get string value from data map.
     */
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * Check if field is a known entity field.
     */
    private boolean isKnownField(String key) {
        return switch (key) {
            case "ipAddress", "userAgent", "url", "pathname", "userRole",
                 "userId", "reason", "error", "sessionId", "allowedRoles", "severity" -> true;
            default -> false;
        };
    }

    /**
     * Add value to map if not null.
     */
    private void addIfNotNull(Map<String, Object> map, String key, Object value) {
        if (value != null) {
            map.put(key, value);
        }
    }
}
