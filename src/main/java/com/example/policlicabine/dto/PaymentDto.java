package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentDto {

    private UUID paymentId;
    private List<UUID> invoiceIds;
    private UUID generatedByUserId;
    private String generatedByUsername;
    private BigDecimal amount;
    private String currency;
    private OffsetDateTime paymentDate;
    private PaymentType paymentType;
    private String notes;
    private OffsetDateTime createdAt;
}
