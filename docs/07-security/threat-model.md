# Threat Model Analysis

| Threat | Impact | Mitigation |
|---|---|---|
| Tenant Infiltration (Accessing Company B data) | Critical | Spring Data @TenantId + PostgreSQL Row Level Security |
| Payroll Modification Post-Approval | High | Immutable database flags; approved runs cannot be edited |
| PII Exposure (Aadhaar / PAN leakage) | High | Aadhaar numbers masked; documents stored in private S3 with signed URLs |
| Privilege Escalation to Super Admin | High | Method-level security annotations (@PreAuthorize) on all sensitive endpoints |
