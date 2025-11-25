package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.Specialty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationTypeFilterCriteria {
    private String name;                    // Partial match, case-insensitive
    private Specialty specialty;            // Exact match
    private Boolean isActive;               // true/false/null (all)
    private BigDecimal minPrice;            // >= minPrice
    private BigDecimal maxPrice;            // <= maxPrice
    private String priceCurrency;           // Exact match (RON, EUR, USD)
    private Integer minDurationMinutes;     // >= minDuration
    private Integer maxDurationMinutes;     // <= maxDuration
    private Boolean requiresSurgeryRoom;    // true/false/null (all)
    private OffsetDateTime createdAfter;    // >= createdAfter
    private OffsetDateTime createdBefore;   // <= createdBefore
}
