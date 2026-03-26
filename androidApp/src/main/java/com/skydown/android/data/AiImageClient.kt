package com.skydown.android.data

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.ResponseModality
import com.google.firebase.ai.type.generationConfig

data class AiGeneratedVisualResult(
    val text: String,
    val imageBytes: ByteArray,
    val mimeType: String,
)

class AiImageClient {
    private val model by lazy {
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash-image",
                generationConfig = generationConfig {
                    responseModalities = listOf(
                        ResponseModality.TEXT,
                        ResponseModality.IMAGE,
                    )
                },
            )
    }

    suspend fun generateVisual(prompt: String): AiGeneratedVisualResult {
        val response = model.generateContent(prompt)
        val imagePart = response.inlineDataParts.firstOrNull()
            ?: throw IllegalStateException("Keine Bilddaten in der Antwort.")

        return AiGeneratedVisualResult(
            text = response.text?.takeIf { it.isNotBlank() } ?: "Visual generiert.",
            imageBytes = imagePart.inlineData,
            mimeType = imagePart.mimeType,
        )
    }
}
