package com.example.policlicabine.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConditionalOnProperty(prefix = "swagger", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearer-jwt";

        return new OpenAPI()
                .info(new Info()
                        .title("PoliclinaBine Clinic Management API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for comprehensive clinic management system.

                                **Features:**
                                - Patient registration and profile management
                                - Doctor profiles and specialty management
                                - Appointment scheduling and session management
                                - Medical consultations with questionnaires
                                - Diagnosis tracking (ICD-10 compatible)
                                - Billing, invoicing, and payment processing
                                - Medical file access control

                                **Authentication:**
                                The API uses JWT (JSON Web Token) Bearer authentication.
                                Obtain a token by calling the `/api/auth/login` or `/api/auth/register` endpoint,
                                then include it in the Authorization header as: `Bearer <token>`

                                **Domain Event Architecture:**
                                The system uses domain events for cross-service communication
                                and audit logging. All significant business actions trigger events.
                                """)
                        .contact(new Contact()
                                .name("PoliclinaBine Development Team")
                                .email("support@policlicabine.com")
                                .url("https://policlicabine.com"))
                        .license(new License()
                                .name("Apache License 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server (local)"),
                        new Server()
                                .url("https://api.policlicabine.com")
                                .description("Production server")
                ))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                                .description("JWT Bearer token authentication. Format: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName));
    }
}
