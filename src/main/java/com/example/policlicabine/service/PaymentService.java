package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.entity.Invoice;
import com.example.policlicabine.entity.Payment;
import com.example.policlicabine.entity.Patient;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.entity.enums.PaymentType;
import com.example.policlicabine.event.PaymentProcessed;
import com.example.policlicabine.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Payment entities.
 * Handles payment processing across invoices.
 *
 * Architecture:
 * - Only uses PaymentRepository (single responsibility)
 * - Calls InvoiceService and UserService for validation and entity access
 * - Uses EntityGraph to prevent N+1 queries
 * - BigDecimal for all monetary calculations
 * - Defensive programming for financial operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentService {

    // Only our repository - single responsibility principle
    private final PaymentRepository paymentRepository;

    // Services for validation and entity access
    private final InvoiceService invoiceService;
    private final UserService userService;

    private final ApplicationEventPublisher eventPublisher;

    /**
     * Processes a payment across one or more invoices.
     *
     * Architecture notes:
     * - Uses InvoiceService to validate and get invoice entities with billings (EntityGraph)
     * - Uses UserService to get user entity
     *
     * @param invoiceIds List of invoice identifiers to apply payment to
     * @param amount Payment amount
     * @param paymentType Type of payment (CASH, CARD, etc.)
     * @param processedByUserId User processing the payment
     * @param notes Optional payment notes
     * @return Result containing Payment or error message
     */
    public Result<Payment> processPayment(List<UUID> invoiceIds, BigDecimal amount,
                                         PaymentType paymentType, UUID processedByUserId, String notes) {
        try {
            // Validate inputs
            if (invoiceIds == null || invoiceIds.isEmpty()) {
                return Result.failure("At least one invoice is required");
            }
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.failure("Payment amount must be positive");
            }
            if (paymentType == null) {
                return Result.failure("Payment type is required");
            }
            if (processedByUserId == null) {
                return Result.failure("User ID is required");
            }

            // Validate all invoices exist via InvoiceService
            Result<Void> validationResult = invoiceService.validateInvoicesExist(invoiceIds);
            if (validationResult.isFailure()) {
                return Result.failure(validationResult.getErrorMessage());
            }

            // Get invoices with sessionBillings loaded (EntityGraph) via InvoiceService
            List<Invoice> invoices = invoiceService.getEntitiesWithBillings(invoiceIds);

            // Get user entity via UserService
            User processedBy = userService.getEntityById(processedByUserId);
            if (processedBy == null) {
                return Result.failure("User not found");
            }

            // Validate payment amount against invoice totals
            BigDecimal totalInvoiceAmount = invoices.stream()
                .map(Invoice::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (amount.compareTo(totalInvoiceAmount) > 0) {
                return Result.failure("Payment amount exceeds total invoice amount");
            }

            // Create payment
            Payment payment = Payment.builder()
                .invoices(invoices)
                .generatedBy(processedBy)
                .amount(amount)
                .paymentDate(LocalDateTime.now())
                .paymentType(paymentType)
                .notes(notes != null ? notes.trim() : null)
                .build();

            Payment savedPayment = paymentRepository.save(payment);

            // Publish domain event (extract patient IDs from invoices→sessionBillings→sessions)
            List<UUID> patientIds = savedPayment.getPatients().stream()
                .map(Patient::getPatientId)
                .collect(Collectors.toList());

            eventPublisher.publishEvent(new PaymentProcessed(
                savedPayment.getPaymentId(), invoiceIds, amount, paymentType, patientIds));

            log.info("Payment processed: {} for amount {} across {} invoices",
                savedPayment.getPaymentId(), amount, invoiceIds.size());

            return Result.success(savedPayment);

        } catch (Exception e) {
            log.error("Error processing payment", e);
            return Result.failure("Failed to process payment: " + e.getMessage());
        }
    }

    /**
     * Retrieves a payment by ID.
     *
     * Architecture notes:
     * - Uses EntityGraph to load payment with invoices and billings
     *
     * @param paymentId Payment identifier
     * @return Result containing Payment or error message
     */
    @Transactional(readOnly = true)
    public Result<Payment> getPaymentById(UUID paymentId) {
        try {
            if (paymentId == null) {
                return Result.failure("Payment ID is required");
            }

            // Use EntityGraph to load payment with invoices and their sessionBillings
            Payment payment = paymentRepository.findWithInvoicesAndBillingsByPaymentId(paymentId)
                .orElse(null);
            if (payment == null) {
                return Result.failure("Payment not found");
            }

            return Result.success(payment);

        } catch (Exception e) {
            log.error("Error getting payment by ID", e);
            return Result.failure("Failed to get payment: " + e.getMessage());
        }
    }

    /**
     * Retrieves all payments.
     *
     * @return Result containing list of Payment or error message
     */
    @Transactional(readOnly = true)
    public Result<List<Payment>> getAllPayments() {
        try {
            List<Payment> payments = paymentRepository.findAll();
            return Result.success(payments);

        } catch (Exception e) {
            log.error("Error getting all payments", e);
            return Result.failure("Failed to get payments: " + e.getMessage());
        }
    }
}
