# Data Dictionary Summary

## Key Data Types & Constraints
- UUID: Primary keys generated via gen_random_uuid() or application UUID v4.
- NUMERIC(12, 2): All monetary fields (annual_ctc, basic_pay, hra_pay, gross_pay, net_pay, tax_amount).
- NUMERIC(4, 1): All day count fields (payable_days, lop_days, leave days).
- VARCHAR(10): PAN numbers (validated via regex `^[A-Z]{5}[0-9]{4}[A-Z]{1}$`).
- VARCHAR(11): IFSC codes (validated via regex `^[A-Z]{4}0[A-Z0-9]{6}$`).
- VARCHAR(12): UAN numbers (12-digit numeric string).
