"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs/promises");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  deleteDoc,
  getDoc,
  setDoc,
  updateDoc,
  Timestamp,
} = require("firebase/firestore");
const {
  deleteObject,
  ref,
  uploadBytes,
} = require("firebase/storage");
const {buildUploadStoragePath} = require("../src/security/upload-slots");

const PROJECT_ID = "demo-skydown";
const MAX_IMAGE_BYTES = 5 * 1024 * 1024;

let testEnv;

async function seedRuntimeConfig(config = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "system", "runtimeConfig"), {
      lockdown: false,
      uploadsEnabled: true,
      registrationsEnabled: true,
      userWritesEnabled: true,
      appCheckMode: "monitor",
      ...config,
    });
  });
}

async function seedUser(uid, overrides = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "users", uid), {
      email: `${uid}@example.com`,
      username: uid,
      registrationDateEpochMillis: 1_700_000_000_000,
      isAdmin: false,
      role: "user",
      quotaPlan: "free",
      aiAccessEnabled: true,
      termsAcceptedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      privacyAcceptedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      termsVersion: "12. April 2026",
      privacyVersion: "12. April 2026",
      legalConsentSource: "rules_test_seed",
      aiConsentGiven: true,
      aiConsentUpdatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      aiConsentSource: "rules_test_seed",
      aiTextRequestsPerDay: 30,
      aiVisualRequestsPerDay: 4,
      aiAgentRequestsPerDay: 18,
      aiHistoryRetentionDays: 3,
      canManageMusicCatalog: false,
      canManageVideoCatalog: false,
      canModerateProfiles: false,
      ...overrides,
    });
  });
}

async function seedUserProfile(uid, overrides = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "userProfiles", uid), {
      ownerUid: uid,
      username: uid,
      createdAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      updatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      ...overrides,
    });
  });
}

async function seedArtistPage(artistId, overrides = {}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "artistPages", artistId), {
      slug: "janno",
      brand: "zweizwei",
      artistName: "JANNO",
      tagline: "Artist",
      bio: "Bio",
      profileImageURL: "https://example.com/avatar.jpg",
      heroImageURL: "https://example.com/hero.jpg",
      instagramURL: "https://instagram.com/janno",
      spotifyURL: "https://open.spotify.com/artist/example",
      youtubeURL: "https://youtube.com/@janno",
      editorUids: [],
      createdAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      updatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
      ...overrides,
    });
  });
}

async function seedUploadSlot({
  slotId,
  uid,
  kind,
  fileName,
  contentType = "image/jpeg",
  maxBytes = MAX_IMAGE_BYTES,
}) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "uploadSlots", slotId), {
      ownerUid: uid,
      kind,
      fileName,
      storagePath: buildUploadStoragePath(uid, kind, fileName),
      contentType,
      maxBytes,
      status: "approved",
      createdAt: Timestamp.now(),
      expiresAt: Timestamp.fromDate(new Date(Date.now() + (10 * 60 * 1000))),
    });
  });
}

test.before(async () => {
  const firestoreRules = await fs.readFile(path.resolve(__dirname, "../../firestore.rules"), "utf8");
  const storageRules = await fs.readFile(path.resolve(__dirname, "../../storage.rules"), "utf8");

  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {rules: firestoreRules},
    storage: {rules: storageRules},
  });
});

test.after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test.beforeEach(async () => {
  await testEnv.clearFirestore();
  await seedRuntimeConfig();
});

test("user darf eigene Daten lesen und Profil aktualisieren", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(getDoc(doc(aliceDb, "users", "alice")));
  await assertSucceeds(updateDoc(doc(aliceDb, "users", "alice"), {
    profileBio: "Hi there",
  }));
});

test("user darf eigenen KI-Zugang samt Consent-Status aktualisieren", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(updateDoc(doc(aliceDb, "users", "alice"), {
    aiAccessEnabled: false,
    aiConsentGiven: false,
    aiConsentUpdatedAt: Timestamp.fromDate(new Date("2026-04-03T09:00:00.000Z")),
    aiConsentSource: "android_settings",
  }));
});

test("user darf KI-Zugang nicht ohne passende Consent-Felder umstellen", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertFails(updateDoc(doc(aliceDb, "users", "alice"), {
    aiAccessEnabled: false,
  }));
});

test("user darf eigenes userProfiles Dokument fuer Bild-Uploads anlegen", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(setDoc(doc(aliceDb, "userProfiles", "alice"), {
    ownerUid: "alice",
    username: "alice",
    profileImageURL: "https://example.com/avatar.jpg",
    profileImagePath: "users/alice/profile/avatar.jpg",
    createdAt: Timestamp.fromDate(new Date("2026-04-03T08:00:00.000Z")),
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T08:00:00.000Z")),
  }));
});

