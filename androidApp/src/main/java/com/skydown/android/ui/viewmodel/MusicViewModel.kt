package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.ui.model.MusicUiState
import com.skydown.shared.model.Track
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MusicViewModel : ViewModel() {
    private val musicService = AppContainer.musicService
    private val _uiState = MutableStateFlow(MusicUiState())
    val uiState: StateFlow<MusicUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            SpotifyAuthManager.isConnected.collectLatest { connected ->
                _uiState.update {
                    if (connected) {
                        it.copy(
                            isSpotifyConnected = true,
                            errorMessage = null,
                        )
                    } else {
                        it.copy(
                            isSpotifyConnected = false,
                            errorMessage = null,
                            currentlyPlayingId = null,
                            currentPreviewUrl = null,
                        )
                    }
                }

                val currentState = _uiState.value
                if (currentState.tracks.isEmpty() && !currentState.isLoading) {
                    selectArtist(_uiState.value.selectedArtist)
                }
            }
        }

        selectArtist(_uiState.value.selectedArtist)
    }

    fun selectArtist(artist: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    selectedArtist = artist,
                    isLoading = true,
                    tracks = emptyList(),
                    currentlyPlayingId = null,
                    currentPreviewUrl = null,
                    errorMessage = null,
                )
            }

            musicService.fetchTracks(artist)
                .onSuccess { tracks ->
                    _uiState.update {
                        it.copy(
                            selectedArtist = artist,
                            isLoading = false,
                            tracks = tracks,
                            currentlyPlayingId = null,
                            currentPreviewUrl = null,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            selectedArtist = artist,
                            isLoading = false,
                            tracks = emptyList(),
                            currentlyPlayingId = null,
                            currentPreviewUrl = null,
                            errorMessage = error.message ?: "Tracks konnten gerade nicht geladen werden.",
                        )
                    }
                }
        }
    }

    fun togglePreview(track: Track) {
        _uiState.update {
            if (it.currentlyPlayingId == track.trackId) {
                it.copy(
                    currentlyPlayingId = null,
                    currentPreviewUrl = null,
                )
            } else {
                it.copy(
                    currentlyPlayingId = track.trackId,
                    currentPreviewUrl = track.previewUrl,
                )
            }
        }
    }

    fun stopPreview() {
        _uiState.update {
            it.copy(
                currentlyPlayingId = null,
                currentPreviewUrl = null,
            )
        }
    }

    fun disconnectSpotify() {
        SpotifyAuthManager.disconnect()
        _uiState.update {
            it.copy(
                currentlyPlayingId = null,
                currentPreviewUrl = null,
            )
        }
    }

    fun clearSpotifyError() {
        SpotifyAuthManager.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }
}
