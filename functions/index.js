"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {enableFirebaseTelemetry} = require("@genkit-ai/firebase");
const {onCallGenkit, HttpsError} = require("firebase-functions/https");
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

const agentTurnSchema = z.object({
  role: z.enum(["user", "assistant"]),
  text: z.string().trim().min(1).max(4000),
});

const agentRequestSchema = z.object({
  prompt: z.string().trim().min(1).max(4000),
  history: z.array(agentTurnSchema).max(24).default([]),
});

const systemPrompt = `
Du bist Skydown Agent, der umsetzungsorientierte Assistent fuer die Skydown App.
Antworte auf Deutsch, klar, modern und konkret.
Du hilfst vor allem bei Release-Planung, Briefings, Kampagnenideen, To-dos, Freigaben und naechsten Schritten.
Arbeite pragmatisch statt generisch.
Wenn sinnvoll, strukturiere die Antwort in kurze Abschnitte oder kompakte Listen.
Wenn wichtige Infos fehlen, frage gezielt und kurz nach.
Bleib markentauglich und creator-nah.
`.trim();

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
  `.trim();

  const {stream, response} = ai.generateStream({
    system: systemPrompt,
    prompt,
    config: {
      temperature: 0.8,
      maxOutputTokens: 768,
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
}, skydownAgentFlow);

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
