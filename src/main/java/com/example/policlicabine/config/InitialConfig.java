package com.example.policlicabine.config;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.controller.FormTemplateController;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.model.FieldOption;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.ManagerRepository;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.service.FormTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.List;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitialConfig {

    private final FormTemplateService formTemplateService;
    private final FormTemplateController formTemplateController;
    private final UserRepository userRepository;
    private final ManagerRepository managerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
//            if (Boolean.TRUE) return;
//            createGdprConsentFormTemplate();
            List<FormTemplateDto> body = formTemplateController.getAllActiveTemplates().getBody();
            System.out.println("ASD");

            // Create default manager if no managers exist
            createDefaultManagerIfNeeded();
        };
    }

    private void createGdprConsentFormTemplate() {
        if (formTemplateService.getEntityByCode("GDPR_CONSENT_V1") != null) {
            log.info("GDPR Consent Form template already exists, skipping creation");
            return;
        }

        log.info("Creating GDPR Consent Form template...");

        FormStructure structure = FormStructure.builder()
            .formId("gdpr-consent-form")
            .version("1.0")
            .title("Formular de Consimțământ GDPR")
            .description("Acord pentru prelucrarea datelor personale conform GDPR")
            .sections(List.of(
                buildPersonalInfoSection(),
                buildInitialConsentSection(),
                buildMarketingPreferencesSection(),
                buildFinalSignatureSection()
            ))
            .build();

        Result<FormTemplateDto> result = formTemplateService.createTemplate(
            "GDPR_CONSENT_V1",
            "Formular de Consimțământ GDPR",
            structure,
            FormPurpose.GDPR_CONSENT,
            12,  // Valid for 12 months
            null // No specific creator
        );

        if (result.isSuccess()) {
            log.info("GDPR Consent Form template created successfully with ID: {}", result.getValue().getId());

            formTemplateService.publishTemplate(result.getValue().getId());
            log.info("GDPR Consent Form template published and activated");
        } else {
            log.error("Failed to create GDPR Consent Form template: {}", result.getErrorMessage());
        }
    }

    private FormSection buildPersonalInfoSection() {
        List<FormField> fields = new ArrayList<>();

        fields.add(FormField.builder()
            .name("full_name")
            .type("text")
            .label("Nume și Prenume")
            .placeholder("Introduceți numele complet")
            .required(true)
            .maxLength(100)
            .build());

        fields.add(FormField.builder()
            .name("address")
            .type("text")
            .label("Adresă")
            .placeholder("Introduceți adresa completă")
            .required(true)
            .maxLength(200)
            .build());

        fields.add(FormField.builder()
            .name("patient_or_legal_representative")
            .type("text")
            .label("Pacient / Reprezentant Legal")
            .placeholder("Specificați calitatea")
            .required(true)
            .maxLength(100)
            .build());

        fields.add(FormField.builder()
            .name("phone_number_mobile")
            .type("tel")
            .label("Telefon Mobil")
            .placeholder("07xxxxxxxx")
            .required(true)
            .pattern("^[0-9+\\-\\s()]+$")
            .build());

        fields.add(FormField.builder()
            .name("email_address")
            .type("email")
            .label("Adresă Email")
            .placeholder("exemplu@email.ro")
            .required(true)
            .pattern("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
            .build());

        return FormSection.builder()
            .sectionId("personal_information")
            .title("Informații Personale")
            .description("Vă rugăm să completați datele dvs. personale")
            .fields(fields)
            .collapsible(false)
            .order(1)
            .build();
    }

    private FormSection buildInitialConsentSection() {
        List<FormField> fields = new ArrayList<>();

        fields.add(FormField.builder()
            .name("name_surname_section1")
            .type("text")
            .label("Nume și Prenume (Confirmare)")
            .placeholder("Confirmați numele complet")
            .required(true)
            .maxLength(100)
            .build());

        fields.add(FormField.builder()
            .name("signature_section1")
            .type("signature")
            .label("Semnătură")
            .required(true)
            .signatureType("digital")
            .requiresWitness(false)
            .build());

        fields.add(FormField.builder()
            .name("date_section1")
            .type("date")
            .label("Data")
            .required(true)
            .build());

        return FormSection.builder()
            .sectionId("initial_consent")
            .title("Consimțământ Inițial")
            .description("Confirmați consimțământul și semnați formularul")
            .fields(fields)
            .collapsible(false)
            .order(2)
            .build();
    }

    private FormSection buildMarketingPreferencesSection() {
        List<FormField> fields = new ArrayList<>();

        fields.add(FormField.builder()
            .name("marketing_consent")
            .type("radio")
            .label("Consimțământ pentru Marketing")
            .required(true)
            .options(List.of(
                FieldOption.builder().value("agree").label("sunt de acord").build(),
                FieldOption.builder().value("disagree").label("nu sunt de acord").build()
            ))
            .build());

        fields.add(FormField.builder()
            .name("marketing_contact_channels")
            .type("checkbox")
            .label("Canale de Contact pentru Marketing")
            .required(false)
            .options(List.of(
                FieldOption.builder().value("email").label("e-mail").build(),
                FieldOption.builder().value("sms").label("SMS").build(),
                FieldOption.builder().value("phone").label("telefon").build(),
                FieldOption.builder().value("whatsapp").label("Whatsapp").build()
            ))
            .build());

        fields.add(FormField.builder()
            .name("preference_analysis_consent")
            .type("radio")
            .label("Consimțământ pentru Analiza Preferințelor")
            .required(true)
            .options(List.of(
                FieldOption.builder().value("agree").label("Sunt de acord").build(),
                FieldOption.builder().value("disagree").label("Nu sunt de acord").build()
            ))
            .build());

        fields.add(FormField.builder()
            .name("preference_contact_channels")
            .type("checkbox")
            .label("Canale de Contact pentru Preferințe")
            .required(false)
            .options(List.of(
                FieldOption.builder().value("email").label("e-mail").build(),
                FieldOption.builder().value("sms").label("SMS").build(),
                FieldOption.builder().value("phone").label("telefon").build(),
                FieldOption.builder().value("whatsapp").label("Whatsapp").build()
            ))
            .build());

        fields.add(FormField.builder()
            .name("health_data_processing_consent")
            .type("radio")
            .label("Consimțământ pentru Prelucrarea Datelor Medicale")
            .required(true)
            .options(List.of(
                FieldOption.builder().value("agree").label("sunt de acord").build(),
                FieldOption.builder().value("disagree").label("nu sunt de acord").build()
            ))
            .build());

        return FormSection.builder()
            .sectionId("marketing_and_preferences")
            .title("Marketing și Preferințe")
            .description("Selectați preferințele dvs. pentru comunicări și marketing")
            .fields(fields)
            .collapsible(true)
            .order(3)
            .build();
    }

    private FormSection buildFinalSignatureSection() {
        List<FormField> fields = new ArrayList<>();

        fields.add(FormField.builder()
            .name("signature_final")
            .type("signature")
            .label("Semnătură Finală")
            .required(true)
            .signatureType("digital")
            .requiresWitness(false)
            .build());

        fields.add(FormField.builder()
            .name("date_final")
            .type("date")
            .label("Data Semnării")
            .required(true)
            .build());

        return FormSection.builder()
            .sectionId("final_signature")
            .title("Semnătură Finală")
            .description("Semnați formularul pentru a confirma consimțământul")
            .fields(fields)
            .collapsible(false)
            .order(4)
            .build();
    }

    private void createDefaultManagerIfNeeded() {
        // Check if any managers exist
        if (managerRepository.count() > 0) {
            log.info("Managers already exist in database, skipping default manager creation");
            return;
        }

        log.info("No managers found in database, creating default manager 'a'...");

        try {
            // 1. Get or create MANAGER role
            Role managerRole = roleRepository.findByName(UserRole.MANAGER)
                    .orElseGet(() -> {
                        log.info("MANAGER role not found, creating it...");
                        Role newRole = Role.builder()
                                .name(UserRole.MANAGER)
                                .description("Manager role with administrative privileges")
                                .build();
                        return roleRepository.save(newRole);
                    });

            // 2. Create User entity with username="a" and password="a"
            User managerUser = User.builder()
                    .username("a")
                    .password(passwordEncoder.encode("a"))
                    .enabled(true)
                    .accountNonLocked(true)
                    .build();

            // 3. Assign MANAGER role using helper method
            managerUser.addRole(managerRole);

            // 4. Save user (to get generated userId)
            managerUser = userRepository.save(managerUser);

            // 5. Create Manager profile
            Manager managerProfile = Manager.builder()
                    .user(managerUser)
                    .fullName("Default Manager")
                    .build();

            // 6. Set bidirectional relationship
            managerUser.setManagerProfile(managerProfile);

            // 7. Save user again (cascade saves manager profile)
            userRepository.save(managerUser);

            log.info("Default manager 'a' created successfully with ID: {}", managerUser.getUserId());
        } catch (Exception e) {
            log.error("Failed to create default manager: {}", e.getMessage(), e);
        }
    }
}
