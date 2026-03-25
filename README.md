# Skydown App

Skydown is a multiplatform project with:

- an existing SwiftUI iOS app in `Skydown App/`
- a Kotlin Multiplatform `shared` module
- an Android app in `androidApp/`

Current setup:

- iOS bundle identifier: `com.skydown.ios`
- Android package name: `com.skydown.android`
- Firebase is configured for both platforms

Main modules:

- `Skydown App/`: iOS SwiftUI app and platform services
- `shared/`: shared models, repositories, services, and use cases
- `androidApp/`: Android Compose app

Notes:

- iOS uses `GoogleService-Info.plist` in `Skydown App/`
- Android uses `google-services.json` in `androidApp/`
- Android Studio should open the repository root, not only `androidApp/`
