package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for security audit statistics.
 * Used in admin dashboard for overview and reporting.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityAuditStatsDto {

    private Long totalEvents;
    private Map<String, Long> eventTypeCounts;
    private Map<String, Long> severityCounts;
    private Long criticalEvents;
    private Long warningEvents;
    private Long infoEvents;
    private Long failedLoginAttempts;
    private Long unauthorizedAccessAttempts;
    private Long suspiciousActivities;

    /**
     * Top principals with most events
     */
    private Map<String, Long> topPrincipals;

    /**
     * Recent activity summary
     */
    private String period;  // e.g., "Last 24 hours", "Last 7 days"
}
