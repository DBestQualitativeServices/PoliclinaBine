package com.example.policlicabine.service;

import com.example.policlicabine.dto.SecurityAuditSearchDto;
import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.repository.SecurityAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Service for exporting security audit logs to various formats.
 * Supports CSV and JSON exports with filtering.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SecurityAuditExportService {

    private final SecurityAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Export audit logs to CSV format.
     *
     * @param searchDto Search criteria for filtering
     * @return CSV data as byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportToCsv(SecurityAuditSearchDto searchDto) {
        try {
            // Get all matching records (use pagination to avoid memory issues)
            Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "timestamp"));

            Page<SecurityAuditLog> results = auditLogRepository.searchAuditLogs(
                searchDto.getEventType(),
                searchDto.getSeverity(),
                searchDto.getPrincipal(),
                searchDto.getUserId(),
                searchDto.getAfter(),
                searchDto.getBefore(),
                pageable
            );

            // Build CSV
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(baos, true, StandardCharsets.UTF_8);

            // Write header
            writer.println("Audit ID,Event Type,Severity,Principal,User ID,User Role,Timestamp,IP Address," +
                "URL,Pathname,Allowed Roles,Reason,Error,Session ID,Created At");

            // Write data rows
            for (SecurityAuditLog log : results.getContent()) {
                writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    escapeCsv(log.getAuditId() != null ? log.getAuditId().toString() : ""),
                    escapeCsv(log.getEventType() != null ? log.getEventType().name() : ""),
                    escapeCsv(log.getSeverity() != null ? log.getSeverity().name() : ""),
                    escapeCsv(log.getPrincipal()),
                    escapeCsv(log.getUserId()),
                    escapeCsv(log.getUserRole()),
                    escapeCsv(log.getTimestamp() != null ? log.getTimestamp().toString() : ""),
                    escapeCsv(log.getIpAddress()),
                    escapeCsv(log.getUrl()),
                    escapeCsv(log.getPathname()),
                    escapeCsv(log.getAllowedRoles()),
                    escapeCsv(log.getReason()),
                    escapeCsv(log.getError()),
                    escapeCsv(log.getSessionId()),
                    escapeCsv(log.getCreatedAt() != null ? log.getCreatedAt().toString() : "")
                ));
            }

            writer.flush();

            log.info("Exported {} audit log entries to CSV", results.getNumberOfElements());

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export audit logs to CSV", e);
            throw new RuntimeException("CSV export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Export audit logs to JSON format.
     *
     * @param searchDto Search criteria for filtering
     * @return JSON data as byte array
     */
    @Transactional(readOnly = true)
    public byte[] exportToJson(SecurityAuditSearchDto searchDto) {
        try {
            // Get all matching records
            Pageable pageable = PageRequest.of(0, 10000, Sort.by(Sort.Direction.DESC, "timestamp"));

            Page<SecurityAuditLog> results = auditLogRepository.searchAuditLogs(
                searchDto.getEventType(),
                searchDto.getSeverity(),
                searchDto.getPrincipal(),
                searchDto.getUserId(),
                searchDto.getAfter(),
                searchDto.getBefore(),
                pageable
            );

            // Convert to JSON using injected ObjectMapper (already configured with JavaTimeModule)
            List<SecurityAuditLog> logs = results.getContent();

            byte[] jsonBytes = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(logs)
                .getBytes(StandardCharsets.UTF_8);

            log.info("Exported {} audit log entries to JSON", logs.size());

            return jsonBytes;

        } catch (Exception e) {
            log.error("Failed to export audit logs to JSON", e);
            throw new RuntimeException("JSON export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Escape CSV field values.
     * Handles quotes, commas, and newlines.
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        // If value contains comma, quote, or newline, wrap in quotes and escape internal quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
