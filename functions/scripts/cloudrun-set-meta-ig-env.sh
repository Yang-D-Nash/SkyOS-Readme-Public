#!/usr/bin/env bash
# Laeuft auf DEINEM Mac (gcloud), nicht in CI. Tragt Meta-IG-Werte in Cloud Run fuer skydownAgent ein.
# Vorher: brew install --cask google-cloud-sdk
#   gcloud auth login
#   gcloud config set project skydown-a6add
#
# Token/IDs NIE in Git. Optional: functions/.env.meta.local (gitignored) mit nur
#   export META_IG_USER_ID=12345
#   export META_GRAPH_API_VERSION=v25.0
# (ohne Token in der Datei)

set -euo pipefail
PROJECT="${GOOGLE_CLOUD_PROJECT:-skydown-a6add}"
REGION="us-central1"

if ! command -v gcloud &>/dev/null; then
  echo "gcloud fehlt. Einmalig installieren:"
  echo "  brew install --cask google-cloud-sdk"
  echo "Dann: gcloud auth login && gcloud config set project ${PROJECT}"
  exit 1
fi

echo "Projekt: ${PROJECT}  Region: ${REGION}"
gcloud config set project "${PROJECT}" 2>/dev/null || true

# Kein mapfile: macOS liefert oft Bash 3.2 (ohne mapfile).
SERVICES=()
while IFS= read -r _line; do
  [[ -n "${_line}" ]] && SERVICES+=("${_line}")
done < <(gcloud run services list --region="${REGION}" --format="value(metadata.name)" 2>/dev/null | grep -Ei 'skydown.*agent' || true)
if [[ ${#SERVICES[@]} -eq 0 ]]; then
  ALL=()
  while IFS= read -r _line; do
    [[ -n "${_line}" ]] && ALL+=("${_line}")
  done < <(gcloud run services list --region="${REGION}" --format="value(metadata.name)" 2>/dev/null)
  if [[ ${#ALL[@]} -eq 0 ]]; then
    echo "Keine Cloud-Run-Dienste. gcloud auth login & Projekt pruefen."
    exit 1
  fi
  echo "Welcher Dienst ist der Callable skydownAgent? (suche nach aehnlichem Namen in der Cloud-Konsole)"
  for i in "${!ALL[@]}"; do
    echo "  $((i+1))) ${ALL[i]}"
  done
  read -r -p "Nummer: " NUM
  SERVICE="${ALL[$((NUM-1))]}"
else
  SERVICE="${SERVICES[0]}"
  if [[ ${#SERVICES[@]} -gt 1 ]]; then
    for i in "${!SERVICES[@]}"; do
      echo "  $((i+1))) ${SERVICES[i]}"
    done
    read -r -p "Nummer (Enter=1): " NUM
    NUM=${NUM:-1}
    SERVICE="${SERVICES[$((NUM-1))]}"
  fi
fi
echo "Dienst: ${SERVICE}"

HERE="$(cd "$(dirname "$0")" && pwd)"
if [[ -f "${HERE}/../.env.meta.local" ]]; then
  echo "Lade ID/Version aus .env.meta.local? [j/N]"
  read -r L
  if [[ "$L" =~ ^[jJ] ]]; then
    # shellcheck source=/dev/null
    set -a
    source "${HERE}/../.env.meta.local"
    set +a
  fi
fi

if [[ -z "${META_IG_USER_ID:-}" ]]; then
  read -r -p "META_IG_USER_ID (Instagram Business id, Zahl): " META_IG_USER_ID
fi
if [[ -z "${META_IG_USER_ACCESS_TOKEN:-}" ]]; then
  read -r -s -p "META_IG_USER_ACCESS_TOKEN: "
  echo ""
  META_IG_USER_ACCESS_TOKEN="${REPLY:-}"
fi
GRAPH_VERSION="${META_GRAPH_API_VERSION:-v25.0}"

if [[ -z "${META_IG_USER_ID}" ]]; then
  echo "Fehler: META_IG_USER_ID fehlt. Abbruch ohne Deployment."
  exit 1
fi

if [[ -z "${META_IG_USER_ACCESS_TOKEN}" ]]; then
  echo "Fehler: META_IG_USER_ACCESS_TOKEN fehlt. Abbruch ohne Deployment."
  echo "Hinweis: Ohne Token liefert Social Analysis nur Handle-basierten Fallback."
  exit 1
fi

# Eine einzige Service-Revision, robust fuer Sonderzeichen via benutzerdefiniertem Delimiter.
gcloud run services update "${SERVICE}" \
  --region="${REGION}" --project="${PROJECT}" \
  --update-env-vars "^#^META_IG_USER_ID=${META_IG_USER_ID}#META_GRAPH_API_VERSION=${GRAPH_VERSION}#META_IG_USER_ACCESS_TOKEN=${META_IG_USER_ACCESS_TOKEN}"

echo "Fertig. Cloud Run startet eine neue Revision; 1–2 Min warten, dann App testen."
echo "Hinweis: Token erscheint in Cloud-Run-Env-Variablen (einsehbar mit Projektzugriff). Fuer Produkion spaeter: Secret Manager."

unset META_IG_USER_ACCESS_TOKEN
unset REPLY
