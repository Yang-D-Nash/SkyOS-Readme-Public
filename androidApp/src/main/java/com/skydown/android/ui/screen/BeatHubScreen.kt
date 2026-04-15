package com.skydown.android.ui.screen

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.data.ArtistPageBrand
import com.skydown.android.data.ArtistPagesStore
import com.skydown.android.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
import com.skydown.android.ui.model.NicmaBeatHubItem
import com.skydown.android.ui.model.NicmaProducerUiState
import com.skydown.android.ui.model.NicmaSelectedBeatFile
import com.skydown.android.ui.viewmodel.NicmaProducerViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeatHubScreen(
    onBack: () -> Unit,
    viewModel: NicmaProducerViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val nicmaPage = ArtistPagesStore.pageFor(brand = ArtistPageBrand.Nicma, artistName = "NICMA MUSIC")
    val context = LocalContext.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var currentBeatId by rememberSaveable { mutableStateOf<String?>(null) }
    val player = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            viewModel.setSelectedFiles(context, uris)
        },
    )
    var showUploadSheet by rememberSaveable { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.stop()
                    player.clearMediaItems()
                    currentBeatId = null
                }
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(uiState.feedbackMessage) {
        if (!uiState.feedbackMessage.isNullOrBlank()) {
            delay(3500)
            viewModel.dismissFeedback()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        title = "Beat Hub",
                        subtitle = if (uiState.isAdmin) {
                            "Beats, Uploads, Freigaben."
                        } else {
                            "Freigegebene Beats."
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                        )
                    }
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
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload oeffnen",
                            )
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
                    BeatHubHeroCard(
                        isAdmin = uiState.isAdmin,
                        heroImageUrl = nicmaPage.heroImageURL,
                    )
                }

                item {
                    BeatHubLibrarySection(
                        uiState = uiState,
                        currentBeatId = currentBeatId,
                        onPlayToggle = { beat ->
                            if (beat.isPlayable) {
                                if (currentBeatId == beat.id) {
                                    player.stop()
                                    player.clearMediaItems()
                                    currentBeatId = null
                                } else {
                                    player.setMediaItem(MediaItem.fromUri(beat.downloadUrl))
                                    player.prepare()
                                    player.play()
                                    currentBeatId = beat.id
                                }
                            }
                        },
                        onOpenOriginal = { beat ->
                            if (beat.openUrl.isNotBlank()) {
                                openExternalLink(context, beat.openUrl)
                            }
                        },
                        onVisibilityToggle = viewModel::toggleBeatVisibility,
                        onDeleteBeat = viewModel::deleteBeat,
                    )
                }
            }

            ToastHost(
                message = uiState.feedbackMessage,
                type = if (uiState.feedbackIsError) ToastType.Error else ToastType.Success,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp),
            )

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
                            BeatHubUploadCard(
                                uiState = uiState,
                                onBeatTitleChanged = viewModel::updateBeatTitle,
                                onArtistNameChanged = viewModel::updateArtistName,
                                onEmailChanged = viewModel::updateEmail,
                                onNotesChanged = viewModel::updateNotes,
                                onExternalBeatUrlChanged = viewModel::updateExternalBeatUrl,
                                onPickFiles = {
                                    dismissKeyboard()
                                    pickerLauncher.launch(
                                        arrayOf(
                                            "audio/*",
                                            "application/zip",
                                            "application/x-zip-compressed",
                                        ),
                                    )
                                },
                                onRemoveFile = viewModel::removeFile,
                                onUpload = {
                                    dismissKeyboard()
                                    viewModel.upload(context)
                                },
                                onAddExternalBeat = {
                                    dismissKeyboard()
                                    viewModel.addExternalBeat()
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
private fun BeatHubHeroCard(
    isAdmin: Boolean,
    heroImageUrl: String?,
) {
    BrandHeroCard(
        eyebrow = "NICMA",
        title = "Beat Hub",
        subtitle = if (isAdmin) {
            "Uploads, Freigaben und Beat-Library in einem Flow."
        } else {
            "Freigegebene Beats direkt hoeren und durchsuchen."
        },
        detail = if (isAdmin) {
            "Header kompakt, Aktionen darunter."
        } else {
            "Public Beat-Library."
        },
        backgroundImageUrl = heroImageUrl?.takeIf { it.isNotBlank() },
        accent = MaterialTheme.colorScheme.primary,
        secondaryAccent = MaterialTheme.colorScheme.tertiary,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = if (isAdmin) "Upload" else "Curated", tint = MaterialTheme.colorScheme.primary)
            BrandPill(text = "Listen", tint = MaterialTheme.colorScheme.secondary)
            BrandPill(text = if (isAdmin) "Admin aktiv" else "Public Beats", tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun BeatHubUploadCard(
    uiState: NicmaProducerUiState,
    onBeatTitleChanged: (String) -> Unit,
    onArtistNameChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onNotesChanged: (String) -> Unit,
    onExternalBeatUrlChanged: (String) -> Unit,
    onPickFiles: () -> Unit,
    onRemoveFile: (NicmaSelectedBeatFile) -> Unit,
    onUpload: () -> Unit,
    onAddExternalBeat: () -> Unit,
) {
    val context = LocalContext.current

    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Beat Upload")
        Text(
            text = "Nur Admins laden neue Beats oder ZIP-Sessions hoch. Audio-Dateien koennen danach direkt im Hub getestet und bei Bedarf verborgen werden.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        OutlinedTextField(
            value = uiState.beatTitle,
            onValueChange = onBeatTitleChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            label = { Text("Beat Titel (optional)") },
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.artistName,
            onValueChange = onArtistNameChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Artist / Projekt") },
            singleLine = true,
        )

        OutlinedTextField(
            value = uiState.email,
            onValueChange = onEmailChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("E-Mail") },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Email),
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = onNotesChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            label = { Text("Notiz (optional)") },
            minLines = 3,
            maxLines = 5,
        )

        OutlinedButton(
            onClick = onPickFiles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Icon(
                imageVector = Icons.Default.CloudUpload,
                contentDescription = null,
            )
            Text(
                text = "Audio-Dateien oder ZIP waehlen",
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        if (uiState.selectedFiles.isNotEmpty()) {
            Column(
                modifier = Modifier.padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                uiState.selectedFiles.forEach { file ->
                    BeatHubSelectedFileRow(
                        file = file,
                        fileSize = Formatter.formatShortFileSize(context, file.fileSizeBytes),
                        onRemove = { onRemoveFile(file) },
                    )
                }
            }
        }

        Text(
            text = "Oder als externer Beat-Link freigeben.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 14.dp),
        )

        OutlinedTextField(
            value = uiState.externalBeatUrl,
            onValueChange = onExternalBeatUrlChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            label = { Text("Drive / MEGA / anderer Audio-Link") },
            minLines = 2,
        )

        if (!uiState.validationMessage.isNullOrBlank()) {
            Text(
                text = uiState.validationMessage.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
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
                Text("In den Beat Hub hochladen")
            }
        }

        OutlinedButton(
            onClick = onAddExternalBeat,
            enabled = !uiState.isUploading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text("Externen Beat freigeben")
        }
    }
}

@Composable
private fun BeatHubLibrarySection(
    uiState: NicmaProducerUiState,
    currentBeatId: String?,
    onPlayToggle: (NicmaBeatHubItem) -> Unit,
    onOpenOriginal: (NicmaBeatHubItem) -> Unit,
    onVisibilityToggle: (NicmaBeatHubItem) -> Unit,
    onDeleteBeat: (NicmaBeatHubItem) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Beat Library")
        Text(
            text = if (uiState.isAdmin) {
                "${uiState.beats.size} Beats im Hub. Admins sehen auch private Uploads."
            } else {
                "${uiState.beats.size} freigegebene Beats im Hub."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        when {
            uiState.isLoadingBeats -> {
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                    )
                    Text(
                        text = "Beat Hub wird geladen ...",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }

            uiState.beats.isEmpty() -> {
                Text(
                    text = if (uiState.isAdmin) {
                        "Noch keine Beats im Hub. Neue Uploads tauchen hier sofort auf."
                    } else {
                        "Noch keine Beats verfuegbar. Sobald ein Beat live geschaltet wird, erscheint er hier."
                    },
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
                    uiState.beats.forEach { beat ->
                        BeatHubLibraryRow(
                            beat = beat,
                            isAdmin = uiState.isAdmin,
                            isPlaying = currentBeatId == beat.id,
                            onPlayToggle = { onPlayToggle(beat) },
                            onOpenOriginal = { onOpenOriginal(beat) },
                            onVisibilityToggle = { onVisibilityToggle(beat) },
                            onDelete = { onDeleteBeat(beat) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BeatHubLibraryRow(
    beat: NicmaBeatHubItem,
    isAdmin: Boolean,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onOpenOriginal: () -> Unit,
    onVisibilityToggle: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (beat.isPlayable) Icons.Default.MusicNote else Icons.Default.CloudUpload,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = beat.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (isAdmin) {
                    "${beat.artistName} • ${beat.uploaderName}"
                } else {
                    beat.artistName
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )

            if (beat.notes.isNotBlank()) {
                Text(
                    text = beat.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BeatHubBadge(
                    text = if (beat.isPublic) "Live" else "Review",
                    isActive = beat.isPublic,
                )
                BeatHubBadge(
                    text = when (beat.provider.rawValue) {
                        "google_drive" -> "Drive"
                        "mega" -> "MEGA"
                        "external_link" -> "Extern"
                        else -> "Storage"
                    },
                    isActive = false,
                )
                if (isAdmin) {
                    BeatHubBadge(
                        text = beat.fileName,
                        isActive = false,
                    )
                } else if (beat.isPlayable) {
                    BeatHubBadge(
                        text = "Preview",
                        isActive = false,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (beat.isPlayable) {
                    FilledTonalButton(
                        onClick = onPlayToggle,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Text(
                            text = if (isPlaying) "Stoppen" else "Abspielen",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenOriginal,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (beat.openUrl.isNotBlank()) "Original oeffnen" else "Nicht direkt abspielbar")
                    }
                }

                if (isAdmin) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onVisibilityToggle,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                imageVector = if (beat.isPublic) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                            )
                            Text(
                                text = if (beat.isPublic) "Verbergen" else "Freigeben",
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }

                        OutlinedButton(
                            onClick = onDelete,
                            modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun BeatHubSelectedFileRow(
    file: NicmaSelectedBeatFile,
    fileSize: String,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = fileSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
        }

        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Datei entfernen",
            )
        }
    }
}

@Composable
private fun BeatHubBadge(
    text: String,
    isActive: Boolean,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isActive) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surface
                },
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
