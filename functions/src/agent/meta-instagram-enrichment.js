"use strict";

/**
 * Instagram: Meta Graph API nur fuer **verbundene** Business-/Creator-Konten sinnvoll.
 *
 * Ohne serverseitigen User/Page-Token: kein oeffentlicher Katalog wie YouTube/Spotify.
 * Optional (Founder-Test, spaeter pro Nutzer in DB):
 *   META_IG_USER_ACCESS_TOKEN, META_IG_USER_ID
 * Graph-Version: META_GRAPH_API_VERSION (default v20.0)
 * @see https://developers.facebook.com/docs/instagram-api
 */

const DEFAULT_GRAPH_VERSION = "v20.0";

/**
 * @param {string} handle Normalisierter @name / Suchbegriff
 * @returns {string} Statischer Kontext, wenn kein API-Zugang
 */
function buildInstagramContextWithoutToken(handle) {
  const safe = (handle || "").replace(/^@+/, "").replace(/\s+/g, " ").trim();
  const at = safe ? "@" + safe : "(leer)";
  return [
    "Instagram (Meta, ohne Graph-API-Zugriff):",
    "Es liegt nur der Handle-Bezug vor: " + at + ".",
    "Arbeite mit dem Handle und dem Nutzerprompt. Keine erfundenen Impressions, Reichweite oder Insights.",
    "Live-Profilfelder aus der Graph API sind moeglich, sobald serverseitig " +
    "META_IG_USER_ACCESS_TOKEN + META_IG_USER_ID (verbundenes Konto) gesetzt sind.",
  ].join(" ");
}

function buildInstagramContextWithConfiguredButUnavailableGraph(handle, reasons = []) {
  const safe = (handle || "").replace(/^@+/, "").replace(/\s+/g, " ").trim();
  const at = safe ? "@" + safe : "(leer)";
  const reasonLines = Array.isArray(reasons) ?
    reasons.map((r) => nonEmpty(r)).filter(Boolean).slice(0, 2) :
    [];
  return [
    "Instagram Graph API ist serverseitig konfiguriert, aber der Live-Aufruf liefert gerade keine verwertbaren Daten.",
    "Handle-Bezug: " + at + ".",
    reasonLines.length ? "Live-Status: " + reasonLines.join(" | ") + "." :
      "Wahrscheinlich: Token abgelaufen, fehlende Business-/Creator-Verknuepfung, fehlende Permissions oder Business Discovery nicht freigegeben.",
    "Arbeite mit Handle und Nutzerprompt. Keine erfundenen Impressions, Reichweite oder Insights.",
  ].join(" ");
}

/**
 * @param {object} p
 * @param {string} p.accessToken
 * @param {string} p.igUserId Instagram-Scoped-User-Id
 * @param {string} [p.graphVersion]
 * @returns {Promise<string>} Kurz-Profil oder leer bei Fehler
 */
