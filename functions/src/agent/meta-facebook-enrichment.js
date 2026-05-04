"use strict";

/**
 * Facebook/Meta Page context for the SkyOS social analysis agent.
 *
 * Public Facebook metrics are not a generic unauthenticated catalog. When a
 * valid Page/User access token is available, SkyOS reads a small public Page
 * profile snapshot. Otherwise the agent receives only the handle context and
 * must not invent reach, impressions, followers, or engagement.
 */

const DEFAULT_GRAPH_VERSION = "v20.0";

async function resolveFacebookPageContextForAgent({
  handle = "",
  accessToken = "",
  pageId = "",
  graphVersion = (process.env.META_GRAPH_API_VERSION || "").trim() || DEFAULT_GRAPH_VERSION,
} = {}) {
  const raw = String(handle || pageId || "").trim();
  if (!raw) {
    return "";
  }
  const ref = normalizeFacebookPageReference(pageId || handle);
  if (!ref) {
    return buildFacebookContextWithoutToken(raw);
  }
  const token = String(
      accessToken ||
      process.env.META_FACEBOOK_PAGE_ACCESS_TOKEN ||
      process.env.META_PAGE_ACCESS_TOKEN ||
      process.env.META_GRAPH_ACCESS_TOKEN ||
      "",
  ).trim();
  if (!token) {
    return buildFacebookContextWithoutToken(raw);
  }
  const live = await fetchFacebookPageResult({
    accessToken: token,
    pageRef: ref,
    graphVersion,
  });
  return live.summary || buildFacebookContextWithConfiguredButUnavailableGraph(raw, live.reason);
}

async function fetchFacebookPageSummary({
  accessToken,
  pageRef,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const result = await fetchFacebookPageResult({accessToken, pageRef, graphVersion});
  return result.summary || "";
}

async function fetchFacebookPageResult({
  accessToken,
  pageRef,
  graphVersion = DEFAULT_GRAPH_VERSION,
}) {
  const token = String(accessToken || "").trim();
  const ref = normalizeFacebookPageReference(pageRef);
  if (!token || !ref) {
    return {summary: "", reason: "Meta Page Token oder Page-Referenz fehlt."};
  }
  const full = await graphGetPage({
    accessToken: token,
    pageRef: ref,
    graphVersion,
    fields: [
      "id",
      "name",
      "category",
      "about",
      "link",
      "fan_count",
      "followers_count",
      "verification_status",
    ],
  });
  if (full.data) {
    return {summary: formatFacebookPageBlock(full.data), reason: ""};
  }
  const minimal = await graphGetPage({
    accessToken: token,
    pageRef: ref,
    graphVersion,
    fields: ["id", "name", "category", "link"],
  });
  if (minimal.data) {
    return {summary: formatFacebookPageBlock(minimal.data), reason: ""};
  }
  return {summary: "", reason: minimal.reason || full.reason || "Graph API lieferte keine Page-Daten."};
}

async function graphGetPage({accessToken, pageRef, graphVersion, fields}) {
  const base = "https://graph.facebook.com";
  const url = `${base}/${graphVersion}/${encodeURIComponent(pageRef)}?` +
    `fields=${encodeURIComponent(fields.join(","))}&access_token=${encodeURIComponent(accessToken)}`;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 12_000);
  try {
    const response = await fetch(url, {method: "GET", signal: controller.signal});
    let body = null;
    try {
      body = await response.json();
    } catch {
      body = null;
    }
    if (!response.ok || !body || body.error) {
      return {data: null, reason: describeGraphError(body?.error || null, response.status)};
    }
    return {data: body, reason: ""};
  } catch {
    return {data: null, reason: "Graph-Aufruf fehlgeschlagen oder Timeout."};
  } finally {
    clearTimeout(timeout);
  }
}

