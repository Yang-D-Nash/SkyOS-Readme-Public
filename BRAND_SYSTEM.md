# SkyOS Premium Brand System

## Positionierung

SkyOS wirkt wie ein ruhiges Atelier fuer digitale Musik-, Commerce- und Agent-Workflows: reduziert, vertrauenswuerdig, praezise und bewusst exklusiv. Die Marke soll nicht laut nach Aufmerksamkeit greifen, sondern Qualitaet durch Materialitaet, Rhythmus und klare Entscheidungen ausstrahlen.

## Brand Audit

- Die App besitzt bereits eigene Hero-, Card-, Motion- und Brand-Komponenten, aber die Premium-Sprache war noch fragmentiert: einzelne Screens nutzen starke Komponenten, andere fallen auf direkte Material-Flaechen, lokale `dp`/`Color`-Werte und heterogene Abstaende zurueck.
- Der aktuelle Android-Compose-Stand ist deutlich weiter premiumisiert als eine Standard-Material-App: `BrandHeroCard`, `SkydownCard`, Premium Sheets, Premium Icon Actions, Premium Inputs, zentrale Progress-Komponenten und eigene Topbar-/Toast-Chrome sind vorhanden.
- Die wichtigste verbleibende Qualitaetsluecke ist Rest-Streuung: Screen-Code enthaelt weiterhin viele lokale `dp`-Werte, lokale Alpha-Entscheidungen und einzelne direkte Material-Surfaces. Das ist nicht automatisch falsch, aber es macht die Wahrnehmung weniger kontrolliert und erschwert iOS/Android-Paritaet.
- Die alte Palette war solide, aber zu generisch kuehl-blaugrau. Premium braucht staerkere semantische Spannung: Obsidian fuer Tiefe, Porzellan/Platin fuer Licht, Champagner als kontrollierter Akzent.
- Typografie hatte teils negative Letter-Spacing-Werte. Das erzeugt zwar Dichte, wirkt aber bei Dynamic Type und lokalisierten Texten riskant. Die Skala bleibt editorial, aber ohne kuenstliches Zusammendruecken.
- Radius und Elevation waren an manchen Stellen weich und stark dekorativ. Die neue Richtung ist praeziser: kleinere Material-Radien, klarere Schattenrollen, weniger zufaellige Glow-Wirkung.
- Empty-, Loading- und Feedback-Zustaende existieren, muessen aber konsequenter ueber `SkydownPremiumStatePanel`, Toasts und zentrale Button-Zustaende laufen.
- Modal Sheets und sekundaere Overlays waren noch einer der sichtbarsten Brueche: mehrere Flows nutzten direkte `MaterialTheme.colorScheme.surface/background`, lokale Drag-Handle-Masse und Standard-Scrims. Das wirkt funktional, aber nicht wie ein zusammenhaengender Luxus-Chrome.
- Settings/Admin bleibt der groesste sichtbare Komplexitaetsbereich: viele Formulare sind funktional notwendig, brauchen aber konsequente Premium-Input-Rhythmik, verdichtete Gruppierung und bessere progressive Disclosure, damit der Bereich nicht nach Backoffice-Default wirkt.
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
- Elevation: `elevationHairline`, `elevationRaised`, `elevationStateIcon`, `elevationPanel`, `elevationHero`.
- Buttons: gefuellt fuer primaere Entscheidung, outline fuer sekundaere Aktionen, Loading/Disabled integriert.
- Cards: keine nackten Material-Cards in Screens; `SkydownCard`/`skydownPanelSurface` als Standard.
- Inputs: `SkydownPremiumTextField` statt roher `OutlinedTextField`; Mindesthoehe, Progress-Stroke und Switch-Masse werden ueber `SkydownUiTokens` gefuehrt.
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
- Toast-, Auth- und Media-Upload-Chrome wurden weiter tokenisiert: Icon-Surfaces, Toast-Rail, Preview-Hoehen und Overlay-Paddings liegen zentral in `SkydownUiTokens`.
- Google Auth Loading sowie Editable Image/Video Uploads nutzen zentrale Premium-Progress-Komponenten statt roher Material-Indikatoren.
- `BrandActionButton`, AI/Agent Composer, VideoHub und Settings Admin-Zustaende verwenden zentrale Premium-Progress-Komponenten; `SkydownPremiumLinearProgress` unterstuetzt nun determinierte Fortschritte.
- AI- und Agent-Composer-FABs wurden auf `BrandActionButton` umgestellt, damit CTA-Motion, Elevation und Loading-States nicht mehr vom Material-FAB abweichen.
- `SkydownPremiumIconAction` fuehrt kleine Icon-Aktionen zentral; Profile-Hero und VideoHub-Upload verwenden damit keine Material-FABs mehr.
- AI Conversation Session-Aktionen und Portal Chips wurden weiter tokenisiert; kleine Icon-Aktionen geben nun konsistentes Selection-Haptic-Feedback.
- Settings Admin verwendet nun zentrale Segment-/Selectable-Row-Bausteine fuer Automation Provider, Shopify Collection-Auswahl und Artist-Editor-Zuweisung statt lokaler Material-Surfaces.
- Home- und ArtistPage-Link-Pills verwenden `SkydownPremiumLinkSurface`; Social/External-Link-Oberflaechen teilen damit Border, Press-Motion und Gradient-Materialitaet.
- Home Topbar-Action, Founder-Briefing-Body, Productivity-Hinweise, Metric Chips, Count Badges und Quick Actions verwenden zentrale Premium Icon-/Inline-Surfaces.
- ArtistPage Now-Playing und Home Live Signal wurden auf Premium Inline/Icon-Surfaces migriert; Audio- und Status-Chrome teilen damit dieselbe Haptik.
- Home Media Section Banner und Daily Ops Strip verwenden zentrale Premium Inline/Icon-Surfaces; Musik-/Video-Panels teilen damit dieselbe kleine Status-Materialitaet.
- Native iOS-Paritaet ergaenzt: `SkydownLayout` fuehrt dieselben Icon-/Link-/Inline-Surface-Tokens wie Android, und `SkydownBrandActionButton.swift` stellt `SkydownPremiumIconAction`, `SkydownPremiumLinkSurface` und `SkydownPremiumInlineSurface` bereit.
- iOS Home Social Links, ArtistPage Connect/Now-Playing und VideoHub Upload-Dock nutzen jetzt zentrale Premium-Surfaces statt lokaler Capsule-/Button-Styles; Android und iOS sprechen damit sichtbar dieselbe Brand-Sprache.
- iOS AI Session Strip, Session-Aktionen und Prompt-Sheet-Loading/Close laufen ueber zentrale Premium Inline-/Icon-/Progress-Komponenten.
- iOS Profile-Hero-Aktionen verwenden `SkydownBrandActionButton` statt lokaler Capsule-Modifier; Edit-, Avatar- und Delete-Aktionen teilen damit CTA-Motion, Elevation und Disabled States.
- iOS AI/Agent Prompt-FABs verwenden nun `SkydownBrandActionButton` statt lokaler Frosted-Material-Pills; Working/Idle States teilen damit Loading, Elevation und CTA-Hierarchie.
- iOS Membership Control Loading nutzt zentrale Premium Inline-/Progress-Komponenten statt rohem `ProgressView`.
- iOS AI/Agent Usage Meter nutzen `SkydownPremiumLinearProgress`; Quota-Feedback teilt damit dieselbe ruhige Fortschritts-Materialitaet.
- iOS AI/Agent Streaming-Bubbles verwenden Premium Inline-/Circular-Progress States statt roher `ProgressView`s; Agent Result Links nutzen zentrale Link-Surfaces.
- iOS Quick-Prompt-Karten laufen ueber `SkydownPremiumPromptTile`; Text-, Visual- und Agent-Prompts teilen damit Radius, Border und Press-Motion.
- iOS Agent Attachments verwenden zentrale Premium Link-, Inline- und Icon-Actions fuer Add, Remove und Remove-All statt lokaler Button-Zeilen.
- iOS `SkydownPremiumIconSurface` trennt dekorative Premium-Icon-Flaechen von echten Icon-Aktionen; dadurch bleiben verschachtelte Buttons aus Media-Overlays und Result Cards heraus.
- iOS AI Generated-Visual-Preview und Fullscreen-Chrome nutzen zentrale Premium Icon-Surfaces/Icon-Actions fuer Open/Close statt lokaler Capsule-Overlays.
- iOS Agent Result Cards und Audio Preview Player verwenden nun denselben Premium-Gradient-, Border- und Icon-Chrome wie die uebrigen AI/Agent-Flows.
- iOS `AppColors.error` ergaenzt den Danger/Error-Token fuer hochwertige destruktive Aktionen ohne lokale `Color.red`-Streuung.
- iOS Profile Upload/Gallery und Gallery Viewer nutzen zentrale Premium Progress-/Icon-Actions fuer Upload, Delete und Close.
- iOS Video Reel Preparing, Fullscreen Close/Control-Bar und Selected Video Rows wurden auf Premium Progress-/Icon-Surfaces migriert.
- iOS Nicma Selected File Rows nutzen Premium Icon-Surfaces, Error-Actions und ruhige Gradient-Row-Materialitaet statt lokaler Circles/Standard-Destructive-Buttons.
- iOS VideoHub YouTube-Zeilen, Library-Delete-Actions, Edit-Panel-Remove-Actions und Viewer-Toolbar-Buttons laufen jetzt ueber Premium Link-/Brand-Actions mit zentralem Error-Token.
- iOS Settings, Music Toolbar und VideoHub Loading-/Validation-Zustaende verwenden keine rohen Destructive-Buttons, `.red`-Fehlertexte oder nackten Spinner mehr in den migrierten Premium-Flows.
- iOS Views-Sweep: Login, Registration, Home, Orders, Agent, Shop, Cart, MainTab und Profile wurden von rohen `ProgressView`, `Color.red` und `Button(role: .destructive)` auf Premium Progress-/Brand-/Error-Tokens migriert.
- iOS Editable Image/Video Utilities verwenden Premium Progress und Error Brand-Actions fuer Preview Loading, Upload Overlay und Remove-Actions statt roher Spinner/destruktiver Buttons.
- Android Home Manager Rows und Agent Composer/Attachments verwenden Premium Icon-Actions statt roher Material `IconButton`s; Edit, Delete, Refresh, Attach, Send und Close teilen jetzt Shape, Border, Elevation und Haptik.
- Android AI, Music, VideoHub, ArtistPage und Shop Hub-Actions wurden weiter auf Premium Icon-Actions migriert; verbleibende rohe IconButtons in diesen Screens sind primaer native Back-Navigation oder bestehender Fullscreen-Chrome.
- Android gemeinsame Topbar Session-Actions, YouTube Dialog Close, Nicma Preislisten-Editor, Profile Gallery Delete und Track Preview Close nutzen Premium Icon-Actions; rohe IconButtons bleiben nur noch fuer native Navigation, Password Visibility oder Fullscreen-Chrome.
- Android Fullscreen-Chrome nutzt intern keine Material `IconButton`s mehr; die eigene `SkydownFullscreenChromeIconButton` bleibt als spezieller Media-Control-Chrome erhalten, basiert aber auf einer kontrollierten Premium Surface-Action.
- Android Settings hat keinen lokalen Material-`IconButton`-Wrapper mehr; die Settings Close/Back-Aktion nutzt Premium Icon-Action, sodass rohe IconButtons nur noch native Back-Navigation und Password-Visibility abdecken.
- Android Login und Registration Password-Visibility-Actions nutzen Premium Icon-Actions; rohe IconButtons bleiben damit nur noch fuer native Back-Navigation und den benannten Fullscreen-Wrapper.
- Android Back-Navigation in ArtistPage, Profile, Music, VideoHub, OwnerHub, AI Hub, Shop und Cart wurde auf Premium Icon-Actions migriert; globale `IconButton(`-Treffer stammen jetzt nur noch vom benannten Fullscreen-Chrome-Wrapper.
- iOS Agent Note Detail Delete nutzt eine Premium Error Brand-Action; verbleibende destruktive SwiftUI-Buttons sind bewusst native Alert-, Confirmation- und Swipe-Actions.
- Android App-Shell/Music-Hub Back-Chrome in `SkydownApp.kt` nutzt ebenfalls Premium Icon-Actions, damit Launch- und Feature-Navigation dieselbe taktile Brand-Sprache sprechen.
- Android Settings Selection Controls sind weiter premiumisiert: `SkydownPremiumSwitch` ersetzt rohe Material-Switches, und Appearance-Auswahl nutzt einen ruhigen Brand-Indikator statt Standard-RadioButton.
- Android AI, Agent, Home und Settings nutzen nun systemweit `SkydownPremiumSwitch`; `Switch(`-Treffer stammen nur noch vom zentralen Premium-Control selbst.
- Android Premium-Control-Masse fuer Input-Hoehe, Progress-Stroke und Switch-Track/Knob liegen jetzt in `SkydownUiTokens`; die zentrale Komponente traegt keine magischen Produktmasse mehr.
- Android Screen-Imports wurden bereinigt: Shop, Home, AI und Agent importieren keine rohen Material-`Switch`/`IconButton` Defaults mehr, wenn die Premium-Actions die tatsaechliche UI stellen.
- Android Home Productivity Empty-Zeilen laufen ueber `HomeProductivityEmptyLine` und `SkydownPremiumInlineSurface`; kleine leere Zustaende wirken damit bewusst gesetzt statt wie nackte Text-Fallbacks.
- Android Shop Message Cards verwenden `skydownPanelSurface` und zentrale Padding-/Icon-Tokens statt lokaler SurfaceVariant-/Border-Konstruktion; Commerce Empty-, Pause- und Filter-Zustaende wirken dadurch konsistenter mit dem restlichen Premium-Chrome.
- Android kleine Statuspunkte verwenden `SkydownUiTokens.statusDotSize`; Home Empty Lines und Nicma Active Signals teilen damit dieselbe Mikro-Metrik.
- Android Settings Toggle Rows und Admin Workspace Summary Cards verwenden `skydownPanelSurface` mit zentralem Padding und Elevation; dichte Admin-Controls wirken dadurch weniger nach Backoffice-Default und staerker nach kontrolliertem Produkt-Chrome.
- Android Appearance-Auswahl nutzt `SettingsSelectableRow` und `SettingsSelectionDot`; Light/Dark/System ist damit keine lokale Surface-Sonderloesung mehr, sondern Teil derselben Premium-Auswahl-Sprache.
- iOS `SkydownPremiumToggleStyle` fuehrt Toggle-Materialitaet zentral mit Brand-Track, Knob, Haptik und Light/Dark-Farben; Auth Consent, Agent Automation/Social, Profile AI Access, Home Due-Date und VideoHub Visibility nutzen diesen Stil.
- iOS Settings Admin Toggles und Segmented Picker laufen nun ueber zentrale Premium Controls: `SkydownPremiumToggleStyle` fuer dichte Admin-Schalter und `SkydownPremiumSegmentedPicker` fuer Root-, Command-, Automation-, AI-Runtime- und Retention-Auswahlen.
