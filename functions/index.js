"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const functionsV1 = require("firebase-functions/v1");
const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {onMessagePublished} = require("firebase-functions/v2/pubsub");
const {onSchedule} = require("firebase-functions/v2/scheduler");
const {enableFirebaseTelemetry} = require("@genkit-ai/firebase");
const {onCall, onRequest, HttpsError} = require("firebase-functions/v2/https");
const {defineSecret} = require("firebase-functions/params");
const {genkit, z} = require("genkit");
const {vertexAI} = require("@genkit-ai/google-genai");
const nodemailer = require("nodemailer");
const crypto = require("crypto");
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
  acknowledgeGooglePlaySubscriptionPurchase,
  fetchGooglePlaySubscriptionPurchase,
} = require("./src/payments/google-play-subscriptions");
const {
  buildReturnPageUrl,
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
const shopifyAdminAccessToken = defineSecret("SHOPIFY_ADMIN_ACCESS_TOKEN");
const manusApiKey = defineSecret("MANUS_API_KEY");
const xaiApiKey = defineSecret("XAI_API_KEY");
const agentRunCallbackSecret = defineSecret("AGENT_RUN_CALLBACK_SECRET");
const workflowApiSecret = defineSecret("SKYOS_WORKFLOW_SECRET");
const OWNER_EMAIL = normalizeEmail(process.env.SKYOS_OWNER_EMAIL || process.env.SKYDOWN_OWNER_EMAIL) || "nash.lioncorna@gmail.com";
const IOS_APP_BUNDLE_ID = nonEmptyString(process.env.SKYOS_IOS_APP_BUNDLE_ID) || "com.skydown.ios";
const SHOPIFY_STORE_DOMAIN_DEFAULT = "k5t1sc-ps.myshopify.com";
const SHOPIFY_API_VERSION = "2026-01";
const SHOPIFY_CONFIG_COLLECTION = "appConfig";
const SHOPIFY_CONFIG_DOCUMENT = "shopifyMerch";
const SHOPIFY_PRIVATE_CONFIG_COLLECTION = "adminConfig";
const SHOPIFY_PRIVATE_CONFIG_DOCUMENT = "shopifyMerchPrivate";
const AUTOMATION_CONFIG_COLLECTION = "adminConfig";
const OWNER_ACTIVEPIECES_CONFIG_DOCUMENT = "ownerActivepiecesFlow";
const USER_AUTOMATION_CONFIG_DOCUMENT_PREFIX = "automationN8n_";
const AGENT_EXTERNAL_AUDIT_COLLECTION = "agentExternalBridgeAudit";
const PERSONAL_AGENT_PROFILE_DOCUMENT_PREFIX = "agentProfile_";
const AI_PROMPT_SETTINGS_COLLECTION = "adminConfig";
const AI_PROMPT_SETTINGS_DOCUMENT = "aiPromptSettings";
const STRIPE_SECRET_STATUS_COLLECTION = "adminConfig";
const STRIPE_SECRET_STATUS_DOCUMENT = "stripeCheckoutSecrets";
const REMINDER_STATUSES = Object.freeze({
  scheduled: "scheduled",
  completed: "completed",
  cancelled: "cancelled",
});
const REMINDER_SOURCES = Object.freeze({
  agent: "agent",
  workflow: "workflow",
  manual: "manual",
});
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
    shippingNotes: "",
  },
  invoice: {
    companyName: "Skydown OS",
    companyAddress: "",
    taxNumber: "",
    vatId: "",
    taxRate: 19.0,
    invoicePrefix: "SD",
    supportEmail: "skydownent@gmail.com",
  },
});
const DEFAULT_LEGAL_CONTENT_SETTINGS = Object.freeze({
  brandName: "SkyOS",
  operatorName: "Skydown OS",
  rightsHolderName: "Skydown OS",
  supportEmail: "skydownent@gmail.com",
  lastUpdatedLabel: "20. April 2026",
  imprintReference: "Anbieter: Skydown OS · Erich-Plate-Weg 44 · 22419 Hamburg · DE · skydownent@gmail.com",
});
const EU_COUNTRY_CODES = new Set([
  "AT", "BE", "BG", "HR", "CY", "CZ", "DK", "EE", "FI", "FR", "DE", "GR", "HU",
  "IE", "IT", "LV", "LT", "LU", "MT", "NL", "PL", "PT", "RO", "SK", "SI", "ES", "SE",
]);

const AI_EXPERIENCE_LEVELS = {
  standard: "standard",
  advanced: "advanced",
  pro: "pro",
};

const agentTurnSchema = z.object({
  role: z.enum(["user", "assistant"]),
  text: z.string().trim().min(1).max(4000),
});

const agentAttachmentSchema = z.object({
  id: z.string().trim().max(200).optional(),
  name: z.string().trim().min(1).max(255),
  kind: z.enum(["image", "video", "audio", "text", "document", "file"]).default("file"),
  mimeType: z.string().trim().max(120).optional(),
  source: z.enum(["inline"]).default("inline"),
  inlineBase64: z.string().min(1).max(400_000),
});

const agentRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(4000),
  history: z.array(agentTurnSchema).max(24).default([]),
  mode: z.enum(["release", "briefing", "content", "merch", "automation"]).default("release"),
  aiLevel: z.enum(["standard", "advanced", "pro"]).default("standard"),
  executeAutomation: z.boolean().default(false),
  automationScope: z.enum(["owner", "personal"]).default("owner"),
  confirmedByUser: z.boolean().default(false),
  manusApiKeyOverride: z.string().trim().min(16).max(1024).optional(),
  attachments: z.array(agentAttachmentSchema).max(5).optional().default([]),
});

const agentFlowRequestSchema = agentRequestSchema.omit({
  manusApiKeyOverride: true,
}).extend({
  systemInstruction: z.string().trim().min(1).max(12000),
  workspaceContext: z.string().trim().max(12000).default(""),
  attachmentSummary: z.string().trim().max(4000).optional().default(""),
});

const aiTextRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(12000),
  mode: z.enum(["general", "faq", "caption", "release_plan", "briefing", "merch_copy", "video_concept"]).default("general"),
  aiLevel: z.enum(["standard", "advanced", "pro"]).default("standard"),
});

const aiVisualRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(12000),
  aiLevel: z.enum(["standard", "advanced", "pro"]).default("standard"),
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

const DEFAULT_AI_FAQ_INSTRUCTION = `
Du bist der SkyOS FAQ Core.
Deine Aufgabe ist nicht kreatives Schreiben, sondern praezise, ehrliche, vertrauenswuerdige Hilfe.

Arbeitsregeln:
- Antworte klar, ruhig und konkret.
- Nutze zuerst die bereitgestellten Fakten und Produktregeln.
- Wenn eine Information nicht in den Fakten steht, sage klar "Das weiss ich gerade nicht sicher."
- Erfinde keine Policies, Preise, Versandzeiten, Account-Regeln oder Membership-Rechte.
- Wenn sinnvoll, gib eine kurze Antwort zuerst und danach optional 2 bis 4 hilfreiche Punkte.
- Wenn die Frage eher Support, Debug oder Kontakt braucht, sage den naechsten sinnvollen Schritt statt zu halluzinieren.
- Wenn die Frage nach App-Nutzung, Membership, Login, Orders, Datenschutz, AI oder Help klingt, behandle sie als FAQ-/Help-Anfrage.
- Prioritaet fuer Fakten: 1) Live Facts aus dem System, 2) Owner Knowledge, 3) vorsichtige generische Hilfe.
- Antworte auf Deutsch.
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

const DEFAULT_AI_FAQ_KNOWLEDGE_BASE = `
SkyOS FAQ Knowledge Base v2

Grundprinzip:
- Ziel ist echte Hilfe, nicht Marketing-Blabla.
- Keine erfundenen Preise, Fristen, Versandversprechen, Rechtsaussagen oder Entitlements.
- Wenn ein Fakt fehlt: klar sagen, dass er nicht sicher vorliegt, und den naechsten sinnvollen Schritt nennen.
- Bei Membership, Checkout, Restore, Orders, AI-Limits und Legal immer zuerst Live Facts nutzen.

Schnellfakten:
- SkyOS verbindet Home, AI, Music, Video, Shop, Profile und Settings.
- Bot: schnelle Hilfe, FAQ, Copy und Ideen.
- Agent: strukturierte Aufgaben, Planung, Briefings und Workflows.
- Membership ist faehigkeitsbasiert und kein Token-Shop.
- Kaeufe und Restore koennen kurze Synchronisierungszeit brauchen.
- Support: skydownent@gmail.com
- Betreiberhinweis: Skydown OS, Erich-Plate-Weg 44, 22419 Hamburg, Deutschland.

Kernfragen mit Zielantworten:

[Einstieg / Getting Started]
Q1: Was ist SkyOS in einem Satz?
A1: SkyOS ist eine Creator-App, die AI, Media und Commerce in einem Flow verbindet.

Q2: Wie starte ich am besten als neuer Nutzer?
A2: Melde dich zuerst an, oeffne Home zur Orientierung und starte dann mit einer klaren Frage im Bot.

Q3: Wo sehe ich, was ich als naechstes tun soll?
A3: Nutze Home fuer Einstieg und wechsel dann gezielt in AI, Shop, Music oder Video.

Q4: Ist SkyOS eher fuer Creator oder normale Nutzer?
A4: Beides ist moeglich; Creator profitieren besonders von AI- und Workflow-Funktionen.

[Login / Account]
Q5: Ich komme nicht rein. Was pruefe ich zuerst?
A5: Verbindung pruefen, App neu oeffnen, erneut anmelden und danach Login-Methode kontrollieren.

Q6: Was tun, wenn Login weiter fehlschlaegt?
A6: Support mit Konto-E-Mail, Plattform, Uhrzeit und Screenshot kontaktieren.

Q7: Warum sehe ich manche Bereiche nicht?
A7: Meist fehlen Rolle, Freigabe oder ein aktives Entitlement.

Q8: Wird mein Verlauf beim Account-Wechsel behalten?
A8: Verlauf ist kontoabhaengig; bei Wechsel ist anderer Kontext sichtbar.

[Membership / Abo / Restore]
Q9: Welche Membership habe ich?
A9: Der Plan soll aus Live Entitlements gelesen werden, nicht aus rein lokalem UI-Status.

Q10: Welche Membership passt zu mir?
A10: Free fuer Einstieg, Pro fuer regelmaessigen Creator-Flow, Creator fuer tiefere Workflows und Prioritaet.

Q11: Warum lohnt sich ein Upgrade?
A11: Ein Upgrade reduziert Reibung bei Limits und schaltet staerkere AI-Nutzung fuer echte Produktionsarbeit frei.

Q12: Wie restore ich mein Abo?
A12: Restore im Membership-Bereich ausloesen und kurz auf Synchronisierung warten.

Q13: Restore klappt nicht. Was dann?
A13: Store-Account pruefen, App neu starten, erneut Restore; danach Support mit Konto und Zeitstempel.

Q14: Wie kuendige ich mein Abo?
A14: Kuendigung laeuft ueber den jeweiligen Store-/Abo-Manager, nicht direkt im Chat.

Q15: Wird beim Upgrade sofort umgestellt?
A15: In der Regel ja, aber die Entitlement-Sync kann kurz dauern.

[AI Features / Limits / Freischaltung]
Q16: Warum ist AI gesperrt?
A16: Haeufige Gruende sind fehlende Freigabe, Rolle, Entitlement oder ein aktiver Sicherheits-/Runtime-Block.

Q17: Warum kann ich gerade nichts mehr senden?
A17: Wahrscheinlich ist ein Tageslimit oder Cost Guard erreicht; Bot soll den Grund lesbar nennen.

Q18: Was ist der Unterschied zwischen Bot und Agent?
A18: Bot fuer schnelle Antworten, Agent fuer strukturierte laengere Aufgaben.

Q19: Warum ist eine Antwort kuerzer als erwartet?
A19: Bei aktivem Cost Guard oder knappem Limit kann die Antwort bewusst verkuerzt werden.

Q20: Welche Prompts geben bessere Ergebnisse?
A20: Klare Ziele, gewuenschter Stil, Format, Plattform und Tiefe in einem Satz helfen am meisten.

[Merch / Bestellung / Versand]
Q21: Wo ist meine Bestellung?
A21: Im Order-Bereich den Live-Status pruefen; ohne Orderdaten keine Versandprognose erfinden.

Q22: Wann kommt meine Bestellung an?
A22: Nur konkrete ETA nennen, wenn Live Versanddaten verfuegbar sind.

Q23: Ist Versand kostenlos?
A23: Nur beantworten, wenn Commerce-/Checkout-Facts es klar zeigen; sonst offen als unbekannt markieren.

Q24: Kann ich Bestellung oder Adresse nachtraeglich aendern?
A24: Das haengt vom Orderstatus und Shop-Prozess ab; ohne Fakt keine feste Zusage geben.

Q25: Warum sehe ich keine Trackingnummer?
A25: Tracking erscheint erst, wenn Versanddaten vom Fulfillment vorliegen.

[Zahlungsarten / Checkout]
Q26: Welche Zahlungsarten gibt es?
A26: Nur die aktuell verfuegbaren Payment Methods aus Live Facts nennen.

Q27: Checkout ist fehlgeschlagen. Was jetzt?
A27: Nicht mehrfach triggern, kurz warten, dann erneut versuchen und bei Fehlercode Support kontaktieren.

Q28: Wurde ich doppelt belastet?
A28: Erst Order-/Payment-Status pruefen; bei Unsicherheit Zahlungsreferenz und Zeitstempel an Support senden.

Q29: Warum ist meine Zahlung pending?
A29: Je nach Provider kann Autorisierung und Bestaetigung verzoegert eintreffen.

[Datenschutz / AGB / Hilfe]
Q30: Welche Daten speichert SkyOS?
A30: Nur bestaetigte Datenkategorien nennen; falls Legal Content fehlt, klar als nicht sicher markieren.

Q31: Wo finde ich Datenschutz und AGB?
A31: Im Legal-/Settings-Bereich; bei fehlender Anzeige Support kontaktieren.

Q32: Gibt SkyOS Daten an Dritte weiter?
A32: Nur nach legal bestaetigten Inhalten beantworten, keine Vermutungen.

Q33: Welche Daten soll ich nicht in den Bot schreiben?
A33: Keine Passwoerter, Private Keys, volle Kartendaten oder sensible Fremddaten.

[App Nutzung / Features]
Q34: Wie nutze ich SkyOS effizient im Alltag?
A34: Mit klarem Tagesziel starten, Bot fuer schnelle Aufgaben nutzen und Agent fuer tiefe Ausarbeitung.

Q35: Kann ich SkyOS auch nur fuer Content-Ideen nutzen?
A35: Ja, Bot eignet sich genau fuer schnelle Hooks, Captions und kreative Varianten.

Q36: Warum unterscheidet sich Antwortqualitaet manchmal?
A36: Modus, Promptqualitaet, Runtime-Limits und verfuegbare Fakten beeinflussen die Ausgabe.

[Creator / Owner]
Q37: Wie kann Owner eigenes FAQ-Wissen hinterlegen?
A37: Ueber FAQ / Owner Knowledge in den Prompt-Settings, damit Bot auf reale Owner-Fakten zugreift.

Q38: Was passiert ohne Owner-Eintrag?
A38: Dann nutzt der Bot nur Standardwissen und Live Facts, ohne Owner-Regeln zu erfinden.

Q39: Kann ich FAQ-Antworten markenspezifisch steuern?
A39: Ja, ueber FAQ Instruction und Owner Knowledge, solange Fakten korrekt bleiben.

[Vertrauen / Sicherheit]
Q40: Warum sollte ich der FAQ vertrauen?
A40: Weil sie unbekannte Punkte offen kennzeichnet und keine Policies halluziniert.

Q41: Wann soll ich direkt Support kontaktieren?
A41: Bei Login-Blockern, Restore-Problemen, fehlgeschlagenem Checkout, unklaren Orders oder rechtlichen Fragen.

Q42: Was braucht Support fuer schnelle Loesung?
A42: Konto-E-Mail, Plattform, betroffener Bereich, Zeitpunkt, Screenshot und ggf. Referenznummer.

[Revenue-orientierte, aber faire Hilfe]
Q43: Warum lohnt sich Creator oder Pro ohne Hard-Sell?
A43: Wenn AI Teil deines Workflows ist, sparen hoehere Plaene Zeit, Abbrueche und Kontextwechsel.

Q44: Ich bin unsicher beim Upgrade - was ist die sichere Empfehlung?
A44: Mit dem kleineren passenden Plan starten, Nutzung beobachten und bei Bedarf spaeter hochstufen.

Q45: Wie antworte ich auf "zu teuer" fair?
A45: Transparent auf Nutzen und Arbeitsersparnis verweisen, nie Druck aufbauen.

