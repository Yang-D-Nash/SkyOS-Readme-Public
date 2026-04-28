"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");

const indexSource = fs.readFileSync(path.join(__dirname, "..", "index.js"), "utf8");

function callableBlocks() {
  const exportPattern = /exports\.([A-Za-z0-9_]+)\s*=\s*onCall\s*\(/g;
  const matches = [...indexSource.matchAll(exportPattern)];
  return matches.map((match, index) => {
    const next = matches[index + 1];
    return {
      name: match[1],
      body: indexSource.slice(match.index, next ? next.index : indexSource.length),
    };
  });
}

test("all callable functions run the shared App Check gate", () => {
  const missing = callableBlocks()
      .filter(({body}) => !body.includes("assertCallableSecurity(request"))
      .map(({name}) => name);

  assert.deepEqual(missing, []);
});

test("validateManusApiKey requires an authenticated caller", () => {
  const block = callableBlocks().find(({name}) => name === "validateManusApiKey");

  assert.ok(block, "validateManusApiKey callable should exist");
  assert.match(block.body, /assertAuthenticatedUser\(\s*request\.auth/);
});

test("triggerWorkflowAutomation enforces owner for owner scope", () => {
  const block = callableBlocks().find(({name}) => name === "triggerWorkflowAutomation");

  assert.ok(block, "triggerWorkflowAutomation callable should exist");
  assert.match(
      block.body,
      /if\s*\(\s*automationScope\s*===\s*"owner"\s*\)\s*\{\s*await\s+assertOwner\(request\.auth\);/s,
  );
});

test("skydownAgent blocks non-owner global owner flow scope", () => {
  const block = callableBlocks().find(({name}) => name === "skydownAgent");

  assert.ok(block, "skydownAgent callable should exist");
  assert.match(block.body, /const globalOwnerFlowRequiresOwner = selectedAutomationScope === "owner"/);
  assert.match(block.body, /callerRole !== USER_ROLES\.owner/);
});

test("external workflow webhook URL is validated before fetch", () => {
  assert.match(indexSource, /function assertAutomationWebhookUrlAllowed\(/);
  assert.match(indexSource, /assertAutomationWebhookUrlAllowed\(webhookUrl,\s*runtimePolicy\)/);
  assert.match(indexSource, /parsedUrl\.protocol !== "https:"/);
});

test("skydownAgent enforces idempotency read/write guard for automation triggers", () => {
  const block = callableBlocks().find(({name}) => name === "skydownAgent");

  assert.ok(block, "skydownAgent callable should exist");
  assert.match(indexSource, /const AUTOMATION_IDEMPOTENCY_TTL_MS = 60 \* 60 \* 1000/);
  assert.match(block.body, /loadRecentAutomationIdempotencyRecord\(/);
  assert.match(block.body, /if\s*\(idempotencyKeyUsable\)/);
  assert.match(block.body, /if\s*\(idempotencyKeyUsable\s*&&\s*fresh\s*&&\s*fresh\.triggered === true\)/);
  assert.match(block.body, /saveAutomationIdempotencyRecord\(/);
});

test("agentRunStatusCallback supports requestId fallback correlation", () => {
  assert.match(indexSource, /exports\.agentRunStatusCallback\s*=\s*onRequest\(/);
  assert.match(indexSource, /if\s*\(\(!uid\s*\|\|\s*!runId\)\s*&&\s*!requestId\)/);
  assert.match(indexSource, /resolveAgentRunRef\(\{uid,\s*runId,\s*requestId\}\)/);
  assert.match(indexSource, /collectionGroup\("agentRuns"\)\s*\.where\("automationRequestId",\s*"==",\s*normalizedRequestId\)/);
});

test("FAQ prompts include published AI Studio owner knowledge", () => {
  assert.match(indexSource, /const AI_STUDIO_FAQ_KNOWLEDGE_DOCUMENT = "aiStudioFaqKnowledge"/);
  assert.match(indexSource, /async function loadPublishedAiStudioFaqKnowledge\(\)/);
  assert.match(indexSource, /publishedOwnerFaqKnowledge/);
  assert.match(indexSource, /composeFaqKnowledge\(promptSettings,\s*publishedOwnerFaqKnowledge\)/);
});

test("triggerWorkflowAutomation keeps personal scope owner-check free", () => {
  const block = callableBlocks().find(({name}) => name === "triggerWorkflowAutomation");

  assert.ok(block, "triggerWorkflowAutomation callable should exist");
  assert.match(block.body, /const automationScope = .*=== "personal" \? "personal" : "owner"/);
  assert.match(block.body, /if\s*\(automationScope === "owner"\)\s*\{\s*await assertOwner\(request\.auth\);\s*\}/s);
  assert.doesNotMatch(block.body, /if\s*\(automationScope === "personal"\)\s*\{\s*await assertOwner\(request\.auth\);\s*\}/s);
});
