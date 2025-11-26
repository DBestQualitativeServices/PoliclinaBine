package com.example.policlicabine.repository;

import com.example.policlicabine.entity.SessionBilling;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SessionBillingRepository extends JpaRepository<SessionBilling, UUID> {

    Optional<SessionBilling> findBySessionSessionId(UUID sessionId);

    boolean existsBySessionSessionId(UUID sessionId);

    // EntityGraph methods to prevent N+1 queries
    // Load billing with session and consultations for subtotal calculation and event publishing
    @EntityGraph(attributePaths = {"session", "session.consultationTypes", "session.patient"})
    Optional<SessionBilling> findWithSessionBySessionSessionId(UUID sessionId);

    /**
     * Finds session billing with session and discounts loaded.
     * Use for DTO mapping with nested BillingDiscountDto list.
     */
    @EntityGraph(attributePaths = {"session", "discounts", "discounts.appliedBy"})
    Optional<SessionBilling> findWithSessionAndDiscountsByBillingId(UUID billingId);
}
