# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**PoliclinaBine** is a Spring Boot 4.0.0-RC1 clinic management system for handling:
- Patient registration and medical records
- Doctor profiles and specialties
- Appointment scheduling and session management
- Medical consultations with questionnaires
- Billing, invoicing, and payment tracking
- Medical file access control

**Technology Stack:**
- Java 25
- Spring Boot 4.0.0-RC1
- Spring Data JPA for data persistence
- PostgreSQL (production) / H2 (development)
- Lombok 1.18.38 for boilerplate reduction
- Maven for build management

## Common Commands

### Build and Run
```bash
# Clean and build the project
mvn clean install

# Compile only (faster for development)
mvn compile

# Run the application
mvn spring-boot:run

# Package as JAR
mvn clean package
```

### Testing
```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName
```

### IntelliJ-Specific Commands
```bash
# Using Maven wrapper with IntelliJ integration
cmd.exe /c "mvnw.cmd clean compile"
```

## Architecture

### Layered Architecture Pattern

The application follows a **clean layered architecture** with strict separation of concerns:

```
┌─────────────────────────────────────┐
│      Controller Layer (Future)      │  ← REST endpoints
├─────────────────────────────────────┤
│         Service Layer               │  ← Business logic, transactions
├─────────────────────────────────────┤
│       Repository Layer              │  ← Data access (Spring Data JPA)
├─────────────────────────────────────┤
│        Entity Layer                 │  ← Domain model (JPA entities)
└─────────────────────────────────────┘
```

### Package Structure

**Base package:** `com.example.policlicabine`

- **`entity/`** - JPA entities representing domain model
  - All entities use `@Getter/@Setter` (not `@Data`) for JPA compatibility
  - UUID primary keys with `@PrePersist` generation
  - BigDecimal for all monetary amounts
  - Proper `equals()`/`hashCode()` based on ID only
  - Bidirectional relationships with helper methods (e.g., `addQuestion()`, `removeQuestion()`)
  - `@BatchSize` annotations to prevent N+1 queries

- **`repository/`** - Spring Data JPA repositories
  - Extend `JpaRepository<Entity, UUID>`
  - Custom query methods following Spring Data naming conventions
  - Complex queries use `@Query` annotations

- **`service/`** - Business logic layer (11 services total)
  - All methods return `Result<T>` (custom result type from `common/`)
  - `@Transactional` for write operations
  - `@Transactional(readOnly = true)` for queries
  - Constructor-based dependency injection
  - Each service manages ONLY its own repository (single responsibility)
  - Services communicate through service-to-service calls (no direct repository sharing)
  - Defensive validation on all inputs
  - Domain events published via `ApplicationEventPublisher`
  - **Core Services**: PatientService, DoctorService, UserService, ConsultationService, DiagnosisService
  - **Medical Services**: QuestionService, AnswerService, AppointmentSessionService
  - **Billing Services**: BillingService, InvoiceService, PaymentService
  - **Access Control**: MedicalFileAccessService

- **`service/base/`** - Base service infrastructure
  - `BaseService<E, D, ID>` - Generic interface for complete CRUD operations
  - `BaseServiceImpl<E, D, ID>` - Abstract class with reusable implementations
  - `ServiceHelper` - Utility class for validation patterns
  - Eliminates ~150-200 lines of duplicate CRUD code across services
  - All services can optionally extend base implementation
  - Full CRUD support: Create (business-specific), Read, Update, Delete

- **`dto/`** - Data Transfer Objects
  - Immutable DTOs using `@Data` and `@Builder`
  - Used for service layer responses
  - Prevent entity exposure to upper layers

- **`mapper/`** - Entity ↔ DTO converters
  - MapStruct 1.6.3 compile-time mappers for type-safe conversions
  - `@Mapper(componentModel = "spring")` for dependency injection
  - Handle null safety and relationship navigation automatically
  - Custom default methods for complex mappings (e.g., collection ID extraction)
  - `@Mapping(target = "...", ignore = true)` for bidirectional relationships in `toEntity()` methods

