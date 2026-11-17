# Pull Request Checklist

## Task Reference
Task ID: TASK-[NUMBER]
Specification: [Link]

## Quality Gates Checklist
- [ ] No hardcoded statutory values (EPF, ESIC, PT, TDS).
- [ ] Zero emojis anywhere in code, comments, commit messages, or UI.
- [ ] Multi-tenancy isolation enforced with company_id.
- [ ] Exact decimal precision (NUMERIC(12,2) / BigDecimal) used for money.
- [ ] Unit and integration tests pass with zero regressions.
- [ ] Documentation updated to reflect changes.
