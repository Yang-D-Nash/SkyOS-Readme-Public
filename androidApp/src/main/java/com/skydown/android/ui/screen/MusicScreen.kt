package com.skydown.android.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.TrackRow
import com.skydown.android.ui.viewmodel.MusicViewModel

@Composable
fun MusicScreen(
    viewModel: MusicViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Skydown Music",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Die Artist-Umschaltung folgt der SwiftUI-Ansicht mit Segment-Control und Track-Liste.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
            }
        }

        item {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                uiState.availableArtists.forEachIndexed { index, artist ->
                    SegmentedButton(
                        selected = uiState.selectedArtist == artist,
                        onClick = { viewModel.selectArtist(artist) },
                        shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = uiState.availableArtists.size,
                        ),
                    ) {
                        Text(artist)
                    }
                }
            }
        }

        items(uiState.tracks, key = { it.trackId }) { track ->
            TrackRow(
                track = track,
                isPlaying = uiState.currentlyPlayingId == track.trackId,
                onPlayToggle = { viewModel.togglePreview(track.trackId) },
            )
        }
    }
}
