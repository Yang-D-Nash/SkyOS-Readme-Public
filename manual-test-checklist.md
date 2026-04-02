# Manual Test Checklist

## Rollen und Zugriff

- [ ] `owner` kann Owner-only Bereiche nutzen
- [ ] `admin` sieht keine Owner-only Aktionen
- [ ] `subadmin` hat nur begrenzte Moderationsrechte
- [ ] `user` sieht keine Adminbereiche
- [ ] fremde `users/{uid}` sind fuer normale User nicht lesbar

## Auth und Registrierung

- [ ] Login mit bestehendem Konto funktioniert
- [ ] neues Nutzerkonto kann nur registriert werden, wenn `registrationsEnabled = true`
- [ ] bei Lockdown werden neue Registrierungen blockiert
- [ ] Owner bleibt im Recovery-Fall handlungsfaehig

## Profil und Galerie

- [ ] User kann eigenes Profil lesen
- [ ] User kann eigenes Profil aktualisieren
- [ ] User kann eigenes Profilbild hochladen
- [ ] User kann Galerie-Bild hochladen
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

## App Check

- [ ] Android Debug-Token ist in Firebase Console hinterlegt
- [ ] iOS Debug-Token ist in Firebase Console hinterlegt
- [ ] Android auf echtem Geraet funktioniert mit App Check
- [ ] iOS auf echtem Geraet funktioniert mit App Check
- [ ] in `monitor` kommen valide Requests an
- [ ] vor `enforce` gibt es keine unerwarteten Missing-Token-Logs

## Functions

- [ ] `requestUploadSlot` liefert klare Ablehnungsgruende
- [ ] `syncCurrentUserClaims` funktioniert
- [ ] `setUserRole` ist nur fuer Owner nutzbar
- [ ] `setRuntimeLockdown` ist nur fuer Owner nutzbar
- [ ] `submitMerchOrder` ist im Lockdown fuer normale User blockiert
- [ ] `skydownAgent` ist nur mit gueltigem App Check + erlaubtem AI-Zugriff nutzbar

## Kosten-/Monitoring-Checks

- [ ] Budget Alert Topic existiert
- [ ] Budget Notification erreicht `applyBudgetLockdown`
- [ ] Lockdown wird bei Budget-Alarm gesetzt
- [ ] Logs zeigen denied requests / App Check / Upload-Blockierungen sauber an
