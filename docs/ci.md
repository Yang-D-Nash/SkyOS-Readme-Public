# CI Guide

This document describes what the `CI` workflow checks and when each job runs.

## Workflow Scope

The workflow is defined in `.github/workflows/ci.yml` and runs on:

- every pull request
- every push to `main`

Concurrency is enabled (`ci-${{ github.ref }}`), so older in-progress runs for the same ref are cancelled.

For local preflight parity, use:

```bash
./scripts/ci_local_gate.sh
```

## Job Overview

### 1) `changes` (always)

Detects changed areas using `dorny/paths-filter@v3` and exports booleans:

- `shared`
- `android`
- `functions`

These outputs gate all downstream jobs.
The job also writes a short step summary with the detected booleans.

### 2) `shared-tests` (conditional)

Runs only when `shared == true`.

Command:

```bash
./gradlew :shared:allTests --no-daemon
```

Purpose:

- enforce KMP shared test health
- catch cross-target compile/test regressions early

### 3) `android-lint` (conditional, named "Android quality gate")

Runs only when `android == true`.

Command:

```bash
./gradlew :androidApp:lintDebug :shared:compileKotlinMetadata --no-daemon
```

Purpose:

- enforce Android lint quality
- ensure shared metadata compilation remains valid for Android integration

Artifacts:

- uploads `androidApp/build/reports/lint-results-debug.*` on every run (`if: always()`)

### 4) `functions-tests` (conditional)

Runs only when `functions == true`.

Commands (in `functions/`):

```bash
npm ci
npm test
```

Purpose:

- enforce backend test health for Firebase Functions

### 5) `ci-summary` (always)

Runs with `if: always()` after all gates and writes a compact summary to the workflow UI:

- changed-area booleans (`shared/android/functions`)
- resulting job outcomes (`success/skipped/failure`)

## Path Filters

Current filter rules:

- `shared`:
  - `shared/**`
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradle/**`
  - `gradle.properties`
- `android`:
  - `androidApp/**`
  - `shared/**`
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradle/**`
  - `gradle.properties`
- `functions`:
  - `functions/**`

## Practical Notes

- If a job is skipped, that is expected when its area was not touched.
- When changing build logic (`gradle*`, root build files), both `shared` and `android` gates can trigger.
- Keep commands in CI aligned with local release/readiness scripts when adding new quality gates.

## Local Usage

Use the local gate script to mirror CI checks before pushing:

```bash
./scripts/ci_local_gate.sh
```

Optional focused runs:

```bash
./scripts/ci_local_gate.sh --shared-only
./scripts/ci_local_gate.sh --android-only
./scripts/ci_local_gate.sh --functions-only
```
