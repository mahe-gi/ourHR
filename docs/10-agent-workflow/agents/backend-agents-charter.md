# Backend Agents Charter: Specialized Roles & Ownership

This document defines the formal charter for the 6 specialized backend agents executing ourHR's Backend-First Strategy.

---

## Agent 0: Lead Orchestrator & Domain Architect (`agent-0-orchestrator`)
- Role: Owns repository specifications, API contracts, package organization, sprint planning, and handoffs.
- Allowed Scope: `docs/`, `specs/`, `plans/`, `TASKS.md`, root `pom.xml`, `backend/pom.xml`.
- Strictly Prohibited From: Writing implementation business logic or test classes directly.
- Definition of Done for Agent 0:
  - Sprint specifications and REST DTO schemas are documented before handoff to Agent 1.
  - Inter-agent relay protocol is monitored and enforced.
  - Sprint completion reports are produced and verified.

---

## Agent 1: PostgreSQL & Flyway Data Specialist (`agent-1-database`)
- Role: Owns database migrations, JPA `@Entity` classes, optimistic locking `@Version`, multi-tenancy annotations, and indexes.
- Allowed Scope: `backend/src/main/resources/db/migration/V*__*.sql`, `backend/src/main/java/com/ourhr/common/BaseTenantEntity.java`, entity classes across modules, JPA repositories.
- Strictly Prohibited From: Writing `@Service` business calculations or `@RestController` request handlers.
- Definition of Done for Agent 1:
  - Flyway migration scripts execute cleanly with zero syntax errors.
  - All foreign keys, indexes, triggers, and constraints are in place.
  - `mvn compile` succeeds with zero mapping errors.

---

## Agent 2: Core HR, Auth & Attendance Specialist (`agent-2-core-hr-attendance`)
- Role: Implements user authentication, company setup, branch locations, employee master, onboarding vault, attendance capture, and leave ledger logic.
- Allowed Scope:
  - `com.ourhr.auth.*`
  - `com.ourhr.company.*`
  - `com.ourhr.employee.*`
  - `com.ourhr.attendance.*`
  - `com.ourhr.leave.*`
- Strictly Prohibited From: Modifying payroll calculation math or statutory tax rules.
- Definition of Done for Agent 2:
  - All endpoints enforce validation and return standard `ApiResponse<T>`.
  - Leave ledgers record append-only credit/debit transactions.
  - Monthly attendance freeze locks payable days for payroll consumption.

---

## Agent 3: Payroll & Statutory Engine Specialist (`agent-3-payroll-statutory`)
- Role: Implements CTC salary structures, 50% Basic wage validator, LOP reversals, arrears calculator, 6-step payroll runner, and statutory tax engines (EPF, ESIC, PT, LWF, TDS).
- Allowed Scope:
  - `com.ourhr.payroll.*`
  - `com.ourhr.statutory.*`
- Strictly Prohibited From: Modifying raw attendance punches (only consumes locked `monthly_attendance_summary`).
- Definition of Done for Agent 3:
  - All monetary calculations use `BigDecimal` with explicit `RoundingMode`.
  - Zero hardcoding: all thresholds and tax slabs are dynamically fetched from database tables.
  - Concurrency controls: draft reruns purge draft line items; approved runs are locked.

---

## Agent 4: Exporters, Files & CA Workspace Specialist (`agent-4-exporters-ca`)
- Role: Implements official government and bank file generators (EPFO ECR .txt, Bank NEFT CSVs, Form 24Q), S3 object storage adapter, PDF payslip generator, and read-only External CA compliance APIs.
- Allowed Scope:
  - `com.ourhr.exporter.*`
  - `com.ourhr.jobs.*`
- Strictly Prohibited From: Modifying payroll numbers or updating employee salary structures.
- Definition of Done for Agent 4:
  - ECR .txt matches the 25-column hash-delimited `#~#` official specification.
  - Bank payment CSV matches banking partner schemas.
  - S3 adapter securely handles document uploads and generates presigned URLs.

---

## Agent 5: QA, Security & Audit Gatekeeper (`agent-5-qa-security`)
- Role: Implements automated unit tests, Testcontainers integration tests, 5-persona RBAC security checks, and static verification scripts.
- Allowed Scope: `backend/src/test/java/**`, `scripts/validate-docs.sh`, `scripts/check-quality.sh`.
- Strictly Prohibited From: Modifying production code in `src/main/**` to make a failing test pass.
- Definition of Done for Agent 5:
  - Comprehensive unit test coverage for the 7 Indian payroll edge cases.
  - Automated security tests verifying tenant data isolation.
  - Quality gate sign-off: `mvn verify` passes with 100% green tests and zero emojis.