test("user darf eigenes galleryMeta Bild nach Upload speichern", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(setDoc(doc(aliceDb, "galleryMeta", "alice", "items", "img_1"), {
    ownerUid: "alice",
    type: "image",
    title: "Bild 03.04 10:00",
    mediaURL: "https://firebasestorage.googleapis.com/v0/b/demo/o/users%2Falice%2Fgallery%2Fimg_1.jpg",
    thumbnailURL: "https://firebasestorage.googleapis.com/v0/b/demo/o/users%2Falice%2Fgallery%2Fimg_1.jpg",
    storagePath: "users/alice/gallery/img_1.jpg",
    contentType: "image/jpeg",
    createdAt: Timestamp.fromDate(new Date("2026-04-03T08:00:00.000Z")),
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T08:00:00.000Z")),
  }));
});

test("owner email darf eigenes User-Dokument auch ohne owner claim fuer Profilbilder aktualisieren", async () => {
  await seedUser("owneruid", {
    email: "nash.lioncorna@gmail.com",
    isAdmin: true,
    role: "owner",
    quotaPlan: "owner_unlimited",
    aiTextRequestsPerDay: 5000,
    aiVisualRequestsPerDay: 1200,
    aiAgentRequestsPerDay: 3000,
    aiHistoryRetentionDays: 30,
    canManageMusicCatalog: true,
    canManageVideoCatalog: true,
    canModerateProfiles: true,
  });
  const ownerDb = testEnv.authenticatedContext("owneruid", {
    email: "nash.lioncorna@gmail.com",
    role: "user",
  }).firestore();

  await assertSucceeds(updateDoc(doc(ownerDb, "users", "owneruid"), {
    profileImageURL: "https://example.com/avatar.jpg",
    profileImagePath: "users/owneruid/profile/avatar.jpg",
  }));
});

test("user darf keine fremden User-Daten lesen oder schreiben", async () => {
  await seedUser("alice");
  await seedUser("bob");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertFails(getDoc(doc(aliceDb, "users", "bob")));
  await assertFails(updateDoc(doc(aliceDb, "users", "bob"), {
    profileBio: "not allowed",
  }));
});

test("owner und admin duerfen User-Daten lesen, subadmin nicht", async () => {
  await seedUser("alice");
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();
  const adminDb = testEnv.authenticatedContext("admin1", {role: "admin"}).firestore();
  const subadminDb = testEnv.authenticatedContext("sub1", {role: "subadmin"}).firestore();

  await assertSucceeds(getDoc(doc(ownerDb, "users", "alice")));
  await assertSucceeds(getDoc(doc(adminDb, "users", "alice")));
  await assertFails(getDoc(doc(subadminDb, "users", "alice")));
});

test("owner darf Staff-Rechte und Limits inklusive updatedAt auf User-Dokumenten speichern", async () => {
  await seedUser("owner", {
    email: "nash.lioncorna@gmail.com",
    isAdmin: true,
    role: "owner",
    quotaPlan: "owner_unlimited",
    aiTextRequestsPerDay: 5000,
    aiVisualRequestsPerDay: 1200,
    aiAgentRequestsPerDay: 3000,
    aiHistoryRetentionDays: 30,
    canManageMusicCatalog: true,
    canManageVideoCatalog: true,
    canModerateProfiles: true,
  });
  await seedUser("alice");

  const ownerDb = testEnv.authenticatedContext("owner", {
    email: "nash.lioncorna@gmail.com",
    role: "owner",
  }).firestore();

  await assertSucceeds(updateDoc(doc(ownerDb, "users", "alice"), {
    role: "admin",
    isAdmin: true,
    quotaPlan: "internal_team",
    aiAccessEnabled: true,
    aiTextRequestsPerDay: 400,
    aiVisualRequestsPerDay: 120,
    aiAgentRequestsPerDay: 240,
    aiHistoryRetentionDays: 30,
    canManageMusicCatalog: true,
    canManageVideoCatalog: false,
    canModerateProfiles: true,
    updatedAt: Timestamp.fromDate(new Date("2026-04-20T12:00:00.000Z")),
  }));
});

