# Localization Audit

Generated: 2026-04-22 22:06:36 CEST

## iOS (Localizable.strings)

| Locale | Keys |
| --- | ---: |
| de.lproj | 220 |
| en.lproj | 220 |
| es.lproj | 155 |
| fr.lproj | 155 |
| it.lproj | 38 |
| ja.lproj | 38 |
| nl.lproj | 38 |
| pl.lproj | 38 |
| pt.lproj | 155 |
| tr.lproj | 38 |

Hardcoded UI literal matches in Swift files: **779**

Top Swift files by hardcoded-literal matches:

```text
 267 Skydown App/Views/Settings/SettingsView.swift
  77 Skydown App/Views/Music/VideoHubView.swift
  52 Skydown App/Views/Cart/CartView.swift
  47 Skydown App/Views/Music/MusicView.swift
  42 Skydown App/Views/Music/ArtistPageView.swift
  35 Skydown App/Views/Cart/SubView/MerchandiseDetailView.swift
  29 Skydown App/Views/LaunchScreenView.swift
  25 Skydown App/Views/Orders/OrderView.swift
  25 Skydown App/Views/MainTabView.swift
  21 Skydown App/Views/Profile/ProfileView.swift
  19 Skydown App/Views/Music/SubView/MusicSharedComponents.swift
  16 Skydown App/Views/Music/BeatHubView.swift
  15 Skydown App/Views/AI/AgentView.swift
  14 Skydown App/Views/Shop/SubView/MerchandiseCollabNavigator.swift
  14 Skydown App/Views/Settings/SubView/SettingsMembershipCommandCenterView.swift
  11 Skydown App/Views/Shop/ShopView.swift
  11 Skydown App/Views/AI/AIView.swift
   9 Skydown App/Views/Music/NicmaProducerView.swift
   9 Skydown App/Views/Home/HomeSignals.swift
   7 Skydown App/Utilities/AI/AIShareSheet.swift
```

## Android (strings.xml)

| Resource dir | Keys |
| --- | ---: |
| values-de | 174 |
| values-es | 151 |
| values-fr | 151 |
| values-it | 56 |
| values-ja | 56 |
| values-nl | 56 |
| values-pl | 56 |
| values-pt | 151 |
| values-tr | 56 |
| values | 180 |

Hardcoded UI literal matches in Kotlin UI files: **566**

Top Kotlin files by hardcoded-literal matches:

```text
 244 androidApp/src/main/java/com.nash.skyos/ui/screen/SettingsScreen.kt
  62 androidApp/src/main/java/com.nash.skyos/ui/screen/VideoHubScreen.kt
  40 androidApp/src/main/java/com.nash.skyos/ui/screen/CartScreen.kt
  31 androidApp/src/main/java/com.nash.skyos/ui/screen/MusicScreen.kt
  27 androidApp/src/main/java/com.nash.skyos/ui/screen/AiScreen.kt
  22 androidApp/src/main/java/com.nash.skyos/ui/screen/OrderScreen.kt
  21 androidApp/src/main/java/com.nash.skyos/ui/screen/ShopScreen.kt
  16 androidApp/src/main/java/com.nash.skyos/ui/screen/HomeScreen.kt
  16 androidApp/src/main/java/com.nash.skyos/ui/SkydownApp.kt
  15 androidApp/src/main/java/com.nash.skyos/ui/screen/BeatHubScreen.kt
  14 androidApp/src/main/java/com.nash.skyos/ui/screen/AgentScreen.kt
  10 androidApp/src/main/java/com.nash.skyos/ui/screen/ProfileScreen.kt
  10 androidApp/src/main/java/com.nash.skyos/ui/screen/AiHubScreen.kt
   9 androidApp/src/main/java/com.nash.skyos/ui/component/TrackRow.kt
   8 androidApp/src/main/java/com.nash.skyos/ui/screen/NicmaProducerScreen.kt
   6 androidApp/src/main/java/com.nash.skyos/ui/screen/ArtistPageScreen.kt
   4 androidApp/src/main/java/com.nash.skyos/ui/component/OriginalVideoViewerDialog.kt
   3 androidApp/src/main/java/com.nash.skyos/ui/component/YouTubePlayerDialog.kt
   2 androidApp/src/main/java/com.nash.skyos/ui/component/MerchandiseCard.kt
   2 androidApp/src/main/java/com.nash.skyos/ui/component/EditableImageFieldCard.kt
```

## Summary

- Locale folders are present for 10 languages on iOS and Android.
- Full UI localization is **not complete** while hardcoded literals remain in source.
- Priority should be Settings, AI/Agent, Cart/Orders, Shop, Home, Profile, Music, and Video release paths.
