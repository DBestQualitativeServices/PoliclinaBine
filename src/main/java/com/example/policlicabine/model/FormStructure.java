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
public class FormStructure {
    private String formId;
    private String version;
    private String title;
    private String description;
    private List<FormSection> sections;
}
