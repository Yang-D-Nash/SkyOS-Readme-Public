#!/usr/bin/env node
/**
 * Lists Cloud Functions public exports from functions/index.js (single main entry).
 * Use: node scripts/list_skyos_cloud_function_exports.mjs [--json]
 *       SKYOS_FUNCTIONS_INDEX=path/to/index.js node ...
 */
import fs from "node:fs";
import path from "node:path";
import {fileURLToPath} from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(__dirname, "..");
const defaultIndex = path.join(root, "functions", "index.js");
const indexPath = process.env.SKYOS_FUNCTIONS_INDEX || defaultIndex;
const asJson = process.argv.includes("--json");

const src = fs.readFileSync(indexPath, "utf8");
const lines = src.split(/\r?\n/);

const entries = [];
const re = /^exports\.([A-Za-z0-9_]+)\s*=\s*(.*)$/;

function kindFromRhs(rhs) {
  const t = rhs.replace(/\s+/g, " ").trim();
  if (t.startsWith("onCall(") || t.startsWith("onCall (")) return "onCall";
  if (t.startsWith("onRequest(") || t.startsWith("onRequest (")) return "onRequest";
  if (t.startsWith("onSchedule(") || t.startsWith("onSchedule (")) return "onSchedule";
  if (t.startsWith("onDocumentWritten(") || t.startsWith("onDocumentWritten ("))
    return "onDocumentWritten";
  if (t.startsWith("onDocumentCreated(") || t.startsWith("onDocumentCreated ("))
    return "onDocumentCreated";
  if (t.startsWith("onMessagePublished(") || t.startsWith("onMessagePublished ("))
    return "onMessagePublished";
  if (t.startsWith("functionsV1")) return "functionsV1";
  return "other";
}

for (let i = 0; i < lines.length; i++) {
  const line = lines[i];
  const m = line.match(re);
  if (!m) continue;
  const name = m[1];
  const rhs = m[2];
  entries.push({
    name,
    line: i + 1,
    kind: kindFromRhs(rhs),
  });
}

entries.sort((a, b) => a.line - b.line);

if (asJson) {
  process.stdout.write(
    JSON.stringify(
      {source: path.relative(root, indexPath), count: entries.length, exports: entries},
      null,
      2,
    ) + "\n",
  );
} else {
  console.log(
    `SkyOS Cloud Functions — exports in ${path.relative(root, indexPath)} (${entries.length}):\n`,
  );
  for (const e of entries) {
    console.log(`${String(e.line).padStart(5, " ")}  ${e.kind.padEnd(16)}  ${e.name}`);
  }
}
