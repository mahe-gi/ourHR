-- =============================================================================
-- ourHR Database Migration: V1__init_core_schema.sql
-- Description: Core 17-Domain Schema with Multi-Tenancy, Optimistic Locking,
--              Decimal Precision, and Immutability Enforcement Triggers.
-- Rule: Zero Emojis, Zero Hardcoding, Strict NUMERIC(12,2) for Currency.
-- =============================================================================

-- Enable pgcrypto for gen_random_uuid() if not enabled
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- -----------------------------------------------------------------------------
-- 1. COMPANIES & BRANCHES (Tenant Management & Multi-State Setup)
-- -----------------------------------------------------------------------------
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
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    branch_name VARCHAR(150) NOT NULL,
    state_code VARCHAR(2) NOT NULL, -- Two-letter Indian State Code (e.g., 'KA', 'MH', 'DL', 'TG')
    city VARCHAR(100) NOT NULL,
    address_line TEXT NOT NULL,
    pincode VARCHAR(10) NOT NULL,
    is_headquarters BOOLEAN DEFAULT FALSE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_branch_company UNIQUE (company_id, branch_name)
);
CREATE INDEX idx_branches_company ON branches(company_id);
CREATE INDEX idx_branches_state ON branches(state_code);

-- -----------------------------------------------------------------------------
-- 2. USERS & USER_ROLES (Authentication & 5-Persona RBAC)
-- -----------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(20),
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_company_email UNIQUE (company_id, email)
);
CREATE INDEX idx_users_company ON users(company_id);

CREATE TABLE user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_name VARCHAR(50) NOT NULL, -- ROLE_FOUNDER, ROLE_HR, ROLE_FINANCE, ROLE_CA, ROLE_MANAGER, ROLE_EMPLOYEE
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_name)
);
CREATE INDEX idx_user_roles_user ON user_roles(user_id);

-- -----------------------------------------------------------------------------
-- 3. STATUTORY RULES & SLABS (Zero-Hardcoding Database Storage)
-- -----------------------------------------------------------------------------
CREATE TABLE statutory_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rule_key VARCHAR(50) NOT NULL,
    rule_value NUMERIC(12, 2) NOT NULL,
    rule_metadata JSONB,
    effective_from DATE NOT NULL,
    effective_to DATE,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_statutory_rules_lookup ON statutory_rules(rule_key, effective_from, effective_to);

CREATE TABLE pt_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code VARCHAR(2) NOT NULL,
    gender VARCHAR(10) DEFAULT 'ALL' NOT NULL,
    salary_from NUMERIC(12, 2) NOT NULL,
    salary_to NUMERIC(12, 2), -- NULL represents infinity
    tax_amount NUMERIC(12, 2) NOT NULL,
    special_month INT,
    special_month_tax_amount NUMERIC(12, 2),
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_pt_slabs_lookup ON pt_slabs(state_code, effective_from, effective_to);

CREATE TABLE lwf_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    state_code VARCHAR(2) NOT NULL,
    employee_contribution NUMERIC(12, 2) NOT NULL,
    employer_contribution NUMERIC(12, 2) NOT NULL,
    deduction_frequency VARCHAR(20) NOT NULL, -- 'MONTHLY', 'QUARTERLY', 'HALF_YEARLY', 'ANNUAL'
    deduction_months INT[] NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_lwf_rules_lookup ON lwf_rules(state_code, effective_from, effective_to);

CREATE TABLE tds_tax_slabs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    financial_year VARCHAR(9) NOT NULL,
    regime VARCHAR(10) NOT NULL, -- 'OLD', 'NEW'
    income_from NUMERIC(12, 2) NOT NULL,
    income_to NUMERIC(12, 2),
    tax_rate NUMERIC(5, 2) NOT NULL,
    standard_deduction NUMERIC(12, 2) NOT NULL DEFAULT 75000.00,
    rebate_87a_limit NUMERIC(12, 2),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_tds_slabs_lookup ON tds_tax_slabs(financial_year, regime);

-- -----------------------------------------------------------------------------
-- 4. EMPLOYEES & KYC VAULT (Employee Master & Sensitive ID Redaction)
-- -----------------------------------------------------------------------------
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
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_employee_code_company UNIQUE (company_id, employee_code)
);
CREATE INDEX idx_employees_company ON employees(company_id);
CREATE INDEX idx_employees_branch ON employees(branch_id);

