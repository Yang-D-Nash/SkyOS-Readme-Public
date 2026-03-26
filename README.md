# Skydown App

Multiplatform repository for the Skydown iOS and Android apps.

## Technologies

- `SwiftUI` for the iOS app UI
- `Kotlin Multiplatform` for shared models and business logic
- `Jetpack Compose` for the Android app UI
- `Firebase`
  - Auth
  - Firestore
  - Storage
  - Functions
  - App Distribution
  - AI / Gemini integration
- `Spotify Web API / OAuth` for music loading and Spotify handoff
- `AVFoundation` for iOS preview playback
- `ExoPlayer / Media3` for Android preview playback

## Repository Structure

- `Skydown App/`
  iOS app source, SwiftUI views, iOS services and view models
- `androidApp/`
  Android app source, Compose screens and Android-specific integrations
- `shared/`
  shared Kotlin Multiplatform models and domain logic

## Platform Setup

- iOS bundle identifier: `com.skydown.ios`
- Android package name: `com.skydown.android`
- iOS Firebase config: `Skydown App/GoogleService-Info.plist`
- Android Firebase config: `androidApp/google-services.json`

## Notes

- Open the repo root in Android Studio, not only `androidApp/`
- Firebase App Distribution tester lists are kept local and should not be committed
