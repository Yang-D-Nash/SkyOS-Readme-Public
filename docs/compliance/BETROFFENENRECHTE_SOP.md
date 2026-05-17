# SOP Betroffenenrechte (SkyOS / Skydown)

Zweck:
- Standardprozess fuer DSGVO-Anfragen von Nutzern.

Rechte:
- Auskunft
- Berichtigung
- Loeschung
- Einschraenkung
- Datenuebertragbarkeit
- Widerspruch

---

## 1) Eingang der Anfrage

Kanaele:
- Support Mail: `skydownent@gmail.com`
- In-App Support: `Settings > Rechtliches > Kontakt`

Erfassen:
- Datum/Uhrzeit
- Kanal
- Account (Email/UID falls vorhanden)
- Art der Anfrage

Frist:
- Eingang sofort bestaetigen
- in der Regel innerhalb 1 Monat beantworten

---

## 2) Identitaetspruefung

- Ist der Nutzer eingeloggt und eindeutig zuordenbar?
- Bei Mail-Anfragen: verifizierbare Rueckantwort an registrierte Mailadresse.
- Ohne ausreichende Verifikation: keine Herausgabe personenbezogener Daten.

---

## 3) Prozess je Anfrageart

### 3.1 Auskunft

Liefern:
- welche Datenkategorien gespeichert sind
- zu welchem Zweck
- Empfaenger/Verarbeiter
- Speicherdauer

Quelle:
- `users/<uid>`
- `userProfiles/<uid>`
- relevante Logs/Metadaten (nur soweit zulaessig)

### 3.2 Berichtigung

- Falsche Stammdaten korrigieren.
- Aenderung dokumentieren (Zeitpunkt, Bearbeiter).

### 3.3 Loeschung

- Account-Loeschung ueber vorgesehenen technischen Flow ausfuehren.
- Zugeordnete Daten und Medien loeschen, soweit keine Aufbewahrungspflicht entgegensteht.
- Ergebnis dokumentieren.

### 3.4 Datenuebertragbarkeit

- Nutzerdaten in strukturiertem, gaengigem Format bereitstellen (z. B. JSON/CSV).

### 3.5 Widerspruch / Einschraenkung

- Verarbeitung fuer betroffene Zwecke deaktivieren/einschraenken, sofern einschlaegig.

---

## 4) Antwort an Nutzer

Mindestens enthalten:
- Ergebnis (erledigt/teilweise/abgelehnt)
- Begruendung
- ggf. Restdaten wegen gesetzlicher Pflichten
- Kontakt fuer Rueckfragen

---

## 5) Interne Dokumentation

Pro Fall dokumentieren:
- Ticket-ID
- Anfrageart
- Verifikation
- Massnahmen
- Abschlussdatum
- Bearbeiter

Aufbewahrung:
- Nachweis zweckgebunden aufbewahren.
