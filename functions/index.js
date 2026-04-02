"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {enableFirebaseTelemetry} = require("@genkit-ai/firebase");
const {onCallGenkit} = require("firebase-functions/https");
const {onCall, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {genkit, z} = require("genkit");
const {vertexAI} = require("@genkit-ai/google-genai");
const nodemailer = require("nodemailer");

admin.initializeApp();
void enableFirebaseTelemetry().catch((error) => {
  console.error("Firebase telemetry could not be enabled.", error);
});

const ai = genkit({
  plugins: [vertexAI({location: "us-central1"})],
  model: vertexAI.model("gemini-2.5-flash-lite"),
});

const smtpConnectionUrl = defineSecret("SMTP_CONNECTION_URL");
const OWNER_EMAIL = "nash.lioncorna@gmail.com";
const SHOPIFY_STORE_DOMAIN_DEFAULT = "k5t1sc-ps.myshopify.com";
const SHOPIFY_API_VERSION = "2026-01";
const SHOPIFY_CONFIG_COLLECTION = "appConfig";
const SHOPIFY_CONFIG_DOCUMENT = "shopifyMerch";
const SHOPIFY_PRIVATE_CONFIG_COLLECTION = "adminConfig";
const SHOPIFY_PRIVATE_CONFIG_DOCUMENT = "shopifyMerchPrivate";
const AUTOMATION_CONFIG_COLLECTION = "adminConfig";
const AUTOMATION_CONFIG_DOCUMENT = "automationN8n";
const SHOPIFY_VARIANT_OPTION_KEYS = {
  size: ["size", "groesse", "größe"],
  color: ["color", "colour", "farbe"],
};
const SHOPIFY_STOREFRONT_PRODUCT_FIELDS = `
  id
  title
  description
  handle
  featuredImage {
    url
  }
  images(first: 10) {
    nodes {
      url
    }
  }
  variants(first: 100) {
    nodes {
      id
      title
      sku
      availableForSale
      price {
        amount
        currencyCode
      }
      selectedOptions {
        name
        value
      }
    }
  }
`;
const SHOPIFY_STOREFRONT_PRODUCTS_QUERY = `
  query AppProducts($cursor: String) {
    products(first: 100, after: $cursor) {
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
        ${SHOPIFY_STOREFRONT_PRODUCT_FIELDS}
      }
    }
  }
`;
const SHOPIFY_STOREFRONT_COLLECTION_PRODUCTS_QUERY = `
  query AppCollectionProducts($handle: String!, $cursor: String) {
    collection(handle: $handle) {
      products(first: 100, after: $cursor) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          ${SHOPIFY_STOREFRONT_PRODUCT_FIELDS}
        }
      }
    }
  }
`;

const agentTurnSchema = z.object({
  role: z.enum(["user", "assistant"]),
  text: z.string().trim().min(1).max(4000),
});

const agentRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(4000),
  history: z.array(agentTurnSchema).max(24).default([]),
});

const systemPrompt = `
Du bist Skydown Agent, der umsetzungsorientierte Assistent fuer Skydown Entertainment und 22.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die Marke arbeitet in Musik, Videos, Merch und App-Releases.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Antworte auf Deutsch, klar, modern und konkret.
Du hilfst bei Release-Planung, Briefings, Content-Strategie, Videography, Merch-Drops, Kampagnenideen, To-dos, Freigaben und naechsten Schritten.
Arbeite pragmatisch statt generisch.
Keine langen Vorreden. Keine leeren Motivationssaetze.
Wenn du planen sollst, liefere eine umsetzbare Struktur.
Wenn du ein Briefing schreiben sollst, liefere ein copy-pastebares Briefing.
Wenn Infos fehlen, triff sinnvolle Annahmen und kennzeichne sie kurz. Frage nur dann gezielt nach, wenn ohne die Info ein schlechter Plan entstehen wuerde.
Bevorzuge kurze klare Abschnitte wie Ziel, Deliverables, Schritte, Timing, Assets, Risiken, Naechste Schritte.
`.trim();

const USER_ROLES = {
  owner: "owner",
  admin: "admin",
  subadmin: "subadmin",
  user: "user",
};

const AI_USAGE_KINDS = {
  text: "text",
  visual: "visual",
  agent: "agent",
};

function normalizeEmail(email) {
  return nonEmptyString(email)?.toLowerCase() || null;
}

function resolveUserRole(rawRole, isAdminFlag = false, email = null) {
  if (normalizeEmail(email) === OWNER_EMAIL) {
    return USER_ROLES.owner;
  }

  const normalizedRole = nonEmptyString(rawRole)?.toLowerCase();
  if (normalizedRole && Object.values(USER_ROLES).includes(normalizedRole)) {
    return normalizedRole;
  }

  return isAdminFlag === true ? USER_ROLES.admin : USER_ROLES.user;
}

function roleHasStaffAccess(role) {
  return [USER_ROLES.owner, USER_ROLES.admin].includes(role);
}

function roleHasAdminAccess(role) {
  return [USER_ROLES.owner, USER_ROLES.admin].includes(role);
}

function roleHasOwnerAccess(role) {
  return role === USER_ROLES.owner;
}

function defaultAiLimitsForRole(role) {
  switch (role) {
    case USER_ROLES.owner:
      return {text: 400, visual: 80, agent: 250, historyRetentionDays: 30};
    case USER_ROLES.admin:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_ROLES.subadmin:
      return {text: 120, visual: 20, agent: 70, historyRetentionDays: 7};
    case USER_ROLES.user:
    default:
      return {text: 30, visual: 4, agent: 18, historyRetentionDays: 3};
  }
}

function resolveAiLimits(userData = {}) {
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const defaults = defaultAiLimitsForRole(role);
  const text = Number(userData.aiTextRequestsPerDay);
  const visual = Number(userData.aiVisualRequestsPerDay);
  const agent = Number(userData.aiAgentRequestsPerDay);
  const historyRetentionDays = Number(userData.aiHistoryRetentionDays);

  return {
    role,
    isEnabled: userData.aiAccessEnabled !== false,
    text: Number.isFinite(text) && text > 0 ? Math.floor(text) : defaults.text,
    visual: Number.isFinite(visual) && visual > 0 ? Math.floor(visual) : defaults.visual,
    agent: Number.isFinite(agent) && agent > 0 ? Math.floor(agent) : defaults.agent,
    historyRetentionDays: [1, 3, 7, 30].includes(historyRetentionDays) ?
      historyRetentionDays :
      defaults.historyRetentionDays,
  };
}

function buildUserProfile(userData = {}) {
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const limits = resolveAiLimits(userData);
  return {
    role,
    isStaff: roleHasStaffAccess(role),
    isAdmin: roleHasAdminAccess(role),
    isOwner: roleHasOwnerAccess(role),
    aiAccessEnabled: limits.isEnabled,
    aiLimits: limits,
  };
}

async function loadUserData(uid) {
  if (!uid) {
    return null;
  }

  const userSnapshot = await admin.firestore().doc(`users/${uid}`).get();
  return userSnapshot.exists ? (userSnapshot.data() || {}) : null;
}

async function isAdminAuth(auth) {
  if (!auth?.uid) {
    return false;
  }

  const userData = await loadUserData(auth.uid);
  if (!userData) {
    return false;
  }

  return buildUserProfile(userData).isAdmin;
}

async function isOwnerAuth(auth) {
  if (!auth?.uid) {
    return false;
  }

  const userData = await loadUserData(auth.uid);
  if (!userData) {
    return false;
  }

  return buildUserProfile(userData).isOwner;
}

async function isStaffAuth(auth) {
  if (!auth?.uid) {
    return false;
  }

  const userData = await loadUserData(auth.uid);
  if (!userData) {
    return false;
  }

  return buildUserProfile(userData).isStaff;
}

