package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.SecurityAuditLogDto;
import com.example.policlicabine.dto.SecurityAuditSearchDto;
import com.example.policlicabine.dto.SecurityAuditStatsDto;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.example.policlicabine.service.SecurityAuditExportService;
import com.example.policlicabine.service.SecurityAuditRetentionService;
import com.example.policlicabine.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * REST controller for security audit logging.
 * Provides endpoints for logging events from frontend and admin dashboard for querying logs.
 */
@RestController
@RequestMapping("/api/security")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Security Audit", description = "Security audit logging and monitoring")
public class SecurityAuditController {

    private final SecurityAuditService auditService;
    private final SecurityAuditExportService exportService;
    private final SecurityAuditRetentionService retentionService;

    /**
     * Log security event from frontend.
     * PUBLIC ENDPOINT - No authentication required to prevent recursive errors.
     */
    @PostMapping("/log")
    @StandardApiResponses
    @Operation(summary = "Log security event from frontend", description = "Endpoint for frontend to log security events")
    public ResponseEntity<Void> logSecurityEvent(@RequestBody SecurityAuditLogDto eventDto) {
        try {
            // Log asynchronously (don't wait for result)
            auditService.logEventAsync(eventDto);

            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to log security event from frontend", e);
            // Return 200 anyway to prevent breaking frontend
            return ResponseEntity.ok().build();
        }
    }

    /**
     * Get single audit log by ID.
     * ADMIN/MANAGER only.
     */
    @GetMapping("/admin/events/{auditId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Get audit log by ID", description = "Retrieve a single audit log entry")
    public ResponseEntity<Result<SecurityAuditLogDto>> getAuditLog(@PathVariable UUID auditId) {
        Result<SecurityAuditLogDto> result = auditService.findById(auditId);

        return result.isSuccess()
            ? ResponseEntity.ok(result)
            : ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
    }

    /**
     * Search audit logs with filters.
     * ADMIN/MANAGER only.
     */
    @GetMapping("/admin/events")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Search audit logs", description = "Search and filter audit logs with pagination")
    public ResponseEntity<Result<Page<SecurityAuditLogDto>>> searchAuditLogs(
        @RequestParam(required = false) AuditEventType eventType,
        @RequestParam(required = false) AuditSeverity severity,
        @RequestParam(required = false) String principal,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before,
        @RequestParam(required = false) String ipAddress,
        @RequestParam(required = false) String pathname,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "timestamp") String sortBy,
        @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        SecurityAuditSearchDto searchDto = SecurityAuditSearchDto.builder()
            .eventType(eventType)
            .severity(severity)
            .principal(principal)
            .userId(userId)
            .after(after)
            .before(before)
            .ipAddress(ipAddress)
            .pathname(pathname)
            .page(page)
            .size(size)
            .sortBy(sortBy)
            .sortDirection(sortDirection)
            .build();

        Result<Page<SecurityAuditLogDto>> result = auditService.search(searchDto);

        return ResponseEntity.ok(result);
    }

    /**
     * Get audit log statistics.
     * ADMIN/MANAGER only.
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Get audit statistics", description = "Get security audit statistics and metrics")
    public ResponseEntity<Result<SecurityAuditStatsDto>> getStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before
    ) {
        Result<SecurityAuditStatsDto> result = auditService.getStatistics(after, before);

        return ResponseEntity.ok(result);
    }

    /**
     * Export audit logs to CSV.
     * ADMIN only.
     */
    @GetMapping("/admin/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Export audit logs to CSV", description = "Export filtered audit logs as CSV file")
    public ResponseEntity<byte[]> exportToCsv(
        @RequestParam(required = false) AuditEventType eventType,
        @RequestParam(required = false) AuditSeverity severity,
        @RequestParam(required = false) String principal,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before
    ) {
        SecurityAuditSearchDto searchDto = SecurityAuditSearchDto.builder()
            .eventType(eventType)
            .severity(severity)
            .principal(principal)
            .userId(userId)
            .after(after)
            .before(before)
            .build();

        byte[] csvData = exportService.exportToCsv(searchDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment",
            "security-audit-logs-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".csv");

        return ResponseEntity.ok()
            .headers(headers)
            .body(csvData);
    }

    /**
     * Export audit logs to JSON.
     * ADMIN only.
     */
    @GetMapping("/admin/export/json")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Export audit logs to JSON", description = "Export filtered audit logs as JSON file")
    public ResponseEntity<byte[]> exportToJson(
        @RequestParam(required = false) AuditEventType eventType,
        @RequestParam(required = false) AuditSeverity severity,
        @RequestParam(required = false) String principal,
        @RequestParam(required = false) String userId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime after,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime before
    ) {
        SecurityAuditSearchDto searchDto = SecurityAuditSearchDto.builder()
            .eventType(eventType)
            .severity(severity)
            .principal(principal)
            .userId(userId)
            .after(after)
            .before(before)
            .build();

        byte[] jsonData = exportService.exportToJson(searchDto);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setContentDispositionFormData("attachment",
            "security-audit-logs-" + OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE) + ".json");

        return ResponseEntity.ok()
            .headers(headers)
            .body(jsonData);
    }

    /**
     * Manually trigger retention cleanup.
     * ADMIN only.
     */
    @PostMapping("/admin/cleanup")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearer-jwt")
    @StandardApiResponses
    @Operation(summary = "Manually cleanup old audit logs", description = "Trigger manual cleanup of audit logs older than specified days")
    public ResponseEntity<String> manualCleanup(@RequestParam(defaultValue = "90") int daysToKeep) {
        try {
            int deletedCount = retentionService.manualCleanup(daysToKeep);

            return ResponseEntity.ok(String.format("Successfully deleted %d old audit log entries", deletedCount));

        } catch (Exception e) {
            log.error("Manual cleanup failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Cleanup failed: " + e.getMessage());
        }
    }
}
