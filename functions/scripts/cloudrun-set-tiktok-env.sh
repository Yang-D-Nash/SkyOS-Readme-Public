#!/usr/bin/env bash
# Laeuft auf DEINEM Mac (gcloud), nicht in CI.
# Setzt TikTok OAuth Variablen fuer den skydownAgent Cloud-Run-Service.
#
# Vorher:
#   brew install --cask google-cloud-sdk
#   gcloud auth login
#   gcloud config set project skydown-a6add
#
# Optional: functions/.env.tiktok.local (gitignored), z. B.
#   export TIKTOK_CLIENT_KEY=...
#   export TIKTOK_CLIENT_SECRET=...
#   export TIKTOK_REDIRECT_URI=https://us-central1-skydown-a6add.cloudfunctions.net/tiktokOAuthCallback
#   export TIKTOK_OAUTH_SCOPES=user.info.profile,user.info.stats,video.list

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
    echo "Keine Cloud-Run-Dienste gefunden. gcloud auth login & Projekt pruefen."
    exit 1
  fi
  echo "Welcher Dienst ist der skydownAgent?"
  for i in "${!ALL[@]}"; do
    echo "  $((i+1))) ${ALL[i]}"
  done
  read -r -p "Nummer: " NUM
  SERVICE="${ALL[$((NUM-1))]}"
else
  SERVICE="${SERVICES[0]}"
  if [[ ${#SERVICES[@]} -gt 1 ]]; then
    echo "Mehrere passende Services gefunden:"
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
if [[ -f "${HERE}/../.env.tiktok.local" ]]; then
  echo "Lade Werte aus .env.tiktok.local? [j/N]"
  read -r L
  if [[ "$L" =~ ^[jJ] ]]; then
    # shellcheck source=/dev/null
    set -a
    source "${HERE}/../.env.tiktok.local"
    set +a
  fi
fi

if [[ -z "${TIKTOK_CLIENT_KEY:-}" ]]; then
  read -r -p "TIKTOK_CLIENT_KEY: " TIKTOK_CLIENT_KEY
fi
if [[ -z "${TIKTOK_CLIENT_SECRET:-}" ]]; then
  read -r -s -p "TIKTOK_CLIENT_SECRET: " TIKTOK_CLIENT_SECRET
  echo ""
fi
if [[ -z "${TIKTOK_REDIRECT_URI:-}" ]]; then
  read -r -p "TIKTOK_REDIRECT_URI [default cloudfunctions callback]: " TIKTOK_REDIRECT_URI
fi
if [[ -z "${TIKTOK_OAUTH_SCOPES:-}" ]]; then
  read -r -p "TIKTOK_OAUTH_SCOPES [user.info.profile,user.info.stats,video.list]: " TIKTOK_OAUTH_SCOPES
fi

TIKTOK_REDIRECT_URI="${TIKTOK_REDIRECT_URI:-https://us-central1-skydown-a6add.cloudfunctions.net/tiktokOAuthCallback}"
TIKTOK_OAUTH_SCOPES="${TIKTOK_OAUTH_SCOPES:-user.info.profile,user.info.stats,video.list}"

if [[ -z "${TIKTOK_CLIENT_KEY}" || -z "${TIKTOK_CLIENT_SECRET}" ]]; then
  echo "Fehler: TIKTOK_CLIENT_KEY/SECRET fehlen. Abbruch."
  exit 1
fi

gcloud run services update "${SERVICE}" \
  --region="${REGION}" --project="${PROJECT}" \
  --update-env-vars "^#^TIKTOK_CLIENT_KEY=${TIKTOK_CLIENT_KEY}#TIKTOK_CLIENT_SECRET=${TIKTOK_CLIENT_SECRET}#TIKTOK_REDIRECT_URI=${TIKTOK_REDIRECT_URI}#TIKTOK_OAUTH_SCOPES=${TIKTOK_OAUTH_SCOPES}"

echo "Fertig. Neue Revision ausgerollt."
echo "OAuth-Start:"
echo "  https://us-central1-skydown-a6add.cloudfunctions.net/tiktokAuthStart?uid=<deine_uid>"

unset TIKTOK_CLIENT_SECRET
