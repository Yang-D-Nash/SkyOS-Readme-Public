#!/usr/bin/env python3
from __future__ import annotations

import re
import html
import time
from pathlib import Path

import argparse

from deep_translator import GoogleTranslator, MyMemoryTranslator


ROOT = Path("/Users/nash/StudioProjects/SkyOs-App")

IOS_EN_FILE = ROOT / "Skydown App/en.lproj/Localizable.strings"
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

ANDROID_EN_FILE = ROOT / "androidApp/src/main/res/values/strings.xml"
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

IOS_PAIR_RE = re.compile(r'^"([^"]+)"\s*=\s*"((?:[^"\\]|\\.)*)";\s*$', re.MULTILINE)
ANDROID_STR_RE = re.compile(r'(<string\b[^>]*\bname="([^"]+)"[^>]*>)(.*?)(</string>)', re.DOTALL)
NON_TRANSLATABLE_RE = re.compile(r'<string\b[^>]*\bname="([^"]+)"[^>]*\btranslatable="false"[^>]*>')
PLACEHOLDER_RE = re.compile(r"%\d+\$[sdf]|%[sdf]")


def escape_ios(v: str) -> str:
    return v.replace("\\", "\\\\").replace('"', '\\"')


def normalize_android(v: str) -> str:
    return re.sub(r"\s+", " ", v.strip())


def should_skip_key(key: str) -> bool:
    # Keep technical identifiers untouched.
    return key.startswith("brand.") or key.startswith("app_name")


def protect_placeholders(text: str) -> tuple[str, list[str]]:
    tokens = []
    protected = text
    for i, ph in enumerate(PLACEHOLDER_RE.findall(text)):
        token = f"__PH_{i}__"
        protected = protected.replace(ph, token, 1)
        tokens.append(ph)
    return protected, tokens


def restore_placeholders(text: str, tokens: list[str]) -> str:
    restored = text
    for i, ph in enumerate(tokens):
        restored = restored.replace(f"__PH_{i}__", ph)
    return restored


def translate_text(translator, text: str) -> str:
    protected, tokens = protect_placeholders(text)
    translated = translator.translate(protected)
    translated = restore_placeholders(translated, tokens)
    return translated


def batch_translate(translator, texts: list[str], chunk_size: int = 64) -> list[str]:
    out: list[str] = []
    for i in range(0, len(texts), chunk_size):
        chunk = texts[i : i + chunk_size]
        protected_chunks = []
        token_sets = []
        for item in chunk:
            protected, tokens = protect_placeholders(item)
            protected_chunks.append(protected)
            token_sets.append(tokens)
        translated = translator.translate_batch(protected_chunks)
        if isinstance(translated, str):
            translated = [translated]
        for raw, tokens in zip(translated, token_sets):
            out.append(restore_placeholders(raw, tokens))
        time.sleep(0.15)
    return out


def build_translator(target_locale: str, backend: str):
    if backend == "mymemory":
        target_map = {
            "de": "german",
            "es": "spanish",
            "fr": "french",
            "it": "italian",
            "ja": "japanese",
            "nl": "dutch",
            "pl": "polish",
            "pt": "portuguese",
            "tr": "turkish",
        }
        return MyMemoryTranslator(source="english", target=target_map.get(target_locale, target_locale))
    return GoogleTranslator(source="en", target=target_locale)


