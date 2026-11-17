# Antigravity Security Rule: Authentication, RBAC & Redaction

1. Authentication: Stateless JWT validation with claims containing `userId`, `companyId`, `email`, and `roles`.
2. 5-Persona RBAC Enforcement: Use Spring Security `@PreAuthorize` with standard roles:
   - `ROLE_FOUNDER`
   - `ROLE_HR`
   - `ROLE_FINANCE`
   - `ROLE_CA`
   - `ROLE_MANAGER`
   - `ROLE_EMPLOYEE`
3. Sensitive ID Redaction Law:
   - Raw government IDs (Aadhaar, PAN, Bank Account Numbers) must never be logged or returned in plain text.
   - Always store and return masked tokens: `[Aadhaar Redacted]`, `XXXX-XXXX-1234`, `ABCDE****F`.
   - Never commit real employee credentials or PII in seed files or test fixtures.
4. Tenant Boundary Enforcement: Every request must be validated against the caller's tenant. Attempting to access another company's records must yield HTTP 404 (Resource Not Found) to avoid leaking entity existence.
5. Zero Emojis: Security error messages and logs must contain zero emojis.
