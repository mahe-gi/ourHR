# GitHub Copilot & Coding Agent Instructions

Adhere to AGENTS.md rules for all code generation:
1. Never hardcode statutory ceilings or tax brackets. Query from database tables.
2. Never introduce emojis into code, comments, or documentation.
3. Use BigDecimal in Java and NUMERIC(12,2) in PostgreSQL for monetary amounts.
4. Ensure company_id and Hibernate @TenantId multi-tenancy on all tenant-specific operations.