test("admin-Rechte greifen in Firestore auch bei veraltetem role-Claim ueber das User-Dokument", async () => {
  await seedUser("video-admin", {
    role: "admin",
    isAdmin: true,
    canManageVideoCatalog: true,
  });

  const staleTokenDb = testEnv.authenticatedContext("video-admin", {role: "user"}).firestore();

  await assertSucceeds(setDoc(doc(staleTokenDb, "videographyHub", "stale-claim-video"), {
    title: "Stale Claim Video",
    projectName: "22xSky",
    email: "video-admin@example.com",
    notes: "",
    fileName: "stale-claim-video.mp4",
    mimeType: "video/mp4",
    downloadURL: "https://example.com/stale-claim-video.mp4",
    externalURL: "",
    embedURL: "",
    storagePath: "videos/stale-claim-video.mp4",
    uploaderName: "Video Admin",
    uploaderEmail: "video-admin@example.com",
    uploaderID: "video-admin",
    isPublic: true,
    isHomeFeatured: false,
    sourceProvider: "firebase_storage",
    sourceFileID: "",
    createdAt: Timestamp.fromDate(new Date("2026-04-20T08:00:00.000Z")),
  }));
});

test("subadmin bleibt ausserhalb von Owner- und Admin-Bereichen", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "admin", "moderation_queue"), {
      title: "Queue",
    });
    await setDoc(doc(context.firestore(), "admin", "settings"), {
      title: "Settings",
    });
  });

  const subadminDb = testEnv.authenticatedContext("sub1", {role: "subadmin"}).firestore();

  await assertFails(getDoc(doc(subadminDb, "admin", "moderation_queue")));
  await assertFails(updateDoc(doc(subadminDb, "admin", "settings"), {
    title: "Nope",
  }));
});

test("user darf eigenen Automation- und Agent-Config in adminConfig schreiben", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(setDoc(doc(aliceDb, "adminConfig", "automationN8n_alice"), {
    provider: "n8n",
    isEnabled: true,
    sendsUserContext: true,
    workflowName: "Alice Workflow",
    baseURL: "https://n8n.example.com",
    webhookPath: "webhook/skydown",
    authHeaderName: "X-Workflow-Key",
    authHeaderValue: "secret-value",
    knowledgeContext: "Brand SOP",
    updatedAt: Timestamp.now(),
  }));

  await assertSucceeds(setDoc(doc(aliceDb, "adminConfig", "agentProfile_alice"), {
    isEnabled: true,
    roleLabel: "Artist Release Strategist",
    skillProfile: "Hooks, Shotlists, Captions",
    outputFormat: "Plan + Checkliste + CTA",
    guardrails: "kein Clickbait",
    knowledgeContext: "Release Plan Q2",
    updatedAt: Timestamp.now(),
  }));
});

test("user darf keine fremden personal adminConfig Dokus schreiben", async () => {
  await seedUser("alice");
  await seedUser("bob");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertFails(setDoc(doc(aliceDb, "adminConfig", "automationN8n_bob"), {
    provider: "n8n",
    isEnabled: true,
    updatedAt: Timestamp.now(),
  }));
  await assertFails(setDoc(doc(aliceDb, "adminConfig", "agentProfile_bob"), {
    isEnabled: true,
    updatedAt: Timestamp.now(),
  }));
});

test("user darf keine globalen Owner-adminConfig Dokumente schreiben", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertFails(setDoc(doc(aliceDb, "adminConfig", "aiPromptSettings"), {
    textInstruction: "Nope",
    updatedAt: Timestamp.now(),
  }));
});

test("lockdown blockiert User-Schreibzugriffe", async () => {
  await seedRuntimeConfig({
    lockdown: true,
    uploadsEnabled: false,
    registrationsEnabled: false,
    userWritesEnabled: false,
  });
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertFails(updateDoc(doc(aliceDb, "users", "alice"), {
    profileBio: "blocked",
  }));
});

test("user darf nur mit passendem Upload-Slot in eigenen Storage-Pfad hochladen", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_gallery_001",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_001_valid.jpg",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/gallery/slot_gallery_001_valid.jpg");

  await assertSucceeds(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3, 4]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_gallery_001",
        ownerUid: "alice",
      },
    },
  ));
});

test("falscher contentType wird im Storage abgelehnt", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_gallery_002",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_002_valid.jpg",
    contentType: "image/gif",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/gallery/slot_gallery_002_valid.jpg");

  await assertFails(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/gif",
      customMetadata: {
        uploadSlotId: "slot_gallery_002",
        ownerUid: "alice",
      },
    },
  ));
});

