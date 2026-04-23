# SkyOS Commerce

SkyOS treats commerce as part of the product system, not as a disconnected store tab. Merch,
checkout, orders, shipping, payment rails, and membership monetization all need to feel aligned
with the same trust standard.

## 1. Commerce Scope

SkyOS currently covers:

- merch catalog and discovery
- cart management
- hosted checkout preparation
- order creation and confirmation
- shipping and invoice configuration
- membership monetization for AI capability tiers

## 2. Catalog and Merch Data

The repo includes two important commerce patterns:

- owner-managed sync from Shopify through Cloud Functions
- public storefront loading from Shopify-backed configuration when catalog fallback is needed

This allows SkyOS to present live merch while keeping owner control over which collections and
products become visible in-app.

## 3. Checkout Model

Merch checkout is server-led:

1. Client prepares cart state.
2. Function validates payload and creates the order shell.
3. Hosted checkout is prepared when the selected method requires it.
4. Payment confirmation updates the order through server-side logic or owner/admin action.
5. Order state becomes visible back in the app.

Where enabled, hosted payment methods can include:

- Stripe
- Klarna via Stripe

Do not market a payment method as live until provider setup, webhook validation, and order support flows are confirmed.

## 4. Orders and Fulfillment

Orders are not just receipts. The system is designed to support:

- user-visible order history
- owner visibility into global orders
- payment confirmation workflows
- fulfillment preparation after confirmation

Normal users should only ever see their own orders and should not inherit owner lifecycle controls.

## 5. Membership and Subscription Monetization

SkyOS also monetizes through AI capability memberships. The repo includes:

- native subscription sync paths for iOS and Android
- hosted subscription checkout support where configured
- plan-aware gating in AI surfaces
- restore visibility from Settings and AI-adjacent flows

The product language should continue to treat this as capability-based access, not token consumption.

## 6. Shipping, Invoicing, and Support

Commerce settings include support for:

- domestic, EU, and international shipping cost logic
- free-shipping thresholds
- invoice metadata such as company name and tax-related fields
- support email alignment

These settings are operationally important and should be owner-controlled, not scattered across client copy.

## 7. Trust Rules For Commerce

Before public launch:

- live payment methods must be explicitly verified
- subscription restore must work on live store paths
- checkout return pages and redirects must be readable
- support path must be visible for billing and order issues
- legal copy must match the actual active provider setup

Commerce that looks premium but cannot be explained by support is not release-ready.
