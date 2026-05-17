# Release Readiness Report (SkyOS / Skydown)

Stand: `2026-05-05`
Scope: aktueller Store-Review-Stand fuer iOS TestFlight/App Store Connect, Android Internal Testing, Functions und Live-Konfiguration.

## 1) Verifizierte technische Gates

- iOS Device-Test auf Bobs iPhone:
  `xcodebuild test -project "Skydown App.xcodeproj" -scheme "Skydown App" -destination "id=9FFBA1FD-0734-5E66-9A33-A07D00BF5A54"`
  Ergebnis: `11 Tests erfolgreich, 0 Fehler` (4 Unit-Tests + 7 UI-Tests).
- iOS TestFlight/App Store Connect Upload:
  Build `10024` wurde erfolgreich zu App Store Connect hochgeladen und ist der aktuelle Store-Review-Kandidat.
- iOS Buildnummer:
  `CURRENT_PROJECT_VERSION = 10024`
- Android Release Bundle Smoke-Test:
  `ANDROID_HOME=<android-sdk-path> ./gradlew :androidApp:bundleRelease -PallowDebugReleaseSigning=true`
  Ergebnis: `BUILD SUCCESSFUL`
- Android Release Lint:
  `ANDROID_HOME=<android-sdk-path> ./gradlew :androidApp:lintRelease -PallowDebugReleaseSigning=true`
  Ergebnis: `No errors or warnings`
  Report: `androidApp/build/reports/lint-results-release.html`
- Android Unit-Test-Gate:
  `ANDROID_HOME=<android-sdk-path> ./gradlew :androidApp:testDebugUnitTest`
  Ergebnis: `BUILD SUCCESSFUL` (`NO-SOURCE`, aktuell keine JVM-Unit-Tests definiert).
- Android Versionsstand:
  `versionCode = 10026`, `versionName = "1.0.0"`
- Android Google Play Internal Testing:
  versionCode `10026` wurde am `2026-05-05` als interner Draft hochgeladen.
- Functions Rules Tests:
  `cd functions && npm run test:rules`
  Ergebnis: `42/42 Tests gruen`
- Functions Node-Test:
  `cd functions && npm run test:node`
  Ergebnis: `4/4 Tests gruen`
- Functions Full Gate:
  `npm --prefix functions run test`
  Ergebnis: `82/82 Tests gruen` (2026-05-05)

## 2) Sichtbar gehaertete UX-/UI-Punkte

- iPhone ist fuer den produktiven App-Flow jetzt portrait-only konfiguriert.
- iOS UI-Tests setzen das Geraet explizit auf Portrait, damit Fullscreen-/Close-Flows nicht durch falsche Ausrichtung kippen.
- Android Original-Viewer laedt die Start-URL nicht mehr bei jeder Recomposition neu.
- Android Video-Hub- und Merch-Fullscreen-Header nutzen jetzt explizit Statusbar-Padding, damit `Schliessen` auf echten Geraeten nicht unter Cutouts oder der Statusleiste sitzt.
- Android Release Lint ist nach den letzten String- und WebView-Anpassungen komplett sauber.

## 3) Harte Release-Gates, die noch offen sind

- Google Play Internal Testing ist fuer versionCode `10026` hochgeladen.
- Production-/Open-Testing-Rollout bleibt ein manueller Store-Console-Gate und startet erst nach finalem Device-Smoke.
- TestFlight Upload war erfolgreich, aber Apple hat beim Upload weiterhin dSYM-Warnungen fuer folgende Vendor-Frameworks ausgegeben:
  `FirebaseFirestoreInternal`, `absl`, `grpc`, `grpcpp`, `openssl_grpc`
- Diese Warnungen blockieren den Upload nicht, sind aber fuer spaetere Crash-Symbolication relevant.

## 4) Live-Konfigurations-/Server-Gates vor Go-Live

- Live Functions Inventory wurde am `2026-05-05` read-only gegen Firebase geprueft.
  Wichtige Endpunkte sind produktiv vorhanden, darunter:
  `authorizeAiUsage`, `generateAiText`, `generateAiVisual`, `skydownAgent`,
  `startMerchCheckout`, `confirmMerchOrderPayment`, `submitMerchOrder`,
  `startAiSubscriptionCheckout`, `stripeMerchWebhook`, `syncShopifyMerch`,
  `requestUploadSlot`, `deleteCurrentUserAccount`.
