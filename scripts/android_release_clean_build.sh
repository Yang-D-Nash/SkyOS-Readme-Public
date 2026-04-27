#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

BUILD_FILE="androidApp/build.gradle.kts"
APK_METADATA="androidApp/build/outputs/apk/release/output-metadata.json"
BUNDLE_FILE="androidApp/build/outputs/bundle/release/androidApp-release.aab"
APK_FILE="androidApp/build/outputs/apk/release/androidApp-release.apk"

expected_version_code="$(sed -n 's/^[[:space:]]*versionCode[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$BUILD_FILE" | head -1)"
expected_version_name="$(sed -n 's/^[[:space:]]*versionName[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$BUILD_FILE" | head -1)"

if [[ -z "$expected_version_code" || -z "$expected_version_name" ]]; then
  echo "Could not read versionCode/versionName from $BUILD_FILE." >&2
  exit 1
fi

echo "Cleaning Android build outputs..."
rm -rf androidApp/build/outputs

echo "Building Android release APK and AAB for version ${expected_version_name} (${expected_version_code})..."
./gradlew :androidApp:clean :androidApp:assembleRelease :androidApp:bundleRelease

if [[ ! -f "$APK_FILE" ]]; then
  echo "Missing release APK: $APK_FILE" >&2
  exit 1
fi

if [[ ! -f "$BUNDLE_FILE" ]]; then
  echo "Missing release AAB: $BUNDLE_FILE" >&2
  exit 1
fi

if [[ ! -f "$APK_METADATA" ]]; then
  echo "Missing release metadata: $APK_METADATA" >&2
  exit 1
fi

if ! grep -q "\"versionCode\": ${expected_version_code}" "$APK_METADATA"; then
  echo "Release APK metadata does not match versionCode ${expected_version_code}." >&2
  cat "$APK_METADATA" >&2
  exit 1
fi

if ! grep -q "\"versionName\": \"${expected_version_name}\"" "$APK_METADATA"; then
  echo "Release APK metadata does not match versionName ${expected_version_name}." >&2
  cat "$APK_METADATA" >&2
  exit 1
fi

BUNDLE_METADATA="androidApp/build/outputs/bundle/release/output-metadata.json"
if [[ -f "$BUNDLE_METADATA" ]]; then
  if ! grep -q "\"versionCode\": ${expected_version_code}" "$BUNDLE_METADATA"; then
    echo "Release bundle output-metadata does not match versionCode ${expected_version_code}." >&2
    cat "$BUNDLE_METADATA" >&2
    exit 1
  fi
  if ! grep -q "\"versionName\": \"${expected_version_name}\"" "$BUNDLE_METADATA"; then
    echo "Release bundle output-metadata does not match versionName ${expected_version_name}." >&2
    cat "$BUNDLE_METADATA" >&2
    exit 1
  fi
fi

echo
echo "Fresh Android release artifacts:"
ls -lh "$APK_FILE" "$BUNDLE_FILE"
echo
echo "Release metadata (APK):"
cat "$APK_METADATA"

./scripts/verify_android_release_artifacts.sh
