package com.nash.skyos.ui.screen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPageUi
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.StudioPriceItemUi
import com.nash.skyos.data.mediaAttributionContext
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandHeroMetricCard
import com.nash.skyos.ui.component.BrandPreviewFrame
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.EditableImageFieldCard
import com.nash.skyos.ui.component.EditableVideoFieldCard
import com.nash.skyos.ui.component.LocalSessionUser
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.TrackRow
import com.nash.skyos.ui.component.YouTubePlayerDialog
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.VideoYouTubeItem
import com.nash.skyos.ui.theme.InstagramPink
import com.nash.skyos.ui.theme.SpotifyGreen
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.shared.model.Track
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPageScreen(
    artistName: String,
    brand: ArtistPageBrand,
    onBack: () -> Unit,
) {
    val currentUser = LocalSessionUser.current
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
    val heroVideoPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f
        }
    }
    val page = remember(pages, artistName, brand) {
        ArtistPagesStore.pageFor(brand = brand, artistName = artistName)
    }
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)
    val allowsStudioPriceEditing = brand == ArtistPageBrand.Nicma && page.artistName.equals("NICMA STUDIO", ignoreCase = true)
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val listState = rememberLazyListState()
    val visualStyle = artistPageVisualStyle(brand)

    var isEditing by rememberSaveable(page.slug) { mutableStateOf(false) }
    var taglineDraft by rememberSaveable(page.slug) { mutableStateOf(page.tagline.orEmpty()) }
    var bioDraft by rememberSaveable(page.slug) { mutableStateOf(page.bio.orEmpty()) }
    var profileImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.profileImageURL.orEmpty()) }
    var heroImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroImageURL.orEmpty()) }
    var heroVideoDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroVideoURL.orEmpty()) }
    var instagramDraft by rememberSaveable(page.slug) { mutableStateOf(page.instagramURL.orEmpty()) }
    var spotifyDraft by rememberSaveable(page.slug) { mutableStateOf(page.spotifyURL.orEmpty()) }
    var youtubeDraft by rememberSaveable(page.slug) { mutableStateOf(page.youtubeURL.orEmpty()) }
    var studioPriceListDraft by rememberSaveable(page.slug) {
        mutableStateOf(
            page.studioPriceList.joinToString("\n") { "${it.title} | ${it.detail} | ${it.price}" }
        )
    }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var tracks by remember(page.slug) { mutableStateOf<List<Track>>(emptyList()) }
    var isLoadingTracks by remember(page.slug) { mutableStateOf(true) }
    var tracksError by remember(page.slug) { mutableStateOf<String?>(null) }
    var selectedTrackId by rememberSaveable(page.slug) { mutableStateOf<Int?>(null) }
    var currentPreviewUrl by remember(page.slug) { mutableStateOf<String?>(null) }
    var currentlyPlayingId by remember(page.slug) { mutableStateOf<Int?>(null) }
    var selectedYouTubeItem by remember(page.slug) { mutableStateOf<VideoYouTubeItem?>(null) }
    var pendingImageTarget by remember { mutableStateOf<ArtistPageImageTarget?>(null) }
    var activeImageUploadTarget by remember { mutableStateOf<ArtistPageImageTarget?>(null) }
    var isUploadingHeroVideo by remember(page.slug) { mutableStateOf(false) }
    var feedbackMessage by remember(page.slug) { mutableStateOf<String?>(null) }
    var feedbackType by remember(page.slug) { mutableStateOf(ToastType.Info) }
    var editingBaseProfileImageUrl by rememberSaveable(page.slug) { mutableStateOf(page.profileImageURL.orEmpty()) }
    var editingBaseHeroImageUrl by rememberSaveable(page.slug) { mutableStateOf(page.heroImageURL.orEmpty()) }
    var editingBaseHeroVideoUrl by rememberSaveable(page.slug) { mutableStateOf(page.heroVideoURL.orEmpty()) }
    val temporaryUploadedAssetUrls = remember(page.slug) { mutableStateListOf<String>() }
    val isUploadingAsset = activeImageUploadTarget != null || isUploadingHeroVideo

    val displayPage = remember(
        page,
        isEditing,
        taglineDraft,
        bioDraft,
        profileImageDraft,
        heroImageDraft,
        heroVideoDraft,
        instagramDraft,
        spotifyDraft,
        youtubeDraft,
        studioPriceListDraft,
    ) {
        if (!isEditing) {
            page
        } else {
            page.copy(
                tagline = taglineDraft.trimmedOrNull(),
                bio = bioDraft.trimmedOrNull(),
                profileImageURL = profileImageDraft.trimmedOrNull(),
                heroImageURL = heroImageDraft.trimmedOrNull(),
                heroVideoURL = heroVideoDraft.trimmedOrNull(),
                instagramURL = instagramDraft.trimmedOrNull(),
                spotifyURL = spotifyDraft.trimmedOrNull(),
                youtubeURL = youtubeDraft.trimmedOrNull(),
                studioPriceList = if (allowsStudioPriceEditing) parseStudioPriceItems(studioPriceListDraft) else page.studioPriceList,
                isPlaceholder = false,
            )
        }
    }

    val spotlightTrack = remember(tracks, selectedTrackId) {
        tracks.firstOrNull { it.trackId == selectedTrackId } ?: tracks.firstOrNull()
    }

    val latestReleaseText = remember(tracks) {
        tracks.mapNotNull { it.releaseDate?.take(10) }
            .maxOrNull()
    }

    val currentEditableImageUrl: (ArtistPageImageTarget) -> String = { target ->
        when (target) {
            ArtistPageImageTarget.Profile -> profileImageDraft
            ArtistPageImageTarget.Hero -> heroImageDraft
        }
    }
    val applyEditableImageUrl: (ArtistPageImageTarget, String) -> Unit = { target, imageUrl ->
        when (target) {
            ArtistPageImageTarget.Profile -> profileImageDraft = imageUrl
            ArtistPageImageTarget.Hero -> heroImageDraft = imageUrl
        }
    }

    fun resetDraftsFromPage() {
        taglineDraft = page.tagline.orEmpty()
        bioDraft = page.bio.orEmpty()
        profileImageDraft = page.profileImageURL.orEmpty()
        heroImageDraft = page.heroImageURL.orEmpty()
        heroVideoDraft = page.heroVideoURL.orEmpty()
        instagramDraft = page.instagramURL.orEmpty()
        spotifyDraft = page.spotifyURL.orEmpty()
        youtubeDraft = page.youtubeURL.orEmpty()
        studioPriceListDraft = page.studioPriceList.joinToString("\n") { "${it.title} | ${it.detail} | ${it.price}" }
    }

    fun beginEditing() {
        editingBaseProfileImageUrl = page.profileImageURL.orEmpty()
        editingBaseHeroImageUrl = page.heroImageURL.orEmpty()
        editingBaseHeroVideoUrl = page.heroVideoURL.orEmpty()
        temporaryUploadedAssetUrls.clear()
        resetDraftsFromPage()
        pendingImageTarget = null
        activeImageUploadTarget = null
        isUploadingHeroVideo = false
        isEditing = true
    }

    fun discardEditing() {
        val cleanupUrls = temporaryUploadedAssetUrls.toList()
        temporaryUploadedAssetUrls.clear()
        resetDraftsFromPage()
        pendingImageTarget = null
        activeImageUploadTarget = null
        isUploadingHeroVideo = false
        isEditing = false
        if (cleanupUrls.isNotEmpty()) {
            coroutineScope.launch {
                cleanupUrls.forEach { assetUrl ->
                    runCatching { editableImageAssetRepository.deleteAsset(assetUrl) }
                }
            }
        }
        feedbackMessage = "Aenderungen verworfen."
        feedbackType = ToastType.Info
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pendingImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            activeImageUploadTarget = target
            coroutineScope.launch {
                val previousImageUrl = currentEditableImageUrl(target)
                val result = editableImageAssetRepository.uploadImageAsset(
                    uri = uri,
                    contentResolver = context.contentResolver,
                )
                if (result.isSuccess) {
                    val uploadedImage = result.getOrNull()
                    if (uploadedImage != null) {
                        if (previousImageUrl.isNotBlank() && previousImageUrl != uploadedImage.downloadUrl) {
                            val removedTemporaryImage = temporaryUploadedAssetUrls.remove(previousImageUrl)
                            if (removedTemporaryImage) {
                                editableImageAssetRepository.deleteAsset(previousImageUrl)
                            }
                        }
                        applyEditableImageUrl(target, uploadedImage.downloadUrl)
                        if (!temporaryUploadedAssetUrls.contains(uploadedImage.downloadUrl)) {
                            temporaryUploadedAssetUrls.add(uploadedImage.downloadUrl)
                        }
                    }
                    feedbackMessage = "Bild hochgeladen und uebernommen."
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message ?: "Bild konnte nicht hochgeladen werden."
                    feedbackType = ToastType.Error
                }
                activeImageUploadTarget = null
                pendingImageTarget = null
            }
        } else {
            activeImageUploadTarget = null
            pendingImageTarget = null
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            isUploadingHeroVideo = true
            coroutineScope.launch {
                val previousVideoUrl = heroVideoDraft
                val result = editableImageAssetRepository.uploadVideoAsset(
                    uri = uri,
                    context = context,
                )
                if (result.isSuccess) {
                    val uploadedVideo = result.getOrNull()
                    if (uploadedVideo != null) {
                        if (previousVideoUrl.isNotBlank() && previousVideoUrl != uploadedVideo.downloadUrl) {
                            val removedTemporaryVideo = temporaryUploadedAssetUrls.remove(previousVideoUrl)
                            if (removedTemporaryVideo) {
                                editableImageAssetRepository.deleteAsset(previousVideoUrl)
                            }
                        }
                        heroVideoDraft = uploadedVideo.downloadUrl
                        if (!temporaryUploadedAssetUrls.contains(uploadedVideo.downloadUrl)) {
                            temporaryUploadedAssetUrls.add(uploadedVideo.downloadUrl)
                        }
                    }
                    feedbackMessage = "Hero-Video hochgeladen und als Motion-Stage gesetzt."
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message ?: "Hero-Video konnte nicht hochgeladen werden."
                    feedbackType = ToastType.Error
                }
                isUploadingHeroVideo = false
            }
        } else {
            isUploadingHeroVideo = false
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

    DisposableEffect(heroVideoPlayer) {
        onDispose {
            heroVideoPlayer.release()
        }
    }

    LaunchedEffect(page.slug, page.updatedAtEpochMillis, isEditing) {
        if (!isEditing) {
            resetDraftsFromPage()
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

    LaunchedEffect(displayPage.heroVideoURL) {
        val heroVideoUrl = displayPage.heroVideoURL
        if (heroVideoUrl.isNullOrBlank()) {
            heroVideoPlayer.stop()
            heroVideoPlayer.clearMediaItems()
        } else {
            heroVideoPlayer.setMediaItem(MediaItem.fromUri(heroVideoUrl))
            heroVideoPlayer.prepare()
            heroVideoPlayer.play()
        }
    }

    LaunchedEffect(feedbackMessage) {
        if (!feedbackMessage.isNullOrBlank()) {
            delay(3000)
            feedbackMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = page.artistName,
                        subtitle = "${brand.displayTitle} Artist Page",
                        accent = visualStyle.accent,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurueck")
                    }
                },
                actions = {
                    if (canEdit) {
                        if (isEditing) {
                            TextButton(
                                onClick = ::discardEditing,
                                enabled = !isSaving && !isUploadingAsset,
                            ) {
                                Text("Abbrechen")
                            }
                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isSaving = true
                                        val updatedPage = displayPage.copy(
                                            updatedAtEpochMillis = System.currentTimeMillis(),
                                            isPlaceholder = false,
                                        )
                                        val savedAssetUrls = setOfNotNull(
                                            updatedPage.profileImageURL?.trimmedOrNull(),
                                            updatedPage.heroImageURL?.trimmedOrNull(),
                                            updatedPage.heroVideoURL?.trimmedOrNull(),
                                        )
                                        val cleanupUrls = buildSet {
                                            editingBaseProfileImageUrl.trimmedOrNull()
                                                ?.takeIf { it !in savedAssetUrls }
                                                ?.let(::add)
                                            editingBaseHeroImageUrl.trimmedOrNull()
                                                ?.takeIf { it !in savedAssetUrls }
                                                ?.let(::add)
                                            editingBaseHeroVideoUrl.trimmedOrNull()
                                                ?.takeIf { it !in savedAssetUrls }
                                                ?.let(::add)
                                            temporaryUploadedAssetUrls
                                                .filter { it !in savedAssetUrls }
                                                .forEach(::add)
                                        }
                                        val result = ArtistPagesStore.save(updatedPage)
                                        isSaving = false
                                        if (result.isSuccess) {
                                            cleanupUrls.forEach { assetUrl ->
                                                runCatching { editableImageAssetRepository.deleteAsset(assetUrl) }
                                            }
                                            temporaryUploadedAssetUrls.clear()
                                            editingBaseProfileImageUrl = updatedPage.profileImageURL.orEmpty()
                                            editingBaseHeroImageUrl = updatedPage.heroImageURL.orEmpty()
                                            editingBaseHeroVideoUrl = updatedPage.heroVideoURL.orEmpty()
                                            pendingImageTarget = null
                                            activeImageUploadTarget = null
                                            isUploadingHeroVideo = false
                                            isEditing = false
                                            feedbackMessage = "Artist-Seite gespeichert."
                                            feedbackType = ToastType.Success
                                        } else {
                                            feedbackMessage = result.exceptionOrNull()?.message
                                                ?: "Artist-Seite konnte nicht gespeichert werden."
                                            feedbackType = ToastType.Error
                                        }
                                    }
                                },
                                enabled = !isSaving && !isUploadingAsset,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text(if (isSaving) "Speichert..." else "Speichern")
                            }
                        } else {
                            Button(
                                onClick = ::beginEditing,
                                enabled = !isSaving,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            ) {
                                Text("Bearbeiten")
                            }
                        }
                    }
                },
                colors = skydownTopBarColors(),
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = visualStyle.accent,
                        secondaryColor = visualStyle.secondaryAccent,
                    ),
                )
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                item {
                    ArtistPageHeroCard(
                        page = displayPage,
                        brand = brand,
                        trackCount = tracks.size,
                        latestReleaseText = latestReleaseText,
                        onOpenYouTube = { item -> selectedYouTubeItem = item },
                        visualStyle = visualStyle,
                        compactVisualDensity = compactVisualDensity,
                        heroVideoPlayer = heroVideoPlayer,
                        onSurfaceClick = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(1)
                            }
                        },
                    )
                }
                item {
                    ArtistPageSpotlightCard(
                        page = displayPage,
                        trackCount = tracks.size,
                        latestReleaseText = latestReleaseText,
                        spotlightTrack = spotlightTrack,
                        linkCount = listOf(displayPage.instagramURL, displayPage.spotifyURL, displayPage.youtubeURL).count { !it.isNullOrBlank() },
                        visualStyle = visualStyle,
                    )
                }
                item {
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
                }
                item {
                    ArtistPageLinksCard(
                        page = displayPage,
                        onOpenYouTube = { item -> selectedYouTubeItem = item },
                        visualStyle = visualStyle,
                    )
                }

                if (canEdit && isEditing) {
                    item {
                        SkydownCard {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                BrandSectionBanner(
                                    title = "Artist Page bearbeiten",
                                    subtitle = "Bilder und Links werden erst nach `Speichern` live uebernommen.",
                                    accent = visualStyle.secondaryAccent,
                                    icon = Icons.Default.AutoAwesome,
                                    tag = "ADMIN",
                                )

                                ArtistPageInput(title = "Kurzzeile", value = taglineDraft, onValueChange = { taglineDraft = it })
                                ArtistPageInput(title = "Bio", value = bioDraft, onValueChange = { bioDraft = it }, singleLine = false)
                                EditableImageFieldCard(
                                    title = "Profilbild",
                                    imageUrl = profileImageDraft,
                                    isUploading = activeImageUploadTarget == ArtistPageImageTarget.Profile,
                                    enabled = !isSaving && !isUploadingAsset,
                                    uploadStatusText = "Profilbild wird fuer die Artist-Seite uebernommen.",
                                    onPickImage = {
                                        pendingImageTarget = ArtistPageImageTarget.Profile
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onImageUrlChange = { profileImageDraft = it },
                                    onRemoveImage = {
                                        val previousImageUrl = profileImageDraft
                                        profileImageDraft = ""
                                        if (temporaryUploadedAssetUrls.remove(previousImageUrl)) {
                                            coroutineScope.launch {
                                                editableImageAssetRepository.deleteAsset(previousImageUrl)
                                            }
                                        }
                                        feedbackMessage = "Bild entfernt. Live wird es erst nach dem Speichern uebernommen."
                                        feedbackType = ToastType.Info
                                    },
                                )
                                EditableImageFieldCard(
                                    title = "Hero-Bild",
                                    imageUrl = heroImageDraft,
                                    isUploading = activeImageUploadTarget == ArtistPageImageTarget.Hero,
                                    enabled = !isSaving && !isUploadingAsset,
                                    uploadStatusText = "Hero-Bild wird fuer die Artist-Seite uebernommen.",
                                    onPickImage = {
                                        pendingImageTarget = ArtistPageImageTarget.Hero
                                        imagePicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                        )
                                    },
                                    onImageUrlChange = { heroImageDraft = it },
                                    onRemoveImage = {
                                        val previousImageUrl = heroImageDraft
                                        heroImageDraft = ""
                                        if (temporaryUploadedAssetUrls.remove(previousImageUrl)) {
                                            coroutineScope.launch {
                                                editableImageAssetRepository.deleteAsset(previousImageUrl)
                                            }
                                        }
                                        feedbackMessage = "Bild entfernt. Live wird es erst nach dem Speichern uebernommen."
                                        feedbackType = ToastType.Info
                                    },
                                )
                                EditableVideoFieldCard(
                                    title = "Hero-Video",
                                    videoUrl = heroVideoDraft,
                                    isUploading = isUploadingHeroVideo,
                                    enabled = !isSaving && !isUploadingAsset,
                                    uploadStatusText = "Hero-Video wird als Motion-Stage vorbereitet.",
                                    onPickVideo = {
                                        videoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                                        )
                                    },
                                    onRemoveVideo = {
                                        val previousVideoUrl = heroVideoDraft
                                        heroVideoDraft = ""
                                        if (temporaryUploadedAssetUrls.remove(previousVideoUrl)) {
                                            coroutineScope.launch {
                                                editableImageAssetRepository.deleteAsset(previousVideoUrl)
                                            }
                                        }
                                        feedbackMessage = "Hero-Video entfernt. Live wird es erst nach dem Speichern uebernommen."
                                        feedbackType = ToastType.Info
                                    },
                                )
                                ArtistPageInput(title = "Instagram", value = instagramDraft, onValueChange = { instagramDraft = it })
                                ArtistPageInput(title = "Spotify", value = spotifyDraft, onValueChange = { spotifyDraft = it })
                                ArtistPageInput(title = "YouTube", value = youtubeDraft, onValueChange = { youtubeDraft = it })
                                if (allowsStudioPriceEditing) {
                                    ArtistPageInput(
                                        title = "Studio-Preisliste (Titel | Detail | Preis)",
                                        value = studioPriceListDraft,
                                        onValueChange = { studioPriceListDraft = it },
                                        singleLine = false,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ToastHost(
                message = feedbackMessage,
                type = feedbackType,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp),
            )
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
    visualStyle: ArtistPageVisualStyle,
    compactVisualDensity: Boolean,
    heroVideoPlayer: ExoPlayer,
    onSurfaceClick: (() -> Unit)? = null,
) {
    val linkCount = listOf(page.instagramURL, page.spotifyURL, page.youtubeURL).count { !it.isNullOrBlank() }
    BrandHeroCard(
        eyebrow = visualStyle.eyebrow,
        title = page.artistName,
        subtitle = page.tagline ?: "${brand.displayTitle} Artist-Profil mit klarer Stage, Sound und Links.",
        detail = page.bio ?: "Noch keine Artist-Story hinterlegt. Mit Bild, Bio und Links wird aus dem Profil eine echte Artist-Stage.",
        backgroundImageUrl = page.heroImageURL?.takeIf { it.isNotBlank() },
        accent = visualStyle.accent,
        secondaryAccent = visualStyle.secondaryAccent,
        marks = visualStyle.marks,
        compactVisualDensity = compactVisualDensity,
        onSurfaceClick = onSurfaceClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandStatusChip(
                    text = if (page.hasCustomPresentation) "Stage live" else "Stage draft",
                    accent = visualStyle.accent,
                    icon = Icons.Default.AutoAwesome,
                    isActive = page.hasCustomPresentation,
                )
                BrandStatusChip(
                    text = artistTrackCountLabel(trackCount),
                    accent = SpotifyGreen,
                    icon = Icons.Default.GraphicEq,
                    isActive = trackCount > 0,
                )
                latestReleaseText?.let {
                    BrandStatusChip(
                        text = it,
                        accent = visualStyle.secondaryAccent,
                        icon = Icons.Default.PlayCircleFilled,
                    )
                }
                if (linkCount > 0) {
                    BrandStatusChip(
                        text = artistLinkCountLabel(linkCount),
                        accent = MaterialTheme.colorScheme.secondary,
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                    )
                }
            }

            ArtistPageHeroMotionStage(
                page = page,
                visualStyle = visualStyle,
                heroVideoPlayer = heroVideoPlayer,
            )

            ArtistPagePresenceGrid(
                page = page,
                trackCount = trackCount,
                linkCount = linkCount,
                visualStyle = visualStyle,
            )

            ArtistPageHeroQuickLinks(
                page = page,
                onOpenYouTube = onOpenYouTube,
            )
        }
    }
}