[Owner definierte Fragen]
- Owner-spezifisches FAQ-Wissen kann zusaetzlich im Feld FAQ / Owner Knowledge hinterlegt werden.
- Wenn dort nichts hinterlegt ist, darf der Bot keine erfundenen Owner-Regeln behaupten.
`.trim();

const DEFAULT_AI_PROMPT_SETTINGS = Object.freeze({
  textInstruction: DEFAULT_AI_TEXT_INSTRUCTION,
  visualInstruction: DEFAULT_AI_VISUAL_INSTRUCTION,
  agentSystemInstruction: DEFAULT_AGENT_SYSTEM_PROMPT,
  faqInstruction: DEFAULT_AI_FAQ_INSTRUCTION,
  faqKnowledgeBase: DEFAULT_AI_FAQ_KNOWLEDGE_BASE,
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
  faq: "faq",
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
const AI_FAQ_INTELLIGENCE_DAILY_COLLECTION = "aiFaqIntelligenceDaily";
const AI_MEMBERSHIP_RECOMMENDATION_LIFECYCLE_COLLECTION = "recommendationLifecycle";
const AI_FAQ_REVIEW_CHANGE_LOG_COLLECTION = "aiFaqReviewChangeLog";
const AI_GUARDRAILS_COLLECTION = "guardrails";
const AI_GUARDRAILS_DAILY_BURN_PREFIX = "aiDailyBurn_";
const AI_ENTITLEMENTS_SUBCOLLECTION = "entitlements";
const AI_ENTITLEMENT_DOCUMENT = "ai";
const AI_ENTITLEMENT_EVENTS_SUBCOLLECTION = "entitlementEvents";
const AI_MEMBERSHIP_EVENT_TYPES = Object.freeze([
  "app_open",
  "signup_start",
  "signup_complete",
  "onboarding_started",
  "onboarding_completed",
  "first_value_moment",
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
      allowWorkflow: true,
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
  bot: {
    promptVersion: "bot-max-v1",
    qualityMode: "balanced",
    faqMode: "auto",
    ownerMode: "standard",
    answerLength: "adaptive",
    personalityStyle: "calm_precise",
    loggingLevel: "standard",
    diagnosticsMode: "owner_only",
    killSwitchEnabled: false,
    modelPolicy: {
      textPrimaryModel: "gemini-2.5-flash-lite",
      textFallbackModel: "gemini-2.5-flash-lite",
      visualPrimaryModel: "gemini-2.5-flash-image",
      visualFallbackModel: "imagen-3.0-generate-002",
    },
    costGuard: {
      enabled: true,
      preferBriefAnswersWhenCritical: true,
      shortAnswerMaxOutputTokens: 240,
      standardAnswerMaxOutputTokens: 768,
    },
    routingPolicy: {
      preferFaqWhenTopicMatched: true,
      preferProductGuideForNewUsers: true,
      allowVisualGeneration: true,
    },
    fallbackPolicy: {
      allowTextFallback: true,
      allowVisualFallback: true,
      exposeFallbackReason: true,
    },
    safetyPolicy: {
      safeModeEnabled: true,
      strictUnknownHandling: true,
      blockSpeculativeFaqAnswers: true,
    },
    actionLayer: {
      proactiveHintsEnabled: true,
      triggerAiLimitNearEnabled: true,
      triggerRestoreAvailableEnabled: true,
      triggerOrderShippedEnabled: true,
      triggerPaymentMethodsChangedEnabled: true,
      triggerUsageBasedUpgradeEnabled: true,
      warningThresholdPercent: 70,
      criticalThresholdPercent: 90,
      upgradeHintFreeToProText: "Deine Nutzung ist hoch. Ein Upgrade auf Pro reduziert Abbrueche durch Limits.",
      upgradeHintProToCreatorText: "Deine Nutzung ist hoch. Creator kann dir mehr Workflow-Tiefe und Reserve geben.",
      faqPriorityMode: "live_owner_generic",
      promptVersionAlias: "bot-max-v1",
    },
    agentCore: {
      allowedTasks: ["support_recovery", "commerce_order", "owner_ops"],
      blockedTasks: [],
      toolPolicy: {
        allowedTools: ["knowledge_lookup", "order_lookup", "membership_lookup", "owner_runtime"],
        allowWorkflowAutomation: true,
      },
      confirmationPolicy: {
        requireConfirmationForCommerce: true,
        requireConfirmationForOwnerOps: true,
      },
      safetyPolicy: {
        blockWhenKillSwitchEnabled: true,
        blockUnknownTasks: true,
      },
      fallbackPolicy: {
        blockedState: "blocked",
        retryableState: "retryable",
        partialState: "partial",
      },
      externalPolicy: {
        activepiecesEnabled: true,
        n8nEnabled: true,
        manusEnabled: true,
        allowedExternalTaskTypes: ["support_recovery", "commerce_order", "owner_ops"],
        providerPriority: ["activepieces", "n8n"],
        maxExternalCallsPerRequest: 1,
        externalTimeoutMs: 12000,
        externalRetryAttempts: 2,
        allowedAutomationLinkHosts: [],
      },
      diagnosticsMode: "owner_only",
      ownerMode: "standard",
      killSwitch: false,
    },
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
  const isStaff = roleHasStaffAccess(role);
  const defaults = defaultAiLimitsForQuotaPlan(
      normalizedPlan ||
      (role === USER_ROLES.owner ? USER_QUOTA_PLANS.ownerUnlimited : USER_QUOTA_PLANS.free),
  );
  return {
    botText: true,
    botImage: isStaff || isActive || normalizedPlan === USER_QUOTA_PLANS.creator || normalizedPlan === USER_QUOTA_PLANS.studio,
    agentStandard: isStaff || isActive || normalizedPlan === USER_QUOTA_PLANS.creator || normalizedPlan === USER_QUOTA_PLANS.studio,
    workflowAutomation: isStaff || isActive || normalizedPlan === USER_QUOTA_PLANS.creator || normalizedPlan === USER_QUOTA_PLANS.studio,
    premiumOutputs: isStaff || (isActive && normalizedPlan === USER_QUOTA_PLANS.studio),
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
  const nextState = resolveCanonicalAiEntitlement(payload);
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
      provider: nextState.provider || null,
      externalEventId: nonEmptyString(externalEventId) || null,
      productId: nextState.productId || null,
      previousState: previousState || null,
      nextState,
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
    faqInstruction: normalizeAiPromptSetting(
        data.faqInstruction,
        DEFAULT_AI_PROMPT_SETTINGS.faqInstruction,
    ),
    faqKnowledgeBase: normalizeAiPromptSetting(
        data.faqKnowledgeBase,
        DEFAULT_AI_PROMPT_SETTINGS.faqKnowledgeBase,
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

function normalizeAgentRunProgress(value) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric)) {
    return null;
  }
  return Math.max(0, Math.min(100, Math.floor(numeric)));
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

function resolveBotSettingValue(value, fallback, allowedValues) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (normalized && allowedValues.includes(normalized)) {
    return normalized;
  }
  return fallback;
}

function resolveBotStringSetting(value, fallback, maxChars = 120) {
  const normalized = nonEmptyString(value);
  if (!normalized) {
    return fallback;
  }
  return normalized.slice(0, maxChars);
}

function resolveAiBotModelPolicy(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    textPrimaryModel: resolveBotStringSetting(
        source.textPrimaryModel,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.textPrimaryModel,
        120,
    ),
    textFallbackModel: resolveBotStringSetting(
        source.textFallbackModel,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.textFallbackModel,
        120,
    ),
    visualPrimaryModel: resolveBotStringSetting(
        source.visualPrimaryModel,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.visualPrimaryModel,
        120,
    ),
    visualFallbackModel: resolveBotStringSetting(
        source.visualFallbackModel,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.visualFallbackModel,
        120,
    ),
  };
}

function resolveAiBotCostGuard(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    enabled: source.enabled !== false,
    preferBriefAnswersWhenCritical: source.preferBriefAnswersWhenCritical !== false,
    shortAnswerMaxOutputTokens: clampIntegerSetting(
        source.shortAnswerMaxOutputTokens,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.costGuard.shortAnswerMaxOutputTokens,
        80,
        1200,
    ),
    standardAnswerMaxOutputTokens: clampIntegerSetting(
        source.standardAnswerMaxOutputTokens,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.costGuard.standardAnswerMaxOutputTokens,
        120,
        2400,
    ),
  };
}

function resolveAiBotRoutingPolicy(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    preferFaqWhenTopicMatched: source.preferFaqWhenTopicMatched !== false,
    preferProductGuideForNewUsers: source.preferProductGuideForNewUsers !== false,
    allowVisualGeneration: source.allowVisualGeneration !== false,
  };
}

function resolveAiBotFallbackPolicy(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    allowTextFallback: source.allowTextFallback !== false,
    allowVisualFallback: source.allowVisualFallback !== false,
    exposeFallbackReason: source.exposeFallbackReason !== false,
  };
}

function resolveAiBotSafetyPolicy(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    safeModeEnabled: source.safeModeEnabled !== false,
    strictUnknownHandling: source.strictUnknownHandling !== false,
    blockSpeculativeFaqAnswers: source.blockSpeculativeFaqAnswers !== false,
  };
}

function resolveAiBotActionLayer(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    proactiveHintsEnabled: source.proactiveHintsEnabled !== false,
    triggerAiLimitNearEnabled: source.triggerAiLimitNearEnabled !== false,
    triggerRestoreAvailableEnabled: source.triggerRestoreAvailableEnabled !== false,
    triggerOrderShippedEnabled: source.triggerOrderShippedEnabled !== false,
    triggerPaymentMethodsChangedEnabled: source.triggerPaymentMethodsChangedEnabled !== false,
    triggerUsageBasedUpgradeEnabled: source.triggerUsageBasedUpgradeEnabled !== false,
    warningThresholdPercent: clampIntegerSetting(
        source.warningThresholdPercent,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.warningThresholdPercent,
        50,
        99,
    ),
    criticalThresholdPercent: clampIntegerSetting(
        source.criticalThresholdPercent,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.criticalThresholdPercent,
        60,
        100,
    ),
    upgradeHintFreeToProText: resolveBotStringSetting(
        source.upgradeHintFreeToProText,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintFreeToProText,
        220,
    ),
    upgradeHintProToCreatorText: resolveBotStringSetting(
        source.upgradeHintProToCreatorText,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintProToCreatorText,
        220,
    ),
    faqPriorityMode: resolveBotSettingValue(
        source.faqPriorityMode,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.faqPriorityMode,
        ["live_owner_generic", "owner_live_generic", "balanced"],
    ),
    promptVersionAlias: resolveBotStringSetting(
        source.promptVersionAlias,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.promptVersionAlias,
        120,
    ),
  };
}

function resolveAiBotAgentCore(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  const allowedTasksRaw = Array.isArray(source.allowedTasks) ? source.allowedTasks : [];
  const blockedTasksRaw = Array.isArray(source.blockedTasks) ? source.blockedTasks : [];
  const allowedTasks = [...new Set(allowedTasksRaw
      .map((entry) => nonEmptyString(entry))
      .filter(Boolean))]
      .filter((task) => ["support_recovery", "commerce_order", "owner_ops"].includes(task));
  const blockedTasks = [...new Set(blockedTasksRaw
      .map((entry) => nonEmptyString(entry))
      .filter(Boolean))]
      .filter((task) => ["support_recovery", "commerce_order", "owner_ops"].includes(task));
  const toolPolicy = source.toolPolicy && typeof source.toolPolicy === "object" && !Array.isArray(source.toolPolicy) ?
    source.toolPolicy : {};
  const confirmationPolicy = source.confirmationPolicy && typeof source.confirmationPolicy === "object" && !Array.isArray(source.confirmationPolicy) ?
    source.confirmationPolicy : {};
  const safetyPolicy = source.safetyPolicy && typeof source.safetyPolicy === "object" && !Array.isArray(source.safetyPolicy) ?
    source.safetyPolicy : {};
  const fallbackPolicy = source.fallbackPolicy && typeof source.fallbackPolicy === "object" && !Array.isArray(source.fallbackPolicy) ?
    source.fallbackPolicy : {};
  const defaultCore = DEFAULT_AI_RUNTIME_SETTINGS.bot.agentCore;
  const allowedToolsRaw = Array.isArray(toolPolicy.allowedTools) ? toolPolicy.allowedTools : [];
  return {
    allowedTasks: allowedTasks.length ? allowedTasks : defaultCore.allowedTasks,
    blockedTasks,
    toolPolicy: {
      allowedTools: [...new Set(allowedToolsRaw
          .map((entry) => nonEmptyString(entry))
          .filter(Boolean))].slice(0, 10),
      allowWorkflowAutomation: toolPolicy.allowWorkflowAutomation !== false,
    },
    confirmationPolicy: {
      requireConfirmationForCommerce: confirmationPolicy.requireConfirmationForCommerce !== false,
      requireConfirmationForOwnerOps: confirmationPolicy.requireConfirmationForOwnerOps !== false,
    },
    safetyPolicy: {
      blockWhenKillSwitchEnabled: safetyPolicy.blockWhenKillSwitchEnabled !== false,
      blockUnknownTasks: safetyPolicy.blockUnknownTasks !== false,
    },
    fallbackPolicy: {
      blockedState: resolveBotSettingValue(
          fallbackPolicy.blockedState,
          defaultCore.fallbackPolicy.blockedState,
          ["blocked", "failed", "cancelled"],
      ),
      retryableState: resolveBotSettingValue(
          fallbackPolicy.retryableState,
          defaultCore.fallbackPolicy.retryableState,
          ["retryable", "failed", "partial"],
      ),
      partialState: resolveBotSettingValue(
          fallbackPolicy.partialState,
          defaultCore.fallbackPolicy.partialState,
          ["partial", "completed"],
      ),
    },
    externalPolicy: {
      activepiecesEnabled: source.externalPolicy?.activepiecesEnabled !== false,
      n8nEnabled: source.externalPolicy?.n8nEnabled !== false,
      manusEnabled: source.externalPolicy?.manusEnabled !== false,
      allowedExternalTaskTypes: [...new Set((Array.isArray(source.externalPolicy?.allowedExternalTaskTypes) ?
          source.externalPolicy.allowedExternalTaskTypes :
          defaultCore.externalPolicy.allowedExternalTaskTypes)
          .map((entry) => nonEmptyString(entry))
          .filter(Boolean))]
          .filter((task) => ["support_recovery", "commerce_order", "owner_ops"].includes(task)),
      providerPriority: [...new Set((Array.isArray(source.externalPolicy?.providerPriority) ?
          source.externalPolicy.providerPriority :
          defaultCore.externalPolicy.providerPriority)
          .map((entry) => nonEmptyString(entry))
          .filter(Boolean))]
          .filter((provider) => ["activepieces", "n8n"].includes(provider)),
      maxExternalCallsPerRequest: clampIntegerSetting(
          source.externalPolicy?.maxExternalCallsPerRequest,
          defaultCore.externalPolicy.maxExternalCallsPerRequest,
          0,
          3,
      ),
      externalTimeoutMs: clampIntegerSetting(
          source.externalPolicy?.externalTimeoutMs,
          defaultCore.externalPolicy.externalTimeoutMs,
          2000,
          30000,
      ),
      externalRetryAttempts: clampIntegerSetting(
          source.externalPolicy?.externalRetryAttempts,
          defaultCore.externalPolicy.externalRetryAttempts,
          0,
          4,
      ),
      allowedAutomationLinkHosts: (Array.isArray(source.externalPolicy?.allowedAutomationLinkHosts) ?
        source.externalPolicy.allowedAutomationLinkHosts :
        defaultCore.externalPolicy.allowedAutomationLinkHosts || [])
          .map((entry) => nonEmptyString(entry)?.trim().toLowerCase())
          .filter(Boolean)
          .slice(0, 32)
          .map((entry) => entry.slice(0, 128)),
    },
    diagnosticsMode: resolveBotSettingValue(
        source.diagnosticsMode,
        defaultCore.diagnosticsMode,
        ["off", "owner_only", "verbose"],
    ),
    ownerMode: resolveBotSettingValue(
        source.ownerMode,
        defaultCore.ownerMode,
        ["standard", "diagnostic"],
    ),
    killSwitch: source.killSwitch === true,
  };
}

function resolveAiBotRuntime(raw) {
  const source = raw && typeof raw === "object" && !Array.isArray(raw) ? raw : {};
  return {
    promptVersion: resolveBotStringSetting(
        source.promptVersion,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.promptVersion,
        120,
    ),
    qualityMode: resolveBotSettingValue(
        source.qualityMode,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.qualityMode,
        ["balanced", "high"],
    ),
    faqMode: resolveBotSettingValue(
        source.faqMode,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.faqMode,
        ["off", "auto", "prefer_faq"],
    ),
    ownerMode: resolveBotSettingValue(
        source.ownerMode,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.ownerMode,
        ["standard", "diagnostic"],
    ),
    answerLength: resolveBotSettingValue(
        source.answerLength,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.answerLength,
        ["adaptive", "short", "detailed"],
    ),
    personalityStyle: resolveBotStringSetting(
        source.personalityStyle,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.personalityStyle,
        160,
    ),
    loggingLevel: resolveBotStringSetting(
        source.loggingLevel,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.loggingLevel,
        80,
    ),
    diagnosticsMode: resolveBotSettingValue(
        source.diagnosticsMode,
        DEFAULT_AI_RUNTIME_SETTINGS.bot.diagnosticsMode,
        ["off", "owner_only", "verbose"],
    ),
    killSwitchEnabled: source.killSwitchEnabled === true,
    modelPolicy: resolveAiBotModelPolicy(source.modelPolicy),
    costGuard: resolveAiBotCostGuard(source.costGuard),
    routingPolicy: resolveAiBotRoutingPolicy(source.routingPolicy),
    fallbackPolicy: resolveAiBotFallbackPolicy(source.fallbackPolicy),
    safetyPolicy: resolveAiBotSafetyPolicy(source.safetyPolicy),
    actionLayer: resolveAiBotActionLayer(source.actionLayer),
    agentCore: resolveAiBotAgentCore(source.agentCore),
  };
}

function resolveReviewRecommendationAction({
  recommendation = {},
  currentActionLayer = {},
  repeatHeavyTopics = [],
}) {
  const recommendationId = nonEmptyString(recommendation.id) || "";
  const actionType = nonEmptyString(recommendation.actionType) || "";
  const targetField = nonEmptyString(recommendation.targetField) || "";
  const suggestedValue = recommendation.suggestedValue;
  const recommendationEntry = {recommendationId, actionType, targetField, suggestedValue};
  const nextActionLayer = {...currentActionLayer};
  const safeguards = [];

  const triggerFieldByKey = {
    ai_limit_fast_erreicht: "triggerAiLimitNearEnabled",
    restore_verfuegbar: "triggerRestoreAvailableEnabled",
    bestellung_versendet: "triggerOrderShippedEnabled",
    payment_methode_geaendert: "triggerPaymentMethodsChangedEnabled",
    upgrade_sinnvoll_basierend_auf_nutzung: "triggerUsageBasedUpgradeEnabled",
  };

  if (!recommendationId || !actionType || !targetField) {
    safeguards.push("Recommendation ist unvollstaendig.");
    return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
  }

  if (actionType === "adjust_threshold") {
    const field = targetField === "criticalThresholdPercent" ? "criticalThresholdPercent" : "warningThresholdPercent";
    const min = field === "warningThresholdPercent" ? 50 : 60;
    const max = field === "warningThresholdPercent" ? 99 : 100;
    const fallback = Number(currentActionLayer[field] || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer[field]);
    const proposed = clampIntegerSetting(suggestedValue, fallback, min, max);
    const warning = field === "warningThresholdPercent" ? proposed : Number(currentActionLayer.warningThresholdPercent || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.warningThresholdPercent);
    const critical = field === "criticalThresholdPercent" ? proposed : Number(currentActionLayer.criticalThresholdPercent || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.criticalThresholdPercent);
    if (warning >= critical) {
      safeguards.push("Warning Threshold muss unter Critical Threshold liegen.");
      return {allowed: false, nextActionLayer: {...currentActionLayer}, safeguards, recommendationEntry};
    }
    if (Math.abs(proposed - fallback) > 15) {
      safeguards.push("Threshold-Sprung >15 Punkte ist blockiert.");
      return {allowed: false, nextActionLayer: {...currentActionLayer}, safeguards, recommendationEntry};
    }
    nextActionLayer[field] = proposed;
    return {allowed: true, nextActionLayer, safeguards, recommendationEntry};
  }

  if (actionType === "disable_trigger") {
    const mappedField = triggerFieldByKey[targetField];
    if (!mappedField) {
      safeguards.push("Unbekannter Trigger fuer Deaktivierung.");
      return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
    }
    const activeTriggerCount = [
      "triggerAiLimitNearEnabled",
      "triggerRestoreAvailableEnabled",
      "triggerOrderShippedEnabled",
      "triggerPaymentMethodsChangedEnabled",
      "triggerUsageBasedUpgradeEnabled",
    ].reduce((sum, key) => sum + (currentActionLayer[key] !== false ? 1 : 0), 0);
    if (activeTriggerCount <= 1) {
      safeguards.push("Mindestens ein Trigger muss aktiv bleiben.");
      return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
    }
    nextActionLayer[mappedField] = false;
    return {allowed: true, nextActionLayer, safeguards, recommendationEntry};
  }

  if (actionType === "improve_hint_text") {
    const field = targetField === "upgradeHintProToCreatorText" ? "upgradeHintProToCreatorText" : "upgradeHintFreeToProText";
    const source = nonEmptyString(suggestedValue) || nonEmptyString(currentActionLayer[field]) || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer[field];
    const nextText = truncateTextBlock(source, 220);
    if (nextText.length < 24) {
      safeguards.push("Hint-Text ist zu kurz.");
      return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
    }
    nextActionLayer[field] = nextText;
    return {allowed: true, nextActionLayer, safeguards, recommendationEntry};
  }

  if (actionType === "adjust_faq_priority") {
    const mode = nonEmptyString(suggestedValue);
    const allowedModes = ["live_owner_generic", "owner_live_generic", "balanced"];
    if (!mode || !allowedModes.includes(mode)) {
      safeguards.push("FAQ Priority Mode ist ungueltig.");
      return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
    }
    if (mode === "owner_live_generic" && repeatHeavyTopics.length >= 2) {
      safeguards.push("Owner->Live bei repeat-heavy Topics blockiert.");
      return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
    }
    nextActionLayer.faqPriorityMode = mode;
    return {allowed: true, nextActionLayer, safeguards, recommendationEntry};
  }

  safeguards.push("Action Type nicht unterstuetzt.");
  return {allowed: false, nextActionLayer, safeguards, recommendationEntry};
}

function buildFaqStrategyInsights({
  docs = [],
  actionLayer = {},
  triggerInsights = [],
  repeatHeavyTopics = [],
}) {
  const safeDocs = Array.isArray(docs) ? docs : [];
  const dayRows = safeDocs
      .map((doc) => doc?.data?.() || {})
      .filter((row) => row && typeof row === "object");
  const latest14 = dayRows.slice(-14);
  const previous14 = dayRows.slice(-28, -14);
  const aggregateMap = (rows, fieldName) => {
    const output = {};
    for (const row of rows) {
      const source = row?.[fieldName];
      if (!source || typeof source !== "object" || Array.isArray(source)) continue;
      for (const [key, value] of Object.entries(source)) {
        output[key] = (Number(output[key]) || 0) + (Number(value) || 0);
      }
    }
    return output;
  };
  const sumField = (rows, fieldName) => rows.reduce((sum, row) => sum + (Number(row?.[fieldName]) || 0), 0);
  const latestHintTriggers = aggregateMap(latest14, "hintTriggers");
  const previousHintTriggers = aggregateMap(previous14, "hintTriggers");
  const latestHintConversions = aggregateMap(latest14, "hintConversions");
  const previousHintConversions = aggregateMap(previous14, "hintConversions");
  const latestFaqByTopic = aggregateMap(latest14, "faqByTopic");
  const previousFaqByTopic = aggregateMap(previous14, "faqByTopic");
  const latestHintWindows = sumField(latest14, "totalHintWindows");
  const previousHintWindows = sumField(previous14, "totalHintWindows");
  const latestRepeatAfterHint = sumField(latest14, "repeatAfterHint");
  const previousRepeatAfterHint = sumField(previous14, "repeatAfterHint");

  const strategyInsights = [];
  const pushInsight = (payload) => {
    if (!payload || !payload.id) return;
    strategyInsights.push(payload);
  };

  const triggerDeltas = Object.keys(latestHintTriggers).map((key) => {
    const latest = Number(latestHintTriggers[key]) || 0;
    const previous = Number(previousHintTriggers[key]) || 0;
    const latestConversions = Number(latestHintConversions[key]) || 0;
    const previousConversions = Number(previousHintConversions[key]) || 0;
    const latestRate = latest > 0 ? latestConversions / latest : 0;
    const previousRate = previous > 0 ? previousConversions / previous : 0;
    return {
      triggerKey: key,
      latest,
      previous,
      delta: latest - previous,
      latestRate,
      previousRate,
    };
  }).sort((a, b) => a.latestRate - b.latestRate);

  const underperforming = triggerDeltas.find((entry) => entry.latest >= 5 && entry.latestRate < 0.05);
  if (underperforming) {
    pushInsight({
      id: `trigger_underperform_${underperforming.triggerKey}`,
      category: "trigger_underperform",
      title: `Trigger ${underperforming.triggerKey} underperformt seit 14 Tagen`,
      summary: `Conversion ${Number((underperforming.latestRate * 100).toFixed(1))}% bei ${underperforming.latest} Triggern.`,
      expectedImpact: "Trigger-Fokus schaerfen kann Repeat-Support reduzieren und Hint-Qualitaet verbessern.",
      confidence: 0.78,
      severity: "high",
    });
  }

  const repeatTopicDeltas = Object.keys(latestFaqByTopic).map((key) => ({
    topicKey: key,
    latest: Number(latestFaqByTopic[key]) || 0,
    previous: Number(previousFaqByTopic[key]) || 0,
  }))
      .map((entry) => ({...entry, growthRate: entry.previous > 0 ? (entry.latest - entry.previous) / entry.previous : (entry.latest > 0 ? 1 : 0)}))
      .sort((a, b) => b.growthRate - a.growthRate);
  const risingRepeatTopic = repeatTopicDeltas.find((entry) => entry.latest >= 6 && entry.growthRate >= 0.35);
  if (risingRepeatTopic) {
    pushInsight({
      id: `repeat_topic_rise_${risingRepeatTopic.topicKey}`,
      category: "repeat_topic_rising",
      title: `Repeat Topic ${risingRepeatTopic.topicKey} steigt stark`,
      summary: `14d Volume ${risingRepeatTopic.latest} vs vorher ${risingRepeatTopic.previous}.`,
      expectedImpact: "FAQ-Prioritaet und Fakten-Tiefe fuer dieses Thema koennen Tickets und Rueckfragen senken.",
      confidence: 0.74,
      severity: "medium",
    });
  }

  const latestRepeatRate = latestHintWindows > 0 ? latestRepeatAfterHint / latestHintWindows : 0;
  const previousRepeatRate = previousHintWindows > 0 ? previousRepeatAfterHint / previousHintWindows : 0;
  const warningThreshold = Number(actionLayer.warningThresholdPercent || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.warningThresholdPercent);
  if (latestRepeatRate >= 0.28 && warningThreshold <= 75) {
    pushInsight({
      id: "threshold_too_loose",
      category: "threshold_tuning",
      title: "Warning Threshold wirkt zu locker",
      summary: `Repeat-after-hint Rate liegt bei ${Number((latestRepeatRate * 100).toFixed(1))}%.`,
      expectedImpact: "Hoeherer Warning Threshold reduziert Hint-Rauschen und unnötige Support-Rueckfragen.",
      confidence: 0.71,
      severity: "medium",
    });
  } else if (latestRepeatRate <= 0.12 && latestHintWindows >= 20 && warningThreshold >= 75) {
    pushInsight({
      id: "threshold_too_strict",
      category: "threshold_tuning",
      title: "Warning Threshold wirkt zu streng",
      summary: `Repeat-after-hint Rate ${Number((latestRepeatRate * 100).toFixed(1))}% bei solidem Hint-Volumen.`,
      expectedImpact: "Leicht niedrigerer Threshold kann fruehere Upgrade-Hinweise und mehr Revenue-Uplift erzeugen.",
      confidence: 0.68,
      severity: "medium",
    });
  }

  const conversionLeaders = triggerInsights
      .filter((entry) => Number(entry.triggers) >= 5)
      .sort((a, b) => Number(b.conversionRate || 0) - Number(a.conversionRate || 0));
  if (conversionLeaders.length >= 2) {
    const best = conversionLeaders[0];
    const second = conversionLeaders[1];
    if ((Number(best.conversionRate) - Number(second.conversionRate)) >= 0.04) {
      pushInsight({
        id: `hint_better_${best.triggerKey}_vs_${second.triggerKey}`,
        category: "hint_comparison",
        title: `Upgrade Hint ${best.triggerKey} konvertiert besser als ${second.triggerKey}`,
        summary: `${Number((best.conversionRate * 100).toFixed(1))}% vs ${Number((second.conversionRate * 100).toFixed(1))}% Conversion.`,
        expectedImpact: "Top-Hint prominenter nutzen kann Upgrade-Rate erhoehen.",
        confidence: 0.76,
        severity: "medium",
      });
    }
  }

  if (!strategyInsights.length && repeatHeavyTopics.length) {
    pushInsight({
      id: "strategy_keep_monitoring",
      category: "stability",
      title: "Strategie stabil, Monitoring fortsetzen",
      summary: "Keine harte Verschlechterung im 14d Vergleich erkannt.",
      expectedImpact: "Weiteres Monitoring haelt den Loop sicher ohne vorschnelle Eingriffe.",
      confidence: 0.62,
      severity: "low",
    });
  }

  return strategyInsights.slice(0, 6);
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
    bot: resolveAiBotRuntime(data.bot),
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

async function assertUidAccess(auth, requestedUid, {allowStaffOverride = false} = {}) {
  const authUid = assertAuthenticatedUser(auth, "Bitte melde dich an.");
  const normalizedRequestedUid = nonEmptyString(requestedUid) || authUid;
  if (normalizedRequestedUid === authUid) {
    return normalizedRequestedUid;
  }
  if (allowStaffOverride && await isStaffAuth(auth)) {
    return normalizedRequestedUid;
  }
  throw new HttpsError("permission-denied", "Du darfst nur eigene Daten veraendern.");
}

function normalizeReminderStatus(value, fallback = REMINDER_STATUSES.scheduled) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (!normalized) {
    return fallback;
  }
  return Object.values(REMINDER_STATUSES).includes(normalized) ? normalized : fallback;
}

function normalizeReminderSource(value, fallback = REMINDER_SOURCES.manual) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  if (!normalized) {
    return fallback;
  }
  return Object.values(REMINDER_SOURCES).includes(normalized) ? normalized : fallback;
}

function parseReminderScheduledAtTimestamp(value) {
  const raw = nonEmptyString(value);
  if (!raw) {
    throw new HttpsError("invalid-argument", "scheduledAt fehlt.");
  }
  const parsedMs = Date.parse(raw);
  if (!Number.isFinite(parsedMs)) {
    throw new HttpsError("invalid-argument", "scheduledAt ist ungueltig.");
  }
  return admin.firestore.Timestamp.fromDate(new Date(parsedMs));
}

function formatReminderResultLine({title = "", scheduledAt = null, timezone = "UTC"}) {
  const normalizedTitle = nonEmptyString(title) || "Erinnerung";
  const date = scheduledAt instanceof admin.firestore.Timestamp ?
    scheduledAt.toDate() :
    new Date();
  const formatter = new Intl.DateTimeFormat("de-DE", {
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    timeZone: nonEmptyString(timezone) || "UTC",
  });
  return `${normalizedTitle} - ${formatter.format(date).replace(",", " um")}`;
}

function buildSkyosWorkflowResponse({
  message = "Erledigt.",
  workflowStatus = "completed",
  text = "",
  schemaVersion = "v1",
  extraResults = [],
}) {
  const resultText = nonEmptyString(text) || nonEmptyString(message) || "Erledigt.";
  return {
    message: nonEmptyString(message) || "Erledigt.",
    workflowStatus: nonEmptyString(workflowStatus) || "completed",
    schemaVersion: nonEmptyString(schemaVersion) || "v1",
    results: [
      {
        type: "text",
        text: resultText,
      },
      ...(Array.isArray(extraResults) ? extraResults : []),
    ],
  };
}

function loadWorkflowApiSecret() {
  try {
    return nonEmptyString(workflowApiSecret.value()) || nonEmptyString(process.env.SKYOS_WORKFLOW_SECRET) || "";
  } catch (error) {
    return nonEmptyString(process.env.SKYOS_WORKFLOW_SECRET) || "";
  }
}

function parseOptionalIsoTimestamp(value, fieldName, {required = false} = {}) {
  const raw = nonEmptyString(value);
  if (!raw) {
    if (required) {
      throw new HttpsError("invalid-argument", `${fieldName} fehlt.`);
    }
    return null;
  }
  const parsedMs = Date.parse(raw);
  if (!Number.isFinite(parsedMs)) {
    throw new HttpsError("invalid-argument", `${fieldName} ist ungueltig.`);
  }
  return admin.firestore.Timestamp.fromDate(new Date(parsedMs));
}

function trimOptionalText(value, maxChars) {
  return trimTextMax(nonEmptyString(value) || "", maxChars);
}

function isSecretMatch(provided, expected) {
  if (!provided || !expected) {
    return false;
  }
  const expectedBuffer = Buffer.from(expected, "utf8");
  const providedBuffer = Buffer.from(provided, "utf8");
  if (expectedBuffer.length !== providedBuffer.length) {
    return false;
  }
  return crypto.timingSafeEqual(expectedBuffer, providedBuffer);
}

function getWorkflowRequestBase(payload) {
  const uid = nonEmptyString(payload?.uid);
  if (!uid) {
    throw new HttpsError("invalid-argument", "uid fehlt.");
  }
  const source = nonEmptyString(payload?.source) || "activepieces";
  if (source !== "activepieces") {
    throw new HttpsError("invalid-argument", "source ist ungueltig.");
  }
  return {
    uid,
    source,
    requestId: trimOptionalText(payload?.requestId, 120) || null,
  };
}

function assertWorkflowHttpRequest(request, response) {
  if (request.method !== "POST") {
    response.status(405).json({ok: false, error: "method_not_allowed"});
    return null;
  }
  const expectedSecret = loadWorkflowApiSecret();
  if (!expectedSecret) {
    logger.error("Workflow API secret missing.");
    response.status(500).json({ok: false, error: "workflow_secret_missing"});
    return null;
  }
  const providedSecret = nonEmptyString(request.headers["x-skyos-workflow-secret"]) || "";
  if (!isSecretMatch(providedSecret, expectedSecret)) {
    response.status(401).json({ok: false, error: "unauthorized"});
    return null;
  }
  if (!request.body || typeof request.body !== "object" || Array.isArray(request.body)) {
    response.status(400).json({ok: false, error: "invalid_json_body"});
    return null;
  }
  return request.body;
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

async function deleteCollectionDocumentsByField(collectionName, fieldName, fieldValue) {
  const normalizedValue = nonEmptyString(fieldValue);
  if (!normalizedValue) {
    return 0;
  }

  const snapshot = await admin.firestore()
      .collection(collectionName)
      .where(fieldName, "==", normalizedValue)
      .get();

  return deleteDocumentsFromSnapshot(snapshot);
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
  const [
    deletedOrders,
    deletedUploadSlots,
    deletedStorageObjects,
    deletedAiUsageEvents,
    deletedAiMembershipEvents,
  ] = await Promise.all([
    deleteOrdersForAccount(uid, email),
    deleteDocumentsFromSnapshot(uploadSlotsSnapshot),
    deleteStoragePrefix(`users/${uid}/`),
    deleteCollectionDocumentsByField(AI_USAGE_EVENTS_COLLECTION, "uid", uid),
    deleteCollectionDocumentsByField(AI_MEMBERSHIP_EVENTS_COLLECTION, "uid", uid),
  ]);

  return {
    deletedOrders,
    deletedStorageObjects,
    deletedUploadSlots,
    deletedAiUsageEvents,
    deletedAiMembershipEvents,
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

async function loadOwnerActivepiecesWorkflowSettings() {
  const snapshot = await admin.firestore()
      .collection(AUTOMATION_CONFIG_COLLECTION)
      .doc(OWNER_ACTIVEPIECES_CONFIG_DOCUMENT)
      .get();
  return decodeWorkflowAutomationSettings(snapshot.data() || {}, {scope: "owner"});
}

function userAutomationConfigDocumentIdFor(userId) {
  return `${USER_AUTOMATION_CONFIG_DOCUMENT_PREFIX}${userId}`;
}

function personalAgentProfileDocumentIdFor(userId) {
  return `${PERSONAL_AGENT_PROFILE_DOCUMENT_PREFIX}${userId}`;
}

async function loadPersonalWorkflowAutomationSettings(userId) {
  const uid = nonEmptyString(userId);
  if (!uid) {
    return decodeWorkflowAutomationSettings({}, {scope: "personal"});
  }

  const snapshot = await admin.firestore()
      .collection(AUTOMATION_CONFIG_COLLECTION)
      .doc(userAutomationConfigDocumentIdFor(uid))
      .get();
  return decodeWorkflowAutomationSettings(snapshot.data() || {}, {scope: "personal"});
}

async function loadWorkflowAutomationSettingsForUser(userId, automationScope = "owner") {
  return automationScope === "personal" ?
    loadPersonalWorkflowAutomationSettings(userId) :
    loadOwnerActivepiecesWorkflowSettings();
}

function decodeWorkflowAutomationSettings(data = {}, {scope = "owner"} = {}) {
  const isPersonalScope = scope === "personal";
  const provider = nonEmptyString(data.provider) || "activepieces";
  const normalizedProvider = isPersonalScope && ["activepieces", "n8n"].includes(provider) ?
    provider :
    "activepieces";
  return {
    provider: normalizedProvider,
    scope: isPersonalScope ? "user_personal" : "owner_global",
    isEnabled: data.isEnabled === true,
    sendsUserContext: data.sendsUserContext !== false,
    workflowName: nonEmptyString(data.workflowName) || (
      isPersonalScope ? "Persoenlicher Workflow" : "SkyOS Owner Activepieces Flow"
    ),
    baseURL: normalizeUrlString(data.baseURL) || "",
    webhookPath: normalizeAutomationWebhookPath(data.webhookPath) || "",
    authHeaderName: nonEmptyString(data.authHeaderName) || "",
    authHeaderValue: nonEmptyString(data.authHeaderValue) || "",
    signingEnabled: data.signingEnabled === true,
    signatureHeaderName: nonEmptyString(data.signatureHeaderName) || "x-skyos-signature",
    signingSecret: nonEmptyString(data.signingSecret) || "",
    timeoutMs: clampIntegerSetting(data.timeoutMs, 12000, 2000, 30000),
    retryAttempts: clampIntegerSetting(data.retryAttempts, 2, 0, 4),
    retryBackoffMs: clampIntegerSetting(data.retryBackoffMs, 1200, 200, 5000),
    knowledgeContext: nonEmptyString(data.knowledgeContext) || "",
  };
}

function signAutomationPayload(payloadText, signingSecret) {
  const secret = nonEmptyString(signingSecret) || "";
  if (!secret) {
    return "";
  }
  return crypto.createHmac("sha256", secret).update(payloadText, "utf8").digest("hex");
}

async function createAgentExternalAuditEntry({
  requestId = "",
  uid = "",
  provider = "activepieces",
  route = "external",
  trigger = "",
  source = "",
  workflowName = "",
  status = "",
  state = "",
  reason = "",
  httpStatus = 0,
  durationMs = 0,
}) {
  try {
    await admin.firestore()
        .collection(AGENT_EXTERNAL_AUDIT_COLLECTION)
        .add({
          requestId: nonEmptyString(requestId) || null,
          uid: nonEmptyString(uid) || null,
          provider: nonEmptyString(provider) || "activepieces",
          route: nonEmptyString(route) || "external",
          trigger: nonEmptyString(trigger) || null,
          source: nonEmptyString(source) || null,
          workflowName: nonEmptyString(workflowName) || null,
          status: nonEmptyString(status) || "unknown",
          state: nonEmptyString(state) || null,
          reason: nonEmptyString(reason) || null,
          httpStatus: Number.isFinite(Number(httpStatus)) ? Number(httpStatus) : null,
          durationMs: Number.isFinite(Number(durationMs)) ? Number(durationMs) : null,
          createdAt: admin.firestore.FieldValue.serverTimestamp(),
        });
  } catch (error) {
    logger.warn("Agent external audit write failed.", {
      requestId: nonEmptyString(requestId) || null,
      error: error instanceof Error ? error.message : `${error}`,
    });
  }
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
    quotaPlan: profile.aiLimits?.quotaPlan || defaultQuotaPlanForRole(profile.role),
    aiAccessEnabled: profile.aiAccessEnabled,
    agentDailyLimit: profile.aiLimits?.agent || 0,
    isAdmin: profile.isAdmin,
    isOwner: profile.isOwner,
    isStaff: profile.isStaff,
  };
}

const AGENT_STRUCTURED_RESULT_TYPES = new Set([
  "text",
  "workflow",
  "image",
  "video",
  "audio",
  "file",
  "link",
  "table",
  "html",
]);

function normalizeAutomationAllowedLinkHosts(policy) {
  const raw = policy?.allowedAutomationLinkHosts;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw
      .map((entry) => nonEmptyString(entry)?.trim().toLowerCase())
      .filter(Boolean)
      .slice(0, 32)
      .map((entry) => entry.slice(0, 128));
}

function isAutomationUrlHostnameAllowed(hostname, allowedHosts) {
  const host = nonEmptyString(hostname)?.trim().toLowerCase() || "";
  if (!host || !Array.isArray(allowedHosts) || !allowedHosts.length) {
    return true;
  }
  return allowedHosts.some((entry) => host === entry || host.endsWith("." + entry));
}

function safeHttpsUrlStringForAgentResult(raw, allowedHosts = []) {
  const candidate = trimTextMax(nonEmptyString(raw) || "", 2000);
  if (!candidate) {
    return "";
  }
  try {
    const parsed = new URL(candidate);
    if (parsed.protocol !== "https:") {
      return "";
    }
    const host = nonEmptyString(parsed.hostname)?.trim().toLowerCase() || "";
    if (!host) {
      return "";
    }
    if (Array.isArray(allowedHosts) && allowedHosts.length &&
      !isAutomationUrlHostnameAllowed(host, allowedHosts)) {
      return "";
    }
    return candidate;
  } catch (error) {
    return "";
  }
}

function extractAutomationSchemaVersion(parsedBody) {
  if (!parsedBody || typeof parsedBody !== "object" || Array.isArray(parsedBody)) {
    return "";
  }
  const version = nonEmptyString(parsedBody.schemaVersion) ||
    nonEmptyString(parsedBody.automationSchemaVersion);
  return trimTextMax(version || "", 64);
}

function coerceAutomationWebhookParsedBody(parsedBody) {
  if (parsedBody === null || parsedBody === undefined) {
    return null;
  }
  if (Array.isArray(parsedBody)) {
    return parsedBody;
  }
  if (typeof parsedBody !== "object") {
    return null;
  }
  const body = {...parsedBody};
  const arrayKeys = ["results", "outputs", "assets", "files"];
  for (const key of arrayKeys) {
    if (Object.prototype.hasOwnProperty.call(body, key) && !Array.isArray(body[key])) {
      logger.warn("Automation webhook response ignored non-array field.", {
        key,
        type: typeof body[key],
      });
      delete body[key];
    }
  }
  return body;
}

function normalizeAgentResultType(value) {
  const normalized = nonEmptyString(value)?.toLowerCase() || "text";
  switch (normalized) {
    case "url":
    case "button":
      return "link";
    case "pdf":
    case "document":
    case "download":
      return "file";
    case "markdown":
      return "text";
    default:
      return AGENT_STRUCTURED_RESULT_TYPES.has(normalized) ? normalized : "text";
  }
}

function stringifyAgentResultValue(value) {
  if (value === null || value === undefined) {
    return "";
  }
  if (typeof value === "string") {
    return value.trim();
  }
  if (typeof value === "number" || typeof value === "boolean") {
    return `${value}`;
  }
  try {
    return JSON.stringify(value);
  } catch (error) {
    return "";
  }
}

function normalizeAgentTableColumns(rawColumns, rows) {
  if (Array.isArray(rawColumns)) {
    return rawColumns
        .map((column) => {
          if (typeof column === "string") {
            return column.trim();
          }
          return nonEmptyString(column?.title) ||
            nonEmptyString(column?.label) ||
            nonEmptyString(column?.name) ||
            nonEmptyString(column?.key) ||
            "";
        })
        .filter(Boolean)
        .slice(0, 8);
  }

  const firstObjectRow = Array.isArray(rows) ?
    rows.find((row) => row && typeof row === "object" && !Array.isArray(row)) :
    null;
  return firstObjectRow ? Object.keys(firstObjectRow).slice(0, 8) : [];
}

function normalizeAgentTableRows(rawRows, columns) {
  if (!Array.isArray(rawRows)) {
    return [];
  }

  return rawRows
      .slice(0, 12)
      .map((row) => {
        if (Array.isArray(row)) {
          return row.slice(0, 8).map(stringifyAgentResultValue);
        }
        if (row && typeof row === "object") {
          const keys = columns.length ? columns : Object.keys(row).slice(0, 8);
          return keys.map((key) => stringifyAgentResultValue(row[key]));
        }
        return [stringifyAgentResultValue(row)];
      })
      .filter((row) => row.some((cell) => cell.length > 0));
}

function normalizeAgentResultEntry(rawEntry, index = 0, options = {}) {
  const allowedLinkHosts = Array.isArray(options.allowedLinkHosts) ? options.allowedLinkHosts : [];
  const stripWorkflowFromAutomation = options.stripWorkflowFromAutomation === true;

  if (typeof rawEntry === "string") {
    const text = rawEntry.trim();
    return text ? {type: "text", text: trimTextMax(text, 2000)} : null;
  }

  if (!rawEntry || typeof rawEntry !== "object" || Array.isArray(rawEntry)) {
    return null;
  }

  const type = normalizeAgentResultType(rawEntry.type || rawEntry.kind || rawEntry.mediaType);
  if (stripWorkflowFromAutomation && type === "workflow") {
    return null;
  }
  const rawUrl = nonEmptyString(rawEntry.url) ||
    nonEmptyString(rawEntry.href) ||
    nonEmptyString(rawEntry.downloadUrl) ||
    nonEmptyString(rawEntry.downloadURL) ||
    "";
  const url = safeHttpsUrlStringForAgentResult(rawUrl, allowedLinkHosts);
  const title = nonEmptyString(rawEntry.title) ||
    nonEmptyString(rawEntry.name) ||
    nonEmptyString(rawEntry.filename) ||
    nonEmptyString(rawEntry.fileName) ||
    "";
  const text = nonEmptyString(rawEntry.text) ||
    nonEmptyString(rawEntry.message) ||
    nonEmptyString(rawEntry.summary) ||
    nonEmptyString(rawEntry.description) ||
    "";
  const html = nonEmptyString(rawEntry.html) || "";
  const rawRows = Array.isArray(rawEntry.rows) ? rawEntry.rows : [];
  const columns = normalizeAgentTableColumns(rawEntry.columns, rawRows);
  const rows = normalizeAgentTableRows(rawRows, columns);

  const normalized = {
    id: nonEmptyString(rawEntry.id) || `result_${index + 1}`,
    type,
  };

  if (url) normalized.url = trimTextMax(url, 2000);
  if (title) normalized.title = trimTextMax(title, 180);
  if (text) normalized.text = trimTextMax(text, 4000);
  if (html) normalized.html = trimTextMax(html, 8000);
  if (nonEmptyString(rawEntry.mimeType) || nonEmptyString(rawEntry.mime) || nonEmptyString(rawEntry.contentType)) {
    normalized.mimeType = nonEmptyString(rawEntry.mimeType) ||
      nonEmptyString(rawEntry.mime) ||
      nonEmptyString(rawEntry.contentType);
  }
  if (nonEmptyString(rawEntry.fileName) || nonEmptyString(rawEntry.filename)) {
    normalized.fileName = nonEmptyString(rawEntry.fileName) || nonEmptyString(rawEntry.filename);
  }
  if (columns.length) normalized.columns = columns;
  if (rows.length) normalized.rows = rows;

  if (type === "table" && !rows.length && !text) {
    return null;
  }
  if ((type === "image" || type === "video" || type === "audio" || type === "file" || type === "link") &&
    !url && !text) {
    return null;
  }
  if (type === "html" && !html && !text && !url) {
    return null;
  }

  return normalized;
}

function normalizeAgentResultEntries(rawResults, options = {}) {
  if (!Array.isArray(rawResults)) {
    return [];
  }

  return rawResults
      .slice(0, 12)
      .map((entry, index) => normalizeAgentResultEntry(entry, index, options))
      .filter(Boolean);
}

function parseAutomationResponseBody(bodyText) {
  const trimmed = typeof bodyText === "string" ? bodyText.trim() : "";
  if (!trimmed) {
    return null;
  }
  try {
    return JSON.parse(trimmed);
  } catch (error) {
    return null;
  }
}

function extractAutomationResponseMessage(bodyText, workflowName, parsedBody = null) {
  const trimmed = typeof bodyText === "string" ? bodyText.trim() : "";
  if (!trimmed) {
    return `Test an ${workflowName} gesendet.`;
  }

  if (parsedBody && typeof parsedBody === "object" && !Array.isArray(parsedBody)) {
    const message = nonEmptyString(parsedBody?.reply)
      || nonEmptyString(parsedBody?.message)
      || nonEmptyString(parsedBody?.status)
      || nonEmptyString(parsedBody?.result);
    if (message) {
      return message;
    }
  }

  return trimmed.length > 160 ? `Test an ${workflowName} gesendet.` : trimmed;
}

function extractAutomationResponseResults(parsedBody, options = {}) {
  if (Array.isArray(parsedBody)) {
    return normalizeAgentResultEntries(parsedBody, options);
  }

  if (!parsedBody || typeof parsedBody !== "object") {
    return [];
  }

  return normalizeAgentResultEntries(
      parsedBody.results ||
      parsedBody.outputs ||
      parsedBody.assets ||
      parsedBody.files ||
      [],
      options,
  );
}

function normalizeAutomationWorkflowStatus(value, fallback = "completed") {
  const normalized = nonEmptyString(value)?.trim().toLowerCase() || "";
  if (!normalized) {
    return fallback;
  }
  if (["queued", "queue", "pending", "accepted", "scheduled"].includes(normalized)) {
    return "queued";
  }
  if (["running", "processing", "in_progress", "in-progress", "started"].includes(normalized)) {
    return "running";
  }
  if (["completed", "complete", "success", "successful", "done", "ok"].includes(normalized)) {
    return "completed";
  }
  if (["failed", "error", "errored", "timeout", "cancelled", "canceled"].includes(normalized)) {
    return "failed";
  }
  return fallback;
}

async function triggerWorkflowAutomationWebhook({
  trigger,
  source,
  auth,
  data = {},
  runtimePolicy = {},
  automationScope = "owner",
}) {
  const scope = automationScope === "personal" ? "personal" : "owner";
  const settings = await loadWorkflowAutomationSettingsForUser(auth?.uid, scope);

  if (scope === "owner" && settings.provider !== "activepieces") {
    throw new HttpsError("failed-precondition", "Der globale Owner-Workflow nutzt Activepieces.");
  }

  if (scope === "personal" && !["activepieces", "n8n"].includes(settings.provider)) {
    throw new HttpsError("failed-precondition", "Persoenlicher Workflow-Provider ist ungueltig.");
  }

  if (settings.provider === "activepieces" && runtimePolicy.activepiecesEnabled === false) {
    throw new HttpsError("failed-precondition", "Activepieces ist fuer Agent-Workflows aktuell deaktiviert.");
  }

  if (settings.provider === "n8n" && runtimePolicy.n8nEnabled === false) {
    throw new HttpsError("failed-precondition", "n8n ist fuer persoenliche Agent-Workflows aktuell deaktiviert.");
  }

  if (!settings.isEnabled) {
    throw new HttpsError("failed-precondition", "Externer Workflow ist aktuell nicht aktiviert.");
  }

  const webhookUrl = buildAutomationWebhookUrl(settings.baseURL, settings.webhookPath);
  if (!webhookUrl) {
    throw new HttpsError("failed-precondition", "Externer Workflow ist noch nicht vollstaendig konfiguriert.");
  }

  const safeData = data && typeof data === "object" && !Array.isArray(data) ? data : {};
  const knowledgeContext = nonEmptyString(settings.knowledgeContext) || "";
  const requestId = `${Date.now()}_${Math.random().toString(36).slice(2, 10)}`;
  const payload = {
    provider: settings.provider,
    scope: settings.scope || "owner_global",
    requestId,
    workflowName: settings.workflowName,
    trigger,
    source,
    timestamp: new Date().toISOString(),
    data: {
      ...safeData,
      knowledgeContext,
      sourceRole: "agent_gateway",
    },
    knowledgeContext,
  };

  if (settings.sendsUserContext) {
    payload.user = await loadAutomationCaller(auth);
  }

  const payloadText = JSON.stringify(payload);
  const headers = {
    "Content-Type": "application/json",
    "Accept": "application/json",
    "x-skyos-request-id": requestId,
    "x-skydown-request-id": requestId,
  };

  if (settings.authHeaderName && settings.authHeaderValue) {
    headers[settings.authHeaderName] = settings.authHeaderValue;
  }
  if (settings.signingEnabled) {
    const signature = signAutomationPayload(payloadText, settings.signingSecret);
    if (!signature) {
      throw new HttpsError("failed-precondition", "Webhook Signing ist aktiv, aber signingSecret fehlt.");
    }
    headers[settings.signatureHeaderName] = `sha256=${signature}`;
  }

  const effectiveTimeoutMs = clampIntegerSetting(
      runtimePolicy.externalTimeoutMs,
      settings.timeoutMs,
      2000,
      30000,
  );
  const effectiveRetryAttempts = clampIntegerSetting(
      runtimePolicy.externalRetryAttempts,
      settings.retryAttempts,
      0,
      4,
  );

  let response = null;
  let responseBody = "";
  let lastError = null;
  const startedAt = Date.now();
  const attempts = Math.max(1, effectiveRetryAttempts + 1);
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const controller = new AbortController();
      const timeoutHandle = setTimeout(() => controller.abort(), effectiveTimeoutMs);
      try {
        response = await fetch(webhookUrl, {
          method: "POST",
          headers,
          body: payloadText,
          signal: controller.signal,
        });
      } finally {
        clearTimeout(timeoutHandle);
      }
      responseBody = await response.text().catch(() => "");
      if (response.ok || attempt >= attempts || (response.status < 500 && response.status !== 429)) {
        break;
      }
      await waitForMs(settings.retryBackoffMs * attempt);
    } catch (error) {
      lastError = error;
      if (attempt >= attempts) {
        break;
      }
      await waitForMs(settings.retryBackoffMs * attempt);
    }
  }

  const durationMs = Date.now() - startedAt;

  if (!response) {
    await createAgentExternalAuditEntry({
      requestId,
      uid: auth?.uid || "",
      provider: settings.provider,
      route: "external",
      trigger,
      source,
      workflowName: settings.workflowName,
      status: "network_error",
      state: "external_failed",
      reason: lastError instanceof Error ? lastError.message : "network_error",
      durationMs,
    });
    throw new HttpsError("internal", `${settings.provider} Netzwerkfehler: Webhook nicht erreichbar.`);
  }

  if (!response.ok) {
    logger.error("External workflow webhook trigger failed.", {
      trigger,
      source,
      workflowName: settings.workflowName,
      status: response.status,
      statusText: response.statusText,
      responseBody: responseBody.slice(0, 500),
      requestId,
    });
    await createAgentExternalAuditEntry({
      requestId,
      uid: auth?.uid || "",
      provider: settings.provider,
      route: "external",
      trigger,
      source,
      workflowName: settings.workflowName,
      status: "http_error",
      state: "external_failed",
      reason: `${response.status} ${response.statusText}`,
      httpStatus: response.status,
      durationMs,
    });
    throw new HttpsError("internal", `${settings.provider} Fehler (${response.status} ${response.statusText}).`);
  }

  logger.info("External workflow webhook triggered.", {
    trigger,
    source,
    workflowName: settings.workflowName,
    status: response.status,
    sendsUserContext: settings.sendsUserContext,
    requestId,
  });
  await createAgentExternalAuditEntry({
    requestId,
    uid: auth?.uid || "",
    provider: settings.provider,
    route: "external",
    trigger,
    source,
    workflowName: settings.workflowName,
    status: "ok",
    state: "external_completed",
    httpStatus: response.status,
    durationMs,
  });

  const parsedResponseBody = coerceAutomationWebhookParsedBody(
      parseAutomationResponseBody(responseBody),
  );
  const workflowStatus = normalizeAutomationWorkflowStatus(
      parsedResponseBody?.workflowStatus ||
      parsedResponseBody?.workflow_state ||
      parsedResponseBody?.state ||
      parsedResponseBody?.status ||
      "",
      "completed",
  );
  const allowedLinkHosts = normalizeAutomationAllowedLinkHosts(runtimePolicy);
  const automationResultOptions = {
    allowedLinkHosts,
    stripWorkflowFromAutomation: true,
  };
  return {
    message: extractAutomationResponseMessage(responseBody, settings.workflowName, parsedResponseBody),
    workflowName: settings.workflowName,
    status: response.status,
    provider: settings.provider,
    requestId,
    workflowStatus,
    schemaVersion: extractAutomationSchemaVersion(parsedResponseBody),
    results: extractAutomationResponseResults(parsedResponseBody, automationResultOptions),
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

function normalizeAiExperienceLevel(value) {
  const normalized = nonEmptyString(value)?.toLowerCase();
  return normalized && Object.values(AI_EXPERIENCE_LEVELS).includes(normalized) ?
    normalized :
    AI_EXPERIENCE_LEVELS.standard;
}

function aiLevelCounterField(kind, level) {
  return `level_${aiMetricKey(normalizeAiExperienceLevel(level))}_${aiUsageCounterField(kind)}`;
}

function aiLevelRequestWeightMultiplier(level) {
  switch (normalizeAiExperienceLevel(level)) {
    case AI_EXPERIENCE_LEVELS.pro:
      return 4;
    case AI_EXPERIENCE_LEVELS.advanced:
      return 2;
    case AI_EXPERIENCE_LEVELS.standard:
    default:
      return 1;
  }
}

function aiLevelOutputTokenMultiplier(level) {
  switch (normalizeAiExperienceLevel(level)) {
    case AI_EXPERIENCE_LEVELS.pro:
      return 1.2;
    case AI_EXPERIENCE_LEVELS.advanced:
      return 1;
    case AI_EXPERIENCE_LEVELS.standard:
    default:
      return 0.72;
  }
}

function aiLevelLimitForPlan({level, kind, baseLimit, hardCap, plan}) {
  const normalizedLevel = normalizeAiExperienceLevel(level);
  const safeBaseLimit = Number.isFinite(Number(baseLimit)) ? Math.max(0, Math.floor(Number(baseLimit))) : 0;
  const safeHardCap = Number.isFinite(Number(hardCap)) ? Math.max(0, Math.floor(Number(hardCap))) : safeBaseLimit;
  const effectiveBaseLimit = safeHardCap > 0 ? Math.min(safeBaseLimit, safeHardCap) : safeBaseLimit;
  const normalizedPlan = nonEmptyString(plan)?.toLowerCase() || USER_QUOTA_PLANS.free;

  if (normalizedLevel === AI_EXPERIENCE_LEVELS.standard) {
    return Math.max(1, effectiveBaseLimit);
  }

  const isStaffPlan = [
    USER_QUOTA_PLANS.internalTeam,
    USER_QUOTA_PLANS.ownerUnlimited,
    USER_QUOTA_PLANS.studio,
  ].includes(normalizedPlan);
  const isCreatorPlan = normalizedPlan === USER_QUOTA_PLANS.creator;

  if (normalizedLevel === AI_EXPERIENCE_LEVELS.advanced) {
    const planCap = isStaffPlan ? 24 : isCreatorPlan ? 12 : 3;
    const ratio = isStaffPlan ? 0.45 : isCreatorPlan ? 0.35 : 0.12;
    const floor = isStaffPlan ? 4 : isCreatorPlan ? 2 : 1;
    return Math.max(floor, Math.min(planCap, Math.floor(effectiveBaseLimit * ratio) || floor));
  }

  if (!isStaffPlan && !isCreatorPlan) {
    return 0;
  }

  const proCap = isStaffPlan ? 10 : 4;
  const proRatio = isStaffPlan ? 0.18 : 0.12;
  const proFloor = isStaffPlan ? 2 : 1;
  const baseProLimit = Math.floor(effectiveBaseLimit * proRatio) || proFloor;
  return Math.max(proFloor, Math.min(proCap, baseProLimit));
}

function resolveAiTextModelForLevel(level, runtimeSettings) {
  const modelPolicy = runtimeSettings.bot.modelPolicy;
  switch (normalizeAiExperienceLevel(level)) {
    case AI_EXPERIENCE_LEVELS.standard:
      return nonEmptyString(modelPolicy.textFallbackModel) ||
        nonEmptyString(modelPolicy.textPrimaryModel) ||
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.textPrimaryModel;
    case AI_EXPERIENCE_LEVELS.advanced:
    case AI_EXPERIENCE_LEVELS.pro:
    default:
      return nonEmptyString(modelPolicy.textPrimaryModel) ||
        DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.textPrimaryModel;
  }
}

function resolveAiVisualModelForLevel(level, runtimeSettings) {
  const modelPolicy = runtimeSettings.bot.modelPolicy;
  return nonEmptyString(modelPolicy.visualPrimaryModel) ||
    nonEmptyString(modelPolicy.visualFallbackModel) ||
    DEFAULT_AI_RUNTIME_SETTINGS.bot.modelPolicy.visualPrimaryModel;
}

function resolveAiAgentModelForProvider(provider, runtimeSettings) {
  switch (provider) {
    case AI_AGENT_PROVIDERS.grok:
      return GROK_AGENT_MODEL;
    case AI_AGENT_PROVIDERS.manus:
      return "manus";
    case AI_AGENT_PROVIDERS.gemini:
    default:
      return resolveAiTextModelForLevel(AI_EXPERIENCE_LEVELS.standard, runtimeSettings);
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
  const normalizedPlan = normalizeAiSubscriptionPlan(plan) || nonEmptyString(plan)?.toLowerCase() || USER_QUOTA_PLANS.free;
  if (
    normalizedPlan === USER_QUOTA_PLANS.studio ||
    normalizedPlan === USER_QUOTA_PLANS.ownerUnlimited ||
    normalizedPlan === USER_QUOTA_PLANS.internalTeam
  ) {
    return runtimeSettings.planRouting.studio;
  }
  if (normalizedPlan === USER_QUOTA_PLANS.creator) {
    return runtimeSettings.planRouting.creator;
  }
  return runtimeSettings.planRouting.free;
}

function hasActivePaidAiEntitlement(entitlement = {}) {
  const plan = normalizeAiSubscriptionPlan(entitlement.plan);
  return Boolean(plan) && isAiSubscriptionStatusActive(entitlement.status);
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
  } else if (denyReason === "paid_subscription_required") {
    userFacingReason = "SkyOS AI braucht ein aktives Pro- oder Creator-Abo. So werden AI-Kosten planbasiert freigeschaltet.";
  } else if (denyReason === "plan_provider_not_allowed") {
    userFacingReason = "Diese Modellklasse ist in deinem Plan nicht freigeschaltet.";
  } else if (denyReason === "plan_cost_cap_exceeded") {
    userFacingReason = "Diese Anfrage ist gerade zu rechenintensiv fuer dein Plan-Budget.";
  } else if (denyReason === "daily_burn_cap_reached") {
    userFacingReason = "SkyOS reduziert gerade Lastspitzen, um Stabilitaet und Fairness zu sichern.";
  } else if (denyReason === "suspicious_spike") {
    userFacingReason = "Wir sehen gerade ungewoehnlich viele Requests in kurzer Zeit.";
  } else if (denyReason === "ai_level_unavailable") {
    userFacingReason = "Dieser Modus ist fuer deinen aktuellen Zugang noch nicht verfuegbar.";
  } else if (denyReason === "ai_level_limit_reached") {
    userFacingReason = "Dein heutiges AI-Limit ist erreicht. Morgen kannst du weiterarbeiten oder einen hoeheren Modus nutzen, sobald er freigeschaltet ist.";
  } else if (denyReason === "hard_limit_reached") {
    userFacingReason = "Dein heutiges AI-Limit ist erreicht. Morgen kannst du weiterarbeiten oder einen hoeheren Modus nutzen, sobald er freigeschaltet ist.";
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
    aiLevel: normalizeAiExperienceLevel(payload.aiLevel),
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
  aiLevel = AI_EXPERIENCE_LEVELS.standard,
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
  const quotaPlan = profile.aiLimits?.quotaPlan || effectiveEntitlement.plan || USER_QUOTA_PLANS.free;
  const plan = effectiveEntitlement.plan || quotaPlan || USER_QUOTA_PLANS.free;
  const normalizedAiLevel = normalizeAiExperienceLevel(aiLevel);
  const routingPolicy = resolvePlanRoutingPolicy(runtimeSettings, plan);
  const effectiveCapabilities = profile.isStaff ?
    resolveAiCapabilities({plan: quotaPlan, status: "active", role: profile.role}) :
    effectiveEntitlement.capabilities;
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
      aiLevel: normalizedAiLevel,
      success: false,
      denyReason,
      sourceRoute,
      functionName,
      resultType,
      requestId: normalizedRequestId,
    });
    const errorCode = ["paid_subscription_required", "plan_feature_not_allowed"].includes(denyReason) ?
      "failed-precondition" :
      "resource-exhausted";
    throw new HttpsError(errorCode, hints.userFacingReason, {
      denyReason,
      ...hints,
      allow: false,
      degrade: allowDegrade && runtimeSettings.costGuardrails.enableDegradeMode,
      requestId: normalizedRequestId,
    });
  };

  if (!profile.isStaff && !hasActivePaidAiEntitlement(effectiveEntitlement)) {
    await denyAndRecord("paid_subscription_required");
  }

  if (!featureAllowedByCapability(effectiveCapabilities, normalizedFeatureClass)) {
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
    const levelCounterField = aiLevelCounterField(kind, normalizedAiLevel);
    const currentLevelCount = Number(currentData[levelCounterField]) || 0;
    const levelLimit = runtimeSettings.costGuardEnabled ?
      aiLevelLimitForPlan({
        level: normalizedAiLevel,
        kind,
        baseLimit,
        hardCap,
        plan: quotaPlan,
      }) :
      limit;

    if (currentCount >= limit) {
      denyReasonInTransaction = "hard_limit_reached";
      throw new HttpsError("resource-exhausted", aiLimitReachedMessage(kind, limit));
    }

    if (levelLimit <= 0) {
      denyReasonInTransaction = "ai_level_unavailable";
      throw new HttpsError("permission-denied", "Dieser Modus ist fuer deinen aktuellen Zugang noch nicht verfuegbar.");
    }

    if (currentLevelCount >= levelLimit) {
      denyReasonInTransaction = "ai_level_limit_reached";
      throw new HttpsError("resource-exhausted", "Dein heutiges AI-Limit ist erreicht. Morgen kannst du weiterarbeiten oder einen hoeheren Modus nutzen, sobald er freigeschaltet ist.");
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
    const nextLevelCount = currentLevelCount + 1;
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
    const levelKey = aiMetricKey(normalizedAiLevel);
    const byLevel = {...(currentMonthlyData.byLevel || {})};
    byLevel[levelKey] = (Number(byLevel[levelKey]) || 0) + normalizedWeight;
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
      [levelCounterField]: nextLevelCount,
      recentBurstCount: nextBurstCount,
      lastEventType: normalizedEventType,
      lastProvider: normalizedProvider,
      lastModel: normalizedModel || admin.firestore.FieldValue.delete(),
      lastAiLevel: normalizedAiLevel,
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
      byLevel,
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
      aiLevel: normalizedAiLevel,
      remainingForKind: Math.max(limit - nextCount, 0),
      limitForKind: limit,
      remainingForLevel: Math.max(levelLimit - nextLevelCount, 0),
      limitForLevel: levelLimit,
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
      aiLevel: normalizedAiLevel,
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
      aiLevel: normalizedAiLevel,
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
    aiLevel: normalizedAiLevel,
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
    aiLevel: usageSummary.aiLevel,
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
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      mode: nonEmptyString(mode) || "release",
      agentProvider: nonEmptyString(agentProvider) || "unknown",
      providerFallbackUsed: providerFallbackUsed === true,
      automationAttempted: automation && automation.attempted === true,
      automationTriggered: automation && automation.triggered === true,
      automationStatus: nonEmptyString(automation?.workflowStatus) || (
        automation && automation.attempted === true ? "failed" : "completed"
      ),
      workflowName: nonEmptyString(automation?.workflowName) || "",
      automationMessage: trimTextMax(nonEmptyString(automation?.message) || "", 1200),
      workflowStep: "",
      workflowDetails: "",
      workflowEtaSeconds: 0,
      workflowProgressPercent: 0,
      automationRequestId: nonEmptyString(automation?.requestId) || "",
      automationSchemaVersion: trimTextMax(nonEmptyString(automation?.schemaVersion) || "", 64),
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

async function updateAgentRunStatus({
  uid,
  runId,
  state,
  message = "",
  workflowName = "",
  provider = "",
  requestId = "",
  progressPercent = null,
  step = "",
  etaSeconds = null,
  details = "",
}) {
  const normalizedUid = nonEmptyString(uid) || "";
  const normalizedRunId = nonEmptyString(runId) || "";
  if (!normalizedUid || !normalizedRunId) {
    return false;
  }
  const resolved = await resolveAgentRunRef({
    uid: normalizedUid,
    runId: normalizedRunId,
    requestId,
  });
  if (!resolved) {
    return false;
  }
  const runRef = resolved.ref;
  const snapshot = await runRef.get();
  if (!snapshot.exists) {
    return false;
  }
  const normalizedState = normalizeAutomationWorkflowStatus(state, "running");
  const normalizedProgress = normalizeAgentRunProgress(progressPercent);
  const normalizedEta = clampIntegerSetting(etaSeconds, 0, 0, 86400);
  await runRef.set({
    automationAttempted: true,
    automationTriggered: normalizedState !== "failed",
    automationStatus: normalizedState,
    automationMessage: trimTextMax(nonEmptyString(message) || "", 1200),
    workflowName: nonEmptyString(workflowName) || "",
    workflowStep: trimTextMax(nonEmptyString(step) || "", 160),
    workflowDetails: trimTextMax(nonEmptyString(details) || "", 1200),
    workflowEtaSeconds: normalizedEta,
    workflowProgressPercent: normalizedProgress === null ? (
      normalizedState === "completed" ? 100 : (normalizedState === "failed" ? 0 : 0)
    ) : normalizedProgress,
    automationRequestId: nonEmptyString(requestId) || "",
    agentProvider: nonEmptyString(provider) || (snapshot.data()?.agentProvider || ""),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  return true;
}

async function resolveAgentRunRef({uid = "", runId = "", requestId = ""}) {
  const normalizedUid = nonEmptyString(uid) || "";
  const normalizedRunId = nonEmptyString(runId) || "";
  const normalizedRequestId = nonEmptyString(requestId) || "";

  if (normalizedUid && normalizedRunId) {
    const directRef = admin.firestore()
        .collection("users")
        .doc(normalizedUid)
        .collection("agentRuns")
        .doc(normalizedRunId);
    const directSnapshot = await directRef.get();
    if (directSnapshot.exists) {
      return {uid: normalizedUid, runId: normalizedRunId, ref: directRef};
    }
  }

  if (!normalizedRequestId) {
    return null;
  }

  if (normalizedUid) {
    const scopedSnapshot = await admin.firestore()
        .collection("users")
        .doc(normalizedUid)
        .collection("agentRuns")
        .where("automationRequestId", "==", normalizedRequestId)
        .orderBy("createdAt", "desc")
        .limit(1)
        .get();
    if (!scopedSnapshot.empty) {
      const doc = scopedSnapshot.docs[0];
      return {uid: normalizedUid, runId: doc.id, ref: doc.ref};
    }
  }

  const collectionSnapshot = await admin.firestore()
      .collectionGroup("agentRuns")
      .where("automationRequestId", "==", normalizedRequestId)
      .orderBy("createdAt", "desc")
      .limit(1)
      .get();
  if (collectionSnapshot.empty) {
    return null;
  }
  const doc = collectionSnapshot.docs[0];
  const parentUserId = doc.ref.parent.parent?.id || "";
  if (!parentUserId) {
    return null;
  }
  return {
    uid: parentUserId,
    runId: doc.id,
    ref: doc.ref,
  };
}

exports.getAgentRunStatus = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "getAgentRunStatus");
  const uid = assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um den Run-Status abzurufen.",
  );
  const runId = nonEmptyString(request.data?.runId) || "";
  if (!runId || runId.length < 8 || runId.length > 120) {
    throw new HttpsError("invalid-argument", "Ungueltige runId.");
  }

  const runRef = admin.firestore()
      .collection("users")
      .doc(uid)
      .collection("agentRuns")
      .doc(runId);
  const snapshot = await runRef.get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Agent-Run wurde nicht gefunden.");
  }

  const data = snapshot.data() || {};
  const automationAttempted = data.automationAttempted === true;
  const automationTriggered = data.automationTriggered === true;
  const state = normalizeAutomationWorkflowStatus(
      data.automationStatus,
      automationAttempted ? (automationTriggered ? "completed" : "failed") : "completed",
  );
  const workflowName = nonEmptyString(data.workflowName) || "";
  const automationMessage = nonEmptyString(data.automationMessage) || "";
  const workflowStep = nonEmptyString(data.workflowStep) || "";
  const workflowDetails = nonEmptyString(data.workflowDetails) || "";
  const workflowEtaSeconds = clampIntegerSetting(data.workflowEtaSeconds, 0, 0, 86400);
  const workflowProgressPercent = normalizeAgentRunProgress(data.workflowProgressPercent);
  return {
    runId,
    state,
    automationAttempted,
    automationTriggered,
    workflowName,
    automationMessage,
    workflowStep,
    workflowDetails,
    workflowEtaSeconds,
    workflowProgressPercent: workflowProgressPercent === null ? (
      state === "completed" ? 100 : (state === "failed" ? 0 : 0)
    ) : workflowProgressPercent,
    provider: nonEmptyString(data.agentProvider) || "",
    automationSchemaVersion: trimTextMax(nonEmptyString(data.automationSchemaVersion) || "", 64),
    updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ?
      data.updatedAt.toDate().toISOString() :
      (
        data.createdAt instanceof admin.firestore.Timestamp ?
          data.createdAt.toDate().toISOString() :
          ""
      ),
  };
});

exports.agentRunStatusCallback = onRequest({
  region: "us-central1",
  timeoutSeconds: 30,
  secrets: [agentRunCallbackSecret],
}, async (request, response) => {
  if (request.method !== "POST") {
    response.status(405).send("Method Not Allowed");
    return;
  }
  const expectedSecret = nonEmptyString(agentRunCallbackSecret.value()) || "";
  if (!expectedSecret) {
    logger.error("Agent run callback secret missing.");
    response.status(500).json({ok: false, error: "callback_secret_missing"});
    return;
  }
  const providedSecret = nonEmptyString(request.headers["x-skyos-run-callback-secret"]) || "";
  if (!providedSecret || providedSecret !== expectedSecret) {
    response.status(401).json({ok: false, error: "unauthorized"});
    return;
  }

  const payload = request.body && typeof request.body === "object" ? request.body : {};
  const uid = nonEmptyString(payload.uid) || "";
  const runId = nonEmptyString(payload.runId) || "";
  const requestId = nonEmptyString(payload.requestId) || "";
  const state = normalizeAutomationWorkflowStatus(payload.state || payload.status || payload.workflowStatus || "", "running");
  const message = nonEmptyString(payload.message) || nonEmptyString(payload.summary) || "";
  const workflowName = nonEmptyString(payload.workflowName) || "";
  const provider = nonEmptyString(payload.provider) || "";
  const progressPercent = normalizeAgentRunProgress(payload.progressPercent ?? payload.progress ?? payload.percent);
  const step = nonEmptyString(payload.step) || "";
  const details = nonEmptyString(payload.details) || "";
  const etaSeconds = clampIntegerSetting(payload.etaSeconds, 0, 0, 86400);

  if ((!uid || !runId) && !requestId) {
    response.status(400).json({ok: false, error: "uid_runId_or_requestId_required"});
    return;
  }
  try {
    const updated = await updateAgentRunStatus({
      uid,
      runId,
      state,
      message,
      workflowName,
      provider,
      requestId,
      progressPercent,
      step,
      etaSeconds,
      details,
    });
    if (!updated) {
      response.status(404).json({ok: false, error: "run_not_found"});
      return;
    }
    const resolved = await resolveAgentRunRef({uid, runId, requestId});
    response.status(200).json({
      ok: true,
      runId: resolved?.runId || runId,
      uid: resolved?.uid || uid,
      state,
    });
  } catch (error) {
    logger.error("Agent run callback failed.", {
      uid,
      runId,
      state,
      error: error instanceof Error ? error.message : `${error}`,
    });
    response.status(500).json({ok: false, error: "internal_error"});
  }
});

exports.createReminderFromWorkflow = onRequest({
  region: "us-central1",
  timeoutSeconds: 30,
  secrets: [workflowApiSecret],
}, async (request, response) => {
  const payload = assertWorkflowHttpRequest(request, response);
  if (!payload) {
    return;
  }
  try {
    const {uid, source, requestId} = getWorkflowRequestBase(payload);
    const title = nonEmptyString(payload.title);
    if (!title) {
      response.status(400).json({ok: false, error: "title_required"});
      return;
    }
    if (title.length > 180) {
      response.status(400).json({ok: false, error: "title_too_long"});
      return;
    }
    const bodyRaw = nonEmptyString(payload.body) || "";
    if (bodyRaw.length > 2000) {
      response.status(400).json({ok: false, error: "body_too_long"});
      return;
    }
    const body = trimOptionalText(payload.body, 2000);
    const scheduledAt = parseOptionalIsoTimestamp(payload.scheduledAt, "scheduledAt", {required: true});
    const timezone = nonEmptyString(payload.timezone);
    if (!timezone) {
      response.status(400).json({ok: false, error: "timezone_required"});
      return;
    }
    const now = admin.firestore.FieldValue.serverTimestamp();
    const reminderRef = admin.firestore()
        .collection("users")
        .doc(uid)
        .collection("reminders")
        .doc();
    await reminderRef.set({
      title: trimTextMax(title, 180),
      body,
      scheduledAt,
      timezone: trimTextMax(timezone, 80),
      status: REMINDER_STATUSES.scheduled,
      source,
      requestId,
      createdAt: now,
      updatedAt: now,
    });
    response.status(200).json({ok: true, uid, reminderId: reminderRef.id});
  } catch (error) {
    if (error instanceof HttpsError && error.code === "invalid-argument") {
      response.status(400).json({ok: false, error: "invalid_argument", message: error.message});
      return;
    }
    logger.error("createReminderFromWorkflow failed.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
    response.status(500).json({ok: false, error: "internal_error"});
  }
});

exports.createTaskFromWorkflow = onRequest({
  region: "us-central1",
  timeoutSeconds: 30,
  secrets: [workflowApiSecret],
}, async (request, response) => {
  const payload = assertWorkflowHttpRequest(request, response);
  if (!payload) {
    return;
  }
  try {
    const {uid, source, requestId} = getWorkflowRequestBase(payload);
    const title = nonEmptyString(payload.title);
    if (!title) {
      response.status(400).json({ok: false, error: "title_required"});
      return;
    }
    if (title.length > 180) {
      response.status(400).json({ok: false, error: "title_too_long"});
      return;
    }
    const descriptionRaw = nonEmptyString(payload.description) || "";
    if (descriptionRaw.length > 5000) {
      response.status(400).json({ok: false, error: "description_too_long"});
      return;
    }
    const description = trimOptionalText(payload.description, 5000);
    const priorityRaw = nonEmptyString(payload.priority)?.toLowerCase() || "normal";
    const priority = ["low", "normal", "high"].includes(priorityRaw) ? priorityRaw : "normal";
    const dueAt = parseOptionalIsoTimestamp(payload.dueAt, "dueAt");
    const now = admin.firestore.FieldValue.serverTimestamp();
    const taskRef = admin.firestore()
        .collection("users")
        .doc(uid)
        .collection("tasks")
        .doc();
    await taskRef.set({
      title: trimTextMax(title, 180),
      description,
      dueAt: dueAt || null,
      priority,
      status: "open",
      source,
      requestId,
      createdAt: now,
      updatedAt: now,
      completedAt: null,
    });
    response.status(200).json({ok: true, uid, taskId: taskRef.id});
  } catch (error) {
    if (error instanceof HttpsError && error.code === "invalid-argument") {
      response.status(400).json({ok: false, error: "invalid_argument", message: error.message});
      return;
    }
    logger.error("createTaskFromWorkflow failed.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
    response.status(500).json({ok: false, error: "internal_error"});
  }
});

exports.createNoteFromWorkflow = onRequest({
  region: "us-central1",
  timeoutSeconds: 30,
  secrets: [workflowApiSecret],
}, async (request, response) => {
  const payload = assertWorkflowHttpRequest(request, response);
  if (!payload) {
    return;
  }
  try {
    const {uid, source, requestId} = getWorkflowRequestBase(payload);
    const title = nonEmptyString(payload.title);
    if (!title) {
      response.status(400).json({ok: false, error: "title_required"});
      return;
    }
    if (title.length > 180) {
      response.status(400).json({ok: false, error: "title_too_long"});
      return;
    }
    const contentRaw = nonEmptyString(payload.content) || "";
    if (contentRaw.length > 5000) {
      response.status(400).json({ok: false, error: "content_too_long"});
      return;
    }
    const content = trimOptionalText(payload.content, 5000);
    const now = admin.firestore.FieldValue.serverTimestamp();
    const noteRef = admin.firestore()
        .collection("users")
        .doc(uid)
        .collection("notes")
        .doc();
    await noteRef.set({
      title: trimTextMax(title, 180),
      content,
      source,
      requestId,
      createdAt: now,
      updatedAt: now,
    });
    response.status(200).json({ok: true, uid, noteId: noteRef.id});
  } catch (error) {
    if (error instanceof HttpsError && error.code === "invalid-argument") {
      response.status(400).json({ok: false, error: "invalid_argument", message: error.message});
      return;
    }
    logger.error("createNoteFromWorkflow failed.", {
      error: error instanceof Error ? error.message : `${error}`,
    });
    response.status(500).json({ok: false, error: "internal_error"});
  }
});

const FAQ_TOPIC_DEFINITIONS = Object.freeze([
  {
    key: "app_usage",
    title: "SkyOS App Nutzung",
    keywords: ["app", "skyos", "nutzen", "benutzen", "home", "music", "video", "shop", "profile", "settings"],
  },
  {
    key: "account_login",
    title: "Account / Login / Probleme",
    keywords: ["account", "konto", "login", "anmelden", "einloggen", "passwort", "session", "signin", "sign in"],
  },
  {
    key: "membership",
    title: "Membership",
    keywords: ["membership", "abo", "upgrade", "restore", "plan", "creator", "pro", "billing", "subscription"],
  },
  {
    key: "ai_features",
    title: "AI Features",
    keywords: ["ai", "bot", "agent", "visual", "image", "workflow", "limit", "quota", "prompt"],
  },
  {
    key: "merch_orders_shipping",
    title: "Merch / Orders / Versand",
    keywords: ["merch", "order", "bestellung", "checkout", "cart", "warenkorb", "versand", "shipping", "tracking"],
  },
  {
    key: "music_media",
    title: "Music / Beat / Media",
    keywords: ["music", "track", "song", "beat", "artist", "video", "media", "player", "spotify", "youtube"],
  },
  {
    key: "privacy_security",
    title: "Datenschutz / Sicherheit",
    keywords: ["privacy", "datenschutz", "security", "sicherheit", "dsgvo", "private", "daten", "legal"],
  },
  {
    key: "contact_help",
    title: "Kontakt / Hilfe",
    keywords: ["kontakt", "hilfe", "help", "support", "email", "mail", "error", "fehler", "problem"],
  },
  {
    key: "product_intro",
    title: "Produkt-Erklaerung fuer neue Nutzer",
    keywords: ["neu", "new user", "einsteiger", "start", "schnellstart", "was ist", "wofuer", "erklaer", "guide", "faq"],
  },
]);

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

function promptLooksLikeFaq(prompt) {
  const normalized = nonEmptyString(prompt)?.toLowerCase() || "";
  if (!normalized) {
    return false;
  }

  if (normalized.includes("?")) {
    return true;
  }

  return [
    "wie ",
    "was ",
    "wo ",
    "warum ",
    "wann ",
    "kann ich",
    "hilfe",
    "help",
    "faq",
    "problem",
    "error",
    "support",
    "restore",
    "versand",
    "login",
    "konto",
    "membership",
  ].some((fragment) => normalized.includes(fragment));
}

function resolveFaqTopic(prompt) {
  const normalized = nonEmptyString(prompt)?.toLowerCase() || "";
  if (!normalized) {
    return null;
  }

  for (const definition of FAQ_TOPIC_DEFINITIONS) {
    if (definition.keywords.some((keyword) => normalized.includes(keyword))) {
      return definition;
    }
  }

  if (promptLooksLikeFaq(normalized)) {
    return {
      key: "general",
      title: "Allgemeine Hilfe",
      keywords: [],
    };
  }

  return null;
}

function resolveFaqRoute({prompt, mode, botRuntime}) {
  const topic = resolveFaqTopic(prompt);
  const looksLikeFaq = promptLooksLikeFaq(prompt);

  if (mode === AI_TEXT_MODES.faq) {
    return {
      useFaq: true,
      topic,
      routeReason: topic ?
        `FAQ-Modus explizit gewaehlt (${topic.title}).` :
        "FAQ-Modus explizit gewaehlt.",
    };
  }

  if (botRuntime.faqMode === "off") {
    return {
      useFaq: false,
      topic,
      routeReason: "FAQ-Routing ist owner-seitig deaktiviert.",
    };
  }

  if (!botRuntime.routingPolicy.preferFaqWhenTopicMatched) {
    return {
      useFaq: false,
      topic,
      routeReason: "Freier Assistant-Modus ohne automatische FAQ-Priorisierung.",
    };
  }

  if (botRuntime.faqMode === "prefer_faq" && (topic || looksLikeFaq)) {
    return {
      useFaq: true,
      topic,
      routeReason: topic ?
        `FAQ priorisiert, weil die Anfrage wie ${topic.title} aussieht.` :
        "FAQ priorisiert, weil die Anfrage wie eine Hilfe-Frage aussieht.",
    };
  }

  if (botRuntime.faqMode === "auto" && topic && (looksLikeFaq || topic.key === "product_intro")) {
    return {
      useFaq: true,
      topic,
      routeReason: `FAQ automatisch gewaehlt, weil die Anfrage zu ${topic.title} passt.`,
    };
  }

  return {
    useFaq: false,
    topic,
    routeReason: topic ?
      `Thema ${topic.title} erkannt, aber die Anfrage wirkt eher offen/kreativ.` :
      "Keine klare FAQ-/Help-Anfrage erkannt.",
  };
}

function shouldExposeVerboseBotDiagnostics(botRuntime, role) {
  if (botRuntime.diagnosticsMode === "verbose") {
    return true;
  }
  return botRuntime.diagnosticsMode === "owner_only" && role === USER_ROLES.owner;
}

function resolveTextGenerationConfig({botRuntime, isFaq, warningLevel, aiLevel = AI_EXPERIENCE_LEVELS.standard}) {
  const qualityMode = botRuntime.qualityMode;
  const answerLength = botRuntime.answerLength;
  const shortMax = botRuntime.costGuard.shortAnswerMaxOutputTokens;
  const standardMax = botRuntime.costGuard.standardAnswerMaxOutputTokens;

  let maxOutputTokens = isFaq ? Math.min(420, standardMax) : standardMax;
  let temperature = isFaq ? 0.24 : (qualityMode === "high" ? 0.62 : 0.7);
  let responseLimited = false;
  let responseLimitReason = "";

  if (answerLength === "short") {
    maxOutputTokens = Math.min(maxOutputTokens, shortMax);
    responseLimited = true;
    responseLimitReason = "Antwortlaenge ist owner-seitig auf kurz gestellt.";
  } else if (answerLength === "detailed") {
    maxOutputTokens = Math.min(Math.max(maxOutputTokens, isFaq ? 560 : 960), 1400);
  }

  if (botRuntime.costGuard.enabled &&
    botRuntime.costGuard.preferBriefAnswersWhenCritical &&
    warningLevel === "critical") {
    maxOutputTokens = Math.min(maxOutputTokens, shortMax);
    responseLimited = true;
    responseLimitReason = "Antwort wurde wegen aktivem Cost Guard kuerzer gehalten.";
  }

  if (isFaq) {
    temperature = qualityMode === "high" ? 0.18 : 0.24;
  }

  maxOutputTokens = Math.max(
      120,
      Math.min(1400, Math.floor(maxOutputTokens * aiLevelOutputTokenMultiplier(aiLevel))),
  );

  return {
    temperature,
    maxOutputTokens,
    responseLimited,
    responseLimitReason,
  };
}

function composeFaqKnowledge(promptSettings) {
  const sections = [promptSettings.faqKnowledgeBase];
  if (promptSettings.assetReferenceNotes) {
    sections.push(`Owner / Zusatzkontext:\n${promptSettings.assetReferenceNotes}`);
  }
  return sections.filter(Boolean).join("\n\n");
}

function aiTextModeHint(mode) {
  switch (mode) {
    case AI_TEXT_MODES.faq:
      return "Modus FAQ: Gib eine kurze, ehrliche Hilfe-Antwort. Nur wenn Fakten fehlen, sage klar, dass du es nicht sicher weisst.";
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

function composeTextGenerationPrompt(inputPrompt, textInstruction, mode, assetContext, botRuntime = null) {
  const runtimeLines = botRuntime ? `
