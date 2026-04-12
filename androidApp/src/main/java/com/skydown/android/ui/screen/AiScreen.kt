package com.skydown.android.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.SectionHeader
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.skydownScreenBrush
import com.skydown.android.ui.component.skydownTopBarColors
import com.skydown.android.ui.model.AiComposerMode
import com.skydown.android.ui.model.AiMessage
import com.skydown.android.ui.model.AiMessageRole
import com.skydown.android.ui.model.AiTextMode
import com.skydown.android.ui.model.AiVisualPrompt
import com.skydown.android.ui.viewmodel.AiViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: AiViewModel = viewModel(),
    showTopBar: Boolean = true,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val compactLayout = rememberIsCompactAppLayout()
    val listState = rememberLazyListState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var localFeedbackType by remember { mutableStateOf(ToastType.Info) }

    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.isAiEnabled && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex + 2)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailability()
    }

    LaunchedEffect(uiState.errorMessage) {
        if (!uiState.errorMessage.isNullOrBlank()) {
            delay(3500)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(localFeedbackMessage) {
        if (!localFeedbackMessage.isNullOrBlank()) {
            delay(3000)
            localFeedbackMessage = null
        }
    }

    Scaffold(
        modifier = if (showTopBar) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier
        },
        containerColor = Color.Transparent,
        topBar = if (showTopBar) {
            {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = "Bot",
                            subtitle = "Hooks, Captions, Visuals.",
                        )
                    },
                    colors = skydownTopBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            }
        } else {
            {}
        },
        bottomBar = {
            if (uiState.isAiEnabled) {
                AiComposerBar(
                    draft = uiState.draft,
                    composerMode = uiState.composerMode,
                    textMode = uiState.textMode,
                    isSending = uiState.isSending,
                    compactLayout = compactLayout,
                    embeddedInTools = !showTopBar,
                    applyBottomSystemInset = showTopBar,
                    onDraftChanged = viewModel::updateDraft,
                    onComposerModeChange = viewModel::updateComposerMode,
                    onTextModeChange = viewModel::updateTextMode,
                    onSend = {
                        viewModel.sendDraft()
                        dismissKeyboard()
                    },
                    onReset = {
                        dismissKeyboard()
                        viewModel.resetConversation()
                    },
                    onDismissKeyboard = dismissKeyboard,
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    skydownScreenBrush(
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.08f,
                        secondaryAlpha = 0.06f,
                    ),
                )
        ) {
            if (uiState.isAiEnabled && uiState.messages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = SkydownUiTokens.screenHorizontalPadding,
                            top = innerPadding.calculateTopPadding() + if (showTopBar) 2.dp else 0.dp,
                            end = SkydownUiTokens.screenHorizontalPadding,
                            bottom = innerPadding.calculateBottomPadding(),
                        ),
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    AiEmptyStateHeader()
                    QuickPromptCard(
                        prompts = uiState.quickPrompts,
                        onPromptSelected = { prompt ->
                            dismissKeyboard()
                            viewModel.sendPrompt(prompt)
                        },
                        compactLayout = compactLayout,
                    )
                    VisualPromptCard(
                        prompts = uiState.visualPrompts,
                        onPromptSelected = { prompt ->
                            dismissKeyboard()
                            viewModel.generateVisual(prompt)
                        },
                        compactLayout = compactLayout,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = SkydownUiTokens.screenHorizontalPadding,
                        top = if (showTopBar) {
                            innerPadding.calculateTopPadding()
                        } else {
                            0.dp
                        },
                        end = SkydownUiTokens.screenHorizontalPadding,
                        bottom = innerPadding.calculateBottomPadding() + if (showTopBar) {
                            2.dp
                        } else {
                            4.dp
                        },
                    ),
                    verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
                ) {
                    if (!uiState.isAiEnabled) {
                        item {
                            AiDisabledCard()
                        }
                    } else {
                        items(uiState.messages, key = { it.id }) { message ->
                            AiMessageBubble(
                                message = message,
                                compactLayout = compactLayout,
                                onFeedback = { messageText, type ->
                                    localFeedbackMessage = messageText
                                    localFeedbackType = type
                                },
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    }
                }
            }

            ToastHost(
                message = localFeedbackMessage ?: uiState.errorMessage,
                type = if (localFeedbackMessage != null) localFeedbackType else ToastType.Error,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = if (uiState.isAiEnabled) {
                        if (compactLayout) 64.dp else 76.dp
                    } else {
                        28.dp
                    }),
            )
        }
    }
}

