package com.example.policlicabine.service;

import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@Slf4j
@RequiredArgsConstructor
public class FormValidationService {

    public List<String> validate(FormStructure structure, Map<String, Object> data) {
        List<String> errors = new ArrayList<>();

        if (structure == null) {
            errors.add("Form structure is null");
            return errors;
        }

        if (structure.getSections() == null || structure.getSections().isEmpty()) {
            errors.add("Form structure has no sections");
            return errors;
        }

        for (FormSection section : structure.getSections()) {
            if (section.getFields() == null) continue;

            for (FormField field : section.getFields()) {
                String fieldName = field.getName();
                String displayName = getFieldDisplayName(field);
                Object value = data.get(fieldName);

                // Skip signature fields - they are validated separately via /signatures endpoint
                if ("signature".equals(field.getType())) {
                    continue;
                }

                if (field.getRequired() != null && field.getRequired()) {
                    // Special handling for checkbox type
                    if ("checkbox".equals(field.getType())) {
                        if (value == null || !Boolean.TRUE.equals(value)) {
                            errors.add("Field '" + displayName + "' is required");
                            continue;
                        }
                    } else {
                        if (value == null || value.toString().trim().isEmpty()) {
                            errors.add("Field '" + displayName + "' is required");
                            continue;
                        }
                    }
                }

                if (value == null || value.toString().trim().isEmpty()) {
                    continue;
                }

                String valueStr = value.toString().trim();

                if (field.getMinLength() != null && valueStr.length() < field.getMinLength()) {
                    errors.add("Field '" + displayName + "' must be at least " + field.getMinLength() + " characters");
                }

                if (field.getMaxLength() != null && valueStr.length() > field.getMaxLength()) {
                    errors.add("Field '" + displayName + "' must not exceed " + field.getMaxLength() + " characters");
                }

                if (field.getPattern() != null && !field.getPattern().trim().isEmpty()) {
                    try {
                        if (!Pattern.matches(field.getPattern(), valueStr)) {
                            errors.add("Field '" + displayName + "' does not match required pattern");
                        }
                    } catch (Exception e) {
                        log.warn("Invalid regex pattern for field {}: {}", fieldName, field.getPattern());
                    }
                }

                if ("email".equals(field.getType())) {
                    if (!isValidEmail(valueStr)) {
                        errors.add("Field '" + displayName + "' must be a valid email address");
                    }
                }

                if ("number".equals(field.getType())) {
                    try {
                        double numValue = Double.parseDouble(valueStr);
                        if (field.getMin() != null) {
                            double min = Double.parseDouble(field.getMin());
                            if (numValue < min) {
                                errors.add("Field '" + displayName + "' must be at least " + field.getMin());
                            }
                        }
                        if (field.getMax() != null) {
                            double max = Double.parseDouble(field.getMax());
                            if (numValue > max) {
                                errors.add("Field '" + displayName + "' must not exceed " + field.getMax());
                            }
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Field '" + displayName + "' must be a valid number");
                    }
                }
            }
        }

        return errors;
    }

    private String getFieldDisplayName(FormField field) {
        return field.getLabel() != null && !field.getLabel().trim().isEmpty()
                ? field.getLabel()
                : field.getName();
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }
}
