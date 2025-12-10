package com.example.policlicabine.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSignatureDto {
    private UUID id;
    private UUID formSubmissionId;
    private String signatureFieldId;
    private UUID signedByUserId;
    private String signedByUserName;
    private String signatureData;
    private LocalDateTime signedAt;
}
