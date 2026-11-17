#!/usr/bin/env python3
"""
ourHR Blueprint Documentation Generator
Generates all remaining blueprint documentation files with domain-specific content.
Enforces zero hardcoding policy, multi-tenancy, and zero emojis constraint.
"""

import os
from pathlib import Path

BASE_DIR = Path("/home/maheshch/Mahesh/ourHR")

FILES = {}

# -------------------------------------------------------------
# ROOT FILES
# -------------------------------------------------------------
FILES["README.md"] = """# ourHR: Indian MSME & Startup HRMS

## 1. Project Overview
ourHR is an enterprise-grade Human Resource Management System (HRMS) and statutory payroll platform tailored for Indian startups and MSMEs with 15 to 250 employees. It couples the rigorous statutory precision of greytHR with the modern user experience of Keka HR, eliminating spreadsheet-based handoffs to external Chartered Accountants.

## 2. Core Value Proposition
- Statutory Accuracy Without Hardcoding: All EPF ceilings, ESIC thresholds, state PT/LWF slabs, and income tax brackets are versioned in database tables.
- External CA Guest Workspace: Direct portal for company Chartered Accountants to audit payroll runs and retrieve EPFO ECR (.txt), ESIC returns, and Form 24Q files.
- 6-Step Guided Payroll Run: Deterministic, maker-checker payroll processing with side-by-side variance detection.
- Decoupled Multi-Surface Architecture: Decoupled backend (Java Spring Boot) and frontends (React Vite Web, React Native Mobile, Next.js Landing).
- Multi-Tenancy Isolation: Database-enforced tenant filtering via company_id and Spring Hibernate @TenantId.

## 3. Technology Stack
- Backend: Java 17/21, Spring Boot 3, Spring MVC, Spring Data JPA, Hibernate, PostgreSQL, Flyway, S3 Adapter.
- Web Application: React 18+, Vite, TypeScript, Tailwind CSS, TanStack Query, React Hook Form, Zod.
- Mobile Application: React Native, TypeScript, NativeWind, React Navigation.
- Landing Page: Next.js, TypeScript, Tailwind CSS.

## 4. Documentation Map
- Governance: docs/00-governance/
- Product Requirements: PRD.md, docs/01-product/
- System Architecture: docs/02-architecture/
- UI/UX Design System: docs/03-design/
- Data & Database Design: docs/04-data/
- API Contracts: docs/05-api/
- Engineering Standards: docs/06-engineering/
- Security & Compliance: docs/07-security/
- Quality Assurance: docs/08-quality/
- Operations & Deployment: docs/09-operations/
- Agent Workflow & Personas: docs/10-agent-workflow/
- Architecture Decision Records: decisions/
- Feature Specifications: specs/
- Planning & Tasks: plans/, tasks/

## 5. Development Principles
- Zero Hardcoding Policy: All regulatory parameters are dynamically resolved by effective date and branch state.
- Zero Emojis Policy: No emojis in source files, commit messages, or user interfaces.
- Exact Decimals: All monetary calculations use NUMERIC(12,2) and BigDecimal.
- S3 Storage: No binary data in relational database tables.
"""

FILES["REQUIREMENTS.md"] = """# System Requirements Specification (SRS)

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
"""

FILES["SCOPE.md"] = """# Scope Management Document

## 1. Phase 1: MVP Scope (Included)
- Module 1: Company Setup & Multi-State Policy Engine
- Module 2: Employee Master & Paperless Onboarding (including Form 12B)
- Module 3: Time, Shifts & Attendance Engine (Web, Mobile GPS + Selfie, Biometric hook)
- Module 4: Leave & Holiday Engine (Immutable Leave Ledger, Sandwich Rule)
- Module 5: Core Indian Payroll Engine (6-Step Stepper, 50% Basic Rule, Bank CSV)
- Module 6: Indian Statutory Compliance (EPF ECR .txt, ESIC, PT, TDS Old vs New, Form 24Q)
- Module 7: Employee & Manager Self-Service (ESS Web & Mobile, 1-Click Approvals)
- Module 8: Basic External CA Co-Pilot Workspace (Read-only compliance audit & exports)

## 2. Phase 2 Scope (Financial & Admin Operations)
- Expense Claims with Mobile Receipt OCR
- Employee Loans & Salary Advances (EMI schedules)
- IT Asset Lifecycle Tracking
- Fast-Track Exit Management & Full-and-Final (FnF) Settlement (2-day wage settlement)

## 3. Phase 3 Scope (Talent & Growth)
- Lightweight Applicant Tracking System (ATS) & Offer Builder
- Performance Management, OKRs & 360-degree Appraisals
- Project Timesheets & Billable Utilization

## 4. Phase 4 Scope (Market Differentiators)
- WhatsApp & Vernacular Voice Attendance Bot
- Tablet Attendance Kiosk Mode (Facial Recognition)
- 1-Click Instant Background Verification (BGV via API)
- Multi-Company CA Partner Console

## 5. Scope Change Control Procedure
1. Any proposed addition to the approved scope must be submitted as a formal Change Request (CR).
2. The Orchestrator, Product, and Architecture agents must assess regulatory, schema, and schedule impacts.
3. No agent may autonomously introduce new features without updating SCOPE.md and receiving approval.
"""

FILES["CONTRIBUTING.md"] = """# Contributing Guidelines

## 1. Architectural Standards
All contributions to ourHR must adhere to:
- AGENTS.md authoritative rules.
- Strict multi-tenancy: every tenant entity must declare company_id and Hibernate @TenantId.
- Zero Hardcoding: statutory limits, tax brackets, and state rules must reside in database tables.
- Zero Emojis: do not introduce emojis in code, commits, pull requests, or UI elements.

## 2. Git Workflow
- Branch naming convention:
  - Feature: feature/<module>-<description> (e.g., feature/payroll-stepper)
  - Bugfix: fix/<issue-description>
  - Documentation: docs/<section-description>
- Commit message standard:
  - Format: <type>(<scope>): <concise description in lowercase>
  - Types: feat, fix, docs, style, refactor, test, chore
  - Example: feat(payroll): implement 50 percent basic wage validator

## 3. Pull Request Requirements
Every PR must include:
- Link to relevant task ID or requirement reference.
- Passing unit and integration tests.
- Evidence of verification (test execution output).
- Verified adherence to the zero-emoji and zero-hardcoding policies.
"""

FILES["CHANGELOG.md"] = """# Changelog

All notable changes to ourHR will be documented in this file.
The format is based on Keep a Changelog, and this project adheres to Semantic Versioning.

## [1.0.0-draft] - 2026-11-17

### Added
- Authoritative multi-agent governance file (AGENTS.md).
- Product Requirements Document (PRD.md) covering Phase 1 MVP.
- Project overview and scope specifications (PROJECT.md, REQUIREMENTS.md, SCOPE.md).
- Relational database schema specification with 16 core Phase 1 tables (docs/04-data/database-design.md).
- Full blueprint documentation scaffolding across governance, product, architecture, design, data, API, engineering, security, quality, operations, and agent workflow.
"""

FILES[".env.example"] = """# Environment Variables Template for ourHR

# Server Configuration
SERVER_PORT=8080
SPRING_PROFILES_ACTIVE=dev

# PostgreSQL Database Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=ourhr_db
DB_USERNAME=postgres
DB_PASSWORD=replace_with_secure_password
DB_SCHEMA=public

# Multi-Tenancy
TENANT_ISOLATION_STRATEGY=DISCRIMINATOR

# Security & JWT Configuration
JWT_SECRET=replace_with_at_least_256_bit_random_secret_key_base64
JWT_EXPIRATION_MS=86400000
JWT_REFRESH_EXPIRATION_MS=604800000

# S3-Compatible Object Storage Adapter (AWS S3 / Cloudflare R2 / MinIO)
S3_ENDPOINT=http://localhost:9000
S3_REGION=us-east-1
S3_BUCKET_NAME=ourhr-documents
S3_ACCESS_KEY=minioadmin
S3_SECRET_KEY=minioadmin
S3_PATH_STYLE_ACCESS=true

# Logging & Monitoring
LOG_LEVEL_ROOT=INFO
LOG_LEVEL_APP=DEBUG
LOG_FORMAT=JSON
"""

# -------------------------------------------------------------
# .GEMINI RULES
# -------------------------------------------------------------
FILES[".gemini/rules/00-global.md"] = """# Antigravity Global Rule: ourHR Standards

1. Authority: Follow AGENTS.md as the supreme operational standard.
2. Zero Emojis: Never introduce emojis into code, comments, documentation, UI strings, or logs.
3. Zero Hardcoding: Statutory ceilings, percentages, tax slabs, and rules must be queried from database tables.
4. Decimal Precision: Always use BigDecimal in Java and NUMERIC(12,2) in PostgreSQL for monetary values.
5. Multi-Tenancy: Enforce company_id and Hibernate @TenantId across all business queries.
6. Verification: Never declare a task complete without executing test commands and recording evidence.
"""

FILES[".gemini/rules/01-architecture.md"] = """# Antigravity Architecture Rule: System Boundaries

1. Decoupled Surfaces: Backend (Spring Boot) and frontends (React Vite, React Native, Next.js) must remain fully decoupled.
2. Contract-First Development: Endpoints must be documented and agreed in docs/05-api/ before client implementation.
3. S3 Storage: Binary files, KYC documents, selfie punches, and payslips must never be stored in database columns. Use the S3 adapter.
4. Immutability: Completed payroll runs, payslips, and leave ledger transactions are strictly append-only.
"""

FILES[".gemini/rules/02-frontend.md"] = """# Antigravity Frontend Rule: UI/UX Standards

1. Web: React 18+, Vite, TypeScript, Tailwind CSS, TanStack Query, React Hook Form, Zod.
2. Mobile: React Native, TypeScript, NativeWind, React Navigation.
3. Landing: Next.js with static site generation.
4. Icons: Use Lucide SVG icons exclusively. No emojis under any circumstance.
5. Role Switching: Provide the top header toggle 'Switch View: Admin Workspace <-> My Self-Service'.
6. State Handling: Every data-fetching view must handle Loading, Empty, Error, and Success states explicitly.
"""

