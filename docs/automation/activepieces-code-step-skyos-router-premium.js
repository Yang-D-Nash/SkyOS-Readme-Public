/**
 * Activepieces – Code (Premium Router) für SkyOS Agent → Webhook
 * ----------------------------------------------------------------
 * Einfuegen: Code Step → "Run Javascript" (export const code = async (inputs) => { ... })
 *
 * Erwartet in `inputs` (oder verschachtelt):
 *   - projectId (z. B. skydown-a6add)
 *   - workflowSecret (muss mit Firebase HTTP Workflow-Secret uebereinstimmen)
 *   - trigger.body / body / payload als Objekt ODER JSON-String (AP variiert)
 *
 * SkyOS sendet u. a.:
 *   data.mode = "automation" fuer Modus Analyse
 *   data.socialContext.intent = "social_analysis" wenn Plattform-Toggles / Intent
 *   data.prompt, data.reply, data.history, data.attachments, data.automationScope
 *   user.uid, user.isOwner (wenn sendsUserContext in den Automation-Settings an ist)
 *
 * Return: JSON passend zu docs/automation/agent-results-contract.md (results[].text, nicht "content")
 *
 * Version: skyos.activepieces.router.premium.v3.1
 */
export const code = async (inputs) => {
  const startedAt = Date.now();
  const schemaVersion = "skyos.activepieces.router.premium.v3.1";

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

  const clean = (v, fallback = "", max = 5000) =>
    String(v ?? "").trim().slice(0, max) || fallback;

  const parseBool = (v) => {
    if (v === true) return true;
    if (v === false) return false;
    const s = String(v ?? "").trim().toLowerCase();
    return s === "true" || s === "1" || s === "yes";
  };

  const isValidYmd = (value) => {
    const s = String(value || "").trim();
    if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
    const d = new Date(`${s}T00:00:00.000Z`);
    return !Number.isNaN(d.getTime()) && d.toISOString().slice(0, 10) === s;
  };

  const isValidIsoDateTime = (value) => {
    const s = String(value || "").trim();
    return !!s && !Number.isNaN(Date.parse(s)) && s.includes("T");
  };

  const normalizePriority = (value, fallback = "normal") => {
    const p = String(value || "").trim().toLowerCase();
    return ["low", "normal", "high"].includes(p) ? p : fallback;
  };

  const formatSocialContextForNote = (sc) => {
    if (!sc || typeof sc !== "object") return "(kein socialContext)";
    const intent = clean(sc.intent, "", 80);
    const sp = asObject(sc.socialProfiles) || {};
    const selected = Array.isArray(sc.selectedPlatforms) ? sc.selectedPlatforms.join(", ") : "";
    const missing = Array.isArray(sc.missingPlatforms) ? sc.missingPlatforms.join(", ") : "";
    const lines = [
      `Intent: ${intent || "—"}`,
      "Handles (merged):",
      `  · instagram: ${sp.instagram || "—"}`,
      `  · tiktok: ${sp.tiktok || "—"}`,
      `  · youtube: ${sp.youtube || "—"}`,
      `  · facebook: ${sp.facebook || "—"}`,
      `  · spotify: ${sp.spotify || "—"}`,
      `Aktiv gewaehlt: ${selected || "—"}`,
      `Noch leer (Hinweis): ${missing || "—"}`,
    ];
    return lines.join("\n");
  };

  const buildAutomationNoteBody = (data) => {
    const prompt = clean(data?.prompt, "", 4000);
    const reply = clean(data?.reply, "", 4000);
    const scope = clean(data?.automationScope, "", 40);
    const att = Array.isArray(data?.attachments) ? data.attachments.length : 0;
    const sc = formatSocialContextForNote(data?.socialContext);
    const socialContext = asObject(data?.socialContext) || {};
    const platformStatus = [
      ["instagram", "Instagram", socialContext.instagramPublicGraphSummary],
      ["tiktok", "TikTok", socialContext.tiktokPublicSummary],
      ["youtube", "YouTube", socialContext.youtubePublicCatalogSummary],
      ["facebook", "Facebook/Meta", socialContext.facebookMetaSummary],
      ["spotify", "Spotify", socialContext.spotifyPublicCatalogSummary],
    ]
      .map(([platform, label, value]) => {
        const hasHandle = clean((asObject(socialContext.socialProfiles) || {})[platform], "", 240);
        const hasLive = clean(value, "", 1200);
        if (hasLive) return `- ${label}: Live-Daten erhalten.`;
        if (hasHandle) return `- ${label}: Handle vorhanden, Live-API-Daten nicht verfuegbar.`;
        return `- ${label}: kein Handle im Payload.`;
      })
      .join("\n");
    const block = [
      "## SkyOS · Agent Automation",
      "",
      `**automationScope:** ${scope || "—"}`,
      `**Anhaenge (Anzahl):** ${att}`,
      "",
      "### Nutzer-Prompt",
      prompt || "—",
      "",
      "### Agent-Antwort (Snapshot)",
      reply || "—",
      "",
      "### Datenstatus",
      platformStatus || "—",
      "",
      "### Social-Kontext",
      sc,
    ].join("\n");
    return block.slice(0, 5000);
  };

  // ---------- Input normalization (AP liefert oft body als String) ----------
  const root = asObject(inputs) || {};

  const bodyCandidate =
    asObject(root.body) ||
    asObject(root.payload) ||
    asObject(root.trigger?.body) ||
    parseJsonSafe(root.body) ||
    parseJsonSafe(root.payload) ||
    parseJsonSafe(root.rawBody) ||
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

  const userObj = asObject(body.user) || asObject(data.user) || {};

  // ---------- Config ----------
  const projectId = clean(root?.projectId, "skydown-a6add", 100);
  const workflowSecret = clean(root?.workflowSecret, "", 300);

  const requestId = clean(
    body?.requestId || data?.requestId || `skyos-${Date.now()}`,
    `skyos-${Date.now()}`,
    120,
  );

  const uid =
    body?.uid ||
    data?.uid ||
    userObj?.uid ||
    null;

  const rawMode = clean(data?.mode || body?.mode || body?.action, "", 80).toLowerCase();
  const socialIntent = clean(
    data?.socialContext?.intent || body?.socialContext?.intent,
    "",
    80,
  ).toLowerCase();

  /** SkyOS: Modus „Analyse“ = data.mode "automation". Social-Flag sitzt in socialContext.intent. */
  const effectiveMode =
    rawMode === "automation" && socialIntent === "social_analysis" ?
      "social_analysis" :
    rawMode === "automation" && socialIntent === "" ?
      "automation" :
    rawMode;

  const traceId = `${requestId}-${Math.random().toString(36).slice(2, 8)}`;
  const todayYmd = new Date().toISOString().slice(0, 10);
  const baseUrl = `https://us-central1-${projectId}.cloudfunctions.net`;

  const allowedModes = [
    "release",
    "briefing",
    "content",
    "merch",
    "social_analysis",
    "automation",
  ];

  const fail = (message, reason, extra = {}) => {
    const responseForWebhook = {
      message,
      workflowStatus: "failed",
      private: "",
      group: "",
    };
    return {
      message,
      workflowStatus: "failed",
      results: [
        {
          type: "text",
          title: "Nicht ausfuehrbar",
          text: message,
        },
      ],
      private: null,
      group: null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        mode: effectiveMode || null,
        rawMode: rawMode || null,
        socialIntent: socialIntent || null,
        uid: uid || null,
        reason,
        generatedAt: new Date().toISOString(),
        ...extra,
      },
    };
  };

  // ---------- Preflight ----------
  if (!projectId) return fail("PROJECT_ID fehlt.", "project_id_missing");
  if (!workflowSecret) return fail("Workflow-Secret fehlt.", "workflow_secret_missing");
  if (!uid) {
    return fail("Firebase UID fehlt. Pruefe sendsUserContext in den SkyOS-Automation-Settings.", "uid_missing", {
      debugBodyKeys: Object.keys(body || {}),
      hasUser: !!Object.keys(userObj).length,
    });
  }
  if (!rawMode) return fail("data.mode fehlt.", "mode_missing");
  if (!allowedModes.includes(effectiveMode)) {
    return fail("Ungueltiger oder nicht unterstuetzter Modus (nach Abbildung).", "invalid_mode", {
      allowedModes,
      rawMode,
      effectiveMode,
    });
  }

  // ---------- Owner logic (Briefing Group) ----------
  const isOwner = parseBool(body?.isOwner ?? data?.isOwner ?? userObj?.isOwner);
  const targetRaw = clean(
    data?.briefingTarget || body?.briefingTarget || data?.target || body?.target,
    "both",
    20,
  ).toLowerCase();
  const briefingTarget = ["private", "group", "both"].includes(targetRaw) ? targetRaw : "both";
  const requiresOwner = effectiveMode === "briefing" && (briefingTarget === "group" || briefingTarget === "both");
  if (requiresOwner && !isOwner) {
    return fail(
      "Nur Owner darf Group/Both-Briefing ausfuehren.",
      "owner_required_for_group_briefing",
      { briefingTarget, isOwner: false },
    );
  }

  const returnAnalysisOutputOnly = ({ title, text, action = "social_analysis" }) => {
    const durationMs = Date.now() - startedAt;
    const results = [
      {
        type: "text",
        title,
        text,
      },
    ];
    const responseForWebhook = {
      message: "Analyse bereit. Im Verlauf nutzbar; als Notiz nur bei Bedarf speichern.",
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
        mode: effectiveMode,
        rawMode,
        socialIntent: socialIntent || null,
        generatedAt: new Date().toISOString(),
        savedToNotes: false,
        persistence: "chat_history_only",
        actions: [
          {
            type: action,
            functionName: null,
            status: "completed",
            httpStatus: null,
            attemptCount: 0,
            durationMs,
            requestId: `${requestId}-${action}`,
            savedToNotes: false,
          },
        ],
      },
    };
  };

  // ---------- Routing ----------
  let functionName = null;
  let payload = null;
  let actionType = null;

  if (effectiveMode === "briefing") {
    const date = clean(data?.date || body?.date, todayYmd, 20);
    if (!isValidYmd(date)) {
      return fail("Fuer briefing fehlt valides date (YYYY-MM-DD).", "briefing_date_invalid");
    }
    functionName = "createFounderBriefingFromWorkflow";
    actionType = "briefing";
    payload = {
      uid,
      mode: briefingTarget,
      date,
      source: "activepieces",
      requestId: `${requestId}-briefing`,
    };
  } else if (effectiveMode === "release") {
    const title = clean(data?.title || body?.title, "", 180);
    if (!title) return fail("Fuer release fehlt title.", "release_title_missing");
    functionName = "createTaskFromWorkflow";
    actionType = "release";
    payload = {
      uid,
      title,
      description: clean(
        data?.description || body?.description,
        "Release-Aufgabe aus Workflow.",
        5000,
      ),
      priority: normalizePriority(data?.priority || body?.priority, "high"),
      source: "activepieces",
      requestId: `${requestId}-release`,
    };
    const dueAt = data?.dueAt || body?.dueAt;
    if (isValidIsoDateTime(dueAt)) payload.dueAt = String(dueAt).trim();
  } else if (effectiveMode === "content") {
    functionName = "createNoteFromWorkflow";
    actionType = "content";
    payload = {
      uid,
      title: clean(data?.title || body?.title, "Content Notiz", 180),
      content: clean(
        data?.content || body?.content || data?.description || body?.description,
        "Content-Input aus Workflow.",
        5000,
      ),
      source: "activepieces",
      requestId: `${requestId}-content`,
    };
  } else if (effectiveMode === "merch") {
    const notePreferred = parseBool(data?.notePreferred ?? body?.notePreferred);
    if (notePreferred) {
      functionName = "createNoteFromWorkflow";
      actionType = "merch_note";
      payload = {
        uid,
        title: clean(data?.title || body?.title, "Merch Notiz", 180),
        content: clean(
          data?.content || body?.content || data?.description || body?.description,
          "Merch-Workflow Input.",
          5000,
        ),
        source: "activepieces",
        requestId: `${requestId}-merch-note`,
      };
    } else {
      functionName = "createTaskFromWorkflow";
      actionType = "merch_task";
      payload = {
        uid,
        title: clean(data?.title || body?.title, "Merch Task", 180),
        description: clean(
          data?.content || body?.content || data?.description || body?.description,
          "Merch-Workflow Input.",
          5000,
        ),
        priority: normalizePriority(data?.priority || body?.priority, "normal"),
        source: "activepieces",
        requestId: `${requestId}-merch-task`,
      };
      const dueAt = data?.dueAt || body?.dueAt;
      if (isValidIsoDateTime(dueAt)) payload.dueAt = String(dueAt).trim();
    }
  } else if (effectiveMode === "social_analysis") {
    const title = clean(
      data?.title || data?.noteTitle,
      "Social / Oeffentliche Profil-Snapshot",
      180,
    );
    const customContent = clean(data?.content || data?.analysis, "", 5000);
    const bodyText = customContent || buildAutomationNoteBody(data);
    const saveToNotes = parseBool(
      data?.saveToNotes ??
        body?.saveToNotes ??
        data?.notePreferred ??
        body?.notePreferred ??
        data?.persistToNotes ??
        body?.persistToNotes,
    );
    if (!saveToNotes) {
      return returnAnalysisOutputOnly({ title: title || "Social Analysis", text: bodyText, action: "social_analysis" });
    }
    functionName = "createNoteFromWorkflow";
    actionType = "social_analysis";
    payload = {
      uid,
      title: title || "Social Analysis",
      content: bodyText,
      source: "activepieces",
      requestId: `${requestId}-social-analysis`,
    };
  } else if (effectiveMode === "automation") {
    functionName = "createNoteFromWorkflow";
    actionType = "automation";
    payload = {
      uid,
      title: clean(data?.title, "Agent · Analyse (ohne social_analysis-Intent)", 180),
      content: buildAutomationNoteBody(data),
      source: "activepieces",
      requestId: `${requestId}-automation`,
    };
  }

  if (!functionName || !payload || !actionType) {
    return fail("Routing konnte nicht bestimmt werden.", "route_resolution_failed", { effectiveMode, rawMode });
  }

  const callOnce = async () => {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20000);
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
    const ok = response.ok;
    const durationMs = Date.now() - startedAt;

    const privateText = clean(
      responseBody?.private ??
        responseBody?.briefingPrivate ??
        responseBody?.founderBriefingMarkdown ??
        responseBody?.data?.private ??
        responseBody?.result?.private,
      "",
      12000,
    );
    const groupText = clean(
      responseBody?.group ??
        responseBody?.briefingGroup ??
        responseBody?.data?.group ??
        responseBody?.result?.group,
      "",
      12000,
    );

    const results = actionType === "social_analysis" ? [] : [
      {
        type: "table",
        title: "Ausfuehrung",
        columns: ["Modus", "Aktion", "Status", "HTTP", "Laufzeit ms"],
        rows: [[
          String(effectiveMode),
          String(actionType),
          ok ? "ok" : "error",
          String(response.status),
          String(durationMs),
        ]],
      },
    ];

    if (ok && actionType === "social_analysis") {
      const text = clean(payload?.content, "", 5000);
      if (text) {
        results.push({
          type: "text",
          title: clean(payload?.title, "Social Analysis", 180),
          text,
        });
      }
      const noteId = clean(responseBody?.noteId, "", 120);
      if (noteId) {
        results.push({
          type: "note",
          id: noteId,
          title: clean(payload?.title, "Social Analysis", 180),
          text: "Analyse als Notiz gespeichert.",
        });
      }
    } else if (ok && actionType === "automation") {
      const summary = `Notiz in Firebase angelegt (${actionType}). requestId: ${String(payload?.requestId || requestId)}`;
      results.push({
        type: "text",
        title: "Hinweis",
        text: summary,
      });
    }

    if (actionType === "briefing") {
      if (privateText) {
        results.push({ type: "text", title: "Founder Briefing (Private)", text: privateText });
      }
      if (groupText) {
        results.push({ type: "text", title: "Founder Briefing (Group)", text: groupText });
      }
      if (ok && !privateText && !groupText) {
        results.push({
          type: "text",
          title: "Hinweis",
          text: "Briefing-Callable ok, aber private/group leer — Response pruefen.",
        });
      }
    }

    const responseForWebhook = {
      message: ok
        ? "Erledigt. Aktion ausgefuehrt."
        : "SkyOS/HTTP: Antwort war nicht ok.",
      workflowStatus: ok ? "completed" : "failed",
      private: actionType === "briefing" ? (privateText || "") : "",
      group: actionType === "briefing" ? (groupText || "") : "",
    };

    return {
      message: responseForWebhook.message,
      workflowStatus: responseForWebhook.workflowStatus,
      results,
      private: actionType === "briefing" ? (privateText || null) : null,
      group: actionType === "briefing" ? (groupText || null) : null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        uid,
        mode: effectiveMode,
        rawMode,
        socialIntent: socialIntent || null,
        generatedAt: new Date().toISOString(),
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
      message: "Technischer Fehler (Netz/Timeout/Runtime).",
      workflowStatus: "failed",
      private: "",
      group: "",
    };
    return {
      message: responseForWebhook.message,
      workflowStatus: "failed",
      results: [
        {
          type: "text",
          title: "Fehler",
          text: err instanceof Error ? err.message : "network_or_runtime_error",
        },
        {
          type: "table",
          title: "Kontext",
          columns: ["rawMode", "effectiveMode", "Aktion"],
          rows: [[String(rawMode), String(effectiveMode), String(actionType || "—")]],
        },
      ],
      private: null,
      group: null,
      responseForWebhook,
      responseForWebhookJson: JSON.stringify(responseForWebhook),
      meta: {
        schemaVersion,
        traceId,
        requestId,
        uid: uid || null,
        mode: effectiveMode || null,
        rawMode: rawMode || null,
        generatedAt: new Date().toISOString(),
        error: err instanceof Error ? err.message : "network_or_runtime_error",
      },
    };
  }
};
