package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.ErrorResponse;
import com.example.policlicabine.dto.QuestionDto;
import com.example.policlicabine.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for Question Management Operations.
 *
 * Provides CRUD endpoints for medical questionnaire questions
 * associated with consultation types.
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Slf4j
@Tag(
        name = "Question Management",
        description = "APIs for managing medical questionnaire questions for consultations"
)
public class QuestionController {

    private final QuestionService questionService;

    @Operation(
            summary = "Create a new question",
            description = """
                    Creates a new question for a consultation type.

                    **Business Rules:**
                    - Consultation type must exist
                    - Question text is required
                    - Question type (TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE) must be valid
                    - Publishes QuestionCreated domain event on success
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Question created successfully",
                    content = @Content(schema = @Schema(implementation = QuestionDto.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data or consultation not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping
    public ResponseEntity<?> createQuestion(
            @Valid @RequestBody QuestionDto questionDto,
            HttpServletRequest request
    ) {
        log.info("REST: Creating new question for consultation: {}",
                questionDto.getConsultationName());

        Result<QuestionDto> result = questionService.createQuestion(
                questionDto.getConsultationName(),
                questionDto.getQuestionText()
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
            summary = "Get question by ID",
            description = "Retrieves a question with its associated consultation information"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Question found",
                    content = @Content(schema = @Schema(implementation = QuestionDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/{questionId}")
    public ResponseEntity<?> getQuestion(
            @Parameter(description = "Question UUID", required = true)
            @PathVariable UUID questionId,
            HttpServletRequest request
    ) {
        log.info("REST: Getting question by ID: {}", questionId);

        Result<QuestionDto> result = questionService.findById(questionId);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result.getValue());
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

    @Operation(
            summary = "Get all questions",
            description = "Retrieves a list of all questions in the system"
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of questions retrieved successfully"
    )
    @GetMapping
    public ResponseEntity<List<QuestionDto>> getAllQuestions() {
        log.info("REST: Getting all questions");

        Result<List<QuestionDto>> result = questionService.findAll();

        return ResponseEntity.ok(result.getValue());
    }

    @Operation(
            summary = "Update question",
            description = """
                    Updates mutable fields of an existing question.

                    **Mutable Fields:**
                    - Question text
                    - Question type
                    - Answer options

                    **Immutable Fields:**
                    - Question ID
                    - Consultation relationship
                    - Created timestamp
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Question updated successfully",
                    content = @Content(schema = @Schema(implementation = QuestionDto.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PutMapping("/{questionId}")
    public ResponseEntity<?> updateQuestion(
            @Parameter(description = "Question UUID", required = true)
            @PathVariable UUID questionId,
            @Valid @RequestBody QuestionDto questionDto,
            HttpServletRequest request
    ) {
        log.info("REST: Updating question: {}", questionId);

        Result<QuestionDto> result = questionService.update(questionId, questionDto);

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
            summary = "Delete question",
            description = """
                    Permanently deletes a question from the system.

                    **Warning:** This operation cannot be undone.
                    Use with caution and ensure proper authorization.
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Question deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Question not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @DeleteMapping("/{questionId}")
    public ResponseEntity<?> deleteQuestion(
            @Parameter(description = "Question UUID", required = true)
            @PathVariable UUID questionId,
            HttpServletRequest request
    ) {
        log.info("REST: Deleting question: {}", questionId);

        Result<Void> result = questionService.deleteById(questionId);

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
