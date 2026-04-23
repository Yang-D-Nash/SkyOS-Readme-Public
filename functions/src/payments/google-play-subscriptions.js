"use strict";

const {GoogleAuth} = require("google-auth-library");

const GOOGLE_PLAY_SCOPE = "https://www.googleapis.com/auth/androidpublisher";
const GOOGLE_PLAY_API_BASE_URL = "https://androidpublisher.googleapis.com/androidpublisher/v3";

const googlePlayAuth = new GoogleAuth({
  scopes: [GOOGLE_PLAY_SCOPE],
});

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

async function getAccessToken() {
  const client = await googlePlayAuth.getClient();
  const tokenResponse = await client.getAccessToken();
  const accessToken = typeof tokenResponse === "string" ?
    tokenResponse :
    tokenResponse?.token;

  if (!accessToken) {
    throw new Error("Google-Play-Access-Token fehlt.");
  }

  return accessToken;
}

async function googlePlayRequest(path, {
  method = "GET",
  body = null,
} = {}) {
  const accessToken = await getAccessToken();
  const response = await fetch(`${GOOGLE_PLAY_API_BASE_URL}/${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (!response.ok) {
    const errorPayload = await response.json().catch(async () => ({
      error: {
        message: await response.text().catch(() => "Google Play API Fehler."),
      },
    }));
    const message = nonEmptyString(errorPayload?.error?.message) || "Google Play API Fehler.";
    const error = new Error(message);
    error.status = response.status;
    error.payload = errorPayload;
    throw error;
  }

  if (response.status === 204) {
    return {};
  }

  return response.json().catch(() => ({}));
}

async function fetchGooglePlaySubscriptionPurchase({
  packageName,
  purchaseToken,
}) {
  const normalizedPackageName = nonEmptyString(packageName);
  const normalizedPurchaseToken = nonEmptyString(purchaseToken);
  if (!normalizedPackageName || !normalizedPurchaseToken) {
    throw new Error("packageName oder purchaseToken fehlt fuer die Play-Store-Pruefung.");
  }

  return googlePlayRequest(
      `applications/${encodeURIComponent(normalizedPackageName)}` +
      `/purchases/subscriptionsv2/tokens/${encodeURIComponent(normalizedPurchaseToken)}`,
  );
}

async function acknowledgeGooglePlaySubscriptionPurchase({
  packageName,
  subscriptionId,
  purchaseToken,
  developerPayload = "",
}) {
  const normalizedPackageName = nonEmptyString(packageName);
  const normalizedSubscriptionId = nonEmptyString(subscriptionId);
  const normalizedPurchaseToken = nonEmptyString(purchaseToken);
  if (!normalizedPackageName || !normalizedSubscriptionId || !normalizedPurchaseToken) {
    throw new Error("packageName, subscriptionId oder purchaseToken fehlt fuer das Play-Acknowledge.");
  }

  const body = {};
  const normalizedPayload = nonEmptyString(developerPayload);
  if (normalizedPayload) {
    body.developerPayload = normalizedPayload;
  }

  return googlePlayRequest(
      `applications/${encodeURIComponent(normalizedPackageName)}` +
      `/purchases/subscriptions/${encodeURIComponent(normalizedSubscriptionId)}` +
      `/tokens/${encodeURIComponent(normalizedPurchaseToken)}:acknowledge`,
      {
        method: "POST",
        body,
      },
  );
}

module.exports = {
  acknowledgeGooglePlaySubscriptionPurchase,
  fetchGooglePlaySubscriptionPurchase,
};
