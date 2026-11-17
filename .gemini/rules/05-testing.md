# Antigravity Testing Rule: QA, Testcontainers & Statutory Edge Cases

1. Ownership: `agent-5-qa-security` owns all test classes in `src/test/java/**`.
2. Stack: JUnit 5, Mockito, and Testcontainers PostgreSQL for end-to-end repository and service verification.
3. Zero Mocking of Math: Statutory formulas and financial calculations must never be mocked; test the actual calculation strategy beans with live database parameters.
4. Mandatory 7 Indian Payroll Edge Cases:
   - Case 1: 50% Basic wage rule enforcement (allowance reallocation to statutory wage base).
   - Case 2: EPF ceiling cap (INR 15,000 / INR 25,000) and 8.33% EPS / 3.67% EPF split.
   - Case 3: ESIC wage ceiling (INR 21,000) crossover and 6-month contribution window continuation.
   - Case 4: Maharashtra February special month PT deduction (INR 300 vs. normal INR 200).
   - Case 5: Mid-month joiner and exit salary proration by calendar days vs. payable days.
   - Case 6: Mid-year joiner annual tax computation using Form 12B previous employer income.
   - Case 7: Backdated salary revision arrears calculation without mutating historical disbursed runs.
5. Quality Gate: No task may be marked completed unless `mvn test` or `mvn verify` passes with 100% success.
6. Zero Emojis: Test descriptions, assertion failure messages, and logs must have zero emojis.
