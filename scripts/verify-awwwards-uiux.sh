#!/usr/bin/env bash
# Runs the strict Awwwards-Level UI/UX Quality Gate Checker
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
python3 "${SCRIPT_DIR}/verify-awwwards-uiux.py" "$@"
