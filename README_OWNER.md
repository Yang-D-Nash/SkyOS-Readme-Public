# SkyOS Owner Guide

Dieses Handbuch beschreibt den Betrieb von SkyOS aus Owner-Sicht.
SkyOS ist die Produkt- und Steuerungsschicht des `Skydown x 22` Universums und verbindet Experience, Rollen, KI, Workflows, Commerce, Sicherheit und Release-Governance.

Zuletzt aktualisiert: `20. April 2026`

---

## 1) Zielbild Owner

Als Owner verantwortest du nicht nur Einstellungen, sondern das Systemgefuehl.
Du steuerst:

- Rollen und Rechte
- KI-Freigaben, Tageslimits und globale Caps
- Prompt- und Runtime-Governance
- Workflow-Automation und BYOS-Setups
- Commerce, Payments und Checkout-Freigaben
- Rechtliches, Compliance und Release-Gates
- Lockdown, Recovery und Kostenkontrolle

Kurz:
Du verantwortest Produktqualitaet, Betriebssicherheit und Vertrauensniveau.

Rechtlich Verantwortlicher und Betreiber:

- `Skydown`
- `Erich-Plate-Weg 44, 22419 Hamburg, Deutschland`
- Kontakt: `skydownent@gmail.com`

---

## 2) Produktlogik Owner

SkyOS ist als System gedacht.
Deshalb gilt fuer Owner-Entscheidungen:

- Klarheit vor Feature-Dichte
- Kontrolle vor Spektakel
- konsistente Freigaben vor schnellen Ausnahmen
- sichere Defaults vor aggressiver Oeffnung

Bei jeder Owner-Entscheidung sollte geprueft werden:

1. Verbessert das die Experience?
2. Erhoeht das Kosten- oder Missbrauchsrisiken?
3. Versteht ein normaler Nutzer noch, was passiert?
4. Bleibt die App als ein System lesbar?

---

## 3) Rollensteuerung

Fester Owner-Account:

- `nash.lioncorna@gmail.com`

Rollen im System:

- `owner`
- `admin`
- `subadmin`
- `user`
- `gast`

Wichtige Regeln:

- Rollenwechsel sind owner-only und serverseitig abgesichert.
- Teamrechte werden gezielt und bereichsbezogen vergeben.
- Neue Konten werden technisch gebootstrapped, damit sie sauber im User-Management erscheinen.

Empfohlener Ablauf:

1. `Settings > Owner > Users` oeffnen.
2. Zielkonto waehlen.
3. Rolle, Quota und Zusatzrechte setzen.
4. Speichern.
5. Mit Testkonto oder Zielkonto verifizieren.

---

## 4) KI Governance

SkyOS trennt zwischen Produktwert und Kostenkontrolle.

### Pro Konto steuerbar

- KI an/aus
- Tageslimits fuer:
  - Bot / Text
  - Visuals
  - Agent
- History-Retention

### Global steuerbar

- Bot-Systemanweisung
- Visual-Systemanweisung
- Agent-Systemanweisung
- globale Stil- und Brandvorgaben
- Provider- und Runtime-Konfiguration

### Runtime-Steuerung

- Agent Provider
- Fallback Provider
- Hard Daily Caps
- Global Daily Caps
- Timeouts / Polling / History-Grenzen
- Lockdown bei Budget- oder Abuse-Risiken

Empfehlung:

1. konservativ starten
2. echte Nutzung beobachten
3. erst dann segmentiert anheben

---

## 5) Workflow Automation Governance

SkyOS nutzt persoenliche Workflow-Konfigurationen je Konto.

- `adminConfig/automationN8n_<uid>`
- optional `adminConfig/agentProfile_<uid>`

Konsequenz:

- jeder Nutzer bleibt in seiner eigenen Workflow-Welt
- kein unbeabsichtigtes Cross-User-Mischen
- klare Verantwortlichkeit fuer externe Dienste und Kosten

Webhook-Flow:

1. Nutzer oder Agent sendet Trigger an die Cloud Function.
2. Das Backend laedt nur die persoenliche Konfiguration des Users.
3. Der konfigurierten Webhook wird aufgerufen.
4. Erfolg oder Fehler wird an die App zurueckgegeben.

---

## 6) Commerce, Shopify und Payments

Owner-only Bereiche:

- Shopify-Setup
- Commerce-Basiseinstellungen
- Payment-Methoden
- Zahlungs- und Secret-Verwaltung

Pruefregeln:

1. Produkt-Sync stimmt
2. Checkout ist Ende-zu-Ende testbar
3. Payment-Statuswechsel landen korrekt im Order-Flow
4. Support- und Recovery-Pfad sind klar

---

## 7) Sicherheit, Lockdown und Recovery

Technische Leitplanken:

- deny-by-default Rules
- Claims- und Rollenlogik
- App Check / Abuse-Schutz
- Upload-Slots statt offener Upload-Flaechen
- serverseitige Limits
- Runtime Lockdown
- Budget Lockdown

Wichtige Runtime-Felder:

- `lockdown`
- `uploadsEnabled`
- `registrationsEnabled`
- `userWritesEnabled`
- `budgetLockdownEnabled`

Wenn etwas kippt:

1. Lockdown aktivieren
2. betroffene Funktion isolieren
3. Fehlerbild und Logs sichern
4. Fix in Testumgebung pruefen
5. kontrolliert wieder oeffnen

---

## 8) Release Gate

Vor jeder externen Verteilung:

1. Android Build gruen
2. iOS Build / Archive gruen
3. Functions und Rules gruen
4. frisches Konto: Registrierung / Login / Profil pruefen
5. Rollenwechsel und Rechte pruefen
6. Bot / Visual / Agent inkl. Limit-Fehler pruefen
7. Workflow-Test pro Testkonto pruefen
8. Shop / Checkout / Payment pruefen
9. Rechtliches, Support und In-App Dokumente pruefen

Release darf erst raus, wenn Funktion, Sprache und Vertrauen zusammenpassen.

---

## 9) Relevante Dokumente

- [README.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/README.md)
- [README_USER.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/README_USER.md)
- [docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md)
- [docs/compliance/README.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/docs/compliance/README.md)
