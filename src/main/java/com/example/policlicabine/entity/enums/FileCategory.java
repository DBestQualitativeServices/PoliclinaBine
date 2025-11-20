package com.example.policlicabine.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Categories for file classification in the medical system.
 *
 * <p>Each category represents a specific type of document or image
 * that can be uploaded and managed within the application.
 *
 * @author PoliclicaBine System
 */
@Getter
@RequiredArgsConstructor
public enum FileCategory {

    /**
     * Patient consent documents for medical procedures and data processing
     */
    CONSENT_FILE("Patient consent documents"),

    /**
     * Medical examination reports and clinical notes
     */
    MEDICAL_REPORT("Medical examination reports"),

    /**
     * Prescription documents and medication orders
     */
    PRESCRIPTION("Prescriptions and medication orders"),

    /**
     * Patient identification documents (ID cards, passports, etc.)
     */
    ID_DOCUMENT("Patient identification documents"),

    /**
     * Waste collection documentation photos
     */
    WASTE_COLLECTION_PHOTO("Waste collection documentation photos"),

    /**
     * Laboratory test results and analysis reports
     */
    LAB_RESULT("Laboratory test results"),

    /**
     * Radiology images (X-rays, CT scans, MRI, ultrasound)
     */
    RADIOLOGY_IMAGE("X-rays, CT scans, MRI images"),

    /**
     * Other miscellaneous file types
     */
    OTHER("Other file types");

    /**
     * Human-readable description of the file category
     */
    private final String description;
}
