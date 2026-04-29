# SOP Datenpannen / Security Incidents (SkyOS / Skydown)

Ziel:
- Schnell, konsistent und nachvollziehbar auf Datenschutzvorfaelle reagieren.

---

## 1) Sofortmassnahmen (erste 60 Minuten)

1. Incident klassifizieren:
   - Vertraulichkeit
   - Integritaet
   - Verfuegbarkeit
2. Eindaemmung starten:
   - Runtime Lockdown aktivieren
   - betroffene Features deaktivieren (KI, Uploads, Registrierungen, Writes)
3. Evidenz sichern:
   - Logs, Zeitstempel, betroffene UIDs/Pfade
4. Incident Owner benennen.

---

## 2) Bewertung (innerhalb weniger Stunden)

Pruefen:
- Welche Daten sind betroffen?
- Wie viele Personen sind betroffen?
- Besteht ein Risiko fuer Rechte/Freiheiten?
- Laufender Angriff oder einmaliger Vorfall?

Dokumentieren:
- Was ist passiert?
- Wann begonnen / entdeckt?
- Welche Systeme betroffen?
- Welche Massnahmen umgesetzt?

---

## 3) Meldeentscheidung

Wenn meldepflichtig:
- Meldung an zustaendige Aufsichtsbehoerde ohne unnoetige Verzoegerung
- Zielwert: innerhalb 72 Stunden ab Kenntnis

Wenn voraussichtlich hohes Risiko fuer Betroffene:
- Benachrichtigung der betroffenen Personen vorbereiten und ausrollen

---

## 4) Kommunikation

Intern:
- Status-Updates im festen Takt
- klare Verantwortlichkeiten

Extern:
- sachlich, transparent, ohne Spekulation
- nur abgestimmte Kerninfos

---

## 5) Recovery und Nachbereitung

1. Ursache beheben
2. Fix verifizieren
3. kontrollierte Wiederinbetriebnahme
4. Post-Mortem:
   - Root Cause
   - Lessons Learned
   - dauerhafte Praeventivmassnahmen

---

## 6) Incident-Protokoll Template

- Incident-ID:
- Datum/Uhrzeit Entdeckung:
- Entdeckt durch:
- Betroffene Systeme:
- Betroffene Daten:
- Anzahl Betroffene:
- Risikoeinschaetzung:
- Meldepflicht: ja/nein
- Massnahmen:
- Abschlussdatum:
- Freigabe durch:

