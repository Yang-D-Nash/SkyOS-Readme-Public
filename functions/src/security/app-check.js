"use strict";

const logger = require("firebase-functions/logger");
const {HttpsError} = require("firebase-functions/v2/https");
const {APP_CHECK_MODES} = require("./constants");

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
    throw new HttpsError("failed-precondition", "App Check Validierung fehlt.");
  }

  return {appId: null, mode, missing: true};
}

module.exports = {
  assertAppCheck,
};
