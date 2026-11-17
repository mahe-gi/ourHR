#!/usr/bin/env bash
# Runs local formatting, linting, and quality checks.
set -euo pipefail

echo "Running quality verification checks..."
# Check for emojis across repo
bash scripts/validate-docs.sh
echo "All quality gates satisfied."
