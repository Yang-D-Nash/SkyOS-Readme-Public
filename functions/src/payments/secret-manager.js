"use strict";

const {GoogleAuth} = require("google-auth-library");

const SECRET_MANAGER_BASE_URL = "https://secretmanager.googleapis.com/v1";
const CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";

const secretManagerAuth = new GoogleAuth({
  scopes: [CLOUD_PLATFORM_SCOPE],
});

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

async function getAccessToken() {
  const client = await secretManagerAuth.getClient();
  const tokenResponse = await client.getAccessToken();
  const accessToken = typeof tokenResponse === "string" ?
    tokenResponse :
    tokenResponse?.token;

  if (!accessToken) {
    throw new Error("Google Access Token fuer Secret Manager fehlt.");
  }

  return accessToken;
}

async function secretManagerRequest(path, {
  method = "GET",
  body = null,
  allowNotFound = false,
  allowConflict = false,
} = {}) {
  const accessToken = await getAccessToken();
  const response = await fetch(`${SECRET_MANAGER_BASE_URL}/${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    },
    body: body ? JSON.stringify(body) : undefined,
  });

  if (allowNotFound && response.status === 404) {
    return null;
  }

  if (allowConflict && response.status === 409) {
    return {conflict: true};
  }

  if (!response.ok) {
    const errorPayload = await response.json().catch(async () => ({
      error: {
        message: await response.text().catch(() => "Secret Manager Fehler."),
      },
    }));
    const message = nonEmptyString(errorPayload?.error?.message) || "Secret Manager Fehler.";
    throw new Error(message);
  }

  if (response.status === 204) {
    return {};
  }

  return response.json().catch(() => ({}));
}

async function ensureSecretExists(projectId, secretId) {
  const normalizedProjectId = nonEmptyString(projectId);
  const normalizedSecretId = nonEmptyString(secretId);
  if (!normalizedProjectId || !normalizedSecretId) {
    throw new Error("Secret Manager Projekt oder Secret-ID fehlt.");
  }

  const existing = await secretManagerRequest(
      `projects/${normalizedProjectId}/secrets/${normalizedSecretId}`,
      {allowNotFound: true},
  );
  if (existing) {
    return existing;
  }

  await secretManagerRequest(
      `projects/${normalizedProjectId}/secrets?secretId=${encodeURIComponent(normalizedSecretId)}`,
      {
        method: "POST",
        body: {
          replication: {
            automatic: {},
          },
        },
        allowConflict: true,
      },
  );

  return secretManagerRequest(`projects/${normalizedProjectId}/secrets/${normalizedSecretId}`);
}

async function addSecretVersion(projectId, secretId, secretValue) {
  const normalizedValue = nonEmptyString(secretValue);
  if (!normalizedValue) {
    throw new Error(`${secretId} ist leer.`);
  }

  await ensureSecretExists(projectId, secretId);

  return secretManagerRequest(
      `projects/${projectId}/secrets/${secretId}:addVersion`,
      {
        method: "POST",
        body: {
          payload: {
            data: Buffer.from(normalizedValue, "utf8").toString("base64"),
          },
        },
      },
  );
}

module.exports = {
  addSecretVersion,
};
