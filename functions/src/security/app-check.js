"use strict";

const logger = require("firebase-functions/logger");
const {HttpsError} = require("firebase-functions/v2/https");
const {APP_CHECK_MODES} = require("./constants");

const AUTHENTICATED_APP_CHECK_FALLBACK_FUNCTIONS = new Set([
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

  if (mode === APP_CHECK_MODES.enforce) {
    if (request.auth?.uid && AUTHENTICATED_APP_CHECK_FALLBACK_FUNCTIONS.has(functionName)) {
      logger.warn("App Check enforce fallback active for authenticated AI callable.", {
        functionName,
        uid: request.auth.uid,
      });
      return {appId: null, mode, missing: true, fallback: "authenticated_ai_callable"};
    }

    throw new HttpsError("failed-precondition", "App Check Validierung fehlt.");
  }

  return {appId: null, mode, missing: true};
}

module.exports = {
  assertAppCheck,
};