def translate_ios(locales: set[str] | None = None, max_per_locale: int | None = None, backend: str = "google") -> int:
    en_pairs = dict(IOS_PAIR_RE.findall(IOS_EN_FILE.read_text(encoding="utf-8")))
    total = 0
    for locale, path in IOS_LOCALES.items():
        if locales and locale not in locales:
            continue
        translator = build_translator(locale, backend)
        text = path.read_text(encoding="utf-8")
        locale_pairs = dict(IOS_PAIR_RE.findall(text))
        changed = False
        keys_to_translate: list[str] = []
        src_texts: list[str] = []
        for key, en_val in en_pairs.items():
            if should_skip_key(key):
                continue
            local_val = locale_pairs.get(key, "")
            if local_val != en_val:
                continue
            if not en_val.strip():
                continue
            keys_to_translate.append(key)
            src_texts.append(en_val)
            if max_per_locale and len(keys_to_translate) >= max_per_locale:
                break

        if not keys_to_translate:
            continue

        try:
            translated_values = batch_translate(translator, src_texts, chunk_size=48)
        except Exception:
            time.sleep(1.0)
            translated_values = batch_translate(translator, src_texts, chunk_size=24)

        for key, translated in zip(keys_to_translate, translated_values):
            local_val = locale_pairs.get(key, "")
            if translated and translated != local_val:
                pattern = re.compile(rf'^{re.escape(f"\"{key}\" = \"{local_val}\";")}$', re.MULTILINE)
                replacement = f"\"{key}\" = \"{escape_ios(translated)}\";"
                text = pattern.sub(replacement, text, count=1)
                locale_pairs[key] = translated
                total += 1
                changed = True
        if changed:
            path.write_text(text, encoding="utf-8")
    return total


def translate_android(locales: set[str] | None = None, max_per_locale: int | None = None, backend: str = "google") -> int:
    en_text = ANDROID_EN_FILE.read_text(encoding="utf-8")
    base_map = {name: body for _, name, body, _ in ANDROID_STR_RE.findall(en_text)}
    total = 0

    for locale, path in ANDROID_LOCALES.items():
        if locales and locale not in locales:
            continue
        translator = build_translator(locale, backend)
        text = path.read_text(encoding="utf-8")
        non_translatable = set(NON_TRANSLATABLE_RE.findall(text))
        changed = False
        keys_to_translate: list[str] = []
        src_texts: list[str] = []

        entries = list(ANDROID_STR_RE.finditer(text))
        locale_values = {m.group(2): m.group(3) for m in entries}
        for name, body in locale_values.items():
            if name in non_translatable or should_skip_key(name):
                continue
            en_body = base_map.get(name)
            if en_body is None:
                continue
            if normalize_android(body) != normalize_android(en_body):
                continue
            if not normalize_android(en_body):
                continue
            keys_to_translate.append(name)
            src_texts.append(normalize_android(en_body))
            if max_per_locale and len(keys_to_translate) >= max_per_locale:
                break

        translated_map: dict[str, str] = {}
        if keys_to_translate:
            try:
                translated_values = batch_translate(translator, src_texts, chunk_size=48)
            except Exception:
                time.sleep(1.0)
                translated_values = batch_translate(translator, src_texts, chunk_size=24)
            translated_map = {k: v for k, v in zip(keys_to_translate, translated_values)}

        def replace_one(match: re.Match[str]) -> str:
            nonlocal total, changed
            open_tag, name, body, close_tag = match.groups()
            if name in non_translatable or should_skip_key(name):
                return match.group(0)
            en_body = base_map.get(name)
            if en_body is None:
                return match.group(0)
            if normalize_android(body) != normalize_android(en_body):
                return match.group(0)
            if not normalize_android(en_body):
                return match.group(0)
            translated = translated_map.get(name, body)
            if translated and normalize_android(translated) != normalize_android(body):
                changed = True
                total += 1
                return f"{open_tag}{html.escape(translated, quote=False)}{close_tag}"
            return match.group(0)

        text = ANDROID_STR_RE.sub(replace_one, text)
        if changed:
            path.write_text(text, encoding="utf-8")
    return total


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--platform", choices=["ios", "android", "both"], default="both")
    parser.add_argument("--locales", default="")
    parser.add_argument("--max-per-locale", type=int, default=0)
    parser.add_argument("--backend", choices=["google", "mymemory"], default="google")
    args = parser.parse_args()

    locales = {part.strip() for part in args.locales.split(",") if part.strip()} or None
    max_per_locale = args.max_per_locale if args.max_per_locale > 0 else None

    ios_count = 0
    android_count = 0
    if args.platform in {"ios", "both"}:
        ios_count = translate_ios(locales=locales, max_per_locale=max_per_locale, backend=args.backend)
    if args.platform in {"android", "both"}:
        android_count = translate_android(locales=locales, max_per_locale=max_per_locale, backend=args.backend)
    print(f"Bulk translation completed. iOS updated: {ios_count}, Android updated: {android_count}.")


if __name__ == "__main__":
    main()
