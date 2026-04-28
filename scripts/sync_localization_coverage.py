#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path


ROOT = Path("/Users/nash/StudioProjects/SkyOs-App")

IOS_EN = ROOT / "Skydown App/en.lproj/Localizable.strings"
IOS_ALL = sorted((ROOT / "Skydown App").glob("*.lproj/Localizable.strings"))

ANDROID_BASE = ROOT / "androidApp/src/main/res/values/strings.xml"
ANDROID_ALL = sorted((ROOT / "androidApp/src/main/res").glob("values*/strings.xml"))


IOS_PAIR_RE = re.compile(r'^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$', re.MULTILINE)
IOS_CALL_RE = re.compile(
    r'AppLocalized\.text\(\s*"([^"]+)"\s*,\s*fallback:\s*"((?:[^"\\]|\\.)*)"',
    re.MULTILINE,
)
ANDROID_STRING_TAG_RE = re.compile(
    r"(<string\b[^>]*\bname=\"([^\"]+)\"[^>]*>.*?</string>)",
    re.DOTALL,
)


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")


def sync_ios() -> tuple[int, int]:
    en_text = read_text(IOS_EN)
    en_pairs = dict(IOS_PAIR_RE.findall(en_text))

    swift_files = list((ROOT / "Skydown App").rglob("*.swift"))
    discovered: dict[str, str] = {}
    for file in swift_files:
        for key, fallback in IOS_CALL_RE.findall(read_text(file)):
            discovered.setdefault(key, fallback)

    added_to_en = 0
    if discovered:
        additions: list[str] = []
        for key in sorted(discovered):
            if key not in en_pairs:
                value = discovered[key].replace("\\", "\\\\").replace('"', '\\"')
                additions.append(f"\"{key}\" = \"{value}\";")
                en_pairs[key] = discovered[key]
                added_to_en += 1
        if additions:
            if not en_text.endswith("\n"):
                en_text += "\n"
            en_text += "\n" + "\n".join(additions) + "\n"
            write_text(IOS_EN, en_text)

    total_added_to_locales = 0
    en_text = read_text(IOS_EN)
    en_pairs = dict(IOS_PAIR_RE.findall(en_text))
    for locale_file in IOS_ALL:
        if locale_file == IOS_EN:
            continue
        locale_text = read_text(locale_file)
        locale_pairs = dict(IOS_PAIR_RE.findall(locale_text))
        missing = [k for k in sorted(en_pairs) if k not in locale_pairs]
        if not missing:
            continue
        additions = []
        for key in missing:
            value = en_pairs[key].replace("\\", "\\\\").replace('"', '\\"')
            additions.append(f"\"{key}\" = \"{value}\";")
        if not locale_text.endswith("\n"):
            locale_text += "\n"
        locale_text += "\n" + "\n".join(additions) + "\n"
        write_text(locale_file, locale_text)
        total_added_to_locales += len(missing)

    return added_to_en, total_added_to_locales


def sync_android() -> int:
    base_text = read_text(ANDROID_BASE)
    base_entries = {name: full for full, name in ANDROID_STRING_TAG_RE.findall(base_text)}
    total_added = 0

    for locale_file in ANDROID_ALL:
        if locale_file == ANDROID_BASE:
            continue
        locale_text = read_text(locale_file)
        locale_names = {name for _, name in ANDROID_STRING_TAG_RE.findall(locale_text)}
        missing = [k for k in sorted(base_entries) if k not in locale_names]
        if not missing:
            continue

        insertion = "\n".join(f"    {base_entries[key]}" for key in missing)
        marker = "</resources>"
        idx = locale_text.rfind(marker)
        if idx == -1:
            raise RuntimeError(f"Invalid Android strings file: {locale_file}")
        before = locale_text[:idx].rstrip() + "\n\n"
        after = locale_text[idx:]
        new_text = before + insertion + "\n\n" + after
        write_text(locale_file, new_text)
        total_added += len(missing)

    return total_added


def main() -> None:
    ios_en_added, ios_locale_added = sync_ios()
    android_added = sync_android()
    print(
        f"Localization sync done. iOS added to en: {ios_en_added}, "
        f"iOS added to other locales: {ios_locale_added}, "
        f"Android added to locale files: {android_added}."
    )


if __name__ == "__main__":
    main()
