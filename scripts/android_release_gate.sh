#!/usr/bin/env bash
# Strict Android release gate:
# 1) Always builds fresh release artifacts from clean outputs
# 2) Verifies APK/AAB match versionName/versionCode from androidApp/build.gradle.kts
# 3) Prints an explicit upload allow/deny result
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "== SkyOS Android release gate =="
echo "Root: $ROOT_DIR"
echo

run_step() {
  local label="$1"
  local command="$2"
  echo "$label"
  "$command"
  echo
}

run_step "[1/2] Clean release build" "./scripts/android_release_clean_build.sh"
run_step "[2/2] Artifact/version verification" "./scripts/verify_android_release_artifacts.sh"

echo "RELEASE GATE PASSED"
echo "Upload is allowed only for the exact artifacts listed above."
