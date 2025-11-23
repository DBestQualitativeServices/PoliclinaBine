package com.example.policlicabine.exception;

import lombok.Getter;

import java.util.List;

/**
 * Exception thrown when business validation or business logic fails.
 * <p>
 * Mapped to HTTP 400 Bad Request by GlobalExceptionHandler.
 * <p>
 * Examples:
 * - Username already exists
 * - Invalid input data
 * - Business rule violations
 */
@Getter
public class BusinessException extends RuntimeException {

    private final List<String> errors;

    public BusinessException(String message) {
        super(message);
        this.errors = null;
    }

    public BusinessException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errors = null;
    }
}
