"use strict";

const crypto = require("node:crypto");
const fs = require("node:fs");
const path = require("node:path");
const {
  Environment,
  SignedDataVerifier,
} = require("@apple/app-store-server-library");

const APP_ACCOUNT_TOKEN_NAMESPACE_UUID = "8fadb2be-b2a4-4cd4-b17d-7aa3c3ddf4e5";
const APPLE_ROOT_CERTIFICATE_FILES = Object.freeze([
  "AppleIncRootCertificate.cer",
  "AppleRootCA-G2.cer",
  "AppleRootCA-G3.cer",
]);

let appleRootCertificatesCache = null;
const verifierCache = new Map();

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function uuidBytes(uuidValue) {
  const normalized = nonEmptyString(uuidValue)?.replace(/-/g, "").toLowerCase() || "";
  if (!/^[0-9a-f]{32}$/.test(normalized)) {
    throw new Error("UUID-Namespace fuer App Account Token ist ungueltig.");
  }
  return Buffer.from(normalized, "hex");
}

function formatUuidFromBytes(buffer) {
  const hex = Buffer.from(buffer).toString("hex");
  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20, 32),
  ].join("-");
}

function createDeterministicAppAccountToken(userId) {
  const normalizedUserId = nonEmptyString(userId);
  if (!normalizedUserId) {
    throw new Error("UID fuer App Account Token fehlt.");
  }

  const namespaceBytes = uuidBytes(APP_ACCOUNT_TOKEN_NAMESPACE_UUID);
  const nameBytes = Buffer.from(normalizedUserId, "utf8");
  const hash = crypto.createHash("sha1")
      .update(namespaceBytes)
      .update(nameBytes)
      .digest();
  const bytes = Buffer.from(hash.subarray(0, 16));
  bytes[6] = (bytes[6] & 0x0f) | 0x50;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;
  return formatUuidFromBytes(bytes);
}

function loadAppleRootCertificates() {
  if (appleRootCertificatesCache) {
    return appleRootCertificatesCache;
  }

  appleRootCertificatesCache = APPLE_ROOT_CERTIFICATE_FILES.map((fileName) => fs.readFileSync(
      path.join(__dirname, "apple-certs", fileName),
  ));
  return appleRootCertificatesCache;
}

function verifierCacheKey({environment, bundleId, appAppleId, enableOnlineChecks}) {
  return [
    environment,
    bundleId,
    Number.isFinite(appAppleId) && appAppleId > 0 ? `${Math.floor(appAppleId)}` : "none",
    enableOnlineChecks === true ? "online" : "offline",
  ].join(":");
}

function getVerifier({
  environment,
  bundleId,
  appAppleId,
  enableOnlineChecks = false,
}) {
  const key = verifierCacheKey({
    environment,
    bundleId,
    appAppleId,
    enableOnlineChecks,
  });
  if (!verifierCache.has(key)) {
    verifierCache.set(key, new SignedDataVerifier(
        loadAppleRootCertificates(),
        enableOnlineChecks === true,
        environment,
        bundleId,
        environment === Environment.PRODUCTION ? Math.floor(appAppleId) : undefined,
    ));
  }
  return verifierCache.get(key);
}

async function verifyAndDecodeAppStoreTransaction({
  signedTransaction,
  bundleId,
  appAppleId,
  enableOnlineChecks = false,
}) {
  const normalizedSignedTransaction = nonEmptyString(signedTransaction);
  if (!normalizedSignedTransaction) {
    throw new Error("Signierte App-Store-Transaktion fehlt.");
  }

  let sandboxError = null;
  try {
    const payload = await getVerifier({
      environment: Environment.SANDBOX,
      bundleId,
      appAppleId,
      enableOnlineChecks,
    }).verifyAndDecodeTransaction(normalizedSignedTransaction);
    return {environment: Environment.SANDBOX, payload};
  } catch (error) {
    sandboxError = error;
  }

  const normalizedAppleAppId = Number(appAppleId || 0);
  if (!Number.isFinite(normalizedAppleAppId) || normalizedAppleAppId <= 0) {
    throw sandboxError;
  }

  const payload = await getVerifier({
    environment: Environment.PRODUCTION,
    bundleId,
    appAppleId: normalizedAppleAppId,
    enableOnlineChecks,
  }).verifyAndDecodeTransaction(normalizedSignedTransaction);
  return {environment: Environment.PRODUCTION, payload};
}

