package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Invoice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    boolean existsByInvoiceNumber(String invoiceNumber);

    long countByInvoiceIdIn(List<UUID> invoiceIds);

    // EntityGraph methods to prevent N+1 queries
    @EntityGraph(attributePaths = {"sessionBillings", "generatedBy"})
    Optional<Invoice> findWithSessionBillingsByInvoiceId(UUID invoiceId);

    @EntityGraph(attributePaths = {"sessionBillings", "payments", "generatedBy"})
    Optional<Invoice> findWithSessionBillingsAndPaymentsByInvoiceId(UUID invoiceId);

    @EntityGraph(attributePaths = {"sessionBillings", "sessionBillings.session", "generatedBy"})
    List<Invoice> findAllWithSessionBillingsByInvoiceIdIn(List<UUID> invoiceIds);
}