- **`event/`** - Domain events for event-driven architecture
  - All events implemented as Java records (immutable, concise, zero dependencies)
  - Modern Spring Framework pattern (POJO events, no need to extend ApplicationEvent)
  - Published after successful operations via `ApplicationEventPublisher`
  - Enable asynchronous processing and decoupling
  - Transaction-safe: events only delivered if transaction commits

- **`common/`** - Shared utilities
  - `Result<T>` - Consistent error handling pattern across all services

### Key Domain Entities

**Core Entities:**
- `Patient` - Patient demographics and consent
- `Doctor` - Doctor profiles linked to `User` accounts
- `User` - System users (doctors, admin, receptionist)
- `AppointmentSession` - Scheduled medical appointments
- `Consultation` - Types of medical services offered
- `Question` - Medical history questions per consultation
- `Answer` - Patient answers during sessions

**Billing Entities:**
- `SessionBilling` - Billing records for sessions
- `BillingDiscount` - Applied discounts with audit trail
- `Invoice` - Invoices (can be proforma or final)
- `Payment` - Payment records

**Supporting Entities:**
- `Diagnosis` - ICD-10 diagnosis codes
- `WeeklyAvailability` - Doctor schedules

### Entity Dependency Hierarchy & Aggregate Roots

The system follows a **strict hierarchical entity model** with clear aggregate roots and downward-only DTO navigation:

#### Level 0: Foundation Entities (No Dependencies)
```
User (base user)          Diagnosis (ICD-10 reference data)
```

#### Level 1: Primary Domain Entities
```
Patient                   Doctor → User              Consultation (service catalog)
```

#### Level 2: Child Entities
```
Question → Consultation          WeeklyAvailability → Doctor
```

#### Level 3: **PRIMARY AGGREGATE ROOT** ⭐
```
AppointmentSession (MAIN AGGREGATE ROOT)
  ├─→ Patient (ManyToOne)
  ├─→ Doctor (ManyToOne)
  ├─→ Consultations[] (ManyToMany)
  ├─→ Diagnoses[] (ManyToMany)
  └─→ Answers[] (OneToMany, cascade ALL, orphanRemoval)
```

#### Level 4: Session Details
```
Answer
  ├─→ AppointmentSession (ManyToOne)
  ├─→ Question (ManyToOne)
  └─→ Consultation (ManyToOne)
```

#### Level 5: Billing Aggregate
```
SessionBilling (AGGREGATE ROOT)
  ├─→ AppointmentSession (OneToOne)
  └─→ BillingDiscounts[] (OneToMany, cascade ALL, orphanRemoval)

BillingDiscount
  ├─→ SessionBilling (ManyToOne)
  └─→ User/appliedBy (ManyToOne)
```

#### Level 6: Invoice Aggregate
```
Invoice (AGGREGATE ROOT)
  ├─→ User/generatedBy (ManyToOne)
  ├─→ SessionBillings[] (ManyToMany)
  └─⟷ Payments[] (ManyToMany inverse - CIRCULAR!)
```

#### Level 7: Payment Aggregate
```
Payment (AGGREGATE ROOT)
  ├─→ Invoices[] (ManyToMany)
  └─→ User/generatedBy (ManyToOne)
```

### DTO Hierarchy Rules - ALWAYS GO DOWN

**✅ CORRECT: Parent → Child (Nested DTOs)**
```
AppointmentSessionDto:
  ├─→ PatientDto
  ├─→ DoctorDto
  │     ├─→ UserDto
  │     └─→ WeeklyAvailabilityDto[]
  ├─→ ConsultationDto[]
  │     └─→ QuestionDto[]
  ├─→ DiagnosisDto[]
  └─→ AnswerDto[]

InvoiceDto:
  ├─→ UserDto (generatedBy)
  └─→ SessionBillingDto[]
        └─→ BillingDiscountDto[]

PaymentDto:
  ├─→ UserDto (generatedBy)
  └─→ invoiceIds[] (UUIDs only to avoid circular!)
```