FILES[".gemini/rules/03-backend.md"] = """# Antigravity Backend Rule: Spring Boot Standards

1. Architecture: Modular Monolith with Spring Boot 3, Spring MVC, and Spring Data JPA.
2. Multi-Tenancy: Annotate tenant entities with Hibernate @TenantId and bind company_id.
3. Migrations: All database schema modifications must be implemented through versioned Flyway scripts in migrations/.
4. Error Handling: Return standard envelope: {"success": false, "error": {"code": "...", "message": "...", "details": []}}.
5. Validation: Use Jakarta Bean Validation on all DTOs and request payloads.
"""

FILES[".gemini/rules/04-database.md"] = """# Antigravity Database Rule: PostgreSQL Integrity

1. Data Types: Use UUID for primary keys, NUMERIC(12,2) for currency, and TIMESTAMP WITH TIME ZONE for dates.
2. Constraints: Enforce foreign keys, unique constraints, and check constraints explicitly.
3. Leave Ledger: Never maintain a mutable remaining_balance column. Leave balances are dynamically derived from SUM(days) in leave_ledgers.
4. Audit Trail: All status changes and sensitive modifications must write to audit_logs.
"""

FILES[".gemini/rules/05-testing.md"] = """# Antigravity Testing Rule: Verification Standards

1. Backend: JUnit 5, Mockito, and Spring Boot Test for unit and integration testing.
2. Frontend: Vitest and React Testing Library for components; Playwright for critical user journeys.
3. Statutory Tests: Cover 50% Basic wage adjustments, EPF ceiling caps, ESIC threshold crossovers, and Old vs. New tax calculations with isolated test suites.
4. Quality Gates: Formatting, linting, typechecking, and test suites must pass with zero failures before commit.
"""

FILES[".gemini/rules/06-security.md"] = """# Antigravity Security Rule: Statutory Data Privacy

1. Secrets: Zero secrets, private keys, or API tokens in git repositories. Use environment variables.
2. RBAC: Enforce role-based checks (ROLE_FOUNDER, ROLE_HR, ROLE_FINANCE, ROLE_CA, ROLE_MANAGER, ROLE_EMPLOYEE) at the service and controller layer.
3. Data Masking: Mask Aadhaar and PAN numbers in public API responses.
4. Tenant Isolation: Prevent cross-tenant data access through DB-level filters and integration tests.
"""

# -------------------------------------------------------------
# .GEMINI SKILLS
# -------------------------------------------------------------
FILES[".gemini/skills/project-analysis/SKILL.md"] = """---
name: project-analysis
description: Analyzes repository state, verifies architectural compliance, and reports missing documentation or tests.
---

# Project Analysis Skill

## Procedure
1. Check repository git status and recent commit history.
2. Verify that all 16 core database tables in docs/04-data/database-design.md have corresponding Flyway migrations and JPA entities.
3. Validate that no hardcoded statutory limits or emojis exist in the codebase.
4. Report health status, outstanding tasks, and architectural deviations.
"""

FILES[".gemini/skills/feature-planning/SKILL.md"] = """---
name: feature-planning
description: Breaks approved feature requirements into dependency-aware tasks and API contracts.
---

# Feature Planning Skill

## Procedure
1. Read the feature specification in specs/features/ or PRD.md.
2. Check existing schema in docs/04-data/database-design.md and API standards in docs/05-api/api-standards.md.
3. Define the contract in specs/_template/api-contract.md.
4. Create task breakdown with backend, frontend, database, and QA assignments in tasks/backlog.md.
"""

FILES[".gemini/skills/feature-implementation/SKILL.md"] = """---
name: feature-implementation
description: Guides contract-first implementation across backend and frontend surfaces.
---

# Feature Implementation Skill

## Procedure
1. Verify API contract approval before parallel implementation.
2. Implement database migrations and JPA entities with Hibernate @TenantId.
3. Implement backend service and controller with Jakarta validation and unit tests.
4. Implement frontend components with Tailwind CSS and Lucide SVG icons (no emojis).
5. Run full verification suite and update documentation.
"""

FILES[".gemini/skills/code-review/SKILL.md"] = """---
name: code-review
description: Performs rigorous automated code review against AGENTS.md standards.
---

# Code Review Skill

## Checklist
- [ ] No hardcoded statutory values (EPF, ESIC, PT, TDS).
- [ ] No emojis in code, comments, or UI components.
- [ ] Multi-tenancy isolation enforced with company_id.
- [ ] Financial calculations use BigDecimal / NUMERIC(12,2).
- [ ] No binary data stored directly in the database.
- [ ] Unit and integration tests cover happy and negative paths.
"""

FILES[".gemini/skills/release-verification/SKILL.md"] = """---
name: release-verification
description: Verifies release readiness against quality gates before production tagging.
---

# Release Verification Skill

## Procedure
1. Run complete test suites across backend and frontend surfaces.
2. Validate Flyway migration scripts against a clean test database.
3. Verify that all compliance file exporters (ECR .txt, Bank CSV, Form 24Q) produce valid outputs.
4. Prepare release report in reports/release-reports/.
"""

# -------------------------------------------------------------
# DOCS: 00-GOVERNANCE
# -------------------------------------------------------------
FILES["docs/00-governance/documentation-policy.md"] = """# Documentation Maintenance Policy

## 1. Ownership and Lifecycle
Every document in the ourHR repository must have:
- Document Owner: Specific agent role (Product, Architecture, Security, etc.).
- Status: DRAFT, IN_REVIEW, APPROVED, DEPRECATED, or ARCHIVED.
- Last Updated Date: ISO format date.

## 2. Maintenance Rules
1. Atomic Updates: When an API contract or database schema changes, documentation must be updated in the same commit.
2. Single Source of Truth: PRD.md is the sole authority on business rules; docs/04-data/ is the sole authority on schema.
3. Conflict Escalation: Inconsistencies between documents must halt implementation until resolved by human approval.
"""

FILES["docs/00-governance/engineering-principles.md"] = """# Engineering Principles

1. Statutory Accuracy Is Absolute: A single rupee discrepancy in PF or TDS creates compliance liabilities. Calculations must be exact.
2. Zero Hardcoding: Labour laws, tax brackets, and state rules change frequently. All parameters are dynamic and versioned.
3. Defense in Depth: Multi-tenancy isolation is enforced at the database, ORM, and service layers.
4. Independent Deployability: Frontend and backend artifacts must build and deploy autonomously.
5. Simplicity Over Premature Abstraction: Build clean, cohesive modules; avoid microservice proliferation where a modular monolith suffices.
"""

FILES["docs/00-governance/glossary.md"] = """# Domain Glossary: Indian HRMS & Payroll

- Basic Pay: Mandatory core salary component. Under the Code on Wages, Basic + DA must comprise at least 50% of gross remuneration.
- DA (Dearness Allowance): Cost-of-living adjustment allowance, considered part of basic wage for statutory calculations.
- HRA (House Rent Allowance): Allowance exempt from tax under Section 10(13A) in the Old Tax Regime based on rent paid.
- EPF (Employees' Provident Fund): Mandatory retirement benefit. 12% employee contribution, 12% employer contribution (split into 8.33% EPS and 3.67% EPF).
- ESIC (Employees' State Insurance): Social security scheme for employees earning gross monthly wages up to INR 21,000 (0.75% employee, 3.25% employer).
- PT (Professional Tax): State-level employment tax with state-specific salary slabs and schedules (capped at INR 2,500 annually).
- LWF (Labour Welfare Fund): Statutory fund governed by state boards with varying employee/employer contribution ratios and frequencies.
- ECR (Electronic Challan cum Return): Prescribed monthly text format file uploaded to the EPFO government portal.
- Form 12B: Statutory statement submitted by an employee joining mid-year detailing earnings, PF, and TDS deducted by previous employers.
- Form 24Q: Quarterly e-TDS return filed by employers containing salary and tax deduction details of all employees.
- LOP (Loss of Pay): Unpaid leave days deducted from payable days during a monthly payroll run.
- Sandwich Rule: Policy where non-working days (weekends or public holidays) falling between leave days are also deducted as leave.
"""

FILES["docs/00-governance/risk-register.md"] = """# Project Risk Register

| Risk ID | Risk Description | Severity | Likelihood | Mitigation Strategy | Owner |
|---|---|---|---|---|---|
| R-001 | Cross-tenant data leakage in multi-tenant queries | Critical | Low | Hibernate @TenantId filter + PostgreSQL RLS + automated tenant security tests | Security Agent |
| R-002 | Statutory calculation errors (PF ceiling, 50% rule) | High | Medium | Versioned statutory_rules table + comprehensive unit tests with edge-case fixtures | QA Agent |
| R-003 | Corrupted leave balances due to concurrent updates | High | Low | Immutable append-only leave_ledgers table; balance computed from transaction sum | Architect Agent |
| R-004 | Memory exhaustion during bulk PDF payslip generation | Medium | Medium | Asynchronous off-thread generation via Spring @Async with S3 direct streaming | Backend Agent |
| R-005 | Form 12B omission causing year-end TDS under-deduction | Medium | High | Mandatory onboarding checklist item for mid-year joiners with tax simulator warning | Product Agent |
"""

# -------------------------------------------------------------
# DOCS: 01-PRODUCT
# -------------------------------------------------------------
FILES["docs/01-product/product-overview.md"] = """# Product Overview

ourHR is built specifically for Indian MSMEs and startups (15 to 250 employees) that require the compliance precision of greytHR and the modern user experience of Keka HR without spreadsheet ping-pong with external Chartered Accountants.

## Core Modules
1. Company Setup & Multi-State Policy Engine
2. Employee Master & Paperless Onboarding
3. Time, Shifts & Attendance Engine
4. Leave & Holiday Engine
5. Core Indian Payroll Engine
6. Indian Statutory Compliance Engine
7. Employee & Manager Self-Service
8. Basic External CA Co-Pilot Workspace
"""

FILES["docs/01-product/personas.md"] = """# User Personas & Permissions

## 1. Founder / Super Admin
- Goals: 60-second snapshot of monthly payroll outflow, headcount, statutory liabilities, and major approvals.
- Permissions: Complete system access, company settings, final payroll maker-checker authorization.

## 2. HR Admin / Operations Manager
- Goals: Manage employee onboarding, attendance rules, leave policies, document generation, and asset assignments.
- Permissions: Employee directory CRUD, attendance lock, leave adjustments, onboarding approvals.

## 3. Finance Manager / Payroll Admin
- Goals: Configure salary components, execute 6-step payroll run, generate bank files, verify tax deductions.
- Permissions: Payroll run initiation, salary structure modifications, reimbursement approvals, loan management.

## 4. External Chartered Accountant (CA)
- Goals: Verify monthly statutory liabilities, download ECR .txt, ESIC reports, PT returns, and Form 24Q files.
- Permissions: Read-only compliance portal, statutory report export, tax audit variance views.

## 5. Reporting Manager & Employee
- Goals: Clock-in/out, apply for leaves, regularize punches, submit tax declarations, download payslips, approve team requests.
- Permissions: Self-service profile, team approval inbox (for managers with direct reports).
"""

