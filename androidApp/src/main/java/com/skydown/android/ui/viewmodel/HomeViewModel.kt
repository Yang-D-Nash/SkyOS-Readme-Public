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
                        "Verbinde Spotify im Musikbereich, damit der neueste Release direkt hier erscheint."
                    } else {
                        null
                    },
                    homeVideoMessage = if (latestVideo == null) {
                        "Sobald das erste oeffentliche Video live ist, taucht es hier direkt auf."
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
        val snapshot = firestore.collection("videographyHub")
            .whereEqualTo("isPublic", true)
            .limit(12)
            .get()
            .await()

        val latestDocument = snapshot.documents
            .sortedByDescending { document ->
                when (val createdAt = document.get("createdAt")) {
                    is Timestamp -> createdAt.toDate().time
                    is java.util.Date -> createdAt.time
                    is Number -> createdAt.toLong()
                    else -> 0L
                }
            }
            .firstOrNull()
            ?: return null

        val title = latestDocument.getString("title").orEmpty()
        if (title.isBlank()) return null

        return FeaturedVideoHighlight(
            title = title,
            projectName = latestDocument.getString("projectName").orEmpty().ifBlank { "Skydown Visual" },
            notes = latestDocument.getString("notes").orEmpty(),
        )
    }
}
