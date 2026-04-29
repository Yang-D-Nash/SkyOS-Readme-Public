"use strict";

/**
 * Lädt öffentliche Katalog-Daten (Artist-Profil, oberer Snapshot) für den Agent-Workspace.
 * Verwendet OAuth "client_credentials" (nur serverseitig, Client ID + Client Secret).
 * @see https://developer.spotify.com/documentation/web-api
 */

const SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
const SPOTIFY_API = "https://api.spotify.com/v1";

const tokenCache = {token: "", expMs: 0};

/**
 * @param {object} opts
 * @param {string} opts.clientId
 * @param {string} opts.clientSecret
 * @param {string} opts.handle Normalisierter Handle, Suchstring, Künstler-URL oder spotify:artist:ID
 * @param {string} [opts.market=DE]
 * @returns {Promise<string>} Kurz-Text für LLM oder leer
 */
async function fetchSpotifyHandleCatalogSummary({clientId, clientSecret, handle, market = "DE"}) {
  const h = (handle || "").trim();
  if (!h || !clientId || !clientSecret) {
    return "";
  }

  try {
    const access = await getClientAccessToken(clientId, clientSecret);
    const parsed = parseArtistLookup(h);
    let artistId = null;
    if (parsed.id) {
      artistId = parsed.id;
    } else if (parsed.query) {
      const q = encodeURIComponent(parsed.query);
      const sUrl = `${SPOTIFY_API}/search?q=${q}&type=artist&limit=3&market=${encodeURIComponent(market)}`;
      const sRes = await spotifyRequest(sUrl, access);
      if (sRes.status === 404 || sRes.status === 429) {
        return "";
      }
      if (!sRes.ok) {
        return "";
      }
      const sJ = await sRes.json();
      const items = sJ?.artists?.items || [];
      if (!items.length) {
        return "Spotify (Katalogsuche): Kein passendes Künstlerprofil gefunden. Bitte Handle oder " +
        "Künstlername prüfen.";
      }
      artistId = items[0].id;
    } else {
      return "";
    }

    const aUrl = `${SPOTIFY_API}/artists/${encodeURIComponent(artistId)}`;
    const tUrl = `${SPOTIFY_API}/artists/${encodeURIComponent(artistId)}/top-tracks?market=${encodeURIComponent(market)}`;
    const [aRes, tRes] = await Promise.all([
      spotifyRequest(aUrl, access),
      spotifyRequest(tUrl, access),
    ]);
    if (!aRes.ok) {
      return "";
    }
    const a = await aRes.json();
    const tracksJ = tRes.ok ? await tRes.json() : {tracks: []};
    const tracks = (tracksJ.tracks || []).slice(0, 5)
        .map((t) => t?.name)
        .filter(Boolean);
    const parts = [
      `Künstler: ${a.name || "—"}`,
      a.id ? `Spotify-URI: spotify:artist:${a.id}` : null,
      a.followers && typeof a.followers.total === "number" ?
        `Follower (Katalog, ca.): ${a.followers.total}` : null,
      a.popularity != null ? `Popularitäts-Index: ${a.popularity}/100` : null,
      Array.isArray(a.genres) && a.genres.length ? `Genres: ${a.genres.join(", ")}` : null,
      tracks.length ? `Beispiel-Top-Tracks: ${tracks.join(" · ")}` : null,
    ].filter(Boolean);

    return `Spotify Web API (öffentliches Katalog-Profil, Stand Abfrage):\n${parts.join("\n")}`;
  } catch {
    return "";
  }
}

module.exports = {fetchSpotifyHandleCatalogSummary};

async function getClientAccessToken(clientId, clientSecret) {
  const now = Date.now();
  if (tokenCache.token && now < tokenCache.expMs - 60_000) {
    return tokenCache.token;
  }
  const body = "grant_type=client_credentials";
  const auth = Buffer.from(`${clientId}:${clientSecret}`, "utf8").toString("base64");
  const res = await fetchWithTimeout(
      SPOTIFY_TOKEN_URL,
      {
        method: "POST",
        headers: {
          "Content-Type": "application/x-www-form-urlencoded",
          "Authorization": `Basic ${auth}`,
        },
        body,
      },
      12_000,
  );
  if (!res.ok) {
    throw new Error("spotify_token");
  }
  const j = await res.json();
  const token = j.access_token || "";
  const expIn = (Number(j.expires_in) || 3600) * 1000;
  tokenCache.token = token;
  tokenCache.expMs = now + expIn;
  return token;
}

/**
 * @param {string} raw
 * @returns {{ id: string, query: string } | { id: null, query: string } | { id: null, query: null }}
 */
function parseArtistLookup(raw) {
  const s = raw.trim();
  if (!s) {
    return {id: null, query: null};
  }
  if (/^spotify:artist:/i.test(s)) {
    const id = s.split(":").pop() || "";
    if (id) {
      return {id, query: null};
    }
  }
  if (/open\.spotify\.com\/artist\//i.test(s)) {
    const m = s.match(/\/artist\/([0-9A-Za-z]+)/i);
    if (m) {
      return {id: m[1], query: null};
    }
  }
  if (/^[0-9A-Za-z]{15,32}$/i.test(s) && s.length < 64) {
    return {id: s, query: null};
  }
  return {id: null, query: s};
}

function spotifyRequest(url, accessToken) {
  return fetchWithTimeout(url, {
    headers: {Authorization: `Bearer ${accessToken}`},
  }, 12_000);
}

function fetchWithTimeout(url, init, timeoutMs) {
  const c = new AbortController();
  const t = setTimeout(() => c.abort(), timeoutMs);
  return fetch(url, {...init, signal: c.signal})
      .finally(() => {
        clearTimeout(t);
      });
}
