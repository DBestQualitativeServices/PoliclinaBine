package com.example.policlicabine.service.audit;

import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.repository.SecurityAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.audit.AuditEvent;
import org.springframework.boot.actuate.audit.AuditEventRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom implementation of Spring Boot Actuator's AuditEventRepository.
 * Stores audit events in SecurityAuditLog entity using JPA.
 *
 * Enables the /actuator/auditevents endpoint to work with our database-backed audit logs.
 */
@Repository
@Slf4j
@RequiredArgsConstructor
public class CustomAuditEventRepository implements AuditEventRepository {

    private final SecurityAuditLogRepository auditLogRepository;
    private final AuditEventConverter converter;

    /**
     * Save an audit event to the database.
     * Called by Spring Boot Actuator when security events occur.
     */
    @Override
    public void add(AuditEvent event) {
        try {
            SecurityAuditLog entity = converter.toEntity(event);
            auditLogRepository.save(entity);
            log.debug("Saved audit event: {} by {}", event.getType(), event.getPrincipal());
        } catch (Exception e) {
            log.error("Failed to save audit event: {}", event.getType(), e);
            // Don't throw - audit logging failure shouldn't break application
        }
    }

    /**
     * Find audit events with filters.
     * Used by /actuator/auditevents endpoint.
     *
     * @param principal Filter by principal (username). Can be null.
     * @param after     Find events after this timestamp. Can be null.
     * @param type      Filter by event type. Can be null.
     * @return List of matching audit events.
     */
    @Override
    public List<AuditEvent> find(String principal, Instant after, String type) {
        try {
            OffsetDateTime afterDateTime = after != null
                ? OffsetDateTime.ofInstant(after, ZoneOffset.UTC)
                : null;

            AuditEventType eventType = null;
            if (type != null && !type.isEmpty()) {
                try {
                    eventType = AuditEventType.valueOf(type);
                } catch (IllegalArgumentException e) {
                    log.debug("Unknown event type for filtering: {}", type);
                }
            }

            // Use repository search with pagination (limit to 100 results for performance)
            Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "timestamp"));

            Page<SecurityAuditLog> results = auditLogRepository.searchAuditLogs(
                eventType,
                null,  // severity
                principal,
                null,  // userId
                afterDateTime,
                null,  // before
                pageable
            );

            return results.stream()
                .map(converter::toAuditEvent)
                .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to find audit events", e);
            return List.of();
        }
    }
}
