package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.FeaturedBeatHighlight
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
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

class HomeViewModel : ViewModel() {
    private val musicService = AppContainer.musicService
    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

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
        viewModelScope.launch {
            supervisorScope {
                launch {
                    val latestTrack = loadLatestTrack()
                    _uiState.update {
                        it.copy(
                            featuredTrack = latestTrack,
                            homeTrackMessage = if (latestTrack == null) {
                                "Sobald ein neuer Release verfuegbar ist, taucht er hier direkt auf."
                            } else {
                                null
                            },
                        )
                    }
                }

                launch {
                    val latestBeat = loadLatestBeat()
                    _uiState.update {
                        it.copy(
                            featuredBeat = latestBeat,
                            homeBeatMessage = if (latestBeat == null) {
                                "Sobald ein freigegebener Beat live ist, taucht er hier direkt auf."
                            } else {
                                null
                            },
                        )
                    }
                }

                launch {
                    val latestVideo = loadLatestVideo()
                    _uiState.update {
                        it.copy(
                            featuredVideo = latestVideo,
                            homeVideoMessage = if (latestVideo == null) {
                                "Sobald ein oeffentliches Video live ist, taucht hier dein Highlight auf."
                            } else {
                                null
                            },
                        )
                    }
                }
            }
        }
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

        val lhsHasPlayback = !lhs.previewUrl.isNullOrBlank() || !lhs.externalUrl.isNullOrBlank()
        val rhsHasPlayback = !rhs.previewUrl.isNullOrBlank() || !rhs.externalUrl.isNullOrBlank()
        if (lhsHasPlayback != rhsHasPlayback) {
            return if (lhsHasPlayback) -1 else 1
        }

        return lhs.trackName.lowercase().compareTo(rhs.trackName.lowercase())
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

    private suspend fun loadLatestBeat(): FeaturedBeatHighlight? {
        val snapshot = firestore.collection("nicmaBeatHub")
            .whereEqualTo("isPublic", true)
            .limit(20)
            .get()
            .await()

        val latestDocument = snapshot.documents
            .sortedByDescending(::documentTimestamp)
            .firstOrNull()
            ?: return null

        return mapFeaturedBeat(latestDocument)
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
            projectName = document.getString("projectName").orEmpty().ifBlank { "Skydown Visual" },
            notes = document.getString("notes").orEmpty(),
            downloadUrl = document.getString("downloadURL").orEmpty(),
        )
    }

    private fun mapFeaturedBeat(document: com.google.firebase.firestore.DocumentSnapshot): FeaturedBeatHighlight? {
        val title = document.getString("title").orEmpty()
        val artistName = document.getString("artistName").orEmpty()
        val downloadUrl = document.getString("downloadURL").orEmpty()
        val fileName = document.getString("fileName").orEmpty()
        val mimeType = document.getString("mimeType").orEmpty()
        if (title.isBlank()) return null

        return FeaturedBeatHighlight(
            id = document.id,
            title = title,
            artistName = artistName.ifBlank { "Skydown Beat" },
            notes = document.getString("notes").orEmpty(),
            downloadUrl = downloadUrl,
            isPlayable = mimeType.startsWith("audio/") ||
                fileName.endsWith(".mp3", ignoreCase = true) ||
                fileName.endsWith(".wav", ignoreCase = true) ||
                fileName.endsWith(".m4a", ignoreCase = true),
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
