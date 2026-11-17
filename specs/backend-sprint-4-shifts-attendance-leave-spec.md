# Backend Sprint 4 Specification: Shifts, Attendance & Leave Ledger Engine

Author: agent-0-orchestrator
Project: ourHR (Indian MSME HRMS & Statutory Payroll)
Sprint: Backend Sprint 4
Status: Approved

## 1. Objective and Scope
Backend Sprint 4 delivers the Shifts, GPS/Biometric Attendance recording, Monthly Attendance Freeze engine, and Immutable Leave Ledger model featuring the Indian Sandwich rule.

## 2. Statutory Laws and Domain Logic
1. Sandwich Rule Law:
   - If `leave_types.is_sandwich_rule_enabled = true`, when an employee applies for leave on Friday and Monday (or spanning weekends/holidays), intermediate non-working days (Saturday, Sunday, and public holidays) are automatically counted as leave days.
2. Mutable Leave Balance Trap Prevention:
   - Leave balances are NEVER stored as mutable counters on an employee record.
   - All balance changes are immutable rows in `leave_ledgers`:
     * Positive `days` for ACCRUAL (+1.5, +1.0)
     * Negative `days` for CONSUMPTION (-1.0, -0.5)
     * `SUM(days)` computes the exact balance at any point in time with full auditability.
3. Monthly Attendance Freeze:
   - At month-end, HR/Finance triggers the attendance freeze job for (payroll_month, payroll_year).
   - Once `monthly_attendance_summary.is_locked = true`, punches for that month cannot be altered, and payable days and LOP days become the immutable input for Sprint 5 Payroll Engine.
4. Mode Support:
   - WEB (browser punch)
   - MOBILE_GPS (with latitude, longitude, and selfie verification)
   - BIOMETRIC_CSV (bulk upload)
   - BIOMETRIC_API (integration)

## 3. Database Entities (Owner: agent-1-database)
- `Shift`: `com.ourhr.attendance.Shift` (maps to `shifts`)
- `AttendanceLog`: `com.ourhr.attendance.AttendanceLog` (maps to `attendance_logs`)
- `MonthlyAttendanceSummary`: `com.ourhr.attendance.MonthlyAttendanceSummary` (maps to `monthly_attendance_summary`)
- `HolidayCalendar`: `com.ourhr.attendance.HolidayCalendar` (maps to `holiday_calendars`)
- `LeaveType`: `com.ourhr.leave.LeaveType` (maps to `leave_types`)
- `LeaveApplication`: `com.ourhr.leave.LeaveApplication` (maps to `leave_applications`)
- `LeaveLedger`: `com.ourhr.leave.LeaveLedger` (maps to `leave_ledgers`)

## 4. Services & Controllers (Owner: agent-2-core-hr-attendance)
- `AttendanceService` & `AttendanceController`: punchIn, punchOut, list logs, bulk CSV upload.
- `AttendanceFreezeService` & `AttendanceFreezeController`: freeze monthly attendance, lock status.
- `ShiftService` & `ShiftController`: configure shifts, grace periods, penalty thresholds.
- `LeaveLedgerService` & `LeaveController`: apply leave (with Sandwich rule), approve/reject, accruals, ledger queries.

## 5. QA Acceptance Criteria (Owner: agent-5-qa-security)
- Unit and integration tests verifying:
  1. Sandwich rule accurately counts intermediate holidays/weekends when enabled, and excludes them when disabled.
  2. Leave ledger credit/debit arithmetic computes exact remaining balance.
  3. Over-consumption of leave beyond balance throws validation error.
  4. Monthly attendance summary locks records and prevents edits.
  5. 100% green tests with zero emojis.
