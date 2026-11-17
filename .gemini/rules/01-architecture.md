# Antigravity Architecture Rule: Modular Monolith & Contracts

1. Package Structure: All backend code must reside under `com.ourhr.*`:
   - `com.ourhr.common`: Base entities, API envelopes, exceptions, tenant context.
   - `com.ourhr.security`: JWT filter, authentication handlers, RBAC config.
   - `com.ourhr.company`: Legal entity, multi-state branches, org hierarchy.
   - `com.ourhr.employee`: Master records, KYC document vault, Form 12B.
   - `com.ourhr.attendance`: Punches, shifts, regularization, monthly attendance freeze.
   - `com.ourhr.leave`: Leave types, immutable leave ledger, sandwich rule.
   - `com.ourhr.payroll`: Structures, 6-step runner, arrears, LOP reversals, Section 194J/C.
   - `com.ourhr.statutory`: EPF, ESIC, state PT/LWF, Old vs. New TDS calculation strategies.
   - `com.ourhr.exporter`: EPFO ECR .txt, Bank CSVs, PDF payslips, Form 24Q.
   - `com.ourhr.jobs`: Persistent batch job queue execution.
2. Inter-Module Coupling: Modules must communicate via public `@Service` interfaces or DTOs. Never inject another module's JPA Repository directly.
3. Contract-First API: All public REST endpoints must be defined with OpenAPI 3 annotations (@Operation, @ApiResponse) and tested before publishing.
4. Zero Emojis: Strictly forbidden in code, comments, documentation, and API messages.
