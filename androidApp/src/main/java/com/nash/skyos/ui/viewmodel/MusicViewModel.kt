package com.nash.skyos.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.SpotifyAuthManager
import com.nash.skyos.ui.model.MusicUiState
import com.nash.skyos.ui.model.mergeZweizweiMusicArtists
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
            ArtistPagesStore.pages.collectLatest { pages ->
                val artists = mergeZweizweiMusicArtists(
                    pages
                        .filter { it.brand == ArtistPageBrand.Zweizwei }
                        .map { it.artistName },
                )
                _uiState.update { state ->
                    val selectedArtist = artists.firstOrNull { it.equals(state.selectedArtist, ignoreCase = true) }
                        ?: artists.firstOrNull()
                        ?: state.selectedArtist
                    state.copy(
                        availableArtists = artists,
                        selectedArtist = selectedArtist,
                    )
                }
            }
        }

        viewModelScope.launch {
            SpotifyAuthManager.isConnected.collectLatest { connected ->
                setSpotifyConnectionState(connected)

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
            setArtistLoading(artist)

            musicService.fetchTracks(artist)
                .onSuccess { tracks ->
                    applyTrackLoadSuccess(artist, tracks)
                }
                .onFailure { error ->
                    applyTrackLoadFailure(
                        artist = artist,
                        message = error.message ?: "Tracks konnten gerade nicht geladen werden.",
                    )
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

    private fun setSpotifyConnectionState(connected: Boolean) {
        _uiState.update { it.copy(isSpotifyConnected = connected, errorMessage = null) }
    }

    private fun setArtistLoading(artist: String) {
        _uiState.update {
            it.copy(
                selectedArtist = artist,
                isLoading = true,
                tracks = emptyList(),
                errorMessage = null,
            )
        }
    }

    private fun applyTrackLoadSuccess(artist: String, tracks: List<Track>) {
        _uiState.update {
            it.copy(
                selectedArtist = artist,
                isLoading = false,
                tracks = tracks,
                errorMessage = null,
            )
        }
    }

    private fun applyTrackLoadFailure(artist: String, message: String) {
        _uiState.update {
            it.copy(
                selectedArtist = artist,
                isLoading = false,
                tracks = emptyList(),
                errorMessage = message,
            )
        }
    }
}