Bot Runtime:
- promptVersion: ${botRuntime.promptVersion}
- qualityMode: ${botRuntime.qualityMode}
- personalityStyle: ${botRuntime.personalityStyle}
- answerLength: ${botRuntime.answerLength}
` : "";
  return `
${textInstruction}

${runtimeLines}

${aiTextModeHint(mode)}

${assetContext ? `Verfuegbare Referenzen:\n${assetContext}\n` : ""}

Nutzeranfrage:
${inputPrompt}
  `.trim();
}

function composeFaqPrompt({
  inputPrompt,
  promptSettings,
  botRuntime,
  topic,
  liveFacts,
}) {
  const faqKnowledge = composeFaqKnowledge(promptSettings);
  const safetyNotes = botRuntime.safetyPolicy.safeModeEnabled ?
    "- Safe Mode ist aktiv: keine riskanten Behauptungen, keine faulen Schluessen.\n" :
    "";
  const unknownHandling = botRuntime.safetyPolicy.strictUnknownHandling ?
    "- Wenn Fakten fehlen oder unsicher sind, sag klar, dass die Information gerade nicht sicher vorliegt.\n" :
    "";
  const speculationBlock = botRuntime.safetyPolicy.blockSpeculativeFaqAnswers ?
    "- Erfinde keine Preis-, Versand-, Account- oder Membership-Details.\n" :
    "";
  const faqPriorityMode = botRuntime?.actionLayer?.faqPriorityMode || "live_owner_generic";
  const priorityHint = liveFacts ?
    (faqPriorityMode === "owner_live_generic" ?
      "Prioritaet: Nutze zuerst Owner Knowledge. Nutze Live FAQ-Fakten danach fuer Verifizierung und Aktualitaet.\n" :
      faqPriorityMode === "balanced" ?
        "Prioritaet: Balanciere Live FAQ-Fakten und Owner Knowledge. Bei Konflikten bevorzugt die verifizierbare Live-Faktlage.\n" :
        "Prioritaet: Nutze zuerst die Live FAQ-Fakten aus dem System. Nutze Owner-Wissen danach als Zusatzkontext.\n") :
    "";

  return `
