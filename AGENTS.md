# AGENTS.md

Authoritative Multi-Agent Engineering Standard
Project: ourHR (Indian MSME HRMS)
Version: 1.0.0
Status: Approved

## 1. Project Identity and Context
ourHR is a modern Human Resource Management System and Payroll platform built specifically for Indian MSMEs and startups (15 to 250 employees). It combines the statutory payroll accuracy of greytHR with the user experience of Keka HR, eliminating spreadsheet handoffs to external Chartered Accountants.

## 2. Fundamental Engineering Constraints

### 2.1. Zero Hardcoding Policy
Under no circumstances shall statutory rates, salary contribution percentages, tax slabs, or wage thresholds be hardcoded into application code, configuration files, or enums.
- All statutory thresholds (such as EPF wage ceilings, ESIC wage caps, standard deduction values, and 50% wage code ratios) must reside in versioned database tables with effective date ranges (`effective_from`, `effective_to`).
- All state-specific Professional Tax (PT) and Labour Welfare Fund (LWF) slabs must be dynamically resolved based on the employee work branch state and payroll execution date.

### 2.2. Multi-Tenancy Isolation
- Every tenant-specific table must contain a `company_id` foreign key referencing the `companies` table.
- Multi-tenancy must be enforced at the JPA/ORM level via Spring Hibernate `@TenantId` or PostgreSQL Row-Level Security (RLS).
- No query may rely solely on developer discipline to include tenant filters.

### 2.3. Zero Emojis Policy
- No emojis are permitted anywhere in the repository: source code, comments, documentation, UI components, Git commit messages, or log statements. Use standard SVG icons (such as Lucide) for user interfaces.

### 2.4. Independent Deployment Architecture
- The backend (Java Spring Boot) and frontend surfaces (React Vite Web App, React Native Mobile App, Next.js Landing Page) must remain completely decoupled.
- The backend must expose contract-first RESTful APIs and must be able to run either as a standalone API service or optionally serve compiled static web assets for single-instance deployments.

### 2.5. Storage and File Handling
- No binary files, documents, selfie punches, or PDF payslips may be stored directly in relational database columns (`BYTEA`/`BLOB`).
- An S3-compatible object storage interface (AWS S3, Cloudflare R2, MinIO) must be used, storing only the secure object key or URL reference in the database.

### 2.6. Financial Precision and Immutability
- All monetary calculations must use exact decimal representation (`NUMERIC(12, 2)` in PostgreSQL and `BigDecimal` in Java). Floating-point data types (`FLOAT`, `DOUBLE`) are strictly forbidden for currency and days.
- Completed payroll runs and generated payslip line items are strictly immutable. Adjustments, backdated salary revisions, and LOP reversals must be recorded as retroactive arrears or adjustments in the current open payroll cycle.
- Leave balances must be computed from an append-only transaction ledger (`leave_ledgers`), never from a mutable balance counter.

## 3. Authority Hierarchy
When conflicting requirements or decisions arise, agents must adhere to the following order of precedence:
1. Explicit human instructions and approvals.
2. Approved PRD and functional specifications (`PRD.md`, `specs/`).
3. Approved architecture and architectural decision records (`docs/02-architecture/`, `decisions/`).
4. Database and API contracts (`docs/04-data/`, `docs/05-api/`).
5. General engineering and coding standards (`docs/06-engineering/`).
6. Existing codebase conventions.
7. Automated agent suggestions.

If two authoritative documents conflict, stop affected work, document the inconsistency, and request clarification.

## 4. Specialized Agent Responsibilities
- Orchestrator Agent: Oversees lifecycle execution, dependency enforcement, and quality gate sign-offs.
- Product Agent: Maintains the PRD, user journeys, acceptance criteria, and statutory business rules.
- Architect Agent: Governs system boundaries, ADRs, database normalization, and contract definitions.
- UI/UX Agent: Governs design system tokens, Tailwind components, responsive layouts, and accessibility.
- Backend Agent: Implements Spring Boot controllers, services, JPA repositories, Flyway migrations, and statutory logic.
- Frontend Agent: Implements React/Vite web interfaces, React Native screens, and Next.js static pages against approved API contracts.
- QA Agent: Executes automated unit, integration, and contract tests; validates edge cases.
- Security Agent: Enforces RBAC permissions, audit logging, input sanitization, and PII masking.
- DevOps Agent: Governs Docker configurations, CI/CD pipelines, and environment strategies.

## 5. Mandatory Task Completion Checklist (Definition of Done)
A task is marked completed only when:
- All functional requirements and acceptance criteria are satisfied.
- Unit and integration tests pass with zero regressions.
- Code conforms to architectural boundaries and naming standards.
- Database changes are encapsulated in versioned Flyway migration scripts.
- No secrets, credentials, or sensitive PII are exposed.
- All documentation and API contracts reflect the change.
- Verification commands have been executed and output recorded.
