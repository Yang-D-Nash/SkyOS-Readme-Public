#!/usr/bin/env python3
"""
Sync localization key parity (English default fills missing keys).

- iOS: en.lproj/Localizable.strings is canonical; other *.lproj get missing lines appended.
- Android: values/strings.xml is canonical; values-*/strings.xml get missing <string> blocks.

Run: python3 scripts/sync_localizations.py
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

REPO = Path(__file__).resolve().parents[1]


def ios_line_key(line: str) -> str | None:
    """Extract key from a Localizable.strings line (single-line "key" = "value";)."""
    m = re.match(r'^[\s\xEF\xBB\xBF]*"((?:\\.|[^"\\])*)"\s*=\s*"', line)
    return m.group(1) if m else None


def merge_ios() -> None:
    ios = REPO / "Skydown App"
    en_path = ios / "en.lproj" / "Localizable.strings"
    en_lines = en_path.read_text(encoding="utf-8").splitlines(keepends=True)
    en_by_key: dict[str, str] = {}
    for line in en_lines:
        k = ios_line_key(line)
        if k is not None:
            en_by_key[k] = line
    for loc in ["de", "es", "fr", "it", "ja", "nl", "pl", "pt", "tr"]:
        path = ios / f"{loc}.lproj" / "Localizable.strings"
        if not path.is_file():
            continue
        body = path.read_text(encoding="utf-8")
        have = set()
        for line in body.splitlines(keepends=True):
            k = ios_line_key(line)
            if k is not None:
                have.add(k)
        miss = [k for k in en_by_key if k not in have]
        if not miss:
            print(f"iOS {loc}: OK")
            continue
        out = body
        if not out.endswith("\n"):
            out += "\n"
        out += "\n/* sync_localizations: filled from en */\n"
        for k in sorted(miss):
            out += en_by_key[k]
        path.write_text(out, encoding="utf-8", newline="\n")
        print(f"iOS {loc}: +{len(miss)}")


def android_merge_by_snippet() -> None:
    res = REPO / "androidApp" / "src" / "main" / "res"
    base = (res / "values" / "strings.xml").read_text(encoding="utf-8")
    names: dict[str, str] = {}
    for m in re.finditer(
        r'    <string\s+name="([^"]+)"([^>]*)>(.*?)</string>',
        base,
        re.DOTALL,
    ):
        names[m.group(1)] = m.group(0)
    for folder in sorted(res.glob("values-*")):
        p = folder / "strings.xml"
        if not p.is_file():
            continue
        body = p.read_text(encoding="utf-8")
        have = set(re.findall(r'<string\s+name="([^"]+)"', body))
        miss = [n for n in names if n not in have]
        if not miss:
            print(f"Android {folder.name.replace('values-','')}: OK")
            continue
        ins = "\n    <!-- sync_localizations -->\n"
        for n in sorted(miss):
            ins += "    " + names[n].strip() + "\n"
        body = body.rstrip()
        if not body.rstrip().endswith("</resources>"):
            print("bad xml", p, file=sys.stderr)
            continue
        body = body[: -len("</resources>")].rstrip() + "\n" + ins + "\n</resources>\n"
        p.write_text(body, encoding="utf-8", newline="\n")
        print(f"Android {folder.name.replace('values-','')}: +{len(miss)}")


if __name__ == "__main__":
    merge_ios()
    android_merge_by_snippet()
