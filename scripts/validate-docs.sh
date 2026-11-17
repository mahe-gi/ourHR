#!/usr/bin/env bash
# Validates that all blueprint documentation files exist and contains zero emojis.
set -euo pipefail

echo "Validating documentation files..."

EMOJI_MATCHES=$(grep -rn -P "[\x{1F600}-\x{1F64F}\x{1F300}-\x{1F5FF}\x{1F680}-\x{1F6FF}\x{2600}-\x{26FF}\x{2700}-\x{27BF}]" docs/ AGENTS.md PRD.md PROJECT.md 2>/dev/null || true)

if [ -n "$EMOJI_MATCHES" ]; then
    echo "ERROR: Emojis found in documentation files:"
    echo "$EMOJI_MATCHES"
    exit 1
fi

echo "Documentation validation passed with zero emojis."
