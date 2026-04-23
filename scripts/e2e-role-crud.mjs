#!/usr/bin/env node

const API_KEY = process.env.SKYDOWN_FIREBASE_WEB_API_KEY || "";
const PROJECT_ID = "skydown-a6add";
const REGION = "us-central1";
const FIRESTORE_BASE = `https://firestore.googleapis.com/v1/projects/${PROJECT_ID}/databases/(default)/documents`;

const OWNER_EMAIL = process.env.SKYDOWN_OWNER_EMAIL || "";
const OWNER_PASSWORD = process.env.SKYDOWN_OWNER_PASSWORD || "";

function requireOwnerCredentials() {
  if (!OWNER_EMAIL || !OWNER_PASSWORD) {
    throw new Error("Missing SKYDOWN_OWNER_EMAIL or SKYDOWN_OWNER_PASSWORD.");
  }
  if (!API_KEY) {
    throw new Error("Missing SKYDOWN_FIREBASE_WEB_API_KEY.");
  }
}

async function postJson(url, payload, headers = {}) {
  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...headers,
    },
    body: JSON.stringify(payload),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    const message =
      body?.error?.message ||
      body?.error?.status ||
      `HTTP ${response.status}`;
    throw new Error(`${url} failed: ${message}`);
  }
  return body;
}

async function signUp(email, password) {
  const url = `https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${API_KEY}`;
  return postJson(url, {
    email,
    password,
    returnSecureToken: true,
  });
}

async function signIn(email, password) {
  const url = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`;
  return postJson(url, {
    email,
    password,
    returnSecureToken: true,
  });
}

async function deleteAccount(idToken) {
  const url = `https://identitytoolkit.googleapis.com/v1/accounts:delete?key=${API_KEY}`;
  return postJson(url, {idToken});
}

async function callCallable(name, idToken, data = {}) {
  const url = `https://${REGION}-${PROJECT_ID}.cloudfunctions.net/${name}`;
  const body = await postJson(
    url,
    {data},
    {
      Authorization: `Bearer ${idToken}`,
    },
  );

  if (body?.error) {
    const message =
      body.error.message ||
      body.error.status ||
      JSON.stringify(body.error);
    throw new Error(`${name} callable failed: ${message}`);
  }
  return body?.result ?? body;
}

function firestorePatchBodyForRole(role) {
  const isAdmin = role === "owner" || role === "admin";
  const quotaPlan =
    role === "owner" ? "owner_unlimited" :
    role === "admin" ? "internal_team" :
    role === "subadmin" ? "creator" :
    "free";
  const aiTextRequestsPerDay =
    role === "owner" ? 5000 :
    role === "admin" ? 240 :
    role === "subadmin" ? 120 :
    30;
  const aiVisualRequestsPerDay =
    role === "owner" ? 1200 :
    role === "admin" ? 40 :
    role === "subadmin" ? 20 :
    4;
  const aiAgentRequestsPerDay =
    role === "owner" ? 3000 :
    role === "admin" ? 140 :
    role === "subadmin" ? 70 :
    18;
  const aiHistoryRetentionDays =
    role === "owner" || role === "admin" ? 30 :
    role === "subadmin" ? 7 :
    3;

  return {
    fields: {
      role: {stringValue: role},
      isAdmin: {booleanValue: isAdmin},
      quotaPlan: {stringValue: quotaPlan},
      aiTextRequestsPerDay: {integerValue: String(aiTextRequestsPerDay)},
      aiVisualRequestsPerDay: {integerValue: String(aiVisualRequestsPerDay)},
      aiAgentRequestsPerDay: {integerValue: String(aiAgentRequestsPerDay)},
      aiHistoryRetentionDays: {integerValue: String(aiHistoryRetentionDays)},
      canManageMusicCatalog: {booleanValue: role === "admin"},
      canManageVideoCatalog: {booleanValue: role === "admin"},
      canModerateProfiles: {booleanValue: role === "admin"},
      aiAccessEnabled: {booleanValue: true},
    },
  };
}

async function firestoreGetUserDoc(uid, idToken) {
  const url = `${FIRESTORE_BASE}/users/${uid}`;
  const response = await fetch(url, {
    headers: {Authorization: `Bearer ${idToken}`},
  });
  if (response.status === 404) {
    return null;
  }
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(`Firestore get users/${uid} failed: ${body?.error?.message || response.status}`);
  }
  return body;
}

