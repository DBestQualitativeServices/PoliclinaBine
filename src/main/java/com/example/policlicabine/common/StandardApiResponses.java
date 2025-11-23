package com.example.policlicabine.common;

import com.example.policlicabine.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation for standard API responses across all controllers.
 * <p>
 * Reduces boilerplate by providing common response patterns for endpoints.
 * SpringDoc automatically infers the 200 response schema from the controller
 * method return type.
 * <p>
 * Usage:
 * <pre>
 * &#64;GetMapping("/patients/{id}")
 * &#64;StandardApiResponses
 * public PatientDto getPatient(@PathVariable UUID id) {
 *     return patientService.findById(id).getValue();
 * }
 * </pre>
 * <p>
 * This automatically documents:
 * - 200: Success response (schema inferred from return type)
 * - 400: Validation/business logic errors
 * - 500: Server errors
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
        @ApiResponse(
                responseCode = "200",
                description = "Operation successful"
        ),
        @ApiResponse(
                responseCode = "400",
                description = "Invalid request or business validation failed",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        ),
        @ApiResponse(
                responseCode = "500",
                description = "Internal server error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))
        )
})
public @interface StandardApiResponses {
}
