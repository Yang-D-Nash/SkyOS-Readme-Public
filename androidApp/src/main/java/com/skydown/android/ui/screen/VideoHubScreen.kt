package com.skydown.android.ui.screen

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
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
import com.skydown.android.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.dismissKeyboardOnTap
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.VideoEquipmentItem
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.model.VideoYouTubeItem
import com.skydown.android.ui.model.skydownProducedWithArtists
import com.skydown.android.ui.viewmodel.VideoHubViewModel
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
    onOpenSettings: (() -> Unit)? = null,
    viewModel: VideoHubViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
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
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    var shouldAutoplaySelection by rememberSaveable {
        mutableStateOf(autoplayInitialSelection && !initialSelectedVideoId.isNullOrBlank())
    }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val selectedVideo = uiState.videos.firstOrNull { it.id == selectedVideoId } ?: uiState.videos.firstOrNull()
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            viewModel.setSelectedFiles(context, uris)
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
                        subtitle = "Reels, Clips, Equipment und YouTube direkt in der App.",
                    )
                },
                actions = {
                    if (onOpenSettings != null) {
                        AppTopBarSessionActions(
                            onOpenCart = onOpenCart,
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
                            coroutineScope.launch {
                                listState.animateScrollToItem(VideoHubListIndex.uploadSection(uiState.isAdmin))
                            }
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

                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            listState.animateScrollToItem(VideoHubListIndex.youtubeSection)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Zu YouTube",
                    )
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
                    VideoCollaborationsCard(
                        onOpenLink = { url -> openExternalLink(context, url) },
                    )
                }

                item {
                    VideoEquipmentCard(items = uiState.publicConfig.equipmentItems)
                }

                item {
                    VideoYouTubeCard(
                        items = uiState.publicConfig.youtubeItems,
                        onOpenLink = { url -> openExternalLink(context, url) },
                    )
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
                            onRemoveEquipment = viewModel::removeEquipmentItem,
                            onAddYouTube = viewModel::addYouTubeItem,
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
                            onSave = viewModel::savePublicConfig,
                        )
                    }

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
        }
    }
}

private object VideoHubListIndex {
    const val youtubeSection = 3

    fun uploadSection(isAdmin: Boolean): Int {
        return if (isAdmin) 8 else youtubeSection
    }
}

@Composable
private fun VideoHubHeroCard(
    isAdmin: Boolean,
) {
    BrandHeroCard(
        eyebrow = "Video",
        title = "Video",
        subtitle = "Wenn du schauen willst, bist du hier richtig: Reels, Clips und neue Uploads.",
        detail = "Von hier aus gehst du direkt weiter zu YouTube, Equipment oder Kontakt.",
        accent = MaterialTheme.colorScheme.secondary,
        secondaryAccent = MaterialTheme.colorScheme.tertiary,
        marks = listOf(BrandArtwork.Skydown),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = "Videos", tint = MaterialTheme.colorScheme.secondary)
            BrandPill(text = "Equipment", tint = MaterialTheme.colorScheme.primary)
            BrandPill(text = "YouTube", tint = MaterialTheme.colorScheme.tertiary)
            BrandPill(text = "Collabs", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun VideoFormatCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Format-Hinweis")
        Text(
            text = "Empfohlen sind MP4, MOV oder M4V. Am stabilsten laufen H.264 oder H.265 mit sauberem Export fuer mobile Wiedergabe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = "Querformat und Hochformat funktionieren beide. Fuer schnellere Uploads sind komprimierte Social-Cuts besser als rohe Master-Dateien.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

@Composable
private fun VideoCollaborationsCard(
    onOpenLink: (String) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Collaborators")
        Text(
            text = "Artists und Creatives, mit denen in Produktionen zusammengearbeitet wurde.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            skydownProducedWithArtists.forEach { artist ->
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
            text = "Das Setup hinter den Videos, Reels und Produktionen.",
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
                    title = item.title,
                    detail = item.detail,
                )
            }
        }
    }
}

@Composable
private fun VideoYouTubeCard(
    items: List<VideoYouTubeItem>,
    onOpenLink: (String) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("YouTube")
        Text(
            text = "Hier koennen oeffentliche YouTube-Arbeiten, Making-ofs oder Musikvideos gesammelt werden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        if (items.isEmpty()) {
            Text(
                text = "Noch keine YouTube-Videos hinterlegt.",
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
                        onOpenLink = onOpenLink,
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoEquipmentRow(
    title: String,
    detail: String,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = detail,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun VideoPublicConfigEditorCard(
    uiState: com.skydown.android.ui.model.VideoHubUiState,
    onAddEquipment: () -> Unit,
    onUpdateEquipmentTitle: (String, String) -> Unit,
    onUpdateEquipmentDetail: (String, String) -> Unit,
    onRemoveEquipment: (String) -> Unit,
    onAddYouTube: () -> Unit,
    onUpdateYouTubeTitle: (String, String) -> Unit,
    onUpdateYouTubeSubtitle: (String, String) -> Unit,
    onUpdateYouTubeUrl: (String, String) -> Unit,
    onRemoveYouTube: (String) -> Unit,
    onSave: () -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Videography Editor")
        Text(
            text = "Admins steuern hier die oeffentliche Equipment-Liste und die YouTube-Sparte fuer alle Nutzer.",
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
            text = "YouTube",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 18.dp),
        )
        Column(
            modifier = Modifier.padding(top = 10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            uiState.publicConfig.youtubeItems.forEach { item ->
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
                        onValueChange = { onUpdateYouTubeTitle(item.id, it) },
                        label = { Text("Titel") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.subtitle,
                        onValueChange = { onUpdateYouTubeSubtitle(item.id, it) },
                        label = { Text("Untertitel") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = item.url,
                        onValueChange = { onUpdateYouTubeUrl(item.id, it) },
                        label = { Text("URL") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedButton(
                        onClick = { onRemoveYouTube(item.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Eintrag entfernen")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onAddYouTube,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
        ) {
            Text("YouTube-Video hinzufuegen")
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
                text = artist.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = artist.role,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            artist.spotifyArtistId?.takeIf { it.isNotBlank() }?.let { spotifyArtistId ->
                TextButton(onClick = { onOpenLink("https://open.spotify.com/artist/$spotifyArtistId") }) {
                    Text("Spotify")
                }
            }
            artist.instagramUrl?.takeIf { it.isNotBlank() }?.let { instagramUrl ->
                TextButton(onClick = { onOpenLink(instagramUrl) }) {
                    Text("Instagram")
                }
            }
        }
    }
}

@Composable
private fun VideoYouTubeRow(
    item: VideoYouTubeItem,
    onOpenLink: (String) -> Unit,
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

        TextButton(onClick = { onOpenLink(item.url) }) {
            Text("Oeffnen")
        }
    }
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
