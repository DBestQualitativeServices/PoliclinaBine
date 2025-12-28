package com.example.policlicabine.config.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Jackson-deserializable record representing a consultation entry from consultations.json.
 * 
 * This record maps the JSON structure with proper @JsonProperty annotations to handle
 * the specific field names and spacing in the JSON file.
 * 
 * Fields with null values are handled gracefully - the isValid() method filters out
 * entries that lack essential data (workflowStep, categoryLevel1, categoryLevel2).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ConsultationJsonEntry(
    @JsonProperty("Pas flux")
    Integer workflowStep,
    
    @JsonProperty("Categorie Nivel 1")
    String categoryLevel1,
    
    @JsonProperty("Categorie Nivel 2")
    String categoryLevel2,
    
    @JsonProperty("Categorie Final String")
    String categoryFinalString,
    
    @JsonProperty("Subcategorie Nivel 1")
    String subcategoryLevel1,
    
    @JsonProperty("Subcatagorie Nivel 2")
    String subcategoryLevel2,
    
    @JsonProperty("Document")
    String documentName,
    
    @JsonProperty("Tarif Pret lista ")
    Object priceRaw
) {
    /**
     * Validates that this entry has the required fields for consultation creation.
     * 
     * @return true if workflowStep, categoryLevel1, and categoryLevel2 are non-null
     */
    public boolean isValid() {
        return workflowStep != null 
            && categoryLevel1 != null 
            && categoryLevel2 != null;
    }
}
