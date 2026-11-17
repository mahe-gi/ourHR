# UI/UX Experience Guidelines

## 1. Zero Spreadsheet Ping-Pong Principle
- Provide inline-editable spreadsheet grids on web screens for bulk actions (e.g., variable pay entries, attendance overrides).
- Support standard keyboard shortcuts (Enter, Tab, Arrow navigation) within data grids.

## 2. Role-Switching Header Component
- Users with administrative or manager permissions have a persistent header pill:
  `[ Switch View: Admin Workspace <-> My Self-Service ]`
- Toggling immediately adjusts the active navigation hierarchy without re-authenticating.

## 3. The 6-Step Payroll Stepper UX
- Visual horizontal progress stepper indicating current phase.
- Step 5 (Audit & Variance) must highlight changes using green (+increase) and red (-decrease) tags with tooltips explaining reasons.
- Step 6 requires explicit confirmation before triggering disbursement files.