**❌ WRONG: Child → Parent (Would cause infinite loops)**
```
❌ PatientDto → AppointmentSessionDto[] (loads all patient history)
❌ ConsultationDto → AppointmentSessionDto[] (loads all sessions using consultation)
❌ QuestionDto → ConsultationDto → AppointmentSessionDto[] (infinite recursion)
❌ SessionBillingDto → InvoiceDto[] (circular billing graph)
```

### EntityGraph Strategy for Aggregate Roots

**Purpose:** Prevent N+1 queries by eagerly fetching relationships in a single query using `@EntityGraph`.

#### 1. AppointmentSession (MAIN ROOT) - Full DTO Mapping
```java
// Full load for DTO mapping (goes DOWN the hierarchy)
@EntityGraph(attributePaths = {
    "patient", "doctor", "consultations", "diagnoses", "answers"
})
Optional<AppointmentSession> findWithAllRelationshipsById(UUID id);

// Basic load for events/validation
@EntityGraph(attributePaths = {"patient", "doctor"})
Optional<AppointmentSession> findWithBasicRelationshipsById(UUID id);

// Specific load for question validation
@EntityGraph(attributePaths = {"consultations"})
Optional<AppointmentSession> findWithConsultationsById(UUID id);
```

#### 2. Doctor - Includes nested relationships
```java
@EntityGraph(attributePaths = {"user", "weeklyAvailability"})
Optional<Doctor> findWithUserAndAvailabilityById(UUID id);
```

#### 3. Consultation - Includes questions
```java
@EntityGraph(attributePaths = {"questions"})
Optional<Consultation> findWithQuestionsById(UUID id);

@EntityGraph(attributePaths = {"questions"})
List<Consultation> findWithQuestionsByNameInAndIsActiveTrue(List<String> names);
```

#### 4. Invoice - Includes billing data
```java
@EntityGraph(attributePaths = {"generatedBy", "sessionBillings"})
Optional<Invoice> findWithSessionBillingsById(UUID id);

@EntityGraph(attributePaths = {"generatedBy", "sessionBillings", "payments"})
Optional<Invoice> findWithSessionBillingsAndPaymentsById(UUID id);
```

#### 5. Payment - Includes invoices and billings
```java
@EntityGraph(attributePaths = {"invoices", "generatedBy"})
Optional<Payment> findWithInvoicesById(UUID id);

@EntityGraph(attributePaths = {"invoices", "invoices.sessionBillings", "generatedBy"})
Optional<Payment> findWithInvoicesAndBillingsById(UUID id);
```

#### 6. SessionBilling - Includes discounts
```java
@EntityGraph(attributePaths = {"session", "discounts", "discounts.appliedBy"})
Optional<SessionBilling> findWithSessionAndDiscountsById(UUID id);
```

### Entity Relationships Architecture

**Critical Design Patterns:**

1. **Bidirectional Relationships** - Always managed through helper methods:
   ```java
   consultation.addQuestion(question);  // Sets both sides
   consultation.removeQuestion(question); // Clears both sides
   ```

2. **Monetary Values** - Always use `BigDecimal`, never `Double`:
   ```java
   BigDecimal price = consultation.getPrice();
   BigDecimal total = amounts.stream()
       .reduce(BigDecimal.ZERO, BigDecimal::add);
   ```

3. **Answer Validation** - Answers must validate that the question belongs to one of the session's consultations.
   This validation is performed via service-to-service communication:
   ```java
   // In AnswerService
   Result<Void> validation = appointmentSessionService
       .validateQuestionBelongsToSession(sessionId, question.getConsultation().getConsultationId());

   // In AppointmentSessionService (internal method)
   @Transactional(readOnly = true)
   public Result<Void> validateQuestionBelongsToSession(UUID sessionId, UUID consultationId) {
       AppointmentSession session = appointmentRepository
           .findWithConsultationsById(sessionId).orElse(null);
       boolean valid = session.getConsultations().stream()
           .anyMatch(c -> c.getConsultationId().equals(consultationId));
       return valid ? Result.success(null) : Result.failure("Invalid question");
   }
   ```

