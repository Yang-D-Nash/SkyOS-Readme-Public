# Skydown x 22

Skydown x 22 is a cross-platform music and creator app built for the Skydown / 22 ecosystem.
It brings together artist discovery, Spotify-powered listening, social touchpoints, producer services, beat listening, merch and in-app AI in one mobile product across iOS and Android.

## Product Highlights

- artist selection and music discovery
- Spotify connection with in-app previews and premium handoff
- Instagram links for artists, Skydown and 22
- `NICMA MUSIC` producer area for services and inquiries
- `Beat Hub` for listening and admin-managed uploads
- merch, cart and order flows
- bot and agent features inside the app

## Built With

### Client

- `SwiftUI` for the iOS app UI
- `Jetpack Compose` for the Android app UI
- `Kotlin Multiplatform` for shared models and business logic

### Music And Media

- `Spotify Web API + OAuth` for music loading and Spotify connection
- `AVFoundation` for iOS preview playback
- `ExoPlayer / Media3` for Android preview playback

### Backend And Data

- `Firebase Auth` for account and sign-in flows
- `Cloud Firestore` as the main cloud database
- `Firebase Storage` for media and uploads
- `Firebase Functions` for backend logic and agent endpoints
- `Firebase App Distribution` for internal tester releases
- `Firebase AI / Gemini` for in-app AI features

## Repository Structure

- `Skydown App/` for the iOS source, SwiftUI views, services and view models
- `androidApp/` for the Android source, Compose screens and integrations
- `shared/` for shared Kotlin Multiplatform code

## Platform Setup

- iOS bundle identifier: `com.skydown.ios`
- Android package name: `com.skydown.android`
- iOS Firebase config: `Skydown App/GoogleService-Info.plist`
- Android Firebase config: `androidApp/google-services.json`

## Development Notes

- open the repository root in Android Studio, not only `androidApp/`
- Firebase App Distribution tester lists stay local and should not be committed

## Shopify Merch Flow

The app keeps its own cart, checkout and payment flow.
Shopify is used as the external merch catalog + fulfillment bridge to PODpartner.

### Architecture

- `PODpartner` handles production, shipping and tracking
- `Shopify` stores products and variants and receives external orders
- `Firestore` stores app visibility, featured state, sort order and order metadata
- the app calculates the customer-facing shipping price itself
- Cloud Functions create the Shopify order after payment is confirmed

### Required Firebase Functions Secret

Set the Shopify Admin token before deploying the merch flow:

```bash
firebase functions:secrets:set SHOPIFY_ADMIN_ACCESS_TOKEN
```

Optional environment override:

- `SHOPIFY_STORE_DOMAIN`
  default in code: `k5t1sc-ps.myshopify.com`

### Merch Sync

- callable function: `syncShopifyMerch`
- admin-triggered from the merch admin UI
- sync updates Shopify title, description, images, variants, prices and availability
- app-specific fields stay intact:
  - `isVisibleInApp`
  - `featured`
  - `sortOrder`
  - `customBadge`
  - `customImageOverride`

### Payment Confirmation

After an external payment succeeds, confirm the order in one of two ways:

- admin UI button in the order queue
- callable function: `confirmMerchOrderPayment`

Payload shape:

```json
{
  "orderId": "firestore-order-id",
  "paymentMethod": "PayPal",
  "paymentReference": "optional-external-reference"
}
```

Once `paymentStatus` becomes `confirmed`, the backend submits PODpartner-bound merch orders to Shopify through GraphQL `orderCreate`.
