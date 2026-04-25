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