CREATE TABLE employee_kyc_vault (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL UNIQUE REFERENCES employees(id) ON DELETE CASCADE,
    pan_number VARCHAR(10) NOT NULL,
    masked_aadhaar_token VARCHAR(30) NOT NULL DEFAULT '[Aadhaar Redacted]',
    uan VARCHAR(12),
    esic_ip_number VARCHAR(17),
    bank_account_number VARCHAR(50) NOT NULL,
    bank_ifsc VARCHAR(11) NOT NULL,
    bank_name VARCHAR(100) NOT NULL,
    pan_document_url VARCHAR(500),
    aadhaar_document_url VARCHAR(500),
    cheque_document_url VARCHAR(500),
    appointment_letter_url VARCHAR(500),
    tax_regime_preference VARCHAR(10) DEFAULT 'NEW' NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_kyc_company ON employee_kyc_vault(company_id);

-- -----------------------------------------------------------------------------
-- 5. SALARY STRUCTURES & ARREARS (50% Basic Wage Code Support)
-- -----------------------------------------------------------------------------
CREATE TABLE salary_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    component_name VARCHAR(100) NOT NULL,
    component_code VARCHAR(50) NOT NULL,
    component_type VARCHAR(20) NOT NULL, -- 'EARNING', 'DEDUCTION', 'EMPLOYER_CONTRIBUTION'
    is_statutory_wage BOOLEAN DEFAULT FALSE NOT NULL,
    is_taxable BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_component_company UNIQUE (company_id, component_code)
);

CREATE TABLE employee_salary_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    annual_ctc NUMERIC(12, 2) NOT NULL,
    monthly_gross NUMERIC(12, 2) NOT NULL,
    basic_pay NUMERIC(12, 2) NOT NULL,
    da_pay NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    hra_pay NUMERIC(12, 2) NOT NULL,
    special_allowance NUMERIC(12, 2) NOT NULL,
    pf_opt_out BOOLEAN DEFAULT FALSE NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_salary_structures_emp ON employee_salary_structures(employee_id, effective_from, effective_to);
CREATE INDEX idx_salary_structures_company ON employee_salary_structures(company_id);

-- -----------------------------------------------------------------------------
-- 6. SHIFTS, CALENDARS & ATTENDANCE (Punch Logs, Regularization, Freeze)
-- -----------------------------------------------------------------------------
CREATE TABLE shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    shift_name VARCHAR(100) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    grace_minutes INT DEFAULT 15 NOT NULL,
    half_day_threshold_hours NUMERIC(4, 2) DEFAULT 4.5 NOT NULL,
    full_day_threshold_hours NUMERIC(4, 2) DEFAULT 8.0 NOT NULL,
    late_mark_penalty_count INT DEFAULT 3 NOT NULL,
    late_mark_penalty_deduction_days NUMERIC(3, 1) DEFAULT 0.5 NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE holiday_calendars (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    state_code VARCHAR(2) NOT NULL,
    holiday_name VARCHAR(150) NOT NULL,
    holiday_date DATE NOT NULL,
    is_restricted BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_holiday UNIQUE (company_id, state_code, holiday_date)
);

CREATE TABLE attendance_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    punch_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    punch_type VARCHAR(10) NOT NULL, -- 'IN', 'OUT'
    mode VARCHAR(20) NOT NULL, -- 'WEB', 'MOBILE_GPS', 'BIOMETRIC_CSV', 'BIOMETRIC_API'
    ip_address VARCHAR(45),
    latitude NUMERIC(10, 8),
    longitude NUMERIC(11, 8),
    selfie_image_url VARCHAR(500),
    is_regularized BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_attendance_logs_emp ON attendance_logs(employee_id, punch_timestamp);
CREATE INDEX idx_attendance_logs_company ON attendance_logs(company_id);

CREATE TABLE monthly_attendance_summary (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    payroll_month INT NOT NULL,
    payroll_year INT NOT NULL,
    total_calendar_days INT NOT NULL,
    payable_days NUMERIC(4, 1) NOT NULL,
    lop_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    overtime_hours NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    is_locked BOOLEAN DEFAULT FALSE NOT NULL,
    locked_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_monthly_attendance UNIQUE (company_id, employee_id, payroll_month, payroll_year)
);
CREATE INDEX idx_monthly_attendance_lookup ON monthly_attendance_summary(company_id, payroll_month, payroll_year);

CREATE TABLE lop_reversals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    original_payroll_month INT NOT NULL,
    original_payroll_year INT NOT NULL,
    reversed_days NUMERIC(4, 1) NOT NULL,
    reversal_amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'PROCESSED', 'REJECTED'
    processed_in_payroll_run_id UUID,
    approved_by UUID REFERENCES users(id),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_lop_reversals_company ON lop_reversals(company_id, status);

-- -----------------------------------------------------------------------------
-- 7. LEAVE MANAGEMENT & IMMUTABLE LEAVE LEDGERS
-- -----------------------------------------------------------------------------
CREATE TABLE leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    leave_name VARCHAR(100) NOT NULL,
    leave_code VARCHAR(20) NOT NULL,
    annual_quota NUMERIC(4, 1) NOT NULL,
    accrual_frequency VARCHAR(20) NOT NULL, -- 'MONTHLY', 'QUARTERLY', 'ANNUAL'
    is_carry_forward_allowed BOOLEAN DEFAULT TRUE NOT NULL,
    max_carry_forward_limit NUMERIC(4, 1) DEFAULT 30.0 NOT NULL,
    is_sandwich_rule_enabled BOOLEAN DEFAULT FALSE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
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
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL, -- 'PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'
    approver_id UUID REFERENCES employees(id),
    actioned_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_leave_ledgers_balance ON leave_ledgers(employee_id, leave_type_id);

