# Home Copy Freeze Checklist

This checklist defines the approved microcopy tone for the Home surface across iOS and Android.
Use it as a guardrail when adding or editing copy in Home flows.

## Brand Tone Rules

- Keep copy short, calm, and directive.
- Prefer action verbs that signal intent (`Create`, `Open`, `Refresh`, `Show`).
- Avoid alarmist language; recovery text should feel controlled and reversible.
- Favor present tense and plain words over technical phrasing.
- Keep CTA labels to one or two words whenever possible.

## Approved CTA Verb System (Home)

- **Create**: for new user objects (reminder, task, note).
- **Open**: for navigation to existing content or modules.
- **Refresh**: for recoverable sync/data retries.
- **Show less / Show fewer sections**: for progressive disclosure toggles.

## Approved Recovery and Empty-State Copy

- **Empty prompt**: "Nothing active yet. Start with a quick reminder, task, or note."
- **Recovery prompt**: "Sync is paused. Tap Refresh to continue."

## Where This Copy Lives

- Android base strings: `androidApp/src/main/res/values/strings.xml`
- Android locale strings: `androidApp/src/main/res/values-*/strings.xml`
- iOS fallback copy for Home: `Skydown App/Views/Home/HomeView.swift`

## Freeze Scope

This freeze currently applies to:

- Home productivity card copy
- Home collapsed/expanded section labels
- Home creation CTAs and sheet primary action labels
- Home recovery inline banner copy

## Open Follow-Ups (post-freeze)

- Localize newer Home fallback keys on iOS `Localizable.strings` for non-default languages.
- Audit hardcoded founder-workflow status strings in Android Home and migrate to resources.
- Run an optional linguistic QA pass with native speakers for `es`, `fr`, `it`, `nl`, `pl`, `pt`, `tr`, `ja`.
