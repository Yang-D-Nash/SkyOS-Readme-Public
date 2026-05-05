# AI Subscription Rollout Brief

Stand: 2026-05-05

## Ziel

### Release-Sicherheitslinie fuer die naechsten 2 Wochen

- Die mobile App sollte bis zum Release keine externen KI-Abo-Kaufwege innerhalb der App anbieten.
- Fuer iOS ist ohne passende StoreKit-Entitlements oder In-App Purchase ein externer Abo-Kaufpfad im App-Review riskant.
- Fuer Google Play ist ein externer Kaufpfad fuer digitale Creator-/Studio-Funktionen ohne passendes Billing-Programm ebenfalls riskant.
- Sicherer Release-Pfad:
  - KI-Abo in den mobilen Apps nur als Status darstellen
  - keine mobile Stripe-Aktivierung fuer digitale Features
  - Stripe/Web nur ausserhalb der mobilen App oder spaeter als separater Web-Kanal
  - nativen Rollout ueber `StoreKit` auf iOS und `Play Billing` auf Android nachziehen

Skydown soll KI-Nutzung sauber monetarisieren, ohne die bestehende User Experience oder die Store-Freigabe zu gefaehrden. Die Basis dafuer ist bereits im Projekt vorhanden: Nutzerrollen, Quota-Plaene, Stripe-Checkout, Webhooks und serverseitige Limit-Umschaltung.

## Was heute schon existiert

- Bezahlte KI-Plaene sind bereits im Backend angelegt:
  - `creator`
  - `studio`
- Die serverseitigen Standardlimits sind bereits definiert:
  - `creator`: `120 Text / 20 Visual / 70 Agent / 7 Tage Historie`
  - `studio`: `240 Text / 40 Visual / 140 Agent / 30 Tage Historie`
- Ein Stripe-Abo-Checkout ist bereits vorhanden:
  - Callable: `startAiSubscriptionCheckout`
  - Stripe Checkout Session mit `mode=subscription`
  - Webhook-Verarbeitung fuer `checkout.session.*` und `customer.subscription.*`
- Nach erfolgreicher Zahlung werden Quotas bereits automatisch auf den passenden Plan umgestellt.
- Der mobile Checkout-Pfad ist fuer den Release-Schutz derzeit bewusst deaktiviert.

## Wichtige Ist-Zustaende im Code

### Backend

- AI-Planlogik: `functions/index.js`
- Stripe-Abo-Checkout: `functions/src/payments/stripe-checkout.js`
- Payment-Settings mit `creatorPriceId` und `studioPriceId`: `functions/index.js`

### iOS

- Status-UI fuer den aktuellen KI-Plan ist in den Settings vorhanden:
  - `Skydown App/Views/Settings/SettingsView.swift`
- Der mobile Checkout wird serverseitig und im UI fuer den Release aktuell nicht angeboten.
- Native Billing-Integration fehlt noch:
  - `StoreKit` / In-App Purchase fuer iOS ist noch nicht eingebaut.

### Android

- Es gibt aktuell keinen gleichwertigen sichtbaren KI-Abo-Checkout-Flow fuer Android.
- Native Billing-Integration fehlt auch hier:
  - `Play Billing` ist noch nicht eingebaut.

## Produktvorschlag

### Empfohlene Pläne

#### Creator

- Zielgruppe: normale User mit regelmaessiger KI-Nutzung
- Limits:
  - `120 Text`
  - `20 Visual`
  - `70 Agent`
- Historie: `7 Tage`

#### Studio

- Zielgruppe: Power-User, Artists, internes Team, Creator mit hohem Bedarf
- Limits:
  - `240 Text`
  - `40 Visual`
  - `140 Agent`
- Historie: `30 Tage`

## Preislogik

Die Visual-Anfragen sind der wesentliche Kostentreiber. Text und Agent sind deutlich guenstiger.

Grobe interne Kosten bei Vollauslastung:

- `Creator`: grob im Bereich von `unter 1 USD pro Tag`
- `Studio`: grob im Bereich von `1 bis 2 USD pro Tag`

Deshalb sollte der Planpreis nicht aus dem Text- oder Agent-Verbrauch, sondern vor allem aus dem Visual-Budget her gedacht werden.

### Praktischer Preisstart

- `Creator`: `9.99 EUR / Monat`
- `Studio`: `19.99 EUR / Monat`

Das ist bewusst defensiv. Die Preise koennen spaeter ueber echte Nutzungsdaten nachgezogen werden.

## Store- und Plattformstrategie

## iPhone

Fuer digitale Features und digitale Subscriptions gelten Apple-Regeln.

Wichtiger Stand:

- Fuer eine weltweit robuste App-Store-Strategie ist `StoreKit` der sauberste Weg.
- Ein reiner Stripe-App-zu-Web-Flow ist fuer einen globalen App-Store-Release deutlich riskanter als ein nativer Billing-Flow.

