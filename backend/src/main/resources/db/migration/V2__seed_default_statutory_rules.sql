-- =============================================================================
-- ourHR Database Migration: V2__seed_default_statutory_rules.sql
-- Description: Seed Effective-Dated Indian Statutory Rules for FY 2026-2027.
-- Rule: Zero Emojis, Zero Hardcoding, Strict NUMERIC(12,2) for Currency.
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. STATUTORY RULES (EPF, ESIC, Code on Wages Ratios)
-- -----------------------------------------------------------------------------
INSERT INTO statutory_rules (rule_key, rule_value, rule_metadata, effective_from, effective_to, description)
VALUES
(
    'EPF_WAGE_CEILING',
    15000.00,
    '{"employee_rate": 0.12, "employer_eps_rate": 0.0833, "employer_epf_rate": 0.0367, "edli_rate": 0.005, "admin_rate": 0.005, "admin_min_amount": 500.00}',
    '2024-04-01',
    NULL,
    'Standard statutory EPF monthly wage ceiling of INR 15,000'
),
(
    'EPF_WAGE_CEILING_ENHANCED',
    25000.00,
    '{"employee_rate": 0.12, "employer_eps_rate": 0.0833, "employer_epf_rate": 0.0367, "edli_rate": 0.005, "admin_rate": 0.005}',
    '2025-04-01',
    NULL,
    'Proposed enhanced EPF monthly wage ceiling of INR 25,000'
),
(
    'ESIC_WAGE_CEILING',
    21000.00,
    '{"employee_rate": 0.0075, "employer_rate": 0.0325, "pwd_ceiling": 25000.00}',
    '2024-04-01',
    NULL,
    'ESIC gross monthly wage threshold of INR 21,000 (INR 25,000 for PwD)'
),
(
    'BASIC_WAGE_RATIO_MIN',
    0.50,
    '{"statute": "Code on Wages 2019", "reallocation_target": "STATUTORY_WAGE_BASE"}',
    '2024-04-01',
    NULL,
    'Code on Wages requirement that Basic + DA comprises at least 50% of gross remuneration'
),
(
    'GRATUITY_SERVICE_YEARS_REGULAR',
    5.00,
    '{"days_per_year": 15, "month_divisor": 26}',
    '2024-04-01',
    NULL,
    'Statutory 5-year continuous service threshold for regular employee gratuity'
),
(
    'GRATUITY_SERVICE_YEARS_FTE',
    1.00,
    '{"days_per_year": 15, "month_divisor": 26, "prorata": true}',
    '2024-04-01',
    NULL,
    'Statutory 1-year pro-rata service threshold for Fixed-Term Employees (FTE)'
);

-- -----------------------------------------------------------------------------
-- 2. TDS TAX SLABS (FY 2026-2027: New vs. Old Tax Regimes)
-- -----------------------------------------------------------------------------
-- New Tax Regime (Default)
INSERT INTO tds_tax_slabs (financial_year, regime, income_from, income_to, tax_rate, standard_deduction, rebate_87a_limit)
VALUES
('2026-2027', 'NEW', 0.00, 300000.00, 0.00, 75000.00, 1275000.00),
('2026-2027', 'NEW', 300000.01, 700000.00, 5.00, 75000.00, 1275000.00),
('2026-2027', 'NEW', 700000.01, 1000000.00, 10.00, 75000.00, 1275000.00),
('2026-2027', 'NEW', 1000000.01, 1200000.00, 15.00, 75000.00, 1275000.00),
('2026-2027', 'NEW', 1200000.01, 1500000.00, 20.00, 75000.00, 1275000.00),
('2026-2027', 'NEW', 1500000.01, NULL, 30.00, 75000.00, 1275000.00);

-- Old Tax Regime (Optional)
INSERT INTO tds_tax_slabs (financial_year, regime, income_from, income_to, tax_rate, standard_deduction, rebate_87a_limit)
VALUES
('2026-2027', 'OLD', 0.00, 250000.00, 0.00, 50000.00, 500000.00),
('2026-2027', 'OLD', 250000.01, 500000.00, 5.00, 50000.00, 500000.00),
('2026-2027', 'OLD', 500000.01, 1000000.00, 20.00, 50000.00, 500000.00),
('2026-2027', 'OLD', 1000000.01, NULL, 30.00, 50000.00, 500000.00);

