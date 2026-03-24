package com.skydown.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skydown.android.data.AppContainer
import com.skydown.android.ui.model.MusicUiState
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
        selectArtist("Yang D. Nash")
    }

    fun selectArtist(artist: String) {
        viewModelScope.launch {
            val tracks = musicService.fetchTracks(artist).getOrDefault(emptyList())
            _uiState.update {
                it.copy(
                    selectedArtist = artist,
                    tracks = tracks,
                    currentlyPlayingId = null,
                )
            }
        }
    }

    fun togglePreview(trackId: Int) {
        _uiState.update {
            it.copy(
                currentlyPlayingId = if (it.currentlyPlayingId == trackId) null else trackId,
            )
        }
    }
}
