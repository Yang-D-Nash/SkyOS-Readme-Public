# SkyOS

| 22 | SkyOS | Skydown |
| --- | --- | --- |
| ![22 Logo](<Skydown App/Assets.xcassets/Sky22BrandLogo.imageset/22-logo.png>) | ![SkyOS](<Skydown App/Assets.xcassets/SkydownX22BrandLogo.imageset/skydown-x22-logo.png>) | ![Skydown](<Skydown App/Assets.xcassets/SkydownBrandLogo.imageset/skydown-logo.png>) |

SkyOS ist die aktuelle Produkt- und Erlebnis-Schicht des `Skydown x 22` Universums.
Es verbindet Content, Commerce, KI, Profile und Workflow-Automation so, dass sich alles wie ein einziges System anfuehlt statt wie lose verbundene Features.

Stand: `Release Candidate` fuer produktive Tests, Release-Haertung und System-Veredelung.
Zuletzt aktualisiert: `20. April 2026`

---

## 1) Produktbild

SkyOS ist kein normaler App-Baukasten, sondern ein `Creator Operating System`.
Das Ziel ist ein Produkt, das:

- klar fuehrt statt zu ueberladen
- emotional bindet, ohne die Kontrolle zu verlieren
- modern wirkt, aber nicht nur trendig ist
- Workflows, Medien, Identitaet und Commerce in einem mentalen Modell vereint

Systembereiche:

- `Home`: Orientierung, Status, Einstieg und Systemfokus
- `Music`: Releases, Artists, Beat-Hub und Studio-Flows
- `Video`: Clips, Reels, Playback und Creator-Visuals
- `Shop`: Merch, Produkte, Checkout und Bestellungen
- `AI`: Bot, Visuals, Agent und Workflow-Anbindung
- `Profile`: Identitaet, Galerie, Creator-Praesenz
- `Settings`: Rollen, Limits, Rechtliches, Betrieb und Recovery

---

## 2) SkyOS Experience Principles

SkyOS soll sich wie ein zusammenhaengendes Produkt anfuehlen. Deshalb gelten diese Regeln systemweit:

### Hierarchie vor Spektakel

- Jede Flaeche braucht eine klare Reihenfolge: Kontext, wichtigste Aktion, sekundaere Optionen, Details.
- Farbe und Glow duerfen Orientierung unterstuetzen, aber nie Inhalte ueberdecken.
- Text wird gekuerzt, wenn Chips, Bildsprache oder Statussignale dieselbe Information bereits tragen.

### Kontrolle vor Immersion

- Jeder Screen muss eindeutig schliessbar, verlassbar und wieder auffindbar sein.
- Fokus, Scrollbarkeit und Rueckwege haben Vorrang vor Effekten.
- Wichtige Interaktionen duerfen nicht verdeckt, abgeschnitten oder durch Overlays blockiert sein.

### Ein System, keine Inseln

- Home, Music, Video, Shop, AI und Settings sprechen dieselbe visuelle Sprache.
- Komponenten muessen auf iOS und Android verwandt wirken, auch wenn sie plattformspezifisch angepasst sind.
- Der Nutzer soll nie das Gefuehl haben, in verschiedene Apps zu springen.

### Meaningful Feedback

- Jede relevante Aktion braucht Rueckmeldung: pressed, loading, success, blocked, error.
- Feedback muss schnell, lesbar und emotional ruhig sein.
- Kein unerklaertes Schweigen bei Netzwerk-, Rechte- oder Limit-Fehlern.

### Progressive Disclosure

- Erst Orientierung, dann Tiefe.
- Komplexe Dinge wie AI-Runtime, Rollen, Workflow oder Payments werden schrittweise geoeffnet.
- Laien duerfen sich nicht dumm fuehlen; Power-User duerfen sich nicht ausgebremst fuehlen.

### Vertrauen ist Teil des Designs

- Rechtliches, Rollen, Kostenkontrolle und Datenschutz sind Teil der Produktqualitaet.
- Die App soll hochwertig wirken, weil sie sicher, sauber und erklaerbar ist, nicht nur weil sie glaenzt.