${promptSettings.faqInstruction}

Bot Runtime:
- promptVersion: ${botRuntime.promptVersion}
- qualityMode: ${botRuntime.qualityMode}
- faqMode: ${botRuntime.faqMode}
- ownerMode: ${botRuntime.ownerMode}
- answerLength: ${botRuntime.answerLength}
- personalityStyle: ${botRuntime.personalityStyle}

${safetyNotes}${unknownHandling}${speculationBlock}${priorityHint}
Erkannter FAQ-Bereich: ${topic?.title || "Allgemeine Hilfe"}

${liveFacts ? `Live FAQ-Fakten aus SkyOS:\n${liveFacts}\n\n` : ""}Verfuegbare FAQ-/Help-Fakten aus Owner Knowledge:
${faqKnowledge}

Nutzeranfrage:
${inputPrompt}
  `.trim();
}

function composeVisualGenerationPrompt(inputPrompt, visualInstruction, assetContext, botRuntime = null) {
  const runtimeLines = botRuntime ? `
Bot Runtime:
- promptVersion: ${botRuntime.promptVersion}
- qualityMode: ${botRuntime.qualityMode}
- personalityStyle: ${botRuntime.personalityStyle}
` : "";
  return `
${visualInstruction}

${runtimeLines}

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

function buildBotDecision({
  state = "complete",
  route = "assistant",
  routeReason = "",
  summary = "",
  topic = "",
  runtimeSettings,
  model = "",
  provider = "google_vertex",
  fallbackActivated = false,
  fallbackReason = "",
  responseLimited = false,
  responseLimitReason = "",
  blocked = false,
  blockReason = "",
  retryable = false,
  retryReason = "",
  diagnostics = [],
  role = USER_ROLES.user,
}) {
  const botRuntime = runtimeSettings.bot;
  const verboseDiagnostics = shouldExposeVerboseBotDiagnostics(botRuntime, role);
  const trace = [
    routeReason,
    model ? `Modell: ${model}` : "",
    fallbackActivated && fallbackReason ? `Fallback: ${fallbackReason}` : "",
    responseLimited && responseLimitReason ? `Limitierung: ${responseLimitReason}` : "",
    blocked && blockReason ? `Block: ${blockReason}` : "",
    retryable && retryReason ? `Retry: ${retryReason}` : "",
    ...diagnostics,
  ].filter(Boolean);

  return {
    state,
    route,
    topic,
    summary: summary || routeReason || "Antwort wurde verarbeitet.",
    promptVersion: botRuntime.promptVersion,
    qualityMode: botRuntime.qualityMode,
    faqMode: botRuntime.faqMode,
    ownerMode: botRuntime.ownerMode,
    answerLength: botRuntime.answerLength,
    personalityStyle: botRuntime.personalityStyle,
    loggingLevel: botRuntime.loggingLevel,
    diagnosticsMode: botRuntime.diagnosticsMode,
    ownerDiagnosticActive: verboseDiagnostics && botRuntime.ownerMode === "diagnostic",
    selectedModel: model,
    selectedProvider: provider,
    fallbackActivated,
    fallbackReason,
    responseLimited,
    responseLimitReason,
    blocked,
    blockReason,
    retryable,
    retryReason,
    trace: verboseDiagnostics ? trace : trace.slice(0, 2),
  };
}

async function generateTextWithModel({
  modelName,
  prompt,
  temperature,
  maxOutputTokens,
}) {
  const response = await ai.generate({
    model: vertexAI.model(modelName),
    prompt,
    config: {
      temperature,
      maxOutputTokens,
    },
  });

  return nonEmptyString(response.text);
}

async function generateAiTextReply({
  prompt,
  mode,
  aiLevel = AI_EXPERIENCE_LEVELS.standard,
  selectedModel = "",
  runtimeSettings,
  usage,
  auth,
}) {
  const promptSettings = await loadAiPromptSettings();
  const assetContext = composeAssetLibraryPromptContext(promptSettings);
  const botRuntime = runtimeSettings.bot;
  const faqRoute = resolveFaqRoute({
    prompt,
    mode,
    botRuntime,
  });
  const textConfig = resolveTextGenerationConfig({
    botRuntime,
    isFaq: faqRoute.useFaq,
    warningLevel: usage.warningLevel || "ok",
    aiLevel,
  });
  const faqLiveFacts = faqRoute.useFaq ?
    await loadFaqLiveFacts({auth, runtimeSettings}) :
    "";
  const requestPrompt = faqRoute.useFaq ?
    composeFaqPrompt({
      inputPrompt: prompt,
      promptSettings,
      botRuntime,
      topic: faqRoute.topic,
      liveFacts: faqLiveFacts,
    }) :
    composeTextGenerationPrompt(
        prompt,
        promptSettings.textInstruction,
        mode,
        assetContext,
        botRuntime,
    );

  if (faqRoute.useFaq && nonEmptyString(auth?.uid)) {
    try {
      await recordFaqOwnerIntelligence({
        uid: auth.uid,
        prompt,
        topicKey: faqRoute.topic?.key || "unknown",
        proactiveHintKeys: resolveProactiveHintKeysFromFacts(faqLiveFacts),
      });
    } catch (error) {
      logger.warn("FAQ owner intelligence record failed.", {
        uid: auth.uid,
        error: error instanceof Error ? error.message : `${error}`,
      });
    }
  }

  const primaryModel = nonEmptyString(selectedModel) || botRuntime.modelPolicy.textPrimaryModel;
  const fallbackModel = botRuntime.modelPolicy.textFallbackModel;
  let reply = "";
  let resolvedSelectedModel = primaryModel;
  let fallbackActivated = false;
  let fallbackReason = "";

  try {
    reply = await generateTextWithModel({
      modelName: primaryModel,
      prompt: requestPrompt,
      temperature: textConfig.temperature,
      maxOutputTokens: textConfig.maxOutputTokens,
    });
  } catch (error) {
    if (!botRuntime.fallbackPolicy.allowTextFallback || fallbackModel === primaryModel) {
      throw error;
    }
    reply = await generateTextWithModel({
      modelName: fallbackModel,
      prompt: requestPrompt,
      temperature: textConfig.temperature,
      maxOutputTokens: textConfig.maxOutputTokens,
    });
    resolvedSelectedModel = fallbackModel;
    fallbackActivated = true;
    fallbackReason = "Primermodell war nicht stabil erreichbar. Text-Fallback wurde genutzt.";
  }

  if (!reply) {
    throw new HttpsError("internal", "Skydown Bot konnte keine Antwort erzeugen.");
  }

  const decisionState = faqRoute.useFaq ?
    "faq_answer" :
    (fallbackActivated || textConfig.responseLimited ? "degraded" : "complete");

  return {
    reply,
    decision: buildBotDecision({
      state: decisionState,
      route: faqRoute.useFaq ? "faq" : "assistant",
      routeReason: faqRoute.routeReason,
      summary: faqRoute.useFaq ?
        "FAQ-/Help-Antwort priorisiert." :
        "Standard-Assistant-Antwort genutzt.",
      topic: faqRoute.topic?.key || "",
      runtimeSettings,
      model: resolvedSelectedModel,
      provider: "google_vertex",
      fallbackActivated,
      fallbackReason: botRuntime.fallbackPolicy.exposeFallbackReason ? fallbackReason : "",
      responseLimited: textConfig.responseLimited,
      responseLimitReason: textConfig.responseLimited ? textConfig.responseLimitReason : "",
      diagnostics: [
        `qualityMode=${botRuntime.qualityMode}`,
        `answerLength=${botRuntime.answerLength}`,
        `promptVersion=${botRuntime.promptVersion}`,
        faqRoute.useFaq ? `faqLiveFacts=${faqLiveFacts ? "enabled" : "unavailable"}` : "",
      ],
      role: usage.role || USER_ROLES.user,
    }),
  };
}

