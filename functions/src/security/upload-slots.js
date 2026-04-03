"use strict";

const admin = require("firebase-admin");
const {HttpsError} = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const {
  ALLOWED_IMAGE_CONTENT_TYPES,
  SECURITY_LIMITS,
  UPLOAD_SLOT_COLLECTION,
  UPLOAD_USAGE_COLLECTION,
} = require("./constants");
const {getRuntimeConfig, areUploadsBlocked} = require("./runtime-config");
const {resolveRoleFromClaims} = require("./roles");

function nonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0 ? value.trim() : null;
}

function normalizeMimeType(value) {
  const mimeType = nonEmptyString(value)?.toLowerCase();
  return ALLOWED_IMAGE_CONTENT_TYPES.includes(mimeType) ? mimeType : null;
}

function normalizeUploadKind(value) {
  const kind = nonEmptyString(value)?.toLowerCase();
  return kind === "profile" || kind === "gallery" || kind === "asset" ? kind : null;
}

function resolveMaxUploadsPer24Hours(role, kind) {
  if (role === "owner") {
    return 250;
  }

  if (role === "admin") {
    return 120;
  }

  return kind === "asset" ? 40 : SECURITY_LIMITS.maxUploadsPer24Hours;
}

function resolveMaxGalleryImagesPerUser(role) {
  if (role === "owner") {
    return 60;
  }

  if (role === "admin") {
    return 30;
  }

  return SECURITY_LIMITS.maxGalleryImagesPerUser;
}

function resolveExtension(mimeType, requestedExtension) {
  const normalizedRequested = nonEmptyString(requestedExtension)?.toLowerCase()?.replace(/^\./, "");
  const fallbackByMime = {
    "image/jpeg": "jpg",
    "image/png": "png",
    "image/webp": "webp",
  };
  const expected = fallbackByMime[mimeType] || "jpg";
  return normalizedRequested && ["jpg", "jpeg", "png", "webp"].includes(normalizedRequested) ?
    normalizedRequested :
    expected;
}

async function loadUploadUsage(uid) {
  const snapshot = await admin.firestore().collection(UPLOAD_USAGE_COLLECTION).doc(uid).get();
  return snapshot.exists ? (snapshot.data() || {}) : {};
}

async function countGalleryItems(uid) {
  const snapshot = await admin.firestore()
      .collection("galleryMeta")
      .doc(uid)
      .collection("items")
      .limit(SECURITY_LIMITS.maxGalleryImagesPerUser + 1)
      .get();
  return snapshot.size;
}

function nextUsageWindow(now) {
  return {
    startedAt: admin.firestore.Timestamp.fromDate(now),
    expiresAt: admin.firestore.Timestamp.fromDate(new Date(now.getTime() + (24 * 60 * 60 * 1000))),
    count: 0,
  };
}

async function requestUploadSlot({auth, data, appCheckState}) {
  if (!auth?.uid) {
    throw new HttpsError("unauthenticated", "Bitte melde dich an.");
  }

  const role = resolveRoleFromClaims(auth);
  const requestedUserId = nonEmptyString(data?.userId);
  if (requestedUserId && requestedUserId !== auth.uid) {
    throw new HttpsError("permission-denied", "Uploads sind nur fuer das eigene Konto erlaubt.");
  }

  const runtimeConfig = await getRuntimeConfig();
  if (areUploadsBlocked(runtimeConfig, role)) {
    return {
      allowed: false,
      reason: "uploads_disabled",
      message: "Uploads sind derzeit deaktiviert.",
    };
  }

  const kind = normalizeUploadKind(data?.kind);
  if (!kind) {
    throw new HttpsError("invalid-argument", "kind muss profile, gallery oder asset sein.");
  }

  const mimeType = normalizeMimeType(data?.mimeType);
  if (!mimeType) {
    return {
      allowed: false,
      reason: "invalid_content_type",
      message: "Nur JPEG, PNG und WEBP sind erlaubt.",
    };
  }

  const declaredBytes = Number(data?.byteSize);
  if (Number.isFinite(declaredBytes) && declaredBytes > SECURITY_LIMITS.maxImageBytes) {
    return {
      allowed: false,
      reason: "file_too_large",
      message: "Die Datei ist zu gross.",
      maxBytes: SECURITY_LIMITS.maxImageBytes,
    };
  }

  const extension = resolveExtension(mimeType, data?.fileExtension);
  const maxGalleryImagesPerUser = resolveMaxGalleryImagesPerUser(role);
  const galleryCount = kind === "gallery" ? await countGalleryItems(auth.uid) : 0;
  if (kind === "gallery" && galleryCount >= maxGalleryImagesPerUser) {
    return {
      allowed: false,
      reason: "gallery_limit_reached",
      message: "Das Galerie-Limit ist erreicht.",
      maxGalleryImagesPerUser,
    };
  }

  const usage = await loadUploadUsage(auth.uid);
  const now = new Date();
  const existingWindowExpiry = usage.windowExpiresAt?.toDate?.();
  const isWindowActive = existingWindowExpiry instanceof Date && existingWindowExpiry.getTime() > now.getTime();
  const nextWindow = isWindowActive ? {
    startedAt: usage.windowStartedAt || admin.firestore.Timestamp.fromDate(now),
    expiresAt: usage.windowExpiresAt,
    count: Number(usage.windowUploadCount) || 0,
  } : nextUsageWindow(now);

  const maxUploadsPer24Hours = resolveMaxUploadsPer24Hours(role, kind);
  if (nextWindow.count >= maxUploadsPer24Hours) {
    return {
      allowed: false,
      reason: "daily_upload_limit_reached",
      message: "Das Upload-Limit fuer 24 Stunden ist erreicht.",
      maxUploadsPer24Hours,
      windowExpiresAt: nextWindow.expiresAt.toDate().toISOString(),
    };
  }

  const slotId = admin.firestore().collection(UPLOAD_SLOT_COLLECTION).doc().id;
  const fileName = kind === "profile" ? `avatar.${extension}` : `${slotId}.${extension}`;
  const storagePath = `users/${auth.uid}/${kind}/${fileName}`;
  const expiresAt = admin.firestore.Timestamp.fromDate(
      new Date(now.getTime() + (SECURITY_LIMITS.uploadSlotTtlMinutes * 60 * 1000)),
  );

  await admin.firestore().collection(UPLOAD_SLOT_COLLECTION).doc(slotId).set({
    ownerUid: auth.uid,
    kind,
    storagePath,
    fileName,
    contentType: mimeType,
    maxBytes: SECURITY_LIMITS.maxImageBytes,
    status: "approved",
    declaredBytes: Number.isFinite(declaredBytes) ? Math.floor(declaredBytes) : null,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    expiresAt,
    appCheckAppId: appCheckState.appId || null,
  });

  await admin.firestore().collection(UPLOAD_USAGE_COLLECTION).doc(auth.uid).set({
    ownerUid: auth.uid,
    windowStartedAt: nextWindow.startedAt,
    windowExpiresAt: nextWindow.expiresAt,
    windowUploadCount: nextWindow.count + 1,
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  }, {merge: true});

  logger.info("Upload slot granted.", {
    uid: auth.uid,
    slotId,
    kind,
    mimeType,
    appCheckMissing: appCheckState.missing === true,
  });

  return {
    allowed: true,
    slotId,
    kind,
    storagePath,
    fileName,
    maxBytes: SECURITY_LIMITS.maxImageBytes,
    expiresAt: expiresAt.toDate().toISOString(),
    metadata: {
      uploadSlotId: slotId,
      ownerUid: auth.uid,
    },
  };
}

module.exports = {
  requestUploadSlot,
};
