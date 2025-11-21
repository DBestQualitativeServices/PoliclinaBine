package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.FormPurpose;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Filter criteria for searching FormTemplate entities.
 * All fields are optional and combined with AND logic.
 *
 * Supported filters:
 * - code, name - partial match, case-insensitive
 * - purpose - exact match (enum)
 * - active, isDeleted - boolean filters
 * - createdAfter, createdBefore - date range filtering
 * - createdByUserId - filter by creator user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormTemplateFilterCriteria {
    private String code;                    // Partial match on template code
    private String name;                    // Partial match on template name
    private FormPurpose purpose;            // Exact match (enum)
    private Boolean active;                 // Boolean filter
    private Boolean isDeleted;              // Boolean filter
    private LocalDateTime createdAfter;     // Date range (inclusive)
    private LocalDateTime createdBefore;    // Date range (inclusive)
    private UUID createdByUserId;           // Filter by creator user ID
}
