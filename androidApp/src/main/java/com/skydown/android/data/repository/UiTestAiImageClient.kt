package com.skydown.android.data.repository

import android.graphics.Bitmap
import android.graphics.Color
import com.skydown.android.data.AiGeneratedVisualResult
import com.skydown.android.data.AiImageClient
import java.io.ByteArrayOutputStream

class UiTestAiImageClient : AiImageClient() {
    override suspend fun generateVisual(prompt: String): AiGeneratedVisualResult {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.parseColor("#1F6FEB"))
        }
        val imageBytes = ByteArrayOutputStream().use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.toByteArray()
        }

        return AiGeneratedVisualResult(
            text = "UI-Test-Visual fuer: $prompt",
            imageBytes = imageBytes,
            mimeType = "image/png",
            historyRetentionDays = 3,
        )
    }
}
