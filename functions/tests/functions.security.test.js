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
  assert.match(indexSource, /triggerEpochMillis/);
  assert.match(indexSource, /triggerLocalTimeBerlin/);
  assert.match(indexSource, /socialWindowHours/);
  assert.match(indexSource, /socialPlatforms/);
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
  assert.match(indexSource, /const AI_STUDIO_OWNER_INSPIRATION_DOCUMENT = "aiStudioOwnerInspiration"/);
  assert.match(indexSource, /async function loadPublishedAiStudioFaqKnowledge\(\)/);
  assert.match(indexSource, /async function loadPublishedAiStudioOwnerInspiration\(\)/);
  assert.match(indexSource, /publishedOwnerFaqKnowledge/);
  assert.match(indexSource, /publishedOwnerInspiration/);
  assert.match(indexSource, /composeFaqKnowledge\(promptSettings,\s*publishedOwnerFaqKnowledge,\s*publishedOwnerInspiration\)/);
});

test("agent workspace context includes recent agent conversation memory", () => {
  assert.match(indexSource, /const AGENT_MEMORY_RETENTION_DAYS = 30/);
  assert.match(indexSource, /collection\("aiEntries"\)\s*\.where\("source",\s*"==",\s*"agent"\)/);
  assert.match(indexSource, /where\("createdAt",\s*">=",\s*memoryRetentionStart\)/);
  assert.match(indexSource, /Memory-Agent-Verlauf:/);
  assert.match(indexSource, /recentAgentHistory/);
});

test("agent social analytics restricts request context without wiping remembered handles", () => {
  assert.match(indexSource, /const promptSelectedPlatforms = SOCIAL_PLATFORM_ORDER/);
  assert.match(indexSource, /mergeSocialProfiles\(extractedFromPrompt,\s*structuredProfiles\)/);
  assert.match(indexSource, /const socialProfilesForMemory = \{\.\.\.socialProfiles\}/);
  assert.match(indexSource, /socialSelectedPlatforms\.length > 0 \|\| promptSelectedPlatforms\.length > 0/);
  assert.match(indexSource, /persistSocialProfilesToMemoryProfile\(uid,\s*socialProfilesForMemory\)/);
  assert.match(indexSource, /socialSelectedPlatforms:\s*requiredPlatforms/);
  assert.match(indexSource, /const AGENT_SOCIAL_LIVE_CACHE_COLLECTION = "agentSocialLiveCache"/);
  assert.match(indexSource, /SOCIAL_LIVE_CACHE_TTL_MINUTES/);
  assert.match(indexSource, /const META_OAUTH_TOKEN_DOCUMENT = "metaOAuth"/);
  assert.match(indexSource, /async function resolveInstagramCredentialsForAgent\(\)/);
  assert.match(indexSource, /async function resolveFacebookCredentialsForAgent\(\)/);
  assert.match(indexSource, /loadSocialLiveSummaryCache\(\{/);
  assert.match(indexSource, /saveSocialLiveSummaryCache\(\{/);
});

test("workflow task creation deduplicates open tasks by normalized title", () => {
  assert.match(indexSource, /function upsertOpenTaskWithDedup\(/);
  assert.match(indexSource, /where\("status",\s*"==",\s*"open"\)/);
  assert.match(indexSource, /normalizeTaskDedupKey\(/);
  assert.match(indexSource, /deduplicated:\s*taskResult\.deduplicated/);
});

test("Activepieces workflow endpoints bind the documented SkyOS secret", () => {
  assert.match(indexSource, /defineSecret\("SKYOS_WORKFLOW_SECRET"\)/);
  assert.match(indexSource, /request\.headers\["x-skyos-workflow-secret"\]/);
});

test("AI generation callables apply shared burst rate limits", () => {
  const text = callableBlocks().find(({name}) => name === "generateAiText");
  const visual = callableBlocks().find(({name}) => name === "generateAiVisual");
  const agent = callableBlocks().find(({name}) => name === "skydownAgent");

  assert.ok(text, "generateAiText callable should exist");
  assert.ok(visual, "generateAiVisual callable should exist");
  assert.ok(agent, "skydownAgent callable should exist");
  assert.match(text.body, /assertAiGenerationBurstRateLimit\(/);
  assert.match(visual.body, /assertAiGenerationBurstRateLimit\(/);
  assert.match(agent.body, /assertAiGenerationBurstRateLimit\(/);
});

test("workflow automation HTTP ingest throttles per uid after auth", () => {
  assert.match(indexSource, /async function assertWorkflowHttpRequest\(/);
  assert.match(indexSource, /assertWorkflowUidBurstThrottle\(/);
});

test("agent-created productivity records use rule-compatible source values", () => {
  assert.doesNotMatch(indexSource, /source:\s*"agent_intent"/);
  assert.match(indexSource, /source:\s*"agent"/);
});

test("triggerWorkflowAutomation keeps personal scope owner-check free", () => {
  const block = callableBlocks().find(({name}) => name === "triggerWorkflowAutomation");

  assert.ok(block, "triggerWorkflowAutomation callable should exist");
  assert.match(block.body, /const automationScope = .*=== "personal" \? "personal" : "owner"/);
  assert.match(block.body, /if\s*\(automationScope === "owner"\)\s*\{\s*await assertOwner\(request\.auth\);\s*\}/s);
  assert.doesNotMatch(block.body, /if\s*\(automationScope === "personal"\)\s*\{\s*await assertOwner\(request\.auth\);\s*\}/s);
});

test("Shopify collection sync prunes deleted handles instead of falling back silently", () => {
  const syncBlock = callableBlocks().find(({name}) => name === "syncShopifyMerch");
  const listBlock = callableBlocks().find(({name}) => name === "listShopifyCollections");

  assert.ok(syncBlock, "syncShopifyMerch callable should exist");
  assert.ok(listBlock, "listShopifyCollections callable should exist");
  assert.match(syncBlock.body, /secrets:\s*\[shopifyAdminAccessToken\]/);
  assert.match(listBlock.body, /secrets:\s*\[shopifyAdminAccessToken\]/);
  assert.match(indexSource, /function reconcileShopifyCollectionHandles\(/);
  assert.match(indexSource, /function isVisibleShopifyCollection\(/);
  assert.match(indexSource, /async function syncConfiguredShopifyCollectionHandles\(/);
  assert.match(indexSource, /lastRemovedCollectionHandles/);
  assert.match(indexSource, /collection_filter_pruned_empty/);
  assert.match(indexSource, /productCount > 0/);
  assert.match(indexSource, /products\(first: 1\)/);
  assert.match(indexSource, /removedCollectionHandles/);
  assert.match(indexSource, /const prunedAllConfiguredCollections =/);
  assert.doesNotMatch(indexSource, /collection returned no products\. Falling back to all storefront products/);
  assert.doesNotMatch(indexSource, /collection sync returned no products\. Falling back to all public products/);
  assert.doesNotMatch(indexSource, /public collection sync failed\. Falling back to all public products/);
  assert.match(listBlock.body, /persistShopifyCollectionHandlesIfChanged\(/);
  assert.match(listBlock.body, /selectedCollectionHandles:\s*reconciliation\.activeCollectionHandles/);
});
