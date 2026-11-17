# ADR-002: Multi-Surface Frontend Architecture

Status: Accepted
Date: 2026-11-17
Owner: Architect Agent

## Context
ourHR serves multiple user personas with varying device requirements: desktop web for HR/Finance/CA administrators, mobile for deskless and field employee attendance, and a public marketing landing page.

## Decision
- Web App: React 18+ with Vite and TypeScript.
- Mobile App: React Native with TypeScript.
- Landing Page: Next.js with static site generation.

## Rationale
Enables independent build and deployment lifecycles while maximizing TypeScript skill and validation schema reuse across all frontends.
