package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/**
 * Filter criteria DTO for searching and filtering patients.
 * <p>
 * This DTO is used as a request parameter in search endpoints to specify
 * filtering conditions. All fields are optional (nullable), and null values
 * are ignored during specification building.
 * </p>
 * <p>
 * Supports:
 * <ul>
 *   <li>Text filters: Partial match, case-insensitive (firstName, lastName, phone, email)</li>
 *   <li>Date range filters: registeredAfter, registeredBefore</li>
 *   <li>Boolean filter: hasConsent (checks if consentFileUrl is not null)</li>
 * </ul>
 * </p>
 *
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientFilterCriteria {

    /**
     * Filter by patient first name (partial match, case-insensitive).
     * <p>
     * Example: "joh" matches "John", "JOHANNA", "johann"
     * </p>
     */
    private String firstName;

    /**
     * Filter by patient last name (partial match, case-insensitive).
     * <p>
     * Example: "smi" matches "Smith", "SMITHSON", "smit"
     * </p>
     */
    private String lastName;

    /**
     * Filter by patient phone number (partial match).
     * <p>
     * Example: "0723" matches any phone containing "0723"
     * </p>
     */
    private String phone;

    /**
     * Filter by patient email address (partial match, case-insensitive).
     * <p>
     * Example: "gmail" matches "user@gmail.com", "admin@GMAIL.com"
     * </p>
     */
    private String email;

    /**
     * Filter patients registered on or after this date/time.
     * <p>
     * Inclusive: patients with registrationDate >= registeredAfter
     * </p>
     */
    private OffsetDateTime registeredAfter;

    /**
     * Filter patients registered on or before this date/time.
     * <p>
     * Inclusive: patients with registrationDate <= registeredBefore
     * </p>
     */
    private OffsetDateTime registeredBefore;

    /**
     * Filter by consent file presence.
     * <p>
     * - true: only patients WITH consent file (consentFileUrl IS NOT NULL)
     * - false: only patients WITHOUT consent file (consentFileUrl IS NULL)
     * - null: no filtering on consent status
     * </p>
     */
    private Boolean hasConsent;
}
