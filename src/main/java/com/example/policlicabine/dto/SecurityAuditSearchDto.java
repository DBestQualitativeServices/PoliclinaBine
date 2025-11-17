package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * DTO for searching security audit logs with multiple filters.
 * Used in admin dashboard for advanced queries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditSearchDto {

    private AuditEventType eventType;
    private AuditSeverity severity;
    private String principal;
    private String userId;
    private OffsetDateTime after;
    private OffsetDateTime before;
    private String ipAddress;
    private String pathname;

    // Pagination
    private Integer page;
    private Integer size;
    private String sortBy;
    private String sortDirection;
}
