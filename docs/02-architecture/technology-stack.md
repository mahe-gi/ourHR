# Technology Stack Specification

| Tier | Component | Technology | Version | Justification |
|---|---|---|---|---|
| Backend | Language | Java | 17 / 21 LTS | Enterprise stability, strong typing, massive compliance ecosystem |
| Backend | Framework | Spring Boot | 3.2+ | Industry standard, robust dependency injection, Spring Security |
| Backend | Web Layer | Spring MVC | 6.1+ | Contract-first REST APIs, Jakarta validation integration |
| Backend | Persistence | Spring Data JPA / Hibernate | 6.4+ | Native @TenantId multi-tenancy, transactional integrity |
| Database | Relational Store | PostgreSQL | 15+ | Relational guarantees, JSONB for flexible rules, ACID compliance |
| Migrations | Schema Versioning | Flyway | 10+ | Repeatable, deterministic schema evolution |
| Storage | Object Store | S3 Adapter (R2 / MinIO / S3) | - | Secure document vault, zero binary data in database |
| Frontend | Web Application | React + Vite | React 18+ | Fast compilation, clean component architecture |
| Frontend | Mobile Application | React Native | 0.73+ | Cross-platform mobile for iOS and Android |
| Frontend | Landing Page | Next.js | 14+ | Static site generation, SEO optimization |
| Styling | CSS Engine | Tailwind CSS | 3.4+ | Design token consistency, zero runtime CSS overhead |
| Validation | Client Schema | Zod | 3.22+ | TypeScript-native validation matching Jakarta constraints |