4. **Lazy Loading with Batch Size** - Prevent N+1 queries:
   ```java
   @OneToMany(fetch = FetchType.LAZY)
   @BatchSize(size = 20)
   private List<Answer> answers;
   ```

### Service Layer Patterns

**Base Service Architecture:**

To eliminate code duplication and ensure consistency, all services extend a generic base service:

```
┌─────────────────────────────────────┐
│  BaseService<E, D, ID> Interface    │  ← Generic service contract
├─────────────────────────────────────┤
│  BaseServiceImpl<E, D, ID>          │  ← Reusable CRUD implementations
├─────────────────────────────────────┤
│  Concrete Services                  │  ← PatientService, ConsultationService, etc.
└─────────────────────────────────────┘
```

**Location:** `com.example.policlicabine.service.base`

**Files:**
- `BaseService.java` - Generic interface defining common CRUD operations
- `BaseServiceImpl.java` - Abstract class with reusable implementations
- `ServiceHelper.java` - Utility class for common validation patterns

**Common Methods (inherited by all services):**

1. **Public API Methods** - Return `Result<DTO>`:
   - `findById(ID id)` → `Result<D>` (Read operation)
   - `findAll()` → `Result<List<D>>` (Read operation)
   - `update(ID id, D dto)` → `Result<D>` (Update operation)
   - `deleteById(ID id)` → `Result<Void>` (Delete operation)

2. **Internal Methods** - For service-to-service communication:
   - `validateExists(ID id)` → `Result<Void>` (validation with error messages)
   - `getEntityById(ID id)` → `E` (direct entity access, returns null if not found)
   - `getEntitiesByIds(List<ID> ids)` → `List<E>` (batch entity retrieval)
   - `existsById(ID id)` → `boolean` (efficient existence check)

**Creating a New Service:**

```java
@Service
@Slf4j
@Transactional
public class PatientService extends BaseServiceImpl<Patient, PatientDto, UUID> {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    public PatientService(PatientRepository repository, PatientMapper mapper) {
        super(repository, mapper);
        this.patientRepository = repository;
        this.patientMapper = mapper;
    }

    // Required implementations
    @Override
    protected PatientDto toDto(Patient entity) {
        return patientMapper.toDto(entity);
    }

    @Override
    protected String getEntityName() {
        return "Patient";
    }

    @Override
    protected void updateEntityFromDto(Patient entity, PatientDto dto) {
        // Update only mutable fields
        if (dto.getFirstName() != null && !dto.getFirstName().trim().isEmpty()) {
            entity.setFirstName(dto.getFirstName().trim());
        }
        if (dto.getPhone() != null) {
            entity.setPhone(dto.getPhone().trim());
        }
        // ... other mutable fields
        // DON'T update: patientId, createdAt, or other immutable fields
    }

    // Business-specific methods
    public Result<PatientDto> registerNewPatient(...) {
        // Custom business logic
    }
}
```

**Benefits:**
- ✅ Eliminates ~150-200 lines of duplicate CRUD code across 7 services
- ✅ Complete CRUD operations: Create (business-specific), Read, Update, Delete
- ✅ Consistent API across all services
- ✅ Type-safe with generics
- ✅ Optional adoption (can extend or not)
- ✅ Focuses services on business logic, not boilerplate
- ✅ Template method pattern for customizable updates

**Services Using Base Architecture with Full CRUD (7 of 12 services):**

All 7 services have complete CRUD operations through base service:
- ✅ **PatientService** - Full CRUD, publishes PatientRegistered event
- ✅ **ConsultationService** - Full CRUD, manages active/inactive consultations
- ✅ **DiagnosisService** - Full CRUD for ICD-10 codes
- ✅ **UserService** - Full CRUD, username immutable after creation
- ✅ **QuestionService** - Full CRUD, custom EntityGraph method retained
- ✅ **InvoiceService** - Full CRUD, overrides `findById()` with EntityGraph
- ✅ **DoctorService** - Full CRUD, publishes DoctorProfileCreated event

Each service implements `updateEntityFromDto()` defining which fields are mutable.