async function fetchInstagramGraphUserSummary({
  accessToken,
  igUserId,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const result = await fetchInstagramGraphUserResult({accessToken, igUserId, graphVersion});
  return result.summary || "";
}

async function fetchInstagramGraphUserResult({
  accessToken,
  igUserId,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const t = (accessToken || "").trim();
  const id = (igUserId || "").trim();
  if (!t || !id) {
    return {summary: "", reason: "Meta-Token oder IG User ID fehlt."};
  }
  const fields = [
    "id",
    "username",
    "name",
    "profile_picture_url",
    "biography",
    "followers_count",
    "follows_count",
    "media_count",
  ].join(",");
  const base = "https://graph.facebook.com";
  const url = `${base}/${graphVersion}/${encodeURIComponent(id)}?` +
    `fields=${encodeURIComponent(fields)}&access_token=${encodeURIComponent(t)}`;
  try {
    const graph = await fetchGraphJson(url);
    if (!graph.ok) {
      return {summary: "", reason: describeGraphError(graph.error, graph.status)};
    }
    return {summary: formatGraphIgUserBlock(graph.data), reason: ""};
  } catch {
    return {summary: "", reason: "Graph-Aufruf fehlgeschlagen oder Timeout."};
  }
}

/**
 * @param {object} p
 * @param {string} p.accessToken
 * @param {string} p.igUserId
 * @param {string} p.username
 * @param {string} [p.graphVersion]
 * @returns {Promise<string>}
 */
async function fetchInstagramGraphBusinessDiscoverySummary({
  accessToken,
  igUserId,
  username,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const result = await fetchInstagramGraphBusinessDiscoveryResult({
    accessToken,
    igUserId,
    username,
    graphVersion,
  });
  return result.summary || "";
}

async function fetchInstagramGraphBusinessDiscoveryResult({
  accessToken,
  igUserId,
  username,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const t = (accessToken || "").trim();
  const id = (igUserId || "").trim();
  const u = normalizeInstagramUsername(username);
  if (!t || !id || !u) {
    return {summary: "", reason: "Meta-Token, IG User ID oder Ziel-Username fehlt."};
  }
  const discoveryFields = [
    "username",
    "name",
    "biography",
    "followers_count",
    "follows_count",
    "media_count",
    "profile_picture_url",
  ].join(",");
  const fields = `business_discovery.username(${u}){${discoveryFields}}`;
  const base = "https://graph.facebook.com";
  const url = `${base}/${graphVersion}/${encodeURIComponent(id)}?` +
    `fields=${encodeURIComponent(fields)}&access_token=${encodeURIComponent(t)}`;
  try {
    const graph = await fetchGraphJson(url);
    if (!graph.ok) {
      return {summary: "", reason: describeGraphError(graph.error, graph.status)};
    }
    const bd = graph.data.business_discovery;
    if (!bd || typeof bd !== "object") {
      return {summary: "", reason: "Business Discovery lieferte kein Profilobjekt."};
    }
    return {summary: formatGraphIgBusinessDiscoveryBlock(bd), reason: ""};
  } catch {
    return {summary: "", reason: "Business-Discovery-Aufruf fehlgeschlagen oder Timeout."};
  }
}

/**
 * @param {string} handle aus resolveSocialProfiles
 * @param {string} [accessToken]
 * @param {string} [igUserId]
 * @param {string} [graphVersion]
 * @returns {Promise<string>}
 */
async function resolveInstagramContextForAgent({
  handle = "",
  accessToken = "",
  igUserId = "",
  graphVersion = (process.env.META_GRAPH_API_VERSION || "").trim() || DEFAULT_GRAPH_VERSION,
} = {}) {
  const h = (handle || "").trim();
  if (!h) {
    return "";
  }
  const normalized = normalizeInstagramUsername(h);
  const t = (accessToken || process.env.META_IG_USER_ACCESS_TOKEN || "").trim();
  const id = (igUserId || process.env.META_IG_USER_ID || "").trim();
  if (t && id) {
    const reasons = [];
    if (normalized) {
      const discovery = await fetchInstagramGraphBusinessDiscoveryResult({
        accessToken: t,
        igUserId: id,
        username: normalized,
        graphVersion,
      });
      if (discovery.summary) {
        return discovery.summary;
      }
      if (discovery.reason) {
        reasons.push("Business Discovery: " + discovery.reason);
      }
    }
    // Fallback to connected account profile (kept for backwards compatibility),
    // but label it clearly to avoid mixing it up with handle-specific discovery.
    const ownProfile = await fetchInstagramGraphUserResult({
      accessToken: t,
      igUserId: id,
      graphVersion,
    });
    if (ownProfile.summary) {
      return ownProfile.summary + `\nHinweis: Handle-spezifische Discovery fuer @${normalized || h} war nicht verfuegbar.`;
    }
    if (ownProfile.reason) {
      reasons.push("Verbundenes Konto: " + ownProfile.reason);
    }
    return buildInstagramContextWithConfiguredButUnavailableGraph(h, reasons);
  }
  return buildInstagramContextWithoutToken(h);
}

module.exports = {
  buildInstagramContextWithoutToken,
  fetchInstagramGraphBusinessDiscoverySummary,
  fetchInstagramGraphUserSummary,
  resolveInstagramContextForAgent,
};

function formatGraphIgUserBlock(j) {
  const parts = [
    "Instagram Graph API (verbundenes Konto, oeffentl. Profilfelder, Stand Abfrage):",
    j.username ? "Username: @" + j.username : null,
    j.name && j.name !== j.username ? "Name: " + j.name : null,
    j.biography != null && `${j.biography}`.length ? "Bio: " + trimDesc(j.biography) : null,
    j.followers_count != null ? "Follower (Katalog, ca.): " + j.followers_count : null,
    j.follows_count != null ? "Folge ich: " + j.follows_count : null,
    j.media_count != null ? "Beitraege (Zaehler): " + j.media_count : null,
  ].filter(Boolean);
  return parts.join("\n");
}

function trimDesc(s) {
  return (String(s) || "").replace(/\s+/g, " ").trim().slice(0, 500);
}

async function fetchGraphJson(url) {
  const c = new AbortController();
  const timer = setTimeout(() => c.abort(), 12_000);
  try {
    const r = await fetch(url, {method: "GET", signal: c.signal});
    let body = null;
    try {
      body = await r.json();
    } catch {
      body = null;
    }
    if (!r.ok || body?.error) {
      return {ok: false, status: r.status, error: body?.error || null, data: body || null};
    }
    return {ok: true, status: r.status, error: null, data: body || {}};
  } finally {
    clearTimeout(timer);
  }
}

function describeGraphError(error, status = 0) {
  const code = error?.code != null ? String(error.code) : "";
  const subcode = error?.error_subcode != null ? String(error.error_subcode) : "";
  const type = nonEmpty(error?.type);
  const message = nonEmpty(error?.message);
  const lower = message.toLowerCase();
  if (code === "190" || lower.includes("access token") || lower.includes("session has expired")) {
    if (subcode === "463" || lower.includes("expired")) {
      return "Meta Access Token ist abgelaufen; neu verbinden/Long-Lived Token setzen.";
    }
    return "Meta Access Token ist ungueltig; neu verbinden oder Token pruefen.";
  }
  if (code === "10" || code === "200" || lower.includes("permission")) {
    return "Meta Permission fehlt; Instagram Business Discovery/Pages-Berechtigungen pruefen.";
  }
  if (code === "100") {
    return "Meta Parameter/IG User ID oder Username nicht aufloesbar.";
  }
  if (status === 429 || lower.includes("rate")) {
    return "Meta Rate-Limit erreicht; spaeter erneut versuchen.";
  }
  const details = [type ? `Typ ${type}` : "", code ? `Code ${code}` : "", subcode ? `Subcode ${subcode}` : ""]
      .filter(Boolean)
      .join(", ");
  return details ? `Meta Graph Fehler (${details}).` : "Meta Graph Fehler ohne verwertbare Detailmeldung.";
}

function nonEmpty(value) {
  return String(value || "").replace(/\s+/g, " ").trim();
}

function normalizeInstagramUsername(raw) {
  const s = String(raw || "").trim();
  if (!s) {
    return "";
  }
  const withoutAt = s.replace(/^@+/, "");
  const fromUrl = withoutAt.match(/instagram\.com\/([a-zA-Z0-9._]+)/i);
  const base = (fromUrl ? fromUrl[1] : withoutAt).split(/[/?#]/)[0];
  return base.replace(/[^a-zA-Z0-9._]/g, "").slice(0, 30);
}

function formatGraphIgBusinessDiscoveryBlock(j) {
  const parts = [
    "Instagram Graph API (business discovery, oeffentl. Profilfelder, Stand Abfrage):",
    j.username ? "Username: @" + j.username : null,
    j.name && j.name !== j.username ? "Name: " + j.name : null,
    j.biography != null && `${j.biography}`.length ? "Bio: " + trimDesc(j.biography) : null,
    j.followers_count != null ? "Follower (Katalog, ca.): " + j.followers_count : null,
    j.follows_count != null ? "Folge ich: " + j.follows_count : null,
    j.media_count != null ? "Beitraege (Zaehler): " + j.media_count : null,
  ].filter(Boolean);
  return parts.join("\n");
}
