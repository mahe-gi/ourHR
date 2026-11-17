# Database Design Specification

Project: ourHR
Status: Approved
Target Database: PostgreSQL 15+
Primary ORM: Spring Data JPA / Hibernate with `@TenantId` Multi-Tenancy

---

## 1. Architectural Principles

### 1.1. Tenant Isolation
Every business table must include a `company_id` column referencing `companies.id`. At the JPA layer, Hibernate `@TenantId` is declared on all multi-tenant entities to ensure automatic filtering on all select, update, and delete queries.

### 1.2. Financial Precision
All monetary balances, allowances, deductions, and tax values must use `NUMERIC(12, 2)` (mapped to Java `BigDecimal`). Floating point numbers are strictly forbidden.

### 1.3. Zero Hardcoding Policy
Statutory formulas, contribution rates, wage ceilings, and state tax slabs are stored as versioned records in relational tables (`statutory_rules`, `pt_slabs`, `lwf_rules`, `tds_tax_slabs`) utilizing `effective_from` and `effective_to` date ranges.

### 1.4. Immutability and Audit Trails
- Completed payroll runs and generated payslip line items are strictly immutable.
- Leave balances are calculated dynamically from an append-only transaction ledger (`leave_ledgers`).
- System-wide modifications are recorded in an append-only `audit_logs` table.

### 1.5. Object Storage Decoupling
Binary data (KYC proofs, biometric selfie punches, signed appointment letters, generated PDF payslips) is stored externally in S3-compatible object storage. Relational tables store only the S3 object key or URL string.

---

## 2. Refined 16-Table Phase 1 Core Schema

```
                                  +-------------------+
                                  |     companies     |
                                  +---------+---------+
                                            |
         +----------------------------------+----------------------------------+
         |                                  |                                  |
+--------v--------+                +--------v--------+                +--------v--------+
|    branches     |                |      users      |                | statutory_rules |
+--------+--------+                +--------+--------+                +-----------------+
         |                                  |
         |                         +--------v--------+
         |                         |   user_roles    |
         |                         +-----------------+
         |
+--------v--------+                +-----------------+                +-----------------+
|    employees    |                |    pt_slabs     |                |  tds_tax_slabs  |
+--------+--------+                +-----------------+                +-----------------+
         |
         +-----------------------+-----------------------+-----------------------+
         |                       |                       |                       |
+--------v--------+     +--------v--------+     +--------v--------+     +--------v--------+
| employee_kyc_   |     | salary_struc-   |     | attendance_     |     | leave_ledgers   |
| vault           |     | tures & revs    |     | logs & summary  |     | & applications  |
+-----------------+     +-----------------+     +-----------------+     +-----------------+
                                 |                       |                       |
                        +--------v--------+     +--------v--------+              |
                        |  payroll_runs   |     |  lop_reversals  |              |
                        +--------+--------+     +-----------------+              |
                                 |                                               |
                        +--------v--------+     +-----------------+              |
                        | payslip_line_   |     | prev_employer_  |              |
                        | items           |     | income (12B)    |              |
                        +-----------------+     +-----------------+              |
                                                                                 |
                                                +-----------------+              |
                                                |   audit_logs    |<-------------+
                                                +-----------------+
```

---

### Table 1: `companies` and `branches`
Stores tenant organizations and geographical work locations.

```sql
CREATE TABLE companies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    legal_name VARCHAR(255) NOT NULL,
    trade_name VARCHAR(255),
    pan_number VARCHAR(10) NOT NULL UNIQUE,
    tan_number VARCHAR(10) NOT NULL UNIQUE,
    gstin VARCHAR(15),
    pf_establishment_code VARCHAR(20),
    esic_code VARCHAR(17),
    lin VARCHAR(20),
    signatory_name VARCHAR(255) NOT NULL,
    signatory_designation VARCHAR(100) NOT NULL,
    signatory_signature_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    branch_name VARCHAR(150) NOT NULL,
    state_code VARCHAR(2) NOT NULL, -- e.g., 'KA', 'MH', 'DL'
    city VARCHAR(100) NOT NULL,
    address_line TEXT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    is_headquarters BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_branch_company UNIQUE (company_id, branch_name)
);
CREATE INDEX idx_branches_company ON branches(company_id);
```

---

### Table 2: `users` and `user_roles`
Authentication credentials, multi-tenant binding, and role memberships.

```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_company_email UNIQUE (company_id, email)
);
CREATE INDEX idx_users_company ON users(company_id);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL, -- ROLE_FOUNDER, ROLE_HR, ROLE_FINANCE, ROLE_CA, ROLE_MANAGER, ROLE_EMPLOYEE
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_name)
);
```

