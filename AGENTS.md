# AGENTS.md

Authoritative Multi-Agent Engineering Standard
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Version: 2.0.0
Status: Approved

## 1. Project Identity and Operating Strategy
ourHR is an enterprise-grade Human Resource Management System and Payroll platform tailored specifically for Indian MSMEs and startups (15 to 250 employees). It couples the statutory accuracy of greytHR with the modern user experience of Keka HR, eliminating spreadsheet exchanges with external Chartered Accountants.

Operating Strategy: Strict Backend-First Execution. 100% of the Java Spring Boot backend, PostgreSQL schema, statutory rule engine, and RESTful APIs are developed, verified with automated test suites, and documented via OpenAPI 3 before any frontend client work begins.

## 2. The 6 Specialized Backend Agents

| Agent ID | Specialized Role | Exact Ownership (Files & Packages) | Strict Prohibitions |
|---|---|---|---|
| agent-0-orchestrator | Lead Orchestrator & Domain Architect | docs/, specs/, TASKS.md, pom.xml, API contracts, package structure (com.ourhr.*) | Never writes implementation business logic or test classes directly |
| agent-1-database | PostgreSQL & Flyway Data Specialist | src/main/resources/db/migration/V*__*.sql, JPA @Entity classes, @TenantId filters, Indexes, Audit listeners | Never writes @Service business logic or @RestController endpoints |
| agent-2-core-hr-attendance | Core HR, Auth & Attendance Specialist | com.ourhr.auth.*, com.ourhr.company.*, com.ourhr.employee.*, com.ourhr.attendance.*, com.ourhr.leave.* | Cannot modify Payroll calculation math or Statutory tax engines |
| agent-3-payroll-statutory | Payroll & Statutory Engine Specialist | com.ourhr.payroll.*, com.ourhr.statutory.* (EPF, ESIC, PT, LWF, TDS Old/New Regime, 50% Wage Code, Arrears) | Never modifies raw attendance punches; only consumes locked monthly_attendance_summary |
| agent-4-exporters-ca | Exporters, Files & CA Workspace Specialist | com.ourhr.exporter.* (EPFO ECR .txt, Bank CSVs, PDF Payslips, Form 24Q, S3 adapter, ROLE_CA APIs) | Read-only access to locked payroll runs; never mutates payroll figures |
| agent-5-qa-security | QA, Security & Audit Gatekeeper | src/test/java/** (JUnit 5, Mockito, Testcontainers PostgreSQL, Spring Security @PreAuthorize RBAC tests) | Never edits production code in src/main/** to make a failing test pass |

## 3. The 4-Step Relay Protocol
Within every backend sprint, agents collaborate through a sequential 4-step relay:

```
+-------------------------------------------------------------------------------+
| STEP 1: AGENT 0 (Architect)                                                   |
| Defines sprint spec, REST DTO contracts, and Indian statutory edge cases.     |
+---------------------------------------+---------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
| STEP 2: AGENT 1 (PostgreSQL & Flyway Specialist)                              |
| Writes Flyway SQL migrations (V1, V2...) + JPA entities + repositories.       |
| Gate Check: `mvn compile` & Flyway schema validation must pass before Step 3. |
+---------------------------------------+---------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
| STEP 3: PARALLEL DOMAIN CODING (Agents 2, 3, and 4)                           |
| - Agent 2: Core HR, Auth, Attendance, and Leave services & controllers.       |
| - Agent 3: Payroll calculation, arrears, and statutory tax engines.           |
| - Agent 4: ECR .txt, Bank CSV, PDF payslip generators & CA portal endpoints.  |
+---------------------------------------+---------------------------------------+
                                        |
                                        v
+-------------------------------------------------------------------------------+
| STEP 4: AGENT 5 (QA & Security Gatekeeper)                                    |
| Executes JUnit 5 + Testcontainers + 5-Persona RBAC checks + zero-hardcoding   |
| static scan. Marks sprint COMPLETED only when `mvn verify` is 100% green.     |
+-------------------------------------------------------------------------------+
```

## 4. The 7-Sprint Backend Build Order

1. Backend Sprint 1: Foundation, Multi-Tenancy & RBAC - Spring Boot 3.x setup, PostgreSQL + Flyway, JWT Auth, Tenant Isolation (company_id filter), 5-Persona Security (ROLE_FOUNDER, ROLE_HR, ROLE_FINANCE, ROLE_CA, ROLE_EMPLOYEE), batch_jobs queue, and audit_logs.
2. Backend Sprint 2: Company, Multi-State Branches & Zero-Hardcode Statutory DB - companies, branches (state_code), statutory_rules, pt_slabs, lwf_rules, and tds_tax_slabs seeded via Flyway migrations.
3. Backend Sprint 3: Employee Master, Onboarding & KYC Vault - employees, employee_kyc_vault (with strict masking, e.g., [Aadhaar Redacted]), S3 document adapter, 50% Basic Wage appointment letter generator, and Excel bulk import API.
4. Backend Sprint 4: Shifts, Attendance & Leave Ledger Engine - Web/Mobile geo-punch APIs, late-mark penalty processor, missed-punch regularization, leave_ledgers (credit/debit model + Sandwich rule), and Monthly Attendance Freeze job.
5. Backend Sprint 5: CTC Builder, Arrears & 6-Step Payroll Engine - Formula-driven salary_structures, 50% Wage Code validator, LOP reversal processor, backdated revision arrears calculator, 6-step payroll state machine (DRAFT to DISBURSED), and Section 194J/194C consultant payouts.
6. Backend Sprint 6: Indian Statutory Calculation Engine - Dynamic EPF/EPS splitter, ESIC eligibility and 6-month continuation rule, state-wise PT/LWF calculator (Strategy Pattern), and Old vs. New Tax Regime TDS projector (including Form 12B).
7. Backend Sprint 7: Exporters, PDF Engine & External CA API - EPFO ECR .txt generator, Bank NEFT/RTGS CSV exporters (HDFC, ICICI, SBI, Axis), asynchronous PDF payslip generator, Statutory Electronic Registers, and read-only CA compliance APIs.

## 5. Strict Backend Engineering & Domain Laws
Every backend agent must obey these 7 laws without exception:
1. Zero Hardcoding Law: Statutory rates, ceilings (INR 15,000 / 25,000 PF, INR 21,000 ESIC), tax slabs, and PT rules must be queried from effective-dated PostgreSQL tables (effective_from, effective_to), never hardcoded in Java constants.
2. BigDecimal Law: All monetary and tax calculations must use `java.math.BigDecimal` with explicit RoundingMode and `NUMERIC(12,2)` in PostgreSQL. `double` and `float` are strictly banned in financial code.
3. Flyway Sequential Ownership: Only `agent-1-database` may create or modify Flyway SQL files (V1__, V2__), preventing migration version collisions.
4. Modular Monolith Boundaries: Cross-module communication must happen through public Service interfaces or DTOs; never inject another module's JPA Repository directly.
5. Immutable Payroll Law: Once `payroll_runs.status = 'DISBURSED'`, `payslip_line_items` cannot be updated or deleted. Draft runs in status DRAFT or CALCULATED can recalculate by purging unapproved draft line items.
6. Sensitive ID Redaction Law: Government IDs must be masked and encrypted at rest. Never log or expose raw IDs in plain text; always use safe masked representations (such as [Aadhaar Redacted]) in test fixtures and logs.
7. Standard API Envelope Law: Every REST endpoint must return `ApiResponse<T>` (`{ "success": boolean, "data": T, "error": ErrorDetail, "timestamp": Instant }`) and enforce `@PreAuthorize` for respective roles.
8. Zero Emojis Law: No emojis permitted anywhere in the repository: source code, comments, documentation, UI strings, Git commit messages, or log statements. Use standard SVG icons (Lucide) for user interfaces.
