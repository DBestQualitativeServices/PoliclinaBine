package com.example.policlicabine.repository;

import com.example.policlicabine.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // EntityGraph methods to prevent N+1 queries
    @EntityGraph(attributePaths = {"invoices", "generatedBy"})
    Optional<Payment> findWithInvoicesByPaymentId(UUID paymentId);

    @EntityGraph(attributePaths = {"invoices", "invoices.sessionBillings", "generatedBy"})
    Optional<Payment> findWithInvoicesAndBillingsByPaymentId(UUID paymentId);
}
