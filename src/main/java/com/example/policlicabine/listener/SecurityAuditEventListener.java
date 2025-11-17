package com.example.policlicabine.listener;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.event.*;
import com.example.policlicabine.service.SecurityAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Event listener for security-related domain events.
 * Automatically logs security events to the audit trail.
 *
 * Listens to existing security events and creates audit log entries asynchronously.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditEventListener {

    private final SecurityAuditService auditService;

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

        auditService.logEventAsync(auditLog);
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

        auditService.logEventAsync(auditLog);
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

        auditService.logEventAsync(auditLog);
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

        auditService.logEventAsync(auditLog);
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

        auditService.logEventAsync(auditLog);
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

        auditService.logEventAsync(auditLog);
    }

    // Add more event listeners for other security events as needed
    // Examples: SessionStarted, SessionCompleted, etc.
}
