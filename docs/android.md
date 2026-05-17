# SkyOS Android Guide

SkyOS on Android is a native Jetpack Compose app backed by Firebase, hosted checkout support,
membership logic, owner/admin controls, and adaptive layout work for phones and foldables.

## 1. Project Layout

Important paths:

- `androidApp/src/main/java/com.nash.skyos/ui/screen/` - Compose feature screens
- `androidApp/src/main/java/com.nash.skyos/ui/viewmodel/` - screen state logic
- `androidApp/src/main/java/com.nash.skyos/data/` - repositories, billing, legal, commerce, AI
- `androidApp/src/main/res/` - resources, strings, app assets
- `androidApp/src/androidTest/` - instrumentation tests
- `androidApp/build.gradle.kts` - Android app build and signing config

## 2. Requirements

- Android Studio and Android SDK
- JDK 17
- `androidApp/google-services.json` for the intended Firebase project
- release signing material through `keystore.properties` or `SKYOS_UPLOAD_*` environment variables
  (`SKYDOWN_UPLOAD_*` remains accepted for older local setups)
- optional but recommended: `ANDROID_HOME` with build-tools so `verify_android_release_artifacts.sh`
  can re-check the binary APK via `aapt2`

## 3. Build Commands

Debug build:

```bash
./gradlew :androidApp:assembleDebug
```

Debug instrumentation APK:

```bash
./gradlew :androidApp:assembleDebugAndroidTest
```

Release build validation:

```bash
./gradlew :androidApp:assembleRelease
```

The release build intentionally fails if signing is not configured, unless
`-PallowDebugReleaseSigning=true` is used for non-store smoke only.

Clean public release build:

```bash
./scripts/android_release_clean_build.sh
```

Use this script before distributing Android artifacts. It deletes old Android build outputs,
rebuilds the release APK and AAB from the current checkout, and verifies that the generated release
metadata matches the `versionCode` and `versionName` in `androidApp/build.gradle.kts`.

## 4. Android Studio Workflow

1. Open the project root in Android Studio.
2. Confirm SDK and Gradle sync.
3. Confirm the correct `google-services.json`.
4. Select emulator or physical device.
5. Run the `androidApp` configuration for development only.

Android Studio's green Run button normally installs the `debug` variant. The public app should be
validated from `release` artifacts built by `./scripts/android_release_clean_build.sh`, because
debug and release differ in signing, minification, and Firebase App Check behavior.

### Statische Analyse (Detekt)

Das Projekt nutzt **Detekt 2** (`dev.detekt` Gradle-Plugin) für Kotlin in `:androidApp` und
`:shared` (nur `commonMain`). Standardbefehl:

```bash
./gradlew detektAll
```

- Konfiguration: `config/detekt.yml` (u. a. leise gemacht: Länge, Magic Numbers, viele
  Komplexitätsregeln; Fokus auf u. a. unbenutzte Parameter).
- `ignoreFailures` ist derzeit `false` — `detektAll` schlägt fehl, sobald die Analyse Befunde meldet.
  HTML-Reports: `androidApp/build/reports/detekt/`, `shared/build/reports/detekt/`.
- Groessere Altlasten: optional Baseline mit `./gradlew :androidApp:detektBaseline` (und analog
  `:shared`) statt harter Fehler, oder `config/detekt.yml` enger stellen.

### Play uploads (AAB and APK)

Store-facing **versionCode** and **versionName** are defined only in `androidApp/build.gradle.kts`.
Before any Google Play upload (manual drag-and-drop or Fastlane), use this sequence so you never
ship the wrong file or an outdated build number:

1. Produce a clean release: `./scripts/android_release_clean_build.sh` (fails if metadata or
   signing do not match Gradle).
2. Immediately before upload: `./scripts/verify_android_release_artifacts.sh` (re-checks metadata,
   optional `aapt2` on the APK when `ANDROID_HOME` is set, prints paths and SHA-256).

Fastlane lanes `validate_android_internal` and `upload_android_internal` run the same verify step
first. Do not upload an AAB from Downloads, another checkout, or an old `androidApp/build/`
directory from a previous run.

## 5. Fold and Large-Screen Testing

SkyOS supports large screens, but foldables need intentional QA. Validate:

- cover-state and opened-state navigation
- wide content density and scroll behavior
- split attention between floating navigation and content
- CTA reachability in portrait and opened layouts
- media, AI, and Settings surfaces on wide canvases

Recommended commands:

```bash
adb devices -l
adb shell wm size
adb shell dumpsys device_state
```

## 6. Android-Specific Architecture Notes

The Android app includes:

- Compose screens for Home, AI, Agent, Music, Video, Shop, Orders, Profile, and Settings
- repositories for legal content, Shopify, payment methods, membership ops, AI runtime, and upload flows
- Firebase App Check integration
- Firestore offline caching configuration
- hosted checkout handling and store subscription sync paths

## 7. Firebase App Check on Debug Devices

- Debug builds install the Firebase App Check debug provider.
- Release builds stay protected through Play Integrity.
- The Android Firebase package in this repo is `com.nash.skyos`.

## 7a. Reminder Push / Firebase Messaging

Reminder push uses Firebase Cloud Messaging end-to-end:

- dependency: `com.google.firebase:firebase-messaging`
- token acquisition: `PushTokenSyncClient` fetches and caches the FCM registration token
- token refresh: `SkyOsFirebaseMessagingService.onNewToken` updates the local token cache
- backend sync: signed-in sessions call `upsertPushToken` with `platform=android`
- foreground delivery: reminder data messages are shown through `ProductivityReminderNotificationCenter`

The server path is `users/{uid}/reminders/{id}` -> `processDueReminders` -> FCM. Android still
uses local alarms for reminders created directly on device, but workflow-created reminders depend
on the synced FCM token.

If owner-only Shopify calls such as `syncShopifyMerch` or `listShopifyCollections` fail with app verification errors on a test device:

1. Start the app once on the device.
2. Read logcat and copy the debug secret printed by `DebugAppCheckProvider`.
3. Register that secret in Firebase Console under App Check for the Android app `com.nash.skyos`.
4. Retry the Shopify sync or collection load after the token is allow-listed.

**Ohne Browser (lokal, einmal):** Wenn `gcloud` installiert und mit deinem Google-Konto angemeldet ist (`gcloud auth login`), kannst du denselben Debug-Secret per API eintragen:

```bash
cd /path/to/SkyOs-App
./scripts/register_android_appcheck_debug_token.py 'PASTE-UUID-FROM-LOGCAT-HERE'
```

Falls `gcloud` nicht im PATH des Skripts liegt (z. B. IDE-Terminal), Token explizit übergeben:

```bash
cd /path/to/SkyOs-App
export GCLOUD_ACCESS_TOKEN="$(gcloud auth print-access-token)"
python3 scripts/register_android_appcheck_debug_token.py 'PASTE-UUID-FROM-LOGCAT-HERE'
```

Das Skript nutzt die Android-App-ID aus `androidApp/google-services.json` (`com.nash.skyos`). Bei anderem Projekt oder zweiter Android-App die Umgebungsvariablen `FIREBASE_PROJECT_NUMBER` und `FIREBASE_ANDROID_APP_ID` setzen (siehe Skriptkopf).

Helpful commands:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB logcat -d | rg "DebugAppCheckProvider|App Check|App attestation failed|placeholder token"
./gradlew -q :androidApp:signingReport
```

If a debug build does not print a debug secret, verify the debug App Check dependency and the
active `google-services.json`. If a release build fails App Check, verify the release signing
SHA-1 and SHA-256 in Firebase before changing any server-side enforcement.

## 8. Launcher icon (adaptive)

Adaptive foreground lives in `drawable/ic_launcher_foreground.xml` (bitmap `ic_launcher_foreground_src`,
`android:gravity="fill"` so the PNG scales into the layer instead of being center-clipped).

Inset margin: `@dimen/ic_launcher_foreground_inset` in `values/dimens.xml`. Keep this **small**
(a few dp): the Android-specific PNG already carries launcher safe-area padding, so a large XML
inset would make the round mark look tiny on the home screen.

**Note:** iOS uses the full 1024×1024 SkyOS master as Apple-compliant opaque
AppIcon PNGs without alpha. Android uses
`docs/assets/skyos-app-icon-1024-android-padded.png`, a 78% scaled mirror of that master, as
`ic_launcher_foreground_src.png` so OEM adaptive masks do not clip the circle or bottom badge.

## 9. Instrumentation Expectations

Before release, validate on a real Android device:

- app launch and restore
- navigation between product hubs
- AI success and failure states
- merch and checkout entry
- legal center
- membership restore path
- owner/admin visibility rules
- fold-state layout stability where foldables are in scope

The repo already contains Android instrumentation coverage for AI, merch, music, and video flows.
Treat failures in these flows as release-significant.

## 10. Release Standard

Android is only public-launch ready when:

- debug and release builds are green
- instrumentation smoke passes on a real device
- Play billing and entitlement sync are verified
- App Check hardening is confirmed on device
- fold and standard-phone layouts both hold up where supported

See [release-checklist.md](release-checklist.md) for the full gate.