---

### Table 3: `statutory_rules`
Date-effective configuration values for Indian labor laws.

```sql
CREATE TABLE statutory_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_key VARCHAR(50) NOT NULL, -- EPF_WAGE_CEILING, ESIC_WAGE_CEILING, BASIC_WAGE_PERCENTAGE_MIN
    rule_value NUMERIC(12, 2) NOT NULL,
    rule_metadata JSONB, -- Additional coefficients (e.g., EPS split 8.33%, EPF split 3.67%)
    effective_from DATE NOT NULL,
    effective_to DATE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_statutory_rules_lookup ON statutory_rules(rule_key, effective_from, effective_to);
```

---

### Table 4: `pt_slabs` and `lwf_rules`
State-specific, gender-aware Professional Tax and Labour Welfare Fund brackets.

```sql
CREATE TABLE pt_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code VARCHAR(2) NOT NULL,
    gender VARCHAR(10) DEFAULT 'ALL', -- 'MALE', 'FEMALE', 'ALL'
    salary_from NUMERIC(12, 2) NOT NULL,
    salary_to NUMERIC(12, 2), -- NULL represents infinity
    tax_amount NUMERIC(12, 2) NOT NULL,
    special_month INT, -- e.g., 2 for February in Maharashtra
    special_month_tax_amount NUMERIC(12, 2),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_pt_slabs_lookup ON pt_slabs(state_code, effective_from, effective_to);

CREATE TABLE lwf_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code VARCHAR(2) NOT NULL,
    employee_contribution NUMERIC(12, 2) NOT NULL,
    employer_contribution NUMERIC(12, 2) NOT NULL,
    deduction_frequency VARCHAR(20) NOT NULL, -- 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUAL'
    deduction_months INT[] NOT NULL, -- e.g., '{6, 12}' for June and December
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

### Table 5: `tds_tax_slabs`
Brackets for Old vs. New Tax Regimes with effective date bounds.

```sql
CREATE TABLE tds_tax_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financial_year VARCHAR(9) NOT NULL, -- e.g., '2026-2027'
    regime VARCHAR(10) NOT NULL, -- 'OLD', 'NEW'
    income_from NUMERIC(12, 2) NOT NULL,
    income_to NUMERIC(12, 2),
    tax_rate NUMERIC(5, 2) NOT NULL, -- e.g., 5.00, 10.00, 20.00, 30.00
    standard_deduction NUMERIC(12, 2) NOT NULL DEFAULT 75000.00,
    rebate_87a_limit NUMERIC(12, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_tds_slabs ON tds_tax_slabs(financial_year, regime);
```

---

### Table 6: `employees` and `employee_kyc_vault`
Core employee master data and encrypted/masked compliance document references.

```sql
CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    branch_id UUID NOT NULL REFERENCES branches(id),
    user_id UUID UNIQUE REFERENCES users(id),
    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(10) NOT NULL,
    date_of_joining DATE NOT NULL,
    employment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- 'ACTIVE', 'PROBATION', 'NOTICE_PERIOD', 'RESIGNED', 'TERMINATED'
    worker_type VARCHAR(30) NOT NULL DEFAULT 'REGULAR', -- 'REGULAR', 'FTE', 'INTERN', 'CONSULTANT_194J'
    reporting_manager_id UUID REFERENCES employees(id),
    department VARCHAR(100),
    designation VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employee_code_company UNIQUE (company_id, employee_code)
);
CREATE INDEX idx_employees_company ON employees(company_id);
CREATE INDEX idx_employees_branch ON employees(branch_id);

CREATE TABLE employee_kyc_vault (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL UNIQUE REFERENCES employees(id) ON DELETE CASCADE,
    pan_number VARCHAR(10) NOT NULL,
    masked_aadhaar_token VARCHAR(20),
    uan VARCHAR(12),
    esic_ip_number VARCHAR(17),
    bank_account_number VARCHAR(50) NOT NULL,
    bank_ifsc VARCHAR(11) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    pan_document_url VARCHAR(500),
    aadhaar_document_url VARCHAR(500),
    cheque_document_url VARCHAR(500),
    appointment_letter_url VARCHAR(500),
    tax_regime_preference VARCHAR(10) DEFAULT 'NEW', -- 'OLD', 'NEW'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_kyc_company ON employee_kyc_vault(company_id);
```

---

### Table 7: `salary_components` and `employee_salary_structures`
Effective-dated CTC structures supporting retroactive arrears calculation.

```sql
CREATE TABLE salary_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    component_name VARCHAR(100) NOT NULL,
    component_code VARCHAR(50) NOT NULL, -- BASIC, DA, HRA, SPECIAL_ALLOWANCE, MEDICAL_REIMBURSEMENT
    component_type VARCHAR(20) NOT NULL, -- 'EARNING', 'DEDUCTION', 'EMPLOYER_CONTRIBUTION'
    is_statutory_wage BOOLEAN DEFAULT FALSE,
    is_taxable BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_component_company UNIQUE (company_id, component_code)
);

