# Backend Sprint 5 Specification: CTC Builder, Arrears & 6-Step Payroll Engine

Author: agent-0-orchestrator
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Sprint: Backend Sprint 5
Status: Approved

## 1. Objective and Scope
Backend Sprint 5 delivers the 6-Step Payroll State Machine, Immutable Payroll Disbursal, LOP Reversal Processing, Backdated Revision Arrears Calculator, and Section 194J/194C Consultant Payouts.

## 2. 6-Step Payroll State Machine
The state machine strictly transitions in the following order:
`DRAFT` -> `ATTENDANCE_LOCKED` -> `ADJUSTMENTS_APPLIED` -> `CALCULATED` -> `AUDITED` -> `APPROVED` -> `DISBURSED`.

1. `DRAFT`: Payroll run initiated for (payroll_month, payroll_year).
2. `ATTENDANCE_LOCKED`: Validates that `monthly_attendance_summary.is_locked = true` for the period.
3. `ADJUSTMENTS_APPLIED`: Ingests approved `lop_reversals`, salary arrears, and reimbursements.
4. `CALCULATED`: Runs gross-to-net calculations across employees and creates draft `payslip_line_items`.
   * Recalculation Rule: In status `DRAFT` or `CALCULATED`, draft line items may be purged and recomputed without restriction.
5. `AUDITED`: Generates audit diff against previous month (headcount delta, net salary variations).
6. `APPROVED`: Locked by `ROLE_FOUNDER` or `ROLE_FINANCE`. No further recalculations allowed.
7. `DISBURSED`: Bank payout file and ECR file triggered. Database immutability trigger blocks any updates or deletes to `payslip_line_items`.

## 3. Section 194J & 194C Consultant Payout Runner
- Dedicated execution path for `worker_type = 'CONSULTANT_194J'`:
  * Deducts TDS under Section 194J (10% or 2% for tech fees) or 194C (1% individual / 2% entity).
  * Excluded from PF/ESIC/PT payroll deductions.

## 4. Database Entities (Owner: agent-1-database)
- `SalaryComponent`: `com.ourhr.payroll.SalaryComponent` (`salary_components`)
- `LopReversal`: `com.ourhr.payroll.LopReversal` (`lop_reversals`)
- `PayrollRun`: `com.ourhr.payroll.PayrollRun` (`payroll_runs`)
- `PayslipLineItem`: `com.ourhr.payroll.PayslipLineItem` (`payslip_line_items`)

## 5. Domain Engine Services (Owner: agent-3-payroll-statutory)
- `PayrollRunnerService`: Implements the 6-step state machine transitions.
- `LopReversalService`: Manages LOP credit adjustments.
- `ConsultantPayoutService`: Handles Section 194J/194C payouts.
- `PayrollAuditService`: Produces diff against previous month's run.
- `PayrollController`, `LopReversalController`, `ConsultantPayoutController`.

## 6. QA Acceptance Criteria (Owner: agent-5-qa-security)
- Unit and integration tests verifying:
  1. Full state machine sequence `DRAFT` through `DISBURSED`.
  2. Immutability: Attempting to update `payslip_line_items` when run is `DISBURSED` throws an exception.
  3. Purge & recalculate: Running calculate twice in `CALCULATED` replaces draft items cleanly without duplicates.
  4. LOP reversal correctly adds positive adjustment.
  5. Section 194J/194C TDS calculation arithmetic.
  6. Zero emojis and 100% green tests.
