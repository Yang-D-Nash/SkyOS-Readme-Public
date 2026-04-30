// SkyOS Activepieces Router, premium v2.5.2
// Drop-in Code Step for "SkyOS - Agent Webhook Master PRO".
// Secrets stay in Activepieces inputs. Do not commit real secret values.

export const code = async (inputs) => {
  const startedAt = Date.now();

  const parseJsonSafe = (value) => {
    if (typeof value !== "string") return null;
    const s = value.trim();
    if (!s) return null;
    try {
      return JSON.parse(s);
    } catch {
      return null;
    }
  };

  const asObject = (value) =>
    value && typeof value === "object" && !Array.isArray(value) ? value : null;

  const clean = (value, fallback = "", max = 5000) =>
    String(value ?? "").trim().slice(0, max) || fallback;

  const firstClean = (values, fallback = "", max = 5000) => {
    for (const value of values) {
      const s = clean(value, "", max);
      if (s) return s;
    }
    return fallback;
  };

  const firstCleanWithSource = (entries, fallback = "", max = 5000) => {
    for (const [source, value] of entries) {
      const s = clean(value, "", max);
      if (s) return { value: s, source, length: s.length };
    }
    return { value: fallback, source: "missing", length: clean(fallback, "", max).length };
  };

  const parseBool = (value) => {
    if (value === true) return true;
    if (value === false) return false;
    const s = String(value ?? "").trim().toLowerCase();
    return s === "true" || s === "1" || s === "yes" || s === "owner";
  };

  const isValidYmd = (value) => {
    const s = String(value || "").trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
    const d = new Date(`${s}T00:00:00.000Z`);
    return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === s;
  };

  const isValidIsoDateTime = (value) => {
    const s = String(value || "").trim();
    return Boolean(s && s.includes("T") && !Number.isNaN(Date.parse(s)));
  };

  const normalizePriority = (value, fallback = "normal") => {
    const p = String(value || "").trim().toLowerCase();
    return ["low", "normal", "high"].includes(p) ? p : fallback;
  };

  const normalizeProductivityMode = (value) => {
    const s = clean(value, "", 80)
      .toLowerCase()
      .replace(/^create[_-]/, "")
      .replace(/^workflow[_-]/, "");
    if (["reminder", "remind", "erinnerung", "erinner"].includes(s)) return "reminder";
    if (["task", "todo", "to-do", "aufgabe"].includes(s)) return "task";
    if (["note", "notiz", "memo"].includes(s)) return "note";
    return "";
  };

  const normalizeMode = ({ rawMode, data, body }) => {
    const explicitProductivity =
      normalizeProductivityMode(rawMode) ||
      normalizeProductivityMode(data?.entityType) ||
      normalizeProductivityMode(data?.productivityType) ||
      normalizeProductivityMode(data?.kind) ||
      normalizeProductivityMode(data?.type) ||
      normalizeProductivityMode(data?.intent) ||
      normalizeProductivityMode(data?.action) ||
      normalizeProductivityMode(body?.entityType) ||
      normalizeProductivityMode(body?.productivityType) ||
      normalizeProductivityMode(body?.kind) ||
      normalizeProductivityMode(body?.type) ||
      normalizeProductivityMode(body?.intent) ||
      normalizeProductivityMode(body?.action);
    if (explicitProductivity) return explicitProductivity;

    const m = clean(rawMode, "", 80).toLowerCase();
    const socialIntent = clean(
      data?.socialContext?.intent || body?.socialContext?.intent,
      "",
      80,
    ).toLowerCase();
    if (socialIntent === "social_analysis") return "social_analysis";

    const allowedDirect = ["release", "briefing", "merch", "social_analysis", "reminder", "task", "note"];
    if (allowedDirect.includes(m)) return m;

    const text = [
      data?.prompt,
      data?.reply,
      data?.title,
      data?.content,
      data?.description,
      data?.body,
      data?.analysis,
      body?.prompt,
      body?.reply,
      body?.title,
      body?.content,
      body?.description,
      body?.body,
      body?.analysis,
    ]
      .map((entry) => clean(entry, "", 2000).toLowerCase())
      .filter(Boolean)
      .join(" ");

    if (isValidIsoDateTime(data?.scheduledAt || body?.scheduledAt) ||
      /\b(reminder|remind|erinner|erinnerung|benachrichtig|notify|push|alarm|weck)/i.test(text)) {
      return "reminder";
    }
    if (/\b(briefing|founder|kpi|daily ops|tagesbrief)/i.test(text)) return "briefing";
    if (/\b(shopify|merch|hoodie|tee|produktlaunch|warenkorb|collection)/i.test(text)) return "merch";
    if (/\b(release|changelog|ankuendigung|announcement)/i.test(text)) return "release";
    if (isValidIsoDateTime(data?.dueAt || body?.dueAt) ||
      /\b(task|todo|to-do|aufgabe|deadline|follow[ -]?up|erledigen|abhaken|faellig)/i.test(text)) {
      return "task";
    }
    if (/\b(note|notiz|memo|merken|speichern|aufschreiben|mitschrift)/i.test(text)) return "note";
    return m === "automation" || !m ? "content" : m;
  };

  const buildStatusText = ({ actionType, ok, title, scheduledAt, dueAt, deduplicated }) => {
    if (!ok) return "Die Aktion wurde verstanden, konnte aber nicht abgeschlossen werden.";
    if (actionType === "reminder") {
      return `Reminder erstellt: ${title}${scheduledAt ? `\nFaellig: ${scheduledAt}` : ""}`;
    }
    if (actionType === "task" || actionType === "release" || actionType === "merch_task") {
      return `${deduplicated ? "Task aktualisiert" : "Task erstellt"}: ${title}${dueAt ? `\nFaellig: ${dueAt}` : ""}`;
    }
    if (actionType === "note" || actionType === "content" || actionType === "social_analysis" || actionType === "merch_note") {
      return `Notiz erstellt: ${title}`;
    }
    if (actionType === "briefing") return "Briefing bereit.";
    return "Workflow abgeschlossen.";
  };

  const socialAnalysisTitle = (socialContext) => {
    const ctx = asObject(socialContext) || {};
    const labels = {
      instagram: "Instagram",
      tiktok: "TikTok",
      youtube: "YouTube",
      facebook: "Facebook",
      spotify: "Spotify",
    };
    const profiles = asObject(ctx.socialProfiles) || {};
    const selected = Array.isArray(ctx.selectedPlatforms) ? ctx.selectedPlatforms : [];
    const platforms = (selected.length ? selected : Object.keys(labels).filter((key) => clean(profiles[key], "", 240)))
      .map((key) => labels[key] || key)
      .filter(Boolean)
      .slice(0, 4);
    return platforms.length ? `Social Analysis: ${platforms.join(", ")}` : "Social Analysis";
  };

  const socialLiveStatusLine = ({ platform, label, handle = "", value = "" }) => {
    const h = clean(handle, "", 240);
    const text = clean(value, "", 1200);
    if (!h) return `- ${label}: kein Handle im Payload.`;
    if (!text) return `- ${label}: Handle vorhanden, Live-API-Daten nicht verfuegbar.`;
    const lower = text.toLowerCase();
    if (
      platform === "spotify" &&
      (lower.includes("provider restricted") ||
        lower.includes("policyseitig eingeschraenkt") ||
        lower.includes("live-katalogfelder bleiben"))
    ) {
      return "- Spotify: Referenz erkannt, API-Zugriff eingeschraenkt; keine Live-Katalogfelder.";
    }
    if (
      platform === "tiktok" &&
      (lower.includes("weicht vom verknuepften") || lower.includes("weicht vom verknüpften"))
    ) {
      return `- TikTok: verknuepfter Token-Account weicht von @${h} ab; keine passenden Live-Daten fuer diesen Handle.`;
    }
    if (
      platform === "youtube" &&
      (lower.includes("kein oeffentlicher kanal") || lower.includes("kein öffentlicher kanal"))
    ) {
      return `- YouTube: kein oeffentlicher Kanal zu @${h} gefunden.`;
    }
    if (
      platform === "instagram" &&
      (lower.includes("ohne graph-api-zugriff") || lower.includes("keine verwertbaren daten"))
    ) {
      return `- Instagram: Handle @${h} vorhanden, Graph-/Insight-Daten nicht verfuegbar.`;
    }
    if (
      platform === "facebook" &&
      (lower.includes("ohne verwertbaren graph-api-zugriff") ||
        lower.includes("keine verwertbaren page-daten") ||
        lower.includes("nicht aufloesbar") ||
        lower.includes("nicht auflösbar"))
    ) {
      return "- Facebook/Meta: Handle/Page vorhanden, Graph-Daten nicht verfuegbar.";
    }
    if (
      lower.includes("nicht verfuegbar") ||
      lower.includes("nicht verfügbar") ||
      lower.includes("kein passendes") ||
      lower.includes("nur der handle-bezug") ||
      lower.includes("nur handle-kontext")
    ) {
      return `- ${label}: Handle vorhanden, Live-API-Daten nicht verfuegbar.`;
    }
    return `- ${label}: Live-Daten fuer ${platform === "spotify" ? h : "@" + h} erhalten.`;
  };

  const socialAnalysisContent = ({ data, body }) => {
    const ctx = asObject(data?.socialContext) || asObject(body?.socialContext) || {};
    const profiles = asObject(ctx.socialProfiles) || {};
    const selected = Array.isArray(ctx.selectedPlatforms) ? ctx.selectedPlatforms : [];
    const missing = Array.isArray(ctx.missingPlatforms) ? ctx.missingPlatforms : [];
    const labels = {
      instagram: "Instagram",
      tiktok: "TikTok",
      youtube: "YouTube",
      facebook: "Facebook",
      spotify: "Spotify",
    };
    const accountLines = Object.keys(labels)
      .map((platform) => {
        const handle = clean(profiles[platform], "", 240);
        if (!handle) return "";
        return platform === "spotify" ? `- ${labels[platform]}: ${handle}` : `- ${labels[platform]}: @${handle}`;
      })
      .filter(Boolean);
    const selectedLabels = selected.map((platform) => labels[platform] || platform).filter(Boolean);
    const missingLabels = missing.map((platform) => labels[platform] || platform).filter(Boolean);
    const liveStatus = [
      ["instagram", "Instagram", ctx.instagramPublicGraphSummary],
      ["tiktok", "TikTok", ctx.tiktokPublicSummary],
      ["youtube", "YouTube", ctx.youtubePublicCatalogSummary],
      ["facebook", "Facebook/Meta", ctx.facebookMetaSummary],
      ["spotify", "Spotify", ctx.spotifyPublicCatalogSummary],
    ]
      .filter(([platform]) => selected.length === 0 || selected.includes(platform))
      .map(([platform, label, value]) => {
        return socialLiveStatusLine({ platform, label, handle: profiles[platform], value });
      });
    const sanitizeSocialAnalysisText = (value, max = 5000) => {
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
        max,
      );
    };
    const direct = firstClean(
      [data?.content, body?.content, data?.analysis, body?.analysis, data?.description, body?.description],
      "",
      5000,
    );
    if (direct) return sanitizeSocialAnalysisText(direct, 5000);
    const lines = [
      "# SkyOS Social Analysis",
      "",
      "## Analyse",
      firstClean([data?.reply, body?.reply], "Keine Agent-Auswertung im Payload.", 2400),
      "",
      "## Datenbasis",
      ...(liveStatus.length ? liveStatus : ["- Kein Plattformstatus im Payload."]),
      "",
      "## Anfrage",
      firstClean([data?.prompt, body?.prompt], "Keine Anfrage im Payload.", 600),
    ];
    if (accountLines.length) lines.push("", "## Accounts", ...accountLines);
    if (selectedLabels.length) lines.push("", `Ausgewaehlte Plattformen: ${selectedLabels.join(", ")}.`);
    if (missingLabels.length) lines.push(`Ausgewaehlt, aber ohne Handle: ${missingLabels.join(", ")}.`);
    return clean(lines.join("\n"), "Social Analysis angefordert.", 5000);
  };

  const returnSocialAnalysisOutput = ({ title, text, savedToNotes = false }) => {
    const durationMs = Date.now() - startedAt;
    const results = [
      {
        type: "text",
        title,
        text,
      },
    ];
    const responseForWebhook = {
      message: savedToNotes ? "Analyse bereit und als Notiz gespeichert." : "Analyse bereit. Im Verlauf nutzbar; als Notiz nur bei Bedarf speichern.",
      workflowStatus: "completed",
      private: "",
      group: "",
      results,
    };
    return {
      message: responseForWebhook.message,
      workflowStatus: "completed",
      results,
      private: null,
      group: null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        uid,
        mode: "social_analysis",
        appExecutionMode: appExecutionMode || null,
        ...workflowSecretDiagnostics,
        generatedAt: new Date().toISOString(),
        functionName: null,
        actionType: "social_analysis",
        httpStatus: null,
        cloudBodyOk: null,
        reminderId: null,
        taskId: null,
        noteId: null,
        savedToNotes,
        persistence: savedToNotes ? "note" : "chat_history_only",
        actions: [
          {
            type: "social_analysis",
            functionName: null,
            status: "completed",
            httpStatus: null,
            attemptCount: 0,
            durationMs,
            requestId: `${requestId}-social-analysis`,
            savedToNotes,
          },
        ],
      },
    };
  };

  const root = asObject(inputs) || {};
  const bodyCandidate =
    asObject(root.body) ||
    asObject(root.payload) ||
    parseJsonSafe(root.body) ||
    parseJsonSafe(root.payload) ||
    root;

  const rawBodyObj =
    asObject(bodyCandidate?.rawBody) || parseJsonSafe(bodyCandidate?.rawBody) || null;

  const body = rawBodyObj || bodyCandidate || {};
  const data =
    asObject(body.data) ||
    asObject(body.payload) ||
    parseJsonSafe(body.data) ||
    parseJsonSafe(body.payload) ||
    body;
  const userObj = asObject(body.user) || asObject(data.user) || asObject(root.user) || {};

  const projectId = firstClean(
    [root.projectId, root.firebaseProjectId, root.PROJECT_ID],
    "skydown-a6add",
    100,
  );
  const workflowSecretInput = firstCleanWithSource(
    [
      ["root.workflowSecret", root.workflowSecret],
      ["root.SKYOS_WORKFLOW_SECRET", root.SKYOS_WORKFLOW_SECRET],
      ["root.secrets.workflowSecret", root.secrets?.workflowSecret],
      ["root.secrets.SKYOS_WORKFLOW_SECRET", root.secrets?.SKYOS_WORKFLOW_SECRET],
    ],
    "",
    300,
  );
  const workflowSecret = workflowSecretInput.value;
  const workflowSecretDiagnostics = {
    workflowSecretPresent: Boolean(workflowSecret),
    workflowSecretSource: workflowSecretInput.source,
    workflowSecretLength: workflowSecretInput.length,
  };

  const requestId = firstClean(
    [body.requestId, data.requestId, root.requestId],
    `skyos-${Date.now()}`,
    120,
  );

  const uid = firstClean([body.uid, data.uid, userObj.uid, root.uid], "", 180);
  const appExecutionMode = firstClean([data.appExecutionMode, body.appExecutionMode], "", 80);
  const rawMode = firstClean([data.mode, body.mode, body.action, data.action], "", 80);
  const mode = normalizeMode({ rawMode, data, body });

  const schemaVersion = "skyos.activepieces.router.premium.v2.5.2";
  const traceId = `${requestId}-${Math.random().toString(36).slice(2, 8)}`;
  const todayYmd = new Date().toISOString().slice(0, 10);
  const baseUrl = `https://us-central1-${projectId}.cloudfunctions.net`;
  const allowedModes = ["release", "briefing", "content", "merch", "social_analysis", "reminder", "task", "note"];

  const fail = (message, reason, extra = {}) => {
    const responseForWebhook = {
      message,
      workflowStatus: "failed",
      private: "",
      group: "",
      results: [
        {
          type: "text",
          title: "Ausfuehrung nicht moeglich",
          text: message,
        },
      ],
    };
    return {
      message,
      workflowStatus: "failed",
      results: responseForWebhook.results,
      private: null,
      group: null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        mode: mode || null,
        uid: uid || null,
        appExecutionMode: appExecutionMode || null,
        ...workflowSecretDiagnostics,
        reason,
        generatedAt: new Date().toISOString(),
        ...extra,
      },
    };
  };

  if (!projectId) return fail("PROJECT_ID fehlt.", "project_id_missing");
  if (!workflowSecret) return fail("Workflow-Secret fehlt.", "workflow_secret_missing");
  if (!uid) return fail("Firebase UID fehlt.", "uid_missing", { debugBodyKeys: Object.keys(body || {}) });
  if (!mode) return fail("mode fehlt.", "mode_missing");
  if (!allowedModes.includes(mode)) {
    return fail("Ungueltiger mode.", "invalid_mode", { allowedModes, providedMode: mode });
  }

  const userRole = firstClean([userObj.role, data.role, body.role], "", 80).toLowerCase();
  const isOwner =
    parseBool(body?.isOwner ?? data?.isOwner ?? userObj?.isOwner) ||
    ["owner", "admin", "founder", "owner_unlimited"].includes(userRole);

  const targetRaw = firstClean(
    [data.briefingTarget, body.briefingTarget, data.target, body.target],
    "both",
    20,
  ).toLowerCase();
  const briefingTarget = ["private", "group", "both"].includes(targetRaw) ? targetRaw : "both";
  const requiresOwner = mode === "briefing" && (briefingTarget === "group" || briefingTarget === "both");
  if (requiresOwner && !isOwner) {
    return fail("Nur Owner darf Group/Both Briefing ausfuehren.", "owner_required_for_group_briefing", {
      briefingTarget,
      isOwner: false,
    });
  }

  let functionName = null;
  let payload = null;
  let actionType = null;
  let outputTitle = "";
  let outputScheduledAt = "";
  let outputDueAt = "";

  if (mode === "briefing") {
    const date = firstClean([data.date, body.date], todayYmd, 20);
    if (!isValidYmd(date)) return fail("Fuer briefing fehlt valides date (YYYY-MM-DD).", "briefing_date_invalid");
    functionName = "createFounderBriefingFromWorkflow";
    actionType = "briefing";
    payload = {
      uid,
      mode: briefingTarget,
      date,
      source: "activepieces",
      requestId: `${requestId}-briefing`,
    };
  } else if (mode === "reminder") {
    const title = firstClean([data.title, body.title, data.summary, body.summary], "Reminder", 180);
    const scheduledAt = firstClean([data.scheduledAt, body.scheduledAt, data.dueAt, body.dueAt], "", 80);
    if (!isValidIsoDateTime(scheduledAt)) {
      return fail("Fuer reminder fehlt valides scheduledAt (ISO DateTime).", "reminder_scheduled_at_invalid");
    }
    const bodyText = firstClean([data.body, body.body, data.description, body.description, data.content, body.content], "", 5000);
    functionName = "createReminderFromWorkflow";
    actionType = "reminder";
    outputTitle = title;
    outputScheduledAt = scheduledAt;
    payload = {
      uid,
      title,
      body: bodyText,
      scheduledAt,
      timezone: firstClean([data.timezone, body.timezone, userObj.timezone], "Europe/Berlin", 80),
      source: "activepieces",
      requestId: `${requestId}-reminder`,
    };
  } else if (mode === "release" || mode === "task") {
    const title = firstClean([data.title, body.title, data.taskTitle, body.taskTitle], "", 180);
    if (!title) return fail(`Fuer ${mode} fehlt title.`, `${mode}_title_missing`);
    const dueAt = firstClean([data.dueAt, body.dueAt], "", 80);
    functionName = "createTaskFromWorkflow";
    actionType = mode === "release" ? "release" : "task";
    outputTitle = title;
    outputDueAt = isValidIsoDateTime(dueAt) ? dueAt : "";
    payload = {
      uid,
      title,
      description: firstClean([data.description, body.description, data.content, body.content], mode === "release" ? "Release-Aufgabe aus Workflow." : "", 5000),
      priority: normalizePriority(data.priority || body.priority, mode === "release" ? "high" : "normal"),
      source: "activepieces",
      requestId: `${requestId}-${mode}`,
    };
    if (outputDueAt) payload.dueAt = outputDueAt;
  } else if (mode === "content" || mode === "note") {
    const title = firstClean([data.title, body.title, data.noteTitle, body.noteTitle], mode === "note" ? "Notiz" : "Content Notiz", 180);
    functionName = "createNoteFromWorkflow";
    actionType = mode === "note" ? "note" : "content";
    outputTitle = title;
    payload = {
      uid,
      title,
      content: firstClean(
        [data.content, body.content, data.description, body.description, data.body, body.body, data.reply, body.reply],
        mode === "note" ? "Notiz aus Workflow." : "Content-Input aus Workflow.",
        5000,
      ),
      source: "activepieces",
      requestId: `${requestId}-${mode}`,
    };
  } else if (mode === "merch") {
    const notePreferred = parseBool(data.notePreferred ?? body.notePreferred);
    const title = firstClean([data.title, body.title], notePreferred ? "Merch Notiz" : "Merch Task", 180);
    outputTitle = title;
    if (notePreferred) {
      functionName = "createNoteFromWorkflow";
      actionType = "merch_note";
      payload = {
        uid,
        title,
        content: firstClean([data.content, body.content, data.description, body.description], "Merch-Workflow Input.", 5000),
        source: "activepieces",
        requestId: `${requestId}-merch-note`,
      };
    } else {
      const dueAt = firstClean([data.dueAt, body.dueAt], "", 80);
      functionName = "createTaskFromWorkflow";
      actionType = "merch_task";
      outputDueAt = isValidIsoDateTime(dueAt) ? dueAt : "";
      payload = {
        uid,
        title,
        description: firstClean([data.content, body.content, data.description, body.description], "Merch-Workflow Input.", 5000),
        priority: normalizePriority(data.priority || body.priority, "normal"),
        source: "activepieces",
        requestId: `${requestId}-merch-task`,
      };
      if (outputDueAt) payload.dueAt = outputDueAt;
    }
  } else if (mode === "social_analysis") {
    const title = firstClean([data.title, body.title], socialAnalysisTitle(data.socialContext || body.socialContext), 180);
    outputTitle = title;
    const content = socialAnalysisContent({ data, body });
    const saveToNotes = parseBool(
      data.saveToNotes ??
        body.saveToNotes ??
        data.notePreferred ??
        body.notePreferred ??
        data.persistToNotes ??
        body.persistToNotes,
    );
    if (!saveToNotes) {
      return returnSocialAnalysisOutput({ title, text: content, savedToNotes: false });
    }
    functionName = "createNoteFromWorkflow";
    actionType = "social_analysis";
    payload = {
      uid,
      title,
      content,
      source: "activepieces",
      requestId: `${requestId}-social-analysis`,
    };
  }

  if (!functionName || !payload || !actionType) {
    return fail("Routing konnte nicht bestimmt werden.", "route_resolution_failed");
  }

  const isCloudSuccess = (response, responseBody) => {
    if (!response?.ok) return false;
    if (responseBody && typeof responseBody === "object" && "ok" in responseBody) {
      return responseBody.ok === true;
    }
    return true;
  };

  const callOnce = async () => {
    const controller = new AbortController();
    const timeoutMs = actionType === "briefing" ? 60000 : 20000;
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
      try {
        responseBody = await response.json();
      } catch {
        responseBody = null;
      }
      return { response, responseBody };
    } finally {
      clearTimeout(timeout);
    }
  };

  try {
    let attempts = 1;
    let { response, responseBody } = await callOnce();
    const transient = !response.ok && (response.status === 429 || response.status >= 500);
    if (transient) {
      attempts = 2;
      ({ response, responseBody } = await callOnce());
    }

    const ok = isCloudSuccess(response, responseBody);
    const durationMs = Date.now() - startedAt;
    const privateText = firstClean(
      [
        responseBody?.private,
        responseBody?.briefingPrivate,
        responseBody?.founderBriefingMarkdown,
        responseBody?.data?.private,
        responseBody?.result?.private,
      ],
      "",
      12000,
    );
    const groupText = firstClean(
      [responseBody?.group, responseBody?.briefingGroup, responseBody?.data?.group, responseBody?.result?.group],
      "",
      12000,
    );

    const reminderId = responseBody && typeof responseBody === "object" ? responseBody.reminderId : null;
    const taskId = responseBody && typeof responseBody === "object" ? responseBody.taskId : null;
    const noteId = responseBody && typeof responseBody === "object" ? responseBody.noteId : null;
    const deduplicated = responseBody && typeof responseBody === "object" ? responseBody.deduplicated : null;

    const results = actionType === "social_analysis" ? [] : [
      {
        type: "table",
        title: "Ausfuehrung",
        columns: ["Mode", "Status", "HTTP", "Cloud ok", "Versuche"],
        rows: [
          [
            actionType,
            ok ? "completed" : "failed",
            String(response.status),
            String(responseBody?.ok === false ? "false" : "true"),
            String(attempts),
          ],
        ],
      },
    ];

    if (actionType === "briefing") {
      if (privateText) {
        results.push({
          type: "text",
          title: "Founder Briefing (Private)",
          text: privateText,
        });
      }
      if (groupText) {
        results.push({
          type: "text",
          title: "Founder Briefing (Group)",
          text: groupText,
        });
      }
      if (ok && !privateText && !groupText) {
        results.push({
          type: "text",
          title: "Hinweis",
          text: "Briefing erfolgreich aufgerufen, aber private/group leer. Pruefe founder_daily_kpis und Rechte.",
        });
      }
    } else if (actionType === "social_analysis") {
      const analysisText = clean(payload?.content, "", 12000);
      if (analysisText) {
        results.push({
          type: "text",
          title: outputTitle || payload.title || "Social Analysis",
          text: analysisText,
        });
      }
      if (noteId) {
        results.push({
          type: "note",
          id: String(noteId),
          title: outputTitle || payload.title || "Social Analysis",
          text: buildStatusText({ actionType, ok, title: outputTitle || payload.title || "Social Analysis" }),
        });
      }
    } else if (reminderId) {
      results.push({
        type: "reminder",
        id: String(reminderId),
        title: outputTitle || payload.title || "Reminder",
        text: buildStatusText({ actionType: "reminder", ok, title: outputTitle || payload.title || "Reminder", scheduledAt: outputScheduledAt }),
        scheduledAt: outputScheduledAt || null,
      });
    } else if (taskId) {
      const typeForTask = actionType === "release" || actionType === "merch_task" ? "task" : actionType;
      results.push({
        type: typeForTask,
        id: String(taskId),
        title: outputTitle || payload.title || "Task",
        text: buildStatusText({
          actionType,
          ok,
          title: outputTitle || payload.title || "Task",
          dueAt: outputDueAt,
          deduplicated,
        }),
        dueAt: outputDueAt || null,
      });
    } else if (noteId) {
      results.push({
        type: "note",
        id: String(noteId),
        title: outputTitle || payload.title || "Notiz",
        text: buildStatusText({ actionType, ok, title: outputTitle || payload.title || "Notiz" }),
      });
    } else if (ok) {
      results.push({
        type: "text",
        title: "Hinweis",
        text: "Cloud-Antwort ohne reminderId/taskId/noteId im Body. Roh-Body pruefen.",
      });
    }

    const userMessage = buildStatusText({
      actionType,
      ok,
      title: outputTitle || payload.title || actionType,
      scheduledAt: outputScheduledAt,
      dueAt: outputDueAt,
      deduplicated,
    });

    const responseForWebhook = {
      message: userMessage,
      workflowStatus: ok ? "completed" : "failed",
      private: actionType === "briefing" ? privateText || "" : "",
      group: actionType === "briefing" ? groupText || "" : "",
      results,
    };

    return {
      message: responseForWebhook.message,
      workflowStatus: responseForWebhook.workflowStatus,
      results,
      private: actionType === "briefing" ? privateText || null : null,
      group: actionType === "briefing" ? groupText || null : null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        uid,
        mode,
        appExecutionMode: appExecutionMode || null,
        ...workflowSecretDiagnostics,
        generatedAt: new Date().toISOString(),
        functionName,
        actionType,
        httpStatus: response.status,
        cloudBodyOk: responseBody?.ok,
        reminderId: reminderId || null,
        taskId: taskId || null,
        noteId: noteId || null,
        deduplicated: deduplicated === true,
        actions: [
          {
            type: actionType,
            functionName,
            status: ok ? "completed" : "failed",
            httpStatus: response.status,
            attemptCount: attempts,
            durationMs,
            requestId: payload.requestId,
            briefingTarget: actionType === "briefing" ? briefingTarget : undefined,
            requiresOwner: actionType === "briefing" ? requiresOwner : undefined,
          },
        ],
      },
    };
  } catch (err) {
    const responseForWebhook = {
      message: "Aktion verstanden, aber technisch fehlgeschlagen.",
      workflowStatus: "failed",
      private: "",
      group: "",
      results: [
        {
          type: "text",
          title: "Ausfuehrung",
          text: (err && err.message) || "network_or_runtime_error",
        },
      ],
    };

    return {
      message: responseForWebhook.message,
      workflowStatus: "failed",
      results: responseForWebhook.results,
      private: null,
      group: null,
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
        error: err instanceof Error ? err.message : "network_or_runtime_error",
      },
    };
  }
};
