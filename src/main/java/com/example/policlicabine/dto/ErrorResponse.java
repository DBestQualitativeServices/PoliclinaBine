package com.example.policlicabine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Standard error response structure for API error handling.
 *
 * Provides consistent error response format across all API endpoints
 * with HTTP status code, error message, timestamp, and request path.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        name = "ErrorResponse",
        description = "Standard error response structure returned when API operations fail"
)
public class ErrorResponse {

    @Schema(
            description = "HTTP status code",
            example = "400",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int status;

    @Schema(
            description = "Human-readable error message describing what went wrong",
            example = "Patient with CNP 1234567890123 already exists",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String message;

    @Schema(
            description = "Timestamp when the error occurred",
            example = "2025-01-15T10:30:00Z",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private OffsetDateTime timestamp;

    @Schema(
            description = "Request path that caused the error",
            example = "/api/patients",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String path;

    /**
     * Creates an ErrorResponse from exception details.
     *
     * @param status HTTP status code
     * @param message Error message
     * @param path Request path
     * @return Constructed ErrorResponse
     */
    public static ErrorResponse of(int status, String message, String path) {
        return ErrorResponse.builder()
                .status(status)
                .message(message)
                .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                .path(path)
                .build();
    }
}
