"use strict";

const admin = require("firebase-admin");
const {HttpsError} = require("firebase-functions/v2/https");
const {
  OWNER_EMAIL,
  USER_QUOTA_PLANS,
  USER_ROLES,
  VALID_ROLES,
} = require("./constants");

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function normalizeEmail(email) {
  return nonEmptyString(email)?.toLowerCase() || null;
}

function isAuthUserNotFound(error) {
  return error?.code === "auth/user-not-found";
}

function resolveRoleFromClaims(authOrToken) {
  const token = authOrToken?.token || authOrToken || {};
  const email = normalizeEmail(token.email);
  if (email === OWNER_EMAIL) {
    return USER_ROLES.owner;
  }

  const rawRole = nonEmptyString(token.role)?.toLowerCase();
  return VALID_ROLES.includes(rawRole) ? rawRole : USER_ROLES.user;
}

function roleHasOwnerAccess(role) {
  return role === USER_ROLES.owner;
}

function roleHasAdminAccess(role) {
  return role === USER_ROLES.owner || role === USER_ROLES.admin;
}

function roleHasModerationAccess(role) {
  return roleHasAdminAccess(role);
}

function defaultQuotaPlanForRole(role) {
  switch (role) {
    case USER_ROLES.owner:
      return USER_QUOTA_PLANS.ownerUnlimited;
    case USER_ROLES.admin:
      return USER_QUOTA_PLANS.creator;
    case USER_ROLES.subadmin:
      return USER_QUOTA_PLANS.creator;
    case USER_ROLES.user:
    default:
      return USER_QUOTA_PLANS.free;
  }
}

