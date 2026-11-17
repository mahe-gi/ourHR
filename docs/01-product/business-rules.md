# Indian Statutory Business Rules

## 1. Code on Wages: 50% Basic Wage Rule
- Statutory Definition: Basic Pay + Dearness Allowance + Retaining Allowance must be >= 50% of total gross remuneration.
- Engine Rule: If excluded allowances (HRA, Special Allowance, Conveyance, etc.) exceed 50% of gross remuneration, the excess amount is automatically added back into the statutory wage base for EPF, ESIC, and Gratuity calculations.

## 2. Employees' Provident Fund (EPF & EPS)
- Wage Ceiling: Standard statutory ceiling of INR 15,000 / INR 25,000 per month (configurable in statutory_rules).
- Employee Contribution: 12% of EPF wage base.
- Employer Contribution:
  - EPS (Pension): 8.33% of EPF wage (capped at the ceiling).
  - EPF (Difference): 12% minus EPS share (minimum 3.67%).
  - EDLI (Insurance): 0.5% (subject to ceiling).
  - Admin Charges: 0.5% of total EPF wages (subject to statutory minimums).

## 3. Employees' State Insurance (ESIC)
- Eligibility: Gross monthly salary <= INR 21,000 (INR 25,000 for employees with disability).
- Employee Contribution: 0.75% of gross wages.
- Employer Contribution: 3.25% of gross wages.
- Six-Month Contribution Rule: If an employee's salary crosses INR 21,000 during a contribution period (April-Sept or Oct-March), deductions continue until the end of that period.

## 4. Professional Tax (PT)
- Deducted based on branch state code, monthly gross earnings, and gender.
- Special Month Rule: For example, Maharashtra deducts INR 200/month except February where INR 300 is deducted to reach the annual cap of INR 2,500.

## 5. Income Tax (TDS under Section 192)
- New Tax Regime: Default regime with INR 75,000 standard deduction and Section 87A rebate thresholds.
- Old Tax Regime: Optional regime with exemptions for HRA (Section 10(13A)), Chapter VI-A deductions (80C, 80D, 80CCD), and home loan interest (Section 24B).
- Form 12B Integration: Previous employer gross salary and TDS deducted are aggregated for accurate progressive slab computation.
