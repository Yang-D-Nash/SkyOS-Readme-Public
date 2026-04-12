# Localization Roadmap (10 Languages)

Status date: 2026-04-12

## Scope
- Target locales: `de`, `en`, `es`, `fr`, `it`, `pt`, `nl`, `pl`, `tr`, `ja`
- Platforms: iOS + Android
- Goal: no hardcoded UI literals in app screens/components; all user-facing text localized.

## Baseline (measured)
- iOS `Localizable.strings`: 14 keys per locale.
- Android `strings.xml`: 17 base keys; 11 translated keys per non-base locale.
- Hardcoded literals detected:
  - iOS Swift files: 721
  - Android Kotlin UI files: 532

See `docs/localization-audit.md` for detailed file-level breakdown.

## Rollout Plan
1. Core shell + Settings + Auth
- iOS: `SettingsView`, `LoginView`, `RegistrationSheet`, `MainTabView`
- Android: `SettingsScreen`, `LoginScreen`, `RegistrationScreen`, `SkydownApp`

2. Commerce + Profile
- iOS: `CartView`, `OrderView`, `ProfileView`, `ShopView`
- Android: `CartScreen`, `OrderScreen`, `ProfileScreen`, `ShopScreen`

3. Media + AI
- iOS: `MusicView`, `VideoHubView`, `BeatHubView`, `AIView`, `AgentView`
- Android: `MusicScreen`, `VideoHubScreen`, `BeatHubScreen`, `AiScreen`, `AgentScreen`, `AiHubScreen`

4. Final consistency pass
- unify terminology (`22`, `Skydown`, `22xSky`)
- verify placeholders, toasts, errors, mail templates
- run locale smoke tests on both platforms

## Quality Gate (release)
- 0 hardcoded user-facing literals in app UI packages
- key parity across all 10 locales
- no missing-resource crashes
- manual smoke test in DE + EN + one non-Latin locale (JA)
