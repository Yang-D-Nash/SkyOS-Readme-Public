# SkyOS Screenshot Story

Screenshots should sell one idea: SkyOS feels like one premium product, not a bag of modules.

## Core Principle

Do not capture random pretty screens.

Build a story:

1. what SkyOS is
2. how AI helps
3. how Agent and workflows add leverage
4. how reminders, tasks, and notes keep the day usable
5. how media and membership anchor the product
6. how merch feels trustworthy
7. how native quality carries the whole experience

## Visual Rules

- prefer real app screens over composited fiction
- use short, sharp overlay copy
- avoid tiny feature labels or crowded annotation stacks
- do not use owner/admin screens in public store flows
- keep the top third visually strong for store thumbnail cropping
- preserve the native polish of the real UI rather than covering it with heavy marketing frames

## Recommended Sequence

| # | Story role | Preferred source | EN overlay | DE overlay | Notes |
| --- | --- | --- | --- | --- | --- |
| 1 | Welcome to SkyOS | Home | `One app for AI, workflows, and media` | `Eine App fuer KI, Workflows und Media` | Use the cleanest Home hero with strong hierarchy and no owner controls |
| 2 | Your AI Assistant | AI | `Write, plan, and create faster` | `Schneller schreiben, planen und erstellen` | Show AI in a clear empty or active state, not a cluttered debug state |
| 3 | Smart Agent Actions | Agent | `Turn prompts into usable workflows` | `Aus Prompts werden nutzbare Workflows` | Prefer an agent screen with structure, not a noisy long transcript |
| 4 | Productivity Core | Home or Agent/Productivity | `Reminders, tasks, and notes stay live` | `Reminder, Tasks und Notes bleiben live` | Use a screen that proves real utility without owner-only controls |
| 5 | Music / Media Experience | Music or Video | `Releases, visuals, and artist focus` | `Releases, Visuals und Artist-Fokus` | Pick the stronger of Music or Video per store campaign |
| 6 | Premium Commerce / Merch | Shop | `Discover drops and check out clearly` | `Drops entdecken und klar auschecken` | Prefer catalog or detail view that feels trustworthy, not overly dense |
| 7 | Native Quality | Home, Settings, or Android large-screen view | `Native, clear, and built to last` | `Nativ, klar und auf Dauer gebaut` | Final frame should reinforce trust and polish rather than add more features |

## Platform Notes

### iPhone / App Store

- lead with Home, AI, Agent, Music or Video, Membership, Shop, Trust
- the final frame should feel premium and calm, not admin-heavy
- current screenshot capture foundation exists in `Skydown AppUITests/Skydown_AppUITests.swift`

### Android / Google Play

- keep the same narrative, but use one frame that proves strong Android adaptation
- if Fold or large-screen layouts look meaningfully better, use that advantage explicitly
- do not stretch phone screenshots into tablet marketing art

## Capture Guidance

- capture in English first, then localize overlay text deliberately for German
- use the same visual order across iOS and Android wherever possible
- if one screen is weaker on one platform, replace the screen, not the story role
- never let a weaker screen stay in the set just because it matches the old order

## Current Capture Mapping

Current in-repo capture coverage exists for:

- `store-assets/ios/raw/`: 7 iPhone captures at `1320x2868`
- `screenshots/final/ios/`: 6 iPhone captures at `1320x2868`
- `screenshots/final/ipad/`: 7 iPad captures at `2064x2752`
- `store-assets/android/raw/`: 7 Android phone captures at `1080x2424`
- `screenshots/final/android/`: 6 Android phone captures at `1080x2424`
- `screenshots/final/google-play/android-phone/`: 7 Play-compliant Android phone captures at `1242x2424`
- `store-assets/fold/raw/`: 7 foldable captures at `1812x2176`

Run the automated release gate before uploading screenshots:

```bash
python3 scripts/audit_store_screenshots.py
```

This check validates expected counts, dimensions, alpha safety, Google Play side-ratio compliance,
pixel duplicates inside each set, and low-detail blue debug-placeholder blocks.

That is a strong base, but it is not a complete public upload set until the console mapping and
final story/order review are closed.

Release blockers before listing upload:

- Store exports and listing graphics still need to be uploaded and mapped in App Store Connect and Play Console.
- Final public sets should use the story order above, not raw capture order.
