#!/usr/bin/env python3
"""
Register an Android App Check *debug secret* (UUID from Logcat) for this repo's default Firebase app.

Requires an OAuth access token with scope `cloud-platform` or `firebase`, either from:

- `gcloud auth print-access-token` (needs `gcloud` on PATH), or
- env `GCLOUD_ACCESS_TOKEN` / `GOOGLE_OAUTH_ACCESS_TOKEN` (e.g. `export GCLOUD_ACCESS_TOKEN="$(gcloud auth print-access-token)"`).

Usage:
  export APP_CHECK_DEBUG_TOKEN='xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'
  ./scripts/register_android_appcheck_debug_token.py

  # or pass the secret as the first argument:
  ./scripts/register_android_appcheck_debug_token.py 'xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx'

Override defaults (see androidApp/google-services.json):
  FIREBASE_PROJECT_NUMBER=1069068117600 \\
  FIREBASE_ANDROID_APP_ID='1:1069068117600:android:c955aeea53265155e84505' \\
  APP_CHECK_DEBUG_TOKEN_NAME='My laptop' \\
  ./scripts/register_android_appcheck_debug_token.py
"""

from __future__ import annotations

import json
import os
import subprocess
import sys
import urllib.error
import urllib.request
import uuid


def _access_token() -> str:
    """OAuth access token: env GCLOUD_ACCESS_TOKEN / GOOGLE_OAUTH_ACCESS_TOKEN, or `gcloud`."""
    for key in ("GCLOUD_ACCESS_TOKEN", "GOOGLE_OAUTH_ACCESS_TOKEN"):
        env_tok = os.environ.get(key, "").strip()
        if env_tok:
            return env_tok
    try:
        out = subprocess.check_output(
            ["gcloud", "auth", "print-access-token"],
            text=True,
            stderr=subprocess.STDOUT,
        )
    except FileNotFoundError as exc:
        raise SystemExit(
            "Kein Access-Token: Entweder `gcloud` installieren und `gcloud auth login`, "
            "oder auf deinem Rechner einmal setzen:\n"
            "  export GCLOUD_ACCESS_TOKEN=\"$(gcloud auth print-access-token)\"\n"
            "  ./scripts/register_android_appcheck_debug_token.py '<debug-uuid>'\n"
        ) from exc
    except subprocess.CalledProcessError as exc:
        raise SystemExit(
            "gcloud auth print-access-token ist fehlgeschlagen. "
            "Bitte `gcloud auth login` (oder passende ADC) setzen.\n" + exc.output
        ) from exc
    token = out.strip()
    if not token:
        raise SystemExit("Leerer Access-Token von gcloud.")
    return token


def main() -> None:
    debug_secret = os.environ.get("APP_CHECK_DEBUG_TOKEN") or (
        sys.argv[1] if len(sys.argv) > 1 else None
    )
    if not debug_secret:
        print(
            "APP_CHECK_DEBUG_TOKEN setzen oder UUID als erstes Argument uebergeben.\n"
            "Logcat: Zeile mit 'Enter this debug secret into the allow list'",
            file=sys.stderr,
        )
        sys.exit(1)

    project_number = os.environ.get("FIREBASE_PROJECT_NUMBER", "1069068117600")
    app_id = os.environ.get(
        "FIREBASE_ANDROID_APP_ID",
        "1:1069068117600:android:c955aeea53265155e84505",
    )
    display_name = os.environ.get("APP_CHECK_DEBUG_TOKEN_NAME", "Android debug device")

    debug_resource_id = str(uuid.uuid4()).lower()
    name = f"projects/{project_number}/apps/{app_id}/debugTokens/{debug_resource_id}"
    parent = f"projects/{project_number}/apps/{app_id}"

    url = f"https://firebaseappcheck.googleapis.com/v1/{parent}/debugTokens"
    body = json.dumps(
        {
            "name": name,
            "displayName": display_name,
            "token": debug_secret,
        }
    ).encode("utf-8")

    req = urllib.request.Request(
        url,
        data=body,
        method="POST",
        headers={
            "Authorization": f"Bearer {_access_token()}",
            "Content-Type": "application/json; charset=utf-8",
        },
    )
    try:
        with urllib.request.urlopen(req) as resp:
            payload = json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        err_body = exc.read().decode("utf-8", errors="replace")
        print(f"HTTP {exc.code}: {err_body}", file=sys.stderr)
        sys.exit(exc.code)

    print(json.dumps(payload, indent=2))
    print("\nOK: Debug-Token ist im Projekt hinterlegt. App neu starten.", file=sys.stderr)


if __name__ == "__main__":
    main()
