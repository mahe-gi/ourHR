# ourHR: Indian MSME & Startup HRMS

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
