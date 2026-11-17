# Contributing Guidelines

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
