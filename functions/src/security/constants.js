"use strict";

/**
 * Single source of truth for platform owner identity (aligns with Cloud Functions
 * index.js registration/bootstrap and roles.js claim resolution).
 */
const OWNER_EMAIL = (() => {
  const raw = process.env.SKYOS_OWNER_EMAIL || process.env.SKYDOWN_OWNER_EMAIL || "";
  const trimmed = typeof raw === "string" ? raw.trim().toLowerCase() : "";
  return trimmed || "nash.lioncorna@gmail.com";
})();

/** Keep default in sync with `PlatformContactEmails.DEFAULT_SUPPORT_EMAIL` in shared KMP. */
const DEFAULT_SUPPORT_EMAIL = (() => {
  const raw = process.env.SKYOS_SUPPORT_EMAIL || process.env.SKYDOWN_SUPPORT_EMAIL || "";
  const trimmed = typeof raw === "string" ? raw.trim().toLowerCase() : "";
  return trimmed || "skydownent@gmail.com";
})();

const USER_ROLES = Object.freeze({
  owner: "owner",
  admin: "admin",
  subadmin: "subadmin",
  user: "user",
});

const VALID_ROLES = Object.freeze(Object.values(USER_ROLES));

const USER_QUOTA_PLANS = Object.freeze({
  ownerUnlimited: "owner_unlimited",
  internalTeam: "internal_team",
  free: "free",
  creator: "creator",
  studio: "studio",
});

const APP_CHECK_MODES = Object.freeze({
  off: "off",
  monitor: "monitor",
  enforce: "enforce",
});

const ALLOWED_IMAGE_CONTENT_TYPES = Object.freeze([
  "image/jpeg",
  "image/png",
  "image/webp",
]);

const ALLOWED_ASSET_VIDEO_CONTENT_TYPES = Object.freeze([
  "video/mp4",
]);

const SECURITY_LIMITS = Object.freeze({
  maxGalleryImagesPerUser: 10,
  maxUploadsPer24Hours: 20,
  maxImageBytes: 5 * 1024 * 1024,
  maxVideoBytes: 25 * 1024 * 1024,
  uploadSlotTtlMinutes: 15,
});

const RUNTIME_CONFIG_PATH = "system/runtimeConfig";
const UPLOAD_SLOT_COLLECTION = "uploadSlots";
const UPLOAD_USAGE_COLLECTION = "uploadUsage";
const BILLING_LOCKDOWN_REASON_PREFIX = "budget_alert";

const DEFAULT_RUNTIME_CONFIG = Object.freeze({
  lockdown: false,
  uploadsEnabled: true,
  registrationsEnabled: true,
  userWritesEnabled: true,
  appCheckMode: APP_CHECK_MODES.enforce,
  appCheckAiSoftFailEnabled: false,
  budgetLockdownEnabled: false,
  lastLockdownReason: "",
});

module.exports = {
  ALLOWED_ASSET_VIDEO_CONTENT_TYPES,
  ALLOWED_IMAGE_CONTENT_TYPES,
  APP_CHECK_MODES,
  BILLING_LOCKDOWN_REASON_PREFIX,
  DEFAULT_RUNTIME_CONFIG,
  DEFAULT_SUPPORT_EMAIL,
  OWNER_EMAIL,
  RUNTIME_CONFIG_PATH,
  SECURITY_LIMITS,
  UPLOAD_SLOT_COLLECTION,
  UPLOAD_USAGE_COLLECTION,
  USER_QUOTA_PLANS,
  USER_ROLES,
  VALID_ROLES,
};
