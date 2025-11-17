package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for SecurityAuditLog entity.
 * Used for API responses and frontend communication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditLogDto {

    private UUID auditId;
    private AuditEventType eventType;
    private AuditSeverity severity;
    private String principal;
    private OffsetDateTime timestamp;
    private String userAgent;
    private String url;
    private String pathname;
    private String userRole;
    private String userId;
    private String[] allowedRoles;
    private String reason;
    private String error;
    private String ipAddress;
    private String sessionId;
    private Map<String, Object> additionalData;
    private OffsetDateTime createdAt;

    // Compatibility fields for frontend payload (incoming events from frontend)
    private String event;  // Maps to eventType
}