**Services NOT Using Base Architecture (5 services):**
- ❌ MedicalFileAccessService - No repository, read-only, special access control logic
- ❌ PaymentService - Returns entities (not DTOs) in some methods
- ❌ BillingService - Returns entities (not DTOs), event-driven architecture
- ❌ AnswerService - Complex validation logic, no standard CRUD pattern
- ❌ AppointmentSessionService - Complex orchestrator with many dependencies

**Coverage: 58% of services** (7/12) benefit from base service architecture
**Code Savings: ~150-200 lines** of duplicate CRUD code eliminated

**Service-to-Service Communication Architecture:**

Our services follow a **pragmatic separation of concerns** where each service:
- Manages only its own repository (single responsibility)
- Calls other services for validation and entity access
- Uses Result pattern ONLY where it provides clear benefit
- Uses domain events for cross-service notifications

**Two types of service methods:**

1. **Public API methods** - Return `Result<DTO>` for external use (controllers):
   ```java
   @Transactional(readOnly = true)
   public Result<PatientDto> findPatientById(UUID patientId) {
       // Returns Result<DTO> for clear error handling
   }
   ```

2. **Internal methods** - Return entities directly for service-to-service use:
   ```java
   @Transactional(readOnly = true)
   public List<Consultation> getEntitiesByNames(List<String> names) {
       // Returns entities directly - simpler, no Result wrapper needed
       return consultationRepository.findByNameInAndIsActiveTrue(names);
   }
   ```

**Example: AppointmentSessionService using other services:**
```java
@Service
@RequiredArgsConstructor
public class AppointmentSessionService {
    // ONLY our repository
    private final AppointmentSessionRepository appointmentRepository;

    // Services for validation and entity access
    private final PatientService patientService;
    private final DoctorService doctorService;
    private final ConsultationService consultationService;

    // EntityManager for creating entity references without DB hits
    @PersistenceContext
    private EntityManager entityManager;

    public Result<AppointmentSessionDto> scheduleAppointment(...) {
        // Validate via services (Result for error messages)
        Result<Void> check = patientService.validatePatientExists(patientId);
        if (check.isFailure()) return Result.failure(check.getErrorMessage());

        // Get entities via services (direct return)
        List<Consultation> consultations = consultationService.getEntitiesByNames(names);

        // Use EntityManager.getReference() to avoid DB hits
        Patient patientRef = entityManager.getReference(Patient.class, patientId);

        // Create and save
        AppointmentSession session = AppointmentSession.builder()
            .patient(patientRef)
            .consultations(consultations)
            .build();
    }
}
```

**Example: AnswerService with complex validation:**
```java
@Service
@RequiredArgsConstructor
public class AnswerService {
    // ONLY our repository
    private final AnswerRepository answerRepository;

    // Services for validation and entity access
    private final AppointmentSessionService appointmentSessionService;
    private final QuestionService questionService;

    @PersistenceContext
    private EntityManager entityManager;

    public Result<AnswerDto> saveAnswer(UUID sessionId, UUID questionId, String answerText) {
        // Get question with consultation loaded (EntityGraph prevents N+1)
        Question question = questionService.getEntityWithConsultation(questionId);

        // Validate question belongs to session's consultations via service
        Result<Void> validation = appointmentSessionService
            .validateQuestionBelongsToSession(sessionId, question.getConsultation().getConsultationId());
        if (validation.isFailure()) return Result.failure(validation.getErrorMessage());

        // Use EntityManager.getReference() for session FK (no DB hit)
        AppointmentSession sessionRef = entityManager.getReference(AppointmentSession.class, sessionId);

        Answer answer = Answer.builder()
            .session(sessionRef)
            .question(question)
            .consultation(question.getConsultation())
            .answerText(answerText)
            .build();

        return Result.success(answerMapper.toDto(answerRepository.save(answer)));
    }
}
```

**EntityGraph for Performance:**

