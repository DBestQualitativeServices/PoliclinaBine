package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing a security audit log entry.
 * Stores both backend Spring Security events and frontend-originated security events.
 *
 * Integrates with Spring Boot Actuator's AuditEventRepository for unified audit logging.
 */
@Entity
@Table(name = "security_audit_logs", indexes = {
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_principal", columnList = "principal"),
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_severity", columnList = "severity")
})
@Getter
@Setter
public class SecurityAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;

    /**
     * Type of security event (LOGIN_SUCCESS, UNAUTHORIZED_ACCESS, etc.)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    /**
     * Severity level of the event (INFO, WARNING, CRITICAL)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AuditSeverity severity;

    /**
     * Principal (username) who triggered the event.
     * May be "anonymous" for unauthenticated requests.
     */
    @Column(name = "principal", length = 255)
    private String principal;

    /**
     * Timestamp when the event occurred (from frontend or backend).
     * Stored in UTC timezone.
     */
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    /**
     * User agent string from the client browser.
     */
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Full URL where the event occurred.
     */
    @Column(name = "url", columnDefinition = "TEXT")
    private String url;

    /**
     * Path portion of the URL (e.g., /admin, /api/patients).
     */
    @Column(name = "pathname", length = 500)
    private String pathname;

    /**
     * Role of the user at the time of the event (DOCTOR, ADMIN, etc.)
     */
    @Column(name = "user_role", length = 50)
    private String userRole;

    /**
     * User ID (UUID) if available.
     */
    @Column(name = "user_id", length = 50)
    private String userId;

    /**
     * Comma-separated list of roles allowed for the accessed resource.
     * Used for authorization mismatch events.
     */
    @Column(name = "allowed_roles", length = 255)
    private String allowedRoles;

    /**
     * Reason or description of the event.
     * Examples: "expired_on_load", "role_mismatch", "invalid_credentials"
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Error message if the event represents a failure.
     */
    @Column(name = "error", columnDefinition = "TEXT")
    private String error;

    /**
     * IP address of the client.
     */
    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Session ID if available.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Additional arbitrary data stored as JSON.
     * Allows flexible storage of event-specific information.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_data", columnDefinition = "jsonb")
    private Map<String, Object> additionalData = new HashMap<>();

    /**
     * Timestamp when the audit log entry was created in the database.
     * Always set to current UTC time on creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (auditId == null) {
            auditId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (timestamp == null) {
            timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    /**
     * Helper method to set allowed roles from array.
     */
    public void setAllowedRolesArray(String[] roles) {
        this.allowedRoles = roles != null && roles.length > 0 ? String.join(",", roles) : null;
    }

    /**
     * Helper method to get allowed roles as array.
     */
    public String[] getAllowedRolesArray() {
        return allowedRoles != null && !allowedRoles.isEmpty()
            ? allowedRoles.split(",")
            : new String[0];
    }

    /**
     * Add data to additional data map.
     */
    public void addAdditionalData(String key, Object value) {
        if (additionalData == null) {
            additionalData = new HashMap<>();
        }
        additionalData.put(key, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SecurityAuditLog that)) return false;
        return auditId != null && Objects.equals(auditId, that.auditId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SecurityAuditLog{" +
                "auditId=" + auditId +
                ", eventType=" + eventType +
                ", severity=" + severity +
                ", principal='" + principal + '\'' +
                ", timestamp=" + timestamp +
                ", userRole='" + userRole + '\'' +
                ", pathname='" + pathname + '\'' +
                '}';
    }
}
