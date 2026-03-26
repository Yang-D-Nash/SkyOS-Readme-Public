package com.skydown.android.ui.screen

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
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
    val context = LocalContext.current
    var currentBeatId by rememberSaveable { mutableStateOf<String?>(null) }
    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = true
        }
    }
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            viewModel.setSelectedFiles(context, uris)
        },
    )

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
            LargeTopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Beat Hub",
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (uiState.isAdmin) {
                                "Alle Beats, Hoerproben und Freigaben an einem Ort."
                            } else {
                                "Freigegebene Beats hoeren. Uploads sind nur fuer Admins verfuegbar."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Zurueck",
                        )
                    }
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
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    end = 16.dp,
                    bottom = innerPadding.calculateBottomPadding() + 28.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    BeatHubHeroCard(isAdmin = uiState.isAdmin)
                }

                item {
                    if (uiState.isAdmin) {
                        BeatHubUploadCard(
                            uiState = uiState,
                            onBeatTitleChanged = viewModel::updateBeatTitle,
                            onArtistNameChanged = viewModel::updateArtistName,
                            onEmailChanged = viewModel::updateEmail,
                            onNotesChanged = viewModel::updateNotes,
                            onPickFiles = {
                                pickerLauncher.launch(
                                    arrayOf(
                                        "audio/*",
                                        "application/zip",
                                        "application/x-zip-compressed",
                                    ),
                                )
                            },
                            onRemoveFile = viewModel::removeFile,
                            onUpload = { viewModel.upload(context) },
                        )
                    } else {
                        BeatHubListenerCard()
                    }
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
                        onVisibilityToggle = viewModel::toggleBeatVisibility,
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
        }
    }
}

@Composable
private fun BeatHubHeroCard(
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
                    text = "Beat Hub",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isAdmin) {
                        "Du siehst alle Beats, kannst sie direkt anhoeren und per Tap oeffentlich oder privat schalten."
                    } else {
                        "Der Hub ist fuer die oeffentliche Beat-Library gedacht. Du kannst hier freigegebene Beats direkt anhoeren."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BeatHubBadge(text = if (isAdmin) "Upload" else "Curated", isActive = true)
            BeatHubBadge(text = "Listen", isActive = false)
            BeatHubBadge(text = if (isAdmin) "Admin aktiv" else "Public Beats", isActive = false)
        }
    }
}

@Composable
private fun BeatHubListenerCard() {
    SkydownCard(contentPadding = PaddingValues(18.dp)) {
        SectionHeader("Listening Only")
        Text(
            text = "Dieser Bereich ist fuer Horer gedacht. Neue Uploads und die Pflege der Library bleiben im Admin-Bereich.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp),
        )

        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BeatHubBadge(text = "Public Beats", isActive = true)
            BeatHubBadge(text = "Preview", isActive = false)
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
    onPickFiles: () -> Unit,
    onRemoveFile: (NicmaSelectedBeatFile) -> Unit,
    onUpload: () -> Unit,
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
    }
}

@Composable
private fun BeatHubLibrarySection(
    uiState: NicmaProducerUiState,
    currentBeatId: String?,
    onPlayToggle: (NicmaBeatHubItem) -> Unit,
    onVisibilityToggle: (NicmaBeatHubItem) -> Unit,
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
                            onVisibilityToggle = { onVisibilityToggle(beat) },
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
    onVisibilityToggle: () -> Unit,
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
                text = "${beat.artistName} • ${beat.uploaderName}",
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
                    text = beat.fileName,
                    isActive = false,
                )
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
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Nicht direkt abspielbar")
                    }
                }

                if (isAdmin) {
                    OutlinedButton(
                        onClick = onVisibilityToggle,
                        modifier = Modifier.weight(1f),
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
