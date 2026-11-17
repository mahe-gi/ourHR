# Backend Sprint 7 Specification: Exporters, PDF Engine & External CA API

Author: agent-0-orchestrator
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Sprint: Backend Sprint 7
Status: Approved

## 1. Objective and Scope
Backend Sprint 7 delivers the final compliance outputs and statutory exports of the ourHR platform:
1. Official EPFO ECR 2.0 `.txt` Exporter (25 columns separated by `#~#`).
2. Bank NEFT/RTGS CSV Payout Exporters (HDFC, ICICI, SBI, Axis).
3. Asynchronous PDF Payslip Generator backed by `BatchJobService`.
4. Dedicated External Chartered Accountant (CA) Workspace Portal APIs (`ROLE_CA`).

## 2. Technical Specifications
1. EPFO ECR 2.0 Specification:
   - File extension: `.txt`
   - Delimiter: `#~#`
   - Strict 25 columns per record.
   - Values formatted to 2 decimals without floating artifacts.
   - Non-contributing period (NCP) days populated from LOP days.
2. Bank Exporters:
   - CSV format for HDFC, ICICI, SBI, and Axis bank payout gateways.
   - Account numbers, amounts, and IFSC codes validated against KYC records.
3. PDF Payslip Engine:
   - Renders company branding, employee details, earnings, statutory deductions, and net pay in numbers and words.
   - Asynchronous batch job queuing (`batch_jobs` entity) to avoid HTTP request thread starvation when rendering hundreds of payslips.
   - Uploads to storage and updates `payslip_pdf_url`.
4. CA Workspace API:
   - Dedicated read-only portal for `ROLE_CA`, `ROLE_FOUNDER`, and `ROLE_FINANCE`.
   - Comprehensive monthly statutory compliance overview, ECR verification, bank payout approval audit, and Form 24Q TDS summaries.

## 3. Engineering Deliverables (Owner: agent-4-exporters-ca)
- `EcrExporterService` in `com.ourhr.exporter.ecr`
- `BankExporterService` in `com.ourhr.exporter.bank`
- `PdfPayslipService` in `com.ourhr.exporter.pdf`
- `CaPortalService` & `CaPortalController` in `com.ourhr.exporter.ca`

## 4. QA Acceptance Criteria (Owner: agent-5-qa-security)
- Unit & integration tests verifying:
  1. ECR .txt 25-column format and `#~#` delimiter parsing.
  2. Bank CSV line counts, formatting, and total amount matching net pay.
  3. PDF generation byte structure and batch job execution.
  4. CA portal read-only RBAC protection.
  5. Zero emojis and 100% green tests.
