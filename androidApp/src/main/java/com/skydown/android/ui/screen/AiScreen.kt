package com.skydown.android.ui.screen

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skydown.android.ui.component.BrandArtwork
import com.skydown.android.ui.component.BrandActionButton
import com.skydown.android.ui.component.BrandHeroCard
import com.skydown.android.ui.component.BrandHeroMetricCard
import com.skydown.android.ui.component.BrandPill
import com.skydown.android.ui.component.BrandSectionBanner
import com.skydown.android.ui.component.BrandStatusChip
import com.skydown.android.ui.component.SkydownCard
import com.skydown.android.ui.component.SkydownTopBarTitle
import com.skydown.android.ui.component.SkydownUiTokens
import com.skydown.android.ui.component.ToastHost
import com.skydown.android.ui.component.ToastType
import com.skydown.android.ui.component.rememberIsCompactAppLayout
import com.skydown.android.ui.component.rememberUsesCompactVisualDensity
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
    val baseCompactLayout = rememberIsCompactAppLayout()
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val compactLayout = baseCompactLayout || compactVisualDensity
    val contentMaxWidth = if (compactVisualDensity) 620.dp else Dp.Unspecified
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
            Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        } else {
            Modifier.fillMaxSize()
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = if (showTopBar) {
            {
                TopAppBar(
                    title = {
                        SkydownTopBarTitle(
                            title = "Atelier",
                            subtitle = "Private Text- und Visual Direction.",
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
                    contentMaxWidth = contentMaxWidth,
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
                .testTag("ai.screen.root")
                .background(
                    skydownScreenBrush(
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary,
                        primaryAlpha = 0.058f,
                        secondaryAlpha = 0.038f,
                    ),
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                if (uiState.isAiEnabled && uiState.messages.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(
                                start = SkydownUiTokens.screenHorizontalPadding,
                                top = innerPadding.calculateTopPadding() + if (showTopBar) 2.dp else 0.dp,
                                end = SkydownUiTokens.screenHorizontalPadding,
                                bottom = innerPadding.calculateBottomPadding(),
                            ),
                        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 10.dp else 12.dp),
                    ) {
                        AiCommandHeroCard(compactVisualDensity = compactLayout)
                        AiSessionStrip(
                            composerMode = uiState.composerMode,
                            textMode = uiState.textMode,
                            messageCount = uiState.messages.size,
                            compactVisualDensity = compactLayout,
                        )
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
                        modifier = Modifier
                            .widthIn(max = contentMaxWidth)
                            .fillMaxWidth()
                            .fillMaxHeight(),
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
                                0.dp
                            },
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
                    ) {
                        if (!uiState.isAiEnabled) {
                            item {
                                AiDisabledCard()
                            }
                        } else {
                            item {
                                AiOverviewCard(isEnabled = true)
                            }

                            item {
                                AiSessionStrip(
                                    composerMode = uiState.composerMode,
                                    textMode = uiState.textMode,
                                    messageCount = uiState.messages.size,
                                    compactVisualDensity = compactLayout,
                                )
                            }

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
private fun AiCommandHeroCard(
    compactVisualDensity: Boolean,
) {
    BrandHeroCard(
        eyebrow = "SKY OS",
        title = "KI Atelier",
        subtitle = "Text, Visuals und Konzepte im privaten Sky-Flow.",
        detail = "Persoenlicher Bot. Kontext bleibt exklusiv pro Konto verbunden.",
        accent = MaterialTheme.colorScheme.primary,
        secondaryAccent = MaterialTheme.colorScheme.tertiary,
        marks = listOf(BrandArtwork.Combined),
        compactVisualDensity = compactVisualDensity,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandPill(text = "Private", tint = MaterialTheme.colorScheme.primary)
            BrandPill(text = "Visual", tint = MaterialTheme.colorScheme.tertiary)
            BrandPill(text = "Memory", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
private fun AiOverviewCard(
    isEnabled: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(14.dp)) {
        BrandSectionBanner(
            title = if (isEnabled) "Atelier live" else "Atelier standby",
            subtitle = "Text, Visuals und Stilentscheidungen laufen in einer privaten Session.",
            accent = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.AutoAwesome,
            tag = if (isEnabled) "Owner Mode" else "Standby",
        )
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandHeroMetricCard(
                label = "OUTPUT",
                value = "Text + Visual",
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                isActive = isEnabled,
            )
            BrandHeroMetricCard(
                label = "STATUS",
                value = if (isEnabled) "Online" else "Paused",
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                isActive = isEnabled,
            )
        }
        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BrandStatusChip(
                text = "Private Context",
                accent = MaterialTheme.colorScheme.primary,
                isActive = isEnabled,
            )
            BrandStatusChip(
                text = "Visual Pipeline",
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = isEnabled,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.04f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                        ),
                    ),
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        ),
                    ),
                    shape = RoundedCornerShape(18.dp),
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text(
                text = "Direkter Zugriff auf Caption-, Konzept- und Bildarbeit ohne Toolbruch oder Kontextverlust.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            )
        }
    }
}

@Composable
private fun AiSessionStrip(
    composerMode: AiComposerMode,
    textMode: AiTextMode,
    messageCount: Int,
    compactVisualDensity: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 8.dp else 10.dp)) {
        BrandSectionBanner(
            title = "Session",
            subtitle = "Private Kontextlage mit direktem Fokus fuer Copy und Visuals.",
            accent = MaterialTheme.colorScheme.primary,
            tag = if (messageCount == 0) "Fresh" else "$messageCount Steps",
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            item {
                AiSessionSignalCard(
                    title = "Mode",
                    value = if (composerMode == AiComposerMode.Visual) "Visual" else "Text",
                    detail = if (composerMode == AiComposerMode.Visual) "Bild-Generierung" else "Text-Produktion",
                    accent = MaterialTheme.colorScheme.primary,
                    compactVisualDensity = compactVisualDensity,
                )
            }
            item {
                AiSessionSignalCard(
                    title = "Focus",
                    value = if (composerMode == AiComposerMode.Visual) "Cinematic" else textMode.title,
                    detail = if (composerMode == AiComposerMode.Visual) "Prompt -> Visual" else "Textmodus direkt steuerbar",
                    accent = MaterialTheme.colorScheme.tertiary,
                    compactVisualDensity = compactVisualDensity,
                )
            }
            item {
                AiSessionSignalCard(
                    title = "Memory",
                    value = if (messageCount == 0) "Neu" else "$messageCount Steps",
                    detail = "Verlauf bleibt pro Konto aktiv",
                    accent = MaterialTheme.colorScheme.secondary,
                    compactVisualDensity = compactVisualDensity,
                )
            }
        }
    }
}

@Composable
private fun AiSessionSignalCard(
    title: String,
    value: String,
    detail: String,
    accent: Color,
    compactVisualDensity: Boolean,
) {
    BrandHeroMetricCard(
        label = title.uppercase(),
        value = value,
        accent = accent,
        modifier = Modifier.widthIn(min = if (compactVisualDensity) 148.dp else 164.dp),
        isActive = true,
    )
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
                    text = "SkyOS Bot pausiert",
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
        BrandSectionBanner(
            title = "Prompt Library",
            subtitle = "Kuratiert fuer schnelle, markentaugliche Starts.",
            accent = MaterialTheme.colorScheme.primary,
            tag = "Text",
        )
        LazyRow(
            modifier = Modifier.padding(top = if (compactLayout) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts) { prompt ->
                AiPromptActionCard(
                    eyebrow = "CURATED",
                    text = prompt,
                    accent = MaterialTheme.colorScheme.primary,
                    compactLayout = compactLayout,
                    onClick = { onPromptSelected(prompt) },
                )
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
    SkydownCard(
        modifier = Modifier.testTag("ai.visual.prompt.card"),
        contentPadding = PaddingValues(if (compactLayout) 12.dp else 14.dp),
    ) {
        BrandSectionBanner(
            title = "Visual Deck",
            subtitle = "Sofort cineastische Startpunkte fuer Artwork und Story Frames.",
            accent = MaterialTheme.colorScheme.tertiary,
            tag = "Visual",
        )
        LazyRow(
            modifier = Modifier.padding(top = if (compactLayout) 8.dp else 10.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts, key = { it.label }) { prompt ->
                AiPromptActionCard(
                    eyebrow = "SHOT",
                    text = prompt.label,
                    detail = "Tap fuer cineastischen Start",
                    accent = MaterialTheme.colorScheme.tertiary,
                    compactLayout = compactLayout,
                    modifier = Modifier.testTag("ai.visual.prompt.button"),
                    onClick = { onPromptSelected(prompt.prompt) },
                )
            }
        }
    }
}

@Composable
private fun AiPromptActionCard(
    eyebrow: String,
    text: String,
    accent: Color,
    compactLayout: Boolean,
    modifier: Modifier = Modifier,
    detail: String? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(if (compactLayout) 18.dp else 20.dp)

    Box(
        modifier = modifier
            .widthIn(min = if (detail == null) 208.dp else 176.dp, max = if (detail == null) 248.dp else 214.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                        accent.copy(alpha = 0.13f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
                    ),
                ),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.16f),
                        accent.copy(alpha = 0.24f),
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                    ),
                ),
                shape = shape,
            ),
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compactLayout) 12.dp else 14.dp,
                vertical = if (compactLayout) 11.dp else 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
        ) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = accent.copy(alpha = 0.88f),
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = text,
                style = if (detail == null) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (detail == null) FontWeight.Medium else FontWeight.Bold,
                maxLines = if (detail == null) 3 else 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.70f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            BrandActionButton(
                text = if (detail == null) "Starten" else "Rendern",
                onClick = onClick,
                accent = accent,
                filled = false,
                compact = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
fun AiMessageBubble(
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
    val bubbleBrush = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.secondary,
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.92f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.06f),
                MaterialTheme.colorScheme.surface.copy(alpha = 0.99f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.70f),
            ),
        )
    }
    val bubbleBorder = if (isUser) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.24f),
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.34f),
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
            ),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .testTag("ai.message.bubble")
                .clip(bubbleShape)
                .background(bubbleBrush)
                .border(
                    width = 1.dp,
                    brush = bubbleBorder,
                    shape = bubbleShape,
                )
                .padding(
                    horizontal = if (compactLayout) 12.dp else 14.dp,
                    vertical = if (compactLayout) 10.dp else 12.dp,
                ),
        ) {
            Text(
                text = if (isUser) "Du" else "Atelier",
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
                        text = "Atelier komponiert gerade...",
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
                            .testTag("ai.message.visual")
                            .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
                    )
                }

                if (!isUser && !message.isStreaming) {
                    LazyRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(end = 4.dp),
                    ) {
                        item {
                            BrandStatusChip(
                                text = "Kopieren",
                                accent = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    copyAiText(context, "SkyOS Bot", message.text)
                                    onFeedback("Antwort kopiert.", ToastType.Success)
                                },
                            )
                        }

                        item {
                            BrandStatusChip(
                                text = "Teilen",
                                accent = MaterialTheme.colorScheme.secondary,
                                onClick = {
                                    shareAiText(context, "SkyOS Bot", message.text)
                                    onFeedback("Share-Sheet geoeffnet.", ToastType.Info)
                                },
                            )
                        }

                        if (message.imageBytes != null) {
                            item {
                                BrandStatusChip(
                                    text = "Speichern",
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.testTag("ai.message.save"),
                                    onClick = {
                                        saveAiImage(context, message.imageBytes, message.imageMimeType)
                                            .onSuccess {
                                            onFeedback("Bild gespeichert.", ToastType.Success)
                                            }
                                            .onFailure {
                                            onFeedback("Bild konnte nicht gespeichert werden.", ToastType.Error)
                                            }
                                    },
                                )
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
    contentMaxWidth: Dp,
    embeddedInTools: Boolean,
    applyBottomSystemInset: Boolean,
    onDraftChanged: (String) -> Unit,
    onComposerModeChange: (AiComposerMode) -> Unit,
    onTextModeChange: (AiTextMode) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
    onDismissKeyboard: () -> Unit,
) {
    val composerAccent = if (composerMode == AiComposerMode.Visual) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    val outerVerticalPadding = when {
        embeddedInTools -> 0.dp
        compactLayout -> 6.dp
        else -> 8.dp
    }
    val cardVerticalPadding = when {
        embeddedInTools -> 4.dp
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
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = contentMaxWidth)
                .fillMaxWidth(),
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
                    BrandActionButton(
                        text = "Text",
                        onClick = { onComposerModeChange(AiComposerMode.Text) },
                        accent = MaterialTheme.colorScheme.primary,
                        filled = composerMode == AiComposerMode.Text,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )

                    BrandActionButton(
                        text = "Visual",
                        onClick = { onComposerModeChange(AiComposerMode.Visual) },
                        accent = MaterialTheme.colorScheme.tertiary,
                        filled = composerMode == AiComposerMode.Visual,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )

                    BrandActionButton(
                        text = "Neu",
                        onClick = onReset,
                        accent = MaterialTheme.colorScheme.secondary,
                        icon = Icons.Default.Refresh,
                        filled = false,
                        compact = true,
                        enabled = !isSending,
                        modifier = Modifier.widthIn(min = if (embeddedInTools) 88.dp else 96.dp),
                    )
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
                            BrandStatusChip(
                                text = mode.title,
                                accent = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
                                },
                                isActive = isSelected,
                                onClick = { onTextModeChange(mode) },
                            )
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
                    shape = RoundedCornerShape(if (compactLayout) 18.dp else 20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = composerAccent.copy(alpha = 0.72f),
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        cursorColor = composerAccent,
                        focusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.46f),
                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                    ),
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = sectionSpacing),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BrandStatusChip(
                        text = if (composerMode == AiComposerMode.Text) {
                            textMode.title
                        } else {
                            "Cinematic"
                        },
                        accent = composerAccent,
                        isActive = true,
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrandActionButton(
                            text = "Ausblenden",
                            onClick = onDismissKeyboard,
                            accent = MaterialTheme.colorScheme.secondary,
                            icon = Icons.Default.KeyboardArrowDown,
                            filled = false,
                            compact = true,
                        )
                        BrandActionButton(
                            text = if (composerMode == AiComposerMode.Text) "Senden" else "Rendern",
                            onClick = {
                                onSend()
                                onDismissKeyboard()
                            },
                            accent = composerAccent,
                            icon = Icons.AutoMirrored.Filled.Send,
                            filled = true,
                            compact = true,
                            enabled = draft.isNotBlank() && !isSending,
                            isLoading = isSending,
                        )
                    }
                }
            }
        }
    }
}
