# Engineering Principles

1. Statutory Accuracy Is Absolute: A single rupee discrepancy in PF or TDS creates compliance liabilities. Calculations must be exact.
2. Zero Hardcoding: Labour laws, tax brackets, and state rules change frequently. All parameters are dynamic and versioned.
3. Defense in Depth: Multi-tenancy isolation is enforced at the database, ORM, and service layers.
4. Independent Deployability: Frontend and backend artifacts must build and deploy autonomously.
5. Simplicity Over Premature Abstraction: Build clean, cohesive modules; avoid microservice proliferation where a modular monolith suffices.
