const assert = require("node:assert/strict");
const test = require("node:test");

const {
  artistNamesMatch,
  loadFounderBriefingEnrichment,
} = require("../src/founder/briefing-enrichment");

function fakeCollectionSnapshot(docs) {
  return {
    empty: docs.length === 0,
    docs: docs.map((doc) => ({
      id: doc.id,
      data: () => doc.data || {},
      get: (field) => doc.data?.[field],
    })),
  };
}

function fakeFirestore({appConfig = null, artistPages = [], users = []} = {}) {
  return {
    collection(name) {
      if (name === "appConfig") {
        return {
          doc() {
            return {
              async get() {
                return {
                  exists: Boolean(appConfig),
                  data: () => appConfig || {},
                };
              },
            };
          },
        };
      }
      if (name === "artistPages") {
        return {
          limit() {
            return {
              async get() {
                return fakeCollectionSnapshot(artistPages);
              },
            };
          },
        };
      }
      if (name === "merchandise") {
        return {
          limit() {
            return {
              async get() {
                return fakeCollectionSnapshot([]);
              },
            };
          },
        };
      }
      if (name === "users") {
        return {
          limit() {
            return {
              async get() {
                return fakeCollectionSnapshot(users);
              },
            };
          },
        };
      }
      if (name === "system") {
        return {
          doc() {
            return {
              async get() {
                return {
                  exists: false,
                  data: () => ({}),
                };
              },
            };
          },
        };
      }
      if (name === "orders") {
        return {
          orderBy() {
            return {
              limit() {
                return {
                  async get() {
                    return fakeCollectionSnapshot([]);
                  },
                };
              },
            };
          },
        };
      }
      throw new Error(`Unexpected collection ${name}`);
    },
  };
}

test("founder briefing accepts exact artist matches only", () => {
  assert.equal(artistNamesMatch("Yang D. Nash", "Yang D Nash"), true);
  assert.equal(artistNamesMatch("TANGAJOE007", "TANGAJOE007"), true);
  assert.equal(artistNamesMatch("JANNO", "Janno Gibbs"), false);
  assert.equal(artistNamesMatch("MAVE", "Mave & Random Artist"), false);
});

test("founder briefing uses registered artistPages instead of default demo artists", async (t) => {
  const originalFetch = global.fetch;
  const fetchQueries = [];
  t.after(() => {
    global.fetch = originalFetch;
  });

  global.fetch = async (url) => {
    const parsed = new URL(url);
    const term = parsed.searchParams.get("term");
    fetchQueries.push(term);
    return {
      ok: true,
      async json() {
        if (term === "JANNO") {
          return {
            results: [{
              artistName: "JANNO",
              trackName: "Focus",
              collectionName: "Focus",
              releaseDate: "2026-01-01T00:00:00Z",
              trackViewUrl: "https://example.com/janno",
            }],
          };
        }
        return {
          results: [{
            artistName: "Random Artist",
            trackName: "Wrong Match",
            collectionName: "Wrong",
            releaseDate: "2026-01-02T00:00:00Z",
          }],
        };
      },
    };
  };

  const enrichment = await loadFounderBriefingEnrichment(fakeFirestore({
    artistPages: [
      {id: "zweizwei-janno", data: {brand: "zweizwei", artistName: "JANNO"}},
      {id: "zweizwei-yang-d-nash", data: {brand: "zweizwei", artistName: "Yang D. Nash"}},
      {id: "zweizwei-janno-copy", data: {brand: "zweizwei", artistName: "JANNO"}},
    ],
  }), {
    uid: "owner-1",
    businessDate: "2026-05-04",
  });

  assert.deepEqual(enrichment.musicArtists, ["JANNO", "Yang D. Nash"]);
  assert.equal(enrichment.musicArtistSource, "artistPages");
  assert.deepEqual(fetchQueries.sort(), ["JANNO", "Yang D. Nash"].sort());
  assert.equal(enrichment.music.length, 1);
  assert.equal(enrichment.music[0].artistName, "JANNO");
  assert.equal(enrichment.music.some((row) => row.artistName === "Random Artist"), false);
});
