# Skydown x 22

Skydown x 22 ist die App fuer das Skydown- und 22-Universum.  
Sie verbindet Artists, Releases, Beats, Producer-Services, Merch und Community in einer gemeinsamen mobilen Experience fuer iOS und Android.

## Was Die App Zeigt

Skydown x 22 ist nicht nur ein Shop oder nur ein Music-Player. Die App soll die Welt von `Skydown Entertainment` und `22` als eigene Plattform fuehlbar machen:

- Artists entdecken und zwischen ihren Releases wechseln
- Tracks in der App anhoeren und komplette Wiedergabe ueber Spotify weiterfuehren
- Artists, `22 Music` und `Skydown Entertainment` direkt ueber Instagram erreichen
- `NICMA MUSIC` als Producer-Bereich mit Preisen, Anfrageweg und eigener Identitaet erleben
- Beats im separaten `Beat Hub` hoeren, waehrend Uploads und Review bewusst im Admin-/Producer-Flow bleiben
- Merch, Warenkorb und Bestellungen direkt in derselben App nutzen
- Bot und Agent als kreative Skydown x 22 Tools innerhalb der App verwenden

## Kernbereiche

- `Music`
  Artist-Auswahl, Spotify-Anbindung, In-App-Previews und Social-Verlinkungen
- `NICMA MUSIC`
  Producer-Seite fuer Mixing, Mastering, Recording und direkte Kontaktaufnahme
- `Beat Hub`
  Eigener Bereich fuer Beat-Library, Preview-Listening und Admin-Freigaben
- `Shop`
  Merchandise, Cart und Order-Flow
- `Bot + Agent`
  KI-Flaechen im gebrandeten Skydown x 22 Stil

## Plattformen

- iOS App in `Skydown App/`
- Android App in `androidApp/`
- gemeinsames Kotlin-Multiplatform-Modul in `shared/`

## Technischer Stand

- iOS Bundle Identifier: `com.skydown.ios`
- Android Package Name: `com.skydown.android`
- Firebase ist auf beiden Plattformen eingebunden
- iOS nutzt `GoogleService-Info.plist` in `Skydown App/`
- Android nutzt `google-services.json` in `androidApp/`

## Repo-Struktur

- `Skydown App/`: SwiftUI App, iOS Services und ViewModels
- `shared/`: geteilte Models und Cross-Platform-Logik
- `androidApp/`: Compose UI, Android-spezifische Screens und Integrationen

## Hinweise Fuer Die Entwicklung

- Android Studio sollte das Repo-Root oeffnen, nicht nur `androidApp/`
- Firebase App Distribution Testerlisten bleiben lokal und sollen nicht committed werden
