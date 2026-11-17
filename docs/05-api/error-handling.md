# Standard Error Catalog

| Error Code | HTTP Status | Meaning |
|---|---|---|
| AUTH_INVALID_CREDENTIALS | 401 | Invalid email or password |
| AUTH_TOKEN_EXPIRED | 401 | JWT expired; refresh required |
| PERMISSION_DENIED | 403 | Insufficient role permission |
| RESOURCE_NOT_FOUND | 404 | Entity does not exist or wrong tenant |
| WAGE_RULE_50_VIOLATED | 422 | Basic + DA is less than 50% of gross |
| ATTENDANCE_NOT_LOCKED | 422 | Payroll run requires locked attendance |
| PAYROLL_ALREADY_APPROVED | 409 | Run cannot be modified after approval |
