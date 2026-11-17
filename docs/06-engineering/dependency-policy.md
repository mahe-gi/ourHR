# Dependency Management Policy

- Pin exact versions in Maven pom.xml and npm package.json.
- Run automated security scans (OWASP Dependency Check, npm audit) on every build.
- Prohibit redundant utility packages when standard runtime libraries suffice.
