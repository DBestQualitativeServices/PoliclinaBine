package com.example.policlicabine.event;

import java.util.UUID;

/**
 * Domain event published when a new patient is registered in the system.
 *
 * This event signals that a patient has been created and may need supporting
 * services (e.g., user account creation, notification sending, etc.).
 */
public record NewPatientRegisteredEvent(
    UUID patientId,
    String firstName,
    String lastName,
    String email
) {}
