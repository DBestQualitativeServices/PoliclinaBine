package com.example.policlicabine.entity.enums;

public enum PermissionEnum {

    ALL("Full system access - all permissions granted");

    private final String description;

    PermissionEnum(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
