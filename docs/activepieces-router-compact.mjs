// SkyOS Activepieces Router Compact
// Shorter drop-in Code Step for "SkyOS - Agent Webhook Master PRO".
// Do not paste real secret values into this file; pass workflowSecret as an Activepieces input.

export const code = async (inputs) => {
  const startedAt = Date.now();
  const schemaVersion = "skyos.activepieces.router.compact.v1.2";

  const asObject = (value) =>
    value && typeof value === "object" && !Array.isArray(value) ? value : null;
  const parseJsonSafe = (value) => {
    if (typeof value !== "string" || !value.trim()) return null;
    try { return JSON.parse(value.trim()); } catch { return null; }
  };
  const clean = (value, fallback = "", max = 5000) =>
    String(value ?? "").trim().slice(0, max) || fallback;
  const first = (values, fallback = "", max = 5000) => {
    for (const value of values) {
      const s = clean(value, "", max);
      if (s) return s;
    }
    return fallback;
  };
  const firstWithSource = (entries, fallback = "", max = 5000) => {
    for (const [source, value] of entries) {
      const s = clean(value, "", max);
      if (s) return { value: s, source, length: s.length };
    }
    return { value: fallback, source: "missing", length: clean(fallback, "", max).length };
  };
  const bool = (value) => {
    if (value === true || value === false) return value;
    return ["true", "1", "yes", "owner"].includes(String(value ?? "").trim().toLowerCase());
  };
  const isIsoDateTime = (value) => {
    const s = clean(value, "", 80);
    return Boolean(s && s.includes("T") && !Number.isNaN(Date.parse(s)));
  };
  const isYmd = (value) => /^\d{4}-\d{2}-\d{2}$/.test(clean(value, "", 20));
  const priority = (value, fallback = "normal") => {
    const p = clean(value, "", 20).toLowerCase();
    return ["low", "normal", "high"].includes(p) ? p : fallback;
  };

  const root = asObject(inputs) || {};
  const bodyCandidate =
    asObject(root.body) || asObject(root.payload) ||
    parseJsonSafe(root.body) || parseJsonSafe(root.payload) ||
    parseJsonSafe(root.rawBody) || root;
  const rawBody = asObject(bodyCandidate?.rawBody) || parseJsonSafe(bodyCandidate?.rawBody);
  const body = rawBody || bodyCandidate || {};
  const data =
    asObject(body.data) || asObject(body.payload) ||
    parseJsonSafe(body.data) || parseJsonSafe(body.payload) ||
    body;
  const user = asObject(body.user) || asObject(data.user) || asObject(root.user) || {};

  const projectId = first([root.projectId, root.firebaseProjectId, root.PROJECT_ID], "skydown-a6add", 100);
  const secretInput = firstWithSource([
    ["root.workflowSecret", root.workflowSecret],
    ["root.SKYOS_WORKFLOW_SECRET", root.SKYOS_WORKFLOW_SECRET],
    ["root.secrets.workflowSecret", root.secrets?.workflowSecret],
    ["root.secrets.SKYOS_WORKFLOW_SECRET", root.secrets?.SKYOS_WORKFLOW_SECRET],
  ], "", 300);
  const workflowSecret = secretInput.value;
  const workflowSecretDiagnostics = {
    workflowSecretPresent: Boolean(workflowSecret),
    workflowSecretSource: secretInput.source,
    workflowSecretLength: secretInput.length,
  };

  const requestId = first([body.requestId, data.requestId, root.requestId], `skyos-${Date.now()}`, 120);
  const uid = first([body.uid, data.uid, user.uid, root.uid], "", 180);
  const appExecutionMode = first([data.appExecutionMode, body.appExecutionMode], "", 80);
  const traceId = `${requestId}-${Math.random().toString(36).slice(2, 8)}`;
  const baseUrl = `https://us-central1-${projectId}.cloudfunctions.net`;

  const normalizeProductivityMode = (value) => {
    const s = clean(value, "", 80).toLowerCase().replace(/^create[_-]/, "").replace(/^workflow[_-]/, "");
    if (["reminder", "remind", "erinnerung", "erinner"].includes(s)) return "reminder";
    if (["task", "todo", "to-do", "aufgabe"].includes(s)) return "task";
    if (["note", "notiz", "memo"].includes(s)) return "note";
    return "";
  };
  const normalizeMode = () => {
    const explicit = [
      data.mode, body.mode, body.action, data.action,
      data.entityType, data.productivityType, data.kind, data.type, data.intent,
      body.entityType, body.productivityType, body.kind, body.type, body.intent,
    ].map(normalizeProductivityMode).find(Boolean);
    if (explicit) return explicit;
    const raw = first([data.mode, body.mode, body.action, data.action], "", 80).toLowerCase();
    const socialIntent = clean(data.socialContext?.intent || body.socialContext?.intent, "", 80).toLowerCase();
    if (socialIntent === "social_analysis") return "social_analysis";
    if (["release", "briefing", "content", "merch", "social_analysis"].includes(raw)) return raw;
    return raw === "automation" || !raw ? "content" : raw;
  };
  const mode = normalizeMode();
  const allowedModes = ["release", "briefing", "content", "merch", "social_analysis", "reminder", "task", "note"];

  const result = ({ message, status = "completed", results = [], extraMeta = {}, privateText = null, groupText = null }) => {
    const responseForWebhook = {
      message,
      workflowStatus: status,
      private: privateText || "",
      group: groupText || "",
      results,
    };
    return {
      message,
      workflowStatus: status,
      results,
      private: privateText,
      group: groupText,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        uid: uid || null,
        mode: mode || null,
        appExecutionMode: appExecutionMode || null,
        ...workflowSecretDiagnostics,
        generatedAt: new Date().toISOString(),
        durationMs: Date.now() - startedAt,
        ...extraMeta,
      },
    };
  };
  const fail = (message, reason, extraMeta = {}) => result({
    message,
    status: "failed",
    results: [{ type: "text", title: "Ausfuehrung nicht moeglich", text: message }],
    extraMeta: { reason, ...extraMeta },
  });

  if (!projectId) return fail("PROJECT_ID fehlt.", "project_id_missing");
  if (!workflowSecret) return fail("Workflow-Secret fehlt.", "workflow_secret_missing");
  if (!uid) return fail("Firebase UID fehlt.", "uid_missing", { debugBodyKeys: Object.keys(body || {}) });
  if (!mode) return fail("mode fehlt.", "mode_missing");
  if (!allowedModes.includes(mode)) return fail("Ungueltiger mode.", "invalid_mode", { allowedModes, providedMode: mode });

  const socialLabels = {
    instagram: "Instagram",
    tiktok: "TikTok",
    youtube: "YouTube",
    facebook: "Facebook/Meta",
    spotify: "Spotify",
  };
  const socialStatusLine = ({ platform, handle, value }) => {
    const label = socialLabels[platform] || platform;
    const h = clean(handle, "", 240);
    const text = clean(value, "", 1200);
    if (!h) return `- ${label}: kein Handle im Payload.`;
    if (!text) return `- ${label}: Handle vorhanden, Live-API-Daten nicht verfuegbar.`;
    const lower = text.toLowerCase();
    if (platform === "spotify" && (
      lower.includes("provider restricted") ||
      lower.includes("policyseitig eingeschraenkt") ||
      lower.includes("live-katalogfelder bleiben")
    )) return "- Spotify: Referenz erkannt, API-Zugriff eingeschraenkt; keine Live-Katalogfelder.";
    if (platform === "tiktok" && (
      lower.includes("weicht vom verknuepften") ||
      lower.includes("weicht vom verknüpften")
    )) return `- TikTok: verknuepfter Token-Account weicht von @${h} ab; keine passenden Live-Daten fuer diesen Handle.`;
    if (platform === "youtube" && (
      lower.includes("kein oeffentlicher kanal") ||
      lower.includes("kein öffentlicher kanal")
    )) return `- YouTube: kein oeffentlicher Kanal zu @${h} gefunden.`;
    if (platform === "instagram" && (
      lower.includes("ohne graph-api-zugriff") ||
      lower.includes("keine verwertbaren daten")
    )) return `- Instagram: Handle @${h} vorhanden, Graph-/Insight-Daten nicht verfuegbar.`;
    if (platform === "facebook" && (
      lower.includes("ohne verwertbaren graph-api-zugriff") ||
      lower.includes("keine verwertbaren page-daten") ||
      lower.includes("nicht aufloesbar") ||
      lower.includes("nicht auflösbar")
    )) return "- Facebook/Meta: Handle/Page vorhanden, Graph-Daten nicht verfuegbar.";
    if (
      lower.includes("nicht verfuegbar") ||
      lower.includes("nicht verfügbar") ||
      lower.includes("kein passendes") ||
      lower.includes("nur der handle-bezug") ||
      lower.includes("nur handle-kontext")
    ) return `- ${label}: Handle vorhanden, Live-API-Daten nicht verfuegbar.`;
    return `- ${label}: Live-Daten fuer ${platform === "spotify" ? h : "@" + h} erhalten.`;
  };

  const sanitizeSocialAnalysisText = (value, max = 9000) => {
    const text = clean(value, "", max);
    if (!text) return "";
    return clean(
      text
        .replace(/\n?##\s*(Spotify|YouTube|Instagram|Facebook\/Meta|TikTok)\s+(?:Live-)?Kontext[\s\S]*?(?=\n##\s|$)/g, "")
        .replace(/\n?Workflow:[^\S\r\n]*\nWorkflow abgeschlossen\.[^\S\r\n]*(?:\n|$)/g, "\n")
        .replace(/\n?Es wurde kein Reminder\/Task\/Notiz automatisch angelegt\.[^\S\r\n]*\nDu kannst alles in Home > Productivity als Liste bearbeiten \(CRUD\)\.[^\S\r\n]*(?:\n|$)/g, "\n")
        .replace(/##\s*Agent-Auswertung/g, "## Analyse")
        .replace(/##\s*Datenstatus/g, "## Datenbasis")
        .replace(/\n{3,}/g, "\n\n"),
      "Social Analysis angefordert.",
      max
    );
  };

  const buildSocialAnalysis = () => {
    const ctx = asObject(data.socialContext) || asObject(body.socialContext) || {};
    const profiles = asObject(ctx.socialProfiles) || {};
    const selected = Array.isArray(ctx.selectedPlatforms) ? ctx.selectedPlatforms : [];
    const platforms = selected.length ? selected : Object.keys(socialLabels);
    const statusLines = platforms.map((platform) => socialStatusLine({
      platform,
      handle: profiles[platform],
      value: {
        instagram: ctx.instagramPublicGraphSummary,
        tiktok: ctx.tiktokPublicSummary,
        youtube: ctx.youtubePublicCatalogSummary,
        facebook: ctx.facebookMetaSummary,
        spotify: ctx.spotifyPublicCatalogSummary,
      }[platform],
    }));
    const accounts = Object.keys(socialLabels)
      .map((platform) => {
        const h = clean(profiles[platform], "", 240);
        if (!h) return "";
        return platform === "spotify" ? `- Spotify: ${h}` : `- ${socialLabels[platform].replace("/Meta", "")}: @${h}`;
      })
      .filter(Boolean);
    const direct = first([data.content, body.content, data.analysis, body.analysis, data.description, body.description], "", 9000);
    if (direct) return sanitizeSocialAnalysisText(direct, 9000);
    const lines = [
      "# SkyOS Social Analysis",
      "",
      "## Analyse",
      first([data.reply, body.reply], "Keine Agent-Auswertung im Payload.", 2400),
      "",
      "## Datenbasis",
      ...statusLines,
      "",
      "## Anfrage",
      first([data.prompt, body.prompt], "Keine Anfrage im Payload.", 600),
    ];
    if (accounts.length) lines.push("", "## Accounts", ...accounts);
    return clean(lines.join("\n"), "Social Analysis angefordert.", 9000);
  };

  const callFunction = async (functionName, payload, timeoutMs = 25000) => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(`${baseUrl}/${functionName}`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-skyos-workflow-secret": workflowSecret,
        },
        body: JSON.stringify(payload),
        signal: controller.signal,
      });
      let responseBody = null;
      try { responseBody = await response.json(); } catch { responseBody = null; }
      return { response, responseBody };
    } finally {
      clearTimeout(timeout);
    }
  };

  const briefingTargetRaw = first([data.briefingTarget, body.briefingTarget, data.target, body.target], "both", 20).toLowerCase();
  const briefingTarget = ["private", "group", "both"].includes(briefingTargetRaw) ? briefingTargetRaw : "both";
  const role = first([user.role, data.role, body.role], "", 80).toLowerCase();
  const isOwner = bool(body.isOwner ?? data.isOwner ?? user.isOwner) || ["owner", "admin", "founder", "owner_unlimited"].includes(role);
  if (mode === "briefing" && ["group", "both"].includes(briefingTarget) && !isOwner) {
    return fail("Nur Owner darf Group/Both Briefing ausfuehren.", "owner_required_for_group_briefing");
  }

  if (mode === "social_analysis") {
    const content = buildSocialAnalysis();
    const title = first([data.title, body.title], "Social Analysis", 180);
    const saveToNotes = bool(data.saveToNotes ?? body.saveToNotes ?? data.notePreferred ?? body.notePreferred);
    if (!saveToNotes) {
      return result({
        message: "Analyse bereit. Im Verlauf nutzbar; als Notiz nur bei Bedarf speichern.",
        results: [{ type: "text", title, text: content }],
        extraMeta: {
          actionType: "social_analysis",
          functionName: null,
          httpStatus: null,
          noteId: null,
          savedToNotes: false,
          persistence: "chat_history_only",
        },
      });
    }
    const payload = { uid, title, content, source: "activepieces", requestId: `${requestId}-social-analysis` };
    const { response, responseBody } = await callFunction("createNoteFromWorkflow", payload);
    const ok = response.ok && (!responseBody || responseBody.ok !== false);
    return result({
      message: ok ? "Analyse bereit und als Notiz gespeichert." : "Analyse verstanden, aber Speichern fehlgeschlagen.",
      status: ok ? "completed" : "failed",
      results: [
        { type: "text", title, text: content },
        ...(responseBody?.noteId ? [{ type: "note", id: String(responseBody.noteId), title, text: "Analyse als Notiz gespeichert." }] : []),
      ],
      extraMeta: {
        actionType: "social_analysis",
        functionName: "createNoteFromWorkflow",
        httpStatus: response.status,
        cloudBodyOk: responseBody?.ok,
        noteId: responseBody?.noteId || null,
        savedToNotes: ok,
        persistence: ok ? "note" : "failed",
      },
    });
  }

  let functionName = "";
  let actionType = mode;
  let payload = null;
  if (mode === "briefing") {
    const date = first([data.date, body.date], new Date().toISOString().slice(0, 10), 20);
    if (!isYmd(date)) return fail("Fuer briefing fehlt valides date (YYYY-MM-DD).", "briefing_date_invalid");
    functionName = "createFounderBriefingFromWorkflow";
    payload = { uid, mode: briefingTarget, date, source: "activepieces", requestId: `${requestId}-briefing` };
  } else if (mode === "reminder") {
    const title = first([data.title, body.title, data.summary, body.summary], "Reminder", 180);
    const scheduledAt = first([data.scheduledAt, body.scheduledAt, data.dueAt, body.dueAt], "", 80);
    if (!isIsoDateTime(scheduledAt)) return fail("Fuer reminder fehlt valides scheduledAt (ISO DateTime).", "reminder_scheduled_at_invalid");
    functionName = "createReminderFromWorkflow";
    payload = { uid, title, body: first([data.body, body.body, data.description, body.description, data.content, body.content], "", 5000), scheduledAt, timezone: first([data.timezone, body.timezone, user.timezone], "Europe/Berlin", 80), source: "activepieces", requestId: `${requestId}-reminder` };
  } else if (mode === "release" || mode === "task") {
    const title = first([data.title, body.title, data.taskTitle, body.taskTitle], "", 180);
    if (!title) return fail(`Fuer ${mode} fehlt title.`, `${mode}_title_missing`);
    functionName = "createTaskFromWorkflow";
    actionType = mode === "release" ? "release" : "task";
    payload = { uid, title, description: first([data.description, body.description, data.content, body.content], mode === "release" ? "Release-Aufgabe aus Workflow." : "", 5000), priority: priority(data.priority || body.priority, mode === "release" ? "high" : "normal"), source: "activepieces", requestId: `${requestId}-${mode}` };
    const dueAt = first([data.dueAt, body.dueAt], "", 80);
    if (isIsoDateTime(dueAt)) payload.dueAt = dueAt;
  } else if (mode === "content" || mode === "note") {
    const title = first([data.title, body.title, data.noteTitle, body.noteTitle], mode === "note" ? "Notiz" : "Content Notiz", 180);
    functionName = "createNoteFromWorkflow";
    actionType = mode === "note" ? "note" : "content";
    payload = { uid, title, content: first([data.content, body.content, data.description, body.description, data.body, body.body, data.reply, body.reply], mode === "note" ? "Notiz aus Workflow." : "Content-Input aus Workflow.", 5000), source: "activepieces", requestId: `${requestId}-${mode}` };
  } else if (mode === "merch") {
    const notePreferred = bool(data.notePreferred ?? body.notePreferred);
    const title = first([data.title, body.title], notePreferred ? "Merch Notiz" : "Merch Task", 180);
    functionName = notePreferred ? "createNoteFromWorkflow" : "createTaskFromWorkflow";
    actionType = notePreferred ? "merch_note" : "merch_task";
    payload = notePreferred ?
      { uid, title, content: first([data.content, body.content, data.description, body.description], "Merch-Workflow Input.", 5000), source: "activepieces", requestId: `${requestId}-merch-note` } :
      { uid, title, description: first([data.content, body.content, data.description, body.description], "Merch-Workflow Input.", 5000), priority: priority(data.priority || body.priority, "normal"), source: "activepieces", requestId: `${requestId}-merch-task` };
  }
  if (!functionName || !payload) return fail("Routing konnte nicht bestimmt werden.", "route_resolution_failed");

  try {
    let attempts = 1;
    let { response, responseBody } = await callFunction(functionName, payload, actionType === "briefing" ? 60000 : 25000);
    if (!response.ok && (response.status === 429 || response.status >= 500)) {
      attempts = 2;
      ({ response, responseBody } = await callFunction(functionName, payload, actionType === "briefing" ? 60000 : 25000));
    }
    const ok = response.ok && (!responseBody || responseBody.ok !== false);
    const privateText = first([responseBody?.private, responseBody?.briefingPrivate, responseBody?.founderBriefingMarkdown, responseBody?.data?.private, responseBody?.result?.private], "", 12000);
    const groupText = first([responseBody?.group, responseBody?.briefingGroup, responseBody?.data?.group, responseBody?.result?.group], "", 12000);
    const id = responseBody?.reminderId || responseBody?.taskId || responseBody?.noteId || "";
    const kind = responseBody?.reminderId ? "reminder" : responseBody?.taskId ? "task" : responseBody?.noteId ? "note" : "text";
    const results = [];
    if (actionType === "briefing") {
      if (privateText) results.push({ type: "text", title: "Founder Briefing (Private)", text: privateText });
      if (groupText) results.push({ type: "text", title: "Founder Briefing (Group)", text: groupText });
    } else if (id) {
      results.push({ type: kind, id: String(id), title: payload.title || actionType, text: `${kind === "note" ? "Notiz" : kind === "task" ? "Task" : "Reminder"} erstellt: ${payload.title || actionType}` });
    }
    if (!results.length) {
      results.push({ type: "text", title: "Ausfuehrung", text: ok ? "Workflow abgeschlossen." : "Workflow fehlgeschlagen." });
    }
    return result({
      message: actionType === "briefing" && ok ? "Briefing bereit." : (ok ? (results[0].text || "Workflow abgeschlossen.") : "Die Aktion wurde verstanden, konnte aber nicht abgeschlossen werden."),
      status: ok ? "completed" : "failed",
      results,
      privateText: actionType === "briefing" ? privateText || null : null,
      groupText: actionType === "briefing" ? groupText || null : null,
      extraMeta: {
        functionName,
        actionType,
        httpStatus: response.status,
        cloudBodyOk: responseBody?.ok,
        reminderId: responseBody?.reminderId || null,
        taskId: responseBody?.taskId || null,
        noteId: responseBody?.noteId || null,
        attemptCount: attempts,
      },
    });
  } catch (error) {
    return fail("Aktion verstanden, aber technisch fehlgeschlagen.", "network_or_runtime_error", {
      error: error instanceof Error ? error.message : String(error),
      functionName,
      actionType,
    });
  }
};
