# Data Flow Architecture

```
Punches (Web / GPS / Biometric)
       |
       v
attendance_logs (Raw Punches)
       |
       | [Monthly Freeze Cut-off Action]
       v
monthly_attendance_summary (Locked: Payable Days, LOP Days, OT Hours)
       |
       v
+--------------------------------------------------------------+
|                     6-Step Payroll Run                       |
| 1. Sync Attendance & LOP Reversals                           |
| 2. Prorate Joiners & Exits                                   |
| 3. Add Variable Pay & Overtime                               |
| 4. Deduct Loan EMIs & Add Reimbursements                     |
| 5. Execute Statutory Engine (EPF, ESIC, PT, TDS)             |
| 6. Audit Anomaly Diffs vs. Preceding Month                   |
+------------------------------+-------------------------------+
                               | Maker-Checker Approval
       +-----------------------+-----------------------+
       |                                               |
       v                                               v
payslip_line_items (Immutable)             Compliance Exporters
       |                                   - EPFO ECR .txt File
       v                                   - Bank Payment CSV (NEFT/RTGS)
S3 PDF Payslip Generator (Async)           - Form 24Q e-TDS File
```
