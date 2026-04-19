"use strict";

const crypto = require("node:crypto");

const STRIPE_API_BASE = "https://api.stripe.com/v1";
const STRIPE_REGION = "us-central1";
const STRIPE_WEBHOOK_TOLERANCE_SECONDS = 300;

const HOSTED_CHECKOUT_METHODS = Object.freeze({
  Stripe: "card",
  Klarna: "klarna",
});

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function normalizeHostedCheckoutMethod(value) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (normalized === "stripe") {
    return "Stripe";
  }
  if (normalized === "klarna") {
    return "Klarna";
  }
  return null;
}

function isHostedCheckoutMethod(value) {
  return normalizeHostedCheckoutMethod(value) !== null;
}

function normalizeCheckoutPlatform(value) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  return ["ios", "android"].includes(normalized) ? normalized : "ios";
}

function toStripeAmount(value, label = "amount") {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed < 0) {
    throw new Error(`${label} ist ungueltig.`);
  }
  return Math.round(parsed * 100);
}

function addStripeLineItem(params, index, item) {
  params.append(`line_items[${index}][quantity]`, `${item.quantity}`);
  params.append(`line_items[${index}][price_data][currency]`, item.currency);
  params.append(`line_items[${index}][price_data][unit_amount]`, `${item.unitAmount}`);
  params.append(`line_items[${index}][price_data][product_data][name]`, item.name);

  if (item.description) {
    params.append(`line_items[${index}][price_data][product_data][description]`, item.description);
  }
}

function buildStripeLineItems(orderData) {
  const items = Array.isArray(orderData?.items) ? orderData.items : [];
  const currency = nonEmptyString(orderData?.currency) || "EUR";
  const lineItems = items.map((item) => {
    const name = nonEmptyString(item?.name) || "Merch Item";
    const quantity = Number(item?.quantity || 0);
    if (!Number.isInteger(quantity) || quantity <= 0) {
      throw new Error(`Menge fuer ${name} ist ungueltig.`);
    }

    const descriptionParts = [];
    if (nonEmptyString(item?.size)) {
      descriptionParts.push(`Groesse: ${item.size}`);
    }
    if (nonEmptyString(item?.color)) {
      descriptionParts.push(`Farbe: ${item.color}`);
    }

    return {
      name,
      quantity,
      currency: currency.toLowerCase(),
      unitAmount: toStripeAmount(item?.unitPrice, `unitPrice fuer ${name}`),
      description: descriptionParts.join(" | "),
    };
  });

  const shippingAmount = Number(orderData?.shippingAmount || 0);
  if (shippingAmount > 0) {
    lineItems.push({
      name: `Versand ${nonEmptyString(orderData?.shippingZone) || ""}`.trim(),
      quantity: 1,
      currency: currency.toLowerCase(),
      unitAmount: toStripeAmount(shippingAmount, "shippingAmount"),
      description: nonEmptyString(orderData?.shippingCountryCode) || "",
    });
  }

  if (lineItems.length === 0) {
    throw new Error("Es wurden keine Stripe-Line-Items gefunden.");
  }

  return lineItems;
}

function buildReturnPageUrl({projectId, platform, status, orderId}) {
  const url = new URL(`https://${STRIPE_REGION}-${projectId}.cloudfunctions.net/stripeCheckoutReturn`);
  url.searchParams.set("platform", normalizeCheckoutPlatform(platform));
  url.searchParams.set("status", status === "cancel" ? "cancel" : "success");
  if (orderId) {
    url.searchParams.set("orderId", orderId);
  }
  url.searchParams.set("session_id", "{CHECKOUT_SESSION_ID}");
  return url.toString();
}

function buildDeepLink({platform, status, orderId, sessionId}) {
  const scheme = normalizeCheckoutPlatform(platform) === "android" ? "com.skydown.android" : "skydown";
  const host = status === "cancel" ? "checkout-cancel" : "checkout-success";
  const url = new URL(`${scheme}://${host}`);
  if (orderId) {
    url.searchParams.set("orderId", orderId);
  }
  if (sessionId) {
    url.searchParams.set("sessionId", sessionId);
  }
  return url.toString();
}