@Composable
private fun ArtistPageLinksCard(
    page: ArtistPageUi,
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
    visualStyle: ArtistPageVisualStyle,
) {
    val context = LocalContext.current
    val links = rememberArtistLinks(page)

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandSectionBanner(
                title = "Connect",
                subtitle = "Direkt raus aus der App auf die aktiven Plattformen des Artists.",
                accent = visualStyle.secondaryAccent,
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                tag = if (links.isEmpty()) "OFFLINE" else "${links.size} LIVE",
            )

            if (links.isEmpty()) {
                ArtistPageSupportMessage(
                    message = "Noch keine Links hinterlegt. Sobald Instagram, Spotify oder YouTube gesetzt sind, entsteht hier die direkte Artist-Tuer nach draussen.",
                    accent = visualStyle.secondaryAccent,
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

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        links.forEach { link ->
            BrandActionButton(
                text = link.title,
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
                accent = link.accentColor,
                modifier = Modifier.width(156.dp),
                icon = link.icon,
                compact = true,
                filled = link.kind != ArtistPageLinkKind.YouTube,
            )
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
    visualStyle: ArtistPageVisualStyle,
) {
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandSectionBanner(
                title = "Spotlight",
                subtitle = if (spotlightTrack != null) {
                    "Der schnellste Einstieg in Sound, Haltung und aktuellen Vibe."
                } else {
                    "Profil und Richtung stehen, der musikalische Fokus folgt."
                },
                accent = visualStyle.accent,
                icon = Icons.Default.AutoAwesome,
                tag = if (spotlightTrack != null) "LIVE" else "PROFILE",
            )
            Text(
                text = page.tagline ?: "${page.artistName} entdecken.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ArtistPill(
                    text = page.brand.displayTitle,
                    accent = visualStyle.accent,
                )
                ArtistPill(
                    text = artistTrackCountLabel(trackCount),
                    accent = SpotifyGreen,
                    isActive = trackCount > 0,
                )
                latestReleaseText?.let {
                    ArtistPill(
                        text = it,
                        accent = visualStyle.secondaryAccent,
                    )
                }
                if (linkCount > 0) {
                    ArtistPill(
                        text = artistLinkCountLabel(linkCount),
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            spotlightTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.74f))
                        .padding(horizontal = 14.dp, vertical = 14.dp),
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
                            text = "Jetzt entdecken",
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
                            text = "Direkt unten mit Vorschau oder Spotify weiterhoeren.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                }
            } ?: ArtistPageSupportMessage(
                message = page.bio ?: "Noch kein Fokus-Track hinterlegt. Sobald erste Songs oder Links live sind, bekommt dieser Artist hier seinen staerksten Einstiegspunkt.",
                accent = visualStyle.secondaryAccent,
            )
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
            BrandSectionBanner(
                title = "Top Songs",
                subtitle = when {
                    isLoading -> "Die Song-Liste wird gerade aus dem Feed geladen."
                    !errorMessage.isNullOrBlank() -> "Der Feed braucht gerade einen zweiten Versuch."
                    tracks.isEmpty() -> "Sobald Songs im Feed sind, tauchen sie hier direkt auf."
                    else -> "Preview, Auswahl und Spotify greifen direkt aus der Liste."
                },
                accent = SpotifyGreen,
                icon = Icons.Default.GraphicEq,
                tag = when {
                    isLoading -> "SYNC"
                    !errorMessage.isNullOrBlank() -> "CHECK"
                    tracks.isEmpty() -> "EMPTY"
                    else -> "${tracks.size} LIVE"
                },
            )

            when {
                isLoading -> {
                    ArtistPageSupportMessage(
                        message = "Songs werden geladen ...",
                        accent = SpotifyGreen,
                    )
                }

                !errorMessage.isNullOrBlank() -> {
                    ArtistPageSupportMessage(
                        message = errorMessage.orEmpty(),
                        accent = MaterialTheme.colorScheme.error,
                    )
                }

                tracks.isEmpty() -> {
                    ArtistPageSupportMessage(
                        message = "Fuer $artistName sind gerade noch keine Songs hinterlegt.",
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }

                else -> {
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
private fun ArtistPill(
    text: String,
    accent: Color,
    isActive: Boolean = true,
) {
    Text(
        text = text,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isActive) {
                    accent.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        color = if (isActive) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun ArtistPageHeroMotionStage(
    page: ArtistPageUi,
    visualStyle: ArtistPageVisualStyle,
    heroVideoPlayer: ExoPlayer,
) {
    val hasHeroVideo = !page.heroVideoURL.isNullOrBlank()
    val hasHeroImage = !page.heroImageURL.isNullOrBlank()
    val titleShadow = Shadow(
        color = Color.Black.copy(alpha = 0.32f),
        offset = Offset(0f, 8f),
        blurRadius = 22f,
    )
    val bodyShadow = Shadow(
        color = Color.Black.copy(alpha = 0.24f),
        offset = Offset(0f, 4f),
        blurRadius = 16f,
    )

    BrandPreviewFrame(
        accent = if (hasHeroVideo) visualStyle.secondaryAccent else visualStyle.accent,
        modifier = Modifier
            .fillMaxWidth()
            .height(212.dp),
    ) {
        when {
            hasHeroVideo -> {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { playerContext ->
                        PlayerView(playerContext).apply {
                            useController = false
                            player = heroVideoPlayer
                        }
                    },
                    update = { view ->
                        view.player = heroVideoPlayer
                    },
                )
            }

            hasHeroImage -> {
                AsyncImage(
                    model = page.heroImageURL,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.32f),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircleFilled,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(42.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.10f),
                            Color.Black.copy(alpha = 0.16f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (hasHeroVideo) "Motion Stage" else if (hasHeroImage) "Hero Frame" else "Motion Space",
                style = MaterialTheme.typography.labelLarge.copy(shadow = bodyShadow),
                color = Color.White.copy(alpha = 0.90f),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = when {
                    hasHeroVideo -> "${page.artistName} bleibt direkt in Bewegung und gibt der Seite mehr Erinnerung."
                    hasHeroImage -> "Ein Hero-Video kann diesen Frame noch cineastischer und lebendiger machen."
                    else -> "Mit einem kurzen Hero-Video bekommt der Artist sofort eine viel staerkere Buehnenwirkung."
                },
                style = MaterialTheme.typography.titleMedium.copy(shadow = titleShadow),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ArtistPill(
                text = if (hasHeroVideo) "Autoplay muted" else "Motion ready",
                accent = Color.White,
            )
            if (hasHeroVideo) {
                ArtistPill(
                    text = "Hero video",
                    accent = visualStyle.secondaryAccent,
                )
            }
        }
    }
}

@Composable
private fun ArtistPagePresenceGrid(
    page: ArtistPageUi,
    trackCount: Int,
    linkCount: Int,
    visualStyle: ArtistPageVisualStyle,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 448.dp

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ArtistPagePresenceSummary(
                page = page,
                trackCount = trackCount,
                linkCount = linkCount,
                visualStyle = visualStyle,
            )

            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BrandHeroMetricCard(
                        label = "Status",
                        value = if (page.hasCustomPresentation) "Live" else "Draft",
                        accent = visualStyle.accent,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.AutoAwesome,
                        isActive = page.hasCustomPresentation,
                    )
                    BrandHeroMetricCard(
                        label = "Catalog",
                        value = artistTrackCountLabel(trackCount),
                        accent = SpotifyGreen,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.GraphicEq,
                        isActive = trackCount > 0,
                    )
                    BrandHeroMetricCard(
                        label = "Reach",
                        value = if (linkCount > 0) artistLinkCountLabel(linkCount) else "No links",
                        accent = visualStyle.secondaryAccent,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        isActive = linkCount > 0,
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BrandHeroMetricCard(
                        label = "Status",
                        value = if (page.hasCustomPresentation) "Live" else "Draft",
                        accent = visualStyle.accent,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AutoAwesome,
                        isActive = page.hasCustomPresentation,
                    )
                    BrandHeroMetricCard(
                        label = "Catalog",
                        value = artistTrackCountLabel(trackCount),
                        accent = SpotifyGreen,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.GraphicEq,
                        isActive = trackCount > 0,
                    )
                    BrandHeroMetricCard(
                        label = "Reach",
                        value = if (linkCount > 0) artistLinkCountLabel(linkCount) else "No links",
                        accent = visualStyle.secondaryAccent,
                        modifier = Modifier.weight(1f),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        isActive = linkCount > 0,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtistPagePresenceSummary(
    page: ArtistPageUi,
    trackCount: Int,
    linkCount: Int,
    visualStyle: ArtistPageVisualStyle,
) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    val hasBackgroundMedia = !page.heroImageURL.isNullOrBlank() || !page.heroVideoURL.isNullOrBlank()
    val summaryText = when {
        !page.bio.isNullOrBlank() -> page.bio.orEmpty()
        trackCount > 0 && linkCount > 0 ->
            "Songs, Plattformen und Artist-Identity greifen hier in einer gemeinsamen Stage zusammen."
        trackCount > 0 ->
            "Der Katalog steht bereit und kann direkt ueber Songs und Preview erlebt werden."
        else ->
            "Mit Bild, Bio und Links bekommt dieser Artist eine klare, markentaugliche Startflaeche."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtistPageProfileMedallion(
            page = page,
            accent = visualStyle.accent,
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (page.editorUids.isNotEmpty()) {
                    "Artist Presence • ${page.editorUids.size} Editoren"
                } else {
                    "Artist Presence"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (hasBackgroundMedia) {
                    Color.White.copy(alpha = 0.94f)
                } else {
                    visualStyle.accent
                },
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodyMedium,
                color = if (hasBackgroundMedia) {
                    Color.White.copy(alpha = 0.84f)
                } else {
                    onSurface.copy(alpha = 0.76f)
                },
            )
        }
    }
}

@Composable
private fun ArtistPageProfileMedallion(
    page: ArtistPageUi,
    accent: Color,
    size: androidx.compose.ui.unit.Dp = 72.dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f)),
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
                color = accent,
            )
        }
    }
}

@Composable
private fun ArtistPageSupportMessage(
    message: String,
    accent: Color,
) {
    Text(
        text = message,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
    )
}

private data class ArtistPageVisualStyle(
    val eyebrow: String,
    val accent: Color,
    val secondaryAccent: Color,
    val marks: List<BrandArtwork>,
)

@Composable
private fun artistPageVisualStyle(brand: ArtistPageBrand): ArtistPageVisualStyle {
    val colorScheme = MaterialTheme.colorScheme
    return when (brand) {
        ArtistPageBrand.Zweizwei -> ArtistPageVisualStyle(
            eyebrow = "22 ARTIST",
            accent = SpotifyGreen,
            secondaryAccent = colorScheme.primary,
            marks = listOf(BrandArtwork.Zweizwei),
        )
        ArtistPageBrand.Skydown -> ArtistPageVisualStyle(
            eyebrow = "SKYDOWN ARTIST",
            accent = colorScheme.primary,
            secondaryAccent = colorScheme.tertiary,
            marks = listOf(BrandArtwork.Skydown),
        )
        ArtistPageBrand.Nicma -> ArtistPageVisualStyle(
            eyebrow = "NICMA PRODUCER",
            accent = colorScheme.tertiary,
            secondaryAccent = colorScheme.primary,
            marks = emptyList(),
        )
    }
}

private fun artistTrackCountLabel(trackCount: Int): String = when {
    trackCount <= 0 -> "No songs"
    trackCount == 1 -> "1 Song"
    else -> "$trackCount Songs"
}

private fun artistLinkCountLabel(linkCount: Int): String = when {
    linkCount <= 0 -> "No links"
    linkCount == 1 -> "1 Link"
    else -> "$linkCount Links"
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

private fun parseStudioPriceItems(rawValue: String): List<StudioPriceItemUi> {
    return rawValue
        .lines()
        .mapNotNull { line ->
            val parts = line.split("|").map { it.trim() }
            if (parts.size < 3) return@mapNotNull null
            val title = parts[0]
            val detail = parts[1]
            val price = parts.drop(2).joinToString(" | ").trim()
            if (title.isBlank() || detail.isBlank() || price.isBlank()) return@mapNotNull null
            StudioPriceItemUi(title = title, detail = detail, price = price)
        }
}