async function generateAiVisualResult(prompt, runtimeSettings, usage, selectedModel = "") {
  const promptSettings = await loadAiPromptSettings();
  const botRuntime = runtimeSettings.bot;
  const assetContext = composeAssetLibraryPromptContext(promptSettings);
  const primaryModel = nonEmptyString(selectedModel) || botRuntime.modelPolicy.visualPrimaryModel;
  const fallbackModel = botRuntime.modelPolicy.visualFallbackModel;
  const primaryResult = await generateAiVisualWithRetries({
    modelName: primaryModel,
    prompt: composeVisualGenerationPrompt(
        prompt,
        promptSettings.visualInstruction,
        assetContext,
        botRuntime,
    ),
    responseModalities: ["TEXT", "IMAGE"],
    defaultText: "Visual generiert.",
  });
  if (primaryResult) {
    return {
      ...primaryResult,
      decision: buildBotDecision({
        state: "complete",
        route: "visual",
        routeReason: "Visual-Pipeline direkt ueber das Primaermodell ausgefuehrt.",
        summary: "Visual ueber den Standard-Bildpfad erzeugt.",
        runtimeSettings,
        model: primaryModel,
        provider: "google_vertex",
        diagnostics: [
          `qualityMode=${botRuntime.qualityMode}`,
          `promptVersion=${botRuntime.promptVersion}`,
        ],
        role: usage.role || USER_ROLES.user,
      }),
    };
  }

  logger.warn("Primary visual model failed. Falling back to secondary visual model.", {
    promptLength: typeof prompt === "string" ? prompt.length : 0,
    primaryModel,
    fallbackModel,
  });

  if (!botRuntime.fallbackPolicy.allowVisualFallback) {
    throw new HttpsError(
        "internal",
        "Das Primaermodell fuer Visuals ist gerade nicht stabil verfuegbar.",
    );
  }

  const fallbackResult = await generateAiVisualWithRetries({
    modelName: fallbackModel,
    prompt: composeVisualImageOnlyPrompt(
        prompt,
        promptSettings.visualInstruction,
        assetContext,
    ),
    defaultText: "Cineastisches Visual generiert.",
  });
  if (fallbackResult) {
    return {
      ...fallbackResult,
      decision: buildBotDecision({
        state: "degraded",
        route: "visual",
        routeReason: "Visual-Fallback aktiviert, weil das Primaermodell ausgefallen ist.",
        summary: "Visual ueber den Fallback-Bildpfad erzeugt.",
        runtimeSettings,
        model: fallbackModel,
        provider: "google_vertex",
        fallbackActivated: true,
        fallbackReason: botRuntime.fallbackPolicy.exposeFallbackReason ?
          "Das Primaermodell fuer Visuals hat nicht stabil geantwortet." :
          "",
        diagnostics: [
          `qualityMode=${botRuntime.qualityMode}`,
          `promptVersion=${botRuntime.promptVersion}`,
        ],
        role: usage.role || USER_ROLES.user,
      }),
    };
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

function readShopifyAdminSecretToken() {
  try {
    return nonEmptyString(shopifyAdminAccessToken.value());
  } catch (error) {
    return "";
  }
}

async function loadShopifyAdminToken() {
  const secretToken = readShopifyAdminSecretToken();
  if (secretToken) {
    return secretToken;
  }

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
  const paypal = data.paypal || {};
  const klarna = data.klarna || {};
  const bankTransfer = data.bankTransfer || {};
  const aiSubscriptions = data.aiSubscriptions || {};
  const paymentMethodsUpdatedAtEpochMillis = resolveDateValueToEpochMillis(
      data.updatedAt ||
      data.lastUpdatedAt ||
      data.updatedAtEpochMillis,
  );

  return {
    stripe: {
      connected: stripe.connected === true,
      enabled: stripe.enabled === true,
      accountHint: nonEmptyString(stripe.accountHint) || "",
    },
    paypal: {
      connected: paypal.connected === true,
      enabled: paypal.enabled === true,
      accountHint: nonEmptyString(paypal.accountHint) || "",
    },
    klarna: {
      connected: klarna.connected === true,
      enabled: klarna.enabled === true,
      accountHint: nonEmptyString(klarna.accountHint) || "",
    },
    bankTransfer: {
      enabled: bankTransfer.enabled === true,
      accountHolder: nonEmptyString(bankTransfer.accountHolder) || "",
      iban: nonEmptyString(bankTransfer.iban) || "",
      bic: nonEmptyString(bankTransfer.bic) || "",
      bankName: nonEmptyString(bankTransfer.bankName) || "",
      paymentInstructions: nonEmptyString(bankTransfer.paymentInstructions) || "",
    },
    aiSubscriptions: {
      enabled: aiSubscriptions.enabled === true,
      creatorPriceId: nonEmptyString(aiSubscriptions.creatorPriceId) || "",
      studioPriceId: nonEmptyString(aiSubscriptions.studioPriceId) || "",
      iosCreatorProductId: nonEmptyString(aiSubscriptions.iosCreatorProductId) || "",
      iosStudioProductId: nonEmptyString(aiSubscriptions.iosStudioProductId) || "",
      iosCreatorYearlyProductId: nonEmptyString(aiSubscriptions.iosCreatorYearlyProductId) || "",
      iosStudioYearlyProductId: nonEmptyString(aiSubscriptions.iosStudioYearlyProductId) || "",
      iosAppAppleId: Number(aiSubscriptions.iosAppAppleId || 0),
      androidCreatorProductId: nonEmptyString(aiSubscriptions.androidCreatorProductId) || "",
      androidStudioProductId: nonEmptyString(aiSubscriptions.androidStudioProductId) || "",
      androidCreatorYearlyProductId: nonEmptyString(aiSubscriptions.androidCreatorYearlyProductId) || "",
      androidStudioYearlyProductId: nonEmptyString(aiSubscriptions.androidStudioYearlyProductId) || "",
      annualDiscountCopy: nonEmptyString(aiSubscriptions.annualDiscountCopy) || "",
      planOrder: Array.isArray(aiSubscriptions.planOrder) ?
        aiSubscriptions.planOrder.map((entry) => nonEmptyString(entry)).filter(Boolean) :
        ["free", "pro", "creator"],
      defaultAnnualToggle: aiSubscriptions.defaultAnnualToggle === true,
      highlightedPlan: nonEmptyString(aiSubscriptions.highlightedPlan) || "creator",
      warningThresholdPercent: clampIntegerSetting(
          aiSubscriptions.warningThresholdPercent,
          70,
          1,
          99,
      ),
      criticalThresholdPercent: clampIntegerSetting(
          aiSubscriptions.criticalThresholdPercent,
          90,
          1,
          100,
      ),
      revenueCatEntitlementCreator: nonEmptyString(aiSubscriptions.revenueCatEntitlementCreator) || "skyos_ai_creator",
      revenueCatEntitlementStudio: nonEmptyString(aiSubscriptions.revenueCatEntitlementStudio) || "skyos_ai_studio",
    },
    updatedAtEpochMillis: paymentMethodsUpdatedAtEpochMillis,
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
  const creatorYearlyProductId = nonEmptyString(config.iosCreatorYearlyProductId);
  const studioYearlyProductId = nonEmptyString(config.iosStudioYearlyProductId);
  if (creatorProductId) {
    mapping[creatorProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioProductId) {
    mapping[studioProductId] = USER_QUOTA_PLANS.studio;
  }
  if (creatorYearlyProductId) {
    mapping[creatorYearlyProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioYearlyProductId) {
    mapping[studioYearlyProductId] = USER_QUOTA_PLANS.studio;
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
  const creatorYearlyProductId = nonEmptyString(config.androidCreatorYearlyProductId);
  const studioYearlyProductId = nonEmptyString(config.androidStudioYearlyProductId);
  if (creatorProductId) {
    mapping[creatorProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioProductId) {
    mapping[studioProductId] = USER_QUOTA_PLANS.studio;
  }
  if (creatorYearlyProductId) {
    mapping[creatorYearlyProductId] = USER_QUOTA_PLANS.creator;
  }
  if (studioYearlyProductId) {
    mapping[studioYearlyProductId] = USER_QUOTA_PLANS.studio;
  }
  return mapping;
}

function hasConfiguredAiPlanProductMapping(productIdToPlan) {
  const configuredPlans = new Set(
      Object.values(productIdToPlan || {})
          .map((value) => normalizeAiSubscriptionPlan(value))
          .filter(Boolean),
  );
  return AI_SUBSCRIPTION_PLANS.every((plan) => configuredPlans.has(plan));
}

function parseIsoEpochSeconds(value) {
  const normalizedValue = nonEmptyString(value);
  if (!normalizedValue) {
    return 0;
  }
  const parsed = Date.parse(normalizedValue);
  return Number.isFinite(parsed) && parsed > 0 ? Math.floor(parsed / 1000) : 0;
}

function resolveGooglePlaySubscriptionState({
  purchase = {},
  requestedProductId = "",
  productIdToPlan = {},
  nowEpochSeconds = Math.floor(Date.now() / 1000),
}) {
  const lineItems = Array.isArray(purchase.lineItems) ? purchase.lineItems : [];
  const normalizedRequestedProductId = nonEmptyString(requestedProductId) || "";
  const resolvedItems = lineItems
      .map((lineItem) => {
        const productId = nonEmptyString(lineItem?.productId) || "";
        const plan = productIdToPlan[productId] || null;
        if (!productId || !plan) {
          return null;
        }
        return {
          productId,
          plan,
          expiryEpochSeconds: parseIsoEpochSeconds(lineItem?.expiryTime),
          latestSuccessfulOrderId: nonEmptyString(lineItem?.latestSuccessfulOrderId) || "",
          autoRenewEnabled: lineItem?.autoRenewingPlan?.autoRenewEnabled === true,
        };
      })
      .filter(Boolean)
      .sort((left, right) => right.expiryEpochSeconds - left.expiryEpochSeconds);

  const matchedItem = resolvedItems.find((item) => item.productId === normalizedRequestedProductId) ||
    resolvedItems[0] ||
    null;
  const subscriptionState = nonEmptyString(purchase.subscriptionState) || "";
  const currentPeriodEndEpochSeconds = matchedItem?.expiryEpochSeconds || 0;
  const hasFutureEntitlement = currentPeriodEndEpochSeconds > nowEpochSeconds;
  const acknowledgementState = nonEmptyString(purchase.acknowledgementState) || "";
  const latestOrderId =
    nonEmptyString(purchase.latestOrderId) ||
    matchedItem?.latestSuccessfulOrderId ||
    "";
  let status = "inactive";
  let cancelAtPeriodEnd = false;
  let shouldGrantEntitlement = false;

  switch (subscriptionState) {
    case "SUBSCRIPTION_STATE_ACTIVE":
      status = "active";
      shouldGrantEntitlement = true;
      break;
    case "SUBSCRIPTION_STATE_IN_GRACE_PERIOD":
      status = "active";
      shouldGrantEntitlement = true;
      break;
    case "SUBSCRIPTION_STATE_PENDING":
      status = "pending";
      break;
    case "SUBSCRIPTION_STATE_ON_HOLD":
      status = "past_due";
      break;
    case "SUBSCRIPTION_STATE_CANCELED":
      cancelAtPeriodEnd = hasFutureEntitlement;
      status = hasFutureEntitlement ? "active" : "canceled";
      shouldGrantEntitlement = hasFutureEntitlement;
      break;
    case "SUBSCRIPTION_STATE_PAUSED":
      status = "paused";
      break;
    case "SUBSCRIPTION_STATE_EXPIRED":
      status = "inactive";
      break;
    default:
      status = hasFutureEntitlement ? "active" : "inactive";
      shouldGrantEntitlement = hasFutureEntitlement;
      break;
  }

  return {
    status,
    plan: matchedItem?.plan || null,
    productId: matchedItem?.productId || normalizedRequestedProductId,
    provider: "play_store",
    sourcePlatform: "android",
    currentPeriodEndEpochSeconds,
    purchaseReference: latestOrderId,
    environment: purchase?.testPurchase ? "test" : "production",
    rawState: subscriptionState,
    linkedPurchaseToken: nonEmptyString(purchase.linkedPurchaseToken) || "",
    acknowledgementState,
    cancelAtPeriodEnd,
    shouldGrantEntitlement,
    needsAcknowledgement:
      acknowledgementState === "ACKNOWLEDGEMENT_STATE_PENDING" &&
      subscriptionState !== "SUBSCRIPTION_STATE_PENDING" &&
      !!matchedItem?.productId,
  };
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
      shippingNotes: nonEmptyString(shipping.shippingNotes) || "",
    },
    invoice: {
      companyName: nonEmptyString(invoice.companyName) || DEFAULT_COMMERCE_SETTINGS.invoice.companyName,
      companyAddress: nonEmptyString(invoice.companyAddress) || "",
      taxNumber: nonEmptyString(invoice.taxNumber) || "",
      vatId: nonEmptyString(invoice.vatId) || "",
      taxRate: Math.max(0, parsePrice(invoice.taxRate || DEFAULT_COMMERCE_SETTINGS.invoice.taxRate)),
      invoicePrefix: nonEmptyString(invoice.invoicePrefix) || DEFAULT_COMMERCE_SETTINGS.invoice.invoicePrefix,
      supportEmail: nonEmptyString(invoice.supportEmail) || DEFAULT_COMMERCE_SETTINGS.invoice.supportEmail,
    },
  };
}

async function loadLegalContentSettings() {
  const snapshot = await admin.firestore()
      .collection("appConfig")
      .doc("legalContent")
      .get();
  const data = snapshot.data() || {};

  return {
    brandName: nonEmptyString(data.brandName) || DEFAULT_LEGAL_CONTENT_SETTINGS.brandName,
    operatorName: nonEmptyString(data.operatorName) || DEFAULT_LEGAL_CONTENT_SETTINGS.operatorName,
    rightsHolderName: nonEmptyString(data.rightsHolderName) || DEFAULT_LEGAL_CONTENT_SETTINGS.rightsHolderName,
    supportEmail: nonEmptyString(data.supportEmail) || DEFAULT_LEGAL_CONTENT_SETTINGS.supportEmail,
    lastUpdatedLabel: nonEmptyString(data.lastUpdatedLabel) || DEFAULT_LEGAL_CONTENT_SETTINGS.lastUpdatedLabel,
    imprintReference: nonEmptyString(data.imprintReference) || DEFAULT_LEGAL_CONTENT_SETTINGS.imprintReference,
  };
}

async function loadRecentOrdersForFaq(uid, email = "") {
  const firestore = admin.firestore();
  const normalizedUid = nonEmptyString(uid);
  const normalizedEmail = normalizeEmail(email);
  if (!normalizedUid && !normalizedEmail) {
    return [];
  }

  const queries = [];
  if (normalizedUid) {
    queries.push(
        firestore.collection("orders")
            .where("orderOwnerUid", "==", normalizedUid)
            .get(),
    );
  }
  if (normalizedEmail) {
    queries.push(
        firestore.collection("orders")
            .where("userEmail", "==", normalizedEmail)
            .get(),
    );
  }

  const snapshots = await Promise.all(queries);
  const documents = Array.from(
      new Map(
          snapshots
              .flatMap((snapshot) => snapshot.docs)
              .map((document) => [document.id, document]),
      ).values(),
  );

  return documents
      .map((document) => ({
        id: document.id,
        ...document.data(),
      }))
      .sort((left, right) => (
        resolveDateValueToEpochMillis(right.timestamp) - resolveDateValueToEpochMillis(left.timestamp)
      ))
      .slice(0, 3);
}

function buildFaqOrderFact(orderData = {}) {
  const orderId = nonEmptyString(orderData.id) || "ohne-id";
  const totalAmount = formatEuroAmount(orderData.totalAmount);
  const paymentStatus = nonEmptyString(orderData.paymentStatus) || "unbekannt";
  const fulfillmentStatus = nonEmptyString(orderData.fulfillmentStatus) || "unbekannt";
  const checkoutStatus = nonEmptyString(orderData.stripeCheckoutStatus);
  const orderDate = formatEpochMillisAsIsoDate(orderData.timestamp);
  const orderName = nonEmptyString(orderData.shopifyOrderName);
  const carrier = nonEmptyString(orderData.carrier) ||
    nonEmptyString(orderData.shippingCarrier) ||
    nonEmptyString(orderData.fulfillmentCarrier);
  const trackingUrl = nonEmptyString(orderData.trackingUrl) ||
    nonEmptyString(orderData.shippingTrackingUrl) ||
    nonEmptyString(orderData.fulfillmentTrackingUrl);
  const latestTrackingEvent = nonEmptyString(orderData.latestTrackingEvent) ||
    nonEmptyString(orderData.shippingLatestTrackingEvent) ||
    nonEmptyString(orderData.fulfillmentLatestTrackingEvent);
  const etaWindow = nonEmptyString(orderData.etaWindow) ||
    nonEmptyString(orderData.shippingEtaWindow) ||
    nonEmptyString(orderData.fulfillmentEtaWindow);
  const lastStatusUpdateAt = formatEpochMillisAsIsoDate(
      orderData.lastStatusUpdateAt ||
      orderData.lastTrackingUpdateAt ||
      orderData.fulfillmentUpdatedAt ||
      orderData.updatedAt,
  );

  return [
    `Order ${orderId}`,
    orderName ? `Shopify ${orderName}` : "",
    orderDate ? `vom ${orderDate}` : "",
    totalAmount ? `Gesamt ${totalAmount}` : "",
    `Zahlung ${paymentStatus}`,
    `Fulfillment ${fulfillmentStatus}`,
    checkoutStatus ? `Checkout ${checkoutStatus}` : "",
    carrier ? `Carrier ${carrier}` : "",
    trackingUrl ? `Tracking ${truncateTextBlock(trackingUrl, 160)}` : "",
    latestTrackingEvent ? `Letztes Tracking-Event ${truncateTextBlock(latestTrackingEvent, 120)}` : "",
    etaWindow ? `ETA ${truncateTextBlock(etaWindow, 90)}` : "",
    lastStatusUpdateAt ? `Letztes Status-Update ${lastStatusUpdateAt}` : "",
  ].filter(Boolean).join(" · ");
}

async function loadCurrentAiUsageForFaq(uid) {
  const normalizedUid = nonEmptyString(uid);
  if (!normalizedUid) {
    return {};
  }
  const snapshot = await admin.firestore()
      .collection("users")
      .doc(normalizedUid)
      .collection("aiUsage")
      .doc(aiUsageDateKey())
      .get();
  return snapshot.exists ? (snapshot.data() || {}) : {};
}

function normalizeBillingIntervalFromProductId(productId = "") {
  const normalized = nonEmptyString(productId)?.toLowerCase() || "";
  if (!normalized) return "unbekannt";
  if (/(year|annual|jahr)/.test(normalized)) {
    return "yearly";
  }
  return "monthly";
}

function resolvePlanFitHint(currentPlan, targetPlan) {
  const normalizedCurrent = normalizeAiSubscriptionPlan(currentPlan) || USER_QUOTA_PLANS.free;
  const normalizedTarget = normalizeAiSubscriptionPlan(targetPlan);
  if (!normalizedTarget) {
    return "kein plan mapping";
  }
  if (normalizedCurrent === normalizedTarget) {
    return "passt zu deinem aktuellen Plan";
  }
  if (normalizedCurrent === USER_QUOTA_PLANS.free && normalizedTarget === USER_QUOTA_PLANS.creator) {
    return "naechster Schritt fuer regelmaessige Nutzung";
  }
  if (normalizedCurrent === USER_QUOTA_PLANS.free && normalizedTarget === USER_QUOTA_PLANS.studio) {
    return "power-upgrade fuer hohe AI-Last";
  }
  if (normalizedCurrent === USER_QUOTA_PLANS.creator && normalizedTarget === USER_QUOTA_PLANS.studio) {
    return "upgrade fuer Workflow-Tiefe und Premium Outputs";
  }
  if (normalizedCurrent === USER_QUOTA_PLANS.studio && normalizedTarget === USER_QUOTA_PLANS.creator) {
    return "downshift moeglich, falls weniger Bedarf";
  }
  return "plan fit individuell pruefen";
}

function resolveConfiguredMembershipPriceSnapshot(aiSubscriptions = {}) {
  const mapping = {
    iosCreatorProductId: USER_QUOTA_PLANS.creator,
    iosStudioProductId: USER_QUOTA_PLANS.studio,
    iosCreatorYearlyProductId: USER_QUOTA_PLANS.creator,
    iosStudioYearlyProductId: USER_QUOTA_PLANS.studio,
    androidCreatorProductId: USER_QUOTA_PLANS.creator,
    androidStudioProductId: USER_QUOTA_PLANS.studio,
    androidCreatorYearlyProductId: USER_QUOTA_PLANS.creator,
    androidStudioYearlyProductId: USER_QUOTA_PLANS.studio,
  };
  const entries = [];
  const snapshotByProductId = aiSubscriptions.priceSnapshotByProductId &&
    typeof aiSubscriptions.priceSnapshotByProductId === "object" &&
    !Array.isArray(aiSubscriptions.priceSnapshotByProductId) ?
      aiSubscriptions.priceSnapshotByProductId :
      {};

  for (const [configKey, plan] of Object.entries(mapping)) {
    const productId = nonEmptyString(aiSubscriptions[configKey]);
    if (!productId) continue;
    const rawSnapshot = snapshotByProductId[productId];
    const snapshot = rawSnapshot && typeof rawSnapshot === "object" && !Array.isArray(rawSnapshot) ? rawSnapshot : {};
    const rawAmount = snapshot.price ?? snapshot.amount ?? snapshot.localizedAmount ?? "";
    const amountLabel = typeof rawAmount === "number" ?
      formatEuroAmount(rawAmount) :
      nonEmptyString(rawAmount) || "nicht hinterlegt";
    const currency = nonEmptyString(snapshot.currency) || nonEmptyString(snapshot.currencyCode) || "unbekannt";
    const billingInterval = nonEmptyString(snapshot.billingInterval)?.toLowerCase() || normalizeBillingIntervalFromProductId(productId);
    const sourcePlatform = configKey.startsWith("ios") ? "iOS" : "Android";
    const snapshotUpdatedAt = formatEpochMillisAsIsoDate(snapshot.updatedAtEpochMillis || snapshot.updatedAt);

    entries.push({
      sourcePlatform,
      productId,
      plan,
      amountLabel,
      currency,
      billingInterval,
      snapshotUpdatedAt,
    });
  }

  return entries;
}

function buildProactiveFaqHints({
  profile,
  currentUsage,
  aiSubscriptions,
  entitlementState,
  recentOrders,
  paymentSettings,
  activeCheckoutMethods,
  actionLayer,
}) {
  if (actionLayer?.proactiveHintsEnabled === false) {
    return [];
  }
  const hints = [];
  const warningThreshold = Number(actionLayer?.warningThresholdPercent || aiSubscriptions?.warningThresholdPercent || 70);
  const textRatio = profile.aiLimits.text > 0 ? (Number(currentUsage?.textRequests || 0) / profile.aiLimits.text) * 100 : 0;
  const visualRatio = profile.aiLimits.visual > 0 ? (Number(currentUsage?.visualRequests || 0) / profile.aiLimits.visual) * 100 : 0;
  const agentRatio = profile.aiLimits.agent > 0 ? (Number(currentUsage?.agentRequests || 0) / profile.aiLimits.agent) * 100 : 0;
  const nearLimitKinds = [
    textRatio >= warningThreshold ? `Text ${Math.floor(textRatio)}%` : "",
    visualRatio >= warningThreshold ? `Visual ${Math.floor(visualRatio)}%` : "",
    agentRatio >= warningThreshold ? `Agent ${Math.floor(agentRatio)}%` : "",
  ].filter(Boolean);
  if (actionLayer?.triggerAiLimitNearEnabled !== false && nearLimitKinds.length) {
    hints.push(`[AI Limit fast erreicht] Heute nahe am Limit: ${nearLimitKinds.join(" · ")}. Empfehlung: kurze, fokussierte Prompts und bei regelmaessiger hoher Nutzung passendes Upgrade pruefen.`);
  }

  const hasStoreRestorePath = Boolean(
      aiSubscriptions?.enabled === true &&
      ((aiSubscriptions?.iosCreatorProductId && aiSubscriptions?.iosStudioProductId) ||
      (aiSubscriptions?.androidCreatorProductId && aiSubscriptions?.androidStudioProductId)),
  );
  const entitlementStatus = nonEmptyString(entitlementState?.status) || "inactive";
  if (actionLayer?.triggerRestoreAvailableEnabled !== false && hasStoreRestorePath && entitlementStatus !== "active") {
    hints.push("[Restore verfuegbar] Dein Entitlement ist aktuell nicht aktiv. Restore kann jetzt sinnvoll sein (Store-Konto pruefen, einmal ausloesen, kurz auf Sync warten).");
  }

  const shippedOrder = (recentOrders || []).find((order) => {
    const fulfillmentStatus = nonEmptyString(order?.fulfillmentStatus)?.toLowerCase() || "";
    return ["shipped", "in_transit", "fulfilled", "delivered", "versendet"].includes(fulfillmentStatus);
  });
  if (actionLayer?.triggerOrderShippedEnabled !== false && shippedOrder) {
    const trackingUrl = nonEmptyString(shippedOrder.trackingUrl) ||
      nonEmptyString(shippedOrder.shippingTrackingUrl) ||
      nonEmptyString(shippedOrder.fulfillmentTrackingUrl);
    hints.push(`[Bestellung versendet] Order ${nonEmptyString(shippedOrder.id) || "ohne-id"} scheint versendet. ${trackingUrl ? `Tracking-Link verfuegbar: ${truncateTextBlock(trackingUrl, 140)}.` : "Tracking-Link im Order-Datensatz nicht sicher vorhanden."}`);
  }

  const paymentMethodsUpdatedAt = formatEpochMillisAsIsoDate(paymentSettings?.updatedAtEpochMillis);
  const paymentMethodsUpdatedAtMs = Number(paymentSettings?.updatedAtEpochMillis || 0);
  const changedRecently = Number.isFinite(paymentMethodsUpdatedAtMs) &&
    paymentMethodsUpdatedAtMs > 0 &&
    (Date.now() - paymentMethodsUpdatedAtMs) <= (14 * 24 * 60 * 60 * 1000);
  if (actionLayer?.triggerPaymentMethodsChangedEnabled !== false && changedRecently && activeCheckoutMethods.length) {
    hints.push(`[Payment Methode geaendert] Checkout-Konfiguration wurde kuerzlich aktualisiert (${paymentMethodsUpdatedAt || "kuerzlich"}). Aktive Methoden: ${activeCheckoutMethods.join(", ")}.`);
  }

  const normalizedPlan = normalizeAiSubscriptionPlan(entitlementState?.plan || profile?.aiLimits?.quotaPlan);
  const heavyUsage = [textRatio, visualRatio, agentRatio].some((value) => value >= Math.max(80, warningThreshold));
  if (actionLayer?.triggerUsageBasedUpgradeEnabled !== false &&
    (normalizedPlan === USER_QUOTA_PLANS.free || normalizedPlan === USER_QUOTA_PLANS.creator) &&
    heavyUsage) {
    const hintText = normalizedPlan === USER_QUOTA_PLANS.free ?
      nonEmptyString(actionLayer?.upgradeHintFreeToProText) ||
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintFreeToProText :
      nonEmptyString(actionLayer?.upgradeHintProToCreatorText) ||
        DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintProToCreatorText;
    hints.push(`[Upgrade sinnvoll basierend auf Nutzung] ${hintText}`);
  }

  return hints;
}

function resolveCurrentAiAccessForFaq({featureConfig, profile}) {
  if (!featureConfig?.isEnabled) {
    return false;
  }

  if (featureConfig.accessMode === AI_ACCESS_MODES.off) {
    return false;
  }

  if (!profile?.aiAccessEnabled) {
    return false;
  }

  if (featureConfig.accessMode === AI_ACCESS_MODES.adminOnly) {
    return profile?.isStaff === true;
  }

  return true;
}

async function loadFaqLiveFacts({auth, runtimeSettings = null}) {
  const uid = nonEmptyString(auth?.uid);
  if (!uid) {
    return "";
  }

  const [
    paymentSettings,
    commerceSettings,
    legalContent,
    userData,
    featureConfig,
    canonicalEntitlement,
    currentUsage,
  ] = await Promise.all([
    loadFaqSource("payment_methods", () => loadPaymentMethodSettings(), null),
    loadFaqSource("commerce_settings", () => loadCommerceSettings(), null),
    loadFaqSource("legal_content", () => loadLegalContentSettings(), null),
    loadFaqSource("user_data", () => loadUserData(uid), null),
    loadFaqSource("ai_feature_config", () => loadAiFeatureConfig(), {
      isEnabled: true,
      accessMode: AI_ACCESS_MODES.signedIn,
    }),
    loadFaqSource("canonical_entitlement", () => loadCanonicalAiEntitlement(uid), null),
    loadFaqSource("current_ai_usage", () => loadCurrentAiUsageForFaq(uid), {}),
  ]);

  const rawUserData = userData || {};
  const entitlementState = canonicalEntitlement || resolveLegacyAiEntitlement(rawUserData);
  const effectiveUserData = applyEntitlementToUserData(rawUserData, entitlementState);
  const profile = buildUserProfile(effectiveUserData);
  const email = normalizeEmail(effectiveUserData.email) || normalizeEmail(auth?.token?.email) || "";
  const recentOrders = await loadFaqSource(
      "recent_orders",
      () => loadRecentOrdersForFaq(uid, email),
      [],
  );
  const aiCapabilities = entitlementState?.capabilities || resolveAiCapabilities({
    plan: entitlementState?.plan || null,
    status: entitlementState?.status || "inactive",
    role: profile.role,
  });
  const aiSubscriptions = paymentSettings?.aiSubscriptions || {};
  const bankTransferConfigured = paymentSettings?.bankTransfer?.enabled === true &&
    !!paymentSettings?.bankTransfer?.accountHolder &&
    !!paymentSettings?.bankTransfer?.iban &&
    !!paymentSettings?.bankTransfer?.bankName;
  const activeCheckoutMethods = [
    paymentSettings?.stripe?.connected === true && paymentSettings?.stripe?.enabled === true ? "Stripe" : "",
    paymentSettings?.paypal?.connected === true && paymentSettings?.paypal?.enabled === true ? "PayPal" : "",
    paymentSettings?.klarna?.connected === true && paymentSettings?.klarna?.enabled === true ? "Klarna" : "",
    bankTransferConfigured ? "Bankueberweisung" : "",
  ].filter(Boolean);
  const supportEmail = nonEmptyString(legalContent?.supportEmail) ||
    nonEmptyString(commerceSettings?.invoice?.supportEmail) ||
    DEFAULT_LEGAL_CONTENT_SETTINGS.supportEmail;
  const currentPlan = resolveFaqPlanLabel(
      entitlementState?.plan ||
      profile.aiLimits?.quotaPlan ||
      effectiveUserData.quotaPlan,
  );
  const currentStatus = nonEmptyString(entitlementState?.status) || "inactive";
  const currentProvider = nonEmptyString(entitlementState?.provider) || "kein_provider";
  const currentSource = nonEmptyString(entitlementState?.source) || "kein_source";
  const currentPeriodEnd = formatEpochMillisAsIsoDate(
      Number(entitlementState?.periodEndEpochSeconds || 0) * 1000,
  );
  const registrationDate = formatEpochMillisAsIsoDate(effectiveUserData.registrationDateEpochMillis);
  const contactMask = maskEmail(email);
  const membershipPriceSnapshot = resolveConfiguredMembershipPriceSnapshot(aiSubscriptions);
  const membershipPriceSnapshotLines = membershipPriceSnapshot.length ?
    membershipPriceSnapshot
        .map((entry) => {
          const intervalLabel = entry.billingInterval === "yearly" ? "yearly" : "monthly";
          const planLabel = resolveFaqPlanLabel(entry.plan);
          const fitHint = resolvePlanFitHint(entitlementState?.plan || currentPlan, entry.plan);
          return `${entry.sourcePlatform} ${planLabel} (${intervalLabel}) · Preis ${entry.amountLabel} · Waehrung ${entry.currency} · Product ${entry.productId} · Plan-Fit ${fitHint}${entry.snapshotUpdatedAt ? ` · Snapshot ${entry.snapshotUpdatedAt}` : ""}`;
        }) :
    [
      "Keine Membership Price Snapshots pro Plattform im Payment-Config hinterlegt.",
      "Felder erwartet: paymentMethods.aiSubscriptions.priceSnapshotByProductId[productId] mit price, currency und optional billingInterval, updatedAt.",
    ];
  const iosBillingConfigured = Boolean(
      aiSubscriptions.iosCreatorProductId &&
      aiSubscriptions.iosStudioProductId &&
      aiSubscriptions.iosAppAppleId,
  );
  const androidBillingConfigured = Boolean(
      aiSubscriptions.androidCreatorProductId &&
      aiSubscriptions.androidStudioProductId,
  );
  const normalizedSource = nonEmptyString(entitlementState?.source)?.toLowerCase() || "";
  const restoreSourceHint = normalizedSource.includes("app") || normalizedSource.includes("ios") ?
    "iOS/App Store" :
    normalizedSource.includes("play") || normalizedSource.includes("android") ?
      "Android/Google Play" :
      "Plattform unklar";
  const textUsage = Number(currentUsage?.textRequests || 0);
  const visualUsage = Number(currentUsage?.visualRequests || 0);
  const agentUsage = Number(currentUsage?.agentRequests || 0);
  const reachedKinds = [
    profile.aiLimits.text > 0 && textUsage >= profile.aiLimits.text ? "Text" : "",
    profile.aiLimits.visual > 0 && visualUsage >= profile.aiLimits.visual ? "Visual" : "",
    profile.aiLimits.agent > 0 && agentUsage >= profile.aiLimits.agent ? "Agent" : "",
  ].filter(Boolean);
  const aiBlockedReasons = [
    featureConfig?.isEnabled === false ? "Remote Config: AI global deaktiviert" : "",
    featureConfig?.accessMode === AI_ACCESS_MODES.off ? "Access Mode steht auf off" : "",
    profile.aiAccessEnabled !== true ? "Konto hat AI Access nicht aktiv" : "",
    featureConfig?.accessMode === AI_ACCESS_MODES.adminOnly && !profile.isStaff ?
      "Access Mode admin_only, Konto ist kein Staff" :
      "",
  ].filter(Boolean);
  const recoveryFacts = [
    [
      "[Restore failed]",
      `Moegliche Ursachen: Entitlement aktuell ${currentStatus}; Billing-Setup iOS ${formatYesNo(iosBillingConfigured)} / Android ${formatYesNo(androidBillingConfigured)}; erkannte Source ${restoreSourceHint}.`,
      "Naechste Schritte: Restore genau einmal im Membership-Sheet ausloesen, 30-90s Sync abwarten, mit demselben Store-Konto pruefen, danach Support mit Konto-E-Mail + Plattform + Zeitstempel + Purchase-Referenz kontaktieren.",
    ].join(" "),
    [
      "[Purchase failed]",
      `Moegliche Ursachen: AI Membership global ${formatYesNo(aiSubscriptions.enabled === true)}; aktive Checkout-Methoden ${activeCheckoutMethods.length ? activeCheckoutMethods.join(", ") : "keine bestaetigt"}; mobile Flows erwarten nativen Store-Billing-Flow.`,
      "Naechste Schritte: Kauf nur einmal starten (keine Doppel-Taps), Store-Account + Netzwerk pruefen, bei erneutem Fehlschlag Support mit Plattform, Plan, Uhrzeit und Fehlertext.",
    ].join(" "),
    [
      "[Login failed]",
      `Moegliche Ursachen: Konto-E-Mail ${contactMask || "nicht im FAQ-Kontext sichtbar"}; Rolle ${profile.role}; AI-Zugriff ${formatYesNo(profile.aiAccessEnabled)}.`,
      "Naechste Schritte: Session erneuern (ab-/anmelden), Login-Methode pruefen, danach Support mit Plattform, Login-Zeit und Screenshot.",
    ].join(" "),
    [
      "[AI blocked]",
      `Moegliche Ursachen: ${aiBlockedReasons.length ? aiBlockedReasons.join(" | ") : "kein harter Block aus Live Facts sichtbar; moeglich sind modell-/guardrail-spezifische Laufzeitregeln"}.`,
      "Naechste Schritte: Bot im FAQ-/Text-Modus neu versuchen, bei persistentem Block Plan-/Rollenfreigabe und Runtime-Status pruefen, dann Support mit Anfragezeit + Bereich + Konto.",
    ].join(" "),
    [
      "[Limit reached]",
      `Moegliche Ursachen: heutige Nutzung Text ${textUsage}/${profile.aiLimits.text}, Visual ${visualUsage}/${profile.aiLimits.visual}, Agent ${agentUsage}/${profile.aiLimits.agent}; erreicht: ${reachedKinds.length ? reachedKinds.join(", ") : "laut Snapshot keines sicher erreicht"}.`,
      "Naechste Schritte: auf naechstes UTC-Reset warten, niedrigere Last (kurzere Prompts) nutzen, oder passendes Upgrade pruefen.",
    ].join(" "),
  ];
  const proactiveHints = buildProactiveFaqHints({
    profile,
    currentUsage,
    aiSubscriptions,
    entitlementState,
    recentOrders,
    paymentSettings,
    activeCheckoutMethods,
    actionLayer: runtimeSettings?.bot?.actionLayer || null,
  });

  return [
    composeFaqFactSection("Membership Facts", [
      `AI Membership global aktiviert: ${formatYesNo(aiSubscriptions.enabled === true)}`,
      `Aktueller Entitlement-Stand: Plan ${currentPlan} · Status ${currentStatus} · Provider ${currentProvider} · Source ${currentSource}`,
      currentPeriodEnd ? `Aktuelle Laufzeit endet laut Entitlement am ${currentPeriodEnd}` : "",
      `Quota-Plan im Konto: ${resolveFaqPlanLabel(profile.aiLimits.quotaPlan)}`,
      `Tageslimits: Text ${profile.aiLimits.text} · Visual ${profile.aiLimits.visual} · Agent ${profile.aiLimits.agent} · History ${profile.aiLimits.historyRetentionDays} Tage`,
      `Native Billing Konfiguration: iOS ${formatYesNo(Boolean(aiSubscriptions.iosCreatorProductId && aiSubscriptions.iosStudioProductId && aiSubscriptions.iosAppAppleId))} · Android ${formatYesNo(Boolean(aiSubscriptions.androidCreatorProductId && aiSubscriptions.androidStudioProductId))}`,
      `Yearly Offers konfiguriert: iOS ${formatYesNo(Boolean(aiSubscriptions.iosCreatorYearlyProductId && aiSubscriptions.iosStudioYearlyProductId))} · Android ${formatYesNo(Boolean(aiSubscriptions.androidCreatorYearlyProductId && aiSubscriptions.androidStudioYearlyProductId))}`,
      aiSubscriptions.annualDiscountCopy ? `Annual Offer Copy: ${truncateTextBlock(aiSubscriptions.annualDiscountCopy, 160)}` : "",
      Array.isArray(aiSubscriptions.planOrder) && aiSubscriptions.planOrder.length ?
        `Plan-Reihenfolge in Membership-UI: ${aiSubscriptions.planOrder.join(" -> ")}` :
        "",
      aiSubscriptions.highlightedPlan ? `Hervorgehobener Membership-Plan: ${aiSubscriptions.highlightedPlan}` : "",
      `Usage-Warnschwellen in Membership-UI: warning ${aiSubscriptions.warningThresholdPercent || 70}% · critical ${aiSubscriptions.criticalThresholdPercent || 90}%`,
      "Wichtige Mapping-Notiz: interner Plan 'creator' wird in Teilen der App als 'Pro' angezeigt; interner Plan 'studio' wird in Teilen der App als 'Creator' angezeigt.",
      "Mobile AI-Abos sind fuer iOS/Android auf nativen Store-Billing-Flow ausgelegt; serverseitiger Stripe-Abo-Checkout blockt mobile Plattformangaben bewusst.",
    ]),
    composeFaqFactSection("Membership Price Snapshot Facts", membershipPriceSnapshotLines),
    composeFaqFactSection("Account Facts", [
      `Kontorolle ${profile.role} · Staff ${formatYesNo(profile.isStaff)} · AI-Zugriff auf Kontoebene ${formatYesNo(profile.aiAccessEnabled)}`,
      effectiveUserData.username ? `Username im Konto: ${truncateTextBlock(effectiveUserData.username, 80)}` : "",
      contactMask ? `Hinterlegte Konto-E-Mail (maskiert): ${contactMask}` : "",
      registrationDate ? `Registrierung im Nutzerdokument seit: ${registrationDate}` : "",
      effectiveUserData.whatsApp ? "WhatsApp im Konto vorhanden: ja" : "WhatsApp im Konto vorhanden: nein",
      effectiveUserData.instagramHandle ? "Instagram im Konto vorhanden: ja" : "Instagram im Konto vorhanden: nein",
      effectiveUserData.profileTagline ? "Profil-Tagline vorhanden: ja" : "Profil-Tagline vorhanden: nein",
      effectiveUserData.profileBio ? "Profil-Bio vorhanden: ja" : "Profil-Bio vorhanden: nein",
    ]),
    composeFaqFactSection("App Feature Facts", [
      `Remote Config fuer AI: enabled ${formatYesNo(featureConfig?.isEnabled !== false)} · accessMode ${resolveFaqAccessModeLabel(featureConfig?.accessMode)}`,
      `AI fuer dieses Konto aktuell nutzbar: ${formatYesNo(resolveCurrentAiAccessForFaq({featureConfig, profile}))}`,
      `Capabilities aus Entitlement: botText ${formatYesNo(featureAllowedByCapability(aiCapabilities, AI_FEATURE_CLASSES.text))} · image ${formatYesNo(featureAllowedByCapability(aiCapabilities, AI_FEATURE_CLASSES.image))} · agent ${formatYesNo(featureAllowedByCapability(aiCapabilities, AI_FEATURE_CLASSES.agent))} · workflow ${formatYesNo(featureAllowedByCapability(aiCapabilities, AI_FEATURE_CLASSES.workflow))} · premiumOutputs ${formatYesNo(aiCapabilities.premiumOutputs === true)}`,
      "Es gibt aktuell keine zentrale Live-Feature-Registry fuer Music, Video, Shop oder Profile jenseits der bekannten Produktfakten und AI-Remote-Config.",
    ]),
    composeFaqFactSection("Support Facts", [
      `Support-E-Mail: ${supportEmail}`,
      legalContent?.operatorName ? `Betreiber: ${legalContent.operatorName}` : "",
      legalContent?.brandName ? `Brand: ${legalContent.brandName}` : "",
      legalContent?.rightsHolderName ? `Rights Holder: ${legalContent.rightsHolderName}` : "",
      legalContent?.imprintReference ? `Imprint/Anbieterhinweis: ${truncateTextBlock(legalContent.imprintReference, 220)}` : "",
      legalContent?.lastUpdatedLabel ? `Legal Stand laut App: ${legalContent.lastUpdatedLabel}` : "",
      activeCheckoutMethods.length ? `Aktive Checkout-/Zahlungswege laut Config: ${activeCheckoutMethods.join(", ")}` : "Aktive Checkout-/Zahlungswege laut Config: keine sicher bestaetigt",
      commerceSettings?.shipping ? `Versandkosten laut Config: DE ${formatEuroAmount(commerceSettings.shipping.domesticCost)} · EU ${formatEuroAmount(commerceSettings.shipping.euCost)} · INTL ${formatEuroAmount(commerceSettings.shipping.internationalCost)} · kostenlos ab ${formatEuroAmount(commerceSettings.shipping.freeShippingThreshold)}` : "",
      commerceSettings?.shipping?.shippingNotes ? `Shipping Notes: ${truncateTextBlock(commerceSettings.shipping.shippingNotes, 220)}` : "",
      commerceSettings?.invoice?.companyName ? `Rechnungssteller laut Commerce-Config: ${commerceSettings.invoice.companyName}` : "",
      commerceSettings?.invoice?.supportEmail ? `Commerce Support-E-Mail: ${commerceSettings.invoice.supportEmail}` : "",
      "Wenn Support noetig ist, sind besonders hilfreich: Plattform, betroffener Bereich, Uhrzeit, Screenshots, Referenz-ID und die Konto-E-Mail.",
    ]),
    composeFaqFactSection("Order Facts", recentOrders.length ?
      recentOrders.map((order) => buildFaqOrderFact(order)) :
      ["Keine Bestellungen fuer dieses Konto im aktuellen FAQ-Kontext gefunden."]),
    composeFaqFactSection("Recovery Facts", recoveryFacts),
    composeFaqFactSection("Proaktive Hinweise", proactiveHints.length ?
      proactiveHints :
      ["Keine proaktiven Hinweise mit hohem Nutzen im aktuellen Snapshot."]),
    composeFaqFactSection("Known Data Limits", [
      "Keine live Store-Preisbetraege aus App Store oder Google Play im FAQ-Kontext.",
      "Keine Carrier-Tracking-API und keine live Versandlaufzeiten ausser vorhandenen Order-/Fulfillment-Feldern.",
      "Keine Support-Ticket-Datenbank und keine provider-spezifischen Auth-/Login-Fehlerlogs im FAQ-Kontext.",
      "Keine zentrale Live-Feature-Registry fuer nicht-AI-Bereiche wie Music, Video, Shop oder Profile.",
    ]),
  ].filter(Boolean).join("\n\n");
}

function normalizeFaqQuestionKey(prompt) {
  const base = nonEmptyString(prompt) || "";
  const normalized = base
      .toLowerCase()
      .replace(/[^a-z0-9\u00c0-\u017f\s]/g, " ")
      .replace(/\s+/g, " ")
      .trim() || "";
  if (!normalized) {
    return "empty";
  }
  return normalized.slice(0, 120);
}

function extractFaqSectionLines(factsText, sectionTitle) {
  const text = typeof factsText === "string" ? factsText : "";
  if (!text || !sectionTitle) {
    return [];
  }
  const escapedTitle = sectionTitle.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  const regex = new RegExp(`\\[${escapedTitle}\\]\\n([\\s\\S]*?)(?:\\n\\n\\[|$)`, "i");
  const match = text.match(regex);
  if (!match?.[1]) {
    return [];
  }
  return match[1]
      .split("\n")
      .map((line) => line.trim())
      .filter((line) => line.startsWith("- "))
      .map((line) => line.slice(2).trim())
      .filter(Boolean);
}

function resolveProactiveHintKeysFromFacts(factsText) {
  const lines = extractFaqSectionLines(factsText, "Proaktive Hinweise");
  return uniqueStrings(lines.map((line) => {
    const bracketMatch = line.match(/^\[([^\]]+)\]/);
    const label = bracketMatch?.[1] || line.slice(0, 48);
    return aiMetricKey(label, "hint");
  }));
}

function toEpochMillis(value) {
  if (value instanceof admin.firestore.Timestamp) {
    return value.toMillis();
  }
  const numeric = Number(value);
  if (Number.isFinite(numeric) && numeric > 0) {
    return numeric;
  }
  const parsed = Date.parse(nonEmptyString(value) || "");
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

async function recordFaqOwnerIntelligence({
  uid,
  prompt,
  topicKey,
  proactiveHintKeys,
}) {
  const normalizedUid = nonEmptyString(uid);
  if (!normalizedUid) {
    return;
  }
  const dateKey = aiUsageDateKey();
  const dayRef = admin.firestore().collection(AI_FAQ_INTELLIGENCE_DAILY_COLLECTION).doc(dateKey);
  const userRef = dayRef.collection("userState").doc(normalizedUid);
  const nowMs = Date.now();
  const questionKey = normalizeFaqQuestionKey(prompt);
  const normalizedTopic = aiMetricKey(topicKey, "unknown");
  const hintKeys = uniqueStrings((proactiveHintKeys || []).map((key) => aiMetricKey(key, "hint")));

  await admin.firestore().runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    const previous = userSnapshot.exists ? (userSnapshot.data() || {}) : {};
    const lastQuestionKey = nonEmptyString(previous.lastQuestionKey) || "";
    const lastAskedAt = toEpochMillis(previous.lastAskedAtEpochMillis);
    const lastHintAt = toEpochMillis(previous.lastHintAtEpochMillis);
    const previousHintKeys = Array.isArray(previous.lastHintKeys) ?
      previous.lastHintKeys.map((value) => aiMetricKey(value, "hint")).filter(Boolean) :
      [];
    const isRepeatQuestion = lastQuestionKey === questionKey &&
      lastAskedAt > 0 &&
      (nowMs - lastAskedAt) <= (24 * 60 * 60 * 1000);
    const repeatAfterHint = isRepeatQuestion &&
      lastHintAt > 0 &&
      (nowMs - lastHintAt) <= (24 * 60 * 60 * 1000) &&
      previousHintKeys.length > 0;

    const updates = {
      dateKey,
      totalFaqAnswers: admin.firestore.FieldValue.increment(1),
      [`faqByTopic.${normalizedTopic}`]: admin.firestore.FieldValue.increment(1),
      [`faqByQuestion.${questionKey}`]: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    if (hintKeys.length > 0) {
      updates.totalHintWindows = admin.firestore.FieldValue.increment(1);
      for (const hintKey of hintKeys) {
        updates[`hintTriggers.${hintKey}`] = admin.firestore.FieldValue.increment(1);
      }
    }
    if (isRepeatQuestion) {
      updates.repeatQuestions = admin.firestore.FieldValue.increment(1);
    }
    if (repeatAfterHint) {
      updates.repeatAfterHint = admin.firestore.FieldValue.increment(1);
      for (const hintKey of previousHintKeys) {
        updates[`repeatAfterHintByTrigger.${hintKey}`] = admin.firestore.FieldValue.increment(1);
      }
    }
    transaction.set(dayRef, updates, {merge: true});

    transaction.set(userRef, {
      uid: normalizedUid,
      lastQuestionKey: questionKey,
      lastAskedAtEpochMillis: nowMs,
      lastHintKeys: hintKeys,
      lastHintAtEpochMillis: hintKeys.length ? nowMs : previous.lastHintAtEpochMillis || 0,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
  });
}

async function attributeHintConversionToMembershipEvent({uid, eventName}) {
  const normalizedUid = nonEmptyString(uid);
  const normalizedEventName = normalizeMembershipEventName(eventName);
  if (!normalizedUid || !["purchase_success", "upgrade_after_warning", "upgrade_after_deny"].includes(normalizedEventName)) {
    return;
  }
  const dateKey = aiUsageDateKey();
  const dayRef = admin.firestore().collection(AI_FAQ_INTELLIGENCE_DAILY_COLLECTION).doc(dateKey);
  const userRef = dayRef.collection("userState").doc(normalizedUid);
  const nowMs = Date.now();

  await admin.firestore().runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    if (!userSnapshot.exists) {
      return;
    }
    const state = userSnapshot.data() || {};
    const lastHintAt = toEpochMillis(state.lastHintAtEpochMillis);
    const hintKeys = Array.isArray(state.lastHintKeys) ?
      state.lastHintKeys.map((value) => aiMetricKey(value, "hint")).filter(Boolean) :
      [];
    if (lastHintAt <= 0 || hintKeys.length === 0 || (nowMs - lastHintAt) > (48 * 60 * 60 * 1000)) {
      return;
    }
    const updates = {
      dateKey,
      totalHintAttributedUpgrades: admin.firestore.FieldValue.increment(1),
      [`hintAttributedByEvent.${normalizedEventName}`]: admin.firestore.FieldValue.increment(1),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    };
    for (const hintKey of hintKeys) {
      updates[`hintConversions.${hintKey}`] = admin.firestore.FieldValue.increment(1);
    }
    transaction.set(dayRef, updates, {merge: true});
  });
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
  const zeroSubtotalApplied = normalizedSubtotal === 0;

  return {
    shippingZone,
    shippingAmount: freeShippingApplied || zeroSubtotalApplied ? 0 : roundCurrency(baseRate),
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

function truncateTextBlock(value, maxChars = 600) {
  const normalized = nonEmptyString(value);
  if (!normalized) {
    return "";
  }

  return normalized.length <= maxChars ?
    normalized :
    `${normalized.slice(0, Math.max(0, maxChars - 1)).trim()}…`;
}

function maskEmail(email) {
  const normalized = normalizeEmail(email);
  if (!normalized || !normalized.includes("@")) {
    return "";
  }

  const [localPart, domainPart] = normalized.split("@");
  if (!localPart || !domainPart) {
    return "";
  }

  const prefix = localPart.length <= 2 ? localPart[0] || "*" : localPart.slice(0, 2);
  return `${prefix}***@${domainPart}`;
}

function resolveDateValueToEpochMillis(value) {
  if (!value) {
    return 0;
  }

  if (typeof value?.toMillis === "function") {
    return Number(value.toMillis()) || 0;
  }

  if (value instanceof Date) {
    return value.getTime();
  }

  const numericValue = Number(value);
  if (Number.isFinite(numericValue) && numericValue > 0) {
    return Math.floor(numericValue);
  }

  const parsed = Date.parse(`${value}`);
  return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
}

function formatEpochMillisAsIsoDate(value) {
  const epochMillis = resolveDateValueToEpochMillis(value);
  if (!epochMillis) {
    return "";
  }

  return new Date(epochMillis).toISOString().slice(0, 10);
}

function formatEuroAmount(value) {
  const amount = parsePrice(value);
  return Number.isFinite(amount) ? `${amount.toFixed(2)} EUR` : "";
}

function formatYesNo(value) {
  return value === true ? "ja" : "nein";
}

function resolveFaqPlanLabel(plan) {
  const normalizedPlan = nonEmptyString(plan)?.toLowerCase() || "";
  switch (normalizedPlan) {
    case USER_QUOTA_PLANS.creator:
      return "creator (in Teilen der mobilen UI als Pro gezeigt)";
    case USER_QUOTA_PLANS.studio:
      return "studio (in Teilen der mobilen UI als Creator gezeigt)";
    case USER_QUOTA_PLANS.ownerUnlimited:
      return "owner_unlimited";
    case USER_QUOTA_PLANS.internalTeam:
      return "internal_team";
    case USER_QUOTA_PLANS.free:
      return "free";
    default:
      return normalizedPlan || "unbekannt";
  }
}

function resolveFaqAccessModeLabel(accessMode) {
  switch (nonEmptyString(accessMode)?.toLowerCase()) {
    case AI_ACCESS_MODES.off:
      return "off";
    case AI_ACCESS_MODES.adminOnly:
      return "admin_only";
    case AI_ACCESS_MODES.signedIn:
    default:
      return "signed_in";
  }
}

function composeFaqFactSection(title, facts) {
  const lines = Array.isArray(facts) ? facts.filter(Boolean) : [];
  if (lines.length === 0) {
    return "";
  }

  return `[${title}]\n${lines.map((line) => `- ${line}`).join("\n")}`;
}

async function loadFaqSource(sourceName, loader, fallbackValue) {
  try {
    return await loader();
  } catch (error) {
    logger.warn("FAQ source could not be loaded.", {
      source: sourceName,
      error: error instanceof Error ? error.message : `${error}`,
    });
    return fallbackValue;
  }
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

function isZeroCostOrder(orderData) {
  return roundCurrency(orderData?.totalAmount) === 0;
}

function applyZeroCostOrderState(orderData) {
  const requiresShopifySubmission = orderData.fulfillmentProvider === "podpartner";
  return {
    ...orderData,
    paymentMethod: "Kostenlos",
    paymentStatus: "confirmed",
    paymentConfirmedAt: admin.firestore.FieldValue.serverTimestamp(),
    stripeCheckoutStatus: "not_required",
    shopifySyncStatus: requiresShopifySubmission ? "pending_submission" : "not_required",
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

  if (subtotalAmount < 0 || shippingAmount < 0 || taxRate < 0 || taxAmount < 0 || totalAmount < 0) {
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

  const syncStatus = nonEmptyString(afterData.shopifySyncStatus);
  if (syncStatus && syncStatus !== "pending_submission") {
    return false;
  }

  const previousSyncStatus = nonEmptyString(beforeData?.shopifySyncStatus);
  if (beforeData?.paymentStatus === "confirmed" && previousSyncStatus === syncStatus) {
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
  if (!hasConfiguredAiPlanProductMapping(androidProductIdToPlan)) {
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

  let purchaseData;
  try {
    purchaseData = await fetchGooglePlaySubscriptionPurchase({
      packageName,
      purchaseToken,
    });
  } catch (error) {
    const message = nonEmptyString(error?.message) || "Play-Store-Abo konnte nicht verifiziert werden.";
    if (error?.status === 403) {
      throw new HttpsError(
          "failed-precondition",
          "Google Play API ist fuer dieses Projekt noch nicht mit Abo-Rechten freigeschaltet.",
      );
    }
    if (error?.status === 404 || error?.status === 410) {
      throw new HttpsError(
          "not-found",
          "Der Play-Store-Kauf konnte nicht gefunden oder ist nicht mehr gueltig.",
      );
    }
    logger.error("Google Play subscription verification failed.", {
      uid,
      productId,
      packageName,
      status: error?.status || null,
      message,
    });
    throw new HttpsError("internal", message);
  }

  const resolvedState = resolveGooglePlaySubscriptionState({
    purchase: purchaseData,
    requestedProductId: productId,
    productIdToPlan: androidProductIdToPlan,
  });
  if (!resolvedState.plan) {
    throw new HttpsError(
        "failed-precondition",
        "Der bestaetigte Play-Store-Kauf passt zu keinem konfigurierten SkyOS KI-Plan.",
    );
  }

  if (resolvedState.needsAcknowledgement) {
    try {
      await acknowledgeGooglePlaySubscriptionPurchase({
        packageName,
        subscriptionId: resolvedState.productId,
        purchaseToken,
        developerPayload: uid,
      });
    } catch (error) {
      const message = nonEmptyString(error?.message) || "Play-Store-Kauf konnte nicht bestaetigt werden.";
      logger.error("Google Play subscription acknowledge failed.", {
        uid,
        productId: resolvedState.productId,
        packageName,
        status: error?.status || null,
        message,
      });
      throw new HttpsError(
          "failed-precondition",
          "Der Play-Store-Kauf wurde verifiziert, aber noch nicht sauber bestaetigt. Bitte pruefe die Google-Play-API-Rechte.",
      );
    }
  }

  const role = resolveUserRole(userData.role, userData.isAdmin === true, userData.email);
  const currentProvider = nonEmptyString(userData.aiSubscriptionProvider)?.toLowerCase() || "";
  const userRef = admin.firestore().doc(`users/${uid}`);
  const updates = {
    aiSubscriptionUpdatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  if (resolvedState.shouldGrantEntitlement) {
    Object.assign(updates, resolveAiLimitsUpdateForPlan(resolvedState.plan));
    Object.assign(updates, {
      aiSubscriptionStatus: resolvedState.status,
      aiSubscriptionPlan: resolvedState.plan,
      aiSubscriptionProvider: resolvedState.provider,
      aiSubscriptionSourcePlatform: resolvedState.sourcePlatform,
      aiSubscriptionProductId: resolvedState.productId || admin.firestore.FieldValue.delete(),
      aiSubscriptionStoreEnvironment: resolvedState.environment || admin.firestore.FieldValue.delete(),
      aiSubscriptionOriginalTransactionId: admin.firestore.FieldValue.delete(),
      aiSubscriptionTransactionId: admin.firestore.FieldValue.delete(),
      aiSubscriptionPriceId: admin.firestore.FieldValue.delete(),
      aiSubscriptionCurrentPeriodEndEpochSeconds:
        resolvedState.currentPeriodEndEpochSeconds || admin.firestore.FieldValue.delete(),
      aiSubscriptionCheckoutExpiresAtEpochSeconds: admin.firestore.FieldValue.delete(),
      aiSubscriptionCancelAtPeriodEnd: resolvedState.cancelAtPeriodEnd === true,
      aiSubscriptionActivatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });
  } else if (currentProvider === "play_store") {
    Object.assign(updates, resolveAiLimitsUpdateForRole(role));
    Object.assign(updates, {
      aiSubscriptionStatus: resolvedState.status,
      aiSubscriptionPlan: resolvedState.plan || admin.firestore.FieldValue.delete(),
      aiSubscriptionProvider: resolvedState.provider || admin.firestore.FieldValue.delete(),
      aiSubscriptionSourcePlatform: resolvedState.sourcePlatform || admin.firestore.FieldValue.delete(),
      aiSubscriptionProductId: resolvedState.productId || admin.firestore.FieldValue.delete(),
      aiSubscriptionStoreEnvironment: resolvedState.environment || admin.firestore.FieldValue.delete(),
      aiSubscriptionOriginalTransactionId: admin.firestore.FieldValue.delete(),
      aiSubscriptionTransactionId: admin.firestore.FieldValue.delete(),
      aiSubscriptionPriceId: admin.firestore.FieldValue.delete(),
      aiSubscriptionCurrentPeriodEndEpochSeconds:
        resolvedState.currentPeriodEndEpochSeconds || admin.firestore.FieldValue.delete(),
      aiSubscriptionCheckoutExpiresAtEpochSeconds: admin.firestore.FieldValue.delete(),
      aiSubscriptionCancelAtPeriodEnd: resolvedState.cancelAtPeriodEnd === true,
    });
  } else {
    return {
      status: resolvedState.status,
      provider: resolvedState.provider,
      plan: resolvedState.plan,
      currentPeriodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || null,
      reason: "no_play_store_entitlements",
    };
  }

  await userRef.set(updates, {merge: true});
  const syncEvent = await saveCanonicalAiEntitlement(uid, {
    plan: resolvedState.plan,
    status: resolvedState.status,
    provider: resolvedState.provider,
    source: resolvedState.sourcePlatform,
    productId: resolvedState.productId,
    periodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || 0,
    environment: resolvedState.environment,
    purchaseReference: resolvedState.purchaseReference,
    capabilities: resolveAiCapabilities({
      plan: resolvedState.plan,
      status: resolvedState.status,
      role,
    }),
  }, {
    externalEventId:
      resolvedState.purchaseReference ||
      `android_${productId}_${purchaseToken.slice(-12)}`,
    eventType: "android_sync",
    eventSource: "syncAndroidAiSubscriptionStatus",
    metadata: {
      status: resolvedState.status,
      rawState: resolvedState.rawState,
      requestedPlan: resolvedPlan,
      packageName,
      orderId: orderId || resolvedState.purchaseReference || "",
      purchaseTokenPreview: purchaseToken.slice(-8),
      productId: resolvedState.productId || productId,
      linkedPurchaseTokenPreview:
        resolvedState.linkedPurchaseToken ? resolvedState.linkedPurchaseToken.slice(-8) : "",
      acknowledgementState: resolvedState.acknowledgementState,
    },
    rawRef: "play_purchase_token",
  });

  logger.info("Android AI subscription sync requested.", {
    uid,
    productId: resolvedState.productId || productId,
    plan: resolvedState.plan || resolvedPlan,
    packageName: packageName || null,
    status: resolvedState.status,
  });

  return {
    status: resolvedState.status,
    provider: resolvedState.provider,
    plan: resolvedState.plan || resolvedPlan,
    eventId: syncEvent.eventId,
    currentPeriodEndEpochSeconds: resolvedState.currentPeriodEndEpochSeconds || null,
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
  const finalOrderData = isZeroCostOrder(orderData) ? applyZeroCostOrderState(orderData) : orderData;
  const orderRef = await admin.firestore().collection("orders").add(finalOrderData);

  logger.info("Merch order created.", {
    orderId: orderRef.id,
    itemCount: finalOrderData.items.length,
    fulfillmentProvider: finalOrderData.fulfillmentProvider,
    shippingZone: finalOrderData.shippingZone,
    zeroCost: isZeroCostOrder(finalOrderData),
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

  if (isZeroCostOrder(orderData)) {
    const finalOrderData = applyZeroCostOrderState(orderData);
    await orderRef.set(finalOrderData);

    logger.info("Zero-cost merch order confirmed without hosted payment.", {
      orderId: orderRef.id,
      platform,
      fulfillmentProvider: finalOrderData.fulfillmentProvider,
    });

    return {
      orderId: orderRef.id,
      checkoutUrl: buildReturnPageUrl({
        projectId,
        platform,
        status: "success",
        orderId: orderRef.id,
      }),
      sessionId: null,
    };
  }

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
  if (!hasConfiguredAiPlanProductMapping(iosProductIdToPlan)) {
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
  const automationScope = nonEmptyString(request.data?.automationScope) === "personal" ? "personal" : "owner";
  if (automationScope === "owner") {
    await assertOwner(request.auth);
  }
  const data = request.data?.data && typeof request.data.data === "object" && !Array.isArray(request.data.data)
    ? request.data.data
    : {};

  return triggerWorkflowAutomationWebhook({
    trigger,
    source,
    auth: request.auth,
    data,
    automationScope,
  });
});

exports.debugTriggerWorkflowAutomation = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "debugTriggerWorkflowAutomation");
  assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, um den Workflow-Debug-Trigger zu starten.",
  );

  const automationScope = nonEmptyString(request.data?.automationScope) === "personal" ? "personal" : "owner";
  if (automationScope === "owner") {
    await assertOwner(request.auth);
  }

  const trigger = nonEmptyString(request.data?.trigger) || "owner_debug_test";
  const source = nonEmptyString(request.data?.source) || "owner_debug";
  const data = request.data?.data && typeof request.data.data === "object" && !Array.isArray(request.data.data) ?
    request.data.data :
    {};

  const startedAt = Date.now();
  try {
    const response = await triggerWorkflowAutomationWebhook({
      trigger,
      source,
      auth: request.auth,
      data,
      automationScope,
    });
    return {
      success: true,
      state: "external_completed",
      provider: nonEmptyString(response?.provider) || "activepieces",
      responseTimeMs: Date.now() - startedAt,
      requestId: nonEmptyString(response?.requestId) || "",
      workflowName: nonEmptyString(response?.workflowName) || "",
      status: Number(response?.status) || 0,
      message: nonEmptyString(response?.message) || "Debug trigger erfolgreich.",
      automationSchemaVersion: nonEmptyString(response?.schemaVersion) || "",
      results: Array.isArray(response?.results) ? response.results : [],
      auditExpected: true,
    };
  } catch (error) {
    const message = error instanceof Error ? error.message : `${error}`;
    return {
      success: false,
      state: "external_failed",
      provider: "external",
      responseTimeMs: Date.now() - startedAt,
      requestId: "",
      workflowName: "",
      status: 0,
      message,
      auditExpected: true,
    };
  }
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

  await attributeHintConversionToMembershipEvent({
    uid,
    eventName,
  });

  return {
    status: "recorded",
    eventId,
    eventName,
  };
});

exports.getAiFaqOwnerIntelligence = onCall({
  region: "us-central1",
  timeoutSeconds: 60,
}, async (request) => {
  await assertCallableSecurity(request, "getAiFaqOwnerIntelligence");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) {
    throw new HttpsError("permission-denied", "Nur Owner/Admin darf FAQ-Intelligence sehen.");
  }

  const windowDaysRaw = Number(request.data?.windowDays);
  const windowDays = [7, 14, 30, 60].includes(windowDaysRaw) ? windowDaysRaw : 30;
  const range = rollingWindowRange(windowDays);
  const startDateKey = new Date(range.startMs).toISOString().slice(0, 10);
  const [snapshot, runtimeSettings] = await Promise.all([
    admin.firestore()
        .collection(AI_FAQ_INTELLIGENCE_DAILY_COLLECTION)
        .where("dateKey", ">=", startDateKey)
        .orderBy("dateKey", "asc")
        .limit(120)
        .get(),
    loadAiRuntimeSettings(),
  ]);

  const aggregateMap = (fieldName) => {
    const output = {};
    for (const doc of snapshot.docs) {
      const source = doc.data()?.[fieldName];
      if (!source || typeof source !== "object" || Array.isArray(source)) continue;
      for (const [key, value] of Object.entries(source)) {
        output[key] = (Number(output[key]) || 0) + (Number(value) || 0);
      }
    }
    return output;
  };

  const totalFaqAnswers = snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.totalFaqAnswers) || 0), 0);
  const totalHintWindows = snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.totalHintWindows) || 0), 0);
  const totalHintAttributedUpgrades = snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.totalHintAttributedUpgrades) || 0), 0);
  const repeatQuestions = snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.repeatQuestions) || 0), 0);
  const repeatAfterHint = snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.repeatAfterHint) || 0), 0);
  const faqByTopic = aggregateMap("faqByTopic");
  const faqByQuestion = aggregateMap("faqByQuestion");
  const hintTriggers = aggregateMap("hintTriggers");
  const hintConversions = aggregateMap("hintConversions");
  const repeatAfterHintByTrigger = aggregateMap("repeatAfterHintByTrigger");

  const triggerInsights = Object.keys(hintTriggers)
      .map((key) => {
        const triggers = Number(hintTriggers[key]) || 0;
        const conversions = Number(hintConversions[key]) || 0;
        const repeats = Number(repeatAfterHintByTrigger[key]) || 0;
        const conversionRate = triggers > 0 ? Number((conversions / triggers).toFixed(4)) : 0;
        const repeatRate = triggers > 0 ? Number((repeats / triggers).toFixed(4)) : 0;
        return {
          triggerKey: key,
          triggers,
          conversions,
          repeatsAfterHint: repeats,
          conversionRate,
          repeatRate,
          likelyUseless: triggers >= 5 && conversionRate < 0.05 && repeatRate > 0.25,
        };
      })
      .sort((a, b) => b.triggers - a.triggers);

  const strongestTriggers = triggerInsights
      .filter((entry) => entry.triggers >= 3)
      .sort((a, b) => {
        if (b.conversionRate !== a.conversionRate) return b.conversionRate - a.conversionRate;
        if (a.repeatRate !== b.repeatRate) return a.repeatRate - b.repeatRate;
        return b.triggers - a.triggers;
      })
      .slice(0, 5);
  const weakTriggers = triggerInsights
      .filter((entry) => !entry.likelyUseless && entry.triggers >= 5 && (entry.conversionRate < 0.08 || entry.repeatRate > 0.25))
      .sort((a, b) => {
        if (a.conversionRate !== b.conversionRate) return a.conversionRate - b.conversionRate;
        if (b.repeatRate !== a.repeatRate) return b.repeatRate - a.repeatRate;
        return b.triggers - a.triggers;
      })
      .slice(0, 5);
  const likelyUselessTriggers = triggerInsights.filter((entry) => entry.likelyUseless).slice(0, 5);

  const topTopics = collectTopEntries(faqByTopic, 10);
  const totalTopicVolume = topTopics.reduce((sum, entry) => sum + (Number(entry.value) || 0), 0);
  const repeatHeavyTopics = topTopics
      .map((entry) => ({
        ...entry,
        share: totalTopicVolume > 0 ? Number(((Number(entry.value) || 0) / totalTopicVolume).toFixed(4)) : 0,
      }))
      .filter((entry) => Number(entry.value) >= 5 && entry.share >= 0.18)
      .slice(0, 5);

  const actionLayer = runtimeSettings?.bot?.actionLayer || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer;
  const recommendations = [];
  const pushReviewRecommendation = (recommendation) => {
    if (!recommendation || typeof recommendation !== "object") return;
    if (!nonEmptyString(recommendation.id)) return;
    recommendations.push({
      priority: "medium",
      confidence: 0.6,
      ...recommendation,
      confidence: Number(Math.max(0, Math.min(1, Number(recommendation.confidence || 0.6))).toFixed(2)),
    });
  };

  const aiLimitEntry = triggerInsights.find((entry) => entry.triggerKey === "ai_limit_fast_erreicht");
  if (aiLimitEntry) {
    if (aiLimitEntry.triggers >= 8 && aiLimitEntry.repeatRate >= 0.3 && aiLimitEntry.conversionRate < 0.06) {
      pushReviewRecommendation({
        id: "raise_warning_threshold",
        actionType: "adjust_threshold",
        targetField: "warningThresholdPercent",
        suggestedValue: Math.min(95, Number(actionLayer.warningThresholdPercent || 70) + 5),
        title: "Warning Threshold leicht anheben",
        summary: "AI-Limit-Hinweis erzeugt viele Wiederholungen bei niedriger Conversion. Hoeherer Threshold reduziert Hinweis-Rauschen.",
        rationale: `Trigger ${aiLimitEntry.triggers}x, Conversion ${aiLimitEntry.conversionRate}, Repeat ${aiLimitEntry.repeatRate}`,
        priority: "high",
        confidence: 0.77,
      });
    } else if (aiLimitEntry.triggers >= 10 && aiLimitEntry.conversionRate >= 0.12 && aiLimitEntry.repeatRate <= 0.12) {
      pushReviewRecommendation({
        id: "lower_warning_threshold",
        actionType: "adjust_threshold",
        targetField: "warningThresholdPercent",
        suggestedValue: Math.max(55, Number(actionLayer.warningThresholdPercent || 70) - 5),
        title: "Warning Threshold leicht senken",
        summary: "AI-Limit-Hinweis konvertiert gut ohne Support-Spikes. Frueheres Triggern kann Upside vergroessern.",
        rationale: `Trigger ${aiLimitEntry.triggers}x, Conversion ${aiLimitEntry.conversionRate}, Repeat ${aiLimitEntry.repeatRate}`,
        priority: "medium",
        confidence: 0.71,
      });
    }
  }

  for (const entry of likelyUselessTriggers.slice(0, 2)) {
    pushReviewRecommendation({
      id: `disable_${entry.triggerKey}`,
      actionType: "disable_trigger",
      targetField: entry.triggerKey,
      suggestedValue: false,
      title: `Trigger pruefen: ${entry.triggerKey}`,
      summary: "Trigger wirkt aktuell schwach (niedrige Conversion bei hoher Repeat-Last). Testweise deaktivieren und 7 Tage beobachten.",
      rationale: `Trigger ${entry.triggers}x, Conversion ${entry.conversionRate}, Repeat ${entry.repeatRate}`,
      priority: "high",
      confidence: 0.84,
    });
  }

  const usageTrigger = triggerInsights.find((entry) => entry.triggerKey === "upgrade_sinnvoll_basierend_auf_nutzung");
  if (usageTrigger && usageTrigger.triggers >= 6 && usageTrigger.conversionRate < 0.08) {
    pushReviewRecommendation({
      id: "improve_upgrade_hint_text",
      actionType: "improve_hint_text",
      targetField: "upgradeHintFreeToProText",
      suggestedValue: nonEmptyString(actionLayer.upgradeHintFreeToProText) || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintFreeToProText,
      title: "Upgrade-Hint Text schaerfen",
      summary: "Usage-basierter Upgrade-Hinweis triggert oft, konvertiert aber schwach. Text klarer auf konkreten Nutzen ausrichten.",
      rationale: `Trigger ${usageTrigger.triggers}x, Conversion ${usageTrigger.conversionRate}`,
      priority: "medium",
      confidence: 0.69,
    });
  }

  if (repeatHeavyTopics.length >= 2 && actionLayer.faqPriorityMode !== "live_owner_generic") {
    pushReviewRecommendation({
      id: "prefer_live_facts_faq",
      actionType: "adjust_faq_priority",
      targetField: "faqPriorityMode",
      suggestedValue: "live_owner_generic",
      title: "FAQ-Prioritaet auf Live Facts stellen",
      summary: "Wiederholte FAQ-Themen deuten auf fehlende Praezision hin. Live Facts zuerst reduziert Rueckfragen.",
      rationale: `Repeat-heavy Topics: ${repeatHeavyTopics.map((entry) => entry.key).join(", ")}`,
      priority: "medium",
      confidence: 0.73,
    });
  }
  const strategyInsights = buildFaqStrategyInsights({
    docs: snapshot.docs,
    actionLayer,
    triggerInsights,
    repeatHeavyTopics,
  });

  return {
    generatedAtEpochMillis: Date.now(),
    windowDays,
    totals: {
      faqAnswers: totalFaqAnswers,
      hintWindows: totalHintWindows,
      hintAttributedUpgrades: totalHintAttributedUpgrades,
      repeatQuestions,
      repeatAfterHint,
      hintUpgradeRate: totalHintWindows > 0 ? Number((totalHintAttributedUpgrades / totalHintWindows).toFixed(4)) : 0,
      repeatAfterHintRate: totalHintWindows > 0 ? Number((repeatAfterHint / totalHintWindows).toFixed(4)) : 0,
    },
    topFaqTopics: topTopics,
    topFaqQuestions: collectTopEntries(faqByQuestion, 20),
    triggerInsights,
    uselessTriggers: likelyUselessTriggers,
    reviewLoop: {
      strongestTriggers,
      weakTriggers,
      likelyUselessTriggers,
      repeatHeavyTopics,
      recommendations: recommendations.slice(0, 8),
      strategyInsights,
      actionLayerSnapshot: {
        warningThresholdPercent: Number(actionLayer.warningThresholdPercent || 70),
        criticalThresholdPercent: Number(actionLayer.criticalThresholdPercent || 90),
        faqPriorityMode: nonEmptyString(actionLayer.faqPriorityMode) || "live_owner_generic",
        proactiveHintsEnabled: actionLayer.proactiveHintsEnabled !== false,
      },
    },
    dataNotes: {
      source: AI_FAQ_INTELLIGENCE_DAILY_COLLECTION,
      attributionWindowHours: 48,
      repeatWindowHours: 24,
    },
  };
});

