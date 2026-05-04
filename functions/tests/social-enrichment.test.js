"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");

const {fetchSpotifyHandleCatalogSummary} = require("../src/agent/spotify-enrichment");
const {resolveInstagramContextForAgent} = require("../src/agent/meta-instagram-enrichment");
const {resolveFacebookPageContextForAgent} = require("../src/agent/meta-facebook-enrichment");
const {fetchTikTokHandlePublicSummary} = require("../src/agent/tiktok-enrichment");

function jsonResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => body,
    text: async () => JSON.stringify(body),
  };
}

function textResponse(status, body) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => JSON.parse(body),
    text: async () => body,
  };
}

test("TikTok enrichment reads current universal user-detail payload", async () => {
  const originalFetch = global.fetch;
  global.fetch = async () => textResponse(200, [
    "<!doctype html><html><head></head><body>",
    "<script id=\"__UNIVERSAL_DATA_FOR_REHYDRATION__\" type=\"application/json\">",
    JSON.stringify({
      __DEFAULT_SCOPE__: {
        "webapp.user-detail": {
          userInfo: {
            user: {
              uniqueId: "realartist",
              nickname: "Real Artist",
              signature: "Release week.",
              verified: true,
            },
            statsV2: {
              followerCount: "12345",
              followingCount: "7",
              heartCount: "99000",
              videoCount: "42",
            },
          },
        },
      },
    }),
    "</script></body></html>",
  ].join(""));

  try {
    const summary = await fetchTikTokHandlePublicSummary({handle: "@realartist"});
    assert.match(summary, /TikTok \(oeffentliches Profil/);
    assert.match(summary, /Handle: @realartist/);
    assert.match(summary, /Follower \(ca\.\): 12345/);
    assert.doesNotMatch(summary, /ohne verwertbaren Profil-Snapshot/);
  } finally {
    global.fetch = originalFetch;
  }
});

test("Instagram enrichment reports expired Meta token without inventing data", async () => {
  const originalFetch = global.fetch;
  global.fetch = async () => jsonResponse(400, {
    error: {
      message: "Error validating access token: Session has expired.",
      type: "OAuthException",
      code: 190,
      error_subcode: 463,
    },
  });

  try {
    const summary = await resolveInstagramContextForAgent({
      handle: "@realartist",
      accessToken: "expired-token",
      igUserId: "17841400000000000",
      graphVersion: "v20.0",
    });
    assert.match(summary, /Instagram Graph API ist serverseitig konfiguriert/);
    assert.match(summary, /Meta Access Token ist abgelaufen/);
    assert.match(summary, /Keine erfundenen Impressions/);
  } finally {
    global.fetch = originalFetch;
  }
});

test("Facebook enrichment keeps handle-only fallback when no Page token is configured", async () => {
  const summary = await resolveFacebookPageContextForAgent({
    handle: "zweizwei",
    accessToken: "",
    pageId: "",
    graphVersion: "v20.0",
  });
  assert.match(summary, /Facebook\/Meta \(ohne verwertbaren Graph-API-Zugriff\)/);
  assert.match(summary, /@zweizwei/);
  assert.match(summary, /Keine erfundenen Reichweiten/);
});

test("Spotify enrichment falls back to public oEmbed metadata when Web API is premium-blocked", async () => {
  const originalFetch = global.fetch;
  global.fetch = async (url) => {
    const href = String(url);
    if (href.includes("accounts.spotify.com/api/token")) {
      return jsonResponse(200, {access_token: "access", expires_in: 3600});
    }
    if (href.includes("/v1/artists/")) {
      return jsonResponse(403, {
        error: {
          message: "Active premium subscription required for the owner of the app.",
        },
      });
    }
    if (href.includes("open.spotify.com/oembed")) {
      return jsonResponse(200, {
        title: "Real Artist",
        thumbnail_url: "https://example.test/artist.jpg",
      });
    }
    return jsonResponse(404, {});
  };

  try {
    const summary = await fetchSpotifyHandleCatalogSummary({
      clientId: "client",
      clientSecret: "secret",
      handle: "https://open.spotify.com/artist/1234567890abcdef",
    });
    assert.match(summary, /Spotify oEmbed/);
    assert.match(summary, /Kuenstler: Real Artist/);
    assert.match(summary, /Keine erfundenen Follower/);
  } finally {
    global.fetch = originalFetch;
  }
});
