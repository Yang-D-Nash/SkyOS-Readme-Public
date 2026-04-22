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
  decodeAppStoreTransactions,
  resolveAppStoreSubscriptionState,
} = require("./src/payments/app-store-subscriptions");
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
const xaiApiKey = defineSecret("XAI_API_KEY");
const OWNER_EMAIL = "nash.lioncorna@gmail.com";
const IOS_APP_BUNDLE_ID = "com.skydown.ios";
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
Wenn das Motiv wie ein Foto, Filmstill oder Editorial-Frame gedacht ist, arbeite mit praeziser Kamera-, Lens- und Lichtsprache statt mit vagen Stilwoertern.
Nutze nur sehr wenig Text im Bild. Wenn Text im Motiv vorkommt, dann maximal eine kurze Headline.
Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.
Antworte auf Deutsch.
`.trim();

const AI_VISUAL_PHOTOGRAPHY_DIRECTION = `
Wenn die Anfrage wie ein Foto, Portrait, Street-Shot, Editorial, Produktfoto oder realistischer Kamera-Frame wirkt:
- Inszeniere das Motiv wie ein hochwertiges Filmstill oder Editorial-Foto, nicht wie generische AI-Art.
- Nutze glaubwuerdige Kamera-Sprache: ARRI-artiger Cinematic-Look, hochwertige Prime-Linse, haeufig 30mm oder 35mm, offene Blende etwa f/1.4 bis f/2.0 wenn passend.
- Arbeite mit natuerlicher Tiefenstaffelung, selektivem Fokus, organischem Bokeh und sauberem Fokus-Falloff.
- Lichtsetzung: motiviert, filmisch, weich aber gerichtet, mit realistischen Highlights und Schatten statt flacher Ausleuchtung.
- Materialien wie Haut, Stoffe, Metall, Glas, Rauch und Asphalt sollen hochwertig und real wirken.
- Bevorzuge echte Foto-Anmutung mit realistischer Haut, glaubwuerdigen Proportionen, natuerlichen Haenden, sauberer Anatomie und physikalisch plausibler Perspektive.
- Lieber subtile Filmkoernung, realistische Dynamik und feine Objektivcharakteristik als ueberschaerfte digitale Optik.
- Vermeide explizit Illustration, Painting, 3D-Render, CGI, Plastik-Haut, uebertriebenen Glow, unnatuerliche Farben und den typischen generischen AI-Look.
- Uebertreibe Blur, Bloom und Glow nicht; das Hauptmotiv muss klar lesbar bleiben.
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

const AI_FEATURE_CLASSES = {
  text: "text",
  image: "image",
  agent: "agent",
  workflow: "workflow",
  videoFuture: "videoFuture",
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
  grok: "grok",
};

const XAI_CHAT_COMPLETIONS_URL = "https://api.x.ai/v1/chat/completions";
const GROK_AGENT_MODEL = process.env.GROK_AGENT_MODEL || "grok-2-latest";
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
const AI_USAGE_MONTHLY_METRICS_DOCUMENT_PREFIX = "aiUsageMonthly_";
const AI_USAGE_EVENTS_COLLECTION = "aiUsageEvents";
const AI_MEMBERSHIP_EVENTS_COLLECTION = "aiMembershipEvents";
const AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION = "recommendationLifecycle";
const AI_GUARDRAILS_COLLECTION = "guardrails";
const AI_GUARDRAILS_DAILY_BURN_PREFIX = "aiDailyBurn_";
const AI_ENTITLEMENTS_SUBCOLLECTION = "entitlements";
const AI_ENTITLEMENT_DOCUMENT = "ai";
const AI_ENTITLEMENT_EVENTS_SUBCOLLECTION = "entitlementEvents";
const AI_MEMBERSHIP_EVENT_TYPES = Object.freeze([
  "membership_open",
  "membership_reason",
  "plan_selected",
  "annual_toggle_changed",
  "purchase_started",
  "purchase_success",
  "purchase_cancelled",
  "restore_success",
  "upgrade_after_warning",
  "upgrade_after_deny",
]);
const AI_MEMBERSHIP_RECOMMENDATION_STATUSES = Object.freeze([
  "proposed",
  "active",
  "completed",
  "rejected",
]);
const MANUS_API_BASE_URL = "https://api.manus.ai/v2";
const ACCOUNT_DELETE_RECENT_AUTH_MAX_AGE_SECONDS = 5 * 60;

const DEFAULT_AI_RUNTIME_SETTINGS = Object.freeze({
  costGuardEnabled: true,
  agentProvider: AI_AGENT_PROVIDERS.grok,
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
  costGuardrails: {
    warningThresholdPercent: 70,
    criticalThresholdPercent: 90,
    hardStopPercent: 100,
    dailyBurnCapMicros: 35_000_000,
    enableDegradeMode: true,
    suspiciousSpikeWindowSeconds: 30,
    suspiciousSpikeRequestThreshold: 8,
  },
  planRouting: {
    free: {
      providers: ["google_vertex"],
      maxEstimatedCostMicros: 80_000,
      allowWorkflow: false,
      degradeProvider: "google_vertex",
      degradeModel: "gemini-2.5-flash-lite",
    },
    creator: {
      providers: ["google_vertex", "grok"],
      maxEstimatedCostMicros: 280_000,
      allowWorkflow: false,
      degradeProvider: "google_vertex",
      degradeModel: "gemini-2.5-flash-lite",
    },
    studio: {
      providers: ["google_vertex", "grok", "manus"],
      maxEstimatedCostMicros: 900_000,
      allowWorkflow: true,
      degradeProvider: "grok",
      degradeModel: GROK_AGENT_MODEL,
    },
  },
  costEstimates: {
    byFeatureClassMicros: {
      text: 12_000,
      image: 95_000,
      agent: 110_000,
      workflow: 260_000,
      videoFuture: 750_000,
    },
    providerMultipliers: {
      google_vertex: 100,
      grok: 130,
      manus: 170,
    },
    modelMultipliers: {
      "gemini-2.5-flash-lite": 100,
      "gemini-2.5-flash-image": 115,
      "imagen-3.0-generate-002": 145,
      "grok-2-latest": 140,
    },
  },
  membershipHygiene: {
    cooldownDaysCompleted: 10,
    cooldownDaysRejected: 21,
    cooldownDaysProposed: 7,
    similarityStrictness: "balanced",
    recurringPenalty: 0.18,
    freshnessFloor: 0.2,
    duplicateMergeWindowDays: 14,
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

function resolveLegacyAiEntitlement(userData = {}) {
  const plan = normalizeAiSubscriptionPlan(userData.aiSubscriptionPlan);
  const status = normalizeAiSubscriptionStatus(userData.aiSubscriptionStatus, "inactive");
  const source = nonEmptyString(userData.aiSubscriptionSourcePlatform)?.toLowerCase() || "";
  const provider = nonEmptyString(userData.aiSubscriptionProvider)?.toLowerCase() || "";
  return {
    plan,
    status,
    provider,
    source,
    productId: nonEmptyString(userData.aiSubscriptionProductId) || "",
    periodStartEpochSeconds: Number(userData.aiSubscriptionActivatedAtEpochSeconds || 0) || 0,
    periodEndEpochSeconds: Number(userData.aiSubscriptionCurrentPeriodEndEpochSeconds || 0) || 0,
    environment: nonEmptyString(userData.aiSubscriptionStoreEnvironment)?.toLowerCase() || "",
    originalTransactionId: nonEmptyString(userData.aiSubscriptionOriginalTransactionId) || "",
    purchaseReference:
      nonEmptyString(userData.aiSubscriptionTransactionId) ||
      nonEmptyString(userData.aiSubscriptionStripeSubscriptionId) ||
      "",
    capabilities: resolveAiCapabilities({
      plan,
      status,
      role: resolveUserRole(userData.role, userData.isAdmin === true, userData.email),
    }),
    updatedAtEpochMillis: Date.now(),
    schemaVersion: 2,
  };
}

function resolveAiCapabilities({plan, status, role = USER_ROLES.user} = {}) {
  const normalizedStatus = normalizeAiSubscriptionStatus(status, "inactive");
  const normalizedPlan = normalizeAiSubscriptionPlan(plan);
  const isActive = isAiSubscriptionStatusActive(normalizedStatus);
  const defaults = defaultAiLimitsForQuotaPlan(
      normalizedPlan ||
      (role === USER_ROLES.owner ? USER_QUOTA_PLANS.ownerUnlimited : USER_QUOTA_PLANS.free),
  );
  return {
    botText: true,
    botImage: isActive || normalizedPlan === USER_QUOTA_PLANS.creator || normalizedPlan === USER_QUOTA_PLANS.studio,
    agentStandard: isActive || normalizedPlan === USER_QUOTA_PLANS.creator || normalizedPlan === USER_QUOTA_PLANS.studio,
    workflowAutomation: isActive && normalizedPlan === USER_QUOTA_PLANS.studio,
    premiumOutputs: isActive && normalizedPlan === USER_QUOTA_PLANS.studio,
    textDailyLimit: defaults.text,
    imageDailyLimit: defaults.visual,
    agentDailyLimit: defaults.agent,
  };
}

function resolveCanonicalAiEntitlement(data = {}) {
  const plan = normalizeAiSubscriptionPlan(data.plan);
  const status = normalizeAiSubscriptionStatus(data.status, "inactive");
  const provider = nonEmptyString(data.provider)?.toLowerCase() || "";
  const source = nonEmptyString(data.source || data.sourcePlatform)?.toLowerCase() || "";
  const productId = nonEmptyString(data.productId) || "";
  const periodStartEpochSeconds = Number(data.periodStartEpochSeconds || 0) || 0;
  const periodEndEpochSeconds = Number(
      data.periodEndEpochSeconds || data.currentPeriodEndEpochSeconds || 0,
  ) || 0;
  const environment = nonEmptyString(data.environment)?.toLowerCase() || "";
  const originalTransactionId = nonEmptyString(data.originalTransactionId) || "";
  const purchaseReference = nonEmptyString(data.purchaseReference) || "";
  const schemaVersion = Number(data.schemaVersion || 2) || 2;
  return {
    plan,
    status,
    provider,
    source,
    productId,
    periodStartEpochSeconds,
    periodEndEpochSeconds,
    capabilities: data.capabilities && typeof data.capabilities === "object" && !Array.isArray(data.capabilities) ?
      data.capabilities :
      resolveAiCapabilities({plan, status}),
    environment,
    originalTransactionId,
    purchaseReference,
    updatedAtEpochMillis: Number(data.updatedAtEpochMillis || Date.now()) || Date.now(),
    version: Number(data.version || 1) || 1,
    originEventId: nonEmptyString(data.originEventId) || "",
    schemaVersion,
  };
}

function entitlementPayloadFromState(state, {
  originEventId = "",
  version = 1,
} = {}) {
  const normalized = resolveCanonicalAiEntitlement(state || {});
  return {
    status: normalized.status,
    plan: normalized.plan || admin.firestore.FieldValue.delete(),
    provider: normalized.provider || admin.firestore.FieldValue.delete(),
    source: normalized.source || admin.firestore.FieldValue.delete(),
    productId: normalized.productId || admin.firestore.FieldValue.delete(),
    periodStartEpochSeconds: normalized.periodStartEpochSeconds || admin.firestore.FieldValue.delete(),
    periodEndEpochSeconds: normalized.periodEndEpochSeconds || admin.firestore.FieldValue.delete(),
    capabilities: normalized.capabilities,
    environment: normalized.environment || admin.firestore.FieldValue.delete(),
    originalTransactionId: normalized.originalTransactionId || admin.firestore.FieldValue.delete(),
    purchaseReference: normalized.purchaseReference || admin.firestore.FieldValue.delete(),
    updatedAtEpochMillis: Number(normalized.updatedAtEpochMillis || Date.now()) || Date.now(),
    originEventId: originEventId || normalized.originEventId || admin.firestore.FieldValue.delete(),
    version: Number.isFinite(version) && version > 0 ? Math.floor(version) : 1,
    schemaVersion: Number(normalized.schemaVersion || 2) || 2,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };
}

function normalizeEntitlementEventId(value) {
  const raw = nonEmptyString(value)?.toLowerCase() || "";
  return raw.replace(/[^a-z0-9_-]/g, "_").slice(0, 120);
}

async function loadCanonicalAiEntitlement(uid) {
  const snapshot = await admin.firestore()
      .collection("users")
      .doc(uid)
      .collection(AI_ENTITLEMENTS_SUBCOLLECTION)
      .doc(AI_ENTITLEMENT_DOCUMENT)
      .get();
  return snapshot.exists ? resolveCanonicalAiEntitlement(snapshot.data() || {}) : null;
}

async function saveCanonicalAiEntitlement(uid, state, {
  originEventId = "",
  eventType = "entitlement_sync",
  eventSource = "functions",
  externalEventId = "",
  metadata = {},
  rawRef = "",
} = {}) {
  const userRef = admin.firestore().collection("users").doc(uid);
  const entitlementRef = userRef
      .collection(AI_ENTITLEMENTS_SUBCOLLECTION)
      .doc(AI_ENTITLEMENT_DOCUMENT);
  const normalizedEventType = nonEmptyString(eventType)?.toLowerCase() || "entitlement_sync";
  const normalizedEventSource = nonEmptyString(eventSource)?.toLowerCase() || "functions";
  const dedupeId = normalizeEntitlementEventId(
      nonEmptyString(externalEventId) ||
      nonEmptyString(originEventId) ||
      `${normalizedEventSource}_${normalizedEventType}_${Date.now()}`,
  );
  const eventRef = userRef.collection(AI_ENTITLEMENT_EVENTS_SUBCOLLECTION).doc(dedupeId);
  const previousState = await loadCanonicalAiEntitlement(uid);
  const payload = entitlementPayloadFromState(state, {
    originEventId: originEventId || dedupeId,
    version: 1,
  });
  const normalizedMetadata = metadata && typeof metadata === "object" && !Array.isArray(metadata) ? metadata : {};
  let wasDuplicate = false;
  await admin.firestore().runTransaction(async (transaction) => {
    const existingEvent = await transaction.get(eventRef);
    if (existingEvent.exists) {
      wasDuplicate = true;
      return;
    }
    transaction.set(entitlementRef, payload, {merge: true});
    transaction.set(eventRef, {
      eventType: normalizedEventType,
      source: normalizedEventSource,
      provider: payload.provider || null,
      externalEventId: nonEmptyString(externalEventId) || null,
      productId: payload.productId || null,
      previousState: previousState || null,
      nextState: resolveCanonicalAiEntitlement(payload),
      rawRef: nonEmptyString(rawRef) || null,
      metadata: normalizedMetadata,
      idempotencyKey: dedupeId,
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  });
  return {
    eventId: dedupeId,
    duplicated: wasDuplicate,
  };
}

function applyEntitlementToUserData(userData = {}, entitlementState = null) {
  if (!entitlementState) {
    return userData;
  }
  return {
    ...userData,
    aiSubscriptionStatus: entitlementState.status || userData.aiSubscriptionStatus,
    aiSubscriptionPlan: entitlementState.plan || userData.aiSubscriptionPlan,
    aiSubscriptionProvider: entitlementState.provider || userData.aiSubscriptionProvider,
    aiSubscriptionSourcePlatform: entitlementState.source || userData.aiSubscriptionSourcePlatform,
    aiSubscriptionProductId: entitlementState.productId || userData.aiSubscriptionProductId,
    aiSubscriptionCurrentPeriodEndEpochSeconds:
      entitlementState.periodEndEpochSeconds || userData.aiSubscriptionCurrentPeriodEndEpochSeconds,
  };
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
    platform: nonEmptyString(metadata.platform)?.toLowerCase() || "",
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
  const storedQuotaPlan = typeof userData.quotaPlan === "string" &&
    Object.values(USER_QUOTA_PLANS).includes(userData.quotaPlan) ?
      userData.quotaPlan :
      defaultQuotaPlanForRole(role);
  const hasActivePaidSubscription = AI_SUBSCRIPTION_PLANS.includes(
      normalizeAiSubscriptionPlan(userData.aiSubscriptionPlan) || "",
  ) && isAiSubscriptionStatusActive(userData.aiSubscriptionStatus);
  const quotaPlan = role === USER_ROLES.admin &&
    storedQuotaPlan !== USER_QUOTA_PLANS.internalTeam &&
    !hasActivePaidSubscription ?
      USER_QUOTA_PLANS.internalTeam :
      storedQuotaPlan;
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

function resolveAiCostGuardrails(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    warningThresholdPercent: clampIntegerSetting(
        source.warningThresholdPercent,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.warningThresholdPercent,
        10,
        95,
    ),
    criticalThresholdPercent: clampIntegerSetting(
        source.criticalThresholdPercent,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.criticalThresholdPercent,
        20,
        99,
    ),
    hardStopPercent: clampIntegerSetting(
        source.hardStopPercent,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.hardStopPercent,
        40,
        100,
    ),
    dailyBurnCapMicros: clampIntegerSetting(
        source.dailyBurnCapMicros,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.dailyBurnCapMicros,
        100_000,
        10_000_000_000,
    ),
    enableDegradeMode: source.enableDegradeMode !== false,
    suspiciousSpikeWindowSeconds: clampIntegerSetting(
        source.suspiciousSpikeWindowSeconds,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.suspiciousSpikeWindowSeconds,
        5,
        300,
    ),
    suspiciousSpikeRequestThreshold: clampIntegerSetting(
        source.suspiciousSpikeRequestThreshold,
        DEFAULT_AI_RUNTIME_SETTINGS.costGuardrails.suspiciousSpikeRequestThreshold,
        3,
        50,
    ),
  };
}

function resolveAiPlanRouting(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  const resolveSinglePlan = (planKey) => {
    const planRaw = source[planKey] && typeof source[planKey] === "object" && !Array.isArray(source[planKey]) ?
      source[planKey] :
      DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey];
    const providers = Array.isArray(planRaw.providers) ?
      planRaw.providers.map((entry) => nonEmptyString(entry)?.toLowerCase()).filter(Boolean) :
      DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].providers;
    return {
      providers: providers.length > 0 ? providers : DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].providers,
      maxEstimatedCostMicros: clampIntegerSetting(
          planRaw.maxEstimatedCostMicros,
          DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].maxEstimatedCostMicros,
          1_000,
          20_000_000,
      ),
      allowWorkflow: planRaw.allowWorkflow === true || DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].allowWorkflow === true,
      degradeProvider: nonEmptyString(planRaw.degradeProvider)?.toLowerCase() ||
        DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].degradeProvider,
      degradeModel: nonEmptyString(planRaw.degradeModel) ||
        DEFAULT_AI_RUNTIME_SETTINGS.planRouting[planKey].degradeModel,
    };
  };
  return {
    free: resolveSinglePlan("free"),
    creator: resolveSinglePlan("creator"),
    studio: resolveSinglePlan("studio"),
  };
}

function resolveAiCostEstimates(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  const featureSource = source.byFeatureClassMicros &&
    typeof source.byFeatureClassMicros === "object" &&
    !Array.isArray(source.byFeatureClassMicros) ?
      source.byFeatureClassMicros :
      DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.byFeatureClassMicros;
  const providerSource = source.providerMultipliers &&
    typeof source.providerMultipliers === "object" &&
    !Array.isArray(source.providerMultipliers) ?
      source.providerMultipliers :
      DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.providerMultipliers;
  const modelSource = source.modelMultipliers &&
    typeof source.modelMultipliers === "object" &&
    !Array.isArray(source.modelMultipliers) ?
      source.modelMultipliers :
      DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.modelMultipliers;

  const normalizeMicrosMap = (input, fallback) => {
    const result = {};
    for (const [key, fallbackValue] of Object.entries(fallback)) {
      result[key] = clampIntegerSetting(input[key], fallbackValue, 100, 50_000_000);
    }
    return result;
  };
  const normalizeMultiplierMap = (input, fallback) => {
    const result = {};
    for (const [key, fallbackValue] of Object.entries(fallback)) {
      result[key] = clampIntegerSetting(input[key], fallbackValue, 10, 500);
    }
    return result;
  };

  return {
    byFeatureClassMicros: normalizeMicrosMap(featureSource, DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.byFeatureClassMicros),
    providerMultipliers: normalizeMultiplierMap(providerSource, DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.providerMultipliers),
    modelMultipliers: normalizeMultiplierMap(modelSource, DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.modelMultipliers),
  };
}

function resolveMembershipHygieneSettings(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  const strictness = nonEmptyString(source.similarityStrictness)?.toLowerCase();
  const similarityStrictness = ["strict", "balanced", "loose"].includes(strictness) ?
    strictness :
    DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.similarityStrictness;
  return {
    cooldownDaysCompleted: clampIntegerSetting(
        source.cooldownDaysCompleted,
        DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.cooldownDaysCompleted,
        1,
        90,
    ),
    cooldownDaysRejected: clampIntegerSetting(
        source.cooldownDaysRejected,
        DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.cooldownDaysRejected,
        1,
        120,
    ),
    cooldownDaysProposed: clampIntegerSetting(
        source.cooldownDaysProposed,
        DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.cooldownDaysProposed,
        1,
        45,
    ),
    similarityStrictness,
    recurringPenalty: Math.max(
        0,
        Math.min(
            0.9,
            Number.isFinite(Number(source.recurringPenalty)) ?
              Number(source.recurringPenalty) :
              DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.recurringPenalty,
        ),
    ),
    freshnessFloor: Math.max(
        0.05,
        Math.min(
            0.9,
            Number.isFinite(Number(source.freshnessFloor)) ?
              Number(source.freshnessFloor) :
              DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.freshnessFloor,
        ),
    ),
    duplicateMergeWindowDays: clampIntegerSetting(
        source.duplicateMergeWindowDays,
        DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene.duplicateMergeWindowDays,
        1,
        60,
    ),
  };
}

