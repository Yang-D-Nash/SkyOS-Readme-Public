"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const functionsV1 = require("firebase-functions/v1");
const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onMessagePublished} = require("firebase-functions/v2/pubsub");
const {enableFirebaseTelemetry} = require("@genkit-ai/firebase");
const {onCall, onRequest, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {genkit, z} = require("genkit");
const {vertexAI} = require("@genkit-ai/google-genai");
const nodemailer = require("nodemailer");
const {assertAppCheck} = require("./src/security/app-check");
const {
  BILLING_LOCKDOWN_REASON_PREFIX,
} = require("./src/security/constants");
const {
  assertAdmin: assertAdminClaim,
  assertOwner: assertOwnerClaim,
  defaultQuotaPlanForRole,
  resolveRoleFromClaims,
  setUserRoleClaims,
  syncClaimsForCurrentUser,
} = require("./src/security/roles");
const {
  areRegistrationsBlocked,
  getRuntimeConfig,
  mergeRuntimeConfig,
} = require("./src/security/runtime-config");
const {requestUploadSlot} = require("./src/security/upload-slots");
const {
  createAiSubscriptionCheckoutSession,
  createHostedCheckoutSession,
  deriveStripeCheckoutStatus,
  extractStripeCheckoutIdentifiers,
  formatStripeShippingAddress,
  isHostedCheckoutMethod,
  normalizeCheckoutPlatform,
  normalizeHostedCheckoutMethod,
  renderCheckoutReturnPage,
  shouldConfirmPaymentFromStripeEvent,
  verifyStripeWebhookSignature,
} = require("./src/payments/stripe-checkout");
const {
  addSecretVersion,
} = require("./src/payments/secret-manager");

admin.initializeApp();
void enableFirebaseTelemetry().catch((error) => {
  console.error("Firebase telemetry could not be enabled.", error);
});

const ai = genkit({
  plugins: [vertexAI({location: "us-central1"})],
  model: vertexAI.model("gemini-2.5-flash-lite"),
});

const smtpConnectionUrl = defineSecret("SMTP_CONNECTION_URL");
const stripeSecretKey = defineSecret("STRIPE_SECRET_KEY");
const stripeWebhookSecret = defineSecret("STRIPE_WEBHOOK_SECRET");
const manusApiKey = defineSecret("MANUS_API_KEY");
const OWNER_EMAIL = "nash.lioncorna@gmail.com";
const SHOPIFY_STORE_DOMAIN_DEFAULT = "k5t1sc-ps.myshopify.com";
const SHOPIFY_API_VERSION = "2026-01";
const SHOPIFY_CONFIG_COLLECTION = "appConfig";
const SHOPIFY_CONFIG_DOCUMENT = "shopifyMerch";
const SHOPIFY_PRIVATE_CONFIG_COLLECTION = "adminConfig";
const SHOPIFY_PRIVATE_CONFIG_DOCUMENT = "shopifyMerchPrivate";
const AUTOMATION_CONFIG_COLLECTION = "adminConfig";
const AUTOMATION_CONFIG_DOCUMENT = "automationN8n";
const PERSONAL_AGENT_PROFILE_DOCUMENT_PREFIX = "agentProfile_";
const AI_PROMPT_SETTINGS_COLLECTION = "adminConfig";
const AI_PROMPT_SETTINGS_DOCUMENT = "aiPromptSettings";
const STRIPE_SECRET_STATUS_COLLECTION = "adminConfig";
const STRIPE_SECRET_STATUS_DOCUMENT = "stripeCheckoutSecrets";
const SHOPIFY_VARIANT_OPTION_KEYS = {
  size: ["size", "groesse", "größe"],
  color: ["color", "colour", "farbe"],
};
const SHOPIFY_STOREFRONT_PRODUCT_FIELDS = `
  id
  title
  description
  handle
  collections(first: 20) {
    nodes {
      handle
      title
    }
  }
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
const DEFAULT_COMMERCE_SETTINGS = Object.freeze({
  shipping: {
    domesticCost: 4.90,
    euCost: 6.90,
    internationalCost: 11.90,
    freeShippingThreshold: 89.0,
  },
  invoice: {
    taxRate: 19.0,
  },
});
const EU_COUNTRY_CODES = new Set([
  "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
  "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
]);

const agentTurnSchema = z.object({
  role: z.enum(["user", "assistant"]),
  text: z.string().trim().min(1).max(4000),
});

const agentRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(4000),
  history: z.array(agentTurnSchema).max(24).default([]),
  mode: z.enum(["release", "briefing", "content", "merch", "automation"]).default("release"),
  executeAutomation: z.boolean().default(false),
  manusApiKeyOverride: z.string().trim().min(16).max(1024).optional(),
});

const agentFlowRequestSchema = agentRequestSchema.omit({
  manusApiKeyOverride: true,
}).extend({
  systemInstruction: z.string().trim().min(1).max(12000),
  workspaceContext: z.string().trim().max(12000).default(""),
});

const aiTextRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(12000),
  mode: z.enum(["general", "caption", "release_plan", "briefing", "merch_copy", "video_concept"]).default("general"),
});

const aiVisualRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(12000),
});

const DEFAULT_AI_TEXT_INSTRUCTION = `
Du bist der 22xSky Bot, der kreative Copy- und Content-Assistent fuer Skydown Entertainment.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die App verbindet Musik, Videos, Merch und Creator-Tools.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Antworte auf Deutsch.
Sei direkt nutzbar, markentauglich, modern und nicht generisch.
Keine langen Vorreden, keine Erklaerungen ueber deinen Prozess.
Schreibe lieber Ergebnisse als Theorie.
Wenn die Anfrage nach Caption, Hook, Claim, Reel oder Post klingt, liefere echte copy-pastebare Optionen.
Wenn die Anfrage eher nach Planung, Freigaben, Briefing oder To-dos klingt, antworte kurz hilfreich, verweise aber auf den Agent fuer die tiefe Struktur.
`.trim();

const DEFAULT_AI_VISUAL_INSTRUCTION = `
Du bist der 22xSky Bot und generierst genau ein starkes Key-Visual fuer Skydown Entertainment.
Markenkontext:
- Skydown Entertainment kommt aus Hip Hop und kollaboriert mit 22 aus Hamburg.
- Die Marke lebt von Musik, Videos, Street-Culture und Premium-Underground-Aesthetik.
- Yang D. Nash ist Kern der Marke und Entwickler der App.

Erzeuge ein modernes, hochwertiges Visual mit klarer Stimmung.
Stil: cinematic, urban, moody, premium, nicht kitschig, nicht generisch.
Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
Antworte auf Deutsch.
`.trim();

const DEFAULT_AGENT_SYSTEM_PROMPT = `
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

const DEFAULT_AI_PROMPT_SETTINGS = Object.freeze({
  textInstruction: DEFAULT_AI_TEXT_INSTRUCTION,
  visualInstruction: DEFAULT_AI_VISUAL_INSTRUCTION,
  agentSystemInstruction: DEFAULT_AGENT_SYSTEM_PROMPT,
  assetLibraryLink: "",
  assetReferenceNotes: "",
});

const USER_ROLES = {
  owner: "owner",
  admin: "admin",
  subadmin: "subadmin",
  user: "user",
};

const USER_QUOTA_PLANS = {
  ownerUnlimited: "owner_unlimited",
  internalTeam: "internal_team",
  free: "free",
  creator: "creator",
  studio: "studio",
};

const AI_USAGE_KINDS = {
  text: "text",
  visual: "visual",
  agent: "agent",
};

const AI_ACCESS_MODES = {
  off: "off",
  adminOnly: "admin_only",
  signedIn: "signed_in",
};

const AI_TEXT_MODES = {
  general: "general",
  caption: "caption",
  releasePlan: "release_plan",
  briefing: "briefing",
  merchCopy: "merch_copy",
  videoConcept: "video_concept",
};

const AGENT_MODES = {
  release: "release",
  briefing: "briefing",
  content: "content",
  merch: "merch",
  automation: "automation",
};

const AI_AGENT_PROVIDERS = {
  gemini: "gemini",
  manus: "manus",
};
const AI_SUBSCRIPTION_PLANS = Object.freeze([
  USER_QUOTA_PLANS.creator,
  USER_QUOTA_PLANS.studio,
]);
const AI_SUBSCRIPTION_ACTIVE_STATUSES = Object.freeze(["active", "trialing"]);

const AI_REMOTE_CONFIG_CACHE_TTL_MS = 5 * 60 * 1000;
const AI_PROMPT_SETTINGS_CACHE_TTL_MS = 60 * 1000;
const AI_RUNTIME_CONFIG_CACHE_TTL_MS = 60 * 1000;
const AI_RUNTIME_CONFIG_COLLECTION = "adminConfig";
const AI_RUNTIME_CONFIG_DOCUMENT = "aiRuntime";
const AI_USAGE_METRICS_COLLECTION = "systemMetrics";
const AI_USAGE_METRICS_DOCUMENT_PREFIX = "aiUsageDaily_";
const MANUS_API_BASE_URL = "https://api.manus.ai/v2";
const ACCOUNT_DELETE_RECENT_AUTH_MAX_AGE_SECONDS = 5 * 60;

const DEFAULT_AI_RUNTIME_SETTINGS = Object.freeze({
  costGuardEnabled: true,
  agentProvider: AI_AGENT_PROVIDERS.gemini,
  fallbackAgentProvider: AI_AGENT_PROVIDERS.gemini,
  hardDailyCaps: {
    text: 120,
    visual: 20,
    agent: 40,
  },
  globalDailyCaps: {
    text: 1500,
    visual: 180,
    agent: 350,
  },
  manus: {
    isEnabled: false,
    requestTimeoutMs: 12000,
    pollIntervalMs: 1500,
    maxPollAttempts: 18,
    listMessagesLimit: 30,
    maxPromptChars: 2400,
    maxHistoryTurns: 12,
    autoStopOnWaiting: true,
    blockHighCreditEvents: true,
    includeVerboseEvents: false,
  },
});

const DEFAULT_PERSONAL_AGENT_PROFILE_SETTINGS = Object.freeze({
  isEnabled: false,
  roleLabel: "",
  skillProfile: "",
  outputFormat: "",
  guardrails: "",
  knowledgeContext: "",
});

let aiFeatureConfigCache = {
  expiresAt: 0,
  values: null,
};

let aiPromptSettingsCache = {
  expiresAt: 0,
  values: null,
};

let aiRuntimeSettingsCache = {
  expiresAt: 0,
  values: null,
};

function resolveRoleFromAuthClaims(auth) {
  return resolveRoleFromClaims(auth);
}

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
  return defaultAiLimitsForQuotaPlan(defaultQuotaPlanForRole(role));
}

function defaultAiLimitsForQuotaPlan(plan) {
  switch (plan) {
    case USER_QUOTA_PLANS.ownerUnlimited:
      return {text: 5000, visual: 1200, agent: 3000, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.internalTeam:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.creator:
      return {text: 120, visual: 20, agent: 70, historyRetentionDays: 7};
    case USER_QUOTA_PLANS.studio:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.free:
    default:
      return {text: 30, visual: 4, agent: 18, historyRetentionDays: 3};
  }
}

function normalizeAiSubscriptionPlan(value) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  return normalized && AI_SUBSCRIPTION_PLANS.includes(normalized) ? normalized : null;
}

function normalizeAiSubscriptionStatus(value, fallback = "inactive") {
  const normalized = nonEmptyString(value)?.toLowerCase();
  return normalized || fallback;
}

function isAiSubscriptionStatusActive(value) {
  return AI_SUBSCRIPTION_ACTIVE_STATUSES.includes(normalizeAiSubscriptionStatus(value, ""));
}

function resolveAiSubscriptionStatusFromCheckoutEvent(eventType) {
  if (eventType === "checkout.session.completed" ||
    eventType === "checkout.session.async_payment_succeeded") {
    return "active";
  }
  if (eventType === "checkout.session.async_payment_failed") {
    return "past_due";
  }
  if (eventType === "checkout.session.expired") {
    return "expired";
  }
  return "checkout_pending";
}

function resolveAiSubscriptionStatusFromLifecycleEvent(eventType, subscriptionObject = {}) {
  if (eventType === "customer.subscription.deleted") {
    return "canceled";
  }
  return normalizeAiSubscriptionStatus(subscriptionObject.status, "inactive");
}

function resolveAiSubscriptionMetadata(stripeObject = {}) {
  const metadata = stripeObject?.metadata && typeof stripeObject.metadata === "object" &&
    !Array.isArray(stripeObject.metadata) ?
      stripeObject.metadata :
      {};

  return {
    type: nonEmptyString(metadata.type)?.toLowerCase() || "",
    userId: nonEmptyString(metadata.userId) || nonEmptyString(stripeObject?.client_reference_id),
    plan: normalizeAiSubscriptionPlan(metadata.plan),
    priceId: nonEmptyString(metadata.priceId),
  };
}

function resolveAiLimitsUpdateForPlan(plan) {
  const normalizedPlan = normalizeAiSubscriptionPlan(plan) || USER_QUOTA_PLANS.creator;
  const defaults = defaultAiLimitsForQuotaPlan(normalizedPlan);
  return {
    quotaPlan: normalizedPlan,
    aiTextRequestsPerDay: defaults.text,
    aiVisualRequestsPerDay: defaults.visual,
    aiAgentRequestsPerDay: defaults.agent,
    aiHistoryRetentionDays: defaults.historyRetentionDays,
  };
}

function resolveAiLimitsUpdateForRole(role) {
  return resolveAiLimitsUpdateForPlan(defaultQuotaPlanForRole(role));
}

