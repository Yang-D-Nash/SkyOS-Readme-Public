"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const {assertAppCheck} = require("../src/security/app-check");
const {APP_CHECK_MODES, DEFAULT_RUNTIME_CONFIG} = require("../src/security/constants");

test("default runtime config enforces App Check", () => {
  assert.equal(DEFAULT_RUNTIME_CONFIG.appCheckMode, APP_CHECK_MODES.enforce);
  assert.equal(DEFAULT_RUNTIME_CONFIG.appCheckAiSoftFailEnabled, false);

  assert.throws(
      () => assertAppCheck({auth: {uid: "user-1"}}, DEFAULT_RUNTIME_CONFIG, "generateAiText"),
      (error) => error.code === "failed-precondition",
  );
});

test("explicit monitor mode keeps missing App Check observable but non-blocking", () => {
  const state = assertAppCheck(
      {auth: {uid: "user-1"}},
      {...DEFAULT_RUNTIME_CONFIG, appCheckMode: APP_CHECK_MODES.monitor},
      "generateAiText",
  );

  assert.deepEqual(state, {
    appId: null,
    mode: APP_CHECK_MODES.monitor,
    missing: true,
  });
});

test("AI soft-fail only applies when explicitly enabled", () => {
  const state = assertAppCheck(
      {auth: {uid: "user-1"}},
      {
        ...DEFAULT_RUNTIME_CONFIG,
        appCheckMode: APP_CHECK_MODES.enforce,
        appCheckAiSoftFailEnabled: true,
      },
      "generateAiText",
  );

  assert.deepEqual(state, {
    appId: null,
    mode: APP_CHECK_MODES.enforce,
    missing: true,
    softFailed: true,
  });
});

test("valid App Check app id is accepted in enforce mode", () => {
  const state = assertAppCheck(
      {
        app: {appId: "1:123:web:abc"},
        auth: {uid: "user-1"},
      },
      DEFAULT_RUNTIME_CONFIG,
      "generateAiText",
  );

  assert.deepEqual(state, {
    appId: "1:123:web:abc",
    mode: APP_CHECK_MODES.enforce,
    missing: false,
  });
});
