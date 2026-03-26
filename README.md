# Skydown x 22

Skydown x 22 is a multiplatform app for the Skydown / 22 ecosystem.  
It brings together music discovery, Spotify handoff, social artist access, merch, producer services, beat listening and in-app AI tools across iOS and Android.

## Core Experience

- artist selection and music discovery
- Spotify connection with in-app previews and premium handoff
- Instagram links for artists, Skydown and 22
- `NICMA MUSIC` producer area
- `Beat Hub` for listening and admin-managed uploads
- merch, cart and order flows
- bot and agent features inside the app

## Technology Stack

- `SwiftUI`
  iOS app UI
- `Jetpack Compose`
  Android app UI
- `Kotlin Multiplatform`
  shared models and business logic
- `Firebase Auth`
  account and sign-in flows
- `Cloud Firestore`
  primary cloud database for app data
- `Firebase Storage`
  media and upload storage
- `Firebase Functions`
  backend logic and agent endpoints
- `Firebase App Distribution`
  internal tester distribution
- `Firebase AI / Gemini`
  in-app AI features
- `Spotify Web API + OAuth`
  music loading and Spotify connection
- `AVFoundation`
  iOS preview playback
- `ExoPlayer / Media3`
  Android preview playback

## Data And Backend

- main database: `Cloud Firestore`
- file storage: `Firebase Storage`
- backend functions: `Firebase Functions`
- authentication: `Firebase Auth`
- AI integration: `Firebase AI`

## Repository Structure

- `Skydown App/`
  iOS source, SwiftUI views, iOS services and view models
- `androidApp/`
  Android source, Compose screens and Android integrations
- `shared/`
  shared Kotlin Multiplatform code

## Platform Setup

- iOS bundle identifier: `com.skydown.ios`
- Android package name: `com.skydown.android`
- iOS Firebase config: `Skydown App/GoogleService-Info.plist`
- Android Firebase config: `androidApp/google-services.json`

## Development Notes

- open the repository root in Android Studio, not only `androidApp/`
- Firebase App Distribution tester lists stay local and should not be committed
