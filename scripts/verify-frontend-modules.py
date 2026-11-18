#!/usr/bin/env python3
"""
Vetan Frontend Module Completeness & Development Verification Engine
-------------------------------------------------------------------
Audits the React + MUI + JavaScript frontend codebase to verify whether all 8 core
enterprise modules, required views, domain widgets, and live backend API integrations
are fully developed, production-ready, or missing.

Strict Rules:
- ZERO EMOJIS anywhere in output or code.
- Returns exit code 0 if all 8 modules are 100% complete.
- Returns exit code 1 if any module is incomplete or missing.
"""

import os
import re
import sys

FRONTEND_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "frontend")
SRC_DIR = os.path.join(FRONTEND_DIR, "src")

# ANSI Color Output for CLI
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
BOLD = "\033[1m"
RESET = "\033[0m"

# Specification of all 8 Core Enterprise Modules in Vetan
MODULE_SPECS = [
    {
        "id": "MOD-1",
        "name": "Authentication & Tenant Setup",
        "persona": "ROLE_FOUNDER, ROLE_HR, ALL",
        "required_pages": ["pages/Login.jsx", "pages/Register.jsx"],
        "required_components": ["components/ui/VetanButton.jsx", "components/ui/VetanInput.jsx"],
        "required_api_calls": ["/auth/register-company", "/auth/login", "/auth/me"],
        "description": "Company registration, JWT authentication, current user profile, tenant session."
    },
    {
        "id": "MOD-2",
        "name": "Company & Multi-State Branches",
        "persona": "ROLE_FOUNDER, ROLE_HR",
        "required_pages": ["pages/Branches.jsx"],
        "required_components": ["components/domain/BranchSelector.jsx"],
        "required_api_calls": ["/companies/current", "/branches"],
        "description": "Multi-state branch management with Indian state codes (KA, MH, DL) and headquarters."
    },
    {
        "id": "MOD-3",
        "name": "Employee Master & KYC Vault",
        "persona": "ROLE_HR, ROLE_FOUNDER",
        "required_pages": ["pages/Employees.jsx", "pages/Onboarding.jsx"],
        "required_components": ["components/domain/WageCodeValidator.jsx", "components/domain/KycMaskedViewer.jsx"],
        "required_api_calls": ["/employees"],
        "description": "Employee directory, candidate onboarding wizard, 50% wage ratio, masked Aadhaar/PAN."
    },
    {
        "id": "MOD-4",
        "name": "Shifts, Attendance & Leave Ledger",
        "persona": "ROLE_HR, ROLE_EMPLOYEE",
        "required_pages": ["pages/Attendance.jsx", "pages/Leaves.jsx", "pages/AttendanceFreeze.jsx"],
        "required_components": ["components/domain/SandwichRuleVisualizer.jsx"],
        "required_api_calls": ["/attendance/punch", "/attendance/freeze", "/leaves/apply"],
        "description": "Web/GPS punch, shifts, immutable leave ledger, Sandwich rule, monthly attendance freeze."
    },
    {
        "id": "MOD-5",
        "name": "6-Step Payroll Stepper Engine",
        "persona": "ROLE_FINANCE, ROLE_FOUNDER",
        "required_pages": ["pages/Payroll.jsx", "pages/PayrollStepperPage.jsx", "pages/Consultants.jsx"],
        "required_components": ["components/domain/PayrollStepper.jsx", "components/domain/StatutoryBreakdownCard.jsx"],
        "required_api_calls": ["/payroll/initiate", "/payroll", "/calculate"],
        "description": "6-step state machine runner (DRAFT to DISBURSED), arrears, 194J/194C consultant payouts."
    },
    {
        "id": "MOD-6",
        "name": "Indian Statutory Calculation Engine",
        "persona": "ROLE_FINANCE, ROLE_CA",
        "required_pages": ["pages/PayrollStepperPage.jsx"],
        "required_components": ["components/domain/StatutoryBreakdownCard.jsx", "components/domain/WageCodeValidator.jsx"],
        "required_api_calls": ["/calculate"],
        "description": "Dynamic EPF 12%/8.33% split, ESIC 6-month rule, state PT/LWF, dual-regime TDS + Form 12B."
    },
    {
        "id": "MOD-7",
        "name": "Compliance Exporters & CA Workspace",
        "persona": "ROLE_CA, ROLE_FINANCE, ROLE_FOUNDER",
        "required_pages": ["pages/CaWorkspace.jsx"],
        "required_components": ["components/domain/EcrPreviewModal.jsx", "components/domain/BankExportModal.jsx"],
        "required_api_calls": ["/ca/compliance-summary", "/ca/tax-audit", "/exporters/ecr", "/exporters/bank"],
        "description": "Dedicated read-only CA portal, EPFO ECR 2.0 text preview, bank CSV exports (HDFC/ICICI/SBI)."
    },
    {
        "id": "MOD-8",
        "name": "Employee Self-Service (ESS)",
        "persona": "ROLE_EMPLOYEE",
        "required_pages": ["pages/EssHome.jsx", "pages/EssPayslips.jsx", "pages/EssTax.jsx"],
        "required_components": ["components/domain/SandwichRuleVisualizer.jsx"],
        "required_api_calls": ["/attendance/punch", "/leaves/apply"],
        "description": "Web clock-in/out, leave balance cards, PDF payslip downloads, Old vs New tax declaration."
    }
]