async function stripeApiRequest({path, secretKey, params}) {
  const response = await fetch(`${STRIPE_API_BASE}/${path}`, {
    method: "POST",
    headers: {
      "Authorization": `Bearer ${secretKey}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: params.toString(),
  });

  const payload = await response.json().catch(async () => ({
    error: {
      message: await response.text().catch(() => "Stripe-Antwort konnte nicht gelesen werden."),
    },
  }));

  if (!response.ok) {
    const message = nonEmptyString(payload?.error?.message) || "Stripe Checkout konnte nicht erstellt werden.";
    throw new Error(message);
  }

  return payload;
}

async function createHostedCheckoutSession({
  secretKey,
  projectId,
  orderId,
  orderData,
  paymentMethod,
  platform,
}) {
  const normalizedMethod = normalizeHostedCheckoutMethod(paymentMethod);
  if (!normalizedMethod) {
    throw new Error("Fuer diesen Payment-Provider gibt es keinen Hosted Checkout.");
  }

  const lineItems = buildStripeLineItems(orderData);
  const params = new URLSearchParams();
  params.append("mode", "payment");
  params.append("client_reference_id", orderId);
  params.append("submit_type", "pay");
  params.append("locale", "de");
  params.append("billing_address_collection", "required");
  params.append("phone_number_collection[enabled]", "true");
  params.append("success_url", buildReturnPageUrl({
    projectId,
    platform,
    status: "success",
    orderId,
  }));
  params.append("cancel_url", buildReturnPageUrl({
    projectId,
    platform,
    status: "cancel",
    orderId,
  }));

  const customerEmail = nonEmptyString(orderData?.customerEmail) || nonEmptyString(orderData?.userEmail);
  if (customerEmail) {
    params.append("customer_email", customerEmail);
  }

  const stripePaymentMethod = HOSTED_CHECKOUT_METHODS[normalizedMethod];
  params.append("payment_method_types[0]", stripePaymentMethod);

  if (normalizedMethod === "Klarna") {
    params.append("payment_method_options[klarna][preferred_locale]", "de-DE");
  }

  const shippingCountryCode = nonEmptyString(orderData?.shippingCountryCode)?.toUpperCase();
  if (shippingCountryCode) {
    params.append("shipping_address_collection[allowed_countries][0]", shippingCountryCode);
  }

  params.append("metadata[orderId]", orderId);
  params.append("metadata[paymentMethod]", normalizedMethod);
  params.append("metadata[fulfillmentProvider]", nonEmptyString(orderData?.fulfillmentProvider) || "");
  params.append("payment_intent_data[metadata][orderId]", orderId);
  params.append("payment_intent_data[metadata][paymentMethod]", normalizedMethod);

  lineItems.forEach((item, index) => addStripeLineItem(params, index, item));

  const session = await stripeApiRequest({
    path: "checkout/sessions",
    secretKey,
    params,
  });

  const checkoutUrl = nonEmptyString(session?.url);
  const sessionId = nonEmptyString(session?.id);
  if (!checkoutUrl || !sessionId) {
    throw new Error("Stripe hat keine Checkout-Session zurückgegeben.");
  }

  return {
    sessionId,
    checkoutUrl,
    stripeCheckoutStatus: nonEmptyString(session?.status) || "open",
    expiresAtEpochSeconds: Number(session?.expires_at || 0) || null,
  };
}

async function createAiSubscriptionCheckoutSession({
  secretKey,
  projectId,
  userId,
  customerEmail,
  customerId = null,
  plan,
  priceId,
  platform,
}) {
  const normalizedUserId = nonEmptyString(userId);
  if (!normalizedUserId) {
    throw new Error("userId fehlt fuer den Abo-Checkout.");
  }

  const normalizedPlan = nonEmptyString(plan);
  if (!normalizedPlan) {
    throw new Error("Abo-Plan fehlt fuer den Checkout.");
  }

  const normalizedPriceId = nonEmptyString(priceId);
  if (!normalizedPriceId) {
    throw new Error("Price-ID fuer den gewaehlten Abo-Plan fehlt.");
  }

  const params = new URLSearchParams();
  params.append("mode", "subscription");
  params.append("client_reference_id", normalizedUserId);
  params.append("locale", "de");
  params.append("origin_context", "mobile_app");
  params.append("billing_address_collection", "required");
  params.append("allow_promotion_codes", "true");
  params.append("success_url", buildReturnPageUrl({
    projectId,
    platform,
    status: "success",
    orderId: null,
  }));
  params.append("cancel_url", buildReturnPageUrl({
    projectId,
    platform,
    status: "cancel",
    orderId: null,
  }));
  params.append("line_items[0][price]", normalizedPriceId);
  params.append("line_items[0][quantity]", "1");
  params.append("payment_method_types[0]", "card");

  const normalizedCustomerId = nonEmptyString(customerId);
  if (normalizedCustomerId) {
    params.append("customer", normalizedCustomerId);
  } else if (nonEmptyString(customerEmail)) {
    params.append("customer_email", customerEmail);
  }

  params.append("metadata[type]", "ai_subscription");
  params.append("metadata[userId]", normalizedUserId);
  params.append("metadata[plan]", normalizedPlan);
  params.append("metadata[priceId]", normalizedPriceId);
  params.append("subscription_data[metadata][type]", "ai_subscription");
  params.append("subscription_data[metadata][userId]", normalizedUserId);
  params.append("subscription_data[metadata][plan]", normalizedPlan);
  params.append("subscription_data[metadata][priceId]", normalizedPriceId);

  const session = await stripeApiRequest({
    path: "checkout/sessions",
    secretKey,
    params,
  });

  const checkoutUrl = nonEmptyString(session?.url);
  const sessionId = nonEmptyString(session?.id);
  if (!checkoutUrl || !sessionId) {
    throw new Error("Stripe hat keine Abo-Checkout-Session zurueckgegeben.");
  }

  return {
    sessionId,
    checkoutUrl,
    stripeCheckoutStatus: nonEmptyString(session?.status) || "open",
    expiresAtEpochSeconds: Number(session?.expires_at || 0) || null,
    customerId: nonEmptyString(session?.customer),
    subscriptionId: nonEmptyString(session?.subscription),
  };
}

function parseStripeSignature(signatureHeader) {
  const parts = `${signatureHeader || ""}`.split(",").map((part) => part.trim()).filter(Boolean);
  let timestamp = null;
  const signatures = [];

  for (const part of parts) {
    const [key, value] = part.split("=", 2);
    if (key === "t") {
      timestamp = Number(value);
    } else if (key === "v1" && value) {
      signatures.push(value);
    }
  }

  return {
    timestamp,
    signatures,
  };
}

function verifyStripeWebhookSignature({payloadBuffer, signatureHeader, endpointSecret}) {
  const {timestamp, signatures} = parseStripeSignature(signatureHeader);
  if (!timestamp || signatures.length === 0) {
    throw new Error("Stripe-Signatur fehlt.");
  }

  const ageSeconds = Math.abs(Math.floor(Date.now() / 1000) - timestamp);
  if (ageSeconds > STRIPE_WEBHOOK_TOLERANCE_SECONDS) {
    throw new Error("Stripe-Signatur ist abgelaufen.");
  }

  const signedPayload = `${timestamp}.${payloadBuffer.toString("utf8")}`;
  const expected = crypto
    .createHmac("sha256", endpointSecret)
    .update(signedPayload, "utf8")
    .digest("hex");

  const expectedBuffer = Buffer.from(expected, "utf8");
  const isValid = signatures.some((signature) => {
    const candidate = Buffer.from(signature, "utf8");
    return candidate.length === expectedBuffer.length &&
      crypto.timingSafeEqual(candidate, expectedBuffer);
  });

  if (!isValid) {
    throw new Error("Stripe-Signatur ist ungueltig.");
  }
}

function shouldConfirmPaymentFromStripeEvent(eventType, session) {
  if (eventType === "checkout.session.async_payment_succeeded") {
    return true;
  }

  return eventType === "checkout.session.completed" &&
    nonEmptyString(session?.payment_status)?.toLowerCase() === "paid";
}

function deriveStripeCheckoutStatus(eventType, session) {
  if (eventType === "checkout.session.async_payment_failed") {
    return "failed";
  }
  if (eventType === "checkout.session.expired") {
    return "expired";
  }
  if (eventType === "checkout.session.async_payment_succeeded") {
    return "paid";
  }

  return nonEmptyString(session?.status) ||
    nonEmptyString(session?.payment_status) ||
    "open";
}

function extractStripeCheckoutIdentifiers(session) {
  return {
    sessionId: nonEmptyString(session?.id),
    paymentIntentId: nonEmptyString(session?.payment_intent),
    paymentStatus: nonEmptyString(session?.payment_status),
  };
}

function formatStripeShippingAddress(shippingDetails) {
  const address = shippingDetails?.address || {};
  const address1 = nonEmptyString(address.line1);
  const city = nonEmptyString(address.city);
  const zip = nonEmptyString(address.postal_code);
  const countryCode = nonEmptyString(address.country)?.toUpperCase();

  if (!address1 || !city || !zip || !countryCode) {
    return null;
  }

  const address2 = nonEmptyString(address.line2) || "";
  const countryName = nonEmptyString(address.country) || countryCode;
  const lines = [
    address1,
    address2,
    `${zip} ${city}`.trim(),
    countryName,
  ].filter(Boolean);

  return {
    shippingAddress: lines.join("\n"),
    shippingAddressData: {
      address1,
      address2,
      city,
      zip,
      countryCode,
      countryName,
    },
    shippingCountryCode: countryCode,
  };
}

function renderCheckoutReturnPage({platform, status, orderId, sessionId}) {
  const deepLink = buildDeepLink({platform, status, orderId, sessionId});
  const title = status === "cancel" ? "Checkout abgebrochen" : "Checkout abgeschlossen";
  const body = status === "cancel" ?
    "Du kannst jetzt direkt zur App zurueckspringen und spaeter erneut bezahlen." :
    "Die App wird jetzt wieder geoeffnet. Die Zahlung wird im Hintergrund synchronisiert.";

  return `<!doctype html>
<html lang="de">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${title}</title>
    <style>
      :root {
        color-scheme: dark;
        --bg: #07111d;
        --panel: #101f2e;
        --text: #f4f8fb;
        --muted: #a7b6c8;
        --accent: #5d7795;
      }
      body {
        margin: 0;
        min-height: 100vh;
        display: grid;
        place-items: center;
        background: radial-gradient(circle at top, rgba(93,119,149,0.28), transparent 45%), var(--bg);
        color: var(--text);
        font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      }
      main {
        width: min(92vw, 460px);
        padding: 28px 24px;
        border-radius: 24px;
        background: rgba(16, 31, 46, 0.92);
        box-shadow: 0 18px 48px rgba(0, 0, 0, 0.28);
      }
      h1 {
        margin: 0 0 12px;
        font-size: 1.5rem;
      }
      p {
        margin: 0 0 18px;
        color: var(--muted);
        line-height: 1.5;
      }
      a {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-height: 48px;
        padding: 0 18px;
        border-radius: 16px;
        background: var(--accent);
        color: white;
        text-decoration: none;
        font-weight: 700;
      }
    </style>
  </head>
  <body>
    <main>
      <h1>${title}</h1>
      <p>${body}</p>
      <a id="return-link" href="${deepLink}">Zur App zurueck</a>
    </main>
    <script>
      const deepLink = ${JSON.stringify(deepLink)};
      window.setTimeout(() => {
        window.location.href = deepLink;
      }, 120);
    </script>
  </body>
</html>`;
}

module.exports = {
  createAiSubscriptionCheckoutSession,
  createHostedCheckoutSession,
  deriveStripeCheckoutStatus,
  extractStripeCheckoutIdentifiers,
  formatStripeShippingAddress,
  HOSTED_CHECKOUT_METHODS,
  isHostedCheckoutMethod,
  normalizeCheckoutPlatform,
  normalizeHostedCheckoutMethod,
  renderCheckoutReturnPage,
  shouldConfirmPaymentFromStripeEvent,
  verifyStripeWebhookSignature,
};
