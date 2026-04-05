package com.skydown.android.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.AppContainer
import com.skydown.android.data.mediaAttributionContext
import com.skydown.android.data.resolveYouTubeEmbedUrl
import com.skydown.android.data.resolveYouTubeThumbnailUrl
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.EditableImageFieldCard
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandHeroMetricCard
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.YouTubePlayerDialog
import com.skydown.android.ui.component.dismissKeyboardOnTap
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.ProducedWithArtist
import com.skydown.android.ui.model.VideoEquipmentItem
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.model.VideoYouTubeItem
import com.skydown.android.ui.theme.ArenaGold
import com.skydown.android.ui.theme.ArenaRed
import com.skydown.android.ui.theme.DexBlue
import com.skydown.android.ui.theme.DexBlueDeep
import com.skydown.android.ui.theme.FieldMint
import com.skydown.android.ui.theme.InstagramOrange
import com.skydown.android.ui.theme.InstagramPink
import com.skydown.android.ui.theme.InstagramPurple
import com.skydown.android.ui.theme.SpotifyGreen
import com.skydown.android.ui.theme.YouTubeDeepRed
import com.skydown.android.ui.theme.YouTubeRed
import com.skydown.android.ui.viewmodel.VideoHubViewModel
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
    viewModel: VideoHubViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
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
    var selectedYouTubeItem by remember { mutableStateOf<VideoYouTubeItem?>(null) }
    var showReelViewer by rememberSaveable { mutableStateOf(false) }
    var showUploadSheet by rememberSaveable { mutableStateOf(false) }
    var pendingConfigImageTarget by remember { mutableStateOf<VideoConfigImageTarget?>(null) }
    var activeConfigImageUploadTarget by remember { mutableStateOf<VideoConfigImageTarget?>(null) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var localFeedbackIsError by remember { mutableStateOf(false) }
    var shouldAutoplaySelection by rememberSaveable {
        mutableStateOf(autoplayInitialSelection && !initialSelectedVideoId.isNullOrBlank())
    }
    val listState = rememberLazyListState()
    val selectedVideo = uiState.videos.firstOrNull { it.id == selectedVideoId } ?: uiState.videos.firstOrNull()
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
            selectedVideoId = uiState.videos.firstOrNull()?.id
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

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Video",
                        subtitle = "Reels, Clips, Kollabos.",
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
                .navigationBarsPadding()
                .imePadding()
                .dismissKeyboardOnTap(onDismissKeyboard = dismissKeyboard)
                .background(
                    skydownScreenBrush(
                        primaryColor = DexBlue,
                        secondaryColor = YouTubeRed,
                        primaryAlpha = 0.16f,
                        secondaryAlpha = 0.10f,
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    VideoHubHeroCard(
                        isAdmin = uiState.isAdmin,
                        videoCount = uiState.videos.size,
                        youtubeCount = uiState.publicConfig.youtubeItems.size,
                        collabCount = uiState.publicConfig.collaborationItems.size,
                    )
                }

                item {
                    VideoYouTubeCard(
                        items = uiState.publicConfig.youtubeItems,
                        onPlayItem = { item -> selectedYouTubeItem = item },
                    )
                }

                item {
                    VideoEquipmentCard(items = uiState.publicConfig.equipmentItems)
                }

                item {
                    VideoPlayerCard(
                        video = selectedVideo,
                        player = player,
                        onOpenOriginal = { url -> openExternalLink(context, url) },
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
                        onOpenOriginal = { url -> openExternalLink(context, url) },
                        onToggleHomeFeatured = viewModel::toggleHomeFeatured,
                        onDeleteVideo = viewModel::deleteVideo,
                    )
                }

                item {
                    VideoCollaborationsCard(
                        items = uiState.publicConfig.collaborationItems,
                        onOpenLink = { url -> openExternalLink(context, url) },
                        onOpenYouTube = { item -> selectedYouTubeItem = item },
                    )
                }

                if (uiState.isAdmin) {
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
                            onAddYouTube = viewModel::addYouTubeItem,
                            onAddYouTubeBatch = viewModel::addYouTubeItemsFromText,
                            onUpdateYouTubeTitle = { itemId, value ->
                                viewModel.updateYouTubeItem(itemId, title = value)
                            },
                            onUpdateYouTubeSubtitle = { itemId, value ->
                                viewModel.updateYouTubeItem(itemId, subtitle = value)
                            },
                            onUpdateYouTubeUrl = { itemId, value ->
                                viewModel.updateYouTubeItem(itemId, url = value)
                            },
                            onRemoveYouTube = viewModel::removeYouTubeItem,
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
                            onUpdateCollaborationYouTubeUrl = { itemId, value ->
                                viewModel.updateCollaborationItem(itemId, youtubeUrl = value)
                            },
                            onRemoveCollaboration = viewModel::removeCollaborationItem,
                            onSave = viewModel::savePublicConfig,
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
                        top = innerPadding.calculateTopPadding() + com.skydown.android.ui.component.SkydownUiTokens.screenTopPadding,
                    ),
            )

            if (showReelViewer && uiState.videos.isNotEmpty()) {
                VideoReelViewerDialog(
                    videos = uiState.videos,
                    selectedVideoId = selectedVideoId,
                    onSelectVideo = { video -> selectedVideoId = video.id },
                    onDismiss = { showReelViewer = false },
                )
            }

            selectedYouTubeItem?.let { item ->
                YouTubePlayerDialog(
                    item = item,
                    onDismiss = { selectedYouTubeItem = null },
                    onOpenExternal = { url -> openExternalLink(context, url) },
                )
            }

            if (showUploadSheet && uiState.isAdmin) {
                ModalBottomSheet(
                    onDismissRequest = { showUploadSheet = false },
                    containerColor = MaterialTheme.colorScheme.background,
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 36.dp),
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
        }
    }
}

