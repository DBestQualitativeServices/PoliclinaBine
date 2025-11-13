package com.example.policlicabine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Global CORS (Cross-Origin Resource Sharing) configuration.
 *
 * This configuration enables the frontend application to make requests to the backend API
 * from different origins (domains). Essential for React/Vue/Angular frontends running on
 * different ports during development or different domains in production.
 *
 * @see <a href="https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.cors">Spring Boot CORS Documentation</a>
 */
@Configuration
public class CorsConfig {

    /**
     * Configures global CORS mappings for all API endpoints.
     *
     * Configuration details:
     * - Allowed Origins: localhost:5173 (development) + Azure Static Web Apps (production)
     * - Allowed Methods: All HTTP methods (GET, POST, PUT, DELETE, PATCH, OPTIONS)
     * - Allowed Headers: Authorization (JWT), Content-Type, Accept, Origin
     * - Exposed Headers: Authorization (for JWT token responses)
     * - Credentials: Enabled (required for cookies and Authorization headers)
     * - Max Age: 3600 seconds (preflight requests cached for 1 hour)
     *
     * @return WebMvcConfigurer with CORS configuration
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // Development: React frontend on localhost:5173
                        .allowedOrigins(
                                "http://localhost:5173"
                                // TODO: Add Azure Static Web App URL when deployed
                                // Example: "https://your-app-name.azurestaticapps.net"
                                // Uncomment and replace with your actual Azure URL:
                                // , "https://your-app-name.azurestaticapps.net"
                        )
                        // Allow all HTTP methods
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        // Allow essential headers (including Authorization for JWT)
                        .allowedHeaders("Authorization", "Content-Type", "Accept", "Origin", "X-Requested-With")
                        // Expose Authorization header so frontend can read JWT tokens
                        .exposedHeaders("Authorization")
                        // Enable credentials (required for cookies and Authorization headers)
                        .allowCredentials(true)
                        // Cache preflight requests for 1 hour
                        .maxAge(3600);
            }
        };
    }
}
