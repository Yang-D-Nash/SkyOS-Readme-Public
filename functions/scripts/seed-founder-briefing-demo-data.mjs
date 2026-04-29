#!/usr/bin/env node
/**
 * Schreibt Demo-Daten fuer Founder Briefing in Firestore (reduziert "FEHLT" im Text).
 *
 * Collections / Felder (siehe buildFounderBriefingResponseData in index.js):
 * - founder_daily_kpis/{date}     — Doc-ID = ISO-Datum yyyy-MM-dd
 * - founder_risks                 — Docs mit Feld date == dasselbe Datum
 * - founder_highlights            — Docs mit Feld date == dasselbe Datum
 * - appConfig/founderBriefing     — optional: { musicArtists: ["A","B",…] } fuer iTunes-Release-Zeilen
 * - merchandise, system/runtimeConfig, orders — fuer angereichertes Briefing (automatisch gelesen)
 *
 * Ausfuehren (Production-Projekt, z. B. skydown-a6add):
 *   cd functions
 *
 *   Option A – Service-Account-JSON (Firebase Console → Projekteinstellungen → Dienstkonten
 *   → "Neuen privaten Schlüssel" – die Datei heisst z. B. skydown-a6add-firebase-adminsdk-….json
 *   NICHT den Platzhalter-Namen aus Anleitungen 1:1 nutzen, sondern die echte Datei in Downloads):
 *   export GOOGLE_APPLICATION_CREDENTIALS="$HOME/Downloads/<dein-echter-dateiname>.json"
 *
 *   Option B – ohne Datei, mit gcloud (nach `gcloud auth application-default login`):
 *   unset GOOGLE_APPLICATION_CREDENTIALS
 *
 *   Dann:
 *   node scripts/seed-founder-briefing-demo-data.mjs --project=skydown-a6add --date=2026-04-29
 *
 * Ohne --date wird das heutige Datum (UTC) genutzt.
 */

import { initializeApp, applicationDefault } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

function parseArgs(argv) {
  const out = { projectId: null, date: null, dryRun: false };
  for (const a of argv.slice(2)) {
    if (a === "--dry-run") out.dryRun = true;
    else if (a.startsWith("--project=")) out.projectId = a.slice("--project=".length);
    else if (a.startsWith("--date=")) out.date = a.slice("--date=".length);
  }
  if (!out.projectId) {
    out.projectId = process.env.GCLOUD_PROJECT || process.env.GOOGLE_CLOUD_PROJECT || null;
  }
  if (!out.date) {
    out.date = new Date().toISOString().slice(0, 10);
  }
  return out;
}

function isoDateOrThrow(s) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) {
    throw new Error(`Ungueltiges Datum (erwarte yyyy-MM-dd): ${s}`);
  }
  const ms = Date.parse(`${s}T00:00:00.000Z`);
  if (!Number.isFinite(ms)) throw new Error(`Datum nicht parsebar: ${s}`);
  if (new Date(ms).toISOString().slice(0, 10) !== s) {
    throw new Error(`Datum normalisiert sich nicht auf UTC-Tag: ${s}`);
  }
  return s;
}

const { projectId, date: dateArg, dryRun } = parseArgs(process.argv);
if (!projectId) {
  console.error(
    "Fehlt --project=... oder Umgebungsvariable GCLOUD_PROJECT / GOOGLE_CLOUD_PROJECT.",
  );
  process.exit(1);
}
const date = isoDateOrThrow(dateArg);

const kpiDoc = {
  /** Firebase-Kosten heute (EUR, Zahl) */
  firebase_cost_today: 4.52,
  /** Monat bis heute (EUR) */
  firebase_cost_mtd: 128.9,
  /** Trend vs. Vortag o. a., in Prozentpunkten (kann negativ sein) */
  firebase_cost_trend_pct: -3.2,
  active_users_24h: 142,
  new_users_24h: 8,
  retention_d1_pct: 41.5,
  revenue_today: 0,
  revenue_mtd: 0,
  updatedAt: new Date().toISOString(),
  source: "seed-founder-briefing-demo-data.mjs",
};

const riskDocs = [
  {
    date,
    title: "Onboarding Drop nach Schritt 2",
    severity: "high",
    impact: "Ca. 18% weniger Abschluesse in der Testgruppe.",
    next_step: "Copy in Schritt 2 verkuerzen und A/B-Test starten.",
  },
  {
    date,
    title: "Firestore Reads leicht ueber Budget",
    severity: "medium",
    impact: "Projiziert +12% Kosten wenn unveraendert.",
    next_step: "Liste der Top-Queries reviewen, Pagination pruefen.",
  },
];

const highlightDocs = [
  {
    date,
    title: "Erste zahlende Pilot-Nutzer",
    impact: "Validiert Pricing-Hypothese fuer Q2.",
  },
  {
    date,
    title: "Crash-Rate unter 0,2%",
    impact: "Stabiler Release-Kanal.",
  },
];

async function main() {
  if (dryRun) {
    console.log(JSON.stringify({ projectId, date, kpiDoc, riskDocs, highlightDocs }, null, 2));
    return;
  }

  initializeApp({
    projectId,
    credential: applicationDefault(),
  });
  const db = getFirestore();

  const batch = db.batch();
  const kpiRef = db.collection("founder_daily_kpis").doc(date);
  batch.set(kpiRef, kpiDoc, { merge: true });

  riskDocs.forEach((r, i) => {
    const ref = db.collection("founder_risks").doc(`seed-${date}-risk-${i}`);
    batch.set(ref, r, { merge: true });
  });
  highlightDocs.forEach((h, i) => {
    const ref = db.collection("founder_highlights").doc(`seed-${date}-hl-${i}`);
    batch.set(ref, h, { merge: true });
  });

  await batch.commit();
  console.log(
    `OK: founder_daily_kpis/${date} (merge), ${riskDocs.length} founder_risks, ${highlightDocs.length} founder_highlights — Projekt ${projectId}`,
  );
}

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
