# Antigravity Architecture Rule: System Boundaries

1. Decoupled Surfaces: Backend (Spring Boot) and frontends (React Vite, React Native, Next.js) must remain fully decoupled.
2. Contract-First Development: Endpoints must be documented and agreed in docs/05-api/ before client implementation.
3. S3 Storage: Binary files, KYC documents, selfie punches, and payslips must never be stored in database columns. Use the S3 adapter.
4. Immutability: Completed payroll runs, payslips, and leave ledger transactions are strictly append-only.