function membershipHygieneProfile(hygiene = {}) {
  const strictness = nonEmptyString(hygiene.similarityStrictness)?.toLowerCase() || "balanced";
  const totalCooldown =
    (Number(hygiene.cooldownDaysCompleted) || 0) +
    (Number(hygiene.cooldownDaysRejected) || 0) +
    (Number(hygiene.cooldownDaysProposed) || 0);
  const recurringPenalty = Number(hygiene.recurringPenalty) || 0;
  const freshnessFloor = Number(hygiene.freshnessFloor) || 0;
  if (strictness === "strict" || totalCooldown >= 34 || recurringPenalty >= 0.35 || freshnessFloor >= 0.4) {
    return "conservative";
  }
  if (strictness === "loose" || totalCooldown <= 14 || recurringPenalty <= 0.1) {
    return "aggressive";
  }
  return "balanced";
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
    costGuardrails: resolveAiCostGuardrails(data.costGuardrails),
    planRouting: resolveAiPlanRouting(data.planRouting),
    costEstimates: resolveAiCostEstimates(data.costEstimates),
    membershipHygiene: resolveMembershipHygieneSettings(data.membershipHygiene),
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

  let userData = await loadUserData(auth.uid);
  if (!userData) {
    // Repair missing bootstrap docs lazily so signed-in users are not blocked forever.
    try {
      const authUser = await admin.auth().getUser(auth.uid);
      await ensureAuthUserBootstrapDocument(authUser);
      userData = await loadUserData(auth.uid);
    } catch (error) {
      logger.warn("AI access bootstrap fallback failed.", {
        uid: auth.uid,
        error: error instanceof Error ? error.message : `${error}`,
      });
    }
  }
  if (!userData) {
    throw new HttpsError("permission-denied", "Dein Konto ist noch nicht vollstaendig eingerichtet.");
  }

  const canonicalEntitlement = await loadCanonicalAiEntitlement(auth.uid);
  const entitlementState = canonicalEntitlement || resolveLegacyAiEntitlement(userData);
  if (!canonicalEntitlement) {
    await saveCanonicalAiEntitlement(auth.uid, entitlementState, {
      eventType: "legacy_backfill",
      eventSource: "assert_ai_access",
    });
  }
  const effectiveUserData = applyEntitlementToUserData(userData, entitlementState);
  const profile = buildUserProfile(effectiveUserData);
  const featureConfig = await loadAiFeatureConfig();

  if (!featureConfig.isEnabled || featureConfig.accessMode === AI_ACCESS_MODES.off) {
    throw new HttpsError("permission-denied", "Die KI ist gerade pausiert.");
  }

  if (!profile.aiAccessEnabled) {
    throw new HttpsError("permission-denied", "Die KI ist fuer dein Konto gerade pausiert.");
  }

  if (
    profile.role === USER_ROLES.admin &&
    profile.aiLimits.quotaPlan !== USER_QUOTA_PLANS.internalTeam &&
    (
      !AI_SUBSCRIPTION_PLANS.includes(profile.aiLimits.quotaPlan) ||
      !isAiSubscriptionStatusActive(effectiveUserData.aiSubscriptionStatus)
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
    userData: effectiveUserData,
    profile,
    featureConfig,
    effectiveEntitlement: resolveCanonicalAiEntitlement(entitlementState),
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
  await firestore.doc(`adminConfig/agentProfile_${uid}`).delete();

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

function aiUsageMonthKey(dateKey = aiUsageDateKey()) {
  return dateKey.slice(0, 7);
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

function aiUsageMonthlyMetricsDocumentId(monthKey) {
  return `${AI_USAGE_MONTHLY_METRICS_DOCUMENT_PREFIX}${monthKey}`;
}

function aiDailyBurnDocumentId(dateKey) {
  return `${AI_GUARDRAILS_DAILY_BURN_PREFIX}${dateKey}`;
}

function aiMetricKey(value, fallback = "unknown") {
  const normalized = nonEmptyString(value)?.toLowerCase() || fallback;
  return normalized.replace(/[^a-z0-9_-]/g, "_").slice(0, 48);
}

function resolveFeatureClass(kind, explicitFeatureClass = "") {
  const normalized = nonEmptyString(explicitFeatureClass);
  if (normalized && Object.values(AI_FEATURE_CLASSES).includes(normalized)) {
    return normalized;
  }
  if (kind === AI_USAGE_KINDS.visual) {
    return AI_FEATURE_CLASSES.image;
  }
  if (kind === AI_USAGE_KINDS.agent) {
    return AI_FEATURE_CLASSES.agent;
  }
  return AI_FEATURE_CLASSES.text;
}

function resolvePlanRoutingPolicy(runtimeSettings, plan) {
  const normalizedPlan = normalizeAiSubscriptionPlan(plan);
  if (normalizedPlan === USER_QUOTA_PLANS.studio) {
    return runtimeSettings.planRouting.studio;
  }
  if (normalizedPlan === USER_QUOTA_PLANS.creator) {
    return runtimeSettings.planRouting.creator;
  }
  return runtimeSettings.planRouting.free;
}

function featureAllowedByCapability(capabilities, featureClass) {
  const safe = capabilities && typeof capabilities === "object" && !Array.isArray(capabilities) ? capabilities : {};
  switch (featureClass) {
    case AI_FEATURE_CLASSES.image:
      return safe.botImage !== false;
    case AI_FEATURE_CLASSES.agent:
      return safe.agentStandard !== false;
    case AI_FEATURE_CLASSES.workflow:
      return safe.workflowAutomation === true;
    case AI_FEATURE_CLASSES.videoFuture:
      return safe.premiumOutputs === true;
    case AI_FEATURE_CLASSES.text:
    default:
      return safe.botText !== false;
  }
}

function buildGuardrailHints({
  denyReason = "",
  remainingForKind = null,
  limitForKind = null,
  warningLevel = "ok",
  allowDegrade = false,
  plan = "",
}) {
  const normalizedPlan = normalizeAiSubscriptionPlan(plan);
  const suggestedUpgrade = normalizedPlan === USER_QUOTA_PLANS.creator ?
    "creator_power" :
    normalizedPlan === USER_QUOTA_PLANS.studio ?
      null :
      "pro";
  const resetHint = "Dein Tageskontingent wird um Mitternacht UTC aktualisiert.";
  const retryAfter = denyReason === "suspicious_spike" ? 120 : 30;
  let userFacingReason = "";
  if (denyReason === "plan_feature_not_allowed") {
    userFacingReason = "Dein aktueller Plan deckt diese Funktion noch nicht ab.";
  } else if (denyReason === "plan_provider_not_allowed") {
    userFacingReason = "Diese Modellklasse ist in deinem Plan nicht freigeschaltet.";
  } else if (denyReason === "plan_cost_cap_exceeded") {
    userFacingReason = "Diese Anfrage ist gerade zu rechenintensiv fuer dein Plan-Budget.";
  } else if (denyReason === "daily_burn_cap_reached") {
    userFacingReason = "SkyOS reduziert gerade Lastspitzen, um Stabilitaet und Fairness zu sichern.";
  } else if (denyReason === "suspicious_spike") {
    userFacingReason = "Wir sehen gerade ungewoehnlich viele Requests in kurzer Zeit.";
  } else if (denyReason === "hard_limit_reached") {
    userFacingReason = "Dein Tageslimit fuer diese Funktion ist erreicht.";
  } else {
    userFacingReason = "Diese Anfrage kann gerade nicht freigegeben werden.";
  }
  return {
    userFacingReason,
    retryAfterSeconds: retryAfter,
    resetHint,
    suggestedUpgrade,
    lowerCostOption: allowDegrade ?
      "Nutze den ruhigen Standardmodus fuer eine kostenleichtere Antwort." :
      null,
    warningLevel,
    remainingForKind,
    limitForKind,
  };
}

function resolveEstimatedCostMicros({
  runtimeSettings,
  featureClass,
  provider,
  model,
  requestWeight,
  estimatedCostMicros,
}) {
  const explicit = Number(estimatedCostMicros);
  if (Number.isFinite(explicit) && explicit >= 0) {
    return Math.floor(explicit);
  }
  const estimates = runtimeSettings?.costEstimates || DEFAULT_AI_RUNTIME_SETTINGS.costEstimates;
  const base = Number(estimates.byFeatureClassMicros?.[featureClass]) ||
    Number(DEFAULT_AI_RUNTIME_SETTINGS.costEstimates.byFeatureClassMicros[featureClass]) ||
    10000;
  const providerKey = aiMetricKey(provider, "google_vertex");
  const modelKey = aiMetricKey(model || "", "");
  const providerMultiplier = Number(estimates.providerMultipliers?.[providerKey]) || 100;
  const modelMultiplier = Number(estimates.modelMultipliers?.[modelKey]) || 100;
  const weight = Number.isFinite(Number(requestWeight)) ? Math.max(1, Math.floor(Number(requestWeight))) : 1;
  return Math.max(0, Math.floor(base * (providerMultiplier / 100) * (modelMultiplier / 100) * weight));
}

function buildUsageEventV2(payload = {}) {
  const dateKey = nonEmptyString(payload.dateKey) || aiUsageDateKey();
  const monthKey = nonEmptyString(payload.monthKey) || aiUsageMonthKey(dateKey);
  const estimatedMicros = Number.isFinite(Number(payload.estimatedCostMicros)) ?
    Math.max(0, Math.floor(Number(payload.estimatedCostMicros))) :
    0;
  const actualMicros = Number.isFinite(Number(payload.actualCostMicros)) ?
    Math.max(0, Math.floor(Number(payload.actualCostMicros))) :
    null;
  const varianceMicros = actualMicros == null ? null : (actualMicros - estimatedMicros);
  const varianceRatio = actualMicros == null || estimatedMicros <= 0 ?
    null :
    Number((actualMicros / estimatedMicros).toFixed(4));
  return {
    uid: nonEmptyString(payload.uid) || "",
    plan: normalizeAiSubscriptionPlan(payload.plan) || USER_QUOTA_PLANS.free,
    entitlementStatus: normalizeAiSubscriptionStatus(payload.entitlementStatus, "inactive"),
    eventType: nonEmptyString(payload.eventType) || "ai_request",
    featureClass: resolveFeatureClass(payload.kind, payload.featureClass),
    provider: nonEmptyString(payload.provider) || "unknown",
    model: nonEmptyString(payload.model) || "",
    requestWeight: Number.isFinite(Number(payload.requestWeight)) ?
      Math.max(1, Math.floor(Number(payload.requestWeight))) :
      1,
    estimatedCostMicros: estimatedMicros,
    actualCostMicros: actualMicros,
    costStatus: nonEmptyString(payload.costStatus) || (actualMicros == null ? "estimated" : "reconciled"),
    reconciliationSource: nonEmptyString(payload.reconciliationSource) || "",
    reconciledAt: payload.reconciledAt || null,
    varianceMicros,
    varianceRatio,
    success: payload.success === true,
    denied: payload.success === true ? false : true,
    denyReason: nonEmptyString(payload.denyReason) || "",
    sourceRoute: nonEmptyString(payload.sourceRoute) || "ai",
    functionName: nonEmptyString(payload.functionName) || "unknown",
    resultType: nonEmptyString(payload.resultType) || "text",
    requestId: nonEmptyString(payload.requestId) || `req_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`,
    dateKey,
    monthKey,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdAtEpochMillis: Date.now(),
  };
}

async function recordUsageEventV2(payload = {}) {
  const event = buildUsageEventV2(payload);
  await admin.firestore()
      .collection(AI_USAGE_EVENTS_COLLECTION)
      .doc(event.requestId)
      .set(event, {merge: true});
  return event;
}

function parseDateKeyToEpochMillis(dateKey) {
  const normalized = nonEmptyString(dateKey);
  if (!normalized || !/^\d{4}-\d{2}-\d{2}$/.test(normalized)) {
    return 0;
  }
  const parsed = Date.parse(`${normalized}T00:00:00.000Z`);
  return Number.isFinite(parsed) ? parsed : 0;
}

function collectTopEntries(map, limit = 5) {
  const source = map && typeof map === "object" && !Array.isArray(map) ? map : {};
  return Object.entries(source)
      .map(([key, value]) => ({key, value: Number(value) || 0}))
      .sort((a, b) => b.value - a.value)
      .slice(0, limit);
}

function normalizeMembershipEventName(value) {
  const normalized = nonEmptyString(value)?.toLowerCase() || "";
  return AI_MEMBERSHIP_EVENT_TYPES.includes(normalized) ? normalized : "";
}

function normalizeMembershipSurface(value) {
  return aiMetricKey(nonEmptyString(value) || "unknown_surface", "unknown_surface");
}

function normalizeMembershipReason(value) {
  return aiMetricKey(nonEmptyString(value) || "unknown_reason", "unknown_reason");
}

function normalizeMembershipPlan(value) {
  const normalized = normalizeAiSubscriptionPlan(value);
  if (normalized) {
    return normalized;
  }
  if (nonEmptyString(value)?.toLowerCase() === "pro") {
    return USER_QUOTA_PLANS.creator;
  }
  if (nonEmptyString(value)?.toLowerCase() === "creator") {
    return USER_QUOTA_PLANS.studio;
  }
  return USER_QUOTA_PLANS.free;
}

function asBoolean(value, fallback = false) {
  if (typeof value === "boolean") return value;
  if (typeof value === "string") {
    const normalized = value.trim().toLowerCase();
    if (normalized === "true" || normalized === "1" || normalized === "yes") return true;
    if (normalized === "false" || normalized === "0" || normalized === "no") return false;
  }
  if (typeof value === "number") {
    return value > 0;
  }
  return fallback;
}

function rollingWindowRange(days) {
  const normalizedDays = Math.max(1, Math.floor(Number(days) || 1));
  const endMs = Date.now();
  const startMs = endMs - (normalizedDays * 24 * 60 * 60 * 1000);
  return {
    days: normalizedDays,
    startMs,
    endMs,
  };
}

function initMembershipCounters() {
  return {
    membershipOpens: 0,
    purchaseSuccess: 0,
    purchaseStarted: 0,
    purchaseCancelled: 0,
    restoreSuccess: 0,
    upgradeAfterWarning: 0,
    upgradeAfterDeny: 0,
    byReasonOpen: {},
    byReasonPurchase: {},
    bySurfaceOpen: {},
    bySurfacePurchase: {},
    planMix: {},
    annualPurchases: 0,
    creatorPurchases: 0,
    eventsByType: {},
  };
}

function ingestMembershipEvent(counters, event) {
  const type = normalizeMembershipEventName(event.eventName);
  if (!type) return;
  const reason = normalizeMembershipReason(event.reason);
  const surface = normalizeMembershipSurface(event.surface);
  const plan = normalizeMembershipPlan(event.plan || event.currentPlan);
  const annual = asBoolean(event.annual, false);
  counters.eventsByType[type] = (Number(counters.eventsByType[type]) || 0) + 1;

  if (type === "membership_open") {
    counters.membershipOpens += 1;
    counters.byReasonOpen[reason] = (Number(counters.byReasonOpen[reason]) || 0) + 1;
    counters.bySurfaceOpen[surface] = (Number(counters.bySurfaceOpen[surface]) || 0) + 1;
  } else if (type === "purchase_started") {
    counters.purchaseStarted += 1;
  } else if (type === "purchase_success") {
    counters.purchaseSuccess += 1;
    counters.byReasonPurchase[reason] = (Number(counters.byReasonPurchase[reason]) || 0) + 1;
    counters.bySurfacePurchase[surface] = (Number(counters.bySurfacePurchase[surface]) || 0) + 1;
    const planKey = aiMetricKey(plan);
    counters.planMix[planKey] = (Number(counters.planMix[planKey]) || 0) + 1;
    if (annual) counters.annualPurchases += 1;
    if (plan === USER_QUOTA_PLANS.studio) counters.creatorPurchases += 1;
  } else if (type === "purchase_cancelled") {
    counters.purchaseCancelled += 1;
  } else if (type === "restore_success") {
    counters.restoreSuccess += 1;
  } else if (type === "upgrade_after_warning") {
    counters.upgradeAfterWarning += 1;
  } else if (type === "upgrade_after_deny") {
    counters.upgradeAfterDeny += 1;
  }
}

function mapCvrByDimension(openMap, purchaseMap) {
  const keys = new Set([...Object.keys(openMap || {}), ...Object.keys(purchaseMap || {})]);
  return Array.from(keys).sort().map((key) => {
    const opens = Number(openMap[key]) || 0;
    const purchases = Number(purchaseMap[key]) || 0;
    return {
      key,
      opens,
      purchases,
      cvr: opens > 0 ? Number((purchases / opens).toFixed(4)) : 0,
    };
  });
}

function summarizeMembershipWindow(range, docs) {
  const counters = initMembershipCounters();
  for (const doc of docs) {
    const data = doc.data() || {};
    const createdAtMs = Number(data.createdAtEpochMillis || 0);
    if (!Number.isFinite(createdAtMs) || createdAtMs < range.startMs || createdAtMs > range.endMs) {
      continue;
    }
    ingestMembershipEvent(counters, data);
  }
  const opens = counters.membershipOpens;
  const success = counters.purchaseSuccess;
  const started = counters.purchaseStarted;
  const warningEvents = counters.upgradeAfterWarning;
  const denyEvents = counters.upgradeAfterDeny;
  return {
    rangeDays: range.days,
    membershipOpens: opens,
    purchaseSuccess: success,
    cvr: opens > 0 ? Number((success / opens).toFixed(4)) : 0,
    byReasonCvr: mapCvrByDimension(counters.byReasonOpen, counters.byReasonPurchase),
    bySurfaceCvr: mapCvrByDimension(counters.bySurfaceOpen, counters.bySurfacePurchase),
    planMix: counters.planMix,
    annualShare: success > 0 ? Number((counters.annualPurchases / success).toFixed(4)) : 0,
    creatorTakeRate: success > 0 ? Number((counters.creatorPurchases / success).toFixed(4)) : 0,
    cancelRate: started > 0 ? Number((counters.purchaseCancelled / started).toFixed(4)) : 0,
    restoreRate: opens > 0 ? Number((counters.restoreSuccess / opens).toFixed(4)) : 0,
    purchaseStartedToSuccessDropoff: started > 0 ? Number((1 - (success / started)).toFixed(4)) : null,
    upgradeAfterWarningCvr: warningEvents > 0 ? Number((success / warningEvents).toFixed(4)) : 0,
    upgradeAfterDenyCvr: denyEvents > 0 ? Number((success / denyEvents).toFixed(4)) : 0,
    rawCounts: {
      purchaseStarted: started,
      purchaseCancelled: counters.purchaseCancelled,
      restoreSuccess: counters.restoreSuccess,
      upgradeAfterWarning: warningEvents,
      upgradeAfterDeny: denyEvents,
      eventsByType: counters.eventsByType,
    },
  };
}

function utcDateKeyFromEpochMillis(epochMillis) {
  const value = Number(epochMillis);
  if (!Number.isFinite(value) || value <= 0) {
    return "";
  }
  return new Date(value).toISOString().slice(0, 10);
}

function emptyMembershipDayPoint(dateKey) {
  return {
    dateKey,
    opens: 0,
    purchases: 0,
    cvr: 0,
    annualShare: 0,
    creatorTakeRate: 0,
    cancels: 0,
    restores: 0,
    upgradeAfterWarning: 0,
    upgradeAfterDeny: 0,
  };
}

function buildMembershipDailySeries(days, docs) {
  const range = rollingWindowRange(days);
  const map = {};
  for (const doc of docs) {
    const data = doc.data() || {};
    const createdAtMs = Number(data.createdAtEpochMillis || 0);
    if (!Number.isFinite(createdAtMs) || createdAtMs < range.startMs || createdAtMs > range.endMs) {
      continue;
    }
    const dateKey = utcDateKeyFromEpochMillis(createdAtMs);
    if (!dateKey) continue;
    if (!map[dateKey]) {
      map[dateKey] = {
        dateKey,
        opens: 0,
        purchases: 0,
        annualPurchases: 0,
        creatorPurchases: 0,
        cancels: 0,
        restores: 0,
        upgradeAfterWarning: 0,
        upgradeAfterDeny: 0,
      };
    }
    const eventName = normalizeMembershipEventName(data.eventName);
    const plan = normalizeMembershipPlan(data.plan || data.currentPlan);
    const annual = asBoolean(data.annual, false);
    if (eventName === "membership_open") {
      map[dateKey].opens += 1;
    } else if (eventName === "purchase_success") {
      map[dateKey].purchases += 1;
      if (annual) map[dateKey].annualPurchases += 1;
      if (plan === USER_QUOTA_PLANS.studio) map[dateKey].creatorPurchases += 1;
    } else if (eventName === "purchase_cancelled") {
      map[dateKey].cancels += 1;
    } else if (eventName === "restore_success") {
      map[dateKey].restores += 1;
    } else if (eventName === "upgrade_after_warning") {
      map[dateKey].upgradeAfterWarning += 1;
    } else if (eventName === "upgrade_after_deny") {
      map[dateKey].upgradeAfterDeny += 1;
    }
  }

  const points = [];
  for (let i = days - 1; i >= 0; i -= 1) {
    const ts = Date.now() - (i * 24 * 60 * 60 * 1000);
    const dateKey = utcDateKeyFromEpochMillis(ts);
    const raw = map[dateKey] || null;
    if (!raw) {
      points.push(emptyMembershipDayPoint(dateKey));
      continue;
    }
    const purchases = Number(raw.purchases) || 0;
    const opens = Number(raw.opens) || 0;
    points.push({
      dateKey,
      opens,
      purchases,
      cvr: opens > 0 ? Number((purchases / opens).toFixed(4)) : 0,
      annualShare: purchases > 0 ? Number((raw.annualPurchases / purchases).toFixed(4)) : 0,
      creatorTakeRate: purchases > 0 ? Number((raw.creatorPurchases / purchases).toFixed(4)) : 0,
      cancels: Number(raw.cancels) || 0,
      restores: Number(raw.restores) || 0,
      upgradeAfterWarning: Number(raw.upgradeAfterWarning) || 0,
      upgradeAfterDeny: Number(raw.upgradeAfterDeny) || 0,
    });
  }
  return points;
}

function average(list) {
  if (!Array.isArray(list) || list.length === 0) return 0;
  const total = list.reduce((sum, value) => sum + (Number(value) || 0), 0);
  return total / list.length;
}

function classifyTrend(points, key, neutralDelta = 0.01) {
  if (!Array.isArray(points) || points.length < 4) return "flat";
  const midpoint = Math.floor(points.length / 2);
  const firstAvg = average(points.slice(0, midpoint).map((entry) => Number(entry[key]) || 0));
  const secondAvg = average(points.slice(midpoint).map((entry) => Number(entry[key]) || 0));
  const delta = secondAvg - firstAvg;
  if (delta > neutralDelta) return "improving";
  if (delta < -neutralDelta) return "declining";
  return "flat";
}

function detectCancelSpikeDates(points) {
  if (!Array.isArray(points) || points.length === 0) return [];
  const values = points.map((entry) => Number(entry.cancels) || 0);
  const baseline = average(values);
  if (baseline <= 0) return [];
  return points
      .filter((entry) => (Number(entry.cancels) || 0) >= Math.max(3, baseline * 2))
      .map((entry) => ({dateKey: entry.dateKey, cancels: Number(entry.cancels) || 0}));
}

function compareWindowSummary(current, previous) {
  const safePreviousOpens = Math.max(Number(previous.membershipOpens) || 0, 1);
  const safePreviousPurchases = Math.max(Number(previous.purchaseSuccess) || 0, 1);
  return {
    opensDeltaAbs: (Number(current.membershipOpens) || 0) - (Number(previous.membershipOpens) || 0),
    opensDeltaRatio: Number((((Number(current.membershipOpens) || 0) - (Number(previous.membershipOpens) || 0)) / safePreviousOpens).toFixed(4)),
    purchaseDeltaAbs: (Number(current.purchaseSuccess) || 0) - (Number(previous.purchaseSuccess) || 0),
    purchaseDeltaRatio: Number((((Number(current.purchaseSuccess) || 0) - (Number(previous.purchaseSuccess) || 0)) / safePreviousPurchases).toFixed(4)),
    cvrDelta: Number(((Number(current.cvr) || 0) - (Number(previous.cvr) || 0)).toFixed(4)),
    annualShareDelta: Number(((Number(current.annualShare) || 0) - (Number(previous.annualShare) || 0)).toFixed(4)),
    creatorTakeRateDelta: Number(((Number(current.creatorTakeRate) || 0) - (Number(previous.creatorTakeRate) || 0)).toFixed(4)),
    cancelRateDelta: Number(((Number(current.cancelRate) || 0) - (Number(previous.cancelRate) || 0)).toFixed(4)),
  };
}

function buildTimeseriesTrendSignals(series30, summary7, previousSummary7) {
  const cvrTrend = classifyTrend(series30, "cvr", 0.005);
  const creatorTrend = classifyTrend(series30, "creatorTakeRate", 0.01);
  const annualTrend = classifyTrend(series30, "annualShare", 0.01);
  const cancelSpikeDates = detectCancelSpikeDates(series30);
  const opensTrend = classifyTrend(series30, "opens", 0.5);
  const purchasesTrend = classifyTrend(series30, "purchases", 0.3);
  const signals = {
    cvr: cvrTrend,
    creator: creatorTrend === "improving" ? "rising" : creatorTrend === "declining" ? "falling" : "flat",
    annualMomentum: annualTrend,
    cancelSpikeDates,
    triggerFatigue: summary7.upgradeAfterWarningCvr < 0.2 &&
      summary7.rawCounts.upgradeAfterWarning >= 6 ? "suspected" : "none",
    opensRisingPurchasesFlat: opensTrend === "improving" &&
      (purchasesTrend === "flat" || purchasesTrend === "declining"),
    cvrWindowDirection: (Number(summary7.cvr) || 0) > (Number(previousSummary7.cvr) || 0) ? "improving" :
      (Number(summary7.cvr) || 0) < (Number(previousSummary7.cvr) || 0) ? "declining" : "flat",
  };
  return signals;
}

function recommendationConfidence(base = 0.5, modifiers = []) {
  let value = Number(base) || 0.5;
  for (const modifier of modifiers) {
    value += Number(modifier) || 0;
  }
  return Math.max(0.05, Math.min(0.99, Number(value.toFixed(2))));
}

function recommendationSeverityFromImpact(level = "medium") {
  if (level === "high") return "high";
  if (level === "low") return "low";
  return "medium";
}

function pushRecommendation(list, payload) {
  list.push({
    id: payload.id,
    title: payload.title,
    summary: payload.summary,
    recommendationType: payload.recommendationType,
    confidenceScore: payload.confidenceScore,
    severity: payload.severity,
    likelyCause: payload.likelyCause,
    suggestedAction: payload.suggestedAction,
    supportingSignals: Array.isArray(payload.supportingSignals) ? payload.supportingSignals : [],
    affectedPlans: Array.isArray(payload.affectedPlans) ? payload.affectedPlans : [],
    affectedSurfaces: Array.isArray(payload.affectedSurfaces) ? payload.affectedSurfaces : [],
    generatedAt: Date.now(),
  });
}

function proposedLifecycleIdForRecommendation(recommendationId) {
  return `proposed_${aiMetricKey(recommendationId, "recommendation")}`;
}

async function appendMembershipTimelineEvent({
  recommendation,
  generatedAtEpochMillis = Date.now(),
}) {
  const recommendationId = nonEmptyString(recommendation?.id);
  if (!recommendationId) return null;
  const lifecycleId = proposedLifecycleIdForRecommendation(recommendationId);
  const ref = admin.firestore()
      .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
      .doc(lifecycleId);
  const snapshot = await ref.get();
  const existing = snapshot.data() || {};
  const existingStatus = normalizeRecommendationStatus(existing.status, "proposed");
  if (existingStatus === "active" || existingStatus === "completed" || existingStatus === "rejected") {
    return {
      lifecycleId,
      status: existingStatus,
      skipped: true,
      reason: "lifecycle_already_progressed",
    };
  }

  const expectedImpact = recommendation.expectedImpact && typeof recommendation.expectedImpact === "object" ?
    recommendation.expectedImpact :
    {
      cvrRange: {min: 0, max: 0},
      annualShareRange: {min: 0, max: 0},
      creatorTakeRateRange: {min: 0, max: 0},
      cancelReductionRange: {min: 0, max: 0},
    };
  const confidenceScore = Number.isFinite(Number(recommendation.confidenceScore)) ?
    Math.max(0, Math.min(1, Number(recommendation.confidenceScore))) :
    0;
  const severity = nonEmptyString(recommendation.severity)?.toLowerCase() || "medium";
  const recommendationType = aiMetricKey(nonEmptyString(recommendation.recommendationType) || "unknown");
  const affectedPlans = Array.isArray(recommendation.affectedPlans) ?
    recommendation.affectedPlans.map((entry) => normalizeMembershipPlan(entry)).filter(Boolean) :
    [];
  const affectedSurfaces = Array.isArray(recommendation.affectedSurfaces) ?
    recommendation.affectedSurfaces.map((entry) => normalizeMembershipSurface(entry)).filter(Boolean) :
    [];
  const payload = {
    recommendationId,
    recommendationType,
    startedAt: null,
    startedAtEpochMillis: null,
    completedAt: null,
    completedAtEpochMillis: null,
    status: "proposed",
    expectedImpact,
    actualImpact: null,
    confidenceAtStart: confidenceScore,
    simulationAccuracy: null,
    notes: nonEmptyString(recommendation.summary) || "",
    ownerAction: "recommendation_proposed",
    generatedAt: generatedAtEpochMillis,
    severity,
    title: nonEmptyString(recommendation.title) || recommendationTitleFromId(recommendationId),
    summary: nonEmptyString(recommendation.summary) || "",
    affectedPlans,
    affectedSurfaces,
    idempotencyKey: `recommendation_proposed:${recommendationId}`,
    proposalUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    proposalUpdatedAtEpochMillis: generatedAtEpochMillis,
  };

  await ref.set(payload, {merge: true});
  return {
    lifecycleId,
    status: "proposed",
    skipped: false,
  };
}

async function appendProposedRecommendationsToTimeline(recommendations, generatedAtEpochMillis = Date.now()) {
  const safeRecommendations = Array.isArray(recommendations) ? recommendations : [];
  if (safeRecommendations.length === 0) return [];
  const tasks = safeRecommendations.map((recommendation) => appendMembershipTimelineEvent({
    recommendation,
    generatedAtEpochMillis,
  }));
  return Promise.all(tasks);
}

function recommendationFingerprint(recommendation) {
  const recommendationType = aiMetricKey(nonEmptyString(recommendation?.recommendationType) || "unknown");
  const plans = Array.isArray(recommendation?.affectedPlans) ? recommendation.affectedPlans.map((entry) => aiMetricKey(entry)).sort().join(",") : "";
  const surfaces = Array.isArray(recommendation?.affectedSurfaces) ? recommendation.affectedSurfaces.map((entry) => aiMetricKey(entry)).sort().join(",") : "";
  return aiMetricKey(`${recommendationType}:${plans}:${surfaces}`, "unknown_fingerprint");
}

function lifecycleTimestamp(row) {
  return Number(row?.completedAtEpochMillis) ||
    Number(row?.startedAtEpochMillis) ||
    Number(row?.proposalUpdatedAtEpochMillis) ||
    Number(row?.generatedAt) ||
    0;
}

function buildRecommendationLifecycleIndex(rows = []) {
  const byRecommendationId = {};
  const byFingerprint = {};
  for (const row of rows) {
    const recommendationId = nonEmptyString(row.recommendationId);
    if (recommendationId) {
      if (!byRecommendationId[recommendationId]) byRecommendationId[recommendationId] = [];
      byRecommendationId[recommendationId].push(row);
    }
    const fingerprint = recommendationFingerprint({
      recommendationType: row.recommendationType,
      affectedPlans: row.affectedPlans || [],
      affectedSurfaces: row.affectedSurfaces || [],
    });
    if (!byFingerprint[fingerprint]) byFingerprint[fingerprint] = [];
    byFingerprint[fingerprint].push(row);
  }
  return {byRecommendationId, byFingerprint};
}

function applyRecommendationHygiene({
  recommendations = [],
  lifecycleRows = [],
  hygieneConfig = DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene,
  nowMs = Date.now(),
}) {
  const index = buildRecommendationLifecycleIndex(lifecycleRows);
  const completedCooldownMs = Math.max(1, Number(hygieneConfig.cooldownDaysCompleted) || 10) * 24 * 60 * 60 * 1000;
  const rejectedCooldownMs = Math.max(1, Number(hygieneConfig.cooldownDaysRejected) || 21) * 24 * 60 * 60 * 1000;
  const openCooldownMs = Math.max(1, Number(hygieneConfig.cooldownDaysProposed) || 7) * 24 * 60 * 60 * 1000;
  const duplicateMergeWindowMs = Math.max(1, Number(hygieneConfig.duplicateMergeWindowDays) || 14) * 24 * 60 * 60 * 1000;
  const recurringPenalty = Math.max(0, Math.min(0.9, Number(hygieneConfig.recurringPenalty) || 0.18));
  const freshnessFloor = Math.max(0.05, Math.min(0.9, Number(hygieneConfig.freshnessFloor) || 0.2));
  const strictness = nonEmptyString(hygieneConfig.similarityStrictness)?.toLowerCase() || "balanced";
  const strictDuplicate = strictness === "strict";
  const looseDuplicate = strictness === "loose";

  return recommendations.map((recommendation) => {
    const recommendationId = nonEmptyString(recommendation.id) || "";
    const fingerprint = recommendationFingerprint(recommendation);
    const directRows = [...(index.byRecommendationId[recommendationId] || [])]
        .sort((a, b) => lifecycleTimestamp(b) - lifecycleTimestamp(a));
    const similarRows = [...(index.byFingerprint[fingerprint] || [])]
        .sort((a, b) => lifecycleTimestamp(b) - lifecycleTimestamp(a));
    const recurringCount = directRows.length;
    const latestDirect = directRows[0] || null;
    const latestSimilar = similarRows[0] || null;

    let suppressed = false;
    let duplicateOf = "";
    let reason = "fresh";
    let cooldownUntil = null;
    let freshnessScore = 1;

    if (latestDirect) {
      const status = normalizeRecommendationStatus(latestDirect.status, "proposed");
      const ts = lifecycleTimestamp(latestDirect);
      if (status === "active" || status === "proposed") {
        suppressed = true;
        duplicateOf = nonEmptyString(latestDirect.recommendationId) || recommendationId;
        reason = status === "active" ? "already_active" : "already_proposed";
        cooldownUntil = ts > 0 ? ts + openCooldownMs : null;
        freshnessScore = 0.08;
      } else if (status === "completed" && ts > 0 && (nowMs - ts) < completedCooldownMs) {
        suppressed = true;
        duplicateOf = nonEmptyString(latestDirect.recommendationId) || recommendationId;
        reason = "completed_recently";
        cooldownUntil = ts + completedCooldownMs;
        freshnessScore = 0.2;
      } else if (status === "rejected" && ts > 0 && (nowMs - ts) < rejectedCooldownMs) {
        suppressed = true;
        duplicateOf = nonEmptyString(latestDirect.recommendationId) || recommendationId;
        reason = "rejected_recently";
        cooldownUntil = ts + rejectedCooldownMs;
        freshnessScore = 0.12;
      }
    }

    if (!suppressed && latestSimilar) {
      const similarStatus = normalizeRecommendationStatus(latestSimilar.status, "proposed");
      const ts = lifecycleTimestamp(latestSimilar);
      const similarRecommendationId = nonEmptyString(latestSimilar.recommendationId) || "";
      const insideMergeWindow = ts > 0 ? (nowMs - ts) <= duplicateMergeWindowMs : true;
      if (insideMergeWindow &&
        similarRecommendationId &&
        similarRecommendationId !== recommendationId &&
        (similarStatus === "active" || similarStatus === "proposed")) {
        suppressed = true;
        duplicateOf = similarRecommendationId;
        reason = "duplicate_similar_open";
        cooldownUntil = ts > 0 ? ts + openCooldownMs : null;
        freshnessScore = 0.1;
      } else if (!strictDuplicate && similarStatus === "completed" && ts > 0 && (nowMs - ts) < completedCooldownMs) {
        reason = "similar_completed_recently";
        cooldownUntil = ts + completedCooldownMs;
        freshnessScore = looseDuplicate ? 0.6 : 0.45;
      } else if (!strictDuplicate && similarStatus === "rejected" && ts > 0 && (nowMs - ts) < rejectedCooldownMs) {
        reason = "similar_rejected_recently";
        cooldownUntil = ts + rejectedCooldownMs;
        freshnessScore = looseDuplicate ? 0.5 : 0.35;
      }
    }

    if (recurringCount >= 3 && !suppressed) {
      freshnessScore = Math.min(freshnessScore, 1 - recurringPenalty);
      reason = reason === "fresh" ? "recurring_noise" : reason;
    }
    if (recurringCount >= 5 && !suppressed) {
      freshnessScore = Math.min(freshnessScore, Math.max(0.15, 1 - (recurringPenalty * 2)));
    }
    if (suppressed) {
      freshnessScore = Math.min(freshnessScore, 0.2);
    }
    freshnessScore = Math.max(freshnessFloor, freshnessScore);

    const basePriority = Number(recommendation.priorityScore || 0);
    const adjustedPriority = basePriority > 0 ?
      Math.max(0, Math.round(basePriority * Math.max(freshnessFloor, freshnessScore))) :
      basePriority;

    return {
      ...recommendation,
      suppressed,
      duplicateOf: duplicateOf || null,
      recurringCount,
      freshnessScore: Number(freshnessScore.toFixed(4)),
      cooldownUntil,
      reason,
      priorityScore: adjustedPriority,
    };
  });
}

function buildMembershipTrendRecommendations({
  summary7,
  summary30,
  previousSummary7,
  previousSummary30,
  trendSignals,
  compareWindows,
  alerts,
  costOverlay,
}) {
  const recommendations = [];
  const hasCvrDropAlert = alerts.some((entry) => entry.type === "cvr_drop");
  const hasCancelSpikeAlert = alerts.some((entry) => entry.type === "purchase_cancel_spike");
  const hasOpenNoPurchaseAlert = alerts.some((entry) => entry.type === "many_opens_no_purchases");

  if (hasCvrDropAlert || trendSignals.cvr === "declining") {
    pushRecommendation(recommendations, {
      id: "trigger_tuning_cvr_decline",
      title: "Trigger Timing fuer Upgrade-Hinweise feinjustieren",
      summary: "CVR sinkt; Timing und Intensitaet der Upgrade-Einstiege sollten kontrolliert nachjustiert werden.",
      recommendationType: "trigger_tuning",
      confidenceScore: recommendationConfidence(0.62, [
        hasCvrDropAlert ? 0.12 : 0,
        (summary7.membershipOpens || 0) > 20 ? 0.06 : -0.05,
      ]),
      severity: recommendationSeverityFromImpact("high"),
      likelyCause: "Upgrade-Einstiege greifen zu spaet oder in nicht optimalen Momenten.",
      suggestedAction: "A/B-Test fuer warning/critical Trigger-Schwellen (z. B. 65/88 vs 70/90) auf in-context Surfaces; keine aggressiven Muster.",
      supportingSignals: [
        `7d CVR: ${summary7.cvr}`,
        `vs previous 7d CVR delta: ${compareWindows.last7dVsPrevious7d.cvrDelta}`,
        `Trend: ${trendSignals.cvr}`,
      ],
      affectedPlans: ["free", "pro", "creator"],
      affectedSurfaces: ["ai_chat", "agent_chat", "ai_empty", "agent_empty"],
    });
  }

  if ((summary30.creatorTakeRate || 0) < 0.2 || trendSignals.creator === "falling") {
    pushRecommendation(recommendations, {
      id: "plan_focus_creator_positioning",
      title: "Creator Plan Positionierung schaerfen",
      summary: "Creator Take Rate ist schwach oder fallend; Nutzen und Platzierung des Creator-Plans sollten gezielt verbessert werden.",
      recommendationType: "plan_focus",
      confidenceScore: recommendationConfidence(0.58, [
        trendSignals.creator === "falling" ? 0.1 : 0,
        (summary30.purchaseSuccess || 0) >= 15 ? 0.08 : -0.06,
      ]),
      severity: recommendationSeverityFromImpact("medium"),
      likelyCause: "Creator Value wird nicht klar genug gegen Pro abgegrenzt.",
      suggestedAction: "Creator-spezifische Capability-Proofs im Membership Sheet priorisieren (Workflow-Power, Priority, Premium Outputs), Reihenfolge/Highlight testen.",
      supportingSignals: [
        `30d creatorTakeRate: ${summary30.creatorTakeRate}`,
        `creator trend: ${trendSignals.creator}`,
        `30d purchases: ${summary30.purchaseSuccess}`,
      ],
      affectedPlans: ["creator"],
      affectedSurfaces: ["ai_chat", "agent_chat"],
    });
  }

  if ((summary30.annualShare || 0) < 0.35 || trendSignals.annualMomentum === "declining") {
    pushRecommendation(recommendations, {
      id: "annual_share_momentum_recovery",
      title: "Annual Momentum mit klarer, ruhiger Kommunikation staerken",
      summary: "Annual-Anteil ist zu niedrig oder sinkt; annual value communication und default presentation sollten ueberprueft werden.",
      recommendationType: "plan_focus",
      confidenceScore: recommendationConfidence(0.57, [
        trendSignals.annualMomentum === "declining" ? 0.1 : 0,
        (summary30.purchaseSuccess || 0) >= 12 ? 0.07 : -0.05,
      ]),
      severity: recommendationSeverityFromImpact("medium"),
      likelyCause: "Jahresvorteil wird im Kaufmoment nicht stark genug wahrgenommen.",
      suggestedAction: "Default annual toggle und annual benefit copy in Runtime Config testen; transparent und ohne Druck kommunizieren.",
      supportingSignals: [
        `30d annualShare: ${summary30.annualShare}`,
        `annual trend: ${trendSignals.annualMomentum}`,
        `30d vs previous30d annualShare delta: ${compareWindows.last30dVsPrevious30d.annualShareDelta}`,
      ],
      affectedPlans: ["pro", "creator"],
      affectedSurfaces: ["ai_chat", "agent_chat"],
    });
  }

  if (trendSignals.opensRisingPurchasesFlat || hasOpenNoPurchaseAlert) {
    pushRecommendation(recommendations, {
      id: "surface_priority_value_gap",
      title: "Surface-Priorisierung nach tatsächlicher Conversion ausrichten",
      summary: "Opens steigen, Purchases bleiben flach; Value-Kommunikation auf hochperformante In-Context-Surfaces fokussieren.",
      recommendationType: "surface_prioritization",
      confidenceScore: recommendationConfidence(0.61, [
        trendSignals.opensRisingPurchasesFlat ? 0.1 : 0,
        hasOpenNoPurchaseAlert ? 0.08 : 0,
      ]),
      severity: recommendationSeverityFromImpact("high"),
      likelyCause: "Zu viele Opens kommen aus schwachen Kontexten mit geringer Kaufabsicht.",
      suggestedAction: "Surface-CVR vergleichen und schwache Entry-Points entschlacken; AI/Agent in-context Flows priorisieren.",
      supportingSignals: [
        `opens rising but purchases flat: ${trendSignals.opensRisingPurchasesFlat}`,
        `7d opens: ${summary7.membershipOpens}`,
        `7d purchases: ${summary7.purchaseSuccess}`,
      ],
      affectedPlans: ["free", "pro"],
      affectedSurfaces: ["settings", "ai_empty", "agent_empty", "ai_chat", "agent_chat"],
    });
  }

  if (hasCancelSpikeAlert || (trendSignals.cancelSpikeDates || []).length > 0) {
    pushRecommendation(recommendations, {
      id: "messaging_cancel_trust_clarity",
      title: "Billing-/Trust-Kommunikation rund um Kaufabbruch verbessern",
      summary: "Cancel-Spitzen deuten auf Friktion oder Unsicherheit im Checkout-Moment hin.",
      recommendationType: "messaging_hint",
      confidenceScore: recommendationConfidence(0.6, [
        hasCancelSpikeAlert ? 0.12 : 0,
        (trendSignals.cancelSpikeDates || []).length > 0 ? 0.06 : 0,
      ]),
      severity: recommendationSeverityFromImpact("medium"),
      likelyCause: "Preis-/Laufzeit-/Abrechnungsklarheit ist unmittelbar vor Kauf nicht eindeutig genug.",
      suggestedAction: "Kurz vor CTA klare, ruhige Billing-FAQ-Microcopy testen (Abrechnung, Restore, Steuerung), ohne Countdown/Druck.",
      supportingSignals: [
        `7d cancelRate: ${summary7.cancelRate}`,
        `cancel spikes: ${(trendSignals.cancelSpikeDates || []).map((entry) => entry.dateKey).join(", ") || "none"}`,
      ],
      affectedPlans: ["pro", "creator"],
      affectedSurfaces: ["ai_chat", "agent_chat"],
    });
  }

  if ((costOverlay?.freePlanLoadRatio || 0) > 0.55 && (summary30.creatorTakeRate || 0) < 0.25) {
    pushRecommendation(recommendations, {
      id: "revenue_hygiene_free_burn_vs_uptake",
      title: "Free Load vs Paid Uptake aktiv monitoren",
      summary: "Free-Load ist hoch bei schwacher Creator-Uptake; Conversion-Hebel sollten auf faire Value-Progression ausgerichtet werden.",
      recommendationType: "revenue_hygiene",
      confidenceScore: recommendationConfidence(0.64, [
        (costOverlay?.freePlanLoadRatio || 0) > 0.65 ? 0.08 : 0,
      ]),
      severity: recommendationSeverityFromImpact("high"),
      likelyCause: "Nutzer erhalten Value im Free-Flow, aber Upgrade-Nutzen wird zu wenig konkret.",
      suggestedAction: "Upgrade-Framing entlang echter Capability-Spruenge staerken (nicht Limits-only), plus Free->Pro Fortschrittspfade klarer machen.",
      supportingSignals: [
        `freePlanLoadRatio: ${costOverlay?.freePlanLoadRatio || 0}`,
        `30d creatorTakeRate: ${summary30.creatorTakeRate}`,
        `burnByPlan.free: ${costOverlay?.burnByPlanMicros?.free || 0}`,
      ],
      affectedPlans: ["free", "pro", "creator"],
      affectedSurfaces: ["ai_chat", "agent_chat", "ai_empty", "agent_empty"],
    });
  }

  if (summary7.rawCounts.upgradeAfterDeny >= 8 && summary7.upgradeAfterDenyCvr < 0.15) {
    pushRecommendation(recommendations, {
      id: "trigger_fatigue_deny_path",
      title: "Deny-Upgrade Pfad gegen Trigger Fatigue absichern",
      summary: "Viele deny-getriggerte Upgrade-Momente mit niedriger Conversion deuten auf Framing-Fatigue.",
      recommendationType: "messaging_hint",
      confidenceScore: recommendationConfidence(0.59, [
        summary7.rawCounts.upgradeAfterDeny >= 14 ? 0.1 : 0,
      ]),
      severity: recommendationSeverityFromImpact("medium"),
      likelyCause: "Deny-Situation vermittelt zu wenig konkreten naechsten Schritt oder passenden Lower-Cost-Pfad.",
      suggestedAction: "Deny-Copy und Calm Cards auf konkrete Outcome-Sprache pruefen (was gewinnt der Nutzer sofort), inkl. lower-cost alternative.",
      supportingSignals: [
        `7d upgradeAfterDeny count: ${summary7.rawCounts.upgradeAfterDeny}`,
        `7d upgradeAfterDenyCvr: ${summary7.upgradeAfterDenyCvr}`,
      ],
      affectedPlans: ["free", "pro"],
      affectedSurfaces: ["ai_chat", "agent_chat"],
    });
  }

  return recommendations
      .sort((a, b) => {
        const sev = {high: 3, medium: 2, low: 1};
        const sevDelta = (sev[b.severity] || 0) - (sev[a.severity] || 0);
        if (sevDelta !== 0) return sevDelta;
        return (Number(b.confidenceScore) || 0) - (Number(a.confidenceScore) || 0);
      })
      .slice(0, 12);
}

function parseHorizonDays(value) {
  const numeric = Number(value);
  if ([7, 14, 30].includes(numeric)) return numeric;
  return 14;
}

function clampRange(range) {
  const min = Number(range?.min) || 0;
  const max = Number(range?.max) || 0;
  const safeMin = Number(Math.max(-0.5, Math.min(1, min)).toFixed(4));
  const safeMax = Number(Math.max(safeMin, Math.min(1, max)).toFixed(4));
  return {min: safeMin, max: safeMax};
}

function complexityWeight(level) {
  if (level === "low") return 1;
  if (level === "medium") return 0.75;
  return 0.5;
}

function riskWeight(level) {
  if (level === "low") return 1;
  if (level === "medium") return 0.7;
  return 0.45;
}

function scoreImpactProjection(projection, confidenceScore, executionComplexity, riskLevel) {
  const cvrImpact = Math.max(0, Number(projection?.cvrRange?.max) || 0);
  const annualImpact = Math.max(0, Number(projection?.annualShareRange?.max) || 0);
  const creatorImpact = Math.max(0, Number(projection?.creatorTakeRateRange?.max) || 0);
  const cancelImpact = Math.max(0, Number(projection?.cancelReductionRange?.max) || 0);
  const impactBlend = (cvrImpact * 0.4) + (annualImpact * 0.2) + (creatorImpact * 0.2) + (cancelImpact * 0.2);
  const weighted = impactBlend *
    (Number(confidenceScore) || 0.3) *
    complexityWeight(executionComplexity) *
    riskWeight(riskLevel);
  return Math.max(0, Math.min(100, Math.round(weighted * 220)));
}

function simulateRecommendationImpact(recommendation, context = {}) {
  const type = nonEmptyString(recommendation?.recommendationType) || "messaging_hint";
  const confidence = Number(recommendation?.confidenceScore) || 0.35;
  const lowDataPenalty = (Number(context?.eventCount) || 0) < 50 ? -0.12 : 0;
  let executionComplexity = "medium";
  let riskLevel = "medium";
  let expectedTimeToSignalDays = 14;
  let rollbackEase = "medium";
  let projectedLift = {
    cvrRange: {min: 0.005, max: 0.02},
    annualShareRange: {min: 0.0, max: 0.015},
    creatorTakeRateRange: {min: 0.0, max: 0.015},
    cancelReductionRange: {min: 0.0, max: 0.02},
  };
  let rationale = "Moderater Hebel mit konservativer Wirkung im bestehenden Funnel.";
  let suggestedExperimentDesign = "A/B-Test auf 14 Tage mit Holdout-Gruppe, Fokus auf CVR und Cancel-Rate.";

  if (type === "trigger_tuning") {
    executionComplexity = "low";
    riskLevel = "low";
    expectedTimeToSignalDays = 7;
    rollbackEase = "high";
    projectedLift = {
      cvrRange: {min: 0.01, max: 0.035},
      annualShareRange: {min: 0.0, max: 0.01},
      creatorTakeRateRange: {min: 0.0, max: 0.012},
      cancelReductionRange: {min: 0.0, max: 0.008},
    };
    rationale = "Trigger-Tuning greift direkt in Einstiegslogik und liefert meist schnell messbare Funnel-Effekte.";
    suggestedExperimentDesign = "7-14 Tage Split-Test: aktuelle Schwellen vs. Variante (z. B. warning/critical +/- 3-5 Punkte), ohne UI-Pushiness.";
  } else if (type === "plan_focus") {
    executionComplexity = "medium";
    riskLevel = "low";
    expectedTimeToSignalDays = 14;
    rollbackEase = "high";
    projectedLift = {
      cvrRange: {min: 0.005, max: 0.025},
      annualShareRange: {min: 0.005, max: 0.03},
      creatorTakeRateRange: {min: 0.005, max: 0.03},
      cancelReductionRange: {min: 0.0, max: 0.006},
    };
    rationale = "Plan-Positionierung beeinflusst Mix und Annual/Creator-Uptake, meist mit mittelfristiger Signalzeit.";
    suggestedExperimentDesign = "14-30 Tage Test mit zwei Plan-Sheet Varianten (Highlight/Order/Copy), Primärmetriken: annualShare + creatorTakeRate.";
  } else if (type === "surface_prioritization") {
    executionComplexity = "medium";
    riskLevel = "medium";
    expectedTimeToSignalDays = 14;
    rollbackEase = "medium";
    projectedLift = {
      cvrRange: {min: 0.008, max: 0.03},
      annualShareRange: {min: 0.0, max: 0.012},
      creatorTakeRateRange: {min: 0.0, max: 0.015},
      cancelReductionRange: {min: 0.0, max: 0.01},
    };
    rationale = "Bessere Surface-Allokation kann CVR heben, hat aber Abhaengigkeit von User-Flows und Traffic-Verteilung.";
    suggestedExperimentDesign = "14 Tage Traffic-Rebalancing Test nach Surface-CVR; Safety-Limit fuer Open-Volume je Surface.";
  } else if (type === "messaging_hint") {
    executionComplexity = "low";
    riskLevel = "low";
    expectedTimeToSignalDays = 7;
    rollbackEase = "high";
    projectedLift = {
      cvrRange: {min: 0.004, max: 0.018},
      annualShareRange: {min: 0.0, max: 0.01},
      creatorTakeRateRange: {min: 0.0, max: 0.012},
      cancelReductionRange: {min: 0.005, max: 0.03},
    };
    rationale = "Messaging-Anpassungen sind schnell testbar und wirken haeufig auf Trust-/Cancel-Dynamik.";
    suggestedExperimentDesign = "7-14 Tage Copy-Test (Control vs. Klarheitsversion), Fokus auf cancelRate und purchaseSuccess.";
  } else if (type === "revenue_hygiene") {
    executionComplexity = "high";
    riskLevel = "medium";
    expectedTimeToSignalDays = 30;
    rollbackEase = "medium";
    projectedLift = {
      cvrRange: {min: 0.003, max: 0.02},
      annualShareRange: {min: 0.002, max: 0.015},
      creatorTakeRateRange: {min: 0.004, max: 0.02},
      cancelReductionRange: {min: 0.0, max: 0.012},
    };
    rationale = "Revenue-Hygiene wirkt breiter auf Mix und Kostenbild, braucht aber laengere Messfenster.";
    suggestedExperimentDesign = "30 Tage Guardrail + Positioning Experiment mit klaren Stop-Kriterien bei CVR-Rueckgang.";
  }

  const adjustedConfidence = recommendationConfidence(confidence, [lowDataPenalty]);
  projectedLift = {
    cvrRange: clampRange(projectedLift.cvrRange),
    annualShareRange: clampRange(projectedLift.annualShareRange),
    creatorTakeRateRange: clampRange(projectedLift.creatorTakeRateRange),
    cancelReductionRange: clampRange(projectedLift.cancelReductionRange),
  };

  const priorityScore = scoreImpactProjection(
      projectedLift,
      adjustedConfidence,
      executionComplexity,
      riskLevel,
  );

  return {
    recommendationId: recommendation.id || "",
    projectedLift,
    confidenceScore: adjustedConfidence,
    executionComplexity,
    riskLevel,
    expectedTimeToSignalDays,
    priorityScore,
    rationale,
    suggestedExperimentDesign,
    rollbackEase,
    generatedAt: Date.now(),
  };
}

function normalizeRecommendationStatus(value, fallback = "proposed") {
  const normalized = nonEmptyString(value)?.toLowerCase() || fallback;
  return AI_MEMBERSHIP_RECOMMENDATION_STATUSES.includes(normalized) ? normalized : fallback;
}

function normalizeImpactRange(input, defaultMin = 0, defaultMax = 0) {
  const min = Number(input?.min);
  const max = Number(input?.max);
  const safeMin = Number.isFinite(min) ? min : defaultMin;
  const safeMax = Number.isFinite(max) ? max : defaultMax;
  return clampRange({min: safeMin, max: safeMax});
}

function normalizeActualImpact(input = {}) {
  return {
    cvrDelta: Number.isFinite(Number(input.cvrDelta)) ? Number(input.cvrDelta) : 0,
    annualDelta: Number.isFinite(Number(input.annualDelta)) ? Number(input.annualDelta) : 0,
    creatorDelta: Number.isFinite(Number(input.creatorDelta)) ? Number(input.creatorDelta) : 0,
    cancelDelta: Number.isFinite(Number(input.cancelDelta)) ? Number(input.cancelDelta) : 0,
    observedWindowDays: Number.isFinite(Number(input.observedWindowDays)) ? Math.max(1, Math.floor(Number(input.observedWindowDays))) : 14,
    success: input.success === true,
    learnings: nonEmptyString(input.learnings) || "",
  };
}

function simulationAccuracyFromExpectedActual(expectedImpact = {}, actualImpact = {}) {
  const expectedMid = (Number(expectedImpact?.cvrRange?.min) + Number(expectedImpact?.cvrRange?.max)) / 2;
  const expected = Number.isFinite(expectedMid) ? expectedMid : 0;
  const actual = Number.isFinite(Number(actualImpact?.cvrDelta)) ? Number(actualImpact.cvrDelta) : 0;
  const denominator = Math.max(0.005, Math.abs(expected));
  const relativeError = Math.abs(actual - expected) / denominator;
  const accuracy = Math.max(0, Math.min(1, 1 - relativeError));
  return Number(accuracy.toFixed(4));
}

function summarizeLearningInsights(lifecycleDocs = []) {
  const rows = lifecycleDocs.map((doc) => doc.data ? (doc.data() || {}) : {}).filter(Boolean);
  const completed = rows.filter((row) => normalizeRecommendationStatus(row.status) === "completed");
  const rejected = rows.filter((row) => normalizeRecommendationStatus(row.status) === "rejected");
  const typeStats = {};
  const surfaceStats = {};
  let fastestTimeToSignalDays = null;
  let confidenceCalibrationTotal = 0;
  let confidenceCalibrationCount = 0;
  let simulationAccuracyTotal = 0;
  let simulationAccuracyCount = 0;
  let lowRiskReliableTotal = 0;
  let lowRiskReliableWins = 0;

  for (const row of completed) {
    const type = aiMetricKey(nonEmptyString(row.recommendationType) || "unknown");
    const surfaces = Array.isArray(row.affectedSurfaces) ? row.affectedSurfaces : [];
    const actualImpact = row.actualImpact || {};
    const success = actualImpact.success === true;
    const observedWindowDays = Number(actualImpact.observedWindowDays) || 0;
    const confidenceAtStart = Number(row.confidenceAtStart);
    const simulationAccuracy = Number(row.simulationAccuracy);
    const riskLevel = nonEmptyString(row?.expectedImpact?.riskLevel || row.riskLevel) || "medium";

    if (!typeStats[type]) {
      typeStats[type] = {total: 0, wins: 0, averageAccuracy: 0, accuracyCount: 0};
    }
    typeStats[type].total += 1;
    if (success) typeStats[type].wins += 1;
    if (Number.isFinite(simulationAccuracy)) {
      typeStats[type].averageAccuracy += simulationAccuracy;
      typeStats[type].accuracyCount += 1;
      simulationAccuracyTotal += simulationAccuracy;
      simulationAccuracyCount += 1;
    }

    for (const surfaceRaw of surfaces) {
      const surface = normalizeMembershipSurface(surfaceRaw);
      if (!surfaceStats[surface]) {
        surfaceStats[surface] = {total: 0, wins: 0};
      }
      surfaceStats[surface].total += 1;
      if (success) surfaceStats[surface].wins += 1;
    }

    if (observedWindowDays > 0 && (fastestTimeToSignalDays == null || observedWindowDays < fastestTimeToSignalDays)) {
      fastestTimeToSignalDays = observedWindowDays;
    }

    if (Number.isFinite(confidenceAtStart)) {
      const expectedBinary = confidenceAtStart >= 0.5 ? 1 : 0;
      const actualBinary = success ? 1 : 0;
      confidenceCalibrationTotal += 1 - Math.abs(expectedBinary - actualBinary);
      confidenceCalibrationCount += 1;
    }

    if (riskLevel === "low") {
      lowRiskReliableTotal += 1;
      if (success) lowRiskReliableWins += 1;
    }
  }

  const recommendationTypePerformance = Object.entries(typeStats).map(([type, stats]) => ({
    recommendationType: type,
    total: stats.total,
    successRate: stats.total > 0 ? Number((stats.wins / stats.total).toFixed(4)) : 0,
    averageSimulationAccuracy: stats.accuracyCount > 0 ? Number((stats.averageAccuracy / stats.accuracyCount).toFixed(4)) : null,
  })).sort((a, b) => b.successRate - a.successRate);

  const overestimatedTypes = recommendationTypePerformance
      .filter((entry) => Number(entry.averageSimulationAccuracy) < 0.45)
      .map((entry) => entry.recommendationType);

  const bestConvertingSurfaces = Object.entries(surfaceStats).map(([surface, stats]) => ({
    surface,
    total: stats.total,
    successRate: stats.total > 0 ? Number((stats.wins / stats.total).toFixed(4)) : 0,
  })).sort((a, b) => b.successRate - a.successRate);

  return {
    totalLifecycleRecords: rows.length,
    completedExperiments: completed.length,
    rejectedRecommendations: rejected.length,
    recommendationTypePerformance,
    overestimatedTypes,
    bestConvertingSurfaces,
    fastestTimeToSignalDays,
    mostReliableLowRiskActions: lowRiskReliableTotal > 0 ? Number((lowRiskReliableWins / lowRiskReliableTotal).toFixed(4)) : null,
    confidenceCalibrationScore: confidenceCalibrationCount > 0 ? Number((confidenceCalibrationTotal / confidenceCalibrationCount).toFixed(4)) : null,
    simulationAccuracyTrend: simulationAccuracyCount > 0 ? Number((simulationAccuracyTotal / simulationAccuracyCount).toFixed(4)) : null,
  };
}

function parseTimelineRange(value) {
  const normalized = nonEmptyString(value)?.toLowerCase() || "30d";
  if (normalized === "7d") return 7;
  if (normalized === "30d") return 30;
  if (normalized === "90d") return 90;
  if (normalized === "all") return 3650;
  return 30;
}

function normalizedStringArray(value) {
  if (!Array.isArray(value)) return [];
  return value.map((entry) => nonEmptyString(entry)?.toLowerCase()).filter(Boolean);
}

function recommendationTitleFromId(id) {
  return (nonEmptyString(id) || "recommendation")
      .replace(/_/g, " ")
      .replace(/\b\w/g, (char) => char.toUpperCase());
}

function buildLifecycleTimelineEntries(rows = []) {
  return rows.map((row) => {
    const status = normalizeRecommendationStatus(row.status);
    const generatedAt = Number(row.generatedAt) || Number(row.startedAtEpochMillis) || Number(row.completedAtEpochMillis) || Date.now();
    const recommendationId = nonEmptyString(row.recommendationId) || "";
    const recommendationType = aiMetricKey(nonEmptyString(row.recommendationType) || "unknown");
    const severity = nonEmptyString(row.severity)?.toLowerCase() || "medium";
    const expectedImpact = row.expectedImpact || {};
    const actualImpact = row.actualImpact || {};
    const affectedPlans = Array.isArray(row.affectedPlans) ? row.affectedPlans.map((entry) => normalizeMembershipPlan(entry)) : [];
    const affectedSurfaces = Array.isArray(row.affectedSurfaces) ? row.affectedSurfaces.map((entry) => normalizeMembershipSurface(entry)) : [];
    const type = status === "rejected" ? "recommendation_rejected" :
      status === "completed" ? "experiment_completed" :
      status === "active" ? "experiment_started" :
      "recommendation_proposed";
    const title = status === "rejected" ? "Recommendation rejected" :
      status === "completed" ? "Experiment completed" :
      status === "active" ? "Experiment started" :
      "Recommendation proposed";
    return {
      id: `timeline_${nonEmptyString(row.recommendationId) || "entry"}_${generatedAt}`,
      date: generatedAt,
      dateKey: utcDateKeyFromEpochMillis(generatedAt),
      type,
      title,
      summary: nonEmptyString(row.notes) || recommendationTitleFromId(recommendationId),
      ownerAction: nonEmptyString(row.ownerAction) || "",
      expectedImpact,
      actualImpact,
      confidenceAtStart: Number.isFinite(Number(row.confidenceAtStart)) ? Number(row.confidenceAtStart) : null,
      learnings: nonEmptyString(actualImpact?.learnings) || "",
      recommendationId,
      recommendationType,
      lifecycleId: nonEmptyString(row.lifecycleId) || "",
      severity,
      affectedPlans,
      affectedSurfaces,
      links: {
        recommendationId,
        lifecycleId: nonEmptyString(row.lifecycleId) || "",
      },
      rerunDraft: {
        recommendationId,
        recommendationType,
        hypothesis: `Re-run ${recommendationType} with adjusted threshold/copy based on prior learnings.`,
      },
    };
  });
}

function applyTimelineFilters(entries, filters) {
  const types = normalizedStringArray(filters.types);
  const plans = normalizedStringArray(filters.plans);
  const severities = normalizedStringArray(filters.severities);
  return entries.filter((entry) => {
    if (types.length > 0 && !types.includes(nonEmptyString(entry.type)?.toLowerCase())) return false;
    if (plans.length > 0) {
      const entryPlans = Array.isArray(entry.affectedPlans) ? entry.affectedPlans.map((value) => nonEmptyString(value)?.toLowerCase()).filter(Boolean) : [];
      if (!entryPlans.some((plan) => plans.includes(plan))) return false;
    }
    if (severities.length > 0 && !severities.includes(nonEmptyString(entry.severity)?.toLowerCase())) return false;
    return true;
  });
}

async function authorizeAiUsage({
  auth,
  kind,
  provider = "firebase_functions",
  model = "",
  estimatedCostMicros = null,
  eventType = "",
  featureClass = "",
  requestWeight = 1,
  sourceRoute = "ai",
  functionName = "authorizeAiUsage",
  requestId = "",
  resultType = "text",
  allowDegrade = true,
}) {
  if (!Object.values(AI_USAGE_KINDS).includes(kind)) {
    throw new HttpsError("invalid-argument", "Unbekannte KI-Aktion.");
  }

  const {profile, effectiveEntitlement} = await assertAiAccess(auth);
  const runtimeSettings = await loadAiRuntimeSettings();
  const normalizedProvider = nonEmptyString(provider)?.toLowerCase() || "firebase_functions";
  const normalizedModel = nonEmptyString(model) || "";
  const normalizedFeatureClass = resolveFeatureClass(kind, featureClass);
  const normalizedEventType = nonEmptyString(eventType) || `${kind}_request`;
  const normalizedRequestId = nonEmptyString(requestId) || `req_${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  const normalizedWeight = Number.isFinite(Number(requestWeight)) ? Math.max(1, Math.floor(Number(requestWeight))) : 1;
  const plan = effectiveEntitlement.plan || USER_QUOTA_PLANS.free;
  const routingPolicy = resolvePlanRoutingPolicy(runtimeSettings, plan);
  const normalizedEstimatedCostMicros = resolveEstimatedCostMicros({
    runtimeSettings,
    featureClass: normalizedFeatureClass,
    provider: normalizedProvider,
    model: normalizedModel,
    requestWeight: normalizedWeight,
    estimatedCostMicros,
  });

  const denyAndRecord = async (denyReason) => {
    const hints = buildGuardrailHints({
      denyReason,
      allowDegrade: allowDegrade && runtimeSettings.costGuardrails.enableDegradeMode,
      plan,
    });
    await recordUsageEventV2({
      uid: auth.uid,
      plan,
      entitlementStatus: effectiveEntitlement.status,
      eventType: normalizedEventType,
      kind,
      featureClass: normalizedFeatureClass,
      provider: normalizedProvider,
      model: normalizedModel,
      requestWeight: normalizedWeight,
      estimatedCostMicros: normalizedEstimatedCostMicros,
      success: false,
      denyReason,
      sourceRoute,
      functionName,
      resultType,
      requestId: normalizedRequestId,
    });
    throw new HttpsError("resource-exhausted", hints.userFacingReason, {
      denyReason,
      ...hints,
      allow: false,
      degrade: allowDegrade && runtimeSettings.costGuardrails.enableDegradeMode,
      requestId: normalizedRequestId,
    });
  };

  if (!featureAllowedByCapability(effectiveEntitlement.capabilities, normalizedFeatureClass)) {
    await denyAndRecord("plan_feature_not_allowed");
  }

  if (!routingPolicy.providers.includes(normalizedProvider)) {
    await denyAndRecord("plan_provider_not_allowed");
  }

  if (normalizedEstimatedCostMicros > routingPolicy.maxEstimatedCostMicros) {
    await denyAndRecord("plan_cost_cap_exceeded");
  }

  if (normalizedFeatureClass === AI_FEATURE_CLASSES.workflow && routingPolicy.allowWorkflow !== true) {
    await denyAndRecord("workflow_not_allowed_for_plan");
  }

  const dateKey = aiUsageDateKey();
  const monthKey = aiUsageMonthKey(dateKey);
  const usageRef = admin.firestore().doc(`users/${auth.uid}/aiUsage/${dateKey}`);
  const globalUsageRef = admin.firestore()
      .collection(AI_USAGE_METRICS_COLLECTION)
      .doc(aiUsageMetricsDocumentId(dateKey));
  const monthlyUsageRef = admin.firestore()
      .collection(AI_USAGE_METRICS_COLLECTION)
      .doc(aiUsageMonthlyMetricsDocumentId(monthKey));
  const burnGuardRef = admin.firestore()
      .collection(AI_GUARDRAILS_COLLECTION)
      .doc(aiDailyBurnDocumentId(dateKey));

  let denyReasonInTransaction = "";
  let usageSummary = null;
  try {
    usageSummary = await admin.firestore().runTransaction(async (transaction) => {
      const [userSnapshot, globalSnapshot, monthlySnapshot, burnSnapshot] = await Promise.all([
      transaction.get(usageRef),
      transaction.get(globalUsageRef),
      transaction.get(monthlyUsageRef),
      transaction.get(burnGuardRef),
    ]);
    const currentData = userSnapshot.exists ? (userSnapshot.data() || {}) : {};
    const currentGlobalData = globalSnapshot.exists ? (globalSnapshot.data() || {}) : {};
    const currentMonthlyData = monthlySnapshot.exists ? (monthlySnapshot.data() || {}) : {};
    const currentBurnData = burnSnapshot.exists ? (burnSnapshot.data() || {}) : {};
    const counterField = aiUsageCounterField(kind);
    const currentCount = Number(currentData[counterField]) || 0;
    const currentTotal = Number(currentData.totalRequests) || 0;
    const baseLimit = aiUsageLimitForKind(kind, profile.aiLimits);
    const hardCap = aiUsageLimitForKind(kind, runtimeSettings.hardDailyCaps);
    const limit = runtimeSettings.costGuardEnabled ?
      Math.max(1, Math.min(baseLimit, hardCap)) :
      baseLimit;

    if (currentCount >= limit) {
      denyReasonInTransaction = "hard_limit_reached";
      throw new HttpsError("resource-exhausted", aiLimitReachedMessage(kind, limit));
    }

    const currentGlobalCount = Number(currentGlobalData[counterField]) || 0;
    const currentGlobalTotal = Number(currentGlobalData.totalRequests) || 0;
    const globalLimit = aiUsageLimitForKind(kind, runtimeSettings.globalDailyCaps);
    if (runtimeSettings.costGuardEnabled &&
      Number.isFinite(globalLimit) &&
      globalLimit > 0 &&
      currentGlobalCount >= globalLimit) {
      denyReasonInTransaction = "global_hard_limit_reached";
      throw new HttpsError("resource-exhausted", aiGlobalLimitReachedMessage(kind, globalLimit));
    }

    const lastConsumedAtMillis = currentData.lastConsumedAt?.toMillis ?
      currentData.lastConsumedAt.toMillis() :
      0;
    const windowMs = runtimeSettings.costGuardrails.suspiciousSpikeWindowSeconds * 1000;
    const burstCount = Number(currentData.recentBurstCount) || 0;
    const nextBurstCount = Date.now() - lastConsumedAtMillis <= windowMs ? burstCount + 1 : 1;
    if (nextBurstCount >= runtimeSettings.costGuardrails.suspiciousSpikeRequestThreshold) {
      denyReasonInTransaction = "suspicious_spike";
      throw new HttpsError("resource-exhausted", "Anfragen treffen gerade zu schnell ein.");
    }

    const currentBurnMicros = Number(currentBurnData.totalEstimatedCostMicros) || 0;
    const nextBurnMicros = currentBurnMicros + (normalizedEstimatedCostMicros * normalizedWeight);
    if (runtimeSettings.costGuardEnabled &&
      runtimeSettings.costGuardrails.dailyBurnCapMicros > 0 &&
      nextBurnMicros > runtimeSettings.costGuardrails.dailyBurnCapMicros) {
      denyReasonInTransaction = "daily_burn_cap_reached";
      throw new HttpsError("resource-exhausted", "Tagesbudget fuer AI ist derzeit ausgelastet.");
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
    const nextCostMicros = (Number(currentData.totalEstimatedCostMicros) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const nextGlobalCostMicros = (Number(currentGlobalData.totalEstimatedCostMicros) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const nextMonthlyCostMicros = (Number(currentMonthlyData.totalEstimatedCostMicros) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const featureKey = aiMetricKey(normalizedFeatureClass);
    const providerKey = aiMetricKey(normalizedProvider);
    const modelKey = aiMetricKey(normalizedModel || "default");
    const planKey = aiMetricKey(plan);
    const byFeature = {...(currentMonthlyData.byFeatureClass || {})};
    byFeature[featureKey] = (Number(byFeature[featureKey]) || 0) + normalizedWeight;
    const byProvider = {...(currentMonthlyData.byProvider || {})};
    byProvider[providerKey] = (Number(byProvider[providerKey]) || 0) + normalizedWeight;
    const byPlan = {...(currentMonthlyData.byPlan || {})};
    byPlan[planKey] = (Number(byPlan[planKey]) || 0) + normalizedWeight;
    const byModel = {...(currentMonthlyData.byModel || {})};
    byModel[modelKey] = (Number(byModel[modelKey]) || 0) + normalizedWeight;
    const byPlanCostMicros = {...(currentMonthlyData.byPlanCostMicros || {})};
    byPlanCostMicros[planKey] = (Number(byPlanCostMicros[planKey]) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const byFeatureClassCostMicros = {...(currentMonthlyData.byFeatureClassCostMicros || {})};
    byFeatureClassCostMicros[featureKey] = (Number(byFeatureClassCostMicros[featureKey]) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const byProviderCostMicros = {...(currentMonthlyData.byProviderCostMicros || {})};
    byProviderCostMicros[providerKey] = (Number(byProviderCostMicros[providerKey]) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);
    const byModelCostMicros = {...(currentMonthlyData.byModelCostMicros || {})};
    byModelCostMicros[modelKey] = (Number(byModelCostMicros[modelKey]) || 0) +
      (normalizedEstimatedCostMicros * normalizedWeight);

    transaction.set(usageRef, {
      dateKey,
      role: profile.role,
      textRequests: nextTextCount,
      visualRequests: nextVisualCount,
      agentRequests: nextAgentCount,
      totalRequests: nextTotal,
      totalEstimatedCostMicros: nextCostMicros,
      recentBurstCount: nextBurstCount,
      lastEventType: normalizedEventType,
      lastProvider: normalizedProvider,
      lastModel: normalizedModel || admin.firestore.FieldValue.delete(),
      lastEstimatedCostMicros: normalizedEstimatedCostMicros,
      lastRequestId: normalizedRequestId,
      lastConsumedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(globalUsageRef, {
      dateKey,
      textRequests: nextGlobalText,
      visualRequests: nextGlobalVisual,
      agentRequests: nextGlobalAgent,
      totalRequests: nextGlobalTotal,
      totalEstimatedCostMicros: nextGlobalCostMicros,
      lastEventType: normalizedEventType,
      lastProvider: normalizedProvider,
      lastModel: normalizedModel || admin.firestore.FieldValue.delete(),
      lastEstimatedCostMicros: normalizedEstimatedCostMicros,
      lastRequestId: normalizedRequestId,
      lastConsumedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(monthlyUsageRef, {
      monthKey,
      totalRequests: (Number(currentMonthlyData.totalRequests) || 0) + normalizedWeight,
      totalEstimatedCostMicros: nextMonthlyCostMicros,
      byFeatureClass: byFeature,
      byProvider,
      byPlan,
      byModel,
      byPlanCostMicros,
      byFeatureClassCostMicros,
      byProviderCostMicros,
      byModelCostMicros,
      lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(burnGuardRef, {
      dateKey,
      totalEstimatedCostMicros: nextBurnMicros,
      lastRequestId: normalizedRequestId,
      lastUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    const usagePercent = limit > 0 ? Math.floor((nextCount / limit) * 100) : 0;
    let warningLevel = "ok";
    if (usagePercent >= runtimeSettings.costGuardrails.criticalThresholdPercent) {
      warningLevel = "critical";
    } else if (usagePercent >= runtimeSettings.costGuardrails.warningThresholdPercent) {
      warningLevel = "warning";
    }

    return {
      dateKey,
      monthKey,
      role: profile.role,
      kind,
      featureClass: normalizedFeatureClass,
      requestId: normalizedRequestId,
      requestWeight: normalizedWeight,
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
      effectiveEntitlement: {
        plan: effectiveEntitlement.plan || null,
        status: effectiveEntitlement.status,
        provider: effectiveEntitlement.provider || null,
        source: effectiveEntitlement.source || null,
      },
      provider: normalizedProvider,
      model: normalizedModel || null,
      estimatedCostMicros: normalizedEstimatedCostMicros,
      eventType: normalizedEventType,
      sourceRoute,
      functionName,
      resultType,
      warningLevel,
      guardrailHints: buildGuardrailHints({
        warningLevel,
        remainingForKind: Math.max(limit - nextCount, 0),
        limitForKind: limit,
        allowDegrade: allowDegrade && runtimeSettings.costGuardrails.enableDegradeMode,
        plan,
      }),
      decision: {
        allow: true,
        deny: false,
        degrade: warningLevel === "critical" && runtimeSettings.costGuardrails.enableDegradeMode,
      },
    };
    });
  } catch (error) {
    const denyReason = denyReasonInTransaction || "authorization_denied";
    const hints = buildGuardrailHints({
      denyReason,
      allowDegrade: allowDegrade && runtimeSettings.costGuardrails.enableDegradeMode,
      plan,
    });
    await recordUsageEventV2({
      uid: auth.uid,
      plan,
      entitlementStatus: effectiveEntitlement.status,
      eventType: normalizedEventType,
      kind,
      featureClass: normalizedFeatureClass,
      provider: normalizedProvider,
      model: normalizedModel,
      requestWeight: normalizedWeight,
      estimatedCostMicros: normalizedEstimatedCostMicros,
      success: false,
      denyReason,
      sourceRoute,
      functionName,
      resultType,
      requestId: normalizedRequestId,
    });
    if (error instanceof HttpsError) {
      throw new HttpsError(error.code, hints.userFacingReason, {
        denyReason,
        ...hints,
        allow: false,
        requestId: normalizedRequestId,
      });
    }
    throw error;
  }

  await recordUsageEventV2({
    uid: auth.uid,
    plan,
    entitlementStatus: effectiveEntitlement.status,
    eventType: normalizedEventType,
    kind,
    featureClass: normalizedFeatureClass,
    provider: normalizedProvider,
    model: normalizedModel,
    requestWeight: normalizedWeight,
    estimatedCostMicros: normalizedEstimatedCostMicros,
    success: true,
    denyReason: "",
    sourceRoute,
    functionName,
    resultType,
    requestId: normalizedRequestId,
  });

  logger.info("AI usage authorized.", {
    uid: auth.uid,
    role: usageSummary.role,
    kind,
    featureClass: usageSummary.featureClass,
    remainingForKind: usageSummary.remainingForKind,
    globalRemainingForKind: usageSummary.globalRemainingForKind,
    dateKey,
    requestId: usageSummary.requestId,
    warningLevel: usageSummary.warningLevel,
  });

  return usageSummary;
}

/**
 * Server-only audit trail for Agent runs (no prompt/reply body — lengths only).
 * Clients may read their own documents; writes are Functions-only via Admin SDK.
 */
async function persistAgentRunSummary({
  uid,
  mode,
  agentProvider,
  providerFallbackUsed,
  automation,
  promptText,
  replyText,
}) {
  if (!nonEmptyString(uid)) {
    return null;
  }
  try {
    const runRef = admin.firestore()
        .collection("users")
        .doc(uid)
        .collection("agentRuns")
        .doc();
    const runId = runRef.id;
    const prompt = typeof promptText === "string" ? promptText : "";
    const reply = typeof replyText === "string" ? replyText : "";
    await runRef.set({
      createdAt: admin.firestore.FieldValue.serverTimestamp(),
      mode: nonEmptyString(mode) || "release",
      agentProvider: nonEmptyString(agentProvider) || "unknown",
      providerFallbackUsed: providerFallbackUsed === true,
      automationAttempted: automation && automation.attempted === true,
      automationTriggered: automation && automation.triggered === true,
      promptChars: Math.min(Math.max(prompt.length, 0), 100000),
      replyChars: Math.min(Math.max(reply.length, 0), 500000),
    });
    return runId;
  } catch (error) {
    logger.warn("agentRuns write failed.", {
      uid,
      error: error instanceof Error ? error.message : `${error}`,
    });
    return null;
  }
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

${AI_VISUAL_PHOTOGRAPHY_DIRECTION}

${assetContext ? `Verfuegbare Referenzen:\n${assetContext}\n` : ""}

Nutzeranfrage:
${inputPrompt}
  `.trim();
}

function composeVisualImageOnlyPrompt(inputPrompt, visualInstruction, assetContext) {
  const normalizedInstruction = visualInstruction
    .replace("Liefere neben dem Bild nur eine kurze Ein-Zeilen-Beschreibung des Looks.", "")
    .replace("Antworte auf Deutsch.", "")
    .trim();

  return `
${normalizedInstruction}

${AI_VISUAL_PHOTOGRAPHY_DIRECTION}

Erzeuge genau ein hochwertiges Bild und keinen zusaetzlichen Text.

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
  const primaryResult = await generateAiVisualWithRetries({
    modelName: "gemini-2.5-flash-image",
    prompt: composeVisualGenerationPrompt(
        prompt,
        promptSettings.visualInstruction,
        assetContext,
    ),
    responseModalities: ["TEXT", "IMAGE"],
    defaultText: "Visual generiert.",
  });
  if (primaryResult) {
    return primaryResult;
  }

  logger.warn("Primary visual model failed. Falling back to Imagen.", {
    promptLength: typeof prompt === "string" ? prompt.length : 0,
    primaryModel: "gemini-2.5-flash-image",
    fallbackModel: "imagen-3.0-generate-002",
  });

  const fallbackResult = await generateAiVisualWithRetries({
    modelName: "imagen-3.0-generate-002",
    prompt: composeVisualImageOnlyPrompt(
        prompt,
        promptSettings.visualInstruction,
        assetContext,
    ),
    defaultText: "Cineastisches Visual generiert.",
  });
  if (fallbackResult) {
    return fallbackResult;
  }

  throw new HttpsError(
      "internal",
      "Der Visual-Server konnte das Bild gerade nicht erzeugen. Bitte direkt noch einmal versuchen.",
  );
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function generateAiVisualWithRetries({
  modelName,
  prompt,
  responseModalities = null,
  defaultText,
}) {
  const maxAttempts = 2;

  for (let attempt = 1; attempt <= maxAttempts; attempt += 1) {
    let response;
    try {
      const request = {
        model: vertexAI.model(modelName),
        prompt,
      };
      if (responseModalities) {
        request.config = {responseModalities};
      }
      response = await ai.generate(request);
    } catch (error) {
      const metadata = {
        modelName,
        promptLength: typeof prompt === "string" ? prompt.length : 0,
        attempt,
        maxAttempts,
        error: error instanceof Error ? error.message : `${error}`,
      };
      if (attempt < maxAttempts) {
        logger.warn("AI visual generation attempt failed. Retrying.", metadata);
        await sleep(1200);
        continue;
      }

      logger.error("AI visual generation failed.", metadata);
      return null;
    }

    const media = response.media;
    const encodedImage = extractInlineBase64Media(media?.url, media?.contentType);
    if (encodedImage?.base64) {
      return {
        text: nonEmptyString(response.text) || defaultText,
        imageBase64: encodedImage.base64,
        mimeType: encodedImage.mimeType || "image/png",
      };
    }

    const metadata = {
      modelName,
      promptLength: typeof prompt === "string" ? prompt.length : 0,
      attempt,
      maxAttempts,
      hasMediaUrl: Boolean(media?.url),
      mimeType: media?.contentType || "",
      hasText: Boolean(nonEmptyString(response.text)),
    };
    if (attempt < maxAttempts) {
      logger.warn("AI visual generation returned no decodable image. Retrying.", metadata);
      await sleep(1200);
      continue;
    }

    logger.error("AI visual generation returned no decodable image.", metadata);
  }

  return null;
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
      iosCreatorProductId: nonEmptyString(aiSubscriptions.iosCreatorProductId) || "",
      iosStudioProductId: nonEmptyString(aiSubscriptions.iosStudioProductId) || "",
      iosAppAppleId: Number(aiSubscriptions.iosAppAppleId || 0),
      androidCreatorProductId: nonEmptyString(aiSubscriptions.androidCreatorProductId) || "",
      androidStudioProductId: nonEmptyString(aiSubscriptions.androidStudioProductId) || "",
      revenueCatEntitlementCreator: nonEmptyString(aiSubscriptions.revenueCatEntitlementCreator) || "skyos_ai_creator",
      revenueCatEntitlementStudio: nonEmptyString(aiSubscriptions.revenueCatEntitlementStudio) || "skyos_ai_studio",
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

function buildIosAiSubscriptionProductMap(paymentSettings) {
  const config = paymentSettings?.aiSubscriptions || {};
  if (config.enabled !== true) {
    return {};
  }

  const mapping = {};
  const creatorProductId = nonEmptyString(config.iosCreatorProductId);
  const studioProductId = nonEmptyString(config.iosStudioProductId);
  if (creatorProductId) {
    mapping[creatorProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioProductId) {
    mapping[studioProductId] = USER_QUOTA_PLANS.studio;
  }
  return mapping;
}

function resolveIosAiAppAppleId(paymentSettings) {
  const appAppleId = Number(paymentSettings?.aiSubscriptions?.iosAppAppleId || 0);
  return Number.isFinite(appAppleId) && appAppleId > 0 ? Math.floor(appAppleId) : 0;
}

function buildAndroidAiSubscriptionProductMap(paymentSettings) {
  const config = paymentSettings?.aiSubscriptions || {};
  if (config.enabled !== true) {
    return {};
  }

  const mapping = {};
  const creatorProductId = nonEmptyString(config.androidCreatorProductId);
  const studioProductId = nonEmptyString(config.androidStudioProductId);
  if (creatorProductId) {
    mapping[creatorProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioProductId) {
    mapping[studioProductId] = USER_QUOTA_PLANS.studio;
  }
  return mapping;
}

function buildRevenueCatEntitlementPlanMap(paymentSettings) {
  const config = paymentSettings?.aiSubscriptions || {};
  if (config.enabled !== true) {
    return {};
  }
  const mapping = {};
  const creatorEntitlement = nonEmptyString(config.revenueCatEntitlementCreator) || "skyos_ai_creator";
  const studioEntitlement = nonEmptyString(config.revenueCatEntitlementStudio) || "skyos_ai_studio";
  mapping[creatorEntitlement] = USER_QUOTA_PLANS.creator;
  mapping[studioEntitlement] = USER_QUOTA_PLANS.studio;
  return mapping;
}

function buildUnifiedAiProductPlanMap(paymentSettings) {
  return {
    ...buildIosAiSubscriptionProductMap(paymentSettings),
    ...buildAndroidAiSubscriptionProductMap(paymentSettings),
  };
}

function normalizeRevenueCatEntitlementEvent(data, paymentSettings) {
  const payload = data && typeof data === "object" ? data : {};
  const eventType = nonEmptyString(payload.event)?.toLowerCase() || "unknown";
  const appUserId = nonEmptyString(payload.app_user_id) || "";
  const productId = nonEmptyString(payload.product_id) || "";
  const entitlementId = nonEmptyString(payload.entitlement_id) || "";
  const entitlementMap = buildRevenueCatEntitlementPlanMap(paymentSettings);
  const productMap = buildUnifiedAiProductPlanMap(paymentSettings);
  const plan = entitlementMap[entitlementId] || productMap[productId] || null;
  const expirationAtMs = Number(payload.expiration_at_ms || 0);
  const purchasedAtMs = Number(payload.purchased_at_ms || 0);
  const isActiveEvent = ["initial_purchase", "renewal", "product_change", "uncancellation", "non_renewing_purchase"]
      .includes(eventType);
  const status = isActiveEvent ? "active" : (
    ["cancellation", "billing_issue", "expiration", "subscription_paused"].includes(eventType) ?
      "inactive" :
      "inactive"
  );
  return {
    uid: appUserId,
    eventType,
    externalEventId: nonEmptyString(payload.id) || "",
    productId,
    plan,
    status,
    periodStartEpochSeconds: Number.isFinite(purchasedAtMs) && purchasedAtMs > 0 ? Math.floor(purchasedAtMs / 1000) : 0,
    periodEndEpochSeconds: Number.isFinite(expirationAtMs) && expirationAtMs > 0 ? Math.floor(expirationAtMs / 1000) : 0,
    environment: nonEmptyString(payload.environment)?.toLowerCase() || "",
    purchaseReference:
      nonEmptyString(payload.transaction_id) ||
      nonEmptyString(payload.original_transaction_id) ||
      "",
    originalTransactionId: nonEmptyString(payload.original_transaction_id) || "",
    source: nonEmptyString(payload.store)?.toLowerCase() || "revenuecat",
    provider: "revenuecat",
    metadata: {
      entitlementId,
      aliases: Array.isArray(payload.aliases) ? payload.aliases : [],
      presentedOfferingId: nonEmptyString(payload.presented_offering_id) || "",
      priceInPurchasedCurrency: payload.price_in_purchased_currency ?? null,
      currency: nonEmptyString(payload.currency) || "",
    },
  };
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
    aiSubscriptionProvider: "stripe",
    aiSubscriptionSourcePlatform: metadata.platform || nonEmptyString(userData.aiSubscriptionSourcePlatform) || "web",
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
    aiSubscriptionProvider: "stripe",
    aiSubscriptionSourcePlatform: metadata.platform || nonEmptyString(userData.aiSubscriptionSourcePlatform) || "web",
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

exports.syncAndroidAiSubscriptionStatus = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "syncAndroidAiSubscriptionStatus");

  const uid = assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um dein Play-Store-Abo zu synchronisieren.",
  );
  const userData = await loadUserData(uid);
  if (!userData) {
    throw new HttpsError("not-found", "Konto wurde nicht gefunden.");
  }

  const paymentSettings = await loadPaymentMethodSettings();
  if (paymentSettings?.aiSubscriptions?.enabled !== true) {
    throw new HttpsError("failed-precondition", "KI-Abos sind serverseitig noch nicht live geschaltet.");
  }

  const androidProductIdToPlan = buildAndroidAiSubscriptionProductMap(paymentSettings);
  if (Object.keys(androidProductIdToPlan).length < 2) {
    throw new HttpsError(
        "failed-precondition",
        "Die Android-Produkt-IDs fuer Creator und Studio fehlen noch im Payment-Setup.",
    );
  }

  const productId = nonEmptyString(request.data?.productId);
  const purchaseToken = nonEmptyString(request.data?.purchaseToken);
  const packageName = nonEmptyString(request.data?.packageName) || "";
  const orderId = nonEmptyString(request.data?.orderId) || "";
  if (!productId || !purchaseToken) {
    throw new HttpsError("invalid-argument", "productId und purchaseToken sind erforderlich.");
  }

  const resolvedPlan = androidProductIdToPlan[productId] || null;
  if (!resolvedPlan) {
    throw new HttpsError("invalid-argument", "Unbekanntes Android-Abo-Produkt.");
  }

  // MVP scaffold: entitlement stays unchanged until purchase token verification is wired.
  // This keeps billing source-of-truth server-only and avoids trusting client assertions.
  const syncEvent = await saveCanonicalAiEntitlement(uid, resolveLegacyAiEntitlement(userData), {
    externalEventId: `android_${productId}_${purchaseToken.slice(-12)}`,
    eventType: "android_sync_pending",
    eventSource: "syncAndroidAiSubscriptionStatus",
    metadata: {
      status: "verification_pending",
      requestedPlan: resolvedPlan,
      packageName,
      orderId,
      purchaseTokenPreview: purchaseToken.slice(-8),
      productId,
    },
    rawRef: "play_purchase_token",
  });

  logger.info("Android AI subscription sync requested.", {
    uid,
    productId,
    plan: resolvedPlan,
    packageName: packageName || null,
  });

  return {
    status: "verification_pending",
    provider: "play_store",
    plan: resolvedPlan,
    eventId: syncEvent.eventId,
  };
});

exports.ingestRevenueCatAiEntitlementEvent = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "ingestRevenueCatAiEntitlementEvent");
  await assertOwner(request.auth);

  const paymentSettings = await loadPaymentMethodSettings();
  if (paymentSettings?.aiSubscriptions?.enabled !== true) {
    throw new HttpsError("failed-precondition", "KI-Abos sind serverseitig noch nicht live geschaltet.");
  }

  const normalized = normalizeRevenueCatEntitlementEvent(request.data, paymentSettings);
  if (!normalized.uid) {
    throw new HttpsError("invalid-argument", "RevenueCat Event enthaelt keine gueltige app_user_id.");
  }
  if (!normalized.plan) {
    throw new HttpsError("failed-precondition", "RevenueCat Event konnte keinem SkyOS Plan zugeordnet werden.");
  }

  const targetUserData = await loadUserData(normalized.uid);
  const role = resolveUserRole(
      targetUserData?.role,
      targetUserData?.isAdmin === true,
      targetUserData?.email,
  );
  const saveResult = await saveCanonicalAiEntitlement(normalized.uid, {
    plan: normalized.plan,
    status: normalized.status,
    provider: normalized.provider,
    source: normalized.source,
    productId: normalized.productId,
    periodStartEpochSeconds: normalized.periodStartEpochSeconds,
    periodEndEpochSeconds: normalized.periodEndEpochSeconds,
    environment: normalized.environment,
    originalTransactionId: normalized.originalTransactionId,
    purchaseReference: normalized.purchaseReference,
    capabilities: resolveAiCapabilities({
      plan: normalized.plan,
      status: normalized.status,
      role,
    }),
  }, {
    externalEventId: normalized.externalEventId || `${normalized.eventType}_${normalized.uid}`,
    eventType: `revenuecat_${normalized.eventType}`,
    eventSource: "revenuecat",
    metadata: normalized.metadata,
    rawRef: "revenuecat_webhook_payload",
  });

  return {
    uid: normalized.uid,
    plan: normalized.plan,
    status: normalized.status,
    eventId: saveResult.eventId,
    duplicated: saveResult.duplicated,
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
  if (profile.role === USER_ROLES.owner) {
    throw new HttpsError("failed-precondition", "Owner-Konto braucht kein Self-Pay-Abo.");
  }

  if (!profile.aiAccessEnabled) {
    throw new HttpsError("failed-precondition", "Die KI ist fuer dein Konto aktuell pausiert.");
  }

  const subscriptionStatus = normalizeAiSubscriptionStatus(userData.aiSubscriptionStatus, "");
  if (["active", "trialing", "past_due", "unpaid"].includes(subscriptionStatus)) {
    throw new HttpsError(
        "failed-precondition",
        "Auf diesem Konto existiert bereits ein KI-Abo. Verwaltung und Planwechsel folgen im naechsten Schritt.",
    );
  }

  const checkoutExpiresAt = Number(userData.aiSubscriptionCheckoutExpiresAtEpochSeconds || 0);
  const nowEpochSeconds = Math.floor(Date.now() / 1000);
  if (subscriptionStatus === "checkout_pending" &&
    (!Number.isFinite(checkoutExpiresAt) || checkoutExpiresAt > nowEpochSeconds)) {
    throw new HttpsError(
        "failed-precondition",
        "Fuer dieses Konto ist bereits ein offener Stripe-Checkout aktiv.",
    );
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
  if (["ios", "android"].includes(platform)) {
    throw new HttpsError(
        "failed-precondition",
        "Mobile KI-Abos bleiben bis zur nativen In-App-Abrechnung deaktiviert.",
    );
  }

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
    aiSubscriptionProvider: "stripe",
    aiSubscriptionSourcePlatform: platform,
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

exports.syncIosAiSubscriptionStatus = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "syncIosAiSubscriptionStatus");

  const uid = assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um dein App-Store-Abo zu synchronisieren.",
  );
  const userData = await loadUserData(uid);
  if (!userData) {
    throw new HttpsError("not-found", "Konto wurde nicht gefunden.");
  }

  const profile = buildUserProfile(userData);
  if (profile.role === USER_ROLES.owner) {
    return {
      status: "ignored",
      reason: "owner_account",
    };
  }

  const paymentSettings = await loadPaymentMethodSettings();
  if (paymentSettings?.aiSubscriptions?.enabled !== true) {
    throw new HttpsError("failed-precondition", "KI-Abos sind serverseitig noch nicht live geschaltet.");
  }

  const iosProductIdToPlan = buildIosAiSubscriptionProductMap(paymentSettings);
  if (Object.keys(iosProductIdToPlan).length < 2) {
    throw new HttpsError(
        "failed-precondition",
        "Die iOS-Produkt-IDs fuer Creator und Studio fehlen noch im Payment-Setup.",
    );
  }

  const iosAppAppleId = resolveIosAiAppAppleId(paymentSettings);
  if (!iosAppAppleId) {
    throw new HttpsError(
        "failed-precondition",
        "Die Apple App ID fuer serverseitige StoreKit-Pruefung fehlt noch.",
    );
  }

  const signedTransactions = Array.isArray(request.data?.signedTransactions) ?
    request.data.signedTransactions :
    typeof request.data?.signedTransactions === "string" ?
      [request.data.signedTransactions] :
      [];
  const decodeResult = await decodeAppStoreTransactions({
    signedTransactions,
    productIdToPlan: iosProductIdToPlan,
    userId: uid,
    bundleId: IOS_APP_BUNDLE_ID,
    appAppleId: iosAppAppleId,
    enableOnlineChecks: false,
  });

  if (decodeResult.receivedTransactionCount > 0 && decodeResult.decodedTransactions.length === 0) {
    throw new HttpsError(
        "failed-precondition",
        "Die App-Store-Transaktion konnte keinem gueltigen KI-Abo fuer dieses Konto zugeordnet werden.",
    );
  }

  const resolvedState = resolveAppStoreSubscriptionState(decodeResult.decodedTransactions);
  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const currentProvider = nonEmptyString(userData.aiSubscriptionProvider)?.toLowerCase() || "";
  const userRef = admin.firestore().doc(`users/${uid}`);
  const updates = {
    aiSubscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (resolvedState.status === "active") {
    Object.assign(updates, resolveAiLimitsUpdateForPlan(resolvedState.plan));
    Object.assign(updates, {
      aiSubscriptionStatus: resolvedState.status,
      aiSubscriptionPlan: resolvedState.plan,
      aiSubscriptionProvider: resolvedState.provider,
      aiSubscriptionSourcePlatform: resolvedState.sourcePlatform,
      aiSubscriptionProductId: resolvedState.productId || admin.firestore.FieldValue.delete(),
      aiSubscriptionStoreEnvironment: resolvedState.environment || admin.firestore.FieldValue.delete(),
      aiSubscriptionOriginalTransactionId: resolvedState.originalTransactionId || admin.firestore.FieldValue.delete(),
      aiSubscriptionTransactionId: resolvedState.transactionId || admin.firestore.FieldValue.delete(),
      aiSubscriptionPriceId: admin.firestore.FieldValue.delete(),
      aiSubscriptionCurrentPeriodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || admin.firestore.FieldValue.delete(),
      aiSubscriptionCheckoutExpiresAtEpochSeconds: admin.firestore.FieldValue.delete(),
      aiSubscriptionCancelAtPeriodEnd: false,
      aiSubscriptionActivatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else if (currentProvider === "app_store") {
    Object.assign(updates, resolveAiLimitsUpdateForRole(role));
    Object.assign(updates, {
      aiSubscriptionStatus: resolvedState.status === "inactive" ? "inactive" : resolvedState.status,
      aiSubscriptionPlan: resolvedState.plan || admin.firestore.FieldValue.delete(),
      aiSubscriptionProvider: resolvedState.provider || admin.firestore.FieldValue.delete(),
      aiSubscriptionSourcePlatform: resolvedState.sourcePlatform || admin.firestore.FieldValue.delete(),
      aiSubscriptionProductId: resolvedState.productId || admin.firestore.FieldValue.delete(),
      aiSubscriptionStoreEnvironment: resolvedState.environment || admin.firestore.FieldValue.delete(),
      aiSubscriptionOriginalTransactionId: resolvedState.originalTransactionId || admin.firestore.FieldValue.delete(),
      aiSubscriptionTransactionId: resolvedState.transactionId || admin.firestore.FieldValue.delete(),
      aiSubscriptionPriceId: admin.firestore.FieldValue.delete(),
      aiSubscriptionCurrentPeriodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || admin.firestore.FieldValue.delete(),
      aiSubscriptionCheckoutExpiresAtEpochSeconds: admin.firestore.FieldValue.delete(),
      aiSubscriptionCancelAtPeriodEnd: false,
    });
  } else {
    return {
      status: "ignored",
      reason: "no_app_store_entitlements",
      matchedTransactionCount: decodeResult.decodedTransactions.length,
    };
  }

  await userRef.set(updates, {merge: true});
  await saveCanonicalAiEntitlement(uid, {
    plan: resolvedState.plan || null,
    status: resolvedState.status || "inactive",
    provider: "app_store",
    source: resolvedState.sourcePlatform || "ios",
    productId: resolvedState.productId || "",
    periodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || 0,
    environment: resolvedState.environment || "",
    originalTransactionId: resolvedState.originalTransactionId || "",
    purchaseReference: resolvedState.transactionId || "",
    capabilities: resolveAiCapabilities({
      plan: resolvedState.plan || null,
      status: resolvedState.status || "inactive",
      role,
    }),
  }, {
    externalEventId:
      nonEmptyString(resolvedState.transactionId) ||
      nonEmptyString(resolvedState.originalTransactionId) ||
      `ios_${uid}_${Date.now()}`,
    eventType: "ios_sync",
    eventSource: "syncIosAiSubscriptionStatus",
    metadata: {
      matchedTransactionCount: decodeResult.decodedTransactions.length,
      sourcePlatform: resolvedState.sourcePlatform || "ios",
    },
    rawRef: "app_store_server_validation",
  });

  logger.info("iOS AI subscription status synced.", {
    uid,
    status: updates.aiSubscriptionStatus,
    plan: resolvedState.plan || null,
    provider: resolvedState.provider || null,
    matchedTransactionCount: decodeResult.decodedTransactions.length,
  });

  return {
    status: updates.aiSubscriptionStatus,
    plan: resolvedState.plan || null,
    provider: resolvedState.provider || null,
    sourcePlatform: resolvedState.sourcePlatform || null,
    currentPeriodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || null,
    matchedTransactionCount: decodeResult.decodedTransactions.length,
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
    provider: "firebase_functions",
    eventType: "authorize_preview",
    sourceRoute: "callable.authorizeAiUsage",
    functionName: "authorizeAiUsage",
    featureClass: kind === AI_USAGE_KINDS.visual ? AI_FEATURE_CLASSES.image : AI_FEATURE_CLASSES.text,
    requestId: nonEmptyString(request.data?.requestId) || "",
  });
});

exports.reconcileAiUsageCost = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "reconcileAiUsageCost");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Kostenabgleich ausfuehren.");
  }

  const requestId = nonEmptyString(request.data?.requestId);
  const actualCostMicros = Number(request.data?.actualCostMicros);
  const reconciliationSource = nonEmptyString(request.data?.reconciliationSource) || "provider_invoice";
  if (!requestId) {
    throw new HttpsError("invalid-argument", "requestId fehlt.");
  }
  if (!Number.isFinite(actualCostMicros) || actualCostMicros < 0) {
    throw new HttpsError("invalid-argument", "actualCostMicros fehlt oder ist ungueltig.");
  }

  const eventRef = admin.firestore().collection(AI_USAGE_EVENTS_COLLECTION).doc(requestId);
  const reconcileResult = await admin.firestore().runTransaction(async (transaction) => {
    const eventSnapshot = await transaction.get(eventRef);
    if (!eventSnapshot.exists) {
      throw new HttpsError("not-found", "Usage Event wurde nicht gefunden.");
    }
    const event = eventSnapshot.data() || {};
    const dateKey = nonEmptyString(event.dateKey) || aiUsageDateKey();
    const monthKey = nonEmptyString(event.monthKey) || aiUsageMonthKey(dateKey);
    const uid = nonEmptyString(event.uid);
    if (!uid) {
      throw new HttpsError("failed-precondition", "Usage Event ohne uid kann nicht reconciled werden.");
    }

    const estimated = Number(event.estimatedCostMicros) || 0;
    const currentActual = Number(event.actualCostMicros);
    const previousActual = Number.isFinite(currentActual) ? currentActual : null;
    const nextActual = Math.max(0, Math.floor(actualCostMicros));
    const deltaActual = nextActual - (previousActual || 0);
    const varianceMicros = nextActual - estimated;
    const varianceRatio = estimated > 0 ? Number((nextActual / estimated).toFixed(4)) : null;
    const featureKey = aiMetricKey(event.featureClass || "unknown");
    const providerKey = aiMetricKey(event.provider || "unknown");
    const modelKey = aiMetricKey(event.model || "default");
    const planKey = aiMetricKey(event.plan || USER_QUOTA_PLANS.free);

    const usageRef = admin.firestore().doc(`users/${uid}/aiUsage/${dateKey}`);
    const dailyRef = admin.firestore().collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMetricsDocumentId(dateKey));
    const monthlyRef = admin.firestore().collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMonthlyMetricsDocumentId(monthKey));

    const [usageSnapshot, dailySnapshot, monthlySnapshot] = await Promise.all([
      transaction.get(usageRef),
      transaction.get(dailyRef),
      transaction.get(monthlyRef),
    ]);
    const usageData = usageSnapshot.exists ? (usageSnapshot.data() || {}) : {};
    const dailyData = dailySnapshot.exists ? (dailySnapshot.data() || {}) : {};
    const monthlyData = monthlySnapshot.exists ? (monthlySnapshot.data() || {}) : {};

    const nextUsageActual = Math.max(0, (Number(usageData.totalActualCostMicros) || 0) + deltaActual);
    const nextDailyActual = Math.max(0, (Number(dailyData.totalActualCostMicros) || 0) + deltaActual);
    const nextMonthlyActual = Math.max(0, (Number(monthlyData.totalActualCostMicros) || 0) + deltaActual);
    const nextMonthlyVariance = (Number(monthlyData.totalVarianceMicros) || 0) + (varianceMicros - (Number(event.varianceMicros) || 0));
    const reconciledEvents = (Number(monthlyData.reconciledEvents) || 0) + (previousActual == null ? 1 : 0);
    const estimatedEvents = Number(monthlyData.totalRequests) || 0;

    const byProviderVariance = {...(monthlyData.byProviderVarianceMicros || {})};
    byProviderVariance[providerKey] = (Number(byProviderVariance[providerKey]) || 0) +
      (varianceMicros - (Number(event.varianceMicros) || 0));
    const byModelVariance = {...(monthlyData.byModelVarianceMicros || {})};
    byModelVariance[modelKey] = (Number(byModelVariance[modelKey]) || 0) +
      (varianceMicros - (Number(event.varianceMicros) || 0));
    const byFeatureVariance = {...(monthlyData.byFeatureClassVarianceMicros || {})};
    byFeatureVariance[featureKey] = (Number(byFeatureVariance[featureKey]) || 0) +
      (varianceMicros - (Number(event.varianceMicros) || 0));
    const byPlanActual = {...(monthlyData.byPlanActualCostMicros || {})};
    byPlanActual[planKey] = Math.max(0, (Number(byPlanActual[planKey]) || 0) + deltaActual);
    const byProviderActual = {...(monthlyData.byProviderActualCostMicros || {})};
    byProviderActual[providerKey] = Math.max(0, (Number(byProviderActual[providerKey]) || 0) + deltaActual);
    const byModelActual = {...(monthlyData.byModelActualCostMicros || {})};
    byModelActual[modelKey] = Math.max(0, (Number(byModelActual[modelKey]) || 0) + deltaActual);
    const byFeatureActual = {...(monthlyData.byFeatureClassActualCostMicros || {})};
    byFeatureActual[featureKey] = Math.max(0, (Number(byFeatureActual[featureKey]) || 0) + deltaActual);

    transaction.set(eventRef, {
      actualCostMicros: nextActual,
      costStatus: "reconciled",
      reconciliationSource,
      reconciledAt: admin.firestore.FieldValue.serverTimestamp(),
      varianceMicros,
      varianceRatio,
    }, {merge: true});

    transaction.set(usageRef, {
      dateKey,
      totalActualCostMicros: nextUsageActual,
      lastReconciledRequestId: requestId,
      lastReconciledAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(dailyRef, {
      dateKey,
      totalActualCostMicros: nextDailyActual,
      lastReconciledRequestId: requestId,
      lastReconciledAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    transaction.set(monthlyRef, {
      monthKey,
      totalActualCostMicros: nextMonthlyActual,
      totalVarianceMicros: nextMonthlyVariance,
      reconciledEvents,
      reconciliationCoverage: estimatedEvents > 0 ? Number((reconciledEvents / estimatedEvents).toFixed(4)) : 0,
      byProviderVarianceMicros: byProviderVariance,
      byModelVarianceMicros: byModelVariance,
      byFeatureClassVarianceMicros: byFeatureVariance,
      byPlanActualCostMicros: byPlanActual,
      byProviderActualCostMicros: byProviderActual,
      byModelActualCostMicros: byModelActual,
      byFeatureClassActualCostMicros: byFeatureActual,
      lastReconciledAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});

    return {
      requestId,
      estimatedCostMicros: estimated,
      actualCostMicros: nextActual,
      varianceMicros,
      varianceRatio,
      monthKey,
      dailyDateKey: dateKey,
      reconciliationCoverage: estimatedEvents > 0 ? Number((reconciledEvents / estimatedEvents).toFixed(4)) : 0,
    };
  });

  return reconcileResult;
});

exports.recordAiMembershipEvent = onCall({
  region: "us-central1",
  timeoutSeconds: 20,
}, async (request) => {
  await assertCallableSecurity(request, "recordAiMembershipEvent");
  const uid = nonEmptyString(request.auth?.uid);
  if (!uid) {
    throw new HttpsError("unauthenticated", "Anmeldung erforderlich.");
  }

  const eventName = normalizeMembershipEventName(request.data?.eventName || request.data?.event);
  if (!eventName) {
    throw new HttpsError("invalid-argument", "Unbekanntes Membership Event.");
  }

  const createdAtEpochMillis = Date.now();
  const eventId = nonEmptyString(request.data?.eventId) || `mbr_${uid}_${createdAtEpochMillis}_${Math.random().toString(36).slice(2, 8)}`;
  const payload = {
    eventId,
    uid,
    eventName,
    platform: aiMetricKey(nonEmptyString(request.data?.platform) || "unknown", "unknown"),
    reason: normalizeMembershipReason(request.data?.reason),
    plan: normalizeMembershipPlan(request.data?.plan),
    annual: asBoolean(request.data?.annual, false),
    surface: normalizeMembershipSurface(request.data?.surface),
    currentPlan: normalizeMembershipPlan(request.data?.currentPlan),
    source: aiMetricKey(nonEmptyString(request.data?.source) || "client", "client"),
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdAtEpochMillis,
    dateKey: aiUsageDateKey(),
    monthKey: aiUsageMonthKey(),
  };

  await admin.firestore()
      .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
      .doc(eventId)
      .set(payload, {merge: true});

  return {
    status: "recorded",
    eventId,
    eventName,
  };
});

exports.getAiMembershipDashboard = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getAiMembershipDashboard");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Membership-Metriken sehen.");
  }

  const nowMs = Date.now();
  const range7 = rollingWindowRange(7);
  const range30 = rollingWindowRange(30);
  const prev7 = {days: 7, startMs: range7.startMs - (7 * 24 * 60 * 60 * 1000), endMs: range7.startMs};
  const prev30 = {days: 30, startMs: range30.startMs - (30 * 24 * 60 * 60 * 1000), endMs: range30.startMs};
  const queryStart = Math.min(prev30.startMs, prev7.startMs);

  const [membershipEventsSnapshot, usageMonthlySnapshot] = await Promise.all([
    admin.firestore()
        .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
        .where("createdAtEpochMillis", ">=", queryStart)
        .orderBy("createdAtEpochMillis", "desc")
        .limit(10000)
        .get(),
    admin.firestore()
        .collection(AI_USAGE_METRICS_COLLECTION)
        .doc(aiUsageMonthlyMetricsDocumentId(aiUsageMonthKey()))
        .get(),
  ]);
  const docs = membershipEventsSnapshot.docs;
  const summary7 = summarizeMembershipWindow(range7, docs);
  const summary30 = summarizeMembershipWindow(range30, docs);
  const previousSummary7 = summarizeMembershipWindow(prev7, docs);
  const previousSummary30 = summarizeMembershipWindow(prev30, docs);

  const alerts = [];
  if (previousSummary7.cvr > 0 && summary7.cvr < previousSummary7.cvr * 0.8) {
    alerts.push({type: "cvr_drop", severity: "high", detail: "7d CVR liegt mehr als 20% unter der Vorwoche."});
  }
  if (previousSummary30.annualShare > 0 && summary30.annualShare < previousSummary30.annualShare * 0.85) {
    alerts.push({type: "annual_share_drop", severity: "medium", detail: "Annual Share faellt deutlich gegen Vorperiode."});
  }
  if (summary30.creatorTakeRate < 0.2 && summary30.purchaseSuccess >= 10) {
    alerts.push({type: "creator_stagnation", severity: "medium", detail: "Creator Take Rate bleibt niedrig bei aktivem Funnel."});
  }
  if (summary30.rawCounts.upgradeAfterDeny >= 10 && summary30.upgradeAfterDenyCvr < 0.15) {
    alerts.push({type: "deny_high_low_upgrade", severity: "high", detail: "Viele Deny-Upgradesignale, aber schwache Conversion."});
  }
  if (summary7.membershipOpens >= 20 && summary7.purchaseSuccess === 0) {
    alerts.push({type: "many_opens_no_purchases", severity: "high", detail: "Viele Opens in 7d ohne Purchase Success."});
  }
  if (previousSummary7.cancelRate > 0 && summary7.cancelRate > previousSummary7.cancelRate * 1.5) {
    alerts.push({type: "purchase_cancel_spike", severity: "medium", detail: "Cancel Rate ist stark gegen Vorwoche gestiegen."});
  }

  const monthlyUsage = usageMonthlySnapshot.data() || {};
  const byPlanActual = monthlyUsage.byPlanActualCostMicros || monthlyUsage.byPlanCostMicros || {};
  const freeBurnMicros = Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.free)] || 0);
  const proBurnMicros = Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.creator)] || 0);
  const creatorBurnMicros = Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.studio)] || 0);

  return {
    generatedAtEpochMillis: nowMs,
    windows: {
      d7: summary7,
      d30: summary30,
    },
    baselines: {
      previous7d: previousSummary7,
      previous30d: previousSummary30,
    },
    alerts,
    costOverlay: {
      burnByPlanMicros: {
        free: freeBurnMicros,
        pro: proBurnMicros,
        creator: creatorBurnMicros,
      },
      marginStressByPlan: {
        free: freeBurnMicros,
        pro: proBurnMicros,
        creator: creatorBurnMicros,
      },
      creatorProfitabilitySignal: summary30.creatorTakeRate > 0 ? Number((creatorBurnMicros / Math.max(summary30.creatorTakeRate, 0.0001)).toFixed(2)) : null,
      freePlanLoadRatio: (freeBurnMicros + proBurnMicros + creatorBurnMicros) > 0 ?
        Number((freeBurnMicros / (freeBurnMicros + proBurnMicros + creatorBurnMicros)).toFixed(4)) :
        0,
    },
    dataNotes: {
      membershipEventSource: AI_MEMBERSHIP_EVENTS_COLLECTION,
      warning: "Dashboard basiert auf serverseitig gespiegelten Membership Events.",
    },
  };
});

exports.getAiMembershipDashboardTimeseries = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getAiMembershipDashboardTimeseries");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Membership-Trends sehen.");
  }

  const requestedWindow = Number(request.data?.windowDays);
  const normalizedWindow = [7, 30, 90].includes(requestedWindow) ? requestedWindow : 90;
  const range90 = rollingWindowRange(90);
  const range30 = rollingWindowRange(30);
  const range7 = rollingWindowRange(7);
  const prev30 = {days: 30, startMs: range30.startMs - (30 * 24 * 60 * 60 * 1000), endMs: range30.startMs};
  const prev7 = {days: 7, startMs: range7.startMs - (7 * 24 * 60 * 60 * 1000), endMs: range7.startMs};
  const queryStart = Math.min(range90.startMs, prev30.startMs, prev7.startMs);

  const membershipEventsSnapshot = await admin.firestore()
      .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
      .where("createdAtEpochMillis", ">=", queryStart)
      .orderBy("createdAtEpochMillis", "desc")
      .limit(15000)
      .get();
  const docs = membershipEventsSnapshot.docs;

  const summary7 = summarizeMembershipWindow(range7, docs);
  const summary30 = summarizeMembershipWindow(range30, docs);
  const previousSummary7 = summarizeMembershipWindow(prev7, docs);
  const previousSummary30 = summarizeMembershipWindow(prev30, docs);
  const series7 = buildMembershipDailySeries(7, docs);
  const series30 = buildMembershipDailySeries(30, docs);
  const series90 = buildMembershipDailySeries(90, docs);
  const activeSeries = normalizedWindow === 7 ? series7 : normalizedWindow === 30 ? series30 : series90;
  const trendSignals = buildTimeseriesTrendSignals(series30, summary7, previousSummary7);

  return {
    generatedAtEpochMillis: Date.now(),
    selectedWindowDays: normalizedWindow,
    timeseries: {
      d7: series7,
      d30: series30,
      d90: series90,
      selected: activeSeries,
    },
    trendSignals,
    compareWindows: {
      last7dVsPrevious7d: compareWindowSummary(summary7, previousSummary7),
      last30dVsPrevious30d: compareWindowSummary(summary30, previousSummary30),
    },
    summaries: {
      d7: summary7,
      d30: summary30,
      previous7d: previousSummary7,
      previous30d: previousSummary30,
    },
    dataNotes: {
      membershipEventSource: AI_MEMBERSHIP_EVENTS_COLLECTION,
      coverageDays: 90,
    },
  };
});

exports.getAiMembershipTrendRecommendations = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getAiMembershipTrendRecommendations");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Revenue-Ops Empfehlungen sehen.");
  }

  const range90 = rollingWindowRange(90);
  const range30 = rollingWindowRange(30);
  const range7 = rollingWindowRange(7);
  const prev30 = {days: 30, startMs: range30.startMs - (30 * 24 * 60 * 60 * 1000), endMs: range30.startMs};
  const prev7 = {days: 7, startMs: range7.startMs - (7 * 24 * 60 * 60 * 1000), endMs: range7.startMs};
  const queryStart = Math.min(range90.startMs, prev30.startMs, prev7.startMs);

  const [membershipEventsSnapshot, usageMonthlySnapshot, lifecycleSnapshot, runtimeSettings] = await Promise.all([
    admin.firestore()
        .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
        .where("createdAtEpochMillis", ">=", queryStart)
        .orderBy("createdAtEpochMillis", "desc")
        .limit(15000)
        .get(),
    admin.firestore()
        .collection(AI_USAGE_METRICS_COLLECTION)
        .doc(aiUsageMonthlyMetricsDocumentId(aiUsageMonthKey()))
        .get(),
    admin.firestore()
        .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
        .where("generatedAt", ">=", queryStart)
        .orderBy("generatedAt", "desc")
        .limit(5000)
        .get(),
    loadAiRuntimeSettings(),
  ]);
  const docs = membershipEventsSnapshot.docs;
  const summary7 = summarizeMembershipWindow(range7, docs);
  const summary30 = summarizeMembershipWindow(range30, docs);
  const previousSummary7 = summarizeMembershipWindow(prev7, docs);
  const previousSummary30 = summarizeMembershipWindow(prev30, docs);
  const series30 = buildMembershipDailySeries(30, docs);
  const trendSignals = buildTimeseriesTrendSignals(series30, summary7, previousSummary7);
  const compareWindows = {
    last7dVsPrevious7d: compareWindowSummary(summary7, previousSummary7),
    last30dVsPrevious30d: compareWindowSummary(summary30, previousSummary30),
  };

  const alerts = [];
  if (previousSummary7.cvr > 0 && summary7.cvr < previousSummary7.cvr * 0.8) {
    alerts.push({type: "cvr_drop", severity: "high"});
  }
  if (previousSummary30.annualShare > 0 && summary30.annualShare < previousSummary30.annualShare * 0.85) {
    alerts.push({type: "annual_share_drop", severity: "medium"});
  }
  if (summary30.creatorTakeRate < 0.2 && summary30.purchaseSuccess >= 10) {
    alerts.push({type: "creator_stagnation", severity: "medium"});
  }
  if (summary30.rawCounts.upgradeAfterDeny >= 10 && summary30.upgradeAfterDenyCvr < 0.15) {
    alerts.push({type: "deny_high_low_upgrade", severity: "high"});
  }
  if (summary7.membershipOpens >= 20 && summary7.purchaseSuccess === 0) {
    alerts.push({type: "many_opens_no_purchases", severity: "high"});
  }
  if (previousSummary7.cancelRate > 0 && summary7.cancelRate > previousSummary7.cancelRate * 1.5) {
    alerts.push({type: "purchase_cancel_spike", severity: "medium"});
  }

  const monthlyUsage = usageMonthlySnapshot.data() || {};
  const byPlanActual = monthlyUsage.byPlanActualCostMicros || monthlyUsage.byPlanCostMicros || {};
  const costOverlay = {
    burnByPlanMicros: {
      free: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.free)] || 0),
      pro: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.creator)] || 0),
      creator: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.studio)] || 0),
    },
  };
  const totalBurn = costOverlay.burnByPlanMicros.free + costOverlay.burnByPlanMicros.pro + costOverlay.burnByPlanMicros.creator;
  costOverlay.freePlanLoadRatio = totalBurn > 0 ?
    Number((costOverlay.burnByPlanMicros.free / totalBurn).toFixed(4)) :
    0;

  const baseRecommendations = buildMembershipTrendRecommendations({
    summary7,
    summary30,
    previousSummary7,
    previousSummary30,
    trendSignals,
    compareWindows,
    alerts,
    costOverlay,
  });
  const lifecycleRows = lifecycleSnapshot.docs.map((doc) => ({lifecycleId: doc.id, ...(doc.data() || {})}));
  const recommendations = applyRecommendationHygiene({
    recommendations: baseRecommendations,
    lifecycleRows,
    hygieneConfig: runtimeSettings.membershipHygiene || DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene,
    nowMs: Date.now(),
  });
  await appendProposedRecommendationsToTimeline(
      recommendations.filter((entry) => entry.suppressed !== true),
      Date.now(),
  );

  return {
    generatedAtEpochMillis: Date.now(),
    recommendations,
    diagnostics: {
      trendSignals,
      compareWindows,
      alerts,
      summary7,
      summary30,
      previousSummary7,
      previousSummary30,
      costOverlay,
      activeHygieneParameters: runtimeSettings.membershipHygiene || DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene,
    },
    guardrails: {
      autoApply: false,
      darkPatternsAllowed: false,
      recommendationOnly: true,
    },
  };
});

exports.simulateAiMembershipOpsImpact = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "simulateAiMembershipOpsImpact");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Impact-Simulationen sehen.");
  }

  const horizonDays = parseHorizonDays(request.data?.timeHorizonDays);
  const requestedRecommendationIds = Array.isArray(request.data?.recommendationIds) ?
    request.data.recommendationIds.map((value) => nonEmptyString(value)).filter(Boolean) :
    [];

  const range90 = rollingWindowRange(90);
  const range30 = rollingWindowRange(30);
  const range7 = rollingWindowRange(7);
  const prev30 = {days: 30, startMs: range30.startMs - (30 * 24 * 60 * 60 * 1000), endMs: range30.startMs};
  const prev7 = {days: 7, startMs: range7.startMs - (7 * 24 * 60 * 60 * 1000), endMs: range7.startMs};
  const queryStart = Math.min(range90.startMs, prev30.startMs, prev7.startMs);

  const [membershipEventsSnapshot, usageMonthlySnapshot] = await Promise.all([
    admin.firestore()
        .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
        .where("createdAtEpochMillis", ">=", queryStart)
        .orderBy("createdAtEpochMillis", "desc")
        .limit(15000)
        .get(),
    admin.firestore()
        .collection(AI_USAGE_METRICS_COLLECTION)
        .doc(aiUsageMonthlyMetricsDocumentId(aiUsageMonthKey()))
        .get(),
  ]);
  const docs = membershipEventsSnapshot.docs;
  const summary7 = summarizeMembershipWindow(range7, docs);
  const summary30 = summarizeMembershipWindow(range30, docs);
  const previousSummary7 = summarizeMembershipWindow(prev7, docs);
  const previousSummary30 = summarizeMembershipWindow(prev30, docs);
  const series30 = buildMembershipDailySeries(30, docs);
  const trendSignals = buildTimeseriesTrendSignals(series30, summary7, previousSummary7);
  const compareWindows = {
    last7dVsPrevious7d: compareWindowSummary(summary7, previousSummary7),
    last30dVsPrevious30d: compareWindowSummary(summary30, previousSummary30),
  };
  const alerts = [];
  if (previousSummary7.cvr > 0 && summary7.cvr < previousSummary7.cvr * 0.8) {
    alerts.push({type: "cvr_drop", severity: "high"});
  }
  if (previousSummary30.annualShare > 0 && summary30.annualShare < previousSummary30.annualShare * 0.85) {
    alerts.push({type: "annual_share_drop", severity: "medium"});
  }
  if (summary30.creatorTakeRate < 0.2 && summary30.purchaseSuccess >= 10) {
    alerts.push({type: "creator_stagnation", severity: "medium"});
  }
  if (summary30.rawCounts.upgradeAfterDeny >= 10 && summary30.upgradeAfterDenyCvr < 0.15) {
    alerts.push({type: "deny_high_low_upgrade", severity: "high"});
  }
  if (summary7.membershipOpens >= 20 && summary7.purchaseSuccess === 0) {
    alerts.push({type: "many_opens_no_purchases", severity: "high"});
  }
  if (previousSummary7.cancelRate > 0 && summary7.cancelRate > previousSummary7.cancelRate * 1.5) {
    alerts.push({type: "purchase_cancel_spike", severity: "medium"});
  }
  const monthlyUsage = usageMonthlySnapshot.data() || {};
  const byPlanActual = monthlyUsage.byPlanActualCostMicros || monthlyUsage.byPlanCostMicros || {};
  const costOverlay = {
    burnByPlanMicros: {
      free: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.free)] || 0),
      pro: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.creator)] || 0),
      creator: Number(byPlanActual[aiMetricKey(USER_QUOTA_PLANS.studio)] || 0),
    },
  };
  const totalBurn = costOverlay.burnByPlanMicros.free + costOverlay.burnByPlanMicros.pro + costOverlay.burnByPlanMicros.creator;
  costOverlay.freePlanLoadRatio = totalBurn > 0 ?
    Number((costOverlay.burnByPlanMicros.free / totalBurn).toFixed(4)) :
    0;

  const recommendations = buildMembershipTrendRecommendations({
    summary7,
    summary30,
    previousSummary7,
    previousSummary30,
    trendSignals,
    compareWindows,
    alerts,
    costOverlay,
  });
  const filteredRecommendations = requestedRecommendationIds.length > 0 ?
    recommendations.filter((item) => requestedRecommendationIds.includes(item.id)) :
    recommendations;

  const simulations = filteredRecommendations.map((recommendation) => simulateRecommendationImpact(recommendation, {
    eventCount: docs.length,
    horizonDays,
    summary7,
    summary30,
    trendSignals,
    compareWindows,
  })).sort((a, b) => (Number(b.priorityScore) || 0) - (Number(a.priorityScore) || 0));
  const topNextActions = simulations.slice(0, 3).map((entry) => ({
    recommendationId: entry.recommendationId,
    priorityScore: entry.priorityScore,
    expectedTimeToSignalDays: entry.expectedTimeToSignalDays,
    rationale: entry.rationale,
  }));

  return {
    generatedAtEpochMillis: Date.now(),
    timeHorizonDays: horizonDays,
    simulations,
    topNextActions,
    scoringModel: {
      formula: "impact x confidence x low complexity x low risk",
      conservativeRangesOnly: true,
      noAutoExecution: true,
    },
    diagnostics: {
      recommendationCount: recommendations.length,
      simulatedCount: simulations.length,
      eventCount: docs.length,
      trendSignals,
      compareWindows,
    },
  };
});

exports.startMembershipExperiment = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "startMembershipExperiment");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Experimente starten.");
  }

  const recommendationId = nonEmptyString(request.data?.recommendationId);
  const recommendationType = aiMetricKey(nonEmptyString(request.data?.recommendationType) || "unknown");
  if (!recommendationId) {
    throw new HttpsError("invalid-argument", "recommendationId fehlt.");
  }

  const lifecycleId = nonEmptyString(request.data?.lifecycleId) || `lifecycle_${recommendationId}_${Date.now()}`;
  const now = Date.now();
  const expectedImpactInput = request.data?.expectedImpact || {};
  const expectedImpact = {
    cvrRange: normalizeImpactRange(expectedImpactInput.cvrRange, 0, 0),
    annualShareRange: normalizeImpactRange(expectedImpactInput.annualShareRange, 0, 0),
    creatorTakeRateRange: normalizeImpactRange(expectedImpactInput.creatorTakeRateRange, 0, 0),
    cancelReductionRange: normalizeImpactRange(expectedImpactInput.cancelReductionRange, 0, 0),
    riskLevel: nonEmptyString(expectedImpactInput.riskLevel) || "medium",
    executionComplexity: nonEmptyString(expectedImpactInput.executionComplexity) || "medium",
    expectedTimeToSignalDays: Number.isFinite(Number(expectedImpactInput.expectedTimeToSignalDays)) ?
      Math.max(1, Math.floor(Number(expectedImpactInput.expectedTimeToSignalDays))) :
      14,
  };
  const confidenceAtStart = Number.isFinite(Number(request.data?.confidenceAtStart)) ?
    Math.max(0, Math.min(1, Number(request.data.confidenceAtStart))) :
    0.35;

  const payload = {
    recommendationId,
    recommendationType,
    startedAt: admin.firestore.FieldValue.serverTimestamp(),
    startedAtEpochMillis: now,
    completedAt: null,
    status: "active",
    expectedImpact,
    actualImpact: null,
    confidenceAtStart,
    simulationAccuracy: null,
    notes: nonEmptyString(request.data?.notes) || "",
    ownerAction: nonEmptyString(request.data?.ownerAction) || "start_experiment",
    generatedAt: now,
    affectedPlans: Array.isArray(request.data?.affectedPlans) ? request.data.affectedPlans : [],
    affectedSurfaces: Array.isArray(request.data?.affectedSurfaces) ? request.data.affectedSurfaces : [],
    experimentMeta: {
      hypothesis: nonEmptyString(request.data?.hypothesis) || "",
      experimentDesign: nonEmptyString(request.data?.experimentDesign) || "",
      observedWindowDays: Number.isFinite(Number(request.data?.observedWindowDays)) ?
        Math.max(1, Math.floor(Number(request.data.observedWindowDays))) :
        expectedImpact.expectedTimeToSignalDays,
    },
  };

  await admin.firestore()
      .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
      .doc(lifecycleId)
      .set(payload, {merge: true});

  return {
    status: "active",
    lifecycleId,
    recommendationId,
    recommendationType,
    confidenceAtStart,
    generatedAt: now,
  };
});

exports.completeMembershipExperiment = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "completeMembershipExperiment");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Experimente abschliessen.");
  }

  const lifecycleId = nonEmptyString(request.data?.lifecycleId);
  if (!lifecycleId) {
    throw new HttpsError("invalid-argument", "lifecycleId fehlt.");
  }

  const lifecycleRef = admin.firestore()
      .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
      .doc(lifecycleId);
  const snapshot = await lifecycleRef.get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Lifecycle-Eintrag nicht gefunden.");
  }
  const existing = snapshot.data() || {};
  const expectedImpact = existing.expectedImpact || {};
  const actualImpact = normalizeActualImpact(request.data?.actualImpact || {});
  const simulationAccuracy = simulationAccuracyFromExpectedActual(expectedImpact, actualImpact);
  const status = "completed";

  await lifecycleRef.set({
    completedAt: admin.firestore.FieldValue.serverTimestamp(),
    completedAtEpochMillis: Date.now(),
    status,
    actualImpact,
    simulationAccuracy,
    notes: nonEmptyString(request.data?.notes) || nonEmptyString(existing.notes) || "",
    ownerAction: nonEmptyString(request.data?.ownerAction) || "complete_experiment",
  }, {merge: true});

  return {
    lifecycleId,
    status,
    recommendationId: nonEmptyString(existing.recommendationId) || "",
    actualImpact,
    simulationAccuracy,
    success: actualImpact.success,
    generatedAt: Date.now(),
  };
});

exports.rejectMembershipRecommendation = onCall({
  region: "us-central1",
  timeoutSeconds: 20,
}, async (request) => {
  await assertCallableSecurity(request, "rejectMembershipRecommendation");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Empfehlungen verwerfen.");
  }

  const lifecycleId = nonEmptyString(request.data?.lifecycleId) ||
    `lifecycle_reject_${nonEmptyString(request.data?.recommendationId) || "unknown"}_${Date.now()}`;
  const recommendationId = nonEmptyString(request.data?.recommendationId) || "";
  if (!recommendationId) {
    throw new HttpsError("invalid-argument", "recommendationId fehlt.");
  }

  const payload = {
    recommendationId,
    recommendationType: aiMetricKey(nonEmptyString(request.data?.recommendationType) || "unknown"),
    startedAt: null,
    completedAt: admin.firestore.FieldValue.serverTimestamp(),
    completedAtEpochMillis: Date.now(),
    status: "rejected",
    expectedImpact: request.data?.expectedImpact || null,
    actualImpact: null,
    confidenceAtStart: Number.isFinite(Number(request.data?.confidenceAtStart)) ?
      Math.max(0, Math.min(1, Number(request.data.confidenceAtStart))) :
      null,
    simulationAccuracy: null,
    notes: nonEmptyString(request.data?.notes) || "",
    ownerAction: nonEmptyString(request.data?.ownerAction) || "reject_recommendation",
    generatedAt: Date.now(),
    affectedPlans: Array.isArray(request.data?.affectedPlans) ? request.data.affectedPlans : [],
    affectedSurfaces: Array.isArray(request.data?.affectedSurfaces) ? request.data.affectedSurfaces : [],
  };

  await admin.firestore()
      .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
      .doc(lifecycleId)
      .set(payload, {merge: true});

  return {
    status: "rejected",
    lifecycleId,
    recommendationId,
    generatedAt: Date.now(),
  };
});

exports.getMembershipLearningInsights = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getMembershipLearningInsights");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Learning Insights sehen.");
  }

  const lookbackDays = Math.max(30, Math.min(365, Number(request.data?.lookbackDays) || 180));
  const startMs = Date.now() - (lookbackDays * 24 * 60 * 60 * 1000);
  const lifecycleSnapshot = await admin.firestore()
      .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
      .where("generatedAt", ">=", startMs)
      .orderBy("generatedAt", "desc")
      .limit(5000)
      .get();
  const insights = summarizeLearningInsights(lifecycleSnapshot.docs);
  const dataStrength = insights.completedExperiments >= 20 ? "strong" :
    insights.completedExperiments >= 8 ? "moderate" : "weak";
  const recommendedConfidenceMode = dataStrength === "strong" ? "normal" :
    dataStrength === "moderate" ? "conservative" : "very_conservative";

  return {
    generatedAtEpochMillis: Date.now(),
    lookbackDays,
    dataStrength,
    recommendedConfidenceMode,
    insights,
    transparency: {
      autoRollout: false,
      fakeAttribution: false,
      note: "Insights basieren auf Lifecycle-Eintraegen und beobachteten Experiment-Resultaten.",
    },
  };
});

exports.getMembershipLifecycleTimeline = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getMembershipLifecycleTimeline");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf die Membership Timeline sehen.");
  }

  const rangeDays = parseTimelineRange(request.data?.range);
  const startMs = Date.now() - (rangeDays * 24 * 60 * 60 * 1000);
  const filters = {
    types: request.data?.types,
    plans: request.data?.plans,
    severities: request.data?.severities,
  };

  const [lifecycleSnapshot, membershipEventsSnapshot] = await Promise.all([
    admin.firestore()
        .collection(AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION)
        .where("generatedAt", ">=", startMs)
        .orderBy("generatedAt", "desc")
        .limit(5000)
        .get(),
    admin.firestore()
        .collection(AI_MEMBERSHIP_EVENTS_COLLECTION)
        .where("createdAtEpochMillis", ">=", startMs)
        .orderBy("createdAtEpochMillis", "desc")
        .limit(5000)
        .get(),
  ]);

  const lifecycleRows = lifecycleSnapshot.docs.map((doc) => ({
    lifecycleId: doc.id,
    ...(doc.data() || {}),
  }));
  const lifecycleEntries = buildLifecycleTimelineEntries(lifecycleRows);

  const range7 = rollingWindowRange(7);
  const prev7 = {days: 7, startMs: range7.startMs - (7 * 24 * 60 * 60 * 1000), endMs: range7.startMs};
  const docs = membershipEventsSnapshot.docs;
  const summary7 = summarizeMembershipWindow(range7, docs);
  const previousSummary7 = summarizeMembershipWindow(prev7, docs);
  const compare = compareWindowSummary(summary7, previousSummary7);
  const trendSignals = buildTimeseriesTrendSignals(buildMembershipDailySeries(30, docs), summary7, previousSummary7);

  const systemEntries = [];
  const now = Date.now();
  if (Math.abs(Number(compare.cvrDelta) || 0) >= 0.03) {
    systemEntries.push({
      id: `timeline_kpi_shift_${now}`,
      date: now,
      dateKey: utcDateKeyFromEpochMillis(now),
      type: "significant_kpi_shift",
      title: "Significant KPI shift",
      summary: `CVR shift detected (${compare.cvrDelta}).`,
      ownerAction: "",
      expectedImpact: null,
      actualImpact: {cvrDelta: compare.cvrDelta},
      confidenceAtStart: null,
      learnings: "",
      recommendationId: "",
      recommendationType: "system_signal",
      lifecycleId: "",
      severity: Math.abs(compare.cvrDelta) >= 0.05 ? "high" : "medium",
      affectedPlans: ["free", "creator", "studio"],
      affectedSurfaces: [],
      links: {},
      rerunDraft: null,
    });
  }
  if ((trendSignals.cancelSpikeDates || []).length > 0) {
    systemEntries.push({
      id: `timeline_alert_cancel_${now}`,
      date: now - 1,
      dateKey: utcDateKeyFromEpochMillis(now - 1),
      type: "alert_triggered",
      title: "Alert triggered",
      summary: "Cancel spike detected in Membership funnel.",
      ownerAction: "",
      expectedImpact: null,
      actualImpact: {cancelSpikeDates: trendSignals.cancelSpikeDates},
      confidenceAtStart: null,
      learnings: "",
      recommendationId: "",
      recommendationType: "system_alert",
      lifecycleId: "",
      severity: "medium",
      affectedPlans: ["creator", "studio"],
      affectedSurfaces: [],
      links: {},
      rerunDraft: null,
    });
  }

  const filtered = applyTimelineFilters(
      [...lifecycleEntries, ...systemEntries].sort((a, b) => (Number(b.date) || 0) - (Number(a.date) || 0)),
      filters,
  );

  return {
    generatedAtEpochMillis: now,
    range: request.data?.range || "30d",
    entries: filtered,
    counts: {
      total: filtered.length,
      lifecycle: lifecycleEntries.length,
      system: systemEntries.length,
    },
  };
});

exports.setMembershipHygieneControls = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "setMembershipHygieneControls");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Hygiene Controls verwalten.");
  }

  const resetToDefaults = request.data?.resetToDefaults === true;
  const runtimeRef = admin.firestore().collection(AI_RUNTIME_CONFIG_COLLECTION).doc(AI_RUNTIME_CONFIG_DOCUMENT);
  const runtimeSnapshot = await runtimeRef.get();
  const current = resolveAiRuntimeSettings(runtimeSnapshot.data() || {});
  const nextHygiene = resetToDefaults ?
    DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene :
    resolveMembershipHygieneSettings(request.data?.membershipHygiene || current.membershipHygiene);

  await runtimeRef.set({
    membershipHygiene: nextHygiene,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  return {
    status: "ok",
    membershipHygiene: nextHygiene,
    profile: membershipHygieneProfile(nextHygiene),
  };
});

exports.getMembershipHygieneControls = onCall({
  region: "us-central1",
  timeoutSeconds: 20,
}, async (request) => {
  await assertCallableSecurity(request, "getMembershipHygieneControls");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Hygiene Controls sehen.");
  }
  const runtime = await loadAiRuntimeSettings();
  const hygiene = runtime.membershipHygiene || DEFAULT_AI_RUNTIME_SETTINGS.membershipHygiene;
  return {
    membershipHygiene: hygiene,
    profile: membershipHygieneProfile(hygiene),
  };
});

exports.getAiProfitDashboard = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getAiProfitDashboard");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf Profit-Metriken sehen.");
  }

  const dateKey = aiUsageDateKey();
  const monthKey = aiUsageMonthKey(dateKey);
  const [dailySnapshot, monthlySnapshot, burnSnapshot] = await Promise.all([
    admin.firestore().collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMetricsDocumentId(dateKey)).get(),
    admin.firestore().collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMonthlyMetricsDocumentId(monthKey)).get(),
    admin.firestore().collection(AI_GUARDRAILS_COLLECTION).doc(aiDailyBurnDocumentId(dateKey)).get(),
  ]);
  const daily = dailySnapshot.data() || {};
  const monthly = monthlySnapshot.data() || {};
  const burn = burnSnapshot.data() || {};

  const recentDeniedSnapshot = await admin.firestore()
      .collection(AI_USAGE_EVENTS_COLLECTION)
      .where("denied", "==", true)
      .orderBy("createdAtEpochMillis", "desc")
      .limit(300)
      .get();
  const deniedByReason = {};
  for (const doc of recentDeniedSnapshot.docs) {
    const denyReason = aiMetricKey(doc.data()?.denyReason || "unknown");
    deniedByReason[denyReason] = (Number(deniedByReason[denyReason]) || 0) + 1;
  }

  const topMarginStressSignals = [
    ...collectTopEntries(monthly.byProviderVarianceMicros || {}, 3).map((entry) => ({
      type: "provider_variance",
      key: entry.key,
      varianceMicros: entry.value,
    })),
    ...collectTopEntries(monthly.byModelVarianceMicros || {}, 3).map((entry) => ({
      type: "model_variance",
      key: entry.key,
      varianceMicros: entry.value,
    })),
    ...collectTopEntries(monthly.byFeatureClassVarianceMicros || {}, 3).map((entry) => ({
      type: "feature_variance",
      key: entry.key,
      varianceMicros: entry.value,
    })),
  ].sort((a, b) => b.varianceMicros - a.varianceMicros).slice(0, 8);

  const currentRuntime = await loadAiRuntimeSettings();
  const dailyBurnCapMicros = Number(currentRuntime.costGuardrails.dailyBurnCapMicros) || 0;
  const currentDailyBurnMicros = Number(burn.totalEstimatedCostMicros || 0);

  return {
    dateKey,
    monthKey,
    freeBurnMicros: Number(monthly.byPlanActualCostMicros?.[aiMetricKey(USER_QUOTA_PLANS.free)] || 0),
    proBurnMicros: Number(monthly.byPlanActualCostMicros?.[aiMetricKey(USER_QUOTA_PLANS.creator)] || 0),
    creatorBurnMicros: Number(monthly.byPlanActualCostMicros?.[aiMetricKey(USER_QUOTA_PLANS.studio)] || 0),
    burnByFeatureClass: monthly.byFeatureClassActualCostMicros || monthly.byFeatureClassCostMicros || {},
    burnByProvider: monthly.byProviderActualCostMicros || monthly.byProviderCostMicros || {},
    burnByModel: monthly.byModelActualCostMicros || monthly.byModelCostMicros || {},
    deniedRequestsByReason: deniedByReason,
    topMarginStressSignals,
    reconciliationCoverage: Number(monthly.reconciliationCoverage || 0),
    currentDailyBurnMicros,
    dailyBurnCapMicros,
    burnCapUsageRatio: dailyBurnCapMicros > 0 ? Number((currentDailyBurnMicros / dailyBurnCapMicros).toFixed(4)) : null,
    dailyEstimatedCostMicros: Number(daily.totalEstimatedCostMicros || 0),
    dailyActualCostMicros: Number(daily.totalActualCostMicros || 0),
    monthlyEstimatedCostMicros: Number(monthly.totalEstimatedCostMicros || 0),
    monthlyActualCostMicros: Number(monthly.totalActualCostMicros || 0),
    monthlyVarianceMicros: Number(monthly.totalVarianceMicros || 0),
  };
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
    provider: "google_vertex",
    model: "gemini-2.5-flash-lite",
    eventType: "bot_text_generation",
    sourceRoute: "callable.generateAiText",
    functionName: "generateAiText",
    featureClass: AI_FEATURE_CLASSES.text,
    resultType: "text",
    requestWeight: 1,
    estimatedCostMicros: 12_000,
    requestId: nonEmptyString(request.data?.requestId) || "",
  });
  const reply = await generateAiTextReply({
    prompt: input.prompt,
    mode: input.mode,
  });

  return {
    reply,
    mode: input.mode,
    historyRetentionDays: usage.historyRetentionDays,
    usage: {
      kind: usage.kind,
      featureClass: usage.featureClass,
      remainingForKind: usage.remainingForKind,
      limitForKind: usage.limitForKind,
      warningLevel: usage.warningLevel || "ok",
      guardrailHints: usage.guardrailHints || {},
      effectiveEntitlement: usage.effectiveEntitlement || null,
      decision: usage.decision || null,
    },
  };
});

exports.generateAiVisual = onCall({
  region: "us-central1",
  timeoutSeconds: 120,
}, async (request) => {
  await assertCallableSecurity(request, "generateAiVisual");
  const input = parseCallableInput(
      aiVisualRequestSchema,
      request.data,
      "Die Visual-Anfrage konnte so nicht gestartet werden.",
  );
  try {
    const usage = await authorizeAiUsage({
      auth: request.auth,
      kind: AI_USAGE_KINDS.visual,
      provider: "google_vertex",
      eventType: "bot_visual_generation",
      sourceRoute: "callable.generateAiVisual",
      functionName: "generateAiVisual",
      featureClass: AI_FEATURE_CLASSES.image,
      resultType: "image",
      requestWeight: 2,
      estimatedCostMicros: 95_000,
      requestId: nonEmptyString(request.data?.requestId) || "",
    });
    const visual = await generateAiVisualResult(input.prompt);

    return {
      ...visual,
      historyRetentionDays: usage.historyRetentionDays,
      usage: {
        kind: usage.kind,
        featureClass: usage.featureClass,
        remainingForKind: usage.remainingForKind,
        limitForKind: usage.limitForKind,
        warningLevel: usage.warningLevel || "ok",
        guardrailHints: usage.guardrailHints || {},
        effectiveEntitlement: usage.effectiveEntitlement || null,
        decision: usage.decision || null,
      },
    };
  } catch (error) {
    if (error instanceof HttpsError) {
      throw error;
    }

    logger.error("generateAiVisual failed unexpectedly.", {
      uid: request.auth?.uid || null,
      promptLength: input.prompt.length,
      error: error instanceof Error ? error.message : `${error}`,
    });
    throw new HttpsError(
        "internal",
        "Der Visual-Server konnte das Bild gerade nicht erzeugen. Bitte direkt noch einmal versuchen.",
    );
  }
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

function buildGrokChatMessages({systemInstruction, workspaceContext, history, prompt, mode}) {
  const framework = responseFrameworkHint(mode, prompt);
  const systemParts = [
    nonEmptyString(systemInstruction) || "",
    workspaceContext && workspaceContext.trim() ?
      `Workspace-Kontext:\n${workspaceContext.trim()}` :
      "",
    framework ? `Antwort-Rahmen:\n${framework}` : "",
  ].filter(Boolean);

  const systemContent = systemParts.join("\n\n").trim().slice(0, 12000);
  const messages = [];

  if (Array.isArray(history)) {
    for (const turn of history) {
      const role = turn?.role === "assistant" ? "assistant" : "user";
      const text = nonEmptyString(turn?.text)?.trim();
      if (!text) {
        continue;
      }
      messages.push({
        role,
        content: text.slice(0, 4000),
      });
    }
  }

  messages.push({
    role: "user",
    content: nonEmptyString(prompt)?.trim()?.slice(0, 4000) || "",
  });

  return {
    system: systemContent,
    messages,
  };
}

async function runGrokAgent({
  input,
  systemInstruction,
  workspaceContext,
  apiKey,
}) {
  const {system, messages} = buildGrokChatMessages({
    systemInstruction,
    workspaceContext,
    history: input.history,
    prompt: input.prompt,
    mode: input.mode,
  });

  const payloadMessages = [];
  if (system) {
    payloadMessages.push({role: "system", content: system});
  }
  payloadMessages.push(...messages);

  const response = await fetch(XAI_CHAT_COMPLETIONS_URL, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${apiKey}`,
    },
    body: JSON.stringify({
      model: GROK_AGENT_MODEL,
      temperature: 0.65,
      max_tokens: 2048,
      messages: payloadMessages,
    }),
  });

  const rawBody = await response.text();
  if (!response.ok) {
    logger.error("Grok chat completion failed.", {
      status: response.status,
      bodyPreview: rawBody.slice(0, 800),
    });
    throw new HttpsError(
        "internal",
        "Grok Agent konnte keine Antwort erzeugen.",
    );
  }

  let data;
  try {
    data = JSON.parse(rawBody);
  } catch (error) {
    logger.error("Grok response was not JSON.", {bodyPreview: rawBody.slice(0, 400)});
    throw new HttpsError("internal", "Grok Agent lieferte eine ungueltige Antwort.");
  }

  const text = nonEmptyString(data?.choices?.[0]?.message?.content)?.trim();
  if (!text) {
    logger.error("Grok response missing content.", {
      keys: data && typeof data === "object" ? Object.keys(data) : [],
    });
    throw new HttpsError("internal", "Grok hat keine Antwort geliefert.");
  }

  return text;
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
  secrets: [manusApiKey, xaiApiKey],
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
    provider: AI_AGENT_PROVIDERS.grok,
    eventType: "agent_generation",
    sourceRoute: "callable.skydownAgent",
    functionName: "skydownAgent",
    featureClass: input.executeAutomation ? AI_FEATURE_CLASSES.workflow : AI_FEATURE_CLASSES.agent,
    resultType: input.executeAutomation ? "workflow" : "text",
    requestWeight: input.executeAutomation ? 3 : 2,
    estimatedCostMicros: input.executeAutomation ? 260_000 : 110_000,
    requestId: nonEmptyString(request.data?.requestId) || "",
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
  let agentProvider = runtimeSettings.agentProvider;
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

  if (!reply && runtimeSettings.agentProvider === AI_AGENT_PROVIDERS.grok) {
    const apiKey = (xaiApiKey.value() || "").trim();
    if (apiKey) {
      try {
        reply = await runGrokAgent({
          input,
          systemInstruction: effectiveAgentSystemInstruction,
          workspaceContext,
          apiKey,
        });
        agentProvider = AI_AGENT_PROVIDERS.grok;
      } catch (error) {
        if (runtimeSettings.fallbackAgentProvider === AI_AGENT_PROVIDERS.gemini) {
          providerFallbackUsed = true;
          providerNotice = providerNotice ||
            "Grok war nicht verfuegbar. Antwort wurde ueber Gemini erstellt.";
          logger.warn("Grok agent failed. Falling back to Gemini.", {
            uid: request.auth?.uid || null,
            error: error instanceof Error ? error.message : `${error}`,
          });
        } else {
          throw error;
        }
      }
    } else {
      providerFallbackUsed = true;
      providerNotice = providerNotice ||
        "Grok-API-Key fehlt serverseitig. Antwort wurde ueber Gemini erstellt.";
      logger.warn("Grok selected but XAI_API_KEY is empty. Falling back to Gemini.", {
        uid: request.auth?.uid || null,
      });
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

  const agentRunId = await persistAgentRunSummary({
    uid: request.auth.uid,
    mode: input.mode,
    agentProvider,
    providerFallbackUsed,
    automation,
    promptText: input.prompt,
    replyText: reply,
  });

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
    agentRunId: agentRunId || "",
    resultType: automation.triggered === true || automation.attempted === true ? "workflow" : "text",
    results: [
      {
        type: "text",
        text: reply,
      },
      ...(automation.triggered === true || automation.attempted === true ? [{
        type: "workflow",
        workflowName: nonEmptyString(automation.workflowName) || "n8n Workflow",
        status: automation.triggered === true ? "queued" : "failed",
        summary: nonEmptyString(automation.message) || (
          automation.triggered === true ? "Workflow wurde gestartet." : "Workflow konnte nicht gestartet werden."
        ),
        runId: agentRunId || "",
      }] : []),
    ],
    usage: {
      kind: usage.kind,
      featureClass: usage.featureClass,
      remainingForKind: usage.remainingForKind,
      limitForKind: usage.limitForKind,
      warningLevel: usage.warningLevel || "ok",
      guardrailHints: usage.guardrailHints || {},
      effectiveEntitlement: usage.effectiveEntitlement || null,
      decision: usage.decision || null,
    },
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
