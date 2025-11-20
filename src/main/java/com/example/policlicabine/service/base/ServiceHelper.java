package com.example.policlicabine.service.base;

import com.example.policlicabine.common.Result;

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
 * // In any service
 * Result<Void> validation = ServiceHelper.validateRequiredString(
 *     firstName, "First name"
 * );
 * if (validation.isFailure()) {
 *     return Result.failure(validation.getErrorMessage());
 * }
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
     *
     * @param value String to validate
     * @param fieldName Field name for error message
     * @return Result.success if valid, Result.failure with message otherwise
     */
    public static Result<Void> validateRequiredString(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return Result.failure(fieldName + " is required");
        }
        return Result.success(null);
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
     * Throws IllegalArgumentException if null or empty.
     *
     * @param value String to validate and trim
     * @param fieldName Field name for error message
     * @return Trimmed string (never null)
     * @throws IllegalArgumentException if value is null or empty
     */
    public static String requireTrimmed(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    // ============= OBJECT VALIDATION =============

    /**
     * Validates that an object is not null.
     *
     * @param value Object to validate
     * @param fieldName Field name for error message
     * @param <T> Type of the object
     * @return Result.success if not null, Result.failure with message otherwise
     */
    public static <T> Result<Void> validateRequired(T value, String fieldName) {
        if (value == null) {
            return Result.failure(fieldName + " is required");
        }
        return Result.success(null);
    }

    /**
     * Validates that an object is not null.
     * Throws IllegalArgumentException if null.
     *
     * @param value Object to validate
     * @param fieldName Field name for error message
     * @param <T> Type of the object
     * @return The validated object (never null)
     * @throws IllegalArgumentException if value is null
     */
    public static <T> T requireNonNull(T value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value;
    }

    // ============= NUMERIC VALIDATION =============

    /**
     * Validates that a numeric value is positive (greater than zero).
     *
     * @param value Value to validate
     * @param fieldName Field name for error message
     * @return Result.success if positive, Result.failure with message otherwise
     */
    public static Result<Void> validatePositive(Number value, String fieldName) {
        if (value == null) {
            return Result.failure(fieldName + " is required");
        }
        if (value.doubleValue() <= 0) {
            return Result.failure(fieldName + " must be positive");
        }
        return Result.success(null);
    }

    /**
     * Validates that a numeric value is non-negative (zero or greater).
     *
     * @param value Value to validate
     * @param fieldName Field name for error message
     * @return Result.success if non-negative, Result.failure with message otherwise
     */
    public static Result<Void> validateNonNegative(Number value, String fieldName) {
        if (value == null) {
            return Result.failure(fieldName + " is required");
        }
        if (value.doubleValue() < 0) {
            return Result.failure(fieldName + " cannot be negative");
        }
        return Result.success(null);
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
