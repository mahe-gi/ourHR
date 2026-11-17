# Integration Testing Guidelines

- Use Testcontainers to run tests against real PostgreSQL instances.
- Verify multi-tenancy: insert records under Tenant 1 and Tenant 2; verify Tenant 1 queries never return Tenant 2 data.
- Verify transaction rollbacks on payroll calculation failures.