function resolveAiLimits(userData = {}) {
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const quotaPlan = typeof userData.quotaPlan === "string" &&
    Object.values(USER_QUOTA_PLANS).includes(userData.quotaPlan) ?
      userData.quotaPlan :
      defaultQuotaPlanForRole(role);
  const defaults = defaultAiLimitsForQuotaPlan(quotaPlan);
  const text = Number(userData.aiTextRequestsPerDay);
  const visual = Number(userData.aiVisualRequestsPerDay);
  const agent = Number(userData.aiAgentRequestsPerDay);
  const historyRetentionDays = Number(userData.aiHistoryRetentionDays);

  return {
    role,
    quotaPlan,
    isEnabled: userData.aiAccessEnabled !== false && userData.aiConsentGiven !== false,
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

function resolveAuthUserRegistrationEpochMillis(userRecord) {
  const metadataCreationTime = nonEmptyString(userRecord?.metadata?.creationTime);
  if (metadataCreationTime) {
    const parsed = Date.parse(metadataCreationTime);
    if (Number.isFinite(parsed) && parsed > 0) {
      return Math.floor(parsed);
    }
  }

  const metadataTimestamp = Number(userRecord?.metadata?.creationTimestamp);
  if (Number.isFinite(metadataTimestamp) && metadataTimestamp > 0) {
    return Math.floor(metadataTimestamp);
  }

  return Date.now();
}

function resolveAuthUserBootstrapUsername(userRecord, normalizedEmail = null) {
  const fallbackName = nonEmptyString(normalizedEmail)?.split("@")[0] || "Skydown User";
  const candidate = nonEmptyString(userRecord?.displayName) || fallbackName;
  return candidate.slice(0, 32).trim() || "Skydown User";
}

async function ensureAuthUserBootstrapDocument(userRecord, {forcedRole = null} = {}) {
  const uid = nonEmptyString(userRecord?.uid);
  if (!uid) {
    return;
  }

  const userRef = admin.firestore().doc(`users/${uid}`);
  const snapshot = await userRef.get();
  const existingData = snapshot.exists ? (snapshot.data() || {}) : {};
  const normalizedEmail = normalizeEmail(userRecord?.email) || normalizeEmail(existingData.email);

  const effectiveRole = Object.values(USER_ROLES).includes(forcedRole) ?
    forcedRole :
    resolveUserRole(existingData.role, existingData.isAdmin === true, normalizedEmail);
  const defaultQuotaPlan = defaultQuotaPlanForRole(effectiveRole);
  const defaultLimits = defaultAiLimitsForQuotaPlan(defaultQuotaPlan);
  const updates = {};

  if (!nonEmptyString(existingData.email) && normalizedEmail) {
    updates.email = normalizedEmail;
  }

  if (!nonEmptyString(existingData.username)) {
    updates.username = resolveAuthUserBootstrapUsername(userRecord, normalizedEmail);
  }

  if (!Number.isFinite(Number(existingData.registrationDateEpochMillis))) {
    updates.registrationDateEpochMillis = resolveAuthUserRegistrationEpochMillis(userRecord);
  }

  if (effectiveRole === USER_ROLES.owner) {
    updates.role = USER_ROLES.owner;
    updates.isAdmin = true;
    updates.quotaPlan = USER_QUOTA_PLANS.ownerUnlimited;
    updates.aiAccessEnabled = true;
    if (!(existingData.termsAcceptedAt instanceof admin.firestore.Timestamp)) {
      updates.termsAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }
    if (!(existingData.privacyAcceptedAt instanceof admin.firestore.Timestamp)) {
      updates.privacyAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }
    if (!nonEmptyString(existingData.termsVersion)) {
      updates.termsVersion = "legacy";
    }
    if (!nonEmptyString(existingData.privacyVersion)) {
      updates.privacyVersion = "legacy";
    }
    if (!nonEmptyString(existingData.legalConsentSource)) {
      updates.legalConsentSource = "server_bootstrap";
    }
    updates.aiConsentGiven = true;
    if (!(existingData.aiConsentUpdatedAt instanceof admin.firestore.Timestamp)) {
      updates.aiConsentUpdatedAt = admin.firestore.FieldValue.serverTimestamp();
    }
    if (!nonEmptyString(existingData.aiConsentSource)) {
      updates.aiConsentSource = "server_bootstrap";
    }
    updates.aiTextRequestsPerDay = defaultLimits.text;
    updates.aiVisualRequestsPerDay = defaultLimits.visual;
    updates.aiAgentRequestsPerDay = defaultLimits.agent;
    updates.aiHistoryRetentionDays = defaultLimits.historyRetentionDays;
    updates.canManageMusicCatalog = true;
    updates.canManageVideoCatalog = true;
    updates.canModerateProfiles = true;
  } else {
    if (!Object.values(USER_ROLES).includes(nonEmptyString(existingData.role)?.toLowerCase())) {
      updates.role = effectiveRole;
    }

    if (typeof existingData.isAdmin !== "boolean") {
      updates.isAdmin = roleHasAdminAccess(effectiveRole);
    }

    if (!Object.values(USER_QUOTA_PLANS).includes(nonEmptyString(existingData.quotaPlan)?.toLowerCase())) {
      updates.quotaPlan = defaultQuotaPlan;
    }

    if (typeof existingData.aiAccessEnabled !== "boolean") {
      updates.aiAccessEnabled = true;
    }

    if (!(existingData.termsAcceptedAt instanceof admin.firestore.Timestamp)) {
      updates.termsAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (!(existingData.privacyAcceptedAt instanceof admin.firestore.Timestamp)) {
      updates.privacyAcceptedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (!nonEmptyString(existingData.termsVersion)) {
      updates.termsVersion = "legacy";
    }

    if (!nonEmptyString(existingData.privacyVersion)) {
      updates.privacyVersion = "legacy";
    }

    if (!nonEmptyString(existingData.legalConsentSource)) {
      updates.legalConsentSource = "server_bootstrap";
    }

    if (typeof existingData.aiConsentGiven !== "boolean") {
      updates.aiConsentGiven = existingData.aiAccessEnabled !== false;
    }

    if (!(existingData.aiConsentUpdatedAt instanceof admin.firestore.Timestamp)) {
      updates.aiConsentUpdatedAt = admin.firestore.FieldValue.serverTimestamp();
    }

    if (!nonEmptyString(existingData.aiConsentSource)) {
      updates.aiConsentSource = "server_bootstrap";
    }

    if (!Number.isFinite(Number(existingData.aiTextRequestsPerDay))) {
      updates.aiTextRequestsPerDay = defaultLimits.text;
    }

    if (!Number.isFinite(Number(existingData.aiVisualRequestsPerDay))) {
      updates.aiVisualRequestsPerDay = defaultLimits.visual;
    }

    if (!Number.isFinite(Number(existingData.aiAgentRequestsPerDay))) {
      updates.aiAgentRequestsPerDay = defaultLimits.agent;
    }

    if (![1, 3, 7, 30].includes(Number(existingData.aiHistoryRetentionDays))) {
      updates.aiHistoryRetentionDays = defaultLimits.historyRetentionDays;
    }

    if (typeof existingData.canManageMusicCatalog !== "boolean") {
      updates.canManageMusicCatalog = false;
    }

    if (typeof existingData.canManageVideoCatalog !== "boolean") {
      updates.canManageVideoCatalog = false;
    }

    if (typeof existingData.canModerateProfiles !== "boolean") {
      updates.canModerateProfiles = false;
    }
  }

  if (Object.keys(updates).length === 0) {
    return;
  }

  await userRef.set(updates, {merge: true});
  logger.info("User bootstrap document ensured on auth create.", {
    uid,
    appliedFields: Object.keys(updates),
  });
}

async function loadUserData(uid) {
  if (!uid) {
    return null;
  }

  const userSnapshot = await admin.firestore().doc(`users/${uid}`).get();
  return userSnapshot.exists ? (userSnapshot.data() || {}) : null;
}

function resolveAiAccessMode(value) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  return Object.values(AI_ACCESS_MODES).includes(normalized) ?
    normalized :
    AI_ACCESS_MODES.signedIn;
}

function resolveRemoteConfigBoolean(value, fallbackValue) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (normalized === "true") {
    return true;
  }

  if (normalized === "false") {
    return false;
  }

  return fallbackValue;
}

async function loadAiFeatureConfig() {
  const now = Date.now();
  if (aiFeatureConfigCache.values && aiFeatureConfigCache.expiresAt > now) {
    return aiFeatureConfigCache.values;
  }

  let values = {
    isEnabled: true,
    accessMode: AI_ACCESS_MODES.signedIn,
  };

  try {
    // Enforce the published global AI rollout server-side as well.
    const template = await admin.remoteConfig().getTemplate();
    const parameters = template?.parameters || {};
    values = {
      isEnabled: resolveRemoteConfigBoolean(parameters.ai_enabled?.defaultValue?.value, true),
      accessMode: resolveAiAccessMode(parameters.ai_access_mode?.defaultValue?.value),
    };
  } catch (error) {
    logger.warn("AI Remote Config could not be loaded. Using cached/default AI access policy.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
  }

  aiFeatureConfigCache = {
    expiresAt: now + AI_REMOTE_CONFIG_CACHE_TTL_MS,
    values,
  };

  return values;
}

function normalizeAiPromptSetting(value, fallback) {
  const normalized = nonEmptyString(value);
  if (!normalized) {
    return fallback;
  }

  return normalized.slice(0, 12000);
}

function normalizePersonalAgentProfileSetting(value, maxChars = 4000) {
  const normalized = nonEmptyString(value);
  if (!normalized) {
    return "";
  }
  return normalized.slice(0, maxChars);
}

function resolveAiPromptSettings(data = {}) {
  return {
    textInstruction: normalizeAiPromptSetting(
        data.textInstruction,
        DEFAULT_AI_PROMPT_SETTINGS.textInstruction,
    ),
    visualInstruction: normalizeAiPromptSetting(
        data.visualInstruction,
        DEFAULT_AI_PROMPT_SETTINGS.visualInstruction,
    ),
    agentSystemInstruction: normalizeAiPromptSetting(
        data.agentSystemInstruction,
        DEFAULT_AI_PROMPT_SETTINGS.agentSystemInstruction,
    ),
    assetLibraryLink: normalizeUrlString(data.assetLibraryLink) || "",
    assetReferenceNotes: normalizeAiPromptSetting(
        data.assetReferenceNotes,
        DEFAULT_AI_PROMPT_SETTINGS.assetReferenceNotes,
    ),
  };
}

async function loadAiPromptSettings() {
  const now = Date.now();
  if (aiPromptSettingsCache.values && aiPromptSettingsCache.expiresAt > now) {
    return aiPromptSettingsCache.values;
  }

  let values = DEFAULT_AI_PROMPT_SETTINGS;
  try {
    const snapshot = await admin.firestore()
        .collection(AI_PROMPT_SETTINGS_COLLECTION)
        .doc(AI_PROMPT_SETTINGS_DOCUMENT)
        .get();
    values = resolveAiPromptSettings(snapshot.data() || {});
  } catch (error) {
    logger.warn("AI prompt settings could not be loaded. Falling back to defaults.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
  }

  aiPromptSettingsCache = {
    expiresAt: now + AI_PROMPT_SETTINGS_CACHE_TTL_MS,
    values,
  };

  return values;
}

function clampIntegerSetting(value, fallback, min, max) {
  const numericValue = Number(value);
  if (!Number.isFinite(numericValue)) {
    return fallback;
  }

  const rounded = Math.floor(numericValue);
  return Math.max(min, Math.min(max, rounded));
}

function resolveAiKindLimits(raw, fallback, min, max) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    text: clampIntegerSetting(source.text, fallback.text, min, max),
    visual: clampIntegerSetting(source.visual, fallback.visual, min, max),
    agent: clampIntegerSetting(source.agent, fallback.agent, min, max),
  };
}

function resolveAgentProvider(value, fallback = AI_AGENT_PROVIDERS.gemini) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (normalized && Object.values(AI_AGENT_PROVIDERS).includes(normalized)) {
    return normalized;
  }

  return fallback;
}

function resolveAiRuntimeSettings(data = {}) {
  const manusConfig = data.manus && typeof data.manus === "object" && !Array.isArray(data.manus) ? data.manus : {};
  const agentProvider = resolveAgentProvider(
      data.agentProvider,
      DEFAULT_AI_RUNTIME_SETTINGS.agentProvider,
  );
  const fallbackAgentProvider = resolveAgentProvider(
      data.fallbackAgentProvider,
      DEFAULT_AI_RUNTIME_SETTINGS.fallbackAgentProvider,
  );

  return {
    costGuardEnabled: data.costGuardEnabled !== false,
    agentProvider,
    fallbackAgentProvider: fallbackAgentProvider === agentProvider ?
      AI_AGENT_PROVIDERS.gemini :
      fallbackAgentProvider,
    hardDailyCaps: resolveAiKindLimits(
        data.hardDailyCaps,
        DEFAULT_AI_RUNTIME_SETTINGS.hardDailyCaps,
        1,
        5000,
    ),
    globalDailyCaps: resolveAiKindLimits(
        data.globalDailyCaps,
        DEFAULT_AI_RUNTIME_SETTINGS.globalDailyCaps,
        1,
        100000,
    ),
    manus: {
      isEnabled: manusConfig.isEnabled === true,
      requestTimeoutMs: clampIntegerSetting(
          manusConfig.requestTimeoutMs,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.requestTimeoutMs,
          3000,
          30000,
      ),
      pollIntervalMs: clampIntegerSetting(
          manusConfig.pollIntervalMs,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.pollIntervalMs,
          500,
          5000,
      ),
      maxPollAttempts: clampIntegerSetting(
          manusConfig.maxPollAttempts,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.maxPollAttempts,
          2,
          60,
      ),
      listMessagesLimit: clampIntegerSetting(
          manusConfig.listMessagesLimit,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.listMessagesLimit,
          5,
          100,
      ),
      maxPromptChars: clampIntegerSetting(
          manusConfig.maxPromptChars,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.maxPromptChars,
          300,
          12000,
      ),
      maxHistoryTurns: clampIntegerSetting(
          manusConfig.maxHistoryTurns,
          DEFAULT_AI_RUNTIME_SETTINGS.manus.maxHistoryTurns,
          0,
          24,
      ),
      autoStopOnWaiting: manusConfig.autoStopOnWaiting !== false,
      blockHighCreditEvents: manusConfig.blockHighCreditEvents !== false,
      includeVerboseEvents: manusConfig.includeVerboseEvents === true,
    },
  };
}

async function loadAiRuntimeSettings() {
  const now = Date.now();
  if (aiRuntimeSettingsCache.values && aiRuntimeSettingsCache.expiresAt > now) {
    return aiRuntimeSettingsCache.values;
  }

  let values = DEFAULT_AI_RUNTIME_SETTINGS;
  try {
    const snapshot = await admin.firestore()
        .collection(AI_RUNTIME_CONFIG_COLLECTION)
        .doc(AI_RUNTIME_CONFIG_DOCUMENT)
        .get();
    values = resolveAiRuntimeSettings(snapshot.data() || {});
  } catch (error) {
    logger.warn("AI runtime settings could not be loaded. Falling back to defaults.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
  }

  aiRuntimeSettingsCache = {
    expiresAt: now + AI_RUNTIME_CONFIG_CACHE_TTL_MS,
    values,
  };

  return values;
}

async function assertAiAccess(auth) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Bitte melde dich an, um die KI zu nutzen.");
  }

  const userData = await loadUserData(auth.uid);
  if (!userData) {
    throw new HttpsError("permission-denied", "Dein Konto ist noch nicht vollstaendig eingerichtet.");
  }

  const profile = buildUserProfile(userData);
  const featureConfig = await loadAiFeatureConfig();

  if (!featureConfig.isEnabled || featureConfig.accessMode === AI_ACCESS_MODES.off) {
    throw new HttpsError("permission-denied", "Die KI ist gerade pausiert.");
  }

  if (!profile.aiAccessEnabled) {
    throw new HttpsError("permission-denied", "Die KI ist fuer dein Konto gerade pausiert.");
  }

  if (
    profile.role === USER_ROLES.admin &&
    (
      !AI_SUBSCRIPTION_PLANS.includes(profile.aiLimits.quotaPlan) ||
      !isAiSubscriptionStatusActive(userData.aiSubscriptionStatus)
    )
  ) {
    throw new HttpsError(
        "permission-denied",
        "Admin-KI ist nur mit aktivem Creator- oder Studio-Abo freigeschaltet. Bitte aktiviere dein Abo in den Einstellungen.",
    );
  }

  if (featureConfig.accessMode === AI_ACCESS_MODES.adminOnly && !profile.isStaff) {
    throw new HttpsError("permission-denied", "Die KI ist gerade nur fuer Staff-Konten freigeschaltet.");
  }

  return {
    userData,
    profile,
    featureConfig,
  };
}

async function isAdminAuth(auth) {
  return Boolean(auth?.uid) &&
    [USER_ROLES.owner, USER_ROLES.admin].includes(resolveRoleFromAuthClaims(auth));
}

async function isOwnerAuth(auth) {
  return Boolean(auth?.uid) && resolveRoleFromAuthClaims(auth) === USER_ROLES.owner;
}

async function isStaffAuth(auth) {
  if (!auth?.uid) {
    return false;
  }

  const role = resolveRoleFromAuthClaims(auth);
  return [USER_ROLES.owner, USER_ROLES.admin].includes(role);
}

async function canUseAiAuth(auth) {
  try {
    await assertAiAccess(auth);
    return true;
  } catch (error) {
    logger.debug("AI access denied.", {
      uid: auth?.uid || null,
      code: error instanceof HttpsError ? error.code : "internal",
    });
    return false;
  }
}

async function assertAdmin(auth) {
  assertAdminClaim(auth);
}

async function assertOwner(auth) {
  assertOwnerClaim(auth);
}

async function assertCallableSecurity(request, functionName) {
  const runtimeConfig = await getRuntimeConfig();
  return assertAppCheck(request, runtimeConfig, functionName);
}

