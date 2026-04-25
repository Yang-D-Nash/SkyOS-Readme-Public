package com.nash.skyos.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore

fun copyAiText(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    clipboard.setPrimaryClip(android.content.ClipData.newPlainText(label, text))
}

fun shareAiText(context: Context, title: String, text: String) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, text)
    }

    openExternalIntent(
        context = context,
        intent = Intent.createChooser(shareIntent, title),
        missingMessage = "Teilen konnte nicht gestartet werden.",
    )
}

fun saveAiImage(
    context: Context,
    imageBytes: ByteArray,
    mimeType: String?,
): Result<Unit> = runCatching {
    val resolvedMimeType = mimeType?.takeIf { it.startsWith("image/") } ?: "image/png"
    val extension = when (resolvedMimeType.substringAfter("/", "png").lowercase()) {
        "jpeg", "jpg" -> "jpg"
        "webp" -> "webp"
        else -> "png"
    }
    val displayName = "skydown_ai_${System.currentTimeMillis()}.$extension"
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, resolvedMimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Skydown")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        ?: error("Kein Speicherziel fuer das Bild verfuegbar.")

    resolver.openOutputStream(uri)?.use { stream ->
        stream.write(imageBytes)
        stream.flush()
    } ?: error("Das Bild konnte nicht geschrieben werden.")

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentValues.clear()
        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, contentValues, null, null)
    }
}
