package com.nash.skyos.ui.screen

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import com.nash.skyos.ui.component.SkydownMotionTokens
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.resolveYouTubeThumbnailUrl
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.EditableImageFieldCard
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandHeroMetricCard
import com.nash.skyos.ui.component.BrandPreviewFrame
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.ExternalVideoWebPlayer
import com.nash.skyos.ui.component.OriginalVideoViewerDialog
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.dismissKeyboardOnTap
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownScreenBrush
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.ProducedWithArtist
import com.nash.skyos.ui.model.VideoEquipmentItem
import com.nash.skyos.ui.model.SelectedVideoFile
import com.nash.skyos.ui.model.VideoHubItem
import com.nash.skyos.ui.theme.InstagramOrange
import com.nash.skyos.ui.theme.InstagramPink
import com.nash.skyos.ui.theme.InstagramPurple
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.YouTubeRed
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownCinematicShadow
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownYoutube
import com.nash.skyos.ui.viewmodel.VideoHubViewModel
import coil3.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoHubScreen(
    onBack: (() -> Unit)? = null,
    initialSelectedVideoId: String? = null,
    autoplayInitialSelection: Boolean = false,
    onOpenCart: (() -> Unit)? = null,
    onOpenProfile: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onGuestSignIn: (() -> Unit)? = null,
    viewModel: VideoHubViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val editableImageAssetRepository = remember { AppContainer.editableImageAssetRepository }
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val player = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = false
        }
    }
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    var selectedVideoId by rememberSaveable { mutableStateOf(initialSelectedVideoId) }
    var showReelViewer by rememberSaveable { mutableStateOf(false) }
    var showUploadSheet by rememberSaveable { mutableStateOf(false) }
    var showAdminSheet by rememberSaveable { mutableStateOf(false) }
    var selectedEquipmentItem by remember { mutableStateOf<VideoEquipmentItem?>(null) }
    var pendingConfigImageTarget by remember { mutableStateOf<VideoConfigImageTarget?>(null) }
    var activeConfigImageUploadTarget by remember { mutableStateOf<VideoConfigImageTarget?>(null) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var localFeedbackIsError by remember { mutableStateOf(false) }
    var inAppOriginalUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var inAppOriginalTitle by rememberSaveable { mutableStateOf("Original") }
    var inAppImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var shouldAutoplaySelection by rememberSaveable {
        mutableStateOf(autoplayInitialSelection && !initialSelectedVideoId.isNullOrBlank())
    }
    val listState = rememberLazyListState()
    val fallbackSelectedVideo = uiState.videos.firstOrNull { it.supportsInlinePlayback } ?: uiState.videos.firstOrNull()
    val selectedVideo = uiState.videos.firstOrNull { it.id == selectedVideoId } ?: fallbackSelectedVideo
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.setSelectedFiles(context, uris)
        }
    }
    val currentConfigImageUrl: (VideoConfigImageTarget) -> String = { target ->
        when (target) {
            is VideoConfigImageTarget.Equipment ->
                uiState.publicConfig.equipmentItems.firstOrNull { it.id == target.itemId }?.imageUrl.orEmpty()
            is VideoConfigImageTarget.Collaboration ->
                uiState.publicConfig.collaborationItems.firstOrNull { it.id == target.itemId }?.imageUrl.orEmpty()
        }
    }
    val configImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pendingConfigImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            activeConfigImageUploadTarget = target
            coroutineScope.launch {
                val previousImageUrl = currentConfigImageUrl(target)
                val result = editableImageAssetRepository.uploadImageAsset(
                    uri = uri,
                    contentResolver = context.contentResolver,
                )
                if (result.isSuccess) {
                    val uploadedImage = result.getOrNull()
                    if (uploadedImage != null) {
                        when (target) {
                            is VideoConfigImageTarget.Equipment ->
                                viewModel.updateEquipmentItem(target.itemId, imageUrl = uploadedImage.downloadUrl)
                            is VideoConfigImageTarget.Collaboration ->
                                viewModel.updateCollaborationItem(target.itemId, imageUrl = uploadedImage.downloadUrl)
                        }
                        if (previousImageUrl.isNotBlank() && previousImageUrl != uploadedImage.downloadUrl) {
                            editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                        }
                    }
                    localFeedbackMessage = "Bild hochgeladen und uebernommen."
                    localFeedbackIsError = false
                } else {
                    localFeedbackMessage = result.exceptionOrNull()?.message ?: "Bild konnte nicht hochgeladen werden."
                    localFeedbackIsError = true
                }
                activeConfigImageUploadTarget = null
                pendingConfigImageTarget = null
            }
        } else {
            activeConfigImageUploadTarget = null
            pendingConfigImageTarget = null
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(selectedVideo?.nativePlaybackUrl, shouldAutoplaySelection) {
        val url = selectedVideo?.nativePlaybackUrl
        if (url.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            if (shouldAutoplaySelection || showReelViewer) {
                player.play()
                shouldAutoplaySelection = false
            } else {
                player.pause()
            }
        }
    }

    LaunchedEffect(uiState.videos, initialSelectedVideoId) {
        if (!hasHandledInitialSelection &&
            !initialSelectedVideoId.isNullOrBlank() &&
            uiState.videos.any { it.id == initialSelectedVideoId }
        ) {
            selectedVideoId = initialSelectedVideoId
            hasHandledInitialSelection = true
        } else if (selectedVideoId == null || uiState.videos.none { it.id == selectedVideoId }) {
            selectedVideoId = fallbackSelectedVideo?.id
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        if (!uiState.feedbackMessage.isNullOrBlank()) {
            delay(3_000)
            viewModel.dismissFeedback()
        }
    }

    LaunchedEffect(localFeedbackMessage) {
        if (!localFeedbackMessage.isNullOrBlank()) {
            delay(3_000)
            localFeedbackMessage = null
        }
    }
    val colorScheme = MaterialTheme.colorScheme
    val videoAccent = colorScheme.skydownAccent()
    val videoMysticAccent = colorScheme.skydownAccentMystic()
    val sectionSpacing = rememberSkydownScreenSectionSpacing()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Video",
                        subtitle = "Clips, Videos, Kollabos.",
                        accent = videoMysticAccent,
                    )
                },
                actions = {
                    if (uiState.isUploading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    }

                    if (uiState.isAdmin) {
                        IconButton(onClick = { showUploadSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Upload oeffnen",
                            )
                        }
                    }

                    if (onOpenSettings != null) {
                        AppTopBarSessionActions(
                            onOpenCart = onOpenCart,
                            onOpenProfile = onOpenProfile,
                            onOpenSettings = onOpenSettings,
                            onGuestSignIn = onGuestSignIn,
                            dense = compactVisualDensity,
                        )
                    }
                },
                navigationIcon = if (onBack != null) {
                    {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Zurueck",
                            )
                        }
                    }
                } else {
                    {}
                },
                colors = skydownTopBarColors(),
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (uiState.isAdmin) {
                    FloatingActionButton(
                        onClick = {
                            showUploadSheet = true
                        },
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = "Zum Upload",
                        )
                    }
                }

            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .dismissKeyboardOnTap(onDismissKeyboard = dismissKeyboard)
                .background(
                    skydownScreenBrush(
                        primaryColor = videoAccent,
                        secondaryColor = videoMysticAccent,
                        primaryAlpha = 0.022f,
                        secondaryAlpha = 0.016f,
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video.hub.root"),
                state = listState,
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(sectionSpacing),
            ) {
                item {
                    VideoHubHeroCard(
                        isAdmin = uiState.isAdmin,
                        videoCount = uiState.videos.size,
                        collabCount = uiState.publicConfig.collaborationItems.size,
                        onOpenVideos = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(3)
                            }
                        },
                        onOpenEquipment = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(1)
                            }
                        },
                        onOpenCollaborations = {
                            coroutineScope.launch {
                                listState.animateScrollToItem(4)
                            }
                        },
                    )
                }

                item {
                    VideoEquipmentCard(
                        items = uiState.publicConfig.equipmentItems,
                        onSelectItem = { item -> selectedEquipmentItem = item },
                    )
                }

                item {
                    VideoPlayerCard(
                        video = selectedVideo,
                        player = player,
                        onOpenOriginal = { video ->
                            val originalUrl = video.inAppOriginalUrl.trim()
                            if (originalUrl.isBlank()) return@VideoPlayerCard
                            inAppOriginalTitle = video.title.ifBlank { "Original" }
                            inAppOriginalUrl = originalUrl
                        },
                        onOpenReel = if (selectedVideo != null) {
                            {
                                player.pause()
                                showReelViewer = true
                            }
                        } else {
                            null
                        },
                    )
                }

                item {
                    VideoLibraryCard(
                        uiState = uiState,
                        selectedVideoId = selectedVideoId,
                        onSelectVideo = { video -> selectedVideoId = video.id },
                        onOpenReel = { video ->
                            selectedVideoId = video.id
                            player.pause()
                            showReelViewer = true
                        },
                        onOpenOriginal = { url ->
                            val originalUrl = url.inAppOriginalUrl.trim()
                            if (originalUrl.isBlank()) return@VideoLibraryCard
                            inAppOriginalTitle = url.title.ifBlank { "Original" }
                            inAppOriginalUrl = originalUrl
                        },
                        onToggleHomeFeatured = viewModel::toggleHomeFeatured,
                        onDeleteVideo = viewModel::deleteVideo,
                    )
                }

                item {
                    VideoCollaborationsCard(
                        items = uiState.publicConfig.collaborationItems,
                        onOpenLink = { url -> openExternalLink(context, url) },
                    )
                }

                if (uiState.isAdmin) {
                    item {
                        VideoAdminToolsCard(
                            onOpenEditor = { showAdminSheet = true },
                        )
                    }
                }
            }

            ToastHost(
                message = localFeedbackMessage ?: uiState.feedbackMessage,
                type = if (localFeedbackMessage != null) {
                    if (localFeedbackIsError) ToastType.Error else ToastType.Success
                } else {
                    if (uiState.feedbackIsError) ToastType.Error else ToastType.Success
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        top = innerPadding.calculateTopPadding() + com.nash.skyos.ui.component.SkydownUiTokens.screenTopPadding,
                    ),
            )

            if (showReelViewer && uiState.videos.isNotEmpty()) {
                VideoReelViewerDialog(
                    videos = uiState.videos,
                    selectedVideoId = selectedVideoId,
                    onSelectVideo = { video -> selectedVideoId = video.id },
                    onOpenOriginal = { video ->
                        val originalUrl = video.inAppOriginalUrl.trim()
                        if (originalUrl.isNotBlank()) {
                            inAppOriginalTitle = video.title.ifBlank { "Original" }
                            inAppOriginalUrl = originalUrl
                        }
                    },
                    onDismiss = { showReelViewer = false },
                )
            }

            selectedEquipmentItem?.let { item ->
                VideoEquipmentDetailSheet(
                    item = item,
                    onDismiss = { selectedEquipmentItem = null },
                    onOpenImageFullscreen = { url ->
                        inAppImageUrl = url
                    },
                )
            }

            inAppOriginalUrl?.let { url ->
                OriginalVideoViewerDialog(
                    url = url,
                    title = inAppOriginalTitle,
                    onDismiss = { inAppOriginalUrl = null },
                )
            }

            inAppImageUrl?.let { imageUrl ->
                VideoHubImageViewerDialog(
                    imageUrl = imageUrl,
                    onDismiss = { inAppImageUrl = null },
                )
            }

            if (showUploadSheet && uiState.isAdmin) {
                ModalBottomSheet(
                    onDismissRequest = { showUploadSheet = false },
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            VideoUploadCard(
                                uiState = uiState,
                                onUpdateTitle = viewModel::updateTitle,
                                onUpdateProjectName = viewModel::updateProjectName,
                                onUpdateEmail = viewModel::updateEmail,
                                onUpdateNotes = viewModel::updateNotes,
                                onUpdateExternalVideoUrl = viewModel::updateExternalVideoUrl,
                                onPickFiles = {
                                    dismissKeyboard()
                                    videoPickerLauncher.launch(
                                        arrayOf(
                                            "video/mp4",
                                            "video/quicktime",
                                            "video/x-m4v",
                                            "video/*",
                                        ),
                                    )
                                },
                                onRemoveFile = viewModel::removeFile,
                                onUpload = {
                                    dismissKeyboard()
                                    viewModel.upload(context)
                                },
                                onAddExternalVideo = {
                                    dismissKeyboard()
                                    viewModel.addExternalVideo()
                                },
                            )
                        }
                    }
                }
            }

            if (showAdminSheet && uiState.isAdmin) {
                ModalBottomSheet(
                    onDismissRequest = { showAdminSheet = false },
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        item {
                            VideoFormatCard()
                        }

                        item {
                            VideoPublicConfigEditorCard(
                                uiState = uiState,
                                activeImageUploadTarget = activeConfigImageUploadTarget,
                                onAddEquipment = viewModel::addEquipmentItem,
                                onUpdateEquipmentTitle = { itemId, value ->
                                    viewModel.updateEquipmentItem(itemId, title = value)
                                },
                                onUpdateEquipmentDetail = { itemId, value ->
                                    viewModel.updateEquipmentItem(itemId, detail = value)
                                },
                                onUpdateEquipmentImageUrl = { itemId, value ->
                                    viewModel.updateEquipmentItem(itemId, imageUrl = value)
                                },
                                onPickEquipmentImage = { itemId ->
                                    pendingConfigImageTarget = VideoConfigImageTarget.Equipment(itemId)
                                    configImagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onRemoveEquipmentImage = { itemId ->
                                    val previousImageUrl = uiState.publicConfig.equipmentItems
                                        .firstOrNull { it.id == itemId }
                                        ?.imageUrl
                                        .orEmpty()
                                    viewModel.updateEquipmentItem(itemId, imageUrl = "")
                                    coroutineScope.launch {
                                        editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                                        localFeedbackMessage = "Bild entfernt."
                                        localFeedbackIsError = false
                                    }
                                },
                                onRemoveEquipment = viewModel::removeEquipmentItem,
                                onAddCollaboration = viewModel::addCollaborationItem,
                                onUpdateCollaborationName = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, name = value)
                                },
                                onUpdateCollaborationRole = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, role = value)
                                },
                                onUpdateCollaborationHighlight = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, highlight = value)
                                },
                                onUpdateCollaborationVibe = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, vibe = value)
                                },
                                onUpdateCollaborationImageUrl = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, imageUrl = value)
                                },
                                onPickCollaborationImage = { itemId ->
                                    pendingConfigImageTarget = VideoConfigImageTarget.Collaboration(itemId)
                                    configImagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onRemoveCollaborationImage = { itemId ->
                                    val previousImageUrl = uiState.publicConfig.collaborationItems
                                        .firstOrNull { it.id == itemId }
                                        ?.imageUrl
                                        .orEmpty()
                                    viewModel.updateCollaborationItem(itemId, imageUrl = "")
                                    coroutineScope.launch {
                                        editableImageAssetRepository.deleteImageAsset(previousImageUrl)
                                        localFeedbackMessage = "Bild entfernt."
                                        localFeedbackIsError = false
                                    }
                                },
                                onUpdateCollaborationSpotifyArtistId = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, spotifyArtistId = value)
                                },
                                onUpdateCollaborationInstagramUrl = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, instagramUrl = value)
                                },
                                onUpdateCollaborationYoutubeUrl = { itemId, value ->
                                    viewModel.updateCollaborationItem(itemId, youtubeUrl = value)
                                },
                                onRemoveCollaboration = viewModel::removeCollaborationItem,
                                onSave = viewModel::savePublicConfig,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoHubHeroCard(
    isAdmin: Boolean,
    videoCount: Int,
    collabCount: Int,
    onOpenVideos: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenCollaborations: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val coreAccent = colorScheme.skydownAccent()
    val mysticAccent = colorScheme.skydownAccentMystic()
    val highlightAccent = colorScheme.skydownAccentHighlight()
    BrandHeroCard(
        eyebrow = screenHeaderSettings.videoHubEyebrow.ifBlank { "SKY OS" },
        title = screenHeaderSettings.videoHubTitle.ifBlank { "Video" },
        subtitle = screenHeaderSettings.videoHubSubtitle.ifBlank {
            "Einstieg – der eigentliche Blick fokussiert sich auf den Player und deine Clips unten."
        },
        detail = screenHeaderSettings.videoHubDetail.ifBlank {
            if (isAdmin) {
                "$videoCount Clips, $collabCount Collabs – alles, was im Hub live ist, landet geordnet in der Library."
            } else {
                "$videoCount Clips, $collabCount Collabs – waehlen, schauen, in Ruhe vertiefen."
            }
        },
        backgroundImageUrl = screenHeaderSettings.videoHubImageUrl.ifBlank { null },
        accent = mysticAccent,
        secondaryAccent = highlightAccent,
        marks = listOf(BrandArtwork.Skydown),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BrandPill(
                text = "$videoCount Clips",
                tint = mysticAccent,
                onClick = onOpenVideos,
            )
            BrandPill(
                text = "Equipment",
                tint = coreAccent,
                onClick = onOpenEquipment,
            )
            BrandPill(
                text = "$collabCount Collabs",
                tint = highlightAccent,
                onClick = onOpenCollaborations,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoHubHeroStatCard(
                label = "Reels",
                value = videoCount.toString(),
                icon = Icons.Default.Movie,
                accent = coreAccent,
                isActive = videoCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = "Collabs",
                value = collabCount.toString(),
                icon = Icons.Default.Sync,
                accent = mysticAccent,
                isActive = collabCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = if (isAdmin) "Mode" else "Access",
                value = if (isAdmin) "Admin" else "Public",
                icon = Icons.Default.Sync,
                accent = highlightAccent,
                isActive = isAdmin || collabCount > 0 || videoCount > 0,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun VideoAdminToolsCard(
    onOpenEditor: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.skydownAccentMystic()
    SkydownCard {
        BrandSectionBanner(
            title = "Video Admin",
            subtitle = "Equipment, Featured Collabs und Format-Hinweise in einem Editor.",
            accent = accent,
            icon = Icons.Default.Home,
            tag = "ADMIN",
        )

        BrandActionButton(
            text = "Editor oeffnen",
            onClick = onOpenEditor,
            accent = accent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            icon = Icons.Default.Movie,
        )
    }
}

@Composable
private fun VideoHubHeroStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    accent: Color,
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    BrandHeroMetricCard(
        label = label,
        value = value,
        accent = accent,
        modifier = modifier,
        icon = icon,
        isActive = isActive,
    )
}

@Composable
private fun VideoHubSectionBanner(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
) {
    BrandSectionBanner(
        title = title,
        subtitle = subtitle,
        accent = accent,
        icon = icon,
        tag = tag,
    )
}

@Composable
private fun VideoControlDeckCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tag: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                        accent.copy(alpha = 0.10f),
                        colorScheme.skydownSecondaryBackground().copy(alpha = 0.34f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = {
            VideoHubSectionBanner(
                title = title,
                subtitle = subtitle,
                icon = icon,
                accent = accent,
                tag = tag,
            )
            content()
        },
    )
}

@Composable
private fun VideoFormatCard() {
    val accent = MaterialTheme.colorScheme.skydownAccent()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Format Check",
            subtitle = "Saubere Uploads halten den Hub schnell und klar.",
            icon = Icons.Default.CheckCircle,
            accent = accent,
            tag = "FORMAT",
        )
        Text(
            text = "MP4, MOV oder M4V bleiben die stabilsten Formate fuer den Player und den Video-Flow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = "MP4", isActive = true)
            VideoPill(text = "9:16 ready", isActive = false)
            VideoPill(text = "Compressed", isActive = false)
        }
        Text(
            text = "Komprimierte Cuts laden schneller, wirken im Feed ruhiger und bleiben direkt abrufbar.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun VideoCollaborationsCard(
    items: List<ProducedWithArtist>,
    onOpenLink: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.skydownAccentMystic()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Featured Collabs",
            subtitle = "Artists und Creatives hinter dem visuellen Vibe.",
            icon = Icons.Default.Sync,
            accent = accent,
            tag = "CREW",
        )
        Text(
            text = "Diese Collabs bilden das aktuelle Netzwerk rund um Reels, Looks und gemeinsame Releases.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = "Featured Collabs folgen. Der Bereich bleibt bereit und wird laufend ergaenzt.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 14.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { artist ->
                    ProducedWithArtistRow(
                        artist = artist,
                        onOpenLink = onOpenLink,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoEquipmentCard(
    items: List<VideoEquipmentItem>,
    onSelectItem: (VideoEquipmentItem) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.skydownAccent()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Equipment & Software",
            subtitle = "Visual Stack fuer Shoot, Edit und Finish.",
            icon = Icons.Default.CameraAlt,
            accent = accent,
            tag = "STACK",
        )
        Text(
            text = "Das aktuelle Equipment zeigt, womit die Visuals gebaut und veredelt werden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = "Equipment wird aktuell vorbereitet und erscheint hier in Kuerze.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 14.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.forEach { item ->
                    VideoEquipmentRow(
                        item = item,
                        onClick = { onSelectItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoEquipmentRow(
    item: VideoEquipmentItem,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.skydownAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownCardBackground().copy(alpha = 0.96f),
                        accent.copy(alpha = 0.10f),
                        colorScheme.skydownAccentHighlight().copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp),
            )
            .let { baseModifier ->
                if (onClick != null) {
                    baseModifier
                        .skydownPressable(
                            interactionSource = interactionSource,
                            pressedScale = 0.988f,
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                } else {
                    baseModifier
                }
            }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        BrandPreviewFrame(
            accent = accent,
            modifier = Modifier
                .size(76.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.90f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (!item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.92f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VideoPill(text = "Visual Stack", isActive = true)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VideoEquipmentDetailSheet(
    item: VideoEquipmentItem,
    onDismiss: () -> Unit,
    onOpenImageFullscreen: (String) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VideoPill(text = "Visual Stack", isActive = true)
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = item.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )

            if (!item.imageUrl.isNullOrBlank()) {
                BrandPreviewFrame(
                    accent = MaterialTheme.colorScheme.skydownAccent(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp),
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                }

                OutlinedButton(
                    onClick = { onOpenImageFullscreen(item.imageUrl.orEmpty()) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text("Bild im Vollbild ansehen")
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("Schliessen")
            }
        }
    }
}

@Composable
private fun VideoPublicConfigEditorCard(
    uiState: com.nash.skyos.ui.model.VideoHubUiState,
    activeImageUploadTarget: VideoConfigImageTarget?,
    onAddEquipment: () -> Unit,
    onUpdateEquipmentTitle: (String, String) -> Unit,
    onUpdateEquipmentDetail: (String, String) -> Unit,
    onUpdateEquipmentImageUrl: (String, String) -> Unit,
    onPickEquipmentImage: (String) -> Unit,
    onRemoveEquipmentImage: (String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onAddCollaboration: () -> Unit,
    onUpdateCollaborationName: (String, String) -> Unit,
    onUpdateCollaborationRole: (String, String) -> Unit,
    onUpdateCollaborationHighlight: (String, String) -> Unit,
    onUpdateCollaborationVibe: (String, String) -> Unit,
    onUpdateCollaborationImageUrl: (String, String) -> Unit,
    onPickCollaborationImage: (String) -> Unit,
    onRemoveCollaborationImage: (String) -> Unit,
    onUpdateCollaborationSpotifyArtistId: (String, String) -> Unit,
    onUpdateCollaborationInstagramUrl: (String, String) -> Unit,
    onUpdateCollaborationYoutubeUrl: (String, String) -> Unit,
    onRemoveCollaboration: (String) -> Unit,
    onSave: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val adminAccent = colorScheme.skydownAccentHighlight()
    val equipmentAccent = colorScheme.skydownAccent()
    val collabAccent = colorScheme.skydownAccentMystic()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Admin Control",
            subtitle = "Owner und Video-Admins steuern hier Equipment, Links und Featured Collabs.",
            icon = Icons.Default.Sync,
            accent = adminAccent,
            tag = "ADMIN",
        )
        Text(
            text = "Bilder laufen picker-first mit Vorschau, externe Links werden sauber mit den oeffentlichen Hub-Daten verbunden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Eintraege kannst du neu anlegen, ersetzen oder entfernen. Oeffentliche Daten werden erst nach `Speichern` live.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = "Picker-first", isActive = true)
            VideoPill(text = "Public save", isActive = false)
            VideoPill(text = "Admin only", isActive = false)
        }

        VideoControlDeckCard(
            title = "Equipment Library",
            subtitle = "Kameras, Tools und Software fuer den visuellen Workflow.",
            icon = Icons.Default.CameraAlt,
            accent = equipmentAccent,
            tag = "STACK",
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.publicConfig.equipmentItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                                        equipmentAccent.copy(alpha = 0.10f),
                                        colorScheme.skydownAccentHighlight().copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = equipmentAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VideoPill(text = "Gear", isActive = true)
                            VideoPill(text = if (item.imageUrl.isNullOrBlank()) "No image" else "Image ready", isActive = false)
                        }
                        OutlinedTextField(
                            value = item.title,
                            onValueChange = { onUpdateEquipmentTitle(item.id, it) },
                            label = { Text("Titel") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = item.detail,
                            onValueChange = { onUpdateEquipmentDetail(item.id, it) },
                            label = { Text("Detail") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        EditableImageFieldCard(
                            title = "Equipment-Bild",
                            imageUrl = item.imageUrl.orEmpty(),
                            isUploading = activeImageUploadTarget == VideoConfigImageTarget.Equipment(item.id),
                            uploadStatusText = "Equipment-Bild wird uebernommen.",
                            onPickImage = { onPickEquipmentImage(item.id) },
                            onImageUrlChange = { onUpdateEquipmentImageUrl(item.id, it) },
                            onRemoveImage = { onRemoveEquipmentImage(item.id) },
                        )
                        OutlinedButton(
                            onClick = { onRemoveEquipment(item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Eintrag entfernen")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddEquipment,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Equipment hinzufuegen")
            }
        }

        VideoControlDeckCard(
            title = "Collab Profiles",
            subtitle = "Rollen, Highlights, Vibes und Links fuer das Featured-Netzwerk.",
            icon = Icons.Default.CheckCircle,
            accent = collabAccent,
            tag = "CREW",
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                uiState.publicConfig.collaborationItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                                        collabAccent.copy(alpha = 0.10f),
                                        colorScheme.skydownAccentHighlight().copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = collabAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            VideoPill(text = item.role.ifBlank { "Collab" }, isActive = true)
                            if (item.vibe.isNotBlank()) {
                                VideoPill(text = item.vibe, isActive = false)
                            }
                        }
                        OutlinedTextField(
                            value = item.name,
                            onValueChange = { onUpdateCollaborationName(item.id, it) },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = item.role,
                            onValueChange = { onUpdateCollaborationRole(item.id, it) },
                            label = { Text("Rolle") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = item.highlight,
                            onValueChange = { onUpdateCollaborationHighlight(item.id, it) },
                            label = { Text("Highlight") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                        )
                        OutlinedTextField(
                            value = item.vibe,
                            onValueChange = { onUpdateCollaborationVibe(item.id, it) },
                            label = { Text("Vibe") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        EditableImageFieldCard(
                            title = "Collab-Bild",
                            imageUrl = item.imageUrl.orEmpty(),
                            isUploading = activeImageUploadTarget == VideoConfigImageTarget.Collaboration(item.id),
                            uploadStatusText = "Collab-Bild wird uebernommen.",
                            onPickImage = { onPickCollaborationImage(item.id) },
                            onImageUrlChange = { onUpdateCollaborationImageUrl(item.id, it) },
                            onRemoveImage = { onRemoveCollaborationImage(item.id) },
                        )
                        OutlinedTextField(
                            value = item.spotifyArtistId.orEmpty(),
                            onValueChange = { onUpdateCollaborationSpotifyArtistId(item.id, it) },
                            label = { Text("Spotify Artist ID") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = item.instagramUrl.orEmpty(),
                            onValueChange = { onUpdateCollaborationInstagramUrl(item.id, it) },
                            label = { Text("Instagram URL") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedTextField(
                            value = item.youtubeUrl.orEmpty(),
                            onValueChange = { onUpdateCollaborationYoutubeUrl(item.id, it) },
                            label = { Text("YouTube URL") },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        OutlinedButton(
                            onClick = { onRemoveCollaboration(item.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Collab entfernen")
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddCollaboration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text("Collab hinzufuegen")
            }
        }

        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            if (uiState.isSavingPublicConfig) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("Aenderungen speichern")
            }
        }
    }
}

@Composable
private fun ProducedWithArtistRow(
    artist: com.nash.skyos.ui.model.ProducedWithArtist,
    onOpenLink: (String) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val collabAccent = colorScheme.skydownAccentMystic()
    val highlightAccent = colorScheme.skydownAccentHighlight()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                        collabAccent.copy(alpha = 0.16f),
                        highlightAccent.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = collabAccent.copy(alpha = 0.22f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            collabAccent,
                            colorScheme.skydownAccent(),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = artist.name.take(1).uppercase(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black,
                color = Color.White,
            )
        }

        if (!artist.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = artist.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.08f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.56f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color.Black.copy(alpha = 0.20f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = artist.role.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(highlightAccent.copy(alpha = 0.22f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "COLLAB",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.94f),
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (artist.vibe.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(collabAccent.copy(alpha = 0.82f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = artist.vibe,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (artist.highlight.isNotBlank()) {
                Text(
                    text = artist.highlight,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VideoPill(text = artist.role.ifBlank { "Collab" }, isActive = true)
                if (artist.vibe.isNotBlank()) {
                    VideoPill(text = artist.vibe, isActive = false)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                artist.spotifyArtistId?.takeIf { it.isNotBlank() }?.let { spotifyArtistId ->
                    SocialActionChip(
                        title = "Spotify",
                        icon = Icons.Default.PlayArrow,
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                SpotifyGreen,
                                SpotifyGreen.copy(alpha = 0.72f),
                            ),
                        ),
                        onClick = { onOpenLink("https://open.spotify.com/artist/$spotifyArtistId") },
                    )
                }
                artist.instagramUrl?.takeIf { it.isNotBlank() }?.let { instagramUrl ->
                    SocialActionChip(
                        title = "Instagram",
                        icon = Icons.Default.CameraAlt,
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                InstagramPurple,
                                InstagramPink,
                                InstagramOrange,
                            ),
                        ),
                        onClick = { onOpenLink(instagramUrl) },
                    )
                }
                artist.youtubeUrl?.takeIf { it.isNotBlank() }?.let { youtubeUrl ->
                    SocialActionChip(
                        title = "YouTube",
                        icon = Icons.Default.PlayArrow,
                        gradient = Brush.linearGradient(
                            colors = listOf(
                                YouTubeRed,
                                MaterialTheme.colorScheme.skydownYoutube().copy(alpha = 0.82f),
                            ),
                        ),
                        onClick = { onOpenLink(youtubeUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialActionChip(
    title: String,
    icon: ImageVector,
    gradient: Brush,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .skydownPressable(interactionSource)
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = Color.White,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun VideoUploadCard(
    uiState: com.nash.skyos.ui.model.VideoHubUiState,
    onUpdateTitle: (String) -> Unit,
    onUpdateProjectName: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateExternalVideoUrl: (String) -> Unit,
    onPickFiles: () -> Unit,
    onRemoveFile: (SelectedVideoFile) -> Unit,
    onUpload: () -> Unit,
    onAddExternalVideo: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val uploadAccent = colorScheme.skydownAccentHighlight()
    val pickerAccent = colorScheme.skydownAccent()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Video Upload",
            subtitle = "Admin-only Bereich fuer neue Clips und externe Videoquellen.",
            icon = Icons.Default.Movie,
            accent = uploadAccent,
            tag = "UPLOAD",
        )
        Text(
            text = "Hier steuerst du direkte Uploads nach Firebase Storage oder externe Video-Links fuer den Hub.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = "Storage", isActive = true)
            VideoPill(text = "Video-Link", isActive = false)
            VideoPill(text = "External", isActive = false)
        }

        OutlinedTextField(
            value = uiState.videoTitle,
            onValueChange = onUpdateTitle,
            label = { Text("Titel") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        OutlinedTextField(
            value = uiState.projectName,
            onValueChange = onUpdateProjectName,
            label = { Text("Projekt / Artist") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        OutlinedTextField(
            value = uiState.email,
            onValueChange = onUpdateEmail,
            label = { Text("Kontakt-Mail") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onUpdateNotes,
            label = { Text("Notiz") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            minLines = 3,
        )

        BrandActionButton(
            text = "Videos auswaehlen",
            onClick = onPickFiles,
            accent = pickerAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            icon = Icons.Default.Movie,
            filled = false,
        )

        Text(
            text = "Oder als externer Video-Link freigeben.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 14.dp),
        )

        OutlinedTextField(
            value = uiState.externalVideoUrl,
            onValueChange = onUpdateExternalVideoUrl,
            label = { Text("Google Drive / MEGA / anderer Video-Link") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            minLines = 2,
        )

        if (uiState.selectedFiles.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                uiState.selectedFiles.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.skydownCardBackground().copy(alpha = 0.96f),
                                        pickerAccent.copy(alpha = 0.10f),
                                        colorScheme.skydownAccentHighlight().copy(alpha = 0.08f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = pickerAccent.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(16.dp),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = readableFileSize(file.fileSizeBytes),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        TextButton(onClick = { onRemoveFile(file) }) {
                            Text("Entfernen")
                        }
                    }
                }
            }
        }

        uiState.validationMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        BrandActionButton(
            text = if (uiState.isUploading) "Upload laeuft ..." else "Videos hochladen",
            onClick = onUpload,
            accent = uploadAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            icon = Icons.Default.Movie,
            enabled = !uiState.isUploading,
            isLoading = uiState.isUploading,
        )

        BrandActionButton(
            text = "Externes Video freigeben",
            onClick = onAddExternalVideo,
            accent = colorScheme.skydownAccent(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            icon = Icons.Default.Language,
            enabled = !uiState.isUploading,
            filled = false,
        )
    }
}

@Composable
private fun VideoPlayerCard(
    video: VideoHubItem?,
    player: ExoPlayer,
    onOpenOriginal: (VideoHubItem) -> Unit,
    onOpenReel: (() -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val playerAccent = colorScheme.skydownAccentMystic()
    SkydownCard {
        VideoHubSectionBanner(
            title = "Now Playing",
            subtitle = "Das aktive Video bleibt gross, fokussiert und direkt abspielbar.",
            icon = Icons.Default.Movie,
            accent = playerAccent,
            tag = "LIVE",
        )

        Text(
            text = "Der aktive Clip sitzt jetzt direkt im Fokus. Fuer den ganzen Feed kannst du jederzeit in den Vollbild-Modus springen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (video == null) {
            Text(
                text = "Noch kein Fokus-Video aktiv. Waehle einen Clip aus der Library oder aktualisiere den Hub.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 12.dp),
            )
            return@SkydownCard
        }

        BrandPreviewFrame(
            accent = videoHubProviderAccent(video),
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(top = 14.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    video.usesEmbeddedPreview -> {
                        ExternalVideoWebPlayer(
                            url = video.inlineEmbedUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    video.nativePlaybackUrl.isNotBlank() -> {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { playerContext ->
                                PlayerView(playerContext).apply {
                                    useController = true
                                    this.player = player
                                }
                            },
                            update = { view ->
                                view.player = player
                            },
                        )
                    }

                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = video.originalDestinationDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    androidx.compose.ui.graphics.Color.Transparent,
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.18f),
                                    androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.76f),
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(SkydownUiTokens.panelPadding),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = buildString {
                            append(video.projectName)
                            append(" · ")
                            append(video.providerBadge)
                            append(" · ")
                            append(formatVideoDate(video.createdAtMillis))
                            if (video.isHomeFeatured) {
                                append(" · Home")
                            }
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.76f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (video.notes.isNotBlank()) {
                        Text(
                            text = video.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (video.opensOriginalInApp) {
            BrandActionButton(
                text = video.directOpenActionLabel,
                onClick = { onOpenOriginal(video) },
                accent = if (video.supportsInlinePlayback) playerAccent else videoHubProviderAccent(video),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video.hub.player.original.open")
                    .padding(top = 14.dp),
                icon = if (video.supportsInlinePlayback) Icons.Default.PlayArrow else Icons.Default.Language,
                filled = true,
            )
        } else if (video.supportsInlinePlayback) {
            onOpenReel?.let { openReel ->
                BrandActionButton(
                    text = videoHubInlineActionLabel(video),
                    onClick = openReel,
                    accent = playerAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    icon = Icons.Default.PlayArrow,
                )
            }
        }
    }
}

private enum class VideoLibraryRowPresentation {
    Featured,
    Secondary,
    Catalog,
}

private fun videoHubPreviewImageUrl(video: VideoHubItem): String? {
    val youTubeRef = when {
        video.embedUrl.isNotBlank() -> video.embedUrl
        video.externalUrl.isNotBlank() -> video.externalUrl
        else -> ""
    }
    if (youTubeRef.isNotBlank()) {
        val thumb = resolveYouTubeThumbnailUrl(youTubeRef)
        if (thumb != null) return thumb
    }
    if (video.isPlayable && video.nativePlaybackUrl.isNotBlank()) {
        return video.nativePlaybackUrl
    }
    return null
}

private fun videoHubLibraryMetaLine(
    video: VideoHubItem,
    isAdmin: Boolean,
    presentation: VideoLibraryRowPresentation,
): String {
    val date = formatVideoDate(video.createdAtMillis)
    return when (presentation) {
        VideoLibraryRowPresentation.Catalog -> {
            "${video.projectName} · $date"
        }
        VideoLibraryRowPresentation.Featured -> {
            // Immersion: Bühne trägt den Titel; Meta bleibt eine dezente Zeile ohne Provider-Noise.
            buildString {
                append(video.projectName)
                append(" · ")
                append(date)
                if (isAdmin) {
                    append(" · ")
                    append(if (video.isPublic) "Public" else "Private")
                    if (video.isHomeFeatured) {
                        append(" · Home")
                    }
                }
            }
        }
        else -> {
            buildString {
                append(video.projectName)
                append(" · ")
                append(date)
                append(" · ")
                append(video.providerBadge)
                if (isAdmin) {
                    append(" · ")
                    append(if (video.isPublic) "Public" else "Private")
                    if (video.isHomeFeatured) {
                        append(" · Home")
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoLibraryCard(
    uiState: com.nash.skyos.ui.model.VideoHubUiState,
    selectedVideoId: String?,
    onSelectVideo: (VideoHubItem) -> Unit,
    onOpenReel: (VideoHubItem) -> Unit,
    onOpenOriginal: (VideoHubItem) -> Unit,
    onToggleHomeFeatured: (VideoHubItem) -> Unit,
    onDeleteVideo: (VideoHubItem) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    SkydownCard {
        Column(Modifier.testTag("video.hub.library.header")) {
            Text(
                text = "Aus deinen Clips",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = if (uiState.isAdmin) {
                    "${uiState.videos.size} Titel im Hub"
                } else {
                    "${uiState.videos.size} zum Schauen bereit"
                },
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        when {
            uiState.isLoadingVideos -> {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                    )
                    Text("Videos werden ruhig vorbereitet ...")
                }
            }

            uiState.videos.isEmpty() -> {
                Text(
                    text = "Aktuell sind noch keine Videos sichtbar. Bitte spaeter erneut pruefen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    uiState.videos.forEachIndexed { index, video ->
                        val presentation = when {
                            index == 0 -> VideoLibraryRowPresentation.Featured
                            index == 1 && uiState.videos.size > 1 -> VideoLibraryRowPresentation.Secondary
                            else -> VideoLibraryRowPresentation.Catalog
                        }
                        val topPad = when {
                            index == 0 && uiState.videos.size > 1 -> 2.dp
                            index == 2 && uiState.videos.size > 2 -> 10.dp
                            else -> 0.dp
                        }
                        val bottomAfterFeatured = if (index == 0 && uiState.videos.size > 1) {
                            6.dp
                        } else {
                            0.dp
                        }
                        VideoLibraryRow(
                            video = video,
                            isSelected = video.id == selectedVideoId,
                            isAdmin = uiState.isAdmin,
                            rowIndex = index,
                            presentation = presentation,
                            onSelect = { onSelectVideo(video) },
                            onOpenReel = { onOpenReel(video) },
                            onOpenOriginal = { onOpenOriginal(video) },
                            onToggleHomeFeatured = { onToggleHomeFeatured(video) },
                            onDelete = { onDeleteVideo(video) },
                            modifier = Modifier
                                .padding(top = topPad)
                                .padding(bottom = bottomAfterFeatured),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoLibraryRow(
    video: VideoHubItem,
    isSelected: Boolean,
    isAdmin: Boolean,
    rowIndex: Int,
    presentation: VideoLibraryRowPresentation,
    modifier: Modifier = Modifier,
    onSelect: () -> Unit,
    onOpenReel: () -> Unit,
    onOpenOriginal: () -> Unit,
    onToggleHomeFeatured: () -> Unit,
    onDelete: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val providerAccent = videoHubProviderAccent(video)
    val catalog = presentation == VideoLibraryRowPresentation.Catalog
    val isFeatured = presentation == VideoLibraryRowPresentation.Featured
    val isSecondary = presentation == VideoLibraryRowPresentation.Secondary
    val catalogStripe = catalog && rowIndex % 2 == 1
    val thumbWidth = when (presentation) {
        VideoLibraryRowPresentation.Featured -> 0.dp
        VideoLibraryRowPresentation.Secondary -> 96.dp
        VideoLibraryRowPresentation.Catalog -> 70.dp
    }
    val thumbHeight = when (presentation) {
        VideoLibraryRowPresentation.Featured -> 0.dp
        VideoLibraryRowPresentation.Secondary -> 54.dp
        VideoLibraryRowPresentation.Catalog -> 40.dp
    }
    val cardShape = when (presentation) {
        VideoLibraryRowPresentation.Featured -> RoundedCornerShape(26.dp)
        VideoLibraryRowPresentation.Secondary -> RoundedCornerShape(20.dp)
        VideoLibraryRowPresentation.Catalog -> RoundedCornerShape(17.dp)
    }
    val innerH = when (presentation) {
        VideoLibraryRowPresentation.Featured -> 18.dp
        VideoLibraryRowPresentation.Secondary -> 15.dp
        VideoLibraryRowPresentation.Catalog -> 12.dp
    }
    val innerV = when (presentation) {
        VideoLibraryRowPresentation.Featured -> 0.dp
        VideoLibraryRowPresentation.Secondary -> 12.dp
        VideoLibraryRowPresentation.Catalog -> 9.dp
    }
    val previewModel = remember(video) { videoHubPreviewImageUrl(video) }
    val unselectedBaseAlpha = if (catalog) {
        if (catalogStripe) 0.86f else 0.9f
    } else {
        0.96f
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(
                Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(
                            colorScheme.skydownAccent().copy(alpha = if (isFeatured) 0.2f else 0.15f),
                            colorScheme.skydownAccentHighlight().copy(alpha = if (isFeatured) 0.12f else 0.1f),
                            colorScheme.skydownCardBackground().copy(alpha = 0.97f),
                        )
                    } else {
                        listOf(
                            colorScheme.skydownCardBackground().copy(alpha = unselectedBaseAlpha),
                            colorScheme.skydownSecondaryBackground().copy(
                                alpha = if (catalog) {
                                    if (catalogStripe) 0.42f else 0.5f
                                } else {
                                    0.55f
                                },
                            ),
                        )
                    },
                ),
            )
            .border(
                width = when {
                    isFeatured && isSelected -> 1.25.dp
                    isFeatured -> 1.1.dp
                    isSecondary && isSelected -> 0.9.dp
                    isSecondary -> 0.8.dp
                    isSelected && catalog -> 0.85.dp
                    else -> 0.7.dp
                },
                color = if (isSelected) {
                    colorScheme.skydownAccentHighlight().copy(
                        alpha = if (isFeatured) 0.35f else if (isSecondary) 0.28f else 0.22f,
                    )
                } else {
                    colorScheme.outline.copy(
                        alpha = when {
                            isFeatured -> 0.12f
                            isSecondary -> 0.1f
                            catalogStripe -> 0.11f
                            else -> 0.08f
                        },
                    )
                },
                shape = cardShape,
            ),
    ) {
        if (isFeatured) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                if (previewModel != null) {
                    AsyncImage(
                        model = previewModel,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                RoundedCornerShape(
                                    topStart = 26.dp,
                                    topEnd = 26.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp,
                                ),
                            ),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        providerAccent.copy(alpha = 0.9f),
                                        colorScheme.skydownCinematicShadow().copy(alpha = 0.8f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Movie,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(104.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f),
                                ),
                            ),
                        ),
                )
                Text(
                    text = video.title,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .fillMaxWidth(0.92f),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .size(22.dp),
                )
            }
        }

        Column(
            modifier = Modifier.padding(
                horizontal = innerH,
                vertical = if (isFeatured) 14.dp else innerV,
            ),
            verticalArrangement = Arrangement.spacedBy(if (isFeatured) 10.dp else if (catalog) 6.dp else 8.dp),
        ) {
            if (!isFeatured) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (catalog) 7.dp else 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    BrandPreviewFrame(
                        accent = providerAccent,
                        modifier = Modifier
                            .size(thumbWidth, thumbHeight),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (previewModel != null) {
                                AsyncImage(
                                    model = previewModel,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(14.dp)),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    providerAccent.copy(alpha = 0.88f),
                                                    colorScheme.skydownCinematicShadow().copy(alpha = 0.76f),
                                                ),
                                            ),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(
                                            if (isSecondary) 24.dp else 22.dp,
                                        ),
                                    )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = video.title,
                            style = if (isSecondary) {
                                MaterialTheme.typography.titleSmall
                            } else {
                                MaterialTheme.typography.titleSmall
                            },
                            fontWeight = if (isSecondary) FontWeight.SemiBold else FontWeight.SemiBold,
                            color = if (catalog) {
                                colorScheme.onSurface.copy(alpha = 0.88f)
                            } else {
                                colorScheme.onSurface
                            },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = videoHubLibraryMetaLine(
                                video = video,
                                isAdmin = isAdmin,
                                presentation = presentation,
                            ),
                            style = if (isSecondary) {
                                MaterialTheme.typography.labelSmall
                            } else {
                                MaterialTheme.typography.labelMedium
                            },
                            color = MaterialTheme.colorScheme.onSurface.copy(
                                alpha = if (isSecondary) 0.6f else 0.55f,
                            ),
                            maxLines = if (catalog) 1 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (isAdmin && video.fileName.isNotBlank() && isSecondary) {
                            Text(
                                text = video.fileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                                maxLines = 1,
                            )
                        }
                        if (video.notes.isNotBlank() && isSecondary) {
                            Text(
                                text = video.notes,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = videoHubLibraryMetaLine(
                        video = video,
                        isAdmin = isAdmin,
                        presentation = presentation,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface.copy(
                        alpha = if (isAdmin) 0.6f else 0.55f,
                    ),
                    maxLines = 1,
                )
                if (isAdmin && video.fileName.isNotBlank()) {
                    Text(
                        text = video.fileName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                        maxLines = 1,
                    )
                }
                if (isAdmin && video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1,
                    )
                }
            }

            if (!catalog && !isFeatured) {
                val hintText = when {
                    isSecondary && isAdmin -> "Auswahl sitzt sofort im Player."
                    isSecondary -> "Tippen, um den Fokus zu waehlen – der Player oben folgt."
                    else -> videoLibraryInteractionHint(
                        video = video,
                        isAdmin = isAdmin,
                        isSelected = isSelected,
                    )
                }
                Text(
                    text = hintText,
                    style = MaterialTheme.typography.labelSmall,
                    color = videoHubProviderAccent(video).copy(alpha = if (isSecondary) 0.72f else 0.78f),
                    maxLines = if (isSecondary) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isAdmin) {
                if (video.supportsInlinePlayback) {
                    Button(
                        onClick = onSelect,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(
                            when {
                                isSelected && video.usesEmbeddedPreview -> "Aktiv in Preview"
                                isSelected -> "Aktiv im Player"
                                video.usesEmbeddedPreview -> "In Preview laden"
                                else -> "Im Player laden"
                            },
                        )
                    }
                } else if (video.openUrl.isNotBlank()) {
                    OutlinedButton(
                        onClick = onOpenOriginal,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(video.originalActionLabel)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onToggleHomeFeatured,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = null,
                        )
                        Text(
                            text = if (video.isHomeFeatured) "Home aktiv" else "Im Home zeigen",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }

                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                        )
                        Text(
                            text = "Loeschen",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            } else {
                if (video.opensOriginalInApp) {
                    if (video.supportsInlinePlayback) {
                        Button(
                            onClick = onOpenOriginal,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(videoHubDirectCompactActionLabel(video))
                        }
                    } else {
                        OutlinedButton(
                            onClick = onOpenOriginal,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                        ) {
                            Text(video.originalActionLabel)
                        }
                    }
                } else if (video.supportsInlinePlayback) {
                    Button(
                        onClick = onOpenReel,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                    ) {
                        Text(videoHubInlineCompactActionLabel(video))
                    }
                }
            }
        }
    }
}

private fun videoLibraryInteractionHint(
    video: VideoHubItem,
    isAdmin: Boolean,
    isSelected: Boolean,
): String {
    if (isAdmin) {
        if (video.isPlayable) {
            return if (isSelected) {
                "Der Clip sitzt gerade im Player und bleibt als Fokus-Video oben sichtbar."
            } else {
                "Ein Tap laedt den Clip in den Player und macht ihn zum aktuellen Fokus."
            }
        }
        if (video.opensOriginalInApp) {
            return "Das Original bleibt in der App und fuehrt mit Schliessen sicher zurueck."
        }
        if (video.supportsInlinePlayback) {
            return "Dieser Clip laeuft direkt im In-App-Reel ohne Browser-Zwischenweg."
        }
            return "Aktuell steht hier nur der externe Aufruf zur Verfuegung."
    }

    if (video.opensOriginalInApp) {
        return if (video.supportsInlinePlayback) {
            "Ein Tap oeffnet den Clip direkt in der App, ohne weiteren Zwischenscreen."
        } else {
            "Ein Tap bringt dich in die In-App-Originalansicht mit sicherem Rueckweg."
        }
    }
    if (video.supportsInlinePlayback) {
        return "Ein Tap startet die direkte Videoansicht sofort im Feed-Flow."
    }
    return "Dieses Video wird derzeit ueber einen externen Link geoeffnet."
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoReelViewerDialog(
    videos: List<VideoHubItem>,
    selectedVideoId: String?,
    onSelectVideo: (VideoHubItem) -> Unit,
    onOpenOriginal: (VideoHubItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val reelPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    val initialPage = remember(videos, selectedVideoId) {
        videos.indexOfFirst { it.id == selectedVideoId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videos.size.coerceAtLeast(1) },
    )
    var isTransitioning by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage, videos) {
        val video = videos.getOrNull(pagerState.currentPage)
        video?.let(onSelectVideo)
        isTransitioning = true
        val url = video?.nativePlaybackUrl
        if (url.isNullOrBlank()) {
            reelPlayer.stop()
            reelPlayer.clearMediaItems()
        } else {
            reelPlayer.setMediaItem(MediaItem.fromUri(url))
            reelPlayer.prepare()
            reelPlayer.play()
        }
        delay(220)
        isTransitioning = false
    }

    DisposableEffect(reelPlayer) {
        onDispose {
            reelPlayer.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black),
        ) {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val video = videos.getOrNull(page)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    if (video != null && page == pagerState.currentPage && video.usesEmbeddedPreview) {
                        ExternalVideoWebPlayer(
                            url = video.inlineEmbedUrl,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else if (video != null && page == pagerState.currentPage && video.nativePlaybackUrl.isNotBlank()) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { playerContext ->
                                PlayerView(playerContext).apply {
                                    useController = false
                                    this.player = reelPlayer
                                }
                            },
                            update = { view ->
                                view.player = reelPlayer
                            },
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.16f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                                            androidx.compose.ui.graphics.Color.Black,
                                        ),
                                    ),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                )
                                if (video != null) {
                                    Text(
                                        text = video.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        androidx.compose.ui.graphics.Color.Transparent,
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.20f),
                                        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.84f),
                                    ),
                                ),
                            ),
                    )

                    video?.let {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 22.dp, vertical = 30.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = it.projectName.uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.66f),
                            )
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (it.notes.isNotBlank()) {
                                Text(
                                    text = it.notes,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            if (it.opensOriginalInApp) {
                                BrandActionButton(
                                    text = it.directOpenActionLabel,
                                    onClick = { onOpenOriginal(it) },
                                    accent = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.fillMaxWidth(),
                                    icon = Icons.Default.Language,
                                    filled = false,
                                    compact = true,
                                )
                            }
                        }
                    }
                }
            }

            if (videos.size > 1) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .background(
                            color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.30f),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    videos.forEachIndexed { index, _ ->
                        val isActive = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = if (isActive) 28.dp else 9.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    androidx.compose.ui.graphics.Color.White.copy(
                                        alpha = if (isActive) 0.96f else 0.34f,
                                    ),
                                ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = videos
                            .getOrNull(pagerState.currentPage)
                            ?.let { video ->
                                if (video.usesEmbeddedPreview) "SkyOS Preview" else "SkyOS Reel"
                            }
                            ?: "SkyOS Video",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} von ${videos.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f),
                    )
                    if (videos.size > 1) {
                        Text(
                            text = "Wische vertikal durch alle Clips",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.58f),
                        )
                    }
                }

                BrandActionButton(
                    text = "Zurueck",
                    onClick = onDismiss,
                    accent = MaterialTheme.colorScheme.onPrimary,
                    icon = Icons.Default.Close,
                    filled = false,
                    compact = true,
                )
            }

            AnimatedVisibility(
                visible = isTransitioning,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                        easing = FastOutSlowInEasing,
                    ),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.48f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "Clip wird vorbereitet ...",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.90f),
                    )
                }
            }
        }
    }
}

@Composable
private fun videoHubProviderAccent(video: VideoHubItem): Color {
    return when {
        video.usesEmbeddedPreview -> MaterialTheme.colorScheme.skydownYoutube()
        video.providerBadge.equals("Drive", ignoreCase = true) -> MaterialTheme.colorScheme.skydownAccent()
        video.providerBadge.equals("MEGA", ignoreCase = true) -> MaterialTheme.colorScheme.skydownAccentHighlight()
        else -> MaterialTheme.colorScheme.skydownAccentMystic()
    }
}

private fun videoHubInlineActionLabel(video: VideoHubItem): String {
    return if (video.usesEmbeddedPreview) {
        "In Preview ansehen"
    } else {
        "Im Video ansehen"
    }
}

private fun videoHubInlineCompactActionLabel(video: VideoHubItem): String {
    return if (video.usesEmbeddedPreview) {
        "In Preview"
    } else {
        "Direkt im Video"
    }
}

private fun videoHubDirectCompactActionLabel(video: VideoHubItem): String {
    return if (video.supportsInlinePlayback) {
        "Direkt oeffnen"
    } else {
        video.originalActionLabel
    }
}

@Composable
private fun VideoHubImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.94f)),
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 48.dp),
                contentScale = ContentScale.Fit,
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Bildvorschau",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )

                BrandActionButton(
                    text = "Schliessen",
                    onClick = onDismiss,
                    accent = MaterialTheme.colorScheme.onPrimary,
                    icon = Icons.Default.Close,
                    filled = false,
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun VideoPill(
    text: String,
    isActive: Boolean,
    onClick: (() -> Unit)? = null,
) {
    BrandStatusChip(
        text = text,
        accent = MaterialTheme.colorScheme.skydownAccentHighlight(),
        isActive = isActive,
        onClick = onClick,
    )
}

private fun readableFileSize(bytes: Long): String {
    if (bytes <= 0L) return "Unbekannte Dateigroesse"
    val megabytes = bytes / 1024f / 1024f
    return if (megabytes >= 1024f) {
        String.format(Locale.GERMANY, "%.2f GB", megabytes / 1024f)
    } else {
        String.format(Locale.GERMANY, "%.1f MB", megabytes)
    }
}

private fun formatVideoDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY).format(Date(timestamp))
}

private sealed interface VideoConfigImageTarget {
    data class Equipment(val itemId: String) : VideoConfigImageTarget
    data class Collaboration(val itemId: String) : VideoConfigImageTarget
}
