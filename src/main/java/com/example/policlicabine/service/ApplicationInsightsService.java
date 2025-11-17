package com.example.policlicabine.service;

import com.example.policlicabine.entity.SecurityAuditLog;
import com.example.policlicabine.entity.enums.AuditEventType;
import com.example.policlicabine.entity.enums.AuditSeverity;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.applicationinsights.telemetry.SeverityLevel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending custom telemetry to Azure Application Insights.
 * This service integrates with the security audit system to track security events
 * in Azure Monitor for real-time monitoring and analytics.
 *
 * <p>Features:
 * <ul>
 *   <li>Custom event tracking for security audit events</li>
 *   <li>Metric tracking for security statistics</li>
 *   <li>Exception tracking for errors</li>
 *   <li>Trace messages for diagnostics</li>
 * </ul>
 *
 * <p>Access the data:
 * <ul>
 *   <li>Azure Portal → Your Resource Group → Application Insights</li>
 *   <li>Query with KQL (Kusto Query Language)</li>
 *   <li>Create custom dashboards and alerts</li>
 * </ul>
 *
 * @see SecurityAuditLog
 * @see com.example.policlicabine.listener.SecurityAuditEventListener
 */
@Service
@Slf4j
@ConditionalOnProperty(name = "applicationinsights.enabled", havingValue = "true", matchIfMissing = true)
public class ApplicationInsightsService {

    private final TelemetryClient telemetryClient;

    public ApplicationInsightsService() {
        this.telemetryClient = new TelemetryClient();
        log.info("ApplicationInsightsService initialized - Custom telemetry tracking enabled");
    }

    /**
     * Track a security audit event as a custom event in Application Insights.
     *
     * @param auditLog The security audit log entry to track
     */
    public void trackSecurityEvent(SecurityAuditLog auditLog) {
        try {
            String eventName = buildEventName(auditLog.getEventType());
            Map<String, String> properties = buildEventProperties(auditLog);
            Map<String, Double> metrics = buildEventMetrics(auditLog);

            telemetryClient.trackEvent(eventName, properties, metrics);
            log.debug("Security event tracked: {} for user: {}", eventName, auditLog.getPrincipal());
        } catch (Exception e) {
            log.error("Failed to track security event to Application Insights", e);
        }
    }

    /**
     * Track a security metric (e.g., failed login count, unauthorized access count).
     *
     * @param metricName The name of the metric
     * @param value      The metric value
     */
    public void trackSecurityMetric(String metricName, double value) {
        try {
            telemetryClient.trackMetric(metricName, value);
            log.debug("Security metric tracked: {} = {}", metricName, value);
        } catch (Exception e) {
            log.error("Failed to track security metric to Application Insights", e);
        }
    }

    /**
     * Track a security metric with additional properties.
     *
     * @param metricName The name of the metric
     * @param value      The metric value
     * @param properties Additional properties for filtering/grouping
     */
    public void trackSecurityMetric(String metricName, double value, Map<String, String> properties) {
        try {
            // Note: TelemetryClient.trackMetric with properties requires using MetricTelemetry
            Map<String, String> enrichedProperties = new HashMap<>(properties);
            enrichedProperties.put("metricName", metricName);
            enrichedProperties.put("value", String.valueOf(value));

            telemetryClient.trackEvent("SecurityMetric", enrichedProperties, Map.of(metricName, value));
            log.debug("Security metric with properties tracked: {} = {}", metricName, value);
        } catch (Exception e) {
            log.error("Failed to track security metric with properties to Application Insights", e);
        }
    }

    /**
     * Track an exception related to security operations.
     *
     * @param exception  The exception to track
     * @param properties Additional context properties
     */
    public void trackSecurityException(Exception exception, Map<String, String> properties) {
        try {
            Map<String, String> enrichedProperties = new HashMap<>(properties);
            enrichedProperties.put("exceptionType", exception.getClass().getSimpleName());
            enrichedProperties.put("category", "Security");

            telemetryClient.trackException(exception, enrichedProperties, null);
            log.debug("Security exception tracked: {}", exception.getMessage());
        } catch (Exception e) {
            log.error("Failed to track security exception to Application Insights", e);
        }
    }

