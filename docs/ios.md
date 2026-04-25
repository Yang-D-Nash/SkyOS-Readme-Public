# SkyOS iOS Guide

SkyOS on iOS is a native SwiftUI app with Firebase-backed auth, data, legal content, AI flows,
commerce, and owner controls. This guide is the practical entry point for building, signing,
running, and validating the iOS client.

## 1. Project Layout

Important paths:

- `Skydown App/Views/` - SwiftUI screens and feature surfaces
- `Skydown App/ViewModels/` - screen orchestration and state
- `Skydown App/Services/` - auth, AI, commerce, payments, legal, and app services
- `Skydown App/Utilities/` - localization, privacy/legal helpers, alerts, design utilities
- `Skydown AppUITests/` - device and simulator UI tests
- `Skydown App.xcodeproj` - Xcode project

## 2. Requirements

- Xcode with current iOS SDK support
- Apple signing setup for real-device work
- `Skydown App/GoogleService-Info.plist` for the intended Firebase environment
- access to the correct Apple team and provisioning setup for archive/signing work

## 3. Build From Command Line

Debug simulator build:

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" -configuration Debug -destination "generic/platform=iOS Simulator" build
```

For real-device smoke and archive validation, use Xcode or a device-specific `xcodebuild`
destination with a valid signing context.

## 4. Open In Xcode

Recommended workflow:

1. Open `Skydown App.xcodeproj`.
2. Confirm signing team and bundle identifier behavior.
3. Confirm the correct Firebase plist is present at `Skydown App/GoogleService-Info.plist`.
4. Choose simulator or physical device.
5. Run the app and validate the targeted flow.

## 5. Important iOS-Specific Services

The iOS app contains first-class native services for:

- AI prompt and runtime settings
- FAQ owner review intelligence
- Manus BYOS storage in Keychain
- legal content resolution and display
- hosted checkout redirect handling
- native AI subscription restore and sync
- Shopify config and catalog fallback

This is not a thin shell over a web app. The iOS client carries real platform logic.

## 6. Signing Basics

For real-device and release work:

- confirm bundle ID and team mapping
- verify entitlements are correct
- use a valid provisioning profile
- validate archive/export in Xcode before treating a release as ready

Do not treat a simulator-only green build as store-readiness proof.

## 7. Real-Device Smoke Expectations

Before release, validate at least:

- cold launch and session restore
- login, logout, and account creation where enabled
- Home, Music, Video, Shop, AI, and Settings entry points
- legal center display
- membership upgrade and restore visibility
- owner/admin gating with a non-owner test account
- hosted checkout redirect handling if payment rails are live

UI tests exist in `Skydown AppUITests/`, including screenshot and smoke coverage. Expand them when
new critical flows are added.

## 8. iOS Release Notes For This Repo

The current repo already contains:

- SwiftUI app structure with strong feature coverage
- UI tests for launch, music access, settings access, and screenshot capture
- native legal content rendering
- native billing and restore-related services for AI memberships

Still required before public release:

- final real-device release candidate pass
- final copy and localization cleanup
- final legal approval for production operator details and market language

See [release-checklist.md](release-checklist.md) for the launch gate.
