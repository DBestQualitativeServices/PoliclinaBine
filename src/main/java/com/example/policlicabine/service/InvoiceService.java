package com.example.policlicabine.service;

import com.example.policlicabine.dto.InvoiceDto;
import com.example.policlicabine.entity.Invoice;
import com.example.policlicabine.entity.SessionBilling;
import com.example.policlicabine.entity.User;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.mapper.InvoiceMapper;
import com.example.policlicabine.repository.InvoiceRepository;
import com.example.policlicabine.service.base.BaseServiceImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional
public class InvoiceService extends BaseServiceImpl<Invoice, InvoiceDto, UUID> {

    private final InvoiceRepository invoiceRepository;
    private final UserService userService;
    private final InvoiceMapper invoiceMapper;
    private final ApplicationEventPublisher eventPublisher;

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
        if (dto.getIsProforma() != null) {
            entity.setIsProforma(dto.getIsProforma());
        }
    }

    /**
     * Overrides base findById() to use EntityGraph for loading relationships.
     */
    @Override
    @Transactional(readOnly = true)
    public InvoiceDto findById(UUID invoiceId) {
        if (invoiceId == null) {
            throw new BusinessException("Invoice ID is required");
        }

        Invoice invoice = invoiceRepository.findWithSessionBillingsAndPaymentsByInvoiceId(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        return invoiceMapper.toDto(invoice);
    }

    /**
     * Creates a new invoice.
     */
    public InvoiceDto createInvoice(String invoiceNumber, OffsetDateTime invoiceDate,
                                    UUID generatedByUserId, Boolean isProforma,
                                    List<UUID> sessionBillingIds) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new BusinessException("Invoice number is required");
        }
        if (invoiceDate == null) {
            throw new BusinessException("Invoice date is required");
        }
        if (generatedByUserId == null) {
            throw new BusinessException("User ID is required");
        }
        if (sessionBillingIds == null || sessionBillingIds.isEmpty()) {
            throw new BusinessException("At least one session billing is required");
        }

        if (invoiceRepository.existsByInvoiceNumber(invoiceNumber.trim())) {
            throw new BusinessException("Invoice number already exists");
        }

        User user = userService.getEntityById(generatedByUserId);
        if (user == null) {
            throw new ResourceNotFoundException("User", generatedByUserId);
        }

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

        log.info("Invoice created: {} (proforma: {})", invoiceNumber, isProforma);

        return invoiceMapper.toDto(savedInvoice);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findInvoiceById(UUID invoiceId) {
        return findById(invoiceId);
    }

    @Transactional(readOnly = true)
    public InvoiceDto findInvoiceByNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.trim().isEmpty()) {
            throw new BusinessException("Invoice number is required");
        }

        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber.trim())
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", "number: " + invoiceNumber));

        return invoiceMapper.toDto(invoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDto> getAllInvoices() {
        List<Invoice> invoices = invoiceRepository.findAll();

        return invoices.stream()
            .map(invoiceMapper::toDto)
            .collect(Collectors.toList());
    }

    public InvoiceDto convertProformaToFinal(UUID invoiceId, String newInvoiceNumber) {
        if (invoiceId == null) {
            throw new BusinessException("Invoice ID is required");
        }
        if (newInvoiceNumber == null || newInvoiceNumber.trim().isEmpty()) {
            throw new BusinessException("New invoice number is required");
        }

        if (invoiceRepository.existsByInvoiceNumber(newInvoiceNumber.trim())) {
            throw new BusinessException("Invoice number already exists");
        }

        Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        invoice.convertToFinalInvoice(newInvoiceNumber.trim());
        Invoice savedInvoice = invoiceRepository.save(invoice);

        log.info("Proforma invoice {} converted to final invoice {}", invoiceId, newInvoiceNumber);

        return invoiceMapper.toDto(savedInvoice);
    }

    @Transactional(readOnly = true)
    public List<Invoice> getEntitiesWithBillings(List<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            return List.of();
        }
        return invoiceRepository.findAllWithSessionBillingsByInvoiceIdIn(invoiceIds);
    }

    @Transactional(readOnly = true)
    public void validateInvoicesExist(List<UUID> invoiceIds) {
        if (invoiceIds == null || invoiceIds.isEmpty()) {
            throw new BusinessException("Invoice IDs are required");
        }

        long count = invoiceRepository.countByInvoiceIdIn(invoiceIds);
        if (count != invoiceIds.size()) {
            throw new BusinessException("Some invoices not found");
        }
    }
}
