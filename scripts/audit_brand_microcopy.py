#!/usr/bin/env python3
"""Guard visible brand copy against cheap or machine-translated wording."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]


def parse_ios_strings(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    pattern = re.compile(r'^\s*"((?:\\"|[^"])*)"\s*=\s*"((?:\\"|[^"])*)";')
    for line in path.read_text(encoding="utf-8").splitlines():
        match = pattern.match(line)
        if not match:
            continue
        key, value = match.groups()
        values[key.replace(r"\"", '"')] = value.replace(r"\"", '"')
    return values


def parse_android_strings(path: Path) -> dict[str, str]:
    root = ET.parse(path).getroot()
    values: dict[str, str] = {}
    for item in root.findall("string"):
        name = item.attrib.get("name")
        if not name:
            continue
        values[name] = "".join(item.itertext())
    return values


EXPECTED_VALUES = [
    (
        ROOT / "Skydown App/de.lproj/Localizable.strings",
        parse_ios_strings,
        {
            "tabs.merch": "Merch",
            "tabs.home": "Home",
            "tabs.videos": "Video",
            "home.media.video.title": "Video",
            "home.hero.pill.video.next.load": "Video",
            "home.hero.pill.video.next": "Video",
            "home.hero.pill.video.loading": "Video",
            "home.hero.pill.video.live": "Video",
            "home.utility.videos": "Video",
            "home.utility.merch": "Merch",
        },
    ),
    (
        ROOT / "androidApp/src/main/res/values-de/strings.xml",
        parse_android_strings,
        {
            "tabs_home": "Home",
            "tabs_videos": "Video",
            "home_hero_pill_video_next_load": "Video",
            "home_hero_pill_video_next": "Video",
            "home_hero_pill_video_loading": "Video",
            "home_hero_pill_video_live": "Video",
            "home_utility_videos": "Video",
            "video_lib_loading": "Videos werden vorbereitet...",
            "video_reel_title_reel": "SkyOS Reel",
            "video_reel_preparing": "Clip wird vorbereitet...",
            "ai_disabled_title": "SkyOS Bot pausiert",
            "agent_workflow_provider": "Activepieces",
            "agent_action_share": "Teilen",
            "agent_action_open": "Öffnen",
        },
    ),
]


FORBIDDEN_FRAGMENTS = [
    "UI Test Hoodie",
    "UI Test Reel",
    "SkyOS Owner (UI test)",
    "SkyOS-Haspel",
    "Prüfgegenstand",
    "Testartikel",
    "test item",
    "item de teste",
    "testartikel",
    "Preparing videos",
    "Preparing clip",
    "Aktive Stücke",
    "Teilen Sie",
    "Spieler einladen",
    "Aktiv im Spieler",
]


FORBIDDEN_EXACT_VALUES = {
    "Heim",
    "videos",
}


STORE_FORBIDDEN = [
    "App Name: `SkyOS`",
    "Title: `SkyOS`",
    "Keep the title as `SkyOS`",
]


def localization_files() -> list[Path]:
    return [
        *sorted((ROOT / "Skydown App").glob("*.lproj/Localizable.strings")),
        *sorted((ROOT / "androidApp/src/main/res").glob("values*/strings.xml")),
    ]


def main() -> int:
    failures: list[str] = []

    for path, parser, expected in EXPECTED_VALUES:
        values = parser(path)
        for key, wanted in expected.items():
            actual = values.get(key)
            if actual != wanted:
                failures.append(f"{path.relative_to(ROOT)}:{key}: expected {wanted!r}, found {actual!r}")

    for path in localization_files():
        text = path.read_text(encoding="utf-8")
        for fragment in FORBIDDEN_FRAGMENTS:
            if fragment in text:
                failures.append(f"{path.relative_to(ROOT)}: forbidden microcopy fragment {fragment!r}")

        parser = parse_ios_strings if path.suffix == ".strings" else parse_android_strings
        values = parser(path)
        for key, value in values.items():
            if value in FORBIDDEN_EXACT_VALUES:
                failures.append(f"{path.relative_to(ROOT)}:{key}: forbidden exact value {value!r}")

    for path in [ROOT / "docs/store/app-store.md", ROOT / "docs/store/google-play.md"]:
        text = path.read_text(encoding="utf-8")
        for fragment in STORE_FORBIDDEN:
            if fragment in text:
                failures.append(f"{path.relative_to(ROOT)}: outdated store naming {fragment!r}")

    if failures:
        print("Brand microcopy audit failed:")
        for failure in failures:
            print(f"- {failure}")
        return 1

    print("Brand microcopy audit passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
