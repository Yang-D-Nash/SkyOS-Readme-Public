/**
 * Befuellt founder_daily_kpis/{date} mit Live-Metriken aus Firestore/Auth.
 *
 * Kostenfelder (firebase_cost_*):
 *   Quelle ist systemMetrics mit echten/reconciled actualCostMicros.
 *   Geschaetzte totalEstimatedCostMicros werden bewusst NICHT in Founder-KPIs geschrieben.
 *   Fuer reine GCP-Projektkosten: systemMetrics/projectBilling_{YYYY-MM} oder
 *   adminConfig/projectBillingSnapshot (BigQuery-Billing-Export oder manuelle Pflege).
 *
 * Nutzerfelder: Firebase Auth (lastSignIn / creation), bis MAX_AUTH_USERS.
 *
 * Umsatz / D1-Retention: nicht automatisiert; koennen spaeter (Orders, Analytics) ergaenzt werden.
 */

"use strict";

const admin = require("firebase-admin");
const {logger} = require("firebase-functions");

const AI_USAGE_METRICS_COLLECTION = "systemMetrics";
const AI_USAGE_METRICS_DOCUMENT_PREFIX = "aiUsageDaily_";
const AI_USAGE_MONTHLY_METRICS_DOCUMENT_PREFIX = "aiUsageMonthly_";
const PROJECT_BILLING_DOCUMENT_PREFIX = "projectBilling_";
const ADMIN_CONFIG_COLLECTION = "adminConfig";
const PROJECT_BILLING_SNAPSHOT_DOCUMENT = "projectBillingSnapshot";

const FOUNDER_DAILY_KPIS = "founder_daily_kpis";

const MAX_AUTH_PAGES = 50; // 50 * 1000 = 50k Nutzer max, dann abgeschnitten
const USERS_PER_PAGE = 1000;
const CUTOFF_MS_24H = 24 * 60 * 60 * 1000;

function aiUsageMetricsDocumentId(dateKey) {
  return `${AI_USAGE_METRICS_DOCUMENT_PREFIX}${dateKey}`;
}
function aiUsageMonthlyMetricsDocumentId(monthKey) {
  return `${AI_USAGE_MONTHLY_METRICS_DOCUMENT_PREFIX}${monthKey}`;
}

/**
 * ISO-Datum (yyyy-mm-dd) fuer Europe/Berlin, passend zur Android-Home-Anfrage.
 */
function berlinYmd(d = new Date()) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Berlin",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(d);
  const y = parts.find((p) => p.type === "year").value;
  const m = parts.find((p) => p.type === "month").value;
  const day = parts.find((p) => p.type === "day").value;
  return `${y}-${m}-${day}`;
}

/**
 * Vortag derselben YMD-String-Logik (nur als UTC-Datum, fuer Metrik-Keys).
 */
function previousIsoYmd(ymd) {
  const m = ymd.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!m) {
    return new Date().toISOString().slice(0, 10);
  }
  const t = new Date(Date.UTC(
      parseInt(m[1], 10), parseInt(m[2], 10) - 1, parseInt(m[3], 10) - 1, 12, 0, 0,
  ));
  return t.toISOString().slice(0, 10);
}

/** @param {Date} t */
function toBerlinYmdString(t) {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Berlin",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(t);
  const y = parts.find((p) => p.type === "year").value;
  const m = parts.find((p) => p.type === "month").value;
  const d = parts.find((p) => p.type === "day").value;
  return `${y}-${m}-${d}`;
}

/**
 * UTC-Tag, der 12:00 Uhr in Europe/Berlin (am Kalendertag kpiYmd) am ehesten enthaelt —
 * entspricht den aiUsageDaily_*-Docs, die mit UTC-Tagen geschrieben werden.
 */
function utcYmdAlignedToBerlinNoonForKpiDay(kpiYmd) {
  const m = kpiYmd.match(/^(\d{4})-(\d{2})-(\d{2})$/);
  if (!m) {
    return new Date().toISOString().slice(0, 10);
  }
  const localNoon = new Date(
      Date.UTC(
          parseInt(m[1], 10),
          parseInt(m[2], 10) - 1,
          parseInt(m[3], 10),
          10,
          0,
          0,
      ),
  );
  return localNoon.toISOString().slice(0, 10);
}

