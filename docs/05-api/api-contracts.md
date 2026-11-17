# Core API Contract Registry

| Method | Endpoint | Description | Roles Allowed |
|---|---|---|---|
| POST | /api/v1/auth/login | Authenticate and obtain JWT | Anonymous |
| GET | /api/v1/company/branches | List company branches and states | All Authenticated |
| GET | /api/v1/employees | Search and filter employee master | HR, Finance, Founder, CA |
| POST | /api/v1/employees/onboard | Issue self-service onboarding magic link | HR, Founder |
| POST | /api/v1/attendance/punch | Record Web or Mobile clock-in | Employee |
| POST | /api/v1/attendance/freeze | Lock monthly attendance for payroll | HR, Founder |
| GET | /api/v1/leaves/balance | Retrieve leave ledger balances | Employee, Manager |
| POST | /api/v1/leaves/apply | Submit leave application | Employee |
| POST | /api/v1/payroll/runs | Initiate 6-step monthly payroll run | Finance, Founder |
| POST | /api/v1/payroll/runs/:id/approve| Maker-checker approval of payroll | Founder |
| GET | /api/v1/compliance/epf/ecr | Export EPFO ECR .txt file | Finance, CA, Founder |
| GET | /api/v1/compliance/tds/form24q | Export Form 24Q quarterly e-TDS file | Finance, CA, Founder |
| GET | /api/v1/ca/summary | Retrieve CA compliance audit summary | CA, Founder |
