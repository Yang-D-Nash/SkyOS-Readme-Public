package com.nash.skyos.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandHeroMetricCard
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.AiConversationSessionStrip
import com.nash.skyos.ui.component.AiConversationSessionsSheet
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.ToastHost
import com.nash.skyos.ui.component.ToastType
import com.nash.skyos.ui.component.rememberIsCompactAppLayout
import com.nash.skyos.ui.component.rememberUsesCompactVisualDensity
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.BotInteractionPhase
import com.nash.skyos.ui.model.AiComposerMode
import com.nash.skyos.ui.model.AiExperienceLevel
import com.nash.skyos.ui.model.AiMessage
import com.nash.skyos.ui.model.AiMessageRole
import com.nash.skyos.ui.model.AiTextMode
import com.nash.skyos.ui.model.AiVisualPrompt
import com.nash.skyos.ui.viewmodel.AiViewModel
import com.nash.skyos.data.AiMembershipCoordinator
import com.nash.skyos.data.AiMembershipUiState
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.MembershipOpenReason
import com.nash.skyos.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    viewModel: AiViewModel = viewModel(),
    showTopBar: Boolean = true,
    immersiveInTools: Boolean = false,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val baseCompactLayout = rememberIsCompactAppLayout()
    val compactVisualDensity = rememberUsesCompactVisualDensity()
    val compactLayout = baseCompactLayout || compactVisualDensity
    val contentMaxWidth = if (compactLayout) 620.dp else 1040.dp
    val listState = rememberLazyListState()
    val conversationEndItemIndex = if (uiState.messages.isEmpty()) 0 else uiState.messages.size + 1
    val lastMessageRenderSignature = uiState.messages.lastOrNull()?.let { message ->
        "${message.id}:${message.text.length}:${message.imageBytes?.size ?: 0}:${message.isStreaming}"
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val membershipCoordinator = remember(context) { AiMembershipCoordinator(context.applicationContext) }
    val membershipState by membershipCoordinator.uiState.collectAsStateWithLifecycle()
    var localFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var localFeedbackType by remember { mutableStateOf(ToastType.Info) }
    var pendingImageSave by remember { mutableStateOf<Pair<ByteArray, String?>?>(null) }
    var showPromptComposer by rememberSaveable { mutableStateOf(false) }
    var showSessionsSheet by rememberSaveable { mutableStateOf(false) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    val promptSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val activeSessionSummary = uiState.sessions.firstOrNull { it.sessionId == uiState.activeSessionId }
    val activeSessionSubtitle = when (activeSessionSummary?.promptCount ?: 0) {
        0 -> "Neu"
        1 -> "1 Anfrage"
        else -> "${activeSessionSummary?.promptCount ?: 0} Anfragen"
    }

    val showLocalFeedback: (String, ToastType) -> Unit = { message, type ->
        localFeedbackMessage = message
        localFeedbackType = type
    }
    val saveGeneratedImage: (ByteArray, String?) -> Unit = { imageBytes, mimeType ->
        saveAiImage(context, imageBytes, mimeType)
            .onSuccess {
                showLocalFeedback("Bild gespeichert.", ToastType.Success)
            }
            .onFailure {
                showLocalFeedback("Bild konnte nicht gespeichert werden.", ToastType.Error)
            }
    }
    val legacyImageWritePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        val pending = pendingImageSave
        pendingImageSave = null
        if (isGranted && pending != null) {
            saveGeneratedImage(pending.first, pending.second)
        } else {
            showLocalFeedback("Speicherzugriff wurde nicht freigegeben.", ToastType.Error)
        }
    }
    val requestGeneratedImageSave: (ByteArray, String?) -> Unit = { imageBytes, mimeType ->
        val needsLegacyWritePermission = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
            ) != PackageManager.PERMISSION_GRANTED

        if (needsLegacyWritePermission) {
            pendingImageSave = imageBytes to mimeType
            legacyImageWritePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            saveGeneratedImage(imageBytes, mimeType)
        }
    }

    val dismissKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }
    var hasAutoUpgradePrompted by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(conversationEndItemIndex, lastMessageRenderSignature, uiState.isAiEnabled) {
        if (uiState.isAiEnabled && uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(conversationEndItemIndex)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshAvailability()
    }

    LaunchedEffect(uiState.errorMessage) {
        val message = uiState.errorMessage
        if (!message.isNullOrBlank()) {
            if (
                message.contains("Abo", ignoreCase = true) ||
                message.contains("Membership", ignoreCase = true) ||
                message.contains("Pro oder Creator", ignoreCase = true)
            ) {
                membershipCoordinator.trackUpgradeAfterDeny("ai_chat")
                membershipCoordinator.openMembership(MembershipOpenReason.FeatureLocked, surface = "ai_chat")
            }
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

    LaunchedEffect(uiState.activeSessionId, uiState.activeSessionTitle) {
        renameDraft = uiState.activeSessionTitle
    }

    LaunchedEffect(uiState.usageSnapshot?.warningLevel) {
        if (!hasAutoUpgradePrompted && uiState.usageSnapshot?.warningLevel == "critical") {
            hasAutoUpgradePrompted = true
            membershipCoordinator.openMembership(MembershipOpenReason.CriticalUsage, surface = "ai_chat")
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
                            title = stringResource(R.string.ai_topbar_title),
                            subtitle = stringResource(R.string.ai_topbar_subtitle),
                        )
                    },
                    colors = skydownTopBarColors(),
                    scrollBehavior = scrollBehavior,
                )
            }
        } else {
            {}
        },
        floatingActionButton = {
            if (uiState.isAiEnabled) {
                AiPromptFab(
                    isWorking = uiState.botPhase.isBusy,
                    onOpen = { showPromptComposer = true },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("ai.screen.root")
                .skydownAtmosphereBackground(
                    primaryColor = MaterialTheme.colorScheme.primary,
                    secondaryColor = MaterialTheme.colorScheme.tertiary,
                    primaryAlpha = 0.016f,
                    secondaryAlpha = 0.012f,
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
                                bottom = innerPadding.calculateBottomPadding() + 92.dp,
                            ),
                        verticalArrangement = Arrangement.Top,
                    ) {
                        AiConversationSessionStrip(
                            title = uiState.activeSessionTitle,
                            subtitle = activeSessionSubtitle,
                            accent = MaterialTheme.colorScheme.primary,
                            enabled = !uiState.botPhase.isBusy,
                            onOpenSessions = { showSessionsSheet = true },
                            onCreateNewChat = viewModel::startNewConversation,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AiEmptyStateHeader(compactVisualDensity = compactLayout)
                        Spacer(modifier = Modifier.height(78.dp))
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .testTag("ai.message.list")
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
                            bottom = innerPadding.calculateBottomPadding() + 92.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
                    ) {
                        if (!uiState.isAiEnabled) {
                            item {
                                AiDisabledCard()
                            }
                        } else {
                            item {
                                AiConversationSessionStrip(
                                    title = uiState.activeSessionTitle,
                                    subtitle = activeSessionSubtitle,
                                    accent = MaterialTheme.colorScheme.primary,
                                    enabled = !uiState.botPhase.isBusy,
                                    onOpenSessions = { showSessionsSheet = true },
                                    onCreateNewChat = viewModel::startNewConversation,
                                )
                            }

                            item {
                                AiEmptyStateHeader(compactVisualDensity = true)
                            }

                            items(uiState.messages, key = { it.id }) { message ->
                                AiMessageBubble(
                                    message = message,
                                    compactLayout = compactLayout,
                                    onSaveImage = requestGeneratedImageSave,
                                    onFeedback = { messageText, type ->
                                        showLocalFeedback(messageText, type)
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

            if (showPromptComposer) {
                ModalBottomSheet(
                    onDismissRequest = { showPromptComposer = false },
                    sheetState = promptSheetState,
                ) {
                    AiPromptComposerSheet(
                        draft = uiState.draft,
                        composerMode = uiState.composerMode,
                        textMode = uiState.textMode,
                        selectedLevel = uiState.selectedLevel,
                        botPhase = uiState.botPhase,
                        onDraftChanged = viewModel::updateDraft,
                        onComposerModeChange = viewModel::updateComposerMode,
                        onTextModeChange = viewModel::updateTextMode,
                        onLevelChange = viewModel::updateLevel,
                        onSend = {
                            viewModel.sendDraft()
                            dismissKeyboard()
                            showPromptComposer = false
                        },
                        onReset = {
                            viewModel.startNewConversation()
                            dismissKeyboard()
                            showPromptComposer = false
                        },
                    )
                }
            }

            if (showSessionsSheet) {
                AiConversationSessionsSheet(
                    title = "AI Chats",
                    sessions = uiState.sessions,
                    activeSessionId = uiState.activeSessionId,
                    renameDraft = renameDraft,
                    accent = MaterialTheme.colorScheme.primary,
                    enabled = !uiState.botPhase.isBusy,
                    onRenameDraftChanged = { renameDraft = it },
                    onDismiss = { showSessionsSheet = false },
                    onSelectSession = { sessionId ->
                        viewModel.openConversation(sessionId)
                        showSessionsSheet = false
                    },
                    onCreateNewChat = {
                        viewModel.startNewConversation()
                        showSessionsSheet = false
                    },
                    onRenameActiveSession = {
                        viewModel.renameActiveConversation(renameDraft)
                        showSessionsSheet = false
                    },
                    onDeleteActiveSession = {
                        viewModel.deleteActiveConversation()
                        showSessionsSheet = false
                    },
                )
            }
        }

        if (membershipState.isOpen) {
            AiMembershipSheet(
                state = membershipState,
                onDismiss = { membershipCoordinator.closeMembership() },
                onToggleAnnual = { membershipCoordinator.setAnnualOption(it) },
                onUpgradePro = {
                    val activity = context as? android.app.Activity ?: return@AiMembershipSheet
                    membershipCoordinator.purchaseSelectedPlan(activity, "Pro") {
                        AppContainer.refreshCurrentUser()
                    }
                },
                onUpgradeCreator = {
                    val activity = context as? android.app.Activity ?: return@AiMembershipSheet
                    membershipCoordinator.purchaseSelectedPlan(activity, "Creator") {
                        AppContainer.refreshCurrentUser()
                    }
                },
                onRestore = {
                    membershipCoordinator.restore { AppContainer.refreshCurrentUser() }
                },
            )
        }
    }
}

private fun aiLevelSubtitleResId(level: AiExperienceLevel): Int = when (level) {
    AiExperienceLevel.Standard -> R.string.ai_level_standard_subtitle
    AiExperienceLevel.Advanced -> R.string.ai_level_advanced_subtitle
    AiExperienceLevel.Pro -> R.string.ai_level_pro_subtitle
}

@Composable
private fun AiEmptyStateHeader(
    compactVisualDensity: Boolean,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 6.dp else 8.dp),
    ) {
        Text(
            text = "Hey, ich bin SkyOS AI.",
            style = if (compactVisualDensity) {
                MaterialTheme.typography.headlineSmall
            } else {
                MaterialTheme.typography.headlineMedium
            },
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "Tippe auf +, waehle deine Optionen und starte dann den Prompt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun AiPromptFab(
    isWorking: Boolean,
    onOpen: () -> Unit,
) {
    FloatingActionButton(
        onClick = onOpen,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        if (isWorking) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        } else {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Prompt oeffnen",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AiPromptComposerSheet(
    draft: String,
    composerMode: AiComposerMode,
    textMode: AiTextMode,
    selectedLevel: AiExperienceLevel,
    botPhase: BotInteractionPhase,
    onDraftChanged: (String) -> Unit,
    onComposerModeChange: (AiComposerMode) -> Unit,
    onTextModeChange: (AiTextMode) -> Unit,
    onLevelChange: (AiExperienceLevel) -> Unit,
    onSend: () -> Unit,
    onReset: () -> Unit,
) {
    val composerAccent = if (composerMode == AiComposerMode.Visual) {
        MaterialTheme.colorScheme.tertiary
    } else {
        MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars).only(WindowInsetsSides.Bottom))
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Neue AI-Anfrage",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Erst Optionen waehlen, dann Prompt schreiben.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }

        botPhase.composerStatusLabel?.let { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        Text(
            text = "Optionen",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = composerAccent,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
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
        }

        if (composerMode == AiComposerMode.Text) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiTextMode.entries.forEach { mode ->
                    val isSelected = mode == textMode
                    BrandStatusChip(
                        text = mode.title,
                        accent = if (isSelected) composerAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        isActive = isSelected,
                        onClick = { onTextModeChange(mode) },
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AiExperienceLevel.entries.forEach { level ->
                val isSelected = level == selectedLevel
                BrandStatusChip(
                    text = level.title,
                    accent = if (isSelected) composerAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    isActive = isSelected,
                    onClick = {
                        if (!botPhase.isBusy) {
                            onLevelChange(level)
                        }
                    },
                )
            }
        }

        Text(
            text = stringResource(aiLevelSubtitleResId(selectedLevel)),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text = "Prompt",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = composerAccent,
        )

        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChanged,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    if (composerMode == AiComposerMode.Text) {
                        textMode.placeholder
                    } else {
                        "Zum Beispiel: Dunkles Cover-Art fuer einen neuen Release."
                    },
                )
            },
            minLines = 4,
            maxLines = 8,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { onSend() }),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = composerAccent.copy(alpha = 0.72f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f),
                focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                cursorColor = composerAccent,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onReset,
                enabled = !botPhase.isBusy,
            ) {
                Text("Neuer Chat")
            }
            BrandActionButton(
                text = if (composerMode == AiComposerMode.Text) "Senden" else "Rendern",
                onClick = onSend,
                accent = composerAccent,
                icon = Icons.AutoMirrored.Filled.Send,
                filled = true,
                compact = true,
                enabled = draft.isNotBlank() && !botPhase.isBusy,
                isLoading = botPhase.isBusy,
            )
        }
    }
}

@Composable
private fun AiRevenueUsageCard(
    usage: com.nash.skyos.data.AiUsageSnapshot?,
    planLabel: String,
    onOpenMembership: () -> Unit,
) {
    SkydownCard(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        modifier = Modifier.clickable(onClick = onOpenMembership),
    ) {
        Text(
            text = stringResource(R.string.ai_membership_title_short),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.tertiary,
        )
        Text(
            text = if (usage == null) "${stringResource(R.string.membership_current_plan)}: $planLabel · ${stringResource(R.string.ai_membership_tiers)}" else "${stringResource(R.string.membership_current_plan)}: $planLabel",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (usage == null) {
            Text(
                text = stringResource(R.string.ai_membership_caption),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 6.dp),
            )
            return@SkydownCard
        }

        val used = (usage.limitForKind - usage.remainingForKind).coerceAtLeast(0)
        val progress = if (usage.limitForKind > 0) {
            used.toFloat() / usage.limitForKind.toFloat()
        } else {
            0f
        }.coerceIn(0f, 1f)

        LinearUsageBar(
            progress = progress,
            isCritical = usage.warningLevel == "critical",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
        )
        Text(
            text = "${usage.remainingForKind}/${usage.limitForKind} ${stringResource(R.string.ai_available)}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            modifier = Modifier.padding(top = 4.dp),
        )
        if (usage.warningLevel != "ok") {
            Text(
                text = if (usage.warningLevel == "critical") {
                    stringResource(R.string.ai_warning_critical)
                } else {
                    stringResource(R.string.ai_warning_high)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (usage.userFacingReason.isNotBlank()) {
            Text(
                text = usage.userFacingReason,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (usage.lowerCostOption.isNotBlank()) {
            Text(
                text = usage.lowerCostOption,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (usage.retryAfterSeconds > 0) {
            Text(
                text = "${stringResource(R.string.ai_retry_in)} ${usage.retryAfterSeconds}s.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                    .padding(horizontal = 10.dp, vertical = 7.dp),
            )
        }
        if (usage.suggestedUpgrade.isNotBlank()) {
            Text(
                text = "${stringResource(R.string.ai_next_step)}: ${usage.suggestedUpgrade.uppercase()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (usage.resetHint.isNotBlank()) {
            Text(
                text = usage.resetHint,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun AiDecisionCard(
    decision: com.nash.skyos.data.AiBotDecision,
) {
    SkydownCard(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BrandStatusChip(text = "Route", accent = MaterialTheme.colorScheme.tertiary, isActive = true)
            BrandStatusChip(
                text = when (decision.state) {
                    "faq_answer" -> "FAQ"
                    "degraded" -> "Fallback"
                    "blocked" -> "Blockiert"
                    "retryable" -> "Erneut"
                    else -> "Live"
                },
                accent = MaterialTheme.colorScheme.primary,
                isActive = true,
            )
        }
        Text(
            text = decision.summary.ifBlank { "Antwortpfad dokumentiert." },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 8.dp),
        )
        if (decision.topic.isNotBlank()) {
            Text(
                text = "Thema: ${decision.topic}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (decision.fallbackActivated && decision.fallbackReason.isNotBlank()) {
            Text(
                text = "Fallback: ${decision.fallbackReason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (decision.responseLimited && decision.responseLimitReason.isNotBlank()) {
            Text(
                text = "Limit: ${decision.responseLimitReason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (decision.blocked && decision.blockReason.isNotBlank()) {
            Text(
                text = "Blockiert: ${decision.blockReason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (decision.retryable && decision.retryReason.isNotBlank()) {
            Text(
                text = "Erneut: ${decision.retryReason}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        decision.trace.take(3).forEach { traceLine ->
            Text(
                text = traceLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiMembershipSheet(
    state: AiMembershipUiState,
    onDismiss: () -> Unit,
    onToggleAnnual: (Boolean) -> Unit,
    onUpgradePro: () -> Unit,
    onUpgradeCreator: () -> Unit,
    onRestore: () -> Unit,
) {
    val proProduct = state.products.firstOrNull { it.planLabel == "Pro" }
    val creatorProduct = state.products.firstOrNull { it.planLabel == "Creator" }
    val proAvailable = proProduct?.let { it.monthly != null || it.yearly != null } == true
    val creatorAvailable = creatorProduct?.let { it.monthly != null || it.yearly != null } == true

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(R.string.ai_membership_sheet_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
            Text(stringResource(R.string.ai_membership_sheet_subtitle), style = MaterialTheme.typography.bodyMedium)
            Text(
                "Ausloeser: ${state.reason.name.lowercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
            Text(stringResource(R.string.ai_membership_free_detail), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.ai_membership_pro_detail), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.ai_membership_creator_detail), style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.membership_annual_option), style = MaterialTheme.typography.labelMedium)
                androidx.compose.material3.Switch(
                    checked = state.selectedAnnual,
                    onCheckedChange = onToggleAnnual,
                )
            }
            if (state.annualDiscountCopy.isNotBlank()) {
                Text(state.annualDiscountCopy, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            }
            BrandActionButton(
                text = stringResource(R.string.membership_pro_activate),
                onClick = onUpgradePro,
                accent = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isPurchasing,
                enabled = proAvailable && !state.isPurchasing,
            )
            BrandActionButton(
                text = stringResource(R.string.membership_creator_activate),
                onClick = onUpgradeCreator,
                accent = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth(),
                isLoading = state.isPurchasing,
                enabled = creatorAvailable && !state.isPurchasing,
            )
            OutlinedButton(onClick = onRestore, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.membership_restore))
            }
            androidx.compose.material3.OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.membership_decide_later))
            }
            if (!state.isLoading && state.products.isEmpty() && state.errorMessage.isBlank()) {
                Text(
                    text = "Play Billing ist fuer dieses Build noch nicht live konfiguriert.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (state.successMessage.isNotBlank()) {
                Text(state.successMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
            }
            if (state.errorMessage.isNotBlank()) {
                Text(state.errorMessage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun LinearUsageBar(
    progress: Float,
    isCritical: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .clip(RoundedCornerShape(999.dp))
                .background(if (isCritical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary),
        )
    }
}

@Composable
private fun AiCommandHeroCard(
    compactVisualDensity: Boolean,
) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (compactVisualDensity) 8.dp else 10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "SkyOS AI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = "Text. Visuals. Ideen. Ein klarer kreativer Flow.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BrandStatusChip(text = "Private", accent = MaterialTheme.colorScheme.primary, isActive = true)
            BrandStatusChip(text = "Visual", accent = MaterialTheme.colorScheme.tertiary, isActive = true)
            BrandStatusChip(text = "Memory", accent = MaterialTheme.colorScheme.secondary, isActive = true)
        }
    }
}

@Composable
private fun AiOverviewCard(
    isEnabled: Boolean,
) {
    SkydownCard(contentPadding = PaddingValues(14.dp)) {
        BrandSectionBanner(
            title = if (isEnabled) "AI live" else "AI pausiert",
            subtitle = "Text, Visuals und Stilentscheidungen laufen in einer privaten Session.",
            accent = MaterialTheme.colorScheme.primary,
            icon = Icons.Default.AutoAwesome,
            tag = if (isEnabled) "Private Session" else "Ruhend",
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
                value = if (isEnabled) "Aktiv" else "Pausiert",
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
                text = "Privat",
                accent = MaterialTheme.colorScheme.primary,
                isActive = isEnabled,
            )
            BrandStatusChip(
                text = "Visuals",
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
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = "SkyOS Bot pausiert",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.weight(1f))
            BrandStatusChip(
                text = "Ruhend",
                accent = MaterialTheme.colorScheme.primary,
                isActive = true,
            )
        }
        Text(
            text = "Rest der App bleibt nutzbar — der Bot folgt, sobald er wieder verfuegbar ist.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

@Composable
private fun QuickPromptCard(
    prompts: List<String>,
    onPromptSelected: (String) -> Unit,
    compactLayout: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandStatusChip(
                text = "Text",
                accent = MaterialTheme.colorScheme.primary,
                isActive = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Prompt Library",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Schnelle Starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
        LazyRow(
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
    Column(
        modifier = Modifier.testTag("ai.visual.prompt.card"),
        verticalArrangement = Arrangement.spacedBy(if (compactLayout) 8.dp else 10.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandStatusChip(
                text = "Visual",
                accent = MaterialTheme.colorScheme.tertiary,
                isActive = true,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Visual Deck",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Visual Starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(if (compactLayout) 6.dp else 8.dp),
            contentPadding = PaddingValues(end = 4.dp),
        ) {
            items(prompts, key = { it.label }) { prompt ->
                AiPromptActionCard(
                    eyebrow = "SHOT",
                    text = prompt.label,
                    detail = "Direkt starten",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiMessageBubble(
    message: AiMessage,
    compactLayout: Boolean,
    onSaveImage: (ByteArray, String?) -> Unit,
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
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        Color.Transparent
    }
    val bubbleBorderColor = if (isUser) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }
    val bubbleMaxWidth = when {
        message.imageBytes != null && compactLayout -> 372.dp
        message.imageBytes != null -> 456.dp
        compactLayout -> 360.dp
        else -> 400.dp
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = bubbleMaxWidth)
                .testTag("ai.message.bubble")
                .clip(bubbleShape)
                .background(bubbleColor)
                .border(
                    width = if (isUser) 1.dp else 0.dp,
                    color = bubbleBorderColor,
                    shape = bubbleShape,
                )
                .padding(
                    horizontal = if (compactLayout) 12.dp else 14.dp,
                    vertical = if (compactLayout) 10.dp else 12.dp,
                ),
        ) {
            Text(
                text = if (isUser) "Du" else "SkyOS AI",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
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
                        text = "SkyOS AI antwortet...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    )
                }
            } else {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )

                val generatedBitmap = remember(message.imageBytes) {
                    message.imageBytes?.let { bytes ->
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }

                if (generatedBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .height(220.dp)
                            .testTag("ai.message.visual")
                            .semantics(mergeDescendants = true) {
                                contentDescription = "Generiertes Visual"
                            }
                            .clip(RoundedCornerShape(SkydownUiTokens.buttonCornerRadius)),
                    ) {
                        Image(
                            bitmap = generatedBitmap.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                if (!isUser && !message.isStreaming) {
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        BrandStatusChip(
                            text = "Kopieren",
                            accent = MaterialTheme.colorScheme.primary,
                            onClick = {
                                copyAiText(context, "SkyOS Bot", message.text)
                                onFeedback("Antwort kopiert.", ToastType.Success)
                            },
                        )

                        BrandStatusChip(
                            text = "Teilen",
                            accent = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                shareAiText(context, "SkyOS Bot", message.text)
                                onFeedback("Share-Sheet geoeffnet.", ToastType.Info)
                            },
                        )

                        if (message.imageBytes != null) {
                            BrandStatusChip(
                                text = "Speichern",
                                accent = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.testTag("ai.message.save"),
                                onClick = {
                                    onSaveImage(message.imageBytes, message.imageMimeType)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}