function microsFieldToEur2(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || n < 0) {
    return null;
  }
  return Number((n / 1_000_000).toFixed(2));
}

function eurFieldToMicros(value) {
  const n = Number(value);
  if (!Number.isFinite(n) || n < 0) {
    return null;
  }
  return Math.round(n * 1_000_000);
}

function normalizeProjectCostServices(value) {
  if (Array.isArray(value)) {
    return value
        .map((entry) => {
          const label = String(entry?.label || entry?.service || entry?.name || "").trim();
          const micros = Number.isFinite(Number(entry?.actualCostMicros)) ?
            Number(entry.actualCostMicros) :
            Number.isFinite(Number(entry?.costMicros)) ?
              Number(entry.costMicros) :
              eurFieldToMicros(entry?.costEur);
          if (!label || !Number.isFinite(micros) || micros < 0) {
            return null;
          }
          return {
            label,
            costEur: microsFieldToEur2(micros),
            actualCostMicros: Math.round(micros),
          };
        })
        .filter(Boolean)
        .sort((left, right) => right.actualCostMicros - left.actualCostMicros)
        .slice(0, 8);
  }
  if (value && typeof value === "object") {
    return normalizeProjectCostServices(
        Object.entries(value).map(([label, raw]) => ({
          label,
          actualCostMicros: Number.isFinite(Number(raw)) ? Number(raw) : null,
          costEur: Number.isFinite(Number(raw)) ? null : raw,
        })),
    );
  }
  return [];
}

function normalizeProjectBillingSnapshot(data, {source, monthKey} = {}) {
  const d = data && typeof data === "object" && !Array.isArray(data) ? data : {};
  const snapshotMonth = String(d.monthKey || d.billingMonth || d.period || monthKey || "").slice(0, 7);
  if (monthKey && snapshotMonth && snapshotMonth !== monthKey) {
    return null;
  }
  const actualMicros = Number.isFinite(Number(d.totalActualCostMicros)) ?
    Number(d.totalActualCostMicros) :
    Number.isFinite(Number(d.totalCostMicros)) ?
      Number(d.totalCostMicros) :
      eurFieldToMicros(d.totalCostEur ?? d.projectCostEur ?? d.costEur);
  if (!Number.isFinite(actualMicros) || actualMicros < 0) {
    return null;
  }
  const services = normalizeProjectCostServices(d.services || d.serviceCosts || d.breakdown || d.byService);
  return {
    monthKey: monthKey || snapshotMonth || null,
    totalActualCostMicros: Math.round(actualMicros),
    totalCostEur: microsFieldToEur2(actualMicros),
    currency: String(d.currency || "EUR").trim().toUpperCase() || "EUR",
    source: String(d.source || source || "project_billing_snapshot").trim(),
    updatedAt: String(d.updatedAt || d.generatedAt || "").trim() || null,
    services,
  };
}

async function loadProjectBillingSnapshot(firestore, monthKey) {
  const [metricSnap, adminSnap] = await Promise.all([
    firestore.collection(AI_USAGE_METRICS_COLLECTION).doc(`${PROJECT_BILLING_DOCUMENT_PREFIX}${monthKey}`).get(),
    firestore.collection(ADMIN_CONFIG_COLLECTION).doc(PROJECT_BILLING_SNAPSHOT_DOCUMENT).get(),
  ]);
  return normalizeProjectBillingSnapshot(metricSnap.data(), {
    source: `${AI_USAGE_METRICS_COLLECTION}/${PROJECT_BILLING_DOCUMENT_PREFIX}${monthKey}`,
    monthKey,
  }) || normalizeProjectBillingSnapshot(adminSnap.data(), {
    source: `${ADMIN_CONFIG_COLLECTION}/${PROJECT_BILLING_SNAPSHOT_DOCUMENT}`,
    monthKey,
  });
}