Repositories use `@EntityGraph` to prevent N+1 queries:
```java
@Repository
public interface AppointmentSessionRepository extends JpaRepository<AppointmentSession, UUID> {

    // Load session with all relationships in single query
    @EntityGraph(attributePaths = {"patient", "doctor", "consultations"})
    Optional<AppointmentSession> findWithAllRelationshipsById(UUID id);

    // Load only what you need
    @EntityGraph(attributePaths = {"consultations"})
    Optional<AppointmentSession> findWithConsultationsById(UUID id);
}
```

**When to use EntityGraph:**
- ✅ When loading entities for DTO mapping (prevents N+1)
- ✅ When accessing relationships for business logic
- ✅ For read-heavy operations with multiple relationships
- ❌ For simple CRUD where relationships aren't accessed

**Result Pattern Usage:**
Use Result when:
- ✅ Public API methods returning DTOs
- ✅ Validation methods that need clear error messages
- ❌ Internal entity retrieval (use Optional or direct return)
- ❌ Wrapping every method call (over-engineering)

**Transaction Management:**
- Write operations: `@Transactional` (default propagation)
- Read operations: `@Transactional(readOnly = true)` for performance
- Services manage their own transactional boundaries

**Domain Events:**
Services publish events for cross-service communication:
- All 27 events implemented as **Java records** (immutable, zero dependencies)
- Modern Spring 4.2+ pattern: POJO events, no need to extend ApplicationEvent
- Events published AFTER successful save, BEFORE returning Result.success()
- NO events on failure paths (Result.failure() never publishes events)
- Events are synchronous by default (same transaction)
- Use `@Async` on event listeners only when truly needed

**Complete Event Catalog (27 events):**
- **Patient**: `PatientRegistered`, `PatientPersonalInfoUpdated`, `PatientConsentStatusChanged`
- **Doctor**: `DoctorProfileCreated`
- **User**: `UserCreated`
- **Consultation**: `ConsultationCreated`, `ConsultationPriceUpdated`, `ConsultationDeactivated`, `ConsultationActivated`, `ConsultationTypeAdded`
- **Diagnosis**: `DiagnosisCreated`
- **Question**: `QuestionCreated`, `QuestionUpdated`, `QuestionDeleted`
- **Answer**: `AnswerSaved`, `AnswerUpdated`, `AnswerDeleted`
- **Appointment**: `AppointmentScheduled`, `AppointmentCancelled`, `SessionStarted`, `SessionCompleted`, `SessionDocumentationCompleted`
- **Billing**: `SessionBillingCalculated`, `ManualDiscountApplied`
- **Invoice**: `InvoiceCreated`, `InvoiceConvertedToFinal`
- **Payment**: `PaymentProcessed`

**Event Publishing Pattern:**
```java
try {
    Entity saved = repository.save(entity);
    eventPublisher.publishEvent(new DomainEvent(...));
    return Result.success(dto);
} catch (Exception e) {
    return Result.failure("Failed: " + e.getMessage());
}
```

### Data Access Patterns

**Repository Responsibilities:**
- Each repository manages ONLY its own entity
- Repositories are injected ONLY into their corresponding service
- Cross-entity operations happen through service-to-service calls

**Complete Service-Repository Mapping:**

| Service | Manages Entity | Repository Used | Services Called |
|---------|---------------|-----------------|-----------------|
| `PatientService` | Patient | `PatientRepository` | None |
| `DoctorService` | Doctor | `DoctorRepository` | `UserService`, `ConsultationService` |
| `UserService` | User | `UserRepository` | None |
| `ConsultationService` | Consultation | `ConsultationRepository` | None |
| `DiagnosisService` | Diagnosis | `DiagnosisRepository` | None |
| `QuestionService` | Question | `QuestionRepository` | `ConsultationService` |
| `AnswerService` | Answer | `AnswerRepository` | `QuestionService`, `AppointmentSessionService` |
| `AppointmentSessionService` | AppointmentSession | `AppointmentSessionRepository` | `PatientService`, `DoctorService`, `ConsultationService`, `DiagnosisService` |
| `BillingService` | SessionBilling | `SessionBillingRepository` | `AppointmentSessionService`, `UserService` |
| `InvoiceService` | Invoice | `InvoiceRepository` | `UserService` |
| `PaymentService` | Payment | `PaymentRepository` | `InvoiceService`, `UserService` |
| `MedicalFileAccessService` | N/A (Access Control) | None | `AppointmentSessionService` |

