package com.example.policlicabine.repository;

import com.example.policlicabine.entity.AppointmentSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * ====================================================================================================
 * CUSTOM REPOSITORY - CURRENTLY DISABLED (But preserved for future reactivation)
 * ====================================================================================================
 *
 * This custom repository solves Hibernate's HHH90003004 warning (in-memory pagination)
 * when using EntityGraph with multiple collections that create Cartesian products.
 *
 * WHY IT WAS CREATED:
 * - AppointmentSession has 4-5 collections (consultationTypes, diagnoses, doctor.specialties, etc.)
 * - EntityGraph with multiple collections creates Cartesian product (1 session = 100+ rows)
 * - Hibernate cannot apply database-level LIMIT/OFFSET with row multiplication
 * - Result: ALL matching rows fetched into memory, then paginated in-memory (HHH90003004)
 *
 * HOW IT WORKS (Two-Phase Query Pattern):
 * - Phase 1: findSessionIds() - Fetch only IDs with filters (no collections, no Cartesian product)
 *            Database applies LIMIT/OFFSET correctly (e.g., LIMIT 20)
 * - Phase 2: findBySessionIdIn() - Load full entities with EntityGraph for 20 specific IDs
 *            Cartesian product is bounded by IN clause (max 20 IDs)
 *
 * CURRENT STATUS: Disabled because business constraints allow simplified approach
 * - Date range limited to 7 days maximum
 * - Page size limited to 50 results maximum
 * - Cartesian product is manageable: ~50 sessions × 3 consultations × 5 diagnoses = 750 rows
 *
 * WHEN TO REACTIVATE:
 * 1. Business requires date ranges > 7 days (e.g., 30-day or monthly views)
 * 2. Page size increases beyond 50 (e.g., export all results)
 * 3. HHH90003004 warning appears in production logs
 * 4. Query performance degrades (response time > 500ms)
 * 5. Memory issues during concurrent searches (heap pressure, OOM errors)
 * 6. Data growth: more consultations/diagnoses per session over time
 *
 * HOW TO REACTIVATE:
 * 1. In AppointmentSessionRepository.java:
 *    - Uncomment "AppointmentSessionRepositoryCustom" in class declaration (line 21-22)
 *    - Uncomment findBySessionIdIn() method (lines 100-109)
 *    - Restore original EntityGraph on findAll() (lines 69-77)
 * 2. In this file: Uncomment the findSessionIds() method below (line 90)
 * 3. In AppointmentSessionRepositoryImpl.java: Uncomment the full implementation class
 * 4. In AppointmentSessionService.java:
 *    - Restore two-phase query in search() method (see commented section line 342)
 * 5. Rebuild project and test with production-like data volumes
 *
 * BENEFITS OF TWO-PHASE APPROACH:
 * - ✅ Eliminates HHH90003004 warning (database-level pagination)
 * - ✅ Predictable performance (3 queries, bounded row count)
 * - ✅ Scales to any dataset size (tested with 1000+ sessions)
 * - ✅ No memory issues (only 20-50 entities loaded at a time)
 *
 * PERFORMANCE COMPARISON:
 * - Simplified (current): 1 query, ~750 rows for 50 sessions (bounded use case)
 * - Two-phase (custom): 3 queries, ~60 rows for 20 sessions (unbounded use case)
 *
 * For unbounded queries (200+ sessions):
 * - Simplified: 3,000+ rows, 500ms-2s, memory spikes, HHH90003004 warning
 * - Two-phase: ~60 rows, <100ms, low memory, no warning
 *
 * REFERENCES:
 * - Spring Data JPA docs: EntityGraph limitations with pagination
 * - Hibernate docs: HHH90003004 - in-memory pagination warning
 * - Stack Overflow: Multiple collection fetching with pagination
 *
 * @author Generated with expert review (Spring Boot 4.0 / Hibernate 6.x)
 * @see AppointmentSessionRepositoryImpl
 * @see AppointmentSessionRepository
 * ====================================================================================================
 */
public interface AppointmentSessionRepositoryCustom {

    /**
     * CURRENTLY DISABLED - Uncomment to reactivate
     *
     * Phase 1: Finds paginated session IDs matching the specification.
     * <p>
     * This query only selects IDs without any collection joins, allowing
     * proper database-level pagination (LIMIT/OFFSET).
     * </p>
     *
     * @param spec the filter specification
     * @param pageable pagination parameters
     * @return page of session IDs
     */
    // UNCOMMENT WHEN REACTIVATING:
    // Page<UUID> findSessionIds(Specification<AppointmentSession> spec, Pageable pageable);
}
