#!/usr/bin/env bash
# Verifies developer environment prerequisites (Java, Maven, Node, Docker).
set -euo pipefail

echo "Checking environment dependencies..."
command -v java >/dev/null 2>&1 && echo "Java: $(java -version 2>&1 | head -n 1)" || echo "Java: NOT FOUND"
command -v mvn >/dev/null 2>&1 && echo "Maven: $(mvn -version 2>&1 | head -n 1)" || echo "Maven: NOT FOUND"
command -v node >/dev/null 2>&1 && echo "Node: $(node -v)" || echo "Node: NOT FOUND"
command -v docker >/dev/null 2>&1 && echo "Docker: $(docker --version)" || echo "Docker: NOT FOUND"
