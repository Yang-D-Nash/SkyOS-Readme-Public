"use strict";

const TIKTOK_OPEN_API_BASE = "https://open.tiktokapis.com/v2";

/**
 * @param {object} p
 * @param {string} p.handle
 * @param {string} [p.accessToken]
 * @returns {Promise<string>}
 */
async function fetchTikTokHandlePublicSummary({handle, accessToken = ""}) {
  const normalized = normalizeTikTokHandle(handle);
  if (!normalized) {
    return "";
  }
  const apiToken = String(accessToken || "").trim();
  const profileUrl = `https://www.tiktok.com/@${encodeURIComponent(normalized)}`;
  if (apiToken) {
    const apiBlock = await fetchTikTokAuthorizedUserSummary({accessToken: apiToken, expectedHandle: normalized});
    if (apiBlock) {
      return apiBlock;
    }
  }
  try {
    const html = await fetchHtml(profileUrl);
    if (!html) {
      return buildTikTokHandleOnlyFallback(normalized);
    }
    const fromNext = parseFromNextData(html);
    if (fromNext) {
      return formatTikTokBlock(fromNext, profileUrl);
    }
    const fromUniversal = parseFromUniversalData(html);
    if (fromUniversal) {
      return formatTikTokBlock(fromUniversal, profileUrl);
    }
    const fromSigiState = parseFromSigiState(html);
    if (fromSigiState) {
      return formatTikTokBlock(fromSigiState, profileUrl);
    }
    const fromOg = parseFromOpenGraph(html);
    if (fromOg) {
      return formatTikTokBlock(fromOg, profileUrl);
    }
    return buildTikTokHandleOnlyFallback(normalized);
  } catch {
    return buildTikTokHandleOnlyFallback(normalized);
  }
}

module.exports = {
  fetchTikTokAuthorizedUserSummary,
  fetchTikTokHandlePublicSummary,
};

/**
 * @param {object} p
 * @param {string} p.accessToken
 * @param {string} [p.expectedHandle]
 * @returns {Promise<string>}
 */
async function fetchTikTokAuthorizedUserSummary({accessToken, expectedHandle = ""}) {
  const t = String(accessToken || "").trim();
  if (!t) {
    return "";
  }
  const fields = [
    "open_id",
    "union_id",
    "avatar_url",
    "display_name",
    "username",
    "follower_count",
    "following_count",
    "likes_count",
    "video_count",
    "bio_description",
    "is_verified",
    "profile_deep_link",
  ].join(",");
  const url = `${TIKTOK_OPEN_API_BASE}/user/info/?fields=${encodeURIComponent(fields)}`;
  const c = new AbortController();
  const timer = setTimeout(() => c.abort(), 12_000);
  try {
    const res = await fetch(url, {
      method: "GET",
      signal: c.signal,
      headers: {
        "Authorization": `Bearer ${t}`,
      },
    });
    if (!res.ok) {
      return "";
    }
    const j = await res.json();
    const user = j?.data?.user;
    if (!user || typeof user !== "object") {
      return "";
    }
    const handle = normalizeTikTokHandle(user.username || "");
    const expected = normalizeTikTokHandle(expectedHandle);
    const mismatchNote = expected && handle && expected !== handle ?
      `Hinweis: Angefragter Handle @${expected} weicht vom verknuepften TikTok-Account @${handle} ab.` :
      "";
    const lines = [
      "TikTok Open API (autorisierter Creator, Stand Abfrage):",
      handle ? `Handle: @${handle}` : null,
      nonEmpty(user.display_name) ? `Name: ${nonEmpty(user.display_name)}` : null,
      nonEmpty(user.bio_description) ? `Bio: ${trimText(user.bio_description, 500)}` : null,
      user.follower_count != null ? `Follower (ca.): ${user.follower_count}` : null,
      user.following_count != null ? `Following: ${user.following_count}` : null,
      user.likes_count != null ? `Likes (gesamt, ca.): ${user.likes_count}` : null,
      user.video_count != null ? `Videos (Zaehler): ${user.video_count}` : null,
      user.is_verified != null ? `Verifiziert: ${user.is_verified ? "ja" : "nein"}` : null,
      nonEmpty(user.profile_deep_link) ? `Profil: ${nonEmpty(user.profile_deep_link)}` : null,
      mismatchNote || null,
    ].filter(Boolean);
    return lines.join("\n");
  } catch {
    return "";
  } finally {
    clearTimeout(timer);
  }
}