### Empfehlung fuer iPhone

- Kurzfristig:
  - in der mobilen App keinen externen KI-Abo-Kaufweg zeigen
  - nur Status und vorhandene Entitlements anzeigen
- Mittelfristig:
  - `StoreKit` fuer iOS-Abos einfuehren
  - danach im Backend weiterhin auf dieselben `creator`/`studio`-Quotas mappen

## Android

Google Play ist bei digitalen Guetern und Subscriptions weiterhin deutlich strenger.

### Empfehlung fuer Android

- Wenn die Android-App regulär ueber Google Play verteilt wird:
  - fuer In-App-Digital-Abos `Play Billing` einplanen
- Bis dahin:
  - keinen externen In-App-Kaufweg fuer digitale KI-Features ueber die mobile App freigeben

## Empfohlene Rollout-Reihenfolge

### Phase 1: Aktivierbarer Soft Launch

1. Stripe-Price-IDs fuer `Creator` und `Studio` final setzen.
2. Payment-Settings und Webhooks fuer spaeteren Rollout vorbereiten.
3. Mobile App nur mit Statusanzeige fuer bestehende KI-Plaene ausliefern.
4. Testuser serverseitig oder ueber einen separaten Web-Kanal auf `creator` und `studio` heben.
5. Kosten und Quota-Verbrauch beobachten.

### Phase 2: Produkt sauber machen

1. Natives Billing fuer iOS und Android einbauen.
2. Abo-Einstieg als klarer Screen:
   - Planvergleich
   - Preis
   - enthaltene Nutzung
   - Status
   - Upgrade / Downgrade / Verwalten
3. Upgrade, Downgrade und Abo-Verwaltung sauber anbinden.

### Phase 3: Store-sauber absichern

1. iOS auf `StoreKit` umstellen.
2. Android auf `Play Billing` umstellen.
3. Stripe als Web-/Fallback- oder Non-Store-Kanal behalten.

## Technische To-dos

### Hoch priorisiert

- `SettingsView.swift`
  - nur Status anzeigen, kein externer mobiler Kaufweg
- Android
  - `Play Billing` integrieren
- iOS
  - `StoreKit` / In-App Purchase integrieren
- Subscription Management
  - Status anzeigen
  - aktiven Plan anzeigen
  - Verlängerung / Kuendigung / Ablauf anzeigen

### Mittel priorisiert

- Stripe Checkout Session fuer Mobile verbessern
  - `origin_context = mobile_app`
- Success- und Cancel-Rueckwege sauber fuer beide Plattformen pruefen
- Fehlertexte fuer fehlgeschlagene Checkouts nutzerfreundlicher machen

### Spaeter

- Downgrade-Logik
- Grace-Period / Past-Due UX
- Upgrade innerhalb des laufenden Abos
- Nutzungsauswertung pro Plan

## Risiken

### Produkt

- Wenn die Limits zu hoch sind, kann Visual-Kostenlast zu stark steigen.
- Wenn die Limits zu niedrig sind, fuehlt sich das Abo wertlos an.

### Plattform

- iOS- und Android-Store-Policies koennen den externen Abo-Flow begrenzen.
- Ein weltweit einheitlicher Stripe-In-App-Flow ist fuer digitale App-Features nicht die sicherste Default-Strategie.

### UX

- Nutzer muessen immer verstehen:
  - welcher Plan aktiv ist
  - wie viel noch frei ist
  - wie sie wieder zur App zurueckkommen
  - wie sie kuendigen oder verwalten

## Mein klares Vorgehen fuer Skydown

1. Die vorhandene Stripe-Logik als internen Soft-Launch nutzen.
2. Creator und Studio preislich live vorbereiten.
3. Die bestehende iOS-Admin-Beschraenkung entfernen.
4. Danach einen echten Abo-Screen fuer User bauen.
5. Parallel entscheiden:
   - `StoreKit + Play Billing` als sauberer Langzeitweg
   - oder Stripe nur als Web-/Sonderkanal

## Kurzbriefing fuer dich

Die gute Nachricht: Wir sind nicht bei null. Das Backend ist dafuer schon weitgehend da. Was fehlt, ist vor allem der saubere Produkt- und Plattformabschluss.

Der schnellste realistische Weg ist:

- Stripe-Preise setzen
- vorhandenen Checkout intern testen
- User-facing Einstieg freigeben
- danach Plattform-konform haerten

Der strategisch sauberste Weg ist:

- dieselben Quota-Plaene beibehalten
- aber iOS ueber `StoreKit` und Android ueber `Play Billing` monetarisieren

So bleibt das Datenmodell stabil und nur der Kaufkanal aendert sich.