async function canUseAiAuth(auth) {
  if (!auth?.uid) {
    return false;
  }

  const userData = await loadUserData(auth.uid);
  if (!userData) {
    return false;
  }

  return buildUserProfile(userData).aiAccessEnabled;
}

async function assertAdmin(auth) {
  if (!(await isAdminAuth(auth))) {
    throw new HttpsError("permission-denied", "Nur Admins duerfen diese Aktion ausfuehren.");
  }
}

async function assertOwner(auth) {
  if (!(await isOwnerAuth(auth))) {
    throw new HttpsError("permission-denied", "Nur der Owner darf diese Aktion ausfuehren.");
  }
}

function normalizeStoreDomain(value) {
  const trimmed = nonEmptyString(value);
  if (!trimmed) {
    return null;
  }

  const withoutScheme = trimmed
      .replace(/^https?:\/\//i, "")
      .split("/")[0]
      ?.trim()
      .toLowerCase();

  return nonEmptyString(withoutScheme);
}

function normalizeUrlString(value) {
  const trimmed = nonEmptyString(value);
  if (!trimmed) {
    return null;
  }

  const normalized = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  return normalized.replace(/\/+$/, "");
}

function normalizeCollectionHandle(value, fallbackURL = null) {
  const direct = nonEmptyString(value);
  if (direct) {
    return nonEmptyString(
        direct
            .replace(/^\/?collections\//i, "")
            .split("/")[0],
    );
  }

  const normalizedFallbackURL = normalizeUrlString(fallbackURL);
  if (!normalizedFallbackURL) {
    return null;
  }

  try {
    const url = new URL(normalizedFallbackURL);
    const pathSegments = url.pathname.split("/").filter(Boolean);
    const collectionIndex = pathSegments.findIndex((segment) => segment.toLowerCase() === "collections");
    if (collectionIndex === -1 || !pathSegments[collectionIndex + 1]) {
      return null;
    }

    return nonEmptyString(pathSegments[collectionIndex + 1]);
  } catch (error) {
    logger.warn("Shopify collection handle could not be derived from URL.", {
      storefrontURL: normalizedFallbackURL,
      error: error instanceof Error ? error.message : "unknown_error",
    });
    return null;
  }
}

function normalizeAutomationWebhookPath(value) {
  const trimmed = nonEmptyString(value);
  if (!trimmed) {
    return null;
  }

  if (/^https?:\/\//i.test(trimmed)) {
    try {
      const url = new URL(trimmed);
      return nonEmptyString(url.pathname.replace(/^\/+/, ""));
    } catch (error) {
      logger.warn("Automation webhook path could not be derived from URL.", {
        value: trimmed,
        error: error instanceof Error ? error.message : "unknown_error",
      });
      return null;
    }
  }

  return nonEmptyString(trimmed.replace(/^\/+/, "").replace(/\/+$/, ""));
}

function buildAutomationWebhookUrl(baseURL, webhookPath) {
  const normalizedBaseURL = normalizeUrlString(baseURL);
  if (!normalizedBaseURL) {
    return null;
  }

  const normalizedWebhookPath = normalizeAutomationWebhookPath(webhookPath);
  if (!normalizedWebhookPath) {
    return normalizedBaseURL;
  }

  return `${normalizedBaseURL}/${normalizedWebhookPath}`;
}

async function loadWorkflowAutomationSettings() {
  const snapshot = await admin.firestore()
      .collection(AUTOMATION_CONFIG_COLLECTION)
      .doc(AUTOMATION_CONFIG_DOCUMENT)
      .get();
  const data = snapshot.data() || {};

  return {
    provider: nonEmptyString(data.provider) || "n8n",
    isEnabled: data.isEnabled === true,
    sendsUserContext: data.sendsUserContext !== false,
    workflowName: nonEmptyString(data.workflowName) || "Skydown Automation",
    baseURL: normalizeUrlString(data.baseURL) || "",
    webhookPath: normalizeAutomationWebhookPath(data.webhookPath) || "",
    authHeaderName: nonEmptyString(data.authHeaderName) || "",
    authHeaderValue: nonEmptyString(data.authHeaderValue) || "",
  };
}

function automationConfigDocumentIdFor(userId) {
  return `automationN8n_${userId}`;
}

async function loadWorkflowAutomationSettingsForUser(userId) {
  if (nonEmptyString(userId)) {
    const personalSnapshot = await admin.firestore()
        .collection(AUTOMATION_CONFIG_COLLECTION)
        .doc(automationConfigDocumentIdFor(userId))
        .get();

    if (personalSnapshot.exists) {
      const data = personalSnapshot.data() || {};
      return {
        provider: nonEmptyString(data.provider) || "n8n",
        isEnabled: data.isEnabled === true,
        sendsUserContext: data.sendsUserContext !== false,
        workflowName: nonEmptyString(data.workflowName) || "Skydown Automation",
        baseURL: normalizeUrlString(data.baseURL) || "",
        webhookPath: normalizeAutomationWebhookPath(data.webhookPath) || "",
        authHeaderName: nonEmptyString(data.authHeaderName) || "",
        authHeaderValue: nonEmptyString(data.authHeaderValue) || "",
      };
    }
  }

  return loadWorkflowAutomationSettings();
}

async function loadAutomationCaller(auth) {
  if (!auth?.uid) {
    return null;
  }

  const userData = await loadUserData(auth.uid) || {};
  const profile = buildUserProfile(userData);

  return {
    uid: auth.uid,
    email: nonEmptyString(auth.token?.email) || nonEmptyString(userData.email) || "",
    username: nonEmptyString(userData.username) || "",
    role: profile.role,
    isAdmin: profile.isAdmin,
    isOwner: profile.isOwner,
    isStaff: profile.isStaff,
  };
}

function extractAutomationResponseMessage(bodyText, workflowName) {
  const trimmed = typeof bodyText === "string" ? bodyText.trim() : "";
  if (!trimmed) {
    return `Test an ${workflowName} gesendet.`;
  }

  try {
    const parsed = JSON.parse(trimmed);
    const message = nonEmptyString(parsed?.message)
      || nonEmptyString(parsed?.status)
      || nonEmptyString(parsed?.result);
    if (message) {
      return message;
    }
  } catch (error) {
    // Response body can also be plain text.
  }

  return trimmed.length > 160 ? `Test an ${workflowName} gesendet.` : trimmed;
}

async function triggerWorkflowAutomationWebhook({trigger, source, auth, data = {}}) {
  const settings = await loadWorkflowAutomationSettingsForUser(auth?.uid);

  if (settings.provider !== "n8n") {
    throw new HttpsError("failed-precondition", "Automation ist nicht auf n8n gestellt.");
  }

  if (!settings.isEnabled) {
    throw new HttpsError("failed-precondition", "n8n ist aktuell nicht aktiviert.");
  }

  const webhookUrl = buildAutomationWebhookUrl(settings.baseURL, settings.webhookPath);
  if (!webhookUrl) {
    throw new HttpsError("failed-precondition", "n8n ist noch nicht vollstaendig konfiguriert.");
  }

  const payload = {
    provider: "n8n",
    workflowName: settings.workflowName,
    trigger,
    source,
    timestamp: new Date().toISOString(),
    data: data && typeof data === "object" && !Array.isArray(data) ? data : {},
  };

  if (settings.sendsUserContext) {
    payload.user = await loadAutomationCaller(auth);
  }

  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
  };

  if (settings.authHeaderName && settings.authHeaderValue) {
    headers[settings.authHeaderName] = settings.authHeaderValue;
  }

  const response = await fetch(webhookUrl, {
    method: "POST",
    headers,
    body: JSON.stringify(payload),
  });

  const responseBody = await response.text().catch(() => "");

  if (!response.ok) {
    logger.error("n8n webhook trigger failed.", {
      trigger,
      source,
      workflowName: settings.workflowName,
      status: response.status,
      statusText: response.statusText,
      responseBody: responseBody.slice(0, 500),
    });
    throw new HttpsError("internal", `n8n Fehler (${response.status} ${response.statusText}).`);
  }

  logger.info("n8n webhook triggered.", {
    trigger,
    source,
    workflowName: settings.workflowName,
    status: response.status,
    sendsUserContext: settings.sendsUserContext,
  });

  return {
    message: extractAutomationResponseMessage(responseBody, settings.workflowName),
    workflowName: settings.workflowName,
    status: response.status,
  };
}

function aiUsageDateKey() {
  return new Date().toISOString().slice(0, 10);
}

function aiUsageCounterField(kind) {
  switch (kind) {
    case AI_USAGE_KINDS.visual:
      return "visualRequests";
    case AI_USAGE_KINDS.agent:
      return "agentRequests";
    case AI_USAGE_KINDS.text:
    default:
      return "textRequests";
  }
}

function aiUsageLimitForKind(kind, limits) {
  switch (kind) {
    case AI_USAGE_KINDS.visual:
      return limits.visual;
    case AI_USAGE_KINDS.agent:
      return limits.agent;
    case AI_USAGE_KINDS.text:
    default:
      return limits.text;
  }
}

function aiLimitReachedMessage(kind, limit) {
  switch (kind) {
    case AI_USAGE_KINDS.visual:
      return `Dein Tageslimit fuer Visuals ist erreicht (${limit}/Tag).`;
    case AI_USAGE_KINDS.agent:
      return `Dein Tageslimit fuer den Agent ist erreicht (${limit}/Tag).`;
    case AI_USAGE_KINDS.text:
    default:
      return `Dein Tageslimit fuer den Bot ist erreicht (${limit}/Tag).`;
  }
}

async function authorizeAiUsage({auth, kind}) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Bitte melde dich an, um die KI zu nutzen.");
  }

  if (!Object.values(AI_USAGE_KINDS).includes(kind)) {
    throw new HttpsError("invalid-argument", "Unbekannte KI-Aktion.");
  }

  const userData = await loadUserData(auth.uid);
  const profile = buildUserProfile(userData || {});

  if (!profile.aiAccessEnabled) {
    throw new HttpsError("permission-denied", "Die KI ist fuer dein Konto gerade pausiert.");
  }

  const dateKey = aiUsageDateKey();
  const usageRef = admin.firestore().doc(`users/${auth.uid}/aiUsage/${dateKey}`);
  const usageSummary = await admin.firestore().runTransaction(async (transaction) => {
    const snapshot = await transaction.get(usageRef);
    const currentData = snapshot.exists ? (snapshot.data() || {}) : {};
    const counterField = aiUsageCounterField(kind);
    const currentCount = Number(currentData[counterField]) || 0;
    const currentTotal = Number(currentData.totalRequests) || 0;
    const limit = aiUsageLimitForKind(kind, profile.aiLimits);

    if (currentCount >= limit) {
      throw new HttpsError("resource-exhausted", aiLimitReachedMessage(kind, limit));
    }

    const nextCount = currentCount + 1;
    const nextTotal = currentTotal + 1;

    transaction.set(usageRef, {
      dateKey,
      role: profile.role,
      textRequests: counterField === "textRequests" ? nextCount : (Number(currentData.textRequests) || 0),
      visualRequests: counterField === "visualRequests" ? nextCount : (Number(currentData.visualRequests) || 0),
      agentRequests: counterField === "agentRequests" ? nextCount : (Number(currentData.agentRequests) || 0),
      totalRequests: nextTotal,
      lastConsumedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    return {
      dateKey,
      role: profile.role,
      kind,
      remainingForKind: Math.max(limit - nextCount, 0),
      limitForKind: limit,
      textRemaining: Math.max(profile.aiLimits.text - (counterField === "textRequests" ? nextCount : (Number(currentData.textRequests) || 0)), 0),
      visualRemaining: Math.max(profile.aiLimits.visual - (counterField === "visualRequests" ? nextCount : (Number(currentData.visualRequests) || 0)), 0),
      agentRemaining: Math.max(profile.aiLimits.agent - (counterField === "agentRequests" ? nextCount : (Number(currentData.agentRequests) || 0)), 0),
      historyRetentionDays: profile.aiLimits.historyRetentionDays,
    };
  });

  logger.info("AI usage authorized.", {
    uid: auth.uid,
    role: usageSummary.role,
    kind,
    remainingForKind: usageSummary.remainingForKind,
    dateKey,
  });

  return usageSummary;
}

function getShopifyDomain(storeDomain) {
  return normalizeStoreDomain(storeDomain)
    || normalizeStoreDomain(process.env.SHOPIFY_STORE_DOMAIN)
    || SHOPIFY_STORE_DOMAIN_DEFAULT;
}

function buildStorefrontURL(storeDomain, collectionHandle) {
  if (collectionHandle) {
    return `https://${storeDomain}/collections/${collectionHandle}`;
  }

  return `https://${storeDomain}`;
}

async function loadShopifyAdminConfig() {
  const snapshot = await admin.firestore()
      .collection(SHOPIFY_CONFIG_COLLECTION)
      .doc(SHOPIFY_CONFIG_DOCUMENT)
      .get();
  const data = snapshot.data() || {};
  const configuredStorefrontURL = normalizeUrlString(data.storefrontURL);
  const storeDomain = getShopifyDomain(data.storeDomain || configuredStorefrontURL);
  const collectionHandle = normalizeCollectionHandle(
      data.collectionHandle,
      configuredStorefrontURL,
  ) || "";

  return {
    storeDomain,
    storefrontAccessToken: nonEmptyString(data.storefrontAccessToken) || "",
    collectionHandle,
  };
}

async function loadShopifyAdminToken() {
  const envToken = nonEmptyString(process.env.SHOPIFY_ADMIN_ACCESS_TOKEN);
  if (envToken) {
    return envToken;
  }

  const snapshot = await admin.firestore()
      .collection(SHOPIFY_PRIVATE_CONFIG_COLLECTION)
      .doc(SHOPIFY_PRIVATE_CONFIG_DOCUMENT)
      .get();

  return nonEmptyString(snapshot.data()?.adminApiToken) || "";
}

function getShopifyGraphqlUrl(storeDomain) {
  return `https://${getShopifyDomain(storeDomain)}/admin/api/${SHOPIFY_API_VERSION}/graphql.json`;
}

function getShopifyStorefrontGraphqlUrl(storeDomain) {
  return `https://${getShopifyDomain(storeDomain)}/api/${SHOPIFY_API_VERSION}/graphql.json`;
}

async function shopifyGraphqlRequest({query, variables = {}, token, storeDomain}) {
  const response = await fetch(getShopifyGraphqlUrl(storeDomain), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "X-Shopify-Access-Token": token,
    },
    body: JSON.stringify({query, variables}),
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    logger.error("Shopify GraphQL request failed.", {
      status: response.status,
      statusText: response.statusText,
      body: payload,
    });
    throw new Error(`Shopify API Fehler (${response.status} ${response.statusText}).`);
  }

  if (Array.isArray(payload.errors) && payload.errors.length > 0) {
    logger.error("Shopify GraphQL top-level errors.", {errors: payload.errors});
    throw new Error(payload.errors.map((error) => error.message).join(" | "));
  }

  return payload.data;
}