- Wichtig:
  Diese Inventur bestaetigt, dass die produktiven Endpunkte existieren.
  Sie bestaetigt noch **nicht**, dass jeder lokale Doku-/Client-Stand vom `2026-05-05`
  bereits nach Production deployed wurde.
- Shopify Sync/List Functions wurden am `2026-05-05` mit `SHOPIFY_ADMIN_ACCESS_TOKEN` als Secret-Dependency deployed.
- Meta OAuth Config wurde am `2026-05-05` live fuer Instagram Business Discovery, verbundenes Instagram Business-Konto und Facebook Page verifiziert.
- Runtime Config:
  `system/runtimeConfig`
  Erwartung fuer Production: `appCheckMode = enforce`
- Payment Methods:
  `appConfig/paymentMethods`
  Erwartung: Stripe/Klarna/AI-Subscriptions nur dann `enabled`, wenn die dazugehoerigen Secrets und Store-Produkte wirklich live sind.
- Shopify Public Config:
  `appConfig/shopifyMerch`
- Shopify Private Admin Token:
  `adminConfig/shopifyMerchPrivate`
  Erwartung: `adminApiToken` gesetzt, falls Shopify-Orders serverseitig erstellt werden sollen.
- Stripe Secret Status:
  `adminConfig/stripeCheckoutSecrets`
- Functions Secrets im Einsatz:
  `SMTP_CONNECTION_URL`
  `STRIPE_SECRET_KEY`
  `STRIPE_WEBHOOK_SECRET`
  `MANUS_API_KEY` (nur relevant, wenn der Agent-/Manus-Flow live genutzt wird)
- Secrets-Metadaten wurden am `2026-05-05` read-only gegen Firebase geprueft:
  `SMTP_CONNECTION_URL`: Version `1` aktiviert
  `STRIPE_SECRET_KEY`: Version `1` aktiviert
  `STRIPE_WEBHOOK_SECRET`: Version `1` aktiviert
  `MANUS_API_KEY`: Version `2` aktiviert (Version `1` ebenfalls vorhanden)
- Environment-basierte Konfiguration im Einsatz:
  `SHOPIFY_ADMIN_ACCESS_TOKEN`
  `SHOPIFY_STORE_DOMAIN`
  `ORDER_NOTIFICATION_TO`
  `ORDER_NOTIFICATION_FROM`
  `BILLING_BUDGET_TOPIC`

## 5) Compliance- und Betriebs-Gates, die weiter gelb bleiben

- Die offenen Punkte aus `docs/compliance/DSGVO_RELEASE_CHECKLIST.md` und `docs/compliance/COMPLIANCE_REVIEW_2026-04-15.md` bleiben relevant:
  AVV/DPA/SCC final,
  DSAR-Testfaelle dokumentieren,
  Monitoring-/Incident-Drill dokumentieren.
- Der Live-Check von `system/runtimeConfig` wurde zuletzt am `2026-05-05` dokumentiert.
  Vor finalem Public Release sollte dieser Wert noch einmal direkt gegen Production bestaetigt werden.

## 6) Aktuelle Freigabeeinschaetzung

- iOS TestFlight QA: `GO`
- Android internes QA-Bundle: `GO`
- Android Play Store Internal Upload: `GO` fuer versionCode `10026`; Production-Rollout bleibt manuell gesperrt
- Functions Deploy: `GO` fuer die zuletzt geaenderten Shopify-/Meta-relevanten Pfade; Gesamtbetrieb weiter mit Monitoring/Legal-Gates
- Oeffentlicher Gesamt-Release: `machbar`, aber nicht risikofrei ohne die offenen Ops-/Compliance-Gates und echten Device-Smoke

## 7) Empfohlene naechste Schritte

1. Production Firestore Config + Functions Secrets vor Store-Freigabe nochmals live gegenpruefen.
2. App Store Connect / Google Play Console Build-Auswahl und Testtracks final mappen.
3. Vendor dSYMs fuer den iOS Release-Pfad nachziehen, falls Crash-Symbolication sauber sein soll.
4. Letzten End-to-End Smoke-Test auf echten Geraeten fuer Checkout, KI, Video, Upload und Account-Delete dokumentieren.
