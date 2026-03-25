package com.skydown.android.data

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

data class AgentHistoryTurn(
    val role: String,
    val text: String,
)

class AgentClient {
    private val functions = FirebaseFunctions.getInstance("us-central1")

    suspend fun sendMessage(
        prompt: String,
        history: List<AgentHistoryTurn>,
    ): String {
        val payload = mapOf(
            "prompt" to prompt,
            "history" to history.map { turn ->
                mapOf(
                    "role" to turn.role,
                    "text" to turn.text,
                )
            },
        )

        val result = functions
            .getHttpsCallable("skydownAgent")
            .call(payload)
            .await()

        return when (val data = result.data) {
            is String -> data
            is Map<*, *> -> data["reply"] as? String
            else -> null
        }?.takeIf { it.isNotBlank() }
            ?: error("Skydown Agent hat keine Antwort geliefert.")
    }
}
