package com.example.policlicabine.controller;

import com.example.policlicabine.common.StandardApiResponses;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.exception.BusinessException;
import com.example.policlicabine.exception.ResourceNotFoundException;
import com.example.policlicabine.service.FormSubmissionService;
import com.example.policlicabine.service.FormTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public Forms API Controller.
 *
 * Provides public endpoints for form template access and submission.
 * Separated from admin endpoints (/api/admin/form-templates) for clean API design.
 */
@RestController
@RequestMapping("/api/forms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Forms (Public)", description = "Public API for form templates and submissions")
public class FormController {

    private final FormTemplateService formTemplateService;
    private final FormSubmissionService formSubmissionService;

    /**
     * Gets a form template by name.
     *
     * @param name the template name (unique identifier)
     * @return the FormTemplateDto
     * @throws ResourceNotFoundException if no active template exists with the name
     */
    @GetMapping("/templates/name/{name}")
    @StandardApiResponses
    @Operation(summary = "Get template by name",
               description = "Returns the form template with the specified unique name")
    public FormTemplateDto getTemplateByName(@PathVariable String name) {
        log.info("REST: Getting template by name: {}", name);
        return formTemplateService.getByName(name);
    }

    /**
     * Submits a form using template name instead of UUID.
     * Resolves the template by name and submits the form.
     *
     * @param request the form submission request with template name
     * @return the created FormSubmissionDto
     * @throws BusinessException if validation fails or template not found
     */
    @PostMapping("/submit-by-name")
    @StandardApiResponses
    @Operation(summary = "Submit form using template name",
               description = "Submits a form by template name. Resolves to template ID automatically.")
    public FormSubmissionDto submitFormByName(@Valid @RequestBody SubmitFormByNameRequest request) {
        log.info("REST: Submitting form by name: {} for patient: {}",
                 request.templateName, request.patientId);

        // Resolve template name to ID
        FormTemplateDto template = formTemplateService.getByName(request.templateName);

        // Submit form using template ID
        return formSubmissionService.submitForm(
            template.getId(),
            request.patientId,
            request.data,
            request.appointmentSessionId,
            request.consultationTypeId,
            request.submittedByUserId,
            request.fileIds
        );
    }

    /**
     * Request DTO for submitting a form by template name.
     */
    public record SubmitFormByNameRequest(
        @NotBlank(message = "Template name is required")
        String templateName,

        @NotNull(message = "Patient ID is required")
        UUID patientId,

        @NotNull(message = "Form data is required")
        Map<String, Object> data,

        UUID appointmentSessionId,
        UUID consultationTypeId,
        UUID submittedByUserId,
        List<UUID> fileIds
    ) {}
}
