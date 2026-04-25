package com.nash.skyos.data

import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await

internal suspend fun StorageReference.awaitStableDownloadUrl(
    attempts: Int = 4,
    initialDelayMillis: Long = 250,
): String {
    var nextDelayMillis = initialDelayMillis
    repeat(attempts) { index ->
        try {
            return downloadUrl.await().toString()
        } catch (error: Exception) {
            val storageError = error as? StorageException
            val isTransientNotFound = storageError?.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND
            if (!isTransientNotFound || index == attempts - 1) {
                throw mapDownloadUrlError(error)
            }
            delay(nextDelayMillis)
            nextDelayMillis *= 2
        }
    }

    error("Download-URL konnte nicht geladen werden.")
}

private fun mapDownloadUrlError(error: Exception): Exception {
    val storageError = error as? StorageException ?: return error
    return when (storageError.errorCode) {
        StorageException.ERROR_OBJECT_NOT_FOUND -> IllegalStateException(
            "Bild wurde hochgeladen, aber die Download-URL ist noch nicht verfuegbar. Bitte gleich nochmal versuchen.",
            error,
        )

        StorageException.ERROR_NOT_AUTHENTICATED,
        StorageException.ERROR_NOT_AUTHORIZED,
        -> IllegalStateException(
            "Upload wurde gespeichert, aber Firebase blockiert den Zugriff auf die Bild-URL.",
            error,
        )

        else -> error
    }
}