---

## 3) Leitplanken und Referenzen

SkyOS richtet sich bewusst nach langlebigen Standards statt nach kurzfristigen Trends.

- `Apple Human Interface Guidelines`: klare visuelle Hierarchie, Inhaltsfokus, kontrollierte Tiefe
- `Material 3`: Rollen fuer Farben, Komponenten und Motion statt dekorativem Wildwuchs
- `WCAG 2.2`: Kontrast, sichtbarer Fokus und nicht verdeckte Interaktionen

Produktinterne Experience-Referenz:

- [docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md)

Externe Referenzen:

- [Apple Human Interface Guidelines](https://developer.apple.com/design/human-interface-guidelines/)
- [Material 3](https://m3.material.io/)
- [WCAG 2.2](https://www.w3.org/TR/WCAG22/)

---

## 4) Marke, Betreiber, Rollen

### Produkt

- `SkyOS` ist der Produktname und die Betriebssystem-Metapher innerhalb der App.

### Betreiber

- Betreiber und operative Einheit ist `Skydown`.

### Oekosystem

- `22` bleibt Teil der Markenwelt, Symbolik und Kreativ-DNA.
- `NICMA` ist der Studio-/Producer-Bereich innerhalb derselben Produktwelt.

### Rollen

- `owner`: Systemsteuerung, KI-Governance, Runtime, Commerce, Rechtliches
- `admin`: operative Teamarbeit in zugewiesenen Bereichen
- `subadmin`: Creator-/Power-User mit erweiterten Kontingenten
- `user`: Standardkonto mit Profil, Medien, KI und Workflow innerhalb der Limits
- `gast`: Exploration ohne persistente Kontofunktionen

Fester Owner:

- `nash.lioncorna@gmail.com`

---

## 5) KI, Agent und Workflow

SkyOS verbindet moderne KI-Nutzung mit kontrollierter Systemlogik.

- `Bot`: schnelle Text- und Visual-Unterstuetzung
- `Agent`: auf Umsetzung, Struktur und naechste Schritte ausgerichtet
- `Workflow`: persoenliche Automationen und externe Services wie `n8n`

Governance:

- serverseitige Limits pro Konto
- globale Caps und Runtime-Flags
- Lockdown-Mechanismen fuer Kosten- und Risiko-Kontrolle
- persoenliche BYOS-Setups fuer externe Services

Grundsatz:

- KI ist Assistenz, nicht Autoritaet
- Automationen sind Produktivitaet, nicht Kontrollverlust

---

## 6) Commerce, Vertrauen und Release

SkyOS vereint Inspiration und Transaktion. Deshalb werden visuelle Qualitaet und betriebliche Zuverlaessigkeit gemeinsam gedacht.

- Shopify / Checkout / Fulfillment muessen Ende-zu-Ende testbar sein
- Rollen und Rechte muessen serverseitig konsistent greifen
- App Check, Abuse-Schutz, Limits und Recovery muessen vor Release belastbar sein
- Rechtliches und Support muessen direkt in der App abrufbar sein

Release-Ziel:

- hochwertig
- konsistent
- klar
- kontrollierbar
- erweiterbar

---

## 7) Dokumente im Repo

- [README_USER.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/README_USER.md)
- [README_OWNER.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/README_OWNER.md)
- [docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/docs/design/SKYOS_EXPERIENCE_PRINCIPLES.md)
- [docs/compliance/README.md](/Users/nash/.codex/worktrees/55fe/Skydown-App/docs/compliance/README.md)

In-App Dokumente:

- README / Guide
- Datenschutz
- AGB
- Nutzungsbedingungen

---

## 8) Kurzform

SkyOS soll sich nicht wie eine Ansammlung guter Screens anfuehlen.
Es soll sich wie ein erinnerbares System anfuehlen:

- emotional stark
- funktional klar
- technisch belastbar
- visuell hochwertig
- langfristig erweiterbar