@Composable
private fun AiEmptyStateHeader() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Was brauchst du?",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Kurz tippen und los.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
        Text(
            text = "Dein Verlauf wird pro Konto weitergefuehrt, damit der Bot nicht jedes Mal bei null startet.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
        )
    }
}

@Composable
private fun AiOverviewCard(
    isEnabled: Boolean,
) {
    SkydownCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Skydown x 22 Bot",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = if (isEnabled) "X22 Bot aktiv" else "X22 Bot pausiert",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "Captions, Hooks, Skripte, Visuals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun AiDisabledCard() {
    SkydownCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "X22 Bot pausiert",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Voruebergehend nicht verfuegbar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

    }
}

@Composable
private fun QuickPromptCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    compactLayout: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(if (compactLayout) 12.dp else 14.dp)) {
        SectionHeader("Prompts")
        LazyRow(
            modifier = Modifier.padding(top = if (compactLayout) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts) { prompt ->
                OutlinedButton(
                    onClick = { onPromptSelected(prompt) },
                    modifier = Modifier.widthIn(min = 190.dp, max = 236.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = if (compactLayout) 8.dp else 10.dp,
                    ),
                    shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                ) {
                    Text(
                        text = prompt,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun VisualPromptCard(
    prompts: List<AiVisualPrompt>,
    onPromptSelected: (String) -> Unit,
    compactLayout: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(if (compactLayout) 12.dp else 14.dp)) {
        SectionHeader("Visuals")
        LazyRow(
            modifier = Modifier.padding(top = if (compactLayout) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts, key = { it.label }) { prompt ->
                OutlinedButton(
                    onClick = { onPromptSelected(prompt.prompt) },
                    modifier = Modifier.widthIn(min = 156.dp, max = 190.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = if (compactLayout) 8.dp else 10.dp,
                    ),
                    shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                ) {
                    Text(
                        text = prompt.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiMessageBubble(
    message: AiMessage,
    compactLayout: Boolean,
    onFeedback: (String, ToastType) -> Unit,
) {
    val context = LocalContext.current
    val isUser = message.role == AiMessageRole.User
    val bubbleShape = RoundedCornerShape(
        topStart = SkydownUiTokens.cardCornerRadius,
        topEnd = SkydownUiTokens.cardCornerRadius,
        bottomStart = if (isUser) SkydownUiTokens.cardCornerRadius else 10.dp,
        bottomEnd = if (isUser) 10.dp else SkydownUiTokens.cardCornerRadius,
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .clip(bubbleShape)
                .background(
                    if (isUser) {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                            ),
                        )
                    } else {
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ),
                        )
                    },
                )
                .padding(
                    horizontal = if (compactLayout) 12.dp else 14.dp,
                    vertical = if (compactLayout) 10.dp else 12.dp,
                ),
        ) {
            Text(
                text = if (isUser) "Du" else "X22 Bot",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
            )

            if (message.isStreaming && message.text.isBlank()) {
                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Text(
                        text = "X22 Bot antwortet gerade...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = Modifier.padding(top = 8.dp),
                )

                val generatedBitmap = remember(message.imageBytes) {
                    message.imageBytes?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }

                if (generatedBitmap != null) {
                    Image(
                        bitmap = generatedBitmap.asImageBitmap(),
                        contentDescription = "Generiertes Visual",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(220.dp)
                            .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
                    )
                }

                if (!isUser && !message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Button(
                            onClick = {
                                copyAiText(context, "X22 Bot", message.text)
                                onFeedback("Antwort kopiert.", ToastType.Success)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Kopieren")
                        }

                        OutlinedButton(
                            onClick = {
                                shareAiText(context, "X22 Bot", message.text)
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text("Teilen")
                        }

                        if (message.imageBytes != null) {
                            OutlinedButton(
                            onClick = {
                                saveAiImage(context, message.imageBytes, message.imageMimeType)
                                    .onSuccess {
                                            onFeedback("Bild gespeichert.", ToastType.Success)
                                    }
                                    .onFailure {
                                            onFeedback("Bild konnte nicht gespeichert werden.", ToastType.Error)
                                    }
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text("Bild speichern")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiComposerBar(
    draft: String,
    composerMode: AiComposerMode,
    textMode: AiTextMode,
    isSending: Boolean,
    compactLayout: Boolean,
    embeddedInTools: Boolean,
    applyBottomSystemInset: Boolean,
    onDraftChanged: (String) -> Unit,
    onComposerModeChange: (AiComposerMode) -> Unit,
    onTextModeChange: (AiTextMode) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    val outerVerticalPadding = when {
        embeddedInTools -> 2.dp
        compactLayout -> 6.dp
        else -> 8.dp
    }
    val cardVerticalPadding = when {
        embeddedInTools -> 6.dp
        compactLayout -> 8.dp
        else -> 10.dp
    }
    val sectionSpacing = if (embeddedInTools) 6.dp else if (compactLayout) 8.dp else 10.dp
    val fieldMaxLines = if (embeddedInTools || compactLayout) 3 else 4

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.96f))
            .windowInsetsPadding(
                (if (applyBottomSystemInset) {
                    WindowInsets.navigationBars.union(WindowInsets.ime)
                } else {
                    WindowInsets.ime
                })
                    .only(WindowInsetsSides.Bottom),
            )
            .padding(
                horizontal = if (compactLayout) 8.dp else 10.dp,
                vertical = outerVerticalPadding,
            ),
    ) {
        SkydownCard(
            contentPadding = PaddingValues(
                horizontal = if (compactLayout) 10.dp else 12.dp,
                vertical = cardVerticalPadding,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (composerMode == AiComposerMode.Text) {
                    Button(
                        onClick = { onComposerModeChange(AiComposerMode.Text) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                    ) {
                        Text("Text")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onComposerModeChange(AiComposerMode.Text) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                    ) {
                        Text("Text")
                    }
                }

                if (composerMode == AiComposerMode.Visual) {
                    Button(
                        onClick = { onComposerModeChange(AiComposerMode.Visual) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                    ) {
                        Text("Visual")
                    }
                } else {
                    OutlinedButton(
                        onClick = { onComposerModeChange(AiComposerMode.Visual) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                    ) {
                        Text("Visual")
                    }
                }

                IconButton(
                    onClick = onReset,
                    enabled = !isSending,
                    modifier = Modifier.size(if (embeddedInTools) 32.dp else 36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Chat zuruecksetzen",
                    )
                }
            }

            if (composerMode == AiComposerMode.Text) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sectionSpacing),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp),
                ) {
                    items(AiTextMode.entries, key = { it.rawValue }) { mode ->
                        val isSelected = mode == textMode
                        if (isSelected) {
                            Button(
                                onClick = { onTextModeChange(mode) },
                                shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(mode.title)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onTextModeChange(mode) },
                                shape = RoundedCornerShape(SkydownUiTokens.buttonCornerRadius),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Text(mode.title)
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing),
                placeholder = {
                    Text(
                        if (composerMode == AiComposerMode.Text) {
                            textMode.placeholder
                        } else {
                            "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release."
                        },
                    )
                },
                minLines = 1,
                maxLines = fieldMaxLines,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onSend()
                        onDismissKeyboard()
                    },
                ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = sectionSpacing),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onDismissKeyboard,
                    modifier = Modifier.size(if (embeddedInTools) 32.dp else 36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Tastatur ausblenden",
                    )
                }

                FilledIconButton(
                    onClick = {
                        onSend()
                        onDismissKeyboard()
                    },
                    enabled = draft.isNotBlank() && !isSending,
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (composerMode == AiComposerMode.Text) "Senden" else "Visual generieren",
                    )
                }
            }
        }
    }
}
