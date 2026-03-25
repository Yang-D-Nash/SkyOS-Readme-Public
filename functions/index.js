"use strict";

const admin = require("firebase-admin");
const {enableFirebaseTelemetry} = require("@genkit-ai/firebase");
const {onCallGenkit, HttpsError} = require("firebase-functions/https");
const {genkit, z} = require("genkit");
const {vertexAI} = require("@genkit-ai/google-genai");

admin.initializeApp();
void enableFirebaseTelemetry().catch((error) => {
  console.error("Firebase telemetry could not be enabled.", error);
});

const ai = genkit({
  plugins: [vertexAI({location: "us-central1"})],
  model: vertexAI.model("gemini-2.5-flash-lite"),
});

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