    /**
     * Track a trace message for debugging/diagnostics.
     *
     * @param message  The trace message
     * @param severity The severity level
     */
    public void trackTrace(String message, AuditSeverity severity) {
        try {
            SeverityLevel severityLevel = mapSeverity(severity);
            telemetryClient.trackTrace(message, severityLevel);
            log.debug("Trace tracked: {}", message);
        } catch (Exception e) {
            log.error("Failed to track trace to Application Insights", e);
        }
    }

    /**
     * Manually flush telemetry to ensure it's sent immediately.
     * This is useful before application shutdown or in critical scenarios.
     */
    public void flush() {
        try {
            telemetryClient.flush();
            log.debug("Application Insights telemetry flushed");
        } catch (Exception e) {
            log.error("Failed to flush Application Insights telemetry", e);
        }
    }

    /**
     * Build a user-friendly event name from the audit event type.
     *
     * @param eventType The audit event type
     * @return A formatted event name for Application Insights
     */
    private String buildEventName(AuditEventType eventType) {
        return "Security." + eventType.name();
    }

    /**
     * Build custom properties for the security event.
     *
     * @param auditLog The security audit log entry
     * @return A map of properties to attach to the event
     */
    private Map<String, String> buildEventProperties(SecurityAuditLog auditLog) {
        Map<String, String> properties = new HashMap<>();

        // Core properties
        properties.put("eventType", auditLog.getEventType().name());
        properties.put("severity", auditLog.getSeverity().name());
        properties.put("principal", auditLog.getPrincipal() != null ? auditLog.getPrincipal() : "anonymous");

        // User context
        if (auditLog.getUserId() != null) {
            properties.put("userId", auditLog.getUserId());
        }
        if (auditLog.getUserRole() != null) {
            properties.put("userRole", auditLog.getUserRole());
        }

        // Request context
        if (auditLog.getIpAddress() != null) {
            properties.put("ipAddress", auditLog.getIpAddress());
        }
        if (auditLog.getUrl() != null) {
            properties.put("url", auditLog.getUrl());
        }
        if (auditLog.getPathname() != null) {
            properties.put("pathname", auditLog.getPathname());
        }
        if (auditLog.getUserAgent() != null) {
            properties.put("userAgent", auditLog.getUserAgent());
        }
        if (auditLog.getSessionId() != null) {
            properties.put("sessionId", auditLog.getSessionId());
        }

        // Event details
        if (auditLog.getReason() != null) {
            properties.put("reason", auditLog.getReason());
        }
        if (auditLog.getError() != null) {
            properties.put("error", auditLog.getError());
        }

        // Timestamp
        properties.put("timestamp", auditLog.getTimestamp().toString());

        return properties;
    }

    /**
     * Build metrics for the security event.
     *
     * @param auditLog The security audit log entry
     * @return A map of metrics to attach to the event
     */
    private Map<String, Double> buildEventMetrics(SecurityAuditLog auditLog) {
        Map<String, Double> metrics = new HashMap<>();

        // Add severity as a numeric metric for aggregation
        metrics.put("severityLevel", (double) auditLog.getSeverity().ordinal());

        // Add event type as a numeric code for aggregation
        metrics.put("eventTypeCode", (double) auditLog.getEventType().ordinal());

        return metrics;
    }

    /**
     * Map AuditSeverity to Application Insights SeverityLevel.
     *
     * @param severity The audit severity
     * @return The corresponding Application Insights severity level
     */
    private SeverityLevel mapSeverity(AuditSeverity severity) {
        return switch (severity) {
            case CRITICAL -> SeverityLevel.Critical;
            case WARNING -> SeverityLevel.Warning;
            case INFO -> SeverityLevel.Information;
        };
    }
}
