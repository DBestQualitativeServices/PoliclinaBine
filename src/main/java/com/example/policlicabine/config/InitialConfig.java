package com.example.policlicabine.config;

import com.example.policlicabine.config.dto.ConsultationJsonEntry;
import com.example.policlicabine.dto.*;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.PermissionEnum;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.model.FieldOption;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.ConsultationRepository;
import com.example.policlicabine.repository.PermissionRepository;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.service.AuthenticationService;
import com.example.policlicabine.service.ConsultationService;
import com.example.policlicabine.service.FormSubmissionService;
import com.example.policlicabine.service.FormTemplateService;
import com.example.policlicabine.service.ManagerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;


/**
 * Initial configuration for seeding the database with default data.
 *
 * <h2>Form Architecture Overview</h2>
 * The system supports two distinct form types:
 *
 * <h3>Type 1: Patient Pre-Appointment Forms</h3>
 * <ul>
 *   <li><b>Source:</b> {@code ConsultationType.requiredFormTemplates} (Set)</li>
 *   <li><b>Who fills:</b> Patient (before appointment)</li>
 *   <li><b>Purpose:</b> Consent, medical history, risk acknowledgement</li>
 *   <li><b>Scope:</b> Patient-level (reusable across appointments)</li>
 *   <li><b>Validation:</b> At booking time - patient must have valid signed forms</li>
 *   <li><b>Links:</b> FormSubmission has {@code appointmentSession = NULL}</li>
 * </ul>
 *
 * <h3>Type 2: Doctor Consultation Forms</h3>
 * <ul>
 *   <li><b>Source:</b> {@code ConsultationType.consultationFormTemplate} (single)</li>
 *   <li><b>Who fills:</b> Doctor (during/after consultation)</li>
 *   <li><b>Purpose:</b> Consultation outcome, treatment plan, examination results</li>
 *   <li><b>Scope:</b> Appointment-specific</li>
 *   <li><b>Validation:</b> At appointment completion</li>
 *   <li><b>Links:</b> FormSubmission has {@code appointmentSession = REQUIRED}</li>
 * </ul>
 *
 * <h3>Template Identification</h3>
 * Each template is uniquely identified by its {@code name} field.
 * ConsultationTypes reference templates by ID. Past appointments remain
 * valid because FormSubmission links to exact template used (via FK).
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class InitialConfig {

    private final FormTemplateService formTemplateService;
    private final FormSubmissionService formSubmissionService;
    private final ConsultationService consultationService;
    private final ConsultationRepository consultationRepository;
    private final ManagerService managerService;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Bean
    @Profile("!test")
        // Exclude from test execution to prevent duplicate data issues
    CommandLineRunner commandLineRunner() {
        return args -> {
//            createDermapenConsultationType();
//            if (Boolean.TRUE) return;
//            createGdprConsentFormTemplate();
//            createDermapenConsentFormTemplate();
//             Initialize permissions and roles FIRST
//            initializePermissionAndRoles();

//             Initialize default consultation types
//            initializeDefaultConsultationTypes();

//             Create consultation with GDPR form requirement
//            createConsultationWithGdprForm();

//             Create and link General Dermatology Visited Form
//            createGeneralDermatologyVisitedForm();

//             Create default manager if no managers exist
//            createDefaultManagerIfNeeded();

//             Create Dermapen consent form and consultation type
//            createDermapenConsentFormTemplate();
//            createDermapenConsultationType();

            // Import consultation types from JSON file
//            importConsultationTypesFromJson();
        };
    }

    /**
     * Creates a GDPR Consent Form template (Type 1: Patient Pre-Appointment Form).
     *
     * <p>This form is filled by patients BEFORE their appointment to provide GDPR consent.
     * It is linked to ConsultationTypes via {@code requiredFormTemplates} collection.</p>
     *
     * @see #createConsultationWithGdprForm() for linking this form to a consultation
     */
    private void createGdprConsentFormTemplate() {
        // Check if GDPR form template already exists (idempotency check)
        FormTemplate existingTemplate = formTemplateService.getEntityByName("Formular de Consimțământ GDPR");
        if (existingTemplate != null) {
            log.info("GDPR Consent Form template already exists with ID: {}, skipping creation", existingTemplate.getId());
            return;
        }

        log.info("Creating GDPR Consent Form template (Type 1: Patient Pre-Appointment Form)...");

        // Build form structure
        FormStructure structure = FormStructure.builder()
                .formId("gdpr-consent-form")
                .version("1.0")
                .title("Formular de Consimțământ GDPR V2")
                .description("Acord pentru prelucrarea datelor personale conform GDPR")
                .sections(List.of(
                        buildSection0PatientInfo(),
                        buildSection1SignatureDate(),
                        buildSection21DatabaseConsents(),
                        buildSection22CosmeticStudy(),
                        buildSection23HealthDataAccess()
                ))
                .build();

        // Create template (name is unique identifier)
        try {
            FormTemplateDto result = formTemplateService.createTemplate(
                    "Formular de Consimțământ GDPR",    // name (unique)
                    structure,                          // form structure
                    12,                                 // validityMonths (12 months)
                    null                                // createdByUserId (null = system)
            );
            log.info("GDPR Consent Form template created successfully with ID: {}", result.getId());

            // Publish template to activate
            FormTemplateDto publishResult = formTemplateService.publishTemplate(result.getId());

            log.info("GDPR Consent Form template published and activated");
        } catch (Exception e) {
            log.error("Failed to create/publish GDPR Consent Form template: {}", e.getMessage());
        }
    }

    /**
     * Section 0: Patient Information
     * Fields: firstName, lastName, minorName
     */
    private FormSection buildSection0PatientInfo() {
        return FormSection.builder()
                .sectionId("section_0_patient_info")
                .title("Informații Pacient")
                .description("Completați datele personale")
                .fields(List.of(
                        FormField.builder()
                                .name("firstName")
                                .type("text")
                                .label("Prenume")
                                .placeholder("Introduceți prenumele")
                                .required(true)
                                .maxLength(100)
                                .build(),
                        FormField.builder()
                                .name("lastName")
                                .type("text")
                                .label("Nume de familie")
                                .placeholder("Introduceți numele de familie")
                                .required(true)
                                .maxLength(100)
                                .build(),
                        FormField.builder()
                                .name("minorName")
                                .type("text")
                                .label("Nume Minor (dacă este cazul)")
                                .placeholder("Completați doar dacă pacientul este minor")
                                .required(false)
                                .maxLength(100)
                                .build()
                ))
                .collapsible(false)
                .order(1)
                .build();
    }

    /**
     * Section 1: Signature and Date
     * Fields: documentDate, signature
     */
    private FormSection buildSection1SignatureDate() {
        return FormSection.builder()
                .sectionId("section_1_signature")
                .title("Data și Semnătură")
                .description("Confirmați data și semnați")
                .fields(List.of(
                        FormField.builder()
                                .name("documentDate")
                                .type("date")
                                .label("Data Documentului")
                                .required(true)
                                .build(),
                        FormField.builder()
                                .name("patient_signature")
                                .type("signature")
                                .label("Semnătura Pacientului")
                                .signatureType("PATIENT")
                                .required(true)
                                .acceptedFileTypes(List.of("image/png", "image/jpeg"))
                                .maxFileSizeBytes(5242880L)
                                .build()
                ))
                .collapsible(false)
                .order(2)
                .build();
    }

    /**
     * Section 2.1: Database Information Consents
     * Fields: section21_consent (radio), section21_contact_* (checkboxes)
     */
    private FormSection buildSection21DatabaseConsents() {
        return FormSection.builder()
                .sectionId("section_21_database_consent")
                .title("Secțiunea 2.1 - Consimțământ Baza de Date")
                .description("Consimțământ pentru includerea în baza de date")
                .fields(List.of(
                        FormField.builder()
                                .name("section21_consent")
                                .type("radio")
                                .label("Consimțământ pentru baza de date")
                                .required(true)
                                .options(List.of(
                                        FieldOption.builder().value("true").label("Da, sunt de acord").build(),
                                        FieldOption.builder().value("no").label("Nu, nu sunt de acord").build()
                                ))
                                .build(),
                        FormField.builder().name("section21_contact_email").type("checkbox").label("Contact prin E-mail").required(false).build(),
                        FormField.builder().name("section21_contact_sms").type("checkbox").label("Contact prin SMS").required(false).build(),
                        FormField.builder().name("section21_contact_phone").type("checkbox").label("Contact prin Telefon").required(false).build(),
                        FormField.builder().name("section21_contact_whatsapp").type("checkbox").label("Contact prin WhatsApp").required(false).build()
                ))
                .collapsible(true)
                .order(3)
                .build();
    }

    /**
     * Section 2.2: Cosmetic Product Quality Study
     * Fields: section22_consent (radio), section22_contact_* (checkboxes)
     */
    private FormSection buildSection22CosmeticStudy() {
        return FormSection.builder()
                .sectionId("section_22_cosmetics_study")
                .title("Secțiunea 2.2 - Studiu Produse Cosmetice")
                .description("Consimțământ pentru studii cosmetice")
                .fields(List.of(
                        FormField.builder()
                                .name("section22_consent")
                                .type("radio")
                                .label("Consimțământ pentru studii cosmetice")
                                .required(true)
                                .options(List.of(
                                        FieldOption.builder().value("true").label("Da, sunt de acord").build(),
                                        FieldOption.builder().value("no").label("Nu, nu sunt de acord").build()
                                ))
                                .build(),
                        FormField.builder().name("section22_contact_email").type("checkbox").label("Contact prin E-mail").required(false).build(),
                        FormField.builder().name("section22_contact_sms").type("checkbox").label("Contact prin SMS").required(false).build(),
                        FormField.builder().name("section22_contact_phone").type("checkbox").label("Contact prin Telefon").required(false).build(),
                        FormField.builder().name("section22_contact_whatsapp").type("checkbox").label("Contact prin WhatsApp").required(false).build()
                ))
                .collapsible(true)
                .order(4)
                .build();
    }

    /**
     * Section 2.3: Health Data Access
     * Fields: section23_consent (radio)
     */
    private FormSection buildSection23HealthDataAccess() {
        return FormSection.builder()
                .sectionId("section_23_health_data")
                .title("Secțiunea 2.3 - Acces Date Sănătate")
                .description("Consimțământ pentru acces date medicale")
                .fields(List.of(
                        FormField.builder()
                                .name("section23_consent")
                                .type("radio")
                                .label("Consimțământ pentru acces date sănătate")
                                .required(true)
                                .options(List.of(
                                        FieldOption.builder().value("true").label("Da, sunt de acord").build(),
                                        FieldOption.builder().value("false").label("Nu, nu sunt de acord").build()
                                ))
                                .build()
                ))
                .collapsible(true)
                .order(5)
                .build();
    }

    /**
     * Initializes default consultation types if they don't exist.
     * Currently creates:
     * - General Dermatology (150.00 RON, 30 minutes)
     * <p>
     * This method is idempotent - safe to call multiple times.
     */
    private void initializeDefaultConsultationTypes() {
        // Check if General Dermatology consultation already exists
        if (consultationService.getEntityByName("General Dermatology") != null) {
            log.info("General Dermatology consultation type already exists, skipping creation");
            return;
        }

        log.info("Creating General Dermatology consultation type...");

        try {
            ConsultationTypeDto result = consultationService.createConsultation(
                    "General Dermatology",           // name
                    Specialty.GENERAL_DERMATOLOGY,   // specialty
                    new BigDecimal("150.00"),        // price (must use BigDecimal, not double)
                    "RON",                           // priceCurrency
                    30,                              // durationMinutes
                    false                            // requiresSurgeryRoom
            );

            log.info("General Dermatology consultation type created successfully with ID: {}",
                    result.getConsultationId());
        } catch (Exception e) {
            log.error("Failed to create General Dermatology consultation type: {}",
                    e.getMessage());
        }
    }

    /**
     * Creates "Consultation with GDPR" consultation type and links the GDPR consent form
     * as a required pre-appointment form (Type 1).
     *
     * <p><b>Form Type 1: Patient Pre-Appointment Forms</b></p>
     * <p>Uses {@code ConsultationType.requiredFormTemplates} to specify forms that patients
     * must complete BEFORE their appointment. The system validates form completion at booking time.</p>
     *
     * <p>Key characteristics:</p>
     * <ul>
     *   <li>Patient fills out the form (not the doctor)</li>
     *   <li>Form is reusable across appointments (patient-level)</li>
     *   <li>FormSubmission has {@code appointmentSession = NULL}</li>
     *   <li>Validated at booking based on template ID + expiry date</li>
     * </ul>
     *
     * <p>This method is idempotent - safe to call multiple times.</p>
     *
     * @see #createGdprConsentFormTemplate() for the form template creation
     */
    private void createConsultationWithGdprForm() {
        // Step 1: Get or create the consultation
        ConsultationType consultationWithGdpr = consultationService.getEntityByName("Consultation with GDPR");
        UUID consultationId;

        if (consultationWithGdpr != null) {
            log.info("Consultation with GDPR already exists with ID: {}", consultationWithGdpr.getConsultationId());
            consultationId = consultationWithGdpr.getConsultationId();
        } else {
            log.info("Creating Consultation with GDPR consultation type (with Type 1 pre-appointment form)...");

            try {
                ConsultationTypeDto result = consultationService.createConsultation(
                        "Consultation with GDPR",        // name
                        Specialty.GENERAL_DERMATOLOGY,   // specialty
                        new BigDecimal("150.00"),        // price (must use BigDecimal)
                        "RON",                           // priceCurrency
                        30,                              // durationMinutes
                        false                            // requiresSurgeryRoom
                );

                consultationId = result.getConsultationId();
                log.info("Consultation with GDPR created successfully with ID: {}", consultationId);
            } catch (Exception e) {
                log.error("Failed to create Consultation with GDPR: {}", e.getMessage());
                return; // Cannot proceed without consultation
            }
        }

        // Step 2: Get the GDPR form template by name
        FormTemplate gdprForm = formTemplateService.getEntityByName("Formular de Consimțământ GDPR");
        if (gdprForm == null) {
            log.error("GDPR consent form not found. Cannot link to consultation.");
            return; // Cannot link without form
        }

        // Step 3: Always ensure the form is linked (idempotent linking)
        log.info("Ensuring GDPR form (ID: {}) is linked to consultation (ID: {})", gdprForm.getId(), consultationId);

        try {
            consultationService.setRequiredFormTemplates(
                    consultationId,
                    List.of(gdprForm.getId())
            );

            log.info("Successfully linked GDPR consent form as required form for Consultation with GDPR");
        } catch (Exception e) {
            log.error("Failed to link GDPR form to Consultation with GDPR: {}", e.getMessage());
        }
    }

    /**
     * Creates the General Dermatology Visited Form template and links it to the
     * General Dermatology consultation type (Type 2: Doctor Consultation Form).
     *
     * <p><b>Form Type 2: Doctor Consultation Forms</b></p>
     * <p>Uses {@code ConsultationType.consultationFormTemplate} (single form) that doctors
     * fill out DURING or AFTER the consultation to document outcomes.</p>
     *
     * <p>Key characteristics:</p>
     * <ul>
     *   <li>Doctor fills out the form (not the patient)</li>
     *   <li>Form is appointment-specific (linked to AppointmentSession)</li>
     *   <li>FormSubmission has {@code appointmentSession = REQUIRED}</li>
     *   <li>Validated at appointment completion</li>
     * </ul>
     *
     * <p>This method is idempotent - safe to call multiple times.</p>
     *
     * @see #linkFormToConsultation(UUID) for linking the form to consultation type
     */
    private void createGeneralDermatologyVisitedForm() {
        // Step 1: Check if form already exists (idempotency)
        if (formTemplateService.getEntityByName("Formular Vizită Dermatologie Generală") != null) {
            log.info("General Dermatology Visited Form template already exists, skipping creation");
            return;
        }

        log.info("Creating General Dermatology Visited Form template (Type 2: Doctor Consultation Form)...");

        // Step 2: Build form structure with one section and one boolean field
        FormStructure structure = FormStructure.builder()
                .title("Formular Vizită Dermatologie Generală")
                .description("Chestionar simplu pentru înregistrarea vizitei")
                .sections(List.of(
                        buildVisitedSection()
                ))
                .build();

        // Step 3: Create form template (name is unique identifier)
        try {
            FormTemplateDto result = formTemplateService.createTemplate(
                    "Formular Vizită Dermatologie Generală",  // name (unique)
                    structure,                                // form structure
                    null,                                     // No expiry (null validity months)
                    null                                      // No specific creator
            );

            log.info("General Dermatology Visited Form template created successfully with ID: {}",
                    result.getId());

            // Step 4: Publish the template to activate it
            FormTemplateDto publishResult = formTemplateService.publishTemplate(result.getId());

            log.info("General Dermatology Visited Form template published and activated");

            // Step 5: Link form to General Dermatology consultation type
            linkFormToConsultation(result.getId());
        } catch (Exception e) {
            log.error("Failed to create/publish General Dermatology Visited Form template: {}",
                    e.getMessage());
        }
    }

    /**
     * Helper method to build the "Visited" section with a boolean radio field.
     */
    private FormSection buildVisitedSection() {
        List<FormField> fields = new ArrayList<>();

        // Boolean field using radio buttons with Romanian labels
        fields.add(FormField.builder()
                .name("visited")
                .type("radio")  // Radio type for boolean with custom labels
                .label("Ați mai vizitat clinica noastră?")
                .required(true)
                .options(List.of(
                        FieldOption.builder()
                                .value("true")   // Boolean value as string
                                .label("Da")     // Romanian "Yes"
                                .build(),
                        FieldOption.builder()
                                .value("false")  // Boolean value as string
                                .label("Nu")     // Romanian "No"
                                .build()
                ))
                .build());

        return FormSection.builder()
                .sectionId("visited_section")
                .title("Informații Vizită")
                .description("Vă rugăm să indicați dacă ați mai fost la clinica noastră")
                .fields(fields)
                .collapsible(false)  // Always visible
                .order(1)
                .build();
    }

    /**
     * Links a form template as the Doctor Consultation Form (Type 2) for General Dermatology.
     *
     * <p>This sets {@code ConsultationType.consultationFormTemplate} which is the single form
     * that doctors fill out during/after consultations to document outcomes.</p>
     *
     * <p><b>Type 2 Form Linking:</b> Uses {@code setConsultationFormTemplate()} for single form.</p>
     * <p><b>Type 1 Form Linking:</b> Uses {@code setRequiredFormTemplates()} for collection of forms.</p>
     *
     * @param formTemplateId The ID of the form template to link as consultation form
     * @see #createConsultationWithGdprForm() for Type 1 form linking example
     */
    private void linkFormToConsultation(UUID formTemplateId) {
        try {
            log.info("Linking form template {} to General Dermatology consultation...", formTemplateId);

            // Step 1: Get the General Dermatology consultation entity
            ConsultationType generalDerm = consultationService.getEntityByName("General Dermatology");

            if (generalDerm == null) {
                log.error("General Dermatology consultation type not found. Cannot link form.");
                return;
            }

            // Step 2: Use service method to set the consultation form template (Type 2 form)
            consultationService.setConsultationFormTemplate(generalDerm.getConsultationId(), formTemplateId);

            log.info("Successfully linked form template {} as primary consultation form for General Dermatology (consultation ID: {})",
                    formTemplateId, generalDerm.getConsultationId());

        } catch (Exception e) {
            log.error("Failed to link form template to General Dermatology consultation: {}",
                    e.getMessage(), e);
        }
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
        if (managerService.count() > 0) {
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

            AuthResponseWrapper<ManagerDto> result = authenticationService.registerManager(request);

            log.info("Default manager created successfully with username: {} and ID: {}",
                    result.getAuthResponse().getUsername(),
                    result.getProfile().getManagerId());
        } catch (Exception e) {
            log.error("Exception while creating default manager: {}", e.getMessage(), e);
        }
    }

    // ============================================================================
    // DERMAPEN CONSENT FORM TEMPLATE (Type 1: Patient Pre-Appointment Form)
    // ============================================================================

    /**
     * Creates Dermapen Consent Form template (Type 1: Patient Pre-Appointment Form).
     *
     * <p>This form is filled by patients BEFORE their Dermapen procedure appointment
     * to provide informed consent. It covers procedure explanation, risks, benefits,
     * and data handling consent.</p>
     *
     * @see #createDermapenConsultationType() for linking this form to a consultation
     */
    private void createDermapenConsentFormTemplate() {
        // Check if Dermapen form template already exists (idempotency check)
        FormTemplate existingTemplate = formTemplateService.getEntityByName("Consimțământ Dermapen");
        if (existingTemplate != null) {
            log.info("Dermapen Consent Form template already exists with ID: {}, skipping creation", existingTemplate.getId());
            return;
        }

        log.info("Creating Dermapen Consent Form template (Type 1: Patient Pre-Appointment Form)...");

        // Build form structure
        FormStructure structure = FormStructure.builder()
                .formId("dermapen-consent-form")
                .version("1.0")
                .title("Consimțământ Dermapen")
                .description("Formular de consimțământ pentru procedura DERMAPEN")
                .sections(List.of(
                        buildDermapenPatientInfoSection(),
                        buildDermapenDoctorInfoSection(),
                        buildDermapenConsentQuestionsSection(),
                        buildDermapenSignatureSection()
                ))
                .build();

        // Create template (name is unique identifier)
        try {
            FormTemplateDto result = formTemplateService.createTemplate(
                    "Consimțământ Dermapen",    // name (unique)
                    structure,                   // form structure
                    12,                          // validityMonths (12 months)
                    null                         // createdByUserId (null = system)
            );
            log.info("Dermapen Consent Form template created successfully with ID: {}", result.getId());

            // Set PDF template URL
            FormTemplate template = formTemplateService.getEntityByName("Consimțământ Dermapen");
            if (template != null) {
                template.setPdfTemplateUrl("https://id0storage0.blob.core.windows.net/clinica-bine/templates/ConsimtamantDermapen_v1.pdf");
                log.info("Set PDF template URL for Dermapen Consent Form");
            }

            // Publish template to activate
            FormTemplateDto publishResult = formTemplateService.publishTemplate(result.getId());

            log.info("Dermapen Consent Form template published and activated");
        } catch (Exception e) {
            log.error("Failed to create/publish Dermapen Consent Form template: {}", e.getMessage());
        }
    }

    /**
     * Section: Patient Information
     * Fields: firstName, lastName
     */
    private FormSection buildDermapenPatientInfoSection() {
        return FormSection.builder()
                .sectionId("dermapen_patient_info")
                .title("Informații Pacient")
                .description("Datele personale ale pacientului")
                .fields(List.of(
                        FormField.builder()
                                .name("firstName")
                                .type("text")
                                .label("Prenume")
                                .placeholder("Introduceți prenumele")
                                .required(true)
                                .maxLength(100)
                                .build(),
                        FormField.builder()
                                .name("lastName")
                                .type("text")
                                .label("Nume de familie")
                                .placeholder("Introduceți numele de familie")
                                .required(true)
                                .maxLength(100)
                                .build()
                ))
                .collapsible(false)
                .order(1)
                .build();
    }

    /**
     * Section: Doctor Information
     * Fields: doctor_firstName, doctor_lastName
     */
    private FormSection buildDermapenDoctorInfoSection() {
        return FormSection.builder()
                .sectionId("dermapen_doctor_info")
                .title("Informații Medic")
                .description("Datele medicului care efectuează procedura")
                .fields(List.of(
                        FormField.builder()
                                .name("doctor_firstName")
                                .type("text")
                                .label("Prenume Medic")
                                .placeholder("Prenumele medicului")
                                .required(true)
                                .maxLength(100)
                                .build(),
                        FormField.builder()
                                .name("doctor_lastName")
                                .type("text")
                                .label("Nume Medic")
                                .placeholder("Numele medicului")
                                .required(true)
                                .maxLength(100)
                                .build()
                ))
                .collapsible(false)
                .order(2)
                .build();
    }

    /**
     * Section: Consent Questions (10 radio fields)
     * Fields: section_1 through section_10
     */
    private FormSection buildDermapenConsentQuestionsSection() {
        List<FieldOption> yesNoOptions = List.of(
                FieldOption.builder().value("true").label("Da").build(),
                FieldOption.builder().value("false").label("Nu").build()
        );

        return FormSection.builder()
                .sectionId("dermapen_consent_questions")
                .title("Consimțământ Informat")
                .description("Vă rugăm să confirmați că ați fost informat(ă) cu privire la următoarele aspecte")
                .fields(List.of(
                        FormField.builder()
                                .name("section_1")
                                .type("radio")
                                .label("Ce înseamnă și care este scopul procedurii DERMAPEN")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_2")
                                .type("radio")
                                .label("Intervențiile și strategia de diagnostic și tratament propuse")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_3")
                                .type("radio")
                                .label("Beneficiile și consecințele procedurii DERMAPEN")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_4")
                                .type("radio")
                                .label("Riscurile potențiale ale procedurii DERMAPEN")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_5")
                                .type("radio")
                                .label("Alternative de diagnostic și tratament și riscurile acestora")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_6")
                                .type("radio")
                                .label("Riscurile neefectuării procedurii DERMAPEN")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_7")
                                .type("radio")
                                .label("Riscurile nerespectării recomandărilor medicale")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_8")
                                .type("radio")
                                .label("Pacientul este de acord cu recoltarea, păstrarea și folosirea produselor biologice (exclusiv în scopuri didactice și științifice) cu condiția păstrării secretului identității")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_9")
                                .type("radio")
                                .label("Pacientul dorește să fie informat în continuare despre starea sa de sănătate")
                                .required(true)
                                .options(yesNoOptions)
                                .build(),
                        FormField.builder()
                                .name("section_10")
                                .type("radio")
                                .label("Pacientul este de acord cu stocarea datelor medicale, inclusiv a imaginilor și utilizarea acestora exclusiv în scopuri didactice și științifice cu condiția obligatorie a păstrării secretului identității sale")
                                .required(true)
                                .options(yesNoOptions)
                                .build()
                ))
                .collapsible(false)
                .order(3)
                .build();
    }

    /**
     * Section: Signatures and Dates
     * Fields: documentDate, documentDate_1, patient_signature, doctor_signature
     */
    private FormSection buildDermapenSignatureSection() {
        return FormSection.builder()
                .sectionId("dermapen_signatures")
                .title("Semnături și Data")
                .description("Confirmați documentul prin semnătură")
                .fields(List.of(
                        FormField.builder()
                                .name("documentDate")
                                .type("date")
                                .label("Data Documentului")
                                .required(true)
                                .build(),
                        FormField.builder()
                                .name("documentDate_1")
                                .type("date")
                                .label("Data Documentului (Confirmare)")
                                .required(false)
                                .build(),
                        FormField.builder()
                                .name("patient_signature")
                                .type("signature")
                                .label("Semnătura Pacientului")
                                .signatureType("PATIENT")
                                .required(true)
                                .acceptedFileTypes(List.of("image/png", "image/jpeg"))
                                .maxFileSizeBytes(5242880L)
                                .build(),
                        FormField.builder()
                                .name("doctor_signature")
                                .type("signature")
                                .label("Semnătura Medicului")
                                .signatureType("DOCTOR")
                                .required(true)
                                .acceptedFileTypes(List.of("image/png", "image/jpeg"))
                                .maxFileSizeBytes(5242880L)
                                .build()
                ))
                .collapsible(false)
                .order(4)
                .build();
    }

    // ============================================================================
    // DERMAPEN CONSULTATION TYPE
    // ============================================================================

    /**
     * Creates "Dermapen" consultation type and links both GDPR and Dermapen
     * consent forms as required pre-appointment forms (Type 1).
     *
     * <p>This method is idempotent - safe to call multiple times.</p>
     */
    private void createDermapenConsultationType() {
        // Step 1: Get or create the consultation
        ConsultationType dermapen = consultationService.getEntityByName("Dermapen");
        UUID consultationId;

        if (dermapen != null) {
            log.info("Dermapen consultation already exists with ID: {}", dermapen.getConsultationId());
            consultationId = dermapen.getConsultationId();
        } else {
            log.info("Creating Dermapen consultation type...");

            try {
                ConsultationTypeDto result = consultationService.createConsultation(
                        "Dermapen",                      // name
                        Specialty.GENERAL_DERMATOLOGY,   // specialty
                        new BigDecimal("200.00"),        // price
                        "RON",                           // priceCurrency
                        45,                              // durationMinutes
                        false                            // requiresSurgeryRoom
                );

                consultationId = result.getConsultationId();
                log.info("Dermapen consultation created successfully with ID: {}", consultationId);
            } catch (Exception e) {
                log.error("Failed to create Dermapen consultation: {}", e.getMessage());
                return;
            }
        }

        // Step 2: Get both form templates
        FormTemplate gdprForm = formTemplateService.getEntityByName("Formular de Consimțământ GDPR");
        if (gdprForm == null) {
            log.error("GDPR consent form not found. Cannot link to Dermapen consultation.");
            return;
        }

        FormTemplate dermapenForm = formTemplateService.getEntityByName("Consimțământ Dermapen");
        if (dermapenForm == null) {
            log.error("Dermapen consent form not found. Cannot link to Dermapen consultation.");
            return;
        }

        // Step 3: Link BOTH forms as required pre-appointment forms
        log.info("Linking GDPR form (ID: {}) and Dermapen form (ID: {}) to Dermapen consultation (ID: {})",
                gdprForm.getId(), dermapenForm.getId(), consultationId);

        try {
            consultationService.setRequiredFormTemplates(
                    consultationId,
                    List.of(gdprForm.getId(), dermapenForm.getId())
            );

            log.info("Successfully linked both consent forms as required forms for Dermapen consultation");
        } catch (Exception e) {
            log.error("Failed to link forms to Dermapen consultation: {}", e.getMessage());
        }
    }

    private String buildCategoryPath(
            String categoryLevel1,
            String categoryLevel2,
            String subcategoryLevel1,
            String subcategoryLevel2
    ) {
        List<String> parts = new ArrayList<>();
        if (categoryLevel1 != null && !categoryLevel1.isBlank()) {
            parts.add(categoryLevel1);
        }
        if (categoryLevel2 != null && !categoryLevel2.isBlank()) {
            parts.add(categoryLevel2);
        }
        if (subcategoryLevel1 != null && !subcategoryLevel1.isBlank()) {
            parts.add(subcategoryLevel1);
        }
        if (subcategoryLevel2 != null && !subcategoryLevel2.isBlank()) {
            parts.add(subcategoryLevel2);
        }
        return parts.isEmpty() ? null : String.join(" - ", parts);
    }

    private ConsultationTypeDto createConsultationTypeWithHierarchy(
            String name,
            Specialty specialty,
            BigDecimal price,
            String priceCurrency,
            Integer durationMinutes,
            Boolean requiresSurgeryRoom,
            Integer workflowStep,
            String categoryLevel1,
            String categoryLevel2,
            String subcategoryLevel1,
            String subcategoryLevel2,
            List<UUID> requiredFormTemplateIds
    ) {
        ConsultationType existing = consultationService.getEntityByName(name);
        if (existing != null) {
            log.info("Consultation type '{}' already exists with ID: {}, skipping creation", name, existing.getConsultationId());
            return consultationService.findById(existing.getConsultationId());
        }

        log.info("Creating consultation type '{}' with hierarchy", name);

        ConsultationTypeDto createdDto = consultationService.createConsultation(
                name,
                specialty,
                price,
                priceCurrency,
                durationMinutes,
                requiresSurgeryRoom
        );

        ConsultationType entity = consultationRepository.findById(createdDto.getConsultationId())
                .orElseThrow(() -> new RuntimeException("Failed to retrieve created consultation"));

        entity.setWorkflowStep(workflowStep);
        entity.setCategoryLevel1(categoryLevel1);
        entity.setCategoryLevel2(categoryLevel2);
        entity.setSubcategoryLevel1(subcategoryLevel1);
        entity.setSubcategoryLevel2(subcategoryLevel2);
        entity.setCategoryPath(buildCategoryPath(categoryLevel1, categoryLevel2, subcategoryLevel1, subcategoryLevel2));

        consultationRepository.save(entity);

        if (requiredFormTemplateIds != null && !requiredFormTemplateIds.isEmpty()) {
            consultationService.setRequiredFormTemplates(entity.getConsultationId(), requiredFormTemplateIds);
        }

        log.info("Consultation type '{}' created successfully with ID: {}", name, entity.getConsultationId());

        return consultationService.findById(entity.getConsultationId());
    }

    // ============================================================================
    // JSON IMPORT: CONSULTATION TYPES FROM consultations.json
    // ============================================================================

    /**
     * Builds a full hierarchical name for a consultation from the JSON entry.
     * 
     * <p>Concatenates: categoryFinalString + subcategoryLevel1 + subcategoryLevel2</p>
     * <p>Joins multiple parts with " - " separator</p>
     * <p>Returns single categoryFinalString if no subcategories exist</p>
     * 
     * @param entry the JSON entry to build name from
     * @return the constructed consultation name
     */
    private String buildConsultationName(ConsultationJsonEntry entry) {
        List<String> parts = new ArrayList<>();

        // Add category final string (e.g., "Medical - Consult preventiv")
        if (entry.categoryFinalString() != null && !entry.categoryFinalString().isBlank()) {
            parts.add(entry.categoryFinalString());
        }

        // Add subcategory level 1 if present
        if (entry.subcategoryLevel1() != null && !entry.subcategoryLevel1().isBlank()) {
            parts.add(entry.subcategoryLevel1());
        }

        // Add subcategory level 2 if present
        if (entry.subcategoryLevel2() != null && !entry.subcategoryLevel2().isBlank()) {
            parts.add(entry.subcategoryLevel2());
        }

        // If we only have category final string, return it as is
        if (parts.size() == 1) {
            return parts.get(0);
        }

        // Otherwise join all parts with " - " separator
        return String.join(" - ", parts);
    }

    /**
     * Parses price from JSON Object field, handling all special value cases.
     * 
     * <p>Handles:</p>
     * <ul>
     *   <li>Numeric values (Integer, Double, BigDecimal) → converted to BigDecimal</li>
     *   <li>String numbers ("450") → parsed to BigDecimal</li>
     *   <li>Special values ("#N/A", "?", "in functie de", "in discutie") → BigDecimal.ZERO</li>
     *   <li>null → BigDecimal.ZERO</li>
     *   <li>Unparseable strings → BigDecimal.ZERO with warning</li>
     * </ul>
     * 
     * @param priceRaw the raw price from JSON (can be Number or String)
     * @return parsed BigDecimal price, or ZERO for invalid/variable prices
     */
    private BigDecimal parsePrice(Object priceRaw) {
        if (priceRaw == null) {
            return BigDecimal.ZERO;
        }

        // If it's already a number (Integer, Double, BigDecimal, etc.)
        if (priceRaw instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        // If it's a string
        String priceStr = priceRaw.toString().trim();

        // Handle special/variable price values
        if (priceStr.isEmpty() 
            || priceStr.equals("#N/A") 
            || priceStr.equals("?") 
            || priceStr.toLowerCase().contains("in functie de") 
            || priceStr.toLowerCase().contains("in discutie")) {
            log.info("Variable/unknown price detected: '{}', using BigDecimal.ZERO", priceRaw);
            return BigDecimal.ZERO;
        }

        // Try to parse as number
        try {
            return new BigDecimal(priceStr);
        } catch (NumberFormatException e) {
            log.warn("Unable to parse price '{}', defaulting to BigDecimal.ZERO", priceRaw);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Maps JSON category name to Specialty enum value.
     * 
     * <p>Mapping:</p>
     * <ul>
     *   <li>"Medical" → MEDICAL_DERMATOLOGY</li>
     *   <li>"Estetica" → COSMETIC_DERMATOLOGY</li>
     *   <li>Unknown → GENERAL_DERMATOLOGY (with warning)</li>
     * </ul>
     * 
     * @param categoryLevel1 the category from JSON
     * @return mapped Specialty enum value
     * @throws IllegalArgumentException if categoryLevel1 is null
     */
    private Specialty mapSpecialty(String categoryLevel1) {
        if (categoryLevel1 == null) {
            throw new IllegalArgumentException("Category Level 1 is required for specialty mapping");
        }

        return switch (categoryLevel1.trim()) {
            case "Medical" -> Specialty.MEDICAL_DERMATOLOGY;
            case "Estetica" -> Specialty.COSMETIC_DERMATOLOGY;
            default -> {
                log.warn("Unknown specialty category '{}', defaulting to GENERAL_DERMATOLOGY", categoryLevel1);
                yield Specialty.GENERAL_DERMATOLOGY;
            }
        };
    }

    /**
     * Imports consultation types from consultations.json file.
     * 
     * <p>This method:</p>
     * <ul>
     *   <li>Reads consultations.json from project root using Jackson ObjectMapper</li>
     *   <li>Parses JSON array into ConsultationJsonEntry[] records</li>
     *   <li>Filters out invalid entries (entries where isValid() == false)</li>
     *   <li>Transforms each entry to consultation type using helper methods</li>
     *   <li>Creates consultations via createConsultationTypeWithHierarchy()</li>
     *   <li>Tracks and logs creation, skipping, and error statistics</li>
     * </ul>
     * 
     * <p>This method is idempotent - existing consultations are skipped and safe to run multiple times.</p>
     * 
     * <p>Error Handling:</p>
     * <ul>
     *   <li>File not found → log error, return early (non-fatal)</li>
     *   <li>JSON parse errors → catch, log, continue</li>
     *   <li>Entry processing errors → catch, count, continue</li>
     *   <li>Service errors → already handled by createConsultationTypeWithHierarchy</li>
     * </ul>
     */
    private void importConsultationTypesFromJson() {
        log.info("Starting consultation types import from consultations.json...");

        try {
            // Step 1: Read JSON file from project root
            ObjectMapper objectMapper = new ObjectMapper();
            File jsonFile = new File("consultations.json");

            if (!jsonFile.exists()) {
                log.error("consultations.json not found at project root, skipping import");
                return;
            }

            // Step 2: Parse JSON array into records
            ConsultationJsonEntry[] entries = objectMapper.readValue(
                jsonFile,
                ConsultationJsonEntry[].class
            );

            log.info("Loaded {} entries from consultations.json", entries.length);

            // Step 3: Process entries with statistics tracking
            int created = 0;
            int skipped = 0;
            int errors = 0;

            for (ConsultationJsonEntry entry : entries) {
                // Skip invalid entries (where essential fields are null)
                if (!entry.isValid()) {
                    continue;
                }

                try {
                    // Transform JSON data to consultation parameters
                    String name = buildConsultationName(entry);
                    Specialty specialty = mapSpecialty(entry.categoryLevel1());
                    BigDecimal price = parsePrice(entry.priceRaw());

                    // Create consultation using the hierarchy method
                    ConsultationTypeDto result = createConsultationTypeWithHierarchy(
                        name,                              // name
                        specialty,                         // specialty
                        price,                             // price
                        "RON",                             // priceCurrency
                        30,                                // durationMinutes (default)
                        false,                             // requiresSurgeryRoom (default)
                        entry.workflowStep(),              // workflowStep
                        entry.categoryLevel1(),            // categoryLevel1
                        entry.categoryLevel2(),            // categoryLevel2
                        entry.subcategoryLevel1(),         // subcategoryLevel1
                        entry.subcategoryLevel2(),         // subcategoryLevel2
                        Collections.emptyList()            // requiredFormTemplateIds (empty for now)
                    );

                    if (result != null) {
                        created++;
                    } else {
                        skipped++;  // Already existed
                    }

                } catch (Exception e) {
                    errors++;
                    log.error("Failed to import consultation from entry: {}", entry, e);
                }
            }

            log.info("Consultation import completed: {} created, {} skipped, {} errors",
                created, skipped, errors);

        } catch (Exception e) {
            log.error("Failed to import consultations from JSON: {}", e.getMessage(), e);
        }
    }
}
