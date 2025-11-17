package com.example.policlicabine.entity;

import com.example.policlicabine.entity.enums.PaymentStatus;
import com.example.policlicabine.entity.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "session_billing")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionBilling {

    @Id
    private UUID billingId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    private AppointmentSession session;

    @OneToMany(mappedBy = "sessionBilling", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<BillingDiscount> discounts;

    @ManyToMany(mappedBy = "sessionBillings", fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Invoice> invoices;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    void generateId() {
        if (billingId == null) {
            billingId = UUID.randomUUID();
        }
    }

    public BigDecimal getSubtotalAmount() {
        return session != null ? session.getSubtotalAmount() : BigDecimal.ZERO;
    }

    public BigDecimal getTotalDiscountAmount() {
        if (discounts == null || discounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return discounts.stream()
            .map(discount -> discount.getAmount() != null ? discount.getAmount() : BigDecimal.ZERO)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getFinalAmount() {
        return getSubtotalAmount().subtract(getTotalDiscountAmount());
    }

    public PaymentStatus getPaymentStatus() {
        if (invoices == null || invoices.isEmpty()) {
            return PaymentStatus.PENDING;
        }

        // Simplified payment status calculation to avoid circular navigation
        // Get total amount paid from all related invoices
        BigDecimal totalPaid = invoices.stream()
            .filter(invoice -> !invoice.getIsProforma())
            .flatMap(invoice -> invoice.getPayments() != null ? invoice.getPayments().stream() : java.util.stream.Stream.empty())
            .filter(payment -> payment.getPaymentType() != PaymentType.REFUND)
            .map(Payment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal finalAmount = getFinalAmount();

        if (totalPaid.compareTo(BigDecimal.ZERO) <= 0) {
            return PaymentStatus.PENDING;
        } else if (totalPaid.compareTo(finalAmount) >= 0) {
            return PaymentStatus.FULLY_PAID;
        } else {
            return PaymentStatus.PARTIALLY_PAID;
        }
    }

    public Patient getPatient() {
        return session != null ? session.getPatient() : null;
    }

    public void addDiscount(User appliedBy, BigDecimal amount, String reason) {
        if (discounts == null) {
            discounts = new ArrayList<>();
        }

        BillingDiscount discount = BillingDiscount.builder()
            .sessionBilling(this)
            .appliedBy(appliedBy)
            .amount(amount)
            .reason(reason)
            .build();

        discounts.add(discount);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SessionBilling)) return false;
        SessionBilling that = (SessionBilling) o;
        return billingId != null && Objects.equals(billingId, that.billingId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "SessionBilling{" +
                "billingId=" + billingId +
                ", finalAmount=" + getFinalAmount() +
                '}';
    }
}
