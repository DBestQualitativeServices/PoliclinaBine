package com.example.policlicabine.listener;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.event.*;
import com.example.policlicabine.service.ApplicationInsightsService;
import com.example.policlicabine.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

/**
 * Event listener for security-related domain events.
 * Automatically logs security events to the audit trail.
 *
 * <p>Dual tracking:
 * <ul>
 *   <li>Database: Via SecurityAuditService (persistent audit trail)</li>
 *   <li>Azure Application Insights: For real-time monitoring and analytics</li>
 * </ul>
 *
 * Listens to existing security events and creates audit log entries asynchronously.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditEventListener {

    private final SecurityAuditService auditService;

    @Autowired(required = false)
    private ApplicationInsightsService appInsightsService;

    /**
     * Listen to UserAuthenticated events (successful login).
     */
    @EventListener
    public void onUserAuthenticated(UserAuthenticated event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.LOGIN_SUCCESS)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .timestamp(event.loginTime())
            .ipAddress(event.ipAddress())
            .reason("User successfully authenticated")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Listen to UserRegistered events.
     */
    @EventListener
    public void onUserRegistered(UserRegistered event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.USER_REGISTERED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .userRole(event.role().name())
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("New user registered in the system")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Listen to UserCreated events.
     */
    @EventListener
    public void onUserCreated(UserCreated event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.USER_CREATED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .userRole(event.role().name())
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("User account created")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Listen to PasswordChanged events.
     */
    @EventListener
    public void onPasswordChanged(PasswordChanged event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.PASSWORD_CHANGED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("User password changed successfully")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Listen to PasswordResetInitiated events.
     */
    @EventListener
    public void onPasswordResetInitiated(PasswordResetInitiated event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.PASSWORD_RESET_REQUESTED)
            .severity(AuditSeverity.WARNING)
            .principal(event.username())
            .userId(event.userId().toString())
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("Password reset requested")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Listen to PasswordReset events (completion).
     */
    @EventListener
    public void onPasswordReset(PasswordReset event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.PASSWORD_RESET_COMPLETED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("Password reset completed successfully")
            .build();

        logToAuditTrail(auditLog);
    }

    /**
     * Helper method to log security events to both database and Application Insights.
     *
     * @param auditLogDto The audit log DTO to persist and track
     */
    private void logToAuditTrail(SecurityAuditLogDto auditLogDto) {
        // Log to database (persistent audit trail)
        SecurityAuditLog savedLog = auditService.logEventAsync(auditLogDto);

        // Track in Azure Application Insights (real-time monitoring)
        if (appInsightsService != null && savedLog != null) {
            try {
                appInsightsService.trackSecurityEvent(savedLog);
            } catch (Exception e) {
                log.warn("Failed to track security event in Application Insights: {}", e.getMessage());
            }
        }
    }
}
