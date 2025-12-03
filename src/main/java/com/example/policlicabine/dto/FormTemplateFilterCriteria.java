package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateFilterCriteria {
    private String name;                    // Partial match on template name
    private Boolean active;                 // Boolean filter
    private Boolean isDeleted;              // Boolean filter
    private LocalDateTime createdAfter;     // Date range (inclusive)
    private LocalDateTime createdBefore;    // Date range (inclusive)
    private UUID createdByUserId;           // Filter by creator user ID
}
