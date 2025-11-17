package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingDiscountDto {

    private UUID discountId;
    private BigDecimal amount;
    private String reason;
    private UUID appliedByUserId;
    private String appliedByUsername;
    private OffsetDateTime createdAt;
}
