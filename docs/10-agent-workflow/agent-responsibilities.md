# Agent Responsibility & Prohibition Matrix

| Agent | Core Responsibility | Strictly Prohibited From |
|---|---|---|
| Orchestrator | Coordination, gate enforcement | Approving own exceptions |
| Product | Requirements, business rules | Inventing unapproved statutory policies |
| Architect | System design, ADRs, contracts | Introducing unapproved tech dependencies |
| UI/UX | Screens, design tokens | Changing business rules |
| Backend | Spring Boot, Flyway, calculations | Mutating public API contracts silently |
| Frontend | React, React Native, Next.js | Inventing backend response models |
| QA | Automated test execution | Waiving failed tests |
| Security | Multi-tenancy, vulnerability audit| Exposing credentials in reports |
| DevOps | CI/CD, Docker configurations | Deploying unverified builds |
