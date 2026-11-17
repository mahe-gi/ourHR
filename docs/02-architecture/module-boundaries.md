# Module Boundaries & Isolation

1. Attendance to Payroll Boundary:
   - The Payroll module does not query raw attendance punches directly.
   - It consumes exclusively from monthly_attendance_summary after a formal lock action.
2. Leave to Attendance Boundary:
   - Approved leaves update attendance records by marking days as on-leave during attendance lock calculation.
3. Statutory Engine Boundary:
   - The statutory calculation engine is a pure domain service: given a gross remuneration breakdown, work branch state, and tax regime, it returns calculated PF, ESIC, PT, and TDS. It does not perform database mutations.
4. Tenant Boundary:
   - Every service operation validates that the requesting actor belongs to the tenant matching the target entity.