test("Dateien ueber 5 MB werden im Storage abgelehnt", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_gallery_003",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_003_valid.jpg",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/gallery/slot_gallery_003_valid.jpg");
  const oversized = new Uint8Array(MAX_IMAGE_BYTES + 1);

  await assertFails(uploadBytes(
    fileRef,
    oversized,
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_gallery_003",
        ownerUid: "alice",
      },
    },
  ));
});

test("fremder Storage-Pfad wird abgelehnt", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_gallery_004",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_004_valid.jpg",
  });

  const storage = testEnv.authenticatedContext("bob", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/gallery/slot_gallery_004_valid.jpg");

  await assertFails(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_gallery_004",
        ownerUid: "alice",
      },
    },
  ));
});

test("user darf eigenes Gallery-Bild im Storage wieder loeschen", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_gallery_delete_001",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_delete_001_valid.jpg",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/gallery/slot_gallery_delete_001_valid.jpg");

  await assertSucceeds(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_gallery_delete_001",
        ownerUid: "alice",
      },
    },
  ));

  await assertSucceeds(deleteObject(fileRef));
});

test("fremde Gallery-Bilder duerfen im Storage nicht geloescht werden", async () => {
  await seedUser("alice");
  await seedUser("bob");
  await seedUploadSlot({
    slotId: "slot_gallery_delete_002",
    uid: "alice",
    kind: "gallery",
    fileName: "slot_gallery_delete_002_valid.jpg",
  });

  const aliceStorage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const aliceFileRef = ref(aliceStorage, "users/alice/gallery/slot_gallery_delete_002_valid.jpg");

  await assertSucceeds(uploadBytes(
    aliceFileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_gallery_delete_002",
        ownerUid: "alice",
      },
    },
  ));

  const bobStorage = testEnv.authenticatedContext("bob", {role: "user"}).storage();
  const bobFileRef = ref(bobStorage, "users/alice/gallery/slot_gallery_delete_002_valid.jpg");
  await assertFails(deleteObject(bobFileRef));
});

test("user darf eigene Asset-Bilder fuer editierbare Bereiche hochladen", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_asset_001",
    uid: "alice",
    kind: "asset",
    fileName: "slot_asset_001_valid.jpg",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/assets/slot_asset_001_valid.jpg");

  await assertSucceeds(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_asset_001",
        ownerUid: "alice",
      },
    },
  ));
});

test("owner email darf eigene Asset-Bilder auch ohne owner claim hochladen", async () => {
  await seedUser("nash-owner", {
    email: "nash.lioncorna@gmail.com",
    role: "user",
  });
  await seedUploadSlot({
    slotId: "slot_asset_owner_email_001",
    uid: "nash-owner",
    kind: "asset",
    fileName: "slot_asset_owner_email_001.jpg",
  });

  const storage = testEnv.authenticatedContext("nash-owner", {
    role: "user",
    email: "nash.lioncorna@gmail.com",
  }).storage();
  const fileRef = ref(storage, "users/nash-owner/assets/slot_asset_owner_email_001.jpg");

  await assertSucceeds(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_asset_owner_email_001",
        ownerUid: "nash-owner",
      },
    },
  ));
});

test("admin-Rechte greifen im Storage auch bei veraltetem role-Claim ueber das User-Dokument", async () => {
  await seedUser("video-admin", {
    role: "admin",
    isAdmin: true,
    canManageVideoCatalog: true,
  });

  const storage = testEnv.authenticatedContext("video-admin", {role: "user"}).storage();
  const fileRef = ref(storage, "videos/stale-claim-upload.mp4");

  await assertSucceeds(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3, 4]),
    {
      contentType: "video/mp4",
    },
  ));
});

test("asset upload slots verwenden den pluralen assets-Pfad", () => {
  assert.equal(
    buildUploadStoragePath("alice", "asset", "slot_asset_path.jpg"),
    "users/alice/assets/slot_asset_path.jpg",
  );
});

test("singularer asset-Pfad wird im Storage abgelehnt", async () => {
  await seedUser("alice");
  await seedUploadSlot({
    slotId: "slot_asset_wrong_path_001",
    uid: "alice",
    kind: "asset",
    fileName: "slot_asset_wrong_path_001.jpg",
  });

  const storage = testEnv.authenticatedContext("alice", {role: "user"}).storage();
  const fileRef = ref(storage, "users/alice/asset/slot_asset_wrong_path_001.jpg");

  await assertFails(uploadBytes(
    fileRef,
    Uint8Array.from([1, 2, 3]),
    {
      contentType: "image/jpeg",
      customMetadata: {
        uploadSlotId: "slot_asset_wrong_path_001",
        ownerUid: "alice",
      },
    },
  ));
});

