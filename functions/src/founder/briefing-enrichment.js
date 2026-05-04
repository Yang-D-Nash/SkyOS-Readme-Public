/**
 * Zusatzkontext fuer createFounderBriefing: Merch, System/Notifications, Musik-Snippets.
 * Musik: oeffentliche iTunes Search API (wie Android MusicRepository) — kein API-Key.
 */
"use strict";

const {logger} = require("firebase-functions");

const ITUNES_TIMEOUT_MS = 8_000;
const MAX_FOUNDER_MUSIC_ARTISTS = 8;

function nonEmptyString(v) {
  if (v === null || v === undefined) {
    return "";
  }
  const s = String(v).trim();
  return s;
}

function normalizedArtistIdentity(value) {
  return nonEmptyString(value)
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, "");
}

function artistNamesMatch(expected, actual) {
  const expectedKey = normalizedArtistIdentity(expected);
  const actualKey = normalizedArtistIdentity(actual);
  if (!expectedKey || !actualKey) {
    return false;
  }
  if (expectedKey === actualKey) {
    return true;
  }
  return expectedKey.length >= 8 && actualKey.includes(expectedKey);
}

function uniqueArtistNames(values) {
  const seen = new Set();
  const artists = [];
  for (const value of Array.isArray(values) ? values : []) {
    const artistName = nonEmptyString(value);
    const key = normalizedArtistIdentity(artistName);
    if (!artistName || !key || seen.has(key)) {
      continue;
    }
    seen.add(key);
    artists.push(artistName);
  }
  return artists;
}

function canonicalArtistNameFromPage(documentId, data) {
  const storedName = nonEmptyString(data?.artistName);
  const brand = nonEmptyString(data?.brand).toLowerCase();
  if (brand === "nicma") {
    if (documentId === "nicma-nicma-music") {
      return "NICMA MUSIC";
    }
    if (documentId === "nicma-nicma-studio") {
      return "NICMA STUDIO";
    }
  }
  return storedName;
}

function timestampLikeMillis(value) {
  if (!value) {
    return 0;
  }
  if (typeof value === "number" && Number.isFinite(value)) {
    return value > 10_000_000_000 ? value : value * 1000;
  }
  if (typeof value === "string") {
    const parsed = Date.parse(value);
    return Number.isFinite(parsed) ? parsed : 0;
  }
  if (typeof value.toMillis === "function") {
    return value.toMillis();
  }
  if (typeof value.toDate === "function") {
    const date = value.toDate();
    return date && Number.isFinite(date.getTime()) ? date.getTime() : 0;
  }
  if (typeof value._seconds === "number") {
    return value._seconds * 1000;
  }
  return 0;
}

function resolveBriefingUserRole(data) {
  const role = nonEmptyString(data?.role).toLowerCase();
  if (role) {
    return role;
  }
  return data?.isAdmin === true ? "admin" : "user";
}

function safeBriefingUserLabel(data, fallbackId = "") {
  const direct = nonEmptyString(data?.username) ||
    nonEmptyString(data?.displayName) ||
    nonEmptyString(data?.name);
  if (direct) {
    return direct;
  }
  const email = nonEmptyString(data?.email);
  if (email && email.includes("@")) {
    return email.split("@")[0];
  }
  return nonEmptyString(fallbackId) || "User";
}

function buildFounderUserSnapshot(userSnapshot) {
  const empty = {
    roleCounts: {},
    activeAiSubscriptions: 0,
    recentUsers: [],
    totalUserDocs: 0,
  };
  if (!userSnapshot || userSnapshot.empty || !Array.isArray(userSnapshot.docs)) {
    return empty;
  }

  const roleCounts = {};
  let activeAiSubscriptions = 0;
  const recentUsers = [];
  for (const doc of userSnapshot.docs) {
    const data = doc.data() || {};
    const role = resolveBriefingUserRole(data);
    roleCounts[role] = (roleCounts[role] || 0) + 1;
    const subscriptionStatus = nonEmptyString(data.aiSubscriptionStatus).toLowerCase();
    if (["active", "trialing", "past_due", "unpaid"].includes(subscriptionStatus)) {
      activeAiSubscriptions += 1;
    }
    const updatedMillis = Math.max(
        timestampLikeMillis(data.lastSignInAt),
        timestampLikeMillis(data.lastLoginAt),
        timestampLikeMillis(data.updatedAt),
        timestampLikeMillis(data.registrationDateEpochMillis),
        timestampLikeMillis(data.registrationDate),
    );
    recentUsers.push({
      label: safeBriefingUserLabel(data, doc.id),
      role,
      quotaPlan: nonEmptyString(data.quotaPlan) || nonEmptyString(data.aiSubscriptionPlan) || "free",
      aiStatus: subscriptionStatus || "inactive",
      updatedMillis,
    });
  }

  recentUsers.sort((left, right) => right.updatedMillis - left.updatedMillis);
  return {
    roleCounts,
    activeAiSubscriptions,
    recentUsers: recentUsers.slice(0, 6),
    totalUserDocs: userSnapshot.docs.length,
  };
}

