# Antigravity Database Rule: PostgreSQL 16 & Flyway Standards

1. Sequential Flyway Ownership: Only `agent-1-database` may create or modify Flyway SQL files (`V1__`, `V2__`, etc.) in `src/main/resources/db/migration/`.
2. Multi-Tenancy Isolation: Every business entity table must have a `company_id UUID NOT NULL REFERENCES companies(id)`. At the JPA layer, entities must declare `@TenantId` for automatic query filtering.
3. Optimistic Locking: Mutable transactional tables (`companies`, `branches`, `employees`, `payroll_runs`, `monthly_attendance_summary`, `batch_jobs`, etc.) must have `version BIGINT NOT NULL DEFAULT 0` mapped with JPA `@Version`.
4. Immutable Payroll Trigger: Enforce `trg_prevent_locked_payslip_mutation` so `payslip_line_items` cannot be updated or deleted once `payroll_runs.status IN ('APPROVED', 'DISBURSED')`.
5. Schema Completeness: All 17 core functional tables specified in `docs/04-data/database-design.md` must be maintained without unauthorized column removals.
6. Zero Emojis: SQL comments, migration descriptions, and seed data must contain zero emojis.
