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
  const t = (accessToken || "").trim();
  const id = (igUserId || "").trim();
  if (!t || !id) {
    return "";
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
    const c = new AbortController();
    const timer = setTimeout(() => c.abort(), 12_000);
    const r = await fetch(url, {method: "GET", signal: c.signal});
    clearTimeout(timer);
    if (!r.ok) {
      return "";
    }
    const j = await r.json();
    if (j.error) {
      return "";
    }
    return formatGraphIgUserBlock(j);
  } catch {
    return "";
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
  const t = (accessToken || process.env.META_IG_USER_ACCESS_TOKEN || "").trim();
  const id = (igUserId || process.env.META_IG_USER_ID || "").trim();
  if (t && id) {
    const fromApi = await fetchInstagramGraphUserSummary({
      accessToken: t,
      igUserId: id,
      graphVersion,
    });
    if (fromApi) {
      return fromApi;
    }
    return buildInstagramContextWithoutToken(h) +
      " (Graph-Aufruf fehlgeschlagen oder Berechtigungen/IDs pruefen.)";
  }
  return buildInstagramContextWithoutToken(h);
}

module.exports = {
  buildInstagramContextWithoutToken,
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
