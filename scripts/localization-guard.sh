#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

python3 - <<'PY'
from pathlib import Path
import re
import sys

root = Path("""/Users/nash/StudioProjects/SkyOs-App""")
swift_file = root / "Skydown App/Views/Settings/SettingsView.swift"
kotlin_file = root / "androidApp/src/main/java/com/nash/skyos/ui/screen/SettingsScreen.kt"

swift_patterns = [
    re.compile(r'SettingsSectionCard\(title: "[A-Za-z]'),
    re.compile(r'Text\("[A-Za-z]'),
    re.compile(r'Label\("[A-Za-z]'),
    re.compile(r'Button\("[A-Za-z]'),
]
kotlin_pattern = re.compile(r'Text\("[A-Za-z][^"]*"\)')

def check_file(path: Path, patterns: list[re.Pattern], label: str) -> list[str]:
    failures = []
    for idx, line in enumerate(path.read_text(encoding="utf-8").splitlines(), start=1):
        if any(pattern.search(line) for pattern in patterns):
            failures.append(f"{path}:{idx}:{line.strip()}")
    if failures:
        print(f"Localization guard failed for {label}:")
        for item in failures:
            print(item)
    return failures

swift_failures = check_file(swift_file, swift_patterns, "Swift settings")
kotlin_failures = check_file(kotlin_file, [kotlin_pattern], "Kotlin settings")

if swift_failures or kotlin_failures:
    sys.exit(1)

print("Localization guard passed.")
PY
