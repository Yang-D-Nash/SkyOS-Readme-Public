# Skydown Growth Events

This document defines the minimal cross-platform growth event contract for the first five minutes.

## Event Contract

### `app_open`
- **Bedeutung:** First app start signal for the activation funnel.
- **Trigger:** When the app UI starts.
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/SkydownApp.swift`, `androidApp/src/main/java/com/skydown/android/MainActivity.kt`
- **Warum wichtig:** Funnel entry baseline for install-to-signup conversion.

### `signup_start`
- **Bedeutung:** User opens the registration flow.
- **Trigger:** Registration sheet/screen becomes visible.
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/Views/LogIn/SubView/RegistrationSheet.swift`, `androidApp/src/main/java/com/skydown/android/ui/screen/RegistrationScreen.kt`
- **Warum wichtig:** Measures first account-intent conversion.

### `signup_complete`
- **Bedeutung:** Account creation succeeds (email/password or Google).
- **Trigger:** Successful registration callback.
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/ViewModels/LogIn/RegistrationViewModel.swift`, `androidApp/src/main/java/com/skydown/android/ui/screen/RegistrationScreen.kt`
- **Warum wichtig:** Core activation completion KPI.

### `onboarding_started`
- **Bedeutung:** First-run intro/onboarding starts.
- **Trigger:** Intro screen is shown.
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/Views/LaunchScreenView.swift`, `androidApp/src/main/java/com/skydown/android/ui/SkydownApp.kt`
- **Warum wichtig:** Shows how many users enter the onboarding phase.

### `onboarding_completed`
- **Bedeutung:** First-run onboarding finishes or is skipped.
- **Trigger:** Transition from intro to landing/state after intro.
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/Views/LaunchScreenView.swift`, `androidApp/src/main/java/com/skydown/android/ui/SkydownApp.kt`
- **Warum wichtig:** Measures onboarding completion rate and drop-off before product entry.

### `first_value_moment`
- **Bedeutung:** First real product-value action in the launch journey.
- **Trigger:** User selects first launch entry CTA (Music, Video, Shop).
- **Plattform:** iOS, Android
- **Datei/Ort:** `Skydown App/Views/LaunchScreenView.swift`, `androidApp/src/main/java/com/skydown/android/ui/SkydownApp.kt`
- **Warum wichtig:** Measures time-to-first-value and first meaningful activation.

## Backend Acceptance

`functions/index.js` accepts these events in `AI_MEMBERSHIP_EVENT_TYPES`, so mirrored tracking calls pass validation in `recordAiMembershipEvent`.