async function loadFounderBriefingConfiguredArtists(firestore) {
  try {
    const cfgSnap = await firestore.collection("appConfig").doc("founderBriefing").get();
    if (!cfgSnap.exists) {
      return {
        artists: [],
        includeConfiguredWithArtistPages: false,
      };
    }
    const d = cfgSnap.data() || {};
    const arr = d.musicArtists || d.featuredArtists;
    return {
      artists: uniqueArtistNames(Array.isArray(arr) ? arr : []).slice(0, MAX_FOUNDER_MUSIC_ARTISTS),
      includeConfiguredWithArtistPages: d.includeConfiguredArtists === true,
    };
  } catch (e) {
    logger.warn("founder_briefing.appConfig_founderBriefing_read_failed", {
      err: e instanceof Error ? e.message : String(e),
    });
    return {
      artists: [],
      includeConfiguredWithArtistPages: false,
    };
  }
}

async function loadFounderBriefingArtistPageArtists(firestore) {
  try {
    const snap = await firestore.collection("artistPages").limit(64).get();
    if (!snap || snap.empty) {
      return [];
    }
    return uniqueArtistNames(
        snap.docs
            .map((doc) => canonicalArtistNameFromPage(doc.id, doc.data() || {}))
            .filter(Boolean),
    ).slice(0, MAX_FOUNDER_MUSIC_ARTISTS);
  } catch (e) {
    logger.warn("founder_briefing.artistPages_read_failed", {
      err: e instanceof Error ? e.message : String(e),
    });
    return [];
  }
}

/**
 * @param {string} artist
 * @return {Promise<null|{artistName: string, trackName: string, collection: string, releaseDate: string, url: string}>}
 */
async function fetchLatestItunesTrackForArtist(artist) {
  const term = nonEmptyString(artist);
  if (!term) {
    return null;
  }
  const url = `https://itunes.apple.com/search?term=${encodeURIComponent(term)}&media=music&entity=song&limit=8`;
  const ac = new AbortController();
  const t = setTimeout(() => ac.abort(), ITUNES_TIMEOUT_MS);
  try {
    const res = await fetch(url, {signal: ac.signal});
    if (!res.ok) {
      return null;
    }
    const json = await res.json();
    const results = Array.isArray(json?.results) ? json.results : [];
    const items = results.filter((r) =>
      r &&
      r.trackName &&
      r.artistName &&
      artistNamesMatch(term, r.artistName),
    );
    if (items.length === 0) {
      return null;
    }
    items.sort((a, b) => {
      const da = Date.parse(a.releaseDate || "") || 0;
      const db = Date.parse(b.releaseDate || "") || 0;
      return db - da;
    });
    const best = items[0];
    return {
      queryArtist: term,
      artistName: nonEmptyString(best.artistName) || term,
      trackName: nonEmptyString(best.trackName) || "Track",
      collection: nonEmptyString(best.collectionName) || "",
      releaseDate: nonEmptyString(best.releaseDate) || "",
      url: nonEmptyString(best.trackViewUrl) || nonEmptyString(best.collectionViewUrl) || "",
    };
  } catch (err) {
    logger.warn("founder_briefing.itunes_search_failed", {
      artist: term,
      err: err instanceof Error ? err.message : String(err),
    });
    return null;
  } finally {
    clearTimeout(t);
  }
}

/**
 * @param {import("firebase-admin").firestore.Firestore} firestore
 * @param {{ uid: string, businessDate: string }} ctx
 * @return {Promise<object>}
 */