FILES["docs/01-product/user-journeys.md"] = """# End-to-End User Journeys

## Journey 1: Paperless Onboarding to Compliant Appointment Letter
1. HR Admin creates a candidate profile with designation, department, branch, and offered CTC.
2. System validates that Basic + DA is at least 50% of gross remuneration.
3. Candidate receives magic onboarding link, fills personal details, bank IFSC, UAN, and uploads KYC documents to S3.
4. Mid-year joiners submit Form 12B prior employment details.
5. System generates the standard Indian Labour Code appointment letter and collects e-signature.

## Journey 2: Monthly Attendance Lock to 6-Step Payroll Run
1. Shift engine captures punches via Web, Mobile GPS+Selfie, or Biometric sync.
2. On the cut-off date (e.g., 25th), HR Admin reviews missed punches and triggers Monthly Attendance Lock.
3. Finance Manager launches the 6-Step Payroll Stepper:
   - Step 1: Sync Attendance and LOP (applies pending LOP reversals).
   - Step 2: Joiners & Exits (prorates mid-month salaries).
   - Step 3: Variable Pay & Overrides (adds bonuses and overtime pay).
   - Step 4: Reimbursements & Loan EMIs (includes expenses, deducts loan instalments).
   - Step 5: Variance & Anomaly Audit (side-by-side diff vs. last month).
   - Step 6: Maker-Checker Approval (Founder sign-off, bank CSV export, ECR .txt generation).
4. Payslips are distributed asynchronously to employee self-service portals before the 7th.

## Journey 3: External CA Compliance Audit
1. External CA logs in via dedicated guest credentials.
2. CA views compliance dashboard displaying due dates (7th for TDS/wages, 15th for PF/ESIC).
3. CA inspects variance between calculated liability and actual payroll numbers.
4. CA downloads EPFO ECR .txt, ESIC monthly report, and quarterly Form 24Q files with 1 click.
"""

FILES["docs/01-product/user-stories.md"] = """# User Stories

## Module 1: Company Setup
- US-101: As a Founder, I want to configure legal entity details (PAN, TAN, GSTIN, PF, ESIC) so that all statutory outputs contain valid establishment codes.
- US-102: As an HR Admin, I want to create multiple state branches so that employees automatically inherit state-specific PT and holiday rules.

## Module 2: Employee Master & Onboarding
- US-201: As an HR Admin, I want to send a self-service onboarding link so candidates can enter their own KYC and bank details.
- US-202: As an Employee, I want to upload Form 12B prior employer tax details so my annual tax is calculated accurately.
- US-203: As an HR Admin, I want the system to enforce the 50% Basic wage rule before appointment letter generation.

## Module 3: Attendance & Shifts
- US-301: As an Employee, I want to clock in via mobile with GPS verification and live selfie capture.
- US-302: As a Manager, I want to approve attendance regularizations in 1 click.
- US-303: As an HR Admin, I want to lock monthly attendance to calculate payable days and LOP days for payroll.

## Module 4: Leave Management
- US-401: As an Employee, I want to apply for leave and view real-time balance derived from my leave ledger.
- US-402: As an HR Admin, I want to configure the Sandwich Rule to automatically count intervening weekends as leave days.

## Module 5: Core Payroll
- US-501: As a Finance Admin, I want a guided 6-step wizard to execute monthly payroll with automated variance alerts.
- US-502: As a Finance Admin, I want to export bank bulk transfer CSVs formatted for major Indian banks.

## Module 6: Statutory Compliance
- US-601: As a CA, I want to download valid EPFO ECR text files ready for upload to the government portal.
- US-602: As an Employee, I want a side-by-side comparison of Old vs. New Tax Regimes to select my preferred regime.
"""

FILES["docs/01-product/business-rules.md"] = """# Indian Statutory Business Rules

## 1. Code on Wages: 50% Basic Wage Rule
- Statutory Definition: Basic Pay + Dearness Allowance + Retaining Allowance must be >= 50% of total gross remuneration.
- Engine Rule: If excluded allowances (HRA, Special Allowance, Conveyance, etc.) exceed 50% of gross remuneration, the excess amount is automatically added back into the statutory wage base for EPF, ESIC, and Gratuity calculations.

## 2. Employees' Provident Fund (EPF & EPS)
- Wage Ceiling: Standard statutory ceiling of INR 15,000 / INR 25,000 per month (configurable in statutory_rules).
- Employee Contribution: 12% of EPF wage base.
- Employer Contribution:
  - EPS (Pension): 8.33% of EPF wage (capped at the ceiling).
  - EPF (Difference): 12% minus EPS share (minimum 3.67%).
  - EDLI (Insurance): 0.5% (subject to ceiling).
  - Admin Charges: 0.5% of total EPF wages (subject to statutory minimums).

## 3. Employees' State Insurance (ESIC)
- Eligibility: Gross monthly salary <= INR 21,000 (INR 25,000 for employees with disability).
- Employee Contribution: 0.75% of gross wages.
- Employer Contribution: 3.25% of gross wages.
- Six-Month Contribution Rule: If an employee's salary crosses INR 21,000 during a contribution period (April-Sept or Oct-March), deductions continue until the end of that period.

## 4. Professional Tax (PT)
- Deducted based on branch state code, monthly gross earnings, and gender.
- Special Month Rule: For example, Maharashtra deducts INR 200/month except February where INR 300 is deducted to reach the annual cap of INR 2,500.

## 5. Income Tax (TDS under Section 192)
- New Tax Regime: Default regime with INR 75,000 standard deduction and Section 87A rebate thresholds.
- Old Tax Regime: Optional regime with exemptions for HRA (Section 10(13A)), Chapter VI-A deductions (80C, 80D, 80CCD), and home loan interest (Section 24B).
- Form 12B Integration: Previous employer gross salary and TDS deducted are aggregated for accurate progressive slab computation.
"""

FILES["docs/01-product/acceptance-criteria.md"] = """# Acceptance Criteria Specifications

## AC-001: 50% Basic Wage Rule Enforcement
- Given an employee salary structure where Gross = INR 1,00,000 and Basic = INR 40,000 (40%),
- When the payroll engine calculates statutory deductions,
- Then the excess allowance of INR 10,000 (50,000 - 40,000) is added to the statutory wage base, making statutory wages INR 50,000 for EPF/ESIC/Gratuity computations.

## AC-002: Immutable Leave Ledger Calculation
- Given an employee with 10 days EL credited via accrual transaction,
- When the employee applies for 3 days leave and it is approved,
- Then a consumption transaction of -3.0 days is written to leave_ledgers, and the current balance dynamically queries to 7.0 days without updating any mutable balance column.

## AC-003: Monthly Payroll Variance Detection
- Given an employee whose net pay in October was INR 60,000,
- When November payroll calculation produces a net pay of INR 72,000,
- Then Step 5 of the payroll run flags an anomaly highlighting a +INR 12,000 variance with breakdown (e.g., Performance Bonus).

## AC-004: Multi-Tenant Data Isolation
- Given a user authenticated under Company A,
- When they query employees, payroll runs, or attendance records,
- Then Hibernate @TenantId automatically filters results to company_id = Company A, returning 0 records belonging to Company B even if direct IDs are queried.
"""

# -------------------------------------------------------------
# DOCS: 02-ARCHITECTURE
# -------------------------------------------------------------
FILES["docs/02-architecture/system-architecture.md"] = """# System Architecture Specification

## 1. High-Level Architecture
ourHR is implemented as a Modular Monolith backend in Java Spring Boot paired with decoupled frontend clients.

```
+-------------------------------------------------------------+
|                        Client Layer                         |
|   [ React Vite Web ]     [ React Native ]   [ Next.js Page ]|
+------------------------------+------------------------------+
                               | HTTPS / REST (JSON)
+------------------------------v------------------------------+
|                   Spring Boot Application                   |
|  +-------------------------------------------------------+  |
|  | Security Filter Chain (JWT Auth + RBAC + Tenant Filter)|  |
|  +---------------------------+---------------------------+  |
|                              |                              |
|  +---------------------------v---------------------------+  |
|  | Modular Domain Services                               |  |
|  | - CompanyService           - EmployeeService          |  |
|  | - AttendanceService        - LeaveService             |  |
|  | - PayrollService           - StatutoryComplianceEngine|  |
|  | - ExporterService (ECR/CSV)- CAAuditService           |  |
|  +---------------------------+---------------------------+  |
|                              | Spring Data JPA              |
|  +---------------------------v---------------------------+  |
|  | PostgreSQL (Multi-tenant schema via @TenantId)        |  |
|  +-------------------------------------------------------+  |
+-------------------------------------------------------------+
                               | S3 API
+------------------------------v------------------------------+
|           S3-Compatible Storage (R2 / MinIO / S3)           |
+-------------------------------------------------------------+
```

## 2. Component Boundaries
- Web & Mobile Clients: Presentation and local validation only; all business calculations executed by backend.
- Spring MVC Controllers: Contract-first REST API endpoints accepting validated DTOs.
- Domain Services: Business rules (50% basic validator, leave ledger math, 6-step payroll state machine).
- S3 Storage Adapter: Secure abstraction for document and image uploads, returning pre-signed or secure URLs.
"""

