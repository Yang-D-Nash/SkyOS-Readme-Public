package com.nash.skyos.ui.screen

import android.content.Intent
import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.animation.AnimatedVisibility
import com.nash.skyos.ui.component.SkydownStandardEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import com.nash.skyos.ui.component.SkydownMotionTokens
import com.nash.skyos.ui.component.rememberSkydownReduceMotion
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.nash.skyos.R
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
import androidx.media3.common.Player
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
import com.nash.skyos.ui.component.SkydownFullscreenChromeIconButton
import com.nash.skyos.ui.component.SkydownFullscreenVideoControlBar
import com.nash.skyos.ui.component.isLikelyDirectVideoUrl
import com.nash.skyos.ui.component.seekByAppOffset
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownPremiumSheetDragHandle
import com.nash.skyos.ui.component.SkydownPremiumStatePanel
import com.nash.skyos.ui.component.SkydownPremiumTextField
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.dismissKeyboardOnTap
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownPremiumSheetContainerColor
import com.nash.skyos.ui.component.skydownPremiumSheetContentColor
import com.nash.skyos.ui.component.skydownPremiumSheetScrimColor
import com.nash.skyos.ui.component.skydownPremiumSheetShape
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
    val resources = LocalResources.current
    val defaultOriginalTitle = stringResource(R.string.video_label_original)
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
    var inAppOriginalTitle by rememberSaveable {
        mutableStateOf(resources.getString(R.string.video_label_original))
    }
    var inAppImageUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var shouldAutoplaySelection by rememberSaveable {
        mutableStateOf(autoplayInitialSelection && !initialSelectedVideoId.isNullOrBlank())
    }
    val listState = rememberLazyListState()
    val reduceMotion = rememberSkydownReduceMotion()
    val scrollVideoHubToEquipment: () -> Unit = {
        coroutineScope.launch {
            if (reduceMotion) {
                listState.scrollToItem(2)
            } else {
                listState.animateScrollToItem(2)
            }
        }
    }
    val scrollVideoHubToCollaborations: () -> Unit = {
        coroutineScope.launch {
            if (reduceMotion) {
                listState.scrollToItem(3)
            } else {
                listState.animateScrollToItem(3)
            }
        }
    }
    val fallbackSelectedVideo = uiState.videos.firstOrNull { it.supportsInlinePlayback } ?: uiState.videos.firstOrNull()
    val selectedVideo = uiState.videos.firstOrNull { it.id == selectedVideoId } ?: fallbackSelectedVideo
    val openVideoPlayer: () -> Unit = {
        if (uiState.videos.isNotEmpty()) {
            player.pause()
            showReelViewer = true
        }
    }
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
                    localFeedbackMessage = resources.getString(R.string.video_feedback_image_uploaded)
                    localFeedbackIsError = false
                } else {
                    localFeedbackMessage = result.exceptionOrNull()?.message
                        ?: resources.getString(R.string.video_feedback_image_failed)
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
            val shouldPlayInline = shouldAutoplaySelection && !showReelViewer
            if (shouldAutoplaySelection) {
                shouldAutoplaySelection = false
            }
            if (shouldPlayInline) {
                player.play()
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
            if (autoplayInitialSelection) {
                player.pause()
                showReelViewer = true
            }
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
                        title = stringResource(R.string.video_topbar_title),
                        subtitle = stringResource(R.string.video_topbar_subtitle),
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
                                contentDescription = stringResource(R.string.video_cd_open_upload),
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
                                contentDescription = stringResource(R.string.video_cd_back),
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
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
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
                            contentDescription = stringResource(R.string.video_cd_fab_upload),
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
                .skydownAtmosphereBackground(
                    primaryColor = videoAccent,
                    secondaryColor = videoMysticAccent,
                    primaryAlpha = 0.022f,
                    secondaryAlpha = 0.016f,
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
                        onOpenInAppOriginal = { url, title ->
                            inAppOriginalTitle = title
                            inAppOriginalUrl = url
                        },
                        onOpenVideos = openVideoPlayer,
                        onOpenEquipment = scrollVideoHubToEquipment,
                        onOpenCollaborations = scrollVideoHubToCollaborations,
                    )
                }

                item {
                    VideoPlayerCard(
                        video = selectedVideo,
                        player = player,
                        onOpenOriginal = { video ->
                            val originalUrl = video.inAppOriginalUrl.trim()
                            if (originalUrl.isBlank()) return@VideoPlayerCard
                            inAppOriginalTitle = video.title.ifBlank { defaultOriginalTitle }
                            inAppOriginalUrl = originalUrl
                        },
                        onOpenReel = if (selectedVideo != null) {
                            openVideoPlayer
                        } else {
                            null
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
                    VideoCollaborationsCard(
                        items = uiState.publicConfig.collaborationItems,
                        isAdmin = uiState.isAdmin,
                        onOpenEditor = { showAdminSheet = true },
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
                            inAppOriginalTitle = video.title.ifBlank { defaultOriginalTitle }
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
                    shape = skydownPremiumSheetShape(),
                    containerColor = skydownPremiumSheetContainerColor(),
                    contentColor = skydownPremiumSheetContentColor(),
                    scrimColor = skydownPremiumSheetScrimColor(),
                    tonalElevation = 0.dp,
                    dragHandle = { SkydownPremiumSheetDragHandle() },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
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
                    shape = skydownPremiumSheetShape(),
                    containerColor = skydownPremiumSheetContainerColor(),
                    contentColor = skydownPremiumSheetContentColor(),
                    scrimColor = skydownPremiumSheetScrimColor(),
                    tonalElevation = 0.dp,
                    dragHandle = { SkydownPremiumSheetDragHandle() },
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
                    ) {
                        item {
                            VideoFormatCard()
                        }

                        item {
                            VideoLibraryCard(
                                uiState = uiState,
                                selectedVideoId = selectedVideoId,
                                onSelectVideo = { video -> selectedVideoId = video.id },
                                onOpenReel = { video ->
                                    selectedVideoId = video.id
                                    showAdminSheet = false
                                    player.pause()
                                    showReelViewer = true
                                },
                                onOpenOriginal = { video ->
                                    val originalUrl = video.inAppOriginalUrl.trim()
                                    if (originalUrl.isBlank()) return@VideoLibraryCard
                                    showAdminSheet = false
                                    inAppOriginalTitle = video.title.ifBlank { defaultOriginalTitle }
                                    inAppOriginalUrl = originalUrl
                                },
                                onToggleHomeFeatured = viewModel::toggleHomeFeatured,
                                onDeleteVideo = viewModel::deleteVideo,
                                onEditVideo = viewModel::updateVideo,
                                onCreateVideo = {
                                    showAdminSheet = false
                                    showUploadSheet = true
                                },
                            )
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
                                        localFeedbackMessage = resources.getString(R.string.video_feedback_image_removed)
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
                                        localFeedbackMessage = resources.getString(R.string.video_feedback_image_removed)
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
    onOpenInAppOriginal: (String, String) -> Unit,
    onOpenVideos: () -> Unit,
    onOpenEquipment: () -> Unit,
    onOpenCollaborations: () -> Unit,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val coreAccent = colorScheme.skydownAccent()
    val mysticAccent = colorScheme.skydownAccentMystic()
    val highlightAccent = colorScheme.skydownAccentHighlight()
    val defaultVideoTitle = stringResource(R.string.video_topbar_title)
    val defaultEyebrow = stringResource(R.string.video_eyebrow_default)
    val defaultHeroSubtitle = stringResource(R.string.video_hero_subtitle_default)
    val heroDetailAdmin = stringResource(
        R.string.video_hero_detail_admin,
        videoCount,
        collabCount,
    )
    val heroDetailUser = stringResource(
        R.string.video_hero_detail_user,
        videoCount,
        collabCount,
    )
    val clipsPill = stringResource(R.string.video_hero_pill_clips, videoCount)
    val collabsPill = stringResource(R.string.video_hero_pill_collabs, collabCount)
    val onHeroHeaderClick: () -> Unit = {
        val heroVideoUrl = screenHeaderSettings.videoHubHeroVideoUrl.trim()
        val headerImageUrl = screenHeaderSettings.videoHubImageUrl.trim()
        when {
            heroVideoUrl.isNotEmpty() -> onOpenInAppOriginal(
                heroVideoUrl,
                screenHeaderSettings.videoHubTitle.ifBlank { defaultVideoTitle },
            )
            headerImageUrl.isNotEmpty() && isLikelyDirectVideoUrl(headerImageUrl) -> onOpenInAppOriginal(
                headerImageUrl,
                screenHeaderSettings.videoHubTitle.ifBlank { defaultVideoTitle },
            )
            else -> onOpenVideos()
        }
    }
    BrandHeroCard(
        eyebrow = screenHeaderSettings.videoHubEyebrow.ifBlank { defaultEyebrow },
        title = screenHeaderSettings.videoHubTitle.ifBlank { defaultVideoTitle },
        subtitle = screenHeaderSettings.videoHubSubtitle.ifBlank { defaultHeroSubtitle },
        detail = screenHeaderSettings.videoHubDetail.ifBlank {
            if (isAdmin) heroDetailAdmin else heroDetailUser
        },
        backgroundImageUrl = screenHeaderSettings.videoHubImageUrl.ifBlank { null },
        accent = mysticAccent,
        secondaryAccent = highlightAccent,
        marks = listOf(BrandArtwork.Skydown),
        edgeToEdge = true,
        onSurfaceClick = onHeroHeaderClick,
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            BrandPill(
                text = clipsPill,
                tint = mysticAccent,
                onClick = onOpenVideos,
            )
            BrandPill(
                text = stringResource(R.string.video_hero_pill_equipment),
                tint = coreAccent,
                onClick = onOpenEquipment,
            )
            BrandPill(
                text = collabsPill,
                tint = highlightAccent,
                onClick = onOpenCollaborations,
            )
        }
        Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            VideoHubHeroStatCard(
                label = stringResource(R.string.video_stat_reels),
                value = videoCount.toString(),
                icon = Icons.Default.Movie,
                accent = coreAccent,
                isActive = videoCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = stringResource(R.string.video_stat_collabs),
                value = collabCount.toString(),
                icon = Icons.Default.Sync,
                accent = mysticAccent,
                isActive = collabCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = if (isAdmin) {
                    stringResource(R.string.video_stat_mode)
                } else {
                    stringResource(R.string.video_stat_access)
                },
                value = if (isAdmin) {
                    stringResource(R.string.video_stat_admin)
                } else {
                    stringResource(R.string.video_stat_public)
                },
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
            title = stringResource(R.string.video_admin_title),
            subtitle = stringResource(R.string.video_admin_subtitle),
            accent = accent,
            icon = Icons.Default.Home,
            tag = stringResource(R.string.video_tag_admin),
        )

        BrandActionButton(
            text = stringResource(R.string.video_admin_open_editor),
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
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
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
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
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
            title = stringResource(R.string.video_format_title),
            subtitle = stringResource(R.string.video_format_subtitle),
            icon = Icons.Default.CheckCircle,
            accent = accent,
            tag = stringResource(R.string.video_tag_format),
        )
        Text(
            text = stringResource(R.string.video_format_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            VideoPill(text = stringResource(R.string.video_pill_mp4), isActive = true)
            VideoPill(text = stringResource(R.string.video_pill_916), isActive = false)
            VideoPill(text = stringResource(R.string.video_pill_compressed), isActive = false)
        }
        Text(
            text = stringResource(R.string.video_format_body2),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun VideoCollaborationsCard(
    items: List<ProducedWithArtist>,
    isAdmin: Boolean,
    onOpenEditor: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    val accent = MaterialTheme.colorScheme.skydownAccentMystic()
    SkydownCard {
        VideoHubSectionBanner(
            title = stringResource(R.string.video_collab_featured_title),
            subtitle = stringResource(R.string.video_collab_featured_subtitle),
            icon = Icons.Default.Sync,
            accent = accent,
            tag = stringResource(R.string.video_tag_crew),
        )
        Text(
            text = stringResource(R.string.video_collab_featured_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.video_collab_featured_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 14.dp),
            )
            if (isAdmin) {
                BrandActionButton(
                    text = stringResource(R.string.video_admin_open_editor),
                    onClick = onOpenEditor,
                    accent = accent,
                    modifier = Modifier.padding(top = 12.dp),
                    filled = false,
                )
                Text(
                    text = stringResource(R.string.video_admin_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
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
            title = stringResource(R.string.video_equipment_title),
            subtitle = stringResource(R.string.video_equipment_subtitle),
            icon = Icons.Default.CameraAlt,
            accent = accent,
            tag = stringResource(R.string.video_tag_stack),
        )
        Text(
            text = stringResource(R.string.video_equipment_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = stringResource(R.string.video_equipment_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 14.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
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
    onClick: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colorScheme = MaterialTheme.colorScheme
    val accent = colorScheme.skydownAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
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
                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
            )
            .skydownPressable(
                interactionSource = interactionSource,
                pressedScale = 0.988f,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
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
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
        ) {
            VideoPill(text = stringResource(R.string.video_pill_visual_stack), isActive = true)
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
        shape = skydownPremiumSheetShape(),
        containerColor = skydownPremiumSheetContainerColor(),
        contentColor = skydownPremiumSheetContentColor(),
        scrimColor = skydownPremiumSheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { SkydownPremiumSheetDragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            VideoPill(text = stringResource(R.string.video_pill_visual_stack), isActive = true)
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

                BrandActionButton(
                    text = stringResource(R.string.video_image_fullscreen),
                    onClick = { onOpenImageFullscreen(item.imageUrl.orEmpty()) },
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    filled = false,
                )
            }

            BrandActionButton(
                text = stringResource(R.string.common_close),
                onClick = onDismiss,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
            )
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
    val collabRoleDefault = stringResource(R.string.video_label_collab_default)
    SkydownCard {
        VideoHubSectionBanner(
            title = stringResource(R.string.video_control_title),
            subtitle = stringResource(R.string.video_control_subtitle),
            icon = Icons.Default.Sync,
            accent = adminAccent,
            tag = stringResource(R.string.video_tag_admin),
        )
        Text(
            text = stringResource(R.string.video_control_body1),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = stringResource(R.string.video_control_body2),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            VideoPill(text = stringResource(R.string.video_pill_picker_first), isActive = true)
            VideoPill(text = stringResource(R.string.video_pill_public_save), isActive = false)
            VideoPill(text = stringResource(R.string.video_pill_admin_only), isActive = false)
        }

        VideoControlDeckCard(
            title = stringResource(R.string.video_equip_library_title),
            subtitle = stringResource(R.string.video_equip_library_subtitle),
            icon = Icons.Default.CameraAlt,
            accent = equipmentAccent,
            tag = stringResource(R.string.video_tag_stack),
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
            ) {
                uiState.publicConfig.equipmentItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
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
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                            VideoPill(text = stringResource(R.string.video_pill_gear), isActive = true)
                            VideoPill(
                                text = if (item.imageUrl.isNullOrBlank()) {
                                    stringResource(R.string.video_pill_no_image)
                                } else {
                                    stringResource(R.string.video_pill_image_ready)
                                },
                                isActive = false,
                            )
                        }
                        SkydownPremiumTextField(
                            value = item.title,
                            onValueChange = { onUpdateEquipmentTitle(item.id, it) },
                            label = stringResource(R.string.video_field_title),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SkydownPremiumTextField(
                            value = item.detail,
                            onValueChange = { onUpdateEquipmentDetail(item.id, it) },
                            label = stringResource(R.string.video_field_detail),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                        )
                        EditableImageFieldCard(
                            title = stringResource(R.string.video_equipment_image_title),
                            imageUrl = item.imageUrl.orEmpty(),
                            isUploading = activeImageUploadTarget == VideoConfigImageTarget.Equipment(item.id),
                            uploadStatusText = stringResource(R.string.video_equipment_image_uploading),
                            onPickImage = { onPickEquipmentImage(item.id) },
                            onImageUrlChange = { onUpdateEquipmentImageUrl(item.id, it) },
                            onRemoveImage = { onRemoveEquipmentImage(item.id) },
                        )
                        BrandActionButton(
                            text = stringResource(R.string.video_remove_entry),
                            onClick = { onRemoveEquipment(item.id) },
                            accent = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            filled = false,
                        )
                    }
                }
            }

            BrandActionButton(
                text = stringResource(R.string.video_add_equipment),
                onClick = onAddEquipment,
                accent = equipmentAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                filled = false,
            )
        }

        VideoControlDeckCard(
            title = stringResource(R.string.video_collab_profiles_title),
            subtitle = stringResource(R.string.video_collab_profiles_subtitle),
            icon = Icons.Default.CheckCircle,
            accent = collabAccent,
            tag = stringResource(R.string.video_tag_crew),
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
            ) {
                uiState.publicConfig.collaborationItems.forEach { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
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
                                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                            VideoPill(
                                text = item.role.ifBlank { collabRoleDefault },
                                isActive = true,
                            )
                            if (item.vibe.isNotBlank()) {
                                VideoPill(text = item.vibe, isActive = false)
                            }
                        }
                        SkydownPremiumTextField(
                            value = item.name,
                            onValueChange = { onUpdateCollaborationName(item.id, it) },
                            label = stringResource(R.string.video_field_name),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SkydownPremiumTextField(
                            value = item.role,
                            onValueChange = { onUpdateCollaborationRole(item.id, it) },
                            label = stringResource(R.string.video_field_role),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SkydownPremiumTextField(
                            value = item.highlight,
                            onValueChange = { onUpdateCollaborationHighlight(item.id, it) },
                            label = stringResource(R.string.video_field_highlight),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = false,
                            minLines = 2,
                        )
                        SkydownPremiumTextField(
                            value = item.vibe,
                            onValueChange = { onUpdateCollaborationVibe(item.id, it) },
                            label = stringResource(R.string.video_field_vibe),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        EditableImageFieldCard(
                            title = stringResource(R.string.video_collab_image_title),
                            imageUrl = item.imageUrl.orEmpty(),
                            isUploading = activeImageUploadTarget == VideoConfigImageTarget.Collaboration(item.id),
                            uploadStatusText = stringResource(R.string.video_collab_image_uploading),
                            onPickImage = { onPickCollaborationImage(item.id) },
                            onImageUrlChange = { onUpdateCollaborationImageUrl(item.id, it) },
                            onRemoveImage = { onRemoveCollaborationImage(item.id) },
                        )
                        SkydownPremiumTextField(
                            value = item.spotifyArtistId.orEmpty(),
                            onValueChange = { onUpdateCollaborationSpotifyArtistId(item.id, it) },
                            label = stringResource(R.string.video_field_spotify_artist_id),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SkydownPremiumTextField(
                            value = item.instagramUrl.orEmpty(),
                            onValueChange = { onUpdateCollaborationInstagramUrl(item.id, it) },
                            label = stringResource(R.string.video_field_instagram_url),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SkydownPremiumTextField(
                            value = item.youtubeUrl.orEmpty(),
                            onValueChange = { onUpdateCollaborationYoutubeUrl(item.id, it) },
                            label = stringResource(R.string.video_field_youtube_url),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        BrandActionButton(
                            text = stringResource(R.string.video_remove_collab),
                            onClick = { onRemoveCollaboration(item.id) },
                            accent = MaterialTheme.colorScheme.error,
                            modifier = Modifier.fillMaxWidth(),
                            filled = false,
                        )
                    }
                }
            }

            BrandActionButton(
                text = stringResource(R.string.video_add_collab),
                onClick = onAddCollaboration,
                accent = collabAccent,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                filled = false,
            )
        }

        BrandActionButton(
            text = stringResource(R.string.video_save_changes),
            onClick = onSave,
            accent = adminAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            isLoading = uiState.isSavingPublicConfig,
            enabled = !uiState.isSavingPublicConfig,
        )
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
    val collabRoleDefault = stringResource(R.string.video_label_collab_default)
    val spotifyTitle = stringResource(R.string.video_artist_social_spotify)
    val instagramTitle = stringResource(R.string.video_artist_social_instagram)
    val youtubeTitle = stringResource(R.string.video_artist_social_youtube)
    val socialPendingTitle = stringResource(R.string.video_artist_social_pending)
    val highlightPendingTitle = stringResource(R.string.video_collab_highlight_pending)
    val roleLabel = artist.role.ifBlank { collabRoleDefault }
    val hasSocialLinks = !artist.spotifyArtistId.isNullOrBlank() ||
        !artist.instagramUrl.isNullOrBlank() ||
        !artist.youtubeUrl.isNullOrBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.messageBubbleRadius))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.skydownCardBackground().copy(alpha = 0.98f),
                        collabAccent.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 0.8.dp,
                color = collabAccent.copy(alpha = 0.10f),
                shape = RoundedCornerShape(SkydownUiTokens.messageBubbleRadius),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            collabAccent.copy(alpha = 0.80f),
                            colorScheme.skydownAccent().copy(alpha = 0.70f),
                        ),
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!artist.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artist.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = artist.name.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (artist.vibe.isNotBlank()) {
                    "$roleLabel · ${artist.vibe}"
                } else {
                    roleLabel
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = artist.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Text(
                text = if (artist.highlight.isNotBlank()) artist.highlight else highlightPendingTitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            ) {
                artist.spotifyArtistId?.takeIf { it.isNotBlank() }?.let { spotifyArtistId ->
                    SocialActionChip(
                        title = spotifyTitle,
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
                        title = instagramTitle,
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
                        title = youtubeTitle,
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
                if (!hasSocialLinks) {
                    VideoPill(text = socialPendingTitle, isActive = false)
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

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
            .background(gradient)
            .heightIn(min = 28.dp)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                contentDescription = title
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .skydownPressable(interactionSource)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
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
    val res = LocalContext.current.resources
    val uploadAccent = colorScheme.skydownAccentHighlight()
    val pickerAccent = colorScheme.skydownAccent()
    SkydownCard {
        VideoHubSectionBanner(
            title = stringResource(R.string.video_upload_title),
            subtitle = stringResource(R.string.video_upload_subtitle),
            icon = Icons.Default.Movie,
            accent = uploadAccent,
            tag = stringResource(R.string.video_tag_upload),
        )
        Text(
            text = stringResource(R.string.video_upload_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
        ) {
            VideoPill(text = stringResource(R.string.video_pill_storage), isActive = true)
            VideoPill(text = stringResource(R.string.video_pill_video_link), isActive = false)
            VideoPill(text = stringResource(R.string.video_pill_external), isActive = false)
        }

        SkydownPremiumTextField(
            value = uiState.videoTitle,
            onValueChange = onUpdateTitle,
            label = stringResource(R.string.video_field_title),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
        )
        SkydownPremiumTextField(
            value = uiState.projectName,
            onValueChange = onUpdateProjectName,
            label = stringResource(R.string.video_field_project_artist),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        SkydownPremiumTextField(
            value = uiState.email,
            onValueChange = onUpdateEmail,
            label = stringResource(R.string.video_field_contact_email),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
        )
        SkydownPremiumTextField(
            value = uiState.notes,
            onValueChange = onUpdateNotes,
            label = stringResource(R.string.video_field_note),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            singleLine = false,
            minLines = 3,
        )

        BrandActionButton(
            text = stringResource(R.string.video_select_videos),
            onClick = onPickFiles,
            accent = pickerAccent,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            icon = Icons.Default.Movie,
            filled = false,
        )

        Text(
            text = stringResource(R.string.video_upload_or_external),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 14.dp),
        )

        SkydownPremiumTextField(
            value = uiState.externalVideoUrl,
            onValueChange = onUpdateExternalVideoUrl,
            label = stringResource(R.string.video_field_external_url_label),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            singleLine = false,
            minLines = 2,
        )

        if (uiState.selectedFiles.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            ) {
                uiState.selectedFiles.forEach { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SkydownUiTokens.denseRadius))
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
                                shape = RoundedCornerShape(SkydownUiTokens.denseRadius),
                            )
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
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
                                text = readableFileSize(file.fileSizeBytes, res),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                            )
                        }
                        BrandActionButton(
                            text = stringResource(R.string.video_action_remove),
                            onClick = { onRemoveFile(file) },
                            accent = MaterialTheme.colorScheme.error,
                            filled = false,
                            compact = true,
                        )
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
            text = if (uiState.isUploading) {
                stringResource(R.string.video_upload_in_progress)
            } else {
                stringResource(R.string.video_upload_cta)
            },
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
            text = stringResource(R.string.video_add_external),
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
    val homeBadge = stringResource(R.string.common_home_badge)
    SkydownCard {
        VideoHubSectionBanner(
            title = stringResource(R.string.video_player_section_title),
            subtitle = stringResource(R.string.video_player_subtitle),
            icon = Icons.Default.Movie,
            accent = playerAccent,
            tag = stringResource(R.string.video_tag_live),
        )

        Text(
            text = stringResource(R.string.video_player_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (video == null) {
            Text(
                text = stringResource(R.string.video_player_empty),
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
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingSubtle),
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
                                append(" · ")
                                append(homeBadge)
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

        if (video.supportsInlinePlayback) {
            onOpenReel?.let { openReel ->
                BrandActionButton(
                    text = stringResource(R.string.video_player_open),
                    onClick = openReel,
                    accent = playerAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video.hub.player.open")
                        .padding(top = 14.dp),
                    icon = Icons.Default.PlayArrow,
                    filled = true,
                )
            }
        } else if (video.opensOriginalInApp) {
            BrandActionButton(
                text = video.directOpenActionLabel,
                onClick = { onOpenOriginal(video) },
                accent = videoHubProviderAccent(video),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("video.hub.player.original.open")
                    .padding(top = 14.dp),
                icon = Icons.Default.Language,
                filled = true,
            )
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

@Composable
@ReadOnlyComposable
private fun videoHubLibraryMetaLineText(
    video: VideoHubItem,
    isAdmin: Boolean,
    presentation: VideoLibraryRowPresentation,
): String {
    val date = formatVideoDate(video.createdAtMillis)
    val public = stringResource(R.string.common_public)
    val private = stringResource(R.string.common_private)
    val home = stringResource(R.string.common_home_badge)
    return when (presentation) {
        VideoLibraryRowPresentation.Catalog -> {
            "${video.projectName} · $date"
        }
        VideoLibraryRowPresentation.Featured -> {
            buildString {
                append(video.projectName)
                append(" · ")
                append(date)
                if (isAdmin) {
                    append(" · ")
                    append(if (video.isPublic) public else private)
                    if (video.isHomeFeatured) {
                        append(" · ")
                        append(home)
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
                    append(if (video.isPublic) public else private)
                    if (video.isHomeFeatured) {
                        append(" · ")
                        append(home)
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
    onEditVideo: (VideoHubItem, String, String, String, Boolean) -> Unit,
    onCreateVideo: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var editingVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    SkydownCard {
        Column(
            modifier = Modifier.testTag("video.hub.library.header"),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
        ) {
            Text(
                text = if (uiState.isAdmin) {
                    stringResource(R.string.video_lib_header_owner)
                } else {
                    stringResource(
                        R.string.video_lib_header_guest,
                        uiState.videos.size,
                    )
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
            )
            Text(
                text = if (uiState.isAdmin) {
                    stringResource(
                        R.string.video_lib_subtitle_admin,
                        uiState.videos.size,
                    )
                } else {
                    stringResource(
                        R.string.video_lib_subtitle_guest,
                        uiState.videos.size,
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.64f),
            )
            if (uiState.isAdmin) {
                BrandActionButton(
                    text = stringResource(R.string.video_lib_new_video),
                    onClick = onCreateVideo,
                    accent = colorScheme.skydownAccentMystic(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("video.hub.owner.create"),
                    icon = Icons.Default.Movie,
                )
            }
        }

        when {
            uiState.isLoadingVideos -> {
                SkydownPremiumStatePanel(
                    title = stringResource(R.string.video_lib_loading),
                    body = stringResource(R.string.video_lib_subtitle_guest, 0),
                    icon = Icons.Default.Sync,
                    accent = colorScheme.skydownAccentMystic(),
                    loading = true,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            uiState.videos.isEmpty() -> {
                SkydownPremiumStatePanel(
                    title = stringResource(R.string.video_lib_header_guest, 0),
                    body = stringResource(R.string.video_lib_empty),
                    icon = Icons.Default.Movie,
                    accent = colorScheme.skydownAccentMystic(),
                    actionLabel = if (uiState.isAdmin) stringResource(R.string.video_lib_new_video) else null,
                    onAction = if (uiState.isAdmin) onCreateVideo else null,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(top = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
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
                            onEdit = { editingVideoId = video.id },
                            modifier = Modifier
                                .padding(top = topPad)
                                .padding(bottom = bottomAfterFeatured),
                        )
                        if (uiState.isAdmin && editingVideoId == video.id) {
                            VideoOwnerEditPanel(
                                video = video,
                                onCancel = { editingVideoId = null },
                                onSave = { title, projectName, notes, isPublic ->
                                    onEditVideo(video, title, projectName, notes, isPublic)
                                    editingVideoId = null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoOwnerEditPanel(
    video: VideoHubItem,
    onCancel: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var title by rememberSaveable(video.id) { mutableStateOf(video.title) }
    var projectName by rememberSaveable(video.id) { mutableStateOf(video.projectName) }
    var notes by rememberSaveable(video.id) { mutableStateOf(video.notes) }
    var isPublic by rememberSaveable(video.id) { mutableStateOf(video.isPublic) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(SkydownUiTokens.cardCornerRadius))
            .background(colorScheme.skydownSecondaryBackground().copy(alpha = 0.72f))
            .border(
                width = 1.dp,
                color = colorScheme.skydownAccentMystic().copy(alpha = 0.22f),
                shape = RoundedCornerShape(SkydownUiTokens.cardCornerRadius),
            )
            .padding(14.dp)
            .testTag("video.hub.owner.edit"),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
    ) {
        Text(
            text = stringResource(R.string.video_edit_heading),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface,
        )
        Text(
            text = stringResource(
                R.string.video_edit_provider_date,
                video.providerBadge,
                formatVideoDate(video.createdAtMillis),
            ),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        SkydownPremiumTextField(
            value = title,
            onValueChange = { title = it.take(120) },
            label = stringResource(R.string.video_field_title),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        SkydownPremiumTextField(
            value = projectName,
            onValueChange = { projectName = it.take(120) },
            label = stringResource(R.string.video_field_project_artist),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        SkydownPremiumTextField(
            value = notes,
            onValueChange = { notes = it.take(800) },
            label = stringResource(R.string.video_field_notes),
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            minLines = 2,
            maxLines = 4,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
            BrandActionButton(
                text = if (isPublic) {
                    stringResource(R.string.common_public)
                } else {
                    stringResource(R.string.common_private)
                },
                onClick = { isPublic = !isPublic },
                accent = colorScheme.skydownAccentMystic(),
                modifier = Modifier.weight(1f),
                icon = if (isPublic) Icons.Default.CheckCircle else Icons.Default.Close,
                filled = false,
            )
            BrandActionButton(
                text = stringResource(R.string.video_action_save),
                onClick = { onSave(title, projectName, notes, isPublic) },
                accent = colorScheme.skydownAccent(),
                modifier = Modifier.weight(1f),
            )
        }
        BrandActionButton(
            text = stringResource(R.string.video_action_cancel),
            onClick = onCancel,
            accent = colorScheme.primary,
            modifier = Modifier.align(Alignment.End),
            filled = false,
            compact = true,
        )
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
    onEdit: () -> Unit,
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
        VideoLibraryRowPresentation.Featured -> RoundedCornerShape(SkydownUiTokens.sheetHeroRadius)
        VideoLibraryRowPresentation.Secondary -> RoundedCornerShape(SkydownUiTokens.cardCornerRadius)
        VideoLibraryRowPresentation.Catalog -> RoundedCornerShape(SkydownUiTokens.catalogCornerRadius)
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
            verticalArrangement = Arrangement.spacedBy(if (isFeatured) SkydownUiTokens.stackSpacingPill else if (catalog) SkydownUiTokens.stackSpacingDense else SkydownUiTokens.stackSpacingMicro),
        ) {
            if (!isFeatured) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(if (catalog) SkydownUiTokens.stackSpacingChrome else SkydownUiTokens.stackSpacingCompact),
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
                                        .clip(RoundedCornerShape(SkydownUiTokens.compactRadius)),
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
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
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
                            text = videoHubLibraryMetaLineText(
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
                    text = videoHubLibraryMetaLineText(
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
                    isSecondary && isAdmin -> stringResource(R.string.video_row_hint_secondary_admin)
                    isSecondary -> stringResource(R.string.video_row_hint_secondary_user)
                    else -> videoLibraryInteractionHintText(
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
                    BrandActionButton(
                        text = when {
                            isSelected && video.usesEmbeddedPreview -> stringResource(R.string.video_btn_active_in_preview)
                            isSelected -> stringResource(R.string.video_btn_active_in_player)
                            video.usesEmbeddedPreview -> stringResource(R.string.video_btn_load_in_preview)
                            else -> stringResource(R.string.video_btn_load_in_player)
                        },
                        onClick = onSelect,
                        accent = providerAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (video.openUrl.isNotBlank()) {
                    BrandActionButton(
                        text = video.originalActionLabel,
                        onClick = onOpenOriginal,
                        accent = providerAccent,
                        modifier = Modifier.fillMaxWidth(),
                        filled = false,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                    BrandActionButton(
                        text = if (video.isHomeFeatured) {
                            stringResource(R.string.video_btn_home_on)
                        } else {
                            stringResource(R.string.video_btn_home_add)
                        },
                        onClick = onToggleHomeFeatured,
                        accent = colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Home,
                        filled = false,
                    )

                    BrandActionButton(
                        text = stringResource(R.string.video_action_edit),
                        onClick = onEdit,
                        accent = colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Sync,
                        filled = false,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill)) {
                    BrandActionButton(
                        text = stringResource(R.string.video_action_delete),
                        onClick = onDelete,
                        accent = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Default.Delete,
                        filled = false,
                    )
                }
            } else {
                if (video.supportsInlinePlayback) {
                    BrandActionButton(
                        text = videoHubInlineCompactActionLabelText(video),
                        onClick = onOpenReel,
                        accent = providerAccent,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else if (video.opensOriginalInApp) {
                    BrandActionButton(
                        text = video.originalActionLabel,
                        onClick = onOpenOriginal,
                        accent = providerAccent,
                        modifier = Modifier.fillMaxWidth(),
                        filled = false,
                    )
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun videoLibraryInteractionHintText(
    video: VideoHubItem,
    isAdmin: Boolean,
    isSelected: Boolean,
): String {
    if (isAdmin) {
        if (video.isPlayable) {
            return if (isSelected) {
                stringResource(R.string.video_row_hint_admin_selected)
            } else {
                stringResource(R.string.video_row_hint_admin_tap)
            }
        }
        if (video.supportsInlinePlayback) {
            return stringResource(R.string.video_row_hint_inline_reel)
        }
        if (video.opensOriginalInApp) {
            return stringResource(R.string.video_row_hint_original_in_app)
        }
        return stringResource(R.string.video_row_hint_external_only)
    }

    if (video.supportsInlinePlayback) {
        return stringResource(R.string.video_row_hint_user_inline)
    }
    if (video.opensOriginalInApp) {
        return stringResource(R.string.video_row_hint_user_original_in_app)
    }
    return stringResource(R.string.video_row_hint_user_external)
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
    val reelTitlePreview = stringResource(R.string.video_reel_title_preview)
    val reelTitleReel = stringResource(R.string.video_reel_title_reel)
    val reelTitleVideo = stringResource(R.string.video_reel_title_video)
    val initialPage = remember(videos, selectedVideoId) {
        videos.indexOfFirst { it.id == selectedVideoId }.takeIf { it >= 0 } ?: 0
    }
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videos.size.coerceAtLeast(1) },
    )
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = rememberSkydownReduceMotion()
    var isTransitioning by remember { mutableStateOf(true) }
    var isVideoPlaying by remember { mutableStateOf(false) }
    val currentVideo = videos.getOrNull(pagerState.currentPage)
    val currentVideoSupportsPlayback =
        currentVideo != null && !currentVideo.usesEmbeddedPreview && currentVideo.nativePlaybackUrl.isNotBlank()

    LaunchedEffect(pagerState.currentPage, videos, reduceMotion) {
        val video = videos.getOrNull(pagerState.currentPage)
        video?.let(onSelectVideo)
        isTransitioning = true
        val url = video?.nativePlaybackUrl
        if (video?.usesEmbeddedPreview == true || url.isNullOrBlank()) {
            reelPlayer.stop()
            reelPlayer.clearMediaItems()
            isVideoPlaying = false
        } else {
            reelPlayer.setMediaItem(MediaItem.fromUri(url))
            reelPlayer.prepare()
            reelPlayer.play()
            isVideoPlaying = true
        }
        delay(if (reduceMotion) 0 else 220)
        isTransitioning = false
    }

    DisposableEffect(reelPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                isVideoPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    isVideoPlaying = false
                }
            }
        }
        reelPlayer.addListener(listener)
        onDispose {
            reelPlayer.removeListener(listener)
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
                .testTag("video.reel.viewer.root")
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
                                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
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
                                .padding(start = 22.dp, end = 90.dp, bottom = 30.dp),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
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
                            if (it.opensOriginalInApp && !it.supportsInlinePlayback) {
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
                            shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                        )
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                ) {
                    videos.forEachIndexed { index, _ ->
                        val isActive = index == pagerState.currentPage
                        Box(
                            modifier = Modifier
                                .size(width = 4.dp, height = if (isActive) 28.dp else 9.dp)
                                .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
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
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = videos
                            .getOrNull(pagerState.currentPage)
                            ?.let { video ->
                                if (video.usesEmbeddedPreview) reelTitlePreview else reelTitleReel
                            }
                            ?: reelTitleVideo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f),
                    )
                    Text(
                        text = stringResource(
                            R.string.video_reel_page_of,
                            pagerState.currentPage + 1,
                            videos.size,
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.70f),
                    )
                    if (videos.size > 1) {
                        Text(
                            text = stringResource(R.string.video_reel_swipe_hint),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.58f),
                        )
                    }
                }

                SkydownFullscreenChromeIconButton(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.video_cd_close_video),
                    onClick = onDismiss,
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 18.dp, bottom = 82.dp),
            ) {
                SkydownFullscreenVideoControlBar(
                    isPlaying = isVideoPlaying,
                    playbackControlsEnabled = currentVideoSupportsPlayback,
                    showsClipNavigation = videos.size > 1,
                    canGoToPreviousClip = pagerState.currentPage > 0,
                    canGoToNextClip = pagerState.currentPage < videos.lastIndex,
                    onPreviousClip = {
                        val targetPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                        coroutineScope.launch {
                            if (reduceMotion) {
                                pagerState.scrollToPage(targetPage)
                            } else {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    },
                    onRewind = { reelPlayer.seekByAppOffset(-10_000L) },
                    onPlayPause = {
                        if (currentVideoSupportsPlayback) {
                            if (isVideoPlaying) {
                                reelPlayer.pause()
                            } else {
                                reelPlayer.play()
                            }
                            isVideoPlaying = reelPlayer.isPlaying
                        }
                    },
                    onForward = { reelPlayer.seekByAppOffset(10_000L) },
                    onNextClip = {
                        val targetPage = (pagerState.currentPage + 1).coerceAtMost(videos.lastIndex)
                        coroutineScope.launch {
                            if (reduceMotion) {
                                pagerState.scrollToPage(targetPage)
                            } else {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    },
                    onClose = onDismiss,
                )
            }

            AnimatedVisibility(
                visible = isTransitioning,
                modifier = Modifier.align(Alignment.Center),
                enter = fadeIn(
                    animationSpec = if (reduceMotion) {
                        snap()
                    } else {
                        tween(
                            durationMillis = SkydownMotionTokens.statusEnterDurationMillis,
                            easing = SkydownStandardEasing,
                        )
                    },
                ),
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
                        .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.48f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = stringResource(R.string.video_reel_preparing),
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

@Composable
@ReadOnlyComposable
private fun videoHubInlineCompactActionLabelText(video: VideoHubItem): String {
    return if (video.usesEmbeddedPreview) {
        stringResource(R.string.video_inline_in_preview)
    } else {
        stringResource(R.string.video_inline_in_video)
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
                horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingComfortable),
            ) {
                Text(
                    text = stringResource(R.string.video_image_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.weight(1f),
                )

                BrandActionButton(
                    text = stringResource(R.string.common_close),
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

private fun readableFileSize(bytes: Long, res: Resources): String {
    if (bytes <= 0L) return res.getString(R.string.common_unknown_file_size)
    val megabytes = bytes / 1024f / 1024f
    return if (megabytes >= 1024f) {
        res.getString(R.string.video_size_gb, megabytes / 1024f)
    } else {
        res.getString(R.string.video_size_mb, megabytes)
    }
}

private fun formatVideoDate(timestamp: Long): String {
    return SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
}

private sealed interface VideoConfigImageTarget {
    data class Equipment(val itemId: String) : VideoConfigImageTarget
    data class Collaboration(val itemId: String) : VideoConfigImageTarget
}
