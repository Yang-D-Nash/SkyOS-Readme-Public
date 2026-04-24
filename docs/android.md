# SkyOS Android Guide

SkyOS on Android is a native Jetpack Compose app backed by Firebase, hosted checkout support,
membership logic, owner/admin controls, and adaptive layout work for phones and foldables.

## 1. Project Layout

Important paths:

- `androidApp/src/main/java/com/skydown/android/ui/screen/` - Compose feature screens
- `androidApp/src/main/java/com/skydown/android/ui/viewmodel/` - screen state logic
- `androidApp/src/main/java/com/skydown/android/data/` - repositories, billing, legal, commerce, AI
- `androidApp/src/main/res/` - resources, strings, app assets
- `androidApp/src/androidTest/` - instrumentation tests
- `androidApp/build.gradle.kts` - Android app build and signing config

## 2. Requirements

- Android Studio and Android SDK
- JDK 17
- `androidApp/google-services.json` for the intended Firebase project
- release signing material through `keystore.properties` or `SKYDOWN_UPLOAD_*` environment variables

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

## 4. Android Studio Workflow

1. Open the project root in Android Studio.
2. Confirm SDK and Gradle sync.
3. Confirm the correct `google-services.json`.
4. Select emulator or physical device.
5. Run the `androidApp` configuration.

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
- The Android Firebase package in this repo is `com.skydown.android`.

If owner-only Shopify calls such as `syncShopifyMerch` or `listShopifyCollections` fail with app verification errors on a test device:

1. Start the app once on the device.
2. Read logcat and copy the debug secret printed by `DebugAppCheckProvider`.
3. Register that secret in Firebase Console under App Check for the Android app `com.skydown.android`.
4. Retry the Shopify sync or collection load after the token is allow-listed.

Helpful commands:

```bash
ADB=~/Library/Android/sdk/platform-tools/adb
$ADB logcat -d | rg "DebugAppCheckProvider|App Check|App attestation failed|placeholder token"
./gradlew -q :androidApp:signingReport
```

If a debug build does not print a debug secret, verify the debug App Check dependency and the
active `google-services.json`. If a release build fails App Check, verify the release signing
SHA-1 and SHA-256 in Firebase before changing any server-side enforcement.

## 8. Instrumentation Expectations

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

## 9. Release Standard

Android is only public-launch ready when:

- debug and release builds are green
- instrumentation smoke passes on a real device
- Play billing and entitlement sync are verified
- App Check hardening is confirmed on device
- fold and standard-phone layouts both hold up where supported

See [release-checklist.md](release-checklist.md) for the full gate.