async function decodeAppStoreTransactions({
  signedTransactions,
  productIdToPlan,
  userId,
  bundleId,
  appAppleId,
  enableOnlineChecks = false,
}) {
  const normalizedSignedTransactions = (Array.isArray(signedTransactions) ? signedTransactions : [signedTransactions])
      .map((value) => nonEmptyString(value))
      .filter(Boolean);
  const expectedAppAccountToken = createDeterministicAppAccountToken(userId).toLowerCase();
  const knownProductIds = new Set(Object.keys(productIdToPlan || {}));
  const decodedTransactions = [];
  const rejectedTransactions = [];

  for (const signedTransaction of normalizedSignedTransactions) {
    try {
      const decoded = await verifyAndDecodeAppStoreTransaction({
        signedTransaction,
        bundleId,
        appAppleId,
        enableOnlineChecks,
      });
      const productId = nonEmptyString(decoded.payload?.productId);
      if (!productId || !knownProductIds.has(productId)) {
        continue;
      }

      const appAccountToken = nonEmptyString(decoded.payload?.appAccountToken)?.toLowerCase();
      if (!appAccountToken || appAccountToken !== expectedAppAccountToken) {
        rejectedTransactions.push({
          reason: "app_account_token_mismatch",
          productId,
        });
        continue;
      }

      decodedTransactions.push({
        environment: decoded.environment,
        payload: decoded.payload,
        plan: productIdToPlan[productId] || null,
      });
    } catch (error) {
      rejectedTransactions.push({
        reason: "verification_failed",
        message: nonEmptyString(error?.message) || "App-Store-Transaktion konnte nicht verifiziert werden.",
      });
    }
  }

  return {
    decodedTransactions,
    rejectedTransactions,
    expectedAppAccountToken,
    receivedTransactionCount: normalizedSignedTransactions.length,
  };
}

function sortResolvedTransactions(transactions) {
  return [...transactions].sort((left, right) => {
    const leftExpires = Number(left.expiresDateMs || 0);
    const rightExpires = Number(right.expiresDateMs || 0);
    if (rightExpires !== leftExpires) {
      return rightExpires - leftExpires;
    }

    const leftSigned = Number(left.signedDateMs || 0);
    const rightSigned = Number(right.signedDateMs || 0);
    if (rightSigned !== leftSigned) {
      return rightSigned - leftSigned;
    }

    const leftPurchase = Number(left.purchaseDateMs || 0);
    const rightPurchase = Number(right.purchaseDateMs || 0);
    return rightPurchase - leftPurchase;
  });
}

function resolveAppStoreSubscriptionState(decodedTransactions, nowMs = Date.now()) {
  if (!Array.isArray(decodedTransactions) || decodedTransactions.length === 0) {
    return {status: "inactive"};
  }

  const resolvedTransactions = decodedTransactions.map((entry) => {
    const payload = entry.payload || {};
    const expiresDateMs = Number(payload.expiresDate || 0);
    const purchaseDateMs = Number(payload.purchaseDate || 0);
    const signedDateMs = Number(payload.signedDate || 0);
    const revocationDateMs = Number(payload.revocationDate || 0);

    return {
      plan: entry.plan || null,
      productId: nonEmptyString(payload.productId) || "",
      transactionId: nonEmptyString(payload.transactionId) || "",
      originalTransactionId: nonEmptyString(payload.originalTransactionId) || "",
      environment: nonEmptyString(entry.environment) || "",
      expiresDateMs: Number.isFinite(expiresDateMs) && expiresDateMs > 0 ? Math.floor(expiresDateMs) : 0,
      purchaseDateMs: Number.isFinite(purchaseDateMs) && purchaseDateMs > 0 ? Math.floor(purchaseDateMs) : 0,
      signedDateMs: Number.isFinite(signedDateMs) && signedDateMs > 0 ? Math.floor(signedDateMs) : 0,
      revocationDateMs: Number.isFinite(revocationDateMs) && revocationDateMs > 0 ? Math.floor(revocationDateMs) : 0,
    };
  });

  const activeTransaction = sortResolvedTransactions(
      resolvedTransactions.filter((entry) => entry.revocationDateMs <= 0 && entry.expiresDateMs > nowMs),
  )[0];
  if (activeTransaction) {
    return {
      status: "active",
      plan: activeTransaction.plan,
      productId: activeTransaction.productId,
      transactionId: activeTransaction.transactionId,
      originalTransactionId: activeTransaction.originalTransactionId,
      environment: activeTransaction.environment,
      currentPeriodEndEpochSeconds: Math.floor(activeTransaction.expiresDateMs / 1000),
      provider: "app_store",
      sourcePlatform: "ios",
    };
  }

  const latestTransaction = sortResolvedTransactions(resolvedTransactions)[0];
  if (!latestTransaction) {
    return {status: "inactive"};
  }

  const status = latestTransaction.revocationDateMs > 0 ?
    "canceled" :
    latestTransaction.expiresDateMs > 0 && latestTransaction.expiresDateMs <= nowMs ?
      "expired" :
      "inactive";

  return {
    status,
    plan: latestTransaction.plan,
    productId: latestTransaction.productId,
    transactionId: latestTransaction.transactionId,
    originalTransactionId: latestTransaction.originalTransactionId,
    environment: latestTransaction.environment,
    currentPeriodEndEpochSeconds: latestTransaction.expiresDateMs > 0 ?
      Math.floor(latestTransaction.expiresDateMs / 1000) :
      null,
    provider: status === "inactive" ? null : "app_store",
    sourcePlatform: status === "inactive" ? null : "ios",
  };
}

module.exports = {
  createDeterministicAppAccountToken,
  decodeAppStoreTransactions,
  resolveAppStoreSubscriptionState,
};
