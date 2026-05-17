# Manual Test Checklist

## Release Gate

- [ ] Kein Punkt mit Release-Blocker unten ist offen
- [ ] Alle clientseitigen Punkte wurden auf Android und iPhone geprueft
- [ ] Alle rollenbezogenen Punkte wurden mit `owner`, `admin`, `premium (subadmin)` und `user` geprueft
- [ ] Alle kritischen Aktionen geben sichtbares Feedback und eine klare Erfolgs- oder Fehlermeldung

## Test Setup

- [ ] Android-Release-Build auf echtem Geraet installiert
- [ ] iPhone-Release-Build auf echtem Geraet installiert
- [ ] Testkonten vorhanden: `owner`, `admin`, `premium`, `user`
- [ ] Mindestens ein negativer AI-Testaccount mit `aiAccessEnabled = false` vorhanden
- [ ] Testdaten vorhanden: mindestens 1 Merch-Produkt, 1 Artist, 1 Track oder Beat, 1 Video, 1 Collab, 1 bestehendes Profilbild

## Run Matrix

| Rolle | Android | iPhone | Fokus |
| --- | --- | --- | --- |
| `owner` | [ ] | [ ] | Owner-Workspace, Rollen, Runtime, AI-Prompts |
| `admin` | [ ] | [ ] | Staff-AI, freigegebene Verwaltungsrechte |
| `premium (subadmin)` | [ ] | [ ] | Kontingentmodell ohne Staff-/Owner-Rechte |
| `user` | [ ] | [ ] | normale Endnutzer-Pfade ohne Admin-UI |

## Global UX Gate

Diese Punkte gelten fuer jeden Screen, jede Rolle und beide Plattformen.

- [ ] Jeder tappbare Button, jede Card und jedes Icon mit Aktion hat Press-Feedback
- [ ] Lange Aktionen zeigen direkt am ausloesenden Element einen Loading-Zustand
- [ ] Erfolgreiche Aktionen zeigen Toast, Overlay oder Statusmeldung mit klarem Ergebnis
- [ ] Fehler zeigen eine verstaendliche Nachricht und keine rohen Firebase- oder Techniktexte
- [ ] Disabled-Buttons sehen disabled aus und lassen sich nicht versehentlich erneut ausloesen
- [ ] Sheets, Dialoge, Toasts und Overlays respektieren Safe Areas, Keyboard, Notch und Bottom Bar
- [ ] Nach Speichern oder Submit ist der neue Zustand direkt sichtbar oder klar als "Neuanmeldung fuer Claims noetig" erklaert
- [ ] Es gibt keine abgeschnittenen Texte, springenden Layouts, ueberschneidenden Elemente oder leeren Bildcontainer

## Rollen und Zugriff

### Erwartete Rechte

| Funktion | `owner` | `admin` | `premium (subadmin)` | `user` |
| --- | --- | --- | --- | --- |
| Owner-Workspace in Settings | Ja | Nein | Nein | Nein |
| Staff-Zugriff fuer `ai_access_mode = admin_only` | Ja | Ja | Nein | Nein |
| Rollen anderer Nutzer aendern | Ja | Nein | Nein | Nein |
| Runtime-Lockdown aendern | Ja | Nein | Nein | Nein |
| Premium-/Kontingentkonto | Optional | Optional | Ja | Optional |
| Music/Video/Profile-Verwaltung | Ja | Nur wenn explizit freigegeben | Nein | Nein |

### Sichtbarkeit und Guards

- [ ] `owner` kann Owner-only Bereiche nutzen
- [ ] `admin` sieht keine Owner-only Aktionen
- [ ] `premium (subadmin)` wird in der UI als Premium dargestellt und nicht als Staff/Admin missverstanden
- [ ] `premium (subadmin)` hat kein Admin-Workspace und keine Owner-Rechte
- [ ] `user` sieht keine Adminbereiche
- [ ] fremde `users/{uid}` sind fuer normale User nicht lesbar

## Launch, Navigation und Session

- [ ] Intro und Entry-Landing funktionieren auf Android sauber
- [ ] Intro und Entry-Landing funktionieren auf iPhone sauber
- [ ] Hauptnavigation zu `Merch`, `Music`, `Home`, `Videos`, `Tools` funktioniert ohne leere States
- [ ] `Settings`, `Profile` und `Cart` lassen sich jederzeit oeffnen und sauber wieder schliessen
- [ ] Sign-in nach App-Neustart stellt Session und Rolle korrekt wieder her
- [ ] Sign-out entfernt geschuetzte Bereiche sofort sichtbar aus der UI

