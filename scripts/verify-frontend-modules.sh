#!/usr/bin/env bash
# Runs the Vetan Frontend Module Completeness & Development Verification Auditor
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/verify-frontend-modules.py" "$@"