test("screenHeaders bleiben oeffentlich lesbar, aber nur der Owner darf valide Home-Texte speichern", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "appConfig", "screenHeaders"), {
      homeImageURL: "https://example.com/home.jpg",
      homeEyebrow: "22xSky Home",
      homeTitle: "22xSky",
      homeSubtitle: "Alles direkt im Blick.",
      homeDetail: "Musik, Video, Merch, Tools.",
      musicHubEyebrow: "Music",
      musicHubTitle: "Music",
      musicHubSubtitle: "Releases, Artists und Studio an einem Ort.",
      musicHubDetail: "Direkt zu Songs, Beats und Studio.",
      musicHubImageURL: "",
      shopEyebrow: "Store",
      shopTitle: "Shop",
      shopSubtitle: "Produkte direkt in der App.",
      shopDetail: "Offen fuer Bestellungen.",
      shopImageURL: "",
      videoHubEyebrow: "Video",
      videoHubTitle: "Video",
      videoHubSubtitle: "Reels, Visuals und starke Kollaborationen.",
      videoHubDetail: "Clips, Looks und Leute hinter dem Vibe.",
      videoHubImageURL: "",
      updatedAt: Timestamp.fromDate(new Date("2026-04-03T10:00:00.000Z")),
    });
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();
  const userDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(getDoc(doc(guestDb, "appConfig", "screenHeaders")));
  await assertSucceeds(setDoc(doc(ownerDb, "appConfig", "screenHeaders"), {
    homeImageURL: "https://example.com/home-2.jpg",
    homeEyebrow: "Willkommen bei 22xSky",
    homeTitle: "22xSky",
    homeSubtitle: "Artists, Visuals und Merch an einem Ort.",
    homeDetail: "Hier kannst du einen echten Willkommenstext fuer neue User hinterlegen.",
    musicHubEyebrow: "Music",
    musicHubTitle: "Music",
    musicHubSubtitle: "Releases, Artists und Studio.",
    musicHubDetail: "Direkt zu Songs, Beats und Recording.",
    musicHubImageURL: "https://example.com/music.jpg",
    shopEyebrow: "Store",
    shopTitle: "Shop",
    shopSubtitle: "Merch direkt in der App.",
    shopDetail: "Exklusive Drops und Essentials.",
    shopImageURL: "",
    videoHubEyebrow: "Video",
    videoHubTitle: "Video",
    videoHubSubtitle: "Reels, Visuals und starke Kollabos.",
    videoHubDetail: "Clips, Looks und Leute hinter dem Vibe.",
    videoHubImageURL: "",
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T11:00:00.000Z")),
  }));
  await assertFails(updateDoc(doc(userDb, "appConfig", "screenHeaders"), {
    homeTitle: "Nope",
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T11:00:00.000Z")),
  }));
});

test("owner email darf screenHeaders auch ohne owner claim speichern", async () => {
  const ownerEmailDb = testEnv.authenticatedContext("nash-owner", {
    role: "user",
    email: "nash.lioncorna@gmail.com",
  }).firestore();

  await assertSucceeds(setDoc(doc(ownerEmailDb, "appConfig", "screenHeaders"), {
    homeImageURL: "https://example.com/home-owner-email.jpg",
    homeEyebrow: "Willkommen",
    homeTitle: "Owner Mail Fallback",
    homeSubtitle: "Rules und UI sind jetzt gleichgezogen.",
    homeDetail: "Dieses Dokument darf ueber die feste Owner-Mail geschrieben werden.",
    musicHubEyebrow: "Music",
    musicHubTitle: "Music",
    musicHubSubtitle: "Releases, Artists und Studio.",
    musicHubDetail: "Direkt zu Songs, Beats und Recording.",
    musicHubImageURL: "https://example.com/music.jpg",
    shopEyebrow: "Store",
    shopTitle: "Shop",
    shopSubtitle: "Merch direkt in der App.",
    shopDetail: "Exklusive Drops und Essentials.",
    shopImageURL: "",
    videoHubEyebrow: "Video",
    videoHubTitle: "Video",
    videoHubSubtitle: "Reels, Visuals und starke Kollabos.",
    videoHubDetail: "Clips, Looks und Leute hinter dem Vibe.",
    videoHubImageURL: "",
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T11:30:00.000Z")),
  }));
});

