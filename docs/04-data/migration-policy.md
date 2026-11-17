# Database Migration Policy

1. Tool: Flyway exclusively for all schema evolution.
2. File Naming: V<Version>__<description_in_snake_case>.sql in backend/src/main/resources/db/migration/.
3. Immutability: Once merged to main, migration files are immutable. Schema changes require a new incremental script.
4. Non-Destructive Migrations: Never drop columns or tables in production without a multi-phase deprecation cycle.
