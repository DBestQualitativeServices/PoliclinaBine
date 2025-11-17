package com.example.policlicabine.service;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Service for monitoring security events and sending alerts.
 * Detects suspicious patterns and notifies administrators.
 *
 * NOTE: Email sending is stubbed out - integrate with your mail service when ready.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "security.audit.alert",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SecurityAlertService {

    private final SecurityAuditLogRepository auditLogRepository;

    // Rate limiting: track recent alerts to prevent spam
    private final ConcurrentHashMap<String, Long> recentAlerts = new ConcurrentHashMap<>();

    @Value("${security.audit.alert.email:admin@example.com}")
    private String alertEmail;

    @Value("${security.audit.alert.threshold.failed-logins:5}")
    private int failedLoginThreshold;

    @Value("${security.audit.alert.threshold.unauthorized-access:3}")
    private int unauthorizedAccessThreshold;

    @Value("${security.audit.alert.window-minutes:10}")
    private int alertWindowMinutes;

    /**
     * Listen to audit log events and check for suspicious patterns.
     */
    @EventListener
    @Async("securityAlertExecutor")
    public void onSecurityAuditLog(SecurityAuditLogDto auditLog) {
        try {
            // Check for critical events
            if (AuditSeverity.CRITICAL.equals(auditLog.getSeverity())) {
                sendCriticalEventAlert(auditLog);
            }

            // Check for brute force attacks (multiple failed logins)
            if (AuditEventType.LOGIN_FAILURE.equals(auditLog.getEventType())) {
                checkBruteForceAttempt(auditLog);
            }

            // Check for repeated unauthorized access
            if (AuditEventType.UNAUTHORIZED_ACCESS.equals(auditLog.getEventType())) {
                checkUnauthorizedAccessPattern(auditLog);
            }

            // Check for token refresh failures (potential attack)
            if (AuditEventType.REFRESH_FAILED.equals(auditLog.getEventType())) {
                checkTokenRefreshFailures(auditLog);
            }

        } catch (Exception e) {
            log.error("Failed to process security alert", e);
            // Don't throw - alerting failure shouldn't break audit logging
        }
    }

    /**
     * Check for brute force login attempts.
     */
    private void checkBruteForceAttempt(SecurityAuditLogDto auditLog) {
        if (auditLog.getPrincipal() == null) {
            return;
        }

        // Count failed login attempts in last N minutes
        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(alertWindowMinutes);

        long failedAttempts = auditLogRepository.countEventsByPrincipalSince(
            auditLog.getPrincipal(),
            AuditEventType.LOGIN_FAILURE,
            since
        );

        if (failedAttempts >= failedLoginThreshold) {
            String alertKey = "brute_force_" + auditLog.getPrincipal();
            if (shouldSendAlert(alertKey)) {
                sendAlert(
                    "Brute Force Attack Detected",
                    String.format(
                        "User '%s' has %d failed login attempts in the last %d minutes.\n" +
                        "IP Address: %s\n" +
                        "User Agent: %s",
                        auditLog.getPrincipal(),
                        failedAttempts,
                        alertWindowMinutes,
                        auditLog.getIpAddress(),
                        auditLog.getUserAgent()
                    )
                );
            }
        }
    }

    /**
     * Check for repeated unauthorized access attempts.
     */
    private void checkUnauthorizedAccessPattern(SecurityAuditLogDto auditLog) {
        if (auditLog.getIpAddress() == null) {
            return;
        }

        OffsetDateTime since = OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(alertWindowMinutes);

        long unauthorizedAttempts = auditLogRepository.countEventsByIpAddressSince(
            auditLog.getIpAddress(),
            AuditEventType.UNAUTHORIZED_ACCESS,
            since
        );

        if (unauthorizedAttempts >= unauthorizedAccessThreshold) {
            String alertKey = "unauthorized_" + auditLog.getIpAddress();
            if (shouldSendAlert(alertKey)) {
                sendAlert(
                    "Repeated Unauthorized Access Attempts",
                    String.format(
                        "IP address '%s' has %d unauthorized access attempts in the last %d minutes.\n" +
                        "User: %s\n" +
                        "Latest path: %s\n" +
                        "Allowed roles: %s\n" +
                        "User role: %s",
                        auditLog.getIpAddress(),
                        unauthorizedAttempts,
                        alertWindowMinutes,
                        auditLog.getPrincipal(),
                        auditLog.getPathname(),
                        auditLog.getAllowedRoles() != null ? String.join(", ", auditLog.getAllowedRoles()) : "N/A",
                        auditLog.getUserRole()
                    )
                );
            }
        }
    }

    /**
     * Check for token refresh failures (potential token theft).
     */
    private void checkTokenRefreshFailures(SecurityAuditLogDto auditLog) {
        String alertKey = "token_refresh_failed_" + (auditLog.getPrincipal() != null ? auditLog.getPrincipal() : "anonymous");

        if (shouldSendAlert(alertKey)) {
            sendAlert(
                "Token Refresh Failure Detected",
                String.format(
                    "Token refresh failed for user: %s\n" +
                    "Reason: %s\n" +
                    "IP Address: %s\n" +
                    "This could indicate token theft or session hijacking.",
                    auditLog.getPrincipal(),
                    auditLog.getReason(),
                    auditLog.getIpAddress()
                )
            );
        }
    }

    /**
     * Send alert for critical events immediately.
     */
    private void sendCriticalEventAlert(SecurityAuditLogDto auditLog) {
        String alertKey = "critical_" + auditLog.getEventType() + "_" + System.currentTimeMillis();

        sendAlert(
            "CRITICAL Security Event: " + auditLog.getEventType(),
            String.format(
                "A critical security event has been detected:\n\n" +
                "Event Type: %s\n" +
                "User: %s\n" +
                "IP Address: %s\n" +
                "Path: %s\n" +
                "Reason: %s\n" +
                "Error: %s\n" +
                "Timestamp: %s",
                auditLog.getEventType(),
                auditLog.getPrincipal(),
                auditLog.getIpAddress(),
                auditLog.getPathname(),
                auditLog.getReason(),
                auditLog.getError(),
                auditLog.getTimestamp()
            )
        );
    }

    /**
     * Check if alert should be sent (rate limiting).
     * Returns true if alert hasn't been sent recently for this key.
     */
    private boolean shouldSendAlert(String alertKey) {
        long now = System.currentTimeMillis();
        Long lastAlertTime = recentAlerts.get(alertKey);

        // Don't send same alert more than once per hour
        if (lastAlertTime != null && (now - lastAlertTime) < TimeUnit.HOURS.toMillis(1)) {
            log.debug("Skipping duplicate alert: {} (sent {} ms ago)", alertKey, now - lastAlertTime);
            return false;
        }

        recentAlerts.put(alertKey, now);
        return true;
    }

    /**
     * Send alert email.
     * NOTE: This is a stub - integrate with your actual email service (JavaMailSender, SendGrid, etc.)
     */
    private void sendAlert(String subject, String body) {
        // TODO: Integrate with email service
        // Example with JavaMailSender:
        // SimpleMailMessage message = new SimpleMailMessage();
        // message.setTo(alertEmail);
        // message.setSubject("[SECURITY ALERT] " + subject);
        // message.setText(body);
        // mailSender.send(message);

        // For now, just log the alert
        log.warn("=================================================");
        log.warn("SECURITY ALERT: {}", subject);
        log.warn("To: {}", alertEmail);
        log.warn("-------------------------------------------------");
        log.warn("{}", body);
        log.warn("=================================================");
    }
}
