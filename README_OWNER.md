# 22xSky Owner Guide

Dieses Handbuch ist fuer den Betrieb des Produkts aus Owner-Sicht.
Es beschreibt die taegliche Steuerung von Rollen, KI, Workflows, Commerce, Sicherheit und Release-Freigaben.

---

## 1) Zielbild Owner

Als Owner steuerst du:

- Rollen und Berechtigungen
- KI-Freigaben und Tageslimits
- globale KI-Systemanweisungen
- Agent-Provider und Runtime-Flags
- Shopify/Payments/Commerce
- Runtime-Lockdown und Recovery

Kurz: Du verantwortest sowohl Produktqualitaet als auch Risiko- und Kostenkontrolle.

Rechtlich Verantwortlicher und Rechteinhaber:

- `Ngoc Anh Nguyen (Yang D. Nash - Skydown)`
- `Erich-Plate-Weg 44, 22419 Hamburg, Deutschland`

---

## 2) Rollensteuerung

Fester Owner-Account:

- `nash.lioncorna@gmail.com`

Rollen im System:

- `owner`
- `admin`
- `subadmin`
- `user`
- `gast` (ohne Login)

Wichtige Regeln:

- Rollenwechsel sind Owner-only und laufen serverseitig.
- Teamrechte werden gezielt vergeben, nicht pauschal.
- Neue Auth-Konten bekommen beim Onboarding ein Bootstrap-User-Dokument, damit sie sauber im User-Management erscheinen.

Empfohlener Ablauf:

1. `Settings > Owner > Users` oeffnen.
2. Zielkonto waehlen.
3. Rolle, Quota-Plan und Funktionsrechte setzen.
4. Speichern und anschliessend als Testkonto verifizieren.

---

## 3) KI Governance (Bot, Visuals, Agent)

### 3.1 Pro Konto steuerbar

- KI an/aus
- Tageslimits fuer:
  - Text/Bot
  - Visuals
  - Agent
- History-Retention

### 3.2 Global steuerbar (Owner)

- Bot-Systemanweisung
- Visual-Systemanweisung
- Agent-Systemanweisung
- Asset-/Knowledge-Hinweise fuer die globalen Prompts

### 3.3 Runtime-Steuerung (Owner)

- Agent Provider (`Gemini` oder `Manus`)
- Fallback Provider
- Manus Runtime:
  - Timeouts
  - Polling
  - Prompt/History-Limits
  - Auto-Stop/Cost-Guard-Optionen
- Hard Daily Caps
- Global Daily Caps

Empfehlung:

1. Erst konservative Caps setzen.
2. 3-7 Tage echte Nutzung beobachten.
3. Danach pro Segment (Admin, Creator, User) graduell anheben.

---

## 4) Workflow Automation (n8n) Governance

22xSky nutzt pro User getrennte Workflow-Konfigurationen:

- `adminConfig/automationN8n_<uid>`

Optionales persoenliches Agent-Profil:

- `adminConfig/agentProfile_<uid>`

Wichtige Konsequenz:

- Jeder Account kann einen eigenen Workflow-Service nutzen.
- Es gibt kein globales Cross-User-Mischen im BYOS-Strict-Modus.

Webhook-Trigger-Flow:

1. User/Agent sendet Trigger an Cloud Function.
2. Backend laedt nur die eigene `automationN8n_<uid>`.
3. Backend postet Payload an den konfigurierten Webhook.
4. Antwort wird als Erfolg/Fehler an die App zurueckgegeben.

---

## 5) Commerce, Shopify und Payments

Owner-only Bereiche:

- Shopify Store Domain/Token/Collection
- Payment-Methoden
- Commerce-Basiseinstellungen
- Stripe Backend Secrets

Betriebshinweise:

- Stripe ist der sichere Live-Checkout-Pfad.
- Klarna laeuft ueber Stripe (wenn im Stripe-Account freigeschaltet).
- PayPal/Bank koennen als manueller Owner-Flow gefuehrt werden.

Release vor Commerce-Freigabe:

1. Produkt-Sync pruefen.
2. Checkout-Ende-zu-Ende testen.
3. Payment-Statuswechsel im Order-Flow validieren.

---

## 6) Sicherheit und Kostenkontrolle

Technische Leitplanken:

- Firestore/Storage mit `deny by default`
- Rollen- und Claim-basierte Freigaben
- Upload-Slot-Mechanik statt offener Uploads
- serverseitige KI-Limits
- Runtime-Lockdown
- Budget-Lockdown Hook

Runtime-Dokument:

- `system/runtimeConfig`

Kritische Felder:

- `lockdown`
- `uploadsEnabled`
- `registrationsEnabled`
- `userWritesEnabled`
- `budgetLockdownEnabled`

---

## 7) Release Gate (Owner Checkliste)

Vor jeder externen Verteilung:

1. Android Build (Debug + Release) gruen.
2. iOS Build/Archive gruen.
3. Functions Syntax und Rule-Tests gruen.
4. Registrierung/Login mit frischem Konto pruefen.
5. Rollenwechsel Owner -> Zielrolle pruefen.
6. Bot/Visual/Agent (inkl. Limit-Fehlern) pruefen.
7. n8n Test pro Testkonto pruefen.
8. Merch/Checkout/Payment Ruecklaeufe pruefen.
9. Offline, Notifications und wichtige UX-Feedbacks pruefen.

Nur wenn alles gruen ist, Release weitergeben.

---

## 8) Incident Playbook (Kurz)

Wenn etwas kippt:

1. `Runtime Lockdown` aktivieren.
2. Betroffene Funktionen isolieren (z. B. KI/Automation/Uploads).
3. Logs und Fehlerbild dokumentieren.
4. Fix in Staging testen.
5. Lockdown kontrolliert zuruecknehmen.

---

## 9) Technische Kurzbefehle

iOS Build:

```bash
xcodebuild -project 'Skydown App.xcodeproj' -scheme 'Skydown App' -destination 'generic/platform=iOS' build
```

Android Build:

```bash
./gradlew :androidApp:compileDebugKotlin
./gradlew :androidApp:assembleDebug
```

Functions + Rules:

```bash
node --check functions/index.js
cd functions
npm run test:rules
```

Deploy:

```bash
firebase deploy --only firestore:rules
firebase deploy --only storage
firebase deploy --only functions
```

---

## 10) DSGVO + AVV Kit

Fuer Datenschutz- und Compliance-Betrieb ist jetzt ein eigenes Kit im Repo hinterlegt:

- `docs/compliance/README.md`
- `docs/compliance/DSGVO_RELEASE_CHECKLIST.md`
- `docs/compliance/AVV_VERARBEITER_REGISTER.md`
- `docs/compliance/VVT_VERARBEITUNGSTAETIGKEITEN.md`
- `docs/compliance/BETROFFENENRECHTE_SOP.md`
- `docs/compliance/DATENPANNEN_SOP.md`
- `docs/compliance/TOMS_CHECKLIST.md`

Empfehlung:
Vor jedem produktiven Release die komplette `DSGVO_RELEASE_CHECKLIST.md` auf Gruen setzen.
