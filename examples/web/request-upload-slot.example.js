import {getAuth} from "firebase/auth";
import {getFirestore, doc, setDoc, Timestamp} from "firebase/firestore";
import {getFunctions, httpsCallable} from "firebase/functions";
import {getStorage, ref, uploadBytes, getDownloadURL} from "firebase/storage";

export async function uploadGalleryImage(file) {
  const auth = getAuth();
  const currentUser = auth.currentUser;
  if (!currentUser) {
    throw new Error("Bitte zuerst anmelden.");
  }

  const functions = getFunctions(undefined, "us-central1");
  const requestUploadSlot = httpsCallable(functions, "requestUploadSlot");
  const slotResult = await requestUploadSlot({
    kind: "gallery",
    mimeType: file.type,
    fileExtension: file.name.split(".").pop() || "jpg",
    byteSize: file.size,
  });

  const slot = slotResult.data;
  if (!slot.allowed) {
    throw new Error(slot.message || "Upload wurde abgelehnt.");
  }

  const storage = getStorage();
  const fileRef = ref(storage, slot.storagePath);
  await uploadBytes(fileRef, file, {
    contentType: file.type,
    customMetadata: slot.metadata,
  });

  const downloadURL = await getDownloadURL(fileRef);
  const firestore = getFirestore();
  await setDoc(
    doc(firestore, "galleryMeta", currentUser.uid, "items", slot.slotId),
    {
      ownerUid: currentUser.uid,
      type: "image",
      title: file.name.slice(0, 60),
      caption: null,
      mediaURL: downloadURL,
      thumbnailURL: downloadURL,
      storagePath: slot.storagePath,
      contentType: file.type,
      createdAt: Timestamp.now(),
      updatedAt: Timestamp.now(),
    },
  );
}