## Auth und Registrierung

- [ ] Login mit bestehendem Konto funktioniert
- [ ] neues Nutzerkonto kann nur registriert werden, wenn `registrationsEnabled = true`
- [ ] bei Lockdown werden neue Registrierungen blockiert
- [ ] Owner bleibt im Recovery-Fall handlungsfaehig
- [ ] Formularvalidierung zeigt klare Fehlermeldungen fuer leere oder ungueltige Eingaben
- [ ] Erfolgreicher Login fuehrt ohne haengenden Overlay- oder Sheet-Zustand in die App zurueck

## Settings und Rollenvergabe

### Owner

- [ ] Owner sieht den Owner-Workspace in Settings auf Android
- [ ] Owner sieht den Owner-Workspace in Settings auf iPhone
- [ ] `Users`-Bereich listet Nutzer verlässlich und ohne Platzhalter-Fehler
- [ ] Rollenwechsel `user -> admin` zeigt `Speichert...` und danach klare Erfolgsmeldung
- [ ] Rollenwechsel `user -> premium` zeigt `Speichert...` und danach klare Erfolgsmeldung
- [ ] Rollenwechsel `admin -> user` oder `premium -> user` aktualisiert sichtbare Rolle nach Reload korrekt
- [ ] Zusatzrechte fuer `admin` wie Music, Video und Profilmoderation lassen sich speichern und bleiben bestehen
- [ ] Nach Rollenwechsel bekommt der Zielnutzer die neuen Rechte nach Refresh oder Neuanmeldung korrekt
- [ ] Fehler beim Speichern sind als Fehler visuell erkennbar und nicht als Erfolg gestaltet

### Admin

- [ ] `admin` sieht keinen Owner-Workspace
- [ ] `admin` kann keine Rollen anderer Nutzer aendern
- [ ] `admin` sieht nur die Verwaltungsfunktionen, die ueber Flags freigegeben wurden

### Premium und User

- [ ] `premium (subadmin)` sieht keinen Owner-Workspace
- [ ] `user` sieht keinen Owner-Workspace
- [ ] Beide Rollen koennen keine Rollenverwaltung ausloesen

## AI und Tools

Diese Punkte fuer Android und iPhone jeweils mit `owner`, `admin`, `premium`, `user` pruefen.

- [ ] `ai_enabled = false` zeigt klare Pause-Meldung
- [ ] `ai_access_mode = off` blockiert Bot, Agent und Visuals fuer alle Rollen
- [ ] `ai_access_mode = admin_only` erlaubt AI nur fuer `owner` und `admin`
- [ ] `ai_access_mode = admin_only` zeigt fuer `premium` und `user` die korrekte Staff-only Meldung
- [ ] `ai_access_mode = signed_in` erlaubt AI fuer alle angemeldeten Rollen mit `aiAccessEnabled = true`
- [ ] Nutzer mit `aiAccessEnabled = false` erhalten die kontobezogene Pause-Meldung
- [ ] Senden in Bot oder Agent zeigt direkt Loading und danach Antwort oder Fehlermeldung
- [ ] AI-Antwortbereiche bleiben bei langen Antworten scrollbar und layouten nicht kaputt

## AI-Prompt-Settings

Owner-only.

- [ ] Owner kann Text-, Visual- und Agent-Anweisungen in der App bearbeiten
- [ ] Speichern zeigt klares Loading und klare Erfolgsmeldung
- [ ] Nach erneutem Oeffnen der App sind die geaenderten Werte weiterhin sichtbar
- [ ] Die App erklaert, dass die Werte serverseitig unter `adminConfig/aiPromptSettings` liegen

## Shop, Cart und Orders

- [ ] Shop laedt Produkte ohne leere oder ueberschneidende Cards
- [ ] Filter, Kategorien und Collab-Navigation geben Press-Feedback
- [ ] Produktgalerien und Vollbildansichten oeffnen und schliessen sauber
- [ ] In den Warenkorb legen gibt direkt Feedback
- [ ] Kontakt- und Message-Felder validieren sauber
- [ ] `submitMerchOrder` zeigt im Erfolgsfall eine klare Bestaetigung
- [ ] `submitMerchOrder` zeigt im Fehlerfall eine klare Fehlermeldung
- [ ] Lockdown blockiert Bestellversuche normaler Nutzer sichtbar und verstaendlich