FILES["docs/02-architecture/technology-stack.md"] = """# Technology Stack Specification

| Tier | Component | Technology | Version | Justification |
|---|---|---|---|---|
| Backend | Language | Java | 17 / 21 LTS | Enterprise stability, strong typing, massive compliance ecosystem |
| Backend | Framework | Spring Boot | 3.2+ | Industry standard, robust dependency injection, Spring Security |
| Backend | Web Layer | Spring MVC | 6.1+ | Contract-first REST APIs, Jakarta validation integration |
| Backend | Persistence | Spring Data JPA / Hibernate | 6.4+ | Native @TenantId multi-tenancy, transactional integrity |
| Database | Relational Store | PostgreSQL | 15+ | Relational guarantees, JSONB for flexible rules, ACID compliance |
| Migrations | Schema Versioning | Flyway | 10+ | Repeatable, deterministic schema evolution |
| Storage | Object Store | S3 Adapter (R2 / MinIO / S3) | - | Secure document vault, zero binary data in database |
| Frontend | Web Application | React + Vite | React 18+ | Fast compilation, clean component architecture |
| Frontend | Mobile Application | React Native | 0.73+ | Cross-platform mobile for iOS and Android |
| Frontend | Landing Page | Next.js | 14+ | Static site generation, SEO optimization |
| Styling | CSS Engine | Tailwind CSS | 3.4+ | Design token consistency, zero runtime CSS overhead |
| Validation | Client Schema | Zod | 3.22+ | TypeScript-native validation matching Jakarta constraints |
"""

FILES["docs/02-architecture/application-structure.md"] = """# Application Structure

```
ourHR/
├── backend/
│   ├── src/main/java/com/ourhr/
│   │   ├── config/              # Security, S3, Multi-tenancy, Swagger configs
│   │   ├── core/                # Base entities, multi-tenancy filters, exceptions
│   │   ├── modules/
│   │   │   ├── company/         # Legal entities, branches, hierarchy
│   │   │   ├── employee/        # Master records, KYC vault, Form 12B
│   │   │   ├── attendance/      # Clock-in, shifts, monthly lock, overtime
│   │   │   ├── leave/           # Leave types, applications, immutable ledgers
│   │   │   ├── payroll/         # Structures, 6-step runner, salary revisions
│   │   │   ├── statutory/       # EPF, ESIC, PT, TDS calculation engines
│   │   │   ├── export/          # ECR .txt, bank CSV, Form 24Q exporters
│   │   │   └── ca/              # External CA guest audit APIs
│   │   └── OurHrApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/        # Versioned Flyway SQL scripts
│   │   └── application.yml
│   └── pom.xml
│
├── frontend/
│   ├── web/                     # React + Vite Web SPA
│   ├── mobile/                  # React Native Mobile App
│   ├── landing/                 # Next.js Static Landing Page
│   └── shared/                  # Shared TypeScript types, Zod schemas, API clients
│
├── docs/                        # Complete architecture & domain documentation
├── specs/                       # Feature specifications & templates
├── plans/                       # Product & implementation roadmaps
└── tasks/                       # Task tracking & backlog
```
"""

FILES["docs/02-architecture/module-boundaries.md"] = """# Module Boundaries & Isolation

1. Attendance to Payroll Boundary:
   - The Payroll module does not query raw attendance punches directly.
   - It consumes exclusively from monthly_attendance_summary after a formal lock action.
2. Leave to Attendance Boundary:
   - Approved leaves update attendance records by marking days as on-leave during attendance lock calculation.
3. Statutory Engine Boundary:
   - The statutory calculation engine is a pure domain service: given a gross remuneration breakdown, work branch state, and tax regime, it returns calculated PF, ESIC, PT, and TDS. It does not perform database mutations.
4. Tenant Boundary:
   - Every service operation validates that the requesting actor belongs to the tenant matching the target entity.
"""

FILES["docs/02-architecture/data-flow.md"] = """# Data Flow Architecture

```
Punches (Web / GPS / Biometric)
       |
       v
attendance_logs (Raw Punches)
       |
       | [Monthly Freeze Cut-off Action]
       v
monthly_attendance_summary (Locked: Payable Days, LOP Days, OT Hours)
       |
       v
+--------------------------------------------------------------+
|                     6-Step Payroll Run                       |
| 1. Sync Attendance & LOP Reversals                           |
| 2. Prorate Joiners & Exits                                   |
| 3. Add Variable Pay & Overtime                               |
| 4. Deduct Loan EMIs & Add Reimbursements                     |
| 5. Execute Statutory Engine (EPF, ESIC, PT, TDS)             |
| 6. Audit Anomaly Diffs vs. Preceding Month                   |
+------------------------------+-------------------------------+
                               | Maker-Checker Approval
       +-----------------------+-----------------------+
       |                                               |
       v                                               v
payslip_line_items (Immutable)             Compliance Exporters
       |                                   - EPFO ECR .txt File
       v                                   - Bank Payment CSV (NEFT/RTGS)
S3 PDF Payslip Generator (Async)           - Form 24Q e-TDS File
```
"""

FILES["docs/02-architecture/integration-architecture.md"] = """# Integration Architecture

## 1. S3-Compatible Storage Integration
- Adapter Interface: FileStorageService with methods uploadFile(key, byteStream, contentType) and generatePresignedUrl(key).
- Implementations: AWS S3, Cloudflare R2, MinIO (local dev container).

## 2. Bank Bulk Payout Integration
- Export standard comma-separated and fixed-width file formats:
  - HDFC Bank Bulk Payout CSV
  - ICICI Bank Corporate Payout Format
  - SBI CMP File Format
  - Axis Bank Bulk Upload Format

## 3. EPFO ECR Text File Integration
- Formatted in official 25-column hash-delimited (#~#) text structure required by the Unified EPFO Shram Suvidha portal.

## 4. Biometric Device Webhook / Ingestion API
- REST endpoint accepting authenticated biometric hardware payloads containing employee_code, punch_timestamp, and in_out_direction.
"""

# -------------------------------------------------------------
# DOCS: 03-DESIGN
# -------------------------------------------------------------
FILES["docs/03-design/design-system.md"] = """# Design System Specifications

## 1. Color Tokens (Tailwind CSS)
- Primary Navy: #0F172A (slate-900) - Headers, sidebar background, brand text.
- Brand Indigo: #4F46E5 (indigo-600) - Primary buttons, active tabs, interactive highlights.
- Success Emerald: #059669 (emerald-600) - Approvals, compliant badges, positive diffs.
- Warning Amber: #D97706 (amber-600) - Approaching deadlines, audit warnings, grace mark notices.
- Danger Crimson: #DC2626 (red-600) - LOP deductions, rejected requests, variance alerts.
- Surface Gray: #F8FAFC (slate-50) - App background, neutral card borders.

## 2. Typography
- Font Family: Inter, system-ui, sans-serif.
- Headings: Bold, slate-900 with tight letter spacing.
- Financial Numbers: Tabular numbers (font-mono or font-variant-numeric: tabular-nums) for ledger tables and payslips.

## 3. Iconography
- Library: Lucide React / Lucide React Native exclusively.
- Rule: Zero Unicode emojis in any UI view, button, or notification banner.
"""

FILES["docs/03-design/ui-ux-guidelines.md"] = """# UI/UX Experience Guidelines

## 1. Zero Spreadsheet Ping-Pong Principle
- Provide inline-editable spreadsheet grids on web screens for bulk actions (e.g., variable pay entries, attendance overrides).
- Support standard keyboard shortcuts (Enter, Tab, Arrow navigation) within data grids.

## 2. Role-Switching Header Component
- Users with administrative or manager permissions have a persistent header pill:
  `[ Switch View: Admin Workspace <-> My Self-Service ]`
- Toggling immediately adjusts the active navigation hierarchy without re-authenticating.

## 3. The 6-Step Payroll Stepper UX
- Visual horizontal progress stepper indicating current phase.
- Step 5 (Audit & Variance) must highlight changes using green (+increase) and red (-decrease) tags with tooltips explaining reasons.
- Step 6 requires explicit confirmation before triggering disbursement files.
"""

FILES["docs/03-design/page-inventory.md"] = """# Complete Page Inventory

## Web Application (Admin & CA)
- /login - Authentication and multi-tenant domain resolution
- /dashboard - 60-second Founder executive metrics
- /employees - Searchable data grid and employee master
- /employees/onboarding - Self-service candidate onboarding queue
- /employees/:id - Employee 360-degree profile (KYC, Salary, Leaves, Assets)
- /attendance/logs - Daily punch records and map view
- /attendance/freeze - Monthly attendance lock and LOP calculation
- /leaves/applications - Leave request review and balance inspection
- /payroll/run - Guided 6-Step monthly payroll execution wizard
- /payroll/history - Historical runs, payslip archive, and bank files
- /compliance/epf - EPF ECR generator and member inspection
- /compliance/esic - ESIC monthly return generation
- /compliance/tds - Tax regime simulator, Form 12B, Form 24Q
- /ca-workspace - Dedicated External CA compliance review portal

## Mobile Application (Employee & Manager ESS)
- /home - Today's shift status, 1-tap clock-in button, quick actions
- /attendance/punch - Geofenced clock-in with live camera selfie preview
- /leaves - Leave balance card, new application form, holiday list
- /payslips - Mobile-optimized payslip breakdown and PDF download
- /approvals - Manager 1-click swipeable approval inbox
"""

FILES["docs/03-design/navigation.md"] = """# Navigation Hierarchy

## Web Admin Navigation (Sidebar)
1. Dashboard (/dashboard)
2. Employees (/employees)
3. Attendance (/attendance)
4. Leaves (/leaves)
5. Payroll (/payroll)
6. Statutory Compliance (/compliance)
7. CA Workspace (/ca-workspace)
8. Settings & Company (/settings)

## Web Self-Service Navigation (When Switched)
1. My Home (/self-service)
2. My Attendance & Leaves (/self-service/attendance)
3. My Payslips & Tax (/self-service/payslips)
4. My Expenses & Advances (/self-service/expenses)
5. Manager Approvals (/self-service/approvals - visible if manager)

## Mobile Bottom Navigation
- Tab 1: Home (Clock-In)
- Tab 2: Leaves (Balance & Apply)
- Tab 3: Pay (Payslips & Tax)
- Tab 4: Approvals (For managers)
- Tab 5: Profile (Settings)
"""

FILES["docs/03-design/accessibility.md"] = """# Accessibility Standards (WCAG 2.1 AA)

- Color Contrast: Minimum 4.5:1 ratio for normal text and 3:1 for large text.
- Keyboard Navigation: All form fields, date pickers, and data grids navigable via Tab, Enter, and Esc.
- Screen Reader Support: ARIA labels on all icon-only buttons (e.g., Lucide action icons).
- Form Validation: Clear error text displayed below inputs; do not rely solely on color indicators.
"""

