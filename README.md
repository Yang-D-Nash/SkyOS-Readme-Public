# Skydown x 22

Skydown x 22 is a multiplatform music and creator app for the Skydown / 22 ecosystem.
It combines artist discovery, Spotify-powered listening, social artist access, producer services, beat listening, merch and in-app AI across iOS and Android.

## What The App Includes

- artist selection and music discovery
- Spotify connection with in-app previews and premium handoff
- Instagram links for artists, Skydown and 22
- `NICMA MUSIC` producer area
- `Beat Hub` for listening and admin-managed uploads
- merch, cart and order flows
- bot and agent features inside the app

## Technology Stack

- `SwiftUI` for the iOS app UI
- `Jetpack Compose` for the Android app UI
- `Kotlin Multiplatform` for shared models and business logic
- `Spotify Web API + OAuth` for music loading and Spotify connection
- `AVFoundation` for iOS preview playback
- `ExoPlayer / Media3` for Android preview playback

## Backend And Data Technologies

- `Firebase Auth` for account and sign-in flows
- `Cloud Firestore` as the main cloud database
- `Firebase Storage` for media and uploads
- `Firebase Functions` for backend logic and agent endpoints
- `Firebase App Distribution` for internal tester releases
- `Firebase AI / Gemini` for in-app AI features

## Repository Structure

- `Skydown App/` for the iOS source, SwiftUI views, services and view models
- `androidApp/` for the Android source, Compose screens and integrations
- `shared/` for shared Kotlin Multiplatform code

## Platform Setup

- iOS bundle identifier: `com.skydown.ios`
- Android package name: `com.skydown.android`
- iOS Firebase config: `Skydown App/GoogleService-Info.plist`
- Android Firebase config: `androidApp/google-services.json`

## Development Notes

- open the repository root in Android Studio, not only `androidApp/`
- Firebase App Distribution tester lists stay local and should not be committed
