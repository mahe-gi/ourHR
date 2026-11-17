---
name: project-analysis
description: Analyzes repository state, verifies architectural compliance, and reports missing documentation or tests.
---

# Project Analysis Skill

## Procedure
1. Check repository git status and recent commit history.
2. Verify that all 16 core database tables in docs/04-data/database-design.md have corresponding Flyway migrations and JPA entities.
3. Validate that no hardcoded statutory limits or emojis exist in the codebase.
4. Report health status, outstanding tasks, and architectural deviations.
