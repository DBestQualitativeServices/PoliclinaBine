package com.example.policlicabine.entity.enums;

import lombok.Getter;

/**
 * Enumeration of form lifecycle states.
 *
 * <p>Represents the current state of a form in the signing and processing workflow.
 * Forms progress through these states from creation to archival.
 *
 * <p><strong>Typical Workflow:</strong>
 * <pre>
 * DRAFT → PENDING_SIGNATURE → SIGNED → COMPLETED
 *   ↓          ↓                 ↓         ↓
 * VOIDED ←← REJECTED ←──────── EXPIRED
 * </pre>
 *
 * @author PoliclicaBine System
 */
@Getter
public enum FormStatus {

    /**
     * Form record created but no file uploaded yet.
     *
     * <p><strong>When:</strong> Form record created proactively when appointment is scheduled.
     * System knows which forms are required but patient hasn't filled/signed yet.
     *
     * <p><strong>Next States:</strong> PENDING_SIGNATURE (when file uploaded), VOIDED (if cancelled)
     */
    DRAFT("Draft", "Form created, awaiting file upload"),

    /**
     * Form file uploaded, awaiting patient signature confirmation.
     *
     * <p><strong>When:</strong> Receptionist or patient uploads PDF file from frontend (tablet app).
     * File is uploaded but signature not yet confirmed in backend.
     *
     * <p><strong>Next States:</strong> SIGNED (signature confirmed), REJECTED (signature invalid)
     */
    PENDING_SIGNATURE("Pending Signature", "File uploaded, awaiting signature confirmation"),

    /**
     * Patient has signed the form (signature confirmed).
     *
     * <p><strong>When:</strong> Frontend confirms patient signed on tablet, backend updates status.
     * Form now has patientSignedAt timestamp and patientSignedBy user reference.
     *
     * <p><strong>Next States:</strong> COMPLETED (processing finished), EXPIRED (validity period ended)
     */
    SIGNED("Signed", "Patient signature confirmed"),

    /**
     * Form fully processed and archived.
     *
     * <p><strong>When:</strong> Appointment session completed, all forms validated, invoice generated.
     * Form is now part of permanent medical record.
     *
     * <p><strong>Next States:</strong> EXPIRED (if validity period specified and ends)
     */
    COMPLETED("Completed", "Form processed and archived"),

    /**
     * Signature was rejected or deemed invalid.
     *
     * <p><strong>When:</strong> Quality control finds signature doesn't match patient records,
     * or form was filled incorrectly. Requires re-signing.
     *
     * <p><strong>Next States:</strong> PENDING_SIGNATURE (re-upload), VOIDED (abandon form)
     */
    REJECTED("Rejected", "Signature rejected, requires re-signing"),

    /**
     * Form validity period has ended (for consent forms with expiration dates).
     *
     * <p><strong>When:</strong> Current date exceeds form's validUntil date.
     * Common for consent forms with 1-year validity.
     *
     * <p><strong>Next States:</strong> None (terminal state, requires new form)
     */
    EXPIRED("Expired", "Form validity period ended"),

    /**
     * Form was cancelled or invalidated.
     *
     * <p><strong>When:</strong> Appointment cancelled, patient withdraws consent,
     * or form was created by mistake. Form is no longer valid.
     *
     * <p><strong>Next States:</strong> None (terminal state)
     */
    VOIDED("Voided", "Form cancelled or invalidated");

    /**
     * Human-readable display name for the status.
     */
    private final String displayName;

    /**
     * Detailed description of what this status means.
     */
    private final String description;

    FormStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Checks if this status represents an active form (not yet finalized).
     *
     * @return true if form is DRAFT, PENDING_SIGNATURE, or SIGNED (can still be processed)
     */
    public boolean isActive() {
        return this == DRAFT || this == PENDING_SIGNATURE || this == SIGNED;
    }

    /**
     * Checks if this status represents a completed or terminal state.
     *
     * @return true if form is COMPLETED, EXPIRED, or VOIDED (no further changes)
     */
    public boolean isTerminal() {
        return this == COMPLETED || this == EXPIRED || this == VOIDED;
    }

    /**
     * Checks if form is ready for use (signed and active).
     *
     * @return true if status is SIGNED or COMPLETED
     */
    public boolean isValid() {
        return this == SIGNED || this == COMPLETED;
    }

    /**
     * Checks if form requires action (not yet signed or rejected).
     *
     * @return true if status is DRAFT, PENDING_SIGNATURE, or REJECTED
     */
    public boolean requiresAction() {
        return this == DRAFT || this == PENDING_SIGNATURE || this == REJECTED;
    }
}
