---
name: release-verification
description: Verifies release readiness against quality gates before production tagging.
---

# Release Verification Skill

## Procedure
1. Run complete test suites across backend and frontend surfaces.
2. Validate Flyway migration scripts against a clean test database.
3. Verify that all compliance file exporters (ECR .txt, Bank CSV, Form 24Q) produce valid outputs.
4. Prepare release report in reports/release-reports/.
