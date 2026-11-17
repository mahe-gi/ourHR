# Security Requirements

1. Multi-Tenant Data Isolation: Strict logical isolation via company_id enforced on every query.
2. Secret Protection: Zero credentials in git; all secrets injected via environment variables.
3. Cryptographic Storage: Passwords hashed via BCrypt (work factor 12+).
4. Transport Security: TLS 1.3 for all in-transit communications.
