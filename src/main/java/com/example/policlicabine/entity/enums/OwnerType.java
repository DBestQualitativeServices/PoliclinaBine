package com.example.policlicabine.entity.enums;

/**
 * Defines who owns a form template/submission.
 * The owner's signature determines if the form is considered "signed/complete".
 */
public enum OwnerType {
    /**
     * Patient-owned forms (GDPR consent, medical history, etc.)
     * Complete when patient signs.
     */
    PATIENT,

    /**
     * Doctor-owned forms (consultation notes, prescriptions, etc.)
     * Complete when doctor signs.
     */
    DOCTOR,

    /**
     * Admin/clinic-owned forms (internal documents, etc.)
     * Complete when admin signs.
     */
    ADMIN
}
