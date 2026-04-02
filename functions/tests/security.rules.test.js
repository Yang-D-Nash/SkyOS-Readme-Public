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
  getDoc,
  setDoc,
  updateDoc,
  Timestamp,
} = require("firebase/firestore");
const {
  ref,
  uploadBytes,
} = require("firebase/storage");

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
      slug: artistId,
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
      storagePath: `users/${uid}/${kind}/${fileName}`,
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

test("artistPages sind oeffentlich lesbar, aber nur Owner oder Editoren duerfen Inhalte aendern", async () => {
  await seedArtistPage("janno", {
    editorUids: ["editor1"],
  });

  const guestDb = testEnv.unauthenticatedContext().firestore();
  const editorDb = testEnv.authenticatedContext("editor1", {role: "user"}).firestore();
  const strangerDb = testEnv.authenticatedContext("stranger", {role: "user"}).firestore();

  await assertSucceeds(getDoc(doc(guestDb, "artistPages", "janno")));
  await assertSucceeds(updateDoc(doc(editorDb, "artistPages", "janno"), {
    bio: "Neue Bio",
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
  await assertFails(updateDoc(doc(strangerDb, "artistPages", "janno"), {
    bio: "Nope",
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
});

test("Editoren duerfen artistPages nicht selbst umhaengen", async () => {
  await seedArtistPage("janno", {
    editorUids: ["editor1"],
  });

  const editorDb = testEnv.authenticatedContext("editor1", {role: "user"}).firestore();
  await assertFails(updateDoc(doc(editorDb, "artistPages", "janno"), {
    editorUids: ["editor1", "editor2"],
    updatedAt: Timestamp.fromDate(new Date("2026-04-02T11:00:00.000Z")),
  }));
});

test("Owner darf artistPages anlegen und Editoren setzen", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "artistPages", "yang-d-nash"), {
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

test("Owner darf auch NICMA als editierbare Seite anlegen", async () => {
  const ownerDb = testEnv.authenticatedContext("owner", {role: "owner"}).firestore();

  await assertSucceeds(setDoc(doc(ownerDb, "artistPages", "nicma-music"), {
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
