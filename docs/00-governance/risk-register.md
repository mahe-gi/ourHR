# Project Risk Register

| Risk ID | Risk Description | Severity | Likelihood | Mitigation Strategy | Owner |
|---|---|---|---|---|---|
| R-001 | Cross-tenant data leakage in multi-tenant queries | Critical | Low | Hibernate @TenantId filter + PostgreSQL RLS + automated tenant security tests | Security Agent |
| R-002 | Statutory calculation errors (PF ceiling, 50% rule) | High | Medium | Versioned statutory_rules table + comprehensive unit tests with edge-case fixtures | QA Agent |
| R-003 | Corrupted leave balances due to concurrent updates | High | Low | Immutable append-only leave_ledgers table; balance computed from transaction sum | Architect Agent |
| R-004 | Memory exhaustion during bulk PDF payslip generation | Medium | Medium | Asynchronous off-thread generation via Spring @Async with S3 direct streaming | Backend Agent |
| R-005 | Form 12B omission causing year-end TDS under-deduction | Medium | High | Mandatory onboarding checklist item for mid-year joiners with tax simulator warning | Product Agent |
