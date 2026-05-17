# SkyOS Premium Brand System

## Brand Audit

Die App hat bereits eine eigenstaendige Basis: Syne/Awergy-Typografie, Markenlogos, ein cineastischer Atmosphere-Hintergrund, eigene Cards, Motion-Tokens, Press-Haptics und mehrere brandnahe Komponenten. Sie wirkt nicht wie reine Standard-Material-UI.

Die groessten Premium-Brueche liegen aktuell in der Skalierung:

- Viele Screens nutzen noch rohe `OutlinedTextField`, `IconButton`, `FloatingActionButton`, `Color(...)`, `dp` und Alpha-Werte direkt im Screen.
- Abstaende und Radii sind zwar teilweise tokenisiert, aber Screen-lokal noch zu haeufig individuell gesetzt.
- Formular-Fokus, Helper Copy, Disabled States und Error States waren uneinheitlich und dadurch weniger luxurioes als Cards/Heroes.
- Navigation und Sheets haben starke Einzel-Loesungen, aber noch kein vollstaendig einheitliches Route-/Transition-System.
- Empty States und Loading States sind nicht ueberall als Premium-Komponenten modelliert.
- Iconographie ist Material Icons Extended, aber noch nicht konsequent auf einen ruhigen, duennen, praezisen Stil kuratiert.
- Einige Kommentare und Tokens spiegeln iOS-Paritaet, die Android-/KMP-Schicht braucht aber noch eine kompakte, produktweite Token-Spezifikation.

## Positioning

SkyOS ist ein private operating layer for creative commerce: ein ruhiger, exklusiver Kontrollraum fuer Music, AI, Merch, Video und Owner Workflows.

Die Marke soll nicht laut wirken. Sie soll praezise, rar, kultiviert und vertrauenswuerdig wirken: ein digitales Atelier mit technischer Autoritaet.

## Tonality

- Kurz, klar, sicher.
- Keine generischen SaaS-Floskeln.
- Microcopy fuehlt sich wie Concierge-Kommunikation an: hilfreich, knapp, nie anbiedernd.
- Fehlertexte sagen, was passiert ist und was jetzt moeglich ist.

## Visual Idea

"Cinematic atelier": tiefe, ruhige Flaechen, weiche Luminanz, kontrollierte Akzentpunkte, klare Hierarchie, grosszuegige Atemraeume und praezise Interaktion.

## Color System

Primary:
- Light `SkyLight` #516884
- Dark `SkyDark` #C6D6E8

Secondary:
- Light `MysticLight` #79879C
- Dark `MysticDark` #DBE3EC

Accent:
- Light `AuroraLight` #B59F7D
- Dark `AuroraDark` #F2E3C9

Background / Surface:
- Light background #F7F6F3, surface #FFFBF8
- Dark background #090F18, surface #1A2534

Status:
- Success Light #2F6B55, Dark #9AD8BE
- Error Light #9B2F3F, Dark #FFB3BF

All screen code should resolve colors through `MaterialTheme.colorScheme` or `skydown*` role helpers.

## Typography

Display: Awergy for signature hero moments only.
Text/UI: Syne for navigation, body, labels, panels and controls.

Principles:
- No viewport-scaled font sizes.
- No negative letter spacing in compact controls.
- Hero scale only for true hero surfaces.
- Labels remain high contrast and short.

## Spacing

Base grid: 4dp.

Current canonical tokens live in `SkydownUiTokens`:
- Screen horizontal: 20dp
- Card padding: 16dp
- Panel padding: 20dp
- Section spacing: 22dp
- Compact section spacing: 16dp

New screen work should compose from these tokens, not invent local rhythm.

## Radius

Premium surfaces use controlled softness:
- Micro: 8dp
- Tight: 12dp
- Dense: 16dp
- Card: 22dp
- Hero: 32dp
- Capsule: 999dp

Buttons should feel tailored, not pill-by-default.

## Elevation

Elevation is cinematic, not generic:
- Low contrast ambient shadow for cards.
- Accent shadow only when it signals an actionable premium control.
- Hairline gradient borders for definition.
- Avoid stacked cards inside cards.

## Components

Buttons:
- `BrandActionButton` for primary/secondary actions.
- Filled = high intent.
- Outline = secondary or quiet command.
- Loading state lives inside the button.

Inputs:
- `SkydownPremiumTextField` is the default input component.
- It owns focus color, container color, error color, shape, helper text and disabled state.

Cards:
- `SkydownCard` and `skydownPanelSurface` are the baseline premium surfaces.
- Repeated items can be cards; page sections should stay unframed or full-width.

Empty / Loading:
- `SkydownPremiumStatePanel` is the shared premium state surface.
- Use it for empty libraries, unavailable data, setup prompts and calm loading moments.

Microcopy:
- `SkydownPremiumMicrocopy` keeps helper text consistent and less noisy.

## Motion

Motion is calm and decisive:
- Standard enter: 300-320ms.
- Exit: 180-240ms.
- Press: subtle scale and haptic.
- Respect reduce-motion settings.
- Avoid bounce unless the domain explicitly benefits from playfulness.

## Accessibility

- Use Material roles plus brand color helpers for contrast.
- Disable fields/actions during loading, do not just mute them visually.
- Keep text scalable through `sp` typography.
- Do not rely on color alone for errors or progress.

## Asset TODOs

- Final app icon family aligned with current premium palette.
- Optional splash/launch still frame for users with reduced motion.
- Curated icon set rules: stroke weight, fill use, active/inactive states.
- Real product/media imagery for first viewport moments where remote content is missing.
