package com.example.policlicabine.dto;

import com.example.policlicabine.model.FormStructure;
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
public class FormTemplateDto {
    private UUID id;
    private String name;
    private Boolean active;
    private FormStructure structure;
    private Integer validityMonths;
    private String pdfTemplateUrl;
    private LocalDateTime createdAt;
    private UUID createdByUserId;
}
