# Development Log

## 2026-11-17
- Initialized ourHR project repository with Git.
- Created AGENTS.md authoritative standard enforcing zero hardcoding and zero emojis.
- Created PROJECT.md, PRD.md, and database-design.md with 16-table schema.
- Scaffolding complete blueprint documentation suite across all 10 documentation sections.

## 2026-11-17 - Backend Sprint 1 Completed
- Executed Backend Sprint 1 (Foundation, Multi-Tenancy & RBAC).
- Agent 0 defined Sprint 1 DTO contracts and JWT claims model.
- Agent 1 implemented JPA entities (Company, Branch, User, UserRole, BatchJob, AuditLog) with Hibernate @TenantId and @Version, along with Spring Data JPA repositories.
- Agent 2 implemented JwtService, JwtAuthenticationFilter, TenantContext thread-local isolation, AuthService, and AuthController (register-company, login, me).
- Agent 4 implemented BatchJobService for persistent asynchronous job lifecycle management.
- Agent 5 implemented 19 comprehensive unit and MockMvc integration tests across JwtServiceTest, AuthServiceTest, AuthControllerTest, TenantContextTest, and BatchJobServiceTest.
- Verification: 19/19 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh.

## 2026-11-17 - Backend Sprint 2 Completed
- Executed Backend Sprint 2 (Company, Multi-State Branches & Statutory Database) via specialized subagents under the 4-step relay protocol.
- Step 1 (agent-0): Defined Company, Branch, and Statutory DTO contracts (CompanyResponse, UpdateCompanyRequest, CreateBranchRequest, BranchResponse, StatutoryRuleDto, PtSlabDto, LwfRuleDto, TdsTaxSlabDto).
- Step 2 (agent-1): Created JPA entities StatutoryRule, PtSlab, LwfRule, TdsTaxSlab mapped to effective-dated V2 seed tables.
- Step 3 (agent-2 & agent-3):
  * Agent 2 implemented CompanyService, CompanyController, BranchService, BranchController with state-specific lookups and multi-tenant security.
  * Agent 3 implemented StatutoryRuleRepository, PtSlabRepository, LwfRuleRepository, TdsTaxSlabRepository, and StatutoryQueryService for zero-hardcoding dynamic resolution of EPF ceilings, ESIC limits, PT slabs, LWF rules, and TDS slabs.
- Step 4 (agent-5): Implemented 29 unit tests in StatutoryQueryServiceTest covering effective-date rule resolution, PT slab lookups across states (KA, MH, TN, TG), LWF rules, and TDS brackets.
- Verification: 78/78 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh.

## 2026-11-17 - Backend Sprint 3 Completed
- Executed Backend Sprint 3 (Employee Master, Onboarding, KYC Vault & File Storage) via 6-agent sequential relay protocol.
- Step 1 (agent-0): Defined Employee Master, KYC Vault, Code on Wages 2019, Form 12B, and Paperless Onboarding DTOs; authored specs/backend-sprint-3-employee-kyc-spec.md.
- Step 2 (agent-1): Created JPA entities Employee, EmployeeKycVault, PreviousEmployerIncome, EmployeeSalaryStructure inheriting from BaseTenantEntity with @TenantId, @Version, and repositories.
- Step 3 (agent-2 & agent-4 parallel domain coding):
  * Agent 2 implemented EmployeeService, EmployeeController, BasicWageValidator (50% Code on Wages rule with statutory deficit math), SalaryStructureService, KycVaultService (Aadhaar [Aadhaar Redacted] masking, bank account masking), PreviousEmployerIncomeService (Form 12B), and OnboardingService (magic link generation, public token submission).
  * Agent 4 implemented S3StorageProperties, FileStorageService interface, LocalFallbackFileStorageService (sanitizing raw Aadhaar numbers from file keys, path traversal protection, 5MB limit), S3FileStorageService, and StorageController.
- Step 4 (agent-5): Implemented 99 unit and MockMvc integration tests across 13 test suites validating the 50% wage rule, Sensitive ID Redaction Law, Form 12B arithmetic, Onboarding magic links, file storage safety, and 5-Persona RBAC matrix.
- Verification: 177/177 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh and regex scan.