CREATE TABLE employee_salary_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    annual_ctc NUMERIC(12, 2) NOT NULL,
    monthly_gross NUMERIC(12, 2) NOT NULL,
    basic_pay NUMERIC(12, 2) NOT NULL,
    da_pay NUMERIC(12, 2) DEFAULT 0.00,
    hra_pay NUMERIC(12, 2) NOT NULL,
    special_allowance NUMERIC(12, 2) NOT NULL,
    pf_opt_out BOOLEAN DEFAULT FALSE,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_salary_structures_emp ON employee_salary_structures(employee_id, effective_from, effective_to);
CREATE INDEX idx_salary_structures_company ON employee_salary_structures(company_id);
```

---

### Table 8: `shifts` and `holiday_calendars`
Shift definitions, grace allowances, and state holiday associations.

```sql
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    shift_name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    grace_minutes INT DEFAULT 15,
    half_day_threshold_hours NUMERIC(4, 2) DEFAULT 4.5,
    full_day_threshold_hours NUMERIC(4, 2) DEFAULT 8.0,
    late_mark_penalty_count INT DEFAULT 3,
    late_mark_penalty_deduction_days NUMERIC(3, 1) DEFAULT 0.5,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE holiday_calendars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    state_code VARCHAR(2) NOT NULL,
    holiday_name VARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL,
    is_restricted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_holiday UNIQUE (company_id, state_code, holiday_date)
);
```

---

### Table 9: `attendance_logs` and `monthly_attendance_summary`
Punches with GPS coordinates and S3 selfie URLs, plus the locked monthly payroll snapshot.

```sql
CREATE TABLE attendance_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    punch_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    punch_type VARCHAR(10) NOT NULL, -- 'IN', 'OUT'
    mode VARCHAR(20) NOT NULL, -- 'WEB', 'MOBILE_GPS', 'BIOMETRIC'
    ip_address VARCHAR(45),
    latitude NUMERIC(10, 8),
    longitude NUMERIC(11, 8),
    selfie_image_url VARCHAR(500),
    is_regularized BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_attendance_logs_emp ON attendance_logs(employee_id, punch_timestamp);
CREATE INDEX idx_attendance_logs_company ON attendance_logs(company_id);

CREATE TABLE monthly_attendance_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    payroll_month INT NOT NULL, -- 1 to 12
    payroll_year INT NOT NULL,
    total_calendar_days INT NOT NULL,
    payable_days NUMERIC(4, 1) NOT NULL,
    lop_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    overtime_hours NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    is_locked BOOLEAN DEFAULT FALSE,
    locked_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_monthly_attendance UNIQUE (company_id, employee_id, payroll_month, payroll_year)
);
```

---

### Table 10: `lop_reversals`
Tracks retroactive LOP credit when missed punches or leaves are approved after monthly freeze.

```sql
CREATE TABLE lop_reversals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    original_payroll_month INT NOT NULL,
    original_payroll_year INT NOT NULL,
    reversed_days NUMERIC(4, 1) NOT NULL,
    reversal_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'PROCESSED', 'REJECTED'
    processed_in_payroll_run_id UUID,
    approved_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_lop_reversals_company ON lop_reversals(company_id, status);
```

---

### Table 11: `leave_types`, `leave_applications`, and `leave_ledgers`
Policy configuration and the immutable transaction ledger for all leave actions.

```sql
CREATE TABLE leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    leave_name VARCHAR(100) NOT NULL,
    leave_code VARCHAR(20) NOT NULL, -- CL, SL, EL, MATERNITY, PATERNITY, COMP_OFF
    annual_quota NUMERIC(4, 1) NOT NULL,
    accrual_frequency VARCHAR(20) NOT NULL, -- 'MONTHLY', 'QUARTERLY', 'ANNUAL'
    is_carry_forward_allowed BOOLEAN DEFAULT TRUE,
    max_carry_forward_limit NUMERIC(4, 1) DEFAULT 30.0,
    is_sandwich_rule_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_leave_type UNIQUE (company_id, leave_code)
);