@Composable
private fun VideoHubHeroCard(
    isAdmin: Boolean,
    videoCount: Int,
    youtubeCount: Int,
    collabCount: Int,
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    BrandHeroCard(
        eyebrow = screenHeaderSettings.videoHubEyebrow.ifBlank { "SKY²²" },
        title = screenHeaderSettings.videoHubTitle.ifBlank { "Video" },
        subtitle = screenHeaderSettings.videoHubSubtitle.ifBlank { "Reels, YouTube und Collabs im visuellen Street-Flow." },
        detail = screenHeaderSettings.videoHubDetail.ifBlank {
            if (isAdmin) {
                "$videoCount Clips, $youtubeCount YouTube-Links und $collabCount Collabs live."
            } else {
                "$videoCount Clips und $youtubeCount YouTube-Links im Visual Hub."
            }
        },
        backgroundImageUrl = screenHeaderSettings.videoHubImageUrl.ifBlank { null },
        accent = ArenaGold,
        secondaryAccent = ArenaRed,
        marks = listOf(BrandArtwork.Skydown),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = "$videoCount Clips", tint = ArenaGold)
            BrandPill(text = "$youtubeCount YouTube", tint = YouTubeRed)
            BrandPill(text = "$collabCount Collabs", tint = FieldMint)
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            VideoHubHeroStatCard(
                label = "Reels",
                value = videoCount.toString(),
                icon = Icons.Default.Movie,
                accent = ArenaGold,
                isActive = videoCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = "YouTube",
                value = youtubeCount.toString(),
                icon = Icons.Default.PlayArrow,
                accent = YouTubeRed,
                isActive = youtubeCount > 0,
                modifier = Modifier.weight(1f),
            )
            VideoHubHeroStatCard(
                label = if (isAdmin) "Mode" else "Access",
                value = if (isAdmin) "Admin" else "Public",
                icon = Icons.Default.Sync,
                accent = FieldMint,
                isActive = isAdmin || collabCount > 0 || videoCount > 0 || youtubeCount > 0,
                modifier = Modifier.weight(1f),
            )
        }
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
private fun VideoHubPreviewFrame(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        ArenaGold.copy(alpha = 0.12f),
                        DexBlue.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.24f),
                shape = RoundedCornerShape(22.dp),
            )
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)),
        ) {
            content()
        }
    }
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        accent.copy(alpha = 0.10f),
                        DexBlue.copy(alpha = 0.06f),
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Format Check",
            subtitle = "Saubere Uploads halten den Hub schnell und klar.",
            icon = Icons.Default.CheckCircle,
            accent = ArenaGold,
            tag = "FORMAT",
        )
        Text(
            text = "MP4, MOV oder M4V bleiben die stabilsten Formate fuer den Player und den Reel-Flow.",
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
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Featured Collabs",
            subtitle = "Artists und Creatives hinter dem visuellen Vibe.",
            icon = Icons.Default.Sync,
            accent = FieldMint,
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
                text = "Noch keine Featured Collabs hinterlegt.",
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
                        onOpenYouTube = onOpenYouTube,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoEquipmentCard(
    items: List<VideoEquipmentItem>,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Equipment & Software",
            subtitle = "Setup fuer Shoot, Edit und Finish.",
            icon = Icons.Default.CameraAlt,
            accent = DexBlue,
            tag = "SETUP",
        )
        Text(
            text = "Das aktuelle Setup zeigt, womit die Visuals gebaut und veredelt werden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = "Noch kein Setup hinterlegt.",
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
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoYouTubeCard(
    items: List<VideoYouTubeItem>,
    onPlayItem: (VideoYouTubeItem) -> Unit,
) {
    val featuredItem = items.firstOrNull()

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "YouTube Highlights",
            subtitle = "Mehrere YouTube-Videos direkt im Hub sammeln und anschauen.",
            icon = Icons.Default.PlayArrow,
            accent = YouTubeRed,
            tag = "YT",
        )
        Text(
            text = if (items.isEmpty()) {
                "Sobald Links gesetzt sind, tauchen sie hier als eigene Highlights auf."
            } else {
                "${items.size} YouTube-Links sind aktuell live."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        featuredItem?.let { item ->
            VideoYouTubeSpotlightCard(
                item = item,
                itemCount = items.size,
                modifier = Modifier.padding(top = 14.dp),
                onPlay = { onPlayItem(item) },
            )
        }

        if (items.isEmpty()) {
            Text(
                text = "Noch nichts hinterlegt. Neue Links kannst du unten im Admin Control direkt als YouTube Library einfuegen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 14.dp),
            )
        } else {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items.drop(1).forEach { item ->
                    VideoYouTubeRow(
                        item = item,
                        onPlay = { onPlayItem(item) },
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoYouTubeSpotlightCard(
    item: VideoYouTubeItem,
    itemCount: Int,
    modifier: Modifier = Modifier,
    onPlay: () -> Unit,
) {
    val thumbnailUrl = remember(item.url) { resolveYouTubeThumbnailUrl(item.url) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DexBlueDeep.copy(alpha = 0.96f),
                        YouTubeRed.copy(alpha = 0.26f),
                        ArenaGold.copy(alpha = 0.14f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = YouTubeRed.copy(alpha = 0.22f),
                shape = RoundedCornerShape(24.dp),
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(188.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            YouTubeDeepRed,
                            DexBlueDeep,
                        ),
                    ),
                ),
        ) {
            if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.28f)),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.14f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.58f),
                            ),
                        ),
                    ),
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BrandPill(text = "Featured YouTube", tint = YouTubeRed)
                BrandPill(text = "$itemCount live", tint = ArenaGold)
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.54f))
                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp),
                )
            }
        }

        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            fontWeight = FontWeight.Black,
        )

        if (item.subtitle.isNotBlank()) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.78f),
            )
        }

        Text(
            text = "Mehrere YouTube-Videos sind jetzt direkt als eigene Watch-Zone im Hub sichtbar.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.70f),
        )

        BrandActionButton(
            text = "Jetzt ansehen",
            onClick = onPlay,
            accent = YouTubeRed,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.PlayArrow,
        )
    }
}

