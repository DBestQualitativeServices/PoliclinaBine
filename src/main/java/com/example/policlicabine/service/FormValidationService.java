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
                Object value = data.get(fieldName);

                if (field.getRequired() != null && field.getRequired()) {
                    if (value == null || value.toString().trim().isEmpty()) {
                        errors.add("Field '" + field.getLabel() + "' is required");
                        continue;
                    }
                }

                if (value == null || value.toString().trim().isEmpty()) {
                    continue;
                }

                String valueStr = value.toString();

                if (field.getMinLength() != null && valueStr.length() < field.getMinLength()) {
                    errors.add("Field '" + field.getLabel() + "' must be at least " + field.getMinLength() + " characters");
                }

                if (field.getMaxLength() != null && valueStr.length() > field.getMaxLength()) {
                    errors.add("Field '" + field.getLabel() + "' must not exceed " + field.getMaxLength() + " characters");
                }

                if (field.getPattern() != null && !field.getPattern().trim().isEmpty()) {
                    try {
                        if (!Pattern.matches(field.getPattern(), valueStr)) {
                            errors.add("Field '" + field.getLabel() + "' does not match required pattern");
                        }
                    } catch (Exception e) {
                        log.warn("Invalid regex pattern for field {}: {}", fieldName, field.getPattern());
                    }
                }

                if ("email".equals(field.getType())) {
                    if (!isValidEmail(valueStr)) {
                        errors.add("Field '" + field.getLabel() + "' must be a valid email address");
                    }
                }

                if ("number".equals(field.getType())) {
                    try {
                        double numValue = Double.parseDouble(valueStr);
                        if (field.getMin() != null) {
                            double min = Double.parseDouble(field.getMin());
                            if (numValue < min) {
                                errors.add("Field '" + field.getLabel() + "' must be at least " + field.getMin());
                            }
                        }
                        if (field.getMax() != null) {
                            double max = Double.parseDouble(field.getMax());
                            if (numValue > max) {
                                errors.add("Field '" + field.getLabel() + "' must not exceed " + field.getMax());
                            }
                        }
                    } catch (NumberFormatException e) {
                        errors.add("Field '" + field.getLabel() + "' must be a valid number");
                    }
                }
            }
        }

        return errors;
    }

    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(emailRegex, email);
    }
}
