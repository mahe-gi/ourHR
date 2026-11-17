# System Requirements Specification (SRS)

## 1. Functional Requirements (FR)

| ID | Requirement Description | Priority | Source | Verification |
|---|---|---|---|---|
| FR-001 | Multi-state branch setup with state-specific PT and LWF mapping | Must | PRD Module 1 | Integration Test |
| FR-002 | Self-service paperless onboarding with KYC document vault | Must | PRD Module 2 | E2E Journey |
| FR-003 | Statutory 50% Basic wage appointment letter validation | Must | PRD Module 2 | Unit Test |
| FR-004 | Form 12B previous employer income capture for mid-year joiners | Must | PRD Module 2 | Integration Test |
| FR-005 | Web clock-in and Mobile GPS geofenced punch with selfie upload | Must | PRD Module 3 | E2E Journey |
| FR-006 | Late-mark penalty calculation and missed punch regularization | Must | PRD Module 3 | Unit Test |
| FR-007 | Monthly attendance freeze with payable days and LOP calculation | Must | PRD Module 3 | Unit Test |
| FR-008 | Immutable transaction ledger for all leave accruals and deductions | Must | PRD Module 4 | Unit Test |
| FR-009 | Guided 6-step payroll state machine with variance audit | Must | PRD Module 5 | Integration Test |
| FR-010 | Bank bulk transfer file export (NEFT/RTGS CSV) | Must | PRD Module 5 | Contract Test |
| FR-011 | EPFO ECR text format export conforming to official portal format | Must | PRD Module 6 | Contract Test |
| FR-012 | ESIC contribution calculation and monthly report generation | Must | PRD Module 6 | Unit Test |
| FR-013 | Old vs. New Tax Regime side-by-side TDS computation | Must | PRD Module 6 | Unit Test |
| FR-014 | Form 24Q quarterly e-TDS return file generation | Must | PRD Module 6 | Integration Test |
| FR-015 | External CA guest workspace with direct compliance export | Must | PRD Module 8 | E2E Journey |
| FR-016 | Role-switching header toggle (Admin Workspace <-> Self-Service) | Must | PRD Section 2 | UI Test |

## 2. Non-Functional Requirements (NFR)

| ID | Category | Requirement Target | Verification Method |
|---|---|---|---|
| NFR-001 | Multi-Tenancy | Complete isolation via company_id and Hibernate @TenantId | Security & Tenant Test |
| NFR-002 | Performance | 250-employee payroll execution under 5 seconds | Benchmark Load Test |
| NFR-003 | Precision | All financial calculations in BigDecimal / NUMERIC(12,2) | Static Analysis & Unit Test |
| NFR-004 | Auditability | Immutable audit_logs for all salary revisions and run approvals | Integration Test |
| NFR-005 | Storage | Binary assets stored exclusively in S3-compatible storage | Architecture Audit |
| NFR-006 | Availability | 99.9% uptime for self-service clock-in and payslip retrieval | Monitoring SLA |
| NFR-007 | Security | JWT authentication with role-based access control (RBAC) | Penetration Test |
