# Antigravity Backend Rule: Spring Boot Standards

1. Architecture: Modular Monolith with Spring Boot 3, Spring MVC, and Spring Data JPA.
2. Multi-Tenancy: Annotate tenant entities with Hibernate @TenantId and bind company_id.
3. Migrations: All database schema modifications must be implemented through versioned Flyway scripts in migrations/.
4. Error Handling: Return standard envelope: {"success": false, "error": {"code": "...", "message": "...", "details": []}}.
5. Validation: Use Jakarta Bean Validation on all DTOs and request payloads.
