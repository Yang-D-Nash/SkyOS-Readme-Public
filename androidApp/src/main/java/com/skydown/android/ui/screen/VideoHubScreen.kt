package com.skydown.android.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.EditableImageFieldCard
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.dismissKeyboardOnTap
import com.skydown.android.ui.component.skydownPressable
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownSheen
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.ProducedWithArtist
import com.skydown.android.ui.model.VideoEquipmentItem
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.model.VideoYouTubeItem
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
    var showReelViewer by rememberSaveable { mutableStateOf(false) }
    var showUploadSheet by rememberSaveable { mutableStateOf(false) }
    var pendingConfigImageTarget by remember { mutableStateOf<VideoConfigImageTarget?>(null) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
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
    val configImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        val target = pendingConfigImageTarget ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            coroutineScope.launch {
                val result = editableImageAssetRepository.uploadImageAsset(
                    uri = uri,
                    mimeType = context.contentResolver.getType(uri),
                )
                if (result.isSuccess) {
                    val uploadedUrl = result.getOrNull().orEmpty()
                    when (target) {
                        is VideoConfigImageTarget.Equipment -> viewModel.updateEquipmentItem(target.itemId, imageUrl = uploadedUrl)
                        is VideoConfigImageTarget.Collaboration -> viewModel.updateCollaborationItem(target.itemId, imageUrl = uploadedUrl)
                    }
                    Toast.makeText(context, "Bild hochgeladen und uebernommen.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(
                        context,
                        result.exceptionOrNull()?.message ?: "Bild konnte nicht hochgeladen werden.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                pendingConfigImageTarget = null
            }
        } else {
            pendingConfigImageTarget = null
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    LaunchedEffect(selectedVideo?.downloadUrl, shouldAutoplaySelection) {
        val url = selectedVideo?.downloadUrl
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
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.07f,
                        secondaryAlpha = 0.05f,
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
                    VideoHubHeroCard(isAdmin = uiState.isAdmin)
                }

                item {
                    VideoEquipmentCard(items = uiState.publicConfig.equipmentItems)
                }

                item {
                    VideoPlayerCard(
                        video = selectedVideo,
                        player = player,
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
                        VideoFormatCard()
                    }

                    item {
                        VideoPublicConfigEditorCard(
                            uiState = uiState,
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
                message = uiState.feedbackMessage,
                type = if (uiState.feedbackIsError) ToastType.Error else ToastType.Success,
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
) {
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    BrandHeroCard(
        eyebrow = screenHeaderSettings.videoHubEyebrow.ifBlank { "Video" },
        title = screenHeaderSettings.videoHubTitle.ifBlank { "Video" },
        subtitle = screenHeaderSettings.videoHubSubtitle.ifBlank { "Reels, Visuals und starke Kollaborationen." },
        detail = screenHeaderSettings.videoHubDetail.ifBlank { "Clips, Looks und Leute hinter dem Vibe." },
        backgroundImageUrl = screenHeaderSettings.videoHubImageUrl.ifBlank { null },
        accent = MaterialTheme.colorScheme.secondary,
        secondaryAccent = MaterialTheme.colorScheme.tertiary,
        marks = listOf(BrandArtwork.Skydown),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = "Videos", tint = MaterialTheme.colorScheme.secondary)
            BrandPill(text = "Equipment", tint = MaterialTheme.colorScheme.primary)
            BrandPill(text = "Collabs", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun VideoFormatCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Format")
        Text(
            text = "MP4, MOV oder M4V.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Komprimierte Cuts laden schneller.",
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
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Featured Collabs")
        Text(
            text = "Artists und Creatives, mit denen die Visuals entstehen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

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

@Composable
private fun VideoEquipmentCard(
    items: List<VideoEquipmentItem>,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Equipment & Software")
        Text(
            text = "Setup.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

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

@Composable
private fun VideoYouTubeCard(
    items: List<VideoYouTubeItem>,
    onPlayItem: (VideoYouTubeItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("YouTube")
        Text(
            text = "Videos & Making-ofs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = "Noch nichts hinterlegt.",
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
private fun VideoEquipmentRow(
    item: VideoEquipmentItem,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(16.dp))
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

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
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
    onAddEquipment: () -> Unit,
    onUpdateEquipmentTitle: (String, String) -> Unit,
    onUpdateEquipmentDetail: (String, String) -> Unit,
    onUpdateEquipmentImageUrl: (String, String) -> Unit,
    onPickEquipmentImage: (String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onAddCollaboration: () -> Unit,
    onUpdateCollaborationName: (String, String) -> Unit,
    onUpdateCollaborationRole: (String, String) -> Unit,
    onUpdateCollaborationHighlight: (String, String) -> Unit,
    onUpdateCollaborationVibe: (String, String) -> Unit,
    onUpdateCollaborationImageUrl: (String, String) -> Unit,
    onPickCollaborationImage: (String) -> Unit,
    onUpdateCollaborationSpotifyArtistId: (String, String) -> Unit,
    onUpdateCollaborationInstagramUrl: (String, String) -> Unit,
    onUpdateCollaborationYouTubeUrl: (String, String) -> Unit,
    onRemoveCollaboration: (String) -> Unit,
    onSave: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Videography Editor")
        Text(
            text = "Owner und Video-Admins steuern hier Equipment und Featured Collabs. Bilder laufen jetzt picker-first mit Vorschau statt ueber rohe URLs.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Text(
            text = "Equipment",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.publicConfig.equipmentItems.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        onPickImage = { onPickEquipmentImage(item.id) },
                        onImageUrlChange = { onUpdateEquipmentImageUrl(item.id, it) },
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
                .padding(top = 12.dp),
        ) {
            Text("Equipment hinzufuegen")
        }

        Text(
            text = "Featured Collabs",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp),
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.publicConfig.collaborationItems.forEach { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        onPickImage = { onPickCollaborationImage(item.id) },
                        onImageUrlChange = { onUpdateCollaborationImageUrl(item.id, it) },
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
                .padding(top = 12.dp),
        ) {
            Text("Collab hinzufuegen")
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
                Text("Oeffentliche Daten speichern")
            }
        }
    }
}

@Composable
private fun ProducedWithArtistRow(
    artist: com.skydown.android.ui.model.ProducedWithArtist,
    onOpenLink: (String) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.60f),
                    ),
                ),
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
                                MaterialTheme.colorScheme.tertiary,
                                MaterialTheme.colorScheme.primary,
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

                Spacer(modifier = Modifier.weight(1f))

                if (artist.vibe.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.78f))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                    ) {
                        Text(
                            text = artist.vibe,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
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
            .skydownSheen(alpha = 0.14f)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            if (item.subtitle.isNotBlank()) {
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        TextButton(
            onClick = onPlay,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = Color.White,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            YouTubeRed,
                            YouTubeDeepRed,
                        ),
                    ),
                )
                .skydownSheen(accent = Color.White, alpha = 0.12f),
        ) {
            Text("YouTube")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun VideoYouTubePlayerDialog(
    item: VideoYouTubeItem,
    onDismiss: () -> Unit,
    onOpenExternal: (String) -> Unit,
) {
    val playerSource = remember(item.url) { youtubePlayerSource(item.url) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        SkydownCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (item.subtitle.isNotBlank()) {
                        Text(
                            text = item.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dialog schliessen",
                    )
                }
            }

            if (playerSource != null) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .padding(top = 12.dp),
                    factory = { playerContext ->
                        WebView(playerContext).apply {
                            webViewClient = WebViewClient()
                            webChromeClient = WebChromeClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.mediaPlaybackRequiresUserGesture = false
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            settings.loadsImagesAutomatically = true
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            setBackgroundColor(android.graphics.Color.BLACK)
                            loadDataWithBaseURL(
                                playerSource.baseUrl,
                                playerSource.html,
                                "text/html",
                                "utf-8",
                                null,
                            )
                            tag = playerSource.embedKey
                        }
                    },
                    update = { webView ->
                        if (webView.tag != playerSource.embedKey) {
                            webView.tag = playerSource.embedKey
                            webView.loadDataWithBaseURL(
                                playerSource.baseUrl,
                                playerSource.html,
                                "text/html",
                                "utf-8",
                                null,
                            )
                        }
                    },
                )
            } else {
                Text(
                    text = "Der YouTube Player konnte fuer dieses Video nicht aufgebaut werden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = { onOpenExternal(playerSource?.externalUrl ?: item.url) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("In YouTube oeffnen")
                }

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Schliessen")
                }
            }
        }
    }
}

private data class YouTubePlayerSource(
    val html: String,
    val baseUrl: String,
    val externalUrl: String,
    val embedKey: String,
)

private fun youtubePlayerSource(rawUrl: String): YouTubePlayerSource? {
    val normalizedUrl = normalizedYouTubeUrl(rawUrl) ?: return null
    val videoId = resolvedYouTubeVideoId(rawUrl, normalizedUrl)
    if (videoId == null) {
        return null
    }

    val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0&modestbranding=1&controls=1"
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
              src="$embedUrl"
              title="YouTube video player"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerpolicy="origin"
              allowfullscreen>
            </iframe>
          </body>
        </html>
    """.trimIndent()

    return YouTubePlayerSource(
        html = html,
        baseUrl = "https://www.youtube.com",
        externalUrl = "https://www.youtube.com/watch?v=$videoId",
        embedKey = videoId,
    )
}

private fun normalizedYouTubeUrl(rawUrl: String): String? {
    val trimmed = rawUrl.trim()
    if (trimmed.isBlank()) return null
    return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        trimmed
    } else {
        "https://$trimmed"
    }
}

private fun resolvedYouTubeVideoId(rawUrl: String, normalizedUrl: String): String? {
    val uri = Uri.parse(normalizedUrl)
    val host = uri.host?.lowercase(Locale.ROOT).orEmpty()

    when {
        "youtu.be" in host -> {
            uri.pathSegments.firstOrNull()?.takeIfYouTubeId()?.let { return it }
        }
        uri.path?.contains("/embed/") == true ||
            uri.path?.contains("/shorts/") == true ||
            uri.path?.contains("/live/") == true -> {
            uri.lastPathSegment?.takeIfYouTubeId()?.let { return it }
        }
    }

    uri.getQueryParameter("v")?.takeIfYouTubeId()?.let { return it }
    uri.getQueryParameter("vi")?.takeIfYouTubeId()?.let { return it }

    val pattern = Regex("""(?:(?<=v=)|(?<=vi=)|(?<=/embed/)|(?<=/shorts/)|(?<=youtu\.be/)|(?<=/live/))([A-Za-z0-9_-]{11})""")
    return pattern.find(rawUrl.trim())?.groupValues?.getOrNull(1)?.takeIfYouTubeId()
}

private fun String.takeIfYouTubeId(): String? {
    val trimmed = trim()
    return trimmed.takeIf { it.length == 11 }
}

@Composable
private fun VideoUploadCard(
    uiState: com.skydown.android.ui.model.VideoHubUiState,
    onUpdateTitle: (String) -> Unit,
    onUpdateProjectName: (String) -> Unit,
    onUpdateEmail: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onPickFiles: () -> Unit,
    onRemoveFile: (SelectedVideoFile) -> Unit,
    onUpload: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Video Upload")
        Text(
            text = "Nur Admins sehen diesen Bereich. Die Videos landen direkt in Firebase Storage und erscheinen danach in der Library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

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

        OutlinedButton(
            onClick = onPickFiles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Videos auswaehlen")
        }

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
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f))
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

        Button(
            onClick = onUpload,
            enabled = !uiState.isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            if (uiState.isUploading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "Upload laeuft ...",
                    modifier = Modifier.padding(start = 10.dp),
                )
            } else {
                Text("Videos hochladen")
            }
        }
    }
}

@Composable
private fun VideoPlayerCard(
    video: VideoHubItem?,
    player: ExoPlayer,
    onOpenReel: (() -> Unit)?,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Reel Player")

        Text(
            text = "Der Clip bleibt jetzt gross, vertikal und direkt im Fokus. Fuer den ganzen Feed kannst du jederzeit in den Reel-Modus springen.",
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoPill(text = "Reel", isActive = true)
                    VideoPill(text = video.projectName, isActive = false)
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

        onOpenReel?.let { openReel ->
            Button(
                onClick = openReel,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Im Reel ansehen")
            }
        }

        if (video.notes.isNotBlank()) {
            Text(
                text = video.notes,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 12.dp),
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
    onToggleHomeFeatured: (VideoHubItem) -> Unit,
    onDeleteVideo: (VideoHubItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Video Library")
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
    onToggleHomeFeatured: () -> Unit,
    onDelete: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.34f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)
                },
            )
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
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
                Text(if (isSelected) "Im Player" else "Im Player laden")
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
            Button(
                onClick = onOpenReel,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text("Direkt im Reel")
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
        val url = video?.downloadUrl
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
                    if (video != null && page == pagerState.currentPage) {
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

@Composable
private fun VideoPill(
    text: String,
    isActive: Boolean,
) {
    val background = if (isActive) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f)
    }
    val content = if (isActive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            color = content,
            style = MaterialTheme.typography.labelLarge,
        )
    }
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