async function shopifyStorefrontGraphqlRequest({query, variables = {}, token, storeDomain}) {
  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
  };

  if (token) {
    headers["X-Shopify-Storefront-Access-Token"] = token;
  }

  const response = await fetch(getShopifyStorefrontGraphqlUrl(storeDomain), {
    method: "POST",
    headers,
    body: JSON.stringify({query, variables}),
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    logger.error("Shopify Storefront GraphQL request failed.", {
      status: response.status,
      statusText: response.statusText,
      body: payload,
    });
    throw new Error(`Shopify Storefront API Fehler (${response.status} ${response.statusText}).`);
  }

  if (Array.isArray(payload.errors) && payload.errors.length > 0) {
    logger.error("Shopify Storefront GraphQL top-level errors.", {errors: payload.errors});
    throw new Error(payload.errors.map((error) => error.message).join(" | "));
  }

  return payload.data;
}

function getShopifyPublicProductsUrl({storeDomain, collectionHandle = null, sinceId = null}) {
  const basePath = collectionHandle
    ? `/collections/${collectionHandle}/products.json`
    : "/products.json";
  const url = new URL(`https://${getShopifyDomain(storeDomain)}${basePath}`);
  url.searchParams.set("limit", "250");
  if (sinceId) {
    url.searchParams.set("since_id", `${sinceId}`);
  }
  return url.toString();
}

