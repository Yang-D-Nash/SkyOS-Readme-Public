# App Check Rollout

## Ziel

Firebase App Check kontrolliert in diesem Projekt:

- `Cloud Firestore`
- `Cloud Storage`
- `Callable Functions`

Der Rollout ist bewusst zweistufig:

1. erst `monitor`
2. dann `enforce`

## Bestehende Integration

### Android

- Datei: [`androidApp/src/main/java/com.nash.skyos/SkydownApplication.kt`](androidApp/src/main/java/com.nash.skyos/SkydownApplication.kt)
- `Debug`: `DebugAppCheckProviderFactory`
- `Release`: `PlayIntegrityAppCheckProviderFactory`

### iOS

- Datei: [`Skydown App/FirebaseAppCheckProvider.swift`](Skydown%20App/FirebaseAppCheckProvider.swift)
- Datei: [`Skydown App/SkydownApp.swift`](Skydown%20App/SkydownApp.swift)
- `Debug` und `Simulator`: `AppCheckDebugProvider`
- `Release` auf echten Geraeten: `DeviceCheckProvider`
- Hinweis: `AppAttestProvider` kann spaeter kontrolliert wieder aktiviert werden.

### Functions

- Datei: [`functions/src/security/app-check.js`](functions/src/security/app-check.js)
- Datei: [`functions/index.js`](functions/index.js)
- sensibler Callable-Pfad wird serverseitig gegen App Check geprueft

## Schritt-fuer-Schritt

1. App Check in Firebase Console fuer Android und iOS aufrufen.
2. Sicherstellen, dass die registrierten Apps korrekt sind.
3. Debug-Token von Android-Debug und iOS-Simulator in der Console hinterlegen.
4. `system/runtimeConfig.appCheckMode = "monitor"` setzen.
5. Testbuild auf echtem Android-Geraet starten.
6. Testbuild auf echtem iPhone starten.
7. Firestore-Zugriffe pruefen.
8. Storage-Uploads pruefen.
9. Callable Functions pruefen:
   - `requestUploadSlot`
   - `syncCurrentUserClaims`
   - `setUserRole`
   - `setRuntimeLockdown`
   - `submitMerchOrder`
   - `skydownAgent`
10. In Logs und Metrics verifizieren, dass gueltige App-Check-Requests ankommen.
11. Erst danach `system/runtimeConfig.appCheckMode = "enforce"` setzen.
12. Danach Enforcement fuer Firestore und Storage in Firebase Console aktivieren.

## Monitor-Phase

In `monitor` gilt:

- fehlende App-Check-Tokens werden geloggt
- Requests werden noch nicht hart geblockt
- Client-Setup und echte Device-Pfade koennen ohne Lockout validiert werden

## Enforce-Phase

In `enforce` gilt:

- fehlende oder ungueltige App-Check-Tokens werden serverseitig abgelehnt
- Firestore und Storage sollten erst nach erfolgreicher Monitor-Phase in der Console auf Enforcement gestellt werden

## Vor Enforce pruefen

- Android-Release-Build auf echtem Geraet erfolgreich
- iOS-Release-Build auf echtem Geraet erfolgreich
- Upload-Flow erfolgreich
- Login / Profil / Galerie erfolgreich
- keine unerwarteten `App Check token missing`-Logs mehr
- keine internen Admin-/Owner-Flows blockiert

## Rollback

Wenn Probleme auftreten:

1. `system/runtimeConfig.appCheckMode` wieder auf `monitor`
2. Firestore/Storage Enforcement in Firebase Console deaktivieren
3. Debug-Tokens und App-Registrierungen erneut pruefen
4. erst nach erfolgreichem Re-Test wieder hochziehen