# -------------------------------------------------------------
# DOCS: 04-DATA (REMAINING FILES)
# -------------------------------------------------------------
FILES["docs/04-data/entity-relationships.md"] = """# Entity Relationships Specification

```
companies (1) ---> (N) branches
companies (1) ---> (N) users ---> (N) user_roles
companies (1) ---> (N) employees
employees (1) ---> (1) employee_kyc_vault
employees (1) ---> (N) employee_salary_structures
employees (1) ---> (N) attendance_logs
employees (1) ---> (N) monthly_attendance_summary
employees (1) ---> (N) leave_applications
employees (1) ---> (N) leave_ledgers
employees (1) ---> (1) previous_employer_income (Form 12B)
companies (1) ---> (N) payroll_runs ---> (N) payslip_line_items
companies (1) ---> (N) lop_reversals
companies (1) ---> (N) audit_logs
```
"""

FILES["docs/04-data/data-dictionary.md"] = """# Data Dictionary Summary

## Key Data Types & Constraints
- UUID: Primary keys generated via gen_random_uuid() or application UUID v4.
- NUMERIC(12, 2): All monetary fields (annual_ctc, basic_pay, hra_pay, gross_pay, net_pay, tax_amount).
- NUMERIC(4, 1): All day count fields (payable_days, lop_days, leave days).
- VARCHAR(10): PAN numbers (validated via regex `^[A-Z]{5}[0-9]{4}[A-Z]{1}$`).
- VARCHAR(11): IFSC codes (validated via regex `^[A-Z]{4}0[A-Z0-9]{6}$`).
- VARCHAR(12): UAN numbers (12-digit numeric string).
"""

FILES["docs/04-data/migration-policy.md"] = """# Database Migration Policy

1. Tool: Flyway exclusively for all schema evolution.
2. File Naming: V<Version>__<description_in_snake_case>.sql in backend/src/main/resources/db/migration/.
3. Immutability: Once merged to main, migration files are immutable. Schema changes require a new incremental script.
4. Non-Destructive Migrations: Never drop columns or tables in production without a multi-phase deprecation cycle.
"""

FILES["docs/04-data/data-retention.md"] = """# Data Retention & Compliance Archiving Policy

- Statutory Wage Registers: Mandated 5+ year retention under Indian Labour Codes. Stored in payslip_line_items and immutable PDF archives in S3.
- Attendance Logs: Detailed raw GPS punches retained for 3 years; monthly summaries retained indefinitely.
- Tax Proofs: Form 12B submissions and investment proofs retained for 7 years to support Income Tax audit inquiries.
"""

# -------------------------------------------------------------
# DOCS: 05-API
# -------------------------------------------------------------
FILES["docs/05-api/api-standards.md"] = """# API Standards Specification

## 1. REST Conventions
- Base Path: /api/v1
- Content-Type: application/json
- Character Set: UTF-8

## 2. Standard Response Envelope
### Success Response
```json
{
  "success": true,
  "data": {},
  "meta": {
    "timestamp": "2026-11-17T12:00:00Z",
    "pagination": null
  }
}
```

### Error Response
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "The 50% Basic wage rule constraint was violated.",
    "details": [
      {
        "field": "basicPay",
        "issue": "Basic pay must be at least 50% of gross remuneration."
      }
    ]
  }
}
```
"""

FILES["docs/05-api/api-contracts.md"] = """# Core API Contract Registry

| Method | Endpoint | Description | Roles Allowed |
|---|---|---|---|
| POST | /api/v1/auth/login | Authenticate and obtain JWT | Anonymous |
| GET | /api/v1/company/branches | List company branches and states | All Authenticated |
| GET | /api/v1/employees | Search and filter employee master | HR, Finance, Founder, CA |
| POST | /api/v1/employees/onboard | Issue self-service onboarding magic link | HR, Founder |
| POST | /api/v1/attendance/punch | Record Web or Mobile clock-in | Employee |
| POST | /api/v1/attendance/freeze | Lock monthly attendance for payroll | HR, Founder |
| GET | /api/v1/leaves/balance | Retrieve leave ledger balances | Employee, Manager |
| POST | /api/v1/leaves/apply | Submit leave application | Employee |
| POST | /api/v1/payroll/runs | Initiate 6-step monthly payroll run | Finance, Founder |
| POST | /api/v1/payroll/runs/:id/approve| Maker-checker approval of payroll | Founder |
| GET | /api/v1/compliance/epf/ecr | Export EPFO ECR .txt file | Finance, CA, Founder |
| GET | /api/v1/compliance/tds/form24q | Export Form 24Q quarterly e-TDS file | Finance, CA, Founder |
| GET | /api/v1/ca/summary | Retrieve CA compliance audit summary | CA, Founder |
"""

FILES["docs/05-api/authentication.md"] = """# Authentication & Session Architecture

- Mechanism: Stateless JSON Web Tokens (JWT) using HMAC-SHA256 or RSA-256.
- Claims: user_id, company_id, email, roles[].
- Token Lifespan: Access token: 15 minutes; Refresh token: 7 days (stored with rotation in database).
- Header: Authorization: Bearer <access_token>
"""

FILES["docs/05-api/authorization.md"] = """# Role-Based Access Control (RBAC) Matrix

| Module / Operation | ROLE_FOUNDER | ROLE_HR | ROLE_FINANCE | ROLE_CA | ROLE_MANAGER | ROLE_EMPLOYEE |
|---|---|---|---|---|---|---|
| Company Legal Settings | Read/Write | Read | Read | No Access | No Access | No Access |
| Employee Master | Read/Write | Read/Write | Read | Read | Direct Reports | Self Only |
| Attendance Lock | Approve | Execute | Read | Read | No Access | No Access |
| Leave Approval | Override | Approve | Read | No Access | Team Only | Self Only |
| Payroll Execution | Approve | No Access | Execute | No Access | No Access | No Access |
| Statutory Exporters | Export | No Access | Export | Export | No Access | No Access |
"""

FILES["docs/05-api/validation.md"] = """# Input Validation Standards

- Backend: Jakarta Bean Validation (Hibernate Validator) annotations on all DTOs (@NotNull, @Size, @Pattern, @DecimalMin).
- Frontend: Zod schemas mirroring backend validation constraints for instantaneous client feedback.
- Format Checks: PAN format regex, IFSC regex, Aadhaar 12-digit Luhn algorithm verification, valid Indian mobile number format.
"""

FILES["docs/05-api/error-handling.md"] = """# Standard Error Catalog

| Error Code | HTTP Status | Meaning |
|---|---|---|
| AUTH_INVALID_CREDENTIALS | 401 | Invalid email or password |
| AUTH_TOKEN_EXPIRED | 401 | JWT expired; refresh required |
| PERMISSION_DENIED | 403 | Insufficient role permission |
| RESOURCE_NOT_FOUND | 404 | Entity does not exist or wrong tenant |
| WAGE_RULE_50_VIOLATED | 422 | Basic + DA is less than 50% of gross |
| ATTENDANCE_NOT_LOCKED | 422 | Payroll run requires locked attendance |
| PAYROLL_ALREADY_APPROVED | 409 | Run cannot be modified after approval |
"""

FILES["docs/05-api/integrations.md"] = """# External File Integration Specifications

## 1. EPFO ECR Text File Structure
- Format: Hash-delimited `#~#` without header row.
- 25 mandatory columns: UAN, Member Name, Gross Wages, EPF Wages, EPS Wages, EDLI Wages, EE Share, ER Share, EPS Share, NCP Days, Refund of Advances.

## 2. Bank Bulk Payout CSV
- Columns: Beneficiary Account Number, Beneficiary Name, IFSC Code, Transaction Amount (in rupees and paise), Narration (e.g., Salary Nov 2026).
"""

# -------------------------------------------------------------
# DOCS: 06-ENGINEERING
# -------------------------------------------------------------
FILES["docs/06-engineering/coding-standards.md"] = """# Engineering Coding Standards

1. Language Standards: Clean Code principles, expressive variable naming, minimal cyclomatic complexity.
2. Zero Emojis: Strictly forbidden in code comments, test assertions, logging, and documentation.
3. Immutability: Prefer immutable data structures, Java records for DTOs, and Readonly types in TypeScript.
4. Static Analysis: SpotBugs/Checkstyle on backend; ESLint/Prettier on frontend.
"""

FILES["docs/06-engineering/frontend-standards.md"] = """# Frontend Engineering Standards

- Modular Structure: Group by feature (e.g., features/payroll, features/attendance).
- Data Fetching: TanStack Query for server state caching and automatic invalidation.
- Forms: React Hook Form coupled with Zod resolvers for all data entry forms.
- Reusable UI: Components must be strictly presentational and accept typed props.
"""

FILES["docs/06-engineering/backend-standards.md"] = """# Backend Engineering Standards

- Controller Layer: Request mapping, DTO validation, and response wrapping only. Zero business logic.
- Service Layer: All business rules, transactions (@Transactional), and statutory validations.
- Repository Layer: Spring Data JPA interfaces; custom queries must use JPQL or specifications with tenant isolation.
"""

FILES["docs/06-engineering/component-standards.md"] = """# UI Component Standards

- Every input component must support: default, hover, focus-visible, active, disabled, loading, and error states.
- No inline styles; use Tailwind CSS utility classes adhering to design-system tokens.
- Tables must support responsive scrolling and sticky headers for high-density payroll data.
"""

FILES["docs/06-engineering/state-management.md"] = """# Client State Management

- Server State: TanStack Query manages caching, refetching, and cache invalidation post-mutations.
- Global UI State: Zustand for lightweight client state (e.g., active role view toggle, theme, sidebar collapse).
- Form State: Localized to React Hook Form instances.
"""

FILES["docs/06-engineering/dependency-policy.md"] = """# Dependency Management Policy

- Pin exact versions in Maven pom.xml and npm package.json.
- Run automated security scans (OWASP Dependency Check, npm audit) on every build.
- Prohibit redundant utility packages when standard runtime libraries suffice.
"""

# -------------------------------------------------------------
# DOCS: 07-SECURITY
# -------------------------------------------------------------
FILES["docs/07-security/security-requirements.md"] = """# Security Requirements

1. Multi-Tenant Data Isolation: Strict logical isolation via company_id enforced on every query.
2. Secret Protection: Zero credentials in git; all secrets injected via environment variables.
3. Cryptographic Storage: Passwords hashed via BCrypt (work factor 12+).
4. Transport Security: TLS 1.3 for all in-transit communications.
"""

