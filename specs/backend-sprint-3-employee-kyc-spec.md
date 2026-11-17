# Backend Sprint 3 Specification: Employee Master, Onboarding & KYC Vault

Author: agent-0-orchestrator
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Sprint: Backend Sprint 3
Status: Approved

## 1. Objective and Scope
Backend Sprint 3 delivers the Employee Master domain, Paperless Self-Service Onboarding, KYC Vault (Aadhaar/PAN/Bank), 50% Basic Wage Code validator, and Form 12B mid-year joiner income capture.

## 2. Statutory Laws and Non-Negotiables
1. Code on Wages 2019 (50% Basic Rule):
   - Basic Pay + Dearness Allowance (DA) must be >= 50% of the employee's monthly gross wage.
   - If Basic + DA < 50%, the shortfall must be flagged and rejected or reallocated to the statutory wage base.
2. Sensitive ID Redaction Law:
   - Raw Aadhaar numbers must NEVER be stored in plain text or returned in API responses.
   - The KYC Vault must store a masked token `[Aadhaar Redacted]`.
   - Bank account numbers must be masked in list/read views (showing only last 4 digits).
3. Section 192 & Form 12B Law:
   - For employees joining mid-financial year, previous employer gross salary, PF, PT, and TDS must be recorded to enable accurate progressive TDS computation in the payroll engine.
4. Multi-Tenant Isolation Law:
   - Every entity (`Employee`, `EmployeeKycVault`, `PreviousEmployerIncome`) must inherit from `BaseTenantEntity` and enforce Hibernate `@TenantId` on `company_id`.

## 3. Database Entities (Owner: agent-1-database)
- `Employee`: `com.ourhr.employee.Employee` (maps to `employees` table).
- `EmployeeKycVault`: `com.ourhr.employee.kyc.EmployeeKycVault` (maps to `employee_kyc_vault` table).
- `PreviousEmployerIncome`: `com.ourhr.employee.salary.PreviousEmployerIncome` (maps to `previous_employer_income` table).
- `EmployeeSalaryStructure`: `com.ourhr.employee.salary.EmployeeSalaryStructure` (maps to `employee_salary_structures` table).

## 4. Domain Logic & Services (Owner: agent-2-core-hr-attendance)
- `EmployeeService` & `EmployeeController`: CRUD with tenant isolation and `@PreAuthorize`.
- `KycVaultService` & `KycVaultController`: Secure KYC storage with Aadhaar token masking.
- `BasicWageValidator`: Validates `SalaryOfferRequest` against `StatutoryQueryService.BASIC_WAGE_RATIO_MIN`.
- `OnboardingService` & `OnboardingController`: Magic link token generation and onboarding submission.
- `Form12BService`: Capture and query mid-year joiner previous employer income.

## 5. Storage & Exporters (Owner: agent-4-exporters-ca)
- `FileStorageService`: Interface and MinIO/S3 adapter in `com.ourhr.exporter.storage` for document uploads.

## 6. QA Gatekeeper Acceptance Criteria (Owner: agent-5-qa-security)
- Unit tests verifying:
  1. 50% Basic wage rule enforces >= 50% and rejects < 50%.
  2. KYC Vault Aadhaar masking is strictly enforced.
  3. Form 12B records are stored and retrieved per employee and FY.
  4. Cross-tenant isolation prevents accessing employee records across companies.
  5. Zero emojis in code, commits, and logs.
