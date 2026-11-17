# Role-Based Access Control (RBAC) Matrix

| Module / Operation | ROLE_FOUNDER | ROLE_HR | ROLE_FINANCE | ROLE_CA | ROLE_MANAGER | ROLE_EMPLOYEE |
|---|---|---|---|---|---|---|
| Company Legal Settings | Read/Write | Read | Read | No Access | No Access | No Access |
| Employee Master | Read/Write | Read/Write | Read | Read | Direct Reports | Self Only |
| Attendance Lock | Approve | Execute | Read | Read | No Access | No Access |
| Leave Approval | Override | Approve | Read | No Access | Team Only | Self Only |
| Payroll Execution | Approve | No Access | Execute | No Access | No Access | No Access |
| Statutory Exporters | Export | No Access | Export | Export | No Access | No Access |
