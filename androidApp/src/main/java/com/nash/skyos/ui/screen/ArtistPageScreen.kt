package com.nash.skyos.ui.screen
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.ArtistPageBrand
import com.nash.skyos.data.ArtistPageUi
import com.nash.skyos.data.ArtistPagesStore
import com.nash.skyos.data.StudioPriceItemUi
import com.nash.skyos.data.mediaAttributionContext
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.EditableImageFieldCard
import com.nash.skyos.ui.component.EditableVideoFieldCard
import com.nash.skyos.ui.component.LocalSessionUser
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.TrackRow
import com.nash.skyos.ui.component.TrackRowPresentation
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

private sealed class StudioPriceEditRequest {
    data object New : StudioPriceEditRequest()
    data class AtIndex(val index: Int) : StudioPriceEditRequest()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistPageScreen(
    artistName: String,
    brand: ArtistPageBrand,
    onBack: () -> Unit,
    onNicmaProfileChange: ((String) -> Unit)? = null,
) {
    val currentUser = LocalSessionUser.current
    val pages by ArtistPagesStore.pages.collectAsState()
    val context = LocalContext.current
    val resources = LocalResources.current
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
    val routeArtistName = artistName.trim()
    val page = remember(pages, routeArtistName, brand) {
        ArtistPagesStore.pageFor(brand = brand, artistName = routeArtistName)
    }
    val canEdit = ArtistPagesStore.canEdit(page, currentUser)
    // Navigation-Intent, nicht unbedingt ein evtl. falsch gespeichertes `page.artistName` aus Firestore.
    val allowsStudioPriceEditing = brand == ArtistPageBrand.Nicma &&
        routeArtistName.equals("NICMA STUDIO", ignoreCase = true)
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    val listState = rememberLazyListState()
    val visualStyle = artistPageVisualStyle(brand = brand, artistName = routeArtistName)
    // Studio: kompakter Hub (Preise, Links). Music: vollwertige Artist Page (Katalog, Songs) — eigenes Firestore-Dokument `nicma-nicma-music`.
    val isNicmaStudioPage = brand == ArtistPageBrand.Nicma && isNicmaStudioProfile(routeArtistName)
    val showNicmaProfileSwitch = brand == ArtistPageBrand.Nicma && onNicmaProfileChange != null
    val postHeroScrollItemIndex = if (showNicmaProfileSwitch) 2 else 1

    var isEditing by rememberSaveable(page.slug) { mutableStateOf(false) }
    var taglineDraft by rememberSaveable(page.slug) { mutableStateOf(page.tagline.orEmpty()) }
    var bioDraft by rememberSaveable(page.slug) { mutableStateOf(page.bio.orEmpty()) }
    var profileImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.profileImageURL.orEmpty()) }
    var heroImageDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroImageURL.orEmpty()) }
    var heroVideoDraft by rememberSaveable(page.slug) { mutableStateOf(page.heroVideoURL.orEmpty()) }
    var instagramDraft by rememberSaveable(page.slug) { mutableStateOf(page.instagramURL.orEmpty()) }
    var spotifyDraft by rememberSaveable(page.slug) { mutableStateOf(page.spotifyURL.orEmpty()) }
    var youtubeDraft by rememberSaveable(page.slug) { mutableStateOf(page.youtubeURL.orEmpty()) }
    val studioPriceItems = remember(page.slug) {
        mutableStateListOf<StudioPriceItemUi>().apply { addAll(page.studioPriceList) }
    }
    var studioPriceEdit by remember { mutableStateOf<StudioPriceEditRequest?>(null) }
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

    val displayPage: ArtistPageUi = if (!isEditing) {
        page
    } else {
        page.copy(
            slug = ArtistPagesStore.documentIdFor(brand, routeArtistName),
            artistName = routeArtistName,
            tagline = taglineDraft.trimmedOrNull(),
            bio = bioDraft.trimmedOrNull(),
            profileImageURL = profileImageDraft.trimmedOrNull(),
            heroImageURL = heroImageDraft.trimmedOrNull(),
            heroVideoURL = heroVideoDraft.trimmedOrNull(),
            instagramURL = instagramDraft.trimmedOrNull(),
            spotifyURL = spotifyDraft.trimmedOrNull(),
            youtubeURL = youtubeDraft.trimmedOrNull(),
            studioPriceList = if (allowsStudioPriceEditing) studioPriceItems.toList() else page.studioPriceList,
            isPlaceholder = false,
        )
    }

    val nicmaStudioPage = remember(pages) {
        ArtistPagesStore.pageFor(ArtistPageBrand.Nicma, "NICMA STUDIO")
    }
    val pageForConnect: ArtistPageUi = remember(
        displayPage,
        nicmaStudioPage,
        brand,
        isNicmaStudioPage,
        isEditing,
        routeArtistName,
    ) {
        if (isEditing) {
            displayPage
        } else if (brand == ArtistPageBrand.Nicma && !isNicmaStudioPage) {
            val merged = mergeNicmaMusicConnectFromStudio(displayPage, nicmaStudioPage)
            if (routeArtistName.equals("NICMA MUSIC", ignoreCase = true)) {
                applyNicmaMusicPublicLinkDefaults(merged)
            } else {
                merged
            }
        } else {
            displayPage
        }
    }

    val spotlightTrack = remember(tracks, selectedTrackId) {
        tracks.firstOrNull { it.trackId == selectedTrackId } ?: tracks.firstOrNull()
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
        studioPriceItems.clear()
        studioPriceItems.addAll(page.studioPriceList)
    }

    fun beginEditing() {
        editingBaseProfileImageUrl = page.profileImageURL.orEmpty()
        editingBaseHeroImageUrl = page.heroImageURL.orEmpty()
        editingBaseHeroVideoUrl = page.heroVideoURL.orEmpty()
        temporaryUploadedAssetUrls.clear()
        resetDraftsFromPage()
        if (allowsStudioPriceEditing && page.studioPriceList.isEmpty() && studioPriceItems.isEmpty()) {
            studioPriceItems.addAll(nicmaDefaultStudioPriceItems())
        }
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
        feedbackMessage = resources.getString(R.string.artist_feedback_changes_discarded)
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
                    feedbackMessage = resources.getString(R.string.artist_feedback_image_uploaded)
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message
                        ?: resources.getString(R.string.artist_feedback_image_upload_failed)
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
                    feedbackMessage = resources.getString(R.string.artist_feedback_hero_video_uploaded)
                    feedbackType = ToastType.Success
                } else {
                    feedbackMessage = result.exceptionOrNull()?.message
                        ?: resources.getString(R.string.artist_feedback_hero_video_upload_failed)
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

    LaunchedEffect(brand, routeArtistName, isNicmaStudioPage) {
        if (isNicmaStudioPage) {
            tracks = emptyList()
            isLoadingTracks = false
            tracksError = null
            selectedTrackId = null
            return@LaunchedEffect
        }
        isLoadingTracks = true
        tracksError = null

        AppContainer.musicService.fetchTracks(routeArtistName)
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
                        title = routeArtistName,
                        subtitle = if (isNicmaStudioPage) {
                            nicmaTopBarSubtitle(routeArtistName, page.tagline)
                        } else {
                            if (brand == ArtistPageBrand.Nicma) {
                                page.tagline?.takeIf { it.isNotBlank() }
                                    ?: stringResource(R.string.artist_topbar_subtitle_songs_links)
                            } else {
                                stringResource(R.string.artist_topbar_subtitle_brand_page, brand.displayTitle)
                            }
                        },
                        accent = if (isNicmaStudioPage) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            visualStyle.accent
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                actions = {
                    if (canEdit) {
                        if (isEditing) {
                            TextButton(
                                onClick = ::discardEditing,
                                enabled = !isSaving && !isUploadingAsset,
                            ) {
                                Text(stringResource(R.string.common_cancel))
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
                                            feedbackMessage = resources.getString(R.string.artist_feedback_page_saved)
                                            feedbackType = ToastType.Success
                                        } else {
                                            feedbackMessage = result.exceptionOrNull()?.message
                                                ?: resources.getString(R.string.artist_feedback_page_save_failed)
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
                                Text(
                                    if (isSaving) {
                                        stringResource(R.string.artist_action_saving)
                                    } else {
                                        stringResource(R.string.common_save)
                                    }
                                )
                            }
                        } else {
                            IconButton(
                                onClick = ::beginEditing,
                                enabled = !isSaving,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Edit,
                                    contentDescription = stringResource(R.string.artist_action_edit),
                                )
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
                    if (isNicmaStudioPage) {
                        skydownScreenBrush(
                            primaryColor = MaterialTheme.colorScheme.tertiary,
                            secondaryColor = MaterialTheme.colorScheme.primary,
                            primaryAlpha = 0.08f,
                            secondaryAlpha = 0.06f,
                        )
                    } else {
                        skydownScreenBrush(
                            primaryColor = visualStyle.accent,
                            secondaryColor = visualStyle.secondaryAccent,
                        )
                    },
                )
        ) {
            val nicmaHubSpacing = 16.dp
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 0.dp),
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(
                    if (isNicmaStudioPage) nicmaHubSpacing else sectionSpacing,
                ),
            ) {
                if (isNicmaStudioPage) {
                    onNicmaProfileChange?.let { switch ->
                        item {
                            NicmaProfileSelectorRow(
                                selectedProfile = routeArtistName,
                                onSelectProfile = switch,
                            )
                        }
                    }
                    item {
                        NicmaHubHeroCard(
                            isStudioProfile = true,
                            headline = displayPage.artistName,
                            body = displayPage.bio ?: nicmaProfileFallbackBio(routeArtistName),
                            tagline = nicmaHubTagline(routeArtistName, displayPage.tagline),
                            heroImageUrl = displayPage.heroImageURL,
                        )
                    }
                    item {
                        if (canEdit && allowsStudioPriceEditing && isEditing) {
                            NicmaPriceListEditorCard(
                                items = studioPriceItems.toList(),
                                enabled = !isSaving,
                                onAdd = { studioPriceEdit = StudioPriceEditRequest.New },
                                onEdit = { index ->
                                    studioPriceEdit = StudioPriceEditRequest.AtIndex(index)
                                },
                                onDelete = { index ->
                                    if (index in studioPriceItems.indices) {
                                        studioPriceItems.removeAt(index)
                                    }
                                },
                            )
                        } else {
                            NicmaPriceListCard(
                                priceList = resolvedNicmaProducerPackages(displayPage.studioPriceList),
                            )
                        }
                    }
                    val showStudioLinks = listOf(
                        displayPage.instagramURL?.trim().orEmpty().ifBlank { nicmaDefaultInstagramUrl },
                        displayPage.spotifyURL?.trim().orEmpty(),
                        displayPage.youtubeURL?.trim().orEmpty(),
                    ).any { it.isNotBlank() }
                    if (showStudioLinks) {
                        item {
                            Column(modifier = Modifier.padding(top = 4.dp)) {
                                Text(
                                    text = stringResource(R.string.artist_contact_title),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                )
                                NicmaStudioInlineLinkRow(
                                    instagramUrl = displayPage.instagramURL?.trim().orEmpty()
                                        .ifBlank { nicmaDefaultInstagramUrl },
                                    spotifyUrl = displayPage.spotifyURL,
                                    youtubeUrl = displayPage.youtubeURL,
                                    onOpenLink = { url -> openExternalLink(context, url) },
                                )
                            }
                        }
                    }
                } else {
                    if (showNicmaProfileSwitch) {
                        val selectProfile = requireNotNull(onNicmaProfileChange)
                        item {
                            NicmaProfileSelectorRow(
                                selectedProfile = routeArtistName,
                                onSelectProfile = selectProfile,
                            )
                        }
                    }
                    item {
                        ArtistPageHeroCard(
                            page = pageForConnect,
                            brand = brand,
                            trackCount = tracks.size,
                            visualStyle = visualStyle,
                            compactVisualDensity = compactVisualDensity,
                            heroVideoPlayer = heroVideoPlayer,
                            onSurfaceClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(postHeroScrollItemIndex)
                                }
                            },
                        )
                    }
                    item {
                        ArtistPageSpotlightCard(
                            page = displayPage,
                            spotlightTrack = spotlightTrack,
                            visualStyle = visualStyle,
                            onSelectTrack = spotlight@ { id ->
                                val track = tracks.find { t -> t.trackId == id } ?: return@spotlight
                                if (currentlyPlayingId == track.trackId) {
                                    currentlyPlayingId = null
                                    currentPreviewUrl = null
                                } else {
                                    selectedTrackId = track.trackId
                                    if (!track.previewUrl.isNullOrBlank()) {
                                        currentlyPlayingId = track.trackId
                                        currentPreviewUrl = track.previewUrl
                                    } else {
                                        currentlyPlayingId = null
                                        currentPreviewUrl = null
                                    }
                                }
                            },
                        )
                    }
                    item {
                        ArtistPageTracksCard(
                            artistName = routeArtistName,
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
                            page = pageForConnect,
                            onOpenYouTube = { item -> selectedYouTubeItem = item },
                            visualStyle = visualStyle,
                        )
                    }
                }

                if (canEdit && isEditing) {
                    item {
                        SkydownCard {
                            if (isNicmaStudioPage) {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BrandSectionBanner(
                                        title = stringResource(R.string.artist_studio_editor_title),
                                        subtitle = stringResource(R.string.artist_studio_editor_subtitle),
                                        accent = MaterialTheme.colorScheme.tertiary,
                                        icon = Icons.Default.AutoAwesome,
                                        tag = stringResource(R.string.artist_studio_tag),
                                    )
                                    ArtistPageInput(
                                        title = stringResource(R.string.artist_field_tagline),
                                        value = taglineDraft,
                                        onValueChange = { taglineDraft = it },
                                    )
                                    ArtistPageInput(
                                        title = stringResource(R.string.artist_field_bio_long),
                                        value = bioDraft,
                                        onValueChange = { bioDraft = it },
                                        singleLine = false,
                                    )
                                    EditableImageFieldCard(
                                        title = stringResource(R.string.artist_field_hero_image_tile),
                                        imageUrl = heroImageDraft,
                                        isUploading = activeImageUploadTarget == ArtistPageImageTarget.Hero,
                                        enabled = !isSaving && !isUploadingAsset,
                                        uploadStatusText = stringResource(R.string.artist_upload_status_studio_tile),
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
                                            feedbackMessage = resources.getString(R.string.artist_feedback_image_removed_pending_save)
                                            feedbackType = ToastType.Info
                                        },
                                    )
                                    ArtistPageInput(title = stringResource(R.string.social_instagram), value = instagramDraft, onValueChange = { instagramDraft = it })
                                    ArtistPageInput(title = stringResource(R.string.social_spotify), value = spotifyDraft, onValueChange = { spotifyDraft = it })
                                    ArtistPageInput(title = stringResource(R.string.social_youtube), value = youtubeDraft, onValueChange = { youtubeDraft = it })
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    BrandSectionBanner(
                                        title = stringResource(R.string.artist_editor_title),
                                        subtitle = stringResource(R.string.artist_editor_subtitle),
                                        accent = visualStyle.secondaryAccent,
                                        icon = Icons.Default.AutoAwesome,
                                        tag = stringResource(R.string.artist_admin_tag),
                                    )
                                    ArtistPageInput(title = stringResource(R.string.artist_field_tagline), value = taglineDraft, onValueChange = { taglineDraft = it })
                                    ArtistPageInput(title = stringResource(R.string.artist_field_bio), value = bioDraft, onValueChange = { bioDraft = it }, singleLine = false)
                                    EditableImageFieldCard(
                                        title = stringResource(R.string.artist_field_profile_image),
                                        imageUrl = profileImageDraft,
                                        isUploading = activeImageUploadTarget == ArtistPageImageTarget.Profile,
                                        enabled = !isSaving && !isUploadingAsset,
                                        uploadStatusText = stringResource(R.string.artist_upload_status_profile_image),
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
                                            feedbackMessage = resources.getString(R.string.artist_feedback_image_removed_pending_save)
                                            feedbackType = ToastType.Info
                                        },
                                    )
                                    EditableImageFieldCard(
                                        title = stringResource(R.string.artist_field_hero_image),
                                        imageUrl = heroImageDraft,
                                        isUploading = activeImageUploadTarget == ArtistPageImageTarget.Hero,
                                        enabled = !isSaving && !isUploadingAsset,
                                        uploadStatusText = stringResource(R.string.artist_upload_status_hero_image),
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
                                            feedbackMessage = resources.getString(R.string.artist_feedback_image_removed_pending_save)
                                            feedbackType = ToastType.Info
                                        },
                                    )
                                    EditableVideoFieldCard(
                                        title = stringResource(R.string.artist_field_hero_video),
                                        videoUrl = heroVideoDraft,
                                        isUploading = isUploadingHeroVideo,
                                        enabled = !isSaving && !isUploadingAsset,
                                        uploadStatusText = stringResource(R.string.artist_upload_status_hero_video),
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
                                            feedbackMessage = resources.getString(R.string.artist_feedback_hero_video_removed_pending_save)
                                            feedbackType = ToastType.Info
                                        },
                                    )
                                    ArtistPageInput(title = stringResource(R.string.social_instagram), value = instagramDraft, onValueChange = { instagramDraft = it })
                                    ArtistPageInput(title = stringResource(R.string.social_spotify), value = spotifyDraft, onValueChange = { spotifyDraft = it })
                                    ArtistPageInput(title = stringResource(R.string.social_youtube), value = youtubeDraft, onValueChange = { youtubeDraft = it })
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

    when (val req = studioPriceEdit) {
        is StudioPriceEditRequest.New -> {
            StudioPriceItemEditorDialog(
                isAdd = true,
                item = StudioPriceItemUi(title = "", detail = "", price = ""),
                onDismiss = { studioPriceEdit = null },
                onConfirm = { new ->
                    studioPriceItems.add(new)
                    studioPriceEdit = null
                },
            )
        }
        is StudioPriceEditRequest.AtIndex -> {
            if (req.index in studioPriceItems.indices) {
                val idx = req.index
                key(
                    idx,
                    studioPriceItems[idx].title,
                    studioPriceItems[idx].detail,
                    studioPriceItems[idx].price,
                ) {
                    StudioPriceItemEditorDialog(
                        isAdd = false,
                        item = studioPriceItems[idx],
                        onDismiss = { studioPriceEdit = null },
                        onConfirm = { new ->
                            if (idx in studioPriceItems.indices) {
                                studioPriceItems[idx] = new
                            }
                            studioPriceEdit = null
                        },
                    )
                }
            } else {
                LaunchedEffect(Unit) {
                    studioPriceEdit = null
                }
            }
        }
        null -> Unit
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
    visualStyle: ArtistPageVisualStyle,
    compactVisualDensity: Boolean,
    heroVideoPlayer: ExoPlayer,
    onSurfaceClick: (() -> Unit)? = null,
) {
    val linkCount = listOf(page.instagramURL, page.spotifyURL, page.youtubeURL).count { !it.isNullOrBlank() }
    BrandHeroCard(
        eyebrow = visualStyle.eyebrow,
        title = page.artistName,
        subtitle = page.tagline ?: "${brand.displayTitle} Profil",
        detail = page.bio ?: "Beschreibung fehlt noch.",
        backgroundImageUrl = page.heroImageURL?.takeIf { it.isNotBlank() },
        accent = visualStyle.accent,
        secondaryAccent = visualStyle.secondaryAccent,
        marks = visualStyle.marks,
        compactVisualDensity = compactVisualDensity,
        onSurfaceClick = onSurfaceClick,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ArtistPageHeroMotionStage(
                page = page,
                heroVideoPlayer = heroVideoPlayer,
            )

            ArtistPagePresenceGrid(
                page = page,
                trackCount = trackCount,
                linkCount = linkCount,
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
    val links = artistConnectLinks(page)

    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandSectionBanner(
                title = stringResource(R.string.artist_connect_title),
                subtitle = stringResource(R.string.artist_connect_subtitle),
                accent = visualStyle.secondaryAccent,
                icon = Icons.AutoMirrored.Filled.OpenInNew,
                tag = if (links.isEmpty()) {
                    stringResource(R.string.artist_connect_tag_offline)
                } else {
                    stringResource(R.string.artist_connect_tag_live_count, links.size)
                },
            )

            if (links.isEmpty()) {
                ArtistPageSupportMessage(
                    message = stringResource(R.string.artist_connect_empty),
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
private fun ArtistPageSpotlightCard(
    page: ArtistPageUi,
    spotlightTrack: Track?,
    visualStyle: ArtistPageVisualStyle,
    onSelectTrack: (Int) -> Unit,
) {
    val haptics = LocalHapticFeedback.current
    SkydownCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrandSectionBanner(
                title = stringResource(R.string.artist_spotlight_title),
                subtitle = if (spotlightTrack != null) {
                    stringResource(R.string.artist_spotlight_subtitle_tap_preview)
                } else {
                    stringResource(R.string.artist_spotlight_subtitle_feed)
                },
                accent = visualStyle.accent,
                icon = Icons.Default.AutoAwesome,
                tag = if (spotlightTrack != null) {
                    stringResource(R.string.artist_spotlight_tag_live)
                } else {
                    stringResource(R.string.artist_spotlight_tag_profile)
                },
            )
            Text(
                text = page.tagline ?: stringResource(R.string.artist_spotlight_fallback_tagline, page.artistName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
            )

            spotlightTrack?.let { track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectTrack(track.trackId)
                        }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    AsyncImage(
                        model = track.artworkUrl100,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = track.trackName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = track.collectionName ?: page.artistName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        )
                    }
                }
            } ?: Text(
                text = page.bio ?: stringResource(R.string.artist_spotlight_no_track),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
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
                title = stringResource(R.string.artist_top_songs_title),
                subtitle = when {
                    isLoading -> stringResource(R.string.artist_top_songs_subtitle_loading)
                    !errorMessage.isNullOrBlank() -> stringResource(R.string.artist_top_songs_subtitle_retry)
                    tracks.isEmpty() -> stringResource(R.string.artist_top_songs_subtitle_feed)
                    else -> stringResource(R.string.artist_top_songs_subtitle_named, artistName)
                },
                accent = SpotifyGreen,
                icon = Icons.Default.GraphicEq,
                tag = when {
                    isLoading -> stringResource(R.string.artist_top_songs_tag_sync)
                    !errorMessage.isNullOrBlank() -> stringResource(R.string.artist_top_songs_tag_check)
                    tracks.isEmpty() -> stringResource(R.string.artist_top_songs_tag_empty)
                    else -> stringResource(R.string.artist_top_songs_tag_live_count, tracks.size)
                },
            )

            when {
                isLoading -> {
                    ArtistPageSupportMessage(
                        message = stringResource(R.string.artist_top_songs_loading),
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
                        message = stringResource(R.string.artist_top_songs_empty),
                        accent = MaterialTheme.colorScheme.secondary,
                    )
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        tracks.forEachIndexed { index, track ->
                            val presentation = when (index) {
                                0 -> TrackRowPresentation.Featured
                                1 -> TrackRowPresentation.Secondary
                                else -> TrackRowPresentation.Catalog
                            }
                            TrackRow(
                                track = track,
                                isPlaying = currentlyPlayingId == track.trackId,
                                isSelected = selectedTrackId == track.trackId,
                                onSelectTrack = { onSelectTrack(track.trackId) },
                                onPlayToggle = { onPlayToggle(track) },
                                presentation = presentation,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtistPageHeroMotionStage(
    page: ArtistPageUi,
    heroVideoPlayer: ExoPlayer,
) {
    val hasHeroVideo = !page.heroVideoURL.isNullOrBlank()
    val hasHeroImage = !page.heroImageURL.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
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
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
        }

        if (hasHeroVideo || hasHeroImage) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.22f),
                            ),
                        ),
                    ),
            )
        }
    }
}

@Composable
private fun ArtistPagePresenceGrid(
    page: ArtistPageUi,
    trackCount: Int,
    linkCount: Int,
) {
    val status = if (page.hasCustomPresentation) "Live" else "Entwurf"
    val catalog = artistTrackCountLabel(trackCount)
    val reach = if (linkCount > 0) {
        artistLinkCountLabel(linkCount)
    } else {
        "keine Links"
    }
    val statsLine = listOf(status, catalog, reach).joinToString(" · ")

    Text(
        text = statsLine,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth(),
    )
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
private fun artistPageVisualStyle(brand: ArtistPageBrand, artistName: String = ""): ArtistPageVisualStyle {
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
        ArtistPageBrand.Nicma -> {
            // Kurz halten: `BrandHeroCard` blendet die Eyebrow aus, sobald `title` mit `eyebrow` beginnt
            // (z. B. wäre "NICMA STUDIO" + Titel "NICMA STUDIO" unsichtbar).
            val nicmaEyebrow = when {
                artistName.equals("NICMA STUDIO", ignoreCase = true) -> "STUDIO"
                artistName.equals("NICMA MUSIC", ignoreCase = true) -> "PRODUCER"
                else -> "NICMA"
            }
            ArtistPageVisualStyle(
                eyebrow = nicmaEyebrow,
                accent = colorScheme.tertiary,
                secondaryAccent = colorScheme.primary,
                marks = emptyList(),
            )
        }
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
private fun artistConnectLinks(page: ArtistPageUi): List<ArtistPageLinkUi> {
    val youtubeTint = MaterialTheme.colorScheme.error
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface
    return buildList {
        page.instagramURL?.trimmedOrNull()?.let {
            add(
                ArtistPageLinkUi(
                    title = stringResource(R.string.social_instagram),
                    subtitle = stringResource(R.string.artist_connect_instagram_subtitle, page.artistName),
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
                    title = stringResource(R.string.social_spotify),
                    subtitle = stringResource(R.string.artist_connect_spotify_subtitle),
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
                    title = stringResource(R.string.social_youtube),
                    subtitle = stringResource(R.string.artist_connect_youtube_subtitle),
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

@Composable
private fun ArtistEditorBadge(count: Int) {
    Text(
        text = if (count == 1) {
            stringResource(R.string.artist_editor_count_one, count)
        } else {
            stringResource(R.string.artist_editor_count_other, count)
        },
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        style = MaterialTheme.typography.bodySmall,
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

/** Wenn Links nur im STUDIO-Doc stehen, Music-Ansicht trotzdem fuellen (gleiches Admin-Set wie STUDIO-Hub). */
private fun mergeNicmaMusicConnectFromStudio(
    music: ArtistPageUi,
    studio: ArtistPageUi,
): ArtistPageUi {
    if (studio.slug == music.slug) return music
    return music.copy(
        instagramURL = music.instagramURL?.trimmedOrNull() ?: studio.instagramURL?.trimmedOrNull(),
        spotifyURL = music.spotifyURL?.trimmedOrNull() ?: studio.spotifyURL?.trimmedOrNull(),
        youtubeURL = music.youtubeURL?.trimmedOrNull() ?: studio.youtubeURL?.trimmedOrNull(),
    )
}

/**
 * Wenn Firestore / STUDIO leer ist: dieselben oeffentlichen Kanaele wie in der alten Nicma-Hub-UI
 * (iOS: `nicmaLinks` + Default-Instagram) — sonst bleibt Connect trotz funktionierendem Katalog leer.
 */
private fun applyNicmaMusicPublicLinkDefaults(page: ArtistPageUi): ArtistPageUi {
    if (page.brand != ArtistPageBrand.Nicma) return page
    return page.copy(
        instagramURL = page.instagramURL?.trimmedOrNull() ?: nicmaDefaultInstagramUrl,
        spotifyURL = page.spotifyURL?.trimmedOrNull() ?: nicmaDefaultSpotifyArtistUrl,
        youtubeURL = page.youtubeURL?.trimmedOrNull(),
    )
}

