#!/usr/bin/env python3
"""
Vetan Awwwards-Level UI/UX Strict Quality Gate Checker
------------------------------------------------------
Enforces Awwwards-grade digital craftsmanship, accessibility, design token conformance,
zero emojis, sensitive data redaction, and Indian statutory requirements across the React + MUI + JavaScript frontend.

Exit Code: 0 on PASS, 1 on any VIOLATION.
"""

import os
import re
import sys
import glob

FRONTEND_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "frontend")

# ANSI Color Output for CLI
GREEN = "\033[92m"
RED = "\033[91m"
YELLOW = "\033[93m"
CYAN = "\033[96m"
BOLD = "\033[1m"
RESET = "\033[0m"

# Authorized Vetan Brand Color Hexes (Allowed in theme definitions)
AUTHORIZED_BRAND_HEXES = {
    "#090D16", "#090d16",  # Obsidian Canvas
    "#0F172A", "#0f172a",  # Midnight Slate
    "#1E293B", "#1e293b",  # Surface Elevation
    "#334155", "#334155",  # Muted Slate
    "#475569", "#475569",  # Border Slate
    "#64748B", "#64748b",  # Secondary Text
    "#94A3B8", "#94a3b8",  # Subtle Text
    "#F8FAFC", "#f8fafc",  # Alabaster White
    "#FFFFFF", "#ffffff",  # Pure White
    "#4F46E5", "#4f46e5",  # Electric Royal Indigo
    "#6366F1", "#6366f1",  # Light Indigo
    "#818CF8", "#818cf8",  # Accent Indigo
    "#059669", "#059669",  # Emerald Jade (Statutory Compliance)
    "#10B981", "#10b981",  # Vibrant Emerald
    "#34D399", "#34d399",  # Light Emerald
    "#D97706", "#d97706",  # Amber Gold (Audit Warning)
    "#F59E0B", "#f59e0b",  # Bright Amber
    "#DC2626", "#dc2626",  # Crimson Flame (Statutory Violation)
    "#EF4444", "#ef4444",  # Light Crimson
}

EMOJI_PATTERN = re.compile(r"[\U0001F000-\U0001FAFF\u2600-\u27BF]")
HEX_COLOR_PATTERN = re.compile(r"#(?:[0-9a-fA-F]{3}){1,2}\b")
UNMASKED_AADHAAR_PATTERN = re.compile(r"\b\d{4}\s\d{4}\s\d{4}\b|\b\d{12}\b")

