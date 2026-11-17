# Backend Sprint 6 Specification: Indian Statutory Calculation Engine

Author: agent-0-orchestrator
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Sprint: Backend Sprint 6
Status: Approved

## 1. Objective and Scope
Backend Sprint 6 delivers the zero-hardcoded Indian Statutory Calculation Strategy Engine:
1. Dynamic EPF/EPS Splitter (12% employee, 8.33% EPS capped at 1,250, 3.67% EPF balance, 0.5% EDLI, 0.5% admin).
2. ESIC Eligibility & 6-Month Contribution Window Continuation Rule.
3. State-wise PT Calculator (Strategy Pattern for Maharashtra February INR 300 rule, Karnataka, Telangana, Tamil Nadu).
4. State-wise Labour Welfare Fund (LWF) Calculator (Karnataka annual in Dec, Maharashtra half-yearly in June/Dec).
5. Old vs. New Tax Regime Progressive TDS Projector with Form 12B Previous Income Aggregation.

## 2. Statutory Laws and Algorithms
1. Zero Hardcoding Law:
   - All rates and thresholds must query `statutory_rules`, `pt_slabs`, `lwf_rules`, and `tds_tax_slabs` via `StatutoryQueryService`.
2. EPF Math:
   - Wage Base = MIN(basicPay + daPay, statutoryCeiling).
   - Employee PF = Wage Base * 12%.
   - Employer EPS = MIN(Wage Base, ceiling) * 8.33% (rounded, max INR 1,250).
   - Employer EPF = (Wage Base * 12%) - Employer EPS.
   - EDLI = Wage Base * 0.5% (max INR 75).
   - Admin = Wage Base * 0.5% (minimum INR 500 per establishment or 0.5% of total PF wages).
3. ESIC Math & 6-Month Window Rule:
   - Window 1: April 1 to September 30.
   - Window 2: October 1 to March 31.
   - If an employee's initial wage in the contribution window is <= INR 21,000 (INR 25,000 for PwD), and later exceeds the ceiling during the window, ESIC continues to be deducted on actual wages until the end of that 6-month window.
4. Maharashtra PT February Rule:
   - Tax is INR 200/month for March through January (11 months), and INR 300 in February.
5. Progressive TDS Algorithm (Section 192):
   - Projected Annual Gross = (Current Month Earned Gross * Remaining Months) + YTD Gross + Form 12B Previous Gross.
   - Net Taxable Income = Projected Annual Gross - Standard Deduction (75,000 for New Regime, 50,000 for Old).
   - Calculate progressive tax using date-effective tax slabs from `tds_tax_slabs`.
   - Apply Section 87A rebate if taxable income is within threshold.
   - Add 4% Health & Education Cess.
   - Monthly TDS = MAX(0, Total Tax - YTD TDS Paid - Form 12B Previous TDS) / Remaining Months.

## 3. Engineering Pattern (Owner: agent-3-payroll-statutory)
- `StatutoryCalculationStrategy` interface.
- Concrete strategies:
  * `EpfCalculatorStrategy`
  * `EsicCalculatorStrategy`
  * `PtCalculatorStrategy`
  * `LwfCalculatorStrategy`
  * `TdsProjectorStrategy`
- Orchestrator: `IndianStatutoryEngineService` coordinating all strategies.
- REST endpoint: `POST /api/v1/statutory/calculate` for simulation/breakdown queries.

## 4. QA Acceptance Criteria (Owner: agent-5-qa-security)
- Unit tests verifying all 7 Indian statutory edge cases with exact decimal assertions.
