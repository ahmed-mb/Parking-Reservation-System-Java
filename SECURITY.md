# Security Policy

## Reporting a vulnerability

If you find a security issue, please email the maintainer rather than
opening a public issue. We aim to triage within two business days.

## Supported versions

| Version | Supported          |
|---------|--------------------|
| 1.x     | Yes                |
| 0.x     | No (pre-release)   |

## Supply-chain controls in CI

Every push to `main` runs:

- **gitleaks** — scans diffs for accidentally committed secrets.
- **CodeQL** — static analysis on Java + JavaScript, on push and weekly.
- **Trivy** — scans the production Docker image. The build fails on any
  CRITICAL CVE in OS packages or Java libraries. HIGH-severity findings
  are surfaced via SARIF upload to the GitHub Security tab (visible to
  maintainers but not blocking) and are scheduled for remediation in the
  next sprint following the vendor's patch release. This split is
  deliberate: CRITICAL findings (remote code execution, authentication
  bypass) are stop-ship; HIGH findings are tracked and patched as
  Dependabot opens version-bump PRs against the parent BOM.
- **license-maven-plugin** — fails the build if any transitive
  dependency is GPL/AGPL/LGPL/SSPL-licensed.
- **CycloneDX SBOM** — published as a CI artifact and as a release
  attestation, signed with cosign keyless via GitHub Actions OIDC.
- **npm audit (production deps only)** — fails the build on HIGH or
  CRITICAL advisories in runtime dependencies. See the section below
  for why dev-only advisories are tracked separately.

## Known accepted advisories (dev-only)

These advisories are reported by `npm audit` but live in tooling that
**does not ship in the production artifact**. Only the contents of
`frontend/dist/` (the static build output) are copied into the Spring
Boot JAR and the Docker image; `vite`, `vitest`, `esbuild`, and friends
are dev dependencies used at build/test time only.

| ID              | Component | Severity | Reach    | Status |
|-----------------|-----------|----------|----------|--------|
| GHSA-67mh-4wv8-2f99 | esbuild (via vite, vitest, vite-node, @vitest/coverage-v8) | Moderate | Dev server only | Accepted; bump planned with Vite 6+ migration |

These are intentionally not gated by `npm audit --audit-level=high
--omit=dev` because they do not affect customers. The full picture is
visible to anyone running `npm audit` locally.

The dev-tooling stack will be migrated to Vite 6 (or later) in a
follow-up release; that change is breaking and is being planned
separately to keep the production-readiness work atomic.

## Cryptographic supply-chain

Released container images are signed with [cosign][cosign] using
keyless OIDC signing tied to this repository's GitHub Actions identity.
Verification:

```bash
cosign verify ghcr.io/<owner>/parking-reservation-system:<tag> \
  --certificate-identity-regexp 'github.com/<owner>/parking-reservation-system' \
  --certificate-oidc-issuer https://token.actions.githubusercontent.com
```

The CycloneDX SBOM is attached to each image as a signed attestation:

```bash
cosign download attestation \
  ghcr.io/<owner>/parking-reservation-system@<digest>
```

[cosign]: https://github.com/sigstore/cosign
