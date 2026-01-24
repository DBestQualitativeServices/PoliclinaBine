package com.example.policlicabine.service;

import com.example.policlicabine.dto.SessionBillingDto;
import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.SessionCompleted;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.BillingMapper;
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
import java.util.UUID;

/**
 * Service for managing session billing operations.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Creating session billings after appointment completion</li>
 *   <li>Applying discounts to session billings</li>
 *   <li>Retrieving billing information</li>
 *   <li>Calculating final amounts with discounts</li>
 * </ul>
 *
 * <p>All public methods return DTOs to maintain clean separation from entities.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {

    private final SessionBillingRepository sessionBillingRepository;
    private final AppointmentSessionService appointmentSessionService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final BillingMapper billingMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Creates a new session billing for a completed appointment.
     *
     * @param sessionId the appointment session ID
     * @return the created SessionBillingDto
     * @throws BusinessException if sessionId is null, billing already exists, or session not completed
     * @throws ResourceNotFoundException if session not found
     */
    public SessionBillingDto createSessionBilling(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        if (sessionBillingRepository.existsBySessionSessionId(sessionId)) {
            throw new BusinessException("Billing already exists for this session");
        }

        appointmentSessionService.validateSessionCompleted(sessionId);

        AppointmentSession session = appointmentSessionService.getEntityWithAllRelationships(sessionId);
        if (session == null) {
            throw new ResourceNotFoundException("Session", sessionId);
        }

        AppointmentSession sessionRef = entityManager.getReference(AppointmentSession.class, sessionId);

        SessionBilling billing = SessionBilling.builder()
            .session(sessionRef)
            .build();

        SessionBilling savedBilling = sessionBillingRepository.save(billing);

        log.info("Session billing created: {} for session {} with subtotal {}",
            savedBilling.getBillingId(), sessionId, savedBilling.getSubtotalAmount());

        return billingMapper.toDto(savedBilling);
    }

    /**
     * Applies a discount to an existing session billing.
     *
     * @param sessionId the session ID
     * @param userId the user applying the discount
     * @param discountAmount the discount amount (must be positive)
     * @param reason the reason for the discount
     * @return the updated SessionBillingDto with discount applied
     * @throws BusinessException if validation fails or discount exceeds subtotal
     * @throws ResourceNotFoundException if billing or user not found
     */
    public SessionBillingDto applyDiscount(UUID sessionId, UUID userId,
                                           BigDecimal discountAmount, String reason) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }
        if (userId == null) {
            throw new BusinessException("User ID is required");
        }
        if (discountAmount == null || discountAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Discount amount must be positive");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Discount reason is required");
        }

        SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Billing", "sessionId: " + sessionId));

        User user = userService.getEntityById(userId);
        if (user == null) {
            throw new ResourceNotFoundException("User", userId);
        }

        BigDecimal subtotal = billing.getSubtotalAmount();
        BigDecimal totalDiscounts = billing.getTotalDiscountAmount().add(discountAmount);
        if (totalDiscounts.compareTo(subtotal) > 0) {
            throw new BusinessException("Total discounts cannot exceed subtotal amount");
        }

        billing.addDiscount(user, discountAmount, reason.trim());
        SessionBilling savedBilling = sessionBillingRepository.save(billing);

        log.info("Discount of {} applied to session {} by user {}. New final amount: {}",
            discountAmount, sessionId, userId, savedBilling.getFinalAmount());

        return billingMapper.toDto(savedBilling);
    }

    /**
     * Retrieves billing information for a session.
     *
     * @param sessionId the session ID
     * @return the SessionBillingDto
     * @throws BusinessException if sessionId is null
     * @throws ResourceNotFoundException if billing not found
     */
    @Transactional(readOnly = true)
    public SessionBillingDto getBillingForSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Billing", "sessionId: " + sessionId));

        return billingMapper.toDto(billing);
    }

    /**
     * Internal method for service-to-service communication.
     * Returns SessionBilling entity directly for internal use.
     *
     * @param sessionId the session ID
     * @return the SessionBilling entity or null if not found
     */
    @Transactional(readOnly = true)
    public SessionBilling getEntityBySessionId(UUID sessionId) {
        if (sessionId == null) {
            return null;
        }
        return sessionBillingRepository.findWithSessionBySessionSessionId(sessionId).orElse(null);
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateFinalAmount(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        SessionBilling billing = sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
            .orElse(null);
        
        if (billing == null) {
            AppointmentSession session = appointmentSessionService.getEntityWithAllRelationships(sessionId);
            if (session == null) {
                throw new ResourceNotFoundException("Session", sessionId);
            }
            return session.getSubtotalAmount();
        }

        return billing.getFinalAmount();
    }

    @EventListener
    public void handleSessionCompleted(SessionCompleted event) {
        try {
            log.info("Received SessionCompleted event for session: {}", event.sessionId());
            createSessionBilling(event.sessionId());
        } catch (Exception e) {
            log.error("Error auto-creating billing for completed session: {}", event.sessionId(), e);
        }
    }
}
