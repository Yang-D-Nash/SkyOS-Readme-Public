/**
 * Zusatzkontext fuer createFounderBriefing: Merch, System/Notifications, Musik-Snippets.
 * Musik: oeffentliche iTunes Search API (wie Android MusicRepository) — kein API-Key.
 */
"use strict";

const {logger} = require("firebase-functions");

const DEFAULT_MUSIC_ARTISTS = Object.freeze([
  "JANNO",
  "Yang D. Nash",
  "ThaDude",
  "MAVE",
  "TANGAJOE007",
]);

const ITUNES_TIMEOUT_MS = 8_000;

function nonEmptyString(v) {
  if (v === null || v === undefined) {
    return "";
  }
  const s = String(v).trim();
  return s;
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
    const items = results.filter((r) => r && r.trackName && r.artistName);
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
  };

  if (!uid) {
    return empty;
  }

  let configArtists = null;
  try {
    const cfgSnap = await firestore.collection("appConfig").doc("founderBriefing").get();
    if (cfgSnap.exists) {
      const d = cfgSnap.data() || {};
      const arr = d.musicArtists || d.featuredArtists;
      if (Array.isArray(arr) && arr.length) {
        configArtists = arr
            .map((a) => nonEmptyString(a))
            .filter(Boolean)
            .slice(0, 8);
      }
    }
  } catch (e) {
    logger.warn("founder_briefing.appConfig_founderBriefing_read_failed", {
      err: e instanceof Error ? e.message : String(e),
    });
  }
  const artists = (configArtists && configArtists.length) ? configArtists : [...DEFAULT_MUSIC_ARTISTS];

  const [merchSnapshot, runtimeSnapshot, ordersSnapshot] = await Promise.all([
    firestore.collection("merchandise").limit(64).get().catch(() => ({empty: true, docs: []})),
    firestore.collection("system").doc("runtimeConfig").get().catch(() => null),
    firestore.collection("orders").orderBy("timestamp", "desc").limit(8).get().catch(() => ({empty: true, docs: []})),
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
    "music: itunes.apple.com/search (public)",
  ];

  return {
    businessDate: nonEmptyString(businessDate) || "",
    music: music.sort((l, r) => (l.releaseDate || "").localeCompare(r.releaseDate || "")).reverse(),
    musicErrors,
    merch: productRows,
    systemLines,
    orderLines,
    sources,
  };
}

module.exports = {
  loadFounderBriefingEnrichment,
  DEFAULT_MUSIC_ARTISTS,
};
