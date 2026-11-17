# Entity Relationships Specification

```
companies (1) ---> (N) branches
companies (1) ---> (N) users ---> (N) user_roles
companies (1) ---> (N) employees
employees (1) ---> (1) employee_kyc_vault
employees (1) ---> (N) employee_salary_structures
employees (1) ---> (N) attendance_logs
employees (1) ---> (N) monthly_attendance_summary
employees (1) ---> (N) leave_applications
employees (1) ---> (N) leave_ledgers
employees (1) ---> (1) previous_employer_income (Form 12B)
companies (1) ---> (N) payroll_runs ---> (N) payslip_line_items
companies (1) ---> (N) lop_reversals
companies (1) ---> (N) audit_logs
```