async function loadFaqReviewLoopForWindow(windowDays = 30) {
  const normalizedWindow = [7, 14, 30, 60].includes(Number(windowDays)) ? Number(windowDays) : 30;
  const range = rollingWindowRange(normalizedWindow);
  const startDateKey = new Date(range.startMs).toISOString().slice(0, 10);
  const [snapshot, runtimeSettings] = await Promise.all([
    admin.firestore()
        .collection(AI_FAQ_INTELLIGENCE_DAILY_COLLECTION)
        .where("dateKey", ">=", startDateKey)
        .orderBy("dateKey", "asc")
        .limit(120)
        .get(),
    loadAiRuntimeSettings(),
  ]);

  const aggregateMap = (fieldName) => {
    const output = {};
    for (const doc of snapshot.docs) {
      const source = doc.data()?.[fieldName];
      if (!source || typeof source !== "object" || Array.isArray(source)) continue;
      for (const [key, value] of Object.entries(source)) {
        output[key] = (Number(output[key]) || 0) + (Number(value) || 0);
      }
    }
    return output;
  };

  const hintTriggers = aggregateMap("hintTriggers");
  const hintConversions = aggregateMap("hintConversions");
  const repeatAfterHintByTrigger = aggregateMap("repeatAfterHintByTrigger");
  const faqByTopic = aggregateMap("faqByTopic");
  const triggerInsights = Object.keys(hintTriggers)
      .map((key) => {
        const triggers = Number(hintTriggers[key]) || 0;
        const conversions = Number(hintConversions[key]) || 0;
        const repeats = Number(repeatAfterHintByTrigger[key]) || 0;
        const conversionRate = triggers > 0 ? Number((conversions / triggers).toFixed(4)) : 0;
        const repeatRate = triggers > 0 ? Number((repeats / triggers).toFixed(4)) : 0;
        return {
          triggerKey: key,
          triggers,
          conversions,
          repeatsAfterHint: repeats,
          conversionRate,
          repeatRate,
          likelyUseless: triggers >= 5 && conversionRate < 0.05 && repeatRate > 0.25,
        };
      })
      .sort((a, b) => b.triggers - a.triggers);
  const likelyUselessTriggers = triggerInsights.filter((entry) => entry.likelyUseless).slice(0, 5);
  const topTopics = collectTopEntries(faqByTopic, 10);
  const totalTopicVolume = topTopics.reduce((sum, entry) => sum + (Number(entry.value) || 0), 0);
  const repeatHeavyTopics = topTopics
      .map((entry) => ({
        ...entry,
        share: totalTopicVolume > 0 ? Number(((Number(entry.value) || 0) / totalTopicVolume).toFixed(4)) : 0,
      }))
      .filter((entry) => Number(entry.value) >= 5 && entry.share >= 0.18)
      .slice(0, 5);
  const actionLayer = runtimeSettings?.bot?.actionLayer || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer;

  const recommendations = [];
  const aiLimitEntry = triggerInsights.find((entry) => entry.triggerKey === "ai_limit_fast_erreicht");
  if (aiLimitEntry?.triggers >= 8 && aiLimitEntry.repeatRate >= 0.3 && aiLimitEntry.conversionRate < 0.06) {
    recommendations.push({
      id: "raise_warning_threshold",
      actionType: "adjust_threshold",
      targetField: "warningThresholdPercent",
      suggestedValue: Math.min(95, Number(actionLayer.warningThresholdPercent || 70) + 5),
    });
  } else if (aiLimitEntry?.triggers >= 10 && aiLimitEntry.conversionRate >= 0.12 && aiLimitEntry.repeatRate <= 0.12) {
    recommendations.push({
      id: "lower_warning_threshold",
      actionType: "adjust_threshold",
      targetField: "warningThresholdPercent",
      suggestedValue: Math.max(55, Number(actionLayer.warningThresholdPercent || 70) - 5),
    });
  }
  for (const entry of likelyUselessTriggers.slice(0, 2)) {
    recommendations.push({
      id: `disable_${entry.triggerKey}`,
      actionType: "disable_trigger",
      targetField: entry.triggerKey,
      suggestedValue: false,
    });
  }
  const usageTrigger = triggerInsights.find((entry) => entry.triggerKey === "upgrade_sinnvoll_basierend_auf_nutzung");
  if (usageTrigger && usageTrigger.triggers >= 6 && usageTrigger.conversionRate < 0.08) {
    recommendations.push({
      id: "improve_upgrade_hint_text",
      actionType: "improve_hint_text",
      targetField: "upgradeHintFreeToProText",
      suggestedValue: nonEmptyString(actionLayer.upgradeHintFreeToProText) || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer.upgradeHintFreeToProText,
    });
  }
  if (repeatHeavyTopics.length >= 2 && actionLayer.faqPriorityMode !== "live_owner_generic") {
    recommendations.push({
      id: "prefer_live_facts_faq",
      actionType: "adjust_faq_priority",
      targetField: "faqPriorityMode",
      suggestedValue: "live_owner_generic",
    });
  }
  const strategyInsights = buildFaqStrategyInsights({
    docs: snapshot.docs,
    actionLayer,
    triggerInsights,
    repeatHeavyTopics,
  });

  const totals = {
    hintWindows: snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.totalHintWindows) || 0), 0),
    hintAttributedUpgrades: snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.totalHintAttributedUpgrades) || 0), 0),
    repeatAfterHint: snapshot.docs.reduce((sum, doc) => sum + (Number(doc.data()?.repeatAfterHint) || 0), 0),
  };
  return {
    runtimeSettings,
    actionLayer,
    repeatHeavyTopics,
    triggerInsights,
    recommendations,
    strategyInsights,
    totals,
    windowDays: normalizedWindow,
  };
}

