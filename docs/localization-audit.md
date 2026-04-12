# Localization Audit

Generated: 2026-04-12 19:40:06 CEST

## iOS (Localizable.strings)

| Locale | Keys |
| --- | ---: |
| de.lproj | 38 |
| en.lproj | 38 |
| es.lproj | 38 |
| fr.lproj | 38 |
| it.lproj | 38 |
| ja.lproj | 38 |
| nl.lproj | 38 |
| pl.lproj | 38 |
| pt.lproj | 38 |
| tr.lproj | 38 |

Hardcoded UI literal matches in Swift files: **704**

Top Swift files by hardcoded-literal matches:
```text
 292 /Users/nash/Documents/Skydown-App/Skydown App/Views/Settings/SettingsView.swift
  64 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/VideoHubView.swift
  42 /Users/nash/Documents/Skydown-App/Skydown App/Views/Cart/CartView.swift
  40 /Users/nash/Documents/Skydown-App/Skydown App/Views/Shop/ShopView.swift
  34 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/ArtistPageView.swift
  28 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/SubView/MusicSharedComponents.swift
  28 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/MusicView.swift
  24 /Users/nash/Documents/Skydown-App/Skydown App/Views/LaunchScreenView.swift
  22 /Users/nash/Documents/Skydown-App/Skydown App/Views/MainTabView.swift
  18 /Users/nash/Documents/Skydown-App/Skydown App/Views/Cart/SubView/ContactFormView.swift
  16 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/BeatHubView.swift
  15 /Users/nash/Documents/Skydown-App/Skydown App/Views/Orders/OrderView.swift
  14 /Users/nash/Documents/Skydown-App/Skydown App/Views/AI/AIView.swift
  13 /Users/nash/Documents/Skydown-App/Skydown App/Views/Profile/ProfileView.swift
  12 /Users/nash/Documents/Skydown-App/Skydown App/Views/AI/AgentView.swift
   9 /Users/nash/Documents/Skydown-App/Skydown App/Views/Music/NicmaProducerView.swift
   8 /Users/nash/Documents/Skydown-App/Skydown App/Views/Shop/SubView/MerchandiseCollabNavigator.swift
   7 /Users/nash/Documents/Skydown-App/Skydown App/Utilities/AI/AIShareSheet.swift
   4 /Users/nash/Documents/Skydown-App/Skydown App/ViewModels/Music/VideoHubPublicConfig.swift
   3 /Users/nash/Documents/Skydown-App/Skydown App/Views/Cart/SubView/SubmitSectionView.swift
```

## Android (strings.xml)

| Resource dir | Keys |
| --- | ---: |
| values-de | 50 |
| values-es | 50 |
| values-fr | 50 |
| values-it | 50 |
| values-ja | 50 |
| values-nl | 50 |
| values-pl | 50 |
| values-pt | 50 |
| values-tr | 50 |
| values | 56 |

Hardcoded UI literal matches in Kotlin UI files: **506**

Top Kotlin files by hardcoded-literal matches:
```text
 235 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/SettingsScreen.kt
  59 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/VideoHubScreen.kt
  35 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/CartScreen.kt
  20 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/OrderScreen.kt
  20 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/MusicScreen.kt
  17 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/ArtistPageScreen.kt
  16 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/AiHubScreen.kt
  15 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/BeatHubScreen.kt
  15 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/AiScreen.kt
  13 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/ShopScreen.kt
  12 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/AgentScreen.kt
   9 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/ProfileScreen.kt
   9 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/component/TrackRow.kt
   8 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/NicmaProducerScreen.kt
   8 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/screen/HomeScreen.kt
   6 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/SkydownApp.kt
   3 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/component/YouTubePlayerDialog.kt
   3 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/component/MerchandiseCard.kt
   2 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/ui/component/EditableImageFieldCard.kt
   1 /Users/nash/Documents/Skydown-App/androidApp/src/main/java/com/skydown/android/data/AiUsageAuthorizationClient.kt
```

## Summary

- Locale folders are present for 10 languages on iOS and Android.
- Full UI localization is **not complete** while hardcoded literals remain in source.
- Priority should be top files listed above, then secondary modules.
