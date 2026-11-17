# Backend Engineering Standards

- Controller Layer: Request mapping, DTO validation, and response wrapping only. Zero business logic.
- Service Layer: All business rules, transactions (@Transactional), and statutory validations.
- Repository Layer: Spring Data JPA interfaces; custom queries must use JPQL or specifications with tenant isolation.