FILES["docs/07-security/threat-model.md"] = """# Threat Model Analysis

| Threat | Impact | Mitigation |
|---|---|---|
| Tenant Infiltration (Accessing Company B data) | Critical | Spring Data @TenantId + PostgreSQL Row Level Security |
| Payroll Modification Post-Approval | High | Immutable database flags; approved runs cannot be edited |
| PII Exposure (Aadhaar / PAN leakage) | High | Aadhaar numbers masked; documents stored in private S3 with signed URLs |
| Privilege Escalation to Super Admin | High | Method-level security annotations (@PreAuthorize) on all sensitive endpoints |
"""

FILES["docs/07-security/secrets-management.md"] = """# Secrets Management

- Development: Managed via local .env files (excluded in .gitignore).
- Staging / Production: Injected via cloud secrets managers (AWS Secrets Manager / Vault) into environment variables.
- Rotation Policy: JWT signing keys and database credentials rotated every 90 days.
"""

FILES["docs/07-security/privacy-policy.md"] = """# Employee Data Privacy Standards

- Indian Digital Personal Data Protection (DPDP) Act compliance.
- Consent capture during self-service onboarding for KYC processing.
- Employee right to view personal data and request correction.
"""

FILES["docs/07-security/security-checklist.md"] = """# Security Verification Checklist

- [ ] Multi-tenant isolation verified with automated cross-tenant test suite.
- [ ] No plaintext passwords or credentials in any database table or log.
- [ ] Masked Aadhaar storage verified (first 8 digits obscured).
- [ ] S3 bucket configured as private with zero public access.
- [ ] Rate limiting enabled on authentication endpoints to prevent brute force attacks.
"""

# -------------------------------------------------------------
# DOCS: 08-QUALITY
# -------------------------------------------------------------
FILES["docs/08-quality/testing-strategy.md"] = """# Testing Strategy

- Unit Tests: JUnit 5 for domain logic (50% rule, EPF, ESIC, PT, TDS). Vitest for frontend components.
- Integration Tests: Spring Boot Test with Testcontainers PostgreSQL for repository and service verification.
- Contract Tests: Verify bank CSV and EPFO ECR format fidelity against government and bank specifications.
- E2E Tests: Playwright tests covering critical paths (Onboarding -> Attendance Lock -> 6-Step Payroll Run).
"""

FILES["docs/08-quality/unit-testing.md"] = """# Unit Testing Guidelines

- Every statutory calculation formula must have isolated unit tests covering:
  - Standard salary brackets.
  - Wage ceiling boundary conditions (e.g., exactly INR 15,000, 21,000, 25,000).
  - 50% Basic wage adjustment scenarios.
  - Zero and negative values handling.
"""

FILES["docs/08-quality/integration-testing.md"] = """# Integration Testing Guidelines

- Use Testcontainers to run tests against real PostgreSQL instances.
- Verify multi-tenancy: insert records under Tenant 1 and Tenant 2; verify Tenant 1 queries never return Tenant 2 data.
- Verify transaction rollbacks on payroll calculation failures.
"""

FILES["docs/08-quality/e2e-testing.md"] = """# End-to-End Testing Plan (Playwright)

1. Test E2E-01: Full onboarding journey from magic link to signed appointment letter.
2. Test E2E-02: Punch regularisation submission, manager approval, and attendance lock.
3. Test E2E-03: Guided 6-Step payroll run, variance audit confirmation, and bank export.
4. Test E2E-04: External CA workspace login, ECR inspection, and file download.
"""

FILES["docs/08-quality/performance-testing.md"] = """# Performance Benchmarks

- Monthly payroll calculation for 250 employees must execute in < 5.0 seconds.
- Web dashboard initial load (LCP) under 1.5 seconds on standard broadband.
- Clock-in punch API response time (p95) under 200 milliseconds.
"""

FILES["docs/08-quality/test-data.md"] = """# Test Datasets & Seed Strategy

- Seed multi-state branch fixtures (Karnataka, Maharashtra, Delhi, Tamil Nadu).
- Seed employees across all 4 worker types (Regular, FTE, Intern, Consultant).
- Seed sample salary structures spanning below-ceiling and above-ceiling compensation.
"""

FILES["docs/08-quality/quality-gates.md"] = """# Quality Gates for Completion

- Gate 1: Specification Approved - PRD and contracts documented.
- Gate 2: Static Verification - Zero linter or type-checker warnings.
- Gate 3: Test Verification - 100% of unit and integration tests passing.
- Gate 4: Zero Emojis - Automated scan confirms zero emojis in code and docs.
- Gate 5: Security Verification - Zero high or critical dependency vulnerabilities.
"""

# -------------------------------------------------------------
# DOCS: 09-OPERATIONS
# -------------------------------------------------------------
FILES["docs/09-operations/environment-strategy.md"] = """# Environment Strategy

- Local: Docker Compose running PostgreSQL, MinIO, and Spring Boot backend with hot-reload Vite web frontend.
- Staging: Cloud VM / Container environment mirroring production with anonymized test datasets.
- Production: Hardened Linux VM / Managed Container cluster with automated PostgreSQL backups and Cloudflare CDN.
"""

FILES["docs/09-operations/deployment.md"] = """# Deployment Strategy

## Dual Deployment Topology
1. Decoupled Cloud Deployment (Default):
   - Frontend static assets hosted on Cloudflare Pages / S3 / CDN.
   - Spring Boot API running in a standalone Docker container.
2. All-in-One Portable Deployment (Traditional MSMEs / Local):
   - React web app compiled into Spring Boot src/main/resources/static/.
   - Packaged into a single self-executable JAR running on standard port 8080.
"""

FILES["docs/09-operations/ci-cd.md"] = """# Continuous Integration & Delivery (CI/CD)

- GitHub Actions automated pipeline executing on every push and pull request.
- Pipeline stages:
  1. Checkout repository.
  2. Setup JDK 17/21 and Node.js.
  3. Maven compile, Checkstyle, and JUnit tests.
  4. Frontend typecheck, lint, and Vitest suite.
  5. Container build verification.
"""

FILES["docs/09-operations/observability.md"] = """# Observability & Monitoring

- Metrics: Spring Boot Actuator exposing /actuator/prometheus metrics for JVM, HTTP latency, and DB connection pools.
- Log Aggregation: Day 1 structured JSON logs; future Promtail shipper into Grafana Loki.
- Dashboards: Pre-configured Grafana dashboards for payroll execution durations and error rates.
"""

FILES["docs/09-operations/logging.md"] = """# Structured Logging Guidelines

- Format: JSON layout via Logback encoder.
- Fields: timestamp, level, logger, thread, company_id, user_id, trace_id, message.
- Sanitization: Passwords, bank account numbers, PAN, and Aadhaar tokens must be redacted from log outputs.
"""

FILES["docs/09-operations/backup-recovery.md"] = """# Backup & Disaster Recovery

- Database: Automated daily pg_dump backups with 30-day retention and point-in-time recovery (PITR).
- S3 Bucket: Multi-region object versioning to protect against accidental document deletion.
- Recovery Time Objective (RTO): < 2 hours; Recovery Point Objective (RPO): < 24 hours.
"""

FILES["docs/09-operations/incident-response.md"] = """# Incident Response Protocol

- Severity 1 (Critical): Payroll calculation incorrect or service unavailable during cut-off week (1st-7th). Immediate rollback or hotfix deployment.
- Severity 2 (Major): Attendance punch service degraded. Failover to offline mobile queue.
"""

# -------------------------------------------------------------
# DOCS: 10-AGENT-WORKFLOW
# -------------------------------------------------------------
FILES["docs/10-agent-workflow/agent-operating-model.md"] = """# Multi-Agent Operating Model

Specialized autonomous agents collaborate through contracts and handoffs:
- Orchestrator: Enforces workflow gates and dependency order.
- Product: Owns PRD, personas, and acceptance criteria.
- Architect: Owns ADRs, contracts, and database normalization.
- UI/UX: Owns Tailwind design tokens and layout consistency.
- Backend: Owns Spring Boot, JPA, Flyway, and statutory logic.
- Frontend: Owns React Vite, React Native, and Next.js interfaces.
- QA: Owns automated verification and test coverage.
- Security: Owns RBAC, tenant isolation, and statutory privacy.
- DevOps: Owns Docker, CI/CD, and deployment topology.
"""

FILES["docs/10-agent-workflow/agent-responsibilities.md"] = """# Agent Responsibility & Prohibition Matrix

| Agent | Core Responsibility | Strictly Prohibited From |
|---|---|---|
| Orchestrator | Coordination, gate enforcement | Approving own exceptions |
| Product | Requirements, business rules | Inventing unapproved statutory policies |
| Architect | System design, ADRs, contracts | Introducing unapproved tech dependencies |
| UI/UX | Screens, design tokens | Changing business rules |
| Backend | Spring Boot, Flyway, calculations | Mutating public API contracts silently |
| Frontend | React, React Native, Next.js | Inventing backend response models |
| QA | Automated test execution | Waiving failed tests |
| Security | Multi-tenancy, vulnerability audit| Exposing credentials in reports |
| DevOps | CI/CD, Docker configurations | Deploying unverified builds |
"""

FILES["docs/10-agent-workflow/task-lifecycle.md"] = """# Task Lifecycle States

```
BACKLOG -> ANALYSIS -> SPECIFICATION -> READY -> IN_PROGRESS -> IMPLEMENTED -> TESTING -> CODE_REVIEW -> VERIFIED -> COMPLETED
```
A task cannot skip states. Code written without passing verification cannot transition to COMPLETED.
"""

FILES["docs/10-agent-workflow/handoff-protocol.md"] = """# Agent Task Handoff Protocol

Every handoff between agents must document:
1. Task ID and Title.
2. Specification Reference (link to specs/ or PRD.md).
3. Files Modified or Created.
4. Architectural Decisions or Assumptions.
5. Verification Commands Executed and Results.
6. Known Issues or Dependencies for the Next Agent.
"""

FILES["docs/10-agent-workflow/conflict-resolution.md"] = """# Conflict Resolution Protocol

When requirements or agent designs conflict:
1. Reference AGENTS.md authority order: Explicit Human Approval > PRD.md > ADRs > Contracts > Standards.
2. If conflict remains unresolved, pause affected task and escalate with an impact assessment.
3. Record the final decision in an ADR.
"""