test("screenHeaders lehnen ueberlange Home-Texte ab", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertFails(setDoc(doc(ownerDb, "appConfig", "screenHeaders"), {
    homeImageURL: "https://example.com/home.jpg",
    homeEyebrow: "x".repeat(41),
    homeTitle: "22xSky",
    homeSubtitle: "Alles direkt im Blick.",
    homeDetail: "Musik, Video, Merch, Tools.",
    musicHubEyebrow: "Music",
    musicHubTitle: "Music",
    musicHubSubtitle: "Releases, Artists und Studio an einem Ort.",
    musicHubDetail: "Direkt zu Songs, Beats und Studio.",
    musicHubImageURL: "",
    shopEyebrow: "Store",
    shopTitle: "Shop",
    shopSubtitle: "Produkte direkt in der App.",
    shopDetail: "Offen fuer Bestellungen.",
    shopImageURL: "",
    videoHubEyebrow: "Video",
    videoHubTitle: "Video",
    videoHubSubtitle: "Reels, Visuals und starke Kollaborationen.",
    videoHubDetail: "Clips, Looks und Leute hinter dem Vibe.",
    videoHubImageURL: "",
    updatedAt: Timestamp.fromDate(new Date("2026-04-03T11:00:00.000Z")),
  }));
});

test("legalContent ist oeffentlich lesbar, aber nur Owner darf valide Inhalte speichern", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await setDoc(doc(context.firestore(), "appConfig", "legalContent"), {
      brandName: "22xSky",
      operatorName: "Yang D. Nash - Skydown",
      rightsHolderName: "Yang D. Nash - Skydown",
      supportEmail: "skydownent@gmail.com",
      lastUpdatedLabel: "12. April 2026",
      imprintReference: "Impressum im Store-Eintrag.",
      masterNumberMeaning: "Die Meisterzahl 22 steht fuer Vision plus Umsetzung.",
      brandManifesto: "Dort, wo der Himmel faellt, beginnt unser Denken.",
      symbolicNumericCode: "1337-514-731",
      symbolicLeetCode: "7H3_F4LL_0F_H34/3N",
      symbolicCodeExplanation: "7H3=THE, F4LL=FALL, 0F=OF, H34/3N=HEAVEN.",
      updatedAt: Timestamp.fromDate(new Date("2026-04-12T15:00:00.000Z")),
    });
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();
  const userDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();

  await assertSucceeds(getDoc(doc(guestDb, "appConfig", "legalContent")));
  await assertSucceeds(setDoc(doc(ownerDb, "appConfig", "legalContent"), {
    brandName: "Sky22 Platform",
    operatorName: "Yang D. Nash - Skydown",
    rightsHolderName: "Yang D. Nash - Skydown",
    supportEmail: "legal@skydown.com",
    lastUpdatedLabel: "12. April 2026",
    imprintReference: "Impressum im App-Profil abrufbar.",
    masterNumberMeaning: "Die 22 baut Vision in die Realitaet.",
    brandManifesto: "Der Himmel faellt nicht auf uns - er oeffnet sich in uns.",
    symbolicNumericCode: "1337-514-731",
    symbolicLeetCode: "7H3_F4LL_0F_H34/3N",
    symbolicCodeExplanation: "Symbolischer Zugangscode fuer die Markenidee.",
    updatedAt: Timestamp.fromDate(new Date("2026-04-12T16:00:00.000Z")),
  }));
  await assertFails(updateDoc(doc(userDb, "appConfig", "legalContent"), {
    supportEmail: "hijack@example.com",
    updatedAt: Timestamp.fromDate(new Date("2026-04-12T17:00:00.000Z")),
  }));
});

