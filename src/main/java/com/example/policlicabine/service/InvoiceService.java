package com.example.policlicabine.service;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.InvoiceDto;
import com.example.policlicabine.entity.Invoice;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.event.InvoiceConvertedToFinal;
import com.example.policlicabine.event.InvoiceCreated;
import com.example.policlicabine.mapper.InvoiceMapper;
import com.example.policlicabine.repository.InvoiceRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing Invoice entities.
 *
 * Architecture:
 * - Extends BaseServiceImpl for common CRUD operations (getEntityById, validateExists, etc.)
 * - Only uses InvoiceRepository (single responsibility)
 * - Calls UserService for user validation and entity access
 * - Uses EntityGraph to prevent N+1 queries
 * - Follows service-to-service communication pattern
 *
 * Inherited Methods (from BaseServiceImpl):
 * - findById(UUID) → Result&lt;InvoiceDto&gt; (overridden to use EntityGraph)
 * - validateExists(UUID) → Result&lt;Void&gt;
 * - getEntityById(UUID) → Invoice
 * - getEntitiesByIds(List&lt;UUID&gt;) → List&lt;Invoice&gt;
 * - findAll() → Result&lt;List&lt;InvoiceDto&gt;&gt;
 *
 * Note: findById() is overridden to use EntityGraph for loading relationships.
 */
@Service
@Slf4j
@Transactional
public class InvoiceService extends BaseServiceImpl<Invoice, InvoiceDto, UUID> {

    // Only our repository - single responsibility principle
    private final InvoiceRepository invoiceRepository;

    // Service for user validation and entity access
    private final UserService userService;

    private final InvoiceMapper invoiceMapper;
    private final ApplicationEventPublisher eventPublisher;

    // EntityManager for creating entity references without DB hits
    @PersistenceContext
    private EntityManager entityManager;

