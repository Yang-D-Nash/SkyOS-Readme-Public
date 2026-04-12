# 22xSky User Guide

Dieses Handbuch erklaert die App aus Endnutzer-Sicht.
Es zeigt dir, wie du schnell startest, KI/Agent sinnvoll nutzt und deinen eigenen Workflow-Service verbindest.

---

## 1) Was ist 22xSky?

22xSky ist die gemeinsame Plattform von `Skydown` und `22`.
Du bekommst in einer App:

- Music und Artist-Content
- Video- und Collabo-Bereiche
- Merch
- KI-Tools (Bot + Agent)
- persoenliches Profil mit Galerie

Brand-DNA:

- Meisterzahl `22` steht fuer Vision + Umsetzung (Master Builder).
- Symbolcode: `1337-514-731`
- Leet-Code: `7H3_F4LL_0F_H34/3N`

---

## 2) Schnellstart in 60 Sekunden

1. Konto erstellen oder einloggen.
2. Profilbild, Username und Bio setzen.
3. Im KI-Bereich den Bot testen (Text oder Visual).
4. Den Agent mit einer konkreten Aufgabe testen.
5. Optional in den Settings eigenen Workflow (n8n) hinterlegen.

---

## 3) Profile und Galerie

Mit deinem Konto kannst du:

- Profilinformationen bearbeiten
- Bilder in deine Galerie hochladen
- Inhalte spaeter wieder loeschen oder aktualisieren

Hinweis:

- Uploads und Schreibrechte koennen bei aktivem System-Lockdown temporaer eingeschraenkt sein.

---

## 4) Bot und Agent richtig nutzen

### 4.1 Bot

Nutze den Bot fuer schnelle Outputs:

- Captions
- Hooks
- Post-Text
- Visual-Ideen

Tipp:

- Je klarer dein Prompt (Ziel, Zielgruppe, Stil), desto besser das Ergebnis.

### 4.2 Agent

Nutze den Agent fuer Aufgaben mit Struktur:

- Content-Plan
- Release-Vorbereitung
- Briefing
- Automations-Uebergabe

Tipp:

- Gib dem Agenten immer ein Ergebnisformat vor, z. B. "5 Schritte + To-dos + Risiken".

---

## 5) Eigenen Workflow verbinden (n8n)

Jeder Nutzer kann seinen eigenen Workflow-Service nutzen.

In `Settings` hinterlegst du:

- `n8n aktiv`
- `Base URL`
- `Webhook Path`
- optional `Auth Header`
- optional `Knowledge-Kontext`

Danach kannst du einen Test triggern.

Was passiert dann:

1. Die App sendet den Trigger an das Backend.
2. Das Backend nutzt nur deine persoenliche Workflow-Konfiguration.
3. Dein Webhook wird aufgerufen.
4. Du bekommst direkt Erfolg oder Fehler als Rueckmeldung.

---

## 6) Eigenen Manus-Account (BYOS, optional)

Wenn du willst, kannst du deinen eigenen Manus-Key nutzen.

- iOS: Key wird lokal im Keychain gespeichert.
- Android: Key wird lokal verschluesselt gespeichert.

Wichtig:

- Ohne deinen lokalen Key nutzt die App wieder das Backend-Setup.
- Du kannst den Key jederzeit ersetzen oder entfernen.

---

## 7) Rollen aus User-Sicht

Standard ist `user`.
Wenn dir der Owner eine andere Rolle gibt, aendern sich ggf. deine Rechte und Limits.

Beispiele:

- `subadmin`: hoehere Kontingente
- `admin`: zusaetzliche operative Teamrechte

---

## 8) Sprache, Offline und Notifications

### Sprache

Die App orientiert sich an deiner Systemsprache.
Unterstuetzte Sprachen:

- `DE, EN, ES, FR, IT, PT, NL, PL, TR, JA`

### Offline

- Bei fehlender Verbindung zeigt die App Offline-Hinweise.
- Gecachte Inhalte bleiben verfuegbar, wenn vorhanden.
- Bestimmte Aktionen brauchen weiterhin Internet (z. B. Live-KI, n8n-Test).

### Notifications

- Benachrichtigungen koennen in der App und in den Geraete-Einstellungen verwaltet werden.

---

## 9) Troubleshooting

Wenn etwas nicht klappt:

1. Internetverbindung pruefen.
2. App neu starten und erneut anmelden.
3. In Settings pruefen, ob dein Workflow komplett konfiguriert ist.
4. Bei "nicht verfuegbar"-Meldungen spaeter erneut versuchen.

Wenn es weiter blockiert:

- Support: `skydownent@gmail.com`

---

## 10) Best Practices fuer starke Ergebnisse

1. Aufgaben klar formulieren: Ziel, Plattform, Ton, Laenge.
2. Fuer Agent-Aufgaben ein fixes Ausgabeformat verlangen.
3. Workflow erst lokal testen, dann in produktiven Kampagnen nutzen.
4. Prompts und Guardrails schrittweise verbessern statt alles auf einmal.
