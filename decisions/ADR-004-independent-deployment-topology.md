# ADR-004: Decoupled Multi-Cloud and Single-JAR Deployment

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
ourHR must be deployable either on modern cloud infrastructure (CDN for frontends, containers for API) or as a self-contained single instance for traditional MSMEs with on-premises or simple VPS infrastructure.

## Decision
Maintain completely decoupled frontend and backend codebases, with an automated build option to copy compiled React static assets into Spring Boot's resources/static/ folder for single-JAR packaging.
