# SkyOS — App-Release-Workflow (pflegen)

**Zweck:** Diese Datei ist die **operative Reihenfolge** für einen **App-Store-Release** (iOS / Android) im SkyOS-Repository. Sie ergänzt — und **ersetzt nicht** — die allgemeine [Release-Checkliste](../release-checklist.md), das [Store-Upload-Runbook](store-upload-runbook.md) und [manual-test-checklist.md](../../manual-test-checklist.md).

**Pflege:** Bei jedem Prozess- oder Toolwechsel **hier zuerst** anpassen, dann ggf. README und Runbook spiegeln.

**Verantwortung:** Technische Schritte unten; **rechts- und storewirksame** Entscheidungen (Listings, Preise, Texte) liegen beim **Produktverantwortlichen** — siehe Runbook und Legal-Dokus.

---

## 0. Scope und Go/No-Go

- [ ] **Release-Ziel** klar: nur iOS, nur Android, oder beide.
- [ ] **Backend-Änderung** im Umfang: wenn **nur** Clients: Phasen zu Functions/Rules optional streichen.
- [ ] **Blocker** aus [store-upload-runbook.md](store-upload-runbook.md) (APIs, Upload, Konsolen) sind bekannt und adressierbar.
- [ ] **Version / Build-Nummern** geplant (siehe Runbook „Build Identity“; iOS `CURRENT_PROJECT_VERSION`, Android `versionCode` in `androidApp/build.gradle.kts`).

---

## 1. Repository-Hygiene

- [ ] `git status` sauber oder bewusst begrenzter WIP.
- [ ] Keine Secrets, Keystores, Dumps, große Log-Exports im Commit.
- [ ] Release-Notizen / interne Zusammenfassung vorbereitet (für Store / intern).

---

## 2. Monorepo-Quality-Gate (Pflicht)

Im **Repository-Root:**

```bash
./scripts/ci_local_gate.sh
```

Enthält u. a. `:shared:allTests`, Android `lintDebug` + `detektAll`, KMP-Metadaten, `npm ci` + `npm run build` + `npm test` in `functions/` (inkl. Firestore/Storage-Emulator-Tests).

- **Nur schnell ein Bereich?** `--shared-only`, `--android-only`, `--functions-only` (siehe [scripts/ci_local_gate.sh](../../scripts/ci_local_gate.sh)).

- [ ] Gate **grün** (oder dokumentierter, bewusster Ausschluss — kein stilles Ignorieren).

**iOS-Zusatz (lokal, wie CI):**

```bash
xcodebuild -project "Skydown App.xcodeproj" -scheme "Skydown App" \
  -destination "generic/platform=iOS Simulator" -configuration Debug \
  -sdk iphonesimulator -quiet CODE_SIGNING_ALLOWED=NO build
```

- [ ] **Kompilierung** für Simulator-Destination erfolgreich (entspricht grob dem `ios-build`-Job in [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml)).

---

## 3. Android: Release-Artefakte (Store)

**Sauberer Release-Build und Verifikation** (strenger Gate in einem Rutsch):

```bash
./scripts/android_release_gate.sh
```

Das Skript führt [android_release_clean_build.sh](../../scripts/android_release_clean_build.sh) und [verify_android_release_artifacts.sh](../../scripts/verify_android_release_artifacts.sh) aus.

- [ ] **RELEASE GATE PASSED** in der Konsole; ausgegebene AAB/APK-Hashes/Versionen notieren.
- [ ] Ergebnisse mit [store-upload-runbook.md](store-upload-runbook.md) (Build Identity, Hashes) abgleichen bzw. dort eintragen.

**Optional** (nur Nicht-Store-Tests, nicht für Play-Einspielung): Debug-Signing für Release-Build siehe [README.md](../../README.md) (`allowDebugReleaseSigning`).

---

## 4. iOS: Archiv & Store-Upload (manuell in Xcode / CLI)

- [ ] **Version** und **Build** in Xcode / `SkydownApp-Info.plist` / Projekteinstellungen mit Runbook abgestimmt.
- [ ] **Signing** & Provisioning (Distribution) korrekt.
- [ ] **Archive** erzeugt, **Upload** zu App Store Connect (oder `xcodebuild` mit Export, je nach eurem etablierten Weg — Details [docs/ios.md](../ios.md) und Runbook).
- [ ] **Runbook** „Upload Status“ / Blocker (redundantes Binary, dSYMs, …) aktualisieren.

---

## 5. Firebase Backend (nur bei Backend-Scope)

Nur ausführen, wenn **Functions** und/oder **Regeln** geändert wurden oder deployt werden müssen:

```bash
npm ci --prefix functions
npm run build --prefix functions
npm test --prefix functions
```

Dann deployen (siehe [docs/deployment.md](../deployment.md)):

```bash
firebase deploy --only functions
# ggf. gebündelt:
# firebase deploy --only firestore:rules,storage
```

- [ ] **Secrets** in Firebase/Secret Manager für den Ziel-Stand gesetzt (siehe [`.env.example`](../../.env.example) und [README — Environment](../../README.md)).
- [ ] Nach Deploy: kurze **Rauchprüfung** (z. B. ein Callable, eine kritische Regel) — im Runbook oder intern festhalten.

---

## 6. Store-Konsolen, Listings, Rechtstexte

- [ ] **App Store Connect** / **Play Console**: Metadaten, Screenshots, URLs — mit [store-upload-runbook.md](store-upload-runbook.md) und [docs/store/](../store/README.md).
- [ ] **Öffentliche URLs** (Privacy, Terms, Support) zeigen auf die **produktiven** Seiten ([site/](../../site/) Hostings) — siehe [release-checklist.md](../release-checklist.md) Abschnitt Settings/Legal.
- [ ] **Support-Kontakt** in App, Store und `site/support` konsistent.

---

## 7. Manuelle Smokes (Gerät)

- [ ] [manual-test-checklist.md](../../manual-test-checklist.md) (Matrix Plattform / Rolle) **für die hochgeladenen** Build-Nummern.

---

## 8. Abschluss

- [ ] [store-upload-runbook.md](store-upload-runbook.md) mit finalem **Build Identity**, **Upload-Status**, **Hashes** und offenen Follow-ups aktualisieren.
- [ ] [release-checklist.md](../release-checklist.md) durchgehen (Punkte, die für diesen RC relevant sind).
- [ ] Tag / interne Release-Notiz (siehe [README — Changelog](../../README.md)) — wie im Team vereinbart.

---

## Referenz: Ein-Blick-Befehle

| Aktion | Befehl / Datei |
| --- | --- |
| Volles lokales Gate | `./scripts/ci_local_gate.sh` |
| Android Store-Gate | `./scripts/android_release_gate.sh` |
| iOS CI-ähnlicher Compile-Check | `xcodebuild` (siehe Abschnitt 2) |
| Functions + Rules testen | `cd functions && npm test` |
| Doku-Index Release | [docs/README.md](../README.md) „Current Release Entry“ |

---

*Letzte inhaltliche Ausrichtung: technischer App-Release-Pfad im Monorepo. Store-spezifische Sonderthemen: Runbook und Store-Dokus.*
