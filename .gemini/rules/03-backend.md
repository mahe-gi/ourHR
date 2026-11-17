# Antigravity Backend Rule: Java 21 & Spring Boot 3 Standards

1. Language & Framework: Java 21 LTS with Spring Boot 3.3.x.
2. BigDecimal Standard: All monetary, salary, and statutory calculations must use `java.math.BigDecimal` with explicit `RoundingMode` (HALF_UP or statutory prescribed rule) and `NUMERIC(12,2)` in PostgreSQL. `double` and `float` are strictly prohibited in financial code.
3. Controller Standards: REST controllers must return `ApiResponse<T>` with standard envelope. Business logic belongs in `@Service` classes, never in controllers.
4. Input Validation: All request payloads must be validated using Jakarta Bean Validation annotations (@NotNull, @DecimalMin, @Pattern, etc.).
5. Exception Handling: Handled centrally via `@RestControllerAdvice` mapping domain exceptions to standard HTTP error codes.
6. Zero Emojis: Strictly forbidden in all log messages, exception text, and source comments.
