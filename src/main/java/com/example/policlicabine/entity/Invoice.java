package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.PaymentStatus;
import com.example.policlicabine.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {

    @Id
    private UUID invoiceId;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime invoiceDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Builder.Default
    private Boolean isProforma = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "invoice_session_billings",
        joinColumns = @JoinColumn(name = "invoice_id"),
        inverseJoinColumns = @JoinColumn(name = "billing_id")
    )
    @BatchSize(size = 10)
    private List<SessionBilling> sessionBillings;

    @ManyToMany(mappedBy = "invoices", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Payment> payments;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    void generateId() {
        if (invoiceId == null) {
            invoiceId = UUID.randomUUID();
        }
    }

    public BigDecimal getTotalAmount() {
        if (sessionBillings == null || sessionBillings.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return sessionBillings.stream()
            .map(SessionBilling::getFinalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getTotalPaid() {
        if (payments == null || payments.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return payments.stream()
            .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOutstandingAmount() {
        return getTotalAmount().subtract(getTotalPaid());
    }

    public PaymentStatus getPaymentStatus() {
        if (isProforma) {
            return PaymentStatus.PENDING;
        }

        BigDecimal totalPaid = getTotalPaid();
        BigDecimal totalAmount = getTotalAmount();

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.PENDING;
        } else if (totalPaid.compareTo(totalAmount) >= 0) {
            return PaymentStatus.FULLY_PAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    public boolean canConvertToFinalInvoice() {
        return isProforma && (payments == null || payments.isEmpty());
    }

    public void convertToFinalInvoice(String newInvoiceNumber) {
        if (!canConvertToFinalInvoice()) {
            throw new IllegalStateException("Cannot convert proforma to final invoice: " +
                    (isProforma ? "invoice has existing payments" : "invoice is not proforma"));
        }
        this.isProforma = false;
        this.invoiceNumber = newInvoiceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice)) return false;
        Invoice invoice = (Invoice) o;
        return invoiceId != null && Objects.equals(invoiceId, invoice.invoiceId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Invoice{" +
                "invoiceId=" + invoiceId +
                ", invoiceNumber='" + invoiceNumber + '\'' +
                ", invoiceDate=" + invoiceDate +
                ", isProforma=" + isProforma +
                '}';
    }
}
