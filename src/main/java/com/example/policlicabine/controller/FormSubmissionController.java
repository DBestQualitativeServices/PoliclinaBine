package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.FormSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Form Submission Controller.
 * Handles form submission lifecycle: submit, sign, attach files, and query operations.
 */
@RestController
@RequestMapping("/api/form-submissions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Form Submissions", description = "API for managing form submissions")
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    /**
     * Submits a new form by template ID.
     *
     * @param request the form submission request
     * @return the created FormSubmissionDto
     */
    @PostMapping
    @StandardApiResponses
    @Operation(summary = "Submit a form by template ID",
               description = "Creates a new form submission using the specified template ID")
    public FormSubmissionDto submitForm(@Valid @RequestBody SubmitFormRequest request) {
        log.info("REST: Submitting form with template ID: {} for patient: {}",
                 request.templateId, request.patientId);

        return formSubmissionService.submitForm(
                request.templateId,
                request.patientId,
                request.data,
                request.appointmentSessionId,
                request.consultationTypeId,
                request.submittedByUserId,
                request.fileIds
        );
    }

    /**
     * Signs a form submission.
     *
     * @param id the form submission ID
     * @param request the sign request with witness user ID
     * @return the updated FormSubmissionDto
     */
    @PutMapping("/{id}/sign")
    @StandardApiResponses
    @Operation(summary = "Sign a form submission",
               description = "Marks a form as signed and records the witness")
    public FormSubmissionDto signForm(@PathVariable UUID id, @Valid @RequestBody SignFormRequest request) {
        log.info("REST: Signing form submission: {} by witness: {}", id, request.witnessedByUserId);
        return formSubmissionService.signForm(id, request.witnessedByUserId);
    }

    /**
     * Attaches a file to a form submission.
     *
     * @param id the form submission ID
     * @param request the attach file request
     * @return the updated FormSubmissionDto
     */
    @PostMapping("/{id}/attach-file")
    @StandardApiResponses
    @Operation(summary = "Attach file to form submission",
               description = "Associates a file with a form submission")
    public FormSubmissionDto attachFile(@PathVariable UUID id, @Valid @RequestBody AttachFileRequest request) {
        log.info("REST: Attaching file to submission: {}", id);
        return formSubmissionService.attachFile(id, request.fileId);
    }

    /**
     * Gets a form submission by ID.
     *
     * @param id the form submission ID
     * @return the FormSubmissionDto
     */
    @GetMapping("/{id}")
    @StandardApiResponses
    @Operation(summary = "Get form submission by ID")
    public FormSubmissionDto getSubmission(@PathVariable UUID id) {
        log.info("REST: Getting form submission: {}", id);
        return formSubmissionService.findById(id);
    }

    /**
     * Gets all form submissions for a patient.
     *
     * @param patientId the patient ID
     * @return list of FormSubmissionDto
     */
    @GetMapping("/patients/{patientId}")
    @StandardApiResponses
    @Operation(summary = "Get all form submissions for patient",
               description = "Returns all forms submitted by the specified patient")
    public List<FormSubmissionDto> getFormsByPatient(@PathVariable UUID patientId) {
        log.info("REST: Getting forms for patient: {}", patientId);
        return formSubmissionService.getFormsByPatient(patientId);
    }

    /**
     * Gets all form submissions for an appointment session.
     *
     * @param sessionId the appointment session ID
     * @return list of FormSubmissionDto
     */
    @GetMapping("/appointments/{sessionId}")
    @StandardApiResponses
    @Operation(summary = "Get form submissions for appointment session",
               description = "Returns all forms associated with the specified appointment")
    public List<FormSubmissionDto> getFormsBySession(@PathVariable UUID sessionId) {
        log.info("REST: Getting forms for session: {}", sessionId);
        return formSubmissionService.getFormsBySession(sessionId);
    }

    /**
     * Checks if patient has a valid submission for a specific template.
     *
     * @param patientId the patient ID
     * @param templateId the template ID
     * @return true if patient has valid submission, false otherwise
     */
    @GetMapping("/patients/{patientId}/has-valid-submission")
    @StandardApiResponses
    @Operation(summary = "Check if patient has valid submission for template",
               description = "Returns true if patient has a valid, signed, non-expired form submission for the specified template")
    public Boolean hasValidSubmission(@PathVariable UUID patientId, @RequestParam UUID templateId) {
        log.info("REST: Checking if patient {} has valid submission for template: {}", patientId, templateId);
        return formSubmissionService.hasValidSubmission(patientId, templateId, null);
    }

    /**
     * Gets form submissions that are expiring soon.
     *
     * @param daysAhead number of days to look ahead (default 30)
     * @return list of FormSubmissionDto expiring within the specified timeframe
     */
    @GetMapping("/expiring-soon")
    @StandardApiResponses
    @Operation(summary = "Get forms expiring soon",
               description = "Returns form submissions that will expire within the specified number of days")
    public List<FormSubmissionDto> getExpiringSoon(@RequestParam(defaultValue = "30") int daysAhead) {
        log.info("REST: Getting forms expiring in next {} days", daysAhead);
        return formSubmissionService.getExpiringSoon(daysAhead);
    }

    /**
     * Request DTO for submitting a form.
     */
    public record SubmitFormRequest(
            @NotNull(message = "Template ID is required")
            UUID templateId,

            @NotNull(message = "Patient ID is required")
            UUID patientId,

            @NotNull(message = "Form data is required")
            Map<String, Object> data,

            UUID appointmentSessionId,
            UUID consultationTypeId,
            UUID submittedByUserId,
            List<UUID> fileIds
    ) {}

    /**
     * Request DTO for signing a form.
     */
    public record SignFormRequest(
            @NotNull(message = "Witness user ID is required")
            UUID witnessedByUserId
    ) {}

    /**
     * Request DTO for attaching a file to a form.
     */
    public record AttachFileRequest(
            @NotNull(message = "File ID is required")
            UUID fileId
    ) {}
}
