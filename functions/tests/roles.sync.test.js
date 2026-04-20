"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const Module = require("node:module");

function loadRolesWithMockedAdmin(mockAdmin) {
  const rolesPath = require.resolve("../src/security/roles");
  delete require.cache[rolesPath];

  const originalLoad = Module._load;
  Module._load = function(request, parent, isMain) {
    if (request === "firebase-admin") {
      return mockAdmin;
    }

    return originalLoad.call(this, request, parent, isMain);
  };

  try {
    return require("../src/security/roles");
  } finally {
    Module._load = originalLoad;
  }
}

function createAdminMock({
  userByUid,
  uidByEmail,
  initialDocs,
}) {
  const documents = new Map(Object.entries(initialDocs || {}));
  const claimsWrites = [];

  const firestore = () => ({
    doc(path) {
      return {
        async get() {
          const data = documents.get(path);
          return {
            exists: data != null,
            data: () => data || undefined,
          };
        },
        async set(payload, options = {}) {
          const existing = documents.get(path) || {};
          documents.set(path, options.merge ? {...existing, ...payload} : payload);
        },
      };
    },
  });

  firestore.FieldValue = {
    serverTimestamp() {
      return "__SERVER_TIMESTAMP__";
    },
    delete() {
      return "__DELETE__";
    },
  };

  return {
    auth() {
      return {
        async getUser(uid) {
          if (!Object.prototype.hasOwnProperty.call(userByUid, uid)) {
            const error = new Error(`missing mocked auth user for ${uid}`);
            error.code = "auth/user-not-found";
            throw error;
          }

          return {
            uid,
            email: userByUid[uid],
          };
        },
        async getUserByEmail(email) {
          if (!Object.prototype.hasOwnProperty.call(uidByEmail || {}, email)) {
            const error = new Error(`missing mocked auth user for ${email}`);
            error.code = "auth/user-not-found";
            throw error;
          }

          const uid = uidByEmail[email];
          return {
            uid,
            email,
          };
        },
        async setCustomUserClaims(uid, claims) {
          claimsWrites.push({uid, claims});
        },
      };
    },
    firestore,
    _state: {
      claimsWrites,
      documents,
    },
  };
}

async function syncWithMock({
  userByUid,
  uidByEmail,
  initialDocs,
  auth,
}) {
  const adminMock = createAdminMock({userByUid, uidByEmail, initialDocs});
  const roles = loadRolesWithMockedAdmin(adminMock);

  const result = await roles.syncClaimsForCurrentUser(auth);
  return {
    result,
    claimsWrites: adminMock._state.claimsWrites,
    documents: adminMock._state.documents,
  };
}

test("syncCurrentUserClaims nimmt Firestore-Rolle statt veraltetem Token", async () => {
  const outcome = await syncWithMock({
    userByUid: {
      "alice-uid": "alice@example.com",
    },
    initialDocs: {
      "users/alice-uid": {
        email: "alice@example.com",
        role: "admin",
        isAdmin: true,
        quotaPlan: "internal_team",
        aiAccessEnabled: true,
        aiTextRequestsPerDay: 240,
        aiVisualRequestsPerDay: 40,
        aiAgentRequestsPerDay: 140,
        aiHistoryRetentionDays: 30,
        canManageMusicCatalog: true,
        canManageVideoCatalog: false,
        canModerateProfiles: true,
      },
    },
    auth: {
      uid: "alice-uid",
      token: {
        email: "alice@example.com",
        role: "user",
      },
    },
  });

  assert.equal(outcome.result.role, "admin");
  assert.deepEqual(outcome.result.claims, {
    role: "admin",
    isAdmin: true,
    isStaff: true,
    isOwner: false,
  });

  assert.equal(outcome.claimsWrites.length, 1);
  assert.deepEqual(outcome.claimsWrites[0], {
    uid: "alice-uid",
    claims: {
      role: "admin",
      isAdmin: true,
      isStaff: true,
      isOwner: false,
    },
  });

  const syncedUser = outcome.documents.get("users/alice-uid");
  assert.equal(syncedUser.role, "admin");
  assert.equal(syncedUser.isAdmin, true);
});

test("syncCurrentUserClaims nutzt Token-Rolle als Fallback ohne User-Dokument", async () => {
  const outcome = await syncWithMock({
    userByUid: {
      "bob-uid": "bob@example.com",
    },
    initialDocs: {},
    auth: {
      uid: "bob-uid",
      token: {
        email: "bob@example.com",
        role: "subadmin",
      },
    },
  });

  assert.equal(outcome.result.role, "subadmin");
  assert.equal(outcome.claimsWrites.length, 1);
  assert.equal(outcome.claimsWrites[0].claims.role, "subadmin");
});

