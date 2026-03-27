package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
import com.skydown.shared.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class HomeViewModel : ViewModel() {
    private val musicService = AppContainer.musicService
    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val featuredArtists = listOf(
        "Yang D. Nash",
        "ThaDude",
        "MAVE",
        "JANNO",
        "TANGAJOE007",
        "Toprack941",
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val latestTrack = loadLatestTrack()
            val latestVideo = loadLatestVideo()

            _uiState.update {
                it.copy(
                    featuredTrack = latestTrack,
                    featuredVideo = latestVideo,
                    homeTrackMessage = if (latestTrack == null) {
                        "Sobald ein neuer Release verfuegbar ist, taucht er hier direkt auf."
                    } else {
                        null
                    },
                    homeVideoMessage = if (latestVideo == null) {
                        "Sobald ein oeffentliches Video live ist, taucht hier dein Highlight auf."
                    } else {
                        null
                    },
                )
            }
        }
    }

    private suspend fun loadLatestTrack(): Track? {
        var latestTrack: Track? = null

        featuredArtists.forEach { artist ->
            val tracks = musicService.fetchTracks(artist).getOrNull().orEmpty()
            tracks.forEach { track ->
                val candidateDate = track.releaseDate.orEmpty()
                val currentDate = latestTrack?.releaseDate.orEmpty()
                if (latestTrack == null || candidateDate > currentDate) {
                    latestTrack = track
                }
            }
        }

        return latestTrack
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
            title = title,
            projectName = document.getString("projectName").orEmpty().ifBlank { "Skydown Visual" },
            notes = document.getString("notes").orEmpty(),
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
