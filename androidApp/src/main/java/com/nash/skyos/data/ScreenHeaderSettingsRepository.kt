package com.nash.skyos.data

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
    val homeEyebrow: String = "",
    val homeTitle: String = "",
    val homeSubtitle: String = "",
    val homeDetail: String = "",
    val musicHubImageUrl: String = "",
    val musicHubEyebrow: String = "",
    val musicHubTitle: String = "",
    val musicHubSubtitle: String = "",
    val musicHubDetail: String = "",
    val shopImageUrl: String = "",
    val shopEyebrow: String = "",
    val shopTitle: String = "",
    val shopSubtitle: String = "",
    val shopDetail: String = "",
    val videoHubImageUrl: String = "",
    val videoHubEyebrow: String = "",
    val videoHubTitle: String = "",
    val videoHubSubtitle: String = "",
    val videoHubDetail: String = "",
) {
    val configuredCount: Int
        get() = listOf(
            listOf(homeImageUrl, homeEyebrow, homeTitle, homeSubtitle, homeDetail),
            listOf(musicHubImageUrl, musicHubEyebrow, musicHubTitle, musicHubSubtitle, musicHubDetail),
            listOf(shopImageUrl, shopEyebrow, shopTitle, shopSubtitle, shopDetail),
            listOf(videoHubImageUrl, videoHubEyebrow, videoHubTitle, videoHubSubtitle, videoHubDetail),
        ).count { fields ->
            fields.any { it.isNotBlank() }
        }
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
        homeEyebrow = this["homeEyebrow"] as? String ?: "",
        homeTitle = this["homeTitle"] as? String ?: "",
        homeSubtitle = this["homeSubtitle"] as? String ?: "",
        homeDetail = this["homeDetail"] as? String ?: "",
        musicHubImageUrl = this["musicHubImageURL"] as? String ?: "",
        musicHubEyebrow = this["musicHubEyebrow"] as? String ?: "",
        musicHubTitle = this["musicHubTitle"] as? String ?: "",
        musicHubSubtitle = this["musicHubSubtitle"] as? String ?: "",
        musicHubDetail = this["musicHubDetail"] as? String ?: "",
        shopImageUrl = this["shopImageURL"] as? String ?: "",
        shopEyebrow = this["shopEyebrow"] as? String ?: "",
        shopTitle = this["shopTitle"] as? String ?: "",
        shopSubtitle = this["shopSubtitle"] as? String ?: "",
        shopDetail = this["shopDetail"] as? String ?: "",
        videoHubImageUrl = this["videoHubImageURL"] as? String ?: "",
        videoHubEyebrow = this["videoHubEyebrow"] as? String ?: "",
        videoHubTitle = this["videoHubTitle"] as? String ?: "",
        videoHubSubtitle = this["videoHubSubtitle"] as? String ?: "",
        videoHubDetail = this["videoHubDetail"] as? String ?: "",
    )
}

private fun ScreenHeaderSettings.toMap(): Map<String, Any> {
    return mapOf(
        "homeImageURL" to homeImageUrl.trim(),
        "homeEyebrow" to homeEyebrow.trim(),
        "homeTitle" to homeTitle.trim(),
        "homeSubtitle" to homeSubtitle.trim(),
        "homeDetail" to homeDetail.trim(),
        "musicHubImageURL" to musicHubImageUrl.trim(),
        "musicHubEyebrow" to musicHubEyebrow.trim(),
        "musicHubTitle" to musicHubTitle.trim(),
        "musicHubSubtitle" to musicHubSubtitle.trim(),
        "musicHubDetail" to musicHubDetail.trim(),
        "shopImageURL" to shopImageUrl.trim(),
        "shopEyebrow" to shopEyebrow.trim(),
        "shopTitle" to shopTitle.trim(),
        "shopSubtitle" to shopSubtitle.trim(),
        "shopDetail" to shopDetail.trim(),
        "videoHubImageURL" to videoHubImageUrl.trim(),
        "videoHubEyebrow" to videoHubEyebrow.trim(),
        "videoHubTitle" to videoHubTitle.trim(),
        "videoHubSubtitle" to videoHubSubtitle.trim(),
        "videoHubDetail" to videoHubDetail.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}