function buildFacebookContextWithoutToken(handle) {
  const safe = normalizeFacebookPageReference(handle) || String(handle || "").trim();
  const label = safe ? `@${safe.replace(/^@+/, "")}` : "(leer)";
  return [
    "Facebook/Meta (ohne verwertbaren Graph-API-Zugriff):",
    "Es liegt nur der Page-/Handle-Bezug vor: " + label + ".",
    "Arbeite mit Handle und Nutzerprompt. Keine erfundenen Reichweiten, Follower, Impressions oder Insights.",
    "Live-Page-Felder sind moeglich, sobald serverseitig ein gueltiger Meta Page/User Access Token gesetzt ist.",
  ].join(" ");
}

function buildFacebookContextWithConfiguredButUnavailableGraph(handle, reason = "") {
  const safe = normalizeFacebookPageReference(handle) || String(handle || "").trim();
  const label = safe ? `@${safe.replace(/^@+/, "")}` : "(leer)";
  const status = nonEmpty(reason);
  return [
    "Facebook/Meta Graph API ist serverseitig konfiguriert, aber der Live-Aufruf liefert gerade keine verwertbaren Page-Daten.",
    "Page-/Handle-Bezug: " + label + ".",
    status ? "Live-Status: " + status + "." :
      "Wahrscheinlich: Token abgelaufen, fehlende Page-Berechtigung, Page Public Content Access nicht freigegeben oder Page-ID/Handle nicht aufloesbar.",
    "Arbeite mit Handle und Nutzerprompt. Keine erfundenen Reichweiten, Follower, Impressions oder Insights.",
  ].join(" ");
}

function normalizeFacebookPageReference(raw) {
  const s = String(raw || "").trim();
  if (!s) {
    return "";
  }
  const withoutAt = s.replace(/^@+/, "");
  const fromUrl = withoutAt.match(/facebook\.com\/(?:pages\/)?([a-zA-Z0-9._-]+)/i);
  const base = (fromUrl ? fromUrl[1] : withoutAt).split(/[/?#]/)[0];
  return base.replace(/[^a-zA-Z0-9._-]/g, "").slice(0, 80);
}

function formatFacebookPageBlock(page) {
  const parts = [
    "Facebook/Meta Graph API (Page-Profil, Stand Abfrage):",
    page.name ? "Name: " + page.name : null,
    page.id ? "Page-ID: " + page.id : null,
    page.category ? "Kategorie: " + page.category : null,
    page.about ? "About: " + trimDesc(page.about) : null,
    page.fan_count != null ? "Page Likes (ca.): " + page.fan_count : null,
    page.followers_count != null ? "Follower (ca.): " + page.followers_count : null,
    page.verification_status ? "Verifizierungsstatus: " + page.verification_status : null,
    page.link ? "Page: " + page.link : null,
  ].filter(Boolean);
  return parts.join("\n");
}

function trimDesc(value) {
  return String(value || "").replace(/\s+/g, " ").trim().slice(0, 500);
}

function describeGraphError(error, status = 0) {
  const code = error?.code != null ? String(error.code) : "";
  const subcode = error?.error_subcode != null ? String(error.error_subcode) : "";
  const type = nonEmpty(error?.type);
  const message = nonEmpty(error?.message);
  const lower = message.toLowerCase();
  if (code === "190" || lower.includes("access token") || lower.includes("session has expired")) {
    if (subcode === "463" || lower.includes("expired")) {
      return "Meta Access Token ist abgelaufen; Page neu verbinden/Long-Lived Token setzen.";
    }
    return "Meta Access Token ist ungueltig; Page Token pruefen.";
  }
  if (code === "10" || code === "200" || lower.includes("permission")) {
    return "Meta Page Permission fehlt; pages_read_engagement/Page Public Content Access pruefen.";
  }
  if (code === "100" || status === 404) {
    return "Page-ID/Handle nicht aufloesbar oder Feld fuer diese Page nicht freigegeben.";
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

module.exports = {
  buildFacebookContextWithoutToken,
  fetchFacebookPageSummary,
  resolveFacebookPageContextForAgent,
};
