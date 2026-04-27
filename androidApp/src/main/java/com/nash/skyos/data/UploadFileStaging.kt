package com.nash.skyos.data

import android.content.Context
import android.net.Uri
import com.nash.skyos.R
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

internal suspend fun Context.stagePickerFileForUpload(
    sourceUri: Uri,
    fileName: String,
): File = withContext(Dispatchers.IO) {
    val uploadsDirectory = File(cacheDir, "upload-staging").apply {
        mkdirs()
    }
    val tempFile = File.createTempFile(
        "skydown-upload-",
        uploadTempFileSuffix(fileName),
        uploadsDirectory,
    )

    try {
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        } ?: error(AppTextResolver.string(R.string.upload_error_file_read_failed, fileName))
    } catch (error: Exception) {
        tempFile.delete()
        throw error
    }

    tempFile
}

internal fun Throwable.toReadableStorageUploadError(fileName: String): Throwable {
    val storageException = this as? StorageException
    val readableMessage = when {
        storageException?.errorCode == StorageException.ERROR_NOT_AUTHENTICATED ->
            "Die Admin-Anmeldung ist abgelaufen. Bitte melde dich neu an und versuch den Upload fuer $fileName erneut."

        storageException?.errorCode == StorageException.ERROR_NOT_AUTHORIZED ->
            "Dein Konto darf $fileName gerade nicht in Firebase Storage hochladen."

        storageException?.errorCode == StorageException.ERROR_RETRY_LIMIT_EXCEEDED ->
            "Der Upload von $fileName wurde wegen einer instabilen Verbindung abgebrochen. Bitte versuch es noch einmal."

        message?.contains("Unexpected -1 Code from backend", ignoreCase = true) == true ->
            "Die Datei $fileName konnte vom Geraet nicht stabil an Firebase uebergeben werden. Bitte waehle sie erneut aus und versuch es noch einmal."

        else -> message?.takeIf { it.isNotBlank() }
    }

    return IllegalStateException(
        readableMessage ?: "Die Datei $fileName konnte nicht hochgeladen werden.",
        this,
    )
}

internal suspend fun StorageReference.putStagedFile(
    stagedFile: File,
    metadata: StorageMetadata,
): UploadTask.TaskSnapshot = withContext(Dispatchers.IO) {
    stagedFile.inputStream().buffered().use { inputStream ->
        putStream(inputStream, metadata).await()
    }
}

private fun uploadTempFileSuffix(fileName: String): String {
    val extension = fileName.substringAfterLast('.', "")
        .lowercase()
        .takeIf { it.matches(Regex("[a-z0-9]{1,10}")) }

    return extension?.let { ".$it" } ?: ".bin"
}
