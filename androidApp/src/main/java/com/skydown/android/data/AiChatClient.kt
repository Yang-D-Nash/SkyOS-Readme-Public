package com.skydown.android.data

import com.google.firebase.ai.FirebaseAI
import com.google.firebase.ai.type.GenerativeBackend

class AiChatClient {
    private val model by lazy {
        FirebaseAI.getInstance(backend = GenerativeBackend.googleAI())
            .generativeModel(modelName = "gemini-2.5-flash")
    }

    fun createChat() = model.startChat()
}
