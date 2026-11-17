# Project Overview: ourHR

## 1. Project Identity
- Name: ourHR
- Target Segment: Indian MSMEs and Startups (15 to 250 employees)
- Domain: Human Resource Management System (HRMS) & Payroll Compliance
- Version: 1.0.0
- Status: Discovery and Foundation Phase
- Repository: https://github.com/mahe-gi/ourHR

## 2. Problem Statement
Indian small and medium enterprises face a sharp dilemma in managing human resources and payroll:
1. Legacy platforms (e.g., greytHR) provide accurate statutory compliance for PF, ESIC, PT, and TDS, but suffer from antiquated user interfaces, slow workflows, and high administrative overhead.
2. Modern platforms (e.g., Keka HR) provide clean user experiences, but are expensive for early-stage companies and still require exporting spreadsheets back and forth to external Chartered Accountants for monthly statutory returns.
3. Many early-stage firms rely on manual Excel workbooks, causing statutory calculation errors, missed compliance deadlines (7th for TDS/wages, 15th for PF/ESIC), and non-compliance with the Indian Code on Wages (such as the 50% Basic wage mandate).

ourHR bridges this gap by delivering the statutory rigor of greytHR combined with the modern UX of Keka, introducing a dedicated guest workspace for external Chartered Accountants and fully data-driven compliance rules.

## 3. Core Value Proposition
- Statutory Accuracy Without Code Hardcoding: All tax brackets, contribution rates, and wage limits are dynamic, versioned, and date-effective in the database.
- External CA Collaboration: Chartered Accountants receive a direct, read-only guest portal to inspect variance reports and download generated ECR, ESIC, PT, and Form 24Q files without email spreadsheet exchanges.
- 6-Step Guided Payroll: A deterministic, maker-checker payroll run with anomaly audits against historical runs.
- Multi-Tenant Security: Automatic tenant isolation enforced at the database/ORM layer across all business operations.
- Dual Deployment Capability: Deployable as separate cloud-native microservices/containers (web on CDN, API on cloud VM) or bundled into a single portable container for self-hosted MSME environments.

## 4. Target Personas
1. Founder / Super Admin: Needs a 60-second operational snapshot of headcount, payroll outflow, statutory liability, and pending approvals.
2. HR Admin / Operations Manager: Manages onboarding, employee records, attendance rosters, leave policies, and documentation.
3. Finance Manager / Payroll Admin: Configures salary structures, manages monthly payroll execution, reimbursements, and bank payout files.
4. External Chartered Accountant / Compliance Auditor: Audits monthly tax calculations, verifies PF/ESIC liabilities, and retrieves filing returns.
5. Reporting Manager and Employee: Handles mobile/web attendance clock-in, leave requests, expense claims, tax declarations, and payslip retrieval.

## 5. Technology Stack Summary

### Backend
- Language: Java 17 / 21
- Framework: Spring Boot 3 with Spring MVC
- Build Tool: Apache Maven
- Persistence: Spring Data JPA with Hibernate (Multi-tenancy via `@TenantId`)
- Database: PostgreSQL (Relational schema, JSONB for flexible rule definitions)
- Database Migrations: Flyway
- Security: Spring Security (Stateless JWT with RBAC)
- API Documentation: SpringDoc OpenAPI (Swagger UI)
- File Storage: S3-Compatible Storage Adapter (AWS S3 / Cloudflare R2 / MinIO)
- Logging: SLF4J with Logback (Structured JSON logging)

### Frontend Surfaces
- Web Application: React 18+ with Vite and TypeScript (Tailwind CSS, React Router, TanStack Query, React Hook Form, Zod)
- Mobile Application: React Native with TypeScript (NativeWind, React Navigation)
- Marketing Landing Page: Next.js with TypeScript (Static site generation, Tailwind CSS)
- Shared Logic: Shared TypeScript contracts, Zod validation schemas, and API client types

## 6. Project Constraints
- Zero Emojis: No emojis in source files, commit messages, documentation, or user interfaces.
- Zero Hardcoded Statutory Rules: Every statutory formula, ceiling, and slab must be stored in effective-dated database tables.
- Precision: All monetary calculations must use exact fixed-point decimals (`NUMERIC(12,2)` / `BigDecimal`).
- Multi-Tenancy: Every tenant table must enforce tenant isolation via `company_id`.
- Document Storage: Zero binary blobs in relational tables; file storage must use an S3-compatible adapter.
