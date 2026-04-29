# SkyOS Branding Standard

SkyOS is the primary system identity for this repository.  
All public communication, visual language, and platform assets should reinforce one rule: **SkyOS is the foundation and operating core**.

## 1) Brand hierarchy

| Brand | Role | Scope |
| --- | --- | --- |
| `SkyOS` | system, platform, visual operating language | primary naming in README, product communication, design narrative |
| `Skydown` | app/operator identity inside SkyOS | product surfaces, support and operational context |
| `ZweiZwei / 22` | music and creative sub-brand | only for music-specific moments |
| `Skydown x 22` | merch and commerce mark | only for shop/merch communication |

## 2) Naming rules (public copy)

Preferred labels:

- `SkyOS`
- `Home`
- `AI`
- `Agent`
- `Music`
- `Video`
- `Shop`
- `Membership`
- `Settings`
- `Owner` / `Admin`

Avoid unless strictly necessary:

- legacy mixed product labels
- `Skydown OS`
- `Command Center`
- `Revenue Ops`
- `Runtime` as a user-facing tab label
- provider-first copy (`n8n`, Stripe, etc.) where user value can be explained without vendor names

## 3) Voice and tone

SkyOS communication should be:

- clear
- premium
- calm
- direct
- trustworthy

It should never sound:

- overhyped
- internal-only
- vague
- aggressively sales-driven
- unfinished

## 4) Copy quality checklist

- Lead with user value, not implementation detail.
- Keep labels short and consistent across macOS, iOS, and Android.
- Avoid mixing internal tooling names into user-visible actions.
- Explicitly mark role-restricted features.
- State legal or plan-based constraints in plain language.

## 5) Official icon and asset sources

Primary documentation assets:

- `docs/assets/skyos-logo.png`
- `docs/assets/skyos-app-icon.png` (Galerie-Vorschau, skaliert, entspricht iOS-Master)
- `docs/assets/skyos-app-icon-1024.png` (1024×1024 **voll** — transparenter Master; Apple `AppIcon.appiconset` ist daraus als opaque PNG ohne Alpha exportiert)
- `docs/assets/skyos-app-icon-1024-android-padded.png` (1024×1024 Android-Spiegel des transparenten Masters; auf 78% skaliert, damit Adaptive-Icon-Masken nichts abschneiden; liegende Datei: `ic_launcher_foreground_src.png`)
- `docs/assets/skyos-22-mark.png`

Project icon assets (the three core logos):

- `Skydown App/Assets.xcassets/SkyOSBrandMark.imageset/skyos-mark.png`
- `Skydown App/Assets.xcassets/SkydownBrandLogo.imageset/skydown-logo.png`
- `Skydown App/Assets.xcassets/ZweiZweiBrandLogo.imageset/zweizwei-logo.png`

Additional mark:

- `Skydown App/Assets.xcassets/Sky22BrandLogo.imageset/22-logo.png`

Freisteller (liegen in `docs/assets/`, als PNG normalisiert; für Schritt 2: iOS Image-Sets / Android / README):

- `docs/assets/skydown-mark-freisteller.png`
- `docs/assets/zweizwei-mark-freisteller.png`
- `docs/assets/skydown-x-22-mark-freisteller.png`

## 6) Trust rule

In SkyOS, branding quality includes legal and operational quality:

- legal naming is consistent
- support and ownership context is credible
- copy reflects actual product behavior
- visual polish is backed by technical reliability

If a phrase sounds impressive but reduces clarity, remove it.

## 7) Product integration matrix

Brand usage in product must stay scoped and consistent:

| Surface | Primary mark | Secondary mark | Rule |
| --- | --- | --- | --- |
| Home / system entry | `SkyOS` | `Skydown` | Home communicates system + product entry, never music-first |
| AI / Agent / Video | `Skydown` | `SkyOS` context only | Keep execution/product language, avoid music marks |
| Music | `ZweiZwei / 22` | `Skydown` | Music keeps its own identity inside SkyOS |
| Shop / Merch | `Skydown x 22` | `Skydown` | Commerce mark is scoped to merch only |
| App icon / launcher | `SkyOS` | none | Launcher identity stays system-first |

## 8) Source logos provided by brand

The latest source files are stored for traceable reuse:

- `docs/assets/skyos-logo-original.png`
- `docs/assets/skydown-logo-original.png`
- `docs/assets/zweizwei-logo-original.png`
