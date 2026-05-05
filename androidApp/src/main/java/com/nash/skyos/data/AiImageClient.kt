package com.nash.skyos.data

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.nash.skyos.R
import kotlinx.coroutines.delay

data class AiGeneratedVisualResult(
    val text: String,
    val imageBytes: ByteArray,
    val mimeType: String,
    val historyRetentionDays: Int,
    val usage: AiUsageSnapshot? = null,
    val decision: AiBotDecision? = null,
)

open class AiImageClient(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance("us-central1"),
) {
    open suspend fun generateVisual(prompt: String, aiLevel: String): AiGeneratedVisualResult {
        var lastError: Throwable? = null
        repeat(2) { attempt ->
            try {
                return generateVisualOnce(prompt, aiLevel)
            } catch (error: Throwable) {
                lastError = error
                if (attempt == 0 && error.shouldRetryVisualGeneration()) {
                    delay(800)
                } else {
                    throw error
                }
            }
        }

        throw lastError ?: error(AppTextResolver.string(R.string.ai_visual_error_generate_failed))
    }

    private suspend fun generateVisualOnce(prompt: String, aiLevel: String): AiGeneratedVisualResult {
        if (!AppNetworkMonitor.isOnline.value) {
            error(AppTextResolver.string(R.string.ai_visual_error_offline))
        }

        val result = functions
            .callWithAppCheckRetry(
                functionName = "generateAiVisual",
                payload = mapOf(
                    "prompt" to prompt,
                    "aiLevel" to aiLevel,
                ),
            )

        val data = result.data as? Map<*, *> ?: error(AppTextResolver.string(R.string.ai_visual_error_response_unreadable))
        val imagePayload = data["imageBase64"] as? String
        val imageBytes = imagePayload
            ?.takeIf { it.isNotBlank() }
            ?.let(::decodeImagePayload)
            ?: error(AppTextResolver.string(R.string.ai_visual_error_no_image_data))

        return AiGeneratedVisualResult(
            text = (data["text"] as? String)?.takeIf { it.isNotBlank() }
                ?: AppTextResolver.string(R.string.ai_visual_generated_fallback_text),
            imageBytes = imageBytes,
            mimeType = (data["mimeType"] as? String)?.takeIf { it.isNotBlank() } ?: "image/png",
            historyRetentionDays = (data["historyRetentionDays"] as? Number)?.toInt() ?: 3,
            usage = parseAiUsageSnapshot(data["usage"]),
            decision = parseAiBotDecision(data["botDecision"]),
        )
    }
}

private fun decodeImagePayload(payload: String): ByteArray {
    val normalized = payload
        .trim()
        .substringAfter(",", payload.trim())
        .filterNot(Char::isWhitespace)

    val decodeFlags = listOf(
        Base64.DEFAULT,
        Base64.NO_WRAP,
        Base64.URL_SAFE or Base64.NO_WRAP,
        Base64.URL_SAFE,
    )

    decodeFlags.forEach { flags ->
        runCatching {
            Base64.decode(normalized, flags)
        }.getOrNull()?.takeIf { it.isNotEmpty() }?.let { return it }
    }

    error(AppTextResolver.string(R.string.ai_visual_error_no_image_data))
}

private fun Throwable.shouldRetryVisualGeneration(): Boolean {
    if (this is FirebaseFunctionsException) {
        when (code) {
            FirebaseFunctionsException.Code.INTERNAL,
            FirebaseFunctionsException.Code.UNAVAILABLE,
            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED,
            -> return true

            else -> Unit
        }
    }

    val message = localizedMessage.orEmpty().lowercase()
    return message.contains("server responded with an error") ||
        message.contains("nicht sauber geantwortet") ||
        message.contains("keine bilddaten") ||
        message.contains("visual-antwort")
}
