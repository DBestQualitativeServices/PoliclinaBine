package com.example.policlicabine.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Mapped to HTTP 404 Not Found by GlobalExceptionHandler.
 * <p>
 * Examples:
 * - User not found by ID
 * - Doctor not found by ID
 * - Patient not found by ID
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(String.format("%s not found with identifier: %s", resourceName, identifier));
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
