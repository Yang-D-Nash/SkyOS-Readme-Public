package com.skydown.android.data

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class ScreenHeaderSettings(
    val homeImageUrl: String = "",
    val musicHubImageUrl: String = "",
    val shopImageUrl: String = "",
    val videoHubImageUrl: String = "",
) {
    val configuredCount: Int
        get() = listOf(homeImageUrl, musicHubImageUrl, shopImageUrl, videoHubImageUrl)
            .count { it.isNotBlank() }
}

class ScreenHeaderSettingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    private val collectionName = "appConfig"
    private val documentName = "screenHeaders"
    private val _settings = MutableStateFlow(ScreenHeaderSettings())
    val settings: StateFlow<ScreenHeaderSettings> = _settings.asStateFlow()
    private var listener: ListenerRegistration? = null

    init {
        startObserving()
    }

    suspend fun updateSettings(settings: ScreenHeaderSettings): Result<Unit> {
        return runCatching {
            firestore.collection(collectionName).document(documentName).set(
                settings.toMap(),
                SetOptions.merge(),
            ).await()
        }
    }

    private fun startObserving() {
        listener?.remove()
        listener = firestore.collection(collectionName).document(documentName).addSnapshotListener { snapshot, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            _settings.value = snapshot?.data.orEmpty().toScreenHeaderSettings()
        }
    }
}

private fun Map<String, Any>.toScreenHeaderSettings(): ScreenHeaderSettings {
    return ScreenHeaderSettings(
        homeImageUrl = this["homeImageURL"] as? String ?: "",
        musicHubImageUrl = this["musicHubImageURL"] as? String ?: "",
        shopImageUrl = this["shopImageURL"] as? String ?: "",
        videoHubImageUrl = this["videoHubImageURL"] as? String ?: "",
    )
}

private fun ScreenHeaderSettings.toMap(): Map<String, Any> {
    return mapOf(
        "homeImageURL" to homeImageUrl.trim(),
        "musicHubImageURL" to musicHubImageUrl.trim(),
        "shopImageURL" to shopImageUrl.trim(),
        "videoHubImageURL" to videoHubImageUrl.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
