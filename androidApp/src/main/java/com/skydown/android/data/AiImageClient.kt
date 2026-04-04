package com.skydown.android.data

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class AiGeneratedVisualResult(
    val text: String,
    val imageBytes: ByteArray,
    val mimeType: String,
    val historyRetentionDays: Int,
)

class AiImageClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    suspend fun generateVisual(prompt: String): AiGeneratedVisualResult {
        val result = functions
            .getHttpsCallable("generateAiVisual")
            .call(mapOf("prompt" to prompt))
            .await()

        val data = result.data as? Map<*, *> ?: error("Die Visual-Antwort konnte nicht gelesen werden.")
        val imageBase64 = data["imageBase64"] as? String
        val imageBytes = imageBase64
            ?.takeIf { it.isNotBlank() }
            ?.let { Base64.decode(it, Base64.DEFAULT) }
            ?: error("Keine Bilddaten in der Antwort.")

        return AiGeneratedVisualResult(
            text = (data["text"] as? String)?.takeIf { it.isNotBlank() } ?: "Visual generiert.",
            imageBytes = imageBytes,
            mimeType = (data["mimeType"] as? String)?.takeIf { it.isNotBlank() } ?: "image/png",
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
        )
    }
}