function pickActualDailyCostEur(daily) {
  const d = daily && typeof daily === "object" && !Array.isArray(daily) ? daily : {};
  return microsFieldToEur2(d.totalActualCostMicros);
}

/**
 * @param {import("firebase-admin").firestore.Firestore} firestore
 * @param {import("firebase-admin").auth.Auth} auth
 * @param {{ kpiYmd?: string, now?: Date }} options
 * @return {Promise<object>}
 */
async function runSyncFounderDailyKpis({ firestore, auth, options = {} }) {
  const kpiYmd = options.kpiYmd || berlinYmd(options.now || new Date());
  const utcMetricYmd = utcYmdAlignedToBerlinNoonForKpiDay(kpiYmd);
  const prevUtcMetricYmd = previousIsoYmd(utcMetricYmd);
  const monthKey = kpiYmd.slice(0, 7);

  const [dailySnap, dailyPrevSnap, monthSnap, projectBilling] = await Promise.all([
    firestore.collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMetricsDocumentId(utcMetricYmd)).get(),
    firestore.collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMetricsDocumentId(prevUtcMetricYmd)).get(),
    firestore.collection(AI_USAGE_METRICS_COLLECTION).doc(aiUsageMonthlyMetricsDocumentId(monthKey)).get(),
    loadProjectBillingSnapshot(firestore, monthKey),
  ]);

  let ordersTotalSnap = {empty: true, docs: []};
  try {
    ordersTotalSnap = await firestore
        .collection("orders")
        .orderBy("timestamp", "desc")
        .limit(500)
        .get();
  } catch (err) {
    logger.warn("founder_kpis.orders_query_failed", {err: err instanceof Error ? err.message : String(err)});
  }

  const daily = dailySnap.data() || {};
  const dailyPrev = dailyPrevSnap.data() || {};
  const month = monthSnap.data() || {};

  const costTodayEur = pickActualDailyCostEur(daily);
  const costPrevEur = pickActualDailyCostEur(dailyPrev);
  let trendPct = null;
  if (costTodayEur !== null && Number.isFinite(costTodayEur) && costTodayEur >= 0) {
    if (costPrevEur > 0) {
      trendPct = Number((((costTodayEur - costPrevEur) / costPrevEur) * 100).toFixed(2));
    } else if (costTodayEur > 0 && costPrevEur === 0) {
      trendPct = 0;
    }
  }

  const costMtdEur = microsFieldToEur2(month.totalActualCostMicros);

  let newUsers24h = 0;
  let activeUsers24h = 0;
  let userPages = 0;
  let nextToken = undefined;
  const nowMs = Date.now();
  const start24h = nowMs - CUTOFF_MS_24H;

  while (userPages < MAX_AUTH_PAGES) {
    const page = await auth.listUsers(USERS_PER_PAGE, nextToken);
    for (const u of page.users) {
      const c = u.metadata?.creationTime ? new Date(u.metadata.creationTime).getTime() : null;
      if (Number.isFinite(c) && c >= start24h) {
        newUsers24h += 1;
      }
      const s = u.metadata?.lastSignInTime ? new Date(u.metadata.lastSignInTime).getTime() : null;
      if (Number.isFinite(s) && s >= start24h) {
        activeUsers24h += 1;
      }
    }
    if (!page.pageToken) {
      break;
    }
    nextToken = page.pageToken;
    userPages += 1;
  }

  if (userPages >= MAX_AUTH_PAGES) {
    logger.warn("founder_kpis.listUsers_truncated", {maxChecked: MAX_AUTH_PAGES * USERS_PER_PAGE});
  }

  let revenueToday = 0;
  let revenueMtd = 0;
  if (ordersTotalSnap && !ordersTotalSnap.empty && Array.isArray(ordersTotalSnap.docs)) {
    for (const doc of ordersTotalSnap.docs) {
      const o = doc.data() || {};
      const pay = String(o.paymentStatus || "").toLowerCase();
      if (pay !== "confirmed") {
        continue;
      }
      const ts = o.timestamp;
      const t =
        ts && typeof ts.toDate === "function" ? ts.toDate() :
        ts && typeof ts._seconds === "number" ? new Date(ts._seconds * 1000) :
        null;
      if (!t || !Number.isFinite(t.getTime())) {
        continue;
      }
      const tot = Number(o.totalAmount) || 0;
      if (tot < 0) {
        continue;
      }
      const bYmd = toBerlinYmdString(t);
      if (bYmd.slice(0, 7) === kpiYmd.slice(0, 7)) {
        revenueMtd += tot;
      }
      if (bYmd === kpiYmd) {
        revenueToday += tot;
      }
    }
  }

  const doc = {
    firebase_cost_today: costTodayEur === null ? admin.firestore.FieldValue.delete() : costTodayEur,
    firebase_cost_mtd: costMtdEur === null ? admin.firestore.FieldValue.delete() : Number(costMtdEur.toFixed(2)),
    active_users_24h: activeUsers24h,
    new_users_24h: newUsers24h,
    /** Ohne separate Analytics-Quelle nicht ableitbar — Feld weglassen */
    updatedAt: new Date().toISOString(),
    kpiSource: "syncFounderDailyKpis",
    kpiCostSource: "systemMetrics_ai_usage_actual",
    kpiCostStatus: costTodayEur === null ? "actual_missing" : "actual",
    kpiCostNote: costTodayEur === null ?
      "No totalActualCostMicros in systemMetrics for this day. Estimated values are intentionally not shown." :
      admin.firestore.FieldValue.delete(),
    kpiUserSource: "firebase_admin_listUsers",
    kpiRevenueSource: "orders_merch_confirmed_recent500",
    project_cost_mtd: projectBilling ? projectBilling.totalCostEur : admin.firestore.FieldValue.delete(),
    project_cost_currency: projectBilling ? projectBilling.currency : admin.firestore.FieldValue.delete(),
    project_cost_services: projectBilling ? projectBilling.services : admin.firestore.FieldValue.delete(),
    project_cost_updated_at: projectBilling?.updatedAt || admin.firestore.FieldValue.delete(),
    project_cost_period: projectBilling?.monthKey || admin.firestore.FieldValue.delete(),
    kpiProjectCostSource: projectBilling?.source || admin.firestore.FieldValue.delete(),
    kpiProjectCostStatus: projectBilling ? "actual" : admin.firestore.FieldValue.delete(),
    metricUtcDay: utcMetricYmd,
    source: admin.firestore.FieldValue.delete(),
  };
  if (trendPct === null || !Number.isFinite(trendPct)) {
    doc.firebase_cost_trend_pct = admin.firestore.FieldValue.delete();
  } else {
    doc.firebase_cost_trend_pct = trendPct;
  }

  doc.revenue_today = Number(revenueToday.toFixed(2));
  doc.revenue_mtd = Number(revenueMtd.toFixed(2));

  await firestore
      .collection(FOUNDER_DAILY_KPIS)
      .doc(kpiYmd)
      .set(doc, {merge: true});

  logger.info("founder_kpis.synced", {
    kpiYmd,
    costTodayEur: costTodayEur,
    costStatus: doc.kpiCostStatus,
    newUsers24h,
    activeUsers24h,
    hasOrdersScanned: Boolean(ordersTotalSnap && !ordersTotalSnap.empty),
  });

  return {
    kpiYmd,
    firebase_cost_today: costTodayEur,
    firebase_cost_mtd: costMtdEur,
    firebase_cost_trend_pct: trendPct,
    kpiCostStatus: doc.kpiCostStatus,
    active_users_24h: activeUsers24h,
    new_users_24h: newUsers24h,
    revenue_today: doc.revenue_today,
    revenue_mtd: doc.revenue_mtd,
    project_cost_mtd: projectBilling ? projectBilling.totalCostEur : null,
    project_cost_services: projectBilling ? projectBilling.services : [],
    metricUtcDay: utcMetricYmd,
  };
}

module.exports = {
  runSyncFounderDailyKpis,
  berlinYmd,
};
