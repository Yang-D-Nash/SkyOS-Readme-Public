package com.skydown.android.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.skydown.android.ui.component.AppTopBarSessionActions
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.dismissKeyboardOnTap
import com.skydown.android.ui.component.skydownContentPadding
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.model.SelectedVideoFile
import com.skydown.android.ui.model.VideoHubItem
import com.skydown.android.ui.viewmodel.VideoHubViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoHubScreen(
    onBack: (() -> Unit)? = null,
    initialSelectedVideoId: String? = null,
    autoplayInitialSelection: Boolean = false,
    onOpenCart: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: VideoHubViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val player = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    var selectedVideoId by rememberSaveable { mutableStateOf(initialSelectedVideoId) }
    var hasHandledInitialSelection by rememberSaveable { mutableStateOf(false) }
    var shouldAutoplaySelection by rememberSaveable {
        mutableStateOf(autoplayInitialSelection && !initialSelectedVideoId.isNullOrBlank())
    }
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

    LaunchedEffect(selectedVideo?.downloadUrl) {
        val url = selectedVideo?.downloadUrl
        if (url.isNullOrBlank()) {
            player.stop()
            player.clearMediaItems()
        } else {
            player.setMediaItem(MediaItem.fromUri(url))
            player.prepare()
            if (shouldAutoplaySelection) {
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
                        title = "Videography",
                        subtitle = if (uiState.isAdmin) {
                            "Admin Uploads, Home-Highlights und die komplette Videography-Library."
                        } else {
                            "Oeffentliche Videos direkt schauen, ohne Admin-Overhead."
                        },
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenSettings = onOpenSettings,
                    )
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.94f),
                    scrolledContainerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.98f),
                ),
            )
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
                contentPadding = skydownContentPadding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    VideoHubHeroCard(isAdmin = uiState.isAdmin)
                }

                if (uiState.isAdmin) {
                    item {
                        VideoFormatCard()
                    }

                    item {
                        VideoEquipmentCard()
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

                item {
                    VideoPlayerCard(
                        video = selectedVideo,
                        player = player,
                    )
                }

                item {
                    VideoLibraryCard(
                        uiState = uiState,
                        selectedVideoId = selectedVideoId,
                        onSelectVideo = { video -> selectedVideoId = video.id },
                        onToggleHomeFeatured = viewModel::toggleHomeFeatured,
                        onDeleteVideo = viewModel::deleteVideo,
                    )
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
        }
    }
}

@Composable
private fun VideoHubHeroCard(
    isAdmin: Boolean,
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
                    text = "Skydown Videography",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isAdmin) {
                        "Hier landen Videoarbeiten fuer Reels, Clips, Sessions und Visuals. Admins koennen neue Dateien direkt nach Firebase Storage hochladen."
                    } else {
                        "Hier laufen die oeffentlichen Videoarbeiten von Skydown x 22. Admin-Uploads gehen direkt ueber den internen Bereich."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VideoPill(text = "Videos", isActive = true)
            VideoPill(text = "Admin Upload", isActive = isAdmin)
            VideoPill(text = "Storage", isActive = false)
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
private fun VideoEquipmentCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Equipment & Software")
        Text(
            text = "Damit direkt klar ist, womit Skydown x 22 die Videography umsetzt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VideoEquipmentRow(
                title = "Drohnen",
                detail = "DJI Neo und DJI Avata 2 fuer bewegte Luftshots und FPV-Looks.",
            )
            VideoEquipmentRow(
                title = "Kamera",
                detail = "Sony FX30 mit Sigma 18-50 mm f/2.8 plus Gimbals fuer saubere Motion-Shots.",
            )
            VideoEquipmentRow(
                title = "Mobile Capture",
                detail = "iPhone 16 Pro mit Apple Log fuer flexible schnelle Shoots.",
            )
            VideoEquipmentRow(
                title = "Postproduktion",
                detail = "Adobe Premiere Pro, DaVinci Resolve Studio und Adobe After Effects.",
            )
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
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Player")

        if (video == null) {
            Text(
                text = "Noch kein Video ausgewaehlt. Sobald Uploads live sind, kannst du sie hier direkt abspielen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 12.dp),
            )
            return@SkydownCard
        }

        Text(
            text = video.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            text = video.projectName,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp),
        )

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(20.dp)),
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
            VideoPill(text = if (video.isPublic) "Live" else "Hidden", isActive = video.isPublic)
            if (isAdmin && video.isHomeFeatured) {
                VideoPill(text = "Home", isActive = true)
            } else if (!isAdmin) {
                VideoPill(text = "Clip", isActive = false)
            }
        }

        Button(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(if (isSelected) "Im Player" else "Im Player laden")
        }

        if (isAdmin) {
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
