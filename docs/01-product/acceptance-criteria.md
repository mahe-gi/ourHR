# Acceptance Criteria Specifications

## AC-001: 50% Basic Wage Rule Enforcement
- Given an employee salary structure where Gross = INR 1,00,000 and Basic = INR 40,000 (40%),
- When the payroll engine calculates statutory deductions,
- Then the excess allowance of INR 10,000 (50,000 - 40,000) is added to the statutory wage base, making statutory wages INR 50,000 for EPF/ESIC/Gratuity computations.

## AC-002: Immutable Leave Ledger Calculation
- Given an employee with 10 days EL credited via accrual transaction,
- When the employee applies for 3 days leave and it is approved,
- Then a consumption transaction of -3.0 days is written to leave_ledgers, and the current balance dynamically queries to 7.0 days without updating any mutable balance column.

## AC-003: Monthly Payroll Variance Detection
- Given an employee whose net pay in October was INR 60,000,
- When November payroll calculation produces a net pay of INR 72,000,
- Then Step 5 of the payroll run flags an anomaly highlighting a +INR 12,000 variance with breakdown (e.g., Performance Bonus).

## AC-004: Multi-Tenant Data Isolation
- Given a user authenticated under Company A,
- When they query employees, payroll runs, or attendance records,
- Then Hibernate @TenantId automatically filters results to company_id = Company A, returning 0 records belonging to Company B even if direct IDs are queried.
