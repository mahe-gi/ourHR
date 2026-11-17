# TASKS.md: ourHR Backend-First Sprint Task Board

Unified Task Board covering Backend Sprints 1 through 7 across Agents 0 through 5.

---

## Backend Sprint 1: Foundation, Multi-Tenancy & RBAC
- [x] `TASK-B101` (agent-0): Define Sprint 1 spec, JWT claims model, and tenant context interface.
- [x] `TASK-B102` (agent-1): Write `V1__init_core_schema.sql` (17 tables, triggers, indexes, versioning).
- [x] `TASK-B103` (agent-1): Create `BaseTenantEntity.java` with `@TenantId` and `@Version`.
- [x] `TASK-B104` (agent-2): Configure Spring Security filter chain with JWT authentication and 5-Persona RBAC.
- [x] `TASK-B105` (agent-2): Implement `TenantContext` filter extracting `company_id` from JWT.
- [x] `TASK-B106` (agent-4): Implement `batch_jobs` entity and persistent job repository.
- [x] `TASK-B107` (agent-5): Write integration tests verifying cross-tenant isolation.
- [x] `TASK-B108` (agent-5): Write RBAC security tests verifying role enforcement.

## Backend Sprint 2: Company, Multi-State Branches & Statutory Database
- [x] `TASK-B201` (agent-0): Define DTO contracts for Company and Branch management.
- [x] `TASK-B202` (agent-1): Write `V2__seed_default_statutory_rules.sql` (2026 tax slabs, EPF/ESIC limits, state PT slabs).
- [x] `TASK-B203` (agent-1): Create JPA entities for `Company`, `Branch`, `StatutoryRule`, `PtSlab`, `LwfRule`, `TdsTaxSlab`.
- [x] `TASK-B204` (agent-2): Implement `CompanyService` and `BranchService` with state-specific lookups.
- [x] `TASK-B205` (agent-3): Implement `StatutoryRuleRepository` with effective-date range query methods.
- [x] `TASK-B206` (agent-5): Verify database seed data and write tests for effective-date rule resolution.

## Backend Sprint 3: Employee Master, Onboarding & KYC Vault
- [x] `TASK-B301` (agent-0): Define Employee Master, KYC Vault, and Form 12B REST DTO schemas.
- [x] `TASK-B302` (agent-1): Create JPA entities for `Employee`, `EmployeeKycVault`, and `PreviousEmployerIncome`.
- [x] `TASK-B303` (agent-2): Implement self-service onboarding magic link generation and submission.
- [x] `TASK-B304` (agent-2): Implement 50% Basic wage rule validator on salary offers.
- [x] `TASK-B305` (agent-4): Implement `FileStorageService` S3/MinIO adapter for KYC documents with masked ID storage.
- [x] `TASK-B306` (agent-5): Write unit tests for 50% Basic wage validator and Aadhaar masking enforcement.

## Backend Sprint 4: Shifts, Attendance & Leave Ledger Engine
- [x] `TASK-B401` (agent-0): Define Attendance punch and Leave application DTO contracts.
- [x] `TASK-B402` (agent-1): Create JPA entities for `Shift`, `AttendanceLog`, `MonthlyAttendanceSummary`, `LeaveType`, `LeaveApplication`, `LeaveLedger`.
- [x] `TASK-B403` (agent-2): Implement Web and Mobile GPS punch recording API.
- [x] `TASK-B404` (agent-2): Implement bulk CSV/Excel attendance upload (`BIOMETRIC_CSV`).
- [x] `TASK-B405` (agent-2): Implement immutable `LeaveLedgerService` (credit/debit transaction model and Sandwich rule).
- [x] `TASK-B406` (agent-2): Implement Monthly Attendance Freeze job with payable days and LOP calculation.
- [x] `TASK-B407` (agent-5): Write integration tests for Sandwich rule leave calculation and attendance locking.

## Backend Sprint 5: CTC Builder, Arrears & 6-Step Payroll Engine
- [x] `TASK-B501` (agent-0): Define 6-step payroll state machine and audit diff DTO schemas.
- [x] `TASK-B502` (agent-1): Create JPA entities for `SalaryComponent`, `EmployeeSalaryStructure`, `LopReversal`, `PayrollRun`, `PayslipLineItem`.
- [x] `TASK-B503` (agent-3): Implement 6-step payroll runner service with state transitions (`DRAFT` to `DISBURSED`).
- [x] `TASK-B504` (agent-3): Implement draft recalculation logic (purging draft line items before approval).
- [x] `TASK-B505` (agent-3): Implement LOP reversal processor and backdated salary revision arrears calculator.
- [x] `TASK-B506` (agent-3): Implement Section 194J/194C consultant and intern payout runner.
- [x] `TASK-B507` (agent-5): Write integration tests verifying immutable payslip trigger on `DISBURSED` runs.

## Backend Sprint 6: Indian Statutory Calculation Engine
- [ ] `TASK-B601` (agent-0): Define statutory calculation inputs/outputs and edge case specifications.
- [ ] `TASK-B602` (agent-3): Implement `StatutoryCalculationStrategy` interface and factory.
- [ ] `TASK-B603` (agent-3): Implement EPF/EPS calculator (ceilings, 8.33% EPS / 3.67% EPF split, EDLI, admin charges).
- [ ] `TASK-B604` (agent-3): Implement ESIC calculator with 6-month contribution window continuation rule.
- [ ] `TASK-B605` (agent-3): Implement state PT/LWF calculator (handling Maharashtra February INR 300 rule).
- [ ] `TASK-B606` (agent-3): Implement Old vs. New Tax Regime TDS engine with Form 12B previous income aggregation.
- [ ] `TASK-B607` (agent-5): Write exhaustive unit tests for all 7 Indian payroll edge cases.

## Backend Sprint 7: Exporters, PDF Engine & External CA API
- [ ] `TASK-B701` (agent-0): Define OpenAPI / Swagger UI specifications for CA endpoints and file downloads.
- [ ] `TASK-B702` (agent-4): Implement EPFO ECR .txt exporter (25-column `#~#` official government format).
- [ ] `TASK-B703` (agent-4): Implement Bank NEFT/RTGS CSV exporters (HDFC, ICICI, SBI, Axis).
- [ ] `TASK-B704` (agent-4): Implement asynchronous PDF payslip rendering service backed by `batch_jobs`.
- [ ] `TASK-B705` (agent-4): Implement read-only External CA compliance endpoints (`ROLE_CA`).
- [ ] `TASK-B706` (agent-5): Verify ECR .txt file syntax, test PDF generation, and run full test suite.