function assertAuthenticatedUser(auth, message = "Bitte melde dich an.") {
  const uid = nonEmptyString(auth?.uid);
  if (!uid) {
    throw new HttpsError("unauthenticated", message);
  }

  return uid;
}

function assertRecentAccountDeletionAuth(auth) {
  const uid = assertAuthenticatedUser(
      auth,
      "Bitte melde dich an, bevor du dein Konto loeschst.",
  );
  const authTimeSeconds = Number(auth?.token?.auth_time);

  if (!Number.isFinite(authTimeSeconds)) {
    throw new HttpsError(
        "failed-precondition",
        "Bitte melde dich erneut an, bevor du dein Konto loeschst.",
    );
  }

  const authAgeSeconds = Math.max(
      0,
      Math.floor(Date.now() / 1000) - Math.floor(authTimeSeconds),
  );

  if (authAgeSeconds > ACCOUNT_DELETE_RECENT_AUTH_MAX_AGE_SECONDS) {
    throw new HttpsError(
        "failed-precondition",
        "Bitte melde dich erneut an, bevor du dein Konto loeschst.",
    );
  }

  return uid;
}

async function deleteDocumentsFromSnapshot(snapshot) {
  if (!snapshot || snapshot.empty) {
    return 0;
  }

  return deleteDocumentReferences(snapshot.docs.map((document) => document.ref));
}

async function deleteDocumentReferences(references) {
  if (!Array.isArray(references) || references.length === 0) {
    return 0;
  }

  const firestore = admin.firestore();
  let deletedCount = 0;
  const uniqueReferences = Array.from(
      new Map(
          references
              .filter(Boolean)
              .map((reference) => [reference.path, reference]),
      ).values(),
  );

  for (let index = 0; index < uniqueReferences.length; index += 400) {
    const batch = firestore.batch();
    const chunk = uniqueReferences.slice(index, index + 400);

    for (const reference of chunk) {
      batch.delete(reference);
      deletedCount += 1;
    }

    await batch.commit();
  }

  return deletedCount;
}

async function deleteStoragePrefix(prefix) {
  const bucket = admin.storage().bucket();
  const [files] = await bucket.getFiles({prefix});

  await Promise.all(files.map((file) => file.delete()));
  return files.length;
}

async function deleteOrdersForAccount(uid, email = null) {
  const firestore = admin.firestore();
  const orderReferences = [];
  const ordersByUidSnapshot = await firestore
      .collection("orders")
      .where("orderOwnerUid", "==", uid)
      .get();

  orderReferences.push(...ordersByUidSnapshot.docs.map((document) => document.ref));

  if (email) {
    const legacyOrdersSnapshot = await firestore
        .collection("orders")
        .where("userEmail", "==", email)
        .get();
    orderReferences.push(...legacyOrdersSnapshot.docs.map((document) => document.ref));
  }

  return deleteDocumentReferences(orderReferences);
}