CREATE TABLE leave_applications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days NUMERIC(4, 1) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    approver_id UUID REFERENCES employees(id),
    actioned_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leave_ledgers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_types(id),
    transaction_type VARCHAR(20) NOT NULL, -- 'ACCRUAL', 'CONSUMPTION', 'ADJUSTMENT', 'CARRY_FORWARD', 'ENCASHMENT'
    days NUMERIC(4, 1) NOT NULL, -- Positive for accrual, negative for consumption
    leave_application_id UUID REFERENCES leave_applications(id),
    reference_note VARCHAR(255),
    effective_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_leave_ledgers_balance ON leave_ledgers(employee_id, leave_type_id);
```

---

### Table 12: `tax_declarations` and `previous_employer_income` (Form 12B)
Investment declarations and prior employment records for mid-year joiners.

```sql
CREATE TABLE tax_declarations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL, -- '2026-2027'
    section_code VARCHAR(50) NOT NULL, -- 80C, 80D, 80CCD_1B, 24B_HOME_LOAN
    declared_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    verified_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    proof_document_url VARCHAR(500),
    verification_status VARCHAR(20) DEFAULT 'SUBMITTED', -- 'DRAFT', 'SUBMITTED', 'VERIFIED', 'REJECTED'
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE previous_employer_income (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL UNIQUE REFERENCES employees(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL,
    gross_salary NUMERIC(12, 2) NOT NULL,
    pf_deducted NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    pt_deducted NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    tds_deducted NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    form_12b_document_url VARCHAR(500),
    is_verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

### Table 13: `reimbursements` and `employee_loans`
Expense claims and salary advance EMI schedules.

```sql
CREATE TABLE reimbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL, -- TRAVEL, FOOD, INTERNET, FUEL
    amount NUMERIC(12, 2) NOT NULL,
    receipt_url VARCHAR(500) NOT NULL,
    claim_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'DISBURSED', 'REJECTED'
    approved_by UUID REFERENCES employees(id),
    disbursed_in_payroll_run_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    loan_amount NUMERIC(12, 2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_emi NUMERIC(12, 2) NOT NULL,
    remaining_balance NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE', -- 'ACTIVE', 'COMPLETED', 'WRITTEN_OFF'
    start_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

### Table 14: `payroll_runs`
State machine coordinating the 6-step monthly payroll process.

```sql
CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    payroll_month INT NOT NULL,
    payroll_year INT NOT NULL,
    run_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'ATTENDANCE_LOCKED', 'ADJUSTMENTS_APPLIED', 'CALCULATED', 'AUDITED', 'APPROVED', 'DISBURSED'
    total_gross_pay NUMERIC(12, 2) DEFAULT 0.00,
    total_net_pay NUMERIC(12, 2) DEFAULT 0.00,
    total_pf_liability NUMERIC(12, 2) DEFAULT 0.00,
    total_esic_liability NUMERIC(12, 2) DEFAULT 0.00,
    total_pt_liability NUMERIC(12, 2) DEFAULT 0.00,
    total_tds_liability NUMERIC(12, 2) DEFAULT 0.00,
    bank_payout_file_url VARCHAR(500),
    ecr_txt_file_url VARCHAR(500),
    initiated_by UUID NOT NULL REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_payroll_period UNIQUE (company_id, payroll_month, payroll_year)
);
CREATE INDEX idx_payroll_runs_company ON payroll_runs(company_id);
```

---

### Table 15: `payslip_line_items`
Immutable, audit-ready line items generated per employee per payroll run.

```sql
CREATE TABLE payslip_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    item_category VARCHAR(30) NOT NULL, -- 'EARNING', 'STATUTORY_DEDUCTION', 'OTHER_DEDUCTION', 'EMPLOYER_CONTRIBUTION'
    item_name VARCHAR(100) NOT NULL,
    item_amount NUMERIC(12, 2) NOT NULL,
    payslip_pdf_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payslip_items_lookup ON payslip_line_items(payroll_run_id, employee_id);
CREATE INDEX idx_payslip_items_company ON payslip_line_items(company_id);
```

---

### Table 16: `audit_logs`
Immutable compliance log recording all state transitions and system modifications.

```sql
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL, -- e.g., 'SALARY_REVISION_SUBMITTED', 'PAYROLL_APPROVED'
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    before_json JSONB,
    after_json JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_audit_logs_company ON audit_logs(company_id, created_at);
```
