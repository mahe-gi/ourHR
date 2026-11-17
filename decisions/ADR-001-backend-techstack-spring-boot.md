# ADR-001: Java Spring Boot with PostgreSQL for Backend

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
ourHR requires exact statutory precision, multi-tenancy isolation, complex state machine execution for monthly payroll runs, and long-term enterprise maintainability.

## Decision
Adopt Java 17/21 with Spring Boot 3, Spring MVC, Spring Data JPA, Hibernate, and PostgreSQL.

## Rationale
1. Type Safety & Financial Precision: Java BigDecimal and strict typing eliminate rounding anomalies.
2. Mature Multi-Tenancy: Hibernate @TenantId provides declarative database-level tenant isolation.
3. Industry Standard: Proven enterprise ecosystem for payroll and accounting systems in India.