exports.previewAiFaqReviewRecommendation = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "previewAiFaqReviewRecommendation");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) throw new HttpsError("permission-denied", "Nur Owner/Admin.");
  const recommendation = request.data?.recommendation || {};
  const windowDays = Number(request.data?.windowDays) || 30;
  const reviewLoop = await loadFaqReviewLoopForWindow(windowDays);
  const matched = reviewLoop.recommendations.find((entry) => entry.id === recommendation.id);
  const candidate = matched || recommendation;
  const resolution = resolveReviewRecommendationAction({
    recommendation: candidate,
    currentActionLayer: reviewLoop.actionLayer,
    repeatHeavyTopics: reviewLoop.repeatHeavyTopics,
  });
  return {
    allowed: resolution.allowed,
    safeguards: resolution.safeguards,
    recommendation: resolution.recommendationEntry,
    before: {
      actionLayer: reviewLoop.actionLayer,
      metrics: reviewLoop.totals,
    },
    after: {
      actionLayer: resolution.nextActionLayer,
      metrics: reviewLoop.totals,
    },
  };
});

exports.applyAiFaqReviewRecommendation = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "applyAiFaqReviewRecommendation");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) throw new HttpsError("permission-denied", "Nur Owner/Admin.");

  const reviewLoop = await loadFaqReviewLoopForWindow(Number(request.data?.windowDays) || 30);
  const inputId = nonEmptyString(request.data?.recommendationId) || nonEmptyString(request.data?.recommendation?.id) || "";
  const matched = reviewLoop.recommendations.find((entry) => entry.id === inputId);
  if (!matched) throw new HttpsError("failed-precondition", "Recommendation nicht mehr aktuell. Bitte refresh.");
  const resolution = resolveReviewRecommendationAction({
    recommendation: matched,
    currentActionLayer: reviewLoop.actionLayer,
    repeatHeavyTopics: reviewLoop.repeatHeavyTopics,
  });
  if (!resolution.allowed) {
    return {
      status: "blocked",
      safeguards: resolution.safeguards,
      recommendation: resolution.recommendationEntry,
    };
  }

  const runtimeRef = admin.firestore().collection(AI_RUNTIME_CONFIG_COLLECTION).doc(AI_RUNTIME_CONFIG_DOCUMENT);
  await runtimeRef.set({
    bot: {actionLayer: resolution.nextActionLayer},
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  const changeId = `faq_tune_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
  await admin.firestore().collection(AI_FAQ_REVIEW_CHANGE_LOG_COLLECTION).doc(changeId).set({
    changeId,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    createdAtEpochMillis: Date.now(),
    createdByUid: nonEmptyString(request.auth?.uid) || "",
    reverted: false,
    recommendation: resolution.recommendationEntry,
    before: {
      actionLayer: reviewLoop.actionLayer,
      metrics: reviewLoop.totals,
    },
    after: {
      actionLayer: resolution.nextActionLayer,
      metrics: reviewLoop.totals,
    },
  }, {merge: true});
  aiRuntimeSettingsCache.expiresAt = 0;
  return {
    status: "applied",
    changeId,
    before: {actionLayer: reviewLoop.actionLayer, metrics: reviewLoop.totals},
    after: {actionLayer: resolution.nextActionLayer, metrics: reviewLoop.totals},
    safeguards: resolution.safeguards,
  };
});

exports.revertLastAiFaqReviewChange = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "revertLastAiFaqReviewChange");
  const isOwner = await isOwnerAuth(request.auth);
  const isAdmin = await isAdminAuth(request.auth);
  if (!isOwner && !isAdmin) throw new HttpsError("permission-denied", "Nur Owner/Admin.");

  const snapshot = await admin.firestore()
      .collection(AI_FAQ_REVIEW_CHANGE_LOG_COLLECTION)
      .where("reverted", "==", false)
      .orderBy("createdAtEpochMillis", "desc")
      .limit(1)
      .get();
  if (snapshot.empty) {
    return {status: "noop", message: "Kein offener Change zum Revertieren."};
  }
  const doc = snapshot.docs[0];
  const data = doc.data() || {};
  const beforeActionLayer = data.before?.actionLayer || DEFAULT_AI_RUNTIME_SETTINGS.bot.actionLayer;
  const runtimeRef = admin.firestore().collection(AI_RUNTIME_CONFIG_COLLECTION).doc(AI_RUNTIME_CONFIG_DOCUMENT);
  await runtimeRef.set({
    bot: {actionLayer: beforeActionLayer},
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  await doc.ref.set({
    reverted: true,
    revertedAt: admin.firestore.FieldValue.serverTimestamp(),
    revertedByUid: nonEmptyString(request.auth?.uid) || "",
  }, {merge: true});
  aiRuntimeSettingsCache.expiresAt = 0;
  return {
    status: "reverted",
    changeId: data.changeId || doc.id,
    restoredActionLayer: beforeActionLayer,
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
  maxInstances: 5,
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
  maxInstances: 5,
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
  maxInstances: 5,
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
  maxInstances: 5,
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
  const runtimeSettings = await loadAiRuntimeSettings();
  if (runtimeSettings.bot.killSwitchEnabled) {
    throw new HttpsError("permission-denied", "Der SkyOS Bot ist owner-seitig pausiert.");
  }
  const input = parseCallableInput(
      aiTextRequestSchema,
      request.data,
      "Die KI-Anfrage konnte so nicht gestartet werden.",
  );
  const aiLevel = normalizeAiExperienceLevel(input.aiLevel);
  const selectedModel = resolveAiTextModelForLevel(aiLevel, runtimeSettings);
  const usage = await authorizeAiUsage({
    auth: request.auth,
    kind: AI_USAGE_KINDS.text,
    provider: "google_vertex",
    model: selectedModel,
    eventType: "bot_text_generation",
    sourceRoute: "callable.generateAiText",
    functionName: "generateAiText",
    featureClass: AI_FEATURE_CLASSES.text,
    resultType: "text",
    requestWeight: aiLevelRequestWeightMultiplier(aiLevel),
    aiLevel,
    estimatedCostMicros: 12_000,
    requestId: nonEmptyString(request.data?.requestId) || "",
  });
  const textResult = await generateAiTextReply({
    prompt: input.prompt,
    mode: input.mode,
    aiLevel,
    selectedModel,
    runtimeSettings,
    usage,
    auth: request.auth,
  });

  return {
    reply: textResult.reply,
    mode: input.mode,
    botDecision: textResult.decision,
    historyRetentionDays: usage.historyRetentionDays,
    usage: {
      kind: usage.kind,
      featureClass: usage.featureClass,
      remainingForKind: usage.remainingForKind,
      limitForKind: usage.limitForKind,
      remainingForLevel: usage.remainingForLevel,
      limitForLevel: usage.limitForLevel,
      aiLevel: usage.aiLevel,
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
  const runtimeSettings = await loadAiRuntimeSettings();
  if (runtimeSettings.bot.killSwitchEnabled) {
    throw new HttpsError("permission-denied", "Der SkyOS Bot ist owner-seitig pausiert.");
  }
  if (!runtimeSettings.bot.routingPolicy.allowVisualGeneration) {
    throw new HttpsError("failed-precondition", "Visual-Generierung ist owner-seitig pausiert.");
  }
  const input = parseCallableInput(
      aiVisualRequestSchema,
      request.data,
      "Die Visual-Anfrage konnte so nicht gestartet werden.",
  );
  const aiLevel = normalizeAiExperienceLevel(input.aiLevel);
  const selectedModel = resolveAiVisualModelForLevel(aiLevel, runtimeSettings);
  try {
    const usage = await authorizeAiUsage({
      auth: request.auth,
      kind: AI_USAGE_KINDS.visual,
      provider: "google_vertex",
      model: selectedModel,
      eventType: "bot_visual_generation",
      sourceRoute: "callable.generateAiVisual",
      functionName: "generateAiVisual",
      featureClass: AI_FEATURE_CLASSES.image,
      resultType: "image",
      requestWeight: 2 * aiLevelRequestWeightMultiplier(aiLevel),
      aiLevel,
      estimatedCostMicros: 95_000,
      requestId: nonEmptyString(request.data?.requestId) || "",
    });
    const visual = await generateAiVisualResult(input.prompt, runtimeSettings, usage, selectedModel);

    return {
      ...visual,
      botDecision: visual.decision,
      historyRetentionDays: usage.historyRetentionDays,
      usage: {
        kind: usage.kind,
        featureClass: usage.featureClass,
        remainingForKind: usage.remainingForKind,
        limitForKind: usage.limitForKind,
        remainingForLevel: usage.remainingForLevel,
        limitForLevel: usage.limitForLevel,
        aiLevel: usage.aiLevel,
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
    return "Antwortformat: Workflow-Ziel, Trigger, benoetigte Inputs, erwartete Outputs, Fehlerfaelle, kurze Uebergabe an den globalen Activepieces Owner-Flow, Naechste Schritte.";
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

const AGENT_ATTACHMENT_MAX_COUNT = 5;
const AGENT_ATTACHMENT_MAX_BYTES = 256 * 1024;

function normalizeAgentInboundAttachments(rawAttachments) {
  if (!Array.isArray(rawAttachments) || !rawAttachments.length) {
    return {sanitized: [], summary: ""};
  }

  const sanitized = [];
  const lines = [];

  for (const item of rawAttachments.slice(0, AGENT_ATTACHMENT_MAX_COUNT)) {
    if (!item || typeof item !== "object" || Array.isArray(item)) {
      continue;
    }

    const name = trimTextMax(nonEmptyString(item.name) || "attachment", 255);
    const kind = nonEmptyString(item.kind)?.toLowerCase() || "file";
    const normalizedKind = ["image", "video", "audio", "text", "document", "file"].includes(kind) ?
      kind :
      "file";
    const mimeType = trimTextMax(
        nonEmptyString(item.mimeType) || "application/octet-stream",
        120,
    );
    const source = nonEmptyString(item.source)?.toLowerCase() || "inline";
    if (source !== "inline") {
      continue;
    }

    const inlineBase64 = nonEmptyString(item.inlineBase64) || "";
    if (!inlineBase64) {
      continue;
    }

    let buffer;
    try {
      buffer = Buffer.from(inlineBase64, "base64");
    } catch (error) {
      continue;
    }

    if (!buffer.length || buffer.length > AGENT_ATTACHMENT_MAX_BYTES) {
      continue;
    }

    const id = nonEmptyString(item.id) || `att_${sanitized.length + 1}`;
    sanitized.push({
      id,
      name,
      kind: normalizedKind,
      mimeType,
      source: "inline",
      inlineBase64,
    });
    lines.push(`- ${name} (${normalizedKind}, ${mimeType}, ${buffer.length} Bytes)`);
  }

  const summary = lines.length ?
    `Angehaengte Dateien (nur Metadaten; Binaerinhalt wird separat an Automation uebergeben):\n${lines.join("\n")}` :
    "";

  return {sanitized, summary};
}

function composeAgentExecutionPrompt({workspaceContext, history, prompt, mode, attachmentSummary = ""}) {
  const attachmentBlock = nonEmptyString(attachmentSummary) ?
    `\n\n${attachmentSummary.trim()}` :
    "";
  return `
${workspaceContext}

Bisherige Unterhaltung:
${formatHistory(history)}

Aktuelle Nutzeranfrage:
${prompt}${attachmentBlock}

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

function extractMemoryWriteCandidate(promptText) {
  const prompt = nonEmptyString(promptText)?.trim() || "";
  if (!prompt || prompt.length < 6) {
    return "";
  }
  const memorySignals = [
    /\bmerk dir\b/i,
    /\bmerke dir\b/i,
    /\bnotier(?:e)? dir\b/i,
    /\bremember\b/i,
    /\bnote this\b/i,
    /\bmy goal is\b/i,
    /\bmein ziel ist\b/i,
    /\bich will\b/i,
  ];
  const isMemoryPrompt = memorySignals.some((pattern) => pattern.test(prompt));
  return isMemoryPrompt ? trimTextMax(prompt, 500) : "";
}

function containsSensitiveMemoryContent(text) {
  const value = nonEmptyString(text) || "";
  if (!value) {
    return false;
  }
  const sensitivePatterns = [
    /\bpasswort\b/i,
    /\bpassword\b/i,
    /\botp\b/i,
    /\b2fa\b/i,
    /\bpin\b/i,
    /\bapi[\s-_]?key\b/i,
    /\bsecret\b/i,
    /\btoken\b/i,
    /\bkreditkarte\b/i,
    /\bcredit card\b/i,
    /\biban\b/i,
    /\bwallet\b/i,
    /\bseed phrase\b/i,
  ];
  if (sensitivePatterns.some((pattern) => pattern.test(value))) {
    return true;
  }
  const longDigitChunks = value.match(/\d{12,19}/g) || [];
  return longDigitChunks.length > 0;
}

async function maybePersistUserMemorySignal({auth, prompt, mode = ""}) {
  if (!auth?.uid) {
    return {stored: false, reason: "no_uid"};
  }
  const candidate = extractMemoryWriteCandidate(prompt);
  if (!candidate) {
    return {stored: false, reason: "no_signal"};
  }
  if (containsSensitiveMemoryContent(candidate)) {
    return {stored: false, reason: "sensitive_content"};
  }

  const profileRef = admin.firestore()
      .collection("users")
      .doc(auth.uid)
      .collection("memoryProfile")
      .doc("main");

  try {
    const snapshot = await profileRef.get().catch(() => null);
    const existingData = snapshot?.exists ? (snapshot.data() || {}) : {};
    const existingNotes = Array.isArray(existingData.memoryNotes) ?
      existingData.memoryNotes
          .filter((item) => typeof item === "string")
          .map((item) => trimTextMax(item, 500))
          .filter(Boolean) :
      [];
    const nextNotes = [candidate, ...existingNotes.filter((item) => item !== candidate)]
        .slice(0, 20);

    await profileRef.set({
      memoryNotes: nextNotes,
      lastUserMemoryNote: candidate,
      lastUserMemoryNoteAt: admin.firestore.FieldValue.serverTimestamp(),
      lastMemorySource: "agent_prompt",
      lastMemoryMode: trimTextMax(nonEmptyString(mode) || "", 40),
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    return {stored: true, note: candidate};
  } catch (error) {
    logger.warn("Auto memory write skipped.", {
      uid: auth.uid,
      error: error instanceof Error ? error.message : `${error}`,
    });
    return {stored: false, reason: "write_failed"};
  }
}

async function loadUserMemorySnapshot(uid) {
  if (!uid) {
    return {
      profileSummary: "",
      goals: [],
      habits: [],
      moodLogs: [],
    };
  }
  const userRef = admin.firestore().collection("users").doc(uid);
  const profileSnap = await userRef.collection("memoryProfile").doc("main").get().catch(() => null);
  const goalsSnap = await userRef
      .collection("goals")
      .orderBy("updatedAt", "desc")
      .limit(3)
      .get()
      .catch(() => null);
  const habitsSnap = await userRef
      .collection("habits")
      .orderBy("updatedAt", "desc")
      .limit(3)
      .get()
      .catch(() => null);
  const moodLogsSnap = await userRef
      .collection("moodLogs")
      .orderBy("createdAt", "desc")
      .limit(3)
      .get()
      .catch(() => null);

  const profileData = profileSnap?.exists ? (profileSnap.data() || {}) : {};
  const goals = Array.isArray(goalsSnap?.docs) ?
    goalsSnap.docs.map((doc) => trimTextMax(JSON.stringify(doc.data() || {}), 420)) :
    [];
  const habits = Array.isArray(habitsSnap?.docs) ?
    habitsSnap.docs.map((doc) => trimTextMax(JSON.stringify(doc.data() || {}), 420)) :
    [];
  const moodLogs = Array.isArray(moodLogsSnap?.docs) ?
    moodLogsSnap.docs.map((doc) => trimTextMax(JSON.stringify(doc.data() || {}), 420)) :
    [];

  return {
    profileSummary: Object.keys(profileData).length > 0 ? trimTextMax(JSON.stringify(profileData), 1200) : "",
    goals,
    habits,
    moodLogs,
  };
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

    const ownerWorkflowSettings = await loadWorkflowAutomationSettingsForUser(auth.uid, "owner").catch(() => null);
    if (ownerWorkflowSettings) {
      const workflowStatus = ownerWorkflowSettings.isEnabled && buildAutomationWebhookUrl(ownerWorkflowSettings.baseURL, ownerWorkflowSettings.webhookPath) ?
        `bereit (${ownerWorkflowSettings.workflowName})` :
        "noch nicht bereit";
      lines.push(`Globaler Activepieces Owner-Flow: ${workflowStatus}`);
      const knowledgeContext = nonEmptyString(ownerWorkflowSettings.knowledgeContext);
      if (knowledgeContext) {
        lines.push(`Owner-Flow Knowledge-Kontext: ${knowledgeContext}`);
      }
    }

    const personalWorkflowSettings = await loadWorkflowAutomationSettingsForUser(auth.uid, "personal").catch(() => null);
    if (personalWorkflowSettings?.isEnabled) {
      const personalWorkflowStatus = buildAutomationWebhookUrl(personalWorkflowSettings.baseURL, personalWorkflowSettings.webhookPath) ?
        `bereit (${personalWorkflowSettings.workflowName}, ${personalWorkflowSettings.provider})` :
        "noch nicht bereit";
      lines.push(`Persoenlicher Workflow: ${personalWorkflowStatus}`);
      const personalKnowledgeContext = nonEmptyString(personalWorkflowSettings.knowledgeContext);
      if (personalKnowledgeContext) {
        lines.push(`Persoenlicher Workflow-Kontext: ${personalKnowledgeContext}`);
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

    const memorySnapshot = await loadUserMemorySnapshot(auth.uid).catch(() => null);
    if (memorySnapshot) {
      if (memorySnapshot.profileSummary) {
        lines.push(`Memory-Profil: ${memorySnapshot.profileSummary}`);
      }
      if (memorySnapshot.goals.length > 0) {
        lines.push(`Memory-Goals: ${memorySnapshot.goals.join(" | ")}`);
      }
      if (memorySnapshot.habits.length > 0) {
        lines.push(`Memory-Habits: ${memorySnapshot.habits.join(" | ")}`);
      }
      if (memorySnapshot.moodLogs.length > 0) {
        lines.push(`Memory-Mood: ${memorySnapshot.moodLogs.join(" | ")}`);
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

async function maybeTriggerAgentAutomation({
  auth,
  mode,
  prompt,
  reply,
  history,
  runtimeSettings,
  automationScope = "owner",
  attachments = [],
}) {
  if (!auth?.uid) {
    return {attempted: false, triggered: false};
  }

  try {
    const memorySnapshot = await loadUserMemorySnapshot(auth.uid).catch(() => null);
    const automationResult = await triggerWorkflowAutomationWebhook({
      trigger: `agent_${mode}`,
      source: "agent",
      auth,
      automationScope,
      runtimePolicy: runtimeSettings?.bot?.agentCore?.externalPolicy || {},
      data: {
        mode,
        prompt,
        reply,
        history: history.slice(-8),
        automationScope,
        attachments: Array.isArray(attachments) ? attachments : [],
        memory: memorySnapshot || {
          profileSummary: "",
          goals: [],
          habits: [],
          moodLogs: [],
        },
      },
    });

    return {
      attempted: true,
      triggered: true,
      message: nonEmptyString(automationResult.message) || "An externen Workflow gesendet.",
      workflowName: nonEmptyString(automationResult.workflowName) || "",
      requestId: nonEmptyString(automationResult.requestId) || "",
      schemaVersion: nonEmptyString(automationResult.schemaVersion) || "",
      workflowStatus: normalizeAutomationWorkflowStatus(
          automationResult.workflowStatus,
          "completed",
      ),
      results: Array.isArray(automationResult.results) ? automationResult.results : [],
      externalState: "external_completed",
      route: nonEmptyString(automationResult.provider) || "activepieces",
    };
  } catch (error) {
    return {
      attempted: true,
      triggered: false,
      message: error instanceof Error ? error.message : `${error}`,
      requestId: "",
      schemaVersion: "",
      workflowStatus: "failed",
      externalState: "external_failed",
      route: "external",
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
    attachmentSummary: trimTextMax(nonEmptyString(input.attachmentSummary) || "", 4000),
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

function buildGrokChatMessages({systemInstruction, workspaceContext, history, prompt, mode, attachmentSummary = ""}) {
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

  let userContent = nonEmptyString(prompt)?.trim()?.slice(0, 4000) || "";
  const attachmentTail = nonEmptyString(attachmentSummary)?.trim();
  if (attachmentTail) {
    const merged = `${userContent}\n\n${attachmentTail}`.trim();
    userContent = merged.slice(0, 4000);
  }

  messages.push({
    role: "user",
    content: userContent,
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
    attachmentSummary: nonEmptyString(input.attachmentSummary) || "",
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
    attachmentSummary: nonEmptyString(input.attachmentSummary) || "",
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

exports.validateManusApiKey = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "validateManusApiKey");
  assertAuthenticatedUser(
      request.auth,
      "Bitte melde dich an, bevor du einen Manus API Key pruefst.",
  );
  const rawKey = nonEmptyString(request.data?.apiKey) || "";
  if (!rawKey) {
    throw new HttpsError("invalid-argument", "Bitte gib einen Manus API Key an.");
  }

  try {
    await callManusApi({
      apiKey: rawKey,
      endpoint: "task.list",
      method: "GET",
      query: {limit: 1},
      timeoutMs: 8000,
    });
    return {
      valid: true,
      status: "ok",
      message: "Manus-Key ist gueltig und erreichbar.",
    };
  } catch (error) {
    const message = error instanceof Error ? error.message : "Manus-Key konnte nicht verifiziert werden.";
    return {
      valid: false,
      status: "failed",
      message,
    };
  }
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
  const attachmentPayload = normalizeAgentInboundAttachments(input.attachments || []);
  const agentInput = {
    ...input,
    attachments: attachmentPayload.sanitized,
    attachmentSummary: attachmentPayload.summary,
  };
  const runtimeSettings = await loadAiRuntimeSettings();
  const aiLevel = normalizeAiExperienceLevel(agentInput.aiLevel);
  const authorizationProvider = runtimeSettings.agentProvider;
  const authorizationModel = resolveAiAgentModelForProvider(authorizationProvider, runtimeSettings);
  const usage = await authorizeAiUsage({
    auth: request.auth,
    kind: AI_USAGE_KINDS.agent,
    provider: authorizationProvider,
    model: authorizationModel,
    eventType: "agent_generation",
    sourceRoute: "callable.skydownAgent",
    functionName: "skydownAgent",
    featureClass: agentInput.executeAutomation ? AI_FEATURE_CLASSES.workflow : AI_FEATURE_CLASSES.agent,
    resultType: agentInput.executeAutomation ? "workflow" : "text",
    requestWeight: (agentInput.executeAutomation ? 3 : 2) * aiLevelRequestWeightMultiplier(aiLevel),
    aiLevel,
    estimatedCostMicros: agentInput.executeAutomation ? 260_000 : 110_000,
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
  const agentCore = runtimeSettings.bot.agentCore || resolveAiBotAgentCore({});
  const requestedTask = agentInput.mode === "merch" ?
    "commerce_order" :
    (agentInput.mode === "briefing" ? "owner_ops" : "support_recovery");
  const stateByFallback = agentCore.fallbackPolicy || {};
  const blockedState = nonEmptyString(stateByFallback.blockedState) || "blocked";
  const retryableState = nonEmptyString(stateByFallback.retryableState) || "retryable";
  const partialState = nonEmptyString(stateByFallback.partialState) || "partial";
  const allowedTasks = Array.isArray(agentCore.allowedTasks) ? agentCore.allowedTasks : [];
  const blockedTasks = Array.isArray(agentCore.blockedTasks) ? agentCore.blockedTasks : [];
  const isTaskAllowed = allowedTasks.includes(requestedTask) && !blockedTasks.includes(requestedTask);
  const requiresConfirmation = (
    (requestedTask === "commerce_order" && agentCore.confirmationPolicy?.requireConfirmationForCommerce === true) ||
    (requestedTask === "owner_ops" && agentCore.confirmationPolicy?.requireConfirmationForOwnerOps === true)
  );
  const ownerDiagnosticActive = agentCore.ownerMode === "diagnostic" && (
    agentCore.diagnosticsMode === "verbose" ||
    (agentCore.diagnosticsMode === "owner_only" && resolveRoleFromAuthClaims(request.auth) === USER_ROLES.owner)
  );
  const allowedExternalTaskTypes = agentCore.externalPolicy?.allowedExternalTaskTypes || [];
  const selectedAutomationScope = agentInput.automationScope === "personal" ? "personal" : "owner";
  const canUseActivepieces = agentCore.externalPolicy?.activepiecesEnabled !== false &&
    allowedExternalTaskTypes.includes(requestedTask);
  const canUseN8n = selectedAutomationScope === "personal" &&
    agentCore.externalPolicy?.n8nEnabled !== false &&
    allowedExternalTaskTypes.includes(requestedTask);
  const canUseAnyExternalWorkflow = canUseActivepieces || canUseN8n;
  const canUseManus = agentCore.externalPolicy?.manusEnabled !== false &&
    allowedExternalTaskTypes.includes(requestedTask);
  const manusKeyAvailable = nonEmptyString(agentInput.manusApiKeyOverride) || nonEmptyString(manusApiKey.value());

  const buildAgentDecision = ({
    state = "completed",
    route = "internal",
    selectedExternal = "",
    blocked = false,
    blockReason = "",
    retryable = false,
    retryReason = "",
    confirmationRequired = false,
    confirmationReason = "",
    summary = "",
    policy = "agent_core_v1",
  } = {}) => ({
    state,
    requestedTask,
    route,
    selectedExternal,
    allowedTasks,
    blockedTasks,
    allowedTools: agentCore.toolPolicy?.allowedTools || [],
    policy,
    diagnosticsMode: agentCore.diagnosticsMode || "owner_only",
    ownerMode: agentCore.ownerMode || "standard",
    killSwitch: runtimeSettings.bot.killSwitchEnabled || agentCore.killSwitch === true,
    blocked,
    blockReason,
    retryable,
    retryReason,
    confirmationRequired,
    confirmationReason,
    summary: nonEmptyString(summary) || "Agent-Entscheidung ausgefuehrt.",
    ownerDiagnosticActive,
  });

  if ((runtimeSettings.bot.killSwitchEnabled || agentCore.killSwitch === true) &&
    agentCore.safetyPolicy?.blockWhenKillSwitchEnabled !== false) {
    return {
      reply: "Agent ist aktuell per Kill Switch pausiert.",
      mode: agentInput.mode,
      automationTriggered: false,
      automationAttempted: false,
      automationMessage: "",
      workflowName: "",
      agentProvider: runtimeSettings.agentProvider,
      providerFallbackUsed: false,
      providerNotice: "",
      historyRetentionDays: usage.historyRetentionDays,
      agentRunId: "",
      resultType: "text",
      results: [{type: "text", text: "Agent ist aktuell per Kill Switch pausiert."}],
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
      agentDecision: buildAgentDecision({
        state: blockedState,
        blocked: true,
        blockReason: "kill_switch_enabled",
        summary: "Kill Switch blockiert Agent-Anfragen.",
      }),
    };
  }

  if (!isTaskAllowed && agentCore.safetyPolicy?.blockUnknownTasks !== false) {
    return {
      reply: "Diese Agent-Aufgabe ist aktuell nicht freigeschaltet.",
      mode: agentInput.mode,
      automationTriggered: false,
      automationAttempted: false,
      automationMessage: "",
      workflowName: "",
      agentProvider: runtimeSettings.agentProvider,
      providerFallbackUsed: false,
      providerNotice: "",
      historyRetentionDays: usage.historyRetentionDays,
      agentRunId: "",
      resultType: "text",
      results: [{type: "text", text: "Diese Agent-Aufgabe ist aktuell nicht freigeschaltet."}],
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
      agentDecision: buildAgentDecision({
        state: blockedState,
        blocked: true,
        blockReason: "task_not_allowed",
        summary: "Task ist nicht in allowedTasks oder in blockedTasks.",
      }),
    };
  }

  if (agentInput.executeAutomation && !agentCore.toolPolicy?.allowWorkflowAutomation) {
    return {
      reply: "Workflow-Automation ist fuer den Agent aktuell deaktiviert.",
      mode: agentInput.mode,
      automationTriggered: false,
      automationAttempted: false,
      automationMessage: "",
      workflowName: "",
      agentProvider: runtimeSettings.agentProvider,
      providerFallbackUsed: false,
      providerNotice: "",
      historyRetentionDays: usage.historyRetentionDays,
      agentRunId: "",
      resultType: "text",
      results: [{type: "text", text: "Workflow-Automation ist fuer den Agent aktuell deaktiviert."}],
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
      agentDecision: buildAgentDecision({
        state: blockedState,
        blocked: true,
        blockReason: "tool_policy_block_workflow_automation",
        summary: "Tool Policy blockiert Workflow-Automation.",
      }),
    };
  }

  if (agentInput.executeAutomation && !canUseAnyExternalWorkflow) {
    return {
      reply: "Externe Workflow-Ausfuehrung ist fuer diesen Task aktuell nicht erlaubt.",
      mode: agentInput.mode,
      automationTriggered: false,
      automationAttempted: false,
      automationMessage: "",
      workflowName: "",
      agentProvider: runtimeSettings.agentProvider,
      providerFallbackUsed: false,
      providerNotice: "",
      historyRetentionDays: usage.historyRetentionDays,
      agentRunId: "",
      resultType: "text",
      results: [{type: "text", text: "Externe Workflow-Ausfuehrung ist fuer diesen Task aktuell nicht erlaubt."}],
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
      agentDecision: buildAgentDecision({
        state: blockedState,
        route: "external",
        selectedExternal: "activepieces",
        blocked: true,
        blockReason: "external_task_type_not_allowed",
        summary: "External Task-Type ist in externalPolicy nicht erlaubt.",
      }),
    };
  }

  if (agentInput.executeAutomation && requiresConfirmation && agentInput.confirmedByUser !== true) {
    return {
      reply: "Bitte bestaetige diese Aktion explizit, bevor der Agent Automation ausfuehrt.",
      mode: agentInput.mode,
      automationTriggered: false,
      automationAttempted: false,
      automationMessage: "",
      workflowName: "",
      agentProvider: runtimeSettings.agentProvider,
      providerFallbackUsed: false,
      providerNotice: "",
      historyRetentionDays: usage.historyRetentionDays,
      agentRunId: "",
      resultType: "text",
      results: [{type: "text", text: "Bitte bestaetige diese Aktion explizit, bevor der Agent Automation ausfuehrt."}],
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
      agentDecision: buildAgentDecision({
        state: "awaiting_confirmation",
        confirmationRequired: true,
        confirmationReason: "automation_confirmation_required",
        summary: "Automation wartet auf User-Confirmation.",
      }),
    };
  }
  const memoryWrite = await maybePersistUserMemorySignal({
    auth: request.auth,
    prompt: agentInput.prompt,
    mode: agentInput.mode,
  });
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
    if (!canUseManus) {
      providerFallbackUsed = true;
      providerNotice = "Manus ist fuer diesen Task laut externalPolicy nicht freigeschaltet. Interner Fallback aktiv.";
    } else if (!manusKeyAvailable) {
      if (runtimeSettings.fallbackAgentProvider !== AI_AGENT_PROVIDERS.gemini) {
        return {
          reply: "Manus benoetigt einen API Key. Bitte verbinde deinen Manus-Key oder nutze den internen Agent.",
          mode: agentInput.mode,
          automationTriggered: false,
          automationAttempted: false,
          automationMessage: "",
          workflowName: "",
          agentProvider: AI_AGENT_PROVIDERS.manus,
          providerFallbackUsed: false,
          providerNotice: "",
          historyRetentionDays: usage.historyRetentionDays,
          agentRunId: "",
          resultType: "text",
          results: [{type: "text", text: "Manus benoetigt einen API Key. Bitte verbinde deinen Manus-Key oder nutze den internen Agent."}],
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
          agentDecision: buildAgentDecision({
            state: "awaiting_external_auth",
            route: "manus",
            selectedExternal: "manus",
            confirmationRequired: true,
            confirmationReason: "manus_key_missing",
            summary: "Manus wartet auf BYOS-API-Key.",
          }),
        };
      }
      providerFallbackUsed = true;
      providerNotice = "Manus-Key fehlt. Interner Fallback auf Gemini aktiv.";
    }
  }

  if (runtimeSettings.agentProvider === AI_AGENT_PROVIDERS.manus && !providerFallbackUsed) {
    try {
      const manusResult = await runManusAgent({
        input: agentInput,
        runtimeSettings,
        promptSettings: effectivePromptSettings,
        workspaceContext,
        manusApiKeyOverride: agentInput.manusApiKeyOverride,
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
          input: agentInput,
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
      prompt: agentInput.prompt,
      history: agentInput.history,
      mode: agentInput.mode,
      executeAutomation: agentInput.executeAutomation,
      systemInstruction: effectiveAgentSystemInstruction,
      workspaceContext,
      attachmentSummary: nonEmptyString(agentInput.attachmentSummary) || "",
    });
    agentProvider = AI_AGENT_PROVIDERS.gemini;
  }

  const automation = agentInput.executeAutomation ?
    await maybeTriggerAgentAutomation({
      auth: request.auth,
      mode: agentInput.mode,
      prompt: agentInput.prompt,
      reply,
      history: agentInput.history,
      runtimeSettings,
      automationScope: selectedAutomationScope,
      attachments: agentInput.attachments || [],
    }) :
    {attempted: false, triggered: false};
  const resolvedWorkflowStatus = automation.attempted === true ?
    normalizeAutomationWorkflowStatus(
        automation.workflowStatus,
        automation.triggered === true ? "completed" : "failed",
    ) :
    "completed";

  const agentRunId = await persistAgentRunSummary({
    uid: request.auth.uid,
    mode: agentInput.mode,
    agentProvider,
    providerFallbackUsed,
    automation,
    promptText: agentInput.prompt,
    replyText: reply,
  });

  return {
    reply,
    mode: agentInput.mode,
    automationTriggered: automation.triggered === true,
    automationAttempted: automation.attempted === true,
    automationMessage: nonEmptyString(automation.message) || "",
    workflowName: nonEmptyString(automation.workflowName) || "",
    automationSchemaVersion: nonEmptyString(automation.schemaVersion) || "",
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
        workflowName: nonEmptyString(automation.workflowName) || "External Workflow",
        status: resolvedWorkflowStatus,
        summary: nonEmptyString(automation.message) || (
          resolvedWorkflowStatus === "queued" ?
            "Workflow wurde in die Warteschlange gestellt." :
            (resolvedWorkflowStatus === "running" ?
              "Workflow wird gerade ausgefuehrt." :
              (resolvedWorkflowStatus === "completed" ?
                "Workflow wurde gestartet." :
                "Workflow konnte nicht gestartet werden."))
        ),
        runId: agentRunId || "",
      }] : []),
      ...(
        automation.triggered === true && Array.isArray(automation.results) ?
          automation.results :
          []
      ),
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
    memoryUpdated: memoryWrite.stored === true,
    agentDecision: buildAgentDecision({
      state: agentInput.executeAutomation ?
        (resolvedWorkflowStatus === "queued" ?
          "webhook_pending" :
          (resolvedWorkflowStatus === "running" ?
            "external_running" :
            (automation.triggered === true ? "external_completed" : "external_failed"))) :
        (providerFallbackUsed ? "fallback_internal" : "completed"),
      route: agentInput.executeAutomation ? (automation.route || "external") : (providerFallbackUsed ? "hybrid" : "internal"),
      selectedExternal: agentInput.executeAutomation ? (automation.route || "activepieces") : "",
      retryable: providerFallbackUsed,
      retryReason: providerFallbackUsed ? "provider_fallback_used" : "",
      summary: agentInput.executeAutomation ?
        (resolvedWorkflowStatus === "queued" ?
          "Externer Workflow wurde in die Warteschlange gestellt." :
          (resolvedWorkflowStatus === "running" ?
            "Externer Workflow laeuft." :
            (automation.triggered === true ?
              "Externer Workflow erfolgreich abgeschlossen." :
              "Externer Workflow fehlgeschlagen."))) :
        (providerFallbackUsed ?
          "Antwort mit Fallback erstellt (fallback_internal)." :
          "Agent-Run erfolgreich abgeschlossen."),
    }),
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
    deletedAiUsageEvents: deletionSummary.deletedAiUsageEvents,
    deletedAiMembershipEvents: deletionSummary.deletedAiMembershipEvents,
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

exports.createReminder = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "createReminder");
  const targetUid = await assertUidAccess(request.auth, request.data?.uid, {allowStaffOverride: true});
  const title = nonEmptyString(request.data?.title);
  if (!title) {
    throw new HttpsError("invalid-argument", "title fehlt.");
  }
  const body = nonEmptyString(request.data?.body) || "";
  const timezone = nonEmptyString(request.data?.timezone) || "UTC";
  const scheduledAt = parseReminderScheduledAtTimestamp(request.data?.scheduledAt);
  const source = normalizeReminderSource(request.data?.source, REMINDER_SOURCES.manual);
  const now = admin.firestore.FieldValue.serverTimestamp();
  const reminderRef = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("reminders")
      .doc();

  await reminderRef.set({
    title: trimTextMax(title, 180),
    body: trimTextMax(body, 2000),
    scheduledAt,
    timezone: trimTextMax(timezone, 80),
    status: REMINDER_STATUSES.scheduled,
    source,
    createdAt: now,
    updatedAt: now,
  });

  return {
    reminderId: reminderRef.id,
    uid: targetUid,
    ...buildSkyosWorkflowResponse({
      message: "Erinnerung wurde erstellt.",
      workflowStatus: "completed",
      schemaVersion: "v1",
      text: formatReminderResultLine({
        title,
        scheduledAt,
        timezone,
      }),
    }),
  };
});

exports.listReminders = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "listReminders");
  const targetUid = await assertUidAccess(request.auth, request.data?.uid, {allowStaffOverride: true});
  const limit = clampIntegerSetting(request.data?.limit, 25, 1, 100);
  const statusFilter = nonEmptyString(request.data?.status);
  let query = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("reminders");
  if (statusFilter) {
    query = query.where("status", "==", normalizeReminderStatus(statusFilter));
  }
  const snapshot = await query
      .orderBy("scheduledAt", "asc")
      .limit(limit)
      .get();
  const reminders = snapshot.docs.map((doc) => {
    const data = doc.data() || {};
    return {
      id: doc.id,
      title: nonEmptyString(data.title) || "",
      body: nonEmptyString(data.body) || "",
      timezone: nonEmptyString(data.timezone) || "UTC",
      status: normalizeReminderStatus(data.status),
      source: normalizeReminderSource(data.source),
      scheduledAt: data.scheduledAt instanceof admin.firestore.Timestamp ?
        data.scheduledAt.toDate().toISOString() :
        "",
      createdAt: data.createdAt instanceof admin.firestore.Timestamp ?
        data.createdAt.toDate().toISOString() :
        "",
      updatedAt: data.updatedAt instanceof admin.firestore.Timestamp ?
        data.updatedAt.toDate().toISOString() :
        "",
    };
  });
  return {
    reminders,
    ...buildSkyosWorkflowResponse({
      message: reminders.length > 0 ?
        `${reminders.length} Erinnerung(en) geladen.` :
        "Keine Erinnerungen gefunden.",
      workflowStatus: "completed",
      schemaVersion: "v1",
      text: reminders.length > 0 ?
        `${reminders.length} Erinnerung(en) geladen.` :
        "Keine Erinnerungen gefunden.",
    }),
  };
});

exports.completeReminder = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "completeReminder");
  const targetUid = await assertUidAccess(request.auth, request.data?.uid, {allowStaffOverride: true});
  const reminderId = nonEmptyString(request.data?.reminderId);
  if (!reminderId) {
    throw new HttpsError("invalid-argument", "reminderId fehlt.");
  }
  const reminderRef = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("reminders")
      .doc(reminderId);
  const snapshot = await reminderRef.get();
  if (!snapshot.exists) {
    throw new HttpsError("not-found", "Erinnerung wurde nicht gefunden.");
  }
  await reminderRef.set({
    status: REMINDER_STATUSES.completed,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  return {
    reminderId,
    ...buildSkyosWorkflowResponse({
      message: "Erinnerung wurde abgeschlossen.",
      workflowStatus: "completed",
      schemaVersion: "v1",
      text: "Erinnerung wurde abgeschlossen.",
    }),
  };
});

exports.updateMemoryProfile = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "updateMemoryProfile");
  const targetUid = await assertUidAccess(request.auth, request.data?.uid, {allowStaffOverride: true});
  const profileData = request.data?.data;
  if (!profileData || typeof profileData !== "object" || Array.isArray(profileData)) {
    throw new HttpsError("invalid-argument", "data fuer memoryProfile fehlt.");
  }
  const profileRef = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("memoryProfile")
      .doc("main");
  await profileRef.set({
    ...profileData,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  return buildSkyosWorkflowResponse({
    message: "Memory-Profil wurde aktualisiert.",
    workflowStatus: "completed",
    schemaVersion: "v1",
    text: "Memory-Profil wurde aktualisiert.",
  });
});

exports.upsertPushToken = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "upsertPushToken");
  const targetUid = await assertUidAccess(request.auth, request.data?.uid, {allowStaffOverride: true});
  const token = nonEmptyString(request.data?.token);
  if (!token) {
    throw new HttpsError("invalid-argument", "token fehlt.");
  }
  const platformRaw = nonEmptyString(request.data?.platform)?.toLowerCase() || "";
  const platform = ["ios", "android", "web"].includes(platformRaw) ? platformRaw : "";
  if (!platform) {
    throw new HttpsError("invalid-argument", "platform ist ungueltig.");
  }
  const appVersion = nonEmptyString(request.data?.appVersion) || "";
  const tokenId = nonEmptyString(request.data?.tokenId) ||
    crypto.createHash("sha256").update(`${platform}:${token}`, "utf8").digest("hex");
  const pushTokenRef = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("pushTokens")
      .doc(tokenId);
  await pushTokenRef.set({
    token: trimTextMax(token, 4096),
    platform,
    appVersion: trimTextMax(appVersion, 64),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});
  return {
    tokenId,
    uid: targetUid,
    ...buildSkyosWorkflowResponse({
      message: "Push-Token wurde gespeichert.",
      workflowStatus: "completed",
      schemaVersion: "v1",
      text: `Push-Token (${platform}) wurde gespeichert.`,
    }),
  };
});

exports.handleAutomationIntent = onCall({
  region: "us-central1",
  timeoutSeconds: 30,
}, async (request) => {
  await assertCallableSecurity(request, "handleAutomationIntent");
  const intent = nonEmptyString(request.data?.intent)?.toLowerCase() || "";
  if (!intent) {
    throw new HttpsError("invalid-argument", "Unbekannter intent.");
  }
  const targetUid = await assertUidAccess(
      request.auth,
      request.data?.uid || request.data?.data?.uid,
      {allowStaffOverride: true},
  );
  const payload = request.data?.data && typeof request.data.data === "object" && !Array.isArray(request.data.data) ?
    request.data.data :
    {};
  const remindersCollection = admin.firestore()
      .collection("users")
      .doc(targetUid)
      .collection("reminders");

  if (intent === "create_reminder") {
    const title = nonEmptyString(payload.title);
    if (!title) {
      throw new HttpsError("invalid-argument", "title fehlt.");
    }
    const body = nonEmptyString(payload.body) || "";
    const timezone = nonEmptyString(payload.timezone) || "UTC";
    const scheduledAt = parseReminderScheduledAtTimestamp(payload.scheduledAt);
    const now = admin.firestore.FieldValue.serverTimestamp();
    const reminderRef = remindersCollection.doc();
    await reminderRef.set({
      title: trimTextMax(title, 180),
      body: trimTextMax(body, 2000),
      scheduledAt,
      timezone: trimTextMax(timezone, 80),
      status: REMINDER_STATUSES.scheduled,
      source: REMINDER_SOURCES.workflow,
      createdAt: now,
      updatedAt: now,
    });
    return {
      reminderId: reminderRef.id,
      uid: targetUid,
      ...buildSkyosWorkflowResponse({
        message: "Erinnerung wurde erstellt.",
        workflowStatus: "completed",
        schemaVersion: "v1",
        text: formatReminderResultLine({
          title,
          scheduledAt,
          timezone,
        }),
      }),
    };
  }

  if (intent === "list_reminders") {
    const limit = clampIntegerSetting(payload.limit, 25, 1, 100);
    const statusFilter = nonEmptyString(payload.status);
    let query = remindersCollection;
    if (statusFilter) {
      query = query.where("status", "==", normalizeReminderStatus(statusFilter));
    }
    const snapshot = await query
        .orderBy("scheduledAt", "asc")
        .limit(limit)
        .get();
    const reminders = snapshot.docs.map((doc) => {
      const reminder = doc.data() || {};
      return {
        id: doc.id,
        title: nonEmptyString(reminder.title) || "",
        body: nonEmptyString(reminder.body) || "",
        timezone: nonEmptyString(reminder.timezone) || "UTC",
        status: normalizeReminderStatus(reminder.status),
        source: normalizeReminderSource(reminder.source),
        scheduledAt: reminder.scheduledAt instanceof admin.firestore.Timestamp ?
          reminder.scheduledAt.toDate().toISOString() :
          "",
      };
    });
    return {
      uid: targetUid,
      reminders,
      ...buildSkyosWorkflowResponse({
        message: reminders.length > 0 ?
          `${reminders.length} Erinnerung(en) geladen.` :
          "Keine Erinnerungen gefunden.",
        workflowStatus: "completed",
        schemaVersion: "v1",
        text: reminders.length > 0 ?
          `${reminders.length} Erinnerung(en) geladen.` :
          "Keine Erinnerungen gefunden.",
      }),
    };
  }

  if (intent === "complete_reminder") {
    const reminderId = nonEmptyString(payload.reminderId);
    if (!reminderId) {
      throw new HttpsError("invalid-argument", "reminderId fehlt.");
    }
    const reminderRef = remindersCollection.doc(reminderId);
    const snapshot = await reminderRef.get();
    if (!snapshot.exists) {
      throw new HttpsError("not-found", "Erinnerung wurde nicht gefunden.");
    }
    await reminderRef.set({
      status: REMINDER_STATUSES.completed,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    return {
      uid: targetUid,
      reminderId,
      ...buildSkyosWorkflowResponse({
        message: "Erinnerung wurde abgeschlossen.",
        workflowStatus: "completed",
        schemaVersion: "v1",
        text: "Erinnerung wurde abgeschlossen.",
      }),
    };
  }

  if (intent === "update_memory_profile") {
    if (typeof payload !== "object" || Array.isArray(payload) || Object.keys(payload).length === 0) {
      throw new HttpsError("invalid-argument", "data fuer memoryProfile fehlt.");
    }
    const profileRef = admin.firestore()
        .collection("users")
        .doc(targetUid)
        .collection("memoryProfile")
        .doc("main");
    await profileRef.set({
      ...payload,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    }, {merge: true});
    return {
      uid: targetUid,
      ...buildSkyosWorkflowResponse({
        message: "Memory-Profil wurde aktualisiert.",
        workflowStatus: "completed",
        schemaVersion: "v1",
        text: "Memory-Profil wurde aktualisiert.",
      }),
    };
  }

  throw new HttpsError("invalid-argument", "Unbekannter intent.");
});

async function loadUserPushTokenEntries(uid) {
  if (!uid) {
    return [];
  }
  const snapshot = await admin.firestore()
      .collection("users")
      .doc(uid)
      .collection("pushTokens")
      .limit(500)
      .get();
  return snapshot.docs
      .map((doc) => {
        const data = doc.data() || {};
        const token = nonEmptyString(data.token);
        if (!token) {
          return null;
        }
        return {
          tokenId: doc.id,
          token: token.trim(),
          platform: nonEmptyString(data.platform) || "",
        };
      })
      .filter(Boolean);
}

function isInvalidPushTokenError(errorCode = "") {
  return [
    "messaging/invalid-registration-token",
    "messaging/registration-token-not-registered",
  ].includes(errorCode);
}

async function sendPushToUser({
  uid,
  title,
  body,
  reminderId,
}) {
  const tokenEntries = await loadUserPushTokenEntries(uid);
  if (tokenEntries.length === 0) {
    return {
      sentCount: 0,
      failedCount: 0,
      invalidTokenIds: [],
      noTokens: true,
    };
  }

  const tokens = tokenEntries.map((entry) => entry.token).slice(0, 500);
  const fallbackTitle = "Erinnerung faellig";
  const fallbackBody = "Deine Erinnerung ist jetzt faellig. Oeffne SkyOS fuer Details.";
  const response = await admin.messaging().sendEachForMulticast({
    tokens,
    notification: {
      title: trimTextMax(nonEmptyString(title) || fallbackTitle, 120),
      body: trimTextMax(nonEmptyString(body) || fallbackBody, 240),
    },
    data: {
      type: "reminder",
      reminderId: nonEmptyString(reminderId) || "",
      uid: nonEmptyString(uid) || "",
    },
    android: {
      priority: "high",
    },
    apns: {
      headers: {
        "apns-priority": "10",
      },
      payload: {
        aps: {
          sound: "default",
        },
      },
    },
  });

  const invalidTokenIds = [];
  response.responses.forEach((sendResult, index) => {
    if (!sendResult.success && isInvalidPushTokenError(sendResult.error?.code || "")) {
      const tokenEntry = tokenEntries[index];
      if (tokenEntry?.tokenId) {
        invalidTokenIds.push(tokenEntry.tokenId);
      }
    }
  });

  if (invalidTokenIds.length > 0) {
    const batch = admin.firestore().batch();
    for (const tokenId of invalidTokenIds) {
      const tokenRef = admin.firestore()
          .collection("users")
          .doc(uid)
          .collection("pushTokens")
          .doc(tokenId);
      batch.delete(tokenRef);
    }
    await batch.commit();
  }

  return {
    sentCount: response.successCount || 0,
    failedCount: response.failureCount || 0,
    invalidTokenIds,
    noTokens: false,
  };
}

exports.processDueReminders = onSchedule({
  region: "us-central1",
  schedule: "every 5 minutes",
  timeZone: "Etc/UTC",
  timeoutSeconds: 120,
}, async () => {
  const now = admin.firestore.Timestamp.now();
  const dueSnapshot = await admin.firestore()
      .collectionGroup("reminders")
      .where("status", "==", REMINDER_STATUSES.scheduled)
      .where("scheduledAt", "<=", now)
      .limit(200)
      .get();

  if (dueSnapshot.empty) {
    logger.info("No due reminders found.");
    return;
  }

  for (const doc of dueSnapshot.docs) {
    const reminder = doc.data() || {};
    const uid = doc.ref.parent.parent?.id || "";
    const title = nonEmptyString(reminder.title) || "Erinnerung faellig";
    const body = nonEmptyString(reminder.body) || `Deine Erinnerung "${title}" ist jetzt faellig.`;
    if (!uid) {
      await doc.ref.set({
        status: REMINDER_STATUSES.completed,
        notificationStatus: "failed_missing_uid",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});
      continue;
    }

    try {
      const pushResult = await sendPushToUser({
        uid,
        title,
        body,
        reminderId: doc.id,
      });
      const notificationStatus = pushResult.noTokens ?
        "failed_no_tokens" :
        (pushResult.sentCount > 0 ? "sent" : "failed_delivery");
      await doc.ref.set({
        status: REMINDER_STATUSES.completed,
        notificationStatus,
        notificationSentCount: pushResult.sentCount,
        notificationFailedCount: pushResult.failedCount,
        invalidPushTokenCount: pushResult.invalidTokenIds.length,
        notifiedAt: admin.firestore.FieldValue.serverTimestamp(),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});
      logger.info("Processed due reminder notification.", {
        uid,
        reminderId: doc.id,
        notificationStatus,
        sentCount: pushResult.sentCount,
        failedCount: pushResult.failedCount,
        invalidPushTokenCount: pushResult.invalidTokenIds.length,
      });
    } catch (error) {
      await doc.ref.set({
        status: REMINDER_STATUSES.completed,
        notificationStatus: "failed_exception",
        notificationError: trimTextMax(error instanceof Error ? error.message : `${error}`, 500),
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      }, {merge: true});
      logger.error("Failed to send reminder push notification.", {
        uid,
        reminderId: doc.id,
        error: error instanceof Error ? error.message : `${error}`,
      });
    }
  }
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

exports.enforceRegistrationLockdown = functionsV1
    .runWith({maxInstances: 10})
    .auth.user()
    .onCreate(async (user) => {
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
    });
    await ensureAuthUserBootstrapDocument(user, {forcedRole: USER_ROLES.owner});
    return;
  }

  logger.error("Registration created during lockdown. User will be deleted.", {
    uid: user.uid,
  });
  await admin.auth().deleteUser(user.uid);
});

exports.processConfirmedMerchOrders = onDocumentWritten({
  document: "orders/{orderId}",
  region: "us-central1",
  secrets: [shopifyAdminAccessToken],
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
  });
});
