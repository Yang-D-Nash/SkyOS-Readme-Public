"use strict";

const admin = require("firebase-admin");
const {HttpsError} = require("firebase-functions/v2/https");
const {
  OWNER_EMAIL,
  USER_ROLES,
  VALID_ROLES,
} = require("./constants");

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function normalizeEmail(email) {
  return nonEmptyString(email)?.toLowerCase() || null;
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
  return roleHasAdminAccess(role) || role === USER_ROLES.subadmin;
}

function defaultAiLimitsForRole(role) {
  switch (role) {
    case USER_ROLES.owner:
      return {text: 400, visual: 80, agent: 250, historyRetentionDays: 30};
    case USER_ROLES.admin:
      return {text: 240, visual: 40, agent: 140, historyRetentionDays: 30};
    case USER_ROLES.subadmin:
      return {text: 120, visual: 20, agent: 70, historyRetentionDays: 7};
    case USER_ROLES.user:
    default:
      return {text: 30, visual: 4, agent: 18, historyRetentionDays: 3};
  }
}

function buildRoleClaims(role) {
  return {
    role,
    isAdmin: roleHasAdminAccess(role),
    isStaff: roleHasModerationAccess(role),
    isOwner: roleHasOwnerAccess(role),
  };
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

  const userRecord = await admin.auth().getUser(normalizedUid);
  const email = normalizeEmail(userRecord.email);
  const finalRole = email === OWNER_EMAIL ? USER_ROLES.owner : normalizedRole;

  if (normalizedRole === USER_ROLES.owner && email !== OWNER_EMAIL) {
    throw new HttpsError("permission-denied", "Nur das feste Owner-Konto darf die Owner-Rolle tragen.");
  }

  await admin.auth().setCustomUserClaims(normalizedUid, buildRoleClaims(finalRole));

  const defaults = defaultAiLimitsForRole(finalRole);
  await admin.firestore().doc(`users/${normalizedUid}`).set({
    email: email || admin.firestore.FieldValue.delete(),
    role: finalRole,
    isAdmin: roleHasAdminAccess(finalRole),
    aiAccessEnabled: true,
    aiTextRequestsPerDay: defaults.text,
    aiVisualRequestsPerDay: defaults.visual,
    aiAgentRequestsPerDay: defaults.agent,
    aiHistoryRetentionDays: defaults.historyRetentionDays,
    claimsSyncedAt: admin.firestore.FieldValue.serverTimestamp(),
    claimsUpdatedByUid: updatedByUid || admin.firestore.FieldValue.delete(),
    claimsUpdatedByEmail: updatedByEmail || admin.firestore.FieldValue.delete(),
  }, {merge: true});

  return {
    uid: normalizedUid,
    email,
    role: finalRole,
    claims: buildRoleClaims(finalRole),
  };
}

async function syncClaimsForCurrentUser(auth) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Bitte melde dich an.");
  }

  const role = resolveRoleFromClaims(auth);
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
  normalizeEmail,
  resolveRoleFromClaims,
  roleHasAdminAccess,
  roleHasModerationAccess,
  roleHasOwnerAccess,
  setUserRoleClaims,
  syncClaimsForCurrentUser,
};
