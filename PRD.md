# Product Requirements Document (PRD)

Project: ourHR
Domain: Indian MSME & Startup Human Resource Management System
Version: 1.0.0
Status: Approved

---

## 1. Executive Summary
ourHR is an end-to-end, multi-tenant Human Resource Management System and Payroll platform tailored for Indian startups and MSMEs with 15 to 250 employees. It delivers the statutory accuracy of greytHR and the modern, responsive employee experience of Keka HR, while eliminating manual spreadsheet exchanges with external Chartered Accountants and ensuring complete compliance with the Indian Code on Wages.

---

## 2. User Personas and Access Control

### 2.1. Persona Definitions
1. Founder / Super Admin: High-level visibility into company headcount, gross payroll commitments, statutory liabilities, and cash flow impacts.
2. HR Admin / Operations Manager: Full lifecycle management of employees from paperless onboarding to exit clearance, attendance schedules, leave policies, and HR letter generation.
3. Finance Manager / Payroll Admin: Management of CTC salary components, 6-step monthly payroll execution, one-off allowances, loan deductions, expense disbursements, and bank transfer files.
4. External Chartered Accountant (CA) / Auditor: Secure guest access to monthly compliance summaries, tax liability verification, and direct downloads of EPFO ECR files, ESIC reports, PT registers, and Form 24Q files.
5. Reporting Manager and Employee: Dual-mode access to submit punches, apply for leaves, submit Form 12B and tax declarations, claim reimbursements, and retrieve payslips. Managers receive an approval dashboard for team requests.

### 2.2. Role-Based Navigation and View Switching
Users holding administrative or managerial roles are also individual employees. The user interface provides a persistent top navigation toggle:
- `Switch View: Admin Workspace <-> My Self-Service`
- Dedicated `Manager Approvals` tab visible exclusively to users with direct reporting assignments.

---

## 3. Product Scope by Phase

### 3.1. Phase 1: MVP Core Modules (Non-Negotiables)

#### Module 1: Multi-State Company and Legal Entity Setup
- Storage of company identity: Registration Number, Legal Name, Trade Name, PAN, TAN, GSTIN, PF Establishment Code, ESIC Code, Labour Identification Number (LIN).
- Multi-Branch Architecture: Each branch is linked to an Indian state (`state_code`). Employee assignment to a branch automatically attaches state-specific Professional Tax (PT) slabs, Labour Welfare Fund (LWF) deduction rules, and regional public holiday calendars.
- Organizational Structure: Dynamic configuration of Departments, Designations, Cost Centres, and Reporting Hierarchies.

#### Module 2: Employee Master and Paperless Onboarding
- Self-Service Onboarding: Time-bound magic links sent to new hires for personal information, emergency contacts, bank details (Account Number, IFSC verification), and statutory credentials (UAN, ESIC IP Number).
- Secure Document Vault: Upload of PAN, proof of identity, educational certificates, relieving letters, and cancelled cheques stored via S3-compatible object storage with masked ID storage.
- Statutory 50% Basic Wage Appointment Letter Engine: Validation that Basic + DA comprises at least 50% of total remuneration under the Indian Code on Wages before letter issuance, complete with digital signature capture.
- Worker Classification: Support for Full-Time Regular Employees, Fixed-Term Employees (FTE), Interns, and Professional Contractors.
- Form 12B Support: Mid-year joiners submit prior employment earnings, PF deducted, and TDS paid to enable accurate progressive annual tax deduction.
- 1-Click Data Migration: Standardized CSV/Excel import wizard for employee profiles, opening leave balances, and YTD payroll history.

#### Module 3: Time, Attendance and Shift Management
- Capture Modes:
  - Web dashboard clock-in with IP address and optional office subnet restriction.
  - Mobile GPS geofenced clock-in with live selfie verification stored in object storage.
  - Standardized API hook for external biometric hardware log ingestion.
- Shift Configuration: Fixed shifts, flexible windows, rotational rosters, and statutory 8-hour daily / 48-hour weekly working limits.
- Grace Periods and Penalties: Configurable rules (e.g., 15-minute grace window; automated half-day Casual Leave or LOP deduction upon 3 late instances).
- Regularization Workflow: Employee request submission for missed punches or off-site visits, routed to the direct manager for 1-click approval.
- Overtime Tracking: Automated calculation of hours exceeding regular shifts, statutory quarterly overtime cap validation, and 2x wage rate calculation.
- Monthly Attendance Lock: Formal freeze action locking total payable days, LOP days, and overtime hours for the payroll period.

#### Module 4: Leave and Holiday Engine
- Supported Leave Categories: Casual Leave (CL), Sick Leave (SL), Earned/Privilege Leave (EL/PL), Maternity Leave (26 weeks), Paternity Leave, Bereavement Leave, Compensatory Off, and Unpaid Leave (LOP).
- Immutable Leave Ledger: All leave allocations, monthly accruals, adjustments, and approved consumption are recorded in an append-only `leave_ledgers` table. Current balances are computed dynamically from transaction sums.
- Sandwich Rule Engine: Configurable option to treat intervening weekends and public holidays between consecutive leave days as consumed leave days.
- Year-End Processing: Automated annual rollover enforcing statutory 30-day Earned Leave accumulation caps and encashment payout computations.