## Music, Artists und Video Hub

- [ ] Music Screen laedt Tracks, Artist-Teaser und Deep Links korrekt
- [ ] Artist Page oeffnet ohne Layout-Spruenge
- [ ] BeatHub oder Producer-Bereiche laden ohne leere Platzhalterzustande
- [ ] VideoHub laedt Hero, Listen und Detailaktionen stabil
- [ ] Featured-Collab-Bilder sind auf Android sichtbar
- [ ] Featured-Collab-Karten sind auf iPhone gleich hoch und nicht uebergross
- [ ] Externe Links und Player-Aktionen geben vor dem Wechsel klares Feedback

## Profil und Galerie

- [ ] User kann eigenes Profil lesen
- [ ] User kann eigenes Profil aktualisieren
- [ ] User kann eigenes Profilbild hochladen
- [ ] User kann Galerie-Bild hochladen
- [ ] Erfolgreiche Uploads zeigen direkt sichtbares Feedback
- [ ] Fehler bei Upload-Slot, Dateityp oder Groesse sind in der UI klar benannt
- [ ] User kann keine fremde Galerie lesen oder schreiben

## Upload-Sicherheit

- [ ] Upload ohne `requestUploadSlot` scheitert
- [ ] Upload in fremden Storage-Pfad scheitert
- [ ] `image/jpeg` funktioniert
- [ ] `image/png` funktioniert
- [ ] `image/webp` funktioniert
- [ ] `image/gif` wird abgelehnt
- [ ] Datei ueber `5 MB` wird abgelehnt
- [ ] mehr als `10` Galerie-Bilder werden blockiert
- [ ] mehr als `20` Uploads in `24h` werden blockiert

## Lockdown / Safe Mode

- [ ] `lockdown = true` blockiert normale User-Schreibzugriffe
- [ ] `uploadsEnabled = false` blockiert Uploads
- [ ] `registrationsEnabled = false` blockiert Registrierungen
- [ ] Owner kann Runtime-Config weiterhin fuer Recovery aendern
- [ ] Lockdown-Kommunikation in der App ist fuer Nutzer verstaendlich und nicht technisch formuliert

## App Check

- [ ] Android Debug-Token ist in Firebase Console hinterlegt
- [ ] iOS Debug-Token ist in Firebase Console hinterlegt
- [ ] Android auf echtem Geraet funktioniert mit App Check
- [ ] iOS auf echtem Geraet funktioniert mit App Check
- [ ] in `monitor` kommen valide Requests an
- [ ] vor `enforce` gibt es keine unerwarteten Missing-Token-Logs
- [ ] unter `enforce` werden fehlende Tokens verlässlich blockiert und in Logs eindeutig sichtbar

## Functions

- [ ] `requestUploadSlot` liefert klare Ablehnungsgruende
- [ ] `syncCurrentUserClaims` funktioniert
- [ ] `setUserRole` ist nur fuer Owner nutzbar
- [ ] `setRuntimeLockdown` ist nur fuer Owner nutzbar
- [ ] `submitMerchOrder` ist im Lockdown fuer normale User blockiert
- [ ] `skydownAgent` ist nur mit gueltigem App Check und erlaubtem AI-Zugriff nutzbar

## Kosten- und Monitoring-Checks

- [ ] Budget Alert Topic existiert
- [ ] Budget Notification erreicht `applyBudgetLockdown`
- [ ] Lockdown wird bei Budget-Alarm gesetzt
- [ ] Logs zeigen denied requests, App Check und Upload-Blockierungen sauber an

## Release-Blocker

- [ ] Keine Rolleninkonsistenz zwischen sichtbarer UI und echter Berechtigung
- [ ] Keine Aktion ohne Feedback bei Save, Submit, Delete oder Navigation mit Seiteneffekt
- [ ] Keine fehlenden oder verzerrten Kernbilder in Shop, Music, VideoHub oder Profil
- [ ] Keine falsche AI-Freigabe fuer `admin_only`, `signed_in` oder kontobezogene Sperre
- [ ] Keine App-Check-Fehler auf echten Geraeten im normalen Release-Flow
