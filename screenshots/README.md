# Screenshots Manifest

This folder documents the validated screenshot set already present in the repository and maps it to handover-required categories.

## Required mapping

- `home`: `screenshots/final/ios/01_home.png`
- `main-feature`: `screenshots/final/google-play/android-phone/03_agent.png`
- `dashboard-app-flow`: `screenshots/final/google-play/android-phone/07_membership.png`
- `mobile-view`: `screenshots/final/android/01_home.png`
- `empty-state`: `screenshots/final/ios/02_ai.png` (AI entry state capture)
- `error-state`: not part of public store story set; intentionally excluded from listing assets

## Additional validated sets

- iOS set: `screenshots/final/ios/`
- iPad set: `screenshots/final/ipad/`
- Android set: `screenshots/final/android/`
- Google Play compliant set: `screenshots/final/google-play/android-phone/`

## Note

The authoritative screenshot production and upload process is documented in:

- `docs/store/screenshots.md`
- `docs/release/store-upload-runbook.md`

Before public upload, run:

```bash
python3 scripts/audit_store_screenshots.py
```
