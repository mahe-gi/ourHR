# Antigravity Database Rule: PostgreSQL Integrity

1. Data Types: Use UUID for primary keys, NUMERIC(12,2) for currency, and TIMESTAMP WITH TIME ZONE for dates.
2. Constraints: Enforce foreign keys, unique constraints, and check constraints explicitly.
3. Leave Ledger: Never maintain a mutable remaining_balance column. Leave balances are dynamically derived from SUM(days) in leave_ledgers.
4. Audit Trail: All status changes and sensitive modifications must write to audit_logs.