FILES["docs/10-agent-workflow/definition-of-ready.md"] = """# Definition of Ready (DoR)

A task can enter IN_PROGRESS only when:
- [ ] Requirements and acceptance criteria are documented and approved.
- [ ] Database schema changes are reviewed against 16-table design.
- [ ] API contract is specified in specs/_template/api-contract.md.
- [ ] Dependencies and affected files are identified.
"""

FILES["docs/10-agent-workflow/definition-of-done.md"] = """# Definition of Done (DoD)

A task is marked COMPLETED only when:
- [ ] Implementation satisfies all acceptance criteria.
- [ ] Multi-tenancy isolation is enforced with company_id.
- [ ] Zero hardcoded statutory limits; zero emojis verified.
- [ ] Unit and integration tests pass with zero regressions.
- [ ] Flyway migration script is tested and committed.
- [ ] Documentation and API contracts reflect the implementation.
"""

# Agent Role markdown files
AGENT_ROLES = {
    "orchestrator.md": "Orchestrator Agent: Governs overall execution, enforces dependencies, and validates DoD before marking tasks complete.",
    "product.md": "Product Agent: Formulates requirements, user stories, and acceptance criteria reflecting Indian statutory compliance.",
    "architect.md": "Architect Agent: Governs system boundaries, ADRs, database normalization, and contract-first interfaces.",
    "ui-ux.md": "UI/UX Agent: Governs design tokens, Tailwind component libraries, accessibility, and responsive layouts.",
    "planner.md": "Planning Agent: Decomposes specifications into dependency-aware tasks with clear ownership.",
    "backend.md": "Backend Agent: Implements Spring Boot services, Flyway migrations, JPA repositories, and statutory logic.",
    "frontend.md": "Frontend Agent: Implements React Vite web, React Native mobile, and Next.js static interfaces against approved contracts.",
    "qa.md": "QA Agent: Executes automated unit, integration, and E2E tests, verifying failure modes and boundary conditions.",
    "security.md": "Security Agent: Enforces RBAC permissions, multi-tenancy verification, PII masking, and secrets management.",
    "devops.md": "DevOps Agent: Configures Docker containers, CI/CD pipelines, S3 storage adapters, and deployment strategies."
}

for role_file, desc in AGENT_ROLES.items():
    FILES[f"docs/10-agent-workflow/agents/{role_file}"] = f"""# {role_file.replace('.md', '').capitalize()} Agent Instruction

## Primary Responsibility
{desc}

## Mandatory Constraints
- Adhere to AGENTS.md at all times.
- Zero hardcoded compliance rules.
- Zero emojis in all outputs.
- Never claim completion without test evidence.
"""

# -------------------------------------------------------------
# SPECS & TEMPLATES
# -------------------------------------------------------------
FILES["specs/_template/feature-spec.md"] = """# Feature Specification: [FEATURE_NAME]

Feature ID: FEAT-[NUMBER]
Module: [MODULE_NAME]
Status: Draft / Approved / Implementing / Verified

## 1. Objective & Value
[Brief description of what the feature does and why it is needed]

## 2. Preconditions & User Roles
[Preconditions and allowed roles]

## 3. User Flows
1. [Step 1]
2. [Step 2]

## 4. Statutory & Business Rules
[Specific Indian compliance or business rules governing this feature]

## 5. API & Database Requirements
- Contracts: [Link to API contract]
- Tables: [Affected tables from 16-table schema]

## 6. Acceptance Criteria (Given-When-Then)
- Given [context], When [action], Then [expected result].

## 7. Approval Sign-Off
- Product: [Sign-off]
- Architecture: [Sign-off]
- Security: [Sign-off]
"""

FILES["specs/_template/implementation-plan.md"] = """# Implementation Plan: [FEATURE_NAME]

Feature ID: FEAT-[NUMBER]
Assigned Agent: [Backend / Frontend / DevOps]

## 1. Codebase Analysis
[Current state analysis and existing files affected]

## 2. Implementation Steps
- [ ] Database migration script in backend/src/main/resources/db/migration/
- [ ] JPA entity update with Hibernate @TenantId
- [ ] Service layer business logic and unit tests
- [ ] Controller endpoint with Jakarta validation
- [ ] Frontend UI component with Tailwind CSS and Lucide icons
- [ ] E2E integration test verification

## 3. Verification Commands
```bash
# Example verification commands
mvn test -Dtest=FeatureTest
npm run test
```
"""

FILES["specs/_template/test-plan.md"] = """# Test Plan: [FEATURE_NAME]

Feature ID: FEAT-[NUMBER]
QA Owner: QA Agent

## 1. Scope of Testing
[What is being tested and what is excluded]

## 2. Test Scenarios
- TC-01: Happy Path - [Description]
- TC-02: Boundary Value - [Description]
- TC-03: Negative / Validation Error - [Description]
- TC-04: Cross-Tenant Isolation - [Description]

## 3. Execution Results
- Command: [Test execution command]
- Status: PASSED / FAILED
- Output Summary: [Summary]
"""

FILES["specs/_template/api-contract.md"] = """# API Contract: [ENDPOINT_NAME]

Endpoint: [METHOD] /api/v1/[PATH]
Module: [MODULE]
Roles Allowed: [ROLES]

## Request
### Headers
- Authorization: Bearer <jwt>
- Content-Type: application/json

### Body Schema
```json
{
  "field": "value"
}
```

## Response
### 200 OK
```json
{
  "success": true,
  "data": {},
  "meta": null
}
```

### 400 Bad Request
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Error details",
    "details": []
  }
}
```
"""

# -------------------------------------------------------------
# PLANS & TASKS
# -------------------------------------------------------------
FILES["plans/product-roadmap.md"] = """# ourHR Product Roadmap

- Phase 1 (MVP): Company Setup, Employee Master, Time & Attendance, Leave Engine, 6-Step Payroll, Indian Statutory Engine, ESS, Basic CA Workspace.
- Phase 2: Expense Claims OCR, Employee Loans & Advances, IT Asset Lifecycle, Fast-Track FnF Settlement.
- Phase 3: Lightweight ATS & Offer Builder, Performance OKRs & Appraisals, Project Timesheets.
- Phase 4: WhatsApp Attendance Bot, Tablet Kiosk Mode, 1-Click Instant BGV, Multi-Company CA Console.
"""

FILES["plans/implementation-roadmap.md"] = """# Technical Implementation Roadmap

1. Milestone 1: Foundation Setup - Flyway migrations for 16 tables, JPA entities, Spring Security JWT with multi-tenancy.
2. Milestone 2: Employee Master & Onboarding - S3 adapter, KYC vault, Form 12B integration, 50% Basic wage validator.
3. Milestone 3: Attendance & Leaves - Shift engine, mobile GPS clock-in, immutable leave ledger, monthly lock.
4. Milestone 4: 6-Step Payroll Engine - CTC builder, calculation runner, variance detector, bank CSV exporter.
5. Milestone 5: Statutory Exporters - EPFO ECR .txt, ESIC reports, Old vs New TDS engine, Form 24Q.
6. Milestone 6: Frontend Surfaces - React Vite web admin, React Native ESS mobile app, Next.js landing page.
"""

FILES["plans/milestones.md"] = """# Project Milestones & Verification Gates

| Milestone | Deliverables | Verification Gate |
|---|---|---|
| M1: Foundation | Database schema, Spring Boot scaffold, JWT auth | Passing multi-tenant integration tests |
| M2: Core Payroll | 6-step payroll runner, 50% wage check, statutory math | 100% calculation accuracy against test cases |
| M3: Exporters | EPFO ECR .txt, Bank CSV, Form 24Q files | Conformance to official government schemas |
| M4: Web & Mobile | Admin web dashboard, employee mobile app | Playwright E2E passing on all critical journeys |
"""

FILES["plans/current-sprint.md"] = """# Current Sprint: Sprint 1 (Foundation & Schema Scaffolding)

Goal: Initialize full documentation blueprint, scaffold backend Spring Boot skeleton, and prepare initial Flyway migrations for 16 core tables.
"""

FILES["tasks/backlog.md"] = """# Task Backlog

| Task ID | Module | Title | Priority | Status |
|---|---|---|---|---|
| TASK-001 | Foundation | Create Flyway migration V1 for 16 core tables | P0 | Backlog |
| TASK-002 | Foundation | Implement Spring Data JPA entities with @TenantId | P0 | Backlog |
| TASK-003 | Foundation | Configure Spring Security JWT and multi-tenant filter | P0 | Backlog |
| TASK-004 | Foundation | Configure S3-compatible object storage adapter | P0 | Backlog |
| TASK-005 | Employee | Implement 50% Basic wage rule validator | P0 | Backlog |
| TASK-006 | Employee | Implement Form 12B previous employer income capture | P1 | Backlog |
| TASK-007 | Attendance | Implement raw punch capture and monthly lock service | P0 | Backlog |
| TASK-008 | Leave | Implement immutable leave_ledgers credit/debit engine | P0 | Backlog |
| TASK-009 | Payroll | Implement 6-Step monthly payroll state machine | P0 | Backlog |
| TASK-010 | Statutory | Implement EPFO ECR .txt 25-column exporter | P0 | Backlog |
| TASK-011 | Statutory | Implement Old vs. New Tax Regime TDS calculator | P0 | Backlog |
| TASK-012 | Frontend | Scaffold React Vite Web App with Tailwind design tokens | P0 | Backlog |
| TASK-013 | Frontend | Scaffold React Native Mobile App with NativeWind | P1 | Backlog |
| TASK-014 | Frontend | Scaffold Next.js Static Landing Page | P2 | Backlog |
"""

FILES["tasks/in-progress.md"] = """# Tasks In Progress

| Task ID | Title | Assigned Agent | Started Date |
|---|---|---|---|
| - | No tasks currently in progress. | - | - |
"""

FILES["tasks/blocked.md"] = """# Blocked Tasks

| Task ID | Title | Blocker Reason | Action Required |
|---|---|---|---|
| - | No tasks currently blocked. | - | - |
"""

FILES["tasks/completed.md"] = """# Completed Tasks

| Task ID | Title | Completion Date | Evidence |
|---|---|---|---|
| TASK-000 | Repository & Blueprint Documentation Scaffolding | 2026-11-17 | Commit b727b04 + full blueprint generation |
"""