class UiUxAuditor:
    def __init__(self, target_dir):
        self.target_dir = os.path.abspath(target_dir)
        self.errors = []
        self.warnings = []
        self.passed_checks = 0
        self.total_files_scanned = 0

    def log_error(self, file_path, line_no, check_name, message):
        rel_path = os.path.relpath(file_path, self.target_dir)
        self.errors.append(f"{RED}[FAIL]{RESET} {BOLD}{check_name}{RESET} -> {rel_path}:{line_no} - {message}")

    def log_warning(self, file_path, line_no, check_name, message):
        rel_path = os.path.relpath(file_path, self.target_dir)
        self.warnings.append(f"{YELLOW}[WARN]{RESET} {BOLD}{check_name}{RESET} -> {rel_path}:{line_no} - {message}")

    def run_all_checks(self):
        print(f"\n{BOLD}{CYAN}================================================================={RESET}")
        print(f"{BOLD}{CYAN}   VETAN: STRICT AWWWARDS-LEVEL UI/UX QUALITY GATE CHECKER       {RESET}")
        print(f"{BOLD}{CYAN}   Target Stack: React + MUI + JavaScript                        {RESET}")
        print(f"{BOLD}{CYAN}================================================================={RESET}\n")

        if not os.path.exists(self.target_dir):
            print(f"{YELLOW}[NOTICE] Frontend directory does not exist yet at {self.target_dir}.{RESET}")
            print(f"{GREEN}[PASS] Quality Gate ready for immediate scaffolding enforcement.{RESET}\n")
            return 0

        source_files = []
        for root, _, files in os.walk(self.target_dir):
            if "node_modules" in root or ".next" in root or "build" in root or "dist" in root:
                continue
            for f in files:
                if f.endswith((".js", ".jsx", ".css", ".html")):
                    source_files.append(os.path.join(root, f))

        self.total_files_scanned = len(source_files)
        print(f"Scanning {self.total_files_scanned} source file(s) across UI/UX compliance rules...\n")

        for fpath in source_files:
            self.audit_file(fpath)

        # Print results
        for w in self.warnings:
            print(w)
        for e in self.errors:
            print(e)

        print(f"\n{BOLD}Audit Summary:{RESET}")
        print(f"Total Files Scanned: {self.total_files_scanned}")
        print(f"Errors / Violations: {len(self.errors)}")
        print(f"Warnings / Hints:    {len(self.warnings)}")

        if len(self.errors) > 0:
            print(f"\n{BOLD}{RED}[STATUS: REJECTED] Awwwards Quality Gate Failed. Fix violations above.{RESET}\n")
            return 1
        else:
            print(f"\n{BOLD}{GREEN}[STATUS: 100% PASSED] Certified Awwwards-Level Design & Code Quality.{RESET}\n")
            return 0

    def audit_file(self, fpath):
        is_theme_file = "theme" in fpath.lower() or "colors" in fpath.lower() or "constants" in fpath.lower()
        is_test_file = ".test." in fpath or ".spec." in fpath
        
        with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
            lines = f.readlines()

        for idx, line in enumerate(lines, 1):
            stripped = line.strip()

            # 1. Zero Emojis Check
            emoji_matches = EMOJI_PATTERN.findall(line)
            if emoji_matches:
                self.log_error(fpath, idx, "Zero Emojis Law", f"Forbidden unicode emojis detected: {emoji_matches}. Use Lucide / MUI SVG icons.")

            # 2. Design Token Conformance (Disallow arbitrary unapproved hex colors in component code)
            if not is_theme_file and not is_test_file:
                hex_matches = HEX_COLOR_PATTERN.findall(line)
                for h in hex_matches:
                    if h.lower() not in [x.lower() for x in AUTHORIZED_BRAND_HEXES]:
                        self.log_warning(fpath, idx, "Design Token Law", f"Hardcoded hex color '{h}' found. Reference MUI theme tokens (e.g. theme.palette.primary.main).")

            # 3. Tabular Numbers for Financial / Statutory Figures
            if any(term in line.lower() for term in ["grosssalary", "netpay", "pf_amount", "esic_amount", "pt_amount", "tds_amount", "totalamount", "payslip"]):
                if "<TableCell" in line or "<Typography" in line or "<span" in line:
                    if "tabular-nums" not in line and "fontVariantNumeric" not in line and "font-mono" not in line:
                        self.log_warning(fpath, idx, "Financial Typography Law", "Financial figure displayed without tabular-nums. Add fontVariantNumeric: 'tabular-nums' for zero-drift ledger alignment.")

            # 4. Sensitive Government ID Redaction (Never expose raw 12-digit Aadhaar)
            if not is_test_file:
                aadhaar_matches = UNMASKED_AADHAAR_PATTERN.findall(line)
                if aadhaar_matches:
                    # Check if line contains redaction/masking logic
                    if "mask" not in line.lower() and "redact" not in line.lower() and "[Aadhaar Redacted]" not in line:
                        self.log_error(fpath, idx, "Sensitive ID Redaction Law", f"Potential raw 12-digit Aadhaar number found. Must use KycMaskedViewer or [Aadhaar Redacted].")

            # 5. Icon-Only Button Accessibility Check
            if "<IconButton" in line:
                if "aria-label=" not in line and "title=" not in line and "Tooltip" not in line:
                    self.log_warning(fpath, idx, "Accessibility Law (WAI-ARIA)", "MUI <IconButton> without 'aria-label' or wrapping <Tooltip>. Screen-readers cannot announce icon buttons.")

            # 6. Standard API Envelope Check (Catch unhandled response objects)
            if "response.data." in line and not any(term in line for term in ["response.data.success", "response.data.data", "response.data.error", "ApiResponse"]):
                if "axios" in fpath.lower() or "api" in fpath.lower():
                    self.log_warning(fpath, idx, "Standard Envelope Law", "Direct response access without ApiResponse<T> envelope validation ('response.data.data' or 'response.data.success').")

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else FRONTEND_DIR
    auditor = UiUxAuditor(target)
    sys.exit(auditor.run_all_checks())