async function shopifyPublicProductsRequest({storeDomain, collectionHandle = null, sinceId = null}) {
  const url = getShopifyPublicProductsUrl({storeDomain, collectionHandle, sinceId});
  const response = await fetch(url, {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    logger.error("Shopify public products request failed.", {
      url,
      status: response.status,
      statusText: response.statusText,
      body: payload,
    });
    if (response.status === 401 || response.status === 403) {
      throw new Error("Der Shopify-Store ist nicht oeffentlich lesbar. Hinterlege einen Storefront Access Token in den Shopify-Einstellungen der App oder oeffne den Produktfeed.");
    }
    throw new Error(`Shopify Storefront Fehler (${response.status} ${response.statusText}).`);
  }

  return Array.isArray(payload?.products) ? payload.products : [];
}

function extractNumericId(gid) {
  if (typeof gid !== "string") {
    return "";
  }

  const parts = gid.split("/");
  return parts[parts.length - 1] || gid;
}

function buildShopifyMerchDocumentId(productId) {
  const numericId = extractNumericId(productId);
  return numericId ? `shopify_${numericId}` : `shopify_${Buffer.from(productId).toString("base64url")}`;
}

function parsePrice(value) {
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : 0;
}

function stripHtml(value) {
  return typeof value === "string"
    ? value.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim()
    : "";
}

function nonEmptyString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function uniqueStrings(values) {
  const seen = new Set();
  return values.filter((value) => {
    if (typeof value !== "string" || !value.trim()) {
      return false;
    }

    const normalized = value.trim();
    if (seen.has(normalized)) {
      return false;
    }

    seen.add(normalized);
    return true;
  });
}

function buildSelectedOptionsFromPublicVariant(variant, productOptions) {
  const optionValues = [variant?.option1, variant?.option2, variant?.option3];
  return (Array.isArray(productOptions) ? productOptions : [])
      .map((option, index) => ({
        name: nonEmptyString(option?.name) || `Option ${index + 1}`,
        value: nonEmptyString(optionValues[index]) || "",
      }))
      .filter((option) => option.value);
}

function readSelectedOption(variant, keys) {
  const selectedOptions = Array.isArray(variant?.selectedOptions) ? variant.selectedOptions : [];
  const match = selectedOptions.find((option) => {
    const optionName = `${option?.name || ""}`.trim().toLowerCase();
    return keys.includes(optionName);
  });

  return nonEmptyString(match?.value) || undefined;
}

function normalizeShopifyVariant(variant, currencyCode) {
  const priceValue = typeof variant?.price === "object" && variant?.price !== null ?
    variant.price.amount :
    variant?.price;
  const variantCurrencyCode = nonEmptyString(variant?.price?.currencyCode) || currencyCode || "EUR";

  return {
    id: extractNumericId(variant.id) || variant.id,
    title: variant.title || "",
    size: readSelectedOption(variant, SHOPIFY_VARIANT_OPTION_KEYS.size),
    color: readSelectedOption(variant, SHOPIFY_VARIANT_OPTION_KEYS.color),
    shopifyVariantId: variant.id,
    sku: nonEmptyString(variant.sku) || "",
    price: parsePrice(priceValue),
    currency: variantCurrencyCode,
    availableForSale: Boolean(variant.availableForSale),
  };
}

function normalizeShopifyProduct(product, currencyCode, existingData = {}) {
  const images = uniqueStrings([
    product?.featuredImage?.url,
    ...(product?.images?.nodes || []).map((image) => image?.url),
  ]);
  const variants = (product?.variants?.nodes || []).map((variant) => normalizeShopifyVariant(variant, currencyCode));
  const firstVariant = variants[0];
  const availableForSale = variants.some((variant) => variant.availableForSale);

  return {
    name: product.title || "Unbenanntes Produkt",
    price: firstVariant?.price ?? 0,
    description: stripHtml(product.description || product.descriptionHtml || ""),
    imageURLs: images,
    imageUrls: images,
    available: availableForSale,
    currency: currencyCode || firstVariant?.currency || "EUR",
    sku: nonEmptyString(firstVariant?.sku) || "",
    shopifyProductId: product.id,
    shopifyHandle: nonEmptyString(product.handle) || "",
    availableForSale,
    shopifySyncActive: true,
    variants,
    source: "shopify",
    isVisibleInApp: typeof existingData.isVisibleInApp === "boolean" ? existingData.isVisibleInApp : true,
    featured: typeof existingData.featured === "boolean" ? existingData.featured : false,
    sortOrder: Number.isFinite(existingData.sortOrder) ? existingData.sortOrder : 0,
    customBadge: typeof existingData.customBadge === "string" ? existingData.customBadge : "",
    customImageOverride: typeof existingData.customImageOverride === "string" ? existingData.customImageOverride : "",
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function normalizePublicShopifyProduct(product) {
  const images = uniqueStrings([
    product?.image?.src,
    ...(Array.isArray(product?.images) ? product.images.map((image) => image?.src) : []),
  ]);

  const variants = (Array.isArray(product?.variants) ? product.variants : []).map((variant) => {
    const inventoryQuantity = Number(variant?.inventory_quantity);
    const availableForSale = typeof variant?.available === "boolean"
      ? variant.available
      : !Number.isFinite(inventoryQuantity) || inventoryQuantity > 0;

    return {
      id: `gid://shopify/ProductVariant/${variant.id}`,
      title: variant?.title || "",
      sku: nonEmptyString(variant?.sku) || "",
      price: parsePrice(variant?.price),
      availableForSale,
      selectedOptions: buildSelectedOptionsFromPublicVariant(variant, product?.options),
    };
  });

  return {
    id: `gid://shopify/Product/${product.id}`,
    title: product?.title || "Unbenanntes Produkt",
    description: stripHtml(product?.body_html || ""),
    descriptionHtml: product?.body_html || "",
    handle: product?.handle || "",
    featuredImage: images[0] ? {url: images[0]} : null,
    images: {
      nodes: images.map((url) => ({url})),
    },
    collections: {
      nodes: [],
    },
    variants: {
      nodes: variants,
    },
  };
}

async function fetchAllShopifyProductsViaAdminApi(token, config) {
  const query = `
    query FetchMerchProducts($cursor: String) {
      shop {
        currencyCode
      }
      products(first: 50, after: $cursor, sortKey: UPDATED_AT) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          id
          title
          description
          descriptionHtml
          handle
          status
          featuredImage {
            url
          }
          images(first: 10) {
            nodes {
              url
            }
          }
          collections(first: 20) {
            nodes {
              handle
              title
            }
          }
          variants(first: 100) {
            nodes {
              id
              title
              sku
              price
              availableForSale
              selectedOptions {
                name
                value
              }
            }
          }
        }
      }
    }
  `;

  const products = [];
  let hasNextPage = true;
  let cursor = null;
  let currencyCode = "EUR";

  while (hasNextPage) {
    const data = await shopifyGraphqlRequest({
      query,
      variables: {cursor},
      token,
      storeDomain: config?.storeDomain,
    });

    currencyCode = data?.shop?.currencyCode || currencyCode;
    const connection = data?.products;
    products.push(
      ...(connection?.nodes || []).filter((product) => (product?.variants?.nodes || []).length > 0),
    );
    hasNextPage = Boolean(connection?.pageInfo?.hasNextPage);
    cursor = connection?.pageInfo?.endCursor || null;
  }

  if (config?.collectionHandle) {
    const normalizedCollectionHandle = config.collectionHandle.toLowerCase();
    const filteredProducts = products.filter((product) => {
      const collections = product?.collections?.nodes || [];
      return collections.some((collection) =>
        `${collection?.handle || ""}`.trim().toLowerCase() === normalizedCollectionHandle,
      );
    });

    if (filteredProducts.length === 0 && products.length > 0) {
      logger.warn("Shopify Admin API collection filter returned no products. Falling back to all products.", {
        storeDomain: config?.storeDomain,
        collectionHandle: normalizedCollectionHandle,
      });
      return {
        products,
        currencyCode,
      };
    }

    return {
      products: filteredProducts,
      currencyCode,
    };
  }

  return {products, currencyCode};
}

async function fetchAllShopifyProductsViaStorefrontApi(config) {
  const fetchConnection = async (collectionHandle = null) => {
    const products = [];
    let hasNextPage = true;
    let cursor = null;

    while (hasNextPage) {
      const data = await shopifyStorefrontGraphqlRequest({
        query: collectionHandle ?
          SHOPIFY_STOREFRONT_COLLECTION_PRODUCTS_QUERY :
          SHOPIFY_STOREFRONT_PRODUCTS_QUERY,
        variables: collectionHandle ?
          {handle: collectionHandle, cursor} :
          {cursor},
        token: nonEmptyString(config?.storefrontAccessToken) || "",
        storeDomain: config?.storeDomain,
      });

      const connection = collectionHandle ?
        data?.collection?.products :
        data?.products;

      if (!connection) {
        return [];
      }

      products.push(
        ...((connection?.nodes || []).filter((product) => (product?.variants?.nodes || []).length > 0)),
      );
      hasNextPage = Boolean(connection?.pageInfo?.hasNextPage);
      cursor = connection?.pageInfo?.endCursor || null;
    }

    return products;
  };

  const requestedCollectionHandle = nonEmptyString(config?.collectionHandle);
  let products = await fetchConnection(requestedCollectionHandle);
  let syncSource = requestedCollectionHandle ? "storefront_collection" : "storefront_api";

  if (requestedCollectionHandle && products.length === 0) {
    logger.warn("Shopify Storefront collection returned no products. Falling back to all storefront products.", {
      storeDomain: config?.storeDomain,
      collectionHandle: requestedCollectionHandle,
    });
    products = await fetchConnection(null);
    syncSource = "storefront_api_fallback";
  }

  const currencyCode = nonEmptyString(
      products[0]?.variants?.nodes?.[0]?.price?.currencyCode,
  ) || "EUR";

  return {
    products,
    currencyCode,
    syncSource,
  };
}

async function fetchAllShopifyProductsViaPublicStorefront(config) {
  const fetchAllPages = async (collectionHandle = null) => {
    const products = [];
    let sinceId = null;
    let hasNextPage = true;

    while (hasNextPage) {
      const pageProducts = await shopifyPublicProductsRequest({
        storeDomain: config?.storeDomain,
        collectionHandle,
        sinceId,
      });

      products.push(...pageProducts);
      if (pageProducts.length < 250) {
        hasNextPage = false;
      } else {
        sinceId = pageProducts[pageProducts.length - 1]?.id || null;
        hasNextPage = Boolean(sinceId);
      }
    }

    return products;
  };

  const requestedCollectionHandle = nonEmptyString(config?.collectionHandle);
  let rawProducts = [];
  let syncSource = "public_storefront";
  let usedCollectionFallback = false;

  try {
    rawProducts = await fetchAllPages(requestedCollectionHandle);
    if (requestedCollectionHandle) {
      syncSource = "public_collection";
    }
  } catch (error) {
    if (!requestedCollectionHandle) {
      throw error;
    }

    logger.warn("Shopify public collection sync failed. Falling back to all public products.", {
      storeDomain: config?.storeDomain,
      collectionHandle: requestedCollectionHandle,
      error: error instanceof Error ? error.message : "unknown_error",
    });
    rawProducts = await fetchAllPages(null);
    usedCollectionFallback = true;
  }

  if (requestedCollectionHandle && rawProducts.length === 0) {
    logger.warn("Shopify public collection sync returned no products. Falling back to all public products.", {
      storeDomain: config?.storeDomain,
      collectionHandle: requestedCollectionHandle,
    });
    rawProducts = await fetchAllPages(null);
    usedCollectionFallback = true;
  }

  const products = rawProducts
      .map((product) => normalizePublicShopifyProduct(product))
      .filter((product) => (product?.variants?.nodes || []).length > 0);

  return {
    products,
    currencyCode: "EUR",
    syncSource: usedCollectionFallback ? "public_storefront_fallback" : syncSource,
  };
}

async function fetchAllShopifyProducts(token, config) {
  try {
    return await fetchAllShopifyProductsViaStorefrontApi(config);
  } catch (error) {
    logger.warn("Shopify Storefront API sync failed.", {
      storeDomain: config?.storeDomain,
      collectionHandle: config?.collectionHandle || null,
      hasStorefrontToken: Boolean(nonEmptyString(config?.storefrontAccessToken)),
      error: error instanceof Error ? error.message : "unknown_error",
    });
  }

  if (token) {
    try {
      const result = await fetchAllShopifyProductsViaAdminApi(token, config);
      return {
        ...result,
        syncSource: "admin_api",
      };
    } catch (error) {
      logger.warn("Shopify Admin API sync failed. Falling back to public storefront sync.", {
        storeDomain: config?.storeDomain,
        collectionHandle: config?.collectionHandle || null,
        error: error instanceof Error ? error.message : "unknown_error",
      });
    }
  }

  return fetchAllShopifyProductsViaPublicStorefront(config);
}

async function runShopifyMerchSync() {
  const token = await loadShopifyAdminToken();
  const config = await loadShopifyAdminConfig();

  logger.info("Shopify merch sync started.", {
    storeDomain: config.storeDomain,
    collectionHandle: config.collectionHandle || null,
    hasStorefrontToken: Boolean(config.storefrontAccessToken),
  });

  const firestore = admin.firestore();
  const merchandiseCollection = firestore.collection("merchandise");
  const existingSnapshot = await merchandiseCollection.get();
  const existingByProductId = new Map();
  const existingByDocumentId = new Map();

  existingSnapshot.docs.forEach((document) => {
    const data = document.data();
    const productId = nonEmptyString(data.shopifyProductId);
    if (productId) {
      existingByProductId.set(productId, document);
    }
    existingByDocumentId.set(document.id, document);
  });

  const {products, currencyCode, syncSource} = await fetchAllShopifyProducts(token, config);
  const batch = firestore.batch();
  let createdCount = 0;
  let updatedCount = 0;
  let deactivatedCount = 0;
  const activeProductIds = new Set(products.map((product) => product.id));

  products.forEach((product) => {
    const existingDocument = existingByProductId.get(product.id)
      || existingByDocumentId.get(buildShopifyMerchDocumentId(product.id));
    const documentRef = existingDocument
      ? existingDocument.ref
      : merchandiseCollection.doc(buildShopifyMerchDocumentId(product.id));
    const payload = normalizeShopifyProduct(product, currencyCode, existingDocument?.data() || {});

    batch.set(documentRef, payload, {merge: true});
    if (existingDocument) {
      updatedCount += 1;
    } else {
      createdCount += 1;
    }
  });

  existingByProductId.forEach((document, productId) => {
    if (activeProductIds.has(productId)) {
      return;
    }

    batch.set(document.ref, {
      shopifySyncActive: false,
      available: false,
      availableForSale: false,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    deactivatedCount += 1;
  });

  if (products.length > 0 || deactivatedCount > 0) {
    await batch.commit();
  }

  logger.info("Shopify merch sync completed.", {
    syncedCount: products.length,
    createdCount,
    updatedCount,
    deactivatedCount,
    syncSource,
    storeDomain: config.storeDomain,
    collectionHandle: config.collectionHandle || null,
  });

  return {
    syncedCount: products.length,
    createdCount,
    updatedCount,
    deactivatedCount,
    currencyCode,
    storeDomain: config.storeDomain,
    collectionHandle: config.collectionHandle,
    syncSource,
  };
}

function ensureShippingAddress(data) {
  const shippingAddress = data?.shippingAddressData;
  if (!shippingAddress || typeof shippingAddress !== "object") {
    throw new Error("Lieferadresse fehlt.");
  }

  const address1 = nonEmptyString(shippingAddress.address1);
  const city = nonEmptyString(shippingAddress.city);
  const zip = nonEmptyString(shippingAddress.zip);
  const countryCode = nonEmptyString(shippingAddress.countryCode);

  if (!address1 || !city || !zip || !countryCode) {
    throw new Error("Lieferadresse ist unvollstaendig.");
  }

  return {
    address1,
    address2: nonEmptyString(shippingAddress.address2) || "",
    city,
    zip,
    countryCode,
    countryName: nonEmptyString(shippingAddress.countryName) || countryCode,
  };
}

function normalizeOrderItems(items) {
  if (!Array.isArray(items) || items.length === 0) {
    throw new HttpsError("invalid-argument", "Es wurden keine gueltigen Artikel uebergeben.");
  }

  return items.map((item, index) => {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      throw new HttpsError("invalid-argument", `Artikel ${index + 1} ist ungueltig.`);
    }

    const name = nonEmptyString(item.name);
    const quantity = Number(item.quantity);
    const unitPrice = parsePrice(item.unitPrice);

    if (!name) {
      throw new HttpsError("invalid-argument", `Name fuer Artikel ${index + 1} fehlt.`);
    }

    if (!Number.isInteger(quantity) || quantity <= 0) {
      throw new HttpsError("invalid-argument", `Menge fuer Artikel ${index + 1} ist ungueltig.`);
    }

    if (!Number.isFinite(unitPrice) || unitPrice < 0) {
      throw new HttpsError("invalid-argument", `Preis fuer Artikel ${index + 1} ist ungueltig.`);
    }

    return {
      productId: nonEmptyString(item.productId) || "",
      name,
      quantity,
      size: nonEmptyString(item.size) || "",
      color: nonEmptyString(item.color) || "",
      shopifyVariantId: nonEmptyString(item.shopifyVariantId) || "",
      sku: nonEmptyString(item.sku) || "",
      unitPrice,
    };
  });
}

function normalizeFulfillmentProvider(value) {
  const provider = nonEmptyString(value)?.toLowerCase();
  if (provider === "podpartner" || provider === "manual") {
    return provider;
  }

  throw new HttpsError("invalid-argument", "fulfillmentProvider ist ungueltig.");
}

function buildInitialOrderState(fulfillmentProvider) {
  if (fulfillmentProvider === "podpartner") {
    return {
      paymentStatus: "pending",
      fulfillmentStatus: "pending",
      shopifySyncStatus: "awaiting_payment",
    };
  }

  return {
    paymentStatus: "pending",
    fulfillmentStatus: "manual_review",
    shopifySyncStatus: "not_required",
  };
}

function normalizeOrderSubmissionPayload(data, auth) {
  const authUid = nonEmptyString(auth?.uid);
  const authEmail = normalizeEmail(auth?.token?.email);
  if (!authUid || !authEmail) {
    throw new HttpsError("unauthenticated", "Du musst angemeldet sein.");
  }

  const userEmail = normalizeEmail(data?.userEmail) || authEmail;
  if (userEmail !== authEmail) {
    throw new HttpsError("permission-denied", "Bestellungen duerfen nur fuer das eigene Konto angelegt werden.");
  }

  const customerName = nonEmptyString(data?.customerName);
  const customerEmail = normalizeEmail(data?.customerEmail);
  const shippingAddress = nonEmptyString(data?.shippingAddress);
  const shippingZone = nonEmptyString(data?.shippingZone);
  const shippingCountryCode = nonEmptyString(data?.shippingCountryCode)?.toUpperCase();
  const paymentMethod = nonEmptyString(data?.paymentMethod) || "";
  const whatsApp = nonEmptyString(data?.whatsApp) || "";
  const message = nonEmptyString(data?.message) || "";
  const items = normalizeOrderItems(data?.items);
  const shippingAddressData = ensureShippingAddress(data);
  const fulfillmentProvider = normalizeFulfillmentProvider(data?.fulfillmentProvider);
  const subtotalAmount = parsePrice(data?.subtotalAmount);
  const shippingAmount = parsePrice(data?.shippingAmount);
  const taxRate = parsePrice(data?.taxRate);
  const taxAmount = parsePrice(data?.taxAmount);
  const totalAmount = parsePrice(data?.totalAmount);

  if (!customerName || !customerEmail || !shippingAddress || !shippingZone || !shippingCountryCode) {
    throw new HttpsError("invalid-argument", "Bestellung ist unvollstaendig.");
  }

  if (shippingCountryCode !== shippingAddressData.countryCode.toUpperCase()) {
    throw new HttpsError("invalid-argument", "Lieferland und Lieferadresse passen nicht zusammen.");
  }

  if (subtotalAmount < 0 || shippingAmount < 0 || taxRate < 0 || taxAmount < 0 || totalAmount <= 0) {
    throw new HttpsError("invalid-argument", "Summen der Bestellung sind ungueltig.");
  }

  const expectedTotal = subtotalAmount + shippingAmount;
  if (Math.abs(totalAmount - expectedTotal) > 0.01) {
    throw new HttpsError("invalid-argument", "Gesamtsumme stimmt nicht mit Warenkorb und Versand ueberein.");
  }

  return {
    userEmail,
    orderOwnerUid: authUid,
    customerName,
    customerEmail,
    whatsApp,
    shippingAddress,
    shippingAddressData,
    shippingZone,
    shippingCountryCode,
    paymentMethod,
    message,
    items,
    subtotalAmount,
    shippingAmount,
    shippingPriceCharged: shippingAmount,
    taxRate,
    taxAmount,
    totalAmount,
    fulfillmentProvider,
    ...buildInitialOrderState(fulfillmentProvider),
    isCompleted: false,
    timestamp: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function splitCustomerName(customerName) {
  const trimmed = nonEmptyString(customerName) || "";
  if (!trimmed) {
    return {firstName: "", lastName: ""};
  }

  const parts = trimmed.split(/\s+/);
  if (parts.length === 1) {
    return {firstName: parts[0], lastName: ""};
  }

  return {
    firstName: parts.slice(0, -1).join(" "),
    lastName: parts[parts.length - 1],
  };
}

function buildOrderNote(orderId, data) {
  const message = nonEmptyString(data.message) || "Keine Zusatznachricht.";
  const paymentMethod = nonEmptyString(data.paymentMethod) || "Nicht angegeben";
  const shippingZone = nonEmptyString(data.shippingZone) || "Unbekannt";
  const shippingAmount = parsePrice(data.shippingAmount);

  return [
    `App-Order: ${orderId}`,
    `Payment Method: ${paymentMethod}`,
    `Shipping Zone: ${shippingZone}`,
    `Shipping Charged: ${shippingAmount.toFixed(2)} ${nonEmptyString(data.currency) || "EUR"}`,
    "",
    "Customer Note:",
    message,
  ].join("\n");
}

function buildShopifyOrderInput(orderId, data) {
  const shippingAddress = ensureShippingAddress(data);
  const lineItems = (Array.isArray(data.items) ? data.items : []).map((item, index) => {
    const variantId = nonEmptyString(item?.shopifyVariantId);
    const quantity = Number(item?.quantity || 0);

    if (!variantId) {
      throw new Error(`shopifyVariantId fehlt fuer Artikel ${index + 1}.`);
    }

    if (!Number.isInteger(quantity) || quantity <= 0) {
      throw new Error(`Menge fuer Artikel ${index + 1} ist ungueltig.`);
    }

    return {
      variantId,
      quantity,
      taxable: true,
      requiresShipping: true,
      sku: nonEmptyString(item?.sku) || undefined,
    };
  });

  if (lineItems.length === 0) {
    throw new Error("Es wurden keine gueltigen Shopify-Artikel fuer die Order gefunden.");
  }

  const {firstName, lastName} = splitCustomerName(data.customerName);
  const email = nonEmptyString(data.customerEmail) || nonEmptyString(data.userEmail);
  if (!email) {
    throw new Error("Kunden-E-Mail fehlt.");
  }

  const currencyCode = nonEmptyString(data.currency) || "EUR";
  const shippingAmount = parsePrice(data.shippingAmount);
  const taxAmount = parsePrice(data.taxAmount);
  const taxRate = parsePrice(data.taxRate) / 100;

  return {
    email,
    phone: nonEmptyString(data.whatsApp) || undefined,
    sourceIdentifier: orderId,
    sourceName: "skydown_app",
    note: buildOrderNote(orderId, data),
    financialStatus: "PAID",
    fulfillmentStatus: "UNFULFILLED",
    shippingAddress: {
      firstName,
      lastName,
      address1: shippingAddress.address1,
      address2: shippingAddress.address2,
      city: shippingAddress.city,
      zip: shippingAddress.zip,
      countryCode: shippingAddress.countryCode,
    },
    billingAddress: {
      firstName,
      lastName,
      address1: shippingAddress.address1,
      address2: shippingAddress.address2,
      city: shippingAddress.city,
      zip: shippingAddress.zip,
      countryCode: shippingAddress.countryCode,
    },
    lineItems,
    shippingLines: [
      {
        title: `App Shipping ${nonEmptyString(data.shippingZone) || ""}`.trim(),
        code: nonEmptyString(data.shippingZone) || "app_shipping",
        source: "Skydown App",
        priceSet: {
          shopMoney: {
            amount: shippingAmount.toFixed(2),
            currencyCode,
          },
        },
      },
    ],
    taxesIncluded: taxAmount > 0,
    taxLines: taxAmount > 0 ? [
      {
        title: "VAT",
        rate: Number(taxRate.toFixed(4)),
        priceSet: {
          shopMoney: {
            amount: taxAmount.toFixed(2),
            currencyCode,
          },
        },
      },
    ] : undefined,
    tags: ["skydown-app", "podpartner", nonEmptyString(data.shippingZone) || "unknown-zone"],
    test: false,
  };
}

async function createShopifyOrder({orderId, data}) {
  const token = await loadShopifyAdminToken();
  if (!token) {
    throw new Error("Kein Shopify Admin API Token hinterlegt.");
  }
  const config = await loadShopifyAdminConfig();

  const mutation = `
    mutation CreateExternalOrder($order: OrderCreateOrderInput!, $options: OrderCreateOptionsInput) {
      orderCreate(order: $order, options: $options) {
        userErrors {
          field
          message
        }
        order {
          id
          name
          displayFinancialStatus
          lineItems(first: 10) {
            nodes {
              title
              quantity
              variant {
                id
              }
            }
          }
        }
      }
    }
  `;

  const variables = {
    order: buildShopifyOrderInput(orderId, data),
    options: {
      sendReceipt: false,
      sendFulfillmentReceipt: false,
      inventoryBehaviour: "BYPASS",
    },
  };

  const response = await shopifyGraphqlRequest({
    query: mutation,
    variables,
    token,
    storeDomain: config.storeDomain,
  });
  const result = response?.orderCreate;
  const userErrors = result?.userErrors || [];

  if (userErrors.length > 0) {
    throw new Error(userErrors.map((error) => `${error.field?.join(".") || "order"}: ${error.message}`).join(" | "));
  }

  if (!result?.order?.id) {
    throw new Error("Shopify hat keine Order-ID zurueckgegeben.");
  }

  return {
    shopifyOrderId: result.order.id,
    shopifyOrderName: result.order.name || "",
    storeDomain: config.storeDomain,
  };
}

function shouldSubmitToShopify(beforeData, afterData) {
  if (!afterData || afterData.fulfillmentProvider !== "podpartner") {
    return false;
  }

  if (afterData.paymentStatus !== "confirmed") {
    return false;
  }

  if (afterData.shopifyOrderId || afterData.shopifySyncStatus === "submitted") {
    return false;
  }

  if (beforeData?.paymentStatus === "confirmed" && beforeData?.shopifySyncStatus === afterData.shopifySyncStatus) {
    return false;
  }

  return true;
}

exports.submitMerchOrder = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  const orderData = normalizeOrderSubmissionPayload(request.data, request.auth);
  const orderRef = await admin.firestore().collection("orders").add(orderData);

  logger.info("Merch order created.", {
    orderId: orderRef.id,
    userEmail: orderData.userEmail,
    itemCount: orderData.items.length,
    fulfillmentProvider: orderData.fulfillmentProvider,
    shippingZone: orderData.shippingZone,
  });

  return {
    orderId: orderRef.id,
    message: "Bestellung erfolgreich angelegt.",
  };
});

exports.confirmMerchOrderPayment = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertOwner(request.auth);

  const orderId = nonEmptyString(request.data?.orderId);
  if (!orderId) {
    throw new HttpsError("invalid-argument", "orderId fehlt.");
  }

  const orderRef = admin.firestore().doc(`orders/${orderId}`);
  const orderSnapshot = await orderRef.get();
  if (!orderSnapshot.exists) {
    throw new HttpsError("not-found", "Bestellung wurde nicht gefunden.");
  }

  const orderData = orderSnapshot.data() || {};
  const paymentMethod = nonEmptyString(request.data?.paymentMethod)
    || nonEmptyString(orderData.paymentMethod)
    || "Extern bezahlt";
  const paymentReference = nonEmptyString(request.data?.paymentReference);
  const requiresShopifySubmission = orderData.fulfillmentProvider === "podpartner";

  await orderRef.set({
    paymentMethod,
    paymentStatus: "confirmed",
    shopifySyncStatus: requiresShopifySubmission ? "pending_submission" : (orderData.shopifySyncStatus || "not_required"),
    paymentConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
    paymentConfirmedByUid: request.auth.uid,
    paymentConfirmedByEmail: `${request.auth.token?.email || ""}`.trim().toLowerCase() || admin.firestore.FieldValue.delete(),
    paymentReference: paymentReference || admin.firestore.FieldValue.delete(),
  }, {merge: true});

  logger.info("Merch payment confirmed.", {
    orderId,
    paymentMethod,
    requiresShopifySubmission,
    confirmedByUid: request.auth.uid,
  });

  return {
    message: requiresShopifySubmission
      ? "Zahlung bestaetigt. Shopify-Order wird jetzt erstellt."
      : "Zahlung bestaetigt.",
  };
});

exports.triggerWorkflowAutomation = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertOwner(request.auth);

  const trigger = nonEmptyString(request.data?.trigger) || "admin_settings_test";
  const source = nonEmptyString(request.data?.source) || "settings";
  const data = request.data?.data && typeof request.data.data === "object" && !Array.isArray(request.data.data)
    ? request.data.data
    : {};

  return triggerWorkflowAutomationWebhook({
    trigger,
    source,
    auth: request.auth,
    data,
  });
});

