package com.skydown.android.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.skydown.android.data.AppContainer
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.data.ArtistPageUi
import com.skydown.android.data.ArtistPagesStore
import com.skydown.android.data.mediaAttributionContext
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.EditableImageFieldCard
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.TrackRow
import com.skydown.android.ui.component.YouTubePlayerDialog
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.VideoYouTubeItem
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.SpotifyGreen
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.skydown.shared.model.Track
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPageScreen(
    artistName: String,
    brand: ArtistPageBrand,
    onBack: () -> Unit,
) {
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val pages by ArtistPagesStore.pages.collectAsState()
    val context = LocalContext.current
    val editableImageAssetRepository = remember { AppContainer.editableImageAssetRepository }
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val coroutineScope = rememberCoroutineScope()
    val player = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    val page = remember(pages, artistName, brand) {
        ArtistPagesStore.pageFor(brand = brand, artistName = artistName)
    }
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)

    var isEditing by rememberSaveable(page.slug) { mutableStateOf(false) }
    var taglineDraft by rememberSaveable(page.slug) { mutableStateOf(page.tagline.orEmpty()) }
    var bioDraft by rememberSaveable(page.slug) { mutableStateOf(page.bio.orEmpty()) }
    var profileImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.profileImageURL.orEmpty()) }
    var heroImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroImageURL.orEmpty()) }
    var instagramDraft by rememberSaveable(page.slug) { mutableStateOf(page.instagramURL.orEmpty()) }
    var spotifyDraft by rememberSaveable(page.slug) { mutableStateOf(page.spotifyURL.orEmpty()) }
    var youtubeDraft by rememberSaveable(page.slug) { mutableStateOf(page.youtubeURL.orEmpty()) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var tracks by remember(page.slug) { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTracks by remember(page.slug) { mutableStateOf(true) }
    var tracksError by remember(page.slug) { mutableStateOf<String?>(null) }
    var selectedTrackId by rememberSaveable(page.slug) { mutableStateOf<Int?>(null) }
    var currentPreviewUrl by remember(page.slug) { mutableStateOf<String?>(null) }
    var currentlyPlayingId by remember(page.slug) { mutableStateOf<Int?>(null) }
    var selectedYouTubeItem by remember(page.slug) { mutableStateOf<VideoYouTubeItem?>(null) }
    var pendingImageTarget by remember { mutableStateOf<ArtistPageImageTarget?>(null) }

    val spotlightTrack = remember(tracks, selectedTrackId) {
        tracks.firstOrNull { it.trackId == selectedTrackId } ?: tracks.firstOrNull()
    }

    val latestReleaseText = remember(tracks) {
        tracks.mapNotNull { it.releaseDate?.take(10) }
            .maxOrNull()
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pendingImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            coroutineScope.launch {
                val result = editableImageAssetRepository.uploadImageAsset(
                    uri = uri,
                    contentResolver = context.contentResolver,
                )
                if (result.isSuccess) {
                    val uploadedUrl = result.getOrNull().orEmpty()
                    when (target) {
                        ArtistPageImageTarget.Profile -> profileImageDraft = uploadedUrl
                        ArtistPageImageTarget.Hero -> heroImageDraft = uploadedUrl
                    }
                    Toast.makeText(context, "Bild hochgeladen und uebernommen.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        result.exceptionOrNull()?.message ?: "Bild konnte nicht hochgeladen werden.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                pendingImageTarget = null
            }
        } else {
            pendingImageTarget = null
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    currentlyPlayingId = null
                    currentPreviewUrl = null
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(page.slug, page.updatedAtEpochMillis, isEditing) {
        if (!isEditing) {
            taglineDraft = page.tagline.orEmpty()
            bioDraft = page.bio.orEmpty()
            profileImageDraft = page.profileImageURL.orEmpty()
            heroImageDraft = page.heroImageURL.orEmpty()
            instagramDraft = page.instagramURL.orEmpty()
            spotifyDraft = page.spotifyURL.orEmpty()
            youtubeDraft = page.youtubeURL.orEmpty()
        }
    }

    LaunchedEffect(page.artistName) {
        isLoadingTracks = true
        tracksError = null

        AppContainer.musicService.fetchTracks(page.artistName)
            .onSuccess { fetchedTracks ->
                tracks = fetchedTracks
                if (selectedTrackId == null || fetchedTracks.none { it.trackId == selectedTrackId }) {
                    selectedTrackId = fetchedTracks.firstOrNull()?.trackId
                }
            }
            .onFailure { error ->
                tracks = emptyList()
                tracksError = error.message ?: "Songs konnten gerade nicht geladen werden."
                selectedTrackId = null
            }

        isLoadingTracks = false
    }

    LaunchedEffect(currentPreviewUrl) {
        val previewUrl = currentPreviewUrl
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
        topBar = {
            TopAppBar(
                title = { Text(page.artistName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    if (canEdit) {
                        Button(
                            onClick = {
                                if (isEditing) {
                                    coroutineScope.launch {
                                        isSaving = true
                                        val updatedPage = page.copy(
                                            tagline = taglineDraft.trimmedOrNull(),
                                            bio = bioDraft.trimmedOrNull(),
                                            profileImageURL = profileImageDraft.trimmedOrNull(),
                                            heroImageURL = heroImageDraft.trimmedOrNull(),
                                            instagramURL = instagramDraft.trimmedOrNull(),
                                            spotifyURL = spotifyDraft.trimmedOrNull(),
                                            youtubeURL = youtubeDraft.trimmedOrNull(),
                                            updatedAtEpochMillis = System.currentTimeMillis(),
                                            isPlaceholder = false,
                                        )
                                        val result = ArtistPagesStore.save(updatedPage)
                                        isSaving = false
                                        if (result.isSuccess) {
                                            isEditing = false
                                            Toast.makeText(context, "Artist-Seite gespeichert.", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                result.exceptionOrNull()?.message ?: "Artist-Seite konnte nicht gespeichert werden.",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                    }
                                } else {
                                    isEditing = true
                                }
                            },
                            enabled = !isSaving,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        ) {
                            Text(if (isEditing) "Speichern" else "Bearbeiten")
                        }
                    }
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(skydownScreenBrush())
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ArtistPageHeroCard(
                page = page,
                brand = brand,
                trackCount = tracks.size,
                latestReleaseText = latestReleaseText,
                onOpenYouTube = { item -> selectedYouTubeItem = item },
            )
            ArtistPageSpotlightCard(
                page = page,
                trackCount = tracks.size,
                latestReleaseText = latestReleaseText,
                spotlightTrack = spotlightTrack,
                linkCount = listOf(page.instagramURL, page.spotifyURL, page.youtubeURL).count { !it.isNullOrBlank() },
            )
            ArtistPageTracksCard(
                artistName = page.artistName,
                tracks = tracks.take(5),
                isLoading = isLoadingTracks,
                errorMessage = tracksError,
                selectedTrackId = selectedTrackId,
                currentlyPlayingId = currentlyPlayingId,
                onSelectTrack = { selectedTrackId = it },
                onPlayToggle = { track ->
                    if (currentlyPlayingId == track.trackId) {
                        currentlyPlayingId = null
                        currentPreviewUrl = null
                    } else {
                        selectedTrackId = track.trackId
                        currentlyPlayingId = track.trackId
                        currentPreviewUrl = track.previewUrl
                    }
                },
            )
            ArtistPageLinksCard(
                page = page,
                onOpenYouTube = { item -> selectedYouTubeItem = item },
            )

            if (canEdit && isEditing) {
                SkydownCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Artist-Seite bearbeiten",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )

                        ArtistPageInput(title = "Kurzzeile", value = taglineDraft, onValueChange = { taglineDraft = it })
                        ArtistPageInput(title = "Bio", value = bioDraft, onValueChange = { bioDraft = it }, singleLine = false)
                        EditableImageFieldCard(
                            title = "Profilbild",
                            imageUrl = profileImageDraft,
                            onPickImage = {
                                pendingImageTarget = ArtistPageImageTarget.Profile
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onImageUrlChange = { profileImageDraft = it },
                        )
                        EditableImageFieldCard(
                            title = "Hero-Bild",
                            imageUrl = heroImageDraft,
                            onPickImage = {
                                pendingImageTarget = ArtistPageImageTarget.Hero
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onImageUrlChange = { heroImageDraft = it },
                        )
                        ArtistPageInput(title = "Instagram", value = instagramDraft, onValueChange = { instagramDraft = it })
                        ArtistPageInput(title = "Spotify", value = spotifyDraft, onValueChange = { spotifyDraft = it })
                        ArtistPageInput(title = "YouTube", value = youtubeDraft, onValueChange = { youtubeDraft = it })
                    }
                }
            }
        }
    }

    selectedYouTubeItem?.let { item ->
        YouTubePlayerDialog(
            item = item,
            onDismiss = { selectedYouTubeItem = null },
            onOpenExternal = { url -> openExternalLink(context, url) },
        )
    }
}

private enum class ArtistPageImageTarget {
    Profile,
    Hero,
}

@Composable
private fun ArtistPageHeroCard(
    page: ArtistPageUi,
    brand: ArtistPageBrand,
    trackCount: Int,
    latestReleaseText: String?,
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(0.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(286.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            if (!page.heroImageURL.isNullOrBlank()) {
                AsyncImage(
                    model = page.heroImageURL,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    SpotifyGreen.copy(alpha = 0.88f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                ),
                            ),
                        ),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.18f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.84f),
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ArtistHeroTag(text = brand.displayTitle, background = Color.White.copy(alpha = 0.14f))
                    if (page.hasCustomPresentation) {
                        ArtistHeroTag(text = "Live", background = SpotifyGreen.copy(alpha = 0.84f))
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (trackCount > 0) {
                        ArtistHeroTag(text = "$trackCount Songs", background = Color.Black.copy(alpha = 0.32f))
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!page.profileImageURL.isNullOrBlank()) {
                            AsyncImage(
                                model = page.profileImageURL,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Text(
                                text = page.artistName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = page.artistName,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                        )
                        Text(
                            text = page.tagline ?: "${brand.displayTitle} Profil",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.84f),
                        )
                        latestReleaseText?.let {
                            Text(
                                text = "Latest $it",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.76f),
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = page.bio ?: "Noch keine Artist-Seite hinterlegt. Owner oder zugewiesene Editoren koennen hier eine repraesentative Kurzbeschreibung anlegen.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ArtistMetricCard(
                    title = "Artist",
                    value = brand.displayTitle,
                    modifier = Modifier.weight(1f),
                )
                if (trackCount > 0) {
                    ArtistMetricCard(
                        title = "Songs",
                        value = "$trackCount",
                        modifier = Modifier.weight(1f),
                    )
                }
                latestReleaseText?.let {
                    ArtistMetricCard(
                        title = "Latest",
                        value = it,
                        modifier = Modifier.weight(1f),
                    )
                }
                val linkCount = listOf(page.instagramURL, page.spotifyURL, page.youtubeURL).count { !it.isNullOrBlank() }
                if (linkCount > 0) {
                    ArtistMetricCard(
                        title = "Links",
                        value = "$linkCount",
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            ArtistPageHeroQuickLinks(
                page = page,
                onOpenYouTube = onOpenYouTube,
            )

            if (page.editorUids.isNotEmpty()) {
                ArtistEditorBadge(count = page.editorUids.size)
            }
        }
    }
}

@Composable
private fun ArtistPageLinksCard(
    page: ArtistPageUi,
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
) {
    val context = LocalContext.current
    val links = rememberArtistLinks(page)

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Links",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (links.isEmpty()) {
                Text(
                    text = "Noch keine Links hinterlegt.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
            } else {
                links.forEach { link ->
                    Button(
                        onClick = {
                            if (link.kind == ArtistPageLinkKind.YouTube) {
                                onOpenYouTube(
                                    VideoYouTubeItem(
                                        id = "artist-${page.slug}-links-youtube",
                                        title = page.artistName,
                                        subtitle = link.subtitle,
                                        url = link.url,
                                    ),
                                )
                            } else {
                                openExternalLink(context, link.url)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = link.backgroundColor,
                            contentColor = link.foregroundColor,
                        ),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        Icon(link.icon, contentDescription = null, tint = link.accentColor)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            Text(
                                text = link.title,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = link.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = link.foregroundColor.copy(alpha = 0.74f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPageHeroQuickLinks(
    page: ArtistPageUi,
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
) {
    val context = LocalContext.current
    val links = rememberArtistLinks(page).take(3)

    if (links.isEmpty()) {
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        links.forEach { link ->
            Button(
                onClick = {
                    if (link.kind == ArtistPageLinkKind.YouTube) {
                        onOpenYouTube(
                            VideoYouTubeItem(
                                id = "artist-${page.slug}-hero-youtube",
                                title = page.artistName,
                                subtitle = link.subtitle,
                                url = link.url,
                            ),
                        )
                    } else {
                        openExternalLink(context, link.url)
                    }
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = link.backgroundColor,
                    contentColor = link.foregroundColor,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Icon(link.icon, contentDescription = null, tint = link.accentColor)
                Text(
                    text = link.title,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun ArtistPageSpotlightCard(
    page: ArtistPageUi,
    trackCount: Int,
    latestReleaseText: String?,
    spotlightTrack: Track?,
    linkCount: Int,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Spotlight")
            Text(
                text = page.tagline ?: "${page.artistName} entdecken.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ArtistPill(text = "$trackCount Songs")
                latestReleaseText?.let { ArtistPill(text = it) }
                if (linkCount > 0) {
                    ArtistPill(text = "$linkCount Links")
                }
            }

            spotlightTrack?.let { track ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AsyncImage(
                        model = track.artworkUrl100,
                        contentDescription = null,
                        modifier = Modifier
                            .size(82.dp)
                            .clip(RoundedCornerShape(20.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Jetzt antesten",
                            style = MaterialTheme.typography.labelLarge,
                            color = SpotifyGreen,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = track.trackName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = track.collectionName ?: page.artistName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                        Text(
                            text = "Direkt unten mit Preview oder Spotify Player weiter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPageTracksCard(
    artistName: String,
    tracks: List<Track>,
    isLoading: Boolean,
    errorMessage: String?,
    selectedTrackId: Int?,
    currentlyPlayingId: Int?,
    onSelectTrack: (Int) -> Unit,
    onPlayToggle: (Track) -> Unit,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader("Top Songs")

            when {
                isLoading -> {
                    Text(
                        text = "Songs werden geladen ...",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                !errorMessage.isNullOrBlank() -> {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                tracks.isEmpty() -> {
                    Text(
                        text = "Fuer $artistName sind gerade noch keine Songs hinterlegt.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }

                else -> {
                    Text(
                        text = "Direkt mit Preview oder Spotify Player in den Sound rein.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        tracks.forEach { track ->
                            TrackRow(
                                track = track,
                                isPlaying = currentlyPlayingId == track.trackId,
                                isSelected = selectedTrackId == track.trackId,
                                onSelectTrack = { onSelectTrack(track.trackId) },
                                onPlayToggle = { onPlayToggle(track) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPill(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ArtistHeroTag(
    text: String,
    background: Color,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        color = Color.White,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
    )
}

@Composable
private fun ArtistMetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
        )
    }
}

private data class ArtistPageLinkUi(
    val title: String,
    val subtitle: String,
    val url: String,
    val kind: ArtistPageLinkKind,
    val icon: ImageVector,
    val accentColor: Color,
    val backgroundColor: Color,
    val foregroundColor: Color,
)

private enum class ArtistPageLinkKind {
    Instagram,
    Spotify,
    YouTube,
}

@Composable
private fun rememberArtistLinks(page: ArtistPageUi): List<ArtistPageLinkUi> {
    val youtubeTint = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    return remember(page, youtubeTint, surfaceVariant, onSurface) {
        buildList {
            page.instagramURL?.trimmedOrNull()?.let {
                add(
                    ArtistPageLinkUi(
                        title = "Instagram",
                        subtitle = "${page.artistName} direkt verfolgen",
                        url = it,
                        kind = ArtistPageLinkKind.Instagram,
                        icon = Icons.Default.CameraAlt,
                        accentColor = InstagramPink,
                        backgroundColor = InstagramPink.copy(alpha = 0.14f),
                        foregroundColor = onSurface,
                    )
                )
            }
            page.spotifyURL?.trimmedOrNull()?.let {
                add(
                    ArtistPageLinkUi(
                        title = "Spotify",
                        subtitle = "Artist Profil und ganze Releases",
                        url = it,
                        kind = ArtistPageLinkKind.Spotify,
                        icon = Icons.Default.MusicNote,
                        accentColor = SpotifyGreen,
                        backgroundColor = SpotifyGreen.copy(alpha = 0.16f),
                        foregroundColor = onSurface,
                    )
                )
            }
            page.youtubeURL?.trimmedOrNull()?.let {
                add(
                    ArtistPageLinkUi(
                        title = "YouTube",
                        subtitle = "Videos und Releases",
                        url = it,
                        kind = ArtistPageLinkKind.YouTube,
                        icon = Icons.Default.PlayCircleFilled,
                        accentColor = youtubeTint,
                        backgroundColor = surfaceVariant.copy(alpha = 0.92f),
                        foregroundColor = onSurface,
                    )
                )
            }
        }
    }
}

@Composable
private fun ArtistEditorBadge(count: Int) {
    Text(
        text = "$count Editor${if (count == 1) "" else "en"}",
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.onSurface,
        style = MaterialTheme.typography.labelMedium,
    )
}

@Composable
private fun ArtistPageInput(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(title) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 4,
        shape = RoundedCornerShape(18.dp),
    )
}

private fun String?.trimmedOrNull(): String? {
    val trimmed = this?.trim().orEmpty()
    return trimmed.ifBlank { null }
}