test("syncCurrentUserClaims kann das feste Owner-Konto nicht downgraden", async () => {
  const outcome = await syncWithMock({
    userByUid: {
      "owner-uid": "nash.lioncorna@gmail.com",
    },
    initialDocs: {
      "users/owner-uid": {
        email: "nash.lioncorna@gmail.com",
        role: "user",
        isAdmin: false,
        quotaPlan: "free",
        aiAccessEnabled: true,
        aiTextRequestsPerDay: 30,
        aiVisualRequestsPerDay: 4,
        aiAgentRequestsPerDay: 18,
        aiHistoryRetentionDays: 3,
        canManageMusicCatalog: false,
        canManageVideoCatalog: false,
        canModerateProfiles: false,
      },
    },
    auth: {
      uid: "owner-uid",
      token: {
        email: "nash.lioncorna@gmail.com",
        role: "user",
      },
    },
  });

  assert.equal(outcome.result.role, "owner");
  assert.equal(outcome.claimsWrites.length, 1);
  assert.equal(outcome.claimsWrites[0].claims.role, "owner");
  assert.equal(outcome.documents.get("users/owner-uid").role, "owner");
});

test("setUserRoleClaims migriert veraltete UIDs auf aktiven Auth-User per E-Mail", async () => {
  const adminMock = createAdminMock({
    userByUid: {
      "active-uid": "fresh@example.com",
    },
    uidByEmail: {
      "fresh@example.com": "active-uid",
    },
    initialDocs: {
      "users/stale-uid": {
        email: "fresh@example.com",
        username: "Fresh",
        role: "user",
        isAdmin: false,
        quotaPlan: "free",
        aiAccessEnabled: true,
        aiTextRequestsPerDay: 30,
        aiVisualRequestsPerDay: 4,
        aiAgentRequestsPerDay: 18,
        aiHistoryRetentionDays: 3,
      },
    },
  });
  const roles = loadRolesWithMockedAdmin(adminMock);

  const outcome = await roles.setUserRoleClaims({
    uid: "stale-uid",
    requestedRole: "admin",
    updatedByUid: "owner-uid",
    updatedByEmail: "nash.lioncorna@gmail.com",
  });

  assert.equal(outcome.uid, "active-uid");
  assert.equal(outcome.requestedUid, "stale-uid");
  assert.equal(outcome.migratedFromUid, "stale-uid");
  assert.equal(outcome.role, "admin");
  assert.deepEqual(adminMock._state.claimsWrites[0], {
    uid: "active-uid",
    claims: {
      role: "admin",
      isAdmin: true,
      isStaff: true,
      isOwner: false,
    },
  });

  const activeUser = adminMock._state.documents.get("users/active-uid");
  assert.equal(activeUser.email, "fresh@example.com");
  assert.equal(activeUser.role, "admin");
  assert.equal(activeUser.quotaPlan, "internal_team");
  assert.equal(activeUser.username, "Fresh");

  const staleUser = adminMock._state.documents.get("users/stale-uid");
  assert.equal(staleUser.accountStatus, "migrated");
  assert.equal(staleUser.mergedIntoUid, "active-uid");
});

test("setUserRoleClaims setzt alte Admin-Defaults ohne aktives Abo auf internal_team zurueck", async () => {
  const adminMock = createAdminMock({
    userByUid: {
      "admin-uid": "admin@example.com",
    },
    initialDocs: {
      "users/admin-uid": {
        email: "admin@example.com",
        role: "admin",
        isAdmin: true,
        quotaPlan: "creator",
        aiSubscriptionPlan: "creator",
        aiSubscriptionStatus: "inactive",
        aiAccessEnabled: true,
        aiTextRequestsPerDay: 120,
        aiVisualRequestsPerDay: 20,
        aiAgentRequestsPerDay: 70,
        aiHistoryRetentionDays: 7,
      },
    },
  });
  const roles = loadRolesWithMockedAdmin(adminMock);

  await roles.setUserRoleClaims({
    uid: "admin-uid",
    requestedRole: "admin",
    updatedByUid: "owner-uid",
    updatedByEmail: "nash.lioncorna@gmail.com",
  });

  const adminUser = adminMock._state.documents.get("users/admin-uid");
  assert.equal(adminUser.quotaPlan, "internal_team");
});

test("setUserRoleClaims behaelt aktive Creator- oder Studio-Abos fuer Admins", async () => {
  const adminMock = createAdminMock({
    userByUid: {
      "admin-uid": "admin@example.com",
    },
    initialDocs: {
      "users/admin-uid": {
        email: "admin@example.com",
        role: "admin",
        isAdmin: true,
        quotaPlan: "studio",
        aiSubscriptionPlan: "studio",
        aiSubscriptionStatus: "active",
        aiAccessEnabled: true,
        aiTextRequestsPerDay: 240,
        aiVisualRequestsPerDay: 40,
        aiAgentRequestsPerDay: 140,
        aiHistoryRetentionDays: 30,
      },
    },
  });
  const roles = loadRolesWithMockedAdmin(adminMock);

  await roles.setUserRoleClaims({
    uid: "admin-uid",
    requestedRole: "admin",
    updatedByUid: "owner-uid",
    updatedByEmail: "nash.lioncorna@gmail.com",
  });

  const adminUser = adminMock._state.documents.get("users/admin-uid");
  assert.equal(adminUser.quotaPlan, "studio");
});
