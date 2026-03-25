package com.skydown.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.skydown.android.data.SpotifyAuthManager
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.TrackRow
import com.skydown.android.ui.viewmodel.MusicViewModel

@Composable
fun MusicScreen(
    viewModel: MusicViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    viewModel.stopPreview()
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(uiState.currentPreviewUrl) {
        val previewUrl = uiState.currentPreviewUrl
        if (previewUrl.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(previewUrl))
            player.prepare()
            player.play()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                MusicHeroCard(
                    uiState = uiState,
                    onDisconnect = {
                        player.stop()
                        player.clearMediaItems()
                        viewModel.disconnectSpotify()
                    },
                )
            }

            item {
                ArtistPickerCard(
                    artists = uiState.availableArtists,
                    selectedArtist = uiState.selectedArtist,
                    onArtistSelected = viewModel::selectArtist,
                )
            }

            item {
                when {
                    !uiState.isSpotifyConnected -> {
                        SpotifyConnectCard(
                            selectedArtist = uiState.selectedArtist,
                            onConnect = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, SpotifyAuthManager.buildAuthorizationUri()),
                                )
                            },
                        )
                    }

                    uiState.isLoading -> {
                        MusicStatusCard(
                            title = "Tracks werden geladen",
                            body = "Wir holen gerade die Songs von ${uiState.selectedArtist}.",
                            loading = true,
                        )
                    }

                    !uiState.errorMessage.isNullOrBlank() -> {
                        MusicStatusCard(
                            title = "Spotify konnte nicht geladen werden",
                            body = uiState.errorMessage.orEmpty(),
                            actionLabel = "Erneut verbinden",
                            onAction = {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, SpotifyAuthManager.buildAuthorizationUri()),
                                )
                            },
                        )
                    }

                    uiState.tracks.isEmpty() -> {
                        MusicStatusCard(
                            title = "Noch keine Tracks gefunden",
                            body = "Fur ${uiState.selectedArtist} konnten gerade keine Songs geladen werden.",
                        )
                    }
                }
            }

            if (uiState.tracks.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        SectionHeader("Tracks")
                        Text(
                            text = "${uiState.tracks.size} Songs fur ${uiState.selectedArtist}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                        )
                    }
                }

                items(uiState.tracks, key = { it.trackId }) { track ->
                    TrackRow(
                        track = track,
                        isPlaying = uiState.currentlyPlayingId == track.trackId,
                        onPlayToggle = { viewModel.togglePreview(track) },
                    )
                }
            }
        }
    }
}

@Composable
private fun MusicHeroCard(
    uiState: com.skydown.android.ui.model.MusicUiState,
    onDisconnect: () -> Unit,
) {
    SkydownCard(
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Skydown Music",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Entdecke Releases der Skydown Artists, hore kurze Previews direkt in der App und spring bei Bedarf weiter zu Spotify.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                )
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MusicBadge(
                text = if (uiState.isSpotifyConnected) "Spotify verbunden" else "Spotify fehlt",
                imageVector = if (uiState.isSpotifyConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
            )
            MusicBadge(
                text = if (uiState.currentPreviewUrl != null) "Preview lauft" else "${uiState.tracks.size} Tracks",
                imageVector = if (uiState.currentPreviewUrl != null) Icons.Default.MusicNote else Icons.Default.CheckCircle,
            )
        }

        Text(
            text = "Aktuell ausgewahlt: ${uiState.selectedArtist}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )

        if (uiState.isSpotifyConnected) {
            Button(
                onClick = onDisconnect,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                )
                Text(
                    text = "Spotify trennen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun ArtistPickerCard(
    artists: List<String>,
    selectedArtist: String,
    onArtistSelected: (String) -> Unit,
) {
    SkydownCard {
        SectionHeader("Artists")
        Text(
            text = "Die Android-Ansicht nutzt hier bewusst scrollbare Chips statt gequetschter Segment-Buttons.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(artists, key = { it }) { artist ->
                FilterChip(
                    selected = selectedArtist == artist,
                    onClick = { onArtistSelected(artist) },
                    label = {
                        Text(
                            text = artist,
                            fontWeight = if (selectedArtist == artist) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun SpotifyConnectCard(
    selectedArtist: String,
    onConnect: () -> Unit,
) {
    SkydownCard {
        SectionHeader("Spotify")
        Text(
            text = "Verbinde Spotify, damit wir die Songs von $selectedArtist laden konnen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
            )
            Text(
                text = "Spotify verbinden",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun MusicStatusCard(
    title: String,
    body: String,
    loading: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    SkydownCard {
        SectionHeader(title)
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (loading) {
            Row(
                modifier = Modifier.padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp,
                )
                Text(
                    text = "Ladevorgang lauft ...",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Button(
                onClick = onAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun MusicBadge(
    text: String,
    imageVector: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
