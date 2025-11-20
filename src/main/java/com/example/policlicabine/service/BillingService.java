package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.ConsultationType;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.SessionCompleted;
import com.example.policlicabine.repository.SessionBillingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing billing and discounts.
 * Handles SessionBilling creation and discount management.
 *
 * Architecture:
 * - Only uses SessionBillingRepository (single responsibility)
 * - Calls AppointmentSessionService and UserService for validation and entity access
 * - Uses EntityGraph to prevent N+1 queries
 * - Uses EntityManager.getReference() for FK setting
 * - BigDecimal for all monetary calculations
 * - Defensive programming for financial operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {

    // Only our repository - single responsibility principle
    private final SessionBillingRepository sessionBillingRepository;

    // Services for validation and entity access
    private final AppointmentSessionService appointmentSessionService;
    private final UserService userService;

    private final ApplicationEventPublisher eventPublisher;

    // EntityManager for creating entity references without DB hits
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates billing record for an appointment session.
     * Calculates subtotal from consultations.
     *
     * Architecture notes:
     * - Uses AppointmentSessionService to validate session is completed
     * - Gets session entity with relationships loaded for event publishing
     * - Uses EntityManager.getReference() for session FK (no DB hit)
     *
     * @param sessionId AppointmentSession identifier
     * @return Result containing SessionBilling or error message
     */
    public Result<SessionBilling> createSessionBilling(UUID sessionId) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Check if billing already exists
            if (sessionBillingRepository.existsBySessionSessionId(sessionId)) {
                return Result.failure("Billing already exists for this session");
            }

            // Validate session is completed via AppointmentSessionService
            Result<Void> validationResult = appointmentSessionService.validateSessionCompleted(sessionId);
            if (validationResult.isFailure()) {
                return Result.failure(validationResult.getErrorMessage());
            }

            // Get session with all relationships for event publishing (EntityGraph)
            AppointmentSession session = appointmentSessionService.getEntityWithAllRelationships(sessionId);
            if (session == null) {
                return Result.failure("Session not found");
            }

            // Use EntityManager.getReference() for session FK (no extra DB hit)
            AppointmentSession sessionRef = entityManager.getReference(AppointmentSession.class, sessionId);

            SessionBilling billing = SessionBilling.builder()
                .session(sessionRef)
                .build();

            SessionBilling savedBilling = sessionBillingRepository.save(billing);

            log.info("Session billing created: {} for session {} with subtotal {}",
                savedBilling.getBillingId(), sessionId, savedBilling.getSubtotalAmount());

            return Result.success(savedBilling);

        } catch (Exception e) {
            log.error("Error creating session billing", e);
            return Result.failure("Failed to create billing: " + e.getMessage());
        }
    }

    /**
     * Applies a discount to a session billing.
     * Validates discount amount and user authorization.
     *
     * Architecture notes:
     * - Uses UserService to get user entity for discount application
     * - Uses EntityGraph to load billing with session for subtotal calculation
     *
     * @param sessionId Session identifier
     * @param userId User applying the discount
     * @param discountAmount Discount amount (must be positive)
     * @param reason Reason for discount
     * @return Result containing updated SessionBilling or error message
     */
    public Result<SessionBilling> applyDiscount(UUID sessionId, UUID userId,
                                              BigDecimal discountAmount, String reason) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }
            if (userId == null) {
                return Result.failure("User ID is required");
            }
            if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.failure("Discount amount must be positive");
            }
            if (reason == null || reason.trim().isEmpty()) {
                return Result.failure("Discount reason is required");
            }

            // Load billing with session for subtotal calculation (EntityGraph)
            SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
                .orElse(null);
            if (billing == null) {
                return Result.failure("Billing not found for session");
            }

            // Get user entity via UserService
            User user = userService.getEntityById(userId);
            if (user == null) {
                return Result.failure("User not found");
            }

            // Validate discount doesn't exceed subtotal
            BigDecimal subtotal = billing.getSubtotalAmount();
            BigDecimal totalDiscounts = billing.getTotalDiscountAmount().add(discountAmount);
            if (totalDiscounts.compareTo(subtotal) > 0) {
                return Result.failure("Total discounts cannot exceed subtotal amount");
            }

            // Apply discount using helper method
            billing.addDiscount(user, discountAmount, reason.trim());
            SessionBilling savedBilling = sessionBillingRepository.save(billing);

            log.info("Discount of {} applied to session {} by user {}. New final amount: {}",
                discountAmount, sessionId, userId, savedBilling.getFinalAmount());

            return Result.success(savedBilling);

        } catch (Exception e) {
            log.error("Error applying discount", e);
            return Result.failure("Failed to apply discount: " + e.getMessage());
        }
    }

    /**
     * Retrieves billing information for a session.
     *
     * Architecture notes:
     * - Uses EntityGraph to load billing with session and relationships
     *
     * @param sessionId Session identifier
     * @return Result containing SessionBilling or error message
     */
    @Transactional(readOnly = true)
    public Result<SessionBilling> getBillingForSession(UUID sessionId) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Use EntityGraph to load billing with session for amount calculations
            SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
                .orElse(null);
            if (billing == null) {
                return Result.failure("Billing not found for session");
            }

            return Result.success(billing);

        } catch (Exception e) {
            log.error("Error getting billing for session", e);
            return Result.failure("Failed to get billing: " + e.getMessage());
        }
    }

    /**
     * Calculates the final amount for a session after discounts.
     *
     * Architecture notes:
     * - Uses EntityGraph to load billing with session if billing exists
     * - Falls back to AppointmentSessionService if no billing exists yet
     *
     * @param sessionId Session identifier
     * @return Result containing BigDecimal amount or error message
     */
    @Transactional(readOnly = true)
    public Result<BigDecimal> calculateFinalAmount(UUID sessionId) {
        try {
            if (sessionId == null) {
                return Result.failure("Session ID is required");
            }

            // Try to find billing with session loaded (EntityGraph)
            SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
                .orElse(null);
            if (billing == null) {
                // If no billing exists, get subtotal from session via AppointmentSessionService
                AppointmentSession session = appointmentSessionService.getEntityWithAllRelationships(sessionId);
                if (session == null) {
                    return Result.failure("Session not found");
                }
                return Result.success(session.getSubtotalAmount());
            }

            return Result.success(billing.getFinalAmount());

        } catch (Exception e) {
            log.error("Error calculating final amount", e);
            return Result.failure("Failed to calculate amount: " + e.getMessage());
        }
    }

    /**
     * Event handler to automatically create billing when session is completed.
     * Listens to SessionCompleted events and creates billing records.
     *
     * @param event SessionCompleted event
     */
    @EventListener
    public void handleSessionCompleted(SessionCompleted event) {
        try {
            log.info("Received SessionCompleted event for session: {}", event.sessionId());
            createSessionBilling(event.sessionId());
        } catch (Exception e) {
            log.error("Error auto-creating billing for completed session: {}", event.sessionId(), e);
        }
    }

    private List<String> getConsultationNames(AppointmentSession session) {
        return session.getConsultationTypes().stream()
            .map(ConsultationType::getName)
            .collect(Collectors.toList());
    }
}