function defaultAiLimitsForQuotaPlan(plan) {
  switch (plan) {
    case USER_QUOTA_PLANS.ownerUnlimited:
      return {text: 5000, visual: 1200, agent: 3000, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.internalTeam:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.creator:
      return {text: 120, visual: 20, agent: 70, historyRetentionDays: 7};
    case USER_QUOTA_PLANS.studio:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_QUOTA_PLANS.free:
    default:
      return {text: 30, visual: 4, agent: 18, historyRetentionDays: 3};
  }
}

function defaultAiLimitsForRole(role) {
  return defaultAiLimitsForQuotaPlan(defaultQuotaPlanForRole(role));
}

function buildRoleClaims(role) {
  return {
    role,
    isAdmin: roleHasAdminAccess(role),
    isStaff: roleHasModerationAccess(role),
    isOwner: roleHasOwnerAccess(role),
  };
}

async function resolveRequestedRoleForSync(auth) {
  const fallbackRole = resolveRoleFromClaims(auth);
  if (!auth?.uid) {
    return fallbackRole;
  }

  const snapshot = await admin.firestore().doc(`users/${auth.uid}`).get();
  if (!snapshot.exists) {
    return fallbackRole;
  }

  const data = snapshot.data() || {};
  const roleFromDoc = nonEmptyString(data.role)?.toLowerCase();
  if (VALID_ROLES.includes(roleFromDoc)) {
    return roleFromDoc;
  }

  if (data.isAdmin === true) {
    return USER_ROLES.admin;
  }

  return fallbackRole;
}

async function setUserRoleClaims({
  uid,
  requestedRole,
  updatedByUid = null,
  updatedByEmail = null,
}) {
  const normalizedUid = nonEmptyString(uid);
  if (!normalizedUid) {
    throw new HttpsError("invalid-argument", "uid fehlt.");
  }

  const normalizedRole = nonEmptyString(requestedRole)?.toLowerCase();
  if (!VALID_ROLES.includes(normalizedRole)) {
    throw new HttpsError("invalid-argument", "Ungueltige Rolle.");
  }

  const resolvedTarget = await resolveManagedAuthTarget(normalizedUid);
  const userRecord = resolvedTarget.userRecord;
  const targetUid = resolvedTarget.uid;
  const email = normalizeEmail(userRecord.email);
  const finalRole = email === OWNER_EMAIL ? USER_ROLES.owner : normalizedRole;

  if (normalizedRole === USER_ROLES.owner && email !== OWNER_EMAIL) {
    throw new HttpsError("permission-denied", "Nur das feste Owner-Konto darf die Owner-Rolle tragen.");
  }

  await admin.auth().setCustomUserClaims(targetUid, buildRoleClaims(finalRole));

  const userDocRef = admin.firestore().doc(`users/${targetUid}`);
  const existingSnapshot = await userDocRef.get();
  const canonicalData = existingSnapshot.exists ? (existingSnapshot.data() || {}) : {};
  const existingData = resolvedTarget.staleData ?
    {...resolvedTarget.staleData, ...canonicalData} :
    canonicalData;
  const migrationCarryover = buildMigrationCarryover(resolvedTarget.staleData, canonicalData);
  const existingQuotaPlan = nonEmptyString(existingData.quotaPlan)?.toLowerCase();
  const selfPaidPlans = [USER_QUOTA_PLANS.creator, USER_QUOTA_PLANS.studio];
  const quotaPlan = finalRole === USER_ROLES.owner ?
    USER_QUOTA_PLANS.ownerUnlimited :
    [USER_ROLES.admin, USER_ROLES.subadmin].includes(finalRole) &&
    selfPaidPlans.includes(existingQuotaPlan) ?
      existingQuotaPlan :
      defaultQuotaPlanForRole(finalRole);
  const defaults = defaultAiLimitsForQuotaPlan(quotaPlan);
  const textLimit = Number(existingData.aiTextRequestsPerDay);
  const visualLimit = Number(existingData.aiVisualRequestsPerDay);
  const agentLimit = Number(existingData.aiAgentRequestsPerDay);
  const historyRetentionDays = Number(existingData.aiHistoryRetentionDays);

  await userDocRef.set({
    ...migrationCarryover,
    email: email || admin.firestore.FieldValue.delete(),
    role: finalRole,
    isAdmin: roleHasAdminAccess(finalRole),
    quotaPlan,
    aiAccessEnabled: existingData.aiAccessEnabled !== false,
    aiTextRequestsPerDay: Number.isFinite(textLimit) && textLimit > 0 ? Math.floor(textLimit) : defaults.text,
    aiVisualRequestsPerDay: Number.isFinite(visualLimit) && visualLimit > 0 ? Math.floor(visualLimit) : defaults.visual,
    aiAgentRequestsPerDay: Number.isFinite(agentLimit) && agentLimit > 0 ? Math.floor(agentLimit) : defaults.agent,
    aiHistoryRetentionDays: [1, 3, 7, 30].includes(historyRetentionDays) ?
      historyRetentionDays :
      defaults.historyRetentionDays,
    canManageMusicCatalog: roleHasOwnerAccess(finalRole) ?
      true :
      finalRole === USER_ROLES.admin ? existingData.canManageMusicCatalog === true : false,
    canManageVideoCatalog: roleHasOwnerAccess(finalRole) ?
      true :
      finalRole === USER_ROLES.admin ? existingData.canManageVideoCatalog === true : false,
    canModerateProfiles: roleHasOwnerAccess(finalRole) ?
      true :
      finalRole === USER_ROLES.admin ? existingData.canModerateProfiles === true : false,
    claimsSyncedAt: admin.firestore.FieldValue.serverTimestamp(),
    claimsUpdatedByUid: updatedByUid || admin.firestore.FieldValue.delete(),
    claimsUpdatedByEmail: updatedByEmail || admin.firestore.FieldValue.delete(),
  }, {merge: true});

  if (resolvedTarget.migratedFromUid) {
    await admin.firestore().doc(`users/${resolvedTarget.migratedFromUid}`).set({
      accountStatus: "migrated",
      mergedIntoUid: targetUid,
      migratedAt: admin.firestore.FieldValue.serverTimestamp(),
      migratedByUid: updatedByUid || admin.firestore.FieldValue.delete(),
      migratedByEmail: updatedByEmail || admin.firestore.FieldValue.delete(),
    }, {merge: true});
  }

  return {
    uid: targetUid,
    requestedUid: normalizedUid,
    migratedFromUid: resolvedTarget.migratedFromUid,
    email,
    role: finalRole,
    claims: buildRoleClaims(finalRole),
  };
}

async function resolveManagedAuthTarget(uid) {
  try {
    const userRecord = await admin.auth().getUser(uid);
    return {
      uid,
      userRecord,
      migratedFromUid: null,
      staleData: null,
    };
  } catch (error) {
    if (!isAuthUserNotFound(error)) {
      throw error;
    }
  }

  const staleUserRef = admin.firestore().doc(`users/${uid}`);
  const staleSnapshot = await staleUserRef.get();
  if (!staleSnapshot.exists) {
    throw new HttpsError("not-found", "Konto nicht gefunden.");
  }

  const staleData = staleSnapshot.data() || {};
  const staleEmail = normalizeEmail(staleData.email);
  if (!staleEmail) {
    throw new HttpsError("not-found", "Konto nicht gefunden.");
  }

  let userRecord;
  try {
    userRecord = await admin.auth().getUserByEmail(staleEmail);
  } catch (error) {
    if (isAuthUserNotFound(error)) {
      throw new HttpsError("not-found", "Konto nicht gefunden.");
    }
    throw error;
  }

  return {
    uid: userRecord.uid,
    userRecord,
    migratedFromUid: userRecord.uid !== uid ? uid : null,
    staleData,
  };
}

function buildMigrationCarryover(staleData, canonicalData) {
  if (!staleData) {
    return {};
  }

  const carryover = {};
  const carryoverFields = [
    "username",
    "whatsApp",
    "profileImageURL",
    "profileImagePath",
    "profileTagline",
    "profileBio",
    "instagramHandle",
    "registrationDate",
    "registrationDateEpochMillis",
  ];

  for (const field of carryoverFields) {
    if (canonicalData[field] === undefined && staleData[field] !== undefined) {
      carryover[field] = staleData[field];
    }
  }

  return carryover;
}

async function syncClaimsForCurrentUser(auth) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Bitte melde dich an.");
  }

  const role = await resolveRequestedRoleForSync(auth);
  const synced = await setUserRoleClaims({
    uid: auth.uid,
    requestedRole: role,
    updatedByUid: auth.uid,
    updatedByEmail: normalizeEmail(auth.token?.email),
  });

  return {
    role: synced.role,
    claims: synced.claims,
  };
}

function assertOwner(auth) {
  if (!auth?.uid || !roleHasOwnerAccess(resolveRoleFromClaims(auth))) {
    throw new HttpsError("permission-denied", "Nur der Owner darf diese Aktion ausfuehren.");
  }
}

function assertAdmin(auth) {
  if (!auth?.uid || !roleHasAdminAccess(resolveRoleFromClaims(auth))) {
    throw new HttpsError("permission-denied", "Nur Admins duerfen diese Aktion ausfuehren.");
  }
}

module.exports = {
  assertAdmin,
  assertOwner,
  defaultAiLimitsForRole,
  defaultAiLimitsForQuotaPlan,
  normalizeEmail,
  resolveRoleFromClaims,
  roleHasAdminAccess,
  roleHasModerationAccess,
  roleHasOwnerAccess,
  setUserRoleClaims,
  syncClaimsForCurrentUser,
  defaultQuotaPlanForRole,
};
