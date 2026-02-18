# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

## Project Overview

**PoliclinaBine** is a Spring Boot 4.0.0 clinic management system handling patient registration, doctor profiles, appointment scheduling, medical consultations, and billing/invoicing.

## Technology Stack

- **Java 21 LTS** with virtual threads enabled
- **Spring Boot 4.0.0 GA** with Spring Data JPA
- **PostgreSQL** (production) / **H2** (development)
- **Lombok 1.18.38** for boilerplate reduction
- **MapStruct 1.6.3** for entity ↔ DTO mapping
- **Bucket4j 8.7.0** for rate limiting
- **Flyway** for database migrations
- **Maven** for build management

## Quick Navigation

Detailed documentation is organized into focused files:

@.claude/docs/architecture.md
@.claude/docs/service-patterns.md
@.claude/docs/entity-guidelines.md
@.claude/docs/data-access.md

## Common Commands

```bash
# Build and run
mvn clean install          # Full build
mvn compile                # Quick compile
mvn spring-boot:run        # Run application
mvn clean package          # Package JAR

# Testing
mvn test                   # All tests
mvn test -Dtest=ClassName  # Single class
mvn test -Dtest=ClassName#methodName  # Single method

# IntelliJ Maven wrapper
cmd.exe /c "mvnw.cmd clean compile"
```

## Core Principles

1. **Layered Architecture**: Controller → Service → Repository → Entity
2. **Single Responsibility**: Each service manages only its own repository
3. **Result Pattern**: All public service methods return `Result<DTO>`
4. **EntityGraph Required**: Always use `@EntityGraph` for relationship loading to prevent N+1
5. **DTOs Go Down**: Parent → Child nesting only; use IDs for circular references
6. **Events as Records**: Domain events are Java records, published after save
7. **BigDecimal for Money**: Never use Double for monetary values

## Documentation Maintenance Rules ⚠️

**CRITICAL: Keep documentation in sync with code changes!**

When modifying code, you MUST update the corresponding documentation:

| Code Change | Update Required |
|-------------|-----------------|
| New/modified entity | `.claude/docs/entity-guidelines.md` AND `.claude/docs/architecture.md` (if hierarchy changes) |
| New/modified service | `.claude/docs/service-patterns.md` |
| New/modified repository or EntityGraph | `.claude/docs/data-access.md` |
| New/modified controller or endpoint | `.claude/docs/entity-guidelines.md` (controller section) AND `../SYNC.md` (API contracts) |
| New domain event | `.claude/docs/service-patterns.md` (events catalog) |
| Architecture/pattern changes | `.claude/docs/architecture.md` |
| API contract changes | `../SYNC.md` (ALWAYS update for any endpoint change) |

**Sync Rules:**
1. After implementing a feature → Update all affected docs in the SAME commit
2. After adding an endpoint → Update `../SYNC.md` API Contracts section
3. After adding/changing a DTO → Update `../SYNC.md` Shared Types section
4. After adding a service method → Check if service patterns doc needs update

## Configuration

- **Main config:** `src/main/resources/application.properties`
- **Application name:** `policlicaBine`
- **Base package:** `com.example.policlicabine`

## Key Patterns Summary

| Pattern | Implementation |
|---------|---------------|
| Entity PK | UUID with `@PrePersist` generation |
| Entity annotations | `@Getter/@Setter` (not `@Data`) |
| Transactions | `@Transactional`, `readOnly=true` for queries |
| Validation | Defensive checks, `Result.failure()` for errors |
| Events | Java records, published via `ApplicationEventPublisher` |
| Controllers | `@StandardApiResponses` + `@Operation` on every endpoint |

## Cross-Project Sync

This is the **backend** of a full-stack application. The frontend lives at `../frontend`.

→ See `../SYNC.md` for API contracts and frontend coordination.

**Key rules:**
- Read `../SYNC.md` before creating/modifying any API endpoint
- Update `../SYNC.md` after implementing endpoints or changing DTOs
- Use `intellij` MCP tools for backend, `webstorm` MCP tools to browse frontend
