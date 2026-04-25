"use strict";

const admin = require("firebase-admin");
const logger = require("firebase-functions/logger");
const {
  APP_CHECK_MODES,
  DEFAULT_RUNTIME_CONFIG,
  RUNTIME_CONFIG_PATH,
} = require("./constants");
const {roleHasOwnerAccess} = require("./roles");

let runtimeConfigCache = {
  value: DEFAULT_RUNTIME_CONFIG,
  expiresAt: 0,
};

function sanitizeRuntimeConfig(data = {}) {
  const appCheckMode = typeof data.appCheckMode === "string" &&
      Object.values(APP_CHECK_MODES).includes(data.appCheckMode)
    ? data.appCheckMode
    : DEFAULT_RUNTIME_CONFIG.appCheckMode;

  return {
    lockdown: data.lockdown === true,
    uploadsEnabled: data.uploadsEnabled !== false,
    registrationsEnabled: data.registrationsEnabled !== false,
    userWritesEnabled: data.userWritesEnabled !== false,
    appCheckMode,
    appCheckAiSoftFailEnabled: data.appCheckAiSoftFailEnabled === true,
    budgetLockdownEnabled: data.budgetLockdownEnabled === true,
    lastLockdownReason: typeof data.lastLockdownReason === "string" ? data.lastLockdownReason : "",
  };
}

async function getRuntimeConfig({forceRefresh = false} = {}) {
  if (!forceRefresh && runtimeConfigCache.expiresAt > Date.now()) {
    return runtimeConfigCache.value;
  }

  const snapshot = await admin.firestore().doc(RUNTIME_CONFIG_PATH).get();
  const value = sanitizeRuntimeConfig(snapshot.data() || {});
  runtimeConfigCache = {
    value,
    expiresAt: Date.now() + 15_000,
  };
  return value;
}

function invalidateRuntimeConfigCache() {
  runtimeConfigCache = {
    value: DEFAULT_RUNTIME_CONFIG,
    expiresAt: 0,
  };
}

async function mergeRuntimeConfig(update, source = "manual_update") {
  await admin.firestore().doc(RUNTIME_CONFIG_PATH).set({
    ...update,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedBy: source,
  }, {merge: true});
  invalidateRuntimeConfigCache();
  return getRuntimeConfig({forceRefresh: true});
}

function isLockdownActive(runtimeConfig, role) {
  return runtimeConfig.lockdown === true && !roleHasOwnerAccess(role);
}

function areUserWritesBlocked(runtimeConfig, role) {
  return isLockdownActive(runtimeConfig, role) ||
    (runtimeConfig.userWritesEnabled === false && !roleHasOwnerAccess(role));
}

function areUploadsBlocked(runtimeConfig, role) {
  return isLockdownActive(runtimeConfig, role) ||
    (runtimeConfig.uploadsEnabled === false && !roleHasOwnerAccess(role));
}

function areRegistrationsBlocked(runtimeConfig, role = "user") {
  return isLockdownActive(runtimeConfig, role) ||
    (runtimeConfig.registrationsEnabled === false && !roleHasOwnerAccess(role));
}

function logRuntimeConfig(runtimeConfig) {
  logger.info("Runtime config evaluated.", runtimeConfig);
}

module.exports = {
  areRegistrationsBlocked,
  areUploadsBlocked,
  areUserWritesBlocked,
  getRuntimeConfig,
  invalidateRuntimeConfigCache,
  logRuntimeConfig,
  mergeRuntimeConfig,
  sanitizeRuntimeConfig,
};