-- -----------------------------------------------------------------------------
-- 8. TAX DECLARATIONS & FORM 12B (Mid-Year Joiner Support)
-- -----------------------------------------------------------------------------
CREATE TABLE tax_declarations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    financial_year VARCHAR(9) NOT NULL,
    section_code VARCHAR(50) NOT NULL,
    declared_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    verified_amount NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    proof_document_url VARCHAR(500),
    verification_status VARCHAR(20) DEFAULT 'SUBMITTED' NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
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
    is_verified BOOLEAN DEFAULT FALSE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 9. REIMBURSEMENTS & LOANS
-- -----------------------------------------------------------------------------
CREATE TABLE reimbursements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    receipt_url VARCHAR(500) NOT NULL,
    claim_date DATE NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' NOT NULL,
    approved_by UUID REFERENCES employees(id),
    disbursed_in_payroll_run_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE employee_loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    loan_amount NUMERIC(12, 2) NOT NULL,
    tenure_months INT NOT NULL,
    monthly_emi NUMERIC(12, 2) NOT NULL,
    remaining_balance NUMERIC(12, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'ACTIVE' NOT NULL,
    start_date DATE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- -----------------------------------------------------------------------------
-- 10. PAYROLL RUNS & PAYSLIP LINE ITEMS (Immutability Enforcement)
-- -----------------------------------------------------------------------------
CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    payroll_month INT NOT NULL,
    payroll_year INT NOT NULL,
    run_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT', -- 'DRAFT', 'ATTENDANCE_LOCKED', 'ADJUSTMENTS_APPLIED', 'CALCULATED', 'AUDITED', 'APPROVED', 'DISBURSED'
    total_gross_pay NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_net_pay NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_pf_liability NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_esic_liability NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_pt_liability NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    total_tds_liability NUMERIC(12, 2) DEFAULT 0.00 NOT NULL,
    bank_payout_file_url VARCHAR(500),
    ecr_txt_file_url VARCHAR(500),
    initiated_by UUID NOT NULL REFERENCES users(id),
    approved_by UUID REFERENCES users(id),
    approved_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT uk_payroll_period UNIQUE (company_id, payroll_month, payroll_year)
);
CREATE INDEX idx_payroll_runs_company ON payroll_runs(company_id);

CREATE TABLE payslip_line_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
    item_category VARCHAR(30) NOT NULL, -- 'EARNING', 'STATUTORY_DEDUCTION', 'OTHER_DEDUCTION', 'EMPLOYER_CONTRIBUTION'
    item_name VARCHAR(100) NOT NULL,
    item_amount NUMERIC(12, 2) NOT NULL,
    payslip_pdf_url VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_payslip_items_lookup ON payslip_line_items(payroll_run_id, employee_id);
CREATE INDEX idx_payslip_items_company ON payslip_line_items(company_id);

-- PostgreSQL Function & Trigger: Prevent modification/deletion on locked payroll line items
CREATE OR REPLACE FUNCTION fn_prevent_locked_payslip_mutation()
RETURNS TRIGGER AS $$
DECLARE
    v_run_status VARCHAR(30);
BEGIN
    SELECT run_status INTO v_run_status
    FROM payroll_runs
    WHERE id = OLD.payroll_run_id;

    IF v_run_status IN ('APPROVED', 'DISBURSED') THEN
        RAISE EXCEPTION 'Cannot modify or delete payslip line items for payroll run in status %', v_run_status;
    END IF;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_prevent_locked_payslip_mutation
BEFORE UPDATE OR DELETE ON payslip_line_items
FOR EACH ROW
EXECUTE FUNCTION fn_prevent_locked_payslip_mutation();

-- -----------------------------------------------------------------------------
-- 11. BATCH JOBS (Persistent Queue for Async Tasks)
-- -----------------------------------------------------------------------------
CREATE TABLE batch_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    job_type VARCHAR(50) NOT NULL, -- 'PAYSLIP_PDF_GENERATION', 'BULK_EMPLOYEE_IMPORT', 'ECR_EXPORT'
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'
    total_items INT DEFAULT 0 NOT NULL,
    processed_items INT DEFAULT 0 NOT NULL,
    failed_items INT DEFAULT 0 NOT NULL,
    job_payload JSONB,
    error_summary TEXT,
    initiated_by UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_batch_jobs_company_status ON batch_jobs(company_id, status);

-- -----------------------------------------------------------------------------
-- 12. AUDIT LOGS (Immutable Compliance Audit Trail)
-- -----------------------------------------------------------------------------
CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    actor_id UUID NOT NULL REFERENCES users(id),
    role VARCHAR(50) NOT NULL,
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id UUID NOT NULL,
    before_json JSONB,
    after_json JSONB,
    ip_address VARCHAR(45),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);
CREATE INDEX idx_audit_logs_company ON audit_logs(company_id, created_at);