**Key Examples:**
- `AnswerService` uses only `AnswerRepository`, calls `QuestionService` and `AppointmentSessionService` for validation
- `QuestionService` uses only `QuestionRepository`, calls `ConsultationService` for entity access
- `PaymentService` uses only `PaymentRepository`, calls `InvoiceService` and `UserService` for entity access
- `MedicalFileAccessService` is special - it's a read-only access control service with no repository

**Repository Query Methods:**
- Derived queries: `findByPatientPatientIdOrderByScheduledDateTimeDesc`
- Custom queries: `@Query("SELECT ...")`
- Existence checks: `existsByPhone(String phone)` for validation
- EntityGraph queries: `@EntityGraph(attributePaths = {...})` for performance

**EntityGraph Query Examples:**
```java
// Basic relationships for validation/events
@EntityGraph(attributePaths = {"patient", "doctor"})
Optional<AppointmentSession> findWithBasicRelationshipsById(UUID id);

// All relationships for DTO mapping
@EntityGraph(attributePaths = {"patient", "doctor", "consultations", "diagnoses"})
Optional<AppointmentSession> findWithAllRelationshipsById(UUID id);

// Specific relationships as needed
@EntityGraph(attributePaths = {"consultations"})
Optional<AppointmentSession> findWithConsultationsById(UUID id);
```

**Performance Tips:**
- Use `EntityManager.getReference(Entity.class, id)` for setting FKs (no DB hit)
- Use `@EntityGraph` when loading entities with relationships (prevents N+1)
- Use `existsById()` for validation (cheaper than `findById()`)
- Use `@BatchSize` on collections as last resort if EntityGraph isn't suitable

**⚠️ WARNING: Circular Navigation in Billing Entities**

The billing domain contains **circular relationships** that can cause performance issues if not handled carefully. See "Entity Dependency Hierarchy" above for the complete hierarchy (Levels 5-7).

**Circular Chain:**
```
Payment ↔ Invoice ↔ SessionBilling ↔ AppointmentSession
```

**DTO Strategy:** PaymentDto uses `List<UUID> invoiceIds` (not nested InvoiceDto[]) to break the circular reference.

**Problematic Entity Methods:**
1. `Payment.getPatients()` - Navigates: Payment → Invoice → SessionBilling → Session → Patient
2. `Payment.getSessions()` - Navigates: Payment → Invoice → SessionBilling → Session
3. `SessionBilling.getPaymentStatus()` - Navigates: SessionBilling → Invoice → Payment (circular!)
4. `Invoice.getPaymentStatus()` - Navigates: Invoice → Payment AND Invoice → SessionBilling

**Critical Rules:**
- ✅ **ALWAYS use EntityGraph** when loading Invoice, Payment, or SessionBilling for complex operations
- ✅ **Use InvoiceService.getEntitiesWithBillings()** - loads invoices with sessionBillings via EntityGraph
- ✅ **Use PaymentRepository.findWithInvoicesAndBillingsById()** - loads payment with all needed data
- ❌ **NEVER call Payment.getPatients() or Payment.getSessions() without EntityGraph** - triggers deep lazy loading
- ❌ **NEVER navigate SessionBilling → Invoice → Payment → Invoice** - circular navigation risk

**Recommended Loading Strategies:**
```java
// ✅ GOOD: Load payment with invoices and billings upfront
Payment payment = paymentRepository.findWithInvoicesAndBillingsById(id).orElse(null);
List<Patient> patients = payment.getPatients(); // Safe, already loaded

// ✅ GOOD: Get invoices with billings via service
List<Invoice> invoices = invoiceService.getEntitiesWithBillings(invoiceIds);
BigDecimal total = invoices.stream()
    .map(Invoice::getTotalAmount) // Safe, sessionBillings already loaded
    .reduce(BigDecimal.ZERO, BigDecimal::add);

// ❌ BAD: Load payment without relationships
Payment payment = paymentRepository.findById(id).orElse(null);
List<Patient> patients = payment.getPatients(); // TRIGGERS N+1 QUERIES!

// ❌ BAD: Load invoice without sessionBillings
Invoice invoice = invoiceRepository.findById(id).orElse(null);
BigDecimal total = invoice.getTotalAmount(); // TRIGGERS LAZY LOAD!
```

