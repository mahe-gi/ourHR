# Secrets Management

- Development: Managed via local .env files (excluded in .gitignore).
- Staging / Production: Injected via cloud secrets managers (AWS Secrets Manager / Vault) into environment variables.
- Rotation Policy: JWT signing keys and database credentials rotated every 90 days.
