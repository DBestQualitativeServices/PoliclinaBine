package com.example.policlicabine.service;

import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.dto.SecurityAuditSearchDto;
import com.example.policlicabine.dto.SecurityAuditStatsDto;
import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.mapper.SecurityAuditLogMapper;
import com.example.policlicabine.repository.SecurityAuditLogRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
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
 *
 * Extends BaseServiceImpl to inherit standard CRUD operations.
 * Note: Audit logs are immutable - update operations are blocked.
 */
@Service
@Slf4j
@Transactional
public class SecurityAuditService extends BaseServiceImpl<SecurityAuditLog, SecurityAuditLogDto, UUID> {

    private final SecurityAuditLogRepository auditLogRepository;
    private final SecurityAuditLogMapper auditLogMapper;
    private final ApplicationEventPublisher eventPublisher;

    public SecurityAuditService(
            SecurityAuditLogRepository repository,
            SecurityAuditLogMapper mapper,
            ApplicationEventPublisher eventPublisher) {
        super(repository, mapper);
        this.auditLogRepository = repository;
        this.auditLogMapper = mapper;
        this.eventPublisher = eventPublisher;
    }

    // ============= BASE SERVICE ABSTRACT METHOD IMPLEMENTATIONS =============

    @Override
    protected SecurityAuditLogDto toDto(SecurityAuditLog entity) {
        return auditLogMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Security Audit Log";
    }

    @Override
    protected void updateEntityFromDto(SecurityAuditLog entity, SecurityAuditLogDto dto) {
        // Audit logs are IMMUTABLE - updates not allowed
        throw new UnsupportedOperationException("Audit logs cannot be modified");
    }

    /**
     * Override update to prevent modifications - audit logs are immutable.
     */
    @Override
    public SecurityAuditLogDto update(UUID id, SecurityAuditLogDto dto) {
        throw new BusinessException("Audit logs are immutable and cannot be updated");
    }

    // ============= CORE LOGGING INFRASTRUCTURE =============

    /**
     * Core logging logic - prepares and saves the audit log entity.
     * Extracts common code from logEventAsync, logEvent, and logEventAndReturnEntity.
     *
     * @param dto Security audit log DTO
     * @return Saved SecurityAuditLog entity
     */
    private SecurityAuditLog prepareAndSaveAuditLog(SecurityAuditLogDto dto) {
        SecurityAuditLog entity = auditLogMapper.toEntity(dto);

        // Set default severity if not provided
        if (entity.getSeverity() == null) {
            entity.setSeverity(determineSeverity(entity.getEventType()));
        }

        // Ensure timestamp is in UTC
        if (entity.getTimestamp() != null) {
            entity.setTimestamp(entity.getTimestamp().withOffsetSameInstant(ZoneOffset.UTC));
        }

        return auditLogRepository.save(entity);
    }

    // ============= PUBLIC LOGGING METHODS =============

    /**
     * Log a security event asynchronously.
     * Called from frontend POST /api/security/log and internal event listeners.
     *
     * @param dto Security audit log DTO
     */
    @Async("auditLogExecutor")
    public void logEventAsync(SecurityAuditLogDto dto) {
        try {
            SecurityAuditLog saved = prepareAndSaveAuditLog(dto);
            log.debug("Audit event logged: {} | Principal: {} | Severity: {}",
                saved.getEventType(), saved.getPrincipal(), saved.getSeverity());
            publishAuditEventForAlerting(saved);
        } catch (Exception e) {
            log.error("Failed to log security event asynchronously", e);
            // Don't throw - audit logging failure shouldn't break application
        }
    }

    /**
     * Log a security event synchronously (for cases where immediate persistence is needed).
     *
     * @param dto Security audit log DTO
     * @return SecurityAuditLogDto the saved DTO
     */
    @Transactional
    public SecurityAuditLogDto logEvent(SecurityAuditLogDto dto) {
        SecurityAuditLog saved = prepareAndSaveAuditLog(dto);
        log.info("Audit event logged: {} | Principal: {} | Severity: {}",
            saved.getEventType(), saved.getPrincipal(), saved.getSeverity());
        publishAuditEventForAlerting(saved);
        return auditLogMapper.toDto(saved);
    }

    /**
     * Log a security event and return the entity (for internal use by event listeners).
     * This method is synchronous and returns the saved entity for further processing
     * (e.g., tracking in Application Insights).
     *
     * @param dto Security audit log DTO
     * @return Saved SecurityAuditLog entity, or null if logging failed
     */
    @Transactional
    public SecurityAuditLog logEventAndReturnEntity(SecurityAuditLogDto dto) {
        try {
            SecurityAuditLog saved = prepareAndSaveAuditLog(dto);
            log.debug("Audit event logged and returned: {} | Principal: {} | Severity: {}",
                saved.getEventType(), saved.getPrincipal(), saved.getSeverity());
            publishAuditEventForAlerting(saved);
            return saved;
        } catch (Exception e) {
            log.error("Failed to log security event", e);
            return null;
        }
    }

    // Note: findById() is now inherited from BaseServiceImpl

    /**
     * Search audit logs with filters and pagination.
     */
    @Transactional(readOnly = true)
    public Page<SecurityAuditLogDto> search(SecurityAuditSearchDto searchDto) {
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
        return results.map(auditLogMapper::toDto);
    }

    /**
     * Get statistics for security audit logs.
     * Uses efficient time-filtered database aggregation queries instead of loading events into memory.
     */
    @Transactional(readOnly = true)
    public SecurityAuditStatsDto getStatistics(OffsetDateTime after, OffsetDateTime before) {
        // Default time range if not provided (last 7 days)
        if (after == null) {
            after = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
        }

        // Use time-filtered database aggregations (efficient - no memory loading)
        List<Object[]> eventTypeCounts = auditLogRepository.countByEventTypeAfter(after);
        Map<String, Long> eventTypeMap = eventTypeCounts.stream()
            .collect(Collectors.toMap(
                row -> ((AuditEventType) row[0]).name(),
                row -> (Long) row[1]
            ));

        List<Object[]> severityCounts = auditLogRepository.countBySeverityAfter(after);
        Map<String, Long> severityMap = severityCounts.stream()
            .collect(Collectors.toMap(
                row -> ((AuditSeverity) row[0]).name(),
                row -> (Long) row[1]
            ));

        // Single count query instead of loading all events
        long totalEvents = auditLogRepository.countEventsAfter(after);

        // Calculate specific counts from maps (no additional DB calls)
        long criticalEvents = severityMap.getOrDefault("CRITICAL", 0L);
        long warningEvents = severityMap.getOrDefault("WARNING", 0L);
        long infoEvents = severityMap.getOrDefault("INFO", 0L);

        long failedLoginAttempts = eventTypeMap.getOrDefault("LOGIN_FAILURE", 0L);
        long unauthorizedAccessAttempts = eventTypeMap.getOrDefault("UNAUTHORIZED_ACCESS", 0L);
        long suspiciousActivities = eventTypeMap.getOrDefault("SUSPICIOUS_ACTIVITY", 0L)
            + eventTypeMap.getOrDefault("BRUTE_FORCE_DETECTED", 0L);

        // Database aggregation for top principals (efficient - LIMIT 10 in query)
        List<Object[]> topPrincipalsList = auditLogRepository.findTopPrincipalsSince(after);
        Map<String, Long> topPrincipals = topPrincipalsList.stream()
            .collect(Collectors.toMap(
                row -> (String) row[0],
                row -> (Long) row[1],
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));

        String period = determinePeriodDescription(after, before);

        return SecurityAuditStatsDto.builder()
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
