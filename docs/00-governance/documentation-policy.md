# Documentation Maintenance Policy

## 1. Ownership and Lifecycle
Every document in the ourHR repository must have:
- Document Owner: Specific agent role (Product, Architecture, Security, etc.).
- Status: DRAFT, IN_REVIEW, APPROVED, DEPRECATED, or ARCHIVED.
- Last Updated Date: ISO format date.

## 2. Maintenance Rules
1. Atomic Updates: When an API contract or database schema changes, documentation must be updated in the same commit.
2. Single Source of Truth: PRD.md is the sole authority on business rules; docs/04-data/ is the sole authority on schema.
3. Conflict Escalation: Inconsistencies between documents must halt implementation until resolved by human approval.