function normalizeTikTokHandle(raw) {
  const s = String(raw || "").trim();
  if (!s) {
    return "";
  }
  const noAt = s.replace(/^@+/, "");
  const fromUrl = noAt.match(/tiktok\.com\/@([a-zA-Z0-9._]+)/i);
  const base = (fromUrl ? fromUrl[1] : noAt).split(/[/?#]/)[0];
  return base.replace(/[^a-zA-Z0-9._]/g, "").slice(0, 40);
}

async function fetchHtml(url) {
  const c = new AbortController();
  const t = setTimeout(() => c.abort(), 12_000);
  try {
    const res = await fetch(url, {
      method: "GET",
      signal: c.signal,
      headers: {
        "User-Agent": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
        "Accept-Language": "en-US,en;q=0.9,de;q=0.8",
      },
    });
    if (!res.ok) {
      return "";
    }
    return await res.text();
  } finally {
    clearTimeout(t);
  }
}

function parseFromNextData(html) {
  const raw = extractJsonScript(html, "__NEXT_DATA__");
  if (!raw) {
    return null;
  }
  try {
    const j = JSON.parse(raw);
    const user =
      j?.props?.pageProps?.userInfo?.user ||
      j?.props?.pageProps?.userInfo?.stats && j?.props?.pageProps?.userInfo?.user;
    const stats = j?.props?.pageProps?.userInfo?.stats || {};
    if (!user || typeof user !== "object") {
      return null;
    }
    return {
      username: nonEmpty(user.uniqueId || user.nickname || ""),
      name: nonEmpty(user.nickname || ""),
      bio: nonEmpty(user.signature || ""),
      followersCount: toNum(stats.followerCount),
      followingCount: toNum(stats.followingCount),
      likesCount: toNum(stats.heartCount),
      videosCount: toNum(stats.videoCount),
    };
  } catch {
    return null;
  }
}

function parseFromUniversalData(html) {
  const raw = extractJsonScript(html, "__UNIVERSAL_DATA_FOR_REHYDRATION__");
  if (!raw) {
    return null;
  }
  try {
    const j = JSON.parse(raw);
    const detail =
      j?.__DEFAULT_SCOPE__?.["webapp.user-detail"] ||
      j?.["webapp.user-detail"] ||
      null;
    return parseTikTokUserDetail(detail);
  } catch {
    return null;
  }
}

function parseFromSigiState(html) {
  const raw = extractJsonScript(html, "SIGI_STATE");
  if (!raw) {
    return null;
  }
  try {
    const j = JSON.parse(raw);
    const users = j?.UserModule?.users || {};
    const statsById = j?.UserModule?.stats || {};
    const firstUser = Object.values(users).find((u) => u && typeof u === "object");
    if (!firstUser) {
      return null;
    }
    const stats = statsById[firstUser.id] || {};
    return formatTikTokParsedUser(firstUser, stats);
  } catch {
    return null;
  }
}

function parseTikTokUserDetail(detail) {
  const userInfo = detail?.userInfo || detail?.data?.userInfo || null;
  const user = userInfo?.user || null;
  const stats = userInfo?.statsV2 || userInfo?.stats || {};
  if (!user || typeof user !== "object") {
    return null;
  }
  return formatTikTokParsedUser(user, stats);
}

function formatTikTokParsedUser(user, stats = {}) {
  const username = nonEmpty(user.uniqueId || user.username || user.nickname || "");
  if (!username) {
    return null;
  }
  return {
    username,
    name: nonEmpty(user.nickname || user.display_name || ""),
    bio: nonEmpty(user.signature || user.bio_description || ""),
    followersCount: toNum(stats.followerCount ?? stats.follower_count),
    followingCount: toNum(stats.followingCount ?? stats.following_count),
    likesCount: toNum(stats.heartCount ?? stats.heart ?? stats.likes_count),
    videosCount: toNum(stats.videoCount ?? stats.video_count),
  };
}

function parseFromOpenGraph(html) {
  const title = extractMeta(html, "property", "og:title");
  const description = extractMeta(html, "property", "og:description");
  if (!title && !description) {
    return null;
  }
  const usernameMatch = (title || "").match(/@([a-zA-Z0-9._]+)/);
  return {
    username: nonEmpty(usernameMatch ? usernameMatch[1] : ""),
    name: nonEmpty(title || ""),
    bio: nonEmpty(description || ""),
    followersCount: null,
    followingCount: null,
    likesCount: null,
    videosCount: null,
  };
}

function extractJsonScript(html, id) {
  const re = new RegExp(`<script[^>]*id=["']${escapeRegExp(id)}["'][^>]*>([\\s\\S]*?)<\\/script>`, "i");
  const m = String(html || "").match(re);
  if (!m) {
    return "";
  }
  return decodeHtmlEntities(String(m[1] || "").trim());
}

function extractMeta(html, attrName, attrValue) {
  const re = new RegExp(`<meta[^>]*${attrName}=["']${escapeRegExp(attrValue)}["'][^>]*content=["']([^"']*)["'][^>]*>`, "i");
  const m = html.match(re);
  return m ? String(m[1] || "").trim() : "";
}

function formatTikTokBlock(d, profileUrl) {
  const parts = [
    "TikTok (oeffentliches Profil, Stand Abfrage):",
    d.username ? `Handle: @${d.username}` : null,
    d.name && d.name !== d.username ? `Name: ${d.name}` : null,
    d.bio ? `Bio: ${trimText(d.bio, 500)}` : null,
    d.followersCount != null ? `Follower (ca.): ${d.followersCount}` : null,
    d.followingCount != null ? `Following: ${d.followingCount}` : null,
    d.likesCount != null ? `Likes (gesamt, ca.): ${d.likesCount}` : null,
    d.videosCount != null ? `Videos (Zaehler): ${d.videosCount}` : null,
    `Profil: ${profileUrl}`,
  ].filter(Boolean);
  return parts.join("\n");
}

function buildTikTokHandleOnlyFallback(handle) {
  return [
    "TikTok (ohne verwertbaren Profil-Snapshot):",
    `Handle-Bezug: @${handle}.`,
    "Kein belastbarer oeffentlicher Profilabruf moeglich (Rate-Limit, Region, Anti-Bot oder Profil eingeschraenkt).",
  ].join(" ");
}

function toNum(v) {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
}

function nonEmpty(v) {
  const s = String(v || "").trim();
  return s;
}

function trimText(v, maxLen) {
  return String(v || "").replace(/\s+/g, " ").trim().slice(0, maxLen);
}

function escapeRegExp(v) {
  return String(v).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
}

function decodeHtmlEntities(value) {
  return String(value || "")
      .replace(/&quot;/g, "\"")
      .replace(/&#34;/g, "\"")
      .replace(/&amp;/g, "&")
      .replace(/&lt;/g, "<")
      .replace(/&gt;/g, ">");
}
