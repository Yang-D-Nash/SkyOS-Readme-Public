package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.SpotifyAuthManager
import com.nash.skyos.ui.model.MusicUiState
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
                            errorMessage = error.message ?: "Tracks konnten gerade nicht geladen werden.",
                        )
                    }
                }
        }
    }

    fun disconnectSpotify() {
        SpotifyAuthManager.disconnect()
    }

    fun clearSpotifyError() {
        SpotifyAuthManager.clearError()
        _uiState.update { it.copy(errorMessage = null) }
    }
}
