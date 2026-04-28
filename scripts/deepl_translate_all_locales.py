#!/usr/bin/env python3
from __future__ import annotations

import os
import re
import argparse
from pathlib import Path

import deepl


ROOT = Path("/Users/nash/StudioProjects/SkyOs-App")

IOS_EN = ROOT / "Skydown App/en.lproj/Localizable.strings"
IOS_LOCALES = {
    "de": ROOT / "Skydown App/de.lproj/Localizable.strings",
    "es": ROOT / "Skydown App/es.lproj/Localizable.strings",
    "fr": ROOT / "Skydown App/fr.lproj/Localizable.strings",
    "it": ROOT / "Skydown App/it.lproj/Localizable.strings",
    "ja": ROOT / "Skydown App/ja.lproj/Localizable.strings",
    "nl": ROOT / "Skydown App/nl.lproj/Localizable.strings",
    "pl": ROOT / "Skydown App/pl.lproj/Localizable.strings",
    "pt": ROOT / "Skydown App/pt.lproj/Localizable.strings",
    "tr": ROOT / "Skydown App/tr.lproj/Localizable.strings",
}

ANDROID_EN = ROOT / "androidApp/src/main/res/values/strings.xml"
ANDROID_LOCALES = {
    "de": ROOT / "androidApp/src/main/res/values-de/strings.xml",
    "es": ROOT / "androidApp/src/main/res/values-es/strings.xml",
    "fr": ROOT / "androidApp/src/main/res/values-fr/strings.xml",
    "it": ROOT / "androidApp/src/main/res/values-it/strings.xml",
    "ja": ROOT / "androidApp/src/main/res/values-ja/strings.xml",
    "nl": ROOT / "androidApp/src/main/res/values-nl/strings.xml",
    "pl": ROOT / "androidApp/src/main/res/values-pl/strings.xml",
    "pt": ROOT / "androidApp/src/main/res/values-pt/strings.xml",
    "tr": ROOT / "androidApp/src/main/res/values-tr/strings.xml",
}

TARGET_LANG = {
    "de": "DE",
    "es": "ES",
    "fr": "FR",
    "it": "IT",
    "ja": "JA",
    "nl": "NL",
    "pl": "PL",
    "pt": "PT-PT",
    "tr": "TR",
}

IOS_RE = re.compile(r'^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$', re.MULTILINE)
ANDROID_RE = re.compile(r'(<string\b[^>]*name="([^"]+)"[^>]*>)(.*?)(</string>)', re.DOTALL)
ANDROID_NON_TRANS_RE = re.compile(r'<string\b[^>]*name="([^"]+)"[^>]*translatable="false"[^>]*>')
PLACEHOLDER_RE = re.compile(r"%\d+\$[sdf]|%[sdf]")


def normalize(s: str) -> str:
    return re.sub(r"\s+", " ", s.strip())


def should_skip_key(key: str) -> bool:
    key_lower = key.lower()
    if key_lower in {"app_name"}:
        return True
    if key_lower.startswith("attribution_"):
        return True
    if key_lower.startswith("brand.") or key_lower.startswith("brand_"):
        return True
    if key_lower.endswith("_sample"):
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


def translate_batch(translator: deepl.Translator, target_lang: str, texts: list[str], chunk_size: int = 80) -> list[str]:
    if not texts:
        return []
    out = []
    for i in range(0, len(texts), chunk_size):
        batch = texts[i : i + chunk_size]
        protected = []
        token_sets = []
        for text in batch:
            p, tokens = protect_placeholders(text)
            protected.append(p)
            token_sets.append(tokens)
        results = translator.translate_text(
            protected,
            source_lang="EN",
            target_lang=target_lang,
            preserve_formatting=True,
        )
        if not isinstance(results, list):
            results = [results]
        for result, tokens in zip(results, token_sets):
            out.append(restore_placeholders(result.text, tokens))
    return out


def update_ios_locale(translator: deepl.Translator, locale: str) -> int:
    en_map = dict(IOS_RE.findall(IOS_EN.read_text(encoding="utf-8")))
    locale_path = IOS_LOCALES[locale]
    locale_text = locale_path.read_text(encoding="utf-8")
    locale_map = dict(IOS_RE.findall(locale_text))

    keys = []
    src = []
    for key, value in en_map.items():
        if should_skip_key(key):
            continue
        if locale_map.get(key) != value:
            continue
        if not value.strip():
            continue
        keys.append(key)
        src.append(value)

    if not keys:
        return 0

    translated = translate_batch(translator, TARGET_LANG[locale], src)
    changed = 0
    for key, old, new in zip(keys, src, translated):
        if normalize(old) == normalize(new):
            continue
        pattern = re.compile(rf'("{re.escape(key)}"\s*=\s*")((?:[^"\\]|\\.)*)(";)')
        escaped = new.replace("\\", "\\\\").replace('"', '\\"')
        locale_text, count = pattern.subn(lambda m: f"{m.group(1)}{escaped}{m.group(3)}", locale_text, count=1)
        if count:
            changed += 1

    locale_path.write_text(locale_text, encoding="utf-8")
    return changed


def update_android_locale(translator: deepl.Translator, locale: str) -> int:
    en_map = {name: body for _, name, body, _ in ANDROID_RE.findall(ANDROID_EN.read_text(encoding="utf-8"))}
    locale_path = ANDROID_LOCALES[locale]
    locale_text = locale_path.read_text(encoding="utf-8")
    non_trans = set(ANDROID_NON_TRANS_RE.findall(locale_text))

    keys = []
    src = []
    for _, key, body, _ in ANDROID_RE.findall(locale_text):
        en_body = en_map.get(key)
        if en_body is None:
            continue
        if key in non_trans or should_skip_key(key):
            continue
        if normalize(body) != normalize(en_body):
            continue
        if not normalize(en_body):
            continue
        keys.append(key)
        src.append(normalize(en_body))

    if not keys:
        return 0

    translated = translate_batch(translator, TARGET_LANG[locale], src)
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

    locale_text = ANDROID_RE.sub(repl, locale_text)
    locale_path.write_text(locale_text, encoding="utf-8")
    return changed


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--locales", default="de,es,fr,it,ja,nl,pl,pt,tr")
    args = parser.parse_args()

    key = os.environ.get("DEEPL_API_KEY", "").strip()
    if not key:
        raise SystemExit("DEEPL_API_KEY is required")

    translator = deepl.Translator(key)
    total_ios = 0
    total_android = 0

    locales = [part.strip() for part in args.locales.split(",") if part.strip()]
    for locale in locales:
        if locale not in IOS_LOCALES or locale not in ANDROID_LOCALES:
            print(f"{locale}: skipped (unsupported locale)")
            continue
        try:
            ios_changed = update_ios_locale(translator, locale)
            android_changed = update_android_locale(translator, locale)
        except Exception as exc:
            print(f"{locale}: failed ({exc})")
            continue
        total_ios += ios_changed
        total_android += android_changed
        print(f"{locale}: iOS changed={ios_changed}, Android changed={android_changed}")

    print(f"DeepL all-locales pass complete. iOS total={total_ios}, Android total={total_android}")


if __name__ == "__main__":
    main()
