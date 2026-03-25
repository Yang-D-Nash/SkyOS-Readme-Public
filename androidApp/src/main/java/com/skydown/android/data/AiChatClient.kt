package com.skydown.android.data

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig

class AiChatClient {
    private val model by lazy {
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(
                modelName = "gemini-2.5-flash-lite",
                generationConfig = generationConfig {
                    candidateCount = 1
                    maxOutputTokens = 220
                    thinkingConfig = thinkingConfig {
                        thinkingBudget = 256
                    }
                },
            )
    }

    fun createChat() = model.startChat()
}
