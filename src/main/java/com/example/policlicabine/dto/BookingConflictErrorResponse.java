package com.example.policlicabine.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Error response for booking conflicts (HTTP 409).
 * Extends standard error response with detailed conflict information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Error response when appointment booking conflicts with existing appointments")
public class BookingConflictErrorResponse {

    @Schema(description = "HTTP status code", example = "409")
    private int status;

    @Schema(description = "Error message describing the conflict", example = "Booking conflict: Doctor has 2 overlapping appointments")
    private String message;

    @Schema(description = "Timestamp when the error occurred")
    @Builder.Default
    private OffsetDateTime timestamp = OffsetDateTime.now();

    @Schema(description = "Request path that caused the error", example = "/api/appointments")
    private String path;

    @Schema(description = "List of conflicting appointments")
    private List<BookingConflictDto> conflicts;
}
