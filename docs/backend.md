# SkyOS Backend

SkyOS uses Firebase as the backend authority layer. The mobile clients are rich and native, but
the system does not trust clients with the final say on billing, role changes, AI execution,
legal content, or owner-grade operations.

## 1. Backend Stack

| Layer | Role in SkyOS |
| --- | --- |
| Firebase Auth | Identity, session, and custom claims |
| Firestore | Product state, user data, admin config, legal settings, commerce data, operations data |
| Cloud Storage | Profile images, gallery media, and asset uploads with upload-slot validation |
| Cloud Functions | Privileged writes, AI execution, payment handling, runtime controls, background jobs |
| App Check | Abuse reduction and client integrity hardening |
| Pub/Sub-triggered Functions | Budget and runtime response automation |
| Firebase Secret Manager | Sensitive provider credentials and payment secrets |

## 2. Important Collections and Documents

These are examples of high-value backend surfaces. The repository contains more documents and
collections than listed here.

| Path | Purpose |
| --- | --- |
| `users/{uid}` | canonical user state, role, quota plan, AI consent, limits, and claims sync metadata |
| `userProfiles/{uid}` | profile presentation data |
| `galleryMeta/*` | gallery asset metadata |
| `orders/*` | merch order records and owner workflow data |
| `system/runtimeConfig` | platform-wide runtime switches for lockdown and write control |
| `adminConfig/aiPromptSettings` | owner-governed AI prompt and runtime settings |
| `adminConfig/automationN8n` and per-user variants | automation routing configuration |
| `adminConfig/shopifyMerchPrivate` | private commerce configuration |
| `appConfig/shopifyMerch` | public Shopify storefront configuration |
| `adminConfig/stripeCheckoutSecrets` | Stripe secret status metadata |
| `uploadSlots/*` | upload approvals for Storage writes |
| `uploadUsage/*` | upload rate and hygiene tracking |
| `agentExternalBridgeAudit/*` | audit trail for external agent bridge activity |

## 3. High-Value Cloud Functions

The backend exposes many callables and triggers. These are the functions that shape product trust.

| Function | Why it exists |
| --- | --- |
| `generateAiText` | text assistant execution |
| `generateAiVisual` | visual generation execution |
| `skydownAgent` | agent-style workflow execution |
| `authorizeAiUsage` / `reconcileAiUsageCost` | usage control and cost reconciliation |
| `getAiFaqOwnerIntelligence` | owner review loop for FAQ quality and support insight |
| `getAiMembershipDashboard` and related ops callables | revenue and membership operations |
| `startAiSubscriptionCheckout` | hosted subscription checkout setup when web checkout is enabled |
| `syncIosAiSubscriptionStatus` / `syncAndroidAiSubscriptionStatus` | store-backed membership sync |
| `submitMerchOrder` / `startMerchCheckout` | server-owned order creation and hosted checkout preparation |
| `stripeMerchWebhook` | hosted checkout payment confirmation path |
| `confirmMerchOrderPayment` | owner/admin payment confirmation path |
| `syncShopifyMerch` / `listShopifyCollections` | owner-managed commerce sync |
| `setUserRole` / `syncCurrentUserClaims` | role and claim lifecycle |
| `requestUploadSlot` | secure upload slot issuance for Storage writes |
| `setRuntimeLockdown` | manual incident response and platform control |
| `deleteCurrentUserAccount` | controlled account removal flow |

## 4. Auth, Roles, and Quotas

Role and quota logic is enforced in more than one place:

- owner identity is pinned to a fixed owner email in backend security constants
- Functions set custom claims for `owner`, `admin`, `subadmin`, and `user`
- default quota plans are derived from role and subscription state
- users carry AI usage limits and history-retention values in backend documents

Important operating rule: role visibility in the app is only one layer. Rules and callables must
still block unauthorized behavior.

## 5. Runtime Controls and Kill Switches

`system/runtimeConfig` is the most important live safety surface in the backend. It can govern:

- `lockdown`
- `uploadsEnabled`
- `registrationsEnabled`
- `userWritesEnabled`
- `appCheckMode`
- `appCheckAiSoftFailEnabled`
- `budgetLockdownEnabled`
- `lastLockdownReason`

This enables controlled rollback and incident response without shipping a new binary first.

## 6. Commerce and Payment Backends

SkyOS separates storefront display from payment authority.

- catalog and collection setup can be sourced from Shopify config
- order creation runs through Functions, not raw client writes
- hosted checkout can use Stripe and Klarna when configured
- iOS and Android AI memberships have dedicated subscription sync paths
- payment and order confirmation updates happen server-side

Do not enable live payment methods until provider credentials, store products, webhook validation,
support flows, and legal copy are all aligned.

## 7. Storage and Upload Security

Storage writes are not open-ended. The system uses:

- upload slots
- owner UID checks
- file name constraints
- content-type allowlists
- byte-size limits
- runtime upload lock checks

This is one of the stronger trust signals in the repo and should stay strict.

## 8. Rules Philosophy

Firestore and Storage rules are deny-by-default enough to matter. They are designed to protect:

- owner/admin documents
- legal content settings
- role management
- orders
- uploads
- media management
- personal automation configuration

If new collections are introduced, rules should be updated before those surfaces are treated as release-ready.

## 9. Testing and Deployment

Use these commands from the repo root:

```bash
npm ci --prefix functions
npm test --prefix functions
firebase deploy --only functions
firebase deploy --only firestore:rules,storage
```

Selective deploys are appropriate for lower-risk backend changes:

```bash
firebase deploy --only functions:syncShopifyMerch,functions:startAiSubscriptionCheckout
```

Deployment discipline, rollback basics, and smoke validation live in [deployment.md](deployment.md)
and [release-checklist.md](release-checklist.md).
