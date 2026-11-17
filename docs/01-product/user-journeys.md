# End-to-End User Journeys

## Journey 1: Paperless Onboarding to Compliant Appointment Letter
1. HR Admin creates a candidate profile with designation, department, branch, and offered CTC.
2. System validates that Basic + DA is at least 50% of gross remuneration.
3. Candidate receives magic onboarding link, fills personal details, bank IFSC, UAN, and uploads KYC documents to S3.
4. Mid-year joiners submit Form 12B prior employment details.
5. System generates the standard Indian Labour Code appointment letter and collects e-signature.

## Journey 2: Monthly Attendance Lock to 6-Step Payroll Run
1. Shift engine captures punches via Web, Mobile GPS+Selfie, or Biometric sync.
2. On the cut-off date (e.g., 25th), HR Admin reviews missed punches and triggers Monthly Attendance Lock.
3. Finance Manager launches the 6-Step Payroll Stepper:
   - Step 1: Sync Attendance and LOP (applies pending LOP reversals).
   - Step 2: Joiners & Exits (prorates mid-month salaries).
   - Step 3: Variable Pay & Overrides (adds bonuses and overtime pay).
   - Step 4: Reimbursements & Loan EMIs (includes expenses, deducts loan instalments).
   - Step 5: Variance & Anomaly Audit (side-by-side diff vs. last month).
   - Step 6: Maker-Checker Approval (Founder sign-off, bank CSV export, ECR .txt generation).
4. Payslips are distributed asynchronously to employee self-service portals before the 7th.

## Journey 3: External CA Compliance Audit
1. External CA logs in via dedicated guest credentials.
2. CA views compliance dashboard displaying due dates (7th for TDS/wages, 15th for PF/ESIC).
3. CA inspects variance between calculated liability and actual payroll numbers.
4. CA downloads EPFO ECR .txt, ESIC monthly report, and quarterly Form 24Q files with 1 click.
