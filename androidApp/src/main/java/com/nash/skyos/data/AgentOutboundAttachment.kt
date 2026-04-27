package com.nash.skyos.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Inline file payload for [AgentClient.sendMessage] (base64).
 * Max 5 items × 256 KiB each (enforced when encoding).
 */
data class AgentOutboundAttachment(
    val id: String,
    val name: String,
    val kind: String,
    val mimeType: String,
    val source: String = "inline",
    val inlineBase64: String,
) {
    fun toPayloadMap(): Map<String, Any> =
        mapOf(
            "id" to id,
            "name" to name,
            "kind" to kind,
            "mimeType" to mimeType,
            "source" to source,
            "inlineBase64" to inlineBase64,
        )

    companion object {
        private const val MAX_BYTES = 256 * 1024

        fun load(context: Context, uri: Uri, displayName: String, kindWire: String): AgentOutboundAttachment? {
            val resolver = context.contentResolver
            val mime = resolver.getType(uri)?.trim()?.take(120).orEmpty().ifBlank { guessMimeFromName(displayName) }
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            if (bytes.isEmpty() || bytes.size > MAX_BYTES) return null
            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val name = displayName.trim().ifBlank { "attachment" }.take(255)
            return AgentOutboundAttachment(
                id = uri.toString().take(200),
                name = name,
                kind = kindWire,
                mimeType = mime.ifBlank { "application/octet-stream" },
                inlineBase64 = b64,
            )
        }

        suspend fun batchFromUris(
            context: Context,
            pairs: List<Pair<Uri, Pair<String, String>>>,
            limit: Int = 5,
        ): List<AgentOutboundAttachment> =
            withContext(Dispatchers.IO) {
                pairs
                    .take(limit)
                    .mapNotNull { (uri, meta) ->
                        val (displayName, kindWire) = meta
                        load(context, uri, displayName, kindWire)
                    }
            }

        private fun guessMimeFromName(fileName: String): String {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return when (ext) {
                "png" -> "image/png"
                "jpg", "jpeg" -> "image/jpeg"
                "webp" -> "image/webp"
                "gif" -> "image/gif"
                "pdf" -> "application/pdf"
                "txt" -> "text/plain"
                "json" -> "application/json"
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "mp4" -> "video/mp4"
                else -> "application/octet-stream"
            }
        }
    }
}