test("legalContent lehnt ungueltige Felder oder ueberlange Werte ab", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertFails(setDoc(doc(ownerDb, "appConfig", "legalContent"), {
    brandName: "22xSky",
    operatorName: "Yang D. Nash - Skydown",
    rightsHolderName: "x".repeat(161),
    supportEmail: "support@example.com",
    lastUpdatedLabel: "12. April 2026",
    imprintReference: "Impressum im App-Profil abrufbar.",
    masterNumberMeaning: "ok",
    brandManifesto: "ok",
    symbolicNumericCode: "1337-514-731",
    symbolicLeetCode: "7H3_F4LL_0F_H34/3N",
    symbolicCodeExplanation: "ok",
    updatedAt: Timestamp.fromDate(new Date("2026-04-12T18:00:00.000Z")),
  }));

  await assertFails(setDoc(doc(ownerDb, "appConfig", "legalContent"), {
    brandName: "22xSky",
    operatorName: "Yang D. Nash - Skydown",
    rightsHolderName: "Yang D. Nash - Skydown",
    supportEmail: "support@example.com",
    lastUpdatedLabel: "12. April 2026",
    imprintReference: "Impressum im App-Profil abrufbar.",
    masterNumberMeaning: "ok",
    brandManifesto: "ok",
    symbolicNumericCode: "1337-514-731",
    symbolicLeetCode: "7H3_F4LL_0F_H34/3N",
    symbolicCodeExplanation: "ok",
    updatedAt: Timestamp.fromDate(new Date("2026-04-12T18:30:00.000Z")),
    extraField: "not-allowed",
  }));

  await assertFails(setDoc(doc(ownerDb, "appConfig", "legalContent"), {
    brandName: "22xSky",
    operatorName: "Yang D. Nash - Skydown",
    rightsHolderName: "Yang D. Nash - Skydown",
    supportEmail: "support@example.com",
    lastUpdatedLabel: "12. April 2026",
    imprintReference: "Impressum im App-Profil abrufbar.",
    masterNumberMeaning: "ok",
    brandManifesto: "x".repeat(6001),
    symbolicNumericCode: "1337-514-731",
    symbolicLeetCode: "7H3_F4LL_0F_H34/3N",
    symbolicCodeExplanation: "ok",
    updatedAt: Timestamp.fromDate(new Date("2026-04-12T18:45:00.000Z")),
  }));
});

test("galleryMeta darf nur vom Eigentuemer angelegt werden", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();
  const createdAt = Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z"));

  await assertSucceeds(setDoc(
    doc(aliceDb, "galleryMeta", "alice", "items", "img_1"),
    {
      ownerUid: "alice",
      type: "image",
      title: "Bild 1",
      caption: null,
      mediaURL: "https://example.com/image.jpg",
      thumbnailURL: "https://example.com/image.jpg",
      storagePath: "users/alice/gallery/img_1.jpg",
      contentType: "image/jpeg",
      createdAt,
      updatedAt: createdAt,
    },
  ));

  const bobDb = testEnv.authenticatedContext("bob", {role: "user"}).firestore();
  await assertFails(getDoc(doc(bobDb, "galleryMeta", "alice", "items", "img_1")));
});

test("user darf eigenes galleryMeta Bild wieder loeschen", async () => {
  await seedUser("alice");
  const aliceDb = testEnv.authenticatedContext("alice", {role: "user"}).firestore();
  const createdAt = Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z"));

  await assertSucceeds(setDoc(
    doc(aliceDb, "galleryMeta", "alice", "items", "img_delete_1"),
    {
      ownerUid: "alice",
      type: "image",
      title: "Bild 1",
      caption: null,
      mediaURL: "https://example.com/image.jpg",
      thumbnailURL: "https://example.com/image.jpg",
      storagePath: "users/alice/gallery/img_delete_1.jpg",
      contentType: "image/jpeg",
      createdAt,
      updatedAt: createdAt,
    },
  ));

  await assertSucceeds(deleteDoc(doc(aliceDb, "galleryMeta", "alice", "items", "img_delete_1")));
});

