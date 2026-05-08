# SkyOS Premium Screenshot System

Status: `production capture playbook`

This playbook defines the creative direction, sequence, and capture standards for App Store and Google Play screenshots in a premium Apple/OpenAI-style quality bar.

## Current Release Asset Status

Status as of 2026-05-05: capture foundations and final screenshot sets exist; final console attachment and store listing approval remain external release gates.

Update 2026-05-08: `scripts/audit_store_screenshots.py` is the automated screenshot gate. It must pass before App Store Connect or Play Console upload.

| Set | Current files | Release read |
| --- | --- | --- |
| iPhone raw | `store-assets/ios/raw/`, 7 files at `1320x2868` | Usable capture base |
| iOS final | `screenshots/final/ios/`, 6 files at `1320x2868` | Needs final story/order approval |
| Android phone raw | `store-assets/android/raw/`, 7 files at `1080x2424` | Not Play-upload ready because aspect ratio exceeds 2:1 |
| Android final | `screenshots/final/android/`, 6 files at `1080x2424` | Not Play-upload ready because aspect ratio exceeds 2:1 |
| Google Play Android export | `screenshots/final/google-play/android-phone/`, 7 files at `1242x2424` | Play-compliant phone set; upload/map in Play Console |
| Fold raw | `store-assets/fold/raw/`, 7 files at `1812x2176` | Optional secondary set |
| iPad | `screenshots/final/ipad/`, 7 files at `2064x2752` | App Store-ready iPad set; upload/map in App Store Connect |
| Play listing graphics | `docs/assets/google-play/skyos-play-icon-512.png`, `docs/assets/google-play/skyos-feature-graphic-1024x500.png` | Exported; review/upload in Play Console |

## Creative Objective

Screenshot set must communicate in under 5 seconds:

1. SkyOS is the operating core.
2. Skydown is one coherent premium product.
3. AI, Music, Video, and Merch belong to one system.
4. Trust and clarity are built into the experience.

Primary KPI: higher listing conversion from impression to install.

## Art Direction

- Modern, clean, premium.
- High contrast hierarchy, low visual noise.
- Calm compositional rhythm (no clutter, no busy overlays).
- One message per frame, maximum two lines of text.
- Brand integrity: SkyOS dominant, module brands contextual only.

### Brand mapping for screenshot overlays

- Frame 1 (`Home`): `SkyOS` + `Skydown`
- Frame 2 (`AI`): `Skydown` only
- Frame 3 (`Music`): `ZweiZwei / 22` (optionally with `Skydown` context line)
- Frame 4 (`Video`): `Skydown` only
- Frame 5 (`Merch`): `Skydown x 22`
- Frame 6 (`Trust/Settings`): `SkyOS` + `Skydown`

## Final Screenshot Narrative (shared order for iOS + Android)

| # | Screen | Strategic message | Headline (EN) | Subline (EN) |
| --- | --- | --- | --- | --- |
| 1 | Home / System Entry | One operating world, not fragmented tools | `One System. One Flow.` | `Skydown runs on SkyOS.` |
| 2 | AI Workspace | Practical utility with focus and control | `AI That Stays Clear` | `Prompts, context, and results in one workspace.` |
| 3 | Music (ZweiZwei / 22) | Distinct music identity inside core product | `Music, Curated` | `A dedicated ZweiZwei / 22 experience.` |
| 4 | Video Surface | Strong media utility without noise | `Video, In Focus` | `Clean playback and visual storytelling.` |
| 5 | Merch (Skydown x 22) | Commerce with trust and structure | `Merch, Made Simple` | `From discovery to cart with confidence.` |
| 6 | Settings / Trust | Product maturity and transparency | `Built For Trust` | `Support, policy, and account clarity in-app.` |

## Localized Overlay Copy

| # | DE Headline | DE Subline |
| --- | --- | --- |
| 1 | `Ein System. Ein Flow.` | `Skydown basiert auf SkyOS.` |
| 2 | `AI mit Klarheit` | `Prompts, Kontext und Ergebnisse an einem Ort.` |
| 3 | `Music, kuratiert` | `Ein eigener ZweiZwei / 22 Bereich.` |
| 4 | `Video im Fokus` | `Klare Visuals ohne Ueberladung.` |
| 5 | `Merch, einfach` | `Von Produktansicht bis Cart nachvollziehbar.` |
| 6 | `Fuer Vertrauen gebaut` | `Support, Richtlinien und Kontozugriff in der App.` |

## Platform Capture Specs

### iOS (App Store)

- Primary set: iPhone 6.7"
- Optional secondary: iPhone 6.5" derived from same story
- iPad set: `screenshots/final/ipad/` captured on iPad Pro 13-inch simulator
- Preferred visual mode: Dark Mode if SkyOS quality reads stronger

### Android (Play Store)

- Primary set: phone screenshots
- Secondary sets (tablet/foldable) only when layout quality adds value
- Keep same narrative order as iOS
- Preserve Android-native spacing and component behavior

## Composition Rules Per Frame

- Keep main UI focal area in top 60 percent of the screen.
- Use consistent text alignment and safe margins across all six frames.
- Avoid large decorative gradients that reduce feature readability.
- No more than one branded accent (ZweiZwei / 22) outside music frame.
- Keep status bars clean and consistent where possible.

## Do / Do Not

Do:

- Use real, stable demo data.
- Show complete, intentional states.
- Keep overlays short and readable at thumbnail scale.
- Validate first frame readability at very small preview size.

Do not:

- Show debug labels, test IDs, internal tools, or empty skeleton states.
- Show legal placeholders or unfinished notices.
- Mix music branding into AI/video/merch frames.
- Use hype claims (`best`, `perfect`, `guaranteed`).

## Capture Runbook

### Pre-capture

- Use one clean demo account per platform.
- Preload AI response, music content, video frame, merch product, and settings/legal entries.
- Disable any non-essential experimental flags or internal banners.
- Verify final app icon and brand assets before capture.

### During capture

- Capture each frame at least 3 times, keep strongest composition.
- Check typography and overlay contrast after each capture.
- Maintain identical copy style across iOS and Android.
- Prefer static confidence over flashy transitions.

### Post-capture QA

- Narrative sequence feels intentional from frame 1 to 6.
- SkyOS system identity is obvious from frame 1.
- No forbidden claims or unfinished legal messaging.
- File naming is consistent for handoff and upload.

## Export Naming Convention

- `ios_en_01_home.png` ... `ios_en_06_trust.png`
- `ios_de_01_home.png` ... `ios_de_06_trust.png`
- `android_en_01_home.png` ... `android_en_06_trust.png`
- `android_de_01_home.png` ... `android_de_06_trust.png`

## Final Approval Gate

Approve screenshot set only if all are true:

- Conversion narrative is clear from thumbnails.
- Branding is premium, calm, and modern.
- SkyOS is dominant, Skydown is coherent, sub-brands are scoped correctly.
- App Store and Play Store compliance checks pass.
- iPad screenshots are uploaded/mapped if iOS stays universal.
- Android screenshots satisfy Google Play side-ratio constraints.
- Google Play app icon and feature graphic are exported and reviewed.