exports.authorizeAiUsage = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  const kind = nonEmptyString(request.data?.kind)?.toLowerCase() || AI_USAGE_KINDS.text;
  return authorizeAiUsage({
    auth: request.auth,
    kind,
  });
});

function responseFrameworkHint(prompt) {
  const lower = prompt.toLowerCase();

  if (["briefing", "brief", "shooting", "video", "videography", "shotlist"].some((word) => lower.includes(word))) {
    return "Antwortformat: Ziel, Stil, Deliverables, Shotlist oder Content-Bausteine, Assets, Risiken, Naechste Schritte.";
  }

  if (["release", "launch", "plan", "timeline", "zeitplan", "content-plan"].some((word) => lower.includes(word))) {
    return "Antwortformat: Ziel, 5 bis 10 konkrete Schritte mit Reihenfolge, Timing, benoetigte Assets, Risiken, Naechste 3 Schritte.";
  }

  if (["merch", "drop", "shop"].some((word) => lower.includes(word))) {
    return "Antwortformat: Ziel, Vorbereitung, Content, Launch-Abfolge, Checkliste, Risiken, Naechste 3 Schritte.";
  }

  return "Antwortformat: Ziel, konkrete Umsetzung, benoetigte Assets, Naechste 3 Schritte, offene Fragen nur wenn wirklich noetig.";
}

