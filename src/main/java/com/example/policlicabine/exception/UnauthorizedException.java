package com.example.policlicabine.exception;

/**
 * Exception thrown when authentication fails or user is not authorized.
 * <p>
 * Mapped to HTTP 401 Unauthorized by GlobalExceptionHandler.
 * <p>
 * Examples:
 * - Invalid credentials
 * - Expired or invalid token
 * - Missing authentication
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }

    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
