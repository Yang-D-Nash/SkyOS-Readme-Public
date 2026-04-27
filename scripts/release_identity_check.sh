#!/usr/bin/env bash
# Verifies the release identity across Android, iOS, Fastlane, Firebase client files, and the runbook.
# Run this before creating or uploading store artifacts to avoid shipping the wrong app or version.
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

EXPECTED_VERSION="$(tr -d '[:space:]' < VERSION)"
EXPECTED_ANDROID_ID="${EXPECTED_ANDROID_ID:-com.nash.skyos}"
EXPECTED_IOS_BUNDLE_ID="${EXPECTED_IOS_BUNDLE_ID:-com.skydown.ios}"
EXPECTED_DISPLAY_NAME="${EXPECTED_DISPLAY_NAME:-SkyOS}"
EXPECTED_FIREBASE_PROJECT="${EXPECTED_FIREBASE_PROJECT:-skydown-a6add}"

err=0
fail() {
  echo "release_identity_check: $*" >&2
  err=1
}

pass() {
  echo "OK: $*"
}

expect_eq() {
  local label="$1"
  local actual="$2"
  local expected="$3"
  if [[ "$actual" == "$expected" ]]; then
    pass "$label = $actual"
  else
    fail "$label is '$actual', expected '$expected'"
  fi
}

read_gradle_value() {
  local key="$1"
  sed -n "s/^[[:space:]]*${key}[[:space:]]*=[[:space:]]*\"\{0,1\}\\([^\"]*\\)\"\{0,1\}.*/\\1/p" androidApp/build.gradle.kts | head -1
}

echo "== SkyOS release identity check =="
echo "Root: $ROOT_DIR"
echo

android_application_id="$(read_gradle_value applicationId)"
android_version_code="$(read_gradle_value versionCode)"
android_version_name="$(read_gradle_value versionName)"
android_label="$(
  python3 - <<'PY'
import xml.etree.ElementTree as ET
root = ET.parse("androidApp/src/main/res/values/strings.xml").getroot()
for item in root.findall("string"):
    if item.attrib.get("name") == "app_name":
        print(item.text or "")
        break
