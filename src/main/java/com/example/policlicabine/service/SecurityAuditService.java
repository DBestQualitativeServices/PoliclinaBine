package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.dto.SecurityAuditSearchDto;
import com.example.policlicabine.dto.SecurityAuditStatsDto;
import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.mapper.SecurityAuditLogMapper;
import com.example.policlicabine.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing security audit logs.
 * Handles logging, searching, and statistics for security events.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class SecurityAuditService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final SecurityAuditLogMapper auditLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Log a security event asynchronously.
     * Called from frontend POST /api/security/log and internal event listeners.
     *
     * @param dto Security audit log DTO
     * @return Result indicating success or failure
     */
    @Async("auditLogExecutor")
    public void logEventAsync(SecurityAuditLogDto dto) {
        try {
            SecurityAuditLog entity = auditLogMapper.toEntity(dto);

            // Set default severity if not provided
            if (entity.getSeverity() == null) {
                entity.setSeverity(determineSeverity(entity.getEventType()));
            }

            // Ensure timestamp is in UTC
            if (entity.getTimestamp() != null) {
                entity.setTimestamp(entity.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC));
            }

            // Save to database
            SecurityAuditLog saved = auditLogRepository.save(entity);

            log.debug("Audit event logged: {} | Principal: {} | Severity: {}",
                saved.getEventType(), saved.getPrincipal(), saved.getSeverity());

            // Publish event for alerting (don't wait for result)
            publishAuditEventForAlerting(saved);

        } catch (Exception e) {
            log.error("Failed to log security event asynchronously", e);
            // Don't throw - audit logging failure shouldn't break application
        }
    }

    /**
     * Log a security event synchronously (for cases where immediate persistence is needed).
     */
    @Transactional
    public Result<SecurityAuditLogDto> logEvent(SecurityAuditLogDto dto) {
        try {
            SecurityAuditLog entity = auditLogMapper.toEntity(dto);

            // Set default severity if not provided
            if (entity.getSeverity() == null) {
                entity.setSeverity(determineSeverity(entity.getEventType()));
            }

            // Ensure timestamp is in UTC
            if (entity.getTimestamp() != null) {
                entity.setTimestamp(entity.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC));
            }

            SecurityAuditLog saved = auditLogRepository.save(entity);

            log.info("Audit event logged: {} | Principal: {} | Severity: {}",
                saved.getEventType(), saved.getPrincipal(), saved.getSeverity());

            // Publish event for alerting
            publishAuditEventForAlerting(saved);

            return Result.success(auditLogMapper.toDto(saved));

        } catch (Exception e) {
            log.error("Failed to log security event", e);
            return Result.failure("Failed to log security event: " + e.getMessage());
        }
    }

    /**
     * Find audit log by ID.
     */
    @Transactional(readOnly = true)
    public Result<SecurityAuditLogDto> findById(UUID auditId) {
        return auditLogRepository.findById(auditId)
            .map(entity -> Result.success(auditLogMapper.toDto(entity)))
            .orElse(Result.failure("Security audit log not found with ID: " + auditId));
    }

    /**
     * Search audit logs with filters and pagination.
     */
    @Transactional(readOnly = true)
    public Result<Page<SecurityAuditLogDto>> search(SecurityAuditSearchDto searchDto) {
        try {
            // Build pageable
            int page = searchDto.getPage() != null ? searchDto.getPage() : 0;
            int size = searchDto.getSize() != null ? searchDto.getSize() : 20;
            String sortBy = searchDto.getSortBy() != null ? searchDto.getSortBy() : "timestamp";
            Sort.Direction direction = "asc".equalsIgnoreCase(searchDto.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // Execute search
            Page<SecurityAuditLog> results = auditLogRepository.searchAuditLogs(
                searchDto.getEventType(),
                searchDto.getSeverity(),
                searchDto.getPrincipal(),
                searchDto.getUserId(),
                searchDto.getAfter(),
                searchDto.getBefore(),
                pageable
            );

            // Convert to DTOs
            Page<SecurityAuditLogDto> dtoPage = results.map(auditLogMapper::toDto);

            return Result.success(dtoPage);

        } catch (Exception e) {
            log.error("Failed to search audit logs", e);
            return Result.failure("Failed to search audit logs: " + e.getMessage());
        }
    }

    /**
     * Get statistics for security audit logs.
     */
    @Transactional(readOnly = true)
    public Result<SecurityAuditStatsDto> getStatistics(OffsetDateTime after, OffsetDateTime before) {
        try {
            // Default time range if not provided (last 7 days)
            if (after == null) {
                after = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
            }

            // Count by event type
            List<Object[]> eventTypeCounts = auditLogRepository.countByEventType();
            Map<String, Long> eventTypeMap = eventTypeCounts.stream()
                .collect(Collectors.toMap(
                    row -> ((AuditEventType) row[0]).name(),
                    row -> (Long) row[1]
                ));

            // Count by severity
            List<Object[]> severityCounts = auditLogRepository.countBySeverity();
            Map<String, Long> severityMap = severityCounts.stream()
                .collect(Collectors.toMap(
                    row -> ((AuditSeverity) row[0]).name(),
                    row -> (Long) row[1]
                ));

            // Count recent events
            List<SecurityAuditLog> recentEvents = auditLogRepository.findRecentEvents(after);
            long totalEvents = recentEvents.size();

            // Calculate specific counts
            long criticalEvents = severityMap.getOrDefault("CRITICAL", 0L);
            long warningEvents = severityMap.getOrDefault("WARNING", 0L);
            long infoEvents = severityMap.getOrDefault("INFO", 0L);

            long failedLoginAttempts = eventTypeMap.getOrDefault("LOGIN_FAILURE", 0L);
            long unauthorizedAccessAttempts = eventTypeMap.getOrDefault("UNAUTHORIZED_ACCESS", 0L);
            long suspiciousActivities = eventTypeMap.getOrDefault("SUSPICIOUS_ACTIVITY", 0L)
                + eventTypeMap.getOrDefault("BRUTE_FORCE_DETECTED", 0L);

            // Top principals
            Map<String, Long> topPrincipals = recentEvents.stream()
                .filter(e -> e.getPrincipal() != null)
                .collect(Collectors.groupingBy(
                    SecurityAuditLog::getPrincipal,
                    Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));

            String period = determinePeriodDescription(after, before);

            SecurityAuditStatsDto stats = SecurityAuditStatsDto.builder()
                .totalEvents(totalEvents)
                .eventTypeCounts(eventTypeMap)
                .severityCounts(severityMap)
                .criticalEvents(criticalEvents)
                .warningEvents(warningEvents)
                .infoEvents(infoEvents)
                .failedLoginAttempts(failedLoginAttempts)
                .unauthorizedAccessAttempts(unauthorizedAccessAttempts)
                .suspiciousActivities(suspiciousActivities)
                .topPrincipals(topPrincipals)
                .period(period)
                .build();

            return Result.success(stats);

        } catch (Exception e) {
            log.error("Failed to get audit statistics", e);
            return Result.failure("Failed to get statistics: " + e.getMessage());
        }
    }

    /**
     * Determine severity based on event type.
     */
    private AuditSeverity determineSeverity(AuditEventType eventType) {
        if (eventType == null) {
            return AuditSeverity.INFO;
        }

        return switch (eventType) {
            case BRUTE_FORCE_DETECTED, SUSPICIOUS_ACTIVITY, SQL_INJECTION_ATTEMPT,
                 XSS_ATTEMPT, UNAUTHORIZED_DATA_EXPORT, CSRF_VIOLATION -> AuditSeverity.CRITICAL;
            case LOGIN_FAILURE, UNAUTHORIZED_ACCESS, FORBIDDEN_ACCESS, TOKEN_EXPIRED,
                 REFRESH_FAILED, PASSWORD_RESET_FAILED, USER_LOCKED, INVALID_TOKEN -> AuditSeverity.WARNING;
            default -> AuditSeverity.INFO;
        };
    }

    /**
     * Publish audit event for alerting service.
     */
    private void publishAuditEventForAlerting(SecurityAuditLog auditLog) {
        try {
            // Convert to DTO and publish
            SecurityAuditLogDto dto = auditLogMapper.toDto(auditLog);
            eventPublisher.publishEvent(dto);
        } catch (Exception e) {
            log.error("Failed to publish audit event for alerting", e);
            // Don't throw - alerting failure shouldn't break audit logging
        }
    }

    /**
     * Determine human-readable period description.
     */
    private String determinePeriodDescription(OffsetDateTime after, OffsetDateTime before) {
        if (before == null) {
            before = OffsetDateTime.now(ZoneOffset.UTC);
        }
        if (after == null) {
            return "All time";
        }

        long daysDiff = java.time.Duration.between(after, before).toDays();

        if (daysDiff <= 1) {
            return "Last 24 hours";
        } else if (daysDiff <= 7) {
            return "Last 7 days";
        } else if (daysDiff <= 30) {
            return "Last 30 days";
        } else if (daysDiff <= 90) {
            return "Last 90 days";
        } else {
            return "Last " + daysDiff + " days";
        }
    }
}
