package com.example.policlicabine.controller;

import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.exception.FileStorageException;
import com.example.policlicabine.exception.InvalidFileTypeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Global exception handler for all REST controllers.
 *
 * <p>Provides centralized exception handling with proper HTTP status codes
 * and user-friendly error messages. This ensures consistent error responses
 * across all API endpoints.</p>
 *
 * <p><strong>Exception Handling Strategy:</strong></p>
 * <ul>
 *   <li>{@link DataIntegrityViolationException} → 409 Conflict (duplicate data)</li>
 *   <li>{@link ConstraintViolationException} → 400 Bad Request (validation errors)</li>
 *   <li>{@link MaxUploadSizeExceededException} → 413 Payload Too Large (file too big)</li>
 *   <li>{@link FileStorageException} → 500 Internal Server Error (storage failures)</li>
 *   <li>{@link InvalidFileTypeException} → 400 Bad Request (unsupported file type)</li>
 * </ul>
 *
 * <p><strong>Architecture Notes:</strong></p>
 * <ul>
 *   <li>Database constraints are safety nets; business validation should occur in service layer first</li>
 *   <li>This handler catches edge cases like race conditions or direct database operations</li>
 *   <li>Parses constraint names from exception messages for user-friendly error messages</li>
 * </ul>
 *
 * @see ErrorResponse
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handles database constraint violations (duplicate keys, foreign key violations, etc.).
     *
     * <p>This is a safety net for cases where business validation is bypassed or
     * race conditions occur. Ideally, service layer should validate uniqueness
     * before attempting database operations.</p>
     *
     * <p><strong>Common scenarios:</strong></p>
     * <ul>
     *   <li>Duplicate email/phone despite pre-validation (race condition)</li>
     *   <li>Foreign key violations</li>
     *   <li>NOT NULL constraint violations</li>
     * </ul>
     *
     * @param ex Database integrity exception
     * @param request HTTP request that caused the error
     * @return 409 Conflict response with user-friendly message
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {

        log.warn("Database constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "Database constraint violation";
        String exMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Parse constraint name from exception message to provide meaningful feedback
        // PostgreSQL format: "Detail: Key (column_name)=(value) already exists."
        if (exMessage.contains("email") || exMessage.contains("uka370hmxgv0l5c9panryr1ji7d")) {
            message = "Patient with this email already exists";
        } else if (exMessage.contains("phone") || exMessage.contains("uk_patient_phone")) {
            message = "Patient with this phone number already exists";
        } else if (exMessage.contains("duplicate") || exMessage.contains("already exists")) {
            message = "A record with these values already exists";
        } else if (exMessage.contains("foreign key")) {
            message = "Cannot perform operation: referenced record does not exist";
        } else if (exMessage.contains("not null")) {
            message = "Required field is missing";
        } else {
            // Log full exception for unknown constraint violations
            log.error("Unhandled constraint violation", ex);
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        HttpStatus.CONFLICT.value(),
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handles Jakarta Bean Validation constraint violations.
     *
     * <p>These are typically thrown when method parameters or request bodies
     * fail validation annotations like {@code @NotNull}, {@code @Size}, etc.</p>
     *
     * @param ex Constraint violation exception
     * @param request HTTP request that caused the error
     * @return 400 Bad Request response with validation error details
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request) {

        log.warn("Validation constraint violation at {}: {}", request.getRequestURI(), ex.getMessage());

        // Collect all validation error messages
        StringBuilder messageBuilder = new StringBuilder("Validation failed: ");
        ex.getConstraintViolations().forEach(violation -> {
            messageBuilder.append(violation.getPropertyPath())
                    .append(" ")
                    .append(violation.getMessage())
                    .append("; ");
        });

        String message = messageBuilder.toString();
        if (message.endsWith("; ")) {
            message = message.substring(0, message.length() - 2);
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handles file upload size exceeded exceptions.
     *
     * <p>Thrown when uploaded file exceeds the maximum size configured in
     * {@code spring.servlet.multipart.max-file-size} or {@code file.storage.max-file-size}.</p>
     *
     * @param ex Max upload size exceeded exception
     * @param request HTTP request that caused the error
     * @return 413 Payload Too Large response
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {

        log.warn("File upload size exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        String message = "File size exceeds maximum limit of 25MB. Please upload a smaller file.";

        return ResponseEntity
                .status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ErrorResponse.of(
                        HttpStatus.PAYLOAD_TOO_LARGE.value(),
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handles file storage exceptions (I/O errors, storage failures).
     *
     * <p>Thrown when file cannot be saved to storage due to:
     * <ul>
     *   <li>Disk full or insufficient permissions</li>
     *   <li>Network issues (for cloud storage)</li>
     *   <li>Storage service unavailable</li>
     * </ul></p>
     *
     * @param ex File storage exception
     * @param request HTTP request that caused the error
     * @return 500 Internal Server Error response
     */
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(
            FileStorageException ex,
            HttpServletRequest request) {

        log.error("File storage error at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        String message = "Failed to store file: " + ex.getMessage();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        message,
                        request.getRequestURI()
                ));
    }

    /**
     * Handles invalid file type exceptions.
     *
     * <p>Thrown when uploaded file type is not in the allowed MIME types list.
     * Currently allowed: image/png, image/jpeg, image/jpg.</p>
     *
     * @param ex Invalid file type exception
     * @param request HTTP request that caused the error
     * @return 400 Bad Request response
     */
    @ExceptionHandler(InvalidFileTypeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFileTypeException(
            InvalidFileTypeException ex,
            HttpServletRequest request) {

        log.warn("Invalid file type at {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        HttpStatus.BAD_REQUEST.value(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    /**
     * Handles generic runtime exceptions that aren't caught by more specific handlers.
     *
     * <p>This is a fallback handler that logs the full exception and returns
     * a generic 500 Internal Server Error response.</p>
     *
     * @param ex Runtime exception
     * @param request HTTP request that caused the error
     * @return 500 Internal Server Error response
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(
            RuntimeException ex,
            HttpServletRequest request) {

        log.error("Unhandled runtime exception at {}", request.getRequestURI(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "An unexpected error occurred. Please try again later.",
                        request.getRequestURI()
                ));
    }
}