PY
)"
fastlane_package="$(sed -n 's/^package_name("\([^"]*\)").*/\1/p' fastlane/Appfile | head -1)"

expect_eq "Android applicationId" "$android_application_id" "$EXPECTED_ANDROID_ID"
expect_eq "Android app label" "$android_label" "$EXPECTED_DISPLAY_NAME"
expect_eq "Android versionName" "$android_version_name" "$EXPECTED_VERSION"
expect_eq "Fastlane Play package" "$fastlane_package" "$EXPECTED_ANDROID_ID"
if [[ "$android_version_code" =~ ^[0-9]+$ ]]; then
  pass "Android versionCode = $android_version_code"
else
  fail "Android versionCode '$android_version_code' is not numeric"
fi

python3 - "$EXPECTED_ANDROID_ID" "$EXPECTED_IOS_BUNDLE_ID" "$EXPECTED_FIREBASE_PROJECT" <<'PY'
import json
import sys

expected_android, expected_ios, expected_project = sys.argv[1:]
with open("androidApp/google-services.json", encoding="utf-8") as f:
    data = json.load(f)

project = data.get("project_info", {}).get("project_id")
packages = []
ios_bundles = []

def collect_bundle_ids(value):
    if isinstance(value, dict):
        bundle = value.get("bundle_id")
        if bundle:
            ios_bundles.append(bundle)
        for child in value.values():
            collect_bundle_ids(child)
    elif isinstance(value, list):
        for child in value:
            collect_bundle_ids(child)

for client in data.get("client", []):
    android_info = client.get("client_info", {}).get("android_client_info", {})
    package = android_info.get("package_name")
    if package:
        packages.append(package)

collect_bundle_ids(data)

errors = []
if project != expected_project:
    errors.append(f"Firebase Android project is {project!r}, expected {expected_project!r}")
if expected_android not in packages:
    errors.append(f"Firebase Android clients do not include {expected_android!r}: {packages!r}")
if expected_ios not in ios_bundles:
    errors.append(f"Firebase OAuth iOS clients do not include {expected_ios!r}: {ios_bundles!r}")

for error in errors:
    print(f"release_identity_check: {error}", file=sys.stderr)
if not errors:
    print(f"OK: Firebase Android config project = {project}")
    print(f"OK: Firebase Android config includes package = {expected_android}")
    print(f"OK: Firebase Android config includes iOS OAuth bundle = {expected_ios}")
sys.exit(1 if errors else 0)
PY

build_settings="$(xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Release -destination "generic/platform=iOS" -showBuildSettings 2>/dev/null)"
ios_bundle_id="$(awk -F= '/^[[:space:]]*PRODUCT_BUNDLE_IDENTIFIER[[:space:]]=/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2; exit}' <<<"$build_settings")"
ios_marketing_version="$(awk -F= '/^[[:space:]]*MARKETING_VERSION[[:space:]]=/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2; exit}' <<<"$build_settings")"
ios_build_number="$(awk -F= '/^[[:space:]]*CURRENT_PROJECT_VERSION[[:space:]]=/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2; exit}' <<<"$build_settings")"
ios_team="$(awk -F= '/^[[:space:]]*DEVELOPMENT_TEAM[[:space:]]=/ {gsub(/^[[:space:]]+|[[:space:]]+$/, "", $2); print $2; exit}' <<<"$build_settings")"
ios_display_name="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' SkydownApp-Info.plist)"
ios_firebase_bundle="$(/usr/libexec/PlistBuddy -c 'Print :BUNDLE_ID' "Skydown App/GoogleService-Info.plist")"
ios_firebase_project="$(/usr/libexec/PlistBuddy -c 'Print :PROJECT_ID' "Skydown App/GoogleService-Info.plist")"

expect_eq "iOS bundle ID" "$ios_bundle_id" "$EXPECTED_IOS_BUNDLE_ID"
expect_eq "iOS display name" "$ios_display_name" "$EXPECTED_DISPLAY_NAME"
expect_eq "iOS marketing version" "$ios_marketing_version" "$EXPECTED_VERSION"
expect_eq "iOS Firebase bundle" "$ios_firebase_bundle" "$EXPECTED_IOS_BUNDLE_ID"
expect_eq "iOS Firebase project" "$ios_firebase_project" "$EXPECTED_FIREBASE_PROJECT"
if [[ "$ios_build_number" =~ ^[0-9]+$ ]]; then
  pass "iOS build number = $ios_build_number"
else
  fail "iOS build number '$ios_build_number' is not numeric"
fi
if [[ -n "$ios_team" ]]; then
  pass "iOS team = $ios_team"
else
  fail "iOS DEVELOPMENT_TEAM is empty"
fi

ios_archive_path="$(find build/ios -maxdepth 1 -type d -name "SkyOS-${EXPECTED_VERSION}-${ios_build_number}-*.xcarchive" 2>/dev/null | sort | tail -1 || true)"
if [[ -n "$ios_archive_path" ]]; then
  archive_app_info="$ios_archive_path/Products/Applications/Skydown App.app/Info.plist"
  archive_display_name="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleDisplayName' "$archive_app_info")"
  archive_bundle_id="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$archive_app_info")"
  archive_version="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleShortVersionString' "$archive_app_info")"
  archive_build="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleVersion' "$archive_app_info")"
  expect_eq "iOS archive display name" "$archive_display_name" "$EXPECTED_DISPLAY_NAME"
  expect_eq "iOS archive bundle ID" "$archive_bundle_id" "$EXPECTED_IOS_BUNDLE_ID"
  expect_eq "iOS archive marketing version" "$archive_version" "$EXPECTED_VERSION"
  expect_eq "iOS archive build number" "$archive_build" "$ios_build_number"
  pass "iOS archive path = $ios_archive_path"
else
  echo "Note: no local iOS archive found for version $EXPECTED_VERSION build $ios_build_number."
fi

runbook="docs/release/store-upload-runbook.md"
grep -q -- "- Bundle ID: \`$EXPECTED_IOS_BUNDLE_ID\`" "$runbook" || fail "Runbook does not list iOS bundle $EXPECTED_IOS_BUNDLE_ID"
grep -q -- "- Build: \`$ios_build_number\`" "$runbook" || fail "Runbook does not list iOS build $ios_build_number"
grep -q -- "- Application ID: \`$EXPECTED_ANDROID_ID\`" "$runbook" || fail "Runbook does not list Android app $EXPECTED_ANDROID_ID"
grep -q -- "- versionName: \`$EXPECTED_VERSION\`" "$runbook" || fail "Runbook does not list Android versionName $EXPECTED_VERSION"
grep -q -- "- versionCode: \`$android_version_code\`" "$runbook" || fail "Runbook does not list Android versionCode $android_version_code"

if [[ $err -ne 0 ]]; then
  echo
  echo "RELEASE IDENTITY CHECK FAILED"
  exit 1
fi

echo
echo "RELEASE IDENTITY CHECK PASSED"
echo "iOS: $EXPECTED_DISPLAY_NAME $ios_marketing_version ($ios_build_number), $ios_bundle_id"
echo "Android: $EXPECTED_DISPLAY_NAME $android_version_name ($android_version_code), $android_application_id"
