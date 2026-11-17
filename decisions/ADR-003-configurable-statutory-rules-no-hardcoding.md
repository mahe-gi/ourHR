# ADR-003: Zero Hardcoding Policy for Statutory Rules

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
Indian labor laws, tax brackets, EPF wage limits, and state-specific PT/LWF rates change periodically via government notifications. Hardcoding these in application code creates compliance liabilities.

## Decision
All statutory parameters, tax brackets, wage ceilings, and formula ratios must be stored in database tables (statutory_rules, pt_slabs, lwf_rules, tds_tax_slabs) with effective_from and effective_to date ranges.

## Rationale
Enables seamless regulatory updates without application code changes or redeployment, ensuring historical payroll runs remain accurately reproducible against their respective historical rules.
