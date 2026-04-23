# SkyOS Screenshot Story

Screenshots should sell one idea: SkyOS feels like one premium product, not a bag of modules.

## Core Principle

Do not capture random pretty screens.

Build a story:

1. what SkyOS is
2. how AI helps
3. how Agent adds leverage
4. how media anchors the product
5. how membership adds value
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
| 1 | Welcome to SkyOS | Home | `One app for AI, media, and merch` | `Eine App fuer KI, Media und Merch` | Use the cleanest Home hero with strong hierarchy and no owner controls |
| 2 | Your AI Assistant | AI | `Write, plan, and create faster` | `Schneller schreiben, planen und erstellen` | Show AI in a clear empty or active state, not a cluttered debug state |
| 3 | Smart Agent Actions | Agent | `Turn briefs into clear next steps` | `Aus Briefings werden klare naechste Schritte` | Prefer an agent screen with structure, not a noisy long transcript |
| 4 | Music / Media Experience | Music or Video | `Releases, visuals, and artist focus` | `Releases, Visuals und Artist-Fokus` | Pick the stronger of Music or Video per store campaign |
| 5 | Membership Benefits | Settings Membership or AI membership sheet | `Unlock more reach, not more clutter` | `Mehr Reichweite, nicht mehr Unruhe` | Show plan clarity, restore visibility, and premium upgrade framing |
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

Current in-repo iOS screenshot coverage already exists for:

- Shop
- Music
- Home
- Video
- AI
- Settings

That is a strong base, but the final public set should be reordered and expanded around the story
above instead of shipping the raw capture order as-is.
