# SkyOS Premium Brand System

## Positionierung

SkyOS wirkt wie ein ruhiges Atelier fuer digitale Musik-, Commerce- und Agent-Workflows: reduziert, vertrauenswuerdig, praezise und bewusst exklusiv. Die Marke soll nicht laut nach Aufmerksamkeit greifen, sondern Qualitaet durch Materialitaet, Rhythmus und klare Entscheidungen ausstrahlen.

## Brand Audit

- Die App besitzt bereits eigene Hero-, Card-, Motion- und Brand-Komponenten, aber die Premium-Sprache war noch fragmentiert: einzelne Screens nutzen starke Komponenten, andere fallen auf direkte Material-Flaechen, lokale `dp`/`Color`-Werte und heterogene Abstaende zurueck.
- Die alte Palette war solide, aber zu generisch kuehl-blaugrau. Premium braucht staerkere semantische Spannung: Obsidian fuer Tiefe, Porzellan/Platin fuer Licht, Champagner als kontrollierter Akzent.
- Typografie hatte teils negative Letter-Spacing-Werte. Das erzeugt zwar Dichte, wirkt aber bei Dynamic Type und lokalisierten Texten riskant. Die Skala bleibt editorial, aber ohne kuenstliches Zusammendruecken.
- Radius und Elevation waren an manchen Stellen weich und stark dekorativ. Die neue Richtung ist praeziser: kleinere Material-Radien, klarere Schattenrollen, weniger zufaellige Glow-Wirkung.
- Empty-, Loading- und Feedback-Zustaende existieren, muessen aber konsequenter ueber `SkydownPremiumStatePanel`, Toasts und zentrale Button-Zustaende laufen.
- Modal Sheets und sekundaere Overlays waren noch einer der sichtbarsten Brueche: mehrere Flows nutzten direkte `MaterialTheme.colorScheme.surface/background`, lokale Drag-Handle-Masse und Standard-Scrims. Das wirkt funktional, aber nicht wie ein zusammenhaengender Luxus-Chrome.
- Final fehlende Premium-Assets: echte Hero-/Surface-Fotos oder Renderings, ein konsistentes Icon-Set fuer Produktbereiche, App-Icon-Finetuning, Motion-Spezifikation als Design-Reference.

## Visuelle Leitidee

Quiet precision: dunkle Tiefe, helle mineralische Flaechen, wenige warme Akzente, klare Haptik. Screens sollen wie ein hochwertiges Instrument wirken, nicht wie eine dekorierte Standard-App.

## Farbrollen

- Primary: Sky Ink, fuer Navigation, aktive Zustaende und starke CTA.
- Secondary: Mystic Steel, fuer sekundaere Struktur und Metadaten.
- Accent: Champagne, fuer Premium-Signale, Highlights und seltene Wertigkeit.
- Background: Porcelain/Obsidian, fuer ruhige App-Raeume.
- Surface: Platinum/Satin Graphite, fuer Karten und Sheets.
- Success/Error: gedaempft, kontraststark, nie neon.

## UI-System

- Typografie: Awergy fuer ikonische Hero-Momente, Syne fuer UI und Editorial Copy.
- Spacing: 4dp-basierte Rhythmik mit benannten Tokens in `SkydownUiTokens`.
- Radius: 8/10/14/20/28dp Material-Slots plus produktbezogene Tokens fuer Cards, Hero, Sheets und Pills.
- Elevation: `elevationHairline`, `elevationRaised`, `elevationPanel`, `elevationHero`.
- Buttons: gefuellt fuer primaere Entscheidung, outline fuer sekundaere Aktionen, Loading/Disabled integriert.
- Cards: keine nackten Material-Cards in Screens; `SkydownCard`/`skydownPanelSurface` als Standard.
- Inputs: `SkydownPremiumTextField` statt roher `OutlinedTextField`.
- Sheets: `skydownPremiumSheet*` Defaults fuer Container, Content, Scrim, Shape und Drag Handle; keine lokalen Material-Defaults in wiederkehrenden Premium-Flows.
- Motion: 150-320ms, kontrolliertes Deceleration-Easing, Reduce-Motion respektieren.

## Microcopy

Kurz, bestimmt, ruhig. Keine technische Erklaerprosa in UI-Chrome. Fehler nennen die Ursache und die naechste Handlung. Empty States zeigen Moeglichkeit statt Mangel.

## Naechste Premium-Iteration

- Raw `OutlinedTextField`, `Surface`, lokale Farben und lokale Abstaende screenweise abbauen.
- Home, AI, Shop, Music und Profile mit einem gemeinsamen Premium Empty/Loading Pattern angleichen.
- Echte visuelle Assets fuer Hero-Flaechen definieren: Produkt-Signale, Musik/Commerce/Agent-Visuals, keine generischen Atmosphaeren.
- Screenshot-basierte visuelle Regression fuer Light/Dark, Compact/Expanded und deutsche Lokalisierung etablieren.

## Implementierte Iteration

- Zentrale Light/Dark-Palette auf Obsidian, Porcelain, Platinum und Champagne verfeinert.
- `SkydownUiTokens` um praezisere Radius-, Spacing- und Elevation-Rollen erweitert.
- Cards, Hero-Flaechen und Brand Buttons an die neuen Elevation-Rollen angebunden.
- `SkydownPremiumTextField` fuer Single- und Multi-Line-Felder erweitert.
- AI Session Rename, Cart Checkout, Profile Edit und der Settings-Wrapper nutzen zentrale Premium-Inputs.
- Profile Gallery und Video Library nutzen zentrale Premium State Panels fuer Empty/Loading.
- Agent, AI Composer, VideoHub Upload/Library/Edit und Nicma Preislisten-Editor verwenden keine rohen Screen-`OutlinedTextField`s mehr.
- ArtistPage, Home Quick Create und Settings werden ueber `SkydownPremiumTextField` geroutet; Material `OutlinedTextField` bleibt nur im zentralen Premium-Control als Implementierungsdetail.
- Einfache Loading-Balken und kleine Spinner in Profile, Home, AI und Agent nutzen zentrale Premium-Progress-Komponenten.
- Music Artist Rows, Music Action Buttons, Settings Segments und Admin Workspace Rows nutzen Premium Panel/Capsule Surfaces statt nackter Material-Flaechen.
- Semantische Chrome-Rollen ergaenzt: `skydownChromeSurface`, `skydownSheetSurface`, `skydownSheetScrim` und `skydownStateIconSurface`.
- Premium Sheet Defaults zentralisiert und auf Auth, Orders, Legal Documents, AI Sessions, Shop Collection, AI Composer, Agent Composer, Productivity, Founder Briefing, Video Admin/Upload, Video Equipment und Settings Admin Workspace angewendet.
- State-Icon-Masse, Sheet-Handle-Masse und Icon-Elevation als `SkydownUiTokens` statt lokaler Einzelwerte gefuehrt.
- `SkydownPremiumStatePanel` verwendet nun `SkydownPremiumIconSurface`; Loading- und Icon-Zustaende teilen sich damit dieselbe Materialitaet.
