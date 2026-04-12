#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ios_src="$repo_root/Skydown App"
android_src="$repo_root/androidApp/src/main/java"
android_res="$repo_root/androidApp/src/main/res"

out_file="${1:-}"

emit() {
  printf "%s\n" "$1"
}

count_ios_key_file() {
  local file="$1"
  rg -n '^[[:space:]]*"[^"]+"[[:space:]]*=' "$file" | wc -l | tr -d ' '
}

count_android_key_file() {
  local file="$1"
  rg -n '<string name=|<plurals name=|<string-array name=' "$file" | wc -l | tr -d ' '
}

ios_literal_pattern='Text\("[^"]+"\)|Label\("[^"]+"|Button\("[^"]+"|placeholder:\s*"[^"]+"|title:\s*"[^"]+"'
android_literal_pattern='text\s*=\s*"[^"]+"|Text\("[^"]+"\)|placeholder\s*=\s*\{\s*Text\("[^"]+"\)'

ios_hardcoded_count=$(rg -n "$ios_literal_pattern" "$ios_src" --glob '*.swift' | wc -l | tr -d ' ')
android_hardcoded_count=$(rg -n "$android_literal_pattern" "$android_src" --glob '*.kt' | wc -l | tr -d ' ')

content=""
append() {
  content+="$1"$'\n'
}

append "# Localization Audit"
append ""
append "Generated: $(date '+%Y-%m-%d %H:%M:%S %Z')"
append ""
append "## iOS (Localizable.strings)"
append ""
append "| Locale | Keys |"
append "| --- | ---: |"
while IFS= read -r -d '' f; do
  locale_dir=$(basename "$(dirname "$f")")
  keys=$(count_ios_key_file "$f")
  append "| ${locale_dir} | ${keys} |"
done < <(find "$ios_src" -maxdepth 2 -type f -name 'Localizable.strings' -print0 | sort -z)
append ""
append "Hardcoded UI literal matches in Swift files: **${ios_hardcoded_count}**"
append ""
append "Top Swift files by hardcoded-literal matches:"
append '```text'
append "$(rg -n "$ios_literal_pattern" "$ios_src" --glob '*.swift' | cut -d: -f1 | sort | uniq -c | sort -nr | head -n 20)"
append '```'
append ""
append "## Android (strings.xml)"
append ""
append "| Resource dir | Keys |"
append "| --- | ---: |"
while IFS= read -r -d '' f; do
  dir=$(basename "$(dirname "$f")")
  keys=$(count_android_key_file "$f")
  append "| ${dir} | ${keys} |"
done < <(find "$android_res" -maxdepth 2 -type f -name 'strings.xml' -print0 | sort -z)
append ""
append "Hardcoded UI literal matches in Kotlin UI files: **${android_hardcoded_count}**"
append ""
append "Top Kotlin files by hardcoded-literal matches:"
append '```text'
append "$(rg -n "$android_literal_pattern" "$android_src" --glob '*.kt' | cut -d: -f1 | sort | uniq -c | sort -nr | head -n 20)"
append '```'
append ""
append "## Summary"
append ""
append "- Locale folders are present for 10 languages on iOS and Android."
append "- Full UI localization is **not complete** while hardcoded literals remain in source."
append "- Priority should be top files listed above, then secondary modules."

if [[ -n "$out_file" ]]; then
  printf "%s" "$content" > "$out_file"
fi

printf "%s" "$content"
