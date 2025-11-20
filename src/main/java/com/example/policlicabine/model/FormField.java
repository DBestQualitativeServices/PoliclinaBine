package com.example.policlicabine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormField {
    private String name;
    @Builder.Default
    private String type = "text";
    private String label;
    @Builder.Default
    private String placeholder = "";
    @Builder.Default
    private String value = "";
    private String pattern;
    @Builder.Default
    private Boolean required = false;
    @Builder.Default
    private Boolean disabled = false;
    @Builder.Default
    private Boolean readonly = false;
    private String min;
    private String max;
    private Integer minLength;
    private Integer maxLength;
    @Builder.Default
    private List<FieldOption> options = new ArrayList<>();
    private Boolean requiresWitness;
    private String signatureType;
    private List<String> acceptedFileTypes;
    private Long maxFileSizeBytes;
}
