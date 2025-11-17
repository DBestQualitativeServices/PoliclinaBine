package com.example.policlicabine.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record InvoiceCreated(
    UUID invoiceId,
    String invoiceNumber,
    OffsetDateTime invoiceDate,
    UUID generatedByUserId,
    Boolean isProforma,
    List<UUID> sessionBillingIds,
    BigDecimal totalAmount
) {}
