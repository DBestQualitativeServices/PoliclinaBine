package com.example.policlicabine.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson ObjectMapper configuration for JSON serialization/deserialization.
 * Provides a centralized, reusable ObjectMapper instance with proper Java 8 date/time support.
 *
 * <p>Configuration includes:
 * <ul>
 *   <li>JavaTimeModule for OffsetDateTime, LocalDateTime, etc.</li>
 *   <li>ISO-8601 date format (not timestamps)</li>
 *   <li>Pretty printing for human-readable exports</li>
 *   <li>Defensive configuration to prevent serialization failures</li>
 * </ul>
 */
@Configuration
public class JacksonConfig {

    /**
     * Configure ObjectMapper bean for application-wide JSON processing.
     * This bean is used by both Spring MVC REST endpoints and custom export services.
     *
     * <p>The ObjectMapper is thread-safe and should be reused across the application
     * for optimal performance, following Jackson's "create once, reuse" best practice.
     *
     * @return Configured ObjectMapper instance
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Register Java 8 date/time module for OffsetDateTime support
        mapper.registerModule(new JavaTimeModule());

        // Disable writing dates as timestamps (use ISO-8601 strings instead)
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Enable pretty printing for better readability in exports
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Prevent exceptions on empty beans (defensive)
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}