async function loadFounderBriefingEnrichment(firestore, {uid, businessDate}) {
  const empty = {
    businessDate: nonEmptyString(businessDate) || "",
    music: [],
    musicErrors: 0,
    merch: [],
    systemLines: [],
    orderLines: [],
    sources: [],
    musicArtists: [],
    musicArtistSource: "none",
    users: buildFounderUserSnapshot(null),
  };

  if (!uid) {
    return empty;
  }

  const [configuredArtistSettings, artistPageArtists] = await Promise.all([
    loadFounderBriefingConfiguredArtists(firestore),
    loadFounderBriefingArtistPageArtists(firestore),
  ]);
  const configArtists = configuredArtistSettings.artists || [];
  const artists = artistPageArtists.length ?
    uniqueArtistNames([
      ...artistPageArtists,
      ...(configuredArtistSettings.includeConfiguredWithArtistPages ? configArtists : []),
    ]).slice(0, MAX_FOUNDER_MUSIC_ARTISTS) :
    configArtists.slice(0, MAX_FOUNDER_MUSIC_ARTISTS);
  const musicArtistSource = artistPageArtists.length ?
    "artistPages" :
    (configArtists.length ? "appConfig/founderBriefing" : "none");

  const [merchSnapshot, runtimeSnapshot, ordersSnapshot, usersSnapshot] = await Promise.all([
    firestore.collection("merchandise").limit(64).get().catch(() => ({empty: true, docs: []})),
    firestore.collection("system").doc("runtimeConfig").get().catch(() => null),
    firestore.collection("orders").orderBy("timestamp", "desc").limit(8).get().catch(() => ({empty: true, docs: []})),
    firestore.collection("users").limit(200).get().catch(() => ({empty: true, docs: []})),
  ]);

  /** @type {import("firebase-admin").firestore.QuerySnapshot|{empty: boolean, docs: never[]}} */
  const mSnap = merchSnapshot;
  const productRows = [];
  if (mSnap && !mSnap.empty && Array.isArray(mSnap.docs)) {
    const tsMillis = (doc) => {
      const v = doc.get("updatedAt");
      return v && typeof v.toMillis === "function" ? v.toMillis() : 0;
    };
    const sortedDocs = [...mSnap.docs].sort((a, b) => tsMillis(b) - tsMillis(a));
    for (const doc of sortedDocs) {
      const p = doc.data() || {};
      if (p.shopifySyncActive === false) {
        continue;
      }
      const forSale = p.availableForSale !== false && p.available !== false;
      if (!forSale) {
        continue;
      }
      const name = nonEmptyString(p.name) || "Produkt";
      const price = Number(p.price);
      const cur = nonEmptyString(p.currency) || "EUR";
      const line = Number.isFinite(price) && price > 0 ? `${name} — ${price.toFixed(2)} ${cur}` : name;
      const extra = [nonEmptyString(p.category) ? p.category : "", nonEmptyString(p.collabPartner) ? p.collabPartner : ""]
          .filter(Boolean);
      productRows.push(extra.length ? `${line} (${extra.join(" · ")})` : line);
      if (productRows.length >= 10) {
        break;
      }
    }
  }

  const systemLines = [];
  if (runtimeSnapshot && runtimeSnapshot.exists) {
    const rc = runtimeSnapshot.data() || {};
    if (rc.lockdown === true) {
      systemLines.push("Sicherheitsmodus: Lockdown aktiv — oeffentlich sichtbare Aktionen kurz pruefen.");
    }
    if (rc.uploadsEnabled === false) {
      systemLines.push("Uploads sind in der App gedrosselt (Runtime-Flag).");
    }
    if (rc.userWritesEnabled === false) {
      systemLines.push("Schnellaktionen und User-Writes sind eingeschraenkt (Runtime-Flag).");
    }
    if (rc.registrationsEnabled === false) {
      systemLines.push("Registrierungen derzeit geschlossen (Runtime-Flag).");
    }
  }

  const orderLines = [];
  if (ordersSnapshot && !ordersSnapshot.empty) {
    for (const odoc of ordersSnapshot.docs) {
      const o = odoc.data() || {};
      const pay = String(o.paymentStatus || "").toLowerCase();
      const full = String(o.fulfillmentStatus || "").toLowerCase();
      const tot = Number(o.totalAmount) || 0;
      const cur = nonEmptyString(o.currency) || "EUR";
      const t = o.timestamp;
      const dateObj = t && typeof t.toDate === "function" ? t.toDate() : null;
      const when = dateObj ? dateObj.toISOString().slice(0, 10) : "";
      const idTail = odoc.id.length > 4 ? odoc.id.slice(-6) : odoc.id;
      const line = `Ref. ${idTail} — Zahlung: ${pay || "offen"} — Versand: ${full || "—"} — ${tot.toFixed(2)} ${cur}` +
        (when ? ` · ${when}` : "");
      orderLines.push(line);
      if (orderLines.length >= 3) {
        break;
      }
    }
  }

  const music = [];
  let musicErrors = 0;
  const seenMusic = new Set();
  await Promise.all(artists.map(async (a) => {
    const row = await fetchLatestItunesTrackForArtist(a);
    if (row) {
      const key = `${row.artistName}|${row.trackName}`.toLowerCase();
      if (!seenMusic.has(key)) {
        seenMusic.add(key);
        music.push(row);
      }
    } else {
      musicErrors += 1;
    }
  }));

  const sources = [
    "merch: firestore/merchandise (aktive Produkte)",
    "system: system/runtimeConfig",
    "orders: orders (letzte, fuer Owner-Operationsblick)",
    "users: firestore/users (Rollen, AI-Abo-Signale, letzte User-Dokumente)",
    artists.length ?
      `music: artist source ${musicArtistSource}; itunes.apple.com/search exact artist match` :
      "music: keine hinterlegten Artist-Namen gefunden",
  ];

  return {
    businessDate: nonEmptyString(businessDate) || "",
    music: music.sort((l, r) => (l.releaseDate || "").localeCompare(r.releaseDate || "")).reverse(),
    musicErrors,
    merch: productRows,
    systemLines,
    orderLines,
    sources,
    musicArtists: artists,
    musicArtistSource,
    users: buildFounderUserSnapshot(usersSnapshot),
  };
}

module.exports = {
  loadFounderBriefingEnrichment,
  artistNamesMatch,
  uniqueArtistNames,
};
