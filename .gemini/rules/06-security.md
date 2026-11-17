# Antigravity Security Rule: Statutory Data Privacy

1. Secrets: Zero secrets, private keys, or API tokens in git repositories. Use environment variables.
2. RBAC: Enforce role-based checks (ROLE_FOUNDER, ROLE_HR, ROLE_FINANCE, ROLE_CA, ROLE_MANAGER, ROLE_EMPLOYEE) at the service and controller layer.
3. Data Masking: Mask Aadhaar and PAN numbers in public API responses.
4. Tenant Isolation: Prevent cross-tenant data access through DB-level filters and integration tests.
