package com.example.policlicabine.config;

import com.example.policlicabine.common.Result;
import com.example.policlicabine.controller.FormTemplateController;
import com.example.policlicabine.dto.AuthResponseWrapper;
import com.example.policlicabine.dto.FormTemplateDto;
import com.example.policlicabine.dto.ManagerDto;
import com.example.policlicabine.dto.RegisterManagerRequest;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.FormPurpose;
import com.example.policlicabine.entity.enums.PermissionEnum;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.model.FieldOption;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.ManagerRepository;
import com.example.policlicabine.repository.PermissionRepository;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.repository.UserRepository;
import com.example.policlicabine.service.AuthenticationService;
import com.example.policlicabine.service.FormTemplateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitialConfig {

    private final FormTemplateService formTemplateService;
    private final FormTemplateController formTemplateController;
    private final ManagerRepository managerRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationService authenticationService;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
//            if (Boolean.TRUE) return;
//            createGdprConsentFormTemplate();
            List<FormTemplateDto> body = formTemplateController.getAllActiveTemplates().getBody();
            System.out.println("ASD");

            // Initialize permissions and roles FIRST
            initializePermissionAndRoles();

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

    private void initializePermissionAndRoles() {
        log.info("Initializing permissions and roles...");

        try {
            // 1. Create or get the 'ALL' permission
            Permission allPermission = permissionRepository.findByName(PermissionEnum.ALL)
                    .orElseGet(() -> {
                        log.info("Creating 'ALL' permission...");
                        Permission newPermission = Permission.builder()
                                .name(PermissionEnum.ALL)
                                .description(PermissionEnum.ALL.getDescription())
                                .build();
                        return permissionRepository.save(newPermission);
                    });

            log.info("'ALL' permission ready with ID: {}", allPermission.getPermissionId());

            // 2. Create all roles with the 'ALL' permission
            Set<Permission> permissions = new HashSet<>();
            permissions.add(allPermission);

            for (UserRole roleName : UserRole.values()) {
                roleRepository.findByName(roleName).ifPresentOrElse(
                        existingRole -> {
                            log.info("Role '{}' already exists, skipping creation", roleName);
                        },
                        () -> {
                            log.info("Creating role '{}'...", roleName);
                            Role newRole = Role.builder()
                                    .name(roleName)
                                    .description(getRoleDescription(roleName))
                                    .permissions(new HashSet<>(permissions))
                                    .build();
                            roleRepository.save(newRole);
                            log.info("Role '{}' created successfully with 'ALL' permission", roleName);
                        }
                );
            }

            log.info("Permission and role initialization completed successfully");
        } catch (Exception e) {
            log.error("Failed to initialize permissions and roles: {}", e.getMessage(), e);
        }
    }

    private String getRoleDescription(UserRole role) {
        return switch (role) {
            case DOCTOR -> "Medical doctor with patient care privileges";
            case PATIENT -> "Patient with medical record access";
            case RECEPTIONIST -> "Reception staff with appointment management";
            case MANAGER -> "Manager with administrative privileges";
            case ADMIN -> "System administrator with full access";
        };
    }

    private void createDefaultManagerIfNeeded() {
        // Check if any managers exist
        if (managerRepository.count() > 0) {
            log.info("Managers already exist in database, skipping default manager creation");
            return;
        }

        log.info("No managers found in database, creating default manager...");

        try {
            // Use the new registerManager method from AuthenticationService
            RegisterManagerRequest request = RegisterManagerRequest.builder()
                    .username("manager@gmail.com")
                    .password("manager@gmail.com")
                    .fullName("Default Manager")
                    .build();

            Result<AuthResponseWrapper<ManagerDto>> result = authenticationService.registerManager(request);

            if (result.isSuccess()) {
                log.info("Default manager created successfully with username: {} and ID: {}",
                        result.getValue().getAuthResponse().getUsername(),
                        result.getValue().getProfile().getManagerId());
            } else {
                log.error("Failed to create default manager: {}", result.getErrorMessage());
            }
        } catch (Exception e) {
            log.error("Exception while creating default manager: {}", e.getMessage(), e);
        }
    }
}
