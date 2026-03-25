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
                _uiState.update { it.copy(isSpotifyConnected = connected) }
                if (connected && _uiState.value.tracks.isEmpty()) {
                    selectArtist(_uiState.value.selectedArtist)
                }
            }
        }
    }

    fun selectArtist(artist: String) {
        viewModelScope.launch {
            if (!_uiState.value.isSpotifyConnected) {
                _uiState.update {
                    it.copy(
                        selectedArtist = artist,
                        tracks = emptyList(),
                        currentlyPlayingId = null,
                        currentPreviewUrl = null,
                    )
                }
                return@launch
            }

            val tracks = musicService.fetchTracks(artist).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    selectedArtist = artist,
                    tracks = tracks,
                    currentlyPlayingId = null,
                    currentPreviewUrl = null,
                )
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
}