@Composable
private fun VideoEquipmentRow(
    item: VideoEquipmentItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        DexBlue.copy(alpha = 0.10f),
                        ArenaGold.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = DexBlue.copy(alpha = 0.18f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        VideoHubPreviewFrame(
            accent = DexBlue,
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
            VideoPill(text = "Setup", isActive = true)
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

@Composable
private fun VideoPublicConfigEditorCard(
    uiState: com.skydown.android.ui.model.VideoHubUiState,
    activeImageUploadTarget: VideoConfigImageTarget?,
    onAddEquipment: () -> Unit,
    onUpdateEquipmentTitle: (String, String) -> Unit,
    onUpdateEquipmentDetail: (String, String) -> Unit,
    onUpdateEquipmentImageUrl: (String, String) -> Unit,
    onPickEquipmentImage: (String) -> Unit,
    onRemoveEquipmentImage: (String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onAddYouTube: () -> Unit,
    onAddYouTubeBatch: (String) -> Unit,
    onUpdateYouTubeTitle: (String, String) -> Unit,
    onUpdateYouTubeSubtitle: (String, String) -> Unit,
    onUpdateYouTubeUrl: (String, String) -> Unit,
    onRemoveYouTube: (String) -> Unit,
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
    onUpdateCollaborationYouTubeUrl: (String, String) -> Unit,
    onRemoveCollaboration: (String) -> Unit,
    onSave: () -> Unit,
) {
    var youtubeBatchInput by rememberSaveable { mutableStateOf("") }

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Admin Control",
            subtitle = "Owner und Video-Admins pflegen hier Setup und Featured Collabs.",
            icon = Icons.Default.Sync,
            accent = ArenaRed,
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
            title = "Equipment Setup",
            subtitle = "Kameras, Tools und Software fuer den visuellen Workflow.",
            icon = Icons.Default.CameraAlt,
            accent = DexBlue,
            tag = "SETUP",
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
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                        DexBlue.copy(alpha = 0.10f),
                                        ArenaGold.copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = DexBlue.copy(alpha = 0.18f),
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
            title = "YouTube Library",
            subtitle = "Mehrere YouTube-Videos sammeln, direkt pruefen und als Highlights live schalten.",
            icon = Icons.Default.PlayArrow,
            accent = YouTubeRed,
            tag = "YT",
            modifier = Modifier.padding(top = 18.dp),
        ) {
            Text(
                text = "Du kannst einzelne Eintraege pflegen oder mehrere Links auf einmal einfuegen. Ein Link pro Zeile, optional mit `Titel | URL`.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )

            OutlinedTextField(
                value = youtubeBatchInput,
                onValueChange = { youtubeBatchInput = it },
                label = { Text("Mehrere YouTube-Links") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                minLines = 4,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        val value = youtubeBatchInput.trim()
                        if (value.isNotBlank()) {
                            onAddYouTubeBatch(value)
                            youtubeBatchInput = ""
                        }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Links uebernehmen")
                }

                OutlinedButton(
                    onClick = onAddYouTube,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Eintrag hinzufuegen")
                }
            }

            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (uiState.publicConfig.youtubeItems.isEmpty()) {
                    Text(
                        text = "Noch keine YouTube-Videos angelegt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                } else {
                    uiState.publicConfig.youtubeItems.forEachIndexed { index, item ->
                        val isPlayable = resolveYouTubeEmbedUrl(item.url)?.isNotBlank() == true
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                            YouTubeRed.copy(alpha = 0.10f),
                                            ArenaGold.copy(alpha = 0.06f),
                                        ),
                                    ),
                                )
                                .border(
                                    width = 1.dp,
                                    color = YouTubeRed.copy(alpha = 0.18f),
                                    shape = RoundedCornerShape(18.dp),
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                VideoPill(
                                    text = item.title.ifBlank { "YouTube Video ${index + 1}" },
                                    isActive = true,
                                )
                                VideoPill(
                                    text = if (isPlayable) "Abspielbar" else "URL pruefen",
                                    isActive = false,
                                )
                            }
                            OutlinedTextField(
                                value = item.title,
                                onValueChange = { onUpdateYouTubeTitle(item.id, it) },
                                label = { Text("Titel (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = item.subtitle,
                                onValueChange = { onUpdateYouTubeSubtitle(item.id, it) },
                                label = { Text("Untertitel (optional)") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            OutlinedTextField(
                                value = item.url,
                                onValueChange = { onUpdateYouTubeUrl(item.id, it) },
                                label = { Text("YouTube URL") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                            )
                            OutlinedButton(
                                onClick = { onRemoveYouTube(item.id) },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("YouTube-Video entfernen")
                            }
                        }
                    }
                }
            }
        }

        VideoControlDeckCard(
            title = "Collab Profiles",
            subtitle = "Rollen, Highlights, Vibes und Links fuer das Featured-Netzwerk.",
            icon = Icons.Default.CheckCircle,
            accent = FieldMint,
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
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                                        FieldMint.copy(alpha = 0.10f),
                                        ArenaGold.copy(alpha = 0.06f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = FieldMint.copy(alpha = 0.18f),
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
                            onValueChange = { onUpdateCollaborationYouTubeUrl(item.id, it) },
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
    artist: com.skydown.android.ui.model.ProducedWithArtist,
    onOpenLink: (String) -> Unit,
    onOpenYouTube: (VideoYouTubeItem) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(164.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        DexBlueDeep.copy(alpha = 0.92f),
                        FieldMint.copy(alpha = 0.16f),
                        ArenaGold.copy(alpha = 0.10f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = FieldMint.copy(alpha = 0.22f),
                shape = RoundedCornerShape(18.dp),
            ),
    ) {
        if (!artist.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = artist.imageUrl,
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
                                FieldMint,
                                DexBlue,
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
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.20f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.82f),
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
                        .background(Color.Black.copy(alpha = 0.32f))
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = artist.role.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.94f),
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(ArenaGold.copy(alpha = 0.28f))
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
                            .background(FieldMint.copy(alpha = 0.82f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = artist.vibe,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DexBlueDeep,
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
            )

            if (artist.highlight.isNotBlank()) {
                Text(
                    text = artist.highlight,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.90f),
                    maxLines = 1,
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
                                YouTubeDeepRed,
                            ),
                        ),
                        onClick = {
                            onOpenYouTube(
                                VideoYouTubeItem(
                                    id = "collab-${artist.id}",
                                    title = artist.name,
                                    subtitle = artist.highlight.ifBlank { artist.role },
                                    url = youtubeUrl,
                                ),
                            )
                        },
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
private fun VideoYouTubeRow(
    item: VideoYouTubeItem,
    onPlay: () -> Unit,
) {
    val thumbnailUrl = remember(item.url) { resolveYouTubeThumbnailUrl(item.url) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        YouTubeRed.copy(alpha = 0.12f),
                        ArenaGold.copy(alpha = 0.08f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                color = YouTubeRed.copy(alpha = 0.20f),
                shape = RoundedCornerShape(18.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        VideoHubPreviewFrame(
            accent = YouTubeRed,
            modifier = Modifier.size(88.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (thumbnailUrl != null) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = item.title,
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
                                        YouTubeRed,
                                        YouTubeDeepRed,
                                    ),
                                ),
                            ),
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.52f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            VideoPill(text = "Play", isActive = true)
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp),
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
            Text(
                text = "Direkt im Dialog oder extern in YouTube.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
        }

        BrandActionButton(
            text = "YouTube",
            onClick = onPlay,
            accent = YouTubeRed,
            icon = Icons.Default.PlayArrow,
            compact = true,
            modifier = Modifier
        )
    }
}

@Composable
private fun VideoUploadCard(
    uiState: com.skydown.android.ui.model.VideoHubUiState,
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Video Upload",
            subtitle = "Admin-only Bereich fuer neue Clips und externe Videoquellen.",
            icon = Icons.Default.Movie,
            accent = ArenaRed,
            tag = "UPLOAD",
        )
        Text(
            text = "Hier steuerst du direkte Uploads nach Firebase Storage oder externe Reel-Links. Mehrere YouTube-Videos pflegst du im Admin Control als eigene Library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = "Storage", isActive = true)
            VideoPill(text = "YouTube", isActive = false)
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
            accent = DexBlue,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            icon = Icons.Default.Movie,
            filled = false,
        )

        Text(
            text = "Oder als externer Reel-Link freigeben.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 14.dp),
        )

        OutlinedTextField(
            value = uiState.externalVideoUrl,
            onValueChange = onUpdateExternalVideoUrl,
            label = { Text("YouTube / Google Drive / MEGA / anderer Video-Link") },
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
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                        DexBlue.copy(alpha = 0.10f),
                                        ArenaGold.copy(alpha = 0.08f),
                                    ),
                                ),
                            )
                            .border(
                                width = 1.dp,
                                color = DexBlue.copy(alpha = 0.18f),
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
            accent = ArenaRed,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            icon = Icons.Default.Movie,
            enabled = !uiState.isUploading,
            isLoading = uiState.isUploading,
        )

        BrandActionButton(
            text = "Externes Reel freigeben",
            onClick = onAddExternalVideo,
            accent = ArenaGold,
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
    onOpenOriginal: (String) -> Unit,
    onOpenReel: (() -> Unit)?,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Now Playing",
            subtitle = "Das aktive Video bleibt gross, fokussiert und direkt abspielbar.",
            icon = Icons.Default.Movie,
            accent = ArenaRed,
            tag = "LIVE",
        )

        Text(
            text = "Der aktive Clip sitzt jetzt direkt im Fokus. Fuer den ganzen Feed kannst du jederzeit in den Reel-Modus springen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (video == null) {
            Text(
                text = "Noch kein Video ausgewaehlt. Sobald Uploads live sind, kannst du sie hier direkt abspielen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 12.dp),
            )
            return@SkydownCard
        }

        VideoHubPreviewFrame(
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
                                text = "Dieser Clip wird ueber einen externen Link ausgeliefert.",
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

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    VideoPill(
                        text = if (video.usesEmbeddedPreview) "Embed" else if (video.nativePlaybackUrl.isNotBlank()) "Native" else "Link",
                        isActive = true,
                    )
                    if (video.isHomeFeatured) {
                        VideoPill(text = "Home", isActive = true)
                    }
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VideoPill(text = "Reel", isActive = true)
                        VideoPill(text = video.projectName, isActive = false)
                        VideoPill(text = video.providerBadge, isActive = false)
                    }
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (video.notes.isNotBlank()) {
                        Text(
                            text = video.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = formatVideoDate(video.createdAtMillis), isActive = true)
            VideoPill(text = video.projectName, isActive = false)
            if (video.isHomeFeatured) {
                VideoPill(text = "Home active", isActive = true)
            }
        }

        if (video.supportsInlinePlayback) {
            onOpenReel?.let { openReel ->
                BrandActionButton(
                    text = "Im Reel ansehen",
                    onClick = openReel,
                    accent = ArenaRed,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    icon = Icons.Default.PlayArrow,
                )
            }
        }

        if (video.openUrl.isNotBlank()) {
            BrandActionButton(
                text = "Original oeffnen",
                onClick = { onOpenOriginal(video.openUrl) },
                accent = videoHubProviderAccent(video),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                icon = Icons.Default.Language,
                filled = false,
            )
        }
    }
}

@Composable
private fun VideoLibraryCard(
    uiState: com.skydown.android.ui.model.VideoHubUiState,
    selectedVideoId: String?,
    onSelectVideo: (VideoHubItem) -> Unit,
    onOpenReel: (VideoHubItem) -> Unit,
    onOpenOriginal: (String) -> Unit,
    onToggleHomeFeatured: (VideoHubItem) -> Unit,
    onDeleteVideo: (VideoHubItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        VideoHubSectionBanner(
            title = "Video Library",
            subtitle = "Alle Clips im Hub, klar getrennt nach Fokus und Status.",
            icon = Icons.Default.Movie,
            accent = DexBlue,
            tag = "LIBRARY",
        )
        Text(
            text = if (uiState.isAdmin) {
                "${uiState.videos.size} Videos im Hub."
            } else {
                "${uiState.videos.size} Videos verfuegbar."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

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
                    Text("Videos werden geladen ...")
                }
            }

            uiState.videos.isEmpty() -> {
                Text(
                    text = "Noch keine Videos sichtbar. Sobald ein Clip hochgeladen ist, taucht er hier auf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 16.dp),
                )
            }

            else -> {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    uiState.videos.forEach { video ->
                        VideoLibraryRow(
                            video = video,
                            isSelected = video.id == selectedVideoId,
                            isAdmin = uiState.isAdmin,
                            onSelect = { onSelectVideo(video) },
                            onOpenReel = { onOpenReel(video) },
                            onOpenOriginal = { onOpenOriginal(video.openUrl) },
                            onToggleHomeFeatured = { onToggleHomeFeatured(video) },
                            onDelete = { onDeleteVideo(video) },
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
    onSelect: () -> Unit,
    onOpenReel: () -> Unit,
    onOpenOriginal: () -> Unit,
    onToggleHomeFeatured: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(
                            DexBlue.copy(alpha = 0.18f),
                            ArenaGold.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        )
                    } else {
                        listOf(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f),
                        )
                    },
                ),
            )
            .border(
                width = 1.dp,
                color = if (isSelected) ArenaGold.copy(alpha = 0.28f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                shape = RoundedCornerShape(20.dp),
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            VideoHubPreviewFrame(
                accent = videoHubProviderAccent(video),
                modifier = Modifier
                    .size(64.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    videoHubProviderAccent(video).copy(alpha = 0.88f),
                                    DexBlueDeep.copy(alpha = 0.76f),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Movie,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${video.projectName} • ${formatVideoDate(video.createdAtMillis)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                )
                if (video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VideoPill(text = if (isSelected) "Im Player" else "Auswaehlen", isActive = isSelected)
            VideoPill(text = if (video.isPublic) "Public" else "Private", isActive = video.isPublic)
            VideoPill(text = video.providerBadge, isActive = false)
            if (isAdmin && video.isHomeFeatured) {
                VideoPill(text = "Home", isActive = true)
            } else if (!isAdmin) {
                VideoPill(text = "Clip", isActive = false)
            }
        }

        if (isAdmin) {
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(if (isSelected) "Aktiv im Player" else "Im Player laden")
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
            if (video.supportsInlinePlayback) {
                Button(
                    onClick = onOpenReel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Direkt im Reel")
                }
            }

            if (video.openUrl.isNotBlank()) {
                OutlinedButton(
                    onClick = onOpenOriginal,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text("Original oeffnen")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoReelViewerDialog(
    videos: List<VideoHubItem>,
    selectedVideoId: String?,
    onSelectVideo: (VideoHubItem) -> Unit,
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

    LaunchedEffect(pagerState.currentPage, videos) {
        val video = videos.getOrNull(pagerState.currentPage)
        video?.let(onSelectVideo)
        val url = video?.nativePlaybackUrl
        if (url.isNullOrBlank()) {
            reelPlayer.stop()
            reelPlayer.clearMediaItems()
        } else {
            reelPlayer.setMediaItem(MediaItem.fromUri(url))
            reelPlayer.prepare()
            reelPlayer.play()
        }
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = it.projectName.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                            )
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            if (it.notes.isNotBlank()) {
                                Text(
                                    text = it.notes,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.80f),
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Skydown Reel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Text(
                        text = "${pagerState.currentPage + 1} von ${videos.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.18f)),
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Schliessen",
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ExternalVideoWebPlayer(
    url: String,
    modifier: Modifier = Modifier,
) {
    val playerSource = remember(url) { externalVideoWebPlayerSource(url) }

    AndroidView(
        modifier = modifier,
        factory = { playerContext ->
            WebView(playerContext).apply {
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                settings.javaScriptEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.domStorageEnabled = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.loadsImagesAutomatically = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                setBackgroundColor(android.graphics.Color.BLACK)

                if (playerSource.html != null) {
                    loadDataWithBaseURL(
                        playerSource.baseUrl,
                        playerSource.html,
                        "text/html",
                        "utf-8",
                        null,
                    )
                    tag = playerSource.renderKey
                } else if (playerSource.url != null) {
                    loadUrl(playerSource.url)
                    tag = playerSource.renderKey
                }
            }
        },
        update = { webView ->
            if (webView.tag != playerSource.renderKey) {
                webView.tag = playerSource.renderKey
                if (playerSource.html != null) {
                    webView.loadDataWithBaseURL(
                        playerSource.baseUrl,
                        playerSource.html,
                        "text/html",
                        "utf-8",
                        null,
                    )
                } else if (playerSource.url != null && webView.url != playerSource.url) {
                    webView.loadUrl(playerSource.url)
                }
            }
        },
    )
}

private data class ExternalVideoWebPlayerSource(
    val url: String?,
    val html: String?,
    val baseUrl: String?,
    val renderKey: String,
)

private fun externalVideoWebPlayerSource(rawUrl: String): ExternalVideoWebPlayerSource {
    val youtubeEmbedUrl = resolveYouTubeEmbedUrl(rawUrl)
    if (youtubeEmbedUrl != null) {
        val html = """
            <!doctype html>
            <html>
              <head>
                <meta charset="utf-8" />
                <meta
                  name="viewport"
                  content="width=device-width, initial-scale=1, maximum-scale=1, viewport-fit=cover"
                />
                <style>
                  html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background: #000;
                    overflow: hidden;
                  }
                  iframe {
                    width: 100%;
                    height: 100%;
                    border: 0;
                    background: #000;
                  }
                </style>
              </head>
              <body>
                <iframe
                  src="$youtubeEmbedUrl"
                  title="YouTube video player"
                  allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
                  referrerpolicy="origin"
                  allowfullscreen>
                </iframe>
              </body>
            </html>
        """.trimIndent()

        return ExternalVideoWebPlayerSource(
            url = null,
            html = html,
            baseUrl = "https://www.youtube.com",
            renderKey = youtubeEmbedUrl,
        )
    }

    return ExternalVideoWebPlayerSource(
        url = rawUrl,
        html = null,
        baseUrl = null,
        renderKey = rawUrl,
    )
}

private fun videoHubProviderAccent(video: VideoHubItem): Color {
    return when {
        video.usesEmbeddedPreview -> YouTubeRed
        video.providerBadge.equals("Drive", ignoreCase = true) -> FieldMint
        video.providerBadge.equals("MEGA", ignoreCase = true) -> ArenaRed
        else -> DexBlue
    }
}

@Composable
private fun VideoPill(
    text: String,
    isActive: Boolean,
) {
    BrandStatusChip(
        text = text,
        accent = ArenaGold,
        isActive = isActive,
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
