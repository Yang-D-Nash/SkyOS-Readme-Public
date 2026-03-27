package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.skydown.android.ui.component.openTrackInSpotify
import coil3.compose.AsyncImage
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.model.FeaturedBeatHighlight
import com.skydown.android.ui.model.FeaturedVideoHighlight
import com.skydown.android.ui.model.HomeUiState
import com.skydown.android.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var activeDestination by rememberSaveable { mutableStateOf<String?>(null) }

    if (activeDestination == homeDestinationNicmaProducer) {
        NicmaProducerScreen(
            onBack = { activeDestination = null },
        )
        return
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val context = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    var currentAudioKey by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.stop()
                    player.clearMediaItems()
                    currentAudioKey = null
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { SkydownTopBarTitle("Skydown x 22", "Hip Hop, Music, Video.") },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
                    ) {
                        IconButton(onClick = viewModel::refresh) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Home aktualisieren",
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
                    skydownScreenBrush(),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    HomeHeroCard()
                }

                item {
                    HomeLatestReleaseCard(
                        uiState = uiState,
                        isPlaying = currentAudioKey == uiState.featuredTrack?.let(::homeTrackAudioKey),
                        onPlayToggle = { track ->
                            val previewUrl = track.previewUrl
                            if (previewUrl.isNullOrBlank()) return@HomeLatestReleaseCard
                            val audioKey = homeTrackAudioKey(track)
                            if (currentAudioKey == audioKey) {
                                player.stop()
                                player.clearMediaItems()
                                currentAudioKey = null
                            } else {
                                player.setMediaItem(MediaItem.fromUri(previewUrl))
                                player.prepare()
                                player.play()
                                currentAudioKey = audioKey
                            }
                        },
                        onOpenSpotify = { track ->
                            openTrackInSpotify(
                                context = context,
                                spotifyArtistId = track.spotifyArtistId,
                                spotifyTrackId = track.spotifyTrackId,
                                externalUrl = track.externalUrl,
                            )
                        },
                    )
                }

                item {
                    HomeLatestBeatCard(
                        uiState = uiState,
                        isPlaying = currentAudioKey == uiState.featuredBeat?.let(::homeBeatAudioKey),
                        onPlayToggle = { beat ->
                            if (!beat.isPlayable || beat.downloadUrl.isBlank()) return@HomeLatestBeatCard
                            val audioKey = homeBeatAudioKey(beat)
                            if (currentAudioKey == audioKey) {
                                player.stop()
                                player.clearMediaItems()
                                currentAudioKey = null
                            } else {
                                player.setMediaItem(MediaItem.fromUri(beat.downloadUrl))
                                player.prepare()
                                player.play()
                                currentAudioKey = audioKey
                            }
                        },
                    )
                }

                item {
                    HomeLatestVideoCard(
                        uiState = uiState,
                        onOpenVideo = { video ->
                            if (video.downloadUrl.isNotBlank()) {
                                openExternalLink(
                                    context = context,
                                    url = video.downloadUrl,
                                    browserMissingMessage = "Kein Video-Player gefunden.",
                                )
                            }
                        },
                    )
                }

                item {
                    HomeNicmaProducerCard(
                        onOpen = { activeDestination = homeDestinationNicmaProducer },
                    )
                }

                item {
                    HomeStoryCard()
                }
            }
        }
    }
}