**Service Architecture for Billing:**
- `PaymentService` uses only `PaymentRepository`, calls `InvoiceService` and `UserService`
- `InvoiceService` uses only `InvoiceRepository`, calls `UserService`
- `BillingService` uses only `SessionBillingRepository`, calls `AppointmentSessionService` and `UserService`

**Medical File Access Control:**
Doctors can access patient records if they have appointments within 30 days window. Access is automatically managed by `MedicalFileAccessService`.

## Entity Design Guidelines

### Required Patterns for New Entities

1. **Use `@Getter/@Setter`** instead of `@Data` (JPA compatibility)
2. **UUID primary keys** with `@PrePersist` auto-generation
3. **Override equals/hashCode** based on ID only:
   ```java
   @Override
   public boolean equals(Object o) {
       if (this == o) return true;
       if (!(o instanceof Entity)) return false;
       Entity that = (Entity) o;
       return id != null && Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
       return getClass().hashCode();
   }
   ```
4. **Custom toString()** excluding lazy-loaded relationships
5. **BigDecimal for money** with `@Column(precision = 10, scale = 2)`
6. **LocalDateTime/LocalDate/LocalTime** for temporal fields
7. **Enums** stored as STRING: `@Enumerated(EnumType.STRING)`

### Event Design Guidelines

**All domain events MUST be implemented as Java records:**

```java
package com.example.policlicabine.event;

import java.util.UUID;

public record EntityCreated(
    UUID entityId,
    String name,
    // ... other immutable fields
) {}
```

**Event Design Rules:**
1. **Use Java records** (not classes with Lombok)
2. **All fields immutable** (records are final by default)
3. **Package-level visibility** (`public record`)
4. **Past tense naming** (`PatientRegistered`, not `PatientRegister`)
5. **Include entity ID** as first field for traceability
6. **Include relevant context** (IDs, names, changed values)
7. **NO business logic** in events (pure data carriers)
8. **NO references to entities** (use IDs instead)

**MapStruct Mapper Guidelines:**

When mapping entities with bidirectional relationships, always ignore the inverse side in `toEntity()`:

```java
@Mapper(componentModel = "spring")
public interface PatientMapper {

    PatientDto toDto(Patient patient);

    @Mapping(target = "appointments", ignore = true)  // Ignore bidirectional collection
    Patient toEntity(PatientDto dto);
}
```

**Common bidirectional fields to ignore:**
- `Patient.appointments` (OneToMany to AppointmentSession)
- `Diagnosis.sessions` (ManyToMany to AppointmentSession)
- `Consultation.questions` (OneToMany to Question)
- `Doctor.weeklyAvailability` (OneToMany to WeeklyAvailability)

### Service Implementation Guidelines

1. **Constructor injection** via `@RequiredArgsConstructor`
2. **Defensive validation** on all method parameters
3. **Result pattern** for all return types
4. **Slf4j logging** for operations and errors
5. **Domain events** for significant business actions (published AFTER save, BEFORE Result.success())
6. **Read-only transactions** for query methods
7. **ApplicationEventPublisher** injection for publishing events

## Configuration

- **Main config:** `src/main/resources/application.properties`
- **Application name:** `policlicaBine`
- **Database:** PostgreSQL in pom.xml (H2 for in-memory development)
- **Lombok:** Configured as annotation processor in Maven compiler plugin

## Development Notes

- **Spring Boot version:** 4.0.0-RC1 requires Spring Snapshots repository
- **Java 25** is the target version
- **Lombok version:** 1.18.38 configured as annotation processor
- **Maven wrapper** available for consistent build environment