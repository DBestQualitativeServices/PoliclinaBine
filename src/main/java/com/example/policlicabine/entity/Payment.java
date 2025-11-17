package com.example.policlicabine.entity;

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
import java.util.stream.Collectors;

@Entity
@Table(name = "payments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    private UUID paymentId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "payment_invoices",
        joinColumns = @JoinColumn(name = "payment_id"),
        inverseJoinColumns = @JoinColumn(name = "invoice_id")
    )
    @BatchSize(size = 10)
    private List<Invoice> invoices;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "generated_by", nullable = false)
    private User generatedBy;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Builder.Default
    @Column(length = 3)
    private String currency = "RON";

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime paymentDate;

    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    void generateId() {
        if (paymentId == null) {
            paymentId = UUID.randomUUID();
        }
    }

    public List<AppointmentSession> getSessions() {
        if (invoices == null || invoices.isEmpty()) {
            return new ArrayList<>();
        }
        return invoices.stream()
            .flatMap(invoice -> invoice.getSessionBillings().stream())
            .map(SessionBilling::getSession)
            .collect(Collectors.toList());
    }

    public List<Patient> getPatients() {
        return getSessions().stream()
            .map(AppointmentSession::getPatient)
            .distinct()
            .collect(Collectors.toList());
    }

    public boolean canBeAppliedToProformaInvoice() {
        return invoices.stream().noneMatch(Invoice::getIsProforma);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Payment)) return false;
        Payment payment = (Payment) o;
        return paymentId != null && Objects.equals(paymentId, payment.paymentId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", amount=" + amount +
                ", currency='" + currency + '\'' +
                ", paymentDate=" + paymentDate +
                ", paymentType=" + paymentType +
                '}';
    }
}
