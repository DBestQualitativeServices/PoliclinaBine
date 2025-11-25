package com.example.policlicabine.entity.enums;

import lombok.Getter;

@Getter
public enum Specialty {
    GENERAL_DERMATOLOGY("General"),
    COSMETIC_DERMATOLOGY("Cosmetica"),
    MEDICAL_DERMATOLOGY("Medical");

    private final String displayName;

    Specialty(String displayName) {
        this.displayName = displayName;
    }

}
