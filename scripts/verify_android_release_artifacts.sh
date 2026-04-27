#!/usr/bin/env bash
# Verifies that the release APK and AAB under androidApp/build/outputs/ match
# versionCode and versionName in androidApp/build.gradle.kts. Run this immediately
# before a Play upload or a manual copy of the AAB/APK to avoid wrong artifacts.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BUILD_FILE="androidApp/build.gradle.kts"
APK_FILE="androidApp/build/outputs/apk/release/androidApp-release.apk"
AAB_FILE="androidApp/build/outputs/bundle/release/androidApp-release.aab"
APK_METADATA="androidApp/build/outputs/apk/release/output-metadata.json"
BUNDLE_METADATA="androidApp/build/outputs/bundle/release/output-metadata.json"

expected_version_code="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$BUILD_FILE" | head -1)"
expected_version_name="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$BUILD_FILE" | head -1)"

if [[ -z "$expected_version_code" || -z "$expected_version_name" ]]; then
  echo "Could not read versionCode/versionName from $BUILD_FILE (source of truth for uploads)." >&2
  exit 1
fi

err=0
fail() {
  echo "verify_android_release: $*" >&2
  err=1
}

echo "== SkyOS Android release verify =="
echo "Expected from $BUILD_FILE: versionName=$expected_version_name  versionCode=$expected_version_code"
echo

if [[ ! -f "$APK_FILE" ]]; then
  fail "Missing APK: $APK_FILE (run ./scripts/android_release_clean_build.sh after release signing is configured)."
fi
if [[ ! -f "$AAB_FILE" ]]; then
  fail "Missing AAB: $AAB_FILE (run ./scripts/android_release_clean_build.sh after release signing is configured)."
fi
if [[ ! -f "$APK_METADATA" ]]; then
  fail "Missing $APK_METADATA (stale or partial build; rebuild release)."
else
  if ! grep -q "\"versionCode\": ${expected_version_code}" "$APK_METADATA"; then
    fail "APK output-metadata.json does not list versionCode ${expected_version_code} — APK does not match $BUILD_FILE."
  fi
  if ! grep -q "\"versionName\": \"${expected_version_name}\"" "$APK_METADATA"; then
    fail "APK output-metadata.json does not list versionName ${expected_version_name} — APK does not match $BUILD_FILE."
  fi
fi

if [[ -f "$BUNDLE_METADATA" ]]; then
  if ! grep -q "\"versionCode\": ${expected_version_code}" "$BUNDLE_METADATA"; then
    fail "Bundle output-metadata.json does not list versionCode ${expected_version_code} — AAB does not match $BUILD_FILE."
  fi
  if ! grep -q "\"versionName\": \"${expected_version_name}\"" "$BUNDLE_METADATA"; then
    fail "Bundle output-metadata.json does not list versionName ${expected_version_name} — AAB does not match $BUILD_FILE."
  fi
elif [[ -f "$AAB_FILE" ]]; then
  echo "Note: $BUNDLE_METADATA not found; AAB is present. Version is still covered by APK metadata and optional aapt2 (AGP may omit bundle metadata in some versions)."
  echo
fi

if [[ -f "$APK_FILE" && $err -eq 0 ]]; then
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    bt_dir=""
    if [[ -d "$ANDROID_HOME/build-tools" ]]; then
      bt_dir=$(ls -1 "$ANDROID_HOME/build-tools" 2>/dev/null | sort -V | tail -1 || true)
    fi
    if [[ -n "$bt_dir" && -f "$ANDROID_HOME/build-tools/$bt_dir/aapt2" ]]; then
      aapt2="$ANDROID_HOME/build-tools/$bt_dir/aapt2"
      # First line of badging contains versionCode and versionName.
      first_line=$("$aapt2" dump badging "$APK_FILE" 2>/dev/null | head -1) || true
      if [[ -z "$first_line" ]]; then
        fail "aapt2 dump badging failed or produced no output (check ANDROID_HOME and build-tools)."
      else
        if ! echo "$first_line" | grep -qF "versionCode='${expected_version_code}'"; then
          fail "aapt2: binary APK does not have versionCode '${expected_version_code}'."
        fi
        if ! echo "$first_line" | grep -qF "versionName='${expected_version_name}'"; then
          fail "aapt2: binary APK does not have versionName '${expected_version_name}'."
        fi
        echo "aapt2 ($aapt2): binary APK version matches $BUILD_FILE."
      fi
    else
      echo "Tip: install Android build-tools under ANDROID_HOME so aapt2 can verify the binary APK, not just metadata."
    fi
  else
    echo "Tip: set ANDROID_HOME so aapt2 can double-check the APK binary."
  fi
fi

if [[ $err -ne 0 ]]; then
  echo >&2
  echo "No upload should use these artifacts until the errors above are resolved." >&2
  exit 1
fi

echo
echo "-- Files to upload (only these paths; same directory from the last clean release build) --"
echo "Play App Bundle (AAB): $ROOT_DIR/$AAB_FILE"
ls -l "$AAB_FILE" | awk '{print "  size: " $5 " bytes; modified: " $6 " " $7 " " $8}'
echo
echo "Sideload / QA APK (optional, not for Play): $ROOT_DIR/$APK_FILE"
ls -l "$APK_FILE" | awk '{print "  size: " $5 " bytes; modified: " $6 " " $7 " " $8}'
echo
if command -v shasum >/dev/null 2>&1; then
  echo "SHA-256 fingerprints (compare after copy or before filing a release note):"
  shasum -a 256 "$AAB_FILE" "$APK_FILE"
else
  echo "SHA-256: install with shasum (OpenBSD) or use certutil; binaries match $BUILD_FILE above."
fi
echo
echo "VERIFY OK — version matches $expected_version_name ($expected_version_code) from $BUILD_FILE"
exit 0
