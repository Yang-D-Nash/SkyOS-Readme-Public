# SkyOS Experience Principles

Zuletzt aktualisiert: `5. Mai 2026`

Dieses Dokument beschreibt die Erlebnis- und Systemregeln fuer SkyOS.
Es ist kein Moodboard, sondern die gemeinsame Arbeitsgrundlage fuer Design, Produkt, Entwicklung, Review und Release.

---

## 1) Ziel

SkyOS soll sich wie ein `zusammenhaengendes Creator Operating System` anfuehlen.
Nicht wie:

- mehrere zusammengesteckte Screens
- eine Sammlung von Features
- ein Effekt-Showcase ohne Orientierung

Sondern wie:

- ein gefuehrtes System
- eine klare Welt mit Wiedererkennungswert
- eine Plattform, die Kontrolle und Immersion verbindet

---

## 2) Offizielle Leitplanken

SkyOS orientiert sich an langlebigen Standards:

### Apple Human Interface Guidelines

Relevant fuer:

- klare visuelle Hierarchie
- Fokus auf Inhalt statt Ornament
- kontrollierte Tiefe, nicht chaotische Komplexitaet

Quelle:

- https://developer.apple.com/design/human-interface-guidelines/

### Material 3

Relevant fuer:

- semantische Farbrollen
- konsistente Komponentenlogik
- Motion mit Bedeutung statt Zufall

Quellen:

- https://m3.material.io/
- https://developer.android.com/jetpack/androidx/releases/compose-material3

### WCAG 2.2

Relevant fuer:

- Kontrast
- Fokus-Sichtbarkeit
- nicht verdeckte Interaktionen
- robuste Bedienbarkeit ueber verschiedene Geraete hinweg

Quellen:

- https://www.w3.org/TR/WCAG22/
- https://www.w3.org/WAI/WCAG22/Understanding/focus-appearance.html
- https://www.w3.org/WAI/WCAG22/Understanding/focus-not-obscured-minimum

---

## 3) Psychologische Produktprinzipien

### Orientierung senkt kognitive Last

- Nutzer sollen immer wissen, wo sie sind.
- Jede Ansicht braucht eine erkennbare Hauptfunktion.
- Sekundaere Aktionen duerfen die primaere Aufgabe nicht ueberstrahlen.

### Kontrolle schafft Vertrauen

- Jeder Dialog, Viewer oder Vollbildmodus braucht einen sichtbaren Rueckweg.
- Interaktive Elemente duerfen nie verdeckt oder schwer erreichbar sein.
- Fehlerzustaende muessen erklaert werden, nicht nur angezeigt.

### Schoenheit hilft, aber nur wenn sie traegt

- Ein hochwertiges Erscheinungsbild verbessert die wahrgenommene Qualitaet.
- Dieser Effekt kippt sofort, wenn der Flow verwirrend oder fragil ist.
- Deshalb gilt: Aesthetik darf Usability veredeln, nie ersetzen.

### Wiederholung erzeugt Systemgefuehl

- Dieselben Bedeutungen muessen dieselben Formen haben.
- Beispiel:
  - primaere Aktion = gefuellter Brand-Button
  - sekundaere Aktion = Outline/Button mit klar geringerem Gewicht
  - Status = kleine ruhige Chips oder Metriken

### Erinnerung entsteht an Uebergaengen

- der erste Eindruck
- das erste Erfolgserlebnis
- der Moment nach einer Aktion
- das Ende eines Flows

Deshalb sind Home, Uebergaenge, Feedback, Vollbildmodi und Rueckwege besonders wichtig.

---

## 4) SkyOS Gestaltungsregeln

### 4.1 Hierarchie

Jeder Screen folgt in dieser Reihenfolge:

1. Kontext
2. wichtigste Aktion
3. sekundaere Aktionen
4. Status / Metrik / Orientierung
5. Details

### 4.2 Farbe

- Primary nur fuer Hauptfokus und aktive Zustaende
- Secondary/Tertiary fuer Bereichsidentitaet, nicht als Dauerflaeche
- Surface und Outline tragen die Ruhe des Systems
- Glow ist Akzent, nicht Hintergrundrauschen

### 4.3 Motion

- Motion erklaert Zustandswechsel
- Motion darf nie Bedienbarkeit, Lesbarkeit oder Performance opfern
- Kein permanentes Flackern, kein Selbstzweck

### 4.4 Feedback

Jede relevante Interaktion braucht mindestens eines davon:

- pressed feedback
- loading state
- success state
- error state
- blocked state mit Grund

### 4.5 Text

- kurz vor clever
- klar vor poetisch, ausser in bewusst inszenierten Brand-Momenten
- keine doppelten Informationen in Text und Chips
- Titel benennen die Aufgabe, nicht nur Stimmung

### 4.6 Plattformlogik

- iOS und Android duerfen nicht identisch sein
- sie muessen sich aber wie dieselbe Produktfamilie anfuehlen
- Abweichungen duerfen nur aus Plattformlogik oder Performancegruenden entstehen

---

## 5) Was SkyOS vermeiden soll

- ueberladene Hero-Flaechen
- zu viele gleich starke CTA-Ebenen
- Chips, die aussehen wie Buttons, aber nichts tun
- Overlays, die wichtige Inhalte verdecken
- Vollbildmodi ohne klaren Ausstieg
- zufaellige Bereichsfarben ohne Rollenlogik
- schoene, aber unzuverlaessige Flows

---

## 6) Release-Definition fuer Experience

Ein Screen gilt erst dann als release-reif, wenn:

- er sofort lesbar ist
- die Hauptaktion in unter 2 Sekunden visuell erfassbar ist
- nichts Wichtiges verdeckt, abgeschnitten oder unerreichbar ist
- Rueckwege klar sind
- Feedback konsistent ist
- die Sprache kurz und hochwertig wirkt
- der Screen wie Teil von SkyOS aussieht, nicht wie ein Fremdmodul

---

## 7) Arbeitsmodus fuer weitere Veredelung

Wir veredeln SkyOS in dieser Reihenfolge:

1. gemeinsame Design-Tokens und Interaktionsmuster
2. Home als System-Einstieg
3. Music / Video / Shop / AI als Kernwelten
4. Profile / Settings / Rechtliches / Recovery
5. Motion, 3D, Premium-Frameworks und feinere Inszenierung

Grundsatz:

Erst Stabilitaet und Klarheit.
Dann Spektakel.
