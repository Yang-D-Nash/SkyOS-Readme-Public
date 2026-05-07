# SkyOS Premium Brand System

## Brand Position

SkyOS ist ein ruhiges Atelier fuer digitale Musik-, Commerce- und Agent-Workflows. Die Marke fuehlt sich nicht laut, ornamental oder tech-demohaft an, sondern wie ein hochwertiges Instrument: praezise, kontrolliert, taktil, vertrauenswuerdig und bewusst exklusiv.

## Brand Audit

- Staerken: Die App besitzt bereits eigene Hero-, Card-, Motion-, Sheet-, Input-, Toast-, Icon-Action- und Progress-Komponenten. Android Compose und SwiftUI sprechen in vielen Kernflaechen schon eine erkennbare gemeinsame Sprache.
- Premium-Leck 1: Einzelne Screen- und Komponentenbereiche enthalten weiterhin lokale `dp`-, Border-, Alpha- und Shadow-Entscheidungen. Das wirkt nicht sofort kaputt, aber es schwacht die kompromisslose Kontrolle.
- Premium-Leck 2: Settings/Admin ist funktional stark, aber die Dichte kann noch wie Backoffice statt wie Command Center wirken. Prioritaet: progressive Disclosure, klarere Gruppenrhythmik, weniger gleichwertige Formularfelder.
- Premium-Leck 3: Echte Premium-Assets fehlen noch. Die vorhandene Atmosphaere ist brauchbar, aber Weltklasse braucht spezifische Produkt-, Musik-, Commerce- und Agent-Visuals statt generischer Mood-Flaechen.
- Premium-Leck 4: Motion ist technisch vorhanden, aber noch nicht als Design-Reference mit Szenen, Dauer, Easing und Reduce-Motion-Verhalten dokumentiert.
- Premium-Leck 5: Einige native/Material-Bruecken bleiben absichtlich bestehen, muessen aber in Reviews klar als Implementierungsdetail erkennbar sein, nicht als sichtbarer UI-Default.

## Creative Direction

Quiet Precision. Obsidian fuer Tiefe, Porcelain und Platinum fuer Licht, Mystic Steel fuer Struktur, Champagne fuer seltene Wertigkeit. Keine dekorative Ueberladung, keine laute Farbdramaturgie, keine Standard-Material-Anmutung. Jede Interaktion soll klein, ruhig und bewusst reagieren.

## Voice

Kurz, bestimmt, elegant. SkyOS sagt, was passiert, nicht wie clever die Technik ist. Fehler nennen Ursache und naechsten Schritt. Empty States zeigen Moeglichkeit statt Mangel. Admin-Copy bleibt sachlich, aber nicht kalt.

## Color System

- Primary: Sky Ink / Sky Light fuer Navigation, aktive Zustaende und starke CTA.
- Secondary: Mystic Steel fuer Metadaten, Struktur und sekundare Aktion.
- Accent: Champagne / Aurora fuer Wertigkeit, seltene Highlights und Premium-Signale.
- Background: Porcelain im Light Mode, Obsidian im Dark Mode.
- Surface: Platinum, Satin Graphite und tokenisierte Chrome-/Sheet-Rollen.
- Success/Error: gedaempft, kontraststark, nie neon oder alarmistisch.

## Type System

- Display: Awergy fuer ikonische Hero-Momente und Brand-Signaturen.
- UI/Editorial: Syne fuer Interface, Labels, Body und Admin.
- Skala: klare Hierarchie von Hero ueber Panel bis Caption, ohne negative Letter-Spacing-Werte.
- Accessibility: Schriftgroessen bleiben `sp`, Texte werden ellipsiert oder umbrechen kontrolliert, keine viewport-basierte Font-Skalierung.

## Layout Tokens

- Spacing: `SkydownUiTokens` fuehrt Screen-, Card-, Panel-, Hero-, Stack-, Button-, Input-, Sheet- und Media-Masse zentral.
- Radius: kompakte 8-18dp fuer dichte Controls, 20-28dp fuer Cards/Hero/Spotlight, Capsule nur fuer echte Pills.
- Elevation: `elevationHairline`, `elevationBrandBorder`, `elevationRaised`, `elevationStateIcon`, `elevationPanel`, `elevationHero`.
- Chrome: Hero-Ornamente, Metric-Icons, Sheet-Handles, Button-Strokes und Progress-Strokes sind Tokens, nicht lokale Dekoration.

## Component Rules

- Buttons: `BrandActionButton` fuer Entscheidungen; gefuellt nur fuer primaere Handlung, outline fuer sekundare Handlung, Loading und Disabled integriert.
- Icon Actions: `SkydownPremiumIconAction` fuer kleine Werkzeuge, inklusive Haptik, Border, Elevation und Press-Motion.
- Cards: `SkydownCard` oder `skydownPanelSurface`; keine nackten Card-/Surface-Defaults in sichtbaren Premium-Flows.
- Inputs: `SkydownPremiumTextField`; bestehende Settings-Wrapper duerfen nur als Migrationsbruecke existieren.
- Empty/Loading: `SkydownPremiumStatePanel`, `SkydownPremiumLinearProgress`, `SkydownPremiumCircularProgress`.
- Sheets: `skydownPremiumSheet*` Defaults fuer Shape, Container, Content, Scrim und Drag Handle.

## Motion

150-320ms, ruhige Deceleration, kleine Scale- und Alpha-Antworten, kein verspieltes Springen. Haptik nur bei bewussten Auswahl- und Press-Momenten. Reduce Motion wird respektiert.

## Asset Needs

- Finales App-Icon-System fuer Light/Dark und Store-Kontexte.
- Hero-/Surface-Visuals je Produktbereich: Home, AI, Agent, Music, Video, Shop, Membership.
- Einheitliches Icon-Set fuer Produktmodule und Admin-Aktionen.
- Screenshot-Set fuer Light/Dark, Compact/Expanded, Deutsch/Englisch und Store-Captures.

## Next Premium Iteration

- Settings/Admin in Command-Center-Gruppen schneiden: weniger sichtbare Felder pro Ebene, mehr klare Save-/Review-Zustaende.
- Restliche lokale `dp`/Alpha/Color-Entscheidungen screenweise abbauen.
- Motion-Spec als testbare Referenz mit Szenen und Timing dokumentieren.
- Visuelle Regression fuer Top-Screens in Light/Dark und Deutsch etablieren.

## Implementierte Iteration

- Android Settings/Membership Ops fuehrt `AdminCommandPanel` als Command-Center-Baustein ein: Dashboard- und Experiment-Cluster nutzen Premium Icon-Surface, Panel-Materialitaet, tokenisierten Rhythmus und zentrale Progress-Masse statt loser Text-/Formularbloecke.
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