test("artistPages sind oeffentlich lesbar, aber nur Owner oder Editoren duerfen Inhalte aendern", async () => {
  await seedArtistPage("zweizwei-janno", {
    editorUids: ["editor1"],
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();
  const editorDb = testEnv.authenticatedContext("editor1", {role: "user"}).firestore();
  const strangerDb = testEnv.authenticatedContext("stranger", {role: "user"}).firestore();

  await assertSucceeds(getDoc(doc(guestDb, "artistPages", "zweizwei-janno")));
  await assertSucceeds(updateDoc(doc(editorDb, "artistPages", "zweizwei-janno"), {
    bio: "Neue Bio",
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
  await assertFails(updateDoc(doc(strangerDb, "artistPages", "zweizwei-janno"), {
    bio: "Nope",
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
});

test("Editoren duerfen artistPages nicht selbst umhaengen", async () => {
  await seedArtistPage("zweizwei-janno", {
    editorUids: ["editor1"],
  });

  const editorDb = testEnv.authenticatedContext("editor1", {role: "user"}).firestore();
  await assertFails(updateDoc(doc(editorDb, "artistPages", "zweizwei-janno"), {
    editorUids: ["editor1", "editor2"],
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
});

test("Owner darf artistPages anlegen und Editoren setzen", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "artistPages", "zweizwei-yang-d-nash"), {
    slug: "yang-d-nash",
    brand: "zweizwei",
    artistName: "Yang D. Nash",
    tagline: "Artist",
    bio: "Owner erstellt die Seite",
    profileImageURL: "https://example.com/avatar.jpg",
    heroImageURL: "https://example.com/hero.jpg",
    instagramURL: "https://instagram.com/ydnash",
    spotifyURL: "https://open.spotify.com/artist/example",
    youtubeURL: "https://youtube.com/@ydnash",
    editorUids: ["editor1"],
    createdAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
  }));
});

test("owner email darf artistPages auch ohne owner claim anlegen", async () => {
  const ownerEmailDb = testEnv.authenticatedContext("nash-owner", {
    role: "user",
    email: "nash.lioncorna@gmail.com",
  }).firestore();

  await assertSucceeds(setDoc(doc(ownerEmailDb, "artistPages", "zweizwei-owner-email-artist"), {
    slug: "owner-email-artist",
    brand: "zweizwei",
    artistName: "Owner Mail Artist",
    tagline: "Fallback",
    bio: "Die feste Owner-Mail darf diese Artist-Page anlegen.",
    profileImageURL: "https://example.com/avatar.jpg",
    heroImageURL: "https://example.com/hero.jpg",
    instagramURL: "https://instagram.com/owner",
    spotifyURL: "https://open.spotify.com/artist/example",
    youtubeURL: "https://youtube.com/@owner",
    editorUids: [],
    createdAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
  }));
});

test("Owner darf auch NICMA als editierbare Seite anlegen", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "artistPages", "nicma-nicma-music"), {
    slug: "nicma-music",
    brand: "nicma",
    artistName: "NICMA MUSIC",
    tagline: "Producer & Studio",
    bio: "NICMA ist jetzt ebenfalls als verwaltete Seite freigeschaltet.",
    profileImageURL: "https://example.com/nicma-avatar.jpg",
    heroImageURL: "https://example.com/nicma-hero.jpg",
    instagramURL: "https://instagram.com/nicma.music",
    spotifyURL: "https://open.spotify.com/artist/example",
    youtubeURL: "https://youtube.com/@nicmamusic",
    editorUids: ["editor1"],
    createdAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T10:00:00.000Z")),
  }));
});

test("Owner darf ein externes Drive-Video fuer alle User freigeben", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "videographyHub", "drive_reel"), {
    title: "Launch Reel",
    projectName: "22xSky",
    email: "owner@example.com",
    notes: "Extern ueber Drive",
    fileName: "launch-reel",
    mimeType: "video/external",
    downloadURL: "",
    externalURL: "https://drive.google.com/file/d/demo123/view",
    embedURL: "https://drive.google.com/file/d/demo123/preview",
    storagePath: "",
    uploaderName: "Owner",
    uploaderEmail: "owner@example.com",
    uploaderID: "owner",
    isPublic: true,
    isHomeFeatured: false,
    sourceProvider: "google_drive",
    sourceFileID: "demo123",
    createdAt: Timestamp.fromDate(new Date("2026-04-03T18:00:00.000Z")),
  }));
});

test("Owner darf keine ungueltigen Video-Provider speichern", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertFails(setDoc(doc(ownerDb, "videographyHub", "broken_provider"), {
    title: "Broken Reel",
    projectName: "22xSky",
    email: "owner@example.com",
    notes: "",
    fileName: "broken",
    mimeType: "video/external",
    downloadURL: "",
    externalURL: "https://example.com/reel",
    embedURL: "",
    storagePath: "",
    uploaderName: "Owner",
    uploaderEmail: "owner@example.com",
    uploaderID: "owner",
    isPublic: true,
    isHomeFeatured: false,
    sourceProvider: "dropbox",
    sourceFileID: "",
    createdAt: Timestamp.fromDate(new Date("2026-04-03T18:00:00.000Z")),
  }));
});

test("Owner darf einen externen Beat-Link fuer alle User freigeben", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "nicmaBeatHub", "external_beat"), {
    title: "Atmosphere",
    artistName: "NICMA MUSIC",
    email: "owner@example.com",
    notes: "Direkter Beat-Link",
    fileName: "atmosphere.mp3",
    mimeType: "audio/mpeg",
    downloadURL: "https://cdn.example.com/beats/atmosphere.mp3",
    externalURL: "https://cdn.example.com/beats/atmosphere.mp3",
    storagePath: "",
    uploaderName: "Owner",
    uploaderEmail: "owner@example.com",
    uploaderID: "owner",
    isPublic: true,
    sourceProvider: "external_link",
    sourceFileID: "",
    createdAt: Timestamp.fromDate(new Date("2026-04-03T18:00:00.000Z")),
  }));
});
