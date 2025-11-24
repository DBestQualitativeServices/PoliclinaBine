package com.example.policlicabine.repository;

import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for SecurityAuditLog entity.
 * Provides query methods for security audit log operations.
 */
@Repository
public interface SecurityAuditLogRepository extends JpaRepository<SecurityAuditLog, UUID> {

    /**
     * Find all audit logs by event type, ordered by timestamp descending.
     */
    List<SecurityAuditLog> findByEventTypeOrderByTimestampDesc(AuditEventType eventType);

    /**
     * Find all audit logs by event type with pagination.
     */
    Page<SecurityAuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    /**
     * Find all audit logs by principal, ordered by timestamp descending.
     */
    List<SecurityAuditLog> findByPrincipalOrderByTimestampDesc(String principal);

    /**
     * Find all audit logs by principal with pagination.
     */
    Page<SecurityAuditLog> findByPrincipal(String principal, Pageable pageable);

    /**
     * Find all audit logs by severity.
     */
    Page<SecurityAuditLog> findBySeverity(AuditSeverity severity, Pageable pageable);

    /**
     * Find all audit logs by user ID.
     */
    Page<SecurityAuditLog> findByUserId(String userId, Pageable pageable);

    /**
     * Find recent audit logs after a specific timestamp.
     */
    @Query("SELECT a FROM SecurityAuditLog a WHERE a.timestamp >= :after ORDER BY a.timestamp DESC")
    List<SecurityAuditLog> findRecentEvents(@Param("after") OffsetDateTime after);

    /**
     * Find recent audit logs with pagination.
     */
    @Query("SELECT a FROM SecurityAuditLog a WHERE a.timestamp >= :after ORDER BY a.timestamp DESC")
    Page<SecurityAuditLog> findRecentEvents(@Param("after") OffsetDateTime after, Pageable pageable);

    /**
     * Find audit logs between two timestamps.
     */
    @Query("SELECT a FROM SecurityAuditLog a WHERE a.timestamp >= :after AND a.timestamp <= :before ORDER BY a.timestamp DESC")
    Page<SecurityAuditLog> findBetween(
        @Param("after") OffsetDateTime after,
        @Param("before") OffsetDateTime before,
        Pageable pageable
    );

    /**
     * Advanced search with multiple optional filters.
     */
    @Query("""
        SELECT a FROM SecurityAuditLog a
        WHERE (:eventType IS NULL OR a.eventType = :eventType)
        AND (:severity IS NULL OR a.severity = :severity)
        AND (:principal IS NULL OR a.principal LIKE %:principal%)
        AND (:userId IS NULL OR a.userId = :userId)
        AND (:after IS NULL OR a.timestamp >= :after)
        AND (:before IS NULL OR a.timestamp <= :before)
        ORDER BY a.timestamp DESC
        """)
    Page<SecurityAuditLog> searchAuditLogs(
        @Param("eventType") AuditEventType eventType,
        @Param("severity") AuditSeverity severity,
        @Param("principal") String principal,
        @Param("userId") String userId,
        @Param("after") OffsetDateTime after,
        @Param("before") OffsetDateTime before,
        Pageable pageable
    );

    /**
     * Count audit logs by event type.
     * Returns list of Object[]{AuditEventType, Long}
     */
    @Query("SELECT a.eventType, COUNT(a) FROM SecurityAuditLog a GROUP BY a.eventType")
    List<Object[]> countByEventType();

    /**
     * Count audit logs by severity.
     * Returns list of Object[]{AuditSeverity, Long}
     */
    @Query("SELECT a.severity, COUNT(a) FROM SecurityAuditLog a GROUP BY a.severity")
    List<Object[]> countBySeverity();

    // ============= TIME-FILTERED AGGREGATION QUERIES (for efficient statistics) =============

    /**
     * Count audit logs by event type after a specific timestamp.
     * Returns list of Object[]{AuditEventType, Long}
     */
    @Query("SELECT a.eventType, COUNT(a) FROM SecurityAuditLog a WHERE a.timestamp >= :after GROUP BY a.eventType")
    List<Object[]> countByEventTypeAfter(@Param("after") OffsetDateTime after);

    /**
     * Count audit logs by severity after a specific timestamp.
     * Returns list of Object[]{AuditSeverity, Long}
     */
    @Query("SELECT a.severity, COUNT(a) FROM SecurityAuditLog a WHERE a.timestamp >= :after GROUP BY a.severity")
    List<Object[]> countBySeverityAfter(@Param("after") OffsetDateTime after);

    /**
     * Count total audit events after a specific timestamp.
     * Efficient single count query for statistics.
     */
    @Query("SELECT COUNT(a) FROM SecurityAuditLog a WHERE a.timestamp >= :after")
    long countEventsAfter(@Param("after") OffsetDateTime after);

    /**
     * Find top principals by event count since a specific timestamp.
     * Returns list of Object[]{principal (String), count (Long)} ordered by count descending.
     * Limited to top 10 results for performance.
     */
    @Query("""
        SELECT a.principal, COUNT(a) as cnt
        FROM SecurityAuditLog a
        WHERE a.timestamp >= :after AND a.principal IS NOT NULL
        GROUP BY a.principal
        ORDER BY cnt DESC
        LIMIT 10
        """)
    List<Object[]> findTopPrincipalsSince(@Param("after") OffsetDateTime after);

    /**
     * Count failed login attempts for a specific principal within a time window.
     */
    @Query("""
        SELECT COUNT(a) FROM SecurityAuditLog a
        WHERE a.principal = :principal
        AND a.eventType = :eventType
        AND a.timestamp >= :since
        """)
    long countEventsByPrincipalSince(
        @Param("principal") String principal,
        @Param("eventType") AuditEventType eventType,
        @Param("since") OffsetDateTime since
    );

    /**
     * Count events by IP address within a time window.
     */
    @Query("""
        SELECT COUNT(a) FROM SecurityAuditLog a
        WHERE a.ipAddress = :ipAddress
        AND a.eventType = :eventType
        AND a.timestamp >= :since
        """)
    long countEventsByIpAddressSince(
        @Param("ipAddress") String ipAddress,
        @Param("eventType") AuditEventType eventType,
        @Param("since") OffsetDateTime since
    );

    /**
     * Delete audit logs older than a specific timestamp.
     * Used for retention policy cleanup.
     */
    @Modifying
    @Query("DELETE FROM SecurityAuditLog a WHERE a.timestamp < :before")
    int deleteByTimestampBefore(@Param("before") OffsetDateTime before);

    /**
     * Find distinct principals with events in a time range.
     */
    @Query("""
        SELECT DISTINCT a.principal FROM SecurityAuditLog a
        WHERE a.timestamp >= :after
        AND a.principal IS NOT NULL
        ORDER BY a.principal
        """)
    List<String> findDistinctPrincipalsSince(@Param("after") OffsetDateTime after);

    /**
     * Find audit logs by principal and event type within a time window.
     */
    @Query("""
        SELECT a FROM SecurityAuditLog a
        WHERE a.principal = :principal
        AND a.eventType = :eventType
        AND a.timestamp >= :since
        ORDER BY a.timestamp DESC
        """)
    List<SecurityAuditLog> findByPrincipalAndEventTypeSince(
        @Param("principal") String principal,
        @Param("eventType") AuditEventType eventType,
        @Param("since") OffsetDateTime since
    );
}
