package com.skydown.android.ui.screen

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.skydown.android.ui.model.MusicUiState
import com.skydown.android.ui.viewmodel.MusicViewModel

@OptIn(ExperimentalMaterial3Api::class)
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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Music",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Previews in der App, volle Songs direkt in Spotify.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                actions = {
                    if (uiState.isSpotifyConnected) {
                        IconButton(
                            onClick = {
                                player.stop()
                                player.clearMediaItems()
                                viewModel.disconnectSpotify()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                contentDescription = "Spotify trennen",
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    MusicOverviewCard(
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
                                actionLabel = "Spotify neu verbinden",
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
                                body = "Fur ${uiState.selectedArtist} wurden gerade keine Spotify-Releases gefunden.",
                            )
                        }
                    }
                }

                if (uiState.tracks.isNotEmpty()) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            SectionHeader("Tracks")
                            Text(
                                text = "${uiState.tracks.size} Titel fuer ${uiState.selectedArtist}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.68f),
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
}

@Composable
private fun MusicOverviewCard(
    uiState: MusicUiState,
    onDisconnect: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = uiState.selectedArtist,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Android folgt jetzt derselben klaren Music-Logik wie iOS: Artist auswaehlen, Preview testen, Song in Spotify fertig hoeren.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MusicBadge(
                text = if (uiState.isSpotifyConnected) "Spotify verbunden" else "Spotify bereit machen",
                imageVector = if (uiState.isSpotifyConnected) Icons.Default.CheckCircle else Icons.Default.Sync,
                isActive = uiState.isSpotifyConnected,
            )
            MusicBadge(
                text = if (uiState.currentPreviewUrl != null) "Preview laeuft" else "${uiState.tracks.size} Songs",
                imageVector = if (uiState.currentPreviewUrl != null) Icons.Default.MusicNote else Icons.Default.CheckCircle,
                isActive = uiState.currentPreviewUrl != null,
            )
        }

        Text(
            text = "Artist aktiv: ${uiState.selectedArtist}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )

        if (uiState.isSpotifyConnected) {
            OutlinedButton(
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Artists")
        Text(
            text = "Die Artists stehen auf Android jetzt ebenfalls ruhig untereinander statt als unklare Schnellwahl.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            artists.forEach { artist ->
                ArtistChoiceButton(
                    artist = artist,
                    isSelected = selectedArtist == artist,
                    onClick = { onArtistSelected(artist) },
                )
            }
        }
    }
}

@Composable
private fun ArtistChoiceButton(
    artist: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    if (isSelected) {
        Button(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            ArtistChoiceContent(
                artist = artist,
                helper = "Jetzt aktiv",
                icon = Icons.Default.CheckCircle,
                isSelected = true,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        ) {
            ArtistChoiceContent(
                artist = artist,
                helper = "Tippen zum Laden",
                icon = Icons.Default.MusicNote,
                isSelected = false,
            )
        }
    }
}

@Composable
private fun ArtistChoiceContent(
    artist: String,
    helper: String,
    icon: ImageVector,
    isSelected: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                },
            )
        }
    }
}

@Composable
private fun SpotifyConnectCard(
    selectedArtist: String,
    onConnect: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Spotify")
        Text(
            text = "Verbinde Spotify einmalig. Danach laden wir Songs von $selectedArtist und oeffnen komplette Tracks direkt in der Spotify-App.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Button(
            onClick = onConnect,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
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
                    text = "Ladevorgang laeuft ...",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun MusicBadge(
    text: String,
    imageVector: ImageVector,
    isActive: Boolean,
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = contentColor,
        )
    }
}
