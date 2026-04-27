#!/usr/bin/env bash
# Local CI-quality gate runner.
# Mirrors the most important checks from .github/workflows/ci.yml.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

RUN_SHARED=true
RUN_ANDROID=true
RUN_FUNCTIONS=true

if [[ "${1:-}" == "--shared-only" ]]; then
  RUN_ANDROID=false
  RUN_FUNCTIONS=false
elif [[ "${1:-}" == "--android-only" ]]; then
  RUN_SHARED=false
  RUN_FUNCTIONS=false
elif [[ "${1:-}" == "--functions-only" ]]; then
  RUN_SHARED=false
  RUN_ANDROID=false
fi

echo "== SkyOS local CI gate =="
echo "Root: $ROOT_DIR"
echo

if [[ "$RUN_SHARED" == true ]]; then
  echo "[shared] Running :shared:allTests"
  ./gradlew :shared:allTests --no-daemon
  echo
fi

if [[ "$RUN_ANDROID" == true ]]; then
  echo "[android] Running :androidApp:lintDebug :shared:compileKotlinMetadata"
  ./gradlew :androidApp:lintDebug :shared:compileKotlinMetadata --no-daemon
  echo
fi

if [[ "$RUN_FUNCTIONS" == true ]]; then
  echo "[functions] Running npm ci && npm test"
  npm ci --prefix functions
  npm test --prefix functions
  echo
fi

echo "LOCAL CI GATE PASSED"
