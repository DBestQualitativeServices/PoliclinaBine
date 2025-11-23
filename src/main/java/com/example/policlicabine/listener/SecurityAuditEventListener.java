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

@Component
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditEventListener {

    private final SecurityAuditService auditService;

    @Autowired(required = false)
    private ApplicationInsightsService appInsightsService;

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

    @EventListener
    public void onUserRegistered(UserRegistered event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.USER_REGISTERED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .userRole(event.roles().stream().findFirst().map(Enum::name).orElse("UNKNOWN"))
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("New user registered in the system")
            .build();

        logToAuditTrail(auditLog);
    }

    @EventListener
    public void onUserCreated(UserCreated event) {
        SecurityAuditLogDto auditLog = SecurityAuditLogDto.builder()
            .eventType(AuditEventType.USER_CREATED)
            .severity(AuditSeverity.INFO)
            .principal(event.username())
            .userId(event.userId().toString())
            .userRole(event.roles().stream().findFirst().map(Enum::name).orElse("UNKNOWN"))
            .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
            .reason("User account created")
            .build();

        logToAuditTrail(auditLog);
    }

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

    private void logToAuditTrail(SecurityAuditLogDto auditLogDto) {
        // Log to database (persistent audit trail) and get saved entity
        SecurityAuditLog savedLog = auditService.logEventAndReturnEntity(auditLogDto);

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
