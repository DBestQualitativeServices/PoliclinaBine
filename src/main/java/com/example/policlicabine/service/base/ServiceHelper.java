package com.example.policlicabine.service.base;

import com.example.policlicabine.exception.BusinessException;

/**
 * Utility class providing common validation and helper methods for services.
 *
 * This class contains reusable patterns found across multiple services:
 * - String validation and trimming
 * - Numeric validation
 * - Defensive null checks
 * - Common error message formatting
 *
 * Usage:
 * <pre>
 * {@code
 * // In any service - throws BusinessException if invalid
 * ServiceHelper.validateRequiredString(firstName, "First name");
 * }
 * </pre>
 */
public final class ServiceHelper {

    private ServiceHelper() {
        // Utility class - prevent instantiation
    }

    // ============= STRING VALIDATION =============

    /**
     * Validates that a string is not null or empty (after trimming).
     * Throws BusinessException if invalid.
     *
     * @param value String to validate
     * @param fieldName Field name for error message
     * @throws BusinessException if value is null or empty
     */
    public static void validateRequiredString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(fieldName + " is required");
        }
    }

    /**
     * Validates and trims a string.
     * Returns null if the string is null or empty after trimming.
     *
     * @param value String to validate and trim
     * @return Trimmed string or null
     */
    public static String trimOrNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Validates and trims a required string.
     * Throws BusinessException if null or empty.
     *
     * @param value String to validate and trim
     * @param fieldName Field name for error message
     * @return Trimmed string (never null)
     * @throws BusinessException if value is null or empty
     */
    public static String requireTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new BusinessException(fieldName + " is required");
        }
        return value.trim();
    }

    // ============= OBJECT VALIDATION =============

    /**
     * Validates that an object is not null.
     * Throws BusinessException if null.
     *
     * @param value Object to validate
     * @param fieldName Field name for error message
     * @param <T> Type of the object
     * @throws BusinessException if value is null
     */
    public static <T> void validateRequired(T value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " is required");
        }
    }

    /**
     * Validates that an object is not null.
     * Throws BusinessException if null.
     *
     * @param value Object to validate
     * @param fieldName Field name for error message
     * @param <T> Type of the object
     * @return The validated object (never null)
     * @throws BusinessException if value is null
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " is required");
        }
        return value;
    }

    // ============= NUMERIC VALIDATION =============

    /**
     * Validates that a numeric value is positive (greater than zero).
     * Throws BusinessException if invalid.
     *
     * @param value Value to validate
     * @param fieldName Field name for error message
     * @throws BusinessException if value is null or not positive
     */
    public static void validatePositive(Number value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " is required");
        }
        if (value.doubleValue() <= 0) {
            throw new BusinessException(fieldName + " must be positive");
        }
    }

    /**
     * Validates that a numeric value is non-negative (zero or greater).
     * Throws BusinessException if invalid.
     *
     * @param value Value to validate
     * @param fieldName Field name for error message
     * @throws BusinessException if value is null or negative
     */
    public static void validateNonNegative(Number value, String fieldName) {
        if (value == null) {
            throw new BusinessException(fieldName + " is required");
        }
        if (value.doubleValue() < 0) {
            throw new BusinessException(fieldName + " cannot be negative");
        }
    }

    // ============= ERROR MESSAGE FORMATTING =============

    /**
     * Formats a "not found" error message.
     *
     * @param entityName Entity name (e.g., "Patient", "ConsultationType")
     * @return Formatted error message
     */
    public static String notFoundMessage(String entityName) {
        return entityName + " not found";
    }

    /**
     * Formats a "not found with identifier" error message.
     *
     * @param entityName Entity name (e.g., "Patient", "ConsultationType")
     * @param identifier Identifier value
     * @return Formatted error message
     */
    public static String notFoundWithIdMessage(String entityName, Object identifier) {
        return entityName + " not found with identifier: " + identifier;
    }

    /**
     * Formats an "already exists" error message.
     *
     * @param entityName Entity name (e.g., "Patient", "ConsultationType")
     * @param fieldName Field name that's duplicated
     * @return Formatted error message
     */
    public static String alreadyExistsMessage(String entityName, String fieldName) {
        return entityName + " with this " + fieldName + " already exists";
    }

    /**
     * Formats a generic operation failure message.
     *
     * @param operation Operation name (e.g., "create", "update", "delete")
     * @param entityName Entity name (e.g., "Patient", "ConsultationType")
     * @return Formatted error message
     */
    public static String operationFailedMessage(String operation, String entityName) {
        return "Failed to " + operation + " " + entityName;
    }
}
