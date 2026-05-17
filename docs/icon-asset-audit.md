# SkyOS Icon & Asset Audit

This audit verifies icon readiness across Apple platforms and Android, with SkyOS as the primary visual identity.

## Scope

- Apple: `Assets.xcassets`, `AppIcon.appiconset`, `Contents.json`, PNG dimensions
- Android: launcher mipmaps, adaptive icon config, foreground source
- Branding consistency: SkyOS-first, with Skydown and ZweiZwei marks used contextually

## Apple audit (iOS / iPadOS / macOS)

Path:

- `Skydown App/Assets.xcassets/AppIcon.appiconset/Contents.json`

Result:

- All required iPhone sizes are present (`20`, `29`, `40`, `60` at required scales)
- All required iPad sizes are present (`20`, `29`, `40`, `76`, `83.5` at required scales)
- Marketing icon (`1024x1024`) is present
- All referenced PNG files exist
- Pixel dimensions match expected values from `size x scale`
- Active Apple AppIcon PNGs are opaque RGB files without alpha, matching App Store upload requirements
- No "Icon too large" mismatch found in catalog metadata

## Android audit

Paths:

- `androidApp/src/main/res/mipmap-mdpi/`
- `androidApp/src/main/res/mipmap-hdpi/`
- `androidApp/src/main/res/mipmap-xhdpi/`
- `androidApp/src/main/res/mipmap-xxhdpi/`
- `androidApp/src/main/res/mipmap-xxxhdpi/`
- `androidApp/src/main/res/mipmap-anydpi/ic_launcher.xml`
- `androidApp/src/main/res/mipmap-anydpi/ic_launcher_round.xml`
- `androidApp/src/main/res/drawable/ic_launcher_foreground.xml`
- `androidApp/src/main/res/values/dimens.xml` (`ic_launcher_foreground_inset`)
- `androidApp/src/main/res/drawable-nodpi/ic_launcher_foreground_src.png`

Launcher size verification:

| Density | Expected | `ic_launcher.png` | `ic_launcher_round.png` | Status |
| --- | --- | --- | --- | --- |
| mdpi | 48x48 | 48x48 | 48x48 | PASS |
| hdpi | 72x72 | 72x72 | 72x72 | PASS |
| xhdpi | 96x96 | 96x96 | 96x96 | PASS |
| xxhdpi | 144x144 | 144x144 | 144x144 | PASS |
| xxxhdpi | 192x192 | 192x192 | 192x192 | PASS |

Adaptive icon verification:

- `ic_launcher.xml` and `ic_launcher_round.xml` both use:
  - background: `@color/ic_launcher_background`
  - foreground: `@drawable/ic_launcher_foreground`
- `ic_launcher_background` exists in `androidApp/src/main/res/values/colors.xml`
- Foreground `ic_launcher_foreground.xml` wraps the Android-padded bitmap in an `inset` and uses `android:gravity="fill"` so the source scales into the layer (not `center`, which hard-clips). Inset is **small** (see `ic_launcher_foreground_inset`): the PNG already carries the safe-area padding needed for round adaptive masks.

## Project icon usage (core set)

The project uses three core icon identities plus one secondary mark:

| Asset | Role | Path |
| --- | --- | --- |
| SkyOS Mark | Primary system identity | `Skydown App/Assets.xcassets/SkyOSBrandMark.imageset/skyos-mark.png` |
| Skydown Logo | App/operator identity | `Skydown App/Assets.xcassets/SkydownBrandLogo.imageset/skydown-logo.png` |
| ZweiZwei Logo | Music brand identity | `Skydown App/Assets.xcassets/ZweiZweiBrandLogo.imageset/zweizwei-logo.png` |
| 22 Mark | Music/creative accent | `Skydown App/Assets.xcassets/Sky22BrandLogo.imageset/22-logo.png` |

## Branding enforcement

- SkyOS remains the dominant platform identity
- Skydown is used for app/operator communication
- ZweiZwei / 22 remains scoped to music and creative contexts
- Merch communication uses `Skydown x 22`

## Conclusion

Icon and asset structure is release-ready from a technical sizing and configuration perspective for Apple and Android.
No missing launcher sizes or broken app icon catalog references were detected in this audit.
