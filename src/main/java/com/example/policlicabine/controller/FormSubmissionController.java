package com.example.policlicabine.controller;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.dto.FormSubmissionDto;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.service.FormSubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/form-submissions")
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    @PostMapping
    public ResponseEntity<FormSubmissionDto> submitForm(@RequestBody SubmitFormRequest request) {
        Result<FormSubmissionDto> result = formSubmissionService.submitForm(
                request.templateId,
                request.patientId,
                request.data,
                request.appointmentSessionId,
                request.consultationTypeId,
                request.submittedByUserId
        );

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @PutMapping("/{id}/sign")
    public ResponseEntity<FormSubmissionDto> signForm(@PathVariable UUID id, @RequestBody SignFormRequest request) {
        Result<FormSubmissionDto> result = formSubmissionService.signForm(id, request.witnessedByUserId);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @PostMapping("/{id}/attach-file")
    public ResponseEntity<FormSubmissionDto> attachFile(@PathVariable UUID id, @RequestBody AttachFileRequest request) {
        Result<FormSubmissionDto> result = formSubmissionService.attachFile(id, request.fileId);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormSubmissionDto> getSubmission(@PathVariable UUID id) {
        Result<FormSubmissionDto> result = formSubmissionService.findById(id);

        if (result.isFailure()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/patients/{patientId}")
    public ResponseEntity<List<FormSubmissionDto>> getFormsByPatient(@PathVariable UUID patientId) {
        Result<List<FormSubmissionDto>> result = formSubmissionService.getFormsByPatient(patientId);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/appointments/{sessionId}")
    public ResponseEntity<List<FormSubmissionDto>> getFormsBySession(@PathVariable UUID sessionId) {
        Result<List<FormSubmissionDto>> result = formSubmissionService.getFormsBySession(sessionId);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/patients/{patientId}/has-valid-form")
    public ResponseEntity<Boolean> hasValidForm(@PathVariable UUID patientId, @RequestParam FormPurpose purpose) {
        Result<Boolean> result = formSubmissionService.hasValidForm(patientId, purpose);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    @GetMapping("/expiring-soon")
    public ResponseEntity<List<FormSubmissionDto>> getExpiringSoon(@RequestParam(defaultValue = "30") int daysAhead) {
        Result<List<FormSubmissionDto>> result = formSubmissionService.getExpiringSoon(daysAhead);

        if (result.isFailure()) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(result.getValue());
    }

    public record SubmitFormRequest(
            UUID templateId,
            UUID patientId,
            Map<String, Object> data,
            UUID appointmentSessionId,
            UUID consultationTypeId,
            UUID submittedByUserId
    ) {}

    public record SignFormRequest(UUID witnessedByUserId) {}

    public record AttachFileRequest(UUID fileId) {}
}