@Composable
private fun HomeHeroCard() {
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
                    text = "Skydown x 22",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Hip Hop, Music, Video.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Composable
private fun HomeLatestReleaseCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    onPlayToggle: (com.skydown.shared.model.Track) -> Unit,
    onOpenSpotify: (com.skydown.shared.model.Track) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Neuester Release")
        val track = uiState.featuredTrack

        if (track == null) {
            Text(
                text = uiState.homeTrackMessage ?: "Neuer Song erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.artworkUrl100,
                contentDescription = track.trackName,
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = track.trackName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = track.artistName ?: "Skydown x 22",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Text(
                    text = buildString {
                        append(track.collectionName ?: "Spotify Release")
                        track.releaseDate?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
        }

        val hasPreview = !track.previewUrl.isNullOrBlank()
        val hasSpotifyTarget = homeHasSpotifyTarget(track)

        Text(
            text = when {
                hasPreview && hasSpotifyTarget -> "Preview direkt im Home oder direkt bei Spotify weiterhoeren."
                hasPreview -> "Preview direkt im Home verfuegbar."
                homeResolvedSpotifyTrackId(track) != null -> "Direkt mit Spotify verbunden."
                homeResolvedSpotifyArtistId(track) != null -> "Spotify-Artist direkt erreichbar."
                !track.externalUrl.isNullOrBlank() -> "Spotify-Ziel direkt erreichbar."
                else -> "Neuester Song."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (hasPreview || hasSpotifyTarget) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (hasPreview) {
                    Button(
                        onClick = { onPlayToggle(track) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Text(
                            text = if (isPlaying) "Release stoppen" else "Release abspielen",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }

                if (hasSpotifyTarget) {
                    OutlinedButton(
                        onClick = { onOpenSpotify(track) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                        )
                        Text(
                            text = homeSpotifyActionLabel(track),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLatestBeatCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    onPlayToggle: (FeaturedBeatHighlight) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Neuester Beat")
        val beat = uiState.featuredBeat

        if (beat == null) {
            Text(
                text = uiState.homeBeatMessage ?: "Neuer Beat erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = beat.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = beat.artistName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (beat.notes.isNotBlank()) {
                    Text(
                        text = beat.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        if (beat.isPlayable && beat.downloadUrl.isNotBlank()) {
            OutlinedButton(
                onClick = { onPlayToggle(beat) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                )
                Text(
                    text = if (isPlaying) "Beat stoppen" else "Beat abspielen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeLatestVideoCard(
    uiState: HomeUiState,
    onOpenVideo: (FeaturedVideoHighlight) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Video Highlight")
        val video = uiState.featuredVideo

        if (video == null) {
            Text(
                text = uiState.homeVideoMessage ?: "Neues Video erscheint hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(34.dp),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = video.projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        Text(
            text = "Aus dem Videography Hub fuers Home ausgewaehlt.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (video.downloadUrl.isNotBlank()) {
            OutlinedButton(
                onClick = { onOpenVideo(video) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                )
                Text(
                    text = "Video oeffnen",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeStoryCard() {
    val context = LocalContext.current
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Links")

        Text(
            text = "Hier laufen die direkten Social-Links von Yang D. Nash, 22, Skydown und NICMA MUSIC zusammen. Der Musik-Tab bleibt dadurch fokussierter auf Releases und Beats.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/y.d.nash/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Language,
                    contentDescription = null,
                )
                Text(
                    text = "Yang D. Nash auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/zweizwei_music/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                )
                Text(
                    text = "22 auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/skydown_entertainment/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                )
                Text(
                    text = "Skydown auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    openExternalLink(context, "https://www.instagram.com/nicma.music/")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                )
                Text(
                    text = "NICMA MUSIC auf Instagram",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }

            OutlinedButton(
                onClick = {
                    openEmailDraft(
                        context = context,
                        recipients = listOf("skydownent@gmail.com"),
                        subject = "Skydown x 22 Kontakt",
                        body = "",
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                )
                Text(
                    text = "Kontakt per E-Mail",
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HomeNicmaProducerCard(
    onOpen: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "NICMA MUSIC",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Producer-Seite mit Preisliste fuer Mixing, Mastering und Recording direkt aus dem Home.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        OutlinedButton(
            onClick = onOpen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
            )
            Text(
                text = "NICMA MUSIC oeffnen",
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
private fun HomeBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
) {
    val backgroundColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.70f)
    }
    val contentColor = if (isActive) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeBeatAudioKey(beat: FeaturedBeatHighlight): String = "beat:${beat.id}"

private fun homeHasSpotifyTarget(track: com.skydown.shared.model.Track): Boolean {
    return homeResolvedSpotifyTrackId(track) != null ||
        homeResolvedSpotifyArtistId(track) != null ||
        !track.externalUrl.isNullOrBlank()
}

private fun homeSpotifyActionLabel(track: com.skydown.shared.model.Track): String {
    return when {
        homeResolvedSpotifyTrackId(track) != null -> "Song auf Spotify"
        homeResolvedSpotifyArtistId(track) != null -> "Artist auf Spotify"
        else -> "Spotify oeffnen"
    }
}

private fun homeResolvedSpotifyTrackId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyTrackId.isNullOrBlank()) return track.spotifyTrackId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val trackIndex = pathSegments.indexOf("track")
    if (trackIndex == -1 || trackIndex + 1 >= pathSegments.size) return null
    return pathSegments[trackIndex + 1]
}

private fun homeResolvedSpotifyArtistId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyArtistId.isNullOrBlank()) return track.spotifyArtistId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val artistIndex = pathSegments.indexOf("artist")
    if (artistIndex == -1 || artistIndex + 1 >= pathSegments.size) return null
    return pathSegments[artistIndex + 1]
}

private const val homeDestinationNicmaProducer = "home_nicma_producer"
