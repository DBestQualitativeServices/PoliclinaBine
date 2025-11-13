package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.AnswerDto;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.service.AnswerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Answer Management Operations.
 *
 * Provides endpoints for saving, retrieving, updating, and deleting
 * patient answers to medical questionnaires during appointments.
 */
@RestController
@RequestMapping("/api/answers")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Answer Management",
        description = "APIs for managing patient answers to medical questionnaires"
)
public class AnswerController {

    private final AnswerService answerService;

    @Operation(
            summary = "Save patient answer",
            description = """
                    Records a patient's answer to a questionnaire question during an appointment.

                    **Business Rules:**
                    - Session and question must exist
                    - Question must belong to one of the session's consultations
                    - Answer text is required
                    - Publishes AnswerSaved domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Answer saved successfully",
                    content = @Content(schema = @Schema(implementation = AnswerDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> saveAnswer(
            @Parameter(description = "Appointment Session UUID", required = true)
            @RequestParam UUID sessionId,
            @Parameter(description = "Question UUID", required = true)
            @RequestParam UUID questionId,
            @Parameter(description = "Answer text", required = true)
            @RequestParam String answerText,
            HttpServletRequest request
    ) {
        log.info("REST: Saving answer for session {} and question {}", sessionId, questionId);

        Result<AnswerDto> result = answerService.saveAnswer(sessionId, questionId, answerText);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Get answers for a session",
            description = "Retrieves all answers recorded during a specific appointment session"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Answers retrieved successfully"
    )
    @GetMapping("/session/{sessionId}")
    public ResponseEntity<?> getSessionAnswers(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting all answers for session: {}", sessionId);

        Result<List<AnswerDto>> result = answerService.getAnswersForSession(sessionId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Get answers for session and consultation",
            description = "Retrieves answers for a specific consultation type within an appointment session"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Filtered answers retrieved successfully"
    )
    @GetMapping("/session/{sessionId}/consultation/{consultationId}")
    public ResponseEntity<?> getSessionConsultationAnswers(
            @Parameter(description = "Appointment Session UUID", required = true)
            @PathVariable UUID sessionId,
            @Parameter(description = "Consultation UUID", required = true)
            @PathVariable UUID consultationId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting answers for session {} and consultation {}",
                sessionId, consultationId);

        Result<List<AnswerDto>> result = answerService.getAnswersForSessionAndConsultation(
                sessionId,
                consultationId
        );

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            return ResponseEntity
                    .badRequest()
                    .body(ErrorResponse.of(
                            HttpStatus.BAD_REQUEST.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Update answer",
            description = """
                    Updates the text of an existing answer.

                    **Business Rule:** Only the answer text can be updated.
                    Session and question relationships are immutable.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Answer updated successfully",
                    content = @Content(schema = @Schema(implementation = AnswerDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Answer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{answerId}")
    public ResponseEntity<?> updateAnswer(
            @Parameter(description = "Answer UUID", required = true)
            @PathVariable UUID answerId,
            @Parameter(description = "New answer text", required = true)
            @RequestParam String answerText,
            HttpServletRequest request
    ) {
        log.info("REST: Updating answer: {}", answerId);

        Result<AnswerDto> result = answerService.updateAnswer(answerId, answerText);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
        } else {
            HttpStatus status = result.getErrorMessage().contains("not found")
                    ? HttpStatus.NOT_FOUND
                    : HttpStatus.BAD_REQUEST;

            return ResponseEntity
                    .status(status)
                    .body(ErrorResponse.of(
                            status.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }

    @Operation(
            summary = "Delete answer",
            description = """
                    Permanently deletes an answer from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Answer deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Answer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{answerId}")
    public ResponseEntity<?> deleteAnswer(
            @Parameter(description = "Answer UUID", required = true)
            @PathVariable UUID answerId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting answer: {}", answerId);

        Result<Void> result = answerService.deleteAnswer(answerId);

        if (result.isSuccess()) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(
                            HttpStatus.NOT_FOUND.value(),
                            result.getErrorMessage(),
                            request.getRequestURI()
                    ));
        }
    }
}