    public InvoiceService(InvoiceRepository invoiceRepository,
                         UserService userService,
                         InvoiceMapper invoiceMapper,
                         ApplicationEventPublisher eventPublisher) {
        super(invoiceRepository, invoiceMapper);
        this.invoiceRepository = invoiceRepository;
        this.userService = userService;
        this.invoiceMapper = invoiceMapper;
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected InvoiceDto toDto(Invoice entity) {
        return invoiceMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Invoice";
    }

    @Override
    protected void updateEntityFromDto(Invoice entity, InvoiceDto dto) {
        // Most Invoice fields are calculated or immutable
        // Only isProforma can be updated (e.g., converting proforma to final invoice)
        if (dto.getIsProforma() != null) {
            entity.setIsProforma(dto.getIsProforma());
        }
        // Don't update: invoiceNumber, invoiceDate, generatedBy, sessionBillings, amounts, createdAt
        // These are either immutable or calculated
    }

    /**
     * Overrides base findById() to use EntityGraph for loading relationships.
     * This ensures sessionBillings and payments are loaded efficiently.
     *
     * @param invoiceId Invoice identifier
     * @return Result containing InvoiceDto or error message
     */
    @Override
    @Transactional(readOnly = true)
    public Result<InvoiceDto> findById(UUID invoiceId) {
        try {
            if (invoiceId == null) {
                return Result.failure("Invoice ID is required");
            }

            // Use EntityGraph to load invoice with sessionBillings and payments
            Invoice invoice = invoiceRepository.findWithSessionBillingsAndPaymentsByInvoiceId(invoiceId)
                .orElse(null);
            if (invoice == null) {
                return Result.failure("Invoice not found");
            }

            return Result.success(invoiceMapper.toDto(invoice));

        } catch (Exception e) {
            log.error("Error finding invoice by ID", e);
            return Result.failure("Failed to find invoice: " + e.getMessage());
        }
    }

    /**
     * Creates a new invoice.
     *
     * Architecture notes:
     * - Uses UserService to get user entity
     * - Uses EntityManager.getReference() for SessionBilling FKs
     *
     * @param invoiceNumber Invoice number (must be unique)
     * @param invoiceDate Invoice date
     * @param generatedByUserId User who generated the invoice
     * @param isProforma Whether this is a proforma invoice
     * @param sessionBillingIds List of session billing IDs to include
     * @return Result containing InvoiceDto or error message
     */
    public Result<InvoiceDto> createInvoice(String invoiceNumber, LocalDate invoiceDate,
                                           UUID generatedByUserId, Boolean isProforma,
                                           List<UUID> sessionBillingIds) {
        try {
            if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                return Result.failure("Invoice number is required");
            }
            if (invoiceDate == null) {
                return Result.failure("Invoice date is required");
            }
            if (generatedByUserId == null) {
                return Result.failure("User ID is required");
            }
            if (sessionBillingIds == null || sessionBillingIds.isEmpty()) {
                return Result.failure("At least one session billing is required");
            }

            // Check for duplicate invoice number
            if (invoiceRepository.existsByInvoiceNumber(invoiceNumber.trim())) {
                return Result.failure("Invoice number already exists");
            }

            // Get user entity via UserService
            User user = userService.getEntityById(generatedByUserId);
            if (user == null) {
                return Result.failure("User not found");
            }

            // Use EntityManager.getReference() for SessionBilling entities (no DB hits)
            List<SessionBilling> sessionBillings = sessionBillingIds.stream()
                .map(id -> entityManager.getReference(SessionBilling.class, id))
                .collect(Collectors.toList());

            Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber.trim())
                .invoiceDate(invoiceDate)
                .generatedBy(user)
                .isProforma(isProforma != null ? isProforma : false)
                .sessionBillings(sessionBillings)
                .build();

            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Publish domain event
            eventPublisher.publishEvent(new InvoiceCreated(
                savedInvoice.getInvoiceId(),
                savedInvoice.getInvoiceNumber(),
                savedInvoice.getInvoiceDate(),
                savedInvoice.getGeneratedBy().getUserId(),
                savedInvoice.getIsProforma(),
                sessionBillingIds,
                savedInvoice.getTotalAmount()
            ));

            log.info("Invoice created: {} (proforma: {})", invoiceNumber, isProforma);

            return Result.success(invoiceMapper.toDto(savedInvoice));

        } catch (Exception e) {
            log.error("Error creating invoice", e);
            return Result.failure("Failed to create invoice: " + e.getMessage());
        }
    }

    /**
     * Finds an invoice by ID.
     * Delegates to overridden findById() method which uses EntityGraph.
     *
     * @param invoiceId Invoice identifier
     * @return Result containing InvoiceDto or error message
     */
    @Transactional(readOnly = true)
    public Result<InvoiceDto> findInvoiceById(UUID invoiceId) {
        return findById(invoiceId);
    }

    /**
     * Finds an invoice by invoice number.
     *
     * @param invoiceNumber Invoice number
     * @return Result containing InvoiceDto or error message
     */
    @Transactional(readOnly = true)
    public Result<InvoiceDto> findInvoiceByNumber(String invoiceNumber) {
        try {
            if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
                return Result.failure("Invoice number is required");
            }

            Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber.trim())
                .orElse(null);
            if (invoice == null) {
                return Result.failure("Invoice not found with number: " + invoiceNumber);
            }

            return Result.success(invoiceMapper.toDto(invoice));

        } catch (Exception e) {
            log.error("Error finding invoice by number", e);
            return Result.failure("Failed to find invoice: " + e.getMessage());
        }
    }

    /**
     * Retrieves all invoices.
     *
     * @return Result containing list of InvoiceDto or error message
     */
    @Transactional(readOnly = true)
    public Result<List<InvoiceDto>> getAllInvoices() {
        try {
            List<Invoice> invoices = invoiceRepository.findAll();

            List<InvoiceDto> invoiceDtos = invoices.stream()
                .map(invoiceMapper::toDto)
                .collect(Collectors.toList());

            return Result.success(invoiceDtos);

        } catch (Exception e) {
            log.error("Error getting all invoices", e);
            return Result.failure("Failed to get invoices: " + e.getMessage());
        }
    }

    /**
     * Converts a proforma invoice to a final invoice.
     *
     * @param invoiceId Invoice identifier
     * @param newInvoiceNumber New invoice number for the final invoice
     * @return Result containing updated InvoiceDto or error message
     */
    public Result<InvoiceDto> convertProformaToFinal(UUID invoiceId, String newInvoiceNumber) {
        try {
            if (invoiceId == null) {
                return Result.failure("Invoice ID is required");
            }
            if (newInvoiceNumber == null || newInvoiceNumber.trim().isEmpty()) {
                return Result.failure("New invoice number is required");
            }

            // Check if new invoice number already exists
            if (invoiceRepository.existsByInvoiceNumber(newInvoiceNumber.trim())) {
                return Result.failure("Invoice number already exists");
            }

            Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
            if (invoice == null) {
                return Result.failure("Invoice not found");
            }

            // Store old invoice number for event
            String oldInvoiceNumber = invoice.getInvoiceNumber();

            // Use entity method for business logic
            invoice.convertToFinalInvoice(newInvoiceNumber.trim());
            Invoice savedInvoice = invoiceRepository.save(invoice);

            // Publish domain event
            eventPublisher.publishEvent(new InvoiceConvertedToFinal(
                invoiceId, oldInvoiceNumber, newInvoiceNumber.trim()
            ));

            log.info("Proforma invoice {} converted to final invoice {}",
                invoiceId, newInvoiceNumber);

            return Result.success(invoiceMapper.toDto(savedInvoice));

        } catch (IllegalStateException e) {
            return Result.failure(e.getMessage());
        } catch (Exception e) {
            log.error("Error converting proforma to final invoice", e);
            return Result.failure("Failed to convert invoice: " + e.getMessage());
        }
    }

    // ============= INTERNAL METHODS FOR SERVICE-TO-SERVICE COMMUNICATION =============
    // Note: The following methods are inherited from BaseServiceImpl:
    // - getEntityById(UUID) → Invoice (simple lookup without EntityGraph)
    // - validateExists(UUID) → Result<Void>
    // - getEntitiesByIds(List<UUID>) → List<Invoice>

    /**
     * INTERNAL: Gets invoice entities with sessionBillings loaded.
     * Uses EntityGraph to prevent N+1 queries.
     * Used by other services when they need invoices with billing data.
     *
     * @param invoiceIds List of invoice identifiers
     * @return List of Invoice entities with sessionBillings loaded
     */
    @Transactional(readOnly = true)
    public List<Invoice> getEntitiesWithBillings(List<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return List.of();
        }
        return invoiceRepository.findAllWithSessionBillingsByInvoiceIdIn(invoiceIds);
    }

    /**
     * INTERNAL: Validates that all invoices exist.
     * Used by other services (e.g., PaymentService) to validate invoice references.
     * Returns Result for clear error messaging.
     *
     * @param invoiceIds List of invoice identifiers
     * @return Result success if all invoices exist, failure with message otherwise
     */
    @Transactional(readOnly = true)
    public Result<Void> validateInvoicesExist(List<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return Result.failure("Invoice IDs are required");
        }

        long count = invoiceRepository.countByInvoiceIdIn(invoiceIds);
        if (count != invoiceIds.size()) {
            return Result.failure("Some invoices not found");
        }

        return Result.success(null);
    }
}
