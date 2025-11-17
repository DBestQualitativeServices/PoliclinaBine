package com.example.policlicabine.service;

import com.example.policlicabine.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Service for managing retention policy of security audit logs.
 * Automatically deletes old audit logs based on configured retention period.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "security.audit.retention",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class SecurityAuditRetentionService {

    private final SecurityAuditLogRepository auditLogRepository;

    @Value("${security.audit.retention.days:90}")
    private int retentionDays;

    /**
     * Clean up old audit logs.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "${security.audit.retention.cron:0 0 2 * * *}")
    @Transactional
    public void cleanupOldAuditLogs() {
        try {
            log.info("Starting audit log retention cleanup (retention period: {} days)", retentionDays);

            OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(retentionDays);

            int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffDate);

            log.info("Audit log retention cleanup completed: {} records deleted (older than {})",
                deletedCount, cutoffDate);

        } catch (Exception e) {
            log.error("Failed to cleanup old audit logs", e);
            // Don't throw - cleanup failure shouldn't break application
        }
    }

    /**
     * Manually trigger cleanup (for admin operations).
     */
    @Transactional
    public int manualCleanup(int daysToKeep) {
        try {
            log.info("Manual audit log cleanup triggered (keep last {} days)", daysToKeep);

            OffsetDateTime cutoffDate = OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysToKeep);

            int deletedCount = auditLogRepository.deleteByTimestampBefore(cutoffDate);

            log.info("Manual cleanup completed: {} records deleted", deletedCount);

            return deletedCount;

        } catch (Exception e) {
            log.error("Failed to manually cleanup audit logs", e);
            throw new RuntimeException("Manual cleanup failed: " + e.getMessage(), e);
        }
    }
}
