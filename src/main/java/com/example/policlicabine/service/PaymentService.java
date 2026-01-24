package com.example.policlicabine.service;

import com.example.policlicabine.dto.PaymentDto;
import com.example.policlicabine.entity.Invoice;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.entity.Payment;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.PaymentType;
import com.example.policlicabine.event.PaymentProcessed;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.PaymentMapper;
import com.example.policlicabine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing payment operations.
 *
 * <p>This service handles:</p>
 * <ul>
 *   <li>Processing payments against invoices</li>
 *   <li>Retrieving payment information</li>
 *   <li>Validating payment amounts</li>
 * </ul>
 *
 * <p>All public methods return DTOs to maintain clean separation from entities.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;
    private final PaymentMapper paymentMapper;

    /**
     * Processes a payment against one or more invoices.
     *
     * @param invoiceIds the list of invoice IDs to pay
     * @param amount the payment amount (must be positive and not exceed total invoice amount)
     * @param paymentType the type of payment (CASH, CARD, etc.)
     * @param processedByUserId the user processing the payment
     * @param notes optional payment notes
     * @return the created PaymentDto
     * @throws BusinessException if validation fails
     * @throws ResourceNotFoundException if invoices or user not found
     */
    public PaymentDto processPayment(List<UUID> invoiceIds, BigDecimal amount,
                                     PaymentType paymentType, UUID processedByUserId, String notes) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new BusinessException("At least one invoice is required");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Payment amount must be positive");
        }
        if (paymentType == null) {
            throw new BusinessException("Payment type is required");
        }
        if (processedByUserId == null) {
            throw new BusinessException("User ID is required");
        }

        invoiceService.validateInvoicesExist(invoiceIds);

        List<Invoice> invoices = invoiceService.getEntitiesWithBillings(invoiceIds);

        User processedBy = userService.getEntityById(processedByUserId);
        if (processedBy == null) {
            throw new ResourceNotFoundException("User", processedByUserId);
        }

        BigDecimal totalInvoiceAmount = invoices.stream()
            .map(Invoice::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (amount.compareTo(totalInvoiceAmount) > 0) {
            throw new BusinessException("Payment amount exceeds total invoice amount");
        }

        Payment payment = Payment.builder()
            .invoices(invoices)
            .generatedBy(processedBy)
            .amount(amount)
            .paymentDate(OffsetDateTime.now(ZoneOffset.UTC))
            .paymentType(paymentType)
            .notes(notes != null ? notes.trim() : null)
            .build();

        Payment savedPayment = paymentRepository.save(payment);

        Payment paymentWithRelations = paymentRepository
            .findWithInvoicesAndBillingsByPaymentId(savedPayment.getPaymentId())
            .orElseThrow(() -> new RuntimeException("Payment not found after save"));

        List<UUID> patientIds = paymentWithRelations.getPatients().stream()
            .map(Patient::getPatientId)
            .collect(Collectors.toList());

        eventPublisher.publishEvent(new PaymentProcessed(
            paymentWithRelations.getPaymentId(), invoiceIds, amount, paymentType, patientIds));

        log.info("Payment processed: {} for amount {} across {} invoices",
            paymentWithRelations.getPaymentId(), amount, invoiceIds.size());

        return paymentMapper.toDto(paymentWithRelations);
    }

    /**
     * Retrieves payment information by ID.
     *
     * @param paymentId the payment ID
     * @return the PaymentDto
     * @throws BusinessException if paymentId is null
     * @throws ResourceNotFoundException if payment not found
     */
    @Transactional(readOnly = true)
    public PaymentDto getPaymentById(UUID paymentId) {
        if (paymentId == null) {
            throw new BusinessException("Payment ID is required");
        }

        Payment payment = paymentRepository.findWithInvoicesAndBillingsByPaymentId(paymentId)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        return paymentMapper.toDto(payment);
    }

    /**
     * Retrieves all payments.
     *
     * @return list of PaymentDto
     */
    @Transactional(readOnly = true)
    public List<PaymentDto> getAllPayments() {
        return paymentRepository.findAll().stream()
            .map(paymentMapper::toDto)
            .collect(Collectors.toList());
    }

    /**
     * Internal method for service-to-service communication.
     * Returns Payment entity directly for internal use.
     *
     * @param paymentId the payment ID
     * @return the Payment entity or null if not found
     */
    @Transactional(readOnly = true)
    public Payment getEntityById(UUID paymentId) {
        if (paymentId == null) {
            return null;
        }
        return paymentRepository.findWithInvoicesAndBillingsByPaymentId(paymentId).orElse(null);
    }
}