function formatHistory(history) {
  if (!history.length) {
    return "Noch keine bisherige Unterhaltung.";
  }

  return history
      .map(({role, text}) => `${role === "user" ? "Nutzer" : "Agent"}:\n${text}`)
      .join("\n\n");
}

const skydownAgentFlow = ai.defineFlow({
  name: "skydownAgentFlow",
  inputSchema: agentRequestSchema,
  outputSchema: z.string(),
  streamSchema: z.string(),
}, async (input, sendChunk) => {
  const prompt = `
Bisherige Unterhaltung:
${formatHistory(input.history)}

Aktuelle Nutzeranfrage:
${input.prompt}

${responseFrameworkHint(input.prompt)}
  `.trim();

  const {stream, response} = ai.generateStream({
    system: systemPrompt,
    prompt,
    config: {
      temperature: 0.7,
      maxOutputTokens: 1024,
    },
  });

  for await (const chunk of stream) {
    if (chunk.text) {
      sendChunk(chunk.text);
    }
  }

  const finalText = (await response).text?.trim();
  if (!finalText) {
    throw new HttpsError(
        "internal",
        "Skydown Agent konnte keine Antwort erzeugen.",
    );
  }

  return finalText;
});

exports.skydownAgent = onCallGenkit({
  region: "us-central1",
  enforceAppCheck: false,
  timeoutSeconds: 60,
  authPolicy: canUseAiAuth,
}, skydownAgentFlow);

