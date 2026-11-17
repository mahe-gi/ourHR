# Application Structure

```
ourHR/
├── backend/
│   ├── src/main/java/com/ourhr/
│   │   ├── config/              # Security, S3, Multi-tenancy, Swagger configs
│   │   ├── core/                # Base entities, multi-tenancy filters, exceptions
│   │   ├── modules/
│   │   │   ├── company/         # Legal entities, branches, hierarchy
│   │   │   ├── employee/        # Master records, KYC vault, Form 12B
│   │   │   ├── attendance/      # Clock-in, shifts, monthly lock, overtime
│   │   │   ├── leave/           # Leave types, applications, immutable ledgers
│   │   │   ├── payroll/         # Structures, 6-step runner, salary revisions
│   │   │   ├── statutory/       # EPF, ESIC, PT, TDS calculation engines
│   │   │   ├── export/          # ECR .txt, bank CSV, Form 24Q exporters
│   │   │   └── ca/              # External CA guest audit APIs
│   │   └── OurHrApplication.java
│   ├── src/main/resources/
│   │   ├── db/migration/        # Versioned Flyway SQL scripts
│   │   └── application.yml
│   └── pom.xml
│
├── frontend/
│   ├── web/                     # React + Vite Web SPA
│   ├── mobile/                  # React Native Mobile App
│   ├── landing/                 # Next.js Static Landing Page
│   └── shared/                  # Shared TypeScript types, Zod schemas, API clients
│
├── docs/                        # Complete architecture & domain documentation
├── specs/                       # Feature specifications & templates
├── plans/                       # Product & implementation roadmaps
└── tasks/                       # Task tracking & backlog
```