-- -----------------------------------------------------------------------------
-- 3. STATE PROFESSIONAL TAX (PT) SLABS
-- -----------------------------------------------------------------------------
-- Karnataka (KA)
INSERT INTO pt_slabs (state_code, gender, salary_from, salary_to, tax_amount, special_month, special_month_tax_amount, effective_from, effective_to)
VALUES
('KA', 'ALL', 0.00, 14999.99, 0.00, NULL, NULL, '2024-04-01', NULL),
('KA', 'ALL', 15000.00, NULL, 200.00, NULL, NULL, '2024-04-01', NULL);

-- Maharashtra (MH) - Male
INSERT INTO pt_slabs (state_code, gender, salary_from, salary_to, tax_amount, special_month, special_month_tax_amount, effective_from, effective_to)
VALUES
('MH', 'MALE', 0.00, 7500.00, 0.00, NULL, NULL, '2024-04-01', NULL),
('MH', 'MALE', 7500.01, 10000.00, 175.00, NULL, NULL, '2024-04-01', NULL),
('MH', 'MALE', 10000.01, NULL, 200.00, 2, 300.00, '2024-04-01', NULL);

-- Maharashtra (MH) - Female
INSERT INTO pt_slabs (state_code, gender, salary_from, salary_to, tax_amount, special_month, special_month_tax_amount, effective_from, effective_to)
VALUES
('MH', 'FEMALE', 0.00, 25000.00, 0.00, NULL, NULL, '2024-04-01', NULL),
('MH', 'FEMALE', 25000.01, NULL, 200.00, 2, 300.00, '2024-04-01', NULL);

-- Telangana (TG)
INSERT INTO pt_slabs (state_code, gender, salary_from, salary_to, tax_amount, special_month, special_month_tax_amount, effective_from, effective_to)
VALUES
('TG', 'ALL', 0.00, 15000.00, 0.00, NULL, NULL, '2024-04-01', NULL),
('TG', 'ALL', 15000.01, 20000.00, 150.00, NULL, NULL, '2024-04-01', NULL),
('TG', 'ALL', 20000.01, NULL, 200.00, NULL, NULL, '2024-04-01', NULL);

-- Tamil Nadu (TN) - Monthly Equivalent (Half-Yearly converted to monthly brackets)
INSERT INTO pt_slabs (state_code, gender, salary_from, salary_to, tax_amount, special_month, special_month_tax_amount, effective_from, effective_to)
VALUES
('TN', 'ALL', 0.00, 21000.00, 0.00, NULL, NULL, '2024-04-01', NULL),
('TN', 'ALL', 21000.01, 30000.00, 100.00, NULL, NULL, '2024-04-01', NULL),
('TN', 'ALL', 30000.01, 45000.00, 235.00, NULL, NULL, '2024-04-01', NULL),
('TN', 'ALL', 45000.01, 60000.00, 510.00, NULL, NULL, '2024-04-01', NULL),
('TN', 'ALL', 60000.01, 75000.00, 760.00, NULL, NULL, '2024-04-01', NULL),
('TN', 'ALL', 75000.01, NULL, 1025.00, NULL, NULL, '2024-04-01', NULL);

-- -----------------------------------------------------------------------------
-- 4. LABOUR WELFARE FUND (LWF) RULES
-- -----------------------------------------------------------------------------
-- Karnataka (Annual in December)
INSERT INTO lwf_rules (state_code, employee_contribution, employer_contribution, deduction_frequency, deduction_months, effective_from, effective_to)
VALUES
('KA', 20.00, 40.00, 'ANNUAL', '{12}', '2024-04-01', NULL);

-- Maharashtra (Half-Yearly in June and December)
INSERT INTO lwf_rules (state_code, employee_contribution, employer_contribution, deduction_frequency, deduction_months, effective_from, effective_to)
VALUES
('MH', 12.00, 36.00, 'HALF_YEARLY', '{6, 12}', '2024-04-01', NULL);