#### Module 5: Core Indian Payroll Engine
- Dynamic CTC Builder: Configurable earnings components (Basic, DA, HRA, Special Allowance, Conveyance, Statutory Bonus) and employer contributions (EPF, ESIC, Gratuity, Insurance).
- 50% Basic Wage Validator: Automated detection of non-basic allowances exceeding 50% of gross remuneration, automatically reallocating the excess to statutory wage calculation bases.
- Guided 6-Step Monthly Payroll Execution:
  1. Sync Attendance and LOP: Import locked payable days and calculate pending LOP reversals approved post previous cutoff.
  2. Joiners and Exits: Prorate salaries for mid-month joiners; route exiting staff to Full and Final (FnF) workflows.
  3. Variable Pay and Overrides: Apply sales bonuses, project incentives, overtime wages, and discretionary adjustments.
  4. Reimbursements and Loan EMIs: Incorporate approved expense disbursements and deduct active loan EMI schedules.
  5. Variance and Anomaly Audit: Tabular diff against preceding month identifying net pay deviations exceeding configured thresholds.
  6. Maker-Checker Approval and Disbursement: Cryptographic sign-off generating bank payment transfer files (NEFT/RTGS CSV for major Indian banks), accounting journal vouchers, and asynchronous digital payslip distribution.
- Contractor and Intern Payouts: Standalone payout run applying flat Section 194J (10%) or Section 194C (1%/2%) TDS deductions without PF or ESIC deductions.

#### Module 6: Indian Statutory Compliance Engine
- Provident Fund (EPF/EPS): Support for statutory wage ceiling (INR 15,000 / INR 25,000) or actual Basic wage base. Automatic allocation of employer share into 8.33% EPS and 3.67% EPF, calculation of EDLI and administrative charges, and generation of the official EPFO ECR text format.
- Employees State Insurance (ESIC): Automatic deduction of 0.75% employee contribution and 3.25% employer contribution for gross monthly wages up to INR 21,000 (INR 25,000 for employees with disabilities).
- Professional Tax (PT) and LWF: State-specific tax bracket execution using employee branch location, employee gender, and special month rules (e.g., February in Maharashtra).
- Income Tax (TDS) Computation:
  - Real-time side-by-side comparison of Old Tax Regime vs. New Tax Regime (default with INR 75,000 standard deduction and Section 87A rebate thresholds).
  - Beginning-of-year declaration window and end-of-year document proof upload and verification.
  - Aggregation of Form 12B prior employer income for accurate cumulative tax brackets.
  - Generation of quarterly Form 24Q e-TDS return text files and annual Form 16 Part A and Part B documents.
- Statutory Registers: One-click export of prescribed electronic formats: Register of Employees, Muster Roll, Register of Wages, Overtime Register, and Register of Deductions.

#### Module 7: Employee and Manager Self-Service (ESS)
- Employee self-service on Web and Mobile for attendance punches, leave submissions, payslip downloads, tax simulation, and expense claims.
- Manager dashboard for one-click approval of leaves, punch regularizations, and reimbursement claims.

#### Module 8: Basic External CA Co-Pilot Workspace (Phase 1 Inclusion)
- Dedicated, secure guest access portal for external Chartered Accountants and compliance auditors.
- Direct verification of monthly PF ECR files, ESIC return figures, state PT registers, and Form 24Q quarterly files.
- Automated compliance calendar tracking statutory deadlines (7th for TDS/wages, 15th for PF/ESIC).

---

### 3.2. Phase 2: Financial and Administrative Operations
- Expense Claims and Receipts: Mobile receipt uploads, configurable policy thresholds by category, and multi-tier approval.
- Employee Loans and Salary Advances: Configurable EMI schedules, interest-free policies, and automated payroll deduction.
- IT Asset Tracking: Asset assignment lifecycle (laptops, monitors, identification tokens).
- Fast-Track Exit Management: Two-working-day full-and-final settlement execution, automated Gratuity computation (5-year rule for regular staff, 1-year pro-rata rule for FTEs), and one-click relieving documentation.

### 3.3. Phase 3: Talent, Hiring and Growth
- Lightweight Applicant Tracking System (ATS): Job post management, career page generation, candidate pipeline tracking, and offer letter generation with auto-conversion to onboarding.
- Performance and Appraisals: OKR tracking, self-review, 360-degree peer feedback, manager review, and rating-to-salary revision pipeline.
- Project Timesheets: Client- and project-level hourly logging with billable utilization metrics.

### 3.4. Phase 4: Market Differentiators
- WhatsApp and Voice Attendance Bot: Attendance punch, leave application, and payslip retrieval via WhatsApp integration.
- Tablet Attendance Kiosk: Touchless facial recognition clock-in terminal for office and factory entrances.
- Instant Background Verification (BGV): Automated API verification of PAN, Aadhaar, bank account name match, and UAN employment history.
- Multi-Company CA Partner Console: Consolidated multi-client workspace for accounting firms managing multiple MSME entities.

---

## 4. Non-Functional Requirements (NFR)
- Security: End-to-end multi-tenant data isolation using Spring Hibernate `@TenantId`. Role-based access control across all endpoints.
- Performance: Monthly payroll run calculation for 250 employees must execute in under 5 seconds.
- Auditability: Complete, append-only audit trail for all salary revisions, payroll approvals, and statutory configuration modifications.
- Portability: Decoupled client architecture supporting cloud deployment or single-container self-hosted environments.
- Zero Hardcoding: All statutory ceilings, percentages, tax brackets, and state rules must be stored in database tables with effective date ranges.
