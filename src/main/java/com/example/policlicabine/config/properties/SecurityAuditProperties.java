package com.example.policlicabine.config.properties;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for security audit logging.
 * Centralized configuration for audit, alerting, and retention settings.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "security.audit")
public class SecurityAuditProperties {

    /**
     * Enable/disable security audit logging globally.
     */
    private boolean enabled = true;

    /**
     * Async configuration.
     */
    private Async async = new Async();

    /**
     * Retention policy configuration.
     */
    private Retention retention = new Retention();

    /**
     * Alert configuration.
     */
    private Alert alert = new Alert();

    @Data
    public static class Async {
        /**
         * Enable/disable async audit logging.
         */
        private boolean enabled = true;

        /**
         * Core pool size for audit log executor.
         */
        @Min(1)
        private int corePoolSize = 2;

        /**
         * Maximum pool size for audit log executor.
         */
        @Min(1)
        private int maxPoolSize = 5;

        /**
         * Queue capacity for audit log executor.
         */
        @Min(10)
        private int queueCapacity = 500;
    }

    @Data
    public static class Retention {
        /**
         * Enable/disable automatic retention cleanup.
         */
        private boolean enabled = true;

        /**
         * Number of days to retain audit logs.
         */
        @Min(1)
        private int days = 90;

        /**
         * Cron expression for retention cleanup schedule.
         * Default: daily at 2:00 AM
         */
        @NotBlank
        private String cron = "0 0 2 * * *";
    }

    @Data
    public static class Alert {
        /**
         * Enable/disable security alerting.
         */
        private boolean enabled = true;

        /**
         * Email address for security alerts.
         */
        @Email
        private String email = "admin@example.com";

        /**
         * Alert threshold configuration.
         */
        private Threshold threshold = new Threshold();

        /**
         * Alert window in minutes (for detecting patterns).
         */
        @Min(1)
        private int windowMinutes = 10;

        @Data
        public static class Threshold {
            /**
             * Number of failed login attempts to trigger alert.
             */
            @Min(1)
            private int failedLogins = 5;

            /**
             * Number of unauthorized access attempts to trigger alert.
             */
            @Min(1)
            private int unauthorizedAccess = 3;

            /**
             * Number of token refresh failures to trigger alert.
             */
            @Min(1)
            private int tokenRefreshFailures = 2;
        }
    }
}
