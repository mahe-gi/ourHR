# Continuous Integration & Delivery (CI/CD)

- GitHub Actions automated pipeline executing on every push and pull request.
- Pipeline stages:
  1. Checkout repository.
  2. Setup JDK 17/21 and Node.js.
  3. Maven compile, Checkstyle, and JUnit tests.
  4. Frontend typecheck, lint, and Vitest suite.
  5. Container build verification.