## 2026-11-17 - Backend Sprint 4 Completed
- Executed Backend Sprint 4 (Shifts, Attendance & Leave Ledger Engine) across 6 specialized agents.
- Step 1 (agent-0): Defined Attendance punch, Shift, and Leave DTO schemas; authored specs/backend-sprint-4-shifts-attendance-leave-spec.md.
- Step 2 (agent-1): Created JPA entities Shift, HolidayCalendar, AttendanceLog, MonthlyAttendanceSummary, LeaveType, LeaveApplication, LeaveLedger inheriting BaseTenantEntity with @TenantId and @Version; aligned V1 schema with version and updated_at columns.
- Step 3 (agent-2): Implemented ShiftService, ShiftController, AttendanceService (WEB and MOBILE_GPS punch, BIOMETRIC_CSV batch ingestion), LeaveLedgerService (immutable ledger model, SQL SUM balance, and Indian Sandwich rule calculation), and AttendanceFreezeService (monthly attendance lock, payable days, LOP days).
- Step 4 (agent-5): Implemented 95 unit and MockMvc integration tests across 8 test suites validating the Indian Sandwich rule, immutable ledger transactions, CSV biometric upload, attendance freeze locking, and 5-Persona RBAC matrix.
- Verification: 272/272 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh and regex scan.

## 2026-11-17 - Backend Sprint 5 Completed
- Executed Backend Sprint 5 (CTC Builder, Arrears & 6-Step Payroll Engine) across 6 specialized agents.
- Step 1 (agent-0): Defined 6-step payroll state machine, audit diff, LOP reversal, and consultant payout DTO schemas; authored specs/backend-sprint-5-payroll-engine-spec.md.
- Step 2 (agent-1): Created JPA entities SalaryComponent, LopReversal, PayrollRun, PayslipLineItem with @TenantId and @Version; aligned V1 schema.
- Step 3 (agent-3): Implemented PayrollRunnerService (6-step state machine from DRAFT to DISBURSED), draft recalculation purge-and-replace, LopReversalService (daily wage prorated math), and ConsultantPayoutService (Section 194J/194C TDS calculation).
- Step 4 (agent-5): Implemented 93 unit and MockMvc integration tests across ImmutablePayrollDisbursalTest and service suites verifying the Immutable Payroll Law, draft recalculation clean replacement, state transition integrity, and full 48-check 5-Persona RBAC matrix.
- Verification: 365/365 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh and regex scan.

## 2026-11-17 - Backend Sprint 6 Completed
- Executed Backend Sprint 6 (Indian Statutory Calculation Engine) across 6 specialized agents.
- Step 1 (agent-0): Defined StatutoryCalculationRequest, StatutoryCalculationResult, and component DTOs; authored specs/backend-sprint-6-statutory-engine-spec.md.
- Step 2 (agent-1): Seed tables (statutory_rules, pt_slabs, lwf_rules, tds_tax_slabs) verified.
- Step 3 (agent-3): Implemented StatutoryCalculationStrategy, StatutoryStrategyFactory, EpfCalculatorStrategy, EsicCalculatorStrategy, PtCalculatorStrategy, LwfCalculatorStrategy, TdsProjectorStrategy, IndianStatutoryEngineService, and StatutoryCalculationController.
- Step 4 (agent-5 & agent-3 relay):
  * Agent 5 executed gate check and detected defect DEFECT-B607-01 (EPS statutory ceiling rounding math).
  * Adhering strictly to AGENTS.md, Agent 5 routed the defect to Agent 3 who applied the statutory ceiling cap fix in EpfCalculatorStrategy.
  * Agent 5 re-verified with 21 new unit and integration tests covering the 7 statutory edge cases (EPF/EPS split, ESIC 6-month window continuation, Maharashtra February INR 300 PT, LWF seasonal deduction, Old vs New TDS projection with Form 12B, and RBAC matrix).
- Verification: 386/386 tests passed (100% green). Zero emojis verified via scripts/validate-docs.sh and regex scan.
