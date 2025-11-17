package com.example.policlicabine.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "billing_discounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDiscount {

    @Id
    private UUID discountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_billing_id", nullable = false)
    private SessionBilling sessionBilling;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_by", nullable = false)
    private User appliedBy;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime createdAt;

    @PrePersist
    void generateId() {
        if (discountId == null) {
            discountId = UUID.randomUUID();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BillingDiscount)) return false;
        BillingDiscount that = (BillingDiscount) o;
        return discountId != null && Objects.equals(discountId, that.discountId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "BillingDiscount{" +
                "discountId=" + discountId +
                ", amount=" + amount +
                ", reason='" + reason + '\'' +
                '}';
    }
}