exports.syncShopifyMerch = onCall({
  region: "us-central1",
  timeoutSeconds: 120,
}, async (request) => {
  await assertOwner(request.auth);
  return runShopifyMerchSync();
});

exports.processConfirmedMerchOrders = onDocumentWritten({
  document: "orders/{orderId}",
  region: "us-central1",
  timeoutSeconds: 120,
}, async (event) => {
  const beforeData = event.data?.before?.data() || null;
  const afterData = event.data?.after?.data() || null;
  const orderId = event.params.orderId;

  if (!afterData) {
    return;
  }

  if (!shouldSubmitToShopify(beforeData, afterData)) {
    return;
  }

  const orderRef = admin.firestore().doc(`orders/${orderId}`);

  logger.info("Shopify order submission started.", {
    orderId,
    shippingZone: afterData.shippingZone || null,
    fulfillmentProvider: afterData.fulfillmentProvider || null,
  });

  try {
    await orderRef.set({
      shopifySyncStatus: "submitting",
      shopifySyncError: admin.firestore.FieldValue.delete(),
      shopifySyncUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    const {shopifyOrderId, shopifyOrderName, storeDomain} = await createShopifyOrder({
      orderId,
      data: afterData,
    });

    await orderRef.set({
      shopifyOrderId,
      shopifyOrderName,
      shopifyStoreDomain: storeDomain,
      shopifySyncStatus: "submitted",
      fulfillmentStatus: "submitted_to_shopify",
      shopifySyncUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    logger.info("Shopify order created successfully.", {
      orderId,
      shopifyOrderId,
      shopifyOrderName,
      storeDomain,
    });
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : "Unbekannter Shopify-Fehler.";
    logger.error("Shopify order creation failed.", {
      orderId,
      error: errorMessage,
    });

    await orderRef.set({
      shopifySyncStatus: "failed",
      fulfillmentStatus: "submission_failed",
      shopifySyncError: errorMessage,
      shopifySyncUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
  }
});

function formatOrderItems(items) {
  if (!Array.isArray(items) || items.length === 0) {
    return "- Keine Artikel";
  }

  return items.map((item) => {
    const name = item?.name || "Unbekannter Artikel";
    const quantity = item?.quantity || 0;
    const size = item?.size || "Nicht angegeben";
    return `- ${name} | Groesse: ${size} | Menge: ${quantity}`;
  }).join("\n");
}

function formatOrderNotification(orderId, data) {
  const customerName = data.customerName || "Nicht angegeben";
  const customerEmail = data.customerEmail || data.userEmail || "Nicht angegeben";
  const whatsApp = data.whatsApp || "Nicht angegeben";
  const message = data.message || "Keine zusaetzliche Nachricht.";
  const itemSummary = formatOrderItems(data.items);
  const createdAt = data.timestamp?.toDate?.()?.toISOString?.() || new Date().toISOString();

  const subject = `Neue Skydown Bestellung - ${customerEmail}`;
  const text = `
Hallo Skydown-Team,

es wurde eine neue Bestellung in der Skydown App erstellt.

Bestell-ID: ${orderId}
Erstellt am: ${createdAt}
Account-E-Mail: ${data.userEmail || "Nicht angegeben"}
Kontaktname: ${customerName}
Kontakt-E-Mail: ${customerEmail}
WhatsApp: ${whatsApp}

Warenkorb:
${itemSummary}

Nachricht:
${message}
  `.trim();

  return {subject, text};
}

exports.notifyOrderCreated = onDocumentCreated({
  document: "orders/{orderId}",
  region: "us-central1",
  secrets: [smtpConnectionUrl],
}, async (event) => {
  const snapshot = event.data;
  if (!snapshot) {
    logger.warn("Order notification skipped because no snapshot data was provided.");
    return;
  }

  const data = snapshot.data();
  if (!data) {
    logger.warn("Order notification skipped because the order document was empty.");
    return;
  }

  const connectionUrl = smtpConnectionUrl.value();
  if (!connectionUrl) {
    logger.error("SMTP_CONNECTION_URL is not configured. Cannot send order notification.");
    return;
  }

  const recipient = process.env.ORDER_NOTIFICATION_TO || "skydownent@gmail.com";
  const from = process.env.ORDER_NOTIFICATION_FROM || "Skydown Orders <skydownent@gmail.com>";
  const {subject, text} = formatOrderNotification(event.params.orderId, data);

  const transporter = nodemailer.createTransport(connectionUrl);
  await transporter.sendMail({
    from,
    to: recipient,
    replyTo: data.customerEmail || data.userEmail || undefined,
    subject,
    text,
  });

  logger.info("Order notification sent.", {
    orderId: event.params.orderId,
    recipient,
    customerEmail: data.customerEmail || data.userEmail || null,
  });
});
