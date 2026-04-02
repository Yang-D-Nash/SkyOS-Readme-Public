"use strict";

const OWNER_EMAIL = "nash.lioncorna@gmail.com";

const USER_ROLES = Object.freeze({
  owner: "owner",
  admin: "admin",
  subadmin: "subadmin",
  user: "user",
});

const VALID_ROLES = Object.freeze(Object.values(USER_ROLES));

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

const SECURITY_LIMITS = Object.freeze({
  maxGalleryImagesPerUser: 10,
  maxUploadsPer24Hours: 20,
  maxImageBytes: 5 * 1024 * 1024,
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
  appCheckMode: APP_CHECK_MODES.monitor,
  budgetLockdownEnabled: false,
  lastLockdownReason: "",
});

module.exports = {
  ALLOWED_IMAGE_CONTENT_TYPES,
  APP_CHECK_MODES,
  BILLING_LOCKDOWN_REASON_PREFIX,
  DEFAULT_RUNTIME_CONFIG,
  OWNER_EMAIL,
  RUNTIME_CONFIG_PATH,
  SECURITY_LIMITS,
  UPLOAD_SLOT_COLLECTION,
  UPLOAD_USAGE_COLLECTION,
  USER_ROLES,
  VALID_ROLES,
};