class ModuleCompletenessAuditor:
    def __init__(self, src_dir):
        self.src_dir = os.path.abspath(src_dir)
        self.module_results = []

    def check_file(self, rel_path):
        full_path = os.path.join(self.src_dir, rel_path)
        if not os.path.exists(full_path):
            return False, "File missing"
        
        with open(full_path, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()

        if len(content.strip()) < 50:
            return False, "Empty or stub file (< 50 bytes)"
        
        if "TODO: implement" in content.lower() or "throw new Error('Not implemented')" in content:
            return False, "Contains unimplemented TODO stub"

        return True, "Fully implemented"

    def check_api_integration(self, api_endpoints):
        found_endpoints = set()
        for root, _, files in os.walk(self.src_dir):
            for f in files:
                if f.endswith((".js", ".jsx")):
                    p = os.path.join(root, f)
                    with open(p, "r", encoding="utf-8", errors="ignore") as fl:
                        txt = fl.read()
                        for ep in api_endpoints:
                            if ep in txt:
                                found_endpoints.add(ep)
        
        missing = [ep for ep in api_endpoints if ep not in found_endpoints]
        return missing

    def run_audit(self):
        print(f"\n{BOLD}{CYAN}================================================================={RESET}")
        print(f"{BOLD}{CYAN}   VETAN: FRONTEND MODULE COMPLETENESS & VERIFICATION AUDITOR   {RESET}")
        print(f"{BOLD}{CYAN}   Verifying All 8 Modules Across 5 Personas & Live APIs        {RESET}")
        print(f"{BOLD}{CYAN}================================================================={RESET}\n")

        if not os.path.exists(self.src_dir):
            print(f"{YELLOW}[NOTICE] Frontend source directory not created yet at: {self.src_dir}{RESET}")
            print(f"Modules Status: 0 / {len(MODULE_SPECS)} Complete (0.0% developed)\n")
            print("Ready to scaffold and track completion across all 8 modules.\n")
            return 1

        total_modules = len(MODULE_SPECS)
        completed_modules = 0

        for mod in MODULE_SPECS:
            issues = []
            
            # Check required pages
            for page in mod["required_pages"]:
                ok, msg = self.check_file(page)
                if not ok:
                    issues.append(f"Page '{page}': {msg}")

            # Check required components
            for comp in mod["required_components"]:
                ok, msg = self.check_file(comp)
                if not ok:
                    issues.append(f"Component '{comp}': {msg}")

            # Check API integration
            missing_apis = self.check_api_integration(mod["required_api_calls"])
            if missing_apis:
                issues.append(f"Missing API bindings: {', '.join(missing_apis)}")

            is_complete = len(issues) == 0
            if is_complete:
                completed_modules += 1

            self.module_results.append({
                "mod": mod,
                "is_complete": is_complete,
                "issues": issues
            })

        # Render Table
        print(f"{'MOD ID':<8} | {'MODULE NAME':<34} | {'STATUS':<14} | {'DETAILS'}")
        print("-" * 80)

        for res in self.module_results:
            mod = res["mod"]
            if res["is_complete"]:
                status_str = f"{GREEN}[COMPLETE]{RESET}"
                detail_str = f"All {len(mod['required_pages'])} pages and {len(mod['required_components'])} components active"
            else:
                status_str = f"{RED}[INCOMPLETE]{RESET}"
                detail_str = "; ".join(res["issues"][:2])
                if len(res["issues"]) > 2:
                    detail_str += f" (+{len(res['issues']) - 2} more)"

            print(f"{mod['id']:<8} | {mod['name']:<34} | {status_str:<23} | {detail_str}")

        print("-" * 80)
        percentage = (completed_modules / total_modules) * 100.0
        color = GREEN if completed_modules == total_modules else YELLOW if completed_modules > 0 else RED
        print(f"\n{BOLD}Completeness Score: {color}{completed_modules} / {total_modules} Modules Complete ({percentage:.1f}%){RESET}\n")

        if completed_modules == total_modules:
            print(f"{BOLD}{GREEN}[STATUS: ALL MODULES 100% FULLY DEVELOPED & INTEGRATED]{RESET}\n")
            return 0
        else:
            print(f"{BOLD}{YELLOW}[STATUS: DEVELOPMENT IN PROGRESS - {total_modules - completed_modules} module(s) remaining]{RESET}\n")
            return 1

if __name__ == "__main__":
    src_path = sys.argv[1] if len(sys.argv) > 1 else SRC_DIR
    auditor = ModuleCompletenessAuditor(src_path)
    sys.exit(auditor.run_audit())
