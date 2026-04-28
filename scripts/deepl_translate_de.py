#!/usr/bin/env python3
from __future__ import annotations

import os
import re
from pathlib import Path

import deepl


ROOT = Path("/Users/nash/StudioProjects/SkyOs-App")
IOS_EN = ROOT / "Skydown App/en.lproj/Localizable.strings"
IOS_DE = ROOT / "Skydown App/de.lproj/Localizable.strings"
ANDROID_EN = ROOT / "androidApp/src/main/res/values/strings.xml"
ANDROID_DE = ROOT / "androidApp/src/main/res/values-de/strings.xml"

IOS_RE = re.compile(r'^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$', re.MULTILINE)
ANDROID_RE = re.compile(r'(<string\b[^>]*name="([^"]+)"[^>]*>)(.*?)(</string>)', re.DOTALL)
ANDROID_NON_TRANS_RE = re.compile(r'<string\b[^>]*name="([^"]+)"[^>]*translatable="false"[^>]*>')
PLACEHOLDER_RE = re.compile(r"%\d+\$[sdf]|%[sdf]")


def normalize(s: str) -> str:
    return re.sub(r"\s+", " ", s.strip())


def should_skip_key(key: str) -> bool:
    key_lower = key.lower()
    if key_lower.startswith("brand.") or key_lower == "app_name":
        return True
    if key_lower.endswith("_sample") or key_lower.endswith("_placeholder"):
        return True
    if key_lower in {"settings_automation_activepieces", "settings_automation_n8n"}:
        return True
    return False


def protect_placeholders(text: str) -> tuple[str, list[str]]:
    placeholders = []
    protected = text
    for idx, match in enumerate(PLACEHOLDER_RE.findall(text)):
        token = f"__PH_{idx}__"
        protected = protected.replace(match, token, 1)
        placeholders.append(match)
    return protected, placeholders


def restore_placeholders(text: str, placeholders: list[str]) -> str:
    out = text
    for idx, ph in enumerate(placeholders):
        out = out.replace(f"__PH_{idx}__", ph)
    return out


def translate_batch(translator: deepl.Translator, lines: list[str]) -> list[str]:
    if not lines:
        return []
    protected = []
    placeholder_sets = []
    for line in lines:
        p, s = protect_placeholders(line)
        protected.append(p)
        placeholder_sets.append(s)

    results = translator.translate_text(
        protected,
        source_lang="EN",
        target_lang="DE",
        preserve_formatting=True,
    )
    if not isinstance(results, list):
        results = [results]
    out = []
    for result, placeholders in zip(results, placeholder_sets):
        out.append(restore_placeholders(result.text, placeholders))
    return out


def update_ios(translator: deepl.Translator) -> int:
    en_map = dict(IOS_RE.findall(IOS_EN.read_text(encoding="utf-8")))
    de_text = IOS_DE.read_text(encoding="utf-8")
    de_map = dict(IOS_RE.findall(de_text))

    keys = []
    source = []
    for key, value in en_map.items():
        if should_skip_key(key):
            continue
        if de_map.get(key) != value:
            continue
        if not value.strip():
            continue
        keys.append(key)
        source.append(value)

    translated = translate_batch(translator, source)
    changed = 0
    for key, old_value, new_value in zip(keys, source, translated):
        if normalize(new_value) == normalize(old_value):
            continue
        pattern = re.compile(rf'("{re.escape(key)}"\s*=\s*")((?:[^"\\]|\\.)*)(";)')
        escaped = new_value.replace("\\", "\\\\").replace('"', '\\"')
        de_text, count = pattern.subn(rf'\1{escaped}\3', de_text, count=1)
        if count:
            changed += 1

    IOS_DE.write_text(de_text, encoding="utf-8")
    return changed


def update_android(translator: deepl.Translator) -> int:
    en_text = ANDROID_EN.read_text(encoding="utf-8")
    de_text = ANDROID_DE.read_text(encoding="utf-8")
    en_map = {name: body for _, name, body, _ in ANDROID_RE.findall(en_text)}
    non_trans_keys = set(ANDROID_NON_TRANS_RE.findall(de_text))

    de_entries = list(ANDROID_RE.finditer(de_text))
    keys = []
    source = []
    for m in de_entries:
        _, key, body, _ = m.groups()
        en_body = en_map.get(key)
        if en_body is None or key in non_trans_keys or should_skip_key(key):
            continue
        if normalize(body) != normalize(en_body):
            continue
        if not normalize(en_body):
            continue
        keys.append(key)
        source.append(normalize(en_body))

    translated = translate_batch(translator, source)
    translated_map = {k: v for k, v in zip(keys, translated)}
    changed = 0

    def repl(match: re.Match[str]) -> str:
        nonlocal changed
        open_tag, key, body, close_tag = match.groups()
        new_value = translated_map.get(key)
        if not new_value:
            return match.group(0)
        if normalize(new_value) == normalize(body):
            return match.group(0)
        changed += 1
        return f"{open_tag}{new_value}{close_tag}"

    de_text = ANDROID_RE.sub(repl, de_text)
    ANDROID_DE.write_text(de_text, encoding="utf-8")
    return changed


def main() -> None:
    key = os.environ.get("DEEPL_API_KEY", "").strip()
    if not key:
        raise SystemExit("DEEPL_API_KEY is required")
    translator = deepl.Translator(key)
    ios_changed = update_ios(translator)
    android_changed = update_android(translator)
    print(f"DeepL DE pass complete. iOS changed: {ios_changed}, Android changed: {android_changed}.")


if __name__ == "__main__":
    main()
