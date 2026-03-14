# Security Audit Report

Date: 2026-03-13

## Scope

- Frontend dependency audit (`frontend`, npm)
- Backend dependency audit (Maven/OWASP dependency-check)

## Frontend Results

### Commands Run

```bash
cd frontend
npm audit --json
npm audit fix
npm install
npm run build
npm audit --json
```

### Initial State

- 26 vulnerabilities total
- 14 high, 3 moderate, 9 low
- Primary root cause: `react-scripts@5.0.1` transitive dependency chain

### Remediation Applied

- Migrated frontend build tool from CRA to Vite
- Updated `frontend/package.json`:
  - Removed `react-scripts`
  - Added `vite@^7.1.7` and `@vitejs/plugin-react@^5.0.4`
  - Upgraded `@types/node` to `^20.17.30`
  - Updated scripts to `vite` (`start/dev/build`)
- Added Vite config and entry files:
  - `frontend/vite.config.ts`
  - `frontend/index.html`
  - `frontend/src/main.tsx`
- Updated env usage:
  - `frontend/src/services/contractService.ts`
  - `process.env.REACT_APP_API_URL` -> `import.meta.env.VITE_API_URL`

### Final State

- `npm audit --json`: 0 vulnerabilities
- `npm run build`: success

## Backend Results

### Command Run

```bash
mvn -q org.owasp:dependency-check-maven:check -DskipTests
```

### Current Blocker

- OWASP dependency-check failed due to NVD API rate limit:
  - `NvdApiException: NVD Returned Status Code: 429`
- No backend CVE report was produced in this run.

### Recommended Next Run

Use NVD API key to avoid rate limiting:

```bash
mvn -q org.owasp:dependency-check-maven:check \
  -DskipTests \
  -DnvdApiKey=$NVD_API_KEY
```

Optionally persist in CI/CD environment secret variables and run nightly.

