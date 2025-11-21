package com.example.policlicabine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormSection {
    private String sectionId;
    private String title;
    private String description;
    private List<FormField> fields;
    @Builder.Default
    private Boolean collapsible = false;
    private Integer order;
}
