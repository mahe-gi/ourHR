# Structured Logging Guidelines

- Format: JSON layout via Logback encoder.
- Fields: timestamp, level, logger, thread, company_id, user_id, trace_id, message.
- Sanitization: Passwords, bank account numbers, PAN, and Aadhaar tokens must be redacted from log outputs.