async function purgeCurrentUserAccountData(uid, email = null) {
  const firestore = admin.firestore();

  await firestore.recursiveDelete(firestore.doc(`users/${uid}`));
  await firestore.recursiveDelete(
      firestore.collection("galleryMeta").doc(uid).collection("items"),
  );
  await firestore.doc(`galleryMeta/${uid}`).delete();
  await firestore.doc(`userProfiles/${uid}`).delete();
  await firestore.doc(`uploadUsage/${uid}`).delete();
  await firestore.doc(`adminConfig/automationN8n_${uid}`).delete();

  const uploadSlotsSnapshot = await firestore
      .collection("uploadSlots")
      .where("ownerUid", "==", uid)
      .get();
  const deletedOrders = await deleteOrdersForAccount(uid, email);
  const deletedUploadSlots = await deleteDocumentsFromSnapshot(uploadSlotsSnapshot);
  const deletedStorageObjects = await deleteStoragePrefix(`users/${uid}/`);

  return {
    deletedOrders,
    deletedStorageObjects,
    deletedUploadSlots,
  };
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

function normalizeCollectionHandles(rawValue, fallbackURL = null) {
  let candidates = [];

  if (Array.isArray(rawValue)) {
    candidates = rawValue.filter((value) => typeof value === "string");
  } else if (typeof rawValue === "string") {
    candidates = rawValue.split(/[\n,]/g);
  }

  const normalized = [...new Set(
      candidates
          .map((value) => normalizeCollectionHandle(value))
          .filter(Boolean),
  )];
  if (normalized.length > 0) {
    return normalized;
  }

  const fallbackHandle = normalizeCollectionHandle(null, fallbackURL);
  return fallbackHandle ? [fallbackHandle] : [];
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
  return decodeWorkflowAutomationSettings(snapshot.data() || {});
}

function automationConfigDocumentIdFor(userId) {
  return `automationN8n_${userId}`;
}

function personalAgentProfileDocumentIdFor(userId) {
  return `${PERSONAL_AGENT_PROFILE_DOCUMENT_PREFIX}${userId}`;
}

async function loadWorkflowAutomationSettingsForUser(userId) {
  if (nonEmptyString(userId)) {
    const personalSnapshot = await admin.firestore()
        .collection(AUTOMATION_CONFIG_COLLECTION)
        .doc(automationConfigDocumentIdFor(userId))
        .get();

    if (personalSnapshot.exists) {
      return decodeWorkflowAutomationSettings(personalSnapshot.data() || {});
    }

    // BYOS strict mode: each authenticated account uses only its own automation doc.
    return decodeWorkflowAutomationSettings({});
  }

  return loadWorkflowAutomationSettings();
}

function decodeWorkflowAutomationSettings(data = {}) {
  return {
    provider: nonEmptyString(data.provider) || "n8n",
    isEnabled: data.isEnabled === true,
    sendsUserContext: data.sendsUserContext !== false,
    workflowName: nonEmptyString(data.workflowName) || "Skydown Automation",
    baseURL: normalizeUrlString(data.baseURL) || "",
    webhookPath: normalizeAutomationWebhookPath(data.webhookPath) || "",
    authHeaderName: nonEmptyString(data.authHeaderName) || "",
    authHeaderValue: nonEmptyString(data.authHeaderValue) || "",
    knowledgeContext: nonEmptyString(data.knowledgeContext) || "",
  };
}

function decodePersonalAgentProfileSettings(data = {}) {
  return {
    isEnabled: data.isEnabled === true,
    roleLabel: normalizePersonalAgentProfileSetting(data.roleLabel, 240),
    skillProfile: normalizePersonalAgentProfileSetting(data.skillProfile, 12000),
    outputFormat: normalizePersonalAgentProfileSetting(data.outputFormat, 4000),
    guardrails: normalizePersonalAgentProfileSetting(data.guardrails, 4000),
    knowledgeContext: normalizePersonalAgentProfileSetting(data.knowledgeContext, 4000),
  };
}

async function loadPersonalAgentProfileSettingsForUser(userId) {
  if (!nonEmptyString(userId)) {
    return DEFAULT_PERSONAL_AGENT_PROFILE_SETTINGS;
  }

  const snapshot = await admin.firestore()
      .collection(AUTOMATION_CONFIG_COLLECTION)
      .doc(personalAgentProfileDocumentIdFor(userId))
      .get();
  if (!snapshot.exists) {
    return DEFAULT_PERSONAL_AGENT_PROFILE_SETTINGS;
  }

  return decodePersonalAgentProfileSettings(snapshot.data() || {});
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

  const safeData = data && typeof data === "object" && !Array.isArray(data) ? data : {};
  const knowledgeContext = nonEmptyString(settings.knowledgeContext) || "";
  const payload = {
    provider: "n8n",
    workflowName: settings.workflowName,
    trigger,
    source,
    timestamp: new Date().toISOString(),
    data: {
      ...safeData,
      knowledgeContext,
    },
    knowledgeContext,
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

function aiGlobalLimitReachedMessage(kind, limit) {
  switch (kind) {
    case AI_USAGE_KINDS.visual:
      return `Globales Tageslimit fuer Visuals erreicht (${limit}/Tag).`;
    case AI_USAGE_KINDS.agent:
      return `Globales Tageslimit fuer den Agent erreicht (${limit}/Tag).`;
    case AI_USAGE_KINDS.text:
    default:
      return `Globales Tageslimit fuer den Bot erreicht (${limit}/Tag).`;
  }
}

function aiUsageMetricsDocumentId(dateKey) {
  return `${AI_USAGE_METRICS_DOCUMENT_PREFIX}${dateKey}`;
}

async function authorizeAiUsage({auth, kind}) {
  if (!Object.values(AI_USAGE_KINDS).includes(kind)) {
    throw new HttpsError("invalid-argument", "Unbekannte KI-Aktion.");
  }

  const {profile} = await assertAiAccess(auth);
  const runtimeSettings = await loadAiRuntimeSettings();

  const dateKey = aiUsageDateKey();
  const usageRef = admin.firestore().doc(`users/${auth.uid}/aiUsage/${dateKey}`);
  const globalUsageRef = admin.firestore()
      .collection(AI_USAGE_METRICS_COLLECTION)
      .doc(aiUsageMetricsDocumentId(dateKey));

  const usageSummary = await admin.firestore().runTransaction(async (transaction) => {
    const [userSnapshot, globalSnapshot] = await Promise.all([
      transaction.get(usageRef),
      transaction.get(globalUsageRef),
    ]);
    const currentData = userSnapshot.exists ? (userSnapshot.data() || {}) : {};
    const currentGlobalData = globalSnapshot.exists ? (globalSnapshot.data() || {}) : {};
    const counterField = aiUsageCounterField(kind);
    const currentCount = Number(currentData[counterField]) || 0;
    const currentTotal = Number(currentData.totalRequests) || 0;
    const baseLimit = aiUsageLimitForKind(kind, profile.aiLimits);
    const hardCap = aiUsageLimitForKind(kind, runtimeSettings.hardDailyCaps);
    const limit = runtimeSettings.costGuardEnabled ?
      Math.max(1, Math.min(baseLimit, hardCap)) :
      baseLimit;

    if (currentCount >= limit) {
      throw new HttpsError("resource-exhausted", aiLimitReachedMessage(kind, limit));
    }

    const currentGlobalCount = Number(currentGlobalData[counterField]) || 0;
    const currentGlobalTotal = Number(currentGlobalData.totalRequests) || 0;
    const globalLimit = aiUsageLimitForKind(kind, runtimeSettings.globalDailyCaps);
    if (runtimeSettings.costGuardEnabled &&
      Number.isFinite(globalLimit) &&
      globalLimit > 0 &&
      currentGlobalCount >= globalLimit) {
      throw new HttpsError("resource-exhausted", aiGlobalLimitReachedMessage(kind, globalLimit));
    }

    const nextCount = currentCount + 1;
    const nextTotal = currentTotal + 1;
    const nextGlobalCount = currentGlobalCount + 1;
    const nextGlobalTotal = currentGlobalTotal + 1;
    const nextTextCount = counterField === "textRequests" ? nextCount : (Number(currentData.textRequests) || 0);
    const nextVisualCount = counterField === "visualRequests" ? nextCount : (Number(currentData.visualRequests) || 0);
    const nextAgentCount = counterField === "agentRequests" ? nextCount : (Number(currentData.agentRequests) || 0);
    const nextGlobalText = counterField === "textRequests" ? nextGlobalCount : (Number(currentGlobalData.textRequests) || 0);
    const nextGlobalVisual = counterField === "visualRequests" ? nextGlobalCount : (Number(currentGlobalData.visualRequests) || 0);
    const nextGlobalAgent = counterField === "agentRequests" ? nextGlobalCount : (Number(currentGlobalData.agentRequests) || 0);

    transaction.set(usageRef, {
      dateKey,
      role: profile.role,
      textRequests: nextTextCount,
      visualRequests: nextVisualCount,
      agentRequests: nextAgentCount,
      totalRequests: nextTotal,
      lastConsumedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(globalUsageRef, {
      dateKey,
      textRequests: nextGlobalText,
      visualRequests: nextGlobalVisual,
      agentRequests: nextGlobalAgent,
      totalRequests: nextGlobalTotal,
      lastConsumedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    return {
      dateKey,
      role: profile.role,
      kind,
      remainingForKind: Math.max(limit - nextCount, 0),
      limitForKind: limit,
      textRemaining: Math.max(
          (runtimeSettings.costGuardEnabled ?
            Math.min(profile.aiLimits.text, runtimeSettings.hardDailyCaps.text) :
            profile.aiLimits.text) - nextTextCount,
          0,
      ),
      visualRemaining: Math.max(
          (runtimeSettings.costGuardEnabled ?
            Math.min(profile.aiLimits.visual, runtimeSettings.hardDailyCaps.visual) :
            profile.aiLimits.visual) - nextVisualCount,
          0,
      ),
      agentRemaining: Math.max(
          (runtimeSettings.costGuardEnabled ?
            Math.min(profile.aiLimits.agent, runtimeSettings.hardDailyCaps.agent) :
            profile.aiLimits.agent) - nextAgentCount,
          0,
      ),
      globalRemainingForKind: runtimeSettings.costGuardEnabled ?
        Math.max(globalLimit - nextGlobalCount, 0) :
        null,
      globalLimitForKind: runtimeSettings.costGuardEnabled ? globalLimit : null,
      historyRetentionDays: profile.aiLimits.historyRetentionDays,
    };
  });

  logger.info("AI usage authorized.", {
    uid: auth.uid,
    role: usageSummary.role,
    kind,
    remainingForKind: usageSummary.remainingForKind,
    globalRemainingForKind: usageSummary.globalRemainingForKind,
    dateKey,
  });

  return usageSummary;
}

function composeAssetLibraryPromptContext(promptSettings) {
  const lines = [];
  if (promptSettings.assetLibraryLink) {
    lines.push(`Asset- / Referenzbibliothek: ${promptSettings.assetLibraryLink}`);
  }
  if (promptSettings.assetReferenceNotes) {
    lines.push(`Zusatzhinweise zu Assets und Referenzen:\n${promptSettings.assetReferenceNotes}`);
  }
  return lines.length ? lines.join("\n\n") : "";
}

function aiTextModeHint(mode) {
  switch (mode) {
    case AI_TEXT_MODES.caption:
      return "Modus Caption: Liefere eine starke Hauptversion, danach 3 Varianten, 1 CTA-Zeile und bis zu 5 passende Hashtags.";
    case AI_TEXT_MODES.releasePlan:
      return "Modus Release-Plan: Liefere Ziel, Phasen, To-dos, benoetigte Assets, Timing, Risiken und die naechsten 3 Schritte.";
    case AI_TEXT_MODES.briefing:
      return "Modus Briefing: Liefere ein copy-pastebares Briefing mit Ziel, Format, Tonalitaet, Deliverables, Do's, Don'ts und Deadline.";
    case AI_TEXT_MODES.merchCopy:
      return "Modus Merch-Copy: Liefere Headline, Hauptcaption, Story-CTA, Produkt-/Drop-Angle und 3 kurze Varianten.";
    case AI_TEXT_MODES.videoConcept:
      return "Modus Video-Konzept: Liefere Hook, Konzept, Shotlist, On-Screen-Text, Assets und Abschluss-CTA.";
    case AI_TEXT_MODES.general:
    default:
      return "Modus Allgemein: Liefere eine direkt nutzbare Hauptantwort und bei Bedarf 2 bis 3 starke Varianten.";
  }
}

function composeTextGenerationPrompt(inputPrompt, textInstruction, mode, assetContext) {
  return `
${textInstruction}

${aiTextModeHint(mode)}

${assetContext ? `Verfuegbare Referenzen:\n${assetContext}\n` : ""}

Nutzeranfrage:
${inputPrompt}
  `.trim();
}

function composeVisualGenerationPrompt(inputPrompt, visualInstruction, assetContext) {
  return `
${visualInstruction}

${assetContext ? `Verfuegbare Referenzen:\n${assetContext}\n` : ""}

Nutzeranfrage:
${inputPrompt}
  `.trim();
}

function parseCallableInput(schema, data, message) {
  const parsed = schema.safeParse(data || {});
  if (!parsed.success) {
    logger.warn("Invalid callable input.", {
      issues: parsed.error.issues,
    });
    throw new HttpsError("invalid-argument", message);
  }

  return parsed.data;
}

function extractInlineBase64Media(dataUrl, contentType = null) {
  const normalized = nonEmptyString(dataUrl);
  if (!normalized) {
    return null;
  }

  const dataUrlMatch = normalized.match(/^data:([^;]+);base64,(.+)$/i);
  if (dataUrlMatch) {
    return {
      base64: dataUrlMatch[2],
      mimeType: contentType || dataUrlMatch[1],
    };
  }

  if (normalized.startsWith("http://") || normalized.startsWith("https://") || normalized.startsWith("gs://")) {
    return null;
  }

  return {
    base64: normalized,
    mimeType: contentType,
  };
}

async function generateAiTextReply({prompt, mode}) {
  const promptSettings = await loadAiPromptSettings();
  const assetContext = composeAssetLibraryPromptContext(promptSettings);
  const response = await ai.generate({
    prompt: composeTextGenerationPrompt(
        prompt,
        promptSettings.textInstruction,
        mode,
        assetContext,
    ),
    config: {
      temperature: 0.7,
      maxOutputTokens: 768,
    },
  });

  const reply = nonEmptyString(response.text);
  if (!reply) {
    throw new HttpsError("internal", "Skydown Bot konnte keine Antwort erzeugen.");
  }

  return reply;
}

async function generateAiVisualResult(prompt) {
  const promptSettings = await loadAiPromptSettings();
  const assetContext = composeAssetLibraryPromptContext(promptSettings);
  const response = await ai.generate({
    model: vertexAI.model("gemini-2.5-flash-image"),
    prompt: composeVisualGenerationPrompt(
        prompt,
        promptSettings.visualInstruction,
        assetContext,
    ),
    config: {
      responseModalities: ["TEXT", "IMAGE"],
    },
  });

  const media = response.media;
  const encodedImage = extractInlineBase64Media(media?.url, media?.contentType);
  if (!encodedImage?.base64) {
    throw new HttpsError("internal", "Skydown Bot konnte kein Visual erzeugen.");
  }

  return {
    text: nonEmptyString(response.text) || "Visual generiert.",
    imageBase64: encodedImage.base64,
    mimeType: encodedImage.mimeType || "image/png",
  };
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
  const collectionHandles = normalizeCollectionHandles(
      data.collectionHandles ?? data.collectionHandle,
      configuredStorefrontURL,
  );

  return {
    storeDomain,
    storefrontAccessToken: nonEmptyString(data.storefrontAccessToken) || "",
    collectionHandles,
    collectionHandle: collectionHandles[0] || "",
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

async function loadPaymentMethodSettings() {
  const snapshot = await admin.firestore()
      .collection("appConfig")
      .doc("paymentMethods")
      .get();
  const data = snapshot.data() || {};
  const stripe = data.stripe || {};
  const klarna = data.klarna || {};
  const aiSubscriptions = data.aiSubscriptions || {};

  return {
    stripe: {
      connected: stripe.connected === true,
      enabled: stripe.enabled === true,
    },
    klarna: {
      connected: klarna.connected === true,
      enabled: klarna.enabled === true,
    },
    aiSubscriptions: {
      enabled: aiSubscriptions.enabled === true,
      creatorPriceId: nonEmptyString(aiSubscriptions.creatorPriceId) || "",
      studioPriceId: nonEmptyString(aiSubscriptions.studioPriceId) || "",
    },
  };
}

function resolveAiSubscriptionPriceId(paymentSettings, plan) {
  const normalizedPlan = normalizeAiSubscriptionPlan(plan);
  if (!normalizedPlan) {
    return "";
  }

  const config = paymentSettings?.aiSubscriptions || {};
  if (config.enabled !== true) {
    return "";
  }

  if (normalizedPlan === USER_QUOTA_PLANS.studio) {
    return nonEmptyString(config.studioPriceId) || "";
  }

  return nonEmptyString(config.creatorPriceId) || "";
}

async function loadCommerceSettings() {
  const snapshot = await admin.firestore()
      .collection("appConfig")
      .doc("commerceSettings")
      .get();
  const data = snapshot.data() || {};
  const shipping = data.shipping || {};
  const invoice = data.invoice || {};

  return {
    shipping: {
      domesticCost: Math.max(0, parsePrice(shipping.domesticCost || DEFAULT_COMMERCE_SETTINGS.shipping.domesticCost)),
      euCost: Math.max(0, parsePrice(shipping.euCost || DEFAULT_COMMERCE_SETTINGS.shipping.euCost)),
      internationalCost: Math.max(
          0,
          parsePrice(shipping.internationalCost || DEFAULT_COMMERCE_SETTINGS.shipping.internationalCost),
      ),
      freeShippingThreshold: Math.max(
          0,
          parsePrice(shipping.freeShippingThreshold || DEFAULT_COMMERCE_SETTINGS.shipping.freeShippingThreshold),
      ),
    },
    invoice: {
      taxRate: Math.max(0, parsePrice(invoice.taxRate || DEFAULT_COMMERCE_SETTINGS.invoice.taxRate)),
    },
  };
}

function isHostedPaymentMethodEnabled(paymentSettings, paymentMethod) {
  const normalizedMethod = normalizeHostedCheckoutMethod(paymentMethod);
  if (normalizedMethod === "Stripe") {
    return paymentSettings?.stripe?.connected === true && paymentSettings?.stripe?.enabled === true;
  }
  if (normalizedMethod === "Klarna") {
    return paymentSettings?.klarna?.connected === true && paymentSettings?.klarna?.enabled === true;
  }
  return false;
}

function loadStripeSecretKey() {
  try {
    return nonEmptyString(stripeSecretKey.value()) || nonEmptyString(process.env.STRIPE_SECRET_KEY) || "";
  } catch (error) {
    return nonEmptyString(process.env.STRIPE_SECRET_KEY) || "";
  }
}

function loadStripeWebhookSecret() {
  try {
    return nonEmptyString(stripeWebhookSecret.value()) || nonEmptyString(process.env.STRIPE_WEBHOOK_SECRET) || "";
  } catch (error) {
    return nonEmptyString(process.env.STRIPE_WEBHOOK_SECRET) || "";
  }
}

function resolveShippingZoneFromCountryCode(countryCode) {
  const normalizedCountryCode = nonEmptyString(countryCode)?.toUpperCase();
  if (!normalizedCountryCode) {
    throw new HttpsError("invalid-argument", "Lieferland fehlt.");
  }

  if (normalizedCountryCode === "DE") {
    return "DE";
  }

  if (EU_COUNTRY_CODES.has(normalizedCountryCode)) {
    return "EU";
  }

  return "INTL";
}

function calculateCanonicalShipping({
  settings,
  countryCode,
  subtotalAmount,
}) {
  const shippingZone = resolveShippingZoneFromCountryCode(countryCode);
  const shippingSettings = settings?.shipping || DEFAULT_COMMERCE_SETTINGS.shipping;
  const normalizedSubtotal = roundCurrency(subtotalAmount);
  const baseRate = shippingZone === "DE" ?
    shippingSettings.domesticCost :
    shippingZone === "EU" ?
      shippingSettings.euCost :
      shippingSettings.internationalCost;
  const freeShippingApplied = shippingSettings.freeShippingThreshold > 0 &&
    normalizedSubtotal >= shippingSettings.freeShippingThreshold;

  return {
    shippingZone,
    shippingAmount: freeShippingApplied ? 0 : roundCurrency(baseRate),
  };
}

function resolveProjectId() {
  return nonEmptyString(process.env.GCLOUD_PROJECT) ||
    nonEmptyString(admin.app().options.projectId) ||
    "";
}

function validStripeSecretKey(value) {
  return /^(sk|rk)_(live|test)_/.test(`${value || ""}`.trim());
}

function validStripeWebhookSecret(value) {
  return /^whsec_/.test(`${value || ""}`.trim());
}

async function resolveUserDocumentForAiSubscription({
  userId = null,
  stripeSubscriptionId = null,
  stripeCustomerId = null,
} = {}) {
  const usersRef = admin.firestore().collection("users");
  const normalizedUserId = nonEmptyString(userId);
  if (normalizedUserId) {
    const snapshot = await usersRef.doc(normalizedUserId).get();
    if (snapshot.exists) {
      return {
        ref: snapshot.ref,
        snapshot,
      };
    }
  }

  const normalizedSubscriptionId = nonEmptyString(stripeSubscriptionId);
  if (normalizedSubscriptionId) {
    const query = await usersRef
        .where("aiSubscriptionStripeSubscriptionId", "==", normalizedSubscriptionId)
        .limit(1)
        .get();
    if (!query.empty) {
      const snapshot = query.docs[0];
      return {
        ref: snapshot.ref,
        snapshot,
      };
    }
  }

  const normalizedCustomerId = nonEmptyString(stripeCustomerId);
  if (normalizedCustomerId) {
    const query = await usersRef
        .where("aiSubscriptionStripeCustomerId", "==", normalizedCustomerId)
        .limit(1)
        .get();
    if (!query.empty) {
      const snapshot = query.docs[0];
      return {
        ref: snapshot.ref,
        snapshot,
      };
    }
  }

  return null;
}

async function processAiSubscriptionCheckoutWebhook(eventType, stripeObject = {}) {
  if (nonEmptyString(stripeObject?.mode)?.toLowerCase() !== "subscription") {
    return {handled: false};
  }

  const metadata = resolveAiSubscriptionMetadata(stripeObject);
  const isAiSubscription = metadata.type === "ai_subscription" || Boolean(metadata.userId);
  if (!isAiSubscription) {
    return {handled: false};
  }

  const identifiers = extractStripeCheckoutIdentifiers(stripeObject);
  const userDocument = await resolveUserDocumentForAiSubscription({
    userId: metadata.userId,
    stripeSubscriptionId: nonEmptyString(stripeObject?.subscription),
    stripeCustomerId: nonEmptyString(stripeObject?.customer),
  });
  if (!userDocument) {
    logger.warn("AI subscription checkout webhook received for unknown user.", {
      eventType,
      userId: metadata.userId,
      sessionId: identifiers.sessionId,
    });
    return {handled: true, ignored: true};
  }

  const userData = userDocument.snapshot.data() || {};
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const plan = metadata.plan ||
    normalizeAiSubscriptionPlan(userData.aiSubscriptionPlan) ||
    normalizeAiSubscriptionPlan(userData.quotaPlan) ||
    USER_QUOTA_PLANS.creator;
  const status = resolveAiSubscriptionStatusFromCheckoutEvent(eventType);
  const checkoutExpires = Number(stripeObject?.expires_at);
  const updates = {
    aiSubscriptionStatus: status,
    aiSubscriptionPlan: plan,
    aiSubscriptionPriceId: metadata.priceId || nonEmptyString(userData.aiSubscriptionPriceId) || admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeCheckoutSessionId: identifiers.sessionId ||
      nonEmptyString(userData.aiSubscriptionStripeCheckoutSessionId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeCustomerId: nonEmptyString(stripeObject?.customer) ||
      nonEmptyString(userData.aiSubscriptionStripeCustomerId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeSubscriptionId: nonEmptyString(stripeObject?.subscription) ||
      nonEmptyString(userData.aiSubscriptionStripeSubscriptionId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionCheckoutExpiresAtEpochSeconds: Number.isFinite(checkoutExpires) && checkoutExpires > 0 ?
      Math.floor(checkoutExpires) :
      admin.firestore.FieldValue.delete(),
    aiSubscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (isAiSubscriptionStatusActive(status)) {
    Object.assign(updates, resolveAiLimitsUpdateForPlan(plan));
    updates.aiSubscriptionActivatedAt = admin.firestore.FieldValue.serverTimestamp();
    updates.aiSubscriptionCancelAtPeriodEnd = false;
  } else if (role !== USER_ROLES.admin) {
    Object.assign(updates, resolveAiLimitsUpdateForRole(role));
  }

  await userDocument.ref.set(updates, {merge: true});

  logger.info("AI subscription checkout webhook processed.", {
    eventType,
    uid: userDocument.ref.id,
    status,
    plan,
    sessionId: identifiers.sessionId,
  });

  return {
    handled: true,
    status,
    uid: userDocument.ref.id,
  };
}

async function processAiSubscriptionLifecycleWebhook(eventType, stripeObject = {}) {
  if (!eventType.startsWith("customer.subscription.")) {
    return {handled: false};
  }

  const metadata = resolveAiSubscriptionMetadata(stripeObject);
  const userDocument = await resolveUserDocumentForAiSubscription({
    userId: metadata.userId,
    stripeSubscriptionId: nonEmptyString(stripeObject?.id),
    stripeCustomerId: nonEmptyString(stripeObject?.customer),
  });
  if (!userDocument) {
    logger.warn("AI subscription lifecycle webhook received for unknown user.", {
      eventType,
      subscriptionId: nonEmptyString(stripeObject?.id),
      customerId: nonEmptyString(stripeObject?.customer),
    });
    return {handled: true, ignored: true};
  }

  const userData = userDocument.snapshot.data() || {};
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const plan = metadata.plan ||
    normalizeAiSubscriptionPlan(userData.aiSubscriptionPlan) ||
    normalizeAiSubscriptionPlan(userData.quotaPlan) ||
    USER_QUOTA_PLANS.creator;
  const status = resolveAiSubscriptionStatusFromLifecycleEvent(eventType, stripeObject);
  const currentPeriodEnd = Number(stripeObject?.current_period_end);
  const updates = {
    aiSubscriptionStatus: status,
    aiSubscriptionPlan: plan,
    aiSubscriptionPriceId: metadata.priceId || nonEmptyString(userData.aiSubscriptionPriceId) || admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeCustomerId: nonEmptyString(stripeObject?.customer) ||
      nonEmptyString(userData.aiSubscriptionStripeCustomerId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeSubscriptionId: nonEmptyString(stripeObject?.id) ||
      nonEmptyString(userData.aiSubscriptionStripeSubscriptionId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionCurrentPeriodEndEpochSeconds: Number.isFinite(currentPeriodEnd) && currentPeriodEnd > 0 ?
      Math.floor(currentPeriodEnd) :
      admin.firestore.FieldValue.delete(),
    aiSubscriptionCancelAtPeriodEnd: stripeObject?.cancel_at_period_end === true,
    aiSubscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (isAiSubscriptionStatusActive(status)) {
    Object.assign(updates, resolveAiLimitsUpdateForPlan(plan));
    updates.aiSubscriptionActivatedAt = admin.firestore.FieldValue.serverTimestamp();
  } else if (role !== USER_ROLES.admin) {
    Object.assign(updates, resolveAiLimitsUpdateForRole(role));
  }

  await userDocument.ref.set(updates, {merge: true});

  logger.info("AI subscription lifecycle webhook processed.", {
    eventType,
    uid: userDocument.ref.id,
    status,
    plan,
    subscriptionId: nonEmptyString(stripeObject?.id),
  });

  return {
    handled: true,
    status,
    uid: userDocument.ref.id,
  };
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

async function shopifyPublicCollectionsRequest({storeDomain, page = 1}) {
  const url = new URL(`https://${getShopifyDomain(storeDomain)}/collections.json`);
  url.searchParams.set("limit", "250");
  url.searchParams.set("page", `${Math.max(1, Number(page) || 1)}`);

  const response = await fetch(url.toString(), {
    method: "GET",
    headers: {
      Accept: "application/json",
    },
  });

  const payload = await response.json().catch(() => ({}));

  if (!response.ok) {
    logger.error("Shopify public collections request failed.", {
      url: url.toString(),
      status: response.status,
      statusText: response.statusText,
      body: payload,
    });
    if (response.status === 401 || response.status === 403) {
      throw new Error("Der Shopify-Store ist nicht oeffentlich lesbar. Hinterlege einen Storefront Access Token in den Shopify-Einstellungen der App oder oeffne den Collection-Feed.");
    }
    throw new Error(`Shopify Collections Fehler (${response.status} ${response.statusText}).`);
  }

  return Array.isArray(payload?.collections) ? payload.collections : [];
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

function roundCurrency(value) {
  return Math.round(parsePrice(value) * 100) / 100;
}

function stripHtml(value) {
  return typeof value === "string"
    ? value.replace(/<[^>]+>/g, " ").replace(/\s+/g, " ").trim()
    : "";
}

function nonEmptyString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function normalizeComparableString(value) {
  return nonEmptyString(value)?.toLowerCase() || "";
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

function uniqueCollectionsByHandle(collections) {
  const seenHandles = new Set();
  const normalizedCollections = [];

  for (const collection of Array.isArray(collections) ? collections : []) {
    const handle = nonEmptyString(collection?.handle);
    if (!handle) {
      continue;
    }

    const normalizedHandle = handle.toLowerCase();
    if (seenHandles.has(normalizedHandle)) {
      continue;
    }

    seenHandles.add(normalizedHandle);
    normalizedCollections.push({
      handle,
      title: nonEmptyString(collection?.title) || handle,
      productCount: Number.isFinite(Number(collection?.productCount)) ? Number(collection.productCount) : null,
    });
  }

  return normalizedCollections.sort((left, right) =>
    `${left.title || left.handle}`.localeCompare(`${right.title || right.handle}`, "de", {sensitivity: "base"}),
  );
}

async function fetchShopifyCollectionsViaAdminApi(token, config) {
  const query = `
    query FetchCollections($cursor: String) {
      collections(first: 100, after: $cursor, sortKey: TITLE) {
        pageInfo {
          hasNextPage
          endCursor
        }
        nodes {
          handle
          title
          productsCount {
            count
          }
        }
      }
    }
  `;

  const collections = [];
  let hasNextPage = true;
  let cursor = null;

  while (hasNextPage) {
    const data = await shopifyGraphqlRequest({
      query,
      variables: {cursor},
      token,
      storeDomain: config?.storeDomain,
    });

    const connection = data?.collections;
    collections.push(
        ...((connection?.nodes || []).map((collection) => ({
          handle: nonEmptyString(collection?.handle) || "",
          title: nonEmptyString(collection?.title) || nonEmptyString(collection?.handle) || "",
          productCount: Number(collection?.productsCount?.count),
        }))),
    );
    hasNextPage = Boolean(connection?.pageInfo?.hasNextPage);
    cursor = connection?.pageInfo?.endCursor || null;
  }

  return uniqueCollectionsByHandle(collections);
}

const SHOPIFY_STOREFRONT_COLLECTIONS_QUERY = `
  query FetchCollections($cursor: String) {
    collections(first: 100, after: $cursor, sortKey: UPDATED_AT) {
      pageInfo {
        hasNextPage
        endCursor
      }
      nodes {
        handle
        title
      }
    }
  }
`;

async function fetchShopifyCollectionsViaStorefrontApi(config) {
  const collections = [];
  let hasNextPage = true;
  let cursor = null;

  while (hasNextPage) {
    const data = await shopifyStorefrontGraphqlRequest({
      query: SHOPIFY_STOREFRONT_COLLECTIONS_QUERY,
      variables: {cursor},
      token: nonEmptyString(config?.storefrontAccessToken) || "",
      storeDomain: config?.storeDomain,
    });

    const connection = data?.collections;
    collections.push(
        ...((connection?.nodes || []).map((collection) => ({
          handle: nonEmptyString(collection?.handle) || "",
          title: nonEmptyString(collection?.title) || nonEmptyString(collection?.handle) || "",
          productCount: null,
        }))),
    );
    hasNextPage = Boolean(connection?.pageInfo?.hasNextPage);
    cursor = connection?.pageInfo?.endCursor || null;
  }

  return uniqueCollectionsByHandle(collections);
}

async function fetchShopifyCollectionsViaPublicStorefront(config) {
  const collections = [];
  let page = 1;
  let hasNextPage = true;

  while (hasNextPage) {
    const pageCollections = await shopifyPublicCollectionsRequest({
      storeDomain: config?.storeDomain,
      page,
    });

    collections.push(
        ...pageCollections.map((collection) => ({
          handle: nonEmptyString(collection?.handle) || "",
          title: nonEmptyString(collection?.title) || nonEmptyString(collection?.handle) || "",
          productCount: Number.isFinite(Number(collection?.products_count)) ? Number(collection.products_count) : null,
        })),
    );

    if (pageCollections.length < 250) {
      hasNextPage = false;
    } else {
      page += 1;
    }
  }

  return uniqueCollectionsByHandle(collections);
}

async function fetchAvailableShopifyCollections(config) {
  const adminToken = await loadShopifyAdminToken();
  if (adminToken) {
    try {
      const collections = await fetchShopifyCollectionsViaAdminApi(adminToken, config);
      if (collections.length > 0) {
        return collections;
      }
    } catch (error) {
      logger.warn("Shopify Admin API collection lookup failed.", {
        storeDomain: config?.storeDomain,
        message: error instanceof Error ? error.message : String(error),
      });
    }
  }

  const storefrontToken = nonEmptyString(config?.storefrontAccessToken) || "";
  if (storefrontToken) {
    try {
      const collections = await fetchShopifyCollectionsViaStorefrontApi(config);
      if (collections.length > 0) {
        return collections;
      }
    } catch (error) {
      logger.warn("Shopify Storefront API collection lookup failed.", {
        storeDomain: config?.storeDomain,
        message: error instanceof Error ? error.message : String(error),
      });
    }
  }

  return fetchShopifyCollectionsViaPublicStorefront(config);
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

function taggedMetadataValue(tags, prefixes) {
  const normalizedTags = Array.isArray(tags) ? tags : [];
  for (const tag of normalizedTags) {
    const trimmedTag = `${tag || ""}`.trim();
    const loweredTag = trimmedTag.toLowerCase();
    for (const prefix of prefixes) {
      const normalizedPrefix = prefix.toLowerCase();
      if (!loweredTag.startsWith(normalizedPrefix)) {
        continue;
      }
      const value = trimmedTag.slice(prefix.length).trim();
      if (value) {
        return value;
      }
    }
  }
  return null;
}

function curatedProductType(productType) {
  const trimmedType = nonEmptyString(productType);
  if (!trimmedType) {
    return null;
  }

  const genericTypes = new Set(["apparel", "clothing", "merch", "merchandise", "accessories", "accessory"]);
  return genericTypes.has(trimmedType.toLowerCase()) ? null : trimmedType;
}

function resolveShopifyCollabPartner(product) {
  return taggedMetadataValue(product?.tags, ["collab:", "partner:", "artist:", "creator:"]) ||
    externalVendorName(product?.vendor);
}

function externalVendorName(vendor) {
  const trimmedVendor = nonEmptyString(vendor);
  if (!trimmedVendor) {
    return null;
  }

  const normalizedVendor = trimmedVendor.toLowerCase();
  const genericVendors = [
    "skydown",
    "skydown x 22",
    "skydownx22",
    "sky22",
    "sky 22",
    "sky²²",
    "podpartner",
    "printful",
    "printify",
    "gelato",
  ];

  return genericVendors.some((value) => normalizedVendor.includes(value)) ? null : trimmedVendor;
}

function resolveConfiguredCollectionLabel(product, configuredCollectionHandles = []) {
  const normalizedConfigured = configuredCollectionHandles
      .map((handle) => `${handle || ""}`.trim().toLowerCase())
      .filter(Boolean);
  const hintedCollectionHandle = nonEmptyString(product?._skydownCollectionHandle);
  const productCollections = (product?.collections?.nodes || [])
      .map((collection) => nonEmptyString(collection?.handle))
      .filter(Boolean);

  const matchedHandle = productCollections.find((handle) => normalizedConfigured.includes(handle.toLowerCase()))
      || hintedCollectionHandle
      || productCollections[0]
      || nonEmptyString(configuredCollectionHandles[0]);

  return matchedHandle ?
    matchedHandle
        .split("-")
        .filter(Boolean)
        .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
        .join(" ") :
    null;
}

function resolveShopifyProductCategory(product, configuredCollectionHandles = [], existingData = {}) {
  const existingCategory = nonEmptyString(existingData.category);
  return taggedMetadataValue(product?.tags, ["category:", "collection:", "lane:"]) ||
    resolveConfiguredCollectionLabel(product, configuredCollectionHandles) ||
    (existingCategory && existingCategory !== "Sky22 Essentials" ? existingCategory : null) ||
    curatedProductType(product?.productType) ||
    "Sky22 Essentials";
}

function normalizeShopifyProduct(product, currencyCode, existingData = {}, configuredCollectionHandles = []) {
  const images = uniqueStrings([
    product?.featuredImage?.url,
    ...(product?.images?.nodes || []).map((image) => image?.url),
  ]);
  const variants = (product?.variants?.nodes || []).map((variant) => normalizeShopifyVariant(variant, currencyCode));
  const firstVariant = variants[0];
  const availableForSale = variants.some((variant) => variant.availableForSale);
  const collabPartner = resolveShopifyCollabPartner(product);
  const category = resolveShopifyProductCategory(product, configuredCollectionHandles, existingData);
  const shopifyCollectionHandles = resolveShopifyCollectionHandles(product);

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
    category,
    collabPartner: collabPartner || "",
    shopifyCollectionHandles,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function resolveShopifyCollectionHandles(product) {
  const handles = uniqueStrings([
    ...(product?.collections?.nodes || []).map((collection) => nonEmptyString(collection?.handle)),
    nonEmptyString(product?._skydownCollectionHandle),
  ]);

  return handles
      .map((handle) => `${handle}`.trim().toLowerCase())
      .filter(Boolean);
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
    _skydownCollectionHandle: nonEmptyString(product?._skydownCollectionHandle),
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

  const requestedCollectionHandles = Array.isArray(config?.collectionHandles) ?
    config.collectionHandles.map((handle) => `${handle}`.trim().toLowerCase()).filter(Boolean) :
    [];

  if (requestedCollectionHandles.length > 0) {
    const filteredProducts = products.filter((product) => {
      const collections = product?.collections?.nodes || [];
      return collections.some((collection) =>
        requestedCollectionHandles.includes(`${collection?.handle || ""}`.trim().toLowerCase()),
      );
    });

    if (filteredProducts.length === 0 && products.length > 0) {
      logger.warn("Shopify Admin API collection filter returned no products. Falling back to all products.", {
        storeDomain: config?.storeDomain,
        collectionHandles: requestedCollectionHandles,
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

  const requestedCollectionHandles = Array.isArray(config?.collectionHandles) ?
    config.collectionHandles.map((handle) => nonEmptyString(handle)).filter(Boolean) :
    [];
  let products = [];
  let syncSource = requestedCollectionHandles.length > 0 ? "storefront_collection" : "storefront_api";

  if (requestedCollectionHandles.length > 0) {
    const mergedProducts = [];
    const seenProductIds = new Set();
    for (const handle of requestedCollectionHandles) {
      const collectionProducts = await fetchConnection(handle);
      for (const product of collectionProducts) {
        product._skydownCollectionHandle = handle;
        if (!seenProductIds.has(product.id)) {
          seenProductIds.add(product.id);
          mergedProducts.push(product);
        }
      }
    }
    products = mergedProducts;
  } else {
    products = await fetchConnection(null);
  }

  if (requestedCollectionHandles.length > 0 && products.length === 0) {
    logger.warn("Shopify Storefront collection returned no products. Falling back to all storefront products.", {
      storeDomain: config?.storeDomain,
      collectionHandles: requestedCollectionHandles,
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

  const requestedCollectionHandles = Array.isArray(config?.collectionHandles) ?
    config.collectionHandles.map((handle) => nonEmptyString(handle)).filter(Boolean) :
    [];
  let rawProducts = [];
  let syncSource = "public_storefront";
  let usedCollectionFallback = false;

  try {
    if (requestedCollectionHandles.length > 0) {
      const mergedProducts = [];
      const seenProductIds = new Set();
      for (const handle of requestedCollectionHandles) {
        const collectionProducts = await fetchAllPages(handle);
        for (const product of collectionProducts) {
          product._skydownCollectionHandle = handle;
          const productId = `${product?.id || ""}`.trim();
          if (!productId || seenProductIds.has(productId)) {
            continue;
          }
          seenProductIds.add(productId);
          mergedProducts.push(product);
        }
      }
      rawProducts = mergedProducts;
      syncSource = "public_collection";
    } else {
      rawProducts = await fetchAllPages(null);
    }
  } catch (error) {
    if (requestedCollectionHandles.length === 0) {
      throw error;
    }

    logger.warn("Shopify public collection sync failed. Falling back to all public products.", {
      storeDomain: config?.storeDomain,
      collectionHandles: requestedCollectionHandles,
      error: error instanceof Error ? error.message : "unknown_error",
    });
    rawProducts = await fetchAllPages(null);
    usedCollectionFallback = true;
  }

  if (requestedCollectionHandles.length > 0 && rawProducts.length === 0) {
    logger.warn("Shopify public collection sync returned no products. Falling back to all public products.", {
      storeDomain: config?.storeDomain,
      collectionHandles: requestedCollectionHandles,
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
      collectionHandles: config?.collectionHandles || [],
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
        collectionHandles: config?.collectionHandles || [],
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
    collectionHandles: config.collectionHandles || [],
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
    const payload = normalizeShopifyProduct(
        product,
        currencyCode,
        existingDocument?.data() || {},
        config.collectionHandles || [],
    );

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
    collectionHandles: config.collectionHandles || [],
  });

  return {
    syncedCount: products.length,
    createdCount,
    updatedCount,
    deactivatedCount,
    currencyCode,
    storeDomain: config.storeDomain,
    collectionHandles: config.collectionHandles || [],
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

function resolveMatchingVariant(variants, item, itemLabel) {
  const normalizedVariantId = nonEmptyString(item.shopifyVariantId);
  const normalizedSize = normalizeComparableString(item.size);
  const normalizedColor = normalizeComparableString(item.color);

  if (!Array.isArray(variants) || variants.length === 0) {
    return null;
  }

  if (normalizedVariantId) {
    const exactMatch = variants.find((variant) => {
      return nonEmptyString(variant.shopifyVariantId) === normalizedVariantId ||
        nonEmptyString(variant.id) === normalizedVariantId;
    });

    if (!exactMatch) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist nicht mehr in der gewaehlten Variante verfuegbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    if (exactMatch.availableForSale === false) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist aktuell nicht verfuegbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    return exactMatch;
  }

  if (normalizedSize || normalizedColor) {
    const optionMatch = variants.find((variant) => {
      const sameSize = !normalizedSize || normalizeComparableString(variant.size) === normalizedSize;
      const sameColor = !normalizedColor || normalizeComparableString(variant.color) === normalizedColor;
      return sameSize && sameColor;
    });

    if (!optionMatch) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist in dieser Auswahl nicht mehr verfuegbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    if (optionMatch.availableForSale === false) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist aktuell nicht verfuegbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    return optionMatch;
  }

  return variants.find((variant) => variant.availableForSale !== false) || variants[0] || null;
}

async function resolveCanonicalOrderItems(items) {
  const firestore = admin.firestore();

  return Promise.all(items.map(async (item, index) => {
    const productId = nonEmptyString(item.productId);
    const itemLabel = nonEmptyString(item.name) || `Artikel ${index + 1}`;

    if (!productId) {
      throw new HttpsError("invalid-argument", `${itemLabel} hat keine gueltige Produkt-ID.`);
    }

    const productSnapshot = await firestore.collection("merchandise").doc(productId).get();
    if (!productSnapshot.exists) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} wurde im aktuellen Katalog nicht gefunden. Bitte aktualisiere den Warenkorb.`,
      );
    }

    const productData = productSnapshot.data() || {};
    if (productData.isVisibleInApp === false) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist aktuell nicht sichtbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    if (productData.available === false || productData.availableForSale === false || productData.shopifySyncActive === false) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} ist aktuell nicht verfuegbar. Bitte aktualisiere den Warenkorb.`,
      );
    }

    const variants = Array.isArray(productData.variants) ? productData.variants : [];
    const matchedVariant = resolveMatchingVariant(variants, item, itemLabel);
    const canonicalUnitPrice = matchedVariant ?
      roundCurrency(matchedVariant.price) :
      roundCurrency(productData.price);
    const canonicalCurrency = nonEmptyString(matchedVariant?.currency) ||
      nonEmptyString(productData.currency) ||
      "EUR";
    const canonicalSku = nonEmptyString(matchedVariant?.sku) ||
      nonEmptyString(productData.sku) ||
      "";
    const canonicalVariantId = nonEmptyString(matchedVariant?.shopifyVariantId) || "";
    const canonicalSize = nonEmptyString(matchedVariant?.size) || nonEmptyString(item.size) || "";
    const canonicalColor = nonEmptyString(matchedVariant?.color) || nonEmptyString(item.color) || "";

    if (!Number.isFinite(canonicalUnitPrice) || canonicalUnitPrice < 0) {
      throw new HttpsError(
          "failed-precondition",
          `${itemLabel} hat keinen gueltigen Preis im Katalog.`,
      );
    }

    return {
      productId,
      name: nonEmptyString(productData.name) || itemLabel,
      quantity: item.quantity,
      size: canonicalSize,
      color: canonicalColor,
      shopifyVariantId: canonicalVariantId,
      sku: canonicalSku,
      unitPrice: canonicalUnitPrice,
      currency: canonicalCurrency,
    };
  }));
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

async function normalizeOrderSubmissionPayload(data, auth) {
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
  const submittedItems = normalizeOrderItems(data?.items);
  const items = await resolveCanonicalOrderItems(submittedItems);
  const shippingAddressData = ensureShippingAddress(data);
  const fulfillmentProvider = normalizeFulfillmentProvider(data?.fulfillmentProvider);
  const submittedSubtotalAmount = roundCurrency(data?.subtotalAmount);
  const submittedShippingAmount = roundCurrency(data?.shippingAmount);
  const submittedTaxRate = roundCurrency(data?.taxRate);
  const submittedTaxAmount = roundCurrency(data?.taxAmount);
  const submittedTotalAmount = roundCurrency(data?.totalAmount);

  if (!customerName || !customerEmail || !shippingAddress || !shippingZone || !shippingCountryCode) {
    throw new HttpsError("invalid-argument", "Bestellung ist unvollstaendig.");
  }

  if (shippingCountryCode !== shippingAddressData.countryCode.toUpperCase()) {
    throw new HttpsError("invalid-argument", "Lieferland und Lieferadresse passen nicht zusammen.");
  }

  const commerceSettings = await loadCommerceSettings();
  const subtotalAmount = roundCurrency(
      items.reduce((sum, item) => sum + (item.unitPrice * item.quantity), 0),
  );
  const canonicalShipping = calculateCanonicalShipping({
    settings: commerceSettings,
    countryCode: shippingCountryCode,
    subtotalAmount,
  });
  const shippingAmount = canonicalShipping.shippingAmount;
  const taxRate = roundCurrency(commerceSettings.invoice.taxRate);
  const totalAmount = roundCurrency(subtotalAmount + shippingAmount);
  const taxAmount = taxRate > 0 ?
    roundCurrency(totalAmount * (taxRate / (100 + taxRate))) :
    0;
  const currency = items[0]?.currency || "EUR";
  const hasMixedCurrencies = items.some((item) => (item.currency || currency) !== currency);

  if (hasMixedCurrencies) {
    throw new HttpsError("failed-precondition", "Der Warenkorb enthaelt gemischte Waehrungen und kann nicht verarbeitet werden.");
  }

  if (subtotalAmount < 0 || shippingAmount < 0 || taxRate < 0 || taxAmount < 0 || totalAmount <= 0) {
    throw new HttpsError("invalid-argument", "Summen der Bestellung sind ungueltig.");
  }

  const hasTotalsMismatch = Math.abs(submittedSubtotalAmount - subtotalAmount) > 0.01 ||
    Math.abs(submittedShippingAmount - shippingAmount) > 0.01 ||
    Math.abs(submittedTaxRate - taxRate) > 0.01 ||
    Math.abs(submittedTaxAmount - taxAmount) > 0.01 ||
    Math.abs(submittedTotalAmount - totalAmount) > 0.01;
  const hasZoneMismatch = shippingZone !== canonicalShipping.shippingZone;

  if (hasTotalsMismatch || hasZoneMismatch) {
    throw new HttpsError(
        "failed-precondition",
        "Warenkorb oder Versanddaten haben sich geaendert. Bitte aktualisiere den Checkout und pruefe die Summe erneut.",
    );
  }

  return {
    userEmail,
    orderOwnerUid: authUid,
    customerName,
    customerEmail,
    whatsApp,
    shippingAddress,
    shippingAddressData,
    shippingZone: canonicalShipping.shippingZone,
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
    currency,
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
        source: "22xSky",
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

exports.configureStripeBackendSecrets = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "configureStripeBackendSecrets");
  await assertOwner(request.auth);

  const nextStripeSecretKey = nonEmptyString(request.data?.stripeSecretKey);
  const nextStripeWebhookSecret = nonEmptyString(request.data?.stripeWebhookSecret);

  if (!nextStripeSecretKey && !nextStripeWebhookSecret) {
    throw new HttpsError("invalid-argument", "Bitte mindestens einen Stripe-Wert hinterlegen.");
  }

  if (nextStripeSecretKey && !validStripeSecretKey(nextStripeSecretKey)) {
    throw new HttpsError("invalid-argument", "Der Stripe Secret Key sieht ungueltig aus.");
  }

  if (nextStripeWebhookSecret && !validStripeWebhookSecret(nextStripeWebhookSecret)) {
    throw new HttpsError("invalid-argument", "Das Stripe Webhook Secret sieht ungueltig aus.");
  }

  const projectId = resolveProjectId();
  if (!projectId) {
    throw new HttpsError("internal", "Firebase-Projekt konnte nicht fuer Secret Manager aufgeloest werden.");
  }

  const statusRef = admin.firestore()
      .collection(STRIPE_SECRET_STATUS_COLLECTION)
      .doc(STRIPE_SECRET_STATUS_DOCUMENT);
  const statusSnapshot = await statusRef.get();
  const currentStatus = statusSnapshot.exists ? (statusSnapshot.data() || {}) : {};

  try {
    if (nextStripeSecretKey) {
      await addSecretVersion(projectId, "STRIPE_SECRET_KEY", nextStripeSecretKey);
    }

    if (nextStripeWebhookSecret) {
      await addSecretVersion(projectId, "STRIPE_WEBHOOK_SECRET", nextStripeWebhookSecret);
    }
  } catch (error) {
    logger.error("Stripe secrets could not be stored in Secret Manager.", {
      uid: request.auth.uid,
      error: error instanceof Error ? error.message : "unknown_error",
    });
    throw new HttpsError(
        "internal",
        `Stripe-Secrets konnten nicht sicher gespeichert werden: ${error instanceof Error ? error.message : "Unbekannter Fehler."}`,
    );
  }

  const mergedStatus = {
    hasSecretKey: nextStripeSecretKey ? true : currentStatus.hasSecretKey === true,
    hasWebhookSecret: nextStripeWebhookSecret ? true : currentStatus.hasWebhookSecret === true,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedByUid: request.auth.uid,
    updatedByEmail: `${request.auth.token?.email || ""}`.trim().toLowerCase() || admin.firestore.FieldValue.delete(),
  };

  await statusRef.set(mergedStatus, {merge: true});

  logger.info("Stripe backend secrets updated via owner settings.", {
    uid: request.auth.uid,
    updatedSecretKey: Boolean(nextStripeSecretKey),
    updatedWebhookSecret: Boolean(nextStripeWebhookSecret),
  });

  return {
    message: "Stripe-Backend sicher gespeichert.",
    status: {
      hasSecretKey: mergedStatus.hasSecretKey,
      hasWebhookSecret: mergedStatus.hasWebhookSecret,
    },
  };
});

exports.submitMerchOrder = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "submitMerchOrder");
  const runtimeConfig = await getRuntimeConfig();
  if (runtimeConfig.lockdown === true || runtimeConfig.userWritesEnabled === false) {
    throw new HttpsError("failed-precondition", "Bestellungen sind derzeit pausiert.");
  }

  const orderData = await normalizeOrderSubmissionPayload(request.data, request.auth);
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

exports.startMerchCheckout = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
  secrets: [stripeSecretKey],
}, async (request) => {
  await assertCallableSecurity(request, "startMerchCheckout");
  const runtimeConfig = await getRuntimeConfig();
  if (runtimeConfig.lockdown === true || runtimeConfig.userWritesEnabled === false) {
    throw new HttpsError("failed-precondition", "Bestellungen sind derzeit pausiert.");
  }

  const paymentMethod = normalizeHostedCheckoutMethod(request.data?.paymentMethod);
  if (!paymentMethod || !isHostedCheckoutMethod(paymentMethod)) {
    throw new HttpsError("invalid-argument", "Hosted Checkout ist nur fuer Stripe oder Klarna verfuegbar.");
  }

  const paymentSettings = await loadPaymentMethodSettings();
  if (!isHostedPaymentMethodEnabled(paymentSettings, paymentMethod)) {
    throw new HttpsError("failed-precondition", `${paymentMethod} ist aktuell nicht live fuer den Checkout freigeschaltet.`);
  }

  const secretKey = loadStripeSecretKey();
  if (!secretKey) {
    throw new HttpsError("failed-precondition", "Stripe ist serverseitig noch nicht vollstaendig konfiguriert.");
  }

  const projectId = resolveProjectId();
  if (!projectId) {
    throw new HttpsError("internal", "Firebase-Projekt konnte fuer den Checkout nicht aufgeloest werden.");
  }

  const platform = normalizeCheckoutPlatform(request.data?.platform);
  const orderData = await normalizeOrderSubmissionPayload({
    ...request.data,
    paymentMethod,
  }, request.auth);
  const orderRef = admin.firestore().collection("orders").doc();

  const checkoutSession = await createHostedCheckoutSession({
    secretKey,
    projectId,
    orderId: orderRef.id,
    orderData,
    paymentMethod,
    platform,
  });

  await orderRef.set({
    ...orderData,
    paymentMethod,
    paymentProvider: "stripe",
    stripeCheckoutSessionId: checkoutSession.sessionId,
    stripeCheckoutStatus: checkoutSession.stripeCheckoutStatus,
    stripeCheckoutExpiresAtEpochSeconds: checkoutSession.expiresAtEpochSeconds,
  });

  logger.info("Hosted merch checkout created.", {
    orderId: orderRef.id,
    paymentMethod,
    platform,
    userEmail: orderData.userEmail,
    sessionId: checkoutSession.sessionId,
  });

  return {
    orderId: orderRef.id,
    checkoutUrl: checkoutSession.checkoutUrl,
    sessionId: checkoutSession.sessionId,
  };
});

exports.startAiSubscriptionCheckout = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
  secrets: [stripeSecretKey],
}, async (request) => {
  await assertCallableSecurity(request, "startAiSubscriptionCheckout");

  const uid = assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um ein KI-Abo zu starten.",
  );
  const userData = await loadUserData(uid);
  if (!userData) {
    throw new HttpsError("not-found", "Konto wurde nicht gefunden.");
  }

  const profile = buildUserProfile(userData);
  if (profile.role !== USER_ROLES.admin) {
    throw new HttpsError("permission-denied", "Self-Pay-Abo ist nur fuer Admin-Konten verfuegbar.");
  }

  if (!profile.aiAccessEnabled) {
    throw new HttpsError("failed-precondition", "Die KI ist fuer dein Konto aktuell pausiert.");
  }

  const requestedPlan = normalizeAiSubscriptionPlan(request.data?.plan);
  if (!requestedPlan) {
    throw new HttpsError("invalid-argument", "Bitte waehle Creator oder Studio.");
  }

  const paymentSettings = await loadPaymentMethodSettings();
  if (!isHostedPaymentMethodEnabled(paymentSettings, "Stripe")) {
    throw new HttpsError("failed-precondition", "Stripe Checkout ist aktuell nicht live geschaltet.");
  }

  const priceId = resolveAiSubscriptionPriceId(paymentSettings, requestedPlan);
  if (!priceId) {
    throw new HttpsError("failed-precondition", "KI-Abo-Preis ist fuer den gewaehlten Plan noch nicht konfiguriert.");
  }

  const secretKey = loadStripeSecretKey();
  if (!secretKey) {
    throw new HttpsError("failed-precondition", "Stripe ist serverseitig noch nicht vollstaendig konfiguriert.");
  }

  const projectId = resolveProjectId();
  if (!projectId) {
    throw new HttpsError("internal", "Firebase-Projekt konnte fuer den Abo-Checkout nicht aufgeloest werden.");
  }

  const platform = normalizeCheckoutPlatform(request.data?.platform);
  const checkoutSession = await createAiSubscriptionCheckoutSession({
    secretKey,
    projectId,
    userId: uid,
    customerEmail: nonEmptyString(userData.email) || nonEmptyString(request.auth?.token?.email) || "",
    customerId: nonEmptyString(userData.aiSubscriptionStripeCustomerId),
    plan: requestedPlan,
    priceId,
    platform,
  });

  const userRef = admin.firestore().doc(`users/${uid}`);
  await userRef.set({
    aiSubscriptionStatus: "checkout_pending",
    aiSubscriptionPlan: requestedPlan,
    aiSubscriptionPriceId: priceId,
    aiSubscriptionStripeCheckoutSessionId: checkoutSession.sessionId,
    aiSubscriptionStripeCustomerId: checkoutSession.customerId ||
      nonEmptyString(userData.aiSubscriptionStripeCustomerId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionStripeSubscriptionId: checkoutSession.subscriptionId ||
      nonEmptyString(userData.aiSubscriptionStripeSubscriptionId) ||
      admin.firestore.FieldValue.delete(),
    aiSubscriptionCheckoutExpiresAtEpochSeconds: checkoutSession.expiresAtEpochSeconds || admin.firestore.FieldValue.delete(),
    aiSubscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  logger.info("AI subscription checkout created.", {
    uid,
    role: profile.role,
    plan: requestedPlan,
    platform,
    sessionId: checkoutSession.sessionId,
  });

  return {
    checkoutUrl: checkoutSession.checkoutUrl,
    sessionId: checkoutSession.sessionId,
    plan: requestedPlan,
  };
});

exports.confirmMerchOrderPayment = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "confirmMerchOrderPayment");
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

exports.stripeCheckoutReturn = onRequest({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request, response) => {
  const platform = normalizeCheckoutPlatform(request.query?.platform);
  const status = `${request.query?.status || ""}`.trim().toLowerCase() === "cancel" ? "cancel" : "success";
  const orderId = nonEmptyString(request.query?.orderId);
  const sessionId = nonEmptyString(request.query?.session_id);

  response.set("Content-Type", "text/html; charset=utf-8");
  response.status(200).send(renderCheckoutReturnPage({
    platform,
    status,
    orderId,
    sessionId,
  }));
});

exports.stripeMerchWebhook = onRequest({
  region: "us-central1",
  timeoutSeconds: 60,
  secrets: [stripeWebhookSecret],
}, async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).send("Method Not Allowed");
    return;
  }

  const webhookSecret = loadStripeWebhookSecret();
  if (!webhookSecret) {
    logger.error("Stripe webhook secret missing.");
    response.status(500).send("Stripe webhook secret missing.");
    return;
  }

  try {
    verifyStripeWebhookSignature({
      payloadBuffer: request.rawBody,
      signatureHeader: request.headers["stripe-signature"],
      endpointSecret: webhookSecret,
    });
  } catch (error) {
    logger.error("Stripe webhook signature verification failed.", {
      error: error instanceof Error ? error.message : "unknown_error",
    });
    response.status(400).send("Invalid signature");
    return;
  }

  const event = JSON.parse(request.rawBody.toString("utf8"));
  const eventType = nonEmptyString(event?.type) || "";
  const stripeObject = event?.data?.object || {};

  const aiCheckoutResult = await processAiSubscriptionCheckoutWebhook(eventType, stripeObject);
  if (aiCheckoutResult.handled) {
    response.status(200).json({received: true, aiSubscription: true, status: aiCheckoutResult.status || "ignored"});
    return;
  }

  const aiLifecycleResult = await processAiSubscriptionLifecycleWebhook(eventType, stripeObject);
  if (aiLifecycleResult.handled) {
    response.status(200).json({received: true, aiSubscription: true, status: aiLifecycleResult.status || "ignored"});
    return;
  }

  const session = stripeObject;
  const orderId = nonEmptyString(session?.metadata?.orderId) || nonEmptyString(session?.client_reference_id);

  if (!orderId) {
    logger.warn("Stripe webhook received without order metadata.", {
      eventType,
      sessionId: nonEmptyString(session?.id),
    });
    response.status(200).json({received: true, ignored: true});
    return;
  }

  const orderRef = admin.firestore().doc(`orders/${orderId}`);
  const orderSnapshot = await orderRef.get();
  if (!orderSnapshot.exists) {
    logger.warn("Stripe webhook received for unknown order.", {
      eventType,
      orderId,
      sessionId: nonEmptyString(session?.id),
    });
    response.status(200).json({received: true, ignored: true});
    return;
  }

  const orderData = orderSnapshot.data() || {};
  const identifiers = extractStripeCheckoutIdentifiers(session);
  const stripeCheckoutStatus = deriveStripeCheckoutStatus(eventType, session);
  const shippingUpdate = formatStripeShippingAddress(session?.shipping_details);

  if (shouldConfirmPaymentFromStripeEvent(eventType, session)) {
    const paymentMethod = normalizeHostedCheckoutMethod(session?.metadata?.paymentMethod)
      || normalizeHostedCheckoutMethod(orderData.paymentMethod)
      || "Stripe";
    const requiresShopifySubmission = orderData.fulfillmentProvider === "podpartner";
    const confirmedUpdate = {
      paymentMethod,
      paymentProvider: "stripe",
      paymentStatus: "confirmed",
      paymentConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
      paymentConfirmedByUid: admin.firestore.FieldValue.delete(),
      paymentConfirmedByEmail: admin.firestore.FieldValue.delete(),
      paymentReference: identifiers.paymentIntentId || identifiers.sessionId || admin.firestore.FieldValue.delete(),
      stripeCheckoutSessionId: identifiers.sessionId || orderData.stripeCheckoutSessionId || admin.firestore.FieldValue.delete(),
      stripePaymentIntentId: identifiers.paymentIntentId || orderData.stripePaymentIntentId || admin.firestore.FieldValue.delete(),
      stripeCheckoutStatus,
      paymentFailureReason: admin.firestore.FieldValue.delete(),
      shopifySyncStatus: requiresShopifySubmission ?
        "pending_submission" :
        (orderData.shopifySyncStatus || "not_required"),
      ...shippingUpdate,
    };

    await orderRef.set(confirmedUpdate, {merge: true});

    logger.info("Stripe payment confirmed via webhook.", {
      eventType,
      orderId,
      paymentMethod,
      sessionId: identifiers.sessionId,
      paymentIntentId: identifiers.paymentIntentId,
    });

    response.status(200).json({received: true, confirmed: true});
    return;
  }

  if (["checkout.session.async_payment_failed", "checkout.session.expired"].includes(eventType)) {
    await orderRef.set({
      paymentStatus: eventType === "checkout.session.expired" ? "expired" : "failed",
      paymentFailureReason: eventType === "checkout.session.expired" ?
        "Stripe Checkout ist abgelaufen." :
        "Stripe oder Klarna konnten die Zahlung nicht abschliessen.",
      stripeCheckoutSessionId: identifiers.sessionId || orderData.stripeCheckoutSessionId || admin.firestore.FieldValue.delete(),
      stripePaymentIntentId: identifiers.paymentIntentId || orderData.stripePaymentIntentId || admin.firestore.FieldValue.delete(),
      stripeCheckoutStatus,
    }, {merge: true});

    logger.warn("Stripe payment not completed.", {
      eventType,
      orderId,
      sessionId: identifiers.sessionId,
      paymentIntentId: identifiers.paymentIntentId,
    });

    response.status(200).json({received: true, confirmed: false});
    return;
  }

  await orderRef.set({
    stripeCheckoutSessionId: identifiers.sessionId || orderData.stripeCheckoutSessionId || admin.firestore.FieldValue.delete(),
    stripePaymentIntentId: identifiers.paymentIntentId || orderData.stripePaymentIntentId || admin.firestore.FieldValue.delete(),
    stripeCheckoutStatus,
  }, {merge: true});

  response.status(200).json({received: true, ignored: true});
});

exports.triggerWorkflowAutomation = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "triggerWorkflowAutomation");
  assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um einen Workflow-Test zu starten.",
  );

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
  await assertCallableSecurity(request, "authorizeAiUsage");
  const kind = nonEmptyString(request.data?.kind)?.toLowerCase() || AI_USAGE_KINDS.text;
  return authorizeAiUsage({
    auth: request.auth,
    kind,
  });
});

exports.generateAiText = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "generateAiText");
  const input = parseCallableInput(
      aiTextRequestSchema,
      request.data,
      "Die KI-Anfrage konnte so nicht gestartet werden.",
  );
  const usage = await authorizeAiUsage({
    auth: request.auth,
    kind: AI_USAGE_KINDS.text,
  });
  const reply = await generateAiTextReply({
    prompt: input.prompt,
    mode: input.mode,
  });

  return {
    reply,
    mode: input.mode,
    historyRetentionDays: usage.historyRetentionDays,
  };
});

exports.generateAiVisual = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "generateAiVisual");
  const input = parseCallableInput(
      aiVisualRequestSchema,
      request.data,
      "Die Visual-Anfrage konnte so nicht gestartet werden.",
  );
  const usage = await authorizeAiUsage({
    auth: request.auth,
    kind: AI_USAGE_KINDS.visual,
  });
  const visual = await generateAiVisualResult(input.prompt);

  return {
    ...visual,
    historyRetentionDays: usage.historyRetentionDays,
  };
});

function responseFrameworkHint(mode, prompt) {
  if (mode === AGENT_MODES.release) {
    return "Antwortformat: Ziel, Release-Phasen, konkrete Schritte mit Reihenfolge, Timing, benoetigte Assets, Risiken, Naechste 3 Schritte.";
  }

  if (mode === AGENT_MODES.briefing) {
    return "Antwortformat: Ziel, Kontext, Tonalitaet, Deliverables, Shotlist oder Content-Bausteine, Assets, Risiken, Naechste Schritte.";
  }

  if (mode === AGENT_MODES.content) {
    return "Antwortformat: Ziel, Content-Saulen, Channel-Plan, Hook-Ideen, Asset-Bedarf, CTA, Naechste 3 Schritte.";
  }

  if (mode === AGENT_MODES.merch) {
    return "Antwortformat: Ziel, Produkt-/Drop-Angle, Launch-Abfolge, Store-/Content-Bedarf, Checkliste, Risiken, Naechste 3 Schritte.";
  }

  if (mode === AGENT_MODES.automation) {
    return "Antwortformat: Workflow-Ziel, Trigger, benoetigte Inputs, erwartete Outputs, Fehlerfaelle, kurze Uebergabe an n8n, Naechste Schritte.";
  }

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

function composeAgentExecutionPrompt({workspaceContext, history, prompt, mode}) {
  return `
${workspaceContext}

Bisherige Unterhaltung:
${formatHistory(history)}

Aktuelle Nutzeranfrage:
${prompt}

${responseFrameworkHint(mode, prompt)}
  `.trim();
}

function composePersonalAgentProfileInstruction(personalAgentProfile) {
  if (!personalAgentProfile || personalAgentProfile.isEnabled !== true) {
    return "";
  }

  const sections = [];
  if (personalAgentProfile.roleLabel) {
    sections.push(`Persoenlicher Fokus dieses Kontos:\n${personalAgentProfile.roleLabel}`);
  }
  if (personalAgentProfile.skillProfile) {
    sections.push(`Persoenliche Skills und Spezialisierungen:\n${personalAgentProfile.skillProfile}`);
  }
  if (personalAgentProfile.outputFormat) {
    sections.push(`Gewuenschtes Ausgabeformat / Deliverables:\n${personalAgentProfile.outputFormat}`);
  }
  if (personalAgentProfile.guardrails) {
    sections.push(`Guardrails / No-Gos:\n${personalAgentProfile.guardrails}`);
  }
  if (personalAgentProfile.knowledgeContext) {
    sections.push(`Persoenlicher Knowledge-Kontext:\n${personalAgentProfile.knowledgeContext}`);
  }
  if (!sections.length) {
    return "";
  }

  return `
Zusaetzliche persoenliche Agent-Vorgaben (nur fuer dieses Konto):
${sections.join("\n\n")}
  `.trim();
}

function buildEffectiveAgentSystemInstruction(globalSystemInstruction, personalAgentProfile) {
  const globalInstruction = normalizeAiPromptSetting(
      globalSystemInstruction,
      DEFAULT_AI_PROMPT_SETTINGS.agentSystemInstruction,
  );
  const personalInstruction = composePersonalAgentProfileInstruction(personalAgentProfile);
  if (!personalInstruction) {
    return globalInstruction;
  }

  return normalizeAiPromptSetting(
      `${globalInstruction}\n\n${personalInstruction}`,
      DEFAULT_AI_PROMPT_SETTINGS.agentSystemInstruction,
  );
}

function trimTextMax(value, maxChars) {
  const normalized = nonEmptyString(value) || "";
  if (!maxChars || maxChars < 1 || normalized.length <= maxChars) {
    return normalized;
  }
  return normalized.slice(0, maxChars);
}

function sanitizeHistoryForManus(history, manusSettings) {
  const maxHistoryTurns = manusSettings.maxHistoryTurns;
  if (!Array.isArray(history) || maxHistoryTurns < 1) {
    return [];
  }

  return history
      .slice(-maxHistoryTurns)
      .map(({role, text}) => ({
        role: role === "assistant" ? "assistant" : "user",
        text: trimTextMax(text, manusSettings.maxPromptChars),
      }))
      .filter((entry) => entry.text.length > 0);
}

function manusApiErrorMessage(payload, fallbackMessage = "Unbekannter Manus-Fehler.") {
  return nonEmptyString(payload?.error?.message)
    || nonEmptyString(payload?.message)
    || fallbackMessage;
}

function resolveManusTaskId(payload) {
  return nonEmptyString(payload?.task_id)
    || nonEmptyString(payload?.taskId)
    || nonEmptyString(payload?.task?.id)
    || nonEmptyString(payload?.id)
    || null;
}

function resolveManusMessages(payload) {
  if (Array.isArray(payload?.messages)) {
    return payload.messages;
  }
  if (Array.isArray(payload?.data)) {
    return payload.data;
  }
  return [];
}

function resolveLatestManusStatus(messages) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return null;
  }

  for (const message of messages) {
    if (nonEmptyString(message?.type) !== "status_update") {
      continue;
    }

    const statusUpdate = message.status_update || {};
    return {
      agentStatus: nonEmptyString(statusUpdate.agent_status) || "",
      waitingForEventType: nonEmptyString(statusUpdate?.status_detail?.waiting_for_event_type) || "",
      waitingDescription: nonEmptyString(statusUpdate?.status_detail?.waiting_description) || "",
    };
  }

  return null;
}

function resolveLatestManusAssistantReply(messages) {
  if (!Array.isArray(messages) || messages.length === 0) {
    return null;
  }

  for (const message of messages) {
    if (nonEmptyString(message?.type) !== "assistant_message") {
      continue;
    }

    const content = nonEmptyString(message?.assistant_message?.content)
      || nonEmptyString(message?.content)
      || nonEmptyString(message?.text);
    if (content) {
      return content;
    }
  }

  return null;
}

function waitForMs(durationMs) {
  return new Promise((resolve) => setTimeout(resolve, durationMs));
}

async function loadAgentWorkspaceContext(auth, promptSettings, personalAgentProfile = DEFAULT_PERSONAL_AGENT_PROFILE_SETTINGS) {
  const lines = [];

  if (auth?.uid) {
    const userData = await loadUserData(auth.uid).catch(() => null);
    if (userData) {
      const profile = buildUserProfile(userData);
      lines.push(`Konto-Rolle: ${profile.role}`);
      lines.push(`KI-Zugang aktiv: ${profile.aiAccessEnabled ? "ja" : "nein"}`);
    }

    const workflowSettings = await loadWorkflowAutomationSettingsForUser(auth.uid).catch(() => null);
    if (workflowSettings) {
      const workflowStatus = workflowSettings.isEnabled && buildAutomationWebhookUrl(workflowSettings.baseURL, workflowSettings.webhookPath) ?
        `bereit (${workflowSettings.workflowName})` :
        "noch nicht bereit";
      lines.push(`n8n-Status: ${workflowStatus}`);
      const knowledgeContext = nonEmptyString(workflowSettings.knowledgeContext);
      if (knowledgeContext) {
        lines.push(`Persoenlicher Knowledge-Kontext: ${knowledgeContext}`);
      }
    }

    if (personalAgentProfile?.isEnabled) {
      lines.push("Persoenliches Agent-Profil: aktiv");
      if (personalAgentProfile.roleLabel) {
        lines.push(`Fokus: ${trimTextMax(personalAgentProfile.roleLabel, 220)}`);
      }
      if (personalAgentProfile.skillProfile) {
        lines.push(`Skills-Profil: ${trimTextMax(personalAgentProfile.skillProfile, 320)}`);
      }
    }
  }

  const shopifyConfig = await loadShopifyAdminConfig().catch(() => null);
  if (shopifyConfig) {
    lines.push(`Shopify-Store: ${shopifyConfig.storeDomain}`);
    if (shopifyConfig.collectionHandles.length) {
      lines.push(`Aktive Shopify-Collections: ${shopifyConfig.collectionHandles.join(", ")}`);
    }
  }

  const assetContext = composeAssetLibraryPromptContext(promptSettings);
  if (assetContext) {
    lines.push(assetContext);
  }

  const context = lines.length ?
    `Arbeitskontext:\n- ${lines.join("\n- ")}` :
    "Arbeitskontext: kein zusaetzlicher Workspace-Kontext verfuegbar.";
  return context.slice(0, 12000);
}

async function maybeTriggerAgentAutomation({auth, mode, prompt, reply, history}) {
  if (!auth?.uid) {
    return {attempted: false, triggered: false};
  }

  try {
    const automationResult = await triggerWorkflowAutomationWebhook({
      trigger: `agent_${mode}`,
      source: "agent",
      auth,
      data: {
        mode,
        prompt,
        reply,
        history: history.slice(-8),
      },
    });

    return {
      attempted: true,
      triggered: true,
      message: nonEmptyString(automationResult.message) || "An n8n gesendet.",
      workflowName: nonEmptyString(automationResult.workflowName) || "",
    };
  } catch (error) {
    return {
      attempted: true,
      triggered: false,
      message: error instanceof Error ? error.message : `${error}`,
    };
  }
}

function loadManusApiKey(overrideKey = "") {
  const override = nonEmptyString(overrideKey) || "";
  if (override) {
    return override;
  }

  return nonEmptyString(manusApiKey.value()) || "";
}

async function callManusApi({apiKey, endpoint, method = "GET", query = {}, body = null, timeoutMs = 12000}) {
  const url = new URL(`${MANUS_API_BASE_URL}/${endpoint}`);
  const queryEntries = Object.entries(query || {})
      .filter(([, value]) => value !== undefined && value !== null && `${value}`.trim().length > 0);
  for (const [key, value] of queryEntries) {
    url.searchParams.set(key, `${value}`);
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url.toString(), {
      method,
      headers: {
        "Content-Type": "application/json",
        "x-manus-api-key": apiKey,
      },
      body: body ? JSON.stringify(body) : undefined,
      signal: controller.signal,
    });

    const rawText = await response.text().catch(() => "");
    let payload = null;
    if (rawText) {
      try {
        payload = JSON.parse(rawText);
      } catch (error) {
        payload = null;
      }
    }

    if (!response.ok) {
      const errorMessage = manusApiErrorMessage(
          payload,
          `Manus API Fehler (${response.status} ${response.statusText}).`,
      );
      throw new Error(errorMessage);
    }

    if (payload && payload.ok === false) {
      throw new Error(manusApiErrorMessage(payload));
    }

    return payload || {};
  } finally {
    clearTimeout(timer);
  }
}

async function stopManusTaskQuietly({taskId, runtimeSettings, apiKey}) {
  const resolvedApiKey = loadManusApiKey(apiKey);
  if (!resolvedApiKey || !taskId) {
    return;
  }

  try {
    await callManusApi({
      apiKey: resolvedApiKey,
      endpoint: "task.stop",
      method: "POST",
      body: {task_id: taskId},
      timeoutMs: runtimeSettings.manus.requestTimeoutMs,
    });
  } catch (error) {
    logger.warn("Manus task could not be stopped gracefully.", {
      taskId,
      error: error instanceof Error ? error.message : `${error}`,
    });
  }
}

async function fetchManusMessages({apiKey, taskId, runtimeSettings}) {
  const response = await callManusApi({
    apiKey,
    endpoint: "task.listMessages",
    method: "GET",
    query: {
      task_id: taskId,
      order: "desc",
      limit: runtimeSettings.manus.listMessagesLimit,
      verbose: runtimeSettings.manus.includeVerboseEvents ? "true" : undefined,
    },
    timeoutMs: runtimeSettings.manus.requestTimeoutMs,
  });

  return resolveManusMessages(response);
}

function normalizeManusAgentPrompt({
  input,
  runtimeSettings,
  promptSettings,
  workspaceContext,
}) {
  const safePrompt = trimTextMax(input.prompt, runtimeSettings.manus.maxPromptChars);
  const safeHistory = sanitizeHistoryForManus(input.history, runtimeSettings.manus);
  const composedPrompt = composeAgentExecutionPrompt({
    workspaceContext: trimTextMax(workspaceContext, runtimeSettings.manus.maxPromptChars),
    history: safeHistory,
    prompt: safePrompt,
    mode: input.mode,
  });

  return `
System-Anweisung:
${trimTextMax(promptSettings.agentSystemInstruction, runtimeSettings.manus.maxPromptChars)}

${composedPrompt}
  `.trim();
}

async function runManusAgent({
  input,
  runtimeSettings,
  promptSettings,
  workspaceContext,
  manusApiKeyOverride = "",
}) {
  if (runtimeSettings.manus.isEnabled !== true) {
    throw new HttpsError(
        "failed-precondition",
        "Manus ist noch nicht freigeschaltet. Bitte in adminConfig/aiRuntime aktivieren.",
    );
  }

  const apiKey = loadManusApiKey(manusApiKeyOverride);
  if (!apiKey) {
    throw new HttpsError(
        "failed-precondition",
        "MANUS_API_KEY fehlt. Hinterlege einen Key in den Functions-Secrets oder sende einen eigenen BYOS-Key.",
    );
  }

  const prompt = normalizeManusAgentPrompt({
    input,
    runtimeSettings,
    promptSettings,
    workspaceContext,
  });
  const createPayload = {
    message: {
      content: prompt,
    },
  };

  const createResponse = await callManusApi({
    apiKey,
    endpoint: "task.create",
    method: "POST",
    body: createPayload,
    timeoutMs: runtimeSettings.manus.requestTimeoutMs,
  });
  const taskId = resolveManusTaskId(createResponse);
  if (!taskId) {
    throw new HttpsError("internal", "Manus Task konnte nicht gestartet werden.");
  }

  let taskStatus = "";
  for (let attempt = 0; attempt < runtimeSettings.manus.maxPollAttempts; attempt += 1) {
    const statusResponse = await callManusApi({
      apiKey,
      endpoint: "task.detail",
      method: "GET",
      query: {task_id: taskId},
      timeoutMs: runtimeSettings.manus.requestTimeoutMs,
    });
    taskStatus = nonEmptyString(statusResponse?.task?.status)
      || nonEmptyString(statusResponse?.status)
      || "";

    if (taskStatus === "stopped") {
      break;
    }

    if (taskStatus === "error") {
      const messages = await fetchManusMessages({apiKey, taskId, runtimeSettings});
      const latestReply = resolveLatestManusAssistantReply(messages);
      throw new HttpsError(
          "internal",
          latestReply || "Manus hat die Aufgabe mit einem Fehler beendet.",
      );
    }

    if (taskStatus === "waiting") {
      const messages = await fetchManusMessages({apiKey, taskId, runtimeSettings});
      const statusUpdate = resolveLatestManusStatus(messages);
      const waitingType = statusUpdate?.waitingForEventType || "";
      const waitingDescription = statusUpdate?.waitingDescription || "Aktion muss bestaetigt werden.";

      if (runtimeSettings.manus.blockHighCreditEvents && waitingType === "apiHighCreditNotice") {
        await stopManusTaskQuietly({taskId, runtimeSettings, apiKey});
        throw new HttpsError(
            "resource-exhausted",
            "Kosten-Guard aktiv: Manus hat eine High-Credit-Freigabe angefragt. Task wurde gestoppt.",
        );
      }

      if (runtimeSettings.manus.autoStopOnWaiting) {
        await stopManusTaskQuietly({taskId, runtimeSettings, apiKey});
        throw new HttpsError(
            "failed-precondition",
            `Kosten-Guard aktiv: Manus wartet auf manuelle Freigabe (${waitingDescription}).`,
        );
      }
    }

    if (attempt < runtimeSettings.manus.maxPollAttempts - 1) {
      await waitForMs(runtimeSettings.manus.pollIntervalMs);
    }
  }

  if (taskStatus !== "stopped") {
    await stopManusTaskQuietly({taskId, runtimeSettings, apiKey});
    throw new HttpsError(
        "deadline-exceeded",
        "Manus hat nicht rechtzeitig geantwortet. Task wurde zum Kostenschutz gestoppt.",
    );
  }

  const messages = await fetchManusMessages({apiKey, taskId, runtimeSettings});
  const reply = resolveLatestManusAssistantReply(messages);
  if (!reply) {
    throw new HttpsError("internal", "Manus hat keine Agent-Antwort geliefert.");
  }

  return {
    reply,
    provider: AI_AGENT_PROVIDERS.manus,
    taskId,
  };
}

const skydownAgentFlow = ai.defineFlow({
  name: "skydownAgentFlow",
  inputSchema: agentFlowRequestSchema,
  outputSchema: z.string(),
  streamSchema: z.string(),
}, async (input, sendChunk) => {
  const prompt = composeAgentExecutionPrompt({
    workspaceContext: input.workspaceContext,
    history: input.history,
    prompt: input.prompt,
    mode: input.mode,
  });

  const {stream, response} = ai.generateStream({
    system: input.systemInstruction,
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

exports.skydownAgent = onCall({
  region: "us-central1",
  timeoutSeconds: 90,
  secrets: [manusApiKey],
}, async (request) => {
  await assertCallableSecurity(request, "skydownAgent");
  const input = parseCallableInput(
      agentRequestSchema,
      request.data,
      "Die Agent-Anfrage konnte so nicht verarbeitet werden.",
  );
  const usage = await authorizeAiUsage({
    auth: request.auth,
    kind: AI_USAGE_KINDS.agent,
  });
  const promptSettings = await loadAiPromptSettings();
  const personalAgentProfile = await loadPersonalAgentProfileSettingsForUser(request.auth?.uid);
  const effectiveAgentSystemInstruction = buildEffectiveAgentSystemInstruction(
      promptSettings.agentSystemInstruction,
      personalAgentProfile,
  );
  const effectivePromptSettings = {
    ...promptSettings,
    agentSystemInstruction: effectiveAgentSystemInstruction,
  };
  const runtimeSettings = await loadAiRuntimeSettings();
  const workspaceContext = await loadAgentWorkspaceContext(
      request.auth,
      promptSettings,
      personalAgentProfile,
  );
  let reply = "";
  let agentProvider = AI_AGENT_PROVIDERS.gemini;
  let providerFallbackUsed = false;
  let providerNotice = "";

  if (runtimeSettings.agentProvider === AI_AGENT_PROVIDERS.manus) {
    try {
      const manusResult = await runManusAgent({
        input,
        runtimeSettings,
        promptSettings: effectivePromptSettings,
        workspaceContext,
        manusApiKeyOverride: input.manusApiKeyOverride,
      });
      reply = manusResult.reply;
      agentProvider = manusResult.provider;
    } catch (error) {
      if (runtimeSettings.fallbackAgentProvider === AI_AGENT_PROVIDERS.gemini) {
        providerFallbackUsed = true;
        providerNotice = "Manus war nicht verfuegbar. Antwort wurde ueber Gemini erstellt.";
        logger.warn("Manus agent failed. Falling back to Gemini.", {
          uid: request.auth?.uid || null,
          error: error instanceof Error ? error.message : `${error}`,
        });
      } else {
        throw error;
      }
    }
  }

  if (!reply) {
    reply = await skydownAgentFlow({
      prompt: input.prompt,
      history: input.history,
      mode: input.mode,
      executeAutomation: input.executeAutomation,
      systemInstruction: effectiveAgentSystemInstruction,
      workspaceContext,
    });
    agentProvider = AI_AGENT_PROVIDERS.gemini;
  }

  const automation = input.executeAutomation ?
    await maybeTriggerAgentAutomation({
      auth: request.auth,
      mode: input.mode,
      prompt: input.prompt,
      reply,
      history: input.history,
    }) :
    {attempted: false, triggered: false};

  return {
    reply,
    mode: input.mode,
    automationTriggered: automation.triggered === true,
    automationAttempted: automation.attempted === true,
    automationMessage: nonEmptyString(automation.message) || "",
    workflowName: nonEmptyString(automation.workflowName) || "",
    agentProvider,
    providerFallbackUsed,
    providerNotice,
    historyRetentionDays: usage.historyRetentionDays,
  };
});

exports.syncShopifyMerch = onCall({
  region: "us-central1",
  timeoutSeconds: 120,
}, async (request) => {
  await assertCallableSecurity(request, "syncShopifyMerch");
  await assertOwner(request.auth);
  return runShopifyMerchSync();
});

exports.listShopifyCollections = onCall({
  region: "us-central1",
  timeoutSeconds: 120,
}, async (request) => {
  await assertCallableSecurity(request, "listShopifyCollections");
  await assertOwner(request.auth);

  const config = await loadShopifyAdminConfig();
  const collections = await fetchAvailableShopifyCollections(config);

  return {
    storeDomain: config.storeDomain,
    selectedCollectionHandles: config.collectionHandles || [],
    collections,
  };
});

exports.syncCurrentUserClaims = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "syncCurrentUserClaims");
  return syncClaimsForCurrentUser(request.auth);
});

exports.deleteCurrentUserAccount = onCall({
  region: "us-central1",
  timeoutSeconds: 120,
}, async (request) => {
  await assertCallableSecurity(request, "deleteCurrentUserAccount");
  const uid = assertRecentAccountDeletionAuth(request.auth);
  const email = normalizeEmail(request.auth?.token?.email);
  const deletionSummary = await purgeCurrentUserAccountData(uid, email);
  await admin.auth().deleteUser(uid);

  logger.info("User account deleted with server cleanup.", {
    uid,
    deletedOrders: deletionSummary.deletedOrders,
    deletedStorageObjects: deletionSummary.deletedStorageObjects,
    deletedUploadSlots: deletionSummary.deletedUploadSlots,
  });

  return {
    deleted: true,
    ...deletionSummary,
  };
});

exports.setUserRole = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "setUserRole");
  await assertOwner(request.auth);

  return setUserRoleClaims({
    uid: request.data?.uid,
    requestedRole: request.data?.role,
    updatedByUid: request.auth.uid,
    updatedByEmail: normalizeEmail(request.auth.token?.email),
  });
});

exports.requestUploadSlot = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  const appCheckState = await assertCallableSecurity(request, "requestUploadSlot");
  return requestUploadSlot({
    auth: request.auth,
    data: request.data,
    appCheckState,
  });
});

exports.setRuntimeLockdown = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "setRuntimeLockdown");
  await assertOwner(request.auth);

  const enabled = request.data?.lockdown === true;
  const source = enabled ? "owner_manual_lockdown" : "owner_manual_unlock";
  const reason = nonEmptyString(request.data?.reason) || (enabled ? "owner_manual" : "owner_recovery");

  return mergeRuntimeConfig({
    lockdown: enabled,
    uploadsEnabled: enabled ? false : true,
    registrationsEnabled: enabled ? false : true,
    userWritesEnabled: enabled ? false : true,
    lastLockdownReason: reason,
  }, source);
});

exports.applyBudgetLockdown = onMessagePublished({
  topic: process.env.BILLING_BUDGET_TOPIC || "billing-budget-alerts",
  region: "us-central1",
}, async (event) => {
  const payload = event.data.message.json || {};
  const budgetDisplayName = nonEmptyString(payload?.budgetDisplayName) || "budget";
  const costAmount = Number(payload?.costAmount) || Number(payload?.costAmount?.units) || null;

  logger.error("Budget alert received. Runtime lockdown is being applied.", {
    budgetDisplayName,
    costAmount,
    topic: process.env.BILLING_BUDGET_TOPIC || "billing-budget-alerts",
  });

  await mergeRuntimeConfig({
    lockdown: true,
    uploadsEnabled: false,
    registrationsEnabled: false,
    userWritesEnabled: false,
    budgetLockdownEnabled: true,
    lastLockdownReason: `${BILLING_LOCKDOWN_REASON_PREFIX}:${budgetDisplayName}`,
  }, "billing_budget_alert");
});

exports.enforceRegistrationLockdown = functionsV1.auth.user().onCreate(async (user) => {
  const email = normalizeEmail(user.email);
  const role = email === OWNER_EMAIL ? USER_ROLES.owner : USER_ROLES.user;
  const runtimeConfig = await getRuntimeConfig({forceRefresh: true});

  if (!areRegistrationsBlocked(runtimeConfig, role)) {
    await ensureAuthUserBootstrapDocument(user, {forcedRole: role});
    return;
  }

  if (email === OWNER_EMAIL) {
    logger.warn("Owner registration bypassed lockdown to keep recovery access.", {
      uid: user.uid,
      email,
    });
    await ensureAuthUserBootstrapDocument(user, {forcedRole: USER_ROLES.owner});
    return;
  }

  logger.error("Registration created during lockdown. User will be deleted.", {
    uid: user.uid,
    email,
  });
  await admin.auth().deleteUser(user.uid);
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

  const subject = `Neue 22xSky Bestellung - ${customerEmail}`;
  const text = `
Hallo 22xSky-Team,

es wurde eine neue Bestellung in 22xSky erstellt.

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
