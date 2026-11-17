# External File Integration Specifications

## 1. EPFO ECR Text File Structure
- Format: Hash-delimited `#~#` without header row.
- 25 mandatory columns: UAN, Member Name, Gross Wages, EPF Wages, EPS Wages, EDLI Wages, EE Share, ER Share, EPS Share, NCP Days, Refund of Advances.

## 2. Bank Bulk Payout CSV
- Columns: Beneficiary Account Number, Beneficiary Name, IFSC Code, Transaction Amount (in rupees and paise), Narration (e.g., Salary Nov 2026).