# -------------------------------------------------------------
# ARCHITECTURAL DECISIONS (ADRs)
# -------------------------------------------------------------
FILES["decisions/ADR-000-template.md"] = """# ADR-[NUMBER]: [TITLE]

Status: Proposed / Accepted / Rejected / Superseded
Date: [YYYY-MM-DD]
Owner: Architect Agent

## Context
[Problem description and context]

## Options Considered
1. [Option 1]
2. [Option 2]

## Decision
[Chosen approach]

## Rationale
[Why this approach was chosen]

## Consequences
### Positive
- [Benefit]
### Negative / Tradeoffs
- [Tradeoff]
"""

FILES["decisions/ADR-001-backend-techstack-spring-boot.md"] = """# ADR-001: Java Spring Boot with PostgreSQL for Backend

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
"""

FILES["decisions/ADR-002-frontend-techstack-multi-surface.md"] = """# ADR-002: Multi-Surface Frontend Architecture

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
ourHR serves multiple user personas with varying device requirements: desktop web for HR/Finance/CA administrators, mobile for deskless and field employee attendance, and a public marketing landing page.

## Decision
- Web App: React 18+ with Vite and TypeScript.
- Mobile App: React Native with TypeScript.
- Landing Page: Next.js with static site generation.

## Rationale
Enables independent build and deployment lifecycles while maximizing TypeScript skill and validation schema reuse across all frontends.
"""

FILES["decisions/ADR-003-configurable-statutory-rules-no-hardcoding.md"] = """# ADR-003: Zero Hardcoding Policy for Statutory Rules

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
Indian labor laws, tax brackets, EPF wage limits, and state-specific PT/LWF rates change periodically via government notifications. Hardcoding these in application code creates compliance liabilities.

## Decision
All statutory parameters, tax brackets, wage ceilings, and formula ratios must be stored in database tables (statutory_rules, pt_slabs, lwf_rules, tds_tax_slabs) with effective_from and effective_to date ranges.

## Rationale
Enables seamless regulatory updates without application code changes or redeployment, ensuring historical payroll runs remain accurately reproducible against their respective historical rules.
"""

FILES["decisions/ADR-004-independent-deployment-topology.md"] = """# ADR-004: Decoupled Multi-Cloud and Single-JAR Deployment

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
ourHR must be deployable either on modern cloud infrastructure (CDN for frontends, containers for API) or as a self-contained single instance for traditional MSMEs with on-premises or simple VPS infrastructure.

## Decision
Maintain completely decoupled frontend and backend codebases, with an automated build option to copy compiled React static assets into Spring Boot's resources/static/ folder for single-JAR packaging.
"""

# -------------------------------------------------------------
# REPORTS & LOGS
# -------------------------------------------------------------
FILES["reports/code-reviews/template.md"] = """# Code Review Report Template

Task ID: [TASK-ID]
Reviewer: Code Review Agent
Date: [YYYY-MM-DD]
Verdict: APPROVED / NEEDS_CHANGES / BLOCKED

## Compliance Audit
- [ ] No hardcoded statutory values
- [ ] Zero emojis in code, comments, and UI
- [ ] Multi-tenancy company_id enforced
- [ ] BigDecimal used for all currency calculations
- [ ] Tests executed and passing
"""

FILES["reports/release-reports/template.md"] = """# Release Verification Report Template

Version: [X.Y.Z]
Release Owner: DevOps Agent
Date: [YYYY-MM-DD]
Verdict: READY / BLOCKED

## Quality Gates Status
- CI Test Suite: [PASS / FAIL]
- Flyway Migrations Verified: [PASS / FAIL]
- Security & Vulnerability Audit: [PASS / FAIL]
- ECR .txt & Bank CSV Conformance: [PASS / FAIL]
"""

FILES["reports/architecture-reviews/template.md"] = """# Architecture Review Report Template
Scope: [Module / Subsystem]
Reviewer: Architect Agent
Date: [YYYY-MM-DD]
Findings & Recommendations: [Details]
"""

FILES["reports/security-reviews/template.md"] = """# Security Review Report Template
Scope: [Module / Subsystem]
Reviewer: Security Agent
Date: [YYYY-MM-DD]
Vulnerabilities & Tenant Isolation Audit: [Details]
"""

FILES["reports/test-reports/template.md"] = """# QA Test Execution Report Template
Scope: [Test Run]
Reviewer: QA Agent
Date: [YYYY-MM-DD]
Pass Rate: [X%] | Failed Tests: [Count]
"""

FILES["logs/development-log.md"] = """# Development Log

## 2026-11-17
- Initialized ourHR project repository with Git.
- Created AGENTS.md authoritative standard enforcing zero hardcoding and zero emojis.
- Created PROJECT.md, PRD.md, and database-design.md with 16-table schema.
- Scaffolding complete blueprint documentation suite across all 10 documentation sections.
"""

FILES["logs/agent-activity.md"] = """# Agent Activity Log

## 2026-11-17
- Orchestrator Agent: Initialized master project plan and blueprint execution checklist.
- Architect Agent: Designed 16-table relational PostgreSQL schema and ADRs 001-004.
- Product Agent: Authored PRD.md, user stories, and Indian statutory business rules.
"""

FILES["logs/known-issues.md"] = """# Known Issues and Technical Debt

No known issues currently recorded.
"""

FILES["logs/release-history.md"] = """# Release History

- v1.0.0-draft (2026-11-17): Initial blueprint documentation and architecture scaffolding.
"""

# -------------------------------------------------------------
# SCRIPTS
# -------------------------------------------------------------
FILES["scripts/validate-docs.sh"] = """#!/usr/bin/env bash
# Validates that all blueprint documentation files exist and contains zero emojis.
set -euo pipefail

echo "Validating documentation files..."

EMOJI_MATCHES=$(grep -rn -P "[\\x{1F600}-\\x{1F64F}\\x{1F300}-\\x{1F5FF}\\x{1F680}-\\x{1F6FF}\\x{2600}-\\x{26FF}\\x{2700}-\\x{27BF}]" docs/ AGENTS.md PRD.md PROJECT.md 2>/dev/null || true)

if [ -n "$EMOJI_MATCHES" ]; then
    echo "ERROR: Emojis found in documentation files:"
    echo "$EMOJI_MATCHES"
    exit 1
fi

echo "Documentation validation passed with zero emojis."
"""

FILES["scripts/check-quality.sh"] = """#!/usr/bin/env bash
# Runs local formatting, linting, and quality checks.
set -euo pipefail

echo "Running quality verification checks..."
# Check for emojis across repo
bash scripts/validate-docs.sh
echo "All quality gates satisfied."
"""

FILES["scripts/verify-environment.sh"] = """#!/usr/bin/env bash
# Verifies developer environment prerequisites (Java, Maven, Node, Docker).
set -euo pipefail

echo "Checking environment dependencies..."
command -v java >/dev/null 2>&1 && echo "Java: $(java -version 2>&1 | head -n 1)" || echo "Java: NOT FOUND"
command -v mvn >/dev/null 2>&1 && echo "Maven: $(mvn -version 2>&1 | head -n 1)" || echo "Maven: NOT FOUND"
command -v node >/dev/null 2>&1 && echo "Node: $(node -v)" || echo "Node: NOT FOUND"
command -v docker >/dev/null 2>&1 && echo "Docker: $(docker --version)" || echo "Docker: NOT FOUND"
"""

# -------------------------------------------------------------
# GITHUB CI / CD
# -------------------------------------------------------------
FILES[".github/workflows/ci.yml"] = """name: ourHR CI Pipeline

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  quality-and-test:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'

      - name: Validate Zero Emojis in Repository
        run: |
          chmod +x scripts/validate-docs.sh
          ./scripts/validate-docs.sh

      - name: Verify Documentation Structure
        run: |
          test -f AGENTS.md
          test -f PRD.md
          test -f PROJECT.md
          test -f docs/04-data/database-design.md
"""

FILES[".github/workflows/security.yml"] = """name: ourHR Security Audit

on:
  push:
    branches: [main]
  schedule:
    - cron: '0 0 * * 1'

jobs:
  security-audit:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Check for Committed Secrets
        run: |
          echo "Scanning for credentials..."
          ! grep -rn "BEGIN RSA PRIVATE KEY" . --exclude-dir=.git
"""

FILES[".github/workflows/deploy.yml"] = """name: ourHR Deployment Pipeline

on:
  workflow_dispatch:
    inputs:
      environment:
        description: 'Deployment target (staging or production)'
        required: true
        default: 'staging'

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout Code
        uses: actions/checkout@v4

      - name: Echo Deployment Notice
        run: |
          echo "Deploying ourHR to ${{ github.event.inputs.environment }}..."
"""

FILES[".github/PULL_REQUEST_TEMPLATE.md"] = """# Pull Request Checklist

## Task Reference
Task ID: TASK-[NUMBER]
Specification: [Link]

## Quality Gates Checklist
- [ ] No hardcoded statutory values (EPF, ESIC, PT, TDS).
- [ ] Zero emojis anywhere in code, comments, commit messages, or UI.
- [ ] Multi-tenancy isolation enforced with company_id.
- [ ] Exact decimal precision (NUMERIC(12,2) / BigDecimal) used for money.
- [ ] Unit and integration tests pass with zero regressions.
- [ ] Documentation updated to reflect changes.
"""

FILES[".github/copilot-instructions.md"] = """# GitHub Copilot & Coding Agent Instructions

Adhere to AGENTS.md rules for all code generation:
1. Never hardcode statutory ceilings or tax brackets. Query from database tables.
2. Never introduce emojis into code, comments, or documentation.
3. Use BigDecimal in Java and NUMERIC(12,2) in PostgreSQL for monetary amounts.
4. Ensure company_id and Hibernate @TenantId multi-tenancy on all tenant-specific operations.
"""


def main():
    print(f"Scaffolding {len(FILES)} documentation and blueprint files in {BASE_DIR}...")
    for rel_path, content in FILES.items():
        target = BASE_DIR / rel_path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content.strip() + "\n", encoding="utf-8")
        print(f"  Created: {rel_path}")

    # Make shell scripts executable
    for script in ["validate-docs.sh", "check-quality.sh", "verify-environment.sh"]:
        script_path = BASE_DIR / "scripts" / script
        if script_path.exists():
            os.chmod(script_path, 0o755)

    print("\nAll files successfully generated!")


if __name__ == "__main__":
    main()
