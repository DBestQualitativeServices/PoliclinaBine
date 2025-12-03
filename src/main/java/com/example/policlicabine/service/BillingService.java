package com.example.policlicabine.service;

import com.example.policlicabine.entity.AppointmentSession;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.SessionCompleted;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
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

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BillingService {

    private final SessionBillingRepository sessionBillingRepository;
    private final AppointmentSessionService appointmentSessionService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @PersistenceContext
    private EntityManager entityManager;

    public SessionBilling createSessionBilling(UUID sessionId) {
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

        return savedBilling;
    }

    public SessionBilling applyDiscount(UUID sessionId, UUID userId,
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

        return savedBilling;
    }

    @Transactional(readOnly = true)
    public SessionBilling getBillingForSession(UUID sessionId) {
        if (sessionId == null) {
            throw new BusinessException("Session ID is required");
        }

        return sessionBillingRepository.findWithSessionBySessionSessionId(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Billing", "sessionId: " + sessionId));
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
