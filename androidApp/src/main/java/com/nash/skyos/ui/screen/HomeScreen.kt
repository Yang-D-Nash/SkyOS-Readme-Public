package com.nash.skyos.ui.screen

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import org.json.JSONArray
import org.json.JSONObject
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.nash.skyos.R
import com.nash.skyos.data.AppContainer
import com.nash.skyos.data.ExternalMediaProvider
import com.nash.skyos.data.callWithAppCheckRetry
import com.nash.skyos.data.mediaAttributionContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.nash.skyos.ui.component.skydownPanelSurface
import com.nash.skyos.ui.component.openTrackInSpotify
import coil3.compose.AsyncImage
import com.nash.skyos.ui.component.AppTopBarSessionActions
import com.nash.skyos.ui.component.BrandActionButton
import com.nash.skyos.ui.component.BrandArtwork
import com.nash.skyos.ui.component.BrandHeroCard
import com.nash.skyos.ui.component.BrandPreviewFrame
import com.nash.skyos.ui.component.BrandSectionBanner
import com.nash.skyos.ui.component.BrandPill
import com.nash.skyos.ui.component.BrandStatusChip
import com.nash.skyos.ui.component.ExternalVideoWebPlayer
import com.nash.skyos.ui.component.OriginalVideoViewerDialog
import com.nash.skyos.ui.component.SkydownHapticKind
import com.nash.skyos.ui.component.SkydownCard
import com.nash.skyos.ui.component.SkydownMotionTokens
import com.nash.skyos.ui.component.skydownExitTween
import com.nash.skyos.ui.component.skydownTween
import com.nash.skyos.ui.component.SkydownTopBarTitle
import com.nash.skyos.ui.component.SkydownUiTokens
import com.nash.skyos.ui.component.performSkydownHaptic
import com.nash.skyos.ui.component.rememberSkydownScreenSectionSpacing
import com.nash.skyos.ui.component.skydownPressable
import com.nash.skyos.ui.component.skydownContentPadding
import com.nash.skyos.ui.component.skydownAtmosphereBackground
import com.nash.skyos.ui.component.skydownTopBarColors
import com.nash.skyos.ui.model.FeaturedVideoHighlight
import com.nash.skyos.ui.model.HomeUiState
import com.nash.skyos.ui.theme.InstagramOrange
import com.nash.skyos.ui.theme.SpotifyGreen
import com.nash.skyos.ui.theme.skydownAccent
import com.nash.skyos.ui.theme.skydownAccentHighlight
import com.nash.skyos.ui.theme.skydownAccentMystic
import com.nash.skyos.ui.theme.skydownCardBackground
import com.nash.skyos.ui.theme.skydownIsDarkPalette
import com.nash.skyos.ui.theme.skydownSecondaryBackground
import com.nash.skyos.ui.theme.skydownSpotify
import com.nash.skyos.ui.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenCart: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onGuestSignIn: (() -> Unit)? = null,
    onOpenWorkflow: (() -> Unit)? = null,
    onOpenWorkflowWithPrompt: ((String) -> Unit)? = null,
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: HomeViewModel = viewModel(
        factory = remember(app) { HomeViewModel.provideFactory(app) },
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUser by AppContainer.currentUser.collectAsStateWithLifecycle()
    val homeProductivitySheetRequest by AppContainer.homeProductivitySheetRequest.collectAsStateWithLifecycle()
    val screenHeaderSettings by AppContainer.screenHeaderSettingsRepository.settings.collectAsStateWithLifecycle()
    var activeDestination by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedVideoHubId by rememberSaveable { mutableStateOf<String?>(null) }

    if (activeDestination == homeDestinationNicmaProducer) {
        NicmaProducerScreen(
            onBack = { activeDestination = null },
        )
        return
    }

    if (activeDestination == homeDestinationVideoHub) {
        VideoHubScreen(
            onBack = {
                activeDestination = null
                selectedVideoHubId = null
            },
            initialSelectedVideoId = selectedVideoHubId,
            autoplayInitialSelection = true,
        )
        return
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val mediaContext = remember(context) { context.mediaAttributionContext() }
    var inAppOriginalVideoUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var inAppOriginalVideoTitle by rememberSaveable { mutableStateOf("Original") }
    val audioPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = true
        }
    }
    val videoPlayer = remember(mediaContext) {
        ExoPlayer.Builder(mediaContext).build().apply {
            playWhenReady = false
        }
    }
    var currentAudioKey by rememberSaveable { mutableStateOf<String?>(null) }
    var currentVideoId by rememberSaveable { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(audioPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    audioPlayer.stop()
                    audioPlayer.clearMediaItems()
                    currentAudioKey = null
                }
            }
        }
        audioPlayer.addListener(listener)

        onDispose {
            audioPlayer.removeListener(listener)
            audioPlayer.release()
        }
    }

    DisposableEffect(videoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    videoPlayer.pause()
                    videoPlayer.seekTo(0)
                    currentVideoId = null
                }
            }
        }
        videoPlayer.addListener(listener)

        onDispose {
            videoPlayer.removeListener(listener)
            videoPlayer.release()
        }
    }

    LaunchedEffect(uiState.featuredVideo?.id, uiState.featuredVideo?.nativePlaybackUrl) {
        val featuredVideo = uiState.featuredVideo
        if (featuredVideo == null || featuredVideo.nativePlaybackUrl.isBlank()) {
            videoPlayer.stop()
            videoPlayer.clearMediaItems()
            currentVideoId = null
        } else {
            videoPlayer.setMediaItem(MediaItem.fromUri(featuredVideo.nativePlaybackUrl))
            videoPlayer.prepare()
            videoPlayer.pause()
            currentVideoId = null
        }
    }

    val activeSignalCount = homeTrackedSignalCount(uiState)
    val heroPriorityTarget = homeHeroPriorityTarget(
        hasTrackSignal = uiState.featuredTrack != null,
        hasVideoSignal = uiState.featuredVideo != null,
    )
    val homeGreetingTitle = stringResource(R.string.home_hero_title)
    val homeGreetingSubtitle = stringResource(R.string.home_hero_live_count, activeSignalCount, homeSignalTotal)
    val homeGreetingDetail = stringResource(R.string.home_hero_detail)
    val colorScheme = MaterialTheme.colorScheme
    val homeAccent = colorScheme.skydownAccent()
    val homeMysticAccent = colorScheme.skydownAccentMystic()
    val homeHighlightAccent = colorScheme.skydownAccentHighlight()
    val homeSpotifyAccent = colorScheme.skydownSpotify()
    val heroPillTint: (String) -> Color = { target ->
        val base = when (target) {
            "track" -> homeSpotifyAccent
            else -> homeHighlightAccent
        }
        if (heroPriorityTarget == target) base else base.copy(alpha = 0.33f)
    }
    val heroPillOrder: List<String> = when (heroPriorityTarget) {
        "track" -> listOf("track", "video")
        else -> listOf("video", "track")
    }
    val sectionSpacing = rememberSkydownScreenSectionSpacing()
    var isQuickActionCoolingDown by rememberSaveable { mutableStateOf(false) }
    var activeProductivitySheet by rememberSaveable { mutableStateOf<String?>(null) }
    var reminderTitleDraft by rememberSaveable { mutableStateOf("") }
    var taskTitleDraft by rememberSaveable { mutableStateOf("") }
    var taskDetailDraft by rememberSaveable { mutableStateOf("") }
    var taskUseDue by remember { mutableStateOf(false) }
    var taskDueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 2 * 3_600_000L) }
    var taskShowDatePicker by remember { mutableStateOf(false) }
    var taskShowTimePicker by remember { mutableStateOf(false) }
    var noteTitleDraft by rememberSaveable { mutableStateOf("") }
    var noteContentDraft by rememberSaveable { mutableStateOf("") }
    var founderBriefingSheet by remember { mutableStateOf<FounderBriefingSheetState?>(null) }
    var founderBriefingModeInFlight by remember { mutableStateOf<FounderBriefingMode?>(null) }
    var founderBriefingFeedbackMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var founderBriefingFeedbackIsError by rememberSaveable { mutableStateOf(false) }
    fun openHomeProductivityCapture(sheet: String) {
        if (isQuickActionCoolingDown) return
        isQuickActionCoolingDown = true
        // Productivity capture is for signed-in users (direct Firestore). Do not gate on AI / workflow
        // — onGuestSignIn is always non-null in SkydownApp, so the old logic sent everyone to login.
        when {
            currentUser == null && onGuestSignIn != null -> onGuestSignIn.invoke()
            else -> activeProductivitySheet = sheet
        }
        coroutineScope.launch {
            delay(700)
            isQuickActionCoolingDown = false
        }
    }
    LaunchedEffect(homeProductivitySheetRequest) {
        val request = homeProductivitySheetRequest ?: return@LaunchedEffect
        activeProductivitySheet = request
        AppContainer.clearHomeProductivitySheetRequest()
    }
    val topBarActionDescription = stringResource(
        if (onOpenWorkflow != null) {
            R.string.home_topbar_workflow_a11y
        } else {
            R.string.home_topbar_refresh_a11y
        },
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    SkydownTopBarTitle(
                        "SkyOS",
                        accent = homeAccent,
                    )
                },
                actions = {
                    AppTopBarSessionActions(
                        onOpenCart = onOpenCart,
                        onOpenProfile = onOpenProfile,
                        onOpenSettings = onOpenSettings,
                        onGuestSignIn = onGuestSignIn,
                    ) {
                        val interactionSource = remember { MutableInteractionSource() }
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f),
                            shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                            tonalElevation = 4.dp,
                            modifier = Modifier.skydownPressable(interactionSource),
                        ) {
                            IconButton(
                                onClick = onOpenWorkflow ?: viewModel::refresh,
                                interactionSource = interactionSource,
                            ) {
                                Icon(
                                    imageVector = if (onOpenWorkflow != null) {
                                        Icons.Default.AutoAwesome
                                    } else {
                                        Icons.Default.Refresh
                                    },
                                    contentDescription = topBarActionDescription,
                                )
                            }
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
                .skydownAtmosphereBackground(
                    primaryColor = homeAccent,
                    secondaryColor = homeSpotifyAccent,
                    primaryAlpha = 0.016f,
                    secondaryAlpha = 0.012f,
                ),
        ) {
            HomeMapBackdrop(
                modifier = Modifier.fillMaxSize(),
            )
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val contentMaxWidth = if (maxWidth > 1040.dp) 1040.dp else Dp.Unspecified
                val contentWidthFraction = if (contentMaxWidth != Dp.Unspecified && maxWidth > 0.dp) {
                    contentMaxWidth.value / maxWidth.value
                } else {
                    1f
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(contentWidthFraction)
                        .align(Alignment.TopCenter)
                        .testTag("home.root"),
                    state = listState,
                    contentPadding = skydownContentPadding(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                ) {
                item {
                    val homeAtmosphereBg = colorScheme.background
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .drawBehind {
                                val h = size.height
                                if (h <= 0f) return@drawBehind
                                val wash = Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0f to homeAccent.copy(alpha = 0.024f),
                                        0.45f to homeMysticAccent.copy(alpha = 0.014f),
                                        0.8f to homeSpotifyAccent.copy(alpha = 0.010f),
                                        1f to homeAtmosphereBg,
                                    ),
                                    startY = 0f,
                                    endY = h,
                                )
                                drawRect(wash)
                            },
                    ) {
                        Spacer(Modifier.height(6.dp))
                        HomeAnimatedItem(order = 0) {
                            Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                BrandHeroCard(
                                    eyebrow = screenHeaderSettings.homeEyebrow.ifBlank { "SKY OS" },
                                    title = screenHeaderSettings.homeTitle.ifBlank { homeGreetingTitle },
                                    subtitle = screenHeaderSettings.homeSubtitle.ifBlank { homeGreetingSubtitle },
                                    detail = screenHeaderSettings.homeDetail.ifBlank { homeGreetingDetail },
                                    backgroundImageUrl = screenHeaderSettings.homeImageUrl.ifBlank { null },
                                    accent = homeAccent,
                                    secondaryAccent = homeMysticAccent,
                                    marks = listOf(BrandArtwork.SkyOS, BrandArtwork.Skydown),
                                    immersive = true,
                                    edgeToEdge = true,
                                    onSurfaceClick = onOpenProfile,
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                                    ) {
                                        heroPillOrder.forEachIndexed { index, slot ->
                                            val weight = if (index == 0) 1.3f else 1f
                                            when (slot) {
                                                "track" -> Box(
                                                    modifier = Modifier
                                                        .weight(weight)
                                                        .then(
                                                            if (index == 0) Modifier.padding(end = 3.dp) else Modifier,
                                                        ),
                                                ) {
                                                    BrandPill(
                                                        text = stringResource(R.string.home_utility_music),
                                                        tint = heroPillTint("track"),
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                listState.animateScrollToItem(homeMediaClusterSectionIndex)
                                                            }
                                                        },
                                                    )
                                                }
                                                else -> Box(
                                                    modifier = Modifier
                                                        .weight(weight)
                                                        .then(
                                                            if (index == 0) Modifier.padding(end = 3.dp) else Modifier,
                                                        ),
                                                ) {
                                                    BrandPill(
                                                        text = stringResource(R.string.home_utility_videos),
                                                        tint = heroPillTint("video"),
                                                        onClick = {
                                                            coroutineScope.launch {
                                                                listState.animateScrollToItem(homeMediaClusterSectionIndex)
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
                        HomeAnimatedItem(order = 1) {
                            HomeUtilityRow(
                                onOpenMusic = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                                onOpenVideos = { coroutineScope.launch { listState.animateScrollToItem(homeMediaClusterSectionIndex) } },
                                onOpenMerch = onOpenCart,
                                onOpenSettings = onOpenSettings,
                            )
                        }
                        HomeAnimatedItem(order = 2) {
                            HomeProductivityOverviewCard(
                                remindersToday = uiState.dueTodayReminders,
                                remindersUpcoming = uiState.upcomingReminders,
                                openTasks = uiState.openTasks,
                                recentNotes = uiState.recentNotes,
                                onRequestFounderBriefing = if (onOpenWorkflowWithPrompt == null) null else { mode ->
                                    if (founderBriefingModeInFlight != null) return@HomeProductivityOverviewCard
                                    if (currentUser == null) {
                                        onGuestSignIn?.invoke()
                                        return@HomeProductivityOverviewCard
                                    }
                                    founderBriefingFeedbackMessage = null
                                    founderBriefingModeInFlight = mode
                                    coroutineScope.launch {
                                        val uid = FirebaseAuth.getInstance().currentUser?.uid
                                        if (uid.isNullOrBlank()) {
                                            founderBriefingModeInFlight = null
                                            founderBriefingFeedbackMessage = "Bitte zuerst anmelden."
                                            founderBriefingFeedbackIsError = true
                                            Toast.makeText(context, "Bitte zuerst anmelden.", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        val result = try {
                                            requestFounderBriefingFromCallable(context = context, uid = uid, mode = mode)
                                        } catch (error: Throwable) {
                                            founderBriefingModeInFlight = null
                                            val message = error.message?.trim().takeUnless { it.isNullOrBlank() }
                                                ?: "Briefing derzeit nicht verfuegbar."
                                            founderBriefingFeedbackMessage = message
                                            founderBriefingFeedbackIsError = true
                                            founderBriefingSheet = FounderBriefingSheetState(
                                                mode = mode,
                                                title = if (mode == FounderBriefingMode.Private) "Founder Analyse" else "Team Update",
                                                body = message,
                                            )
                                            return@launch
                                        }
                                        founderBriefingModeInFlight = null
                                        if (result == null) {
                                            founderBriefingFeedbackMessage = "Briefing hat keinen Inhalt geliefert."
                                            founderBriefingFeedbackIsError = true
                                            founderBriefingSheet = FounderBriefingSheetState(
                                                mode = mode,
                                                title = if (mode == FounderBriefingMode.Private) "Founder Analyse" else "Team Update",
                                                body = "ANDROID_PARSER_V6: Kein Text geliefert.",
                                            )
                                        } else {
                                            founderBriefingFeedbackMessage = "Briefing erfolgreich erstellt."
                                            founderBriefingFeedbackIsError = false
                                            founderBriefingSheet = result
                                        }
                                    }
                                },
                                founderBriefingModeInFlight = founderBriefingModeInFlight,
                                founderBriefingFeedbackMessage = founderBriefingFeedbackMessage,
                                founderBriefingFeedbackIsError = founderBriefingFeedbackIsError,
                                onOpenToday = {
                                    openHomeProductivityCapture("reminder_manage")
                                },
                                onOpenUpcoming = {
                                    openHomeProductivityCapture("reminder_manage")
                                },
                                onOpenTasks = {
                                    openHomeProductivityCapture("task_manage")
                                },
                                onOpenNotes = {
                                    openHomeProductivityCapture("note_manage")
                                },
                                onCreateReminder = {
                                    openHomeProductivityCapture("reminder")
                                },
                                onCreateTask = {
                                    openHomeProductivityCapture("task")
                                },
                                onCreateNote = {
                                    openHomeProductivityCapture("note")
                                },
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                item {
                    HomeAnimatedItem(order = 3) {
                        val homeMediaColorScheme = MaterialTheme.colorScheme
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                        ) {
                            Text(
                                text = stringResource(R.string.home_section_current),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = homeMediaColorScheme.onSurface.copy(alpha = 0.50f),
                            )
                        HomeMediaCluster(
                            uiState = uiState,
                            isTrackPlaying = currentAudioKey == uiState.featuredTrack?.let(::homeTrackAudioKey),
                            isVideoPlaying = currentVideoId == uiState.featuredVideo?.id,
                            player = videoPlayer,
                            onPlayTrackToggle = { track ->
                                val previewUrl = track.previewUrl
                                if (previewUrl.isNullOrBlank()) return@HomeMediaCluster
                                val audioKey = homeTrackAudioKey(track)
                                if (currentAudioKey == audioKey) {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                } else {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                    audioPlayer.setMediaItem(MediaItem.fromUri(previewUrl))
                                    audioPlayer.prepare()
                                    audioPlayer.play()
                                    currentAudioKey = audioKey
                                }
                            },
                            onPlayVideoToggle = { video ->
                                val playbackUrl = video.nativePlaybackUrl
                                if (playbackUrl.isBlank()) return@HomeMediaCluster
                                if (currentVideoId == video.id) {
                                    videoPlayer.pause()
                                    videoPlayer.seekTo(0)
                                    currentVideoId = null
                                } else {
                                    audioPlayer.stop()
                                    audioPlayer.clearMediaItems()
                                    currentAudioKey = null
                                    videoPlayer.setMediaItem(MediaItem.fromUri(playbackUrl))
                                    videoPlayer.prepare()
                                    videoPlayer.play()
                                    currentVideoId = video.id
                                }
                            },
                            onOpenSpotify = { track ->
                                openTrackInSpotify(
                                    context = context,
                                    spotifyArtistId = track.spotifyArtistId,
                                    spotifyTrackId = track.spotifyTrackId,
                                    externalUrl = track.externalUrl,
                                )
                            },
                            onOpenVideoHub = { video ->
                                audioPlayer.stop()
                                audioPlayer.clearMediaItems()
                                currentAudioKey = null
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                selectedVideoHubId = video.id
                                activeDestination = homeDestinationVideoHub
                            },
                            onOpenOriginal = { video ->
                                val originalUrl = video.openUrl.trim().ifBlank { video.inlineEmbedUrl.trim() }
                                if (originalUrl.isBlank()) return@HomeMediaCluster
                                videoPlayer.pause()
                                videoPlayer.seekTo(0)
                                currentVideoId = null
                                inAppOriginalVideoTitle = video.title.ifBlank { "Original" }
                                inAppOriginalVideoUrl = originalUrl
                            },
                        )
                        }
                    }
                }

                item {
                    HomeAnimatedItem(order = 4) {
                        Text(
                            text = stringResource(R.string.home_hero_tagline),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                        )
                    }
                }
                }
            }

            inAppOriginalVideoUrl?.let { url ->
                OriginalVideoViewerDialog(
                    url = url,
                    title = inAppOriginalVideoTitle,
                    onDismiss = { inAppOriginalVideoUrl = null },
                )
            }

            if (activeProductivitySheet != null) {
                val sheetType = activeProductivitySheet
                ModalBottomSheet(
                    onDismissRequest = { activeProductivitySheet = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SkydownUiTokens.cardPadding, vertical = SkydownUiTokens.stackSpacingCompact),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                    ) {
                        when (sheetType) {
                            "reminder" -> {
                                HomeReminderCaptureSheet(
                                    titleDraft = reminderTitleDraft,
                                    onTitleChange = { reminderTitleDraft = it },
                                    viewModel = viewModel,
                                    onDone = {
                                        reminderTitleDraft = ""
                                        activeProductivitySheet = null
                                    },
                                )
                            }
                            "reminder_manage" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                                ) {
                                    Text(
                                        text = stringResource(R.string.home_manager_reminders_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    HomeReminderManagerSheet(
                                        reminders = (uiState.dueTodayReminders + uiState.upcomingReminders)
                                            .distinctBy { it.id }
                                            .take(16),
                                        onUpdate = { id, title ->
                                            viewModel.updateReminderTitle(id, title)
                                        },
                                        onDelete = { id ->
                                            viewModel.deleteReminder(id)
                                        },
                                    )
                                }
                            }
                            "task" -> {
                                val taskWhenFormatter = remember {
                                    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                                }
                                Text(stringResource(R.string.home_quick_create_task), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.home_sheet_task_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                                OutlinedTextField(
                                    value = taskTitleDraft,
                                    onValueChange = { taskTitleDraft = it },
                                    label = { Text(stringResource(R.string.tasks_input_title_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        },
                                    ),
                                )
                                OutlinedTextField(
                                    value = taskDetailDraft,
                                    onValueChange = { taskDetailDraft = it },
                                    label = { Text(stringResource(R.string.tasks_input_details_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        },
                                    ),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        stringResource(R.string.home_task_due_sublabel),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    androidx.compose.material3.Switch(
                                        checked = taskUseDue,
                                        onCheckedChange = { taskUseDue = it },
                                    )
                                }
                                if (taskUseDue) {
                                    Text(
                                        text = taskWhenFormatter.format(java.util.Date(taskDueMillis)),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                                    ) {
                                        TextButton(
                                            onClick = { taskShowDatePicker = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.home_sheet_reminder_pick_date))
                                        }
                                        TextButton(
                                            onClick = { taskShowTimePicker = true },
                                            modifier = Modifier.weight(1f),
                                        ) {
                                            Text(stringResource(R.string.home_sheet_reminder_pick_time))
                                        }
                                    }
                                }
                                BrandActionButton(
                                    text = stringResource(R.string.tasks_input_add),
                                    onClick = {
                                        val title = taskTitleDraft.trim()
                                        if (title.isBlank()) return@BrandActionButton
                                        val due = if (taskUseDue) java.util.Date(taskDueMillis) else null
                                        viewModel.createTask(title, taskDetailDraft, due)
                                        taskTitleDraft = ""
                                        taskDetailDraft = ""
                                        taskUseDue = false
                                        activeProductivitySheet = null
                                    },
                                    accent = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (taskShowDatePicker) {
                                    val datePickerState = rememberDatePickerState(
                                        initialSelectedDateMillis = taskDueMillis,
                                    )
                                    DatePickerDialog(
                                        onDismissRequest = { taskShowDatePicker = false },
                                        confirmButton = {
                                            TextButton(
                                                onClick = {
                                                    datePickerState.selectedDateMillis?.let { selectedDay ->
                                                        val cur = java.util.Calendar.getInstance().apply { timeInMillis = taskDueMillis }
                                                        val pick = java.util.Calendar.getInstance().apply { timeInMillis = selectedDay }
                                                        cur.set(java.util.Calendar.YEAR, pick.get(java.util.Calendar.YEAR))
                                                        cur.set(java.util.Calendar.MONTH, pick.get(java.util.Calendar.MONTH))
                                                        cur.set(java.util.Calendar.DAY_OF_MONTH, pick.get(java.util.Calendar.DAY_OF_MONTH))
                                                        taskDueMillis = cur.timeInMillis
                                                    }
                                                    taskShowDatePicker = false
                                                },
                                            ) { Text(stringResource(android.R.string.ok)) }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { taskShowDatePicker = false }) {
                                                Text(stringResource(R.string.common_cancel))
                                            }
                                        },
                                    ) {
                                        DatePicker(state = datePickerState)
                                    }
                                }
                                if (taskShowTimePicker) {
                                    key(taskDueMillis) {
                                        val cal = java.util.Calendar.getInstance().apply { timeInMillis = taskDueMillis }
                                        val timeState = rememberTimePickerState(
                                            initialHour = cal.get(java.util.Calendar.HOUR_OF_DAY),
                                            initialMinute = cal.get(java.util.Calendar.MINUTE),
                                            is24Hour = true,
                                        )
                                        TimePickerDialog(
                                            onDismissRequest = { taskShowTimePicker = false },
                                            confirmButton = {
                                                TextButton(
                                                    onClick = {
                                                        val c = java.util.Calendar.getInstance().apply { timeInMillis = taskDueMillis }
                                                        c.set(java.util.Calendar.HOUR_OF_DAY, timeState.hour)
                                                        c.set(java.util.Calendar.MINUTE, timeState.minute)
                                                        c.set(java.util.Calendar.SECOND, 0)
                                                        c.set(java.util.Calendar.MILLISECOND, 0)
                                                        taskDueMillis = c.timeInMillis
                                                        taskShowTimePicker = false
                                                    },
                                                ) { Text(stringResource(android.R.string.ok)) }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { taskShowTimePicker = false }) {
                                                    Text(stringResource(R.string.common_cancel))
                                                }
                                            },
                                            title = {
                                                Text(stringResource(R.string.home_sheet_reminder_pick_time))
                                            },
                                        ) {
                                            TimePicker(state = timeState)
                                        }
                                    }
                                }
                            }
                            "task_manage" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                                ) {
                                    Text(
                                        text = stringResource(R.string.tasks_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    HomeTaskManagerSheet(
                                        tasks = uiState.openTasks.take(16),
                                        onUpdate = { id, title ->
                                            viewModel.updateTaskTitle(id, title)
                                        },
                                        onDelete = { id ->
                                            viewModel.deleteTask(id)
                                        },
                                    )
                                }
                            }
                            "note_manage" -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
                                ) {
                                    Text(
                                        text = stringResource(R.string.notes_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    HomeNoteManagerSheet(
                                        notes = uiState.recentNotes.take(16),
                                        onUpdate = { id, title ->
                                            viewModel.updateNoteTitle(id, title)
                                        },
                                        onDelete = { id ->
                                            viewModel.deleteNote(id)
                                        },
                                    )
                                }
                            }
                            else -> {
                                Text(stringResource(R.string.home_quick_create_note), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(stringResource(R.string.home_sheet_note_hint), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
                                OutlinedTextField(
                                    value = noteTitleDraft,
                                    onValueChange = { noteTitleDraft = it },
                                    label = { Text(stringResource(R.string.notes_input_title_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        },
                                    ),
                                )
                                OutlinedTextField(
                                    value = noteContentDraft,
                                    onValueChange = { noteContentDraft = it },
                                    label = { Text(stringResource(R.string.notes_input_content_hint)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            focusManager.clearFocus(force = true)
                                            keyboardController?.hide()
                                        },
                                    ),
                                )
                                BrandActionButton(
                                    text = stringResource(R.string.notes_input_add),
                                    onClick = {
                                        val title = noteTitleDraft.trim()
                                        val content = noteContentDraft.trim()
                                        if (title.isBlank() && content.isBlank()) return@BrandActionButton
                                        viewModel.createNote(title, content)
                                        noteTitleDraft = ""
                                        noteContentDraft = ""
                                        activeProductivitySheet = null
                                    },
                                    accent = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }

            if (founderBriefingSheet != null) {
                val sheet = founderBriefingSheet ?: return@Box
                ModalBottomSheet(
                    onDismissRequest = { founderBriefingSheet = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SkydownUiTokens.cardPadding, vertical = SkydownUiTokens.stackSpacingCompact),
                        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
                    ) {
                        Text(
                            text = sheet.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (sheet.mode == FounderBriefingMode.Private) "Private Founder Analyse" else "Team Update fuer WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                        )
                        if (!sheet.metaLine.isNullOrBlank()) {
                            Text(
                                text = sheet.metaLine,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f),
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
                        ) {
                            val scrollState = rememberScrollState()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 180.dp, max = 420.dp)
                                    .verticalScroll(scrollState)
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = sheet.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
                        ) {
                            BrandActionButton(
                                text = "WhatsApp",
                                onClick = { shareFounderBriefingToWhatsApp(context, sheet.body) },
                                accent = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f),
                            )
                            BrandActionButton(
                                text = "Kopieren",
                                onClick = {
                                    copyAiText(context, "Founder Briefing", sheet.body)
                                    Toast.makeText(context, "Kopiert", Toast.LENGTH_SHORT).show()
                                },
                                accent = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        TextButton(
                            onClick = { shareAiText(context, sheet.title, sheet.body) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Teilen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeManageableItemCard(
    title: String,
    subtitle: String?,
    isEditing: Boolean,
    draftTitle: String,
    onDraftTitleChange: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .skydownPanelSurface(
                cornerRadius = SkydownUiTokens.cardCornerRadius,
                shadowRadius = 8.dp,
                shadowYOffset = 4.dp,
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingNano),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                subtitle?.takeIf { it.isNotBlank() }?.let { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.common_edit),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.common_delete),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = isEditing,
            enter = fadeIn(skydownTween<Float>(SkydownMotionTokens.contentRevealEnterMillis)) + expandVertically(),
            exit = fadeOut(skydownExitTween<Float>(SkydownMotionTokens.contentRevealExitMillis)) + shrinkVertically(),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
                OutlinedTextField(
                    value = draftTitle,
                    onValueChange = onDraftTitleChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(stringResource(R.string.home_manager_rename_placeholder)) },
                )
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}

@Composable
private fun HomeReminderManagerSheet(
    reminders: List<com.nash.skyos.ui.model.ProductivityReminderItem>,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftTitle by rememberSaveable { mutableStateOf("") }
    val whenFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }

    if (reminders.isEmpty()) {
        Text(
            text = stringResource(R.string.home_manager_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.padding(vertical = 6.dp),
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
    ) {
        reminders.forEach { reminder ->
            HomeManageableItemCard(
                title = reminder.title,
                subtitle = reminder.dueAt?.let { whenFormatter.format(it) },
                isEditing = editingId == reminder.id,
                draftTitle = draftTitle,
                onDraftTitleChange = { draftTitle = it },
                onEdit = {
                    editingId = reminder.id
                    draftTitle = reminder.title
                },
                onDelete = { onDelete(reminder.id) },
                onSave = {
                    val normalized = draftTitle.trim()
                    if (normalized.isNotBlank()) {
                        onUpdate(reminder.id, normalized)
                        editingId = null
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeTaskManagerSheet(
    tasks: List<com.nash.skyos.ui.model.ProductivityTaskItem>,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftTitle by rememberSaveable { mutableStateOf("") }

    if (tasks.isEmpty()) {
        Text(
            text = stringResource(R.string.home_manager_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.padding(vertical = 6.dp),
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
    ) {
        tasks.forEach { task ->
            HomeManageableItemCard(
                title = task.title,
                subtitle = null,
                isEditing = editingId == task.id,
                draftTitle = draftTitle,
                onDraftTitleChange = { draftTitle = it },
                onEdit = {
                    editingId = task.id
                    draftTitle = task.title
                },
                onDelete = { onDelete(task.id) },
                onSave = {
                    val normalized = draftTitle.trim()
                    if (normalized.isNotBlank()) {
                        onUpdate(task.id, normalized)
                        editingId = null
                    }
                },
            )
        }
    }
}

@Composable
private fun HomeNoteManagerSheet(
    notes: List<com.nash.skyos.ui.model.ProductivityNoteItem>,
    onUpdate: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var draftTitle by rememberSaveable { mutableStateOf("") }
    val whenFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }

    if (notes.isEmpty()) {
        Text(
            text = stringResource(R.string.home_manager_empty),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.padding(vertical = 6.dp),
        )
        return
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
    ) {
        notes.forEach { note ->
            HomeManageableItemCard(
                title = note.title,
                subtitle = note.updatedAt?.let { whenFormatter.format(it) },
                isEditing = editingId == note.id,
                draftTitle = draftTitle,
                onDraftTitleChange = { draftTitle = it },
                onEdit = {
                    editingId = note.id
                    draftTitle = note.title
                },
                onDelete = { onDelete(note.id) },
                onSave = {
                    val normalized = draftTitle.trim()
                    if (normalized.isNotBlank()) {
                        onUpdate(note.id, normalized)
                        editingId = null
                    }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeReminderCaptureSheet(
    titleDraft: String,
    onTitleChange: (String) -> Unit,
    viewModel: HomeViewModel,
    onDone: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var dueMillis by remember { mutableLongStateOf(System.currentTimeMillis() + 3_600_000L) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val whenFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
    }

    Text(stringResource(R.string.home_quick_create_reminder), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
        stringResource(R.string.home_sheet_reminder_hint),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
    )
    OutlinedTextField(
        value = titleDraft,
        onValueChange = onTitleChange,
        label = { Text(stringResource(R.string.home_sheet_reminder_title_hint)) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                focusManager.clearFocus(force = true)
                keyboardController?.hide()
            },
        ),
    )
    Text(
        text = stringResource(R.string.home_sheet_reminder_when),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp),
    )
    Text(
        text = whenFormatter.format(Date(dueMillis)),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f),
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
    ) {
        TextButton(
            onClick = { showDatePicker = true },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.home_sheet_reminder_pick_date))
        }
        TextButton(
            onClick = { showTimePicker = true },
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.home_sheet_reminder_pick_time))
        }
    }
    BrandActionButton(
        text = stringResource(R.string.home_sheet_add),
        onClick = {
            val title = titleDraft.trim()
            if (title.isBlank()) return@BrandActionButton
            viewModel.createReminder(title, Date(dueMillis))
            onDone()
        },
        accent = MaterialTheme.colorScheme.tertiary,
        modifier = Modifier.fillMaxWidth(),
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dueMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDay ->
                            val cur = Calendar.getInstance().apply { timeInMillis = dueMillis }
                            val pick = Calendar.getInstance().apply { timeInMillis = selectedDay }
                            cur.set(Calendar.YEAR, pick.get(Calendar.YEAR))
                            cur.set(Calendar.MONTH, pick.get(Calendar.MONTH))
                            cur.set(Calendar.DAY_OF_MONTH, pick.get(Calendar.DAY_OF_MONTH))
                            dueMillis = cur.timeInMillis
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        key(dueMillis) {
            val cal = Calendar.getInstance().apply { timeInMillis = dueMillis }
            val timeState = rememberTimePickerState(
                initialHour = cal.get(Calendar.HOUR_OF_DAY),
                initialMinute = cal.get(Calendar.MINUTE),
                is24Hour = true,
            )
            TimePickerDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val c = Calendar.getInstance().apply { timeInMillis = dueMillis }
                            c.set(Calendar.HOUR_OF_DAY, timeState.hour)
                            c.set(Calendar.MINUTE, timeState.minute)
                            dueMillis = c.timeInMillis
                            showTimePicker = false
                        },
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text(stringResource(R.string.common_cancel))
                    }
                },
                title = {
                    Text(stringResource(R.string.home_sheet_reminder_pick_time))
                },
            ) {
                TimePicker(state = timeState)
            }
        }
    }
}

@Composable
private fun HomeUtilityRow(
    onOpenMusic: () -> Unit,
    onOpenVideos: () -> Unit,
    onOpenMerch: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    data class UtilityItem(
        val id: String,
        val label: String,
        val icon: ImageVector,
        val action: () -> Unit,
    )
    val items = listOf(
        UtilityItem("music", stringResource(R.string.home_utility_music), Icons.Default.MusicNote, onOpenMusic),
        UtilityItem("videos", stringResource(R.string.home_utility_videos), Icons.Default.Movie, onOpenVideos),
        UtilityItem("merch", stringResource(R.string.home_utility_merch), Icons.Default.ShoppingBag, onOpenMerch),
        UtilityItem("settings", stringResource(R.string.home_utility_settings), Icons.Default.Settings, onOpenSettings),
    )
    val utilityTint: (String) -> Color = { id ->
        when (id) {
            "music" -> colorScheme.skydownAccent().copy(alpha = 0.94f)
            "videos" -> colorScheme.skydownAccentHighlight().copy(alpha = 0.94f)
            "merch" -> colorScheme.skydownAccentMystic().copy(alpha = 0.94f)
            else -> colorScheme.onSurface.copy(alpha = 0.76f)
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
    ) {
        Text(
            text = stringResource(R.string.home_explore_title),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface.copy(alpha = 0.50f),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            items(items) { item ->
                val tint = utilityTint(item.id)
                Surface(
                    onClick = {
                        view.performSkydownHaptic(SkydownHapticKind.Selection)
                        item.action()
                    },
                    shape = RoundedCornerShape(SkydownUiTokens.tightRadius),
                    color = colorScheme.skydownSecondaryBackground().copy(alpha = 0.72f),
                    border = BorderStroke(1.dp, tint.copy(alpha = 0.22f)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tint.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = tint,
                            )
                        }
                        Text(
                            item.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = tint,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeProductivityOverviewCard(
    remindersToday: List<com.nash.skyos.ui.model.ProductivityReminderItem>,
    remindersUpcoming: List<com.nash.skyos.ui.model.ProductivityReminderItem>,
    openTasks: List<com.nash.skyos.ui.model.ProductivityTaskItem>,
    recentNotes: List<com.nash.skyos.ui.model.ProductivityNoteItem>,
    onRequestFounderBriefing: ((FounderBriefingMode) -> Unit)?,
    founderBriefingModeInFlight: FounderBriefingMode?,
    founderBriefingFeedbackMessage: String?,
    founderBriefingFeedbackIsError: Boolean,
    onOpenToday: () -> Unit,
    onOpenUpcoming: () -> Unit,
    onOpenTasks: () -> Unit,
    onOpenNotes: () -> Unit,
    onCreateReminder: () -> Unit,
    onCreateTask: () -> Unit,
    onCreateNote: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    var showsExtendedSignals by rememberSaveable { mutableStateOf(false) }
    SkydownCard(contentPadding = PaddingValues(12.dp)) {
        Text(
            text = stringResource(R.string.home_productivity_ask_anything),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.56f),
        )
        Spacer(modifier = Modifier.height(8.dp))
        HomeProductivityListRow(
            title = stringResource(R.string.home_productivity_today),
            emptyText = stringResource(R.string.home_productivity_empty_today),
            onOpen = onOpenToday,
            count = remindersToday.size,
            items = remindersToday.map { item ->
                item.dueAt?.let { "${item.title} • ${java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(it)}" }
                    ?: item.title
            },
        )
        if (showsExtendedSignals) {
            HomeProductivityListRow(
                title = stringResource(R.string.home_productivity_upcoming),
                emptyText = stringResource(R.string.home_productivity_empty_upcoming),
                onOpen = onOpenUpcoming,
                count = remindersUpcoming.size,
                items = remindersUpcoming.map { it.title },
            )
            HomeProductivityTaskListRow(
                title = stringResource(R.string.home_productivity_open_tasks),
                emptyText = stringResource(R.string.home_productivity_empty_tasks),
                onOpen = onOpenTasks,
                count = openTasks.size,
                tasks = openTasks,
            )
            HomeProductivityListRow(
                title = stringResource(R.string.home_productivity_recent_notes),
                emptyText = stringResource(R.string.home_productivity_empty_notes),
                onOpen = onOpenNotes,
                count = recentNotes.size,
                items = recentNotes.map { it.title },
            )
        } else {
            val rCount = remindersToday.size + remindersUpcoming.size
            TextButton(onClick = { showsExtendedSignals = true }, contentPadding = PaddingValues(0.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.home_productivity_collapse_summary, 3, rCount, openTasks.size, recentNotes.size),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    )
                }
            }
        }
        if (showsExtendedSignals) {
            TextButton(onClick = { showsExtendedSignals = false }, contentPadding = PaddingValues(0.dp)) {
                Text(
                    text = stringResource(R.string.home_productivity_show_less_sections),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.home_productivity_quick_hint),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSurface.copy(alpha = 0.52f),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            HomeQuickActionChip(
                text = stringResource(R.string.home_quick_create_reminder),
                onClick = onCreateReminder,
                modifier = Modifier.weight(1f),
            )
            HomeQuickActionChip(
                text = stringResource(R.string.home_quick_create_task),
                onClick = onCreateTask,
                modifier = Modifier.weight(1f),
            )
            HomeQuickActionChip(
                text = stringResource(R.string.home_quick_create_note),
                onClick = onCreateNote,
                modifier = Modifier.weight(1f),
            )
        }
        if (onRequestFounderBriefing != null) {
            Spacer(modifier = Modifier.height(8.dp))
            HomeOwnerWorkflowRow(
                onRequestFounderBriefing = onRequestFounderBriefing,
                founderBriefingModeInFlight = founderBriefingModeInFlight,
                founderBriefingFeedbackMessage = founderBriefingFeedbackMessage,
                founderBriefingFeedbackIsError = founderBriefingFeedbackIsError,
            )
        }
    }
}

@Composable
private fun HomeOwnerWorkflowRow(
    onRequestFounderBriefing: (FounderBriefingMode) -> Unit,
    founderBriefingModeInFlight: FounderBriefingMode?,
    founderBriefingFeedbackMessage: String?,
    founderBriefingFeedbackIsError: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
        Text(
            text = stringResource(R.string.home_owner_workflows_title),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
        )
        Text(
            text = stringResource(R.string.home_owner_workflows_subtitle),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        if (founderBriefingModeInFlight != null) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Founder Briefing wird erstellt...",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        } else if (!founderBriefingFeedbackMessage.isNullOrBlank()) {
            Text(
                text = founderBriefingFeedbackMessage,
                style = MaterialTheme.typography.labelSmall,
                color = if (founderBriefingFeedbackIsError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro)) {
            HomeQuickActionChip(
                text = if (founderBriefingModeInFlight == FounderBriefingMode.Private) {
                    "Wird erstellt..."
                } else {
                    stringResource(R.string.home_owner_workflows_private_analysis)
                },
                icon = Icons.Default.AttachMoney,
                onClick = { onRequestFounderBriefing(FounderBriefingMode.Private) },
                enabled = founderBriefingModeInFlight == null,
                isLoading = founderBriefingModeInFlight == FounderBriefingMode.Private,
                modifier = Modifier.weight(1f),
            )
            HomeQuickActionChip(
                text = if (founderBriefingModeInFlight == FounderBriefingMode.Group) {
                    "Wird erstellt..."
                } else {
                    stringResource(R.string.home_owner_workflows_group_update)
                },
                icon = Icons.Default.Groups,
                onClick = { onRequestFounderBriefing(FounderBriefingMode.Group) },
                enabled = founderBriefingModeInFlight == null,
                isLoading = founderBriefingModeInFlight == FounderBriefingMode.Group,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun HomeProductivityTaskListRow(
    title: String,
    emptyText: String,
    onOpen: () -> Unit,
    count: Int,
    tasks: List<com.nash.skyos.ui.model.ProductivityTaskItem>,
) {
    val now = remember { Date() }
    val dayFormatter = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault()) }
    val maxVisibleItems = 2
    var isExpanded by rememberSaveable(tasks.size, title) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingTick)) {
        TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                HomeCountBadge(count = count)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (tasks.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f))
        } else {
            val visible = if (isExpanded) tasks else tasks.take(maxVisibleItems)
            visible.forEach { task ->
                val showDueBanner = task.dueAt?.after(now) == true
                val line = buildString {
                    append(task.title)
                    task.dueAt?.let { append(" • ").append(dayFormatter.format(it)) }
                }
                val bg = if (showDueBanner) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                } else {
                    Color.Transparent
                }
                Surface(
                    color = bg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "• $line",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    )
                }
            }
            if (tasks.size > maxVisibleItems) {
                TextButton(onClick = { isExpanded = !isExpanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.home_productivity_show_less)
                        } else {
                            stringResource(R.string.home_productivity_more_count, tasks.size - maxVisibleItems)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeProductivityListRow(
    title: String,
    emptyText: String,
    onOpen: () -> Unit,
    count: Int,
    items: List<String>,
) {
    val maxVisibleItems = 2
    var isExpanded by rememberSaveable(title, items.size) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingTick)) {
        TextButton(onClick = onOpen, contentPadding = PaddingValues(0.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense), verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                HomeCountBadge(count = count)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.size(14.dp),
                )
            }
        }
        if (items.isEmpty()) {
            Text(emptyText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f))
        } else {
            val visibleItems = if (isExpanded) items else items.take(maxVisibleItems)
            visibleItems.forEach { item ->
                Text(
                    text = "• $item",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (items.size > maxVisibleItems) {
                TextButton(onClick = { isExpanded = !isExpanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = if (isExpanded) {
                            stringResource(R.string.home_productivity_show_less)
                        } else {
                            stringResource(R.string.home_productivity_more_count, items.size - maxVisibleItems)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeCountBadge(count: Int) {
    Surface(
        shape = RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
        )
    }
}

@Composable
private fun HomeQuickActionChip(
    text: String,
    icon: ImageVector? = null,
    countBadge: Int? = null,
    onClick: () -> Unit,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(SkydownUiTokens.pillSoftRadius),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        onClick = onClick,
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = SkydownUiTokens.stackSpacingMicro, vertical = SkydownUiTokens.stackSpacingMicro),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(12.dp),
                )
            }
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.size(14.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        // When countBadge is supplied (owner workflow chips), show it even at 0 — same signal as productivity rows.
        countBadge?.let { count ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, end = 2.dp),
                contentAlignment = Alignment.TopEnd,
            ) {
                val muted = count == 0
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(SkydownUiTokens.fullCapsuleRadius))
                        .background(
                            if (muted) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            },
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (muted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.onPrimary
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeLiveSignalSurface(
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
    trackName: String?,
    videoName: String?,
    aiUsageWarning: String? = null,
    creatorLimitZone: Boolean? = null,
    agentRunning: Boolean? = null,
    workflowWaiting: Boolean? = null,
    newOrderHint: String? = null,
    syncPaused: Boolean? = null,
    recoverableError: String? = null,
    contentSignal: String? = null,
) {
    val missingCount = listOf(hasTrackSignal, hasVideoSignal).count { !it }
    val nowText = when {
        hasTrackSignal && !trackName.isNullOrBlank() -> stringResource(R.string.home_live_now_music, trackName)
        hasVideoSignal && !videoName.isNullOrBlank() -> stringResource(R.string.home_live_now_visual, videoName)
        else -> stringResource(R.string.home_live_now_empty)
    }
    val nextText = when {
        !hasTrackSignal -> stringResource(R.string.home_live_next_track)
        !hasVideoSignal -> stringResource(R.string.home_live_next_video)
        else -> stringResource(R.string.home_live_next_focus)
    }
    val riskText = if (missingCount > 0) {
        stringResource(R.string.home_live_risk, missingCount)
    } else {
        null
    }
    val contentFederatedLine = contentSignal
        ?.takeIf { it.isNotBlank() }
        ?.let { stringResource(R.string.home_federated_content, it) }
    val federatedSignals = buildList {
        aiUsageWarning?.takeIf { it.isNotBlank() }?.let { add("AI: $it") }
        if (creatorLimitZone == true) add("AI: Creator limit zone reached.")
        if (agentRunning == true) add("AI: Agent currently running.")
        if (workflowWaiting == true) add("AI: Workflow waiting for next step.")
        newOrderHint?.takeIf { it.isNotBlank() }?.let { add("Commerce: $it") }
        if (syncPaused == true) add("System: Sync currently paused.")
        recoverableError?.takeIf { it.isNotBlank() }?.let { add("System: $it") }
        contentFederatedLine?.let { add(it) }
    }
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
    ) {
        Text(
            text = stringResource(R.string.home_status_signals),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface.copy(alpha = 0.50f),
        )
        Text(nowText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.66f))
        Text(nextText, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.58f))
        riskText?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.55f))
        }
        federatedSignals.takeIf { it.isNotEmpty() }?.forEach { signal ->
            Text(
                text = signal,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface.copy(alpha = 0.52f),
            )
        }
    }
}

@Composable
private fun HomeMediaCluster(
    uiState: HomeUiState,
    isTrackPlaying: Boolean,
    isVideoPlaying: Boolean,
    player: ExoPlayer,
    onPlayTrackToggle: (com.skydown.shared.model.Track) -> Unit,
    onPlayVideoToggle: (FeaturedVideoHighlight) -> Unit,
    onOpenSpotify: (com.skydown.shared.model.Track) -> Unit,
    onOpenVideoHub: (FeaturedVideoHighlight) -> Unit,
    onOpenOriginal: (FeaturedVideoHighlight) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingCompact),
    ) {
        HomeLatestReleaseCard(
            uiState = uiState,
            isPlaying = isTrackPlaying,
            onPlayToggle = onPlayTrackToggle,
            onOpenSpotify = onOpenSpotify,
        )
        HomeLatestVideoCard(
            uiState = uiState,
            isPlaying = isVideoPlaying,
            player = player,
            onPlayToggle = onPlayVideoToggle,
            onOpenVideoHub = onOpenVideoHub,
            onOpenOriginal = onOpenOriginal,
        )
    }
}

@Composable
private fun HomeDailyOpsStrip(
    modifier: Modifier = Modifier,
    activeSignalCount: Int,
    totalSignalCount: Int,
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
    onRefresh: () -> Unit,
    onOpenRelease: () -> Unit,
    onOpenVideo: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val currentHour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val priorityTarget = when {
        !hasTrackSignal -> "music"
        !hasVideoSignal -> "visuals"
        currentHour in 5..11 -> "music"
        else -> "visuals"
    }
    val priorityAccent = when (priorityTarget) {
        "music" -> colorScheme.skydownSpotify()
        else -> colorScheme.skydownAccentHighlight()
    }
    val priorityTitle = when (priorityTarget) {
        "music" -> if (hasTrackSignal) {
            stringResource(R.string.home_dailyops_priority_music_ready)
        } else {
            stringResource(R.string.home_dailyops_priority_music_build)
        }
        else -> if (hasVideoSignal) {
            stringResource(R.string.home_dailyops_priority_visuals_ready)
        } else {
            stringResource(R.string.home_dailyops_priority_visuals_build)
        }
    }
    val priorityHint = when (priorityTarget) {
        "music" -> if (hasTrackSignal) {
            stringResource(R.string.home_dailyops_hint_music_morning)
        } else {
            stringResource(R.string.home_dailyops_hint_music_missing)
        }
        else -> if (hasVideoSignal) {
            stringResource(R.string.home_dailyops_hint_visuals_evening)
        } else {
            stringResource(R.string.home_dailyops_hint_visuals_missing)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = priorityAccent.copy(alpha = 0.45f),
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.home_dailyops_current_focus),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Normal,
                color = colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Success)
                    onRefresh()
                },
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = stringResource(R.string.home_dailyops_refresh_a11y),
                    tint = priorityAccent.copy(alpha = 0.5f),
                )
            }
            Text(
                text = stringResource(R.string.home_dailyops_live_count, activeSignalCount, totalSignalCount),
                style = MaterialTheme.typography.labelSmall,
                color = priorityAccent.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium,
            )
        }

        Text(
            text = stringResource(R.string.home_dailyops_hint),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.55f),
        )

        BrandActionButton(
            text = priorityTitle,
            onClick = {
                view.performSkydownHaptic(SkydownHapticKind.Selection)
                when (priorityTarget) {
                    "music" -> onOpenRelease()
                    else -> onOpenVideo()
                }
            },
            accent = priorityAccent,
            compact = true,
            filled = true,
            modifier = Modifier.fillMaxWidth(),
            icon = Icons.Default.AutoAwesome,
        )

        Text(
            text = priorityHint,
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.52f),
        )
    }
}

@Composable
private fun HomeCommandDockStrip(
    priorityTarget: String,
    onOpenWorkflow: (() -> Unit)?,
    onOpenCart: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val view = LocalView.current
    val priorityAccent = when (priorityTarget) {
        "track" -> colorScheme.skydownSpotify()
        else -> colorScheme.skydownAccentHighlight()
    }
    val actionAccent: (String) -> Color = { target ->
        when (target) {
            "agent" -> colorScheme.skydownAccentMystic().copy(alpha = if (onOpenWorkflow != null) 1f else 0.4f)
            "track" -> {
                val base = colorScheme.skydownSpotify()
                if (priorityTarget == "track") base else base.copy(alpha = 0.56f)
            }
            else -> {
                val base = colorScheme.skydownAccentHighlight()
                if (priorityTarget == "video") base else base.copy(alpha = 0.56f)
            }
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingSubtle),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = priorityAccent.copy(alpha = 0.5f),
                modifier = Modifier.size(11.dp),
            )
            Text(
                text = stringResource(R.string.home_shortcuts_title),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.weight(1f))
        }
        Text(
            text = stringResource(R.string.home_shortcuts_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurface.copy(alpha = 0.52f),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingMicro),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandActionButton(
                text = stringResource(R.string.home_command_ai_agent),
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenWorkflow?.invoke()
                },
                accent = actionAccent("agent"),
                compact = true,
                filled = onOpenWorkflow != null,
                enabled = onOpenWorkflow != null,
            )
            BrandActionButton(
                text = stringResource(R.string.home_command_cart),
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenCart()
                },
                accent = actionAccent("track"),
                compact = true,
                filled = false,
            )
            BrandActionButton(
                text = stringResource(R.string.home_command_settings),
                onClick = {
                    view.performSkydownHaptic(SkydownHapticKind.Selection)
                    onOpenSettings()
                },
                accent = actionAccent("video"),
                compact = true,
                filled = false,
            )
        }
    }
}

@Composable
private fun HomeAnimatedItem(
    order: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember(order) { mutableStateOf(false) }

    LaunchedEffect(order) {
        delay(order.coerceAtMost(4) * SkydownMotionTokens.staggerStepMillis.toLong())
        visible = true
    }

    val fadeSpec = skydownTween<Float>(SkydownMotionTokens.contentRevealEnterMillis)
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = fadeSpec),
    ) {
        content()
    }
}

@Composable
private fun HomeMapBackdrop(
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDarkPalette = colorScheme.skydownIsDarkPalette()
    Box(modifier = modifier) {
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 168.dp, y = (-138).dp),
            haloSize = 280.dp,
            tint = colorScheme.skydownSpotify(),
            opacity = if (isDarkPalette) 0.055f else 0.045f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-172).dp, y = 210.dp),
            haloSize = 240.dp,
            tint = colorScheme.skydownAccentMystic(),
            opacity = if (isDarkPalette) 0.06f else 0.05f,
        )
        HomeBackdropHalo(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 154.dp, y = 498.dp),
            haloSize = 320.dp,
            tint = colorScheme.skydownAccentHighlight(),
            opacity = if (isDarkPalette) 0.05f else 0.04f,
        )
    }
}

@Composable
private fun HomeBackdropHalo(
    modifier: Modifier = Modifier,
    haloSize: androidx.compose.ui.unit.Dp,
    tint: Color,
    opacity: Float,
) {
    Box(
        modifier = modifier.size(haloSize),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(haloSize)
                .clip(CircleShape)
                .drawWithCache {
                    val haloBrush = Brush.radialGradient(
                        colors = listOf(
                            tint.copy(alpha = opacity),
                            tint.copy(alpha = opacity * 0.38f),
                            Color.Transparent,
                        ),
                        radius = minOf(size.width, size.height) * 0.5f,
                    )
                    onDrawBehind {
                        drawCircle(brush = haloBrush)
                    }
                },
        )
        listOf(1f, 0.78f, 0.56f).forEachIndexed { index, scale ->
            Box(
                modifier = Modifier
                    .size(haloSize * scale)
                    .border(
                        width = 1.dp,
                        color = tint.copy(alpha = opacity - (index * 0.02f)),
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun HomeSectionBanner(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    tag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SkydownUiTokens.compactRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.16f),
                shape = RoundedCornerShape(SkydownUiTokens.compactRadius),
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(14.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingHairline),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        tag?.takeIf { it.isNotBlank() }?.let { label ->
            BrandStatusChip(
                text = label,
                accent = accent,
                isActive = true,
            )
        }
    }
}

@Composable
private fun HomeLatestReleaseCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    onPlayToggle: (com.skydown.shared.model.Track) -> Unit,
    onOpenSpotify: (com.skydown.shared.model.Track) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = stringResource(R.string.home_media_music_title),
            subtitle = stringResource(R.string.home_media_music_subtitle),
            icon = Icons.Default.MusicNote,
            accent = SpotifyGreen,
        )
        val track = uiState.featuredTrack

        if (track == null) {
            Text(
                text = uiState.homeTrackMessage ?: stringResource(R.string.home_track_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandPreviewFrame(
                accent = SpotifyGreen,
                modifier = Modifier
                    .size(86.dp),
            ) {
                AsyncImage(
                    model = track.artworkUrl100,
                    contentDescription = track.trackName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                Text(
                    text = track.trackName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = track.artistName ?: "22",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                Text(
                    text = buildString {
                        append(track.collectionName ?: "Spotify Release")
                        track.releaseDate?.takeIf { it.isNotBlank() }?.let {
                            append(" • ")
                            append(it)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                )
            }
        }

        val hasPreview = !track.previewUrl.isNullOrBlank()
        val hasSpotifyTarget = homeHasSpotifyTarget(track)

        Text(
            text = when {
                hasPreview -> stringResource(R.string.home_track_preview_here)
                hasSpotifyTarget -> stringResource(R.string.home_track_open_spotify)
                else -> stringResource(R.string.home_track_ready)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        Column(
            modifier = Modifier.padding(top = 14.dp),
            verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingPill),
        ) {
            if (hasPreview) {
                HomeMediaActionButton(
                    label = if (isPlaying) {
                        stringResource(R.string.home_media_stop)
                    } else {
                        stringResource(R.string.home_media_play)
                    },
                    isActive = isPlaying,
                    accent = SpotifyGreen,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onPlayToggle(track) },
                )
            }

            if (hasSpotifyTarget) {
                BrandActionButton(
                    text = homeSpotifyActionLabel(
                        track = track,
                        songLabel = stringResource(R.string.home_spotify_song),
                        artistLabel = stringResource(R.string.home_spotify_artist),
                        fallbackLabel = stringResource(R.string.home_spotify_open),
                    ),
                    onClick = { onOpenSpotify(track) },
                    accent = SpotifyGreen,
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Default.MusicNote,
                    filled = false,
                )
            }
        }
    }
}

@Composable
private fun HomeLatestVideoCard(
    uiState: HomeUiState,
    isPlaying: Boolean,
    player: ExoPlayer,
    onPlayToggle: (FeaturedVideoHighlight) -> Unit,
    onOpenVideoHub: (FeaturedVideoHighlight) -> Unit,
    onOpenOriginal: (FeaturedVideoHighlight) -> Unit,
) {
    SkydownCard(contentPadding = PaddingValues(SkydownUiTokens.cardPadding)) {
        HomeSectionBanner(
            title = stringResource(R.string.home_media_video_title),
            subtitle = stringResource(R.string.home_media_video_subtitle),
            icon = Icons.Default.Movie,
            accent = InstagramOrange,
        )
        val video = uiState.featuredVideo

        if (video == null) {
            Text(
                text = uiState.homeVideoMessage ?: stringResource(R.string.home_video_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 8.dp),
            )
            return@SkydownCard
        }

        Row(
            modifier = Modifier.padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingRelaxed),
            verticalAlignment = Alignment.Top,
        ) {
            BrandPreviewFrame(
                accent = InstagramOrange,
                modifier = Modifier
                    .size(86.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(34.dp),
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(SkydownUiTokens.stackSpacingDense),
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = video.projectName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
                if (video.notes.isNotBlank()) {
                    Text(
                        text = video.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                    )
                }
            }
        }

        Text(
            text = when {
                video.usesEmbeddedPreview -> stringResource(R.string.home_video_preview_here)
                video.supportsInlinePlayback -> stringResource(R.string.home_video_in_app)
                else -> stringResource(R.string.home_video_open_original)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
            modifier = Modifier.padding(top = 14.dp),
        )

        if (video.usesEmbeddedPreview) {
            ExternalVideoWebPlayer(
                url = video.inlineEmbedUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
            )
        } else if (video.nativePlaybackUrl.isNotBlank()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge),
                factory = { playerContext ->
                    PlayerView(playerContext).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { view ->
                    view.player = player
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 14.dp)
                    .clip(MaterialTheme.shapes.extraLarge)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (video.openUrl.isBlank()) {
                        stringResource(R.string.home_video_no_link)
                    } else {
                        stringResource(R.string.home_video_open_original)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                )
            }
        }

        if (video.nativePlaybackUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = if (isPlaying) {
                    stringResource(R.string.home_media_stop)
                } else {
                    stringResource(R.string.home_media_play)
                },
                isActive = isPlaying,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onPlayToggle(video) },
            )
        } else if (video.openUrl.isNotBlank() || video.inlineEmbedUrl.isNotBlank()) {
            HomeMediaActionButton(
                label = stringResource(R.string.home_video_open),
                isActive = false,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onOpenOriginal(video) },
            )
        } else if (video.supportsInlinePlayback) {
            HomeMediaActionButton(
                label = stringResource(R.string.home_video_open_hub),
                isActive = false,
                accent = InstagramOrange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                onClick = { onOpenVideoHub(video) },
            )
        }
    }
}

@Composable
private fun HomeMediaActionButton(
    label: String,
    isActive: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BrandActionButton(
        text = label,
        onClick = onClick,
        accent = accent,
        modifier = modifier,
        icon = if (isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
        filled = isActive,
    )
}

private fun homeTrackAudioKey(track: com.skydown.shared.model.Track): String = "track:${track.trackId}"

private fun homeTrackedSignalCount(uiState: HomeUiState): Int = listOf(
    uiState.featuredTrack,
    uiState.featuredVideo,
).count { it != null }

private fun homeHeroPriorityTarget(
    hasTrackSignal: Boolean,
    hasVideoSignal: Boolean,
): String {
    if (!hasTrackSignal) return "track"
    if (!hasVideoSignal) return "video"
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return if (hour < 12) "track" else "video"
}

private fun homeHasSpotifyTarget(track: com.skydown.shared.model.Track): Boolean {
    return homeResolvedSpotifyTrackId(track) != null ||
        homeResolvedSpotifyArtistId(track) != null ||
        !track.externalUrl.isNullOrBlank()
}

private fun homeSpotifyActionLabel(
    track: com.skydown.shared.model.Track,
    songLabel: String,
    artistLabel: String,
    fallbackLabel: String,
): String {
    return when {
        homeResolvedSpotifyTrackId(track) != null -> songLabel
        homeResolvedSpotifyArtistId(track) != null -> artistLabel
        else -> fallbackLabel
    }
}

private fun homeResolvedSpotifyTrackId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyTrackId.isNullOrBlank()) return track.spotifyTrackId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val trackIndex = pathSegments.indexOf("track")
    if (trackIndex == -1 || trackIndex + 1 >= pathSegments.size) return null
    return pathSegments[trackIndex + 1]
}

private fun homeResolvedSpotifyArtistId(track: com.skydown.shared.model.Track): String? {
    if (!track.spotifyArtistId.isNullOrBlank()) return track.spotifyArtistId
    val externalUrl = track.externalUrl ?: return null
    val parsed = Uri.parse(externalUrl)
    val pathSegments = parsed.pathSegments
    val artistIndex = pathSegments.indexOf("artist")
    if (artistIndex == -1 || artistIndex + 1 >= pathSegments.size) return null
    return pathSegments[artistIndex + 1]
}

private enum class FounderBriefingMode(val wireValue: String) {
    Private("private"),
    Group("group"),
}

private data class FounderBriefingSheetState(
    val mode: FounderBriefingMode,
    val title: String,
    val body: String,
    val metaLine: String? = null,
)

/**
 * Eine Zeile Status aus [briefingMeta] (Cloud Function), fuer Private + Group.
 */
private fun formatBriefingMetaLine(raw: Any?): String? {
    val m = raw as? Map<*, *> ?: return null
    if (m.isEmpty()) return null
    val parts = ArrayList<String>(4)
    (m["businessDate"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { parts.add("Berichtstag $it") }
    when (val q = (m["dataQuality"] as? String)?.trim()?.lowercase().orEmpty()) {
        "complete" -> parts.add("Daten vollstaendig")
        "partial" -> parts.add("Teildaten")
        "no_kpi_doc" -> parts.add("KPI-Dokument fehlt")
        else -> if (q.isNotEmpty()) parts.add(q)
    }
    (m["kpiUpdatedAt"] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { k ->
        val short = if (k.length > 36) k.take(36) + "…" else k
        parts.add("KPI $short")
    }
    if (parts.isEmpty()) return null
    return parts.joinToString(" · ")
}

private fun jsonObjectToKotlinMap(json: JSONObject): Map<String, Any?> {
    val keys = json.keys().asSequence().toList()
    return buildMap {
        for (key in keys) {
            put(key, json.opt(key))
        }
    }
}

private fun parseJsonObject(raw: String?): JSONObject? {
    val trimmed = raw?.trim().orEmpty()
    if (trimmed.isBlank()) return null
    return runCatching { JSONObject(trimmed) }.getOrNull()
}

private fun coerceAnyToTrimmedString(value: Any?, maxChars: Int = 12000, depth: Int = 0): String {
    if (depth > 12 || maxChars < 1) return ""
    return when (value) {
        null -> ""
        is String -> value.trim().take(maxChars)
        is Map<*, *> -> coerceMapToBriefingString(value, maxChars, depth)
        is List<*> -> {
            for (item in value) {
                val piece = coerceAnyToTrimmedString(item, maxChars, depth + 1)
                if (piece.isNotBlank()) return piece
            }
            runCatching { JSONArray(value).toString() }.getOrNull()?.trim()?.take(maxChars).orEmpty()
        }
        else -> value.toString().trim().take(maxChars)
    }
}

/** Firebase Callable maps may nest briefing text; JSONObject.put often fails on mixed types → empty. */
private fun coerceMapToBriefingString(map: Map<*, *>, maxChars: Int, depth: Int): String {
    val textKeys = listOf(
        "founderBriefingMarkdown",
        "briefingPrivate", "briefingGroup", "briefingResults", "text", "content", "message", "summary",
        "description", "output", "body",
        "private", "group", "reply", "result", "markdown", "value",
    )
    for (key in textKeys) {
        val v = map[key] ?: continue
        when (v) {
            is String -> if (v.isNotBlank()) return v.trim().take(maxChars)
            is Map<*, *> -> {
                val nested = coerceAnyToTrimmedString(v, maxChars, depth + 1)
                if (nested.isNotBlank()) return nested
            }
            is List<*> -> {
                for (item in v) {
                    val nested = coerceAnyToTrimmedString(item, maxChars, depth + 1)
                    if (nested.isNotBlank()) return nested
                }
            }
            else -> {
                val s = v.toString().trim()
                if (s.isNotBlank() && s != "{}" && s != "[]" && s != "null") return s.take(maxChars)
            }
        }
    }
    for ((rawKey, rawVal) in map) {
        val key = rawKey as? String ?: continue
        if (key in textKeys) continue
        when (rawVal) {
            is Map<*, *>, is List<*> -> {
                val nested = coerceAnyToTrimmedString(rawVal, maxChars, depth + 1)
                if (nested.isNotBlank()) return nested
            }
            is String -> if (rawVal.isNotBlank()) return rawVal.trim().take(maxChars)
            else -> Unit
        }
    }
    return runCatching {
        val json = JSONObject()
        for ((rawKey, rawVal) in map) {
            val key = rawKey as? String ?: continue
            when (rawVal) {
                null -> json.put(key, JSONObject.NULL)
                is String, is Number, is Boolean -> json.put(key, rawVal)
                else -> json.put(key, rawVal.toString())
            }
        }
        json.toString().trim().take(maxChars)
    }.getOrElse {
        map.entries.joinToString(
            separator = "\n",
            limit = 40,
            truncated = "\n…",
        ) { entry ->
            val k = entry.key?.toString() ?: "?"
            val v = coerceAnyToTrimmedString(entry.value, minOf(800, maxChars), depth + 1)
            "$k=$v"
        }.trim().take(maxChars)
    }
}

private fun unwrapAutomationEnvelope(map: Map<*, *>?): Map<*, *>? {
    if (map == null) return null
    if (!map.containsKey("body")) return null
    // Only un-wrap { status, headers, body } / Activepieces-style envelopes, not any map
    // that also has a "body" key next to a top-level "private" or "message".
    if (!map.containsKey("status") && !map.containsKey("headers")) return null
    val statusRaw = map["status"]
    val status = when (statusRaw) {
        is Number -> statusRaw.toInt()
        is String -> statusRaw.trim().toIntOrNull()
        else -> null
    }
    val body = map["body"] ?: return null
    // Activepieces sometimes returns status as string ("200"). Treat unknown/missing as OK if body exists.
    if (status != null && status != 200) return null
    return when (body) {
        is Map<*, *> -> body
        is String -> parseJsonObject(body)?.let(::jsonObjectToKotlinMap)
        else -> null
    }
}

private fun bodyMapFrom(map: Map<*, *>?): Map<*, *>? {
    val direct = map?.get("body") ?: return null
    return when (direct) {
        is Map<*, *> -> direct
        is String -> parseJsonObject(direct)?.let(::jsonObjectToKotlinMap)
        else -> null
    }
}

private fun unwrapAutomationResponseMap(root: Map<*, *>): Map<*, *> {
    var m: Map<*, *> = root
    var guard = 0
    while (guard < 10) {
        guard++
        val e0 = unwrapAutomationEnvelope(m)
        if (e0 != null) {
            m = e0
            continue
        }
        val d = m["data"] as? Map<*, *>
        val e1 = d?.let { unwrapAutomationEnvelope(it) }
        if (e1 != null) {
            m = e1
            continue
        }
        // If data / root looks like the HTTP envelope, peel body even without unwrap (edge case).
        if (d != null && (d.containsKey("status") || d.containsKey("headers"))) {
            val b = bodyMapFrom(d)
            if (b != null) {
                m = b
                continue
            }
        }
        if (m.containsKey("status") || m.containsKey("headers")) {
            val b = bodyMapFrom(m)
            if (b != null) {
                m = b
                continue
            }
        }
        break
    }
    return m
}

/**
 * Callable often sets [message] to a human summary while [private] / [results] stay empty.
 * Filter out the short “Test an … gesendet” style status lines, and **empty JSON** like `"{}"` /
 * `"[]"` (Cloud Functions / Activepieces sometimes stringify an empty object as the message).
 */
private fun isGenericWorkflowStatusMessage(message: String): Boolean {
    val t = message.trim()
    if (t.isEmpty()) return true
    if (t.length <= 3 && t.equals("ok", true)) return true
    val lower = t.lowercase(Locale.ROOT)
    if (t.length < 200) {
        if (lower.startsWith("test an ") && (lower.contains("gesendet") || lower.contains("vollständig"))) {
            return true
        }
        if (lower.startsWith("test for ") && (lower.contains("sent") || lower.contains("submitted"))) {
            return true
        }
    }
    return false
}

/**
 * @return true if [message] must not be shown as the briefing body (placeholders, empty JSON, “null” string).
 */
private fun isUnusableAsCallableBriefingText(message: String): Boolean {
    if (isGenericWorkflowStatusMessage(message)) return true
    val t = message.trim()
    if (t == "{}" || t == "[]") return true
    if (t.equals("null", ignoreCase = true) && t.length == 4) return true
    if (t.startsWith("{") && t.endsWith("}")) {
        val keyCount = runCatching { JSONObject(t).length() }.getOrNull() ?: -1
        if (keyCount == 0) return true
    } else if (t.startsWith("[") && t.endsWith("]")) {
        val len = runCatching { JSONArray(t).length() }.getOrNull() ?: -1
        if (len == 0) return true
    }
    return false
}

private suspend fun requestFounderBriefingFromCallable(
    context: Context,
    uid: String,
    mode: FounderBriefingMode,
): FounderBriefingSheetState? {
    val functions = FirebaseFunctions.getInstance("us-central1")
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val requestId = "android-home-${mode.wireValue}-${System.currentTimeMillis()}"
    val payload = mapOf(
        "trigger" to "home_founder_briefing",
        "source" to "android_home_owner_buttons",
        "automationScope" to "owner",
        "data" to mapOf(
            "uid" to uid,
            "mode" to "briefing",
            "briefingTarget" to mode.wireValue,
            "date" to formatter.format(Date()),
            "requestId" to requestId,
            "isOwner" to true,
        ),
    )
    val callableResult = runCatching {
        functions.callWithAppCheckRetry(
            functionName = "triggerWorkflowAutomation",
            payload = payload,
        )
    }.getOrElse { error ->
        Log.e("FounderBriefing", "callWithAppCheckRetry threw", error)
        Toast.makeText(
            context,
            "DEBUG callable threw: ${error.javaClass.simpleName}",
            Toast.LENGTH_LONG,
        ).show()
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(
                context,
                "DEBUG callable threw: ${error.javaClass.simpleName}",
                Toast.LENGTH_LONG,
            ).show()
        }, 1600)
        return null
    }

    val result = callableResult
    val rootData = result.data as? Map<*, *> ?: run {
        Log.e("FounderBriefing", "callableResult.data is not a map: ${result.data?.javaClass?.name}")
        Toast.makeText(context, "DEBUG result.data not map", Toast.LENGTH_LONG).show()
        Handler(Looper.getMainLooper()).postDelayed({
            Toast.makeText(context, "DEBUG result.data not map", Toast.LENGTH_LONG).show()
        }, 1600)
        return null
    }
    Log.e(
        "FounderBriefing",
        "triggerWorkflowAutomation raw keys=${rootData.keys.joinToString(",")}",
    )
    val resolvedRoot = unwrapAutomationResponseMap(rootData)
    val data = (resolvedRoot["data"] as? Map<*, *>) ?: resolvedRoot
    val bodyMap =
        bodyMapFrom(data) ?:
            bodyMapFrom(resolvedRoot) ?:
            (unwrapAutomationEnvelope(resolvedRoot) ?: unwrapAutomationEnvelope(data))

    val responseForWebhookJsonObj = run {
        val raw =
            (bodyMap?.get("responseForWebhookJson") as? String)?.trim().orEmpty().ifBlank { null }
                ?: (data["responseForWebhookJson"] as? String)?.trim().orEmpty().ifBlank { null }
                ?: (resolvedRoot["responseForWebhookJson"] as? String)?.trim().orEmpty().ifBlank { null }
        parseJsonObject(raw)
    }

    val nestedBodyJson = (data["body"] as? String)?.trim().takeUnless { it.isNullOrBlank() }?.let { raw ->
        runCatching { JSONObject(raw) }.getOrNull()
    } ?: (resolvedRoot["body"] as? String)?.trim().takeUnless { it.isNullOrBlank() }?.let { raw ->
        runCatching { JSONObject(raw) }.getOrNull()
    }

    val topLevelMessage = listOf(
        data["message"],
        resolvedRoot["message"],
        bodyMap?.get("message"),
        responseForWebhookJsonObj?.optString("message"),
        nestedBodyJson?.optString("message"),
    ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()

    val rawResults = bodyMap?.get("briefingResults")
        ?: data["briefingResults"]
        ?: resolvedRoot["briefingResults"]
        ?: bodyMap?.get("results")
        ?: data["results"]
        ?: resolvedRoot["results"]
    val dataBodyMap = data["body"] as? Map<*, *>
    val rootBodyMap = resolvedRoot["body"] as? Map<*, *>
    val privatePreview = listOf(
        bodyMap?.get("founderBriefingMarkdown"),
        data["founderBriefingMarkdown"],
        resolvedRoot["founderBriefingMarkdown"],
        bodyMap?.get("briefingPrivate"),
        data["briefingPrivate"],
        resolvedRoot["briefingPrivate"],
        bodyMap?.get("private"),
        dataBodyMap?.get("briefingPrivate"),
        rootBodyMap?.get("briefingPrivate"),
        dataBodyMap?.get("founderBriefingMarkdown"),
        rootBodyMap?.get("founderBriefingMarkdown"),
        responseForWebhookJsonObj?.opt("private"),
        nestedBodyJson?.opt("private"),
        dataBodyMap?.get("private"),
        rootBodyMap?.get("private"),
        data["private"],
        resolvedRoot["private"],
    ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
    val resultCountForLog = when (rawResults) {
        is List<*> -> rawResults.size
        is Map<*, *> -> rawResults.size
        else -> 0
    }
    Log.d(
        "FounderBriefing",
        "resolved keys=${resolvedRoot.keys.joinToString(",")} | data keys=${data.keys.joinToString(",")} | body keys=${bodyMap?.keys?.joinToString(",")} | previewLen=${privatePreview.length} | messageLen=${topLevelMessage.length} msgUsable=${!isUnusableAsCallableBriefingText(topLevelMessage)} | resultsType=${rawResults?.javaClass?.simpleName} resultsN=$resultCountForLog",
    )
    val workflowStatus = (
        (data["workflowStatus"] as? String)
            ?: (bodyMap?.get("workflowStatus") as? String)
            ?: responseForWebhookJsonObj?.optString("workflowStatus")
            ?: (resolvedRoot["workflowStatus"] as? String)
    )?.trim()?.lowercase().orEmpty()
    if (workflowStatus == "failed") {
        val message = (
            (data["message"] as? String)
                ?: (bodyMap?.get("message") as? String)
                ?: responseForWebhookJsonObj?.optString("message")
                ?: (resolvedRoot["message"] as? String)
        )?.trim().orEmpty()
        throw IllegalStateException(
            if (message.isBlank()) "Workflow konnte nicht abgeschlossen werden."
            else message
        )
    }

    val resultEntries: List<*> = when (rawResults) {
        is List<*> -> rawResults
        is Map<*, *> -> rawResults.values.toList()
        else -> emptyList<Any>()
    }
    var privateText = ""
    var groupText = ""
    var fallbackText = ""

    val directPrivate = listOf(
        data["founderBriefingMarkdown"],
        resolvedRoot["founderBriefingMarkdown"],
        bodyMap?.get("founderBriefingMarkdown"),
        data["briefingPrivate"],
        resolvedRoot["briefingPrivate"],
        bodyMap?.get("briefingPrivate"),
        dataBodyMap?.get("briefingPrivate"),
        rootBodyMap?.get("briefingPrivate"),
        dataBodyMap?.get("founderBriefingMarkdown"),
        rootBodyMap?.get("founderBriefingMarkdown"),
        data["private"],
        resolvedRoot["private"],
        bodyMap?.get("private"),
        dataBodyMap?.get("private"),
        rootBodyMap?.get("private"),
        responseForWebhookJsonObj?.opt("private"),
        nestedBodyJson?.opt("private"),
    ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
    val directGroup = listOf(
        data["briefingGroup"],
        resolvedRoot["briefingGroup"],
        bodyMap?.get("briefingGroup"),
        data["group"],
        resolvedRoot["group"],
        bodyMap?.get("group"),
        responseForWebhookJsonObj?.opt("group"),
        nestedBodyJson?.opt("group"),
    ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
    if (directPrivate.isNotBlank()) {
        privateText = directPrivate
        fallbackText = directPrivate
    }
    if (directGroup.isNotBlank()) {
        groupText = directGroup
        if (fallbackText.isBlank()) fallbackText = directGroup
    }

    val responseForWebhook =
        (bodyMap?.get("responseForWebhook") as? Map<*, *>)
            ?: (data["responseForWebhook"] as? Map<*, *>)
            ?: (resolvedRoot["responseForWebhook"] as? Map<*, *>)
    if (privateText.isBlank()) {
        privateText = listOf(
            responseForWebhook?.get("private"),
            findFirstNonBlankStringByKeys(resolvedRoot, setOf("private")),
        ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
    }
    if (groupText.isBlank()) {
        groupText = listOf(
            responseForWebhook?.get("group"),
            findFirstNonBlankStringByKeys(resolvedRoot, setOf("group")),
        ).map { coerceAnyToTrimmedString(it) }.firstOrNull { it.isNotBlank() }.orEmpty()
    }
    if (fallbackText.isBlank()) {
        fallbackText = findFirstNonBlankStringByKeys(resolvedRoot, setOf("content", "text", "private", "group"))
    }

    for (rawEntry in resultEntries) {
        when (rawEntry) {
            is String -> {
                val t = rawEntry.trim()
                if (t.isNotBlank() && fallbackText.isBlank()) fallbackText = t
            }
            is Map<*, *> -> {
                val entry = rawEntry
                val entryType = (entry["type"] as? String)?.trim()?.lowercase().orEmpty()
                if (entryType == "workflow") continue
                val content = extractBriefingTextFromAutomationResultEntry(entry)
                if (content.isBlank()) continue
                if (fallbackText.isBlank()) fallbackText = content
                val title = (entry["title"] as? String)?.trim()?.lowercase().orEmpty()
                when {
                    title.contains("private") || title.contains("only me") || title.contains("nur") ->
                        privateText = content
                    title.contains("group") || title.contains("gruppe") || title.contains("team") ->
                        groupText = content
                    else -> {
                        if (privateText.isBlank() && mode == FounderBriefingMode.Private) {
                            privateText = content
                        }
                        if (groupText.isBlank() && mode == FounderBriefingMode.Group) {
                            groupText = content
                        }
                    }
                }
            }
            else -> Unit
        }
    }

    val messageFallback = topLevelMessage.takeUnless { isUnusableAsCallableBriefingText(it) }.orEmpty()
    val text = if (mode == FounderBriefingMode.Private) {
        privateText.ifBlank { fallbackText }.ifBlank { messageFallback }
    } else {
        groupText.ifBlank { fallbackText }.ifBlank { messageFallback }
    }
    if (text.isBlank()) {
        val errMessage = (data["message"] as? String)?.trim().orEmpty()
        val msgHint =
            "msgLen=${topLevelMessage.length} msgRejected=${topLevelMessage.isNotBlank() && isUnusableAsCallableBriefingText(topLevelMessage)}"
        val debugSummary =
            "DBG preview=${privatePreview.isNotBlank()} private=${privateText.isNotBlank()} group=${groupText.isNotBlank()} fallback=${fallbackText.isNotBlank()} $msgHint keys=${resolvedRoot.keys.joinToString(",")}"
        val userMessage = when {
            errMessage.isNotBlank() && !isUnusableAsCallableBriefingText(errMessage) -> errMessage
            else ->
                "Kein Briefing-Text. Die Cloud Function liefert leere Inhalte (private / results). " +
                    "In den Functions-Logs pruefen, ob der Webhook-Response den vollstaendigen Body inkl. private oder results[].content hat."
        }
        Log.e("FounderBriefing", "Briefing leer. $debugSummary | callableMessage=$errMessage")
        throw IllegalStateException(userMessage)
    }
    val rawBriefingMeta = data["briefingMeta"]
        ?: resolvedRoot["briefingMeta"]
        ?: bodyMap?.get("briefingMeta")
    return FounderBriefingSheetState(
        mode = mode,
        title = if (mode == FounderBriefingMode.Private) "Founder Analyse" else "Team Update",
        body = text,
        metaLine = formatBriefingMetaLine(rawBriefingMeta),
    )
}

private fun findFirstNonBlankStringByKeys(
    value: Any?,
    keys: Set<String>,
): String {
    when (value) {
        is Map<*, *> -> {
            for ((rawKey, rawVal) in value) {
                val key = rawKey as? String ?: continue
                if (key in keys) {
                    val candidate = coerceAnyToTrimmedString(rawVal)
                    if (candidate.isNotBlank()) return candidate
                }
            }
            for ((_, nested) in value) {
                val nestedHit = findFirstNonBlankStringByKeys(nested, keys)
                if (nestedHit.isNotBlank()) return nestedHit
            }
        }
        is List<*> -> {
            for (entry in value) {
                val nestedHit = findFirstNonBlankStringByKeys(entry, keys)
                if (nestedHit.isNotBlank()) return nestedHit
            }
        }
    }
    return ""
}

/** Text fields returned by Firebase Functions / Activepieces agent result normalization. */
private fun extractBriefingTextFromAutomationResultEntry(entry: Map<*, *>): String {
    val kind = (entry["type"] as? String)?.trim()?.lowercase(Locale.ROOT).orEmpty()
    if (kind == "table") {
        val table = formatFounderAutomationTableResult(entry)
        if (table.isNotBlank()) return table
    }
    for (key in listOf("text", "content", "message", "summary", "description", "output", "body")) {
        val chunk = coerceAnyToTrimmedString(entry[key])
        if (chunk.isNotBlank()) return chunk
    }
    val html = coerceAnyToTrimmedString(entry["html"])
    if (html.isNotBlank()) {
        return html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()
    }
    return ""
}

/** Renders Activepieces `type: "table"` blocks (columns + rows) as plain text for the briefing sheet. */
private fun formatFounderAutomationTableResult(entry: Map<*, *>): String {
    val columns = entry["columns"] as? List<*> ?: return ""
    val rows = entry["rows"] as? List<*> ?: return ""
    if (columns.isEmpty() || rows.isEmpty()) return ""
    val colLabels = columns.map { coerceAnyToTrimmedString(it) }
    val lines = ArrayList<String>(rows.size + 1)
    lines.add(colLabels.joinToString(" | "))
    for (row in rows) {
        val cells: List<String> = when (row) {
            is List<*> -> {
                val r = row.map { coerceAnyToTrimmedString(it) }
                if (r.size >= colLabels.size) {
                    r.take(colLabels.size)
                } else {
                    r + List(colLabels.size - r.size) { "" }
                }
            }
            is Map<*, *> -> colLabels.map { k -> coerceAnyToTrimmedString(row[k]) }
            else -> listOf(coerceAnyToTrimmedString(row))
        }
        lines.add(cells.joinToString(" | "))
    }
    return lines.joinToString("\n")
}

private fun shareFounderBriefingToWhatsApp(context: android.content.Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        setPackage("com.whatsapp")
    }
    if (runCatching { context.packageManager.resolveActivity(intent, 0) }.getOrNull() != null) {
        openExternalIntent(
            context = context,
            intent = intent,
            missingMessage = "WhatsApp konnte nicht geoeffnet werden.",
        )
    } else {
        shareAiText(context, "Founder Briefing", text)
    }
}

private const val homeDestinationNicmaProducer = "home_nicma_producer"
private const val homeDestinationVideoHub = "home_video_hub"
/** LazyColumn index of [HomeMediaCluster] (0-based: hero, utility, media, footer). */
private const val homeMediaClusterSectionIndex = 1
private const val homeSignalTotal = 2
