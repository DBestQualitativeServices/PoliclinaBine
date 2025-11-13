package com.example.policlicabine.config;

import com.example.policlicabine.config.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private final CorsProperties corsProperties;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        // Allowed origins from environment-specific configuration
                        .allowedOrigins(corsProperties.getAllowedOriginsArray())
                        // Allowed HTTP methods
                        .allowedMethods(corsProperties.getAllowedMethodsArray())
                        // Allowed request headers (including Authorization for JWT)
                        .allowedHeaders(corsProperties.getAllowedHeadersArray())
                        // Exposed response headers (so frontend can read JWT tokens)
                        .exposedHeaders(corsProperties.getExposedHeadersArray())
                        // Enable credentials (required for cookies and Authorization headers)
                        .allowCredentials(corsProperties.getAllowCredentials())
                        // Cache preflight requests for specified duration
                        .maxAge(corsProperties.getMaxAge());
            }
        };
    }
}
