package com.example.policlicabine.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI API Groups Configuration.
 *
 * Organizes API endpoints into logical groups for better navigation
 * and documentation structure in Swagger UI.
 */
@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi patientManagementApi() {
        return GroupedOpenApi.builder()
                .group("1-patient-management")
                .displayName("Patient Management")
                .pathsToMatch("/api/patients/**")
                .build();
    }

    @Bean
    public GroupedOpenApi doctorManagementApi() {
        return GroupedOpenApi.builder()
                .group("2-doctor-management")
                .displayName("Doctor Management")
                .pathsToMatch("/api/doctors/**", "/api/users/**")
                .build();
    }

    @Bean
    public GroupedOpenApi appointmentManagementApi() {
        return GroupedOpenApi.builder()
                .group("3-appointment-management")
                .displayName("Appointment & Session Management")
                .pathsToMatch("/api/appointments/**", "/api/sessions/**")
                .build();
    }

    @Bean
    public GroupedOpenApi medicalRecordsApi() {
        return GroupedOpenApi.builder()
                .group("4-medical-records")
                .displayName("Medical Records & Consultations")
                .pathsToMatch(
                        "/api/consultations/**",
                        "/api/diagnoses/**",
                        "/api/questions/**",
                        "/api/answers/**",
                        "/api/medical-files/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi billingPaymentApi() {
        return GroupedOpenApi.builder()
                .group("5-billing-payment")
                .displayName("Billing & Payment")
                .pathsToMatch(
                        "/api/billing/**",
                        "/api/invoices/**",
                        "/api/payments/**",
                        "/api/discounts/**"
                )
                .build();
    }

    @Bean
    public GroupedOpenApi allApisGroup() {
        return GroupedOpenApi.builder()
                .group("0-all-apis")
                .displayName("All APIs")
                .pathsToMatch("/api/**")
                .build();
    }
}
