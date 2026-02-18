package com.example.policlicabine.repository;

import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.enums.SessionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AppointmentSessionRepository extends JpaRepository<AppointmentSession, UUID>,
                                                       JpaSpecificationExecutor<AppointmentSession> {
                                                       // CUSTOM REPOSITORY COMMENTED OUT - See reactivation instructions below
                                                       // AppointmentSessionRepositoryCustom {

    @EntityGraph(attributePaths = {"patient", "doctor"})
    Optional<AppointmentSession> findWithBasicRelationshipsBySessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"patient", "doctor", "consultationTypes", "diagnoses", "formSubmissions"})
    Optional<AppointmentSession> findWithAllRelationshipsBySessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"consultationTypes"})
    Optional<AppointmentSession> findWithConsultationsBySessionId(UUID sessionId);

    @EntityGraph(attributePaths = {"patient", "doctor", "consultationTypes"})
    List<AppointmentSession> findWithRelationshipsByPatientPatientIdOrderByScheduledDateTimeDesc(UUID patientId);

    @EntityGraph(attributePaths = {"patient", "doctor"})
    List<AppointmentSession> findWithBasicRelationshipsByDoctorDoctorIdAndScheduledDateTimeBetweenAndStatusNot(
            UUID doctorId, OffsetDateTime start, OffsetDateTime end, SessionStatus excludeStatus);

    /**
     * Override findAll with SIMPLIFIED EntityGraph for bounded queries (≤7 days, ≤50 results).
     *
     * SIMPLIFIED APPROACH (Current):
     * - Only loads 2 collections: consultationTypes + diagnoses
     * - Nested collections (doctor.specialties, consultationTypes.requiredFormTemplates) rely on @BatchSize
     * - Works well for date ranges ≤ 7 days and page sizes ≤ 50
     * - May trigger HHH90003004 warning (in-memory pagination) but impact is minimal
     * - Cartesian product: ~50 sessions × 3 consultations × 5 diagnoses = ~750 rows (manageable)
     *
     * WHEN TO REACTIVATE CUSTOM REPOSITORY:
     * 1. Business requires date ranges > 7 days (e.g., 30-day views)
     * 2. Page size increases beyond 50 results
     * 3. HHH90003004 warning appears in production logs
     * 4. Query performance degrades (response time > 500ms)
     * 5. Memory issues occur during concurrent searches
     *
     * TO REACTIVATE (see AppointmentSessionRepositoryCustom.java):
     * 1. Uncomment "AppointmentSessionRepositoryCustom" extension in class declaration (line 21)
     * 2. Uncomment findBySessionIdIn() method below (lines 75-84)
     * 3. Uncomment custom repository files:
     *    - AppointmentSessionRepositoryCustom.java
     *    - AppointmentSessionRepositoryImpl.java
     * 4. Restore two-phase query in AppointmentSessionService.search() method
     * 5. Restore original EntityGraph below (commented section)
     *
     * <p>Note: formSubmissions loaded separately via batch query to avoid Cartesian product.</p>
     */
    // ORIGINAL EntityGraph (for two-phase custom repository - COMMENTED OUT):
    // @EntityGraph(attributePaths = {
    //     "patient",
    //     "doctor",
    //     "doctor.specialties",
    //     "doctor.weeklyAvailability",
    //     "consultationTypes",
    //     "consultationTypes.requiredFormTemplates",
    //     "diagnoses"
    // })

    // SIMPLIFIED EntityGraph (current - for bounded queries):
    @EntityGraph(attributePaths = {
        "patient",              // ManyToOne - no row multiplication
        "doctor",               // ManyToOne - no row multiplication
        "consultationTypes",    // ManyToMany - causes Cartesian product with diagnoses
        "diagnoses"             // ManyToMany - causes Cartesian product with consultationTypes
        // Nested collections removed - fetched via @BatchSize when accessed:
        // - doctor.specialties (via @BatchSize in Doctor entity)
        // - doctor.weeklyAvailability (via @BatchSize in Doctor entity)
        // - consultationTypes.requiredFormTemplates (via @BatchSize in ConsultationType entity)
    })
    @Override
    Page<AppointmentSession> findAll(Specification<AppointmentSession> spec, Pageable pageable);

    /**
     * COMMENTED OUT - Used by two-phase custom repository pattern.
     * Uncomment when reactivating AppointmentSessionRepositoryCustom.
     *
     * This method loads full entities with relationships for a bounded set of IDs,
     * avoiding Cartesian product pagination issues by fetching only pre-selected IDs.
     */
    // @EntityGraph(attributePaths = {
    //     "patient",
    //     "doctor",
    //     "doctor.specialties",
    //     "doctor.weeklyAvailability",
    //     "consultationTypes",
    //     "consultationTypes.requiredFormTemplates",
    //     "diagnoses"
    // })
    // List<AppointmentSession> findBySessionIdIn(List<UUID> sessionIds);

    boolean existsByDoctorDoctorIdAndPatientPatientIdAndScheduledDateTimeBetweenAndStatusNot(
            UUID doctorId, UUID patientId, OffsetDateTime start, OffsetDateTime end, SessionStatus status);

    @EntityGraph(attributePaths = {"patient", "consultationTypes"})
    @Query("SELECT a FROM AppointmentSession a " +
           "WHERE a.doctor.doctorId = :doctorId " +
           "AND a.status NOT IN :excludedStatuses " +
           "AND :startTime < FUNCTION('TIMESTAMPADD', MINUTE, a.totalDurationMinutes, a.scheduledDateTime) " +
           "AND :endTime > a.scheduledDateTime " +
           "ORDER BY a.scheduledDateTime")
    List<AppointmentSession> findOverlappingAppointments(
            @Param("doctorId") UUID doctorId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludedStatuses") List<SessionStatus> excludedStatuses);

    @EntityGraph(attributePaths = {"patient", "consultationTypes"})
    @Query("SELECT a FROM AppointmentSession a " +
           "WHERE a.doctor.doctorId = :doctorId " +
           "AND a.sessionId != :excludeSessionId " +
           "AND a.status NOT IN :excludedStatuses " +
           "AND :startTime < FUNCTION('TIMESTAMPADD', MINUTE, a.totalDurationMinutes, a.scheduledDateTime) " +
           "AND :endTime > a.scheduledDateTime " +
           "ORDER BY a.scheduledDateTime")
    List<AppointmentSession> findOverlappingAppointmentsExcluding(
            @Param("doctorId") UUID doctorId,
            @Param("excludeSessionId") UUID excludeSessionId,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime,
            @Param("excludedStatuses") List<SessionStatus> excludedStatuses);
}
