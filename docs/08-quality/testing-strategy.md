# Testing Strategy

- Unit Tests: JUnit 5 for domain logic (50% rule, EPF, ESIC, PT, TDS). Vitest for frontend components.
- Integration Tests: Spring Boot Test with Testcontainers PostgreSQL for repository and service verification.
- Contract Tests: Verify bank CSV and EPFO ECR format fidelity against government and bank specifications.
- E2E Tests: Playwright tests covering critical paths (Onboarding -> Attendance Lock -> 6-Step Payroll Run).