async function waitForUserDoc(uid, idToken, timeoutMs = 30000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const doc = await firestoreGetUserDoc(uid, idToken);
    if (doc) return;
    await new Promise((r) => setTimeout(r, 1000));
  }
  throw new Error(`users/${uid} bootstrap document was not created in time.`);
}

async function patchRoleViaFirestore(uid, role, idToken) {
  const params = new URLSearchParams();
  for (const field of [
    "role",
    "isAdmin",
    "quotaPlan",
    "aiTextRequestsPerDay",
    "aiVisualRequestsPerDay",
    "aiAgentRequestsPerDay",
    "aiHistoryRetentionDays",
    "canManageMusicCatalog",
    "canManageVideoCatalog",
    "canModerateProfiles",
    "aiAccessEnabled",
  ]) {
    params.append("updateMask.fieldPaths", field);
  }

  const url = `${FIRESTORE_BASE}/users/${uid}?${params.toString()}`;
  const response = await fetch(url, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${idToken}`,
    },
    body: JSON.stringify(firestorePatchBodyForRole(role)),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(`Firestore patch users/${uid} failed: ${body?.error?.message || response.status}`);
  }
}

function buildTempAccounts() {
  const stamp = Date.now();
  const random = Math.random().toString(36).slice(2, 8);
  const pass = `Tmp-${stamp}-${random}!`;
  return {
    admin: {
      email: `qa.admin.${stamp}.${random}@example.com`,
      password: pass,
      role: "admin",
    },
    subadmin: {
      email: `qa.subadmin.${stamp}.${random}@example.com`,
      password: pass,
      role: "subadmin",
    },
    user: {
      email: `qa.user.${stamp}.${random}@example.com`,
      password: pass,
      role: "user",
    },
  };
}

async function setup() {
  requireOwnerCredentials();
  const ownerAuth = await signIn(OWNER_EMAIL, OWNER_PASSWORD);
  const ownerToken = ownerAuth.idToken;
  const accounts = buildTempAccounts();

  for (const account of Object.values(accounts)) {
    const created = await signUp(account.email, account.password);
    account.uid = created.localId;
  }

  for (const account of Object.values(accounts)) {
    await waitForUserDoc(account.uid, ownerToken);
    try {
      await callCallable("setUserRole", ownerToken, {
        uid: account.uid,
        role: account.role,
      });
    } catch (error) {
      // App Check can block callable access in scripts; use Firestore owner write fallback.
      await patchRoleViaFirestore(account.uid, account.role, ownerToken);
    }
  }

  const payload = {
    owner: {
      email: OWNER_EMAIL,
      password: OWNER_PASSWORD,
    },
    admin: accounts.admin,
    subadmin: accounts.subadmin,
    user: accounts.user,
  };

  process.stdout.write(`${JSON.stringify(payload, null, 2)}\n`);
}

async function cleanup(jsonPath) {
  if (!jsonPath) {
    throw new Error("cleanup requires a json path.");
  }
  const fs = await import("node:fs/promises");
  const raw = await fs.readFile(jsonPath, "utf8");
  const payload = JSON.parse(raw);
  const entries = [
    payload.admin,
    payload.subadmin,
    payload.user,
  ].filter(Boolean);

  for (const account of entries) {
    try {
      const auth = await signIn(account.email, account.password);
      try {
        await callCallable("deleteCurrentUserAccount", auth.idToken, {});
      } catch {
        await deleteAccount(auth.idToken);
      }
    } catch (error) {
      process.stderr.write(`cleanup warning for ${account.email}: ${error.message}\n`);
    }
  }
}

async function main() {
  const mode = process.argv[2];
  try {
    if (mode === "setup") {
      await setup();
      return;
    }
    if (mode === "cleanup") {
      await cleanup(process.argv[3]);
      return;
    }
    throw new Error("Usage: e2e-role-crud.mjs setup|cleanup <jsonPath>");
  } catch (error) {
    process.stderr.write(`${error.message}\n`);
    process.exit(1);
  }
}

await main();
