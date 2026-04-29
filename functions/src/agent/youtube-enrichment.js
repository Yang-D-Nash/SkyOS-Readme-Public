"use strict";

/**
 * Oeffentliche Kanal-Daten via YouTube Data API v3 (API-Key, kein OAuth).
 * In Google Cloud: "YouTube Data API v3" aktivieren + API-Key.
 * @see https://developers.google.com/youtube/v3
 */

const YT = "https://www.googleapis.com/youtube/v3";

/**
 * @param {object} opts
 * @param {string} opts.apiKey
 * @param {string} opts.handle Roher Handle, @name, Kanal-ID (UC…), oder YouTube-URL
 * @returns {Promise<string>} Kurztext fuer LLM oder leer
 */
async function fetchYouTubeHandleCatalogSummary({apiKey, handle}) {
  const key = (apiKey || "").trim();
  const raw = (handle || "").trim();
  if (!key || !raw) {
    return "";
  }
  try {
    const parsed = parseYouTubeInput(raw);
    if (!parsed) {
      return "";
    }
    if (parsed.kind === "id") {
      return await loadChannelById(key, parsed.value);
    }
    if (parsed.kind === "forHandle") {
      const u = `${YT}/channels?part=snippet,statistics&forHandle=${encodeURIComponent(parsed.value)}&key=${encodeURIComponent(key)}`;
      const r = await ytGet(u);
      if (r.ok) {
        const j = await r.json();
        if (j?.items?.[0]) {
          return formatChannelBlock(j.items[0]);
        }
      }
      const fromSearch = await searchChannelByQuery(key, parsed.value);
      if (fromSearch) {
        return await loadChannelById(key, fromSearch);
      }
      return "YouTube (Data API): Kein oeffentlicher Kanal zu diesem Handle gefunden.";
    }
    if (parsed.kind === "search") {
      const fromSearch = await searchChannelByQuery(key, parsed.value);
      if (!fromSearch) {
        return "";
      }
      return await loadChannelById(key, fromSearch);
    }
    return "";
  } catch {
    return "";
  }
}

module.exports = {fetchYouTubeHandleCatalogSummary};

async function loadChannelById(key, channelId) {
  const u = `${YT}/channels?part=snippet,statistics&id=${encodeURIComponent(channelId)}&key=${encodeURIComponent(key)}`;
  const r = await ytGet(u);
  if (!r.ok) {
    return "";
  }
  const j = await r.json();
  const item = j?.items?.[0];
  if (!item) {
    return "";
  }
  return formatChannelBlock(item);
}

function parseYouTubeInput(raw) {
  const s = raw.trim();
  if (!s) {
    return null;
  }
  if (/^UC[_a-zA-Z0-9-]{10,}$/.test(s) && s.startsWith("UC")) {
    return {kind: "id", value: s};
  }
  const channelM = s.match(/youtube\.com\/channel\/([a-zA-Z0-9_-]+)/i);
  if (channelM) {
    return {kind: "id", value: channelM[1]};
  }
  const atM = s.match(/youtube\.com\/@([a-zA-Z0-9._-]+)/i);
  if (atM) {
    return {kind: "forHandle", value: atM[1]};
  }
  const cM = s.match(/youtube\.com\/c\/([a-zA-Z0-9._-]+)/i);
  if (cM) {
    return {kind: "search", value: cM[1]};
  }
  const userM = s.match(/youtube\.com\/user\/([a-zA-Z0-9._-]+)/i);
  if (userM) {
    return {kind: "search", value: userM[1]};
  }
  const h = s.replace(/^@+/, "").replace(/\s+/g, "");
  if (h) {
    return {kind: "forHandle", value: h};
  }
  return null;
}

async function searchChannelByQuery(apiKey, q) {
  const u = `${YT}/search?part=snippet&type=channel&maxResults=2&q=${encodeURIComponent(q)}&key=${encodeURIComponent(apiKey)}`;
  const r = await ytGet(u);
  if (!r.ok) {
    return null;
  }
  const j = await r.json();
  return j?.items?.[0]?.id?.channelId || null;
}

function formatChannelBlock(item) {
  const sn = item.snippet || {};
  const st = item.statistics || {};
  const title = sn.title || "—";
  const desc = (sn.description || "").replace(/\s+/g, " ").trim().slice(0, 400);
  const parts = [
    `Kanal: ${title}`,
    item.id ? `Kanal-ID: ${item.id}` : null,
    sn.customUrl ? `Kurz-URL: /${String(sn.customUrl).replace(/^\//, "")}` : null,
    st.subscriberCount != null ? `Abonnenten (Katalog, ca.): ${st.subscriberCount}` : null,
    st.videoCount != null ? `Videoanzahl: ${st.videoCount}` : null,
    st.viewCount != null ? `Gesamtaufrufe: ${st.viewCount}` : null,
    desc ? `Beschreibung (Auszug): ${desc}` : null,
  ].filter(Boolean);
  return `YouTube Data API v3 (oeffentliches Kanalprofil, Stand Abfrage):\n${parts.join("\n")}`;
}

function ytGet(url) {
  const c = new AbortController();
  const t = setTimeout(() => c.abort(), 12_000);
  return fetch(url, {signal: c.signal})
      .finally(() => {
        clearTimeout(t);
      });
}
