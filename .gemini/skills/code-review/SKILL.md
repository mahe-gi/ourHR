---
name: code-review
description: Performs rigorous automated code review against AGENTS.md standards.
---

# Code Review Skill

## Checklist
- [ ] No hardcoded statutory values (EPF, ESIC, PT, TDS).
- [ ] No emojis in code, comments, or UI components.
- [ ] Multi-tenancy isolation enforced with company_id.
- [ ] Financial calculations use BigDecimal / NUMERIC(12,2).
- [ ] No binary data stored directly in the database.
- [ ] Unit and integration tests cover happy and negative paths.
