package com.example.policlicabine.dto;

import com.example.policlicabine.entity.enums.ConsultCategory;
import com.example.policlicabine.entity.enums.FormType;
import com.example.policlicabine.entity.enums.Specialty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Medical consultation type with pricing and requirements")
public class ConsultationTypeDto {

    private UUID consultationId;
    private String name;
    private Specialty specialty;
    private ConsultCategory consultCategory;
    private BigDecimal price;
    private String priceCurrency;
    private Integer durationMinutes;
    private Boolean requiresSurgeryRoom;
    private Boolean isActive;

    private Integer workflowStep;
    private String categoryLevel1;
    private String categoryLevel2;
    private String subcategoryLevel1;
    private String subcategoryLevel2;
    private String categoryPath;

    @Schema(description = "Form types required for this consultation type")
    private List<FormType> requiredForms;
}
