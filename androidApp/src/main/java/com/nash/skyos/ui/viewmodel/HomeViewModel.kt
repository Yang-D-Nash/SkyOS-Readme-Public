package com.nash.skyos.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.nash.skyos.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.AppContainer
import com.nash.skyos.ui.model.FeaturedVideoHighlight
import com.nash.skyos.ui.model.HomeUiState
import com.skydown.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.tasks.await

class HomeViewModel(
    application: Application
) : AndroidViewModel(application) {
    private val musicService = AppContainer.musicService
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private var refreshGeneration: Long = 0

    private fun resString(resId: Int) = getApplication<Application>().getString(resId)

    private fun resString(resId: Int, vararg formatArgs: Any) =
        getApplication<Application>().getString(resId, *formatArgs)

    companion object {
        fun provideFactory(app: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                    return HomeViewModel(app) as T
                }
                throw IllegalArgumentException(
                    app.getString(
                        R.string.home_error_unknown_viewmodel_class,
                        modelClass,
                    ),
                )
            }
        }
    }

    private val featuredArtists = listOf(
        "JANNO",
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
        "TANGAJOE007",
    )

    init {
        refresh()
    }

    fun refresh() {
        val generation = ++refreshGeneration
        viewModelScope.launch {
            supervisorScope {
                launch {
                    val latestTrack = loadLatestTrack()
                    if (!isCurrentRefresh(generation)) return@launch
                    applyFeaturedTrackUpdate(generation, latestTrack)
                }

                launch {
                    val latestVideo = loadLatestVideo()
                    if (!isCurrentRefresh(generation)) return@launch
                    applyFeaturedVideoUpdate(generation, latestVideo)
                }

                launch {
                    loadRuntimeSignals(generation)
                }
            }
        }
    }

    private suspend fun loadRuntimeSignals(generation: Long) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            applyRuntimeSignals(
                generation = generation,
                aiUsageWarning = null,
                creatorLimitZone = false,
                agentRunning = false,
                workflowWaiting = false,
                commerceSignal = null,
                recoverableError = null,
                newDataAvailable = false,
            )
            return
        }

        var aiUsageWarning: String? = null
        var creatorLimitZone = false
        var agentRunning = false
        var workflowWaiting = false
        var commerceSignal: String? = null
        var recoverableError: String? = null
        var newDataAvailable = false

        runCatching {
            val usageSnapshot = firestore.collection("users")
                .document(uid)
                .collection("aiUsage")
                .orderBy("createdAt")
                .limit(1)
                .get()
                .await()
            val usageData = usageSnapshot.documents.firstOrNull()?.data.orEmpty()
            val warningLevel = (usageData["warningLevel"] as? String).orEmpty().lowercase()
            if (warningLevel == "critical" || warningLevel == "warning") {
                aiUsageWarning = resString(R.string.home_signal_usage_level, warningLevel.uppercase())
            }
            val remaining = (usageData["remainingForKind"] as? Number)?.toInt() ?: -1
            val limit = (usageData["limitForKind"] as? Number)?.toInt() ?: -1
            creatorLimitZone = limit > 0 && remaining >= 0 && (limit - remaining).toDouble() / limit.toDouble() >= 0.8
        }

        runCatching {
            val runsSnapshot = firestore.collection("users")
                .document(uid)
                .collection("agentRuns")
                .orderBy("createdAt")
                .limit(1)
                .get()
                .await()
            val runData = runsSnapshot.documents.firstOrNull()?.data.orEmpty()
            val runStatus = (runData["status"] as? String).orEmpty().lowercase()
            agentRunning = runStatus == "running" || runStatus == "processing"
            workflowWaiting = runStatus == "queued" || runStatus == "waiting"
        }

        runCatching {
            val userDoc = firestore.collection("users").document(uid).get().await().data.orEmpty()
            val role = (userDoc["role"] as? String).orEmpty().lowercase()
            if (role == "owner") {
                val orderSnapshot = firestore.collection("orders")
                    .orderBy("timestamp")
                    .limit(1)
                    .get()
                    .await()
                val orderData = orderSnapshot.documents.firstOrNull()?.data.orEmpty()
                val paymentStatus = (orderData["paymentStatus"] as? String).orEmpty()
                val fulfillmentStatus = (orderData["fulfillmentStatus"] as? String).orEmpty()
                commerceSignal = when {
                    paymentStatus.equals("pending", ignoreCase = true) -> resString(R.string.home_signal_open_payment_review)
                    fulfillmentStatus.isNotBlank() -> resString(R.string.home_signal_shipping_update, fulfillmentStatus)
                    orderData.isNotEmpty() -> resString(R.string.home_signal_new_order_activity)
                    else -> null
                }
            }
        }

        runCatching {
            val runtimeConfig = firestore.collection("system").document("runtimeConfig").get().await().data.orEmpty()
            val lockdown = runtimeConfig["lockdown"] as? Boolean ?: false
            val uploadsEnabled = runtimeConfig["uploadsEnabled"] as? Boolean ?: true
            val userWritesEnabled = runtimeConfig["userWritesEnabled"] as? Boolean ?: true
            val registrationsEnabled = runtimeConfig["registrationsEnabled"] as? Boolean ?: true
            val featurePausedCount = listOf(uploadsEnabled, userWritesEnabled, registrationsEnabled).count { !it }
            newDataAvailable = !lockdown && featurePausedCount == 0
            if (featurePausedCount > 0) {
                recoverableError = resString(R.string.home_signal_system_reduced_mode)
            }
        }

        applyRuntimeSignals(
            generation = generation,
            aiUsageWarning = aiUsageWarning,
            creatorLimitZone = creatorLimitZone,
            agentRunning = agentRunning,
            workflowWaiting = workflowWaiting,
            commerceSignal = commerceSignal,
            recoverableError = recoverableError,
            newDataAvailable = newDataAvailable,
        )
    }

    private fun isCurrentRefresh(generation: Long): Boolean = generation == refreshGeneration

    private fun applyRuntimeSignals(
        generation: Long,
        aiUsageWarning: String?,
        creatorLimitZone: Boolean,
        agentRunning: Boolean,
        workflowWaiting: Boolean,
        commerceSignal: String?,
        recoverableError: String?,
        newDataAvailable: Boolean,
    ) {
        _uiState.update { state ->
            if (!isCurrentRefresh(generation)) return@update state
            val updatedState = state.copy(
                aiUsageWarning = aiUsageWarning,
                creatorLimitZone = creatorLimitZone,
                agentRunning = agentRunning,
                workflowWaiting = workflowWaiting,
                commerceSignal = commerceSignal,
                syncPaused = !newDataAvailable,
                recoverableError = recoverableError,
                newDataAvailable = newDataAvailable,
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun applyFeaturedTrackUpdate(generation: Long, track: Track?) {
        _uiState.update {
            if (!isCurrentRefresh(generation)) return@update it
            val updatedState = it.copy(
                featuredTrack = track,
                homeTrackMessage = if (track == null) {
                    resString(R.string.home_track_placeholder)
                } else {
                    null
                },
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun applyFeaturedVideoUpdate(generation: Long, video: FeaturedVideoHighlight?) {
        _uiState.update {
            if (!isCurrentRefresh(generation)) return@update it
            val updatedState = it.copy(
                featuredVideo = video,
                homeVideoMessage = if (video == null) {
                    resString(R.string.home_video_placeholder)
                } else {
                    null
                },
            )
            updatedState.copy(contentSignal = buildContentSignal(updatedState))
        }
    }

    private fun buildContentSignal(state: HomeUiState): String? {
        val track = state.featuredTrack
        if (track != null) {
            return resString(R.string.home_content_signal_new_drop, track.trackName)
        }
        val video = state.featuredVideo
        if (video != null) {
            return resString(R.string.home_content_signal_video_activity, video.title)
        }
        return null
    }

    private suspend fun loadLatestTrack(): Track? {
        val tracks = supervisorScope {
            featuredArtists
                .map { artist ->
                    async {
                        musicService.fetchTracks(artist).getOrNull().orEmpty()
                    }
                }
                .awaitAll()
                .flatten()
        }

        if (tracks.isEmpty()) return null

        return tracks.sortedWith(::compareTracksForHomePriority).firstOrNull()
    }

    private fun compareTracksForHomePriority(lhs: Track, rhs: Track): Int {
        val lhsHasPreview = !lhs.previewUrl.isNullOrBlank()
        val rhsHasPreview = !rhs.previewUrl.isNullOrBlank()
        if (lhsHasPreview != rhsHasPreview) {
            return if (lhsHasPreview) -1 else 1
        }

        val lhsHasFallbackTarget = trackHasHomeFallbackTarget(lhs)
        val rhsHasFallbackTarget = trackHasHomeFallbackTarget(rhs)
        if (lhsHasFallbackTarget != rhsHasFallbackTarget) {
            return if (lhsHasFallbackTarget) -1 else 1
        }

        val lhsDate = parsedTrackReleaseDate(lhs.releaseDate)
        val rhsDate = parsedTrackReleaseDate(rhs.releaseDate)

        if (lhsDate != rhsDate) {
            return when {
                lhsDate == null -> 1
                rhsDate == null -> -1
                lhsDate > rhsDate -> -1
                else -> 1
            }
        }

        return lhs.trackName.lowercase().compareTo(rhs.trackName.lowercase())
    }

    private fun trackHasHomeFallbackTarget(track: Track): Boolean {
        return !track.spotifyTrackId.isNullOrBlank() ||
            !track.spotifyArtistId.isNullOrBlank() ||
            !track.externalUrl.isNullOrBlank()
    }

    private fun parsedTrackReleaseDate(value: String?): Long? {
        val rawValue = value?.trim().orEmpty()
        if (rawValue.isBlank()) return null

        return runCatching { java.time.Instant.parse(rawValue).toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(rawValue).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.LocalDate.parse(rawValue).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.YearMonth.parse(rawValue).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { java.time.Year.parse(rawValue).atDay(1).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }

    private suspend fun loadLatestVideo(): FeaturedVideoHighlight? {
        val featuredSnapshot = firestore.collection("videographyHub")
            .whereEqualTo("isPublic", true)
            .whereEqualTo("isHomeFeatured", true)
            .limit(1)
            .get()
            .await()

        featuredSnapshot.documents.firstOrNull()
            ?.let(::mapFeaturedVideo)
            ?.let { return it }

        val snapshot = firestore.collection("videographyHub")
            .whereEqualTo("isPublic", true)
            .limit(12)
            .get()
            .await()

        val latestDocument = snapshot.documents
            .sortedByDescending(::documentTimestamp)
            .firstOrNull()
            ?: return null

        return mapFeaturedVideo(latestDocument)
    }

    private fun mapFeaturedVideo(document: com.google.firebase.firestore.DocumentSnapshot): FeaturedVideoHighlight? {
        val title = document.getString("title").orEmpty()
        if (title.isBlank()) return null

        return FeaturedVideoHighlight(
            id = document.id,
            title = title,
            projectName = document.getString("projectName").orEmpty().ifBlank { resString(R.string.home_video_project_fallback) },
            notes = document.getString("notes").orEmpty(),
            downloadUrl = document.getString("downloadURL").orEmpty(),
            externalUrl = document.getString("externalURL").orEmpty(),
            embedUrl = document.getString("embedURL").orEmpty(),
            sourceProvider = document.getString("sourceProvider")
                ?: ExternalMediaProvider.FIREBASE_STORAGE.rawValue,
        )
    }

    private fun documentTimestamp(document: com.google.firebase.firestore.DocumentSnapshot): Long {
        return when (val createdAt = document.get("createdAt")) {
            is Timestamp -> createdAt.toDate().time
            is java.util.Date -> createdAt.time
            is Number -> createdAt.toLong()
            else -> 0L
        }
    }
}
