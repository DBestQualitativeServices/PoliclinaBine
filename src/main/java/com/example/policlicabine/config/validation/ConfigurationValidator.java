package com.example.policlicabine.config.validation;

import com.example.policlicabine.config.properties.CorsProperties;
import com.example.policlicabine.config.properties.JwtProperties;
import io.jsonwebtoken.io.Decoders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidator implements InitializingBean {

    private final JwtProperties jwtProperties;
    private final CorsProperties corsProperties;
    private final DataSource dataSource;

    @Override
    public void afterPropertiesSet() {
        log.info("Validating application configuration...");

        validateJwtConfiguration();
        validateCorsConfiguration();
        validateDatabaseConnection();

        log.info("✓ Configuration validation completed successfully");
    }

    private void validateJwtConfiguration() {
        log.debug("Validating JWT configuration...");

        String secret = jwtProperties.getSecret();

        // Validate secret is not null or empty
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret is not configured. " +
                "Set JWT_SECRET environment variable or jwt.secret property."
            );
        }

        // Validate secret is base64-encoded
        try {
            byte[] decodedSecret = Decoders.BASE64.decode(secret);

            // Validate minimum length (256 bits = 32 bytes for HS256)
            if (decodedSecret.length < 32) {
                throw new IllegalStateException(
                    String.format(
                        "JWT secret is too short: %d bytes (decoded). " +
                        "Minimum required: 32 bytes (256 bits) for HS256 algorithm. " +
                        "Generate a secure secret: openssl rand -base64 64",
                        decodedSecret.length
                    )
                );
            }

            log.debug("✓ JWT secret validation passed ({} bytes)", decodedSecret.length);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "JWT secret is not valid base64: " + e.getMessage() +
                ". Generate a valid secret: openssl rand -base64 64",
                e
            );
        }

        // Validate expiration times
        if (jwtProperties.getExpiration() == null || jwtProperties.getExpiration() <= 0) {
            throw new IllegalStateException("JWT expiration must be positive");
        }

        if (jwtProperties.getRefreshExpiration() == null || jwtProperties.getRefreshExpiration() <= 0) {
            throw new IllegalStateException("JWT refresh expiration must be positive");
        }

        // Warn if expiration is too short (less than 1 minute)
        if (jwtProperties.getExpiration() < 60000) {
            log.warn("⚠ JWT expiration is very short: {} ms. Recommended minimum: 300000 ms (5 minutes)",
                jwtProperties.getExpiration());
        }

        // Warn if refresh expiration is shorter than expiration
        if (jwtProperties.getRefreshExpiration() <= jwtProperties.getExpiration()) {
            log.warn("⚠ JWT refresh expiration ({} ms) should be longer than access token expiration ({} ms)",
                jwtProperties.getRefreshExpiration(), jwtProperties.getExpiration());
        }

        log.debug("✓ JWT configuration is valid");
    }

    private void validateCorsConfiguration() {
        log.debug("Validating CORS configuration...");

        String allowedOrigins = corsProperties.getAllowedOrigins();

        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            throw new IllegalStateException(
                "CORS allowed origins are not configured. " +
                "Set CORS_ALLOWED_ORIGINS environment variable or cors.allowed-origins property."
            );
        }

        String[] origins = corsProperties.getAllowedOriginsArray();
        log.debug("✓ CORS configuration is valid ({} origin(s) configured)", origins.length);

        // Log origins for debugging (helpful in production)
        for (String origin : origins) {
            log.debug("  - Allowed origin: {}", origin.trim());
        }
    }

    private void validateDatabaseConnection() {
        log.debug("Validating database connection...");

        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            String productVersion = connection.getMetaData().getDatabaseProductVersion();

            log.debug("✓ Database connection successful: {} (version {})",
                productName, productVersion);
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to connect to database. " +
                "Verify SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, and SPRING_DATASOURCE_PASSWORD " +
                "are correctly configured. Error: " + e.getMessage(),
                e
            );
        }
    }
}
