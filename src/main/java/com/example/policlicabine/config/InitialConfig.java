package com.example.policlicabine.config;

import com.example.policlicabine.dto.*;
import com.example.policlicabine.entity.*;
import com.example.policlicabine.entity.enums.PermissionEnum;
import com.example.policlicabine.entity.enums.Specialty;
import com.example.policlicabine.entity.enums.UserRole;
import com.example.policlicabine.model.FieldOption;
import com.example.policlicabine.model.FormField;
import com.example.policlicabine.model.FormSection;
import com.example.policlicabine.model.FormStructure;
import com.example.policlicabine.repository.PermissionRepository;
import com.example.policlicabine.repository.RoleRepository;
import com.example.policlicabine.service.AuthenticationService;
import com.example.policlicabine.service.ConsultationService;
import com.example.policlicabine.service.FormSubmissionService;
import com.example.policlicabine.service.FormTemplateService;
import com.example.policlicabine.service.ManagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

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
    private final ManagerService managerService;
    private final AuthenticationService authenticationService;
    private final PasswordEncoder passwordEncoder;
    // Keeping direct repository access for Permission and Role per architectural decision
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Bean
    CommandLineRunner commandLineRunner() {
        return args -> {
//            if (Boolean.TRUE) return;
            createGdprConsentFormTemplate();

//             Initialize permissions and roles FIRST
            initializePermissionAndRoles();

//             Initialize default consultation types
            initializeDefaultConsultationTypes();

//             Create consultation with GDPR form requirement
            createConsultationWithGdprForm();

//             Create and link General Dermatology Visited Form
            createGeneralDermatologyVisitedForm();

//             Create default manager if no managers exist
            createDefaultManagerIfNeeded();
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
            .version("2.0")
            .title("Formular de Consimțământ GDPR")
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
                    .name("signature")
                    .type("signature")
                    .label("Semnătură")
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
     *
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
}
