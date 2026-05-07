"use strict";

const logger = require("firebase-functions/logger");
const {HttpsError} = require("firebase-functions/v2/https");
const {APP_CHECK_MODES} = require("./constants");

const APP_CHECK_SOFT_FAIL_FUNCTIONS = new Set([
  "authorizeAiUsage",
  "generateAiText",
  "generateAiVisual",
  "skydownAgent",
]);

function assertAppCheck(request, runtimeConfig, functionName) {
  const mode = runtimeConfig.appCheckMode || APP_CHECK_MODES.monitor;
  const appId = request.app?.appId || null;

  if (mode === APP_CHECK_MODES.off) {
    return {appId, mode, missing: !appId};
  }

  if (appId) {
    return {appId, mode, missing: false};
  }

  logger.warn("App Check token missing.", {
    functionName,
    mode,
    uid: request.auth?.uid || null,
  });

  const aiSoftFailEnabled = runtimeConfig.appCheckAiSoftFailEnabled === true;
  if (mode === APP_CHECK_MODES.enforce &&
    aiSoftFailEnabled &&
    APP_CHECK_SOFT_FAIL_FUNCTIONS.has(functionName)) {
    logger.warn("App Check soft-fail applied for AI callable.", {
      functionName,
      mode,
      aiSoftFailEnabled,
      uid: request.auth?.uid || null,
    });
    return {appId: null, mode, missing: true, softFailed: true};
  }

  if (mode === APP_CHECK_MODES.enforce) {
    throw new HttpsError("failed-precondition", "App Check Validierung fehlt.");
  }

  return {appId: null, mode, missing: true};
}

module.exports = {
  assertAppCheck,
};
